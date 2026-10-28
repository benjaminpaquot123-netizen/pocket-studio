package com.pocketstudio.app.tuto

import android.content.Context
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.app.guide.GuideOpen
import com.pocketstudio.app.learn.LearnViewModel
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.Project
import com.pocketstudio.model.tuto.TutoChapter
import com.pocketstudio.model.tuto.TutoChecks
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoExample
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoNeed
import com.pocketstudio.model.tuto.TutoSound
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.model.tuto.TutoStep
import com.pocketstudio.model.tuto.TutoTarget
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Where each project's tutorial is (its [TutoProgress]), kept across restarts. */
interface TutoStore {
    fun load(projectId: String): TutoProgress?
    fun save(projectId: String, progress: TutoProgress?)
}

/** The app's [TutoStore]: one entry per project in the app's preferences. */
class PrefsTutoStore(context: Context) : TutoStore {
    private val prefs = context.applicationContext.getSharedPreferences("tuto", Context.MODE_PRIVATE)
    override fun load(projectId: String) = TutoProgress.decode(prefs.getString(projectId, null))
    override fun save(projectId: String, progress: TutoProgress?) {
        prefs.edit().apply { if (progress == null) remove(projectId) else putString(projectId, progress.encode()) }.apply()
    }
}

/** Tests: the player of the examples in place of the engine's (Robolectric has no engine). */
object TutoTestHooks {
    @androidx.annotation.VisibleForTesting
    var player: GuideListenPlayer? = null
}

/**
 * Where the tutorial sends the screen: the Guide's screen ([open], with its control lit by a short
 * pulsing ring when [spot]), the drum pad to show ([pad]), the notes of track [notes], the
 * tutorial's own key sheet ([keySheet]).
 */
data class TutoGo(val open: GuideOpen? = null, val pad: Int? = null, val notes: Int? = null, val keySheet: Boolean = false, val spot: Boolean = true)

/** « Montre-moi » under way: the target appears ([SHOWING]), stays with its caption ([HOLDING]), then fades and is undone ([FADING]). */
enum class TutoDemoPhase { SHOWING, HOLDING, FADING }

/** The tutorial as shown: its chapters (for its level), where it is, what plays. */
data class TutoUi(
    val progress: TutoProgress,
    val chapters: List<TutoChapter>,
    val chapter: Int,
    val step: Int,
    /** The panel unfolded (folded: one line, the screen free to work on). Unfolded again at each new step. */
    val open: Boolean = true,
    /** « ▶ Écouter l'exemple » is being prepared or plays. */
    val preparing: Boolean = false,
    val playing: Boolean = false,
    val failed: Boolean = false,
    /** The step just ticked by itself (« Bravo ! »), until the next is looked at. */
    val bravo: Boolean = false,
    /** What the step needs first (its main button), null: nothing. */
    val need: TutoNeed? = null,
    /** « Montre-moi » under way (null: none). */
    val demo: TutoDemoPhase? = null,
    /** While it fades: the cells it had lit on a pad (pad, steps) and how much they still show (1 → 0). */
    val demoCells: Pair<Int, Set<Int>>? = null,
    val fade: Float = 0f,
) {
    val current: TutoStep get() = chapters[chapter].steps[step]
    val total: Int get() = chapters.sumOf { it.steps.size }
    val doneCount: Int get() = chapters.sumOf { c -> c.steps.count { it.id in progress.done } }
    val finished: Boolean get() = doneCount >= total

    /** The drum pad the current step is about (its dotted cells are on it), null when none. */
    val pad: Int? get() = current.check.let { c ->
        when {
            c.startsWith("lane:") -> TutoChecks.laneOf(c.removePrefix("lane:"))
            c == "velocity:hat" -> TutoChecks.laneOf("hat")
            c == "fill" || c == "gap" -> TutoChecks.laneOf("snare")
            c == "crash" -> TutoChecks.CRASH_LANE
            else -> null
        }
    }
}

