package com.pocketstudio.app.rhythm

import com.pocketstudio.model.i18n.tr
import com.pocketstudio.app.report.forUser
import com.pocketstudio.app.report.AppLog
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import android.provider.OpenableColumns
import com.pocketstudio.app.audio.AudioDecoder
import com.pocketstudio.app.audio.AudioEdits
import com.pocketstudio.app.Metrics
import com.pocketstudio.app.audio.MicRecorder
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.export.WavExporter
import com.pocketstudio.app.instruments.InstrumentLibrary
import com.pocketstudio.engine.Preset
import com.pocketstudio.engine.SoundFont
import com.pocketstudio.model.InstrumentChoice
import com.pocketstudio.app.playback.PlaybackService
import com.pocketstudio.app.playback.PlaybackStatus
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.engine.NativeSample
import com.pocketstudio.engine.NativeStems
import com.pocketstudio.app.remix.StemSet
import com.pocketstudio.app.remix.StemStore
import com.pocketstudio.model.Remix
import com.pocketstudio.model.LiveSlots
import com.pocketstudio.model.LiveState
import com.pocketstudio.model.LiveDivision
import com.pocketstudio.app.live.LiveFxUi
import com.pocketstudio.model.liveColumnsMask
import com.pocketstudio.model.writeJournal
import com.pocketstudio.model.liveQuantumSteps
import com.pocketstudio.model.nextLaunchStep
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.Macros
import com.pocketstudio.model.insertsOf
import com.pocketstudio.model.withInsertScenes
import com.pocketstudio.model.withInsertsSwapped
import com.pocketstudio.model.FxPresets
import com.pocketstudio.model.currentSceneContent
import com.pocketstudio.model.withFx
import com.pocketstudio.model.longFxLimitNotice
import com.pocketstudio.model.longFxOverLimit
import com.pocketstudio.model.withoutStaleInsertLanes
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.copySceneTo
import com.pocketstudio.model.firstEmptyScene
import com.pocketstudio.model.clearCurrentScene
import com.pocketstudio.model.sceneView
import com.pocketstudio.model.switchScene
import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.AutomationLane
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.Insert
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.AcidGenerator
import com.pocketstudio.model.AcidPattern
import com.pocketstudio.model.AcidStep
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.Hit
import com.pocketstudio.model.Harmony
import com.pocketstudio.model.Scale
import com.pocketstudio.model.Note
import com.pocketstudio.model.Performance
import com.pocketstudio.model.Progression
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.ArpMode
import com.pocketstudio.model.PadSample
import com.pocketstudio.app.export.AacEncoder
import com.pocketstudio.app.export.FlacEncoder
import com.pocketstudio.app.export.Loudness
import com.pocketstudio.model.HeardNote
import com.pocketstudio.model.Transcription
import com.pocketstudio.separation.BasicPitch
import com.pocketstudio.model.AcidSound
import com.pocketstudio.model.Favorites
import com.pocketstudio.model.NamedSound
import com.pocketstudio.model.PlaitsPatch
import com.pocketstudio.model.SoundCategories
import com.pocketstudio.model.SoundLibrary
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.LivePads
import com.pocketstudio.model.SampledPatch
import com.pocketstudio.model.RootNote
import com.pocketstudio.model.sampleFiles
import com.pocketstudio.model.Project
import com.pocketstudio.model.hasOwnFx
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.MidiMapping
import com.pocketstudio.model.partView
import com.pocketstudio.model.partSteps
import com.pocketstudio.model.writeTake
import com.pocketstudio.model.takeParts
import com.pocketstudio.model.liveStateOf
import com.pocketstudio.model.songCurveCommands
import com.pocketstudio.model.CurveCommand
import com.pocketstudio.model.hasPoints
import com.pocketstudio.model.withPartScene
import com.pocketstudio.model.sceneSteps
import com.pocketstudio.model.togglePartTrack
import com.pocketstudio.model.songBars
import com.pocketstudio.model.withBeatsPerBar
import com.pocketstudio.model.followSongIndex
import com.pocketstudio.model.songIndexAfterRemoval
import com.pocketstudio.model.songIndexAfterMove
import com.pocketstudio.model.songNextIndex
import com.pocketstudio.model.renameScene
import com.pocketstudio.model.moveScene
import com.pocketstudio.model.SceneMove
import com.pocketstudio.model.insertSongPart
import com.pocketstudio.model.duplicateSongPart
import com.pocketstudio.model.moveSongPart
import com.pocketstudio.model.removeSongPart
import com.pocketstudio.model.AudioTrack
import com.pocketstudio.model.AudioClip
import com.pocketstudio.model.PendingTakes
import com.pocketstudio.model.TakeAnchor
import com.pocketstudio.model.micTrackFor
import com.pocketstudio.model.partAt
import com.pocketstudio.model.restorePreviousTake
import com.pocketstudio.model.withSongRepeats
import com.pocketstudio.model.previousTakeFiles
import com.pocketstudio.model.songCurvesAt
import com.pocketstudio.model.acidKnobsDriven
import com.pocketstudio.model.automationBefore
import com.pocketstudio.model.songStepOf
import com.pocketstudio.model.takeStart
import com.pocketstudio.model.withTake
import com.pocketstudio.model.withTakes
import com.pocketstudio.model.addAudioTrack
import com.pocketstudio.model.audioEndBars
import com.pocketstudio.model.audioPlays
import com.pocketstudio.model.removeAudioTrack
import com.pocketstudio.model.withAudioTrack
import com.pocketstudio.model.instrumentTracks
import com.pocketstudio.model.track
import com.pocketstudio.model.withTrack
import com.pocketstudio.model.addTrack
import com.pocketstudio.model.sceneTitle
import com.pocketstudio.model.duplicateTrack
import com.pocketstudio.model.removeTrack
import com.pocketstudio.model.ProjectSession
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.SynthPatch
import com.pocketstudio.model.TapTempo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import com.pocketstudio.app.audio.finishOrRelease
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.asCoroutineDispatcher
import kotlin.math.roundToInt
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

sealed interface LibraryState {
    data object Missing : LibraryState
    data class Downloading(val progress: Float) : LibraryState
    data object Loading : LibraryState
    data class Ready(val presets: List<Preset>) : LibraryState
    data class Failed(val message: String) : LibraryState
}

/** A microphone take on an audio track: [waiting] during the count-in; [song]: over the whole song. */
data class AudioRecordingUi(val track: Int, val waiting: Boolean = true, val seconds: Float = 0f, val level: Float = 0f, val song: Boolean = false)

/** Where the next microphone take goes: the scene shown, or the whole song from part [fromPart]. */
data class RecordTarget(val song: Boolean = false, val fromPart: Int = 0)

/**
 * « Enregistrer sur » until the user picks: once there is a song, the whole song from the first
 * part of the scene shown (a take then sits at its place in the song); else the scene.
 */
fun defaultRecordTarget(p: Project): RecordTarget =
    if (p.song.isEmpty()) RecordTarget() else RecordTarget(song = true, fromPart = p.song.indexOfFirst { it.scene == p.scene }.coerceAtLeast(0))

/** The keyboard's arpeggiator: [steps] sixteenths per note (0.5 = 1/32 … 4 = 1/4). */
data class ArpUi(val on: Boolean = false, val steps: Float = 1f, val mode: Int = 0, val octaves: Int = 1, val gate: Float = 0.5f)

/** Live playing settings of the keyboard. */
data class PlayUi(val bend: Float = 0f, val mod: Float = 0f, val sustain: Boolean = false, val arp: ArpUi = ArpUi())

sealed interface SampleTask {
    data object Idle : SampleTask
    data class Busy(val message: String) : SampleTask
    data class Recording(val seconds: Float, val level: Float) : SampleTask
    data class Failed(val message: String) : SampleTask
}

/** What an export renders: the current loop, the whole song (scenes chained), or each track apart. */
enum class ExportWhat { Loop, Song, Stems }

/** WAV 24 bits (studio quality), FLAC (the same, about half the size) or M4A/AAC (about ten times smaller, for sharing). */
enum class ExportFormat(val extension: String, val mime: String) { Wav("wav", "audio/wav"), M4a("m4a", "audio/mp4"), Flac("flac", "audio/flac") }

/** « Fondu de fin »: the end of an export fades to silence over [seconds] (none for a loop by default). */
enum class ExportFade(val seconds: Int) {
    None(0), Short(3), Long(8);

    val label: String get() = when (this) {
        None -> tr("Aucun")
        Short -> tr("Court")
        Long -> tr("Long")
    }
}

data class ExportUi(
    /** Length of « La scène », in bars: the scene's own ([sceneBars]) until the user picks one. */
    val bars: Int = 4,
    val barsChosen: Boolean = false,
    /** The current scene's length in bars (offered among the lengths). */
    val sceneBars: Int = 4,
    /** The bar where the audio clips heard end (« Partager » without a song lasts at least that long). */
    val audioBars: Int = 0,
    /** The whole song when the project has one, the scene otherwise, until the user picks. */
    val what: ExportWhat = ExportWhat.Loop,
    val whatChosen: Boolean = false,
    val format: ExportFormat = ExportFormat.Wav,
    /** A short fade for the song, none for a loop, until the user picks; never on separate tracks. */
    val fade: ExportFade = ExportFade.None,
    val fadeChosen: Boolean = false,
    /** « Sans limiteur (pour le mastering) »: the mix without the master's limiter, in 32-bit float WAV. */
    val noLimiter: Boolean = false,
    /** Separate tracks through the master's effects and limiter (off: they add back up to the mix). */
    val stemsWithMaster: Boolean = false,
    /** « Partager mon morceau »: this export opens the share sheet when it is done. */
    val sharing: Boolean = false,
    val running: Boolean = false,
    val progress: Float = 0f,
    /** Step being done ("Batterie 1/4", "Compression M4A"…). */
    val stage: String? = null,
    val files: List<File> = emptyList(),
    val savedInMusic: Boolean = false,
    /** Integrated loudness of the exported mix in LUFS (null for stems or silence). */
    val loudness: Double? = null,
    val error: String? = null,
) {
    val file: File? get() = files.firstOrNull()

    /** These settings for a scene [sceneBars] long: its length is the default. */
    fun forScene(sceneBars: Int): ExportUi = copy(sceneBars = sceneBars, bars = if (barsChosen) bars else sceneBars)

    /** These settings for a project: the scene's length, the song if there is one, the fade that goes with it. */
    fun forProject(sceneBars: Int, hasSong: Boolean): ExportUi {
        val w = if (whatChosen) what else if (hasSong) ExportWhat.Song else ExportWhat.Loop
        val f = when {
            w == ExportWhat.Stems -> ExportFade.None
            fadeChosen -> fade
            else -> defaultFade(w)
        }
        return forScene(sceneBars).copy(what = w, fade = f)
    }

    /** No limiter was asked for (« Sans limiteur », or separate tracks without the master). */
    val limiterOff: Boolean get() = if (what == ExportWhat.Stems) !stemsWithMaster else noLimiter

    /**
     * The export goes through the master's limiter. Without it only a float WAV keeps the peaks
     * over 0 dBFS: M4A and FLAC would clip them hard, so they always keep it.
     */
    val limited: Boolean get() = !limiterOff || format != ExportFormat.Wav

    /** Through the master's effects: separate tracks skip them unless asked (together they give back the mix). */
    val masterFx: Boolean get() = what != ExportWhat.Stems || stemsWithMaster

    /** Frames the end of this export fades over at 48 kHz: never more than half of the [frames] rendered. */
    fun fadeFrames(frames: Long): Long =
        if (what == ExportWhat.Stems) 0L else minOf(fade.seconds.toLong() * com.pocketstudio.engine.OfflineExport.SAMPLE_RATE, frames / 2)

    companion object {
        fun defaultFade(what: ExportWhat) = if (what == ExportWhat.Song) ExportFade.Short else ExportFade.None
    }
}

/** What the audio engine is doing right now, polled for the playhead and pad lights. */
data class LiveUi(val currentStep: Int = -1, val litPads: Set<Int> = emptySet(), val absoluteStep: Int = -1, val songStep: Int = -1)

/** Peaks (0..1) of each stem for the waveform view, and the song length in frames at 48 kHz. */
class StemOverview(val peaks: List<FloatArray>, val frames: Long)

/**
 * Session state. [pending]: the scene waiting to take over; [songIndex]: the song part playing
 * (-1: not the song). [removed]: that part was taken out of the song while it played and still
 * finishes ([songIndex] then only says the song plays). [queued]: the part chosen to come next
 * (-1: the one after [songIndex]; while a removed part finishes, the part that followed it).
 * [ending]: the song ends after the part playing (the engine stops on its last bar line).
 */
data class SessionUi(
    val pending: Int? = null,
    val songIndex: Int = -1,
    val removed: SongPart? = null,
    val queued: Int = -1,
    val ending: Boolean = false,
) {
    /** The part to light up in the song: none while a removed one finishes. */
    val shownSongIndex: Int get() = if (removed == null) songIndex else -1

    /** The part that comes next in a song of [size] parts, or -1 (not playing the song, or it ends). */
    fun upcoming(size: Int, loops: Boolean): Int = when {
        songIndex < 0 -> queued.takeIf { pending != null && it in 0 until size } ?: -1
        ending -> -1
        else -> songNextIndex(songIndex, queued, size, loops)
    }
}

data class TransportUi(
    val playing: Boolean = false,
    val metronome: Boolean = false,
    val countIn: Boolean = false,
    val selectedPad: Int = 0,
)

/**
 * The drum machine: owns the open project (autosaved) and mirrors every edit to the engine.
 * The engine never reads the project directly: it receives small commands.
 */
class RhythmViewModel(app: Application) : AndroidViewModel(app) {
    private val store = ProjectStore(File(app.filesDir, "projects"))

    /** A one-off message for the user (e.g. an unreadable project was set aside). Declared before `session`. */
    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()
    fun dismissNotice() {
        _notice.value = null
        favoritesBeforeDelete = null
    }

    /** The journals of the Live takes in progress ([com.pocketstudio.model.TakeJournal]): one writer for the whole app. Declared before `session`. */
    private val journals = LiveTakeJournals.of(app)

    /** The open project (the last one used). Another project = a new ViewModel (see ProjectsViewModel). */
    val projectId: String = com.pocketstudio.app.projects.CurrentProject.get(app)

    val session: ProjectSession = ProjectSession(
        id = projectId,
        initial = loadOrCreate(),
        store = store,
        scope = viewModelScope,
        io = Dispatchers.IO,
    )
    val project: StateFlow<Project> = session.project

    private val _transport = MutableStateFlow(TransportUi())
    val transport: StateFlow<TransportUi> = _transport.asStateFlow()

    private val tapTempo = TapTempo()

    private val _live = MutableStateFlow(LiveUi())
    val live: StateFlow<LiveUi> = _live.asStateFlow()
    private val lastTriggers = IntArray(NativeEngine.LANES)
    private val triggers = IntArray(NativeEngine.LANES)
    private val litUntil = LongArray(NativeEngine.LANES)

    /** Polls the engine about 30 times a second while the screen is visible. */
    suspend fun pollLive() {
        if (!NativeEngine.isAvailable) return
        NativeEngine.laneTriggers(lastTriggers)
        while (true) {
            NativeEngine.laneTriggers(triggers)
            val now = System.currentTimeMillis()
            for (i in triggers.indices) {
                if (triggers[i] != lastTriggers[i]) litUntil[i] = now + 120
                lastTriggers[i] = triggers[i]
            }
            val lit = (0 until NativeEngine.LANES).filter { litUntil[it] > now }.toSet()
            _live.value = LiveUi(NativeEngine.currentStep(), lit, NativeEngine.absoluteStep(), NativeEngine.songStep())
            kotlinx.coroutines.delay(33)
        }
    }

    private fun loadOrCreate(): Project {
        try {
            // A take of the app closed a moment ago may still be being written, its last edits saved.
            TakeVault.app.awaitIdleBlocking(3000)
            ClosingSaves.awaitIdleBlocking(3000)
            val loaded = runBlocking(Dispatchers.IO) { store.load(projectId) }
            if (loaded != null) return withWaitingLiveTake(withWaitingTakes(loaded))
        } catch (e: Exception) {
            AppLog.add("Projet illisible : ${e.message} / ${e.cause?.message}")
            // Never let the first autosave overwrite files we could not read: move them aside.
            val aside = File(store.folder(projectId).parentFile, "$projectId-illisible-${System.currentTimeMillis()}")
            val moved = store.folder(projectId).renameTo(aside)
            _notice.value = if (moved) {
                tr("Le projet n'a pas pu être relu. Il est mis de côté (%s) et un nouveau projet commence.", aside.name)
            } else {
                tr("Le projet n'a pas pu être relu ; un nouveau projet commence.")
            }
        }
        return starterProject()
    }

    /**
     * Takes finished while the app was closing ([TakeVault]) put on their tracks, saved with the
     * project, then forgotten. The project as loaded when that fails (they wait for next time).
     */
    private fun withWaitingTakes(loaded: Project): Project {
        val folder = store.folder(projectId)
        val takes = runCatching { PendingTakes.read(folder) }.getOrElse { emptyList() }
        if (takes.isEmpty()) return loaded
        val p = loaded.withTakes(takes).sanitized()
        return try {
            runBlocking(Dispatchers.IO) {
                if (p != loaded) store.save(projectId, p)
                PendingTakes.clear(folder, takes)
            }
            if (p != loaded) {
                AppLog.add("Prise récupérée : ${takes.size}")
                _notice.value = tr("Ta dernière prise a été gardée : elle est sur sa piste audio.")
            }
            p
        } catch (e: Exception) {
            AppLog.add("Prise en attente : ${e.message}")
            loaded
        }
    }

    /**
     * A Live take the app could not finish (closed or killed while it recorded, see [keepTakeJournal])
     * written into the song as it was played up to there, saved, then forgotten; the project as
     * loaded when it is another project's, already in the song ([Project.liveTake]: a journal
     * left by a kill after its take was saved) or that fails (an unreadable journal is dropped).
     */
    private fun withWaitingLiveTake(loaded: Project): Project {
        // After the writes of the screen that closed (the writer keeps their order).
        val journal = journals.read(projectId) ?: return loaded
        if (journal.projectId != projectId) return loaded  // another project's: when it opens
        val written = loaded.writeJournal(journal, projectId)
        return try {
            if (written.parts > 0) runBlocking(Dispatchers.IO) { store.save(projectId, written.project) }
            // Only once the song holding it is on disk (a kill before: written at the next opening).
            journals.drop(projectId, journal.takeId.ifEmpty { null })
            if (written.parts > 0) {
                AppLog.add("Prise Live récupérée : ${written.parts} parties")
                _notice.value = tr("Ta prise Live a été gardée : elle est dans le morceau (%d parties).", written.parts)
                written.project
            } else {
                if (written.already) AppLog.add("Prise Live déjà dans le morceau : journal effacé")
                loaded
            }
        } catch (e: Exception) {
            AppLog.add("Prise Live en attente : ${e.message}")
            loaded
        }
    }

    /**
     * The Live take in progress kept on disk (also when the app is killed): written at the next
     * opening if not finished. Only a snapshot is taken here (copies of the take's lists, a few
     * per half beat and setting): the encoding and the write are the journal writer's.
     */
    private fun keepTakeJournal() {
        val t = performance ?: return
        if (_take.value !is com.pocketstudio.app.live.TakeUi.Recording) return
        val step = takeClock()
        if (step < 0) return
        journals.write(t.journal(projectId, performanceEnd(step), performanceAppend))
    }

    /** The take [t] is over and nothing of it is written: its journal goes (after any write of it under way). */
    private fun dropTakeJournal(t: com.pocketstudio.model.PerformanceTake?) {
        if (t == null) return
        journals.seal(t.id)
        journals.drop(projectId, t.id)
    }

    /**
     * Take [id] is in the song: its journal goes once the project holding it is saved (until then
     * a kill finds it at the next opening, written once: see [Project.liveTake]).
     */
    private fun dropTakeJournalOnceSaved(id: String) {
        journals.seal(id)
        viewModelScope.launch {
            if (withContext(NonCancellable) { session.flush() }) journals.drop(projectId, id)
        }
    }

    /** What the app keeps every 5 s while a take records; [wait]: until it is on disk. */
    internal fun keepTakeJournalForTest(wait: Boolean = true) {
        keepTakeJournal()
        if (wait) journals.awaitIdle(5000)
    }
    internal val journalsForTest get() = journals

    /** The audio stream (re)started: everything is sent again, instruments included. */
    fun engineStarted() {
        liveInstruments.clear()
        syncEngine()
    }

    /** Sends the whole project to the engine, e.g. after the audio stream (re)started. */
    fun syncEngine(p: Project = project.value) {
        if (!NativeEngine.isAvailable) return
        val dropped = NativeEngine.droppedCommands
        NativeEngine.setStaging(false)  // whatever happened before, edits go to the playing scene
        NativeEngine.setTempo(p.tempoBpm)
        NativeEngine.setSwing(p.swing)
        NativeEngine.setBeatsPerBar(p.beatsPerBar)
        pushDrumPattern(p.drums)
        NativeEngine.setMetronome(_transport.value.metronome)
        pushAcid(p.acid)
        pushMixer(p)
        pushPads(p)
        pushTracks(p)
        pushAudio(p)
        pushRemix(p)
        pushAutomation(p)
        pushLive(p)
        pushLiveFxSettings(p)
        // Playing the song: the part on now keeps its muted tracks (live only: an export renders
        // the scene or the song as written, whatever plays at that moment).
        if (p === project.value && !NativeEngine.isCapturing) playingPart()?.let { pushPart(p, it.scene, it) }
        sendAgainIfDropped(dropped)
    }

    private var dropRetry: kotlinx.coroutines.Job? = null
    private var dropRetries = 0

    /**
     * The engine's queue stayed full (the stream stalled 100 ms) since [dropped]: some commands of
     * that burst were lost (stale notes or curve points). Everything is sent again a moment later
     * (three times at most in a row: a stream that stays stopped resends on its restart).
     */
    private fun sendAgainIfDropped(dropped: Int) {
        if (NativeEngine.isCapturing) return
        if (NativeEngine.droppedCommands == dropped) {
            if (dropRetry?.isActive != true) dropRetries = 0
            return
        }
        if (dropRetries >= 3 || dropRetry?.isActive == true) return
        dropRetries++
        AppLog.add("Moteur : ${NativeEngine.droppedCommands - dropped} commandes perdues, tout est renvoyé")
        dropRetry = viewModelScope.launch {
            kotlinx.coroutines.delay(300)
            syncEngine()
            val ui = _sessionUi.value
            if (_transport.value.playing && ui.songIndex >= 0 && ui.pending != null && !ui.ending) stageSongPart()
        }
    }

    /** The song part playing now, or null outside song playback. */
    private fun playingPart(): SongPart? {
        val i = _sessionUi.value.songIndex
        return if (i >= 0 && _transport.value.playing) removedPlaying ?: project.value.song.getOrNull(i) else null
    }

    // --- Live pads: sounds switched on and off by a tap, in time ---------------------------------

    private val _liveStates = MutableStateFlow(List(LivePads.MAX) { 0 })
    /** Per live pad: 0 off, 1 waiting for its beat, 2 playing. */
    val liveStates: StateFlow<List<Int>> = _liveStates.asStateFlow()

    /** Polls the pads' lights about 30 times a second while their screen is visible. */
    suspend fun pollLiveStates() {
        if (!NativeEngine.isAvailable) return
        val buf = IntArray(NativeEngine.LIVE_PADS)
        while (true) {
            NativeEngine.clipStates(buf)
            val now = buf.take(LivePads.MAX)
            if (now != _liveStates.value) _liveStates.value = now
            kotlinx.coroutines.delay(33)
        }
    }

    private fun pushLive(p: Project = project.value) {
        if (!NativeEngine.isAvailable) return
        p.live.clips.take(NativeEngine.LIVE_PADS).forEachIndexed { i, clip ->
            if (clip != null) NativeEngine.setClipParams(i, clip.beats, clip.oneShot, clip.gain)
            NativeEngine.setClip(i, clip?.let { padSamples.native(it.file) })
        }
    }

    fun toggleLive(pad: Int) {
        if (NativeEngine.isAvailable && project.value.live.clips.getOrNull(pad) != null) NativeEngine.toggleClip(pad)
    }

    fun stopLive() {
        if (NativeEngine.isAvailable) NativeEngine.toggleClip(-1)
    }

    fun setLiveSize(size: Int) = session.edit { p -> p.copy(live = p.live.copy(size = size)) }

    private fun editLive(pad: Int, key: String? = null, transform: (LiveClip) -> LiveClip) {
        val clip = project.value.live.clips.getOrNull(pad) ?: return
        session.edit(key) { p -> p.copy(live = p.live.with(pad, transform(clip).sanitized())) }
        pushLive()
    }

    fun setLiveBeats(pad: Int, beats: Int) = editLive(pad) { it.copy(beats = beats) }
    fun setLiveOneShot(pad: Int, oneShot: Boolean) = editLive(pad) { it.copy(oneShot = oneShot) }
    fun setLiveGain(pad: Int, gain: Float) = editLive(pad, "live-gain-$pad") { it.copy(gain = gain) }

    fun clearLive(pad: Int) {
        session.edit { p -> p.copy(live = p.live.with(pad, null)) }
        pushLive()
    }

    /** An audio file from the phone onto live pad [pad]. */
    fun importLive(pad: Int, uri: Uri) = sampleJob(tr("Lecture du fichier…"), live = pad) {
        val app = getApplication<Application>()
        val name = PadSamples.nameFrom(displayName(app, uri))
        val pcm = try {
            AudioDecoder.decode(app, uri)
        } catch (e: AudioDecoder.TooLongException) {
            throw UserMessage(tr("Ce fichier est trop long : choisis un son plus court."))
        } catch (e: Exception) {
            throw UserMessage(tr("Ce fichier n'a pas pu être lu (%s).", e.message ?: tr("format inconnu")))
        }
        AppLog.add("Pad live ${pad + 1} : « $name », ${"%.1f".format(pcm.seconds)} s")
        padSamples.add(pcm, name)
    }

    /** Live pad [pad] gets the sound of drum pad [drumPad] (the same file, nothing copied). */
    fun liveFromDrumPad(pad: Int, drumPad: Int) {
        val s = project.value.drums.lanes.getOrNull(drumPad)?.sample ?: return
        val seconds = padSamples.info.value[s.file]?.seconds ?: 0.0
        session.edit { p -> p.copy(live = p.live.with(pad, LiveClip(s.file, s.name, LivePads.beatsFor(seconds, p.tempoBpm)))) }
        pushLive()
    }

    // --- Instrument tracks: piano roll + sound, added at will -----------------------------------

    private val _selectedTrack = MutableStateFlow(0)
    /** The instrument track shown by the synth screen, the keyboard and recording (0 = the first). */
    val selectedTrack: StateFlow<Int> = _selectedTrack.asStateFlow()

    /** The selected track, or the first one when it no longer exists (deleted, undone). */
    private val track: Int get() = _selectedTrack.value.takeIf { id -> id == 0 || project.value.tracks.any { it.id == id } } ?: 0

    private val currentTrack: InstrumentTrack get() = project.value.track(track)

    fun selectTrack(id: Int) {
        if (id == track) return
        heldNotes.clear()
        val from = track
        _selectedTrack.value = id
        NativeEngine.liveTrack = id
        movePlay(from, id)
    }

    private fun editTrack(key: String? = null, id: Int = track, transform: (InstrumentTrack) -> InstrumentTrack) {
        session.edit(key) { p -> p.withTrack(transform(p.track(id)).sanitized()) }
    }

    /** A new instrument track (with its own mixer channel), selected at once. */
    fun addTrack() {
        val p = project.value
        val next = p.addTrack() ?: run {
            _notice.value = tr("Huit pistes d'instruments au maximum : supprime-en une pour en ajouter une autre.")
            return
        }
        val id = next.tracks.first { t -> p.tracks.none { it.id == t.id } }.id
        session.edit { next }
        pushTracks()
        pushMixer()
        selectTrack(id)
        AppLog.add("Piste ajoutée : ${project.value.track(id).name}")
    }

    /**
     * A new instrument track for [kind] (a melody, chords, a bass…), named for it, with a sound
     * suited to it (the style's, when the project was made from one), selected at once; with
     * [write], four bars of melody in the song's key in the scene shown. One step to undo.
     */
    fun addTrackOf(kind: com.pocketstudio.model.TrackKind, write: Boolean = false) {
        val p = project.value
        val style = com.pocketstudio.app.library.styleOfProject(p.name)?.let(com.pocketstudio.model.StarterWizard::style)
        val next = com.pocketstudio.model.NewTracks.add(p, kind, style, write) ?: run {
            _notice.value = tr("Huit pistes d'instruments au maximum : supprime-en une pour en ajouter une autre.")
            return
        }
        val id = next.tracks.first { t -> p.tracks.none { it.id == t.id } }.id
        session.edit { next }
        pushTracks()
        pushMixer()
        selectTrack(id)
        if (write) _notice.value = tr("Mélodie écrite : 4 mesures dans la tonalité du morceau, dans la scène %s. « Annuler » la retire.", project.value.sceneTitle(project.value.scene))
        AppLog.add("Piste ajoutée : ${project.value.track(id).name}${if (write) " (mélodie écrite)" else ""}")
    }

    fun duplicateTrack(id: Int) {
        val p = project.value
        val next = p.duplicateTrack(id) ?: run {
            _notice.value = tr("Huit pistes d'instruments au maximum : supprime-en une pour en dupliquer une autre.")
            return
        }
        if (longFxRefused(next)) return
        session.edit { next }
        pushTracks()
        pushMixer()
        selectTrack(next.tracks.first { t -> p.tracks.none { it.id == t.id } }.id)
    }

    /** Deletes an added track (Annuler brings it back); the first track stays. */
    fun removeTrack(id: Int) {
        if (id == 0) return
        val name = project.value.track(id).name
        if (track == id) selectTrack(0)
        session.edit { it.removeTrack(id) }
        pushTracks()
        pushMixer()
        pushAutomation()
        releaseUnusedFonts()
        AppLog.add("Piste supprimée : $name")
    }

    fun renameTrack(id: Int, name: String) {
        if (name.isBlank()) return
        editTrack(id = id) { it.copy(name = name.trim()) }
    }

    fun setTrackColor(id: Int, color: Int) = editTrack(id = id) { it.copy(color = color) }

    /** Sends every instrument track to the engine; tracks that do not exist are switched off. */
    private fun pushTracks(p: Project = project.value) {
        // A selected track that went away (undo, another project) hands over to the first one.
        if (p.tracks.none { it.id == _selectedTrack.value }) {
            _selectedTrack.value = 0
            if (NativeEngine.isAvailable) NativeEngine.liveTrack = 0
        }
        if (!NativeEngine.isAvailable) return
        for (id in 1 until InstrumentTrack.MAX) {
            if (p.tracks.none { it.id == id }) NativeEngine.setTrackOn(id, false)
        }
        p.instrumentTracks.forEach { t ->
            if (t.id > 0) NativeEngine.setTrackOn(t.id, true)
            pushSynth(t.synth, t.id)
            pushSampled(p, t.id)
            pushPlaits(p, t.id)
            pushNotes(t.notes, t.id)
        }
        pushInstrument(p)
    }

    // --- Audio tracks: recorded or imported clips -----------------------------------------------

    private val _audioRecording = MutableStateFlow<AudioRecordingUi?>(null)
    /** The microphone take in progress (null when none). */
    val audioRecording: StateFlow<AudioRecordingUi?> = _audioRecording.asStateFlow()
    private var recordHandle = 0L
    private var recordPoll: kotlinx.coroutines.Job? = null

    /** Every audio track's clip and settings to the engine; free slots play nothing. */
    private fun pushAudio(p: Project = project.value) {
        if (!NativeEngine.isAvailable) return
        val part = if (NativeEngine.isCapturing) null else playingPart()
        // A "once" clip comes back with its scene played alone; in a song part, with the next part.
        val sceneSteps = if (part != null) null else p.currentSceneContent().stepsFor(p.stepsPerBar)
        // The scene the engine plays on its own (another one while staying on this one).
        val alone = if (NativeEngine.isCapturing) p.scene else heardScene ?: p.scene
        for (id in 0 until AudioTrack.MAX) {
            // On the live grid, a track playing another scene (or stopped) keeps what it plays.
            if (!waitsFor(LiveSlots.AUDIO + id) || _liveGrid.value?.diverged != true) NativeEngine.setAudioOn(id, audioPlays(p, id, part, alone))
            val clip = p.audioTracks.firstOrNull { it.id == id }?.clip
            val native = clip?.let { padSamples.native(it.file) }
            if (clip != null && native != null) {
                NativeEngine.setAudioClipParams(
                    id, clip.start, clip.engineLoopSteps(sceneSteps, p.tempoBpm), clip.gain,
                    (clip.fadeIn * OfflineExport.SAMPLE_RATE).toInt(), (clip.fadeOut * OfflineExport.SAMPLE_RATE).toInt(),
                    song = clip.song, offsetFrames = clip.offsetFrames(OfflineExport.SAMPLE_RATE),
                )
            }
            NativeEngine.setAudioClip(id, native)
        }
    }

    /** Audio track [id] sounds in song [part] (null: scene [scene] alone); never the track being retaken (live only). */
    private fun audioPlays(p: Project, id: Int, part: SongPart?, scene: Int): Boolean =
        p.audioPlays(id, part, scene) && (NativeEngine.isCapturing || takeMuted != id)

    /** The audio track whose clip is silent while a new take is recorded on it (P3: no double voice). */
    private var takeMuted: Int? = null

    /** The retaken track sounds again (the take ended or was left). */
    private fun endTakeMute() {
        if (takeMuted == null) return
        takeMuted = null
        pushAudio()  // the part playing keeps its own mutes
    }

    fun addAudioTrack() {
        val next = project.value.addAudioTrack() ?: run {
            _notice.value = tr("Quatre pistes audio au maximum : supprime-en une pour en ajouter une autre.")
            return
        }
        session.edit { next }
        pushMixer()
    }

    fun removeAudioTrack(id: Int) {
        if (_audioRecording.value?.track == id) cancelAudioRecording()
        session.edit { it.removeAudioTrack(id) }
        pushAudio()
        pushMixer()
        pushAutomation()
    }

    fun renameAudioTrack(id: Int, name: String) {
        if (name.isBlank()) return
        editAudio(id) { it.copy(name = name.trim()) }
    }

    fun setAudioTrackColor(id: Int, color: Int) = editAudio(id) { it.copy(color = color) }

    /** The audio track whose own ● (« Enregistrer », « Nouvelle prise ») was used last. */
    private var lastRecordTrack = -1

    /**
     * The audio track the top bar's ● records the microphone on (see [micTrackFor]): the one
     * recorded on last when this take is a retake of the same passage (or it is empty), else an
     * empty one, else a new « Voix » track. It never replaces another passage's take: with four
     * tracks holding takes, it says so and records nothing (null).
     */
    fun micTakeTrack(): Int? {
        val p = project.value
        p.micTrackFor(lastRecordTrack, takeAnchor(p, currentRecordTarget()), p.scene)?.let { return it }
        val next = p.addAudioTrack(com.pocketstudio.model.voiceName(p)) ?: run {
            _notice.value = tr("Les quatre pistes audio ont déjà une prise : touche « Nouvelle prise » sur celle à refaire.")
            return null
        }
        session.edit { next }
        pushMixer()
        return next.audioTracks.first { t -> p.audioTracks.none { it.id == t.id } }.id
    }

    private fun editAudio(id: Int, key: String? = null, transform: (AudioTrack) -> AudioTrack) {
        val t = project.value.audioTracks.firstOrNull { it.id == id } ?: return
        session.edit(key) { it.withAudioTrack(transform(t)) }
        pushAudio()
    }

    private fun editClip(id: Int, key: String? = null, transform: (AudioClip) -> AudioClip) =
        editAudio(id, key) { t -> t.copy(clip = t.clip?.let(transform)) }

