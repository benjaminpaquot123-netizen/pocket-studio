package com.pocketstudio.app.tuto

import android.content.Context
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.app.guide.GuideOpen
import com.pocketstudio.app.learn.LearnViewModel
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.Project
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.currentSceneContent
import com.pocketstudio.model.instrumentTracks
import com.pocketstudio.model.track
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
 * pulsing ring when [spot], brought into view either way), the drum pad to show ([pad]), the notes
 * of track [notes] (its next dotted note brought into view), the tutorial's own key sheet
 * ([keySheet]), the effects box of mixer channel [effects] (its send [send] drawn dotted), the
 * sound settings of instrument track [settings] (not its ready-made sounds), the fader of mixer
 * channel [strip] (the Mode simple's « Mix » tab).
 */
data class TutoGo(
    val open: GuideOpen? = null, val pad: Int? = null, val notes: Int? = null, val keySheet: Boolean = false, val spot: Boolean = true,
    val effects: Int? = null, val send: Int? = null, val settings: Int? = null, val strip: Int? = null,
    /** The synth knobs ([com.pocketstudio.model.SynthPatch] indices) a "changed:synth:…" step names: each ringed in the settings. */
    val knobs: List<Int> = emptyList(),
)

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
    /** The step just ticked by its detector (« Bravo ! »): it stays shown until « Étape suivante ». */
    val bravo: Boolean = false,
    /** What the step needs first (its main button), null: nothing. */
    val need: TutoNeed? = null,
    /** « Montre-moi » under way (null: none). */
    val demo: TutoDemoPhase? = null,
    /** While it fades: the cells it had lit on a pad (pad, steps) and how much they still show (1 → 0). */
    val demoCells: Pair<Int, Set<Int>>? = null,
    val fade: Float = 0f,
    /**
     * Testers (lot « tuto 4 »): « Suivant » past the last step with steps still to do: the panel says
     * which ones (one touch goes there) instead of going back to the first of them without a word.
     */
    val atEnd: Boolean = false,
    /** What the step's main button just made, in words (« Créer la partie Intro »: what it took out). */
    val made: String? = null,
) {
    val current: TutoStep get() = chapters[chapter].steps[step]
    val total: Int get() = chapters.sumOf { it.steps.size }
    val doneCount: Int get() = chapters.sumOf { c -> c.steps.count { it.id in progress.done } }

    /** The steps still to do for the end, in order (in Pro, an example not listened to does not hold it back). */
    val left: List<TutoStep> get() = chapters.flatMap { it.steps }.filter { it.id !in progress.done && !(progress.level == TutoLevel.Pro && it.listening) }
    val finished: Boolean get() = left.isEmpty()

    /** Where step [s] is: (chapter, step) indices. */
    fun placeOf(s: TutoStep): Pair<Int, Int> = chapters.indexOfFirst { s in it.steps }.let { c -> c to (chapters.getOrNull(c)?.steps?.indexOf(s) ?: 0) }

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
 * step whose detector holds ([TutoChecks.done]), staying on it until « Étape suivante » (owner,
 * 2026-10-28: never on by itself, time to hear the result); « Montre-moi » shows the target step by step
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

    /** What a listening step with [TutoStep.hearMine] has heard so far ("mine", "example"), by step id. */
    private var heard: Pair<String, Set<String>>? = null

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
    fun toggleOpen() { _ui.value = _ui.value?.let { it.copy(open = !it.open) } }

    fun goTo(chapter: Int, step: Int = 0) {
        val u = _ui.value ?: return
        val c = u.chapters.getOrNull(chapter) ?: return
        val s = c.steps.getOrNull(step) ?: return
        save(u.progress.copy(current = s.id))
        _ui.value = _ui.value?.copy(atEnd = false)
    }

    fun next() = move(+1)
    fun previous() = move(-1)

    private fun move(by: Int) {
        val u = _ui.value ?: return
        val flat = u.chapters.flatMap { it.steps }
        val at = flat.indexOf(u.current)
        // « Suivant » on the last step: the end (what is left to do, or « Bravo »), never back to the start.
        if (by > 0 && at == flat.lastIndex) {
            _ui.value = u.copy(atEnd = true, open = true, bravo = false)
            return
        }
        val i = (at + by).coerceIn(0, flat.lastIndex)
        save(u.progress.copy(current = flat[i].id))
    }

    /** « Il te reste… » closed: back to the step shown. */
    fun leaveEnd() { _ui.value = _ui.value?.copy(atEnd = false) }

    /** « C'est fait » / « J'ai compris »: a step only the user can tick. */
    fun markDone() {
        val u = _ui.value ?: return
        tick(setOf(u.current.id), advance = true, bravo = false)
    }

    /** Where « Je le fais » goes for [s] (null: nowhere): the very control the step asks for, on the track it is about. */
    private fun goFor(s: TutoStep, p: Project, spot: Boolean): TutoGo? {
        if (s.spot.isEmpty()) return null
        val pad = _ui.value?.pad
        val c = s.check
        val arg = c.substringAfter(':', "")
        val plain = TutoGo(GuideOpen(s.spot, s.target), pad, spot = spot)
        return when {
            s.spot == KEY_SPOT -> TutoGo(keySheet = true, spot = spot)
            c.startsWith("notes:") || c == "track:sub" -> {
                val t = target ?: return null
                (TutoChecks.ghostNotes(s, p, t)?.first ?: TutoChecks.trackFor(p, if (c == "track:sub") "sub" else c.substringAfter(':')))
                    ?.let { TutoGo(notes = it, spot = spot) } ?: TutoGo(GuideOpen(s.spot, s.target), spot = spot)
            }
            // Testers (lot « tuto 3 »): the filter's « Coupure » is in the sound's settings, not in its ready-made sounds.
            // Testers (lot « tuto 4 »): the very knob it names, the advanced ones unfolded (« Glissé » was 5 scrolls down).
            c.startsWith("changed:synth:") -> TutoChecks.trackFor(p, c.removePrefix("changed:synth:").substringBefore(':'))?.let { TutoGo(settings = it, spot = spot, knobs = knobsOf(c)) } ?: plain
            // A send or an effect: the effects box of the first strip the step still has to do (lot « tuto 4 »: the
            // « Accords » done, the snare's next; the Sub's pump, then the Basse's), the reverb's send there, dotted.
            c.startsWith("send:") || c.startsWith("insert:") -> (TutoChecks.channelsToDo(s, p).firstOrNull() ?: channelOf(p, s.target.substringBefore(',').ifEmpty { "bass" }))
                ?.let { TutoGo(effects = it, send = if (c.startsWith("send:")) arg.toIntOrNull() else null, spot = spot) } ?: plain
            // The volumes: the fader of the track the step names first.
            c == "changed:volumes" || c.startsWith("changed:volumes:") -> volumeRoles(c).firstNotNullOfOrNull { channelOf(p, it) }
                ?.let { TutoGo(open = GuideOpen(s.spot, s.target), strip = it, spot = spot) } ?: plain
            // « Ajouter : » and its buttons, not the parts' list above them.
            c == "song:order" -> TutoGo(GuideOpen(SONG_ADD_SPOT, ""), spot = spot)
            // The hits to take away: the first pad that still has some (« Caisse », then « Charley »).
            c == "gap" -> TutoGo(GuideOpen(s.spot, s.target), TutoChecks.toErase(s, p)?.keys?.minOrNull() ?: pad, spot = spot)
            // A part whose notes the user writes (the Drop 2's bass): its dotted notes, once the part is shown.
            c.startsWith("part:") -> target?.let { TutoChecks.ghostNotes(s, p, it) }?.let { TutoGo(notes = it.first, spot = spot) } ?: plain
            else -> plain
        }
    }

    /** The strips a "send:<bus>" step sends (as [TutoChecks.done] reads them): the reverb's « Accords » and the snare's pad, the echo's lead. */
    private fun sendChannels(p: Project, bus: Int): List<Int> =
        if (bus == 0) listOfNotNull(TutoChecks.trackFor(p, "pad")?.let(com.pocketstudio.model.InstrumentTrack::mixerChannel), com.pocketstudio.model.Mixer.padChannel(TutoChecks.laneOf("snare")))
        else listOfNotNull(TutoChecks.trackFor(p, "lead")?.let(com.pocketstudio.model.InstrumentTrack::mixerChannel))

    private fun sends(p: Project, ch: Int, bus: Int) = (p.mixer.channels.getOrNull(ch)?.sends?.getOrNull(bus) ?: 0f) > SENT

    /**
     * For a send step on [ch]'s effects box (testers, lot « tuto 4 »: nothing said the « Accords » half
     * was done): « Accords ✓ · Caisse : à régler ici », null when the step names one strip only.
     */
    fun sendNote(p: Project = project.value): String? {
        val u = _ui.value ?: return null
        val c = u.current.check
        if (!c.startsWith("send:")) return null
        val bus = c.substringAfter(':').toIntOrNull() ?: return null
        val chans = sendChannels(p, bus).takeIf { it.size > 1 } ?: return null
        val here = goFor(u.current, p, spot = false)?.effects
        return chans.joinToString(" · ") { ch ->
            val name = stripName(p, ch)
            when {
                sends(p, ch, bus) -> "$name ✓"
                ch == here -> com.pocketstudio.model.i18n.tr("%s : à régler ici", name)
                else -> com.pocketstudio.model.i18n.tr("%s : ensuite", name)
            }
        }
    }

    private fun stripName(p: Project, ch: Int): String =
        if (com.pocketstudio.model.Mixer.isPad(ch)) com.pocketstudio.app.rhythm.padName(p, ch - com.pocketstudio.model.Mixer.FIRST_PAD)
        else p.instrumentTracks.firstOrNull { com.pocketstudio.model.InstrumentTrack.mixerChannel(it.id) == ch }?.name?.let(com.pocketstudio.model.DefaultNames::shown) ?: "?"

    /** The roles a "changed:volumes:pad,lead" step lowers, in its order (the target's first for a plain one). */
    private fun volumeRoles(c: String): List<String> =
        c.removePrefix("changed:volumes").removePrefix(":").split(',').map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { listOf("pad") }

    /** « Partie « Intro » créée (copie du Groove) : sans Kick, Basse. » from the part just made in [after]. */
    private fun partMade(before: Project, after: Project, label: String): String {
        val full = before.allScenes.map(TutoChecks::bricks).maxByOrNull { it.size }.orEmpty()
        val now = TutoChecks.bricks(after.currentSceneContent())
        val out = (full - now).map { brickName(after, it) }.distinct()
        val name = com.pocketstudio.model.Scene.shownLabel(label)
        return if (out.isEmpty()) com.pocketstudio.model.i18n.tr("Partie « %s » créée : tout le Groove y joue.", name)
        else com.pocketstudio.model.i18n.tr("Partie « %1\$s » créée (copie du Groove) : sans %2\$s.", name, out.joinToString(", "))
    }

    private fun brickName(p: Project, brick: String): String = when {
        brick.startsWith("pad:") -> brick.removePrefix("pad:").toIntOrNull()?.let { com.pocketstudio.app.rhythm.padName(p, it) } ?: brick
        brick.startsWith("inst:") -> brick.removePrefix("inst:").toIntOrNull()?.let { com.pocketstudio.model.DefaultNames.shown(p.track(it).name) } ?: brick
        brick == "acid" -> "303"
        else -> brick
    }

    /** The mixer channel of [role]'s track ("bass", "pad"…) or pad ("snare"…), null when the project has none. */
    private fun channelOf(p: Project, role: String): Int? =
        TutoChecks.trackFor(p, role)?.let(com.pocketstudio.model.InstrumentTrack::mixerChannel)
            ?: role.takeIf { it in DRUM_ROLES }?.let { com.pocketstudio.model.Mixer.padChannel(TutoChecks.laneOf(it)) }

    /** Where the current step's control is, to bring it into view when the step begins (no ring, the panel as it is). */
    fun place(): TutoGo? = _ui.value?.let { u -> goFor(u.current, project.value, spot = false) }

    /**
     * « Je le fais »: the screen of the step, its control lit a moment; the panel folds (testers, lot
     * « tuto 3 »: the place to touch is never under it, and it no longer grows under the finger).
     */
    fun doIt() {
        val u = _ui.value ?: return
        goFor(u.current, project.value, spot = true)?.let { _goes.tryEmit(it) }
        _ui.value = u.copy(bravo = false, open = false)
    }

    /** The step's main button ([TutoUi.need]): what it needs, made in one undo step, then its screen. */
    fun fulfilNeed() {
        val u = _ui.value ?: return
        val t = target ?: return
        val step = u.current
        val need = TutoChecks.need(step, project.value, t, base()) ?: return
        var made: Int? = null
        val before = project.value
        edit {
            val r = TutoChecks.fulfil(need, step, this, t)
            made = r?.second
            r?.first ?: this
        }
        val p = project.value
        // Testers (lot « tuto 4 »): « Créer la partie » six times and nothing seen changing: what it made, in words.
        if (need is TutoNeed.Part && p !== before) _ui.value = _ui.value?.copy(made = partMade(before, p, need.label))
        val go = made?.let { TutoGo(notes = it, spot = false) } ?: goFor(step, p, spot = false)
        go?.let { _goes.tryEmit(it) }
        // Still to do there (the notes to touch, the cells of the second bar): the panel folds out of the way.
        if (!TutoChecks.done(step, p, t, base())) _ui.value = _ui.value?.copy(open = false)
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
        // Testers: the notes went by too fast to follow; a note at a time, slower (at most about 8 s in all).
        val each = demoFrameMs(step.check.startsWith("notes:") || step.check == "track:sub", demo.frames.size, frameMs)
        stopListening()
        pauseMusic()
        // Folded: the demonstration is seen where it happens (its caption on the folded line).
        _ui.value = u.copy(demo = TutoDemoPhase.SHOWING, bravo = false, demoCells = null, fade = 0f, open = false)
        demoJob = scope.launch {
            var restored = false
            try {
                goFor(step, before, spot = false)?.takeIf { it.notes == null }?.let { _goes.tryEmit(it) }
                demo.frames.forEachIndexed { i, frame ->
                    demoShow(frame)
                    if (i == 0) goFor(step, frame, spot = false)?.takeIf { it.notes != null }?.let { _goes.tryEmit(it) }
                    demo.sounds[i]?.let(demoSound)
                    delay(each)
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
                // A step with nothing to do on the screen (« J'ai compris »): the panel back, its button in sight. Testers
                // (lot « tuto 4 »): so too when the step's track is still to make (the demonstration's went with it, as
                // all it showed): « Créer la piste … » in sight rather than another track's notes without a word.
                val back = step.manual || step.spot.isEmpty() || _ui.value?.need != null
                if (_ui.value?.demo != null) _ui.value = _ui.value?.let { it.copy(demo = null, demoCells = null, fade = 0f, open = it.open || (back && it.current.id == step.id)) }
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
        // Testers (lot « tuto 4 »): the crash « tout au début » of the Drop, dotted on its first cell (the grid
        // showed the second bar), once the Drop is the part shown.
        if (u.current.check == "crash" && u.need == null && u.current.id !in u.progress.done) return TutoChecks.CRASH_LANE to setOf(0)
        return target?.let { TutoChecks.hint(u.current, it, steps) }
    }

    /**
     * The hits the current step asks to take away ([TutoChecks.toErase]), null when it takes none, or
     * while its part is not the one shown (testers: crosses drawn in the Drop for the Montée's hits).
     */
    fun toErase(p: Project): Map<Int, Set<Int>>? = _ui.value?.takeIf { it.need == null }?.let { TutoChecks.toErase(it.current, p) }

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
        _ui.value = u.copy(preparing = true, failed = false)
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
                    if (step.listening) heardPart(step, EXAMPLE)
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

    /**
     * Whether the current listening step can play the user's own music (« Écoute toute ta batterie »,
     * « Écoute ta boucle en entier »): the project already holds what its example plays.
     */
    fun mineToHear(p: Project = project.value): Boolean = _ui.value?.current?.let { mineFor(it, p) } == true

    /** The user's own music played long enough while a listening step is shown ([mineToHear]): it is ticked. */
    fun heardMine() {
        val u = _ui.value ?: return
        if (u.current.listening && mineFor(u.current, project.value)) heardPart(u.current, MINE)
    }

    /**
     * [part] of listening step [step] heard ([MINE] or [EXAMPLE]): ticked, but a step that says « ta
     * boucle, puis l'exemple » ([TutoStep.hearMine]) only once both were (testers, lot « tuto 4 »: it
     * was ticked on the user's loop alone); its example alone when the user has nothing to hear yet.
     */
    private fun heardPart(step: TutoStep, part: String) {
        val now = (heard?.takeIf { it.first == step.id }?.second.orEmpty()) + part
        heard = step.id to now
        val both = !step.hearMine || !mineFor(step, project.value) || (MINE in now && EXAMPLE in now)
        if (both) tick(setOf(step.id), advance = false, bravo = false)
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

    /** [p] saved and shown on its current step (the first not done when none is named); a new step opens the panel, its « Bravo » kept while it stays. */
    private fun save(p: TutoProgress, chapters: List<TutoChapter> = _ui.value?.chapters.orEmpty(), bravo: Boolean = false, atEnd: Boolean = false) {
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
            saved, list, chapter, list[chapter].steps.indexOf(step), open = if (same && !atEnd) kept!!.open else true, bravo = bravo || (same && kept!!.bravo),
            preparing = same && kept!!.preparing, playing = same && kept!!.playing, failed = same && kept!!.failed,
            need = target?.let { TutoChecks.need(step, project.value, it, base()) },
            demo = if (same) kept!!.demo else null, demoCells = if (same) kept!!.demoCells else null, fade = if (same) kept!!.fade else 0f,
            atEnd = atEnd || (same && kept!!.atEnd), made = if (same) kept!!.made else null,
        )
        // A step already done when it is reached (the key already right) is ticked at once.
        if (moved && before != null) scope.launch { check(project.value) }
    }

    /**
     * Ticks [ids]; [advance] (« C'est fait », the user's own touch): on to the next step not done
     * when the current one is among them; [bravo]: « Bravo ! » on the current one, which stays.
     */
    private fun tick(ids: Set<String>, advance: Boolean, bravo: Boolean) {
        val u = _ui.value ?: return
        val done = u.progress.done + ids
        // Nothing new (a second look that found what the first already ticked): nothing moves.
        if (done == u.progress.done && !(advance && u.current.id in ids)) return
        val flat = u.chapters.flatMap { it.steps }
        val moveOn = advance && u.current.id in ids
        // Testers (lot « tuto 4 »): nothing left after this one, the end (what is left, or « Bravo »), never
        // back to the first step not done without a word.
        val after = flat.drop(flat.indexOf(u.current) + 1).firstOrNull { it.id !in done }
        val next = if (moveOn) after ?: u.current else u.current
        save(u.progress.copy(done = done, current = next.id), bravo = bravo && !moveOn && u.current.id in ids, atEnd = moveOn && after == null)
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
        // Never on by itself: the step stays with « Bravo », « Écouter ton résultat » and « Étape suivante ».
        if (found.isNotEmpty()) tick(found, advance = false, bravo = true)
        else if (_ui.value?.current?.id == u.current.id && _ui.value?.need != need) _ui.value = _ui.value?.copy(need = need)
    }

    private fun audible(pcm: FloatArray): Boolean {
        var peak = 0f
        for (v in pcm) { val a = kotlin.math.abs(v); if (a > peak) peak = a; if (peak >= AUDIBLE) return true }
        return false
    }

    companion object {
        /** A send counts from here (as [TutoChecks.done]). */
        private const val SENT = 0.05f

        /** The knobs a "changed:synth:<role>:<KNOB>+<KNOB>" check names (the course's names, as [TutoChecks]). */
        private val KNOBS = mapOf(
            "CUTOFF" to com.pocketstudio.model.SynthPatch.CUTOFF, "RESONANCE" to com.pocketstudio.model.SynthPatch.RESONANCE,
            "OSC2_DETUNE" to com.pocketstudio.model.SynthPatch.OSC2_DETUNE, "GLIDE" to com.pocketstudio.model.SynthPatch.GLIDE,
            "A_ATTACK" to com.pocketstudio.model.SynthPatch.A_ATTACK, "UNISON_DETUNE" to com.pocketstudio.model.SynthPatch.UNISON_DETUNE,
        )

        /** How long each frame of « Montre-moi » shows: a melodic one's notes slower (about 8 s in all, at most 0.7 s each), at [frameMs]'s pace. */
        internal fun demoFrameMs(melodic: Boolean, frames: Int, frameMs: Long = FRAME_MS): Long =
            if (!melodic) frameMs else (NOTES_SHOWN_MS * frameMs / FRAME_MS / frames.coerceAtLeast(1)).coerceIn(frameMs, NOTE_FRAME_MAX * frameMs / FRAME_MS)

        internal fun knobsOf(check: String): List<Int> =
            check.removePrefix("changed:synth:").substringAfter(':', "").split('+').mapNotNull { KNOBS[it.trim()] }

        /** The key step's own sheet (« Gamme » and « Tonique »). */
        const val KEY_SPOT = "tuto.key"
        /** The song's « Ajouter : » buttons ([com.pocketstudio.app.guide.GuideNav]). */
        const val SONG_ADD_SPOT = "song.add"
        private val DRUM_ROLES = setOf("kick", "snare", "clap", "hat", "perc")

        /**
         * Whether listening step [s] can play the user's own music: every instrument its example
         * plays has notes in [p] and at least half its drum sounds have hits (« ta batterie »
         * after the drums, « ta boucle » after the Groove; never the examples of what comes next).
         */
        fun mineFor(s: TutoStep, p: Project): Boolean {
            if (!s.listening) return false
            val (kind, list) = s.listen.split(':', limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1].split(',').map(String::trim) } ?: return false
            if (kind != "drums" && kind != "parts") return false
            val (drums, melodic) = list.partition { it in DRUM_ROLES }
            // The hats count with the open one; « perc » is any other pad of the kit but the crash.
            val known = setOf(0, 1, 2, 3, 4, 5, 6, TutoChecks.CRASH_LANE)
            fun hit(lanes: (Int) -> Boolean) = (p.allScenes.map { it.drums } + p.drums).any { d -> d.lanes.withIndex().any { (i, l) -> lanes(i) && l.hits.isNotEmpty() } }
            val drumsHeard = drums.count { role -> if (role == "perc") hit { it !in known } else TutoChecks.laneOf(role).let { lane -> hit { it == lane } } }
            val melodicHeard = melodic.all { role -> TutoChecks.trackFor(p, role)?.let { id -> p.allScenes.any { sc -> (if (id == 0) sc.synthNotes else sc.trackNotes[id])?.notes?.isNotEmpty() == true } || p.track(id).notes.notes.isNotEmpty() } == true }
            return melodicHeard && (drums.isEmpty() && melodic.isNotEmpty() || drums.isNotEmpty() && drumsHeard * 2 >= drums.size && drumsHeard > 0)
        }

        private const val MINE = "mine"
        private const val EXAMPLE = "example"

        const val FRAME_MS = 200L
        /** A melodic demonstration: about this long in all, each note at most [NOTE_FRAME_MAX] ms (at [FRAME_MS]'s pace). */
        private const val NOTES_SHOWN_MS = 8000L
        private const val NOTE_FRAME_MAX = 700L
        const val HOLD_MS = 3200L
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
