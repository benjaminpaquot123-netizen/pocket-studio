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
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.model.tuto.TutoStep
import com.pocketstudio.model.tuto.TutoTarget
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
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

/** A place « Je le fais » opens: the Guide's screen ([open]) and, for a drum step, the pad to pick. */
data class TutoGo(val open: GuideOpen, val pad: Int? = null)

/** The tutorial as shown: its chapters (for its level), where it is, what plays. */
data class TutoUi(
    val progress: TutoProgress,
    val chapters: List<TutoChapter>,
    val chapter: Int,
    val step: Int,
    /** The panel unfolded (folded: one line, the screen free to work on). */
    val open: Boolean = true,
    /** « ▶ Écouter l'exemple » is being prepared or plays. */
    val preparing: Boolean = false,
    val playing: Boolean = false,
    val failed: Boolean = false,
    /** The step just ticked by itself (« Bravo ! »), until the next is looked at. */
    val bravo: Boolean = false,
) {
    val current: TutoStep get() = chapters[chapter].steps[step]
    val total: Int get() = chapters.sumOf { it.steps.size }
    val doneCount: Int get() = chapters.sumOf { c -> c.steps.count { it.id in progress.done } }
    val finished: Boolean get() = doneCount >= total
}

/**
 * « Tuto de A à Z » in a project ([projectId]'s entry of [store]): follows the project and ticks a
 * step whose detector holds ([TutoChecks.done]), « Montre-moi » as one undo step ([edit]),
 * « Écouter l'exemple » rendered from the course's target ([render]: never the project) and
 * played by [player], « Je le fais » sent to [goes].
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
) {
    private val _ui = MutableStateFlow<TutoUi?>(null)
    val ui: StateFlow<TutoUi?> = _ui.asStateFlow()

    private val _goes = MutableSharedFlow<TutoGo>(extraBufferCapacity = 4)
    val goes: SharedFlow<TutoGo> = _goes.asSharedFlow()

    private var target: TutoTarget? = null

    /** The "changed:…" step's signature when it began (kept in memory: a restart takes it afresh). */
    private var baseline: Pair<String, Any?>? = null
    private var listenJob: Job? = null

    init {
        store.load(projectId)?.let(::show)
        scope.launch { project.debounce(debounceMs).collect { check(it) } }
    }

    /** The tutorial was saved for this project but quit: « Reprendre le tuto » can bring it back. */
    val resumable: Boolean get() = _ui.value == null && store.load(projectId)?.let { TutoCourse.of(it.style) != null } == true

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
        _ui.value?.let { store.save(projectId, it.progress.copy(active = false)) }
        _ui.value = null
    }

    /** « Passer en Pro » (or back to Facile): the steps done stay done. */
    fun setLevel(level: TutoLevel) {
        val u = _ui.value ?: return
        show(u.progress.copy(level = level, current = u.current.id))
    }

    fun toggleOpen() { _ui.value = _ui.value?.let { it.copy(open = !it.open, bravo = false) } }

    fun goTo(chapter: Int, step: Int = 0) {
        val u = _ui.value ?: return
        val c = u.chapters.getOrNull(chapter) ?: return
        val s = c.steps.getOrNull(step) ?: return
        save(u.progress.copy(current = s.id), open = true)
    }

    fun next() = move(+1)
    fun previous() = move(-1)

    private fun move(by: Int) {
        val u = _ui.value ?: return
        val flat = u.chapters.flatMap { it.steps }
        val i = (flat.indexOf(u.current) + by).coerceIn(0, flat.lastIndex)
        save(u.progress.copy(current = flat[i].id), open = true)
    }

    /** « C'est fait » / « J'ai compris »: a step only the user can tick. */
    fun markDone() {
        val u = _ui.value ?: return
        tick(setOf(u.current.id), advance = true, bravo = false)
    }

    /** « Je le fais »: the screen of the step, its control lit; the panel folds to leave room. */
    fun doIt() {
        val u = _ui.value ?: return
        val s = u.current
        if (s.spot.isEmpty()) return
        val pad = if (s.check.startsWith("lane:") || s.check == "velocity:hat") TutoChecks.laneOf(s.check.substringAfter(':')) else null
        _goes.tryEmit(TutoGo(GuideOpen(s.spot, s.target), pad))
        _ui.value = u.copy(open = false, bravo = false)
    }

    /** « Montre-moi »: the step's target put into the project, one undo step. False when the step has none (or nothing changes). */
    fun showMe(): Boolean {
        val u = _ui.value ?: return false
        val t = target ?: return false
        val step = u.current
        if (TutoChecks.show(step, project.value, t) == null) return false
        edit { TutoChecks.show(step, this, t) ?: this }
        return true
    }

    /** Whether the current step has a « Montre-moi ». */
    fun canShow(): Boolean {
        val u = _ui.value ?: return false
        val t = target ?: return false
        return TutoChecks.show(u.current, project.value, t) != null
    }

    /** The dotted cells of the step shown on a pad ([TutoChecks.hint]), null when none. */
    fun hint(steps: Int): Pair<Int, Set<Int>>? {
        val u = _ui.value ?: return null
        return target?.let { TutoChecks.hint(u.current, it, steps) }
    }

    /** « ▶ Écouter l'exemple » (again: it stops). A listening step is ticked once it was heard. */
    fun listen() {
        val u = _ui.value ?: return
        if (u.preparing || u.playing) {
            stopListening()
            return
        }
        val t = target ?: return
        val step = u.current
        pauseMusic()
        _ui.value = u.copy(preparing = true, failed = false, bravo = false)
        listenJob = scope.launch {
            val example = withContext(worker) { TutoChecks.example(step, t) }
            val pcm = example?.let { render(it) }
            if (pcm == null) {
                _ui.value = _ui.value?.copy(preparing = false, failed = true)
                return@launch
            }
            val played = player.play(Pcm(pcm, 2, OfflineExport.SAMPLE_RATE)) {
                scope.launch { _ui.value = _ui.value?.copy(playing = false) }
            }
            _ui.value = _ui.value?.copy(preparing = false, playing = played, failed = !played)
            if (played && step.listening) tick(setOf(step.id), advance = false, bravo = false)
        }
    }

    fun stopListening() {
        listenJob?.cancel()
        listenJob = null
        player.stop()
        _ui.value = _ui.value?.copy(preparing = false, playing = false)
    }

    // ---- inside ----

    private fun show(p: TutoProgress) {
        val course = TutoCourse.of(p.style) ?: return
        target = TutoTarget.of(p.style) ?: return
        if (!p.active) return
        save(p, chapters = course.chaptersFor(p.level), open = true)
        scope.launch { check(project.value) }
    }

    /** [p] saved and shown on its current step (the first not done when none is named). */
    private fun save(p: TutoProgress, chapters: List<TutoChapter> = _ui.value?.chapters.orEmpty(), open: Boolean = _ui.value?.open ?: true, bravo: Boolean = false) {
        val course = TutoCourse.of(p.style) ?: return
        val list = if (_ui.value?.progress?.level == p.level && chapters.isNotEmpty()) chapters else course.chaptersFor(p.level)
        val flat = list.flatMap { it.steps }
        val step = flat.firstOrNull { it.id == p.current } ?: flat.firstOrNull { it.id !in p.done } ?: flat.last()
        val chapter = list.indexOfFirst { step in it.steps }
        val saved = p.copy(current = step.id)
        store.save(projectId, saved)
        val before = _ui.value
        if (before?.current?.id != step.id) {
            stopListening()
            baseline = step.check.takeIf { it.startsWith("changed:") }?.let { step.id to TutoChecks.signature(it.removePrefix("changed:"), project.value) }
        }
        val kept = _ui.value
        _ui.value = TutoUi(saved, list, chapter, list[chapter].steps.indexOf(step), open = open, bravo = bravo,
            preparing = kept?.preparing == true && kept.current.id == step.id, playing = kept?.playing == true && kept.current.id == step.id)
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
        save(u.progress.copy(done = done, current = next.id), open = if (moveOn) true else u.open, bravo = bravo && moveOn)
    }

    /** Every step whose detector holds now is ticked (a "changed:…" one only while it is shown). */
    private suspend fun check(p: Project) {
        val u = _ui.value ?: return
        val t = target ?: return
        val base = baseline?.takeIf { it.first == u.current.id }?.second
        val found = withContext(worker) {
            u.chapters.flatMap { it.steps }.filter { s ->
                s.id !in u.progress.done && (!s.check.startsWith("changed:") || s.id == u.current.id) && TutoChecks.done(s, p, t, base)
            }.map { it.id }.toSet()
        }
        if (found.isNotEmpty() && _ui.value?.progress?.style == u.progress.style) tick(found, advance = true, bravo = true)
    }

    companion object {
        /** Before / after of an example: two renders one after the other. */
        fun joined(before: FloatArray?, after: FloatArray): FloatArray =
            if (before == null) after else LearnViewModel.join(listOf(before, after), LearnViewModel.GAP_FRAMES).first
    }
}