    /** The clip starts on bar [bar] (1 = the first). */
    fun setAudioStart(id: Int, bar: Int) = editClip(id) { it.copy(start = (bar - 1) * project.value.stepsPerBar) }
    fun setAudioLoopBars(id: Int, bars: Int) = editClip(id) { it.copy(loopSteps = bars * project.value.stepsPerBar, once = false) }
    /** Plays once in each part of its scene (true) or loops (false). */
    fun setAudioOnce(id: Int, once: Boolean) = editClip(id) { it.copy(once = once) }
    /**
     * Its start becomes a bar of the song (true: where the scene shown first plays in the song)
     * or of the scene again (false: its first bar).
     */
    fun setAudioSong(id: Int, song: Boolean) = editClip(id) { c ->
        if (c.song == song) return@editClip c
        val p = project.value
        val scene = c.scene ?: p.scene
        val first = p.song.indexOfFirst { it.scene == scene }
        val start = if (song) (if (first >= 0) p.songStepOf(first) else 0) + c.start else 0
        // Back to a scene clip: the scene of the part it started in.
        val part = if (!song) p.partAt(c.start) else -1
        c.copy(song = song, start = start, scene = if (song) null else p.song.getOrNull(part)?.scene ?: p.scene)
    }

    /** A scene clip plays in the parts of [scene] only (null: in every part). */
    fun setAudioScene(id: Int, scene: Int?) = editClip(id) { c ->
        if (c.song) c.copy(song = false, start = 0, scene = scene) else c.copy(scene = scene)
    }

    /** « Décaler la prise » in ms (5 ms steps, ±300), sample-accurate in the engine. */
    fun setAudioOffset(id: Int, ms: Float) = editClip(id, "audio-offset-$id") {
        it.copy(offsetMs = (Math.round(ms / AudioClip.OFFSET_STEP_MS) * AudioClip.OFFSET_STEP_MS).coerceIn(-AudioClip.MAX_OFFSET_MS, AudioClip.MAX_OFFSET_MS))
    }

    /**
     * « Prise précédente »: the take the last one replaced comes back (one undo step), and the
     * newer one becomes the previous (a second touch swaps them back).
     */
    fun restorePreviousTake(id: Int) {
        val t = project.value.audioTracks.firstOrNull { it.id == id } ?: return
        if (t.previous == null || _audioRecording.value != null) return
        session.edit { it.restorePreviousTake(id) }
        // Its sound is on disk only: read again, then sent to the engine; the other one leaves memory.
        reloadMissingSamples()
        pushAudio()
        padSamples.prune(project.value.sampleFiles)
    }

    /**
     * « ▶ Depuis ma prise »: the song plays from the part the clip of track [id] starts in (a
     * scene clip: the first part of its scene), without count-in; no song: its scene plays.
     */
    fun playFromTake(id: Int) {
        val clip = project.value.audioTracks.firstOrNull { it.id == id }?.clip ?: return
        if (!NativeEngine.isAvailable || _audioRecording.value != null) return
        if (_transport.value.playing) toggleTransport()
        val p = project.value
        val part = if (clip.song) p.partAt(clip.start) else p.song.indexOfFirst { clip.belongsTo(it.scene) }
        if (part >= 0) {
            playSongFrom(part, countInBars = 0)
        } else {
            clip.scene?.takeIf { it != p.scene && it in p.allScenes.indices }?.let { scene ->
                session.editQuietly { it.switchScene(scene) }
                syncEngine()
            }
            heardScene = project.value.scene
            pushAudio()
            toggleTransport(countInBars = 0, songStep = aloneSongStep())
        }
    }
    fun setAudioGain(id: Int, gain: Float) = editClip(id, "audio-gain-$id") { it.copy(gain = gain) }
    fun setAudioFadeIn(id: Int, seconds: Float) = editClip(id, "audio-in-$id") { it.copy(fadeIn = seconds) }
    fun setAudioFadeOut(id: Int, seconds: Float) = editClip(id, "audio-out-$id") { it.copy(fadeOut = seconds) }
    fun clearAudioClip(id: Int) = editAudio(id) { it.copy(clip = null) }

    /** A clip made of [pcm] on audio track [id] (one undo step), its loop the bars it lasts. */
    private suspend fun putClip(id: Int, pcm: Pcm, name: String, keep: AudioClip? = null) {
        val (sample, prepared) = withContext(Dispatchers.IO) { padSamples.add(pcm, name) }
        padSamples.register(prepared)
        val bars = AudioClip.barsFor(pcm.seconds, project.value.tempoBpm, project.value.beatsPerBar)
        val clip = (keep ?: AudioClip(sample.file, sample.name, loopSteps = bars * project.value.stepsPerBar)).copy(file = sample.file, name = sample.name, seconds = pcm.seconds)
        editAudio(id) { it.copy(clip = clip) }
        // Long clips weigh tens of MB each: older takes and edits leave memory (Annuler reloads them).
        padSamples.prune(project.value.sampleFiles)
    }

    /** After Annuler / Rétablir: sounds freed by [PadSamples.prune] come back from their files. */
    private fun reloadMissingSamples() {
        val missing = padSamples.missing(project.value.sampleFiles)
        if (missing.isEmpty()) return
        viewModelScope.launch {
            val prepared = runCatching { withContext(Dispatchers.IO) { padSamples.reload(missing) } }.getOrElse { emptyList() }
            // Meanwhile the project may have moved on (« Prise précédente » touched twice): a sound
            // it no longer uses, or one another reload already brought back, is freed at once.
            val used = project.value.sampleFiles
            prepared.forEach { if (it.file in used && padSamples.native(it.file) == null) padSamples.register(it) else it.native.close() }
            pushPads()
            pushLive()
            pushAudio()
            project.value.instrumentTracks.forEach { pushSampled(id = it.id) }
        }
    }

    /** An audio file from the phone onto audio track [id]. */
    /** The longest an audio track loops: 64 bars of the time signature (at most [AudioClip.MAX_LOOP] steps). */
    private fun maxLoopSteps(): Int =
        minOf(com.pocketstudio.model.AudioClip.MAX_LOOP, com.pocketstudio.model.AudioClip.MAX_LOOP / 16 * project.value.stepsPerBar)

    fun importAudio(id: Int, uri: Uri) {
        audioJob(tr("Lecture du fichier…")) {
            val app = getApplication<Application>()
            val name = PadSamples.nameFrom(displayName(app, uri))
            val pcm = try {
                // 5 minutes at most (cut), in mono past 2 minutes: a whole song's voice fits.
                withContext(Dispatchers.IO) { AudioDecoder.decodeForTrack(app, uri, app.cacheDir) }
            } catch (e: Exception) {
                throw UserMessage(tr("Ce fichier n'a pas pu être lu (%s).", e.message ?: tr("format inconnu")))
            }
            AppLog.add("Piste audio ${id + 1} : « $name », ${"%.1f".format(pcm.seconds)} s")
            val loopSeconds = maxLoopSteps() * 15.0 / project.value.tempoBpm
            val clip = project.value.audioTracks.firstOrNull { it.id == id }?.clip
            when {
                pcm.seconds >= AudioDecoder.TRACK_MAX_SECONDS - 0.5 ->
                    _notice.value = tr("Ce fichier dure plus de %d minutes : seul le début est gardé.", AudioDecoder.TRACK_MAX_SECONDS / 60)
                // A song clip plays once from its bar, whatever its length; a scene clip loops.
                clip?.song != true && pcm.seconds > loopSeconds ->
                    _notice.value = tr("À ce tempo, une piste audio boucle au bout de 64 mesures : choisis « Tout le morceau » pour que « %s » joue en entier.", name)
            }
            putClip(id, pcm, name)
        }
    }

    // --- Generated sounds (text to sound): placed like an imported file -------------------------

    /** On the selected drum pad (it plays once placed). */
    /** False when another sound is still being placed or recorded (nothing done). */
    fun placeOnPad(pcm: Pcm, name: String, oneShot: Boolean = false): Boolean {
        if (_sampleTask.value is SampleTask.Busy || _sampleTask.value is SampleTask.Recording) return false
        // A library one-shot loses its silent tail; a loop or a phrase keeps its length.
        sampleJob(tr("Pose du son sur le pad %d…", _transport.value.selectedPad + 1)) { padSamples.add(if (oneShot) pcm.withoutTail() else pcm, name) }
        return true
    }

    /** On the first free live pad, its length in beats fitted to the tempo. */
    fun placeOnLive(pcm: Pcm, name: String) {
        if (_sampleTask.value is SampleTask.Busy || _sampleTask.value is SampleTask.Recording) return
        val pad = project.value.live.clips.indexOfFirst { it == null }
        if (pad < 0) {
            _sampleTask.value = SampleTask.Failed(tr("Les 16 pads live sont pris : vides-en un dans l'onglet Morceau."))
            return
        }
        if (project.value.live.size <= pad) setLiveSize(LivePads.SIZES.first { it > pad })
        sampleJob(tr("Pose du son sur le pad live %d…", pad + 1), live = pad) { padSamples.add(pcm, name) }
    }

    /** On live pad [pad] (from the sound library), its length in beats fitted to the tempo; false when busy. */
    fun placeOnLivePad(pad: Int, pcm: Pcm, name: String): Boolean {
        if (_sampleTask.value is SampleTask.Busy || _sampleTask.value is SampleTask.Recording) return false
        if (pad !in 0 until LivePads.MAX) return true
        AppLog.add("Pad live ${pad + 1} : « $name » de la bibliothèque")
        sampleJob(tr("Pose du son sur le pad live %d…", pad + 1), live = pad) { padSamples.add(pcm, name) }
        return true
    }

    /** On the first audio track without a clip; one is added when all are taken. */
    fun placeOnAudioTrack(pcm: Pcm, name: String) {
        if (_sampleTask.value is SampleTask.Busy || _sampleTask.value is SampleTask.Recording || _audioRecording.value != null) return
        val p = project.value
        val id = p.audioTracks.firstOrNull { it.clip == null }?.id ?: run {
            val next = p.addAudioTrack() ?: run {
                _notice.value = tr("Les quatre pistes audio sont prises : vide-en une ou pose le son sur un pad.")
                return
            }
            session.edit { next }
            pushMixer()
            next.audioTracks.first { t -> p.audioTracks.none { it.id == t.id } }.id
        }
        AppLog.add("Son généré « $name » sur la piste audio ${id + 1}")
        audioJob(tr("Pose du son…")) { putClip(id, pcm, name) }
    }

    /**
     * Busy line and errors like the pad sounds (same "sampleTask" card). [always]: runs even while
     * another sound is being prepared (a take must never be dropped).
     */
    private fun audioJob(busy: String, always: Boolean = false, work: suspend () -> Unit): kotlinx.coroutines.Job? {
        if (!always && _sampleTask.value is SampleTask.Busy) return null
        _sampleTask.value = SampleTask.Busy(busy)
        return viewModelScope.launch {
            try {
                work()
                _sampleTask.value = SampleTask.Idle
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: OutOfMemoryError) {
                _sampleTask.value = SampleTask.Failed(tr("Pas assez de mémoire pour ce son : essaie plus court."))
            } catch (e: Exception) {
                AppLog.add("Piste audio : échec (${e.message})")
                _sampleTask.value = SampleTask.Failed(if (e is UserMessage) e.message!! else tr("Ça n'a pas marché : %s", e.message ?: e.javaClass.simpleName))
            }
        }
    }

    /** « Enregistrer sur » as the user chose it (null: not yet, see [defaultRecordTarget]). */
    private val _recordChoice = MutableStateFlow<RecordTarget?>(null)

    /** Where the next take goes: the scene shown, or the whole song (« Enregistrer sur »). */
    val recordTarget: StateFlow<RecordTarget> = combine(project, _recordChoice) { p, chosen -> chosen ?: defaultRecordTarget(p) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, defaultRecordTarget(project.value))

    /** The target the next take uses now (the flow may not have caught up yet). */
    private fun currentRecordTarget(): RecordTarget = _recordChoice.value ?: defaultRecordTarget(project.value)

    fun setRecordTarget(target: RecordTarget) {
        _recordChoice.value = target
    }

    /** Where a take is anchored for [target] (the song from its part, or the scene shown). */
    private fun takeAnchor(p: Project, target: RecordTarget): TakeAnchor {
        val songPart = if (target.song && p.song.isNotEmpty()) target.fromPart.coerceIn(0, p.song.lastIndex) else -1
        return if (songPart >= 0) TakeAnchor(song = true, from = p.songStepOf(songPart)) else TakeAnchor()
    }

    /** How the take in progress is placed once it ends (see [TakeVault.Request]). */
    private class TakePlan(val track: Int, val anchor: TakeAnchor, val start: Int, val bpm: Double, val stepsPerBar: Int, val replaces: String? = null, val scene: Int? = null)
    private var takePlan: TakePlan? = null
    /** The take plays the whole song: the song ends instead of starting over (the take stops there). */
    private var recordingSong = false

    /**
     * Records the microphone onto audio track [id]: the scene shown (or the whole song, see
     * [recordTarget]) starts after the count-in and the take begins exactly on its first step.
     * The track's own clip is silent meanwhile. [stopAudioRecording] ends it.
     */
    fun recordAudio(id: Int) {
        if (!NativeEngine.isAvailable || _audioRecording.value != null) return
        if (_recording.value) stopRecordingTake()
        if (_transport.value.playing) togglePlay()
        val p = project.value
        val target = currentRecordTarget()
        val songPart = if (target.song && p.song.isNotEmpty()) target.fromPart.coerceIn(0, p.song.lastIndex) else -1
        val anchor = takeAnchor(p, target)
        val previous = p.audioTracks.firstOrNull { it.id == id }?.clip
        lastRecordTrack = id
        takePlan = TakePlan(id, anchor, takeStart(previous, anchor), p.tempoBpm, p.stepsPerBar, previous?.file, scene = if (anchor.song) null else p.scene)
        _audioRecording.value = AudioRecordingUi(id, waiting = true, song = songPart >= 0)
        viewModelScope.launch {
            // Opening the microphone can take up to a second: never on the main thread.
            // No longer than a track can loop at this tempo (64 bars): the rest would never play.
            // A song take may last the whole song (up to the longest take).
            val bars = if (songPart >= 0) maxOf(64, p.songBars - p.songStepOf(songPart) / p.stepsPerBar) else 64
            val seconds = minOf(MAX_AUDIO_SECONDS, (bars * p.stepsPerBar * 15.0 / p.tempoBpm).toInt() + 1)
            // A microphone opened while the project closes is closed again, not left armed.
            val handle = finishOrRelease(Dispatchers.IO, { h: Long -> TakeVault.app.discard(h) }) {
                NativeEngine.recordStart(onStart = true, seconds = seconds)
            } ?: 0L
            if (_audioRecording.value?.track != id) {  // cancelled meanwhile
                TakeVault.app.discard(handle)
                return@launch
            }
            if (handle == 0L) {
                _audioRecording.value = null
                takePlan = null
                _notice.value = tr("Le micro n'a pas pu être ouvert. Vérifie l'autorisation du micro et qu'aucune autre appli ne l'utilise.")
                return@launch
            }
            TakeVault.app.opened(handle)
            recordHandle = handle
            // The output the take is heard on now (a headset unplugged later must not change it).
            takeLatencyMs = Metrics.recordLatencyMs()
            // The previous take of this track stays silent (no double voice, nothing in the microphone).
            takeMuted = id
            NativeEngine.setAudioOn(id, false)
            val countIn = com.pocketstudio.app.settings.SettingsStore.state.value.recordCountIn.coerceAtLeast(1)
            if (songPart >= 0) {
                recordingSong = true
                playSongFrom(songPart, countIn)
            } else {
                _transport.value = _transport.value.copy(playing = true)
                heardScene = project.value.scene
                // The scene on its own: song curves and song clips wait for the song.
                NativeEngine.play(countIn, aloneSongStep())
                PlaybackService.start(getApplication())
            }
            AppLog.add("Prise audio sur la piste ${id + 1}" + if (songPart >= 0) " (morceau, partie ${songPart + 1})" else "")
            recordPoll = launch {
                while (true) {
                    val st = NativeEngine.recordStatus(handle)
                    val ui = _audioRecording.value ?: break
                    _audioRecording.value = ui.copy(waiting = st[0] < 0.5f, seconds = st[1] / OfflineExport.SAMPLE_RATE.toFloat(), level = st[2])
                    if (st[0] > 1.5f) {  // full (or the transport stopped): the take ends by itself
                        if (st[1] >= (seconds - 1) * OfflineExport.SAMPLE_RATE.toFloat()) {
                            _notice.value = tr("La prise s'est arrêtée toute seule : elle a atteint sa durée maximale (%d s). Elle est gardée.", seconds)
                        }
                        stopAudioRecording()
                        break
                    }
                    kotlinx.coroutines.delay(50)
                }
            }
        }
    }

    /** What [TakeVault] needs to place take [handle] (also when the screen is gone). */
    private fun takeRequest(handle: Long, waiting: Boolean): TakeVault.Request {
        val plan = takePlan ?: (_audioRecording.value?.track ?: 0).let { id ->
            TakePlan(id, TakeAnchor(), 0, project.value.tempoBpm, project.value.stepsPerBar, project.value.audioTracks.firstOrNull { it.id == id }?.clip?.file, project.value.scene)
        }
        val sixteenth = 15.0 / plan.bpm * OfflineExport.SAMPLE_RATE
        return TakeVault.Request(
            handle = handle,
            projectFolder = store.folder(projectId),
            track = plan.track,
            // Saved in French, shown in the interface's language (DefaultNames).
            name = "Prise " + java.text.SimpleDateFormat("HH:mm", java.util.Locale.FRANCE).format(java.util.Date()),
            bpm = plan.bpm,
            song = plan.anchor.song,
            start = plan.start,
            // What the microphone hears lags what was played by the round trip (speaker → micro).
            latencyFrames = (takeLatencyMs / 1000.0 * OfflineExport.SAMPLE_RATE).toInt(),
            // A retake that keeps a later start: what was sung before it is cut.
            cutFrames = Math.round((plan.start - plan.anchor.from) * sixteenth).toInt(),
            waiting = waiting,
            replaces = plan.replaces,
            scene = plan.scene,
        )
    }

    /** Ends the take: the song stops and the recording becomes the track's clip (Annuler possible). */
    fun stopAudioRecording() {
        val ui = _audioRecording.value ?: return
        val handle = recordHandle
        recordHandle = 0L
        recordPoll?.cancel()
        _audioRecording.value = null
        recordingSong = false
        val request = if (handle != 0L) takeRequest(handle, ui.waiting) else null
        takePlan = null
        if (_transport.value.playing) togglePlay()
        endTakeMute()
        if (request == null) return
        // Written into the project outside this screen: closing the app now keeps the take.
        val written = TakeVault.app.finish(request)
        _takeSaving.value = true
        takeJob = audioJob(tr("Écriture de la prise…"), always = true) {
            when (val r = written.await()) {
                is TakeVault.Outcome.Saved -> placeTake(r, request.latencyFrames)
                TakeVault.Outcome.Nothing -> Unit  // stopped during the count-in: nothing was recorded
                TakeVault.Outcome.TooShort -> throw UserMessage(tr("La prise est trop courte ou le micro n'a rien donné."))
                TakeVault.Outcome.Silent -> throw UserMessage(tr("Le micro n'a rien entendu : rapproche-toi et réessaie."))
                TakeVault.Outcome.Lost -> throw UserMessage(tr("La prise n'a pas pu être récupérée : réessaie."))
                is TakeVault.Outcome.Failed -> throw r.error
            }
        }
        // Annuler waits until the take is on its track (or has failed).
        takeJob?.invokeOnCompletion { _takeSaving.value = false } ?: run { _takeSaving.value = false }
    }

    /** A written take onto its track (one undo step); once saved with the project, its waiting note goes. */
    private suspend fun placeTake(r: TakeVault.Outcome.Saved, latencyFrames: Int) {
        // On its track and saved first: whatever fails afterwards (memory for the engine's copy),
        // the take is in the project, and its sound comes back from its file.
        session.edit { it.withTake(r.take) }
        AppLog.add("Prise audio : ${"%.1f".format(r.take.seconds)} s, latence compensée ${latencyFrames * 1000 / OfflineExport.SAMPLE_RATE} ms")
        // The waiting note goes only once the project with the take is really on disk.
        if (withContext(NonCancellable) { session.flush() }) {
            withContext(Dispatchers.IO) { PendingTakes.remove(store.folder(projectId), r.take.file) }
        } else {
            AppLog.add("Prise : projet pas encore sauvé, la prise reste en attente")
        }
        val prepared = withContext(Dispatchers.Default) { padSamples.prepared(r.take.file, r.pcm) }
        padSamples.register(prepared)
        pushAudio()
        // Long clips weigh tens of MB each: older takes and edits leave memory (Annuler reloads them).
        padSamples.prune(project.value.sampleFiles)
    }

    private var takeJob: kotlinx.coroutines.Job? = null
    private var takeLatencyMs = 0.0
    private val _takeSaving = MutableStateFlow(false)
    /** A stopped take is still being written to its track (Annuler waits for it). */
    val takeSaving: StateFlow<Boolean> = _takeSaving.asStateFlow()

    /** Before another project opens (or a song is shared): a take in progress is stopped and kept on its track. */
    suspend fun keepTake() {
        if (_audioRecording.value != null) stopAudioRecording()
        takeJob?.join()
    }

    /** Leaves the take without keeping it. */
    fun cancelAudioRecording() {
        val handle = recordHandle
        recordHandle = 0L
        recordPoll?.cancel()
        _audioRecording.value = null
        recordingSong = false
        takePlan = null
        if (_transport.value.playing) togglePlay()
        endTakeMute()
        TakeVault.app.discard(handle)
    }

    /** Clip edits: a new sound file each time, so Annuler brings the previous one back. */
    private fun editClipAudio(id: Int, label: String, keep: AudioClip? = null, transform: (Pcm) -> Pcm) {
        val clip = project.value.audioTracks.firstOrNull { it.id == id }?.clip ?: return
        audioJob("$label…") {
            val pcm = withContext(Dispatchers.Default) { transform(padSamples.read(clip.file)) }
            putClip(id, pcm, clip.name, keep = keep ?: clip)
        }
    }

    /** Loudest point at -1 dBFS. */
    fun normalizeAudio(id: Int) = editClipAudio(id, tr("Normalisation")) { AudioEdits.normalize(it) }
    fun reverseAudio(id: Int) = editClipAudio(id, tr("Inversion")) { AudioEdits.reverse(it) }
    /** Keeps [from]..[to] (fractions of the clip); what is kept stays where it was heard (see [AudioClip.afterCutStart]). */
    fun trimAudio(id: Int, from: Float, to: Float) {
        val clip = project.value.audioTracks.firstOrNull { it.id == id }?.clip ?: return
        val cut = from.coerceIn(0f, 1f) * clip.seconds
        val p = project.value
        val keep = clip.afterCutStart(cut, p.tempoBpm, p.sceneSteps(clip.scene ?: p.scene))
        editClipAudio(id, tr("Découpe"), keep = keep) { AudioEdits.trim(it, from, to) }
    }

    // --- Live playing: velocity, pitch bend, mod wheel, sustain, arpeggiator --------------------

    private val _play = MutableStateFlow(PlayUi())
    /** The keyboard's live settings (not saved in the project). */
    val play: StateFlow<PlayUi> = _play.asStateFlow()

    private fun expression(kind: Int, value: Float, id: Int = track) {
        if (NativeEngine.isAvailable) NativeEngine.liveExpression(kind, value, id)
    }

    /** -1..1, springs back to 0 (±2 semitones). */
    fun setBend(amount: Float) {
        _play.value = _play.value.copy(bend = amount)
        expression(NativeEngine.EXPR_BEND, amount * com.pocketstudio.app.midi.MidiViewModel.BEND_RANGE)
    }

    fun setMod(amount: Float) {
        _play.value = _play.value.copy(mod = amount)
        expression(NativeEngine.EXPR_MOD, amount)
    }

    fun setSustain(on: Boolean) {
        _play.value = _play.value.copy(sustain = on)
        expression(NativeEngine.EXPR_SUSTAIN, if (on) 1f else 0f)
    }

    fun setArp(arp: ArpUi) {
        _play.value = _play.value.copy(arp = arp)
        pushArp(track)
    }

    private fun pushArp(id: Int) {
        val a = _play.value.arp
        expression(NativeEngine.EXPR_ARP_RATE, a.steps, id)
        expression(NativeEngine.EXPR_ARP_MODE, a.mode.toFloat(), id)
        expression(NativeEngine.EXPR_ARP_OCTAVES, a.octaves.toFloat(), id)
        expression(NativeEngine.EXPR_ARP_GATE, a.gate, id)
        expression(NativeEngine.EXPR_ARP_ON, if (a.on) 1f else 0f, id)
    }

    /** The live settings leave the old track and follow the newly selected one. */
    private fun movePlay(from: Int, to: Int) {
        expression(NativeEngine.EXPR_BEND, 0f, from)
        expression(NativeEngine.EXPR_MOD, 0f, from)
        expression(NativeEngine.EXPR_SUSTAIN, 0f, from)
        expression(NativeEngine.EXPR_ARP_ON, 0f, from)
        _play.value = _play.value.copy(bend = 0f, mod = 0f, sustain = false)
        pushArp(to)
    }

    // --- MIDI learn: a controller's knobs move settings ------------------------------------------

    private val _learning = MutableStateFlow<AutoTarget?>(null)
    /** The setting waiting for a knob to be turned (null: not learning). */
    val learning: StateFlow<AutoTarget?> = _learning.asStateFlow()

    fun learn(target: AutoTarget?) {
        _learning.value = target
    }

    fun removeMapping(controller: Int) = session.edit { p -> p.copy(midiMap = p.midiMap.filterNot { it.controller == controller }) }

    /** A knob moved on the MIDI controller: learnt now, or applied to its setting. */
    fun midiControl(channel: Int, controller: Int, value: Int) {
        if (controller == com.pocketstudio.app.midi.MidiViewModel.CC_MOD_WHEEL || controller == com.pocketstudio.app.midi.MidiViewModel.CC_SUSTAIN) {
            if (_learning.value == null) return  // played by the engine already
        }
        _learning.value?.let { target ->
            _learning.value = null
            session.edit { p ->
                p.copy(midiMap = (p.midiMap.filterNot { it.controller == controller || it.target == target } + MidiMapping(controller, target)).take(MidiMapping.MAX))
            }
            AppLog.add("MIDI learn : bouton $controller")
            return
        }
        val mapping = project.value.midiMap.firstOrNull { it.controller == controller } ?: return
        val v = value / 127f
        val t = mapping.target
        when (t.kind) {
            AutoTarget.VOLUME -> setChannelVolume(t.ch, v)
            AutoTarget.PAN -> setChannelPan(t.ch, v * 2f - 1f)
            AutoTarget.SEND -> setSend(t.ch, t.slot, v)
            AutoTarget.INSERT_PARAM -> setInsertParam(t.ch, t.slot, t.param, v)
            AutoTarget.BUS_PARAM -> setBusParam(t.slot, t.param, v)
            // During a take the 303 plays as on the Live page (heard, written into the song, its saved
            // settings kept for the rest of the song).
            AutoTarget.ACID -> if (performanceTaking && t.param in AutoTarget.ACID_PARAMS) setLiveAcid(t.param, v) else setAcidSound(t.param, v)
            MidiMapping.SYNTH -> setSynthParam(t.param, v)
            MidiMapping.PLAITS -> setPlaitsParam(t.param, v)
            // The live effects as the Live page plays them (followed, held against song curves, written into a take).
            MidiMapping.PERFORMANCE -> livePerformance(
                t.param,
                if (t.param in AutoTarget.LIVE_ON_OFF && t.param != AutoTarget.LIVE_DIVISION) (if (v > 0.5f) 1f else 0f) else v,
            )
            MidiMapping.TEMPO -> setTempo((60.0 + 140.0 * v).roundToInt().toDouble())
        }
    }

    // --- A sound played across the keys (track source 2) ----------------------------------------

    private fun pushSampled(p: Project = project.value, id: Int = track) {
        if (!NativeEngine.isAvailable) return
        val t = p.track(id)
        t.sampled.engineValues().forEachIndexed { i, v -> NativeEngine.setTrackSampleParam(i, v, t.id) }
        NativeEngine.setTrackSample(t.id, t.sampled.file?.let { padSamples.native(it) })
    }

    private val _trackSoundBusy = MutableStateFlow<String?>(null)
    /** "Préparation du son…" while a library sound becomes the track's instrument, or an error. */
    val trackSoundBusy: StateFlow<String?> = _trackSoundBusy.asStateFlow()

    /**
     * [pcm] becomes the selected track's instrument, played across the keys from the note it was
     * recorded at (detected). An 808 glides between held notes from the start. One undo step.
     */
    fun useAsTrackSound(pcm: Pcm, name: String, kind: String? = null): Boolean {
        if (_trackSoundBusy.value == BUSY_TRACK_SOUND) return false
        val id = track
        _trackSoundBusy.value = BUSY_TRACK_SOUND
        viewModelScope.launch {
            try {
                val root = withContext(Dispatchers.Default) {
                    val ch = pcm.channels
                    val mono = FloatArray(pcm.data.size / ch) { i -> (0 until ch).sumOf { c -> pcm.data[i * ch + c].toDouble() }.toFloat() / ch }
                    RootNote.detect(mono, pcm.sampleRate)
                }
                val (sample, prepared) = withContext(Dispatchers.IO) { padSamples.add(pcm, name) }
                padSamples.register(prepared)
                // The track went away meanwhile (deleted, undone): never land on another one.
                if (id != 0 && project.value.tracks.none { it.id == id }) {
                    _trackSoundBusy.value = tr("La piste a été supprimée entre-temps : rien n'a changé.")
                    return@launch
                }
                // A 808 glides, unless the track plays chords (one gliding voice would drop them).
                val notes = project.value.track(id).notes.notes
                val slide = kind == "808" && SampledPatch.isMonophonic(notes)
                editTrack(id = id) {
                    it.copy(
                        engine = 2, instrument = null,
                        sampled = SampledPatch(
                            file = sample.file, name = name, root = SampledPatch.fitRoot(root, notes),
                            glide = if (slide) SampledPatch.SLIDE else 0f,
                            release = if (slide || kind == "bass") 0.08f else 0.2f,
                        ),
                    )
                }
                AppLog.add("Piste ${id + 1} : « $name » au clavier, note d'origine ${root?.let { "%.2f".format(it) } ?: "inconnue (Do 3)"}")
                pushInstrument()
                pushSampled(id = id)
                pushPlaits(id = id)
                releaseUnusedFonts()
                padSamples.prune(project.value.sampleFiles)
                _trackSoundBusy.value = if (root == null) tr("Note d'origine introuvable : règle-la à l'oreille avec − et +.") else null
                if (NativeEngine.isAvailable && !_transport.value.playing) previewNote(root?.let { Math.round(it) }?.coerceIn(24, 96) ?: 60)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Throwable) {
                AppLog.add("Son au clavier : échec (${e.message})")
                _trackSoundBusy.value = tr("Ce son n'a pas pu être préparé.")
            }
        }
        return true
    }

    /**
     * The selected pad's own sound, cut to its region (and reversed if it is), played across the
     * keyboard of the selected track from its original note. False when the pad has no sample.
     */
    fun padToKeyboard(): Boolean {
        val s = project.value.drums.lanes[_transport.value.selectedPad].sample ?: return false
        // Another sound is still being prepared for the keyboard: this touch waits for it.
        if (_trackSoundBusy.value == BUSY_TRACK_SOUND) return false
        viewModelScope.launch {
            val pcm = runCatching { withContext(Dispatchers.IO) { padSamples.read(s.file) } }.getOrNull()
            if (pcm == null || pcm.frames == 0) {
                if (_trackSoundBusy.value != BUSY_TRACK_SOUND) _trackSoundBusy.value = tr("Ce son n'a pas pu être lu.")
                return@launch
            }
            val ch = pcm.channels
            val from = (s.start * pcm.frames).toInt().coerceIn(0, pcm.frames - 1)
            val to = (s.end * pcm.frames).toInt().coerceIn(from + 1, pcm.frames)
            var data = pcm.data.copyOfRange(from * ch, to * ch)
            if (s.reverse) {
                val frames = data.size / ch
                data = FloatArray(data.size) { i -> data[(frames - 1 - i / ch) * ch + i % ch] }
            }
            useAsTrackSound(Pcm(data, ch, pcm.sampleRate), s.name)
        }
        return true
    }