/**
 * « Tuto de A à Z » in a project ([projectId]'s entry of [store]): follows the project and ticks a
 * step whose detector holds ([TutoChecks.done]); « Montre-moi » shows the target step by step
 * ([demoShow]: frames shown and heard, never saved nor undoable) then takes it away, the step left
 * to do; « Écouter l'exemple » rendered from the course's target ([render]: never the project)
 * and played by [player], a listening step ticked once enough of it really went out; « Je le fais »
 * and the step's main button ([fulfilNeed], one undo step through [edit]) sent to [goes].
 */
@OptIn(FlowPreview::class)
class TutoController(
    private val scope: CoroutineScope,
    private val projectId: String,
    private val project: StateFlow<Project>,
    private val edit: (Project.() -> Project) -> Unit,
    private val store: TutoStore,
    private val render: suspend (TutoExample) -> FloatArray?,
    private val player: GuideListenPlayer,
    private val pauseMusic: () -> Unit = {},
    private val worker: CoroutineDispatcher = Dispatchers.Default,
    /** Detection waits for the edits to settle (a stroke of cells). */
    debounceMs: Long = 250,
    /** « Montre-moi »'s frames ([TutoDemo.frames]) shown and heard; null: the project as it was. */
    private val demoShow: (Project?) -> Unit = {},
    private val demoSound: (TutoSound) -> Unit = {},
    /** How long each frame of « Montre-moi » shows, the result stays, and it fades. */
    private val frameMs: Long = FRAME_MS,
    private val holdMs: Long = HOLD_MS,
    private val fadeMs: Long = FADE_MS,
) {
    private val _ui = MutableStateFlow<TutoUi?>(null)
    val ui: StateFlow<TutoUi?> = _ui.asStateFlow()

    private val _goes = MutableSharedFlow<TutoGo>(extraBufferCapacity = 8)
    val goes: SharedFlow<TutoGo> = _goes.asSharedFlow()

    private var target: TutoTarget? = null

    /** The "changed:…" step's signature when it began (kept in memory: a restart takes it afresh). */
    private var baseline: Pair<String, Any?>? = null
    private var listenJob: Job? = null
    private var demoJob: Job? = null

    init {
        store.load(projectId)?.let(::show)
        scope.launch { project.debounce(debounceMs).collect { check(it) } }
    }

    /** The tutorial was saved for this project but quit: « Reprendre le tuto » can bring it back. */
    val resumable: Boolean get() = _ui.value == null && store.load(projectId)?.let { TutoCourse.of(it.style) != null } == true

    /** The style and level saved for this project (« Recommencer le tuto » starts them again in a new project). */
    val saved: TutoProgress? get() = store.load(projectId)

    /** Starts (or restarts) [style]'s course at [level] in this project. */
    fun start(style: String, level: TutoLevel) {
        val p = TutoProgress(style, level)
        store.save(projectId, p)
        show(p)
    }

    fun resume() {
        store.load(projectId)?.let { show(it.copy(active = true)) }
    }

    /** « Quitter le tuto »: it folds away, kept to resume. */
    fun quit() {
        stopListening()
        stopDemo()
        _ui.value?.let { store.save(projectId, it.progress.copy(active = false)) }
        _ui.value = null
    }

    /** « Passer en Pro » (or back to Facile): the steps done stay done. */
    fun setLevel(level: TutoLevel) {
        val u = _ui.value ?: return
        show(u.progress.copy(level = level, current = u.current.id))
    }

    /** Folds or unfolds the panel (kept while the step stays: the next one opens it again). */
    fun toggleOpen() { _ui.value = _ui.value?.let { it.copy(open = !it.open, bravo = false) } }

    fun goTo(chapter: Int, step: Int = 0) {
        val u = _ui.value ?: return
        val c = u.chapters.getOrNull(chapter) ?: return
        val s = c.steps.getOrNull(step) ?: return
        save(u.progress.copy(current = s.id))
    }

    fun next() = move(+1)
    fun previous() = move(-1)

    private fun move(by: Int) {
        val u = _ui.value ?: return
        val flat = u.chapters.flatMap { it.steps }
        val i = (flat.indexOf(u.current) + by).coerceIn(0, flat.lastIndex)
        save(u.progress.copy(current = flat[i].id))
    }

    /** « C'est fait » / « J'ai compris »: a step only the user can tick. */
    fun markDone() {
        val u = _ui.value ?: return
        tick(setOf(u.current.id), advance = true, bravo = false)
    }

    /** Where « Je le fais » goes for [s] (null: nowhere). */
    private fun goFor(s: TutoStep, p: Project, spot: Boolean): TutoGo? {
        if (s.spot.isEmpty()) return null
        val pad = _ui.value?.pad
        val c = s.check
        return when {
            s.spot == KEY_SPOT -> TutoGo(keySheet = true, spot = spot)
            c.startsWith("notes:") || c == "track:sub" -> {
                val t = target ?: return null
                (TutoChecks.ghostNotes(s, p, t)?.first ?: TutoChecks.trackFor(p, if (c == "track:sub") "sub" else c.substringAfter(':')))
                    ?.let { TutoGo(notes = it, spot = spot) } ?: TutoGo(GuideOpen(s.spot, s.target), spot = spot)
            }
            else -> TutoGo(GuideOpen(s.spot, s.target), pad, spot = spot)
        }
    }

    /** « Je le fais »: the screen of the step, its control lit a moment; the panel stays as it is. */
    fun doIt() {
        val u = _ui.value ?: return
        goFor(u.current, project.value, spot = true)?.let { _goes.tryEmit(it) }
        _ui.value = u.copy(bravo = false)
    }

    /** The step's main button ([TutoUi.need]): what it needs, made in one undo step, then its screen. */
    fun fulfilNeed() {
        val u = _ui.value ?: return
        val t = target ?: return
        val step = u.current
        val need = TutoChecks.need(step, project.value, t, base()) ?: return
        var made: Int? = null
        edit {
            val r = TutoChecks.fulfil(need, step, this, t)
            made = r?.second
            r?.first ?: this
        }
        val p = project.value
        val go = made?.let { TutoGo(notes = it, spot = false) } ?: goFor(step, p, spot = false)
        go?.let { _goes.tryEmit(it) }
        scope.launch { check(project.value) }
    }

    /**
     * « Montre-moi »: the screen of the step, then its target put in a cell or a note at a time
     * (each heard), « Voilà ce qu'il fallait faire — à toi maintenant ! », then it fades and the
     * project is as it was: the step stays to do, nothing is kept in « Annuler ». False when the
     * step has none.
     */
    fun showMe(): Boolean {
        val u = _ui.value ?: return false
        val t = target ?: return false
        if (u.demo != null) return true
        val step = u.current
        val before = project.value
        val demo = TutoChecks.demo(step, before, t) ?: return false
        stopListening()
        pauseMusic()
        _ui.value = u.copy(demo = TutoDemoPhase.SHOWING, bravo = false, demoCells = null, fade = 0f)
        demoJob = scope.launch {
            var restored = false
            try {
                goFor(step, before, spot = false)?.takeIf { it.notes == null }?.let { _goes.tryEmit(it) }
                demo.frames.forEachIndexed { i, frame ->
                    demoShow(frame)
                    if (i == 0) goFor(step, frame, spot = false)?.takeIf { it.notes != null }?.let { _goes.tryEmit(it) }
                    demo.sounds[i]?.let(demoSound)
                    delay(frameMs)
                }
                _ui.value = _ui.value?.copy(demo = TutoDemoPhase.HOLDING)
                delay(holdMs)
                val pad = _ui.value?.pad
                val cells = pad?.let { lane ->
                    val shown = demo.frames.last().drums
                    (lane to (shown.lanes[lane].hits.keys - before.drums.lanes[lane].hits.keys)).takeIf { it.second.isNotEmpty() && shown.steps == before.drums.steps }
                }
                _ui.value = _ui.value?.copy(demo = TutoDemoPhase.FADING, demoCells = cells, fade = 1f)
                demoShow(null)
                restored = true
                val n = (fadeMs / FADE_TICK).coerceAtLeast(1)
                for (k in 1..n) {
                    delay(FADE_TICK)
                    _ui.value = _ui.value?.copy(fade = 1f - k / n.toFloat())
                }
            } finally {
                // Stopped on the way (another step, « Quitter »): the project as it was all the same.
                if (!restored) demoShow(null)
                if (_ui.value?.demo != null) _ui.value = _ui.value?.copy(demo = null, demoCells = null, fade = 0f)
                demoJob = null
                scope.launch { check(project.value) }
            }
        }
        return true
    }

    /** Stops « Montre-moi » where it is (the project as it was). */
    fun stopDemo() {
        demoJob?.cancel()
    }

    /** Whether the current step has a « Montre-moi ». */
    fun canShow(): Boolean {
        val u = _ui.value ?: return false
        val t = target ?: return false
        return u.demo != null || TutoChecks.show(u.current, project.value, t) != null
    }

    /** The dotted cells of the step shown on a pad ([TutoChecks.hint]), null when none. */
    fun hint(steps: Int): Pair<Int, Set<Int>>? {
        val u = _ui.value ?: return null
        return target?.let { TutoChecks.hint(u.current, it, steps) }
    }

    /** The dotted notes of a melodic step on its track ([TutoChecks.ghostNotes]), null when none. */
    fun ghostNotes(p: Project): Pair<Int, List<com.pocketstudio.model.Note>>? {
        val u = _ui.value ?: return null
        return target?.let { TutoChecks.ghostNotes(u.current, p, it) }
    }

    /**
     * « ▶ Écouter l'exemple » (again: it stops). A listening step is ticked once enough of the
     * example really went out ([GuideListenPlayer.heard]), never only because it started.
     */
    fun listen() {
        val u = _ui.value ?: return
        if (u.preparing || u.playing) {
            stopListening()
            return
        }
        val t = target ?: return
        val step = u.current
        stopDemo()
        pauseMusic()
        _ui.value = u.copy(preparing = true, failed = false, bravo = false)
        listenJob = scope.launch {
            val example = withContext(worker) { TutoChecks.example(step, t) }
            val pcm = example?.let { render(it) }
            // A render with nothing to hear is not played (it would say « Arrêter l'écoute » over silence).
            if (pcm == null || !audible(pcm)) {
                _ui.value = _ui.value?.copy(preparing = false, failed = true)
                return@launch
            }
            val frames = pcm.size / 2L
            val played = player.play(Pcm(pcm, 2, OfflineExport.SAMPLE_RATE)) {
                scope.launch { _ui.value = _ui.value?.copy(playing = false) }
            }
            _ui.value = _ui.value?.copy(preparing = false, playing = played, failed = !played)
            if (!played) return@launch
            // Ticked once half of it (or 2 s) went out to the speakers; nothing out after 3 s: said.
            val enough = minOf(frames / 2, 2L * OfflineExport.SAMPLE_RATE)
            var waited = 0L
            while (true) {
                val heard = player.heard()
                if (heard >= enough) {
                    if (step.listening) tick(setOf(step.id), advance = false, bravo = false)
                    break
                }
                if (heard <= 0 && waited >= SILENT_MS) {
                    player.stop()
                    _ui.value = _ui.value?.copy(playing = false, failed = true)
                    break
                }
                if (_ui.value?.playing != true && heard < enough) break
                delay(LISTEN_POLL_MS)
                waited += LISTEN_POLL_MS
            }
        }
    }

    fun stopListening() {
        listenJob?.cancel()
        listenJob = null
        player.stop()
        _ui.value = _ui.value?.copy(preparing = false, playing = false)
    }

    // ---- inside ----

    private fun base(): Any? = baseline?.takeIf { it.first == _ui.value?.current?.id }?.second

    private fun show(p: TutoProgress) {
        val course = TutoCourse.of(p.style) ?: return
        target = TutoTarget.of(p.style) ?: return
        if (!p.active) return
        save(p, chapters = course.chaptersFor(p.level))
        scope.launch { check(project.value) }
    }

    /** [p] saved and shown on its current step (the first not done when none is named); a new step opens the panel. */
    private fun save(p: TutoProgress, chapters: List<TutoChapter> = _ui.value?.chapters.orEmpty(), bravo: Boolean = false) {
        val course = TutoCourse.of(p.style) ?: return
        val list = if (_ui.value?.progress?.level == p.level && chapters.isNotEmpty()) chapters else course.chaptersFor(p.level)
        val flat = list.flatMap { it.steps }
        val step = flat.firstOrNull { it.id == p.current } ?: flat.firstOrNull { it.id !in p.done } ?: flat.last()
        val chapter = list.indexOfFirst { step in it.steps }
        val saved = p.copy(current = step.id)
        store.save(projectId, saved)
        val before = _ui.value
        val moved = before?.current?.id != step.id
        if (moved) {
            stopListening()
            stopDemo()
            baseline = step.check.takeIf { it.startsWith("changed:") }?.let { step.id to TutoChecks.signature(it.removePrefix("changed:"), project.value) }
        }
        val kept = _ui.value
        val same = kept != null && !moved
        _ui.value = TutoUi(
            saved, list, chapter, list[chapter].steps.indexOf(step), open = if (same) kept!!.open else true, bravo = bravo,
            preparing = same && kept!!.preparing, playing = same && kept!!.playing, failed = same && kept!!.failed,
            need = target?.let { TutoChecks.need(step, project.value, it, base()) },
            demo = if (same) kept!!.demo else null, demoCells = if (same) kept!!.demoCells else null, fade = if (same) kept!!.fade else 0f,
        )
        // A step already done when it is reached (the key already right) is ticked at once.
        if (moved && before != null) scope.launch { check(project.value) }
    }

    /** Ticks [ids]; [advance]: on to the next step not done when the current one is among them. */
    private fun tick(ids: Set<String>, advance: Boolean, bravo: Boolean) {
        val u = _ui.value ?: return
        val done = u.progress.done + ids
        // Nothing new (a second look that found what the first already ticked): nothing moves.
        if (done == u.progress.done && !(advance && u.current.id in ids)) return
        val flat = u.chapters.flatMap { it.steps }
        val moveOn = advance && u.current.id in ids
        val next = if (moveOn) flat.drop(flat.indexOf(u.current) + 1).firstOrNull { it.id !in done } ?: flat.firstOrNull { it.id !in done } ?: u.current else u.current
        save(u.progress.copy(done = done, current = next.id), bravo = bravo && moveOn)
    }

    /** Every step whose detector holds now is ticked (a "changed:…" one only while it is shown); never during « Montre-moi ». */
    private suspend fun check(p: Project) {
        val u = _ui.value ?: return
        val t = target ?: return
        if (u.demo != null || demoJob != null) return
        val base = base()
        val (found, need) = withContext(worker) {
            u.chapters.flatMap { it.steps }.filter { s ->
                s.id !in u.progress.done && (!s.check.startsWith("changed:") || s.id == u.current.id) && TutoChecks.done(s, p, t, base)
            }.map { it.id }.toSet() to TutoChecks.need(u.current, p, t, base)
        }
        if (_ui.value?.progress?.style != u.progress.style || _ui.value?.demo != null) return
        if (found.isNotEmpty()) tick(found, advance = true, bravo = true)
        else if (_ui.value?.current?.id == u.current.id && _ui.value?.need != need) _ui.value = _ui.value?.copy(need = need)
    }

    private fun audible(pcm: FloatArray): Boolean {
        var peak = 0f
        for (v in pcm) { val a = kotlin.math.abs(v); if (a > peak) peak = a; if (peak >= AUDIBLE) return true }
        return false
    }

    companion object {
        /** The key step's own sheet (« Gamme » and « Tonique »). */
        const val KEY_SPOT = "tuto.key"
        const val FRAME_MS = 200L
        const val HOLD_MS = 2600L
        const val FADE_MS = 700L
        private const val FADE_TICK = 50L
        private const val LISTEN_POLL_MS = 100L
        /** Nothing went out this long after « Écouter l'exemple »: it says so rather than pretend. */
        const val SILENT_MS = 3000L
        /** A peak under -60 dBFS: nothing to hear. */
        private const val AUDIBLE = 0.001f

        /** Before / after of an example: two renders one after the other. */
        fun joined(before: FloatArray?, after: FloatArray): FloatArray =
            if (before == null) after else LearnViewModel.join(listOf(before, after), LearnViewModel.GAP_FRAMES).first
    }
}