    /** A sound file of the phone on the keyboard of the selected track. */
    fun importTrackSound(uri: Uri) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val name = PadSamples.nameFrom(displayName(app, uri))
            val pcm = runCatching { withContext(Dispatchers.IO) { AudioDecoder.decode(app, uri) } }.getOrNull()
            if (pcm == null || PadSamples.isSilent(pcm)) {
                if (_trackSoundBusy.value != BUSY_TRACK_SOUND) _trackSoundBusy.value = tr("Ce fichier n'a pas pu être lu.")
                return@launch
            }
            AppLog.add("Son au clavier importé « $name » : ${"%.1f".format(pcm.seconds)} s")
            useAsTrackSound(pcm.withoutTail(), name)
        }
    }

    /** A change to the selected track's sampled sound (slider drags merge into one undo step per [key]). */
    fun editSampled(key: String?, transform: (SampledPatch) -> SampledPatch) {
        editTrack(key?.let { "sampled-$it-$track" }) { it.copy(sampled = transform(it.sampled)) }
        pushSampled()
    }

    fun dismissTrackSoundMessage() {
        if (_trackSoundBusy.value != BUSY_TRACK_SOUND) _trackSoundBusy.value = null
    }

    // --- Plaits (Mutable Instruments) voice for the instrument tracks -------------------------

    private fun pushPlaits(p: Project = project.value, id: Int = track) {
        if (!NativeEngine.isAvailable) return
        val t = p.track(id)
        t.plaits.engineValues().forEachIndexed { i, v -> NativeEngine.setPlaitsParam(i, v, t.id) }
        NativeEngine.setSynthEngine(t.engine, t.id)
    }

    /** 0 = subtractive synth, 1 = Plaits, 2 = the sampled sound; leaves any General MIDI instrument. */
    fun selectSynthEngine(engine: Int) {
        editTrack { it.copy(engine = engine, instrument = null) }
        pushInstrument()
        pushSampled()
        pushPlaits()
        releaseUnusedFonts()
    }

    fun setPlaitsModel(model: Int) {
        editTrack { it.copy(plaits = it.plaits.copy(model = model)) }
        pushPlaits()
        if (NativeEngine.isAvailable && !_transport.value.playing) {
            viewModelScope.launch {
                kotlinx.coroutines.delay(30)
                previewNote(60)  // hear the new model right away
            }
        }
    }

    fun setPlaitsParam(param: Int, value: Float) {
        editTrack("plaits-$param-$track") { it.copy(plaits = it.plaits.with(param, value)) }
        if (NativeEngine.isAvailable) NativeEngine.setPlaitsParam(param, value.coerceIn(0f, 1f), track)
    }

    private fun pushDrumPattern(drums: DrumPattern) {
        if (waitsFor(LiveSlots.DRUMS)) return  // another scene plays: this one goes to the engine on its turn
        NativeEngine.setPatternSteps(drums.steps)
        drums.lanes.forEachIndexed { lane, l ->
            for (step in 0 until DrumPattern.MAX_STEPS) pushHit(lane, step, l.hits[step])
        }
    }

    // --- Scenes and song ------------------------------------------------------------------------

    private val _sessionUi = MutableStateFlow(SessionUi())
    val sessionUi: StateFlow<SessionUi> = _sessionUi.asStateFlow()
    private var swapsSeen = 0
    private var swapWatch: kotlinx.coroutines.Job? = null

    /** The song part still playing after it was taken out of the song (see [SessionUi.removed]). */
    private val removedPlaying: SongPart? get() = _sessionUi.value.removed

    private val _listenSong = MutableStateFlow(project.value.listenSong)
    /**
     * What ▶ plays: the song (true) or the scene shown (false), as last chosen in this project
     * (saved with it); null: never chosen, see [onSongTab].
     */
    val listenSong: StateFlow<Boolean?> = _listenSong.asStateFlow()

    /** ▶ plays the song (from its first part) rather than the scene shown; kept with the project (no undo step). */
    fun setListenSong(song: Boolean) {
        _listenSong.value = song
        keepListenChoice()
    }

    /**
     * « Lancer à » and the Live page's effect settings are not undo steps: Annuler / Rétablir of
     * something else keep them as they were ([before]: the project before the step).
     */
    private fun keepLiveSettings(before: Project) {
        val p = project.value
        if (p.liveQuantum == before.liveQuantum && p.liveFx == before.liveFx) return
        session.editQuietly { it.copy(liveQuantum = before.liveQuantum, liveFx = before.liveFx) }
        pushLiveFxSettings(project.value)
    }

    /** The project keeps the listening choice (also after Annuler / Rétablir, which do not change it). */
    private fun keepListenChoice() {
        val choice = _listenSong.value
        if (project.value.listenSong != choice) session.editQuietly { it.copy(listenSong = choice) }
    }

    /**
     * True while the Morceau tab is shown (set by the screen): there, ▶ plays the song until the
     * user chooses; on the editing tabs, the section shown (what is being edited is heard).
     */
    var onSongTab = false

    /** True when ▶ plays the song. */
    val listensToSong: Boolean get() = project.value.song.isNotEmpty() && (_listenSong.value ?: onSongTab)

    private val _followSong = MutableStateFlow(true)
    /** While the song plays: the screens follow the part playing (true) or stay on the scene picked (false). */
    val followSong: StateFlow<Boolean> = _followSong.asStateFlow()

    fun setFollowSong(follow: Boolean) {
        _followSong.value = follow
        // Following again: the screens jump to what plays now.
        val heard = heardScene
        if (follow && heard != null && heard != project.value.scene && heard in project.value.allScenes.indices) {
            session.editQuietly { it.switchScene(heard) }
        }
    }

    /** At the song's end: stop (false) or start over (true). One undo step. */
    fun setSongLoops(loops: Boolean) {
        session.edit { it.copy(songLoops = loops) }
        if (songPlaying()) stageSongPart()
    }

    /**
     * The scene the engine plays while the transport runs (null: not tracked, the scene shown).
     * Staying on a scene while the song plays, it differs from the scene shown: edits then wait
     * for that scene's turn (see [editsWait]).
     */
    private var heardScene: Int? = null

    /** True while [pushPart] sends a part on purpose (to the playing or the waiting slot). */
    private var pushingPart = false

    /**
     * The engine plays another scene than the one being edited (the song goes on, the screens
     * stay): pattern edits must not reach the playing slot. They reach the engine when their scene
     * is sent ahead of its turn ([stageSongPart], [restageIfEditingUpcoming]).
     */
    /** The scene the engine plays now (another one than [p]'s while staying on a scene during the song). */
    private fun engineScene(p: Project): Int = if (editsWait) heardScene ?: p.scene else p.scene

    private val editsWait: Boolean get() {
        val heard = heardScene ?: return false
        return !pushingPart && !NativeEngine.isCapturing && _transport.value.playing && heard != project.value.scene
    }

    /** The screens follow the scene the engine swaps in (always, except while staying on a scene during the song). */
    private fun screensFollow(ui: SessionUi): Boolean = _followSong.value || (ui.songIndex < 0 && ui.queued < 0)

    /** Stopped: shows and edits [index]. Playing: [index] takes over at the next bar (or, staying on a scene during the song, is just shown). */
    fun selectScene(index: Int) {
        if (index !in 0 until Scene.MAX) return
        if (!_transport.value.playing || !NativeEngine.isAvailable) {
            _sessionUi.value = SessionUi()
            session.edit { it.switchScene(index) }
            syncEngine()
            return
        }
        if (!_followSong.value && songPlaying()) {
            // The song plays on; the screens show [index] (its edits wait for its turn).
            if (index != project.value.scene) session.edit { it.switchScene(index) }
            return
        }
        if (index == project.value.scene && _sessionUi.value.pending == null) return
        _sessionUi.value = SessionUi(pending = index)
        stageScene(index, 0)
    }

    /** Names scene [index] after a song section ("Refrain"; blank: back to its letter). One undo step. */
    fun renameScene(index: Int, label: String) {
        if (index !in project.value.allScenes.indices) return
        session.edit { it.renameScene(index, label) }
    }

    /**
     * Scene [from] dragged onto slot [to] (one undo step): moved to an empty slot, or swapped
     * with the scene there, or put in its place ([replace]). What plays goes on: the scene heard,
     * the one waiting and the song parts follow their scene's new place.
     */
    fun moveScene(from: Int, to: Int, replace: Boolean = false) {
        if (from == to || from !in project.value.allScenes.indices || to !in 0 until Scene.MAX) return
        // A take records into its scene's slot (as for Annuler, the take ends first).
        if (_recording.value || _audioRecording.value != null || _takeSaving.value) {
            _notice.value = tr("Arrête d'abord l'enregistrement pour déplacer une scène.")
            return
        }
        val move = SceneMove(from, to, replace)
        val heard = sceneHeard()?.let(move::forward)
        followSceneMove(move::forward)
        session.edit(tag = move) { it.moveScene(from, to, replace) }
        // The engine gets the scenes as they are now, and the waiting one is sent again (the song
        // keeps its order and the part chosen to come next; outside the song, as after Annuler).
        if (heard != null && _sessionUi.value.songIndex >= 0) {
            syncEngine()
            stageSongPart()
        } else {
            followPlaybackAfterHistory(heard)
        }
    }

    /**
     * True (and the warning shown) when [next] has more long effects of a pool (echoes, reverbs,
     * tape stops) than the engine holds and more than now: the edit must not happen, or the last
     * one would stay silent with nothing said.
     */
    private fun longFxRefused(next: Project): Boolean {
        val over = next.mixer.longFxOverLimit(project.value.mixer) ?: return false
        _notice.value = longFxLimitNotice(over)
        return true
    }

    /** After a project opens, or Annuler/Rétablir: more long effects than the engine holds are said, not left silent. */
    private fun warnLongFxOverLimit(before: Project? = null) {
        project.value.mixer.longFxOverLimit(before?.mixer)?.let { _notice.value = longFxLimitNotice(it) }
    }

    /** What plays and waits follows a scene move (done, undone or redone): [map] gives each scene's new place. */
    private fun followSceneMove(map: (Int) -> Int) {
        heardScene = heardScene?.let(map)
        val ui = _sessionUi.value
        _sessionUi.value = ui.copy(pending = ui.pending?.let(map), removed = ui.removed?.let { it.copy(scene = map(it.scene)) })
    }

    /** Shows a new, empty scene (the first free slot), to start something else. */
    fun newScene() {
        val target = project.value.firstEmptyScene()
        if (target == null) {
            _notice.value = tr("Les %d scènes sont déjà prises : vide-en une pour recommencer.", Scene.MAX)
            return
        }
        selectScene(target)
    }

    /** Empties the scene shown (one undo step): its notes and hits go, its sounds stay. */
    fun clearScene() {
        session.edit { it.clearCurrentScene() }
        syncEngine()
    }

    /** Copies the current scene to the first empty slot and shows it (one undo step): the usual way to make a variation. */
    fun duplicateScene() {
        val p = project.value
        val scenes = p.allScenes
        val target = scenes.indices.firstOrNull { it != p.scene && scenes[it].isEmpty } ?: scenes.size.takeIf { it < Scene.MAX }
        if (target == null) {
            _notice.value = tr("Les %d scènes sont déjà prises : plus de place pour une copie.", Scene.MAX)
            return
        }
        if (!_transport.value.playing || !NativeEngine.isAvailable || (!_followSong.value && songPlaying())) {
            if (!_transport.value.playing || !NativeEngine.isAvailable) _sessionUi.value = SessionUi()
            session.edit { it.copySceneTo(target).switchScene(target) }
            syncEngine()
            return
        }
        // Playing: the copy takes over at the next bar, and the screens follow it then (no second step).
        session.edit { it.copySceneTo(target) }
        selectScene(target)
    }

    /** Time signature [beats]/4: the drum loop keeps its number of bars (up to 64 steps). */
    fun setBeatsPerBar(beats: Int) {
        session.edit { it.withBeatsPerBar(beats) }
        syncEngine()
    }

    /** The timeline's long press: track [key] plays scene [scene] in song part [index] (one undo step). */
    fun setPartScene(index: Int, key: String, scene: Int) {
        val before = project.value
        session.edit { it.withPartScene(index, key, scene) }
        if (project.value == before) return
        val ui = _sessionUi.value
        val song = project.value.song
        if (!NativeEngine.isAvailable || !_transport.value.playing || ui.songIndex < 0 || song.isEmpty()) return
        // The part on now waits for its next turn (its tracks carry on); the next one is sent again.
        stageSongPart()
    }

    /** The timeline: track [key] plays or stays silent in song part [index]. */
    fun togglePartTrack(index: Int, key: String) {
        session.edit { it.togglePartTrack(index, key).sanitized() }
        // Playing the song: the part on now changes at once, the next one is sent again.
        val ui = _sessionUi.value
        val song = project.value.song
        if (!NativeEngine.isAvailable || !_transport.value.playing || ui.songIndex < 0 || song.isEmpty()) return
        if (ui.songIndex == index && removedPlaying == null) pushPart(project.value, song[index].scene, song[index])
        stageSongPart()
    }

    /** Scene [scene] added at the end of the song. */
    fun addSongPart(scene: Int) = insertSongPart(project.value.song.size, scene)

    /** Scene [scene] played once at place [at] of the song (one undo step). */
    fun insertSongPart(at: Int, scene: Int) {
        if (project.value.song.size >= SongPart.MAX_PARTS) {
            _notice.value = tr("Le morceau a déjà %d parties : retires-en une pour en ajouter une autre.", SongPart.MAX_PARTS)
            return
        }
        val place = at.coerceIn(0, project.value.song.size)
        val following = songPlaying()
        val before = project.value.song
        session.edit { it.insertSongPart(place, scene) }
        if (project.value.song == before) return  // refused (a scene no longer there): nothing moved
        // Playing: the part on now keeps playing; one inserted right after it comes next.
        if (following) followSongEdit { i, explicit -> if (if (explicit) place <= i else place < i) i + 1 else i }
    }

    /** Song part [index] played again right after itself (one undo step). */
    fun duplicateSongPart(index: Int) {
        if (index !in project.value.song.indices) return
        if (project.value.song.size >= SongPart.MAX_PARTS) {
            _notice.value = tr("Le morceau a déjà %d parties : retires-en une pour en ajouter une autre.", SongPart.MAX_PARTS)
            return
        }
        val following = songPlaying()
        session.edit { it.duplicateSongPart(index) }
        if (following) followSongEdit { i, explicit -> if (if (explicit) index + 1 <= i else index + 1 < i) i + 1 else i }
    }

    /** Song part [from] moved to place [to] (one undo step): up or down the song. */
    fun moveSongPart(from: Int, to: Int) {
        val song = project.value.song
        if (from !in song.indices || to !in song.indices || from == to) return
        val following = songPlaying()
        session.edit { it.moveSongPart(from, to) }
        if (following) followSongEdit { i, _ -> if (i < song.size) songIndexAfterMove(i, from, to) else i }
    }

    /**
     * Playing the song while it was edited: the part on now and the one chosen to come next keep
     * their place ([map]: an old index to the new one; its flag: an index the user chose, not
     * just "the part that follows"), then the next part is sent again.
     */
    private fun followSongEdit(map: (Int, Boolean) -> Int) {
        val ui = _sessionUi.value
        val playing = if (ui.removed == null) map(ui.songIndex, true) else ui.songIndex
        val queued = if (ui.queued < 0) -1 else map(ui.queued, ui.removed == null)
        _sessionUi.value = ui.copy(songIndex = playing.coerceIn(0, (project.value.song.size - 1).coerceAtLeast(0)), queued = queued)
        stageSongPart()
    }

    fun removeSongPart(index: Int) {
        val before = project.value.song
        if (index !in before.indices) return
        val following = songPlaying()
        session.edit { it.removeSongPart(index) }
        if (!following) return
        // Playing the song: what plays now finishes, then the song goes on in its new order.
        val ui = _sessionUi.value
        val song = project.value.song
        if (song.isEmpty()) {
            endSongFollow()
            return
        }
        val removedNow = ui.removed == null && index == ui.songIndex
        val queued = when {
            ui.queued >= 0 -> if (ui.queued > index) ui.queued - 1 else ui.queued
            removedNow -> index  // the part that followed it (now at its place) comes next
            else -> -1
        }
        val removed = ui.removed ?: before[index].takeIf { removedNow }
        val playing = songIndexAfterRemoval(ui.songIndex, index, song.size)
        _sessionUi.value = ui.copy(songIndex = playing, removed = removed, queued = queued)
        stageSongPart()
    }

    /** What follows the part (song curves, song clips) moves with its new end, in the same undo step. */
    fun setSongRepeats(index: Int, repeats: Int) {
        session.edit("song-$index") { p -> p.withSongRepeats(index, repeats) }
        // The part playing now: its end moves (a swap armed for a bar already gone waits for the next bar).
        if (songPlaying() && index == _sessionUi.value.songIndex && removedPlaying == null) stageSongPart()
    }

    /** True while the song plays in the engine (song mode). */
    private fun songPlaying(): Boolean =
        NativeEngine.isAvailable && _transport.value.playing && _sessionUi.value.songIndex >= 0 && project.value.song.isNotEmpty()

    /**
     * The song stops following its parts: the scene playing goes on as a loop. Staying on another
     * scene, that one takes over at the next bar (the screens and the sound agree again).
     */
    private fun endSongFollow() {
        val edited = project.value.scene
        val heard = heardScene
        if (NativeEngine.isAvailable && _transport.value.playing && heard != null && heard != edited) {
            _sessionUi.value = SessionUi(pending = edited)
            stageScene(edited, 0)
            return
        }
        _sessionUi.value = SessionUi()
        // The scene playing goes on by itself: song curves and song clips wait for the song.
        if (NativeEngine.isAvailable) NativeEngine.leaveSong()
    }

    /** Plays the song from its first part (or stops it). */
    fun toggleSong() {
        if (_transport.value.playing) {
            toggleTransport()
            return
        }
        playSongFrom(0)
    }

    /**
     * A touch on song part [index]: stopped, the song plays from there; playing the song, that
     * part comes next (a touch on the part already chosen goes back to the usual order);
     * playing a scene, the song starts from that part at the next bar.
     */
    fun playSongPart(index: Int) {
        val song = project.value.song
        if (index !in song.indices || !NativeEngine.isAvailable) return
        val ui = _sessionUi.value
        when {
            !_transport.value.playing -> playSongFrom(index)
            ui.songIndex >= 0 -> {
                _sessionUi.value = ui.copy(queued = if (ui.queued == index && ui.removed == null) -1 else index)
                stageSongPart()
            }
            else -> {
                _sessionUi.value = SessionUi(pending = song[index].scene, queued = index)
                stageScene(song[index].scene, 0, song[index], restartsSong = true, songStep = project.value.songStepOf(index))
            }
        }
    }

    /**
     * Starts the song from part [index] (the screens go to its scene, unless they stay on theirs),
     * after [countInBars] (null: the count-in switch). The song position starts at the part's place
     * in the song, with the automation as the parts before it left it: what plays is what the
     * export has there, and song automation records on the right bar.
     */
    private fun playSongFrom(index: Int, countInBars: Int? = null) {
        val song = project.value.song
        if (index !in song.indices || !NativeEngine.isAvailable) return
        val part = song[index]
        // Playing is not an edit: no undo step for the screens going to the part's scene.
        if (_followSong.value) session.editQuietly { it.switchScene(part.scene) }
        syncEngine()
        pushPart(project.value, part.scene, part)  // the part's scene without its muted tracks
        applyAutomationState(project.value, project.value.automationBefore(index))
        heardScene = part.scene
        _sessionUi.value = SessionUi(songIndex = index)
        toggleTransport(countInBars, project.value.songStepOf(index))
        // The whole project just went to the engine: let the queue drain before the next scene.
        viewModelScope.launch {
            kotlinx.coroutines.delay(150)
            val ui = _sessionUi.value
            if (_transport.value.playing && ui.songIndex == index && ui.pending == null && !ui.ending) stageSongPart()
        }
    }

    /**
     * In song mode the next part is sent ahead; if it is the scene being edited (a part
     * repeated, or A then A), edits must reach that waiting copy too, or the swap would undo them.
     * The same for a scene waiting to take over at the next bar.
     */
    private var restageJob: kotlinx.coroutines.Job? = null

    private fun restageIfEditingUpcoming() {
        val ui = _sessionUi.value
        if (ui.pending != project.value.scene || !_transport.value.playing || !NativeEngine.isAvailable) return
        restageJob?.cancel()
        restageJob = viewModelScope.launch {
            kotlinx.coroutines.delay(150)  // one resend after a burst of edits
            val ui2 = _sessionUi.value
            if (ui2.pending != project.value.scene || !_transport.value.playing) return@launch
            when {
                ui2.songIndex >= 0 -> stageSongPart()
                ui2.queued >= 0 -> project.value.song.getOrNull(ui2.queued)?.let { stageScene(it.scene, 0, it, restartsSong = true, songStep = project.value.songStepOf(ui2.queued)) }
                else -> stageScene(ui2.pending, 0)
            }
        }
    }

    /** The engine swapped scenes but the 40 ms watch had not seen it yet: follow it now. */
    private fun catchUpMissedSwap() {
        if (!NativeEngine.isAvailable) return
        val ui = _sessionUi.value
        if (ui.pending != null && NativeEngine.sceneSwaps() != swapsSeen) {
            swapsSeen = NativeEngine.sceneSwaps()
            ui.pending.let { now -> if (screensFollow(ui)) session.editQuietly { it.switchScene(now) } }
        }
    }

    /**
     * Sends scene [index] to the engine's waiting slot and arms the swap after [afterSteps] (0 = next
     * bar). A song [part] plays without its muted tracks.
     */
    private fun stageScene(index: Int, afterSteps: Int, part: SongPart? = null, restartsSong: Boolean = false, songStep: Int = 0) {
        NativeEngine.setStaging(true)
        pushPart(project.value, index, part)
        NativeEngine.setStaging(false)
        // A scene alone (not a song part) plays outside the song.
        NativeEngine.armSceneSwap(afterSteps, restartsSong, songStep, leavesSong = part == null && !restartsSong)
        watchSwaps()
    }

    /** Loop and per-scene lanes' settings as [values] (lane index → 0..1) say, like the engine sets them. */
    private fun applyAutomationState(p: Project, values: Map<Int, Float>) {
        values.forEach { (i, v) ->
            val t = p.automation.getOrNull(i)?.target ?: return@forEach
            when (t.kind) {
                AutoTarget.VOLUME -> NativeEngine.setChannelVolume(t.ch, v)
                AutoTarget.PAN -> NativeEngine.setChannelPan(t.ch, v * 2f - 1f)
                AutoTarget.SEND -> NativeEngine.setSend(t.ch, t.slot, v)
                AutoTarget.INSERT_PARAM -> NativeEngine.setInsertParam(t.ch, t.slot, t.param, v)
                AutoTarget.BUS_PARAM -> NativeEngine.setBusParam(t.slot, t.param, v)
                AutoTarget.ACID -> NativeEngine.setAcidParam(t.param, v)
                AutoTarget.MACRO -> NativeEngine.setMacro(t.ch, t.param, v)
                AutoTarget.LIVE ->
                    if (t.param == AutoTarget.LIVE_REVERB_RETURN || t.param == AutoTarget.LIVE_ECHO_RETURN) {
                        NativeEngine.setBusReturn(t.param - AutoTarget.LIVE_REVERB_RETURN, v * 2f)
                    } else {
                        NativeEngine.setPerformance(t.param, v)
                    }
            }
        }
    }

    /** The patterns of scene [index] as [part] plays them (to the playing or the waiting slot). */
    private fun pushPart(p: Project, index: Int, part: SongPart?) {
        pushingPart = true
        try {
            val view = if (part != null) p.partView(part) else p.sceneView(index)
            pushDrumPattern(view.drums)
            // The 303 knobs that lanes move in this scene stay where the automation has them;
            // the others (a lane without points here) go back to the project's setting.
            pushAcid(view.acid, keep = p.acidKnobsDriven(part?.sceneFor(SongPart.ACID) ?: index, inSong = part != null))
            view.instrumentTracks.forEach { pushNotes(it.notes, it.id) }
            for (id in 0 until AudioTrack.MAX) NativeEngine.setAudioOn(id, audioPlays(p, id, part, index))
            // "Once" clips: once per song part, or once per pass of the scene played alone.
            val sceneSteps = if (part != null) null else p.sceneSteps(index)
            // Every clip's loop goes with its part (a stale one can never be left waiting).
            p.audioTracks.forEach { t -> t.clip?.let { NativeEngine.setAudioLoop(t.id, it.engineLoopSteps(sceneSteps, p.tempoBpm)) } }
            // A part written from the live grid: each track carries on where it was (else from its start).
            for (slot in 0 until LiveSlots.COUNT) NativeEngine.setSlotOffset(slot, (part?.fromBars(LiveSlots.key(slot)) ?: 0) * p.stepsPerBar)
            // The remix: silent in a part that mutes it (it keeps its place), else heard.
            NativeEngine.setRemixOn(part?.plays(SongPart.REMIX) ?: true)
            // The scene a strip's track plays in this part (the master and the buses: the part's).
            fun sceneOf(ch: Int?): Int = ch?.let(::liveSlotOfChannel)?.let { part?.sceneFor(LiveSlots.key(it)) } ?: index
            // Per-scene automation clips: the scene's own steps (all of them, empty ones included).
            p.automation.forEachIndexed { i, lane ->
                if (lane.mode != AutomationLane.PER_SCENE) return@forEachIndexed
                val scene = when (lane.target.kind) {
                    AutoTarget.ACID -> part?.sceneFor(SongPart.ACID) ?: index
                    AutoTarget.BUS_PARAM, AutoTarget.LIVE -> index
                    else -> sceneOf(lane.target.ch)
                }
                lane.valuesFor(scene).forEachIndexed { step, v -> NativeEngine.setAutomationPoint(i, step, v) }
            }
            // « Joue dans »: each effect heard plays in this scene or lets the sound through.
            (usedStrips(p) + MASTER).forEach { ch ->
                insertOnCommands(p.insertsOf(ch), if (ch == MASTER) index else sceneOf(ch)) { slot, on -> NativeEngine.setInsertOn(ch, slot, on) }
            }
        } finally {
            pushingPart = false
        }
    }

    /**
     * Prepares what follows the song part now playing: the next part (or the one chosen, see
     * [SessionUi.queued]), the first again when the song starts over, or the song's end.
     */
    private fun stageSongPart() {
        // The engine may have swapped (or ended the song) since the 40 ms watch last looked: follow
        // it first, or this would stage from a part that is no longer playing.
        if (NativeEngine.isAvailable && _transport.value.playing && NativeEngine.sceneSwaps() != swapsSeen &&
            (_sessionUi.value.pending != null || _sessionUi.value.ending)
        ) {
            swapsSeen = NativeEngine.sceneSwaps()
            onSceneSwapped()
            return
        }
        val ui = _sessionUi.value
        val p = project.value
        val song = p.song
        if (song.isEmpty() || ui.songIndex < 0) return
        val playing = ui.removed ?: song.getOrNull(ui.songIndex) ?: return
        val steps = p.partSteps(playing)
        // Recording a take over the whole song: it ends with the song, never starts it over.
        val next = songNextIndex(ui.songIndex, ui.queued, song.size, p.songLoops && !recordingSong)
        if (next < 0) {
            // The song ends: the engine stops on the last part's last bar line.
            _sessionUi.value = ui.copy(pending = null, ending = true)
            NativeEngine.armSongEnd(steps)
            watchSwaps()
            return
        }
        val upcoming = song[next]
        _sessionUi.value = ui.copy(pending = upcoming.scene, ending = false)
        // The song position follows the arrangement: a part chosen out of order (or the song
        // starting over) moves it to that part's place; the next part in order keeps it running.
        stageScene(upcoming.scene, steps, upcoming, restartsSong = true, songStep = p.songStepOf(next))
    }

    /** Follows the engine's scene swaps (also with the screen off, while the transport runs). */
    private fun watchSwaps() {
        if (swapWatch?.isActive == true) return
        swapsSeen = NativeEngine.sceneSwaps()
        swapWatch = viewModelScope.launch {
            while (_transport.value.playing && (_sessionUi.value.pending != null || _sessionUi.value.songIndex >= 0)) {
                val swaps = NativeEngine.sceneSwaps()
                if (swaps != swapsSeen) {
                    swapsSeen = swaps
                    onSceneSwapped()
                }
                kotlinx.coroutines.delay(40)
            }
        }
    }

    private fun onSceneSwapped() {
        val ui = _sessionUi.value
        if (ui.ending) {
            songEnded()
            return
        }
        val now = ui.pending ?: return
        heardScene = now
        val song = project.value.song
        val index = when {
            song.isEmpty() -> -1
            ui.songIndex >= 0 -> songNextIndex(ui.songIndex, ui.queued, song.size, loops = true)
            else -> ui.queued.takeIf { it in song.indices } ?: -1  // the song starts from the part chosen
        }
        // A whole scene (a song part) took over: the live grid plays it as the engine does, each
        // track on its scene of the part (a muted one stopped), from the step it really started on.
        _liveGrid.value = gridOfPart(song.getOrNull(index)?.takeIf { it.scene == now }, now, liveTransportStep(), slotStatesNow())
        stagedFrom.fill(null)
        // The engine already plays it: only the screens follow (no undo step).
        if (screensFollow(ui)) session.editQuietly { it.switchScene(now) }
        // Edits held for this scene ("rester sur ma scène") that the waiting copy missed: it plays now.
        if (restageJob?.isActive == true && now == project.value.scene) {
            restageJob?.cancel()
            pushPart(project.value, now, song.getOrNull(index)?.takeIf { it.scene == now })
        }
        if (index >= 0) {
            _sessionUi.value = SessionUi(songIndex = index)
            stageSongPart()
        } else {
            _sessionUi.value = SessionUi()
        }
    }

    /** The engine stopped on the song's last bar line: the app stops too (a microphone take is kept). */
    private fun songEnded() {
        AppLog.add("Fin du morceau")
        if (_audioRecording.value != null) stopAudioRecording()
        else if (_transport.value.playing) toggleTransport()
    }

    // --- Live grid (« Live » page): each track launches its own scene on a global quantum -------

    private val _liveGrid = MutableStateFlow<LiveState?>(null)
    /** What each column of the live grid plays and waits for while the transport runs (null: stopped). */
    val liveGrid: StateFlow<LiveState?> = _liveGrid.asStateFlow()

    private val _liveClock = MutableStateFlow(-1)
    /** Transport steps since play (-1: stopped or counting in), polled while the Live page is shown. */
    val liveClock: StateFlow<Int> = _liveClock.asStateFlow()

    private val _liveMuted = MutableStateFlow<Set<String>>(emptySet())
    /** Tracks muted on the Live page (M): instant, never written in the project, gone at Stop. */
    val liveMuted: StateFlow<Set<String>> = _liveMuted.asStateFlow()

    private val _liveAcid = MutableStateFlow<Map<Int, Float>>(emptyMap())
    /** The 303 settings moved on the Live page (param to 0..1): heard until Stop or a double tap, not written. */
    val liveAcid: StateFlow<Map<Int, Float>> = _liveAcid.asStateFlow()

    private val _liveReturns = MutableStateFlow(listOf(0.5f, 0.5f))
    /** The reverb and echo returns on the Live page (0.5 = as mixed, 0 = none, 1 = twice): until Stop. */
    val liveReturns: StateFlow<List<Float>> = _liveReturns.asStateFlow()

    /** The project each slot's part was staged from: launched on the scene edited since, it is sent again. */
    private val stagedFrom = arrayOfNulls<Project>(LiveSlots.COUNT)

    /** The grid as it plays now: every column on the scene heard when nothing was launched apart. */
    private fun liveNow(): LiveState = _liveGrid.value ?: LiveState.on(heardScene ?: project.value.scene)

    /** The grid as song part [part] plays (null: scene [scene] alone), from [start] or the engine's [states]. */
    private fun gridOfPart(part: SongPart?, scene: Int, start: Int, states: IntArray?): LiveState =
        project.value.liveStateOf(part, scene, start, states)

    /** Tests (no engine): the slot states the engine would give (see [NativeEngine.slotStates]). */
    internal var slotStatesForTest: IntArray? = null

    /** The engine's slots now (tag playing, tag waiting, start step each), or null without an engine. */
    private fun slotStatesNow(): IntArray? {
        if (!NativeEngine.isAvailable) return slotStatesForTest
        return IntArray(NativeEngine.SLOTS * 3).also(NativeEngine::slotStates)
    }

    private fun liveTransportStep(): Int = if (NativeEngine.isAvailable) NativeEngine.transportStep().coerceAtLeast(0) else takeClockForTest.coerceAtLeast(0)

    /** Tests: the engine swapped song part [index] in (its slots as [states] say), seen by the 40 ms watch. */
    internal fun songPartSwappedForTest(index: Int, states: IntArray) {
        val part = project.value.song.getOrNull(index) ?: return
        slotStatesForTest = states
        heardScene = part.scene
        _sessionUi.value = SessionUi(songIndex = index)
        _liveGrid.value = gridOfPart(part, part.scene, liveTransportStep(), slotStatesNow())
    }

    /**
     * The scene whose per-scene curves and « Joue dans » effects [slot]'s track hears now: on a
     * grid where tracks play different scenes, its own (null: stopped, left alone); else the
     * scene the engine plays ([engineScene]). Null [slot]: the master, the buses, the whole mix.
     */
    private fun sceneOfSlot(p: Project, slot: Int?): Int? {
        val g = _liveGrid.value
        if (slot == null || g == null || !g.diverged || pushingPart || NativeEngine.isCapturing || !_transport.value.playing) return engineScene(p)
        return g.slot(slot).playing.takeIf { it >= 0 }
    }

    /** The grid slot whose track automation lane target [t] belongs to (null: the master, buses, whole mix). */
    private fun laneSlot(t: AutoTarget): Int? = when (t.kind) {
        AutoTarget.ACID -> LiveSlots.ACID
        AutoTarget.BUS_PARAM, AutoTarget.LIVE -> null
        else -> liveSlotOfChannel(t.ch)
    }

    internal fun laneSceneForTest(t: AutoTarget): Int? = sceneOfSlot(project.value, laneSlot(t))

    /**
     * Edits of [slot]'s track wait for their scene's turn: on the live grid it plays another scene
     * than the one edited, or is stopped (else as [editsWait]).
     */
    private fun waitsFor(slot: Int): Boolean {
        val g = _liveGrid.value
        if (g == null || !g.diverged || pushingPart || NativeEngine.isCapturing || !_transport.value.playing) return editsWait
        return g.slot(slot).playing != project.value.scene
    }

    internal fun waitsForTest(slot: Int): Boolean = waitsFor(slot)

    /** « Lancer à » 1, 2 or 4 bars (kept with the project, no undo step). */
    fun setLiveQuantum(bars: Int) {
        if (bars !in LiveSlots.QUANTA || bars == project.value.liveQuantum) return
        session.editQuietly { it.copy(liveQuantum = bars) }
    }

    /**
     * A pad: track [key] plays its part of scene [scene] from the next launch line (each track
     * counts from its own launch). Stopped, the transport starts with that track alone. During the
     * song, the first touch leaves it (the scene playing goes on by itself).
     */
    fun launchPad(key: String, scene: Int) {
        val slot = LiveSlots.of(key) ?: return
        if (scene !in project.value.allScenes.indices) return
        if (!_transport.value.playing) {
            startLive(scene, LiveSlots.mask(slot))
            return
        }
        leaveSongForLive()
        armSlots(LiveSlots.mask(slot), scene)
    }

    /** « ▶ A · Refrain »: the whole scene from the next launch line (stopped: it starts now). */
    fun launchRow(scene: Int) {
        if (scene !in project.value.allScenes.indices) return
        if (!_transport.value.playing) {
            startLive(scene, LiveSlots.ALL)
            return
        }
        leaveSongForLive()
        armSlots(LiveSlots.ALL, scene)
    }

    /**
     * The grid as it was before each finger's press that launched ([pressLive]), by what it
     * pressed (a column's key, [ROW_PRESS] for a row or « ■ Tout »): a retract puts back what
     * waited before that press, whatever another finger pressed since.
     */
    private val beforePress = HashMap<String, com.pocketstudio.model.LiveState>()

    /**
     * A finger down on the Live grid while it plays on its own: a pad ([key], [scene]), a row
     * ([key] null) or a ■ ([scene] -1) launches at once, so it lands on the line after the press
     * (a let-go after the line would wait a whole quantum more). False (stopped, the song followed:
     * a press would leave it): nothing yet, the tap does it.
     */
    fun pressLive(key: String?, scene: Int): Boolean {
        val ui = _sessionUi.value
        if (!_transport.value.playing || ui.songIndex >= 0 || ui.pending != null || ui.ending) return false
        val grid = _liveGrid.value
        if (grid != null) beforePress[key ?: ROW_PRESS] = grid else beforePress.remove(key ?: ROW_PRESS)
        when {
            key == null && scene >= 0 -> launchRow(scene)
            key == null -> stopLiveAll()
            scene < 0 -> stopLiveTrack(key)
            else -> launchPad(key, scene)
        }
        return true
    }

    /** The finger that pressed [key] (null: a row, « ■ Tout ») slid on: the grid scrolls, the launch is taken back. */
    fun retractLive(key: String?) {
        val before = beforePress.remove(key ?: ROW_PRESS) ?: return
        if (!_transport.value.playing) return
        val mask = if (key == null) LiveSlots.ALL else LiveSlots.of(key)?.let(LiveSlots::mask) ?: return
        // Already launched (the line passed meanwhile): it stays, as heard.
        val waiting = (0 until LiveSlots.COUNT).filter { mask and (1 shl it) != 0 && liveNow().slot(it).pending != null }
        if (waiting.isEmpty()) return
        val m = waiting.fold(0) { acc, k -> acc or (1 shl k) }
        if (NativeEngine.isAvailable) NativeEngine.cancelLaunch(m)
        waiting.forEach { stagedFrom[it] = null }
        _liveGrid.value = liveNow().cancelled(m)
        // What waited before the press waits again.
        waiting.groupBy { before.slot(it).pending }.forEach { (scene, slots) ->
            if (scene != null) armSlots(slots.fold(0) { acc, k -> acc or (1 shl k) }, scene)
        }
    }

    /** ■ under a column: that track stops on the next launch line. */
    fun stopLiveTrack(key: String) {
        val slot = LiveSlots.of(key) ?: return
        if (!_transport.value.playing) return
        leaveSongForLive()
        armSlots(LiveSlots.mask(slot), -1)
    }

    /** « ■ Tout »: every track stops on the next launch line (the transport goes on). */
    fun stopLiveAll() {
        if (!_transport.value.playing) return
        leaveSongForLive()
        armSlots(LiveSlots.ALL, -1)
    }

    /** M: track [key] silent at once, or heard again (not written in the project). */
    fun toggleLiveMute(key: String) {
        val ch = liveChannel(key) ?: return
        val muted = _liveMuted.value.let { if (key in it) it - key else it + key }
        _liveMuted.value = muted
        if (NativeEngine.isAvailable) NativeEngine.setChannelMute(ch, key in muted || project.value.mixer.channels.getOrNull(ch)?.mute == true)
    }

    /** The mixer channel of a grid column. */
    private fun liveChannel(key: String): Int? = when (val slot = LiveSlots.of(key)) {
        null -> null
        LiveSlots.DRUMS -> 0
        LiveSlots.ACID -> 1
        LiveSlots.REMIX -> 3
        in LiveSlots.AUDIO until LiveSlots.AUDIO + AudioTrack.MAX -> AudioTrack.mixerChannel(slot - LiveSlots.AUDIO)
        else -> InstrumentTrack.mixerChannel(slot - LiveSlots.INST)
    }

    private fun liveMutedChannels(): Set<Int> = _liveMuted.value.mapNotNull(::liveChannel).toSet()

    /** A 303 setting moved on the Live page: heard at once, not written (Stop or a double tap brings back the saved one). */
    fun setLiveAcid(param: Int, value: Float) {
        if (param !in AutoTarget.ACID_PARAMS) return
        val v = value.coerceIn(0f, 1f)
        takeGesture(AutoTarget.acid(param), v, _liveAcid.value[param] ?: project.value.acid.sound.values()[param])
        _liveAcid.value = _liveAcid.value + (param to v)
        if (NativeEngine.isAvailable) NativeEngine.setAcidParam(param, v)
    }

    /** Double tap: the 303 setting [param] back to the one saved in the project. */
    fun resetLiveAcid(param: Int) {
        if (param !in _liveAcid.value) return
        takeGesture(AutoTarget.acid(param), project.value.acid.sound.values()[param], _liveAcid.value[param] ?: 0f)
        _liveAcid.value = _liveAcid.value - param
        if (NativeEngine.isAvailable) NativeEngine.setAcidParam(param, project.value.acid.sound.values()[param])
    }

    /** The reverb (0) or echo (1) return on the Live page: 0..1, 0.5 = as mixed. */
    fun setLiveReturn(bus: Int, value: Float) {
        if (bus !in 0..1) return
        val v = value.coerceIn(0f, 1f)
        settleDropIfPassed()
        // Back to as mixed: at once (a double touch, leaving the page), written as a jump.
        takeGesture(AutoTarget.live(AutoTarget.LIVE_REVERB_RETURN + bus), v, _liveReturns.value[bus], jump = v == 0.5f)
        _liveReturns.value = _liveReturns.value.toMutableList().also { it[bus] = v }
        engineBusReturn(bus, v * 2f)
    }

    /**
     * Tests only: the Live page's commands to the engine, in words (« perf 31 1.0 », « hold … »,
     * « macro … », « return … »), kept when set (unit tests run without the engine).
     */
    @androidx.annotation.VisibleForTesting
    internal var liveCommandsForTest: MutableList<String>? = null

    /** The Live page's effects talk to an engine (or a test listens). */
    private val liveEngine: Boolean get() = NativeEngine.isAvailable || liveCommandsForTest != null

    private fun enginePerformance(param: Int, v: Float) {
        liveCommandsForTest?.add("perf $param $v")
        if (NativeEngine.isAvailable) NativeEngine.setPerformance(param, v)
    }

    private fun engineHold(packed: Int, on: Boolean) {
        liveCommandsForTest?.add("hold $packed $on")
        if (NativeEngine.isAvailable) NativeEngine.setAutomationHold(packed, on)
    }

    private fun engineMacro(ch: Int, macro: Int, v: Float) {
        liveCommandsForTest?.add("macro $ch $macro $v")
        if (NativeEngine.isAvailable) NativeEngine.setMacro(ch, macro, v)
    }

    private fun engineBusReturn(bus: Int, gain: Float) {
        liveCommandsForTest?.add("return $bus $gain")
        if (NativeEngine.isAvailable) NativeEngine.setBusReturn(bus, gain)
    }

    /** Where the Live page's whole-mix effects are, by [AutoTarget.LIVE] number (the returns apart: [liveReturns]). */
    private val livePerf = FloatArray(AutoTarget.LIVE_RESET_ALL + 1) { AutoTarget.liveRest(it) }

    /** Song curves of the Live page's effects a finger holds (touch take-over), packed targets. */
    private val liveHeld = HashSet<Int>()

    private val _liveFx = MutableStateFlow(LiveFxUi())
    /**
     * What the Live page's effects are doing (until Stop, not saved): each whole-mix effect away
     * from its rest (held effects at 1, the build-up knob, the DJ filter), the locked ones, the
     * tracks' kills, a Drop armed for the next bar. Their settings are the project's
     * ([com.pocketstudio.model.Project.liveFx], [setLiveFxSettings]).
     */
    val liveFx: StateFlow<LiveFxUi> = _liveFx.asStateFlow()

    private fun publishLiveFx(dropArmed: Boolean = _liveFx.value.dropArmed) {
        val values = AutoTarget.LIVE_GESTURES.filter { livePerf[it] != AutoTarget.liveRest(it) }.associateWith { livePerf[it] }
        _liveFx.value = _liveFx.value.copy(values = values, dropArmed = dropArmed)
    }

    /**
     * Where the engine starts a gesture of the whole-mix effects (sixteenths since play): the next
     * line of [gridSteps] after what it has rendered, as it waits for it (a take writes the gesture
     * there, so the song plays it where it was heard); with no grid, where it was heard.
     */
    private fun gesturePosition(gridSteps: Int): Double {
        if (gridSteps <= 0 || !_transport.value.playing) return takePosition()
        val rendered = if (NativeEngine.isAvailable) NativeEngine.transportPosition() else takeClockForTest.toDouble()
        return com.pocketstudio.model.nextGridLine(rendered, gridSteps)
    }

    /** The grid of the held effects (the project's « Départ des effets »), in sixteenths. */
    private val liveGridSteps: Int get() = project.value.liveFx.gridSteps

    /**
     * The whole-mix effects of the Live page ([AutoTarget.LIVE_GESTURES]: DJ filter, Répéter,
     * Freinage, Hachoir, Rembobinage, À l'envers, Demi-tempo, Sortie en écho, the kills, Réverbe
     * figée, the throws, the risers, the Montée): heard at once (a held effect from the next line of
     * the grid), written into the song during a take. Away from its rest, a song curve leaves it
     * alone. A locked effect ([setLiveLock]) ignores being let go (its rest value).
     */
    fun livePerformance(param: Int, value: Float) {
        if (param !in AutoTarget.LIVE_GESTURES) return
        val v = value.coerceIn(0f, 1f)
        val rest = AutoTarget.liveRest(param)
        if (v == rest && param in _liveFx.value.locks) return
        settleDropIfPassed()
        // Already at rest (a press refused while a Drop waited, let go): nothing to do.
        if (v == rest && livePerf[param] == rest) return
        // On/off effects start on the grid's next line (the kills and the freeze stop there too, the
        // brake restarts there): written there, so the song plays them where they were heard.
        val quantised = param in AutoTarget.LIVE_ON_OFF && param != AutoTarget.LIVE_DIVISION &&
            (v != rest || param in AutoTarget.LIVE_KILL_LOW..AutoTarget.LIVE_KILL_HIGH || param == AutoTarget.LIVE_FREEZE ||
                (param == AutoTarget.LIVE_TAPE_STOP && project.value.liveFx.restart > 0))
        val at = when (param) {
            AutoTarget.LIVE_RISER, AutoTarget.LIVE_DOWNLIFTER, AutoTarget.LIVE_DROP -> gesturePosition(project.value.stepsPerBar)
            AutoTarget.LIVE_THROW_REVERB, AutoTarget.LIVE_THROW_ECHO -> gesturePosition(if (liveGridSteps > 0) 4 else 0)
            AutoTarget.LIVE_RESET_ALL -> null
            else -> if (quantised) gesturePosition(liveGridSteps) else null
        }
        // A Drop waits for its bar line: a press that would start in the last half beat before it
        // (written on that bar's beat, where the Drop clears it) is not played, nor written: the
        // engine and the song agree. The next press after the bar line plays.
        if (v != rest && cutByDrop(at ?: takePositionNow())) return
        // Let go before the grid line its press waits for: the engine never plays it, the take
        // forgets it (else the song would play a beat the performer never heard).
        if (!pressedThenLetGoEarly(AutoTarget.live(param), v != rest, at, param in AutoTarget.LIVE_ON_OFF && !AutoTarget.isTrigger(param))) {
            // Back to rest: at once (a let-go, unlocked), written as a jump rather than a slide.
            takeGesture(AutoTarget.live(param), v, livePerf[param], at, jump = v == rest)
        }
        livePerf[param] = v
        publishLiveFx()
        if (!liveEngine) return
        val target = AutoTarget.live(param).pack()
        val away = v != rest
        if (away && liveHeld.add(target)) engineHold(target, true)
        enginePerformance(param, v)
        if (!away && liveHeld.remove(target)) releaseHold(target)
    }

    /** A held button of the whole mix ([AutoTarget.LIVE_ON_OFF]: Freinage, Hachoir, Rembobinage…): on while held. */
    fun liveHold(param: Int, on: Boolean) {
        if (param !in AutoTarget.LIVE_ON_OFF || param == AutoTarget.LIVE_DIVISION || AutoTarget.isTrigger(param)) return
        if (param == AutoTarget.LIVE_REPEAT) {
            if (on) liveRoll(project.value.liveFx.repeat) else liveRoll(null)
            return
        }
        livePerformance(param, if (on) 1f else 0f)
    }

    /**
     * The roll pads: the whole mix repeats [division] (a slice from its own line of the grid) while
     * held; another pad slid to takes over at the end of the slice playing; null: let go (the
     * division back at rest too, else its song curve would stay « held »).
     */
    fun liveRoll(division: LiveDivision?) {
        if (division == null) {
            livePerformance(AutoTarget.LIVE_REPEAT, 0f)
            if (AutoTarget.LIVE_REPEAT !in _liveFx.value.locks) livePerformance(AutoTarget.LIVE_DIVISION, AutoTarget.liveRest(AutoTarget.LIVE_DIVISION))
            return
        }
        livePerformance(AutoTarget.LIVE_DIVISION, division.engineValue)
        livePerformance(AutoTarget.LIVE_REPEAT, 1f)
    }

    /** The division the roll plays (null: none held). */
    fun liveRollDivision(): LiveDivision? =
        if (livePerf[AutoTarget.LIVE_REPEAT] > 0.5f) LiveDivision.ofValue(livePerf[AutoTarget.LIVE_DIVISION]) else null

    /**
     * A kill: [band] (0 bass, 1 mids, 2 highs) of the whole mix ([ch] = [MASTER]) or of track strip
     * [ch] cut while [on], switching on the grid's lines both ways.
     */
    fun liveKill(band: Int, on: Boolean, ch: Int = MASTER) {
        if (band !in 0..2) return
        if (ch == MASTER) {
            livePerformance(AutoTarget.liveKill(band), if (on) 1f else 0f)
            return
        }
        if (ch !in 0 until Mixer.FIRST_PAD) return
        settleDropIfPassed()
        val kills = _liveFx.value.trackKills
        val now = kills[ch].orEmpty()
        if ((band in now) == on) return
        val target = AutoTarget.macro(ch, AutoTarget.MACRO_KILL_LOW + band)
        val at = gesturePosition(liveGridSteps)
        // Starting where a waiting Drop clears it: not played (see livePerformance).
        if (on && cutByDrop(at)) return
        if (!pressedThenLetGoEarly(target, on, at, true)) takeGesture(target, if (on) 1f else 0f, if (on) 0f else 1f, at)
        val next = if (on) now + band else now - band
        _liveFx.value = _liveFx.value.copy(trackKills = if (next.isEmpty()) kills - ch else kills + (ch to next))
        if (!liveEngine) return
        val packed = target.pack()
        if (on && liveHeld.add(packed)) engineHold(packed, true)
        engineMacro(ch, AutoTarget.MACRO_KILL_LOW + band, if (on) 1f else 0f)
        if (!on && liveHeld.remove(packed)) releaseHold(packed)
    }

    /**
     * A dub « throw »: the whole mix ([ch] = [MASTER]) or track [ch] sent into the reverb ([bus] 0)
     * or the echo (1) for one beat, from the next beat line (written there during a take).
     */
    fun liveThrow(bus: Int, ch: Int = MASTER) {
        if (bus !in 0..1) return
        if (ch == MASTER) {
            livePerformance(AutoTarget.LIVE_THROW_REVERB + bus, 1f)
            livePerformance(AutoTarget.LIVE_THROW_REVERB + bus, 0f)
            return
        }
        if (ch !in 0 until Mixer.FIRST_PAD) return
        val target = AutoTarget.macro(ch, AutoTarget.MACRO_THROW_REVERB + bus)
        val at = gesturePosition(if (liveGridSteps > 0) 4 else 0)
        // Pressed and let go at once: the take writes one beat (an on/off lasts at least a beat).
        if (cutByDrop(at)) return  // the waiting Drop would clear it on its line
        takeGesture(target, 1f, 0f, at)
        takeGesture(target, 0f, 1f, at)
        if (!liveEngine) return
        engineMacro(ch, AutoTarget.MACRO_THROW_REVERB + bus, 1f)
        engineMacro(ch, AutoTarget.MACRO_THROW_REVERB + bus, 0f)
    }

    /** A noise riser ([down]: a downlifter) over the project's [com.pocketstudio.model.LiveFxSettings.riserBars], from the next bar line. */
    fun liveRiser(down: Boolean = false) {
        val param = if (down) AutoTarget.LIVE_DOWNLIFTER else AutoTarget.LIVE_RISER
        livePerformance(param, 1f)
        livePerformance(param, 0f)
    }

    /**
     * The one-knob « Montée » (0 rest .. 1): a high-pass rising over the whole mix, more reverb and
     * echo, noise rising under it and, past the middle, rolls getting faster (1/4 to 1/32). Written
     * as one song curve during a take.
     */
    fun setLiveBuildUp(value: Float) = livePerformance(AutoTarget.LIVE_BUILD_UP, value)

    /**
     * « Drop »: on the next bar line, every whole-mix effect back at rest at once (the Montée, held
     * effects, kills, freeze, throws, returns, the tracks' kills); locks are let go. A take writes
     * each of them at rest on that bar.
     */
    fun liveDrop() {
        settleDropIfPassed()
        if (dropAt != null) return  // already armed: it drops on that bar line
        val bar = project.value.stepsPerBar
        val at = gesturePosition(bar)
        val playing = _transport.value.playing
        // Playing, the engine keeps what it holds until the bar line (a song curve does not take
        // over before the drop); stopped, everything is at rest at once.
        liveBackToRest(at, releaseHolds = !playing)
        // One press to the engine (written into a take too: the song drops on the same bar line).
        livePerformance(AutoTarget.LIVE_DROP, 1f)
        livePerformance(AutoTarget.LIVE_DROP, 0f)
        publishLiveFx(dropArmed = playing)
        if (playing) {
            dropAt = at
            // The armed Drop lights until its bar line has passed; then what was pressed meanwhile
            // shows as the engine cut it.
            dropWatch?.cancel()
            dropWatch = viewModelScope.launch {
                while (_transport.value.playing && takePositionNow() < at) kotlinx.coroutines.delay(50)
                settleDrop()
            }
        }
    }

    private var dropWatch: kotlinx.coroutines.Job? = null

    /** On/off effects pressed and waiting for their grid line (packed target → that line, sixteenths). */
    private val livePressLine = HashMap<Int, Double>()

    /**
     * An on/off [target] (an effect held, a kill: [held]) [pressed] for grid line [at], or let go.
     * True when this let-go comes before the line its press waits for: the engine drops both, and
     * so does the take ([com.pocketstudio.model.PerformanceTake.forget]); nothing to write.
     */
    private fun pressedThenLetGoEarly(target: AutoTarget, pressed: Boolean, at: Double?, held: Boolean): Boolean {
        val packed = target.pack()
        if (!held) return false
        if (pressed) {
            if (at != null) livePressLine[packed] = at else livePressLine.remove(packed)
            return false
        }
        val line = livePressLine.remove(packed) ?: return false
        if (!_transport.value.playing || takePositionNow() >= line) return false
        performance?.forget(target, line)
        return true
    }

    /** The bar line a Drop waits for (sixteenths since play), null: none waiting. */
    private var dropAt: Double? = null

    /**
     * A press starting at [at] (sixteenths) is cleared by the Drop waiting: it would be written on
     * the Drop's beat or later (the nearest beat), where everything is at rest.
     */
    private fun cutByDrop(at: Double): Boolean = dropAt?.let { at >= it - 2.0 } == true

    /** A gesture comes after the Drop's bar line, before the watch saw it pass: settled first. */
    private fun settleDropIfPassed() {
        val at = dropAt ?: return
        if (takePositionNow() >= at) settleDrop()
    }

    /**
     * The Drop's bar line has passed: the engine cleared everything there, what was pressed
     * between the Drop and that line too. The page shows it so, a take writes it at rest on that
     * line (as heard), and the engine's holds are let go (song curves drive them again).
     */
    private fun settleDrop() {
        val at = dropAt ?: return
        dropAt = null
        dropWatch?.cancel()
        dropWatch = null
        liveBackToRest(at, releaseHolds = true)
        publishLiveFx(dropArmed = false)
    }

    private fun takePositionNow(): Double = if (NativeEngine.isAvailable) NativeEngine.transportPosition() else takeClockForTest.toDouble()

    /**
     * « Tout remettre »: every live effect back at rest now (one command to the engine: the whole
     * mix's, the returns, the tracks' kills and choppers, the 303 back to its saved settings),
     * locks let go; written into a take as it is heard.
     */
    fun liveResetAll() {
        settleDropIfPassed()
        // A Drop still waiting is cancelled (the engine's reset drops it too): nothing more happens
        // on its bar line, and a take has everything at rest from now rather than from there.
        dropAt?.let { at ->
            dropAt = null
            dropWatch?.cancel()
            dropWatch = null
            performance?.dropCancelled(at, takePosition())
        }
        livePressLine.clear()
        liveBackToRest(null, releaseHolds = true)
        livePerformance(AutoTarget.LIVE_RESET_ALL, 1f)
        livePerformance(AutoTarget.LIVE_RESET_ALL, 0f)
        _liveChops.value.toList().forEach { liveTrackChop(it, false) }
        _liveAcid.value.keys.toList().forEach { resetLiveAcid(it) }
        publishLiveFx(dropArmed = false)
    }

    /**
     * The whole mix's effects, returns and tracks' kills at rest in the ViewModel (the engine resets
     * its own with one command), each written into a take at [at] (null: as heard now); holds let go.
     */
    private fun liveBackToRest(at: Double?, releaseHolds: Boolean) {
        val position = at ?: takePosition()
        // Each at rest at once: written as a jump (the Montée stays up to the Drop's bar line).
        for (param in AutoTarget.LIVE_GESTURES) {
            val rest = AutoTarget.liveRest(param)
            if (livePerf[param] == rest) continue
            takeGesture(AutoTarget.live(param), rest, livePerf[param], position, jump = true)
            livePerf[param] = rest
        }
        _liveReturns.value.forEachIndexed { bus, v ->
            if (v != 0.5f) takeGesture(AutoTarget.live(AutoTarget.LIVE_REVERB_RETURN + bus), 0.5f, v, position, jump = true)
        }
        _liveReturns.value = listOf(0.5f, 0.5f)
        _liveFx.value.trackKills.forEach { (ch, bands) ->
            bands.forEach { b -> takeGesture(AutoTarget.macro(ch, AutoTarget.MACRO_KILL_LOW + b), 0f, 1f, position) }
        }
        _liveFx.value = _liveFx.value.copy(locks = emptySet(), trackKills = emptyMap())
        if (releaseHolds && liveEngine) {
            val held = liveHeld.filter { (it shr 16) == AutoTarget.LIVE || ((it shr 16) == AutoTarget.MACRO && (it and 0xFF) in AutoTarget.MACRO_KILL_LOW..AutoTarget.MACRO_KILL_HIGH) }
            liveHeld.removeAll(held.toSet())
            held.forEach(::releaseHold)
        }
        publishLiveFx()
    }

    /**
     * Leaving the Live page: nothing it set stays on unseen. The whole-mix effects (the Montée, the
     * DJ filter, what the lock kept), the returns, the tracks' kills and choppers go back to rest
     * (written into a take as heard, at once); a Drop already waiting still drops on its bar line.
     */
    fun leaveLive() {
        _liveFx.value.locks.toList().forEach { setLiveLock(it, false) }
        for (param in AutoTarget.LIVE_GESTURES) {
            if (AutoTarget.isTrigger(param)) continue
            val rest = AutoTarget.liveRest(param)
            if (livePerf[param] != rest) livePerformance(param, rest)
        }
        for (bus in 0..1) if (_liveReturns.value[bus] != 0.5f) setLiveReturn(bus, 0.5f)
        _liveFx.value.trackKills.forEach { (ch, bands) -> bands.forEach { liveKill(it, false, ch) } }
        _liveChops.value.toList().forEach { liveTrackChop(it, false) }
    }

    /**
     * « Verrou »: a locked whole-mix effect stays as it is when let go (the DJ filter where the
     * finger left it, a held effect on) until unlocked; unlocked, it goes back to its rest.
     */
    fun setLiveLock(param: Int, locked: Boolean) {
        if (param !in AutoTarget.LIVE_GESTURES || AutoTarget.isTrigger(param)) return
        settleDropIfPassed()
        val locks = _liveFx.value.locks
        if ((param in locks) == locked) return
        _liveFx.value = _liveFx.value.copy(locks = if (locked) locks + param else locks - param)
        if (!locked) {
            when (param) {
                AutoTarget.LIVE_REPEAT -> liveRoll(null)
                AutoTarget.LIVE_FILTER, AutoTarget.LIVE_RESONANCE -> {
                    livePerformance(AutoTarget.LIVE_FILTER, AutoTarget.liveRest(AutoTarget.LIVE_FILTER))
                    livePerformance(AutoTarget.LIVE_RESONANCE, AutoTarget.liveRest(AutoTarget.LIVE_RESONANCE))
                }
                else -> livePerformance(param, AutoTarget.liveRest(param))
            }
        }
    }

    /** The Live page's effect settings (grid, release effect, lengths), kept with the project (no undo step). */
    fun setLiveFxSettings(settings: com.pocketstudio.model.LiveFxSettings) {
        val s = settings.sanitized()
        if (s == project.value.liveFx) return
        session.editQuietly { it.copy(liveFx = s) }
        pushLiveFxSettings(project.value)
    }

    /** The settings to the engine (an export's too: it is part of the project's description). */
    private fun pushLiveFxSettings(p: Project) {
        if (!NativeEngine.isAvailable) return
        p.liveFx.engineParams().forEach { (param, v) -> NativeEngine.setPerformance(param, v) }
    }

    private val _liveChops = MutableStateFlow<Set<Int>>(emptySet())
    /** Mixer channels whose « Hachoir de voie » is held on the Live page (until let go or Stop, not saved). */
    val liveChops: StateFlow<Set<Int>> = _liveChops.asStateFlow()

    /** « Hachoir de voie »: track strip [ch] chopped in sixteenths while held (written during a take). */
    fun liveTrackChop(ch: Int, on: Boolean) {
        if (ch !in 0 until Mixer.FIRST_PAD || ch == MASTER || (ch in _liveChops.value) == on) return
        takeGesture(AutoTarget.macro(ch, AutoTarget.MACRO_CHOP), if (on) 1f else 0f, if (on) 0f else 1f)
        _liveChops.value = if (on) _liveChops.value + ch else _liveChops.value - ch
        if (!liveEngine) return
        val target = AutoTarget.macro(ch, AutoTarget.MACRO_CHOP).pack()
        if (on && liveHeld.add(target)) engineHold(target, true)
        engineMacro(ch, AutoTarget.MACRO_CHOP, if (on) 1f else 0f)
        if (!on && liveHeld.remove(target)) releaseHold(target)
    }

    /** Settings a take moved (packed targets): held in the engine to the take's end, as written. */
    private val takeHeld = HashSet<Int>()
    internal val takeHeldForTest: Set<Int> get() = takeHeld

    /** Let go of [packed] in the engine unless something else still holds it (a finger, the Live page, a take). */
    private fun releaseHold(packed: Int) {
        if (!liveEngine || packed in liveHeld || packed in takeHeld || heldSettings.any { it.pack() == packed }) return
        engineHold(packed, false)
    }

    /** The take ended: what it held goes back to its lanes. */
    private fun releaseTakeHolds() {
        val held = takeHeld.toList()
        takeHeld.clear()
        held.forEach(::releaseHold)
    }

    /** Stop: the grid, the mutes and the gestures of the Live page end; the project's sound comes back. */
    private fun endLiveGestures() {
        endPerformanceOnStop()
        _liveGrid.value = null
        _liveClock.value = -1
        stagedFrom.fill(null)
        _liveMuted.value = emptySet()
        _liveAcid.value = emptyMap()
        // The whole-mix effects and the track choppers at rest (also where song curves left them).
        val curves = project.value.automation.filter { it.hasPoints }.map { it.target.kind }.toSet()
        if (liveEngine) {
            val held = liveHeld.toList()
            liveHeld.clear()
            held.forEach(::releaseHold)
            // Always (it is cheap): one command, every live effect at rest, the kills, freeze and
            // throws of the buses too, and a Drop still waiting (the engine also settles it on Stop).
            enginePerformance(com.pocketstudio.model.LiveFxSettings.ENGINE_RESET_ALL, 1f)
            enginePerformance(com.pocketstudio.model.LiveFxSettings.ENGINE_RESET_ALL, 0f)
            AutoTarget.LIVE_GESTURES.forEach { enginePerformance(it, AutoTarget.liveRest(it)) }
            for (bus in 0..1) engineBusReturn(bus, 1f)
            if (AutoTarget.MACRO in curves || _liveChops.value.isNotEmpty()) {
                for (ch in 0 until Mixer.FIRST_PAD) if (ch != MASTER) engineMacro(ch, AutoTarget.MACRO_CHOP, 0f)
            }
        }
        liveHeld.clear()
        livePerf.indices.forEach { livePerf[it] = AutoTarget.liveRest(it) }
        livePressLine.clear()
        dropWatch?.cancel()
        dropAt = null
        _liveFx.value = LiveFxUi()
        _liveChops.value = emptySet()
        if (_liveReturns.value.any { it != 0.5f }) _liveReturns.value = listOf(0.5f, 0.5f)
    }

    /** Stopped: the transport starts on [scene] (shown), the tracks outside [mask] silent from the first step. */
    private fun startLive(scene: Int, mask: Int) {
        if (scene != project.value.scene) session.editQuietly { it.switchScene(scene) }
        _sessionUi.value = SessionUi()
        syncEngine()
        heardScene = project.value.scene
        val others = LiveSlots.ALL and mask.inv()
        // Armed before ▶: they take effect on the very first step.
        if (others != 0 && NativeEngine.isAvailable) NativeEngine.armLaunch(others, project.value.liveQuantumSteps, -1)
        toggleTransport(songStep = aloneSongStep())
        if (others != 0) _liveGrid.value = liveNow().armed(others, -1, project.value.liveQuantumSteps)
        watchLaunches()
    }

    /** During the song (or a scene waiting to take over): the grid takes over, what was armed is dropped. */
    private fun leaveSongForLive() {
        var ui = _sessionUi.value
        if (ui.songIndex < 0 && ui.pending == null && !ui.ending) return
        // A swap the 40 ms watch has not seen yet: the part it brought in is what plays.
        if (NativeEngine.isAvailable && (ui.pending != null || ui.ending) && NativeEngine.sceneSwaps() != swapsSeen) {
            swapsSeen = NativeEngine.sceneSwaps()
            onSceneSwapped()
            if (!_transport.value.playing) return  // the song just ended
            ui = _sessionUi.value
        }
        if (NativeEngine.isAvailable) NativeEngine.leaveSong()
        _sessionUi.value = SessionUi()
        // The part playing as the engine plays it (exact starts): what the grid and a take go on from.
        val part = project.value.song.getOrNull(ui.songIndex)
        val g = if (part != null && NativeEngine.isAvailable) gridOfPart(part, part.scene, liveTransportStep(), slotStatesNow()) else liveNow()
        _liveGrid.value = g.cancelled(LiveSlots.ALL)
        AppLog.add("Live : le morceau ne suit plus ses parties")
    }

    /** [mask]'s slots take [scene]'s part (staged now) or stop (-1) on the next launch line. */
    private fun armSlots(mask: Int, scene: Int) {
        val p = project.value
        if (NativeEngine.isAvailable) {
            if (scene >= 0) {
                NativeEngine.setStaging(true)
                if (mask == LiveSlots.ALL) pushPart(p, scene, null)
                else for (k in 0 until LiveSlots.COUNT) if (mask and (1 shl k) != 0) pushSlot(p, k, scene)
                NativeEngine.setStaging(false)
            }
            NativeEngine.armLaunch(mask, p.liveQuantumSteps, scene)
        }
        for (k in 0 until LiveSlots.COUNT) if (mask and (1 shl k) != 0) stagedFrom[k] = p.takeIf { scene >= 0 }
        _liveGrid.value = liveNow().armed(mask, scene, p.liveQuantumSteps)
        watchLaunches()
    }

    /**
     * Slot [slot]'s part of [scene] to the engine (the waiting slot while staging, else the playing
     * one): its notes or clip, its per-scene curves and effects that play in some scenes only.
     */
    private fun pushSlot(p: Project, slot: Int, scene: Int) {
        if (!NativeEngine.isAvailable) return
        pushingPart = true
        try {
            val view = p.sceneView(scene)
            NativeEngine.setSlotOffset(slot, 0)  // a pad starts its part from the start
            when (slot) {
                LiveSlots.DRUMS -> pushDrumPattern(view.drums)
                LiveSlots.ACID -> {
                    NativeEngine.setAcidLength(view.acid.length)
                    view.acid.steps.forEachIndexed { i, st -> NativeEngine.setAcidStep(i, st.pack()) }
                }
                LiveSlots.REMIX -> Unit
                in LiveSlots.AUDIO until LiveSlots.AUDIO + AudioTrack.MAX -> {
                    val id = slot - LiveSlots.AUDIO
                    NativeEngine.setAudioOn(id, audioPlays(p, id, null, scene))
                    p.audioTracks.firstOrNull { it.id == id }?.clip?.let { NativeEngine.setAudioLoop(id, it.engineLoopSteps(p.sceneSteps(scene), p.tempoBpm)) }
                }
                else -> view.instrumentTracks.firstOrNull { it.id == slot - LiveSlots.INST }?.let { pushNotes(it.notes, it.id) }
            }
            p.automation.forEachIndexed { i, lane ->
                val owner = if (lane.target.kind == AutoTarget.ACID) LiveSlots.ACID else liveSlotOfChannel(lane.target.ch)
                if (lane.mode == AutomationLane.PER_SCENE && owner == slot && lane.target.kind != AutoTarget.BUS_PARAM) {
                    lane.valuesFor(scene).forEachIndexed { step, v -> NativeEngine.setAutomationPoint(i, step, v) }
                }
            }
            // « Joue dans » of every strip of the track (the drums: their pads with their own effects too).
            for (ch in slotStrips(p, slot)) insertOnCommands(p.insertsOf(ch), scene) { s, on -> NativeEngine.setInsertOn(ch, s, on) }
        } finally {
            pushingPart = false
        }
    }

    /** The mixer strips in use whose sound is grid slot [slot]'s track (the drums: channel 0 and the pads' own strips). */
    internal fun slotStrips(p: Project, slot: Int): List<Int> = usedStrips(p).filter { liveSlotOfChannel(it) == slot }.sorted()

    /**
     * The 303 launched on [scene] (a pad or a row): its knobs that no lane drives there go back to
     * the project's settings, as the song's part does ([pushAcid] with the knobs driven kept); those
     * moved on the Live page stay where the fingers left them. Without it a knob stays where the
     * last scene's curve left it.
     */
    private fun acidSettingsOnLaunch(p: Project, scene: Int) {
        if (!liveEngine || NativeEngine.isCapturing) return
        val keep = p.acidKnobsDriven(scene, inSong = false)
        val moved = _liveAcid.value
        p.acid.sound.values().forEachIndexed { i, v ->
            if (i in keep || i in moved) return@forEachIndexed
            liveCommandsForTest?.add("acid $i $v")
            if (NativeEngine.isAvailable) NativeEngine.setAcidParam(i, v)
        }
    }

    private fun liveSlotOfChannel(ch: Int): Int? = when {
        ch == 0 || ch >= Mixer.FIRST_PAD -> LiveSlots.DRUMS
        ch == 1 -> LiveSlots.ACID
        ch == 2 -> LiveSlots.INST
        ch == 3 -> LiveSlots.REMIX
        ch in 5..11 -> LiveSlots.INST + ch - 4
        ch in 12..15 -> LiveSlots.AUDIO + ch - 12
        else -> null
    }

    /**
     * The engine launched [slot] on [scene] (-1: stopped) at transport [step]. A part staged before
     * its scene was edited gets the edits now; every column on one scene again makes it the scene
     * heard, and the screens show it.
     */
    private fun onLaunched(slot: Int, scene: Int, step: Int) {
        if (!_transport.value.playing || slot !in 0 until LiveSlots.COUNT) return
        val was = liveNow()
        val g = was.launched(slot, scene, step, project.value.liveColumnsMask)
        _liveGrid.value = g
        if (_take.value.running) performance?.launched(slot, scene, step)
        val p = project.value
        val from = stagedFrom[slot]
        stagedFrom[slot] = null
        if (scene >= 0 && scene == p.scene && from != null && from !== p) pushSlot(p, slot, scene)
        if (slot == LiveSlots.ACID && scene >= 0) acidSettingsOnLaunch(p, scene)
        // Every track brought to another scene pad by pad: the master and the buses follow that
        // row too (a whole row launched brought its own with it), as the song's part plays them.
        if (g.row != was.row && was.pendingRow != g.row) pushRowMaster(p, g.row)
        if (!g.diverged && !g.hasPending) {
            heardScene = g.row
            if (g.row != project.value.scene && g.row in project.value.allScenes.indices) session.editQuietly { it.switchScene(g.row) }
            if (NativeEngine.isAvailable) swapsSeen = NativeEngine.sceneSwaps()
        }
    }

    internal fun liveLaunchedForTest(slot: Int, scene: Int, step: Int) = onLaunched(slot, scene, step)

    /** The master's and buses' part of scene [scene] (« Joue dans » of the master, their per-scene curves) to the engine. */
    private fun pushRowMaster(p: Project, scene: Int) {
        liveCommandsForTest?.add("master $scene")
        if (!NativeEngine.isAvailable) return
        insertOnCommands(p.insertsOf(MASTER), scene) { s, on -> NativeEngine.setInsertOn(MASTER, s, on) }
        p.automation.forEachIndexed { i, lane ->
            if (lane.mode == AutomationLane.PER_SCENE && laneSlot(lane.target) == null) {
                lane.valuesFor(scene).forEachIndexed { step, v -> NativeEngine.setAutomationPoint(i, step, v) }
            }
        }
    }

    private val launchBuf = IntArray(LiveSlots.COUNT * 3 * 4)

    /** Reads the launches the engine made since the last look ([apply] false: forgets them). */
    private fun drainLaunches(apply: Boolean = true) {
        if (!NativeEngine.isAvailable) return
        var total = 0
        while (true) {
            val n = NativeEngine.drainLaunchEvents(launchBuf)
            if (apply) for (i in 0 until n) onLaunched(launchBuf[i * 3], launchBuf[i * 3 + 1], launchBuf[i * 3 + 2])
            total += n
            if (n < launchBuf.size / 3) break
        }
        // Its queue full (the app asleep), some were lost: the engine's own state says where each slot is.
        if (apply && total >= 256) resyncLiveFromEngine()
    }

    private fun resyncLiveFromEngine() {
        val buf = IntArray(NativeEngine.SLOTS * 3)
        NativeEngine.slotStates(buf)
        val g = liveNow()
        _liveGrid.value = g.copy(slots = List(LiveSlots.COUNT) { k ->
            val tag = buf[k * 3]
            val pending = buf[k * 3 + 1]
            com.pocketstudio.model.LiveSlot(
                playing = if (tag == NativeEngine.ROW_TAG) g.row else tag,
                pending = pending.takeIf { it != NativeEngine.NO_LAUNCH },
                start = buf[k * 3 + 2],
            )
        })
    }

    private var launchWatch: kotlinx.coroutines.Job? = null

    /** Follows the engine's launches while some wait (also with the screen off). */
    private fun watchLaunches() {
        if (!NativeEngine.isAvailable || launchWatch?.isActive == true) return
        launchWatch = viewModelScope.launch {
            while (_transport.value.playing && _liveGrid.value?.hasPending == true) {
                drainLaunches()
                kotlinx.coroutines.delay(30)
            }
        }
    }

    /** Polls the grid's clock and launches about 30 times a second while the Live page is shown. */
    suspend fun pollLiveGrid() {
        if (!NativeEngine.isAvailable) return
        while (true) {
            val step = if (_transport.value.playing) NativeEngine.transportStep() else -1
            if (step != _liveClock.value) _liveClock.value = step
            if (_transport.value.playing) drainLaunches()
            kotlinx.coroutines.delay(33)
        }
    }

    // --- « Enregistrer la performance »: the Live page's play written into the song ------------

    private val _take = MutableStateFlow<com.pocketstudio.app.live.TakeUi>(com.pocketstudio.app.live.TakeUi.Ready)
    /** The ● of the Live page: ready, armed, recording, writing, done. */
    val take: StateFlow<com.pocketstudio.app.live.TakeUi> = _take.asStateFlow()

    private val _takeAppend = MutableStateFlow(false)
    /** The take replaces the song (false, the default) or comes after it (« Ajouter à la suite »). */
    val takeAppend: StateFlow<Boolean> = _takeAppend.asStateFlow()

    fun setTakeAppend(append: Boolean) {
        val p = project.value
        // Armed with a full song: « Ajouter à la suite » could write nothing; it stays « Remplacer ».
        if (append && _take.value is com.pocketstudio.app.live.TakeUi.Armed && performance != null && songFullForTake(p)) {
            _notice.value = tr("Le morceau est plein : choisis « Remplacer le morceau » pour enregistrer.")
            return
        }
        _takeAppend.value = append
        // Armed: the take's song position moves with the choice (the song's end, or its start).
        if (_take.value !is com.pocketstudio.app.live.TakeUi.Armed || performance == null) return
        performanceAppend = append && p.song.isNotEmpty()
        val start = performance?.start ?: return
        if (NativeEngine.isAvailable && NativeEngine.songMarkStep() < start) NativeEngine.markSong(p.liveQuantumSteps, if (performanceAppend) p.song.sumOf { p.partSteps(it) } else 0)
    }

    /** No room after song [p] for a take « à la suite » (its parts all used). */
    private fun songFullForTake(p: Project): Boolean = p.song.isNotEmpty() && p.song.size >= SongPart.MAX_PARTS

    /** True while a take is armed or recording: Annuler waits for its end. */
    val performanceTaking: Boolean get() = _take.value.running

    private var performance: com.pocketstudio.model.PerformanceTake? = null
    private var performanceBefore: Project? = null
    private var performanceDepth = 0
    private var performanceAppend = false
    /** Launches of the take counted for its parts (the song holds 128). */
    private var performanceParts = 0
    /** 256-bar stretches of the take counted for its parts (a part lasts 256 bars at most). */
    private var performanceStretch = 0
    /** Song parts the take uses so far (the song's before it, when appending). */
    private var performancePartsUsed = 0
    private var performanceJob: kotlinx.coroutines.Job? = null

    /** Tests (no engine): the transport step the take's clock reads. */
    internal var takeClockForTest = -1

    /** Transport steps since play (-1 stopped or counting in). */
    private fun takeClock(): Int = if (NativeEngine.isAvailable) NativeEngine.transportStep() else takeClockForTest

    /** Where a gesture lands in the take: what is heard lags what is rendered by the output latency. */
    private fun takePosition(): Double {
        if (!NativeEngine.isAvailable) return takeClockForTest.toDouble()
        val latencyMs = Metrics.streamInfo.value.outputLatencyMs.takeIf { it > 0 } ?: 0.0
        return NativeEngine.transportPosition() - latencyMs / 1000.0 * project.value.tempoBpm / 15.0
    }

    /** ●: armed for the next launch line (stopped: the scene shown starts now); ■ Fin during the take; armed: cancelled. */
    fun togglePerformance() {
        when (_take.value) {
            is com.pocketstudio.app.live.TakeUi.Armed -> cancelPerformance()
            is com.pocketstudio.app.live.TakeUi.Recording -> finishPerformance(performanceEnd(takeClock()))
            is com.pocketstudio.app.live.TakeUi.Writing -> Unit
            else -> armPerformance()
        }
    }

    private fun armPerformance() {
        if (_recording.value || _audioRecording.value != null) return
        val p = project.value
        val append = _takeAppend.value && p.song.isNotEmpty()
        val base = if (append) p.song.sumOf { p.partSteps(it) } else 0
        if (append && songFullForTake(p)) {
            _notice.value = tr("Le morceau est plein : choisis « Remplacer le morceau » pour enregistrer.")
            return
        }
        if (append && base / p.stepsPerBar + 1 > effectsEndBar(p)) {
            _notice.value = tr("Le morceau dépasse la mesure %d : à la suite, seuls les lancements seront enregistrés, pas les effets.", effectsEndBar(p))
        }
        performanceBefore = p
        performanceDepth = session.undoDepth
        performanceAppend = append
        performanceParts = 0
        performanceStretch = 0
        performancePartsUsed = (if (append) p.song.size else 0) + 1
        val q = p.liveQuantumSteps
        val start = if (!_transport.value.playing) {
            // Marked before ▶ (the transport's start keeps it): the take's song position is there from step 0.
            if (NativeEngine.isAvailable) NativeEngine.markSong(q, base)
            launchRow(p.scene)  // the scene shown starts now; the take with it
            0
        } else {
            leaveSongForLive()
            if (NativeEngine.isAvailable) NativeEngine.markSong(q, base)
            nextLaunchStep(takeClock(), q)
        }
        performance = com.pocketstudio.model.PerformanceTake(start, liveNow(), p.liveColumnsMask, id = java.util.UUID.randomUUID().toString())
        _take.value = com.pocketstudio.app.live.TakeUi.Armed(start, (start - takeClock().coerceAtLeast(0)).coerceAtLeast(0))
        AppLog.add("Live : prise armée (mesure ${start / p.stepsPerBar + 1}${if (append) ", à la suite" else ""})")
        watchPerformance()
    }

    /** Armed, not started: nothing is written. */
    private fun cancelPerformance() {
        dropTakeJournal(performance)
        if (NativeEngine.isAvailable) NativeEngine.markSong(0, 0)
        releaseTakeHolds()
        performance = null
        performanceBefore = null
        _take.value = com.pocketstudio.app.live.TakeUi.Ready
    }

    /** The bar line a take stopped at [step] ends on (the bar being played is kept). */
    private fun performanceEnd(step: Int): Int {
        val bar = project.value.stepsPerBar
        return (step.coerceAtLeast(0) / bar + 1) * bar
    }

    /** The take ends on [end]: written into the song in one undo step. */
    private fun finishPerformance(end: Int) {
        // A Drop's bar line passed before the watch saw it: what it cleared is written first.
        settleDropIfPassed()
        val t = performance ?: return
        performance = null
        performanceJob?.cancel()
        // Its writes still waiting are dropped; the journal itself goes once the song is saved.
        journals.seal(t.id)
        if (_transport.value.playing) drainLaunches()
        releaseTakeHolds()
        _take.value = com.pocketstudio.app.live.TakeUi.Writing
        val before = performanceBefore ?: project.value
        performanceBefore = null
        var written = com.pocketstudio.model.TakeWritten(project.value, 0, 0)
        session.editQuietly { p ->
            val w = p.writeTake(t, end, performanceAppend).also { written = it }
            if (w.parts > 0) w.project.copy(liveTake = t.id) else w.project
        }
        if (written.parts == 0) {
            dropTakeJournal(t)
            _take.value = com.pocketstudio.app.live.TakeUi.Ready
            _notice.value = if (written.cut) tr("Le morceau est plein : choisis « Remplacer le morceau » pour enregistrer.") else tr("Prise trop courte : rien n'a été écrit.")
            return
        }
        session.commitQuietEdits(before, performanceDepth)
        performanceDoneDepth = session.undoDepth
        dropTakeJournalOnceSaved(t.id)
        // Never silently: the song full (128 parts) or no lane left for a setting moved.
        val lost = listOfNotNull(
            tr("Le morceau est plein (%d parties) : la fin de la prise n'a pas été écrite.", SongPart.MAX_PARTS).takeIf { written.cut },
            tr("Plus de place pour d'autres réglages (16 au plus) : %d n'ont pas été écrits.", written.skipped).takeIf { written.skipped > 0 },
            tr("Les effets joués après la mesure %d n'ont pas été écrits (les lancements, si).", effectsEndBar(project.value)).takeIf { written.late > 0 },
        )
        if (lost.isNotEmpty()) _notice.value = lost.joinToString(" ")
        if (NativeEngine.isAvailable) {
            // The song is written: from here the grid plays on its own again (its curves wait for the song).
            if (_transport.value.playing) NativeEngine.leaveSong()
            pushAutomation()
        }
        val first = if (performanceAppend) before.song.size else 0
        _take.value = com.pocketstudio.app.live.TakeUi.Done(written.parts, written.settings, first.coerceAtMost(project.value.song.lastIndex))
        AppLog.add("Live : morceau écrit (${written.parts} parties, ${written.settings} réglages)")
    }

    /** The clock of the ●: armed until the take starts, then its time and bar; launches go to the take. */
    private fun watchPerformance() {
        performanceJob?.cancel()
        performanceJob = viewModelScope.launch {
            var ticks = 0
            while (_take.value.running) {
                tickPerformance()
                // Every 5 s on disk: a take the app could not finish is not lost (see withWaitingLiveTake).
                if (++ticks % 50 == 0) keepTakeJournal()
                kotlinx.coroutines.delay(100)
            }
        }
    }

    /** One look at the transport for the take (tests call it after moving [takeClockForTest]). */
    internal fun tickPerformance() {
        settleDropIfPassed()
        val t = performance ?: return
        if (!_transport.value.playing) return
        drainLaunches()
        val step = takeClock()
        val p = project.value
        when (val ui = _take.value) {
            is com.pocketstudio.app.live.TakeUi.Armed -> {
                // The engine marks the song on the launch line after its command came: a quantum
                // late when it came past the line planned. The take starts where it marked.
                val mark = if (NativeEngine.isAvailable) NativeEngine.songMarkStep() else t.start.takeIf { step >= it } ?: -1
                if (mark > t.start) t.startsAt(mark)
                val marked = mark >= t.start
                _take.value = if (marked && step >= t.start) com.pocketstudio.app.live.TakeUi.Recording(t.start, 0, songBarOf(p, 0))
                else ui.copy(start = t.start, stepsLeft = (t.start - step).coerceAtLeast(0))
            }
            is com.pocketstudio.app.live.TakeUi.Recording -> {
                val elapsed = (step - t.start).coerceAtLeast(0)
                // 128 parts: the take ends there (the parts that fit are written, and it is said).
                // The parts are counted again at each launch, and each 256 bars (a part's longest).
                val stretch = elapsed / (SongPart.MAX_BARS * p.stepsPerBar)
                if (t.launchCount != performanceParts || stretch != performanceStretch) {
                    performanceParts = t.launchCount
                    performanceStretch = stretch
                    val before = if (performanceAppend) p.song.size else 0
                    performancePartsUsed = before + p.takeParts(t, performanceEnd(step)).size
                    if (performancePartsUsed > SongPart.MAX_PARTS) {
                        finishPerformance(performanceEnd(step))
                        return
                    }
                }
                val bar = songBarOf(p, elapsed)
                _take.value = ui.copy(seconds = (elapsed * 15.0 / p.tempoBpm).toInt(), bar = bar, warning = takeWarning(p, bar))
            }
            else -> Unit
        }
    }

    /**
     * What the REC line says besides the clock: a limit coming or passed, said before it bites (the
     * song's parts: the take ends at 128; the effects: written up to the song's bar
     * [effectsEndBar], the launches after it too). Null: nothing to say.
     */
    private fun takeWarning(p: Project, bar: Int): String? {
        val partsLeft = SongPart.MAX_PARTS - performancePartsUsed
        val effectsEnd = effectsEndBar(p)
        return when {
            partsLeft <= TAKE_PARTS_WARNING -> tr("Plus que %d parties possibles : la prise s'arrêtera ensuite.", partsLeft.coerceAtLeast(0))
            bar > effectsEnd -> tr("Après la mesure %d, les effets ne sont plus enregistrés (les lancements, si).", effectsEnd)
            bar > effectsEnd - TAKE_BARS_WARNING -> tr("Les effets sont enregistrés jusqu'à la mesure %d.", effectsEnd)
            else -> null
        }
    }

    /** The last song bar a take writes effects on (song curves hold a point per beat, [AutomationLane.MAX_SONG_BEATS]). */
    private fun effectsEndBar(p: Project): Int = AutomationLane.MAX_SONG_BEATS / p.beatsPerBar.coerceAtLeast(1)

    /** The song's bar (from 1) a take is on after [elapsed] steps (after the song when appending). */
    private fun songBarOf(p: Project, elapsed: Int): Int {
        val base = if (performanceAppend) p.song.sumOf { p.partSteps(it) } else 0
        return (base + elapsed) / p.stepsPerBar + 1
    }

    /**
     * A gesture on [target] during a take (armed or recording): written into the song at [at]
     * (sixteenths since play; null: where it was heard).
     */
    private fun takeGesture(target: AutoTarget, value: Float, before: Float, at: Double? = null, jump: Boolean = false) {
        val t = performance ?: return
        if (!_take.value.running) return
        t.gesture(target, value, at ?: takePosition(), before, jump)
        // Held to the take's end: no lane pulls it back between two gestures, so what is heard is
        // what the take writes (the setting stays where the finger left it).
        val packed = target.pack()
        if (takeHeld.add(packed) && liveEngine) engineHold(packed, true)
    }

    /** [session.undoDepth] just after the last take was written (its undo step on top). */
    private var performanceDoneDepth = -1

    /**
     * « Annuler » of the « Morceau écrit » line still undoes the take alone: nothing was done
     * since (a macro, a send, a pattern edited after it would be what ↶ takes back instead).
     */
    fun canUndoPerformance(): Boolean =
        _take.value is com.pocketstudio.app.live.TakeUi.Done && session.undoDepth == performanceDoneDepth && session.canUndo.value

    /** « Annuler » on the written take: the song as it was (only while the take is the last change). */
    fun undoPerformance() {
        if (_take.value !is com.pocketstudio.app.live.TakeUi.Done) return
        val alone = canUndoPerformance()
        _take.value = com.pocketstudio.app.live.TakeUi.Ready
        if (alone) undo() else _notice.value = tr("D'autres changements ont suivi la prise : utilise ↶ en haut pour revenir en arrière pas à pas.")
    }

    /** « Écouter »: the song from the take's first part. */
    fun listenPerformance() {
        val done = _take.value as? com.pocketstudio.app.live.TakeUi.Done ?: return
        _take.value = com.pocketstudio.app.live.TakeUi.Ready
        if (_transport.value.playing) toggleTransport()
        playSongFrom(done.firstPart.coerceIn(0, (project.value.song.size - 1).coerceAtLeast(0)))
    }

    /** The « Morceau écrit » line goes. */
    fun dismissPerformance() {
        if (_take.value is com.pocketstudio.app.live.TakeUi.Done) _take.value = com.pocketstudio.app.live.TakeUi.Ready
    }

    /** Stop during a take: it ends at the bar being played (armed: dropped). */
    private fun endPerformanceOnStop() {
        when (_take.value) {
            is com.pocketstudio.app.live.TakeUi.Armed -> cancelPerformance()
            is com.pocketstudio.app.live.TakeUi.Recording -> finishPerformance(performanceEnd(takeClock()))
            else -> Unit
        }
    }

    // --- Automation (mixer settings that move with the loop) ----------------------------------

    private val _automationRecord = MutableStateFlow(false)
    /** While on and playing, moving an automated control writes it on the current step. */
    val automationRecord: StateFlow<Boolean> = _automationRecord.asStateFlow()

    fun toggleAutomationRecord() {
        _automationRecord.value = !_automationRecord.value
    }

    private fun pushAutomation(p: Project = project.value) {
        if (!NativeEngine.isAvailable) return
        val dropped = NativeEngine.droppedCommands
        for (i in 0 until AutomationLane.MAX_LANES) {
            val lane = p.automation.getOrNull(i)
            NativeEngine.setAutomationTarget(i, lane?.target?.pack() ?: -1)
            if (lane == null) continue
            pushSongCurve(p, i, lane)
            // Per-scene clips: those of the scene the lane's track plays (another one while staying
            // on this one, or on the live grid; a track stopped there keeps what it had).
            val scene = sceneOfSlot(p, laneSlot(lane.target)) ?: continue
            lane.valuesFor(scene).forEachIndexed { step, v -> if (v != null) NativeEngine.setAutomationPoint(i, step, v) }
        }
        sendAgainIfDropped(dropped)
    }

    /** A song lane's curve over the bars the screen shows (the song, at least 8 bars). */
    private fun pushSongCurve(p: Project, i: Int, lane: AutomationLane) {
        val points = maxOf(SONG_LANE_MIN_BARS, p.songBars) * lane.pointsPerBar(p.beatsPerBar)
        val curve = if (lane.mode == AutomationLane.SONG) lane.songCurve(points) else emptyList()
        NativeEngine.setAutomationMode(i, lane.mode, curve.size, lane.songPerBeat, lane.hold)
        // Runs of equal values and straight slides in one command each: the curves of a whole song
        // fit in the engine's queue (a full queue waits for the audio thread; see sendAgainIfDropped).
        for (c in songCurveCommands(curve)) when (c) {
            is CurveCommand.Run ->
                if (c.count == 1) NativeEngine.setAutomationSong(i, c.first, c.value) else NativeEngine.setAutomationSongRun(i, c.first, c.count, c.value)
            is CurveCommand.Ramp -> NativeEngine.setAutomationSongRamp(i, c.first, c.count, c.from, c.to)
        }
        // The points before a jump hold (a take's Drop, a let-go): runs of them, one command each.
        for (run in flatRuns(lane.songFlatPoints(curve.size))) NativeEngine.setAutomationSongFlat(i, run.first, run.last - run.first + 1)
    }

    private fun flatRuns(points: List<Int>): List<IntRange> {
        val out = ArrayList<IntRange>()
        var start = -1
        var last = -2
        for (p in points) {
            if (p != last + 1) {
                if (start >= 0) out += start..last
                start = p
            }
            last = p
        }
        if (start >= 0) out += start..last
        return out
    }

    /** Clip kind of lane [index]: loop, per scene or the whole song. */
    fun setAutomationMode(index: Int, mode: Int) {
        if (index !in project.value.automation.indices) return
        session.edit { p ->
            p.copy(automation = p.automation.mapIndexed { i, l ->
                if (i != index || l.mode == mode) l
                // Going per scene: every scene starts from the loop's steps.
                else if (mode == AutomationLane.PER_SCENE) l.copy(mode = mode, sceneValues = p.allScenes.indices.associateWith { l.values })
                else l.copy(mode = mode)
            })
        }
        pushAutomation()
        pushMixer()
    }

    /** The song curve: [value] on bar [bar] (null erases; a curve with a point per beat: its bar's first beat). */
    fun setAutomationSongPoint(index: Int, bar: Int, value: Float?) {
        if (index !in project.value.automation.indices) return
        session.edit("auto-song-$index") { p ->
            p.copy(automation = p.automation.mapIndexed { i, l -> if (i == index) l.withSongPoint(bar * l.pointsPerBar(p.beatsPerBar), value) else l })
        }
        // Only this lane (a finger drag sends many points).
        if (NativeEngine.isAvailable) pushSongCurve(project.value, index, project.value.automation[index])
    }

    fun addAutomation(target: AutoTarget) {
        if (!target.isValid || project.value.automation.size >= AutomationLane.MAX_LANES || project.value.automation.any { it.target == target }) return
        session.edit { p -> p.copy(automation = p.automation + AutomationLane(target)) }
        pushAutomation()
    }

    fun removeAutomation(index: Int) {
        session.edit { p -> p.copy(automation = p.automation.filterIndexed { i, _ -> i != index }) }
        pushAutomation()
        pushMixer(project.value)  // back to the static settings
    }

    fun clearAutomation(index: Int) {
        session.edit { p -> p.copy(automation = p.automation.mapIndexed { i, l -> if (i == index) AutomationLane(l.target) else l }) }
        pushAutomation()
        pushMixer(project.value)
    }

    /** Drawing: [value] 0..1 on [step] (null erases). Consecutive strokes merge into one undo step. */
    fun setAutomationPoint(index: Int, step: Int, value: Float?) {
        if (index !in project.value.automation.indices) return
        session.edit("auto-$index") { p -> p.copy(automation = p.automation.mapIndexed { i, l -> if (i == index) l.withPoint(step, value, p.scene) else l }) }
        val mode = project.value.automation[index].mode
        if (NativeEngine.isAvailable && mode != AutomationLane.SONG && !(mode == AutomationLane.PER_SCENE && editsWait)) {
            NativeEngine.setAutomationPoint(index, step, value?.coerceIn(0f, 1f))
        }
    }

    private fun recordAutomation(target: AutoTarget, value: Float) {
        if (!_automationRecord.value || !_transport.value.playing || !NativeEngine.isAvailable || !target.isValid) return
        recordGesture(target, value, NativeEngine.currentStep(), NativeEngine.songStep())
    }

    /**
     * A gesture on [target] recorded at pattern [step] (song lanes: on the bar of [songStep], the
     * engine's song position): into its lane, made when there is none yet.
     */
    internal fun recordGesture(target: AutoTarget, value: Float, step: Int, songStep: Int) {
        if (!target.isValid || step !in 0 until AutomationLane.STEPS) return
        var index = project.value.automation.indexOfFirst { it.target == target }
        if (index < 0) {
            if (project.value.automation.size >= AutomationLane.MAX_LANES) return
            addAutomation(target)
            index = project.value.automation.indexOfFirst { it.target == target }
        }
        // A song curve records on the bar the song is at.
        if (project.value.automation[index].mode == AutomationLane.SONG) {
            // The bar the engine's song curve is reading now (it restarts when the song loops);
            // none while a scene plays on its own: its bars are not the song's.
            if (songStep >= 0 && (songPlaying() || project.value.song.isEmpty())) {
                val lane = project.value.automation[index]
                if (lane.songPerBeat) {
                    session.edit("auto-song-$index") { p -> p.copy(automation = p.automation.mapIndexed { i, l -> if (i == index) l.withSongPoint(songStep / 4, value) else l }) }
                    if (NativeEngine.isAvailable) pushSongCurve(project.value, index, project.value.automation[index])
                } else {
                    setAutomationSongPoint(index, songStep / project.value.stepsPerBar, value)
                }
            } else {
                val refused = tr("Écoute tout le morceau pour enregistrer sur la ligne du morceau.")
                if (_notice.value != refused) _notice.value = refused
            }
        } else {
            setAutomationPoint(index, step, value)
        }
    }

    /** After a stop, settings left where the automation put them go back to the project's values. */
    private fun restoreAfterAutomation() {
        if (project.value.automation.isNotEmpty()) pushMixer(project.value)
    }

    // --- Remix: a separated song played along with the project ---------------------------------

    private var stems: NativeStems? = null
    private var stemsId: String? = null
    // The song stretched to the project's tempo (Remix.followTempo), and what it was made for.
    private var stretched: NativeStems? = null
    private var stretchedFor: Pair<String?, Double>? = null
    private var remixStretchJob: kotlinx.coroutines.Job? = null
    private var stretchPending: Pair<String, Double>? = null
    /** Stretch of what the engine plays now (engine frames = song frames x this). */
    private var playRatio = 1.0
    private val _remixStretching = MutableStateFlow(false)
    /** True while the song is being fitted to the project's tempo. */
    val remixStretching: StateFlow<Boolean> = _remixStretching.asStateFlow()

    private fun dropStretched() {
        remixStretchJob?.cancel()
        _remixStretching.value = false
        stretched?.close()  // the engine keeps its own reference until it lets go
        stretched = null
        stretchedFor = null
    }

    /** Fits the song to the tempo in the background; tempo drags settle first. */
    private val stretchFailed = HashSet<Pair<String, Double>>()

    private fun stretchStems(source: NativeStems, id: String, ratio: Double) {
        if (remixStretchJob?.isActive == true && stretchPending == id to ratio) return  // already on it
        if (id to ratio in stretchFailed) return  // tried already: no new attempt on every mixer move
        if (ratio !in 0.5..2.0) {
            stretchFailed += id to ratio
            _notice.value = tr("Le tempo du projet est trop loin de celui du morceau pour le caler (de la moitié au double).")
            return
        }
        remixStretchJob?.cancel()
        stretchPending = id to ratio
        _remixStretching.value = true
        remixStretchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(600)
            // Once started it finishes: the copy is closed below (or on cancellation) if no longer wanted.
            val copy = finishOrRelease(Dispatchers.Default, NativeStems::close) {
                runCatching { source.stretched(ratio) }.getOrNull()
            }
            val p = project.value
            if (!isActive || p.remix.setId != id || p.remix.stretchFor(p.tempoBpm) != ratio || stems !== source) {
                copy?.close()
                return@launch
            }
            _remixStretching.value = false
            if (copy == null) {
                stretchFailed += id to ratio
                AppLog.add("Remix : calage au tempo impossible (mémoire ?)")
                _notice.value = tr("Le morceau n'a pas pu être calé au tempo (mémoire insuffisante) : il garde son tempo.")
                return@launch
            }
            val old = stretched
            stretched = copy
            stretchedFor = id to ratio
            pushRemix()
            old?.close()
            AppLog.add("Remix : calé à ${"%.1f".format(p.tempoBpm)} BPM (x${"%.3f".format(ratio)})")
        }
    }

    /** The song follows the project's tempo (stretched) or keeps its own. */
    fun setRemixFollowTempo(on: Boolean) = editRemix(null) { it.copy(followTempo = on) }
    private var stemsJob: kotlinx.coroutines.Job? = null
    private val _remixLoading = MutableStateFlow(false)
    val remixLoading: StateFlow<Boolean> = _remixLoading.asStateFlow()

    /** Waveform overview of the loaded stems (null until loaded). */
    private val _stemOverview = MutableStateFlow<StemOverview?>(null)
    val stemOverview: StateFlow<StemOverview?> = _stemOverview.asStateFlow()

    /** Song position (frames at 48 kHz) while the stems play, else -1. */
    private val _stemPosition = MutableStateFlow(-1L)
    val stemPosition: StateFlow<Long> = _stemPosition.asStateFlow()

    /** Polls the playhead about 30 times a second while the Remix screen is visible. */
    suspend fun pollStemPosition() {
        if (!NativeEngine.isAvailable) return
        while (true) {
            // Back to song frames when a stretched copy plays.
            _stemPosition.value = NativeEngine.stemPosition().let { if (it < 0) it else Math.round(it / playRatio) }
            kotlinx.coroutines.delay(33)
        }
    }

    /** Touching the waveform at [fraction] (0..1) of the song starts the remix at that bar. */
    fun setRemixStartAt(fraction: Float) {
        val r = project.value.remix
        val overview = _stemOverview.value ?: return
        val bar = r.barFrames(OfflineExport.SAMPLE_RATE)
        if (bar <= 0) return
        val frame = (fraction.coerceIn(0f, 1f) * overview.frames).toLong() - Math.round(r.firstBeat * OfflineExport.SAMPLE_RATE)
        setRemixStartBar((frame / bar).toInt().coerceAtLeast(0))
    }

    private fun pushRemix(p: Project = project.value) {
        if (!NativeEngine.isAvailable) return
        val loaded = stems?.takeIf { stemsId == p.remix.setId }
        // Following the project's tempo: the stretched copy once it is ready, the song as is meanwhile.
        val ratio = p.remix.stretchFor(p.tempoBpm)
        val fitted = stretched?.takeIf { loaded != null && stretchedFor == (p.remix.setId to ratio) }
        NativeEngine.setStems(fitted ?: loaded)
        playRatio = if (fitted != null) ratio else 1.0
        NativeEngine.setStemStart(Math.round(p.remix.startFrame(OfflineExport.SAMPLE_RATE) * playRatio))
        if (loaded != null && ratio != 1.0 && fitted == null) stretchStems(loaded, p.remix.setId!!, ratio)
        if (ratio == 1.0 && (stretched != null || remixStretchJob?.isActive == true)) dropStretched()
        p.remix.stems.forEachIndexed { i, m ->
            NativeEngine.setStemParam(i, 0, m.volume)
            NativeEngine.setStemParam(i, 1, if (m.mute) 1f else 0f)
            NativeEngine.setStemParam(i, 2, if (m.solo) 1f else 0f)
        }
        val wanted = p.remix.setId
        if (loaded == null && stems != null) {
            // Another song (or none): the engine let go of the old one, free it now, not after the next load.
            dropStretched()
            stems?.close()
            stems = null
            stemsId = null
            _stemOverview.value = null
        }
        if (loaded == null && wanted != null && stemsJob?.isActive != true) loadStems(wanted)
    }

    /** Reads the stems into the engine's memory (seconds, worker thread), then plays them along. */
    private fun loadStems(id: String) {
        val app = getApplication<Application>()
        _remixLoading.value = true
        stemsJob = viewModelScope.launch {
            // A finished load reaches the code below, which closes it if unwanted (or is closed on cancellation).
            val loaded = finishOrRelease(Dispatchers.IO, NativeStems::close) {
                StemStore.pin(app, id)  // also covers projects reopened from disk
                StemStore.read(StemStore.dirFor(app, id))?.let { set ->
                    NativeStems.load(set.stem("drums").path, set.stem("bass").path, set.stem("other").path, set.stem("vocals").path)
                }
            }
            _remixLoading.value = false
            stemsJob = null  // so pushRemix below may start the next load
            if (!isActive) {
                loaded?.close()
                return@launch
            }
            val wanted = project.value.remix
            if (loaded == null) {
                if (wanted.setId == id) {
                    AppLog.add("Remix : pistes de « ${wanted.name} » introuvables ou illisibles")
                    _notice.value = tr("Les pistes de « %s » ne sont plus sur l'appareil (ou la mémoire manque). Sépare à nouveau le morceau.", wanted.name)
                }
                return@launch
            }
            if (wanted.setId != id) {
                loaded.close()
                if (wanted.setId != null) pushRemix()
                return@launch
            }
            val old = stems
            stems = loaded
            stemsId = id
            pushRemix()
            old?.close()  // the engine keeps its own reference until it lets go
            _stemOverview.value = withContext(Dispatchers.Default) {
                runCatching { StemOverview(List(4) { loaded.peaks(it, OVERVIEW_BUCKETS) }, loaded.frames) }.getOrNull()
            }
            AppLog.add("Remix : « ${wanted.name} » chargé (${loaded.frames / OfflineExport.SAMPLE_RATE} s)")
        }
    }

    /** The project plays a separated song along, at the song's tempo and in its key. One undo step. */
    fun useSong(set: StemSet) {
        val info = set.info
        session.edit("remix") { p ->
            p.copy(
                tempoBpm = if (info.bpm > 0) info.bpm else p.tempoBpm,
                harmony = info.root?.let { Harmony(it, if (info.minor) Scale.Minor else Scale.Major) } ?: p.harmony,
                remix = Remix(set.id, set.name, info.bpm, info.firstBeat),
            )
        }
        StemStore.pin(getApplication(), set.id)
        if (!NativeEngine.isAvailable) return
        NativeEngine.setTempo(project.value.tempoBpm)
        if (project.value.drums.lanes.any { (it.sample?.fitBeats ?: 0) > 0 }) pushPads()
        pushRemix()
    }

    fun clearRemix() {
        dropStretched()
        session.edit("remix") { p -> p.copy(remix = Remix()) }
        StemStore.pin(getApplication(), null)
        pushRemix()
        stems?.close()
        stems = null
        stemsId = null
        _stemOverview.value = null
    }

    fun setStemVolume(stem: Int, v: Float) = editRemix("stem-$stem-vol") { it.withStem(stem) { s -> s.copy(volume = v) } }
    fun toggleStemMute(stem: Int) = editRemix(null) { it.withStem(stem) { s -> s.copy(mute = !s.mute) } }
    fun toggleStemSolo(stem: Int) = editRemix(null) { it.withStem(stem) { s -> s.copy(solo = !s.solo) } }
    fun setRemixStartBar(bar: Int) = editRemix("remix-start") { it.copy(startBar = bar.coerceIn(0, Remix.MAX_START_BAR)) }

    private fun editRemix(key: String?, transform: (Remix) -> Remix) {
        session.edit(key) { p -> p.copy(remix = transform(p.remix)) }
        pushRemix()
    }

    /** [bars] bars of one stem, from the remix start, onto the selected pad. */
    fun stemToPad(stem: Int, bars: Int) {
        val source = stems ?: return
        if (_sampleTask.value is SampleTask.Busy || _sampleTask.value is SampleTask.Recording) return
        val r = project.value.remix
        val count = r.barFrames(OfflineExport.SAMPLE_RATE) * bars
        if (count <= 0) return
        if (count > AudioDecoder.MAX_SECONDS * OfflineExport.SAMPLE_RATE) {
            _sampleTask.value = SampleTask.Failed(tr("Trop long pour un pad : choisis moins de mesures."))
            return
        }
        val label = tr(STEM_LABELS.getOrElse(stem) { "Piste" })
        sampleJob(tr("%s vers le pad…", label)) {
            val audio = source.extract(stem, r.startFrame(OfflineExport.SAMPLE_RATE), count)
            if (audio.isEmpty()) throw UserMessage(tr("Ce passage est après la fin du morceau."))
            padSamples.add(Pcm(audio, 2, OfflineExport.SAMPLE_RATE), com.pocketstudio.model.DefaultNames.stemPad(STEM_LABELS.getOrElse(stem) { "Piste" }, r.name))  // French, shown translated
        }
    }

    /** [bars] bars of one stem onto the first empty live pad: switch the parts on and off in time. */
    fun stemToLive(stem: Int, bars: Int) {
        val source = stems ?: return
        if (_sampleTask.value is SampleTask.Busy || _sampleTask.value is SampleTask.Recording) return
        val pad = project.value.live.clips.indexOfFirst { it == null }
        if (pad < 0) {
            _sampleTask.value = SampleTask.Failed(tr("Les 16 pads live sont pris : vides-en un dans l'onglet Morceau."))
            return
        }
        val r = project.value.remix
        val count = r.barFrames(OfflineExport.SAMPLE_RATE) * bars
        if (count <= 0) return
        if (count > AudioDecoder.MAX_SECONDS * OfflineExport.SAMPLE_RATE) {
            _sampleTask.value = SampleTask.Failed(tr("Trop long pour un pad : choisis moins de mesures."))
            return
        }
        val label = tr(STEM_LABELS.getOrElse(stem) { "Piste" })
        if (project.value.live.size <= pad) setLiveSize(LivePads.SIZES.first { it > pad })
        sampleJob(tr("%1\$s vers le pad live %2\$d…", label, pad + 1), live = pad, beats = bars * 4) {
            val audio = source.extract(stem, r.startFrame(OfflineExport.SAMPLE_RATE), count)
            if (audio.isEmpty()) throw UserMessage(tr("Ce passage est après la fin du morceau."))
            padSamples.add(Pcm(audio, 2, OfflineExport.SAMPLE_RATE), com.pocketstudio.model.DefaultNames.stemPad(STEM_LABELS.getOrElse(stem) { "Piste" }, r.name))  // French, shown translated
        }
    }

    // --- Instrument library (General MIDI banks) -------------------------------------------

    private val _library = MutableStateFlow<LibraryState>(LibraryState.Missing)
    val library: StateFlow<LibraryState> = _library.asStateFlow()
    /** The bank shown in the browser (and loaded once opened). */
    private val _bankId = MutableStateFlow(project.value.instrument?.bankId ?: InstrumentLibrary.BANKS[0].id)
    val bankId: StateFlow<String> = _bankId.asStateFlow()
    /** Banks in memory: the one shown in the browser and those the tracks play. */
    private val fonts = HashMap<String, SoundFont>()
    private val loadingBanks = HashSet<String>()
    /** The bank shown in the browser, once open. */
    private val soundFont: SoundFont? get() = fonts[_bankId.value]
    @Volatile private var downloadCancelled = false

    /** What each track's General MIDI instrument is in the live engine (bank, preset index). */
    private val liveInstruments = HashMap<Int, Pair<SoundFont?, Int>>()

    /** Every track's General MIDI instrument (a track whose bank is not open yet plays its synth). */
    private fun pushInstrument(p: Project = project.value) {
        if (!NativeEngine.isAvailable) return
        // The live engine only gets the tracks that change: a new instance cuts the notes held on
        // that track (an undo while playing must not). An export engine gets them all.
        val live = !NativeEngine.isCapturing
        p.instrumentTracks.forEach { t ->
            val font = t.instrument?.let { fonts[it.bankId] }
            val index = t.instrument?.let { font?.indexOf(it.bank, it.program) } ?: -1
            val wanted = if (font != null && index >= 0) font to index else null to 0
            if (live) {
                val had = liveInstruments[t.id]
                if (had != null && had.first === wanted.first && had.second == wanted.second) return@forEach
            }
            // Remembered only once the engine took it: a lost command is sent again next time.
            if (NativeEngine.setInstrument(wanted.first, wanted.second, t.id) && live) liveInstruments[t.id] = wanted
        }
    }

    /** Opens the banks the tracks play that are on the phone but not in memory yet. */
    private fun loadTrackFonts() {
        val app = getApplication<Application>()
        val wanted = project.value.instrumentTracks.mapNotNull { it.instrument?.bankId }.toSet()
        wanted.filter { it !in fonts && it !in loadingBanks }.forEach { id ->
            val bank = InstrumentLibrary.bank(id)
            if (!InstrumentLibrary.isInstalled(app, bank)) return@forEach
            loadingBanks += id
            viewModelScope.launch {
                val font = withContext(Dispatchers.IO) {
                    if (InstrumentLibrary.verify(app, bank)) SoundFont.load(InstrumentLibrary.file(app, bank)) else null
                }
                loadingBanks -= id
                if (font == null) return@launch
                if (fonts.containsKey(id)) font.close() else fonts[id] = font
                pushInstrument()
                if (_bankId.value == id) _library.value = LibraryState.Ready(fonts[id]!!.presets)
            }
        }
    }

    /** Closes banks nobody plays or browses any more (the engine keeps its own instances). */
    private fun releaseUnusedFonts() {
        // An export may be copying a font on its worker (TinySoundFont's count is not atomic).
        if (running != null || exportJob != null) return
        val used = project.value.instrumentTracks.mapNotNull { it.instrument?.bankId }.toSet() + _bankId.value
        fonts.keys.filter { it !in used }.forEach { fonts.remove(it)?.close() }
    }

    /** Shows another bank in the browser, opening it if it is on the phone. */
    fun selectBank(id: String) {
        if (_library.value is LibraryState.Downloading) return
        _bankId.value = id
        openLibrary()
    }

    /** Opens the shown bank if it is on the phone (checked first), else says it must be downloaded. */
    fun openLibrary() {
        val bank = InstrumentLibrary.bank(_bankId.value)
        if (_library.value is LibraryState.Downloading || _library.value == LibraryState.Loading) return
        fonts[bank.id]?.let {
            _library.value = LibraryState.Ready(it.presets)
            releaseUnusedFonts()
            return
        }
        val app = getApplication<Application>()
        if (!InstrumentLibrary.isInstalled(app, bank)) {
            _library.value = LibraryState.Missing
            return
        }
        _library.value = LibraryState.Loading
        viewModelScope.launch {
            val font = withContext(Dispatchers.IO) {
                if (InstrumentLibrary.verify(app, bank)) SoundFont.load(InstrumentLibrary.file(app, bank))
                else null.also { InstrumentLibrary.file(app, bank).delete() }
            }
            if (font == null) {
                _library.value = LibraryState.Failed(tr("La bibliothèque était abîmée : télécharge-la à nouveau."))
                return@launch
            }
            if (fonts.containsKey(bank.id)) font.close() else fonts[bank.id] = font
            pushInstrument()
            releaseUnusedFonts()
            if (_bankId.value == bank.id) _library.value = LibraryState.Ready(fonts[bank.id]!!.presets)
            AppLog.add("Bibliothèque ${bank.name} : ${font.presets.size} sons")
        }
    }

    fun downloadLibrary() {
        if (_library.value is LibraryState.Downloading) return
        val app = getApplication<Application>()
        val bank = InstrumentLibrary.bank(_bankId.value)
        downloadCancelled = false
        _library.value = LibraryState.Downloading(0f)
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    InstrumentLibrary.download(app, bank, { p -> _library.value = LibraryState.Downloading(p) }) { downloadCancelled }
                }
                AppLog.add("Bibliothèque ${bank.name} téléchargée")
                _library.value = LibraryState.Missing
                openLibrary()
            } catch (e: Exception) {
                AppLog.add("Téléchargement de ${bank.name} : ${e.message}")
                _library.value = if (downloadCancelled) LibraryState.Missing
                else LibraryState.Failed(tr("Le téléchargement a échoué (%s). Vérifie la connexion et réessaie.", e.forUser()))
            }
        }
    }

    fun cancelLibraryDownload() {
        downloadCancelled = true
    }

    /** null: back to the subtractive synth. */
    fun selectInstrument(choice: InstrumentChoice?) {
        editTrack { it.copy(instrument = choice) }
        pushInstrument()
        releaseUnusedFonts()
        if (choice != null && NativeEngine.isAvailable && !_transport.value.playing) {
            viewModelScope.launch {
                kotlinx.coroutines.delay(30)  // let the new instrument arrive first
                previewNote(60)
            }
        }
    }

    /**
     * Puts a General MIDI drum kit on the 16 pads: each pad gets the kit's sound for its role
     * (kick, snare, hats…), hats in one choke group. One undo step.
     */
    fun loadKit(kit: Preset) {
        val font = soundFont
        if (font == null || _sampleTask.value is SampleTask.Busy) {
            android.widget.Toast.makeText(getApplication(), tr("Patiente un instant, un son est en cours de préparation."), android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        _sampleTask.value = SampleTask.Busy(tr("Kit %s…", tr(kit.name)))
        viewModelScope.launch {
            try {
                val pads = ArrayList<Pair<Int, PadSample>>()
                InstrumentLibrary.KIT_NOTES.forEachIndexed { pad, note ->
                    // Rendering shares the font's reference count: main thread, one note at a time.
                    val audio = font.renderNote(kit, note)
                    kotlinx.coroutines.yield()
                    if (audio.size < 480) return@forEachIndexed
                    // Some kit samples peak above full scale: bring them to -1 dBFS.
                    val peak = audio.maxOf { kotlin.math.abs(it) }
                    if (peak > 0.89f) for (i in audio.indices) audio[i] *= 0.89f / peak
                    val (sample, prepared) = withContext(Dispatchers.IO) {
                        padSamples.add(Pcm(audio, 1, OfflineExport.SAMPLE_RATE), com.pocketstudio.model.DefaultNames.kitPad(kit.name, pad))  // French, shown translated
                    }
                    padSamples.register(prepared)
                    pads += pad to sample.copy(choke = if (note == 42 || note == 46) 1 else 0)
                }
                session.edit { p -> p.copy(drums = Slicer.apply(p.drums, pads)) }
                pushPads()
                AppLog.add("Kit ${kit.name} : ${pads.size} sons sur les pads")
                _sampleTask.value = SampleTask.Idle
                if (NativeEngine.isAvailable) NativeEngine.triggerPad(0, 0.9f)
                android.widget.Toast.makeText(
                    getApplication(), tr("Kit « %s » sur les pads : va dans Rythme (Annuler possible).", tr(kit.name)), android.widget.Toast.LENGTH_LONG,
                ).show()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _sampleTask.value = SampleTask.Failed(tr("Le kit n'a pas pu être chargé (%s).", e.message))
                android.widget.Toast.makeText(getApplication(), tr("Le kit n'a pas pu être chargé."), android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    // --- Piano roll ------------------------------------------------------------------------

    /** The engine plays the clip as performed: arpeggios and strums expanded here. */
    private fun pushNotes(clip: NoteClip, id: Int = track) {
        if (!NativeEngine.isAvailable || waitsFor(LiveSlots.INST + id)) return
        val played = clip.performance.expand(clip)
        NativeEngine.setSynthSteps(clip.steps, id)
        played.forEachIndexed { i, n -> NativeEngine.setSynthNote(i, n.start, n.length, n.pitch, n.velocity, id) }
        NativeEngine.setSynthNoteCount(played.size, id)
    }

    // --- Harmony (scale lock, chords, progressions, arpeggio, strum) -------------------------

    fun setHarmony(harmony: Harmony) {
        session.edit { p -> p.copy(harmony = harmony) }
    }

    /** A whole chord of the current scale on [pitch], in one tap. */
    fun addChord(start: Int, length: Int, pitch: Int) {
        val chord = project.value.harmony.chord(pitch)
        editNotes { clip -> chord.fold(clip) { c, p -> c.add(Note(start, length, p)) } }
        if (NativeEngine.isAvailable && !_transport.value.playing) chord.forEach(::previewNote)
    }

    fun applyProgression(progression: Progression) {
        val h = project.value.harmony
        editNotes { it.withProgression(progression, h) }
        AppLog.add("Progression ${progression.label}")
    }

    /** The progression's roots as a bass line (eighth notes, in the bass) on the selected track. */
    fun applyBassLine(progression: Progression) {
        val h = project.value.harmony
        editNotes { it.withBassLine(progression.degrees, h) }
        AppLog.add("Ligne de basse ${progression.label}")
    }

    /** The progression's chords an octave up, played one note at a time by the arpeggiator. */
    fun applyArpeggio(progression: Progression) {
        val h = project.value.harmony
        editNotes {
            it.copy(performance = it.performance.copy(arp = ArpMode.UpDown, arpRate = NoteClip.TICKS_PER_STEP))
                .withChords(progression.degrees, h, octaveRoot = 72, velocity = 0.7f)
        }
        AppLog.add("Arpège ${progression.label}")
    }

    fun setPerformance(performance: Performance) = editNotes("perf") { it.copy(performance = performance).sanitized() }

    /** Keyboard in chord mode: the chord of the scale on the key. */
    fun synthChordOn(pitch: Int) {
        val chord = project.value.harmony.chord(pitch)
        keysDown.pressChord(pitch, chord)
        chord.forEach { synthNoteOn(it) }
    }

    fun synthChordOff(pitch: Int) = (keysDown.releaseChord(pitch) ?: project.value.harmony.chord(pitch)).forEach { synthNoteOff(it) }

    // --- Audio to notes (Basic Pitch): a recording becomes the piano roll's notes --------------

    private val _transcription = MutableStateFlow<SampleTask>(SampleTask.Idle)
    /** Busy / Recording / Failed while turning audio into notes; Idle otherwise. */
    val transcription: StateFlow<SampleTask> = _transcription.asStateFlow()
    private var noteRecorder: MicRecorder? = null

    /** The bundled model, copied out of the APK once and checked before every use. */
    private fun pitchModel(): File {
        val app = getApplication<Application>()
        val file = File(app.filesDir, "basic-pitch.onnx")
        fun ok() = file.exists() && sha256(file) == BasicPitch.MODEL_SHA256
        if (!ok()) {
            // Copied beside it, then renamed: a copy cut short never passes for the model.
            val tmp = File(file.path + ".tmp")
            app.assets.open(BasicPitch.MODEL_ASSET).use { input -> tmp.outputStream().use { input.copyTo(it) } }
            if (!tmp.renameTo(file)) tmp.delete()
            check(ok()) { "modèle de notes abîmé" }
        }
        return file
    }

    private fun sha256(file: File): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                digest.update(buf, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /** An audio file (a hummed tune, a melody played on a guitar…) to piano-roll notes. */
    fun transcribeFile(uri: Uri) = transcribeJob(tr("Lecture du fichier…")) {
        try {
            AudioDecoder.decode(getApplication(), uri)
        } catch (e: AudioDecoder.TooLongException) {
            throw UserMessage(tr("Ce fichier est trop long : choisis un passage plus court."))
        }
    }

    /** Sing, whistle or play near the phone; [stopNoteRecording] ends it (30 s at most). */
    fun recordNotes() {
        if (transcribing?.isActive == true) return
        val rec = MicRecorder().also { noteRecorder = it }
        _transcription.value = SampleTask.Recording(0f, 0f)
        transcribeJob(null) {
            val pcm = rec.record { sec -> _transcription.value = SampleTask.Recording(sec, rec.level) }
            noteRecorder = null
            if (pcm.frames < 4800 || PadSamples.isSilent(pcm)) throw UserMessage(tr("Rien n'a été entendu : rapproche-toi du micro et réessaie."))
            pcm
        }
    }

    fun stopNoteRecording() {
        noteRecorder?.stop()
    }

    fun dismissTranscription() {
        if (_transcription.value is SampleTask.Failed) _transcription.value = SampleTask.Idle
    }

    private var transcribing: kotlinx.coroutines.Job? = null

    private fun transcribeJob(busy: String?, read: suspend () -> Pcm) {
        if (transcribing?.isActive == true && busy != null) return  // one at a time
        if (busy != null) _transcription.value = SampleTask.Busy(busy)
        transcribing = viewModelScope.launch {
            try {
                val heard = withContext(Dispatchers.Default) {
                    val pcm = read()
                    _transcription.value = SampleTask.Busy(tr("Écoute des notes…"))
                    val mono = FloatArray(pcm.frames) { f ->
                        var v = 0f
                        for (c in 0 until pcm.channels) v += pcm.data[f * pcm.channels + c]
                        v / pcm.channels
                    }
                    BasicPitch.transcribe(pitchModel(), mono, pcm.sampleRate).map { HeardNote(it.start, it.end, it.pitch, it.amplitude) }
                }
                if (heard.isEmpty()) throw UserMessage(tr("Aucune note reconnue : essaie une mélodie plus nette, une note à la fois."))
                AppLog.add("Audio vers notes : ${heard.size} notes")
                editNotes { Transcription.toClip(heard, project.value.tempoBpm, base = it) }
                _transcription.value = SampleTask.Idle
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                noteRecorder = null
                AppLog.add("Audio vers notes : échec (${e.message})")
                _transcription.value = SampleTask.Failed(if (e is UserMessage) e.message!! else tr("Ça n'a pas marché : %s", e.message ?: e.javaClass.simpleName))
            }
        }
    }

    private fun editNotes(key: String? = null, transform: (NoteClip) -> NoteClip) {
        val id = track
        editTrack(key, id) { it.copy(notes = transform(it.notes)) }
        pushNotes(project.value.track(id).notes, id)
    }

    /** A whole-clip edit from the piano roll's selection tools. */
    fun replaceNoteClip(clip: NoteClip, key: String?) = editNotes(key) { clip.sanitized() }

    fun addNote(note: Note) {
        editNotes { it.add(note) }
        if (NativeEngine.isAvailable && !_transport.value.playing) previewNote(note.pitch)
    }

    fun removeNote(note: Note) = editNotes { it.remove(note) }

    fun changeNote(old: Note, new: Note) = editNotes { it.replace(old, new) }

    fun quantizeNotes(gridTicks: Int, strength: Float) = editNotes { it.quantized(gridTicks, strength) }

    fun setNoteClipSteps(steps: Int) = editNotes { it.copy(steps = steps).sanitized() }

    fun clearNotes() = editNotes { it.cleared() }

    /** Empties track [id]'s notes in the scene shown (one undo step), whichever track is open. */
    fun clearTrackNotes(id: Int) {
        if (project.value.instrumentTracks.none { it.id == id }) return
        editTrack(id = id) { it.copy(notes = it.notes.cleared()) }
        pushNotes(project.value.track(id).notes, id)
    }

    /** A short note so the user hears what they placed. */
    private fun previewNote(pitch: Int) {
        val id = track
        NativeEngine.synthNoteOn(pitch, 0.8f, id)
        viewModelScope.launch {
            kotlinx.coroutines.delay(250)
            NativeEngine.synthNoteOff(pitch, id)
        }
    }

    // --- Pad samples (import, microphone) --------------------------------------------------

    private var stretchJob: kotlinx.coroutines.Job? = null

    /** Gives every pad its sound; tempo-fitted pads get their stretched copy as soon as it is ready. */
    private fun pushPads(p: Project = project.value) {
        if (NativeEngine.isAvailable) p.drums.lanes.forEachIndexed { i, l -> NativeEngine.setPadPan(i, l.pan) }
        val pads = p.drums.lanes.map { it.sample }
        padSamples.pushPads(pads, p.tempoBpm)
        val missing = padSamples.missingStretches(pads, p.tempoBpm)
        if (missing.isEmpty()) return
        stretchJob?.cancel()
        stretchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(250)  // tempo and slider drags settle first
            val bpm = p.tempoBpm
            for (s in missing) {
                val source = padSamples.sourceOf(s) ?: continue
                val key = padSamples.keyOf(s, bpm)
                // A copy made while the tempo moved again is freed, never lost on the way.
                val copy = finishOrRelease(Dispatchers.Default, NativeSample::close) { padSamples.computeStretch(source, s, bpm) }
                if (copy == null) AppLog.add("Calage au tempo impossible pour « ${s.name} » (son trop court)")
                padSamples.registerStretch(key, copy)
                if (!isActive) break
            }
            if (project.value.tempoBpm == bpm) padSamples.pushPads(project.value.drums.lanes.map { it.sample }, bpm)
        }
    }

    val padSamples = PadSamples(File(store.folder(projectId), "samples"))
    val sampleInfo: StateFlow<Map<String, SampleInfo>> = padSamples.info
    private val _sampleTask = MutableStateFlow<SampleTask>(SampleTask.Idle)
    val sampleTask: StateFlow<SampleTask> = _sampleTask.asStateFlow()
    private var recorder: MicRecorder? = null

    /** The project's sounds being read after it opened (an export waits for it). */
    private var samplesLoad: kotlinx.coroutines.Job? = null

    private fun loadPadSamples() {
        val files = project.value.sampleFiles
        // « Prise précédente »: kept on disk, not loaded.
        val previousTakes = project.value.previousTakeFiles
        // After a failed load the folder may still hold the user's sounds: never clean it then, nor
        // while a take is being written (its sound file would look unused).
        val cleanUp = _notice.value == null && TakeVault.app.busy.value <= 0
        val folder = store.folder(projectId)
        samplesLoad = viewModelScope.launch {
            // Sounds read while the project closes are freed, not lost on the way.
            val prepared = finishOrRelease(Dispatchers.IO, { list: List<PadSamples.Prepared> -> list.forEach { it.native.close() } }) {
                // Takes waiting to be put on their track keep their sound.
                padSamples.loadAll(files, cleanUp, keep = previousTakes + runCatching { PendingTakes.files(folder) }.getOrElse { emptySet() })
            }.orEmpty()
            prepared.forEach(padSamples::register)
            pushPads()
            pushLive()
            pushAudio()
            project.value.instrumentTracks.forEach { pushSampled(id = it.id) }
        }
    }

    /** Decodes an audio file from the phone and puts it on the selected pad. */
    fun importSample(uri: Uri) = sampleJob(tr("Lecture du fichier…")) {
        val app = getApplication<Application>()
        val name = PadSamples.nameFrom(displayName(app, uri))
        val pcm = try {
            AudioDecoder.decode(app, uri)
        } catch (e: AudioDecoder.TooLongException) {
            throw UserMessage(tr("Ce fichier est trop long : choisis un son plus court."))
        } catch (e: Exception) {
            throw UserMessage(tr("Ce fichier n'a pas pu être lu (%s).", e.message ?: tr("format inconnu")))
        }
        AppLog.add("Import « $name » : ${pcm.channels} canal(aux), ${pcm.sampleRate} Hz, ${"%.1f".format(pcm.seconds)} s")
        padSamples.add(pcm, name)
    }

    /**
     * Resampling: renders [bars] bars of the whole song (as heard, metronome off) and puts the
     * result on the selected pad, like recording the master output.
     */
    fun resampleSong(bars: Int) {
        if (!NativeEngine.isAvailable || _sampleTask.value is SampleTask.Busy || _sampleTask.value is SampleTask.Recording) return
        val p = project.value
        val frames = OfflineExport.framesFor(bars, p.tempoBpm, p.beatsPerBar)
        if (frames > AudioDecoder.MAX_SECONDS * OfflineExport.SAMPLE_RATE) {
            _sampleTask.value = SampleTask.Failed(tr("Trop long à ce tempo : choisis moins de mesures."))
            return
        }
        val job = OfflineExport()
        NativeEngine.captureInto(job) {
            syncEngine()
            NativeEngine.setMetronome(false)
        }
        sampleJob(tr("Enregistrement du morceau…")) {
            val data = try {
                job.renderToArray(frames, 0)
            } finally {
                // Native instruments share reference counts: free them on the main thread only.
                withContext(kotlinx.coroutines.NonCancellable + Dispatchers.Main) { job.close() }
            }
            if (data.isEmpty()) throw UserMessage(tr("Le rendu a été interrompu."))
            AppLog.add("Rééchantillonnage : $bars mesures à ${p.tempoBpm.toInt()} BPM")
            padSamples.add(Pcm(data, 2, OfflineExport.SAMPLE_RATE), if (bars > 1) "Morceau $bars mesures" else "Morceau 1 mesure")  // shown translated (DefaultNames)
        }
    }

    /**
     * A sample pack: the audio files of a folder (by name) go onto the pads from the selected
     * one on, one undo step. Files that cannot be read are skipped.
     */
    fun importFolder(tree: Uri) = importPack(tr("Lecture du dossier…"), tr("Aucun son dans ce dossier (MP3, WAV, FLAC, M4A, OGG).")) { app, max ->
        audioFilesIn(app, tree).take(max)
    }

    /** A sample pack as a .zip: its audio files (any folder inside) go onto the pads in name order. */
    fun importZip(zip: Uri) = importPack(tr("Ouverture du pack…"), tr("Aucun son dans ce .zip (MP3, WAV, FLAC, M4A, OGG).")) { app, max ->
        val dir = File(app.cacheDir, "zip-pack")
        val stream = app.contentResolver.openInputStream(zip) ?: throw UserMessage(tr("Le .zip n'a pas pu être ouvert."))
        try {
            stream.use { ZipPack.extractAudio(it, dir, max, AUDIO_EXTENSIONS) }.map { (file, name) -> Uri.fromFile(file) to name }
        } catch (e: ZipPack.TooBigException) {
            throw UserMessage(tr("Ce pack est trop gros (plus de 256 Mo de sons)."))
        } catch (e: java.util.zip.ZipException) {
            throw UserMessage(tr("Ce fichier n'est pas un .zip lisible."))
        }
    }

    /**
     * Sounds of a pack go onto the pads from the selected one on, one undo step. Files that cannot
     * be read are skipped.
     */
    private fun importPack(busy: String, empty: String, list: (Application, Int) -> List<Pair<Uri, String>>) {
        if (_sampleTask.value is SampleTask.Busy || _sampleTask.value is SampleTask.Recording) return
        val first = _transport.value.selectedPad
        val app = getApplication<Application>()
        _sampleTask.value = SampleTask.Busy(busy)
        viewModelScope.launch {
            try {
                val files = withContext(Dispatchers.IO) { list(app, DrumPattern.PAD_COUNT - first) }
                if (files.isEmpty()) throw UserMessage(empty)
                val added = ArrayList<Pair<Int, PadSample>>()
                files.forEachIndexed { i, (uri, name) ->
                    _sampleTask.value = SampleTask.Busy(tr("Import %1\$d/%2\$d : %3\$s", i + 1, files.size, name))
                    val result = withContext(Dispatchers.IO) {
                        runCatching { padSamples.add(AudioDecoder.decode(app, uri), PadSamples.nameFrom(name)) }.getOrNull()
                    }
                    if (result != null) {
                        padSamples.register(result.second)
                        added += (first + i) to result.first
                    }
                }
                session.edit { p -> p.copy(drums = Slicer.apply(p.drums, added)) }
                pushPads()
                AppLog.add("Pack : ${added.size}/${files.size} sons sur les pads ${first + 1}…")
                _sampleTask.value = if (added.size == files.size) SampleTask.Idle
                else SampleTask.Failed(tr("%d fichier(s) illisible(s) ignoré(s).", files.size - added.size))
            } catch (e: UserMessage) {
                _sampleTask.value = SampleTask.Failed(e.message!!)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _sampleTask.value = SampleTask.Failed(tr("Le pack n'a pas pu être lu (%s).", e.message))
            } finally {
                withContext(kotlinx.coroutines.NonCancellable + Dispatchers.IO) { File(app.cacheDir, "zip-pack").deleteRecursively() }
            }
        }
    }

    private fun audioFilesIn(app: Application, tree: Uri): List<Pair<Uri, String>> {
        val children = android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(
            tree, android.provider.DocumentsContract.getTreeDocumentId(tree),
        )
        val cols = arrayOf(
            android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE,
        )
        val out = ArrayList<Pair<Uri, String>>()
        app.contentResolver.query(children, cols, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val name = c.getString(1) ?: continue
                val mime = c.getString(2) ?: ""
                val audio = mime.startsWith("audio/") || name.substringAfterLast('.').lowercase() in AUDIO_EXTENSIONS
                if (audio) out += android.provider.DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0)) to name
            }
        }
        return out.sortedBy { it.second.lowercase() }
    }

    /** Records the microphone until [stopRecording] (or 30 s) and puts it on the selected pad. */
    fun startRecording() {
        if (_sampleTask.value != SampleTask.Idle && _sampleTask.value !is SampleTask.Failed) return
        val rec = MicRecorder().also { recorder = it }
        _sampleTask.value = SampleTask.Recording(0f, 0f)
        sampleJob(null) {
            val pcm = rec.record { sec -> _sampleTask.value = SampleTask.Recording(sec, rec.level) }
            recorder = null
            if (pcm.frames < 480 || PadSamples.isSilent(pcm)) throw UserMessage(tr("Rien n'a été entendu : rapproche-toi du micro et réessaie."))
            AppLog.add("Enregistrement micro : ${"%.1f".format(pcm.seconds)} s")
            padSamples.add(pcm.withoutTail(), "Micro ${java.time.LocalTime.now().withNano(0)}")  // French, shown translated (DefaultNames)
        }
    }

    fun stopRecording() {
        recorder?.stop()
    }

    /** Back to the kit's own sound for the selected pad (undo brings the sample back). */
    fun removeSample() {
        val lane = _transport.value.selectedPad
        session.edit { p -> p.copy(drums = p.drums.withSample(lane, null)) }
        pushPads()
    }

    /** Changes the selected pad's sound settings; the same [key] in a quick burst is one undo step. */
    fun editPadSample(key: String?, transform: (PadSample) -> PadSample) {
        val lane = _transport.value.selectedPad
        val current = project.value.drums.lanes[lane].sample ?: return
        session.edit(key?.let { "pad-$lane-$it" }) { p -> p.copy(drums = p.drums.withSample(lane, transform(current))) }
        val updated = project.value.drums.lanes[lane].sample ?: return
        if (updated.fitBeats > 0 || current.fitBeats > 0) pushPads()
        else if (NativeEngine.isAvailable) NativeEngine.setPadParams(lane, updated.engineParams())
    }

    // --- Slicing ---------------------------------------------------------------------------

    private val _slicer = MutableStateFlow<SlicerUi?>(null)
    val slicer: StateFlow<SlicerUi?> = _slicer.asStateFlow()
    private var sliceJob: kotlinx.coroutines.Job? = null

    fun openSlicer() {
        val sample = project.value.drums.lanes[_transport.value.selectedPad].sample ?: return
        _slicer.value = SlicerUi(sample.file)
        detectSlices()
    }

    fun closeSlicer() {
        sliceJob?.cancel()
        _slicer.value = null
    }

    fun setSliceMode(mode: SliceMode) {
        val s = _slicer.value ?: return
        _slicer.value = s.copy(mode = mode)
        if (mode == SliceMode.Attacks) detectSlices() else _slicer.value = s.copy(mode = mode, markers = Slicer.grid(s.grid))
    }

    fun setSliceSensitivity(v: Float) {
        val s = _slicer.value ?: return
        _slicer.value = s.copy(sensitivity = v)
        detectSlices()
    }

    fun setSliceGrid(n: Int) {
        val s = _slicer.value ?: return
        _slicer.value = s.copy(grid = n, markers = Slicer.grid(n))
    }

    fun toggleSliceMarker(at: Float) {
        val s = _slicer.value ?: return
        _slicer.value = s.copy(markers = Slicer.toggle(s.markers, at))
    }

    /** Puts one slice per pad, from the selected pad on (one undo step). */
    fun applySlices() {
        val s = _slicer.value ?: return
        val lane = _transport.value.selectedPad
        val source = project.value.drums.lanes[lane].sample?.takeIf { it.file == s.file } ?: return closeSlicer()
        val slices = Slicer.toPads(source, s.markers, lane)
        session.edit { p -> p.copy(drums = Slicer.apply(p.drums, slices)) }
        AppLog.add("Découpe : ${slices.size} tranches sur les pads ${lane + 1} à ${lane + slices.size}")
        pushPads()
        closeSlicer()
    }

    private fun detectSlices() {
        val s = _slicer.value ?: return
        val native = padSamples.native(s.file) ?: return
        sliceJob?.cancel()
        _slicer.value = s.copy(busy = true)
        val maxSlices = DrumPattern.PAD_COUNT - _transport.value.selectedPad
        sliceJob = viewModelScope.launch {
            kotlinx.coroutines.delay(150)  // a slider drag computes once, at the end
            val onsets = withContext(Dispatchers.Default) { native.onsets(s.sensitivity, maxSlices) }
            val now = _slicer.value
            if (now != null && now.file == s.file && now.mode == SliceMode.Attacks) {
                _slicer.value = now.copy(markers = onsets.toList(), busy = false)
            }
        }
    }

    fun dismissSampleError() {
        if (_sampleTask.value is SampleTask.Failed) _sampleTask.value = SampleTask.Idle
    }

    private class UserMessage(message: String) : Exception(message)

    /**
     * Runs [work] off the main thread and puts the sound it makes on the selected drum pad,
     * or on live pad [live] (looping every [beats] beats, or as long as the sound lasts).
     */
    private fun sampleJob(busy: String?, live: Int? = null, beats: Int? = null, work: suspend () -> Pair<PadSample, PadSamples.Prepared>) {
        val lane = _transport.value.selectedPad
        if (busy != null) _sampleTask.value = SampleTask.Busy(busy)
        viewModelScope.launch {
            try {
                val (sample, prepared) = withContext(Dispatchers.IO) { work() }
                padSamples.register(prepared)
                if (live != null) {
                    val clip = LiveClip(sample.file, sample.name, beats ?: LivePads.beatsFor(prepared.info.seconds, project.value.tempoBpm))
                    session.edit { p -> p.copy(live = p.live.with(live, clip)) }
                    pushLive()
                    _sampleTask.value = SampleTask.Idle
                    return@launch
                }
                session.edit { p -> p.copy(drums = p.drums.withSample(lane, sample)) }
                pushPads()
                _sampleTask.value = SampleTask.Idle
                if (NativeEngine.isAvailable) NativeEngine.triggerPad(lane, 0.9f)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: OutOfMemoryError) {
                recorder = null
                AppLog.add("Son du pad : mémoire insuffisante")
                _sampleTask.value = SampleTask.Failed(tr("Pas assez de mémoire pour ce son : essaie plus court."))
            } catch (e: Exception) {
                recorder = null
                AppLog.add("Son du pad : échec (${e.message})")
                _sampleTask.value = SampleTask.Failed(
                    if (e is UserMessage) e.message!! else tr("Ça n'a pas marché : %s", e.message ?: e.javaClass.simpleName),
                )
            }
        }
    }

    /** The file's name as the phone shows it; for plain file links, the last part of the path. */
    private fun displayName(app: Application, uri: Uri): String? = runCatching {
        app.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull() ?: uri.lastPathSegment

    // --- Mixer -----------------------------------------------------------------------------

    private val _meters = MutableStateFlow(FloatArray(NativeEngine.MIXER_CHANNELS + 1))
    /** Peak per mixer channel (the master's at [MASTER]), then the limiter gain. */
    val meters: StateFlow<FloatArray> = _meters.asStateFlow()

    /**
     * True while the audio thread has almost no time to spare (too many heavy effects at once):
     * the effects box says so. Polled twice a second, only while a box is on screen.
     */
    val cpuBusy: StateFlow<Boolean> = kotlinx.coroutines.flow.flow {
        if (!NativeEngine.isAvailable) {
            emit(false)
            return@flow
        }
        var busy = false
        while (true) {
            busy = com.pocketstudio.app.mixer.cpuBusyNext(busy, NativeEngine.cpuLoad())
            emit(busy)
            kotlinx.coroutines.delay(500)
        }
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(1000), false)

    /** Each scene's section name, by scene ("" for its letter alone): the effects box's « Joue dans ». */
    val sceneLabels: StateFlow<List<String>> = project.map { p -> p.allScenes.map { it.label } }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, project.value.allScenes.map { it.label })

    /** Who reads the meters now: of the screens watching them (the mixer, an effects box), one polls. */
    private var meterPoller: Any? = null
    private val meterPeaks = FloatArray(NativeEngine.MIXER_CHANNELS + 1)
    private val meterFresh = FloatArray(NativeEngine.MIXER_CHANNELS + 1)

    /**
     * Polls meter peaks about 20 times a second while the mixer or an effects box is visible.
     * Several watchers at once (a box inside the mixer): one of them reads, the others wait and
     * take over when it goes (all on the main thread).
     */
    suspend fun pollMeters() {
        if (!NativeEngine.isAvailable) return
        val n = NativeEngine.MIXER_CHANNELS
        val me = Any()
        try {
            while (true) {
                if (meterPoller == null) meterPoller = me
                if (meterPoller === me) {
                    NativeEngine.mixerMeters(meterFresh)
                    // Published only when a bar moves: stopped, the mixer is left alone.
                    if (com.pocketstudio.app.mixer.holdMeterPeaks(meterPeaks, meterFresh)) _meters.value = meterPeaks.copyOf()
                }
                kotlinx.coroutines.delay(50)
            }
        } finally {
            if (meterPoller === me) meterPoller = null
        }
    }

    private val _spectrum = MutableStateFlow<FloatArray?>(null)
    /** Master output spectrum (dB per band) while the analyser is open. */
    val spectrum: StateFlow<FloatArray?> = _spectrum.asStateFlow()

    /** Spectrum of the output about 15 times a second while the analyser is shown. */
    suspend fun pollSpectrum() {
        if (!NativeEngine.isAvailable) return
        val buf = FloatArray(NativeEngine.SCOPE_FRAMES)
        var smooth: FloatArray? = null
        try {
            while (true) {
                NativeEngine.readScope(false, buf)
                val fresh = withContext(Dispatchers.Default) { com.pocketstudio.app.analysis.Spectrum.bands(buf, OfflineExport.SAMPLE_RATE) }
                // Bars fall gently, rise at once.
                smooth = smooth?.let { old -> FloatArray(fresh.size) { maxOf(fresh[it], old[it] - 3f) } } ?: fresh
                _spectrum.value = smooth
                kotlinx.coroutines.delay(66)
            }
        } finally {
            _spectrum.value = null
        }
    }

    private val micThread = java.util.concurrent.Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val _tuner = MutableStateFlow<com.pocketstudio.app.analysis.TunerReading?>(null)
    val tuner: StateFlow<com.pocketstudio.app.analysis.TunerReading?> = _tuner.asStateFlow()
    private val _tunerMic = MutableStateFlow<Boolean?>(null)
    /** null while opening, false when the microphone is unavailable. */
    val tunerMic: StateFlow<Boolean?> = _tunerMic.asStateFlow()

    /** Listens to the microphone while the tuner is shown (needs the permission). */
    suspend fun runTuner() {
        if (!NativeEngine.isAvailable || _audioRecording.value != null) {
            _tunerMic.value = false
            return
        }
        _tunerMic.value = null
        var open = false
        val buf = FloatArray(NativeEngine.SCOPE_FRAMES)
        try {
            // Opened and closed on one thread, in order: leaving and coming back never closes the new one.
            open = withContext(micThread + kotlinx.coroutines.NonCancellable) { NativeEngine.micOpen() }
            _tunerMic.value = open
            if (!open) return
            while (true) {
                NativeEngine.readScope(true, buf)
                val reading = withContext(Dispatchers.Default) { com.pocketstudio.app.analysis.PitchDetector.detect(buf, OfflineExport.SAMPLE_RATE) }
                // Hold the last note a moment when the sound fades, so the needle does not vanish at once.
                if (reading != null) _tuner.value = reading
                kotlinx.coroutines.delay(80)
            }
        } finally {
            _tuner.value = null
            // Always let go: the engine counts its listeners, so a take that started meanwhile keeps the microphone.
            if (open) withContext(micThread + kotlinx.coroutines.NonCancellable) { NativeEngine.micClose() }
        }
    }

    fun setChannelVolume(ch: Int, v: Float) {
        project.value.mixer.channels.getOrNull(ch)?.let { takeGesture(AutoTarget(AutoTarget.VOLUME, ch), v, it.volume) }
        editChannel(ch, "vol") { it.copy(volume = v) }
        recordAutomation(AutoTarget(AutoTarget.VOLUME, ch), v)
    }

    fun setChannelPan(ch: Int, pan: Float) {
        project.value.mixer.channels.getOrNull(ch)?.let { takeGesture(AutoTarget(AutoTarget.PAN, ch), (pan + 1f) / 2f, (it.pan + 1f) / 2f) }
        editChannel(ch, "pan") { it.copy(pan = pan) }
        recordAutomation(AutoTarget(AutoTarget.PAN, ch), (pan + 1f) / 2f)
    }
    fun toggleMute(ch: Int) = editChannel(ch, null) { it.copy(mute = !it.mute) }
    fun toggleSolo(ch: Int) = editChannel(ch, null) { it.copy(solo = !it.solo) }

    private fun editChannel(ch: Int, key: String?, transform: (com.pocketstudio.model.Channel) -> com.pocketstudio.model.Channel) {
        session.edit(key?.let { "mix-$ch-$it" }) { p ->
            val channels = p.mixer.channels.toMutableList()
            channels[ch] = transform(channels[ch])
            p.copy(mixer = p.mixer.copy(channels = channels))
        }
        pushMixer(project.value)
    }

    private fun pushMixer(p: Project = project.value) {
        if (!NativeEngine.isAvailable) return
        // Only the strips in use: the engine's command queue has room for whole projects.
        val pads = (0 until DrumPattern.PAD_COUNT).filter { p.mixer.channels.getOrNull(Mixer.padChannel(it))?.hasOwnFx() == true }
        val used = usedStrips(p)
        // First the strips of tracks gone (deleted, undone, another project): their effects go in
        // one command, giving their echo, reverb and tape stop memory back before the strips in
        // use ask for it (else an effect there would stay silent live while the export plays it).
        // No solo or mute may linger in the engine either, where a solo would silence the rest.
        stripsToClear(p).forEach { i ->
            NativeEngine.clearInserts(i)
            if (i < Mixer.FIRST_PAD) {
                NativeEngine.setChannelMute(i, false)
                NativeEngine.setChannelSolo(i, false)
            }
        }
        p.mixer.channels.take(NativeEngine.MIXER_CHANNELS).forEachIndexed { i, c ->
            // The master's index (its settings are below) and the strips just emptied.
            if (i == MASTER || i !in used) return@forEachIndexed
            NativeEngine.setChannelVolume(i, c.volume)
            NativeEngine.setChannelPan(i, c.pan)
            NativeEngine.setChannelMute(i, c.mute || (!NativeEngine.isCapturing && i in liveMutedChannels()))
            NativeEngine.setChannelSolo(i, c.solo)
            pushInserts(i, c.inserts)
            c.sends.forEachIndexed { bus, level -> NativeEngine.setSend(i, bus, level) }
            c.macros.forEachIndexed { m, v -> NativeEngine.setMacro(i, m, v) }
        }
        pushInserts(MASTER, p.mixer.master)
        NativeEngine.setMasterGain(p.mixer.masterGain)
        p.mixer.buses.forEachIndexed { bus, params -> params.forEachIndexed { k, v -> NativeEngine.setBusParam(bus, k, v) } }
        // Pads with effects of their own go through their strip, the others straight into the drums.
        (0 until DrumPattern.PAD_COUNT).forEach { NativeEngine.setPadStrip(it, it in pads) }
    }

    /** After a change to a pad's strip: it plays through it only while something there changes the sound. */
    private fun routePad(ch: Int) {
        if (ch < Mixer.FIRST_PAD || !NativeEngine.isAvailable) return
        val c = project.value.mixer.channels.getOrNull(ch) ?: return
        if (c.hasOwnFx()) {
            // Its whole strip first (a strip left from another project must not linger).
            NativeEngine.setChannelVolume(ch, c.volume)
            NativeEngine.setChannelPan(ch, c.pan)
            pushInserts(ch, c.inserts)
            c.sends.forEachIndexed { bus, level -> NativeEngine.setSend(ch, bus, level) }
            c.macros.forEachIndexed { m, v -> NativeEngine.setMacro(ch, m, v) }
        }
        NativeEngine.setPadStrip(ch - Mixer.FIRST_PAD, c.hasOwnFx())
    }

    private fun pushInserts(ch: Int, inserts: List<Insert>) {
        insertCommands(inserts, { slot, type -> NativeEngine.setInsertType(ch, slot, type) }) { slot, k, v -> NativeEngine.setInsertParam(ch, slot, k, v) }
        pushInsertScenes(ch, inserts)
    }

    /**
     * « Joue dans »: whether each effect of [ch] plays in the scene the engine plays now, and in
     * the scene waiting to take over (its copy in the engine was sent before this change).
     */
    private fun pushInsertScenes(ch: Int, inserts: List<Insert>) {
        // The scene this strip's track plays (on the live grid, its own; stopped: left as it is).
        sceneOfSlot(project.value, if (ch == MASTER) null else liveSlotOfChannel(ch))?.let { scene ->
            insertOnCommands(inserts, scene) { slot, on -> NativeEngine.setInsertOn(ch, slot, on) }
        }
        val waiting = _sessionUi.value.pending
        if (waiting != null && _transport.value.playing && !NativeEngine.isCapturing && inserts.any { it.heardType != 0 }) {
            NativeEngine.setStaging(true)
            insertOnCommands(inserts, waiting) { slot, on -> NativeEngine.setInsertOn(ch, slot, on) }
            NativeEngine.setStaging(false)
        }
    }

    /** « Joue dans »: effect [slot] of strip [ch] plays only in [scenes] (null: in every scene). One undo step. */
    fun setInsertScenes(ch: Int, slot: Int, scenes: Set<Int>?) {
        if (ch !in 0 until Mixer.CHANNELS) return
        session.edit { it.withInsertScenes(ch, slot, scenes) }
        if (NativeEngine.isAvailable) pushInsertScenes(ch, project.value.insertsOf(ch))
    }

    // --- Effects (inserts on channels and master, sends, buses) ------------------------------

    /** [ch] 0..3 = a channel, [MASTER] = the master. A new type starts from its defaults. */
    fun setInsertType(ch: Int, slot: Int, type: EffectType) = editInserts(ch, null) { list ->
        list.mapIndexed { i, ins -> if (i == slot) Insert.of(type) else ins }
    }

    /** An effect switched off (kept with its settings) or on again (one undo step). */
    fun setInsertBypass(ch: Int, slot: Int, bypass: Boolean) = editInserts(ch, null) { list ->
        list.mapIndexed { i, ins -> if (i == slot && ins.effect != EffectType.NONE) ins.copy(bypass = bypass) else ins }
    }

    /** An effect moved up or down the chain: slots [from] and [to] swap, their automation with them. */
    fun moveInsert(ch: Int, from: Int, to: Int) {
        if (ch !in 0 until Mixer.CHANNELS || from == to) return
        session.edit { p -> p.withInsertsSwapped(ch, from, to) }
        pushAutomation()
        if (NativeEngine.isAvailable) pushInserts(ch, project.value.insertsOf(ch))
        if (ch != MASTER) routePad(ch)
    }

    // Sliders send only the value that moved (the whole mixer is over a thousand commands).
    fun setInsertParam(ch: Int, slot: Int, param: Int, value: Float) {
        val v = value.coerceIn(0f, 1f)
        // Played during a take (the Wobble pad, « Effets… », a MIDI knob): written into the song.
        project.value.insertsOf(ch).getOrNull(slot)?.params?.getOrNull(param)?.let { takeGesture(AutoTarget(AutoTarget.INSERT_PARAM, ch, slot, param), v, it) }
        editInserts(ch, "fx-$slot-$param", push = false) { list ->
            list.mapIndexed { i, ins -> if (i == slot) ins.copy(params = ins.params.mapIndexed { k, old -> if (k == param) v else old }) else ins }
        }
        if (NativeEngine.isAvailable) NativeEngine.setInsertParam(ch, slot, param, v)
        recordAutomation(AutoTarget(AutoTarget.INSERT_PARAM, ch, slot, param), v)
    }

    // --- Quick sound play: macros, pad XY, ready-made effects -------------------------------

    /** One quick control of a track (a knob drag is one undo step). */
    fun setMacro(ch: Int, macro: Int, value: Float) = setMacros(ch, mapOf(macro to value), "macro-$ch-$macro")

    /** The pad XY: two macros at once (one drag, one undo step). */
    fun setMacroPair(ch: Int, x: Int, xValue: Float, y: Int, yValue: Float) = setMacros(ch, mapOf(x to xValue, y to yValue), "macro-xy-$ch")

    private fun setMacros(ch: Int, values: Map<Int, Float>, key: String) {
        if (ch !in 0 until Mixer.CHANNELS || ch == MASTER) return
        project.value.mixer.channels.getOrNull(ch)?.let { c ->
            values.forEach { (m, v) -> takeGesture(AutoTarget.macro(ch, m), v.coerceIn(0f, 1f), c.macros.getOrElse(m) { 0f }) }
        }
        session.edit(key) { p ->
            val channels = p.mixer.channels.toMutableList()
            channels[ch] = channels[ch].let { c -> c.copy(macros = Macros.sanitized(c.macros.mapIndexed { m, old -> values[m] ?: old })) }
            p.copy(mixer = p.mixer.copy(channels = channels))
        }
        if (NativeEngine.isAvailable) values.keys.forEach { m -> NativeEngine.setMacro(ch, m, project.value.mixer.channels[ch].macros[m]) }
        routePad(ch)
    }

    /** A ready-made effect on a track, in one tap (one undo step). */
    fun applyFx(ch: Int, presetId: String) {
        val preset = FxPresets.byId(presetId) ?: return
        if (ch !in 0 until Mixer.CHANNELS || ch == MASTER) return
        val lanes = project.value.automation.size
        val edit = { p: Project ->
            val channels = p.mixer.channels.toMutableList()
            channels[ch] = channels[ch].withFx(preset)
            p.copy(mixer = p.mixer.copy(channels = channels)).withoutStaleInsertLanes(p)
        }
        if (longFxRefused(edit(project.value))) return
        session.edit(transform = edit)
        if (project.value.automation.size != lanes) pushAutomation()
        if (NativeEngine.isAvailable) {
            val c = project.value.mixer.channels[ch]
            pushInserts(ch, c.inserts)
            c.sends.forEachIndexed { bus, level -> NativeEngine.setSend(ch, bus, level) }
            c.macros.forEachIndexed { m, v -> NativeEngine.setMacro(ch, m, v) }
        }
        AppLog.add("Effet « ${preset.name} » sur la voie $ch")
        routePad(ch)
    }

    private val _auditioning = MutableStateFlow<Pair<Int, String>?>(null)
    /** The ready-made effect heard on a strip while a finger holds its chip (strip, preset id). */
    val auditioning: StateFlow<Pair<Int, String>?> = _auditioning.asStateFlow()

    /**
     * Hold-to-listen: ready-made effect [presetId] heard on strip [ch] as if chosen, nothing
     * written (no undo step, the project unchanged); null: the strip back to what is saved. Refused
     * (said) when it would need more long effects than the engine holds.
     */
    fun auditionFx(ch: Int, presetId: String?) {
        if (ch !in 0 until Mixer.CHANNELS || ch == MASTER) return
        val saved = project.value.mixer.channels.getOrNull(ch) ?: return
        val preset = presetId?.let { FxPresets.byId(it) ?: return }
        val heard = if (preset == null) saved else saved.withFx(preset)
        if (preset != null) {
            val channels = project.value.mixer.channels.toMutableList().also { it[ch] = heard }
            project.value.mixer.copy(channels = channels).longFxOverLimit(project.value.mixer)?.let {
                _notice.value = longFxLimitNotice(it)
                return
            }
        } else if (_auditioning.value?.first != ch) {
            return
        }
        _auditioning.value = preset?.let { ch to it.id }
        if (!NativeEngine.isAvailable) return
        if (ch >= Mixer.FIRST_PAD && !heard.hasOwnFx()) {
            NativeEngine.setPadStrip(ch - Mixer.FIRST_PAD, false)
            return
        }
        pushInserts(ch, heard.inserts)
        heard.sends.forEachIndexed { bus, level -> NativeEngine.setSend(ch, bus, level) }
        heard.macros.forEachIndexed { m, v -> NativeEngine.setMacro(ch, m, v) }
        if (ch >= Mixer.FIRST_PAD) {
            NativeEngine.setChannelVolume(ch, heard.volume)
            NativeEngine.setChannelPan(ch, heard.pan)
            NativeEngine.setPadStrip(ch - Mixer.FIRST_PAD, true)
        }
    }

    fun setSend(ch: Int, bus: Int, level: Float) {
        val v = level.coerceIn(0f, 1f)
        takeGesture(AutoTarget(AutoTarget.SEND, ch, bus), v, project.value.mixer.channels.getOrNull(ch)?.sends?.getOrNull(bus) ?: 0f)
        session.edit("mix-$ch-send-$bus") { p ->
            val channels = p.mixer.channels.toMutableList()
            channels[ch] = channels[ch].let { c -> c.copy(sends = c.sends.mapIndexed { b, old -> if (b == bus) v else old }) }
            p.copy(mixer = p.mixer.copy(channels = channels))
        }
        if (NativeEngine.isAvailable) NativeEngine.setSend(ch, bus, v)
        routePad(ch)
        recordAutomation(AutoTarget(AutoTarget.SEND, ch, bus), v)
    }

    fun setBusParam(bus: Int, param: Int, value: Float) {
        val v = value.coerceIn(0f, 1f)
        session.edit("bus-$bus-$param") { p ->
            val buses = p.mixer.buses.mapIndexed { b, ps -> if (b == bus) ps.mapIndexed { k, old -> if (k == param) v else old } else ps }
            p.copy(mixer = p.mixer.copy(buses = buses))
        }
        if (NativeEngine.isAvailable) NativeEngine.setBusParam(bus, param, v)
        recordAutomation(AutoTarget(AutoTarget.BUS_PARAM, 0, bus, param), v)
    }

    private fun editInserts(ch: Int, key: String?, push: Boolean = true, transform: (List<Insert>) -> List<Insert>) {
        val lanes = project.value.automation.size
        val edit = { p: Project ->
            val edited = if (ch == MASTER) {
                p.copy(mixer = p.mixer.copy(master = transform(p.mixer.master)))
            } else {
                val channels = p.mixer.channels.toMutableList()
                channels[ch] = channels[ch].let { c -> c.copy(inserts = transform(c.inserts)) }
                p.copy(mixer = p.mixer.copy(channels = channels))
            }
            edited.withoutStaleInsertLanes(p)
        }
        // An effect added or switched on past what the engine holds: refused, said (never silent).
        if (longFxRefused(edit(project.value))) return
        session.edit(key?.let { "fx-$ch-$it" }, transform = edit)
        if (project.value.automation.size != lanes) pushAutomation()
        if (push && NativeEngine.isAvailable) {
            pushInserts(ch, if (ch == MASTER) project.value.mixer.master else project.value.mixer.channels[ch].inserts)
        }
        if (ch != MASTER) routePad(ch)
    }

    // --- Subtractive synth -------------------------------------------------------------------

    private val _keyboardOctave = MutableStateFlow(4)
    val keyboardOctave: StateFlow<Int> = _keyboardOctave.asStateFlow()

    fun shiftKeyboardOctave(delta: Int) {
        _keyboardOctave.value = (_keyboardOctave.value + delta).coerceIn(1, 7)
    }

    /** Keys down on the keyboard, the track each one plays (released there, whatever is selected since). */
    private val keysDown = HeldKeys()

    fun synthNoteOn(note: Int, velocity: Float = 0.85f) {
        val press = keysDown.press(note, track)
        if (NativeEngine.isAvailable) NativeEngine.synthNoteOn(note, velocity, press.track)
        // A note another chord already holds sounds again: the take ends it there and starts it anew.
        if (press.alreadyHeld) recordNoteOff(note)
        recordNoteOn(note, velocity)
    }

    fun synthNoteOff(note: Int) {
        // Still held by another key (overlapping chords): it keeps sounding.
        val on = keysDown.release(note, track) ?: return
        if (NativeEngine.isAvailable) NativeEngine.synthNoteOff(note, on)
        recordNoteOff(note)
    }

    // --- Live recording: what is played (keyboard, MIDI, pads) is written into the loop --------

    private val _recording = MutableStateFlow(false)
    /** True while armed: notes go into the piano roll, pad hits into the drum steps. */
    val recording: StateFlow<Boolean> = _recording.asStateFlow()
    private var recordBefore: Project? = null
    /** Undo steps when the take started: an edit made during it folds into the take's step. */
    private var recordDepth = 0
    private val heldNotes = HashMap<Int, Triple<Int, Float, Int>>()  // pitch -> (start tick, velocity, track)

    /** Starts (with a bar of count-in when stopped) or ends a take; the whole take is one undo step. */
    /**
     * ● on the top bar. [notes]: armed from the synth, where a phrase is played on the keyboard (a
     * one-bar loop grows to four bars first, see [TakeLoop]).
     */
    fun toggleRecord(notes: Boolean = false) {
        if (_recording.value) {
            stopRecordingTake()
            return
        }
        // A microphone take is on: the notes wait (one recording at a time).
        if (_audioRecording.value != null) return
        _takeLoopGrown.value = false
        _takeLoopSong.value = false
        if (notes) {
            // Its own undo step, before the take's: « Annuler » after the take gives the bar back.
            com.pocketstudio.app.synth.TakeLoop.grown(currentTrack.notes)?.let { grown ->
                val before = project.value
                editNotes { grown }
                _takeLoopGrown.value = true
                // A longer scene makes its song parts longer too: said, never silently.
                _takeLoopSong.value = before.song.any { it.scene == before.scene } &&
                    project.value.sceneSteps(before.scene) != before.sceneSteps(before.scene)
            }
        }
        recordBefore = project.value
        recordDepth = session.undoDepth
        heldNotes.clear()
        _recording.value = true
        AppLog.add("Enregistrement en direct")
        if (!_transport.value.playing) {
            _transport.value = _transport.value.copy(playing = true)
            if (NativeEngine.isAvailable) {
                applySongCurves(project.value, project.value.scene)
                NativeEngine.play(com.pocketstudio.app.settings.SettingsStore.state.value.recordCountIn, aloneSongStep())
            }
            PlaybackService.start(getApplication())
        }
    }

    private val _takeLoopGrown = MutableStateFlow(false)
    /** The last ● on the synth grew a one-bar note loop to four bars (said under the top bar until dismissed). */
    val takeLoopGrown: StateFlow<Boolean> = _takeLoopGrown.asStateFlow()
    private val _takeLoopSong = MutableStateFlow(false)
    /** That longer loop also made the song's parts of this scene longer (said in the same notice). */
    val takeLoopSong: StateFlow<Boolean> = _takeLoopSong.asStateFlow()
    fun dismissTakeLoop() {
        _takeLoopGrown.value = false
        _takeLoopSong.value = false
    }

    private fun stopRecordingTake() {
        // Keys still down when the take ends keep the length they have so far.
        heldNotes.keys.toList().forEach { recordNoteOff(it) }
        _recording.value = false
        recordBefore?.let { session.commitQuietEdits(it, recordDepth) }
        recordBefore = null
    }

    /** Where the playhead is in [loopTicks], or null when nothing is recorded (not armed, stopped, counting in). */
    private fun recordTick(loopTicks: Int): Int? {
        if (!_recording.value || !NativeEngine.isAvailable || !_transport.value.playing) return null
        // What is heard lags what is rendered by the output latency (much more over Bluetooth).
        val latencyMs = Metrics.streamInfo.value.outputLatencyMs.takeIf { it > 0 } ?: 0.0
        val pos = NativeEngine.playPosition() - latencyMs / 1000.0 * project.value.tempoBpm / 15.0
        // Before the first step (end of the count-in, or the latency pulling a note played on the
        // "1" just before it): up to a sixteenth early lands on the "1", earlier is not recorded.
        if (pos < 0) return if (pos >= -EARLY_STEPS) 0 else null
        return Math.floorMod((pos * NoteClip.TICKS_PER_STEP).toLong(), loopTicks.toLong()).toInt()
    }

    /** A note played live (keyboard or MIDI) while recording: remembered until released. */
    fun recordNoteOn(pitch: Int, velocity: Float) {
        val tick = recordTick(currentTrack.notes.ticks) ?: return
        heldNotes[pitch] = Triple(tick, velocity, track)
    }

    fun recordNoteOff(pitch: Int) {
        // Written on the track it started on, even if another one was selected meanwhile.
        val (start, velocity, id) = heldNotes.remove(pitch) ?: return
        if (project.value.instrumentTracks.none { it.id == id }) return
        val clip = project.value.track(id).notes
        val end = recordTick(clip.ticks) ?: start + NoteClip.TICKS_PER_STEP
        val step = NoteClip.TICKS_PER_STEP
        // Starts land on the sixteenth grid; the length is kept (at least half a sixteenth).
        val snapped = Math.floorMod(Math.round(start.toDouble() / step).toInt() * step, clip.ticks)
        val length = maxOf(step / 2, Math.floorMod(end - start, clip.ticks))
        val note = Note(snapped, minOf(length, NoteClip.MAX_TICKS), pitch, velocity)
        session.editQuietly { p ->
            val t = p.track(id)
            p.withTrack(t.copy(notes = t.notes.copy(notes = t.notes.notes.filterNot { it.start == snapped && it.pitch == pitch } + note).sanitized()))
        }
        pushNotes(project.value.track(id).notes, id)
    }

    /** A pad hit while recording becomes a step of the drum loop (nearest sixteenth). */
    private fun recordPad(lane: Int, velocity: Float) {
        val steps = project.value.drums.steps
        val tick = recordTick(steps * NoteClip.TICKS_PER_STEP) ?: return
        val step = Math.floorMod(Math.round(tick.toDouble() / NoteClip.TICKS_PER_STEP).toInt(), steps)
        session.editQuietly { p ->
            val l = p.drums.lanes[lane]
            p.copy(drums = p.drums.copy(lanes = p.drums.lanes.toMutableList().also { it[lane] = l.copy(hits = l.hits + (step to Hit(velocity))) }))
        }
        pushLane(lane)
    }

    /** MIDI notes arrive here too (they already sound): recorded when armed. */
    fun midiNote(channel: Int, pitch: Int, velocity: Float, on: Boolean) {
        if (channel == 9) {
            if (on && pitch in 36 until 36 + DrumPattern.PAD_COUNT) recordPad(pitch - 36, velocity)
        } else if (on) {
            recordNoteOn(pitch, velocity)
        } else {
            recordNoteOff(pitch)
        }
    }

    /** Safety net: releases every synth voice (leaving the synth tab or the app). */
    fun synthAllOff() {
        keysDown.clear()
        if (NativeEngine.isAvailable) NativeEngine.synthAllOff()
    }

    fun setSynthParam(index: Int, value: Float) {
        val id = track
        editTrack("synth-$index-$id", id) { it.copy(synth = it.synth.with(index, value)) }
        if (NativeEngine.isAvailable) NativeEngine.setSynthParam(index, project.value.track(id).synth.get(index), id)
    }

    fun applySynthPreset(patch: SynthPatch) {
        editTrack { it.copy(synth = patch) }
        pushSynth(currentTrack.synth)
    }

    // --- Sound library: built-in sounds, favourites, random ---------------------------------------

    private val favoritesFile = com.pocketstudio.model.FavoritesFile(File(app.filesDir, "favorites.json"))
    private val _favorites = MutableStateFlow(
        favoritesFile.load().let { loaded ->
            // Unreadable: set aside, never written over by the next favourite; and said.
            loaded.setAside?.let { aside ->
                AppLog.add("Favoris illisibles, mis de côté : $aside")
                _notice.value = tr("Tes favoris n'ont pas pu être relus. Le fichier est mis de côté (%s) et une nouvelle liste commence.", aside)
            }
            if (!favoritesFile.writable) {
                AppLog.add("Favoris illisibles, pas mis de côté")
                _notice.value = tr("Tes favoris n'ont pas pu être relus : le fichier est gardé tel quel et les nouveaux favoris ne seront pas enregistrés.")
            }
            loaded.favorites
        },
    )
    /** The user's own sounds, shared by every project. */
    val favorites: StateFlow<Favorites> = _favorites.asStateFlow()

    private val favoritesWriter = kotlinx.coroutines.sync.Mutex()
    /** The favourites before the last delete, for "Annuler". */
    private var favoritesBeforeDelete: Favorites? = null

    private fun editFavorites(transform: (Favorites) -> Favorites) {
        val next = transform(_favorites.value).sanitized()
        _favorites.value = next
        viewModelScope.launch(Dispatchers.IO) {
            // One write at a time, always of the latest list: an older one can never land last.
            favoritesWriter.withLock {
                runCatching { favoritesFile.save(_favorites.value) }.onFailure { AppLog.add("Favoris non enregistrés : ${it.message}") }
            }
        }
    }

    private fun <T> addFavorite(list: List<T>, add: () -> Favorites) {
        if (list.size >= Favorites.MAX) {
            _notice.value = tr("Tu as déjà %d favoris de ce son : supprime-en un pour en garder un nouveau.", Favorites.MAX)
            return
        }
        editFavorites { add() }
    }

    fun saveSynthFavorite() = _favorites.value.let { f ->
        addFavorite(f.synth) { f.copy(synth = f.synth + NamedSound(Favorites.nextName(f.synth.map { it.name }), SoundCategories.FAVORITES, currentTrack.synth.sanitized())) }
    }
    fun savePlaitsFavorite() = _favorites.value.let { f ->
        addFavorite(f.plaits) { f.copy(plaits = f.plaits + NamedSound(Favorites.nextName(f.plaits.map { it.name }), SoundCategories.FAVORITES, currentTrack.plaits)) }
    }
    fun saveAcidFavorite() = _favorites.value.let { f ->
        addFavorite(f.acid) { f.copy(acid = f.acid + NamedSound(Favorites.nextName(f.acid.map { it.name }), SoundCategories.FAVORITES, project.value.acid.sound)) }
    }

    private fun deleteFavorite(name: String, transform: (Favorites) -> Favorites) {
        favoritesBeforeDelete = _favorites.value
        editFavorites(transform)
        _notice.value = tr("Favori « %s » supprimé.", name)
    }
    fun deleteSynthFavorite(i: Int) = _favorites.value.synth.getOrNull(i)?.let { deleteFavorite(it.name) { f -> f.copy(synth = f.synth.filterIndexed { k, _ -> k != i }) } }
    fun deletePlaitsFavorite(i: Int) = _favorites.value.plaits.getOrNull(i)?.let { deleteFavorite(it.name) { f -> f.copy(plaits = f.plaits.filterIndexed { k, _ -> k != i }) } }
    fun deleteAcidFavorite(i: Int) = _favorites.value.acid.getOrNull(i)?.let { deleteFavorite(it.name) { f -> f.copy(acid = f.acid.filterIndexed { k, _ -> k != i }) } }
    /** Brings back the favourites as they were before the last delete. */
    fun undoFavoriteDelete() {
        val before = favoritesBeforeDelete ?: return
        favoritesBeforeDelete = null
        editFavorites { before }
        _notice.value = null
    }
    val canUndoFavoriteDelete get() = favoritesBeforeDelete != null

    fun randomSynth() = applySynthPreset(SoundLibrary.randomSynth())

    fun applyPlaits(patch: PlaitsPatch) {
        editTrack { it.copy(plaits = patch.sanitized()) }
        pushPlaits()
        if (NativeEngine.isAvailable && !_transport.value.playing) {
            viewModelScope.launch {
                kotlinx.coroutines.delay(30)
                previewNote(60)  // hear it right away
            }
        }
    }

    fun randomPlaits() = applyPlaits(SoundLibrary.randomPlaits().copy(level = currentTrack.plaits.level))

    fun applyAcidSound(sound: AcidSound) {
        session.edit { p -> p.copy(acid = p.acid.copy(sound = sound.sanitized())) }
        if (NativeEngine.isAvailable) project.value.acid.sound.values().forEachIndexed { i, v -> NativeEngine.setAcidParam(i, v) }
    }

    /** New character, same tuning and volume. */
    fun randomAcid() = applyAcidSound(SoundLibrary.randomAcid(keep = project.value.acid.sound))

    private fun pushSynth(patch: SynthPatch, id: Int = track) {
        if (!NativeEngine.isAvailable) return
        patch.sanitized().values.forEachIndexed { i, v -> NativeEngine.setSynthParam(i, v, id) }
    }

    // --- Acid bass -------------------------------------------------------------------------

    private val _selectedAcidStep = MutableStateFlow(0)
    val selectedAcidStep: StateFlow<Int> = _selectedAcidStep.asStateFlow()

    fun selectAcidStep(step: Int) {
        _selectedAcidStep.value = step
    }

    /** New random line (locked steps kept). */
    /** Pattern length in bars (1 bar = 16 steps): the end of a longer line is kept when shortening. */
    fun setAcidBars(bars: Int) {
        session.edit { p -> p.copy(acid = p.acid.copy(length = bars * AcidPattern.STEPS_PER_PAGE).sanitized()) }
        if (_selectedAcidStep.value >= project.value.acid.length) _selectedAcidStep.value = 0
        pushAcid(project.value.acid)
    }

    /** Copies bar [from] onto bar [to] (0-based), the usual way to build a longer line. */
    fun copyAcidPage(from: Int, to: Int) {
        session.edit { p -> p.copy(acid = p.acid.copyPage(from, to)) }
        pushAcid(project.value.acid)
    }

    fun generateAcid() {
        // In the song's key (and with its style's feel), as the assistant's generator: no wrong note.
        session.edit { it.copy(acid = com.pocketstudio.model.WizardAcid.generateFor(it)) }
        pushAcid(project.value.acid)
    }

    fun editAcidStep(transform: (AcidStep) -> AcidStep) {
        val index = _selectedAcidStep.value
        session.edit { p -> p.copy(acid = p.acid.withStep(index, transform(p.acid.steps[index]))) }
        if (NativeEngine.isAvailable && !waitsFor(LiveSlots.ACID)) NativeEngine.setAcidStep(index, project.value.acid.steps[index].pack())
    }

    fun toggleAcidLock() {
        val index = _selectedAcidStep.value
        session.edit { p ->
            val locked = p.acid.locked.let { if (index in it) it - index else it + index }
            p.copy(acid = p.acid.copy(locked = locked))
        }
    }

    fun setAcidSound(index: Int, value: Float) {
        session.edit("acid-sound-$index") { p -> p.copy(acid = p.acid.copy(sound = p.acid.sound.with(index, value))) }
        if (NativeEngine.isAvailable) NativeEngine.setAcidParam(index, project.value.acid.sound.values()[index])
        // Turned while the automation records: the gesture goes into a lane of that knob.
        if (index in AutoTarget.ACID_PARAMS) recordAutomation(AutoTarget.acid(index), value.coerceIn(0f, 1f))
    }

    /** A finger on 303 knob [index] or off it: while held, its automation lane lets the finger play it (see [holdSetting]). */
    fun holdAcidKnob(index: Int, held: Boolean) {
        if (index in AutoTarget.ACID_PARAMS) holdSetting(AutoTarget.acid(index), held)
    }

    /**
     * Touch take-over: while a finger holds the control of [target] (a 303 knob now; a fader or an
     * effect's knob later, the Live screen's big controls), no automation lane writes that setting
     * in the engine, so the gesture is heard during the song; let go, the lanes drive it again from
     * the next step. While recording, the gesture is still written into its lane ([recordAutomation]).
     */
    fun holdSetting(target: AutoTarget, held: Boolean) {
        if (!target.isValid) return
        val changed = if (held) heldSettings.add(target) else heldSettings.remove(target)
        if (!changed || !NativeEngine.isAvailable) return
        if (held) NativeEngine.setAutomationHold(target.pack(), true) else releaseHold(target.pack())
    }

    /** The settings a finger holds now (touch take-over). */
    private val heldSettings = HashSet<AutoTarget>()
    internal val heldSettingsForTests: Set<AutoTarget> get() = heldSettings

    fun shiftAcidRoot(semitones: Int) {
        session.edit { p -> p.copy(acid = p.acid.copy(root = p.acid.root + semitones)) }
        if (NativeEngine.isAvailable) NativeEngine.setAcidRoot(project.value.acid.root)
    }

    fun setAcidDensity(density: Float) {
        session.edit("acid-density") { p -> p.copy(acid = p.acid.copy(density = density)) }
    }

    /** The acid line to the engine: its notes (unless [editsWait]), root and knobs ([keep]: knobs left as they are). */
    private fun pushAcid(acid: AcidPattern, keep: Set<Int> = emptySet()) {
        if (!NativeEngine.isAvailable) return
        // Another scene plays (staying on this one during the song): only the sound goes now.
        val notes = !waitsFor(LiveSlots.ACID)
        if (notes) NativeEngine.setAcidLength(acid.length)
        NativeEngine.setAcidRoot(acid.root)
        if (notes) acid.steps.forEachIndexed { i, s -> NativeEngine.setAcidStep(i, s.pack()) }
        // The knobs moved on the Live page stay where the fingers left them (until Stop).
        val moved = if (NativeEngine.isCapturing) emptyMap() else _liveAcid.value
        acid.sound.values().forEachIndexed { i, v -> if (i !in keep) NativeEngine.setAcidParam(i, moved[i] ?: v) }
    }

    /** ▶ / ■: plays the song from its first part when listening to the song ([listensToSong]), else the scene shown. */
    fun togglePlay() {
        if (!_transport.value.playing && NativeEngine.isAvailable && listensToSong) {
            playSongFrom(0)
            return
        }
        if (!_transport.value.playing) {
            heardScene = project.value.scene
            // The scene on its own: song curves hold the value they have where it first plays.
            if (NativeEngine.isAvailable) applySongCurves(project.value, project.value.scene)
        }
        toggleTransport(songStep = aloneSongStep())
    }

    /**
     * The song position a scene played on its own starts with: outside the song once there is one
     * (song curves and song clips wait for it); without a song, song curves follow the bars from
     * the start of playback, as the Automation card says.
     */
    private fun aloneSongStep(): Int = if (project.value.song.isEmpty()) 0 else NativeEngine.OUTSIDE_SONG

    /** Song lanes set to their value on the bar where [scene] first plays in the song (a scene played on its own). */
    private fun applySongCurves(p: Project, scene: Int) = applyAutomationState(p, p.songCurvesAt(scene))

    /**
     * Starts or stops the transport, whatever it plays. Starting: [countInBars] (null: the count-in
     * switch), the first step being song step [songStep] ([NativeEngine.OUTSIDE_SONG]: a scene on its own).
     */
    private fun toggleTransport(countInBars: Int? = null, songStep: Int = NativeEngine.OUTSIDE_SONG) {
        val t = _transport.value
        val playing = !t.playing
        // A take ends before the transport does: its last launches are read while they still count.
        if (!playing) endPerformanceOnStop()
        _transport.value = t.copy(playing = playing)
        // The live grid starts from the scene heard (a song part: as it plays it, each track from
        // where it carries on); Stop ends what the Live page did.
        if (playing) {
            val part = _sessionUi.value.songIndex.takeIf { it >= 0 }?.let { project.value.song.getOrNull(it) }
            _liveGrid.value = gridOfPart(part, heardScene ?: project.value.scene, 0, null)
        } else {
            endLiveGestures()
        }
        if (!NativeEngine.isAvailable) return
        if (!playing && _recording.value) stopRecordingTake()
        if (playing) {
            drainLaunches(apply = false)  // what an earlier run left
            NativeEngine.play(countInBars ?: if (t.countIn) 1 else 0, songStep)
        } else {
            NativeEngine.stopTransport()
            NativeEngine.toggleClip(-1)  // live pads too: Stop means silence
            catchUpMissedSwap()
            _sessionUi.value = SessionUi()
            heardScene = null
            // Engine and screens agree again on the scene, whatever the swaps did.
            syncEngine()
        }
        val app = getApplication<Application>()
        AppLog.add(if (playing) "Lecture (${project.value.tempoBpm.toInt()} BPM)" else "Stop")
        if (playing) PlaybackService.start(app) else PlaybackService.stopAfterTakes(app)
    }

    /** Stops the transport if it plays (opening the assistant: its previews must not play over the song). */
    fun stop() {
        if (_transport.value.playing) togglePlay()
    }

    /** True while the activity is on screen; set by the UI. */
    var visible = true

    /** The engine must keep running in the background only while the transport plays. */
    val keepsRunningInBackground: Boolean get() = _transport.value.playing

    init {
        viewModelScope.launch { project.collect { restageIfEditingUpcoming() } }
        // A project with more long effects than the engine holds (made elsewhere, or by hand): said at once.
        if (_notice.value == null) warnLongFxOverLimit()
        loadPadSamples()
        if (project.value.instrument != null) openLibrary()
        loadTrackFonts()
        if (NativeEngine.isAvailable) NativeEngine.liveTrack = 0
        // The engine outlives a project: the keyboard's sustain, wheel and arpeggio of the last
        // one must not hold or arpeggiate this one's notes while its buttons show them off.
        for (id in 0 until InstrumentTrack.MAX) {
            expression(NativeEngine.EXPR_BEND, 0f, id)
            expression(NativeEngine.EXPR_MOD, 0f, id)
            expression(NativeEngine.EXPR_SUSTAIN, 0f, id)
            expression(NativeEngine.EXPR_ARP_ON, 0f, id)
        }
        viewModelScope.launch {
            session.lastSaveError.collect { e -> if (e != null) AppLog.add("Sauvegarde impossible : ${e.message}") }
        }
        // What the phone shows while playing (media player) or recording (live activity "REC").
        viewModelScope.launch {
            var since = 0L
            kotlinx.coroutines.flow.combine(
                project.map { com.pocketstudio.model.DefaultNames.shown(it.name) to it.tempoBpm }.distinctUntilChanged(),
                _recording,
                // null: no take; false: the microphone opens or counts in; true: it records.
                _audioRecording.map { it?.let { take -> !take.waiting } }.distinctUntilChanged(),
            ) { song, live, take -> Triple(song, live, take) }.collect { (song, live, take) ->
                val recording = live || take != null
                // The chronometer starts with what is really recorded, as the take's own time does.
                if (!recording) since = 0L else if (since == 0L && (live || take == true)) since = System.currentTimeMillis()
                PlaybackService.update(PlaybackStatus(song.first, song.second, recording, since, microphone = take != null))
            }
        }
        viewModelScope.launch {
            PlaybackService.stops.collect { why ->
                AppLog.add("Arrêt : $why")
                // A microphone take ends and is kept; otherwise the song stops.
                if (_audioRecording.value != null) stopAudioRecording()
                else if (_transport.value.playing) togglePlay()
                stopLive()  // headphones out: no live pad keeps looping full blast on the speaker
                if (!visible && NativeEngine.isAvailable) NativeEngine.stop()
            }
        }
    }

    fun toggleMetronome() {
        val on = !_transport.value.metronome
        _transport.value = _transport.value.copy(metronome = on)
        if (NativeEngine.isAvailable) NativeEngine.setMetronome(on)
    }

    fun toggleCountIn() {
        _transport.value = _transport.value.copy(countIn = !_transport.value.countIn)
    }

    /** Tapping a pad plays it and makes it the pad shown in the step grid. */
    fun hitPad(lane: Int) {
        if (lane != _transport.value.selectedPad) closeSlicer()
        _transport.value = _transport.value.copy(selectedPad = lane)
        if (NativeEngine.isAvailable) NativeEngine.triggerPad(lane, 0.9f)
        recordPad(lane, 0.9f)
    }

    /** Drum loop length in bars (1 to 4). */
    fun setDrumBars(bars: Int) {
        session.edit { it.copy(drums = it.drums.withSteps(bars * it.stepsPerBar)) }
        if (NativeEngine.isAvailable) pushDrumPattern(project.value.drums)
    }

    fun toggleStep(step: Int) {
        val lane = _transport.value.selectedPad
        session.edit { it.copy(drums = it.drums.toggle(lane, step)) }
        pushLane(lane)
    }

    /** Long press: cycles the hit between strong, medium and soft. */
    fun cycleVelocity(step: Int) {
        val lane = _transport.value.selectedPad
        session.edit { p ->
            val current = p.drums.lanes[lane].hits[step]?.velocity
            // Strong 1.0, medium 0.5 (−6 dB), soft 0.2 (−14 dB): clearly audible steps.
            val next = when {
                current == null -> 1f
                current > 0.75f -> 0.5f
                current > 0.35f -> 0.2f
                else -> 1f
            }
            val lanes = p.drums.lanes.toMutableList()
            // Keeps the hit's roll, pan and pitch.
            val hit = lanes[lane].hits[step]?.copy(velocity = next) ?: Hit(next)
            lanes[lane] = lanes[lane].copy(hits = lanes[lane].hits + (step to hit))
            p.copy(drums = p.drums.copy(lanes = lanes))
        }
        pushLane(lane)
    }

    /** The selected pad's place in the stereo field, -1..1 (a drag is one undo step). */
    fun setPadPan(pan: Float) {
        val lane = _transport.value.selectedPad
        session.edit("pad-pan-$lane") { it.copy(drums = it.drums.withPadPan(lane, pan)) }
        if (NativeEngine.isAvailable) NativeEngine.setPadPan(lane, project.value.drums.lanes[lane].pan)
    }

    /** One hit's own pan (-1..1) on the selected pad. */
    fun setHitPan(step: Int, pan: Float) = editHitDetail(step, "hit-pan") { it.copy(pan = pan) }

    /** One hit's strength (« Force »), 0.05..1, on the selected pad; a slide is one undo step. */
    fun setHitVelocity(step: Int, velocity: Float) {
        val lane = _transport.value.selectedPad
        if (project.value.drums.lanes[lane].hits[step] == null) return
        session.edit("hit-vel-$lane-$step") { it.copy(drums = it.drums.withHitVelocity(lane, step, velocity)) }
        if (NativeEngine.isAvailable) pushHit(lane, step, project.value.drums.lanes[lane].hits[step])
    }

    /** One hit's pitch in semitones on the selected pad (its own sound only). */
    fun setHitPitch(step: Int, pitch: Int) = editHitDetail(step, "hit-pitch") { it.copy(pitch = pitch) }

    private fun editHitDetail(step: Int, key: String, transform: (Hit) -> Hit) {
        val lane = _transport.value.selectedPad
        val hit = project.value.drums.lanes[lane].hits[step] ?: return
        val next = transform(hit)
        session.edit("$key-$lane-$step") { it.copy(drums = it.drums.withHitDetail(lane, step, next.pan, next.pitch)) }
        if (NativeEngine.isAvailable) pushHit(lane, step, project.value.drums.lanes[lane].hits[step])
    }

    fun clearPad() {
        val lane = _transport.value.selectedPad
        session.edit { p ->
            val lanes = p.drums.lanes.toMutableList()
            lanes[lane] = lanes[lane].copy(hits = emptyMap())
            p.copy(drums = p.drums.copy(lanes = lanes))
        }
        pushLane(lane)
    }

    fun nudgeTempo(delta: Int) {
        session.edit("tempo") { it.copy(tempoBpm = Math.round(it.tempoBpm).toDouble() + delta) }
        if (NativeEngine.isAvailable) NativeEngine.setTempo(project.value.tempoBpm)
        if (project.value.remix.followTempo) pushRemix()
        if (project.value.drums.lanes.any { (it.sample?.fitBeats ?: 0) > 0 }) pushPads()
    }

    /** An exact tempo from outside (MIDI clock): one undo step while it keeps moving. */
    fun setTempo(bpm: Double) {
        session.edit("tempo") { it.copy(tempoBpm = bpm) }
        if (NativeEngine.isAvailable) NativeEngine.setTempo(project.value.tempoBpm)
        if (project.value.remix.followTempo) pushRemix()
        if (project.value.drums.lanes.any { (it.sample?.fitBeats ?: 0) > 0 }) pushPads()
    }

    fun tapTempo() {
        val bpm = tapTempo.tap(System.currentTimeMillis()) ?: return
        session.edit("tempo") { it.copy(tempoBpm = Math.round(bpm).toDouble()) }
        if (NativeEngine.isAvailable) NativeEngine.setTempo(project.value.tempoBpm)
        if (project.value.remix.followTempo) pushRemix()
        if (project.value.drums.lanes.any { (it.sample?.fitBeats ?: 0) > 0 }) pushPads()
    }

    fun setSwing(swing: Double) {
        session.edit("swing") { it.copy(swing = swing) }
        if (NativeEngine.isAvailable) NativeEngine.setSwing(project.value.swing)
    }

    fun undo() {
        // A take ends first: a live one then undoes in one step; a mic take keeps its track.
        if (_recording.value || _audioRecording.value != null || _takeSaving.value || performanceTaking) return
        val heard = sceneHeard()
        val before = project.value
        // A scene move undone: the scene playing goes on from its old place (not the one now there).
        val move = session.undo() as? SceneMove
        move?.let { followSceneMove(it::back) }
        keepListenChoice()
        keepLiveSettings(before)
        followPlaybackAfterHistory(if (move != null) heard?.let(move::back) else heard)
        warnLongFxOverLimit(before)
        loadTrackFonts()
        reloadMissingSamples()
    }

    fun redo() {
        if (_recording.value || _audioRecording.value != null || _takeSaving.value || performanceTaking) return
        val heard = sceneHeard()
        val before = project.value
        val move = session.redo() as? SceneMove
        move?.let { followSceneMove(it::forward) }
        keepListenChoice()
        keepLiveSettings(before)
        followPlaybackAfterHistory(if (move != null) heard?.let(move::forward) else heard)
        warnLongFxOverLimit(before)
        loadTrackFonts()
        reloadMissingSamples()
    }

    /** Tests (no engine there): behave as while playing, for what follows the scene heard. */
    internal var playingForTest = false

    /** While playing: the scene the engine plays now (a swap the 40 ms watch missed is followed first); else null. */
    private fun sceneHeard(): Int? {
        if (playingForTest) return heardScene ?: project.value.scene
        if (!_transport.value.playing || !NativeEngine.isAvailable) return null
        if ((_sessionUi.value.pending != null || _sessionUi.value.ending) && NativeEngine.sceneSwaps() != swapsSeen) {
            swapsSeen = NativeEngine.sceneSwaps()
            onSceneSwapped()
            if (!_transport.value.playing) return null  // the song just ended
        }
        return heardScene ?: project.value.scene
    }

    /**
     * After undo or redo: the step brought back may show another scene, or another song, than
     * what plays. While playing, the screens stay on the scene [heard] (quietly), the engine gets
     * its content as it is now, and the waiting scene or song part is sent again (or dropped
     * when the song no longer has it).
     */
    private fun followPlaybackAfterHistory(heard: Int?) {
        if (heard == null) {
            syncEngine()
            return
        }
        val ui = _sessionUi.value
        // Staying on a scene during the song: the screens keep the scene the step brought back.
        if (screensFollow(ui) && project.value.scene != heard && heard in project.value.allScenes.indices) session.editQuietly { it.switchScene(heard) }
        if (ui.songIndex >= 0) {
            val index = followSongIndex(project.value.song, ui.songIndex, heard)
            if (index < 0) {
                endSongFollow()
                syncEngine()
                return
            }
            _sessionUi.value = SessionUi(songIndex = index)
            syncEngine()  // the part on now, with its muted tracks
            stageSongPart()
        } else {
            syncEngine()
            // A waiting scene that undo took away (a duplicate) is dropped, not played empty.
            ui.pending?.let { if (it in project.value.allScenes.indices) stageScene(it, 0) else endSongFollow() }
        }
    }

    /** Writes pending changes now (the app is going to the background), and the Live take recording so far. */
    fun flush() {
        keepTakeJournal()
        viewModelScope.launch { withContext(NonCancellable) { session.flush() } }
    }

    /** Writes pending changes and waits for the write (before another project is opened). */
    suspend fun flushNow() = withContext(NonCancellable) { session.flush() }

    /** Renames the open project (one undo step). */
    fun renameProject(name: String) {
        val clean = name.trim().take(com.pocketstudio.model.ProjectCatalog.MAX_NAME)
        if (clean.isNotEmpty()) session.edit { it.copy(name = clean) }
    }

    /** A hit and its roll (rolls only travel with hits: an empty step needs nothing more). */
    private fun pushHit(lane: Int, step: Int, hit: Hit?) {
        if (waitsFor(LiveSlots.DRUMS)) return
        NativeEngine.setDrumHit(lane, step, hit?.velocity ?: 0f, hit?.repeats ?: 1)
        // Only hits with their own pan or pitch need more (the hit command resets both).
        if (hit != null && !hit.isPlain) NativeEngine.setDrumStepDetail(lane, step, hit.pan, hit.pitch)
    }

    fun fillPad(every: Int) {
        val lane = _transport.value.selectedPad
        session.edit { it.copy(drums = it.drums.fill(lane, every)) }
        pushLane(lane)
    }

    fun rotatePad(by: Int) {
        val lane = _transport.value.selectedPad
        session.edit("rotate-$lane") { it.copy(drums = it.drums.rotate(lane, by)) }
        pushLane(lane)
    }

    fun cycleRoll(step: Int) {
        val lane = _transport.value.selectedPad
        session.edit { it.copy(drums = it.drums.cycleRepeats(lane, step)) }
        pushLane(lane)
    }

    /** All-pads grid: toggles [lane] at [step] without changing the selected pad. */
    fun toggleHit(lane: Int, step: Int) {
        session.edit { it.copy(drums = it.drums.toggle(lane, step)) }
        pushLane(lane)
    }

    private fun pushLane(lane: Int) {
        if (!NativeEngine.isAvailable) return
        val hits = project.value.drums.lanes[lane].hits
        for (step in 0 until DrumPattern.MAX_STEPS) pushHit(lane, step, hits[step])
    }

    // --- Export ----------------------------------------------------------------------------

    private val _export = MutableStateFlow(ExportUi())
    /** The export settings, « La scène » as long as the current scene unless a length was picked. */
    val export: StateFlow<ExportUi> = combine(_export, project) { e, p -> e.forProject(sceneBars(p), p.song.isNotEmpty()).copy(audioBars = p.audioEndBars()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ExportUi().forProject(sceneBars(project.value), project.value.song.isNotEmpty()))
    private var running: OfflineExport? = null

    private fun sceneBars(p: Project) = (p.currentSceneContent().stepsFor(p.stepsPerBar) / p.stepsPerBar).coerceIn(1, MAX_EXPORT_BARS)

    fun setExportBars(bars: Int) {
        _export.value = _export.value.copy(bars = bars.coerceIn(1, MAX_EXPORT_BARS), barsChosen = true)
    }

    fun setExportWhat(what: ExportWhat) {
        _export.value = _export.value.copy(what = what, whatChosen = true)
    }

    fun setExportFormat(format: ExportFormat) {
        _export.value = _export.value.copy(format = format)
    }

    fun setExportFade(fade: ExportFade) {
        _export.value = _export.value.copy(fade = fade, fadeChosen = true)
    }

    fun setExportNoLimiter(on: Boolean) {
        _export.value = _export.value.copy(noLimiter = on)
    }

    fun setStemsWithMaster(on: Boolean) {
        _export.value = _export.value.copy(stemsWithMaster = on)
    }

    /** The master's gain before the limiter, in dB (saved with the project). */
    fun setMasterDb(db: Float) {
        session.edit("mix-master-db") { p -> p.copy(mixer = p.mixer.copy(masterDb = db.coerceIn(Mixer.MASTER_MIN_DB, Mixer.MASTER_MAX_DB))) }
        if (NativeEngine.isAvailable) NativeEngine.setMasterGain(project.value.mixer.masterGain)
    }

    /** Files of a finished « Partager mon morceau », for the share sheet (the screen opens it). */
    private val _shareRequests = kotlinx.coroutines.channels.Channel<List<File>>(kotlinx.coroutines.channels.Channel.CONFLATED)
    val shareRequests: kotlinx.coroutines.flow.Flow<List<File>> = _shareRequests.receiveAsFlow()

    /**
     * « Partager mon morceau »: the whole song (or the scene looped when there is none) in M4A,
     * with a short fade, then the share sheet. The export card's own choices stay as they were.
     */
    fun shareSong() {
        val p = project.value
        if (!NativeEngine.isAvailable) {
            _export.value = _export.value.copy(sharing = true, error = tr("Le moteur audio n'est pas prêt : réessaie dans un instant."))
            return
        }
        // A take just stopped may still be being written: the song waits for it.
        viewModelScope.launch {
            if (_takeSaving.value) {
                _export.value = _export.value.copy(sharing = true, running = true, progress = 0f, stage = tr("Écriture de la prise…"), error = null, files = emptyList())
                takeJob?.join()
                _export.value = _export.value.copy(running = false, stage = null)
            }
            val now = project.value
            startExport(com.pocketstudio.app.export.ShareSong.settings(now.song.isNotEmpty(), sceneBars(now), now.audioEndBars()), share = true)
        }
    }

    /** Closes the « Partager mon morceau » window after an error. */
    fun dismissShare() {
        _export.value = _export.value.copy(sharing = false, error = null)
    }

    /** Frames of the whole song (each part: its scene's length × its repeats). */
    private fun songFrames(p: Project): Long {
        val steps = p.song.sumOf { p.partSteps(it).toLong() }
        return Math.round(steps * 15.0 / p.tempoBpm * OfflineExport.SAMPLE_RATE)  // a sixteenth = 15/bpm s
    }

    /**
     * Describes the project to an export engine. For the song, the first part's scene plays first
     * and each next scene is staged half a step into the part before it, swapping on its last bar line.
     */
    private fun describe(job: OfflineExport, p: Project, song: Boolean, soloChannel: Int?, limited: Boolean = true, masterFx: Boolean = true) {
        NativeEngine.captureInto(job) {
            val first = if (song) p.sceneView(p.song[0].scene) else p
            syncEngine(first)
            if (song) pushPart(p, p.song[0].scene, p.song[0])
            NativeEngine.setMetronome(false)
            // A pre-master mix keeps its headroom; separate tracks also skip the master's
            // effects, so that together they give back the mix.
            if (!limited) NativeEngine.setLimiter(false)
            if (soloChannel != null && !masterFx) NativeEngine.setMasterFx(false)
            // The Live page's own sounds (riser, build-up noise): only in the « Effets live » file.
            if (soloChannel != null) NativeEngine.setLiveNoises(if (soloChannel == LIVE_FX_STEM) NativeEngine.LIVE_NOISES_ONLY else NativeEngine.LIVE_NOISES_OFF)
            if (soloChannel != null && soloChannel != LIVE_FX_STEM) {
                // A stem is its strip alone, whatever solos and mutes the mix was left with.
                for (ch in 0 until Mixer.FIRST_PAD) if (ch != MASTER) NativeEngine.setChannelSolo(ch, false)
                NativeEngine.setChannelMute(soloChannel, false)
                NativeEngine.setChannelSolo(soloChannel, true)
            }
            if (song) {
                val sixteenth = 15.0 / p.tempoBpm * OfflineExport.SAMPLE_RATE
                var stepsSoFar = 0L
                p.song.forEachIndexed { i, part ->
                    val steps = p.partSteps(part)
                    val next = p.song.getOrNull(i + 1)
                    if (next != null) {
                        NativeEngine.captureAt(Math.round((stepsSoFar + 0.5) * sixteenth))
                        NativeEngine.setStaging(true)
                        pushPart(p, next.scene, next)
                        NativeEngine.setStaging(false)
                        NativeEngine.armSceneSwap(steps)
                    }
                    stepsSoFar += steps
                }
                NativeEngine.captureAt(-1)
            } else if (p.song.isNotEmpty()) {
                // The scene on its own, as ▶ plays it: song curves hold their value where it first
                // plays in the song, song clips stay silent (no song position from its first step).
                applySongCurves(p, p.scene)
                NativeEngine.captureAt(0)
                NativeEngine.leaveSong()
                NativeEngine.captureAt(-1)
            }
        }
    }

    /**
     * Renders the project offline (never the live engine): the loop, the whole song or each track,
     * into WAV (or M4A), then copies the files to Music › Pocket Studio.
     */
    fun exportWav() {
        val p = project.value
        startExport(_export.value.forProject(sceneBars(p), p.song.isNotEmpty()), share = false)
    }

    /** Renders with [settings] (the card's, or « Partager mon morceau »'s with [share]). */
    private fun startExport(settings: ExportUi, share: Boolean) {
        if (!NativeEngine.isAvailable || running != null || exportJob != null) return
        exportCancelled = false
        val app = getApplication<Application>()
        val p = project.value
        // The card keeps its own choices; only the run's state (progress, files, error) is shown.
        val shown = _export.value.copy(sharing = share)
        val song = settings.what == ExportWhat.Song || (settings.what == ExportWhat.Stems && p.song.isNotEmpty())
        if (settings.what == ExportWhat.Song && p.song.isEmpty()) {
            _export.value = shown.copy(error = tr("Le morceau est vide : ajoute des scènes dans l'onglet Morceau."), files = emptyList())
            return
        }
        val frames = if (song) songFrames(p) else OfflineExport.framesFor(settings.bars, p.tempoBpm, p.beatsPerBar)
        // A fade-out ends the file on silence: no tail to ring out after it.
        val fadeFrames = settings.fadeFrames(frames)
        // Otherwise the tail rings out until the sound has died away (at most MAX_TAIL_FRAMES).
        val tailFrames = if (fadeFrames > 0) 0L else MAX_TAIL_FRAMES
        // Each render: (label for the file and the screen, solo channel or null for the mix).
        val renders: List<Pair<String, Int?>> = when (settings.what) {
            ExportWhat.Stems -> stemExports(p, song)
            ExportWhat.Song -> listOf("morceau" to null)
            ExportWhat.Loop -> listOf("boucle" to null)
        }
        if (renders.isEmpty()) {
            _export.value = shown.copy(error = tr("Rien à exporter : chaque piste est vide ou coupée dans le mixeur."), files = emptyList())
            return
        }
        _export.value = shown.copy(running = true, progress = 0f, files = emptyList(), error = null, stage = null, loudness = null)
        exportJob = viewModelScope.launch {
            val done = ArrayList<File>()
            var musicOk = true
            var loudness: Double? = null
            // Just after the project opened, its sounds, banks and song may still be loading: the
            // file would have the kit and the synth in their place.
            samplesLoad?.join()
            var waited = 0
            while ((loadingBanks.isNotEmpty() || stemsJob?.isActive == true || remixStretchJob?.isActive == true || stretchJob?.isActive == true) &&
                waited++ < 300
            ) kotlinx.coroutines.delay(50)
            try {
                renders.forEachIndexed { index, (label, solo) ->
                    // A private engine costs tens of milliseconds to build: not on the main thread (never lost if cancelled).
                    val job = finishOrRelease(Dispatchers.Default, OfflineExport::close) { OfflineExport() }!!
                    describe(job, p, song, solo, settings.limited, settings.masterFx)
                    running = job
                    val wav = runCatching { WavExporter.newFile(app, "${com.pocketstudio.model.DefaultNames.shown(p.name).take(28)}-$label") }.getOrElse { throw UserMessage(tr("Impossible de préparer le fichier d'export.")) }
                    _export.value = _export.value.copy(stage = if (renders.size > 1) "${stageName(label)} (${index + 1}/${renders.size})" else null)
                    val poll = launch {
                        while (true) {
                            _export.value = _export.value.copy(progress = (index + job.progress) / renders.size)
                            kotlinx.coroutines.delay(100)
                        }
                    }
                    val result = try {
                        withContext(Dispatchers.IO) { job.render(wav, frames, tailFrames, fadeFrames, autoTail = true, floatWav = !settings.limited) }
                    } finally {
                        poll.cancel()
                        // Closed on the main thread, after the render returned.
                        job.close()
                        running = null
                    }
                    when (result) {
                        ExportResult.Cancelled -> { wav.delete(); throw kotlinx.coroutines.CancellationException("annulé") }
                        ExportResult.IoError -> { wav.delete(); throw UserMessage(tr("L'écriture du fichier a échoué (stockage plein ?).")) }
                        ExportResult.Ok -> Unit
                    }
                    // Measured on the full-quality mix, before any compression.
                    if (solo == null) loudness = withContext(Dispatchers.IO) { runCatching { Loudness.integrated(wav) }.getOrNull() }
                    if (exportCancelled) {
                        wav.delete()
                        throw kotlinx.coroutines.CancellationException("annulé")
                    }
                    val out = if (settings.format == ExportFormat.Wav) {
                        wav
                    } else {
                        val name = if (settings.format == ExportFormat.M4a) tr("Compression M4A") else tr("Conversion FLAC")
                        val part = if (renders.size > 1) " (${index + 1}/${renders.size})" else ""
                        _export.value = _export.value.copy(stage = name + part)
                        val packed = File(wav.parentFile, wav.nameWithoutExtension + "." + settings.format.extension)
                        withContext(Dispatchers.IO) {
                            try {
                                if (settings.format == ExportFormat.M4a) {
                                    AacEncoder.encode(wav, packed) { exportCancelled || !isActive }
                                } else {
                                    FlacEncoder.encode(wav, packed, { exportCancelled || !isActive }) { p ->
                                        _export.value = _export.value.copy(stage = "$name ${com.pocketstudio.model.i18n.percent((p * 100).roundToInt())}$part")
                                    }
                                }
                            } finally {
                                wav.delete()
                            }
                        }
                        packed
                    }
                    // Listed first: a cancel from here on deletes it with the others.
                    done += out
                    if (exportCancelled) throw kotlinx.coroutines.CancellationException("annulé")
                    musicOk = withContext(Dispatchers.IO) { WavExporter.copyToMusic(app, out, settings.format.mime) } && musicOk
                }
                _export.value = _export.value.copy(running = false, files = done, savedInMusic = musicOk, stage = null, loudness = loudness, sharing = false)
                AppLog.add("Export ${settings.what} ${settings.format} : ${done.size} fichier(s), ${frames / OfflineExport.SAMPLE_RATE} s" +
                    if (share) " (partage)" else "")
                if (share) _shareRequests.trySend(done.toList())
            } catch (e: kotlinx.coroutines.CancellationException) {
                done.forEach { it.delete() }
                _export.value = _export.value.copy(running = false, files = emptyList(), stage = null, sharing = false)
            } catch (e: Exception) {
                done.forEach { it.delete() }
                AppLog.add("Export : échec (${e.message})")
                // « Partager mon morceau » keeps its window open to show what went wrong.
                _export.value = _export.value.copy(running = false, files = emptyList(), stage = null,
                    error = if (e is UserMessage) e.message else tr("L'export a échoué : %s", e.message ?: e.javaClass.simpleName))
            } finally {
                exportCancelled = false
                exportJob = null
            }
        }
    }

    /** The project's notes as a MIDI file (the whole song when there is one), copied to Music. */
    fun exportMidi() {
        val app = getApplication<Application>()
        val p = project.value
        viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    val wav = WavExporter.newFile(app, com.pocketstudio.model.DefaultNames.shown(p.name) + if (p.song.isNotEmpty()) "-morceau" else "")
                    val mid = File(wav.parentFile, wav.nameWithoutExtension + ".mid")
                    mid.writeBytes(com.pocketstudio.model.MidiFile.export(p))
                    mid
                }
                val music = withContext(Dispatchers.IO) { WavExporter.copyToMusic(app, file, "audio/midi") }
                _export.value = _export.value.copy(files = listOf(file), savedInMusic = music, error = null, loudness = null)
                AppLog.add("Export MIDI : ${file.length()} octets")
            } catch (e: Exception) {
                _export.value = _export.value.copy(error = tr("L'export MIDI a échoué : %s", e.message))
            }
        }
    }

    /** A MIDI file's notes into the current scene (piano roll, drum pads, tempo): one undo step. */
    fun importMidi(uri: Uri) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            try {
                val content = withContext(Dispatchers.IO) {
                    val bytes = app.contentResolver.openInputStream(uri)?.use { input ->
                        val out = java.io.ByteArrayOutputStream()
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            if (out.size() > MAX_MIDI_BYTES) throw UserMessage(tr("Ce fichier MIDI est trop gros."))
                        }
                        out.toByteArray()
                    } ?: throw UserMessage(tr("Le fichier n'a pas pu être ouvert."))
                    com.pocketstudio.model.MidiFile.read(bytes)
                }
                if (content.notes.isEmpty()) throw UserMessage(tr("Aucune note dans ce fichier MIDI."))
                session.edit { com.pocketstudio.model.MidiFile.importInto(it, content) }
                syncEngine()
                AppLog.add("Import MIDI : ${content.notes.size} notes")
                _transcription.value = SampleTask.Idle
            } catch (e: Exception) {
                _transcription.value = SampleTask.Failed(if (e is UserMessage) e.message!! else tr("Ce fichier MIDI n'a pas pu être lu (%s).", e.message))
            }
        }
    }

    private var exportJob: kotlinx.coroutines.Job? = null
    @Volatile private var exportCancelled = false

    fun cancelExport() {
        exportCancelled = true
        running?.cancel()
    }

    override fun onCleared() {
        exportCancelled = true  // a conversion in progress stops and leaves no file behind
        // A Live take recording: kept as it was played up to the bar being played, written into
        // the song at the next opening (the screen's scope is gone: nothing can be written here).
        when (_take.value) {
            // On the journal's own writer: it outlives this screen, and the next one reads after it.
            is com.pocketstudio.app.live.TakeUi.Recording -> keepTakeJournal()
            is com.pocketstudio.app.live.TakeUi.Armed -> dropTakeJournal(performance)
            else -> Unit
        }
        performance = null
        // The last edits (after the flush of ON_STOP, or never flushed) reach the disk all the same;
        // a save being written is waited for too (the next screen, or a trash, waits for these).
        if (!session.settled) ClosingSaves.launch { session.flush() }
        // A take in progress is kept: written into the project away from this screen, it is on
        // its track the next time the project opens.
        recordHandle.takeIf { it != 0L }?.let { handle ->
            AppLog.add("Prise en cours gardée à la fermeture")
            TakeVault.app.finish(takeRequest(handle, _audioRecording.value?.waiting ?: false))
        }
        recordHandle = 0L
        downloadCancelled = true
        fonts.values.forEach { it.close() }
        fonts.clear()
        micThread.close()
        running?.cancel()
        recorder?.stop()
        padSamples.close()
        stems?.close()
        stretched?.close()
        if (NativeEngine.isAvailable) NativeEngine.stopTransport()
        PlaybackService.stopAfterTakes(getApplication())
    }

    companion object {
        /** [beforePress]'s key for a row or « ■ Tout » (a column's key is never empty). */
        private const val ROW_PRESS = ""
        const val PROJECT_ID = "projet-1"
        const val BUSY_TRACK_SOUND = "Préparation du son…"
        /** How early (sixteenths) a note played ahead of the first step still lands on it. */
        private const val EARLY_STEPS = 1.0

        /** The first project of a new install, and the "Rythme de départ" choice: a beat and an acid line. */
        fun starterProject(name: String = "Mon premier rythme"): Project {
            var drums = DrumPattern()
            DrumKitInfo.starterHits.forEach { (lane, steps) -> steps.forEach { drums = drums.toggle(lane, it) } }
            val acid = AcidGenerator().generate(AcidPattern(), AcidGenerator.NOTE_SETS[5])
            return Project(name = name, drums = drums, acid = acid)
        }
        const val MAX_MIDI_BYTES = 4 * 1024 * 1024
        /** The REC line warns when this many song parts are left, or bars before the effects' end. */
        const val TAKE_PARTS_WARNING = 8
        const val TAKE_BARS_WARNING = 16
        /** Stems export: one render per mixer channel. */
        /** A song automation lane spans at least this many bars (the song can be shorter or empty). */
        const val SONG_LANE_MIN_BARS = 8
        /** Longest microphone take (34 MB of memory). */
        /** A take lasts 5 minutes at most (a whole song: 57 MB of mono float in memory). */
        const val MAX_AUDIO_SECONDS = 300
        const val MAX_EXPORT_BARS = 64
        private val AUDIO_EXTENSIONS = setOf("wav", "mp3", "flac", "m4a", "aac", "ogg", "opus", "aif", "aiff")
        /** After the last bar, releases, reverbs and echoes ring out until silent (−90 dBFS), ten seconds at most. */
        const val MAX_TAIL_FRAMES = 10L * OfflineExport.SAMPLE_RATE
        internal val STEM_LABELS = listOf("Batterie", "Basse", "Autres", "Voix")

        /** An export file's label ("batterie", "piste2-basse"…) as the progress line shows it. */
        private fun stageName(label: String): String = when (label) {
            "morceau" -> tr("Morceau")
            "boucle" -> tr("Boucle")
            "batterie" -> tr("Batterie")
            "acide" -> tr("Acide")
            "synthe" -> tr("Synthé")
            LIVE_FX_STEM_LABEL -> tr("Effets live")
            else -> label.replaceFirstChar { it.uppercase() }
        }
        private const val OVERVIEW_BUCKETS = 400
        /** Insert "channel" index of the master. */
        const val MASTER = NativeEngine.MASTER

        /** The mixer strips [p] uses (the drums, acid, synth, its tracks, the pads with effects of their own). */
        internal fun usedStrips(p: Project): Set<Int> {
            val pads = (0 until DrumPattern.PAD_COUNT).filter { p.mixer.channels.getOrNull(Mixer.padChannel(it))?.hasOwnFx() == true }
            return setOf(0, 1, 3) + p.instrumentTracks.map { InstrumentTrack.mixerChannel(it.id) } +
                p.audioTracks.map { AudioTrack.mixerChannel(it.id) } + pads.map { Mixer.padChannel(it) }
        }

        /** The engine's strips [p] does not use (the master's index aside): emptied at each sync. */
        internal fun stripsToClear(p: Project): List<Int> {
            val used = usedStrips(p)
            return (0 until NativeEngine.MIXER_CHANNELS).filter { it != MASTER && it !in used }
        }

        /**
         * A strip's effects as engine commands: each slot's type (a switched-off effect: an empty
         * slot, its settings kept here), and the settings of the slots heard only (the engine
         * ignores them on an empty slot, and switching an effect on sends the slot again).
         */
        /** « Joue dans » as engine commands: each effect heard plays in [scene] or not (empty and switched-off slots: nothing). */
        internal inline fun insertOnCommands(inserts: List<Insert>, scene: Int, on: (slot: Int, on: Boolean) -> Unit) {
            inserts.forEachIndexed { slot, ins -> if (ins.heardType != 0) on(slot, ins.playsIn(scene)) }
        }

        internal inline fun insertCommands(inserts: List<Insert>, type: (slot: Int, type: Int) -> Unit, param: (slot: Int, k: Int, v: Float) -> Unit) {
            inserts.forEachIndexed { slot, ins ->
                type(slot, ins.heardType)
                if (ins.heardType != 0) ins.params.forEachIndexed { k, v -> param(slot, k, v) }
            }
        }
    }
}
