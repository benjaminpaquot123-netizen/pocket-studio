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
import com.pocketstudio.model.keepingVersion
import com.pocketstudio.model.takeUndoTarget
import com.pocketstudio.model.restoreVersion
import com.pocketstudio.model.dropVersion
import com.pocketstudio.model.liveQuantumSteps
import com.pocketstudio.model.liveLaunchSteps
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
import com.pocketstudio.model.inTime
import com.pocketstudio.model.withCaptured
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
import com.pocketstudio.model.PadVoice
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
import com.pocketstudio.model.routesPad
import com.pocketstudio.app.session.applyTo
import com.pocketstudio.app.session.doneText
import com.pocketstudio.app.session.refusal
import com.pocketstudio.model.partEnergy
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.MidiMapping
import com.pocketstudio.model.partView
import com.pocketstudio.model.stemView
import com.pocketstudio.model.partHeard
import com.pocketstudio.model.partSteps
import com.pocketstudio.model.writeTake
import com.pocketstudio.model.takeParts
import com.pocketstudio.model.liveStateOf
import com.pocketstudio.model.songCurveCommands
import com.pocketstudio.model.CurveCommand
import com.pocketstudio.model.Layers
import com.pocketstudio.model.addLayer
import com.pocketstudio.model.allLayers
import com.pocketstudio.model.allTrackSounds
import com.pocketstudio.model.editLayer
import com.pocketstudio.model.layersOf
import com.pocketstudio.model.removeLayer
import com.pocketstudio.model.setLayer
import com.pocketstudio.model.subAsLayer
import com.pocketstudio.model.hasPoints
import com.pocketstudio.model.withPartScene
import com.pocketstudio.model.sceneSteps
import com.pocketstudio.model.togglePartTrack
import com.pocketstudio.model.songBars
import com.pocketstudio.model.withBeatsPerBar
import com.pocketstudio.model.followSongIndex
import com.pocketstudio.model.songIndexAfterRemoval
import com.pocketstudio.model.songIndexAfterMove
import com.pocketstudio.model.songNextIndexLooped
import com.pocketstudio.model.splitSongPart
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
import com.pocketstudio.model.chooseTake
import com.pocketstudio.model.withSongRepeats
import com.pocketstudio.model.previousTakeFiles
import com.pocketstudio.model.versionSampleFiles
import com.pocketstudio.model.songCurvesAt
import com.pocketstudio.model.acidKnobsDriven
import com.pocketstudio.model.automationBefore
import com.pocketstudio.model.songStepOf
import com.pocketstudio.model.takeStart
import com.pocketstudio.model.withTake
import com.pocketstudio.model.withFirstTakeLevel
import com.pocketstudio.model.takeForgets
import com.pocketstudio.model.withTakes
import com.pocketstudio.model.addAudioTrack
import com.pocketstudio.model.exportEndBars
import com.pocketstudio.model.audioPlays
import com.pocketstudio.model.removeAudioTrack
import com.pocketstudio.model.withAudioTrack
import com.pocketstudio.model.instrumentTracks
import com.pocketstudio.model.multiPacks
import com.pocketstudio.model.track
import com.pocketstudio.model.withTrack
import com.pocketstudio.model.addTrack
import com.pocketstudio.model.sceneTitle
import com.pocketstudio.model.editedShownScene
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
import kotlinx.coroutines.flow.update
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
/** A microphone take in progress; [beatsLeft]: beats of count-in still to come (0 once it records). */
data class AudioRecordingUi(val track: Int, val waiting: Boolean = true, val seconds: Float = 0f, val level: Float = 0f, val song: Boolean = false, val beatsLeft: Int = 0)

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
    /** [cancellable]: an « Annuler » stops it (the song recorded onto a pad). */
    data class Busy(val message: String, val cancellable: Boolean = false) : SampleTask
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
    /**
     * The bar where the audio clips heard and the remix's song end (« Partager » without a song,
     * and « La scène » until a length is picked, last at least that long).
     */
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
    /** The exported mix's true peak (dBTP) and length (seconds), measured on the file; null for stems. */
    val truePeakDb: Double? = null,
    val seconds: Double? = null,
    /** S5: what the master chain took off the mix for its loudness target (dB, the « tassé » line); null without one. */
    val lossDb: Double? = null,
    /** Phase 38: an M4A's true peak once decoded again (what a player hears), null when not checked. */
    val decodedPeakDb: Double? = null,
    /** The file written: its format, rate and bits (the report says them). */
    val writtenRate: Int = 48000,
    val writtenBits: Int = 24,
    /** The format the last export was written in (« Partager » may use another than the card's). */
    val writtenFormat: ExportFormat? = null,
    /** « 44,1 kHz » (the CD's rate, resampled after the master) instead of 48 kHz. */
    val cdRate: Boolean = false,
    /** « 16 bits » (dithered) instead of 24 for WAV and FLAC. */
    val bits16: Boolean = false,
    val error: String? = null,
) {
    val file: File? get() = files.firstOrNull()

    /** The end-of-export report in plain words (phase 38), for the mix (null for separate tracks). */
    val summary: com.pocketstudio.app.export.ExportSummary? get() = loudness?.let {
        com.pocketstudio.app.export.ExportSummary(
            seconds, it, decodedPeakDb ?: truePeakDb, checkedAfterCompression = decodedPeakDb != null,
            sampleRate = writtenRate, bits = writtenBits,
            format = when (writtenFormat ?: format) { ExportFormat.M4a -> "M4A"; ExportFormat.Flac -> "FLAC"; ExportFormat.Wav -> "WAV" },
        )
    }

    /**
     * These settings for a scene [sceneBars] long whose audio clips and remix end on bar [audioBars]:
     * the longer of the two is the default (a remix is exported to its end, not cut after the scene).
     */
    fun forScene(sceneBars: Int, audioBars: Int = 0): ExportUi = copy(
        sceneBars = sceneBars, audioBars = audioBars,
        bars = if (barsChosen) bars else maxOf(sceneBars, audioBars.coerceAtMost(RhythmViewModel.MAX_LONG_EXPORT_BARS)),
    )

    /** These settings for a project: the scene's length, the song if there is one, the fade that goes with it. */
    fun forProject(sceneBars: Int, hasSong: Boolean, audioBars: Int = 0): ExportUi {
        val w = if (whatChosen) what else if (hasSong) ExportWhat.Song else ExportWhat.Loop
        val f = when {
            w == ExportWhat.Stems -> ExportFade.None
            fadeChosen -> fade
            else -> defaultFade(w)
        }
        return forScene(sceneBars, audioBars).copy(what = w, fade = f)
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

    /**
     * The part that comes next in a song of [size] parts, or -1 (not playing the song, or it ends);
     * [loop]: the parts the timeline loops over (its last one leads back to its first).
     */
    fun upcoming(size: Int, loops: Boolean, loop: IntRange? = null): Int = when {
        songIndex < 0 -> queued.takeIf { pending != null && it in 0 until size } ?: -1
        ending -> -1
        else -> songNextIndexLooped(songIndex, queued, size, loops, loop)
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
    private val store = ProjectOpening.projectStore(app)

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
    // S6 (G5), see estimateLiveDrive: declared before the init blocks that start it.
    private var liveDriveJob: kotlinx.coroutines.Job? = null
    /** The wait before estimating again after a change of sound or mix (watchSoundForLiveDrive). */
    private var liveDriveSettle: kotlinx.coroutines.Job? = null
    /** The drive last given to the live engine (giveKnownDrive); null: none yet. */
    private var liveDriveGiven: Float? = null
    /** What the drive was estimated for ([liveDriveKeyOf]); null: not yet. */
    private var liveDriveKey: Int? = null
    /** What an estimate was last tried for, found or not: a failed one is not tried again at each ▶. */
    private var liveDriveTriedKey: Int? = null
    /** The estimate given again with the song's own target, at ▶ (once per estimate). */
    private var liveDrivePlayedKey: Int? = null
    /** An export's own drive came in: an estimate does not take its place. */
    private var liveDriveFromExport = false
    /** The estimated drive (dB), for tests and the bug report's journal. */
    @Volatile internal var liveDriveDb: Float? = null
        private set
    /** S7 (mix version 4): the loud part's level before the drive that [liveDriveDb] was found on (LUFS; NaN: none). */
    @Volatile internal var liveDriveLevel: Float = Float.NaN
        private set

    /**
     * The project as read before the screens were made ([ProjectOpening.prepare], off the main
     * thread); read here only when nothing was (the tests, a screen made another way), the saves
     * of a screen just closed waited for 3 s at most in all.
     */
    private val opened: OpenedProject = (
        ProjectOpening.take(projectId) ?: run {
            val deadline = System.currentTimeMillis() + 3000
            TakeVault.app.awaitIdleBlocking(3000)
            ClosingSaves.awaitIdleBlocking((deadline - System.currentTimeMillis()).coerceAtLeast(0))
            ProjectOpener(projectId, store, journals).open()
        }
    ).also { o -> o.notice?.let { _notice.value = it } }

    /** True when the app opened on no project because the last one crashed it twice as it opened. */
    val rescued: Boolean get() = opened.rescued

    val session: ProjectSession = ProjectSession(
        id = projectId,
        initial = opened.project,
        store = store,
        scope = viewModelScope,
        io = Dispatchers.IO,
        // Lot 1: no project open (the last one deleted): a blank one in memory, never written.
        persist = !com.pocketstudio.app.projects.CurrentProject.isNone(projectId),
    )
    val project: StateFlow<Project> = session.project

    /**
     * The Guide (docs/PLAN-GUIDE.md): its analysis follows the project (debounced, off the main
     * thread); its buttons edit through the session, one undo step each, then [afterGuideEdit].
     * Made when its panel first asks for it.
     */
    val guide: com.pocketstudio.app.guide.GuideViewModel by lazy {
        com.pocketstudio.app.guide.GuideViewModel(
            session, viewModelScope, onEdited = ::afterGuideEdit, measurer = ::measureGuide,
            pauseMusic = { if (transport.value.playing) togglePlay() },
            canMeasure = { !recording.value && audioRecording.value == null },
            playing = { transport.value.playing },
        )
    }

    /** Engine updates the Guide's edits asked for (tests). */
    internal var guideSyncs = 0
        private set

    /**
     * After a Guide's step: nothing for the engine when only the Guide's own state changed (a card
     * skipped, a note); otherwise what plays follows as after an undo (the mixer, the part playing
     * with its muted tracks, the next part staged again), and more long effects than the engine
     * holds are said (the Guide refuses to add them; one already over stays said).
     */
    private fun afterGuideEdit(before: Project) {
        val now = project.value
        if (now.copy(guide = before.guide) == before) return
        guideSyncs++
        followPlaybackAfterHistory(sceneHeard(), before.song)
        warnLongFxOverLimit(before)
        // G3: a layer brought back from a track's sheet: its instrument and its General MIDI sound come back too.
        if (now.allLayers != before.allLayers) {
            loadTrackFonts()
            loadMultiBanks()
        }
    }

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
        liveMulti.clear()
        pushedLayers.fill(null)
        pushedStacks.clear()
        pushedVoices = null
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
        NativeEngine.setKitVersion(p.kitVersion)
        NativeEngine.setSoundVersion(p.soundVersion)
        NativeEngine.setMixVersion(p.mixVersion)
        pushPadVoices(p)
        NativeEngine.setDrumSeed(p.drumSeed)
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
        if (project.value.live.clips.getOrNull(pad) == null) return
        // Started with the transport stopped (their clock runs on its own): the music plays all the same.
        val starts = !_transport.value.playing && pad !in padsSoundingNow()
        if (NativeEngine.isAvailable) NativeEngine.toggleClip(pad)
        livePadTapped(pad)
        if (starts) watchPadsAlone()
    }

    private val _padsAlone = MutableStateFlow(false)
    /**
     * Live pads sound with the transport stopped: the music plays as with ▶ (in the background,
     * screen off, the headphones out or a call stopping it, the screen kept on).
     */
    val padsAlone: StateFlow<Boolean> = _padsAlone.asStateFlow()
    private var padsWatch: kotlinx.coroutines.Job? = null

    /** The live pads sounding or waiting now, as the engine says. */
    private fun padsSoundingNow(): Set<Int> {
        livePadsSoundingForTest?.let { return it }
        if (!NativeEngine.isAvailable) return emptySet()
        val buf = IntArray(NativeEngine.LIVE_PADS)
        NativeEngine.clipStates(buf)
        return buf.indices.filterTo(HashSet()) { buf[it] != 0 }
    }

    /** A pad started with the transport stopped: [padsAlone] until they are all off (or ▶ takes over). */
    private fun watchPadsAlone() {
        setPadsAlone(true)
        if (padsWatch?.isActive == true) return
        padsWatch = viewModelScope.launch {
            do { kotlinx.coroutines.delay(250) } while (!_transport.value.playing && padsSoundingNow().isNotEmpty())
            setPadsAlone(false)
        }
    }

    private fun setPadsAlone(on: Boolean) {
        if (_padsAlone.value == on) return
        _padsAlone.value = on
        AppLog.add(if (on) "Pads live sans lecture : le son reste ouvert" else "Pads live arrêtés")
        // The service holds the headphones' and the calls' stops (they call stopLive) and the background.
        val app = getApplication<Application>()
        if (on) PlaybackService.start(app) else if (!_transport.value.playing) PlaybackService.stopAfterTakes(app)
    }

    fun stopLive() {
        if (NativeEngine.isAvailable) NativeEngine.toggleClip(-1)
        livePadTapped(-1)
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

    /**
     * Lot 2: the track a melody goes to in the Mode simple (« ● Enregistrer ma mélodie »): the one
     * named « Lead » or « Mélodie » (the assistant's lead, « + Piste › Mélodie »), the last such
     * one; null when the song has none (the track shown stays).
     */
    fun melodyTrackId(): Int? =
        com.pocketstudio.model.SimpleMelody.trackOf(project.value) ?: project.value.tracks.lastOrNull { isMelodyName(it.name) }?.id

    /**
     * Audit final (01 M3, 05 #5): the Mode simple's melody goes on « Ma mélodie », made at its first
     * take with the sound of the song's melody track (a melody sound of the style without one), and
     * selected. Never the assistant's riff, never the bass. Null when the eight tracks are taken.
     */
    fun simpleMelodyTrack(): Int? {
        val p = project.value
        com.pocketstudio.model.SimpleMelody.trackOf(p)?.let { selectTrack(it); return it }
        val like = p.tracks.lastOrNull { isMelodyName(it.name) }?.id
        val style = com.pocketstudio.app.library.styleOfProject(p.name)?.let(com.pocketstudio.model.StarterWizard::style)
        val next = com.pocketstudio.model.SimpleMelody.add(p, like, style) ?: run {
            _notice.value = tr("Huit pistes d'instruments au maximum : supprime-en une pour en ajouter une autre.")
            return null
        }
        val id = next.tracks.first { t -> p.tracks.none { it.id == t.id } }.id
        session.edit { next }
        pushTracks()
        pushMixer()
        selectTrack(id)
        AppLog.add("Piste ajoutée : ${com.pocketstudio.model.SimpleMelody.NAME}")
        return id
    }

    /** The Mode simple's « ● Enregistrer ma mélodie »: a take of the keys on « Ma mélodie » (or the take recording ends). */
    fun recordMelodyTake() {
        if (_recording.value) {
            recordTake(com.pocketstudio.app.record.TakeSource.KEYS)
            return
        }
        if (_audioRecording.value != null || performanceTaking) return
        simpleMelodyTrack() ?: return
        recordTake(com.pocketstudio.app.record.TakeSource.KEYS, melody = true)
    }

    fun selectTrack(id: Int) {
        if (id == track) return
        heldNotes.clear()
        sustained.clear()
        val from = track
        _selectedTrack.value = id
        NativeEngine.liveTrack = id
        movePlay(from, id)
    }

    /**
     * Phase 37: a track the user adds to the assistant's song keeps its notes as written when the
     * song's chords change (the assistant's own tracks follow them); « Suit les accords » changes it.
     */
    private fun userTrack(p: Project, id: Int): Project =
        if (p.style == null) p else p.withTrack(p.track(id).copy(followChords = false))

    private fun editTrack(key: String? = null, id: Int = track, transform: (InstrumentTrack) -> InstrumentTrack) {
        // A track deleted meanwhile (a sheet still open on it): nothing, never the first track in its place.
        session.edit(key) { p -> if (p.instrumentTracks.none { it.id == id }) p else p.withTrack(transform(p.track(id)).sanitized()) }
    }

    /** Track [id] is in the project (a sheet may still be open on one just deleted). */
    private fun hasTrack(id: Int): Boolean = project.value.instrumentTracks.any { it.id == id }

    /** A new instrument track (with its own mixer channel), selected at once. */
    fun addTrack() {
        val p = project.value
        val next = p.addTrack() ?: run {
            _notice.value = tr("Huit pistes d'instruments au maximum : supprime-en une pour en ajouter une autre.")
            return
        }
        val id = next.tracks.first { t -> p.tracks.none { it.id == t.id } }.id
        session.edit { userTrack(next, id) }
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
        session.edit { userTrack(next, id) }
        pushTracks()
        pushMixer()
        selectTrack(id)
        if (write) _notice.value = tr("Mélodie écrite : 4 mesures dans la tonalité du morceau, dans la partie %s. « Annuler » la retire.", project.value.sceneTitle(project.value.scene))
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
            pushMulti(p, t.id)
            pushPlaits(p, t.id)
            pushNotes(t.notes, t.id)
        }
        pushInstrument(p)
        for (id in 0 until InstrumentTrack.MAX) pushLayers(p, id)
        if (p === project.value) loadMultiBanks()
    }

    // --- G1b: a track's layers (Track.layers), more sounds playing its notes ---------------------

    /**
     * Every layer of track [id] to the engine: its sound (as a sound of its own, [Layers.soundIndex]),
     * how it plays the track's notes, its strip ([Layers.channel]); the layers it does not have (or
     * a track that is not there) are switched off and their strips emptied. Its General MIDI
     * instrument comes with [pushInstrument]. Listening goes back to every sound ([soloLayer]).
     */
    private fun pushLayers(p: Project, id: Int) {
        if (!NativeEngine.isAvailable) return
        val layers = if (id == 0 || p.tracks.any { it.id == id }) p.track(id).layers else emptyList()
        for (k in 0 until NativeEngine.LAYERS) {
            val l = layers.getOrNull(k)
            val ch = NativeEngine.layerChannel(id, k)
            if (l == null) {
                // Already off in the engine (this project never had it there): nothing to send.
                if (!NativeEngine.isCapturing && pushedLayers[id * NativeEngine.LAYERS + k] == false) continue
                NativeEngine.setLayer(id, k, NativeEngine.LAYER_ON, 0f)
                NativeEngine.clearInserts(ch)
                if (!NativeEngine.isCapturing) {
                    // Its sample and its instrument's zones let go (up to 100 MB a pack), whatever project comes next.
                    val none = com.pocketstudio.model.LayerSound().asTrack(NativeEngine.layerSound(id, k))
                    pushSampledSound(none)
                    pushMultiSound(none)
                    pushedLayers[id * NativeEngine.LAYERS + k] = false
                }
                continue
            }
            pushLayer(p, id, k, l)
        }
        // An export hears every sound; live, a layer heard alone stays so while the stack is the same
        // (an undo that moves or removes layers would shift the mask onto another one).
        if (!NativeEngine.isCapturing) {
            val stack = layers.map { it.sound to it.role }
            if (pushedStacks[id] != stack) listening.remove(id)
            pushedStacks[id] = stack
        }
        val present = (1 shl (layers.size + 1)) - 1
        val mask = if (NativeEngine.isCapturing) NativeEngine.LISTEN_ALL
        else listening[id]?.and(present)?.takeIf { it != 0 && it != present } ?: NativeEngine.LISTEN_ALL
        if (!NativeEngine.isCapturing && mask == NativeEngine.LISTEN_ALL) listening.remove(id)
        NativeEngine.setLayerListen(id, mask)
    }

    /** Layer [k] of track [id]: its sound, then how it plays and mixes, then on. */
    private fun pushLayer(p: Project, id: Int, k: Int, l: com.pocketstudio.model.TrackLayer) {
        val sound = l.sound.asTrack(NativeEngine.layerSound(id, k))
        pushSynth(sound.synth, sound.id)
        pushSampledSound(sound)
        pushMultiSound(sound)
        pushPlaitsSound(sound)
        val ch = NativeEngine.layerChannel(id, k)
        NativeEngine.setLayer(id, k, NativeEngine.LAYER_LOW, l.low.toFloat())
        NativeEngine.setLayer(id, k, NativeEngine.LAYER_HIGH, l.high.toFloat())
        NativeEngine.setLayer(id, k, NativeEngine.LAYER_TRANSPOSE, l.transpose.toFloat())
        NativeEngine.setLayer(id, k, NativeEngine.LAYER_FINE, l.fine / 100f)
        NativeEngine.setLayer(id, k, NativeEngine.LAYER_GAIN, l.engineGain)
        NativeEngine.setLayer(id, k, NativeEngine.LAYER_THROUGH, if (l.throughTrackFx) 1f else 0f)
        NativeEngine.setChannelPan(ch, l.pan)
        NativeEngine.setChannelMute(ch, l.mute)
        l.sends.forEachIndexed { bus, level -> NativeEngine.setSend(ch, bus, level) }
        // Its effects in the strip's first slots, the others emptied (no scenes of their own: always on).
        val inserts = (l.inserts + List(NativeEngine.INSERTS) { Insert() }).take(NativeEngine.INSERTS)
        insertCommands(inserts, { slot, type -> NativeEngine.setInsertType(ch, slot, type) }) { slot, key, v -> NativeEngine.setInsertParam(ch, slot, key, v) }
        NativeEngine.setLayer(id, k, NativeEngine.LAYER_ON, 1f)
        if (!NativeEngine.isCapturing) pushedLayers[id * NativeEngine.LAYERS + k] = true
    }

    /** Each track's layers as last pushed live (their sounds and roles): a stack changed lets go of its listening. */
    private val pushedStacks = HashMap<Int, List<Pair<com.pocketstudio.model.LayerSound, String>>>()

    /** Which layers the live engine has on (null: not known, sent at the next push). */
    private val pushedLayers = arrayOfNulls<Boolean>(InstrumentTrack.MAX * NativeEngine.LAYERS)

    /** The tracks heard partly (« cette couche seule », « sans elle »): their listening mask, live only. */
    private val listening = HashMap<Int, Int>()

    /** Tests: what the live engine was last told of each track's listening (absent: every sound). */
    internal fun listeningForTest(id: Int): Int = listening[id] ?: NativeEngine.LISTEN_ALL

    /** Track [id]'s layers changed ([before]: as they were): what changed goes to the engine. */
    private fun pushLayersChange(id: Int, before: List<com.pocketstudio.model.TrackLayer>) {
        if (!NativeEngine.isAvailable) return
        val p = project.value
        val now = p.layersOf(id)
        // Fewer or moved layers: the whole stack again (each layer's strip takes its place).
        if (now.size != before.size || now.indices.any { now[it].sound != before[it].sound && now[it].role != before[it].role }) {
            pushLayers(p, id)
            pushInstrument(p)
            loadTrackFonts()
            loadMultiBanks()
            return
        }
        now.forEachIndexed { k, l ->
            val was = before[k]
            if (l == was) return@forEachIndexed
            if (l.sound != was.sound) {
                pushLayer(p, id, k, l)
                pushInstrument(p)
                loadTrackFonts()
                loadMultiBanks()
                return@forEachIndexed
            }
            val ch = NativeEngine.layerChannel(id, k)
            if (l.low != was.low) NativeEngine.setLayer(id, k, NativeEngine.LAYER_LOW, l.low.toFloat())
            if (l.high != was.high) NativeEngine.setLayer(id, k, NativeEngine.LAYER_HIGH, l.high.toFloat())
            if (l.transpose != was.transpose) NativeEngine.setLayer(id, k, NativeEngine.LAYER_TRANSPOSE, l.transpose.toFloat())
            if (l.fine != was.fine) NativeEngine.setLayer(id, k, NativeEngine.LAYER_FINE, l.fine / 100f)
            if (l.gainDb != was.gainDb) NativeEngine.setLayer(id, k, NativeEngine.LAYER_GAIN, l.engineGain)
            if (l.throughTrackFx != was.throughTrackFx) NativeEngine.setLayer(id, k, NativeEngine.LAYER_THROUGH, if (l.throughTrackFx) 1f else 0f)
            if (l.pan != was.pan) NativeEngine.setChannelPan(ch, l.pan)
            if (l.mute != was.mute) NativeEngine.setChannelMute(ch, l.mute)
            l.sends.forEachIndexed { bus, level -> if (was.sends.getOrNull(bus) != level) NativeEngine.setSend(ch, bus, level) }
            if (l.inserts != was.inserts) {
                val inserts = (l.inserts + List(NativeEngine.INSERTS) { Insert() }).take(NativeEngine.INSERTS)
                insertCommands(inserts, { slot, type -> NativeEngine.setInsertType(ch, slot, type) }) { slot, key, v -> NativeEngine.setInsertParam(ch, slot, key, v) }
            }
        }
    }

    /** Track [id]'s layers as an edit (one undo step; slider drags merge into one per [key]). */
    private fun editLayers(id: Int, key: String?, edit: (Project) -> Project?): Boolean {
        if (!hasTrack(id)) return false
        val before = project.value.layersOf(id)
        var done = false
        // G3: kept in the track's history too (its sheet's « Historique », « Ramener »), in the same undo step.
        val at = System.currentTimeMillis()
        session.edit(key) { p -> edit(p)?.also { done = true }?.let { com.pocketstudio.model.guide.GuideActions.withTrackEdit(p, it, at, merge = key != null) } ?: p }
        if (done) pushLayersChange(id, before)
        return done
    }

    // --- G3: a track's sheet, « Ailleurs ? » -------------------------------------------------------

    /**
     * « Copier cette chaîne sur… »: the effects and sends of track [from] on track [to] (Guide track
     * keys: "inst:3", "pad:0"…), one undo step, kept in [to]'s history; refused and said when it is
     * already the same or past the engine's long effects.
     */
    fun copyChain(from: String, to: String) {
        val out = com.pocketstudio.model.guide.GuideActions.tryCopyChain(project.value, from, to, System.currentTimeMillis())
        out.problem?.let { _notice.value = it }
        if (out.project === project.value) return
        session.edit { out.project }
        pushMixer()
        pushAutomation()
        _notice.value = tr("Chaîne copiée. Annuler, ou « Ramener » dans la fiche, la retire.")
    }

    /** « Garder ce son dans mes favoris » for track [id]: a synth's or a Plaits sound (the others cannot be kept), said. */
    fun saveFavoriteOf(id: Int) {
        if (!hasTrack(id)) return
        val t = project.value.track(id)
        val f = _favorites.value
        when (t.engine) {
            InstrumentTrack.ENGINE_SYNTH -> if (t.instrument == null) {
                val name = Favorites.nextName(f.synth.map { it.name })
                addFavorite(f.synth) { f.copy(synth = f.synth + NamedSound(name, SoundCategories.FAVORITES, t.synth.sanitized())) }
                if (_favorites.value.synth.size > f.synth.size) _notice.value = tr("Son gardé dans tes favoris : « %s ».", name)
                return
            }
            InstrumentTrack.ENGINE_PLAITS -> {
                val name = Favorites.nextName(f.plaits.map { it.name })
                addFavorite(f.plaits) { f.copy(plaits = f.plaits + NamedSound(name, SoundCategories.FAVORITES, t.plaits)) }
                if (_favorites.value.plaits.size > f.plaits.size) _notice.value = tr("Son gardé dans tes favoris : « %s ».", name)
                return
            }
        }
        _notice.value = tr("Seuls les sons du synthé et de Plaits se gardent en favori.")
    }

    /** A new layer of [sound] on track [id] for [role] ([com.pocketstudio.model.LayerRole]): one undo step. False when it has three. */
    fun addLayer(id: Int, sound: com.pocketstudio.model.LayerSound, role: String = "", label: String = ""): Boolean {
        val ok = editLayers(id, null) { it.addLayer(id, sound, role, label) }
        if (!ok && hasTrack(id)) _notice.value = tr("Quatre sons par piste au maximum : retire une couche pour en ajouter une autre.")
        return ok
    }

    /** Layer [index] of track [id] removed: one undo step (« Annuler » brings it back). */
    fun removeLayer(id: Int, index: Int) {
        if (index !in project.value.layersOf(id).indices) return
        listening.remove(id)
        editLayers(id, null) { it.removeLayer(id, index) }
    }

    /** A setting of layer [index] of track [id] (null: as it is). A drag merges into one undo step per [key]. */
    fun setLayer(
        id: Int, index: Int, key: String? = null, gainDb: Float? = null, pan: Float? = null, low: Int? = null, high: Int? = null,
        transpose: Int? = null, fine: Float? = null, mute: Boolean? = null, throughTrackFx: Boolean? = null,
    ) {
        if (index !in project.value.layersOf(id).indices) return
        editLayers(id, key?.let { "layer-$id-$index-$it" }) { it.setLayer(id, index, gainDb, pan, low, high, transpose, fine, mute, throughTrackFx) }
    }

    /** Layer [index] of track [id] changed by [transform] (its sound, its effects…): one undo step per [key]. */
    fun editLayer(id: Int, index: Int, key: String? = null, transform: (com.pocketstudio.model.TrackLayer) -> com.pocketstudio.model.TrackLayer) {
        if (index !in project.value.layersOf(id).indices) return
        editLayers(id, key?.let { "layer-$id-$index-$it" }) { it.editLayer(id, index, transform) }
    }

    /**
     * Listening, live (saved nowhere): only layer [only] of track [id] ([Layers.OWN]: only its own
     * sound), or every sound again (null). [without]: every sound but that one (« sans cette couche »).
     */
    fun soloLayer(id: Int, only: Int?, without: Boolean = false) {
        if (!hasTrack(id)) return
        val mask = when {
            only == null -> NativeEngine.LISTEN_ALL
            without -> Layers.without(only)
            else -> Layers.only(only)
        }
        if (mask == NativeEngine.LISTEN_ALL) listening.remove(id) else listening[id] = mask
        if (NativeEngine.isAvailable) NativeEngine.setLayerListen(id, mask)
    }

    /**
     * « Écouter avec / sans cette couche », offline: [bars] bars of what plays (the part shown, or
     * the loop), rendered twice by a private engine as an export renders it, with every sound of
     * track [id] and without its layer [index] ([Layers.OWN]: without its own sound), the louder of
     * the two brought down to the other's loudness (the same LUFS: the ear judges the sound, not the
     * level). Interleaved stereo at [OfflineExport.SAMPLE_RATE]: (with, without); null when it cannot.
     * Main thread (the description), the renders on a worker.
     */
    suspend fun renderLayerAB(id: Int, index: Int, bars: Int = 8): Pair<FloatArray, FloatArray>? {
        val count = project.value.layersOf(id).size
        if (!NativeEngine.isAvailable || count == 0 || (index != Layers.OWN && index !in 0 until count)) return null
        // As an export: the sounds in first (no synth stand-in), then the part shown.
        awaitSoundsLoaded()
        val p = project.value
        if (p.layersOf(id).size != count) return null
        val frames = OfflineExport.framesFor(bars, p.tempoBpm, p.beatsPerBar)
        val with = renderOffline(p, frames) { NativeEngine.setLayerListen(id, NativeEngine.LISTEN_ALL) } ?: return null
        val without = renderOffline(p, frames) { NativeEngine.setLayerListen(id, Layers.without(index)) } ?: return null
        return matchLoudness(with, without)
    }

    /**
     * The Guide's « écouter avec / sans » (docs/PLAN-GUIDE.md §4.3): the song's first Drop (else
     * Refrain; else its first part, else the scene shown) as [with] and as [without] play it, up to
     * [maxBars] bars each, rendered offline as an export renders it, the louder brought down to the
     * other's loudness. (with, without), interleaved stereo at [OfflineExport.SAMPLE_RATE]; null
     * when it cannot (no engine). Main thread (the descriptions), the renders on a worker.
     */
    suspend fun renderGuideAB(with: Project, without: Project, maxBars: Int = 8): Pair<FloatArray, FloatArray>? {
        guideRenderForTest?.let { return it(with, without) }
        if (!NativeEngine.isAvailable) return null
        awaitSoundsLoaded()
        val a = renderGuidePeak(with, maxBars) ?: return null
        val b = renderGuidePeak(without, maxBars) ?: return null
        return matchLoudness(a, b)
    }

    /** Tests (G6): the Guide's « Sans / Avec » rendered here in place of the engine (Robolectric has none). */
    @androidx.annotation.VisibleForTesting
    internal var guideRenderForTest: (suspend (with: Project, without: Project) -> Pair<FloatArray, FloatArray>?)? = null

    /** The song's first Drop (else Refrain, else its first part, else the scene shown), up to [maxBars] bars, as the Guide's listenings hear it. */
    private suspend fun renderGuidePeak(p: Project, maxBars: Int = 8): FloatArray? {
        val part = com.pocketstudio.model.FirstListen.firstMain(p).coerceAtLeast(0)
        val steps = p.song.getOrNull(part)?.let { p.partSteps(it) } ?: p.sceneSteps(p.scene)
        val bars = (steps / p.stepsPerBar.coerceAtLeast(1)).coerceIn(1, maxBars)
        return renderOffline(p.partHeard(part), OfflineExport.framesFor(bars, p.tempoBpm, p.beatsPerBar)) {}
    }

    /** G4: the reference the Guide compares the song with (« À côté d'un morceau que tu aimes »), kept while the app runs. */
    private val _guideReference = MutableStateFlow<com.pocketstudio.app.guide.GuideReference?>(null)
    val guideReference: StateFlow<com.pocketstudio.app.guide.GuideReference?> = _guideReference.asStateFlow()

    /** An audio file picked as the reference: its loudest 16 s, decoded off the main thread. False when it does not read. */
    suspend fun loadGuideReference(uri: Uri): Boolean {
        val app = getApplication<Application>()
        val name = withContext(Dispatchers.IO) { displayName(app, uri) }?.substringBeforeLast('.')?.takeIf { it.isNotBlank() } ?: tr("Morceau choisi")
        val excerpt = runCatching {
            withContext(Dispatchers.IO) { com.pocketstudio.app.guide.GuideChecks.loudestExcerpt(AudioDecoder.decode(app, uri), REFERENCE_SECONDS) }
        }.getOrNull()?.takeIf { it.size >= 2 * OfflineExport.SAMPLE_RATE } ?: return false
        _guideReference.value = com.pocketstudio.app.guide.GuideReference(name, excerpt)
        return true
    }

    /**
     * G4: a check of stage 6 heard (docs/PLAN-GUIDE.md §3.5): [name] "mono" (the first Drop in stereo,
     * then folded to mono, as it is: what fades away is the point), "phone" (as it is, then through a
     * phone's speaker, at the same loudness), "reference" (the Drop, then the reference's loudest
     * passage, at the same loudness). Null when it cannot (no engine, no reference).
     */
    suspend fun renderGuideCheck(name: String, p: Project): com.pocketstudio.app.guide.GuideCheckTakes? {
        if (!NativeEngine.isAvailable) return null
        val ref = if (name == "reference") _guideReference.value ?: return null else null
        awaitSoundsLoaded()
        val peak = renderGuidePeak(p) ?: return null
        val checks = com.pocketstudio.app.guide.GuideChecks
        return withContext(Dispatchers.Default) {
            when (name) {
                "mono" -> com.pocketstudio.app.guide.GuideCheckTakes(peak, checks.mono(peak), matched = false)
                "phone" -> matchLoudness(peak, checks.phone(peak)).let { (a, b) -> com.pocketstudio.app.guide.GuideCheckTakes(a, b, matched = true) }
                "reference" -> {
                    val diff = (checks.lufs(ref!!.excerpt) ?: 0.0) - (checks.lufs(peak) ?: 0.0)
                    matchLoudness(peak, ref.excerpt).let { (a, b) -> com.pocketstudio.app.guide.GuideCheckTakes(a, b, matched = true, louderDb = diff.toFloat()) }
                }
                else -> null
            }
        }
    }

    /**
     * G4: the Guide's measures of [p] (stage 6): the loudness of each kind of section (its last part,
     * the master's target left out as the « Sans / Avec » does), each track heard alone in the first
     * peak (the kick and the instruments), what the peak loses in mono, and what a phone's speaker keeps
     * of the basses. A few short offline renders (8 s at most each, about 15 of them); null when it
     * cannot (no engine, no song). Main thread (the descriptions), the renders on a worker.
     */
    suspend fun measureGuide(p: Project): com.pocketstudio.model.guide.GuideMeasures? {
        if (!NativeEngine.isAvailable || p.song.isEmpty()) return null
        awaitSoundsLoaded()
        val song = com.pocketstudio.model.guide.GuideSong(p)
        val roles = com.pocketstudio.model.guide.GuideRoles
        val peakPart = song.sectionsOf(com.pocketstudio.model.guide.GuideSection.Kind.PEAK).firstOrNull()?.parts?.last()
            ?: com.pocketstudio.model.FirstListen.firstMain(p).coerceAtLeast(0)
        val barSeconds = p.beatsPerBar * 60.0 / p.tempoBpm
        fun frames(part: Int, seconds: Double): Long {
            val bars = minOf(song.partBars.getOrElse(part) { 1 }, maxOf(2, (seconds / barSeconds).toInt()))
            return OfflineExport.framesFor(bars, p.tempoBpm, p.beatsPerBar)
        }
        val checks = com.pocketstudio.app.guide.GuideChecks
        suspend fun lufsOf(view: Project, part: Int, seconds: Double): Float? =
            renderOffline(view, frames(part, seconds)) {}?.let { a -> withContext(Dispatchers.Default) { checks.lufs(a)?.toFloat() } }
        val sections = HashMap<String, Float>()
        for (kind in song.sections.map { it.kind }.distinct()) {
            val i = song.sectionsOf(kind).first().parts.last()
            lufsOf(p.partHeard(i), i, MEASURE_SECONDS)?.let { sections[kind.name] = it }
        }
        val heard = p.partHeard(peakPart)
        val full = renderOffline(heard, frames(peakPart, MEASURE_SECONDS)) {} ?: return null
        val (integrated, mono) = withContext(Dispatchers.Default) { checks.lufs(full)?.toFloat() to checks.monoLossDb(full) }
        // Each track alone: its strip soloed (a pad's own strip for the kick).
        fun solo(channels: Set<Int>): Project = heard.copy(mixer = heard.mixer.copy(channels = heard.mixer.channels.mapIndexed { i, c -> c.copy(solo = i in channels) }))
        val inPeak = (song.partStarts[peakPart] until song.partStarts[peakPart] + song.partBars[peakPart])
        val tracks = song.tracks.filter { t -> inPeak.any { song.sounds(t.key, it) } }
        val measured = tracks.filter { it.role == roles.KICK }.take(1) +
            tracks.filter { it.key.startsWith("inst:") || it.key == com.pocketstudio.model.guide.GuideTrack.ACID || it.key.startsWith("audio:") }
        val levels = HashMap<String, Float>()
        for (t in measured) lufsOf(solo(setOf(t.channel)), peakPart, SOLO_SECONDS)?.let { levels[t.key] = it }
        val lows = tracks.filter { it.role in setOf(roles.BASS, roles.SUB, roles.BASS2) }.map { it.channel }.toSet()
        val phone = if (lows.isEmpty()) null else renderOffline(solo(lows), frames(peakPart, SOLO_SECONDS)) {}?.let { a -> withContext(Dispatchers.Default) { checks.phoneDb(a) } }
        return com.pocketstudio.model.guide.GuideMeasures(
            at = System.currentTimeMillis(), integratedLufs = integrated, sections = sections, tracks = levels, monoLossDb = mono, phoneDb = phone,
            signature = withContext(Dispatchers.Default) { com.pocketstudio.model.guide.GuideSignatures.sound(p) },
        )
    }

    /** [p] rendered offline for [frames] frames by a private engine, no metronome, [setup] said after it; null when it cannot. */
    private suspend fun renderOffline(p: Project, frames: Long, setup: () -> Unit): FloatArray? {
        val job = finishOrRelease(Dispatchers.IO, OfflineExport::close) { OfflineExport() } ?: return null
        try {
            NativeEngine.captureInto(job) {
                syncEngine(p)
                NativeEngine.setMetronome(false)
                setup()
            }
            return withContext(Dispatchers.Default) { job.renderToArray(frames, 0) }.takeIf { it.isNotEmpty() }
        } finally {
            withContext(kotlinx.coroutines.NonCancellable + Dispatchers.Main) { job.close() }
        }
    }

    /** The bass music sub folded into its bass as its « Sub » layer (SubLayers): one undo step. False when it stays apart. */
    fun foldSubIntoBass(sub: Int): Boolean {
        val bass = com.pocketstudio.model.SubLayers.bassOf(project.value, sub) ?: return false
        if (project.value.subAsLayer(bass, sub) == null) return false
        session.edit { p -> p.subAsLayer(bass, sub) ?: p }
        syncEngine()
        return true
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
            if (!waitsFor(LiveSlots.audio(id)) || _liveGrid.value?.diverged != true) NativeEngine.setAudioOn(id, audioPlays(p, id, part, alone))
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
            _notice.value = tr("Huit pistes audio au maximum : supprime-en une pour en ajouter une autre.")
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
            _notice.value = tr("Les huit pistes audio ont déjà une prise : touche « Nouvelle prise » sur celle à refaire.")
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
        it.withOffset((Math.round(ms / AudioClip.OFFSET_STEP_MS) * AudioClip.OFFSET_STEP_MS).coerceIn(-AudioClip.MAX_OFFSET_MS, AudioClip.MAX_OFFSET_MS))
    }

    /**
     * « Prise précédente »: the take the last one replaced comes back (one undo step), and the
     * newer one becomes the previous (a second touch swaps them back).
     */
    fun restorePreviousTake(id: Int) = chooseTake(id, 0)

    /**
     * The take picker: take [index] of the track's other takes ([AudioTrack.otherTakes], newest
     * first) plays, and the one it replaces is kept first among them. One undo step.
     */
    fun chooseTake(id: Int, index: Int) {
        val t = project.value.audioTracks.firstOrNull { it.id == id } ?: return
        if (index !in t.otherTakes.indices || _audioRecording.value != null || _takeSaving.value) return
        session.edit { it.chooseTake(id, index) }
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
            // Audit 17: the screen gone meanwhile (another project opened), what was read is freed
            // and nothing of this project reaches the engine (the cancellation is not swallowed).
            val prepared = try {
                finishOrRelease(Dispatchers.IO, { list: List<PadSamples.Prepared> -> list.forEach { it.native.close() } }) { padSamples.reload(missing) }
                    ?: emptyList()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyList()
            }
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

    /** [read] on the IO threads; null when it fails, but a cancellation goes on (never taken for a failure). */
    private suspend fun <T> readOrNull(read: () -> T): T? = try {
        withContext(Dispatchers.IO) { read() }
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        null
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
            _sampleTask.value = SampleTask.Failed(tr("Les 16 pads live sont pris : vides-en un dans Live › Pads."))
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
                _notice.value = tr("Les huit pistes audio sont prises : vide-en une ou pose le son sur un pad.")
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

    /**
     * A punch-in (« Refaire mes. 5-8 »): steps [from] until [until] (of the song for a song clip,
     * of the scene otherwise) of [clip] are recorded again; the rest of it stays.
     */
    private class Punch(val clip: AudioClip, val from: Int, val until: Int)

    /** How the take in progress is placed once it ends (see [TakeVault.Request]). */
    private class TakePlan(val track: Int, val anchor: TakeAnchor, val start: Int, val bpm: Double, val stepsPerBar: Int, val replaces: String? = null, val scene: Int? = null, val punch: Punch? = null)

    /**
     * « Refaire mes. [fromBar]-[toBar] » on audio track [id]: the song starts from the part holding
     * the bar before (a clip's part: from its start), after the count-in, the take records and stops by itself after bar [toBar]; only
     * those bars of the take replace the clip's (crossfaded), the whole take before is kept in the
     * take picker. Bars of the song for a song clip, of the part otherwise (from 1).
     */
    fun recordPunch(id: Int, fromBar: Int, toBar: Int) {
        val p = project.value
        val clip = p.audioTracks.firstOrNull { it.id == id }?.clip ?: return
        if (toBar < fromBar || fromBar < 1 || clip.tempoDiffers(p.tempoBpm)) return
        val spb = p.stepsPerBar
        if (!clip.song) clip.scene?.takeIf { it != p.scene && it in p.allScenes.indices }?.let { selectScene(it) }
        recordAudio(id, Punch(clip, (fromBar - 1) * spb, toBar * spb))
    }
    private var takePlan: TakePlan? = null
    /** The take plays the whole song: the song ends instead of starting over (the take stops there). */
    private var recordingSong = false

    /**
     * Records the microphone onto audio track [id]: the scene shown (or the whole song, see
     * [recordTarget]) starts after the count-in and the take begins exactly on its first step.
     * The track's own clip is silent meanwhile. [stopAudioRecording] ends it.
     */
    fun recordAudio(id: Int) = recordAudio(id, null)

    private fun recordAudio(id: Int, punch: Punch?) {
        if (!NativeEngine.isAvailable || _audioRecording.value != null) return
        if (_recording.value) stopRecordingTake()
        if (_transport.value.playing) togglePlay()
        val p = project.value
        val target = currentRecordTarget()
        // A punch-in starts with the part holding the bar before its first one (a song clip), or the part from its start.
        val songPart = when {
            punch != null && punch.clip.song && p.song.isNotEmpty() -> p.partAt((punch.from - p.stepsPerBar).coerceAtLeast(0)).coerceAtLeast(0)
            punch != null -> -1
            target.song && p.song.isNotEmpty() -> target.fromPart.coerceIn(0, p.song.lastIndex)
            else -> -1
        }
        val anchor = if (songPart >= 0) TakeAnchor(song = true, from = p.songStepOf(songPart)) else TakeAnchor()
        val previous = p.audioTracks.firstOrNull { it.id == id }?.clip
        lastRecordTrack = id
        // A punch keeps the whole take from where it began (only its bars are used): nothing cut.
        val start = if (punch != null) anchor.from else takeStart(previous, anchor)
        takePlan = TakePlan(id, anchor, start, p.tempoBpm, p.stepsPerBar, previous?.file, scene = if (anchor.song) null else p.scene, punch = punch)
        _audioRecording.value = AudioRecordingUi(id, waiting = true, song = songPart >= 0)
        viewModelScope.launch {
            // Opening the microphone can take up to a second: never on the main thread.
            // No longer than a track can loop at this tempo (64 bars): the rest would never play.
            // A song take may last the whole song (up to the longest take).
            val bars = if (songPart >= 0) maxOf(64, p.songBars - p.songStepOf(songPart) / p.stepsPerBar) else 64
            val seconds = minOf(MAX_AUDIO_SECONDS, (bars * p.stepsPerBar * 15.0 / p.tempoBpm).toInt() + 1)
            // A microphone opened while the project closes is closed again, not left armed.
            // Two beats of the count-in kept in front of the take: a word sung ahead of the « 1 »
            // (03 M1). A punch keeps only its own bars.
            val preroll = if (punch == null) prerollFrames(p.tempoBpm) else 0
            val handle = finishOrRelease(Dispatchers.IO, { h: Long -> TakeVault.app.discard(h) }) {
                NativeEngine.recordStart(onStart = true, seconds = seconds, prerollFrames = preroll)
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
            takePreroll = preroll
            // The output the take is heard on now (a headset unplugged later must not change it).
            takeLatencyMs = Metrics.recordLatencyMs()
            if (punch != null) {
                // A punch-in: the take is heard up to the first bar sung again, as in a studio (03 M4).
                NativeEngine.recordMuteAt(id, punch.from, punch.clip.song)
            } else {
                // The previous take of this track stays silent (no double voice, nothing in the microphone).
                takeMuted = id
                NativeEngine.setAudioOn(id, false)
            }
            monitorOn = false
            updateMonitor(id)
            // The microphone needs at least a bar to start on time (and to keep what is sung before the « 1 »).
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
                    // Headphones unplugged (or the choice changed): the voice in the headphones follows.
                    updateMonitor(id)
                    val waiting = st[0] < 0.5f
                    // The beats of count-in left, as the editors' takes show them (03 M6).
                    val pos = if (waiting) NativeEngine.playPosition() else 0.0
                    val beatsLeft = if (waiting && pos < 0) kotlin.math.ceil(-pos / 4.0).toInt().coerceAtLeast(1) else 0
                    _audioRecording.value = ui.copy(waiting = waiting, seconds = st[1] / OfflineExport.SAMPLE_RATE.toFloat(), level = st[2], beatsLeft = beatsLeft)
                    // A punch-in ends by itself a beat after its last bar.
                    val punchEnd = punch?.let { (it.until - anchor.from + 4) * 15.0 / p.tempoBpm * OfflineExport.SAMPLE_RATE }
                    if (punchEnd != null && st[0] > 0.5f && st[1] >= punchEnd) {
                        stopAudioRecording()
                        break
                    }
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
            prerollFrames = takePreroll,
            // Sung ahead of the « 1 »: a song take from its anchor may start up to two beats
            // earlier. A scene clip cannot (before its scene's first step is the previous pass).
            leadInSteps = if (plan.punch == null && plan.anchor.song && plan.start == plan.anchor.from) minOf(8, plan.start) else 0,
        )
    }

    /** Frames of count-in kept in front of a take at [bpm]: two beats (at most 4 s). */
    private fun prerollFrames(bpm: Double): Int =
        (2 * 60.0 / bpm.coerceAtLeast(30.0) * OfflineExport.SAMPLE_RATE).toInt().coerceAtMost(4 * OfflineExport.SAMPLE_RATE)

    private var takePreroll = 0
    private var monitorOn = false

    /**
     * « M'entendre dans le casque » (03 G1): during a take on track [id], the voice comes back in
     * wired headphones (or USB), through the track's effects. Never on the speaker (it would
     * howl) nor in Bluetooth (far too late to sing with).
     */
    private fun updateMonitor(id: Int) {
        val wanted = com.pocketstudio.app.settings.SettingsStore.state.value.monitorVoice &&
            com.pocketstudio.app.audio.Headphones.state.value == com.pocketstudio.app.audio.Listening.Wired
        if (wanted == monitorOn) return
        monitorOn = wanted
        NativeEngine.setMonitor(if (wanted) id else -1)
    }

    private fun stopMonitor() {
        if (!monitorOn) return
        monitorOn = false
        NativeEngine.setMonitor(-1)
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
        val punch = takePlan?.punch
        takePlan = null
        stopMonitor()
        if (_transport.value.playing) togglePlay()
        endTakeMute()
        if (request == null) return
        // Written into the project outside this screen: closing the app now keeps the take.
        val written = TakeVault.app.finish(request)
        _takeSaving.value = true
        takeJob = audioJob(tr("Écriture de la prise…"), always = true) {
            when (val r = written.await()) {
                is TakeVault.Outcome.Saved -> if (punch != null) placePunch(r, punch, request.bpm) else placeTake(r, request.latencyFrames)
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
        val forgotten = project.value.takeForgets(r.take)
        // The track's first take: brought to a usual voice level, « Voix claire » on a bare strip
        // (03 G2; one undo step with the take). A retake keeps the volume chosen.
        val first = project.value.audioTracks.firstOrNull { it.id == r.take.track }?.clip == null
        val gain = if (first) withContext(Dispatchers.Default) { AudioEdits.voiceLevelGain(r.pcm, AudioClip.MAX_GAIN) } else 1f
        session.edit { p -> p.withTake(r.take).let { if (first) it.withFirstTakeLevel(r.take.track, r.take.file, gain) else it } }
        // The ninth take: the oldest leaves the take picker (its sound waits a week aside; Annuler brings it back).
        if (forgotten != null) {
            AppLog.add("Prise : la plus ancienne (${forgotten.file}) quitte les prises gardées")
            _done.value = tr("Huit prises gardées : la plus ancienne (« %s ») est oubliée. Annuler la ramène.", forgotten.name)
        }
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

    /**
     * A punch-in's take: its bars replace the clip's in a new sound file (one undo step); the
     * clip as it was is kept first in the take picker. The track moved on meanwhile (another
     * take): placed as a plain take instead.
     */
    private suspend fun placePunch(r: TakeVault.Outcome.Saved, punch: Punch, bpm: Double) {
        val id = r.take.track
        val track = project.value.audioTracks.firstOrNull { it.id == id }
        if (track?.clip?.file != punch.clip.file) {
            placeTake(r, 0)
            return
        }
        val old = punch.clip
        val rate = OfflineExport.SAMPLE_RATE
        val perStep = 15.0 / bpm * rate
        val spliced = withContext(Dispatchers.Default) {
            val before = padSamples.read(old.file)
            AudioEdits.punch(
                before, r.pcm,
                oldAt = AudioEdits.punchOldAt(old, perStep, rate),
                newAt = AudioEdits.punchNewAt(old, r.take.start, perStep, rate),
                from = Math.round(punch.from * perStep), until = Math.round(punch.until * perStep),
            )
        }
        val (sample, prepared) = withContext(Dispatchers.IO) { padSamples.add(spliced, old.name) }
        padSamples.register(prepared)
        session.edit { p ->
            val t = p.audioTracks.firstOrNull { it.id == id } ?: return@edit p
            if (t.clip?.file != old.file) return@edit p
            val kept = listOf(old) + t.otherTakes
            p.withAudioTrack(t.copy(clip = old.copy(file = sample.file, seconds = spliced.seconds), previous = kept.first(), older = kept.drop(1)))
        }
        AppLog.add("Prise audio : mesures ${punch.from / project.value.stepsPerBar + 1} à ${punch.until / project.value.stepsPerBar} refaites")
        if (withContext(NonCancellable) { session.flush() }) {
            withContext(Dispatchers.IO) { PendingTakes.remove(store.folder(projectId), r.take.file) }
        }
        pushAudio()
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
        stopMonitor()
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

    /**
     * « Recaler sur le tempo »: a take recorded at another tempo stretched to the project's, its
     * pitch kept (Signalsmith Stretch, as the pads fitted to the tempo); its start stays on its bar.
     * A new sound file: Annuler brings the old one back.
     */
    fun realignTake(id: Int) {
        val clip = project.value.audioTracks.firstOrNull { it.id == id }?.clip ?: return
        val bpm = project.value.tempoBpm
        if (!clip.tempoDiffers(bpm) || !NativeEngine.isAvailable || _audioRecording.value != null) return
        val ratio = clip.bpm / bpm
        audioJob(tr("Recalage sur le tempo…")) {
            val pcm = withContext(Dispatchers.Default) { padSamples.read(clip.file) }
            val out = withContext(Dispatchers.Default) { NativeEngine.stretchPcm(pcm.data, pcm.channels, Math.round(pcm.frames * ratio), pcm.sampleRate) }
                ?: throw UserMessage(tr("Cette prise n'a pas pu être recalée (trop courte ou trop longue)."))
            AppLog.add("Piste audio ${id + 1} : recalée de ${clip.bpm.toInt()} à ${bpm.toInt()} BPM")
            putClip(id, Pcm(out, pcm.channels, pcm.sampleRate), clip.name, keep = clip.copy(bpm = bpm, offsetMs = (clip.offsetMs * ratio).toFloat()))
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

    /**
     * A knob moved on the MIDI controller: learnt now, or applied to its setting. False when no
     * MIDI learn mapping takes it (the screen's automatic knobs may, phase 39).
     */
    fun midiControl(channel: Int, controller: Int, value: Int): Boolean {
        if (controller == com.pocketstudio.app.midi.MidiViewModel.CC_MOD_WHEEL || controller == com.pocketstudio.app.midi.MidiViewModel.CC_SUSTAIN) {
            // The pedal holds the notes of a take too (the engine already holds the sound).
            if (controller == com.pocketstudio.app.midi.MidiViewModel.CC_SUSTAIN && channel != 9) recordSustain(value >= 64)
            if (_learning.value == null) return true  // played by the engine already
        }
        _learning.value?.let { target ->
            _learning.value = null
            session.edit { p ->
                p.copy(midiMap = (p.midiMap.filterNot { it.controller == controller || it.target == target } + MidiMapping(controller, target)).take(MidiMapping.MAX))
            }
            AppLog.add("MIDI learn : bouton $controller")
            return true
        }
        val mapping = project.value.midiMap.firstOrNull { it.controller == controller } ?: return false
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
        return true
    }

    // --- A sound played across the keys (track source 2) ----------------------------------------

    private fun pushSampled(p: Project = project.value, id: Int = track) = pushSampledSound(p.track(id))

    /** [t]'s sampled sound to engine sound [t.id] (a track's own, or a layer's: [Layers.soundIndex]). */
    private fun pushSampledSound(t: InstrumentTrack) {
        if (!NativeEngine.isAvailable) return
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
            val pcm = readOrNull { padSamples.read(s.file) }
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
            val pcm = readOrNull { AudioDecoder.decode(app, uri) }
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

    private fun pushPlaits(p: Project = project.value, id: Int = track) = pushPlaitsSound(p.track(id))

    /** [t]'s Plaits settings and its source to engine sound [t.id]. */
    private fun pushPlaitsSound(t: InstrumentTrack) {
        if (!NativeEngine.isAvailable) return
        t.plaits.engineValues().forEachIndexed { i, v -> NativeEngine.setPlaitsParam(i, v, t.id) }
        NativeEngine.setSynthEngine(t.engine, t.id)
    }

    /** 0 = subtractive synth, 1 = Plaits, 2 = the sampled sound, 4 = the 808; leaves any General MIDI instrument. */
    fun selectSynthEngine(engine: Int) {
        editTrack { it.copy(engine = engine, instrument = null) }
        pushInstrument()
        pushSampled()
        pushMulti()
        pushPlaits()
        releaseUnusedFonts()
        releaseUnusedMulti()
    }

    // --- Multizone instruments (phase 38: pianos, strings, brass, woodwinds, harp) and the 808 track ----

    /** Instruments in the engine's memory, by pack (shared by every track and export that plays them). */
    private val multiBanks = HashMap<String, com.pocketstudio.app.instruments.MultiBank>()
    private val loadingMulti = HashSet<String>()
    /** What each track's multizone instrument is in the live engine (and its gain): only changes are sent. */
    private val liveMulti = HashMap<Int, Pair<com.pocketstudio.app.instruments.MultiBank?, Float>>()

    private val _multiLoading = MutableStateFlow<Set<String>>(emptySet())
    /** The instruments being read into memory (« Préparation du piano… »). */
    val multiLoading: StateFlow<Set<String>> = _multiLoading.asStateFlow()

    /** Track [id]'s multizone instrument and 808 settings to the engine (a track whose instrument is not loaded yet plays its synth). */
    private fun pushMulti(p: Project = project.value, id: Int = track) = pushMultiSound(p.track(id))

    /** [t]'s multizone instrument and 808 settings to engine sound [t.id] (a track's own, or a layer's). */
    private fun pushMultiSound(t: InstrumentTrack) {
        if (!NativeEngine.isAvailable) return
        if (t.engine == InstrumentTrack.ENGINE_808) {
            com.pocketstudio.model.PadVoice.TRACK_808_PARAMS.forEach { param ->
                NativeEngine.setTrack808Param(t.id, param, t.voice808.param(param) ?: com.pocketstudio.model.PadVoice.track808Default(param))
            }
        }
        val bank = if (t.engine == InstrumentTrack.ENGINE_MULTI) t.multi.pack?.let { multiBanks[it] } else null
        val live = !NativeEngine.isCapturing
        if (live) {
            val had = liveMulti[t.id]
            if (had != null && had.first === bank && had.second == t.multi.gain) return
        }
        // None (or not loaded yet): its zones let go, the synth plays (and an instrument a project
        // before this one left on the track never sounds here).
        if (bank != null) bank.push(t.id, t.multi.gain)
        else if (!live || liveMulti[t.id]?.first != null || t.id !in liveMulti) {
            com.pocketstudio.app.instruments.MultiBank.clear(t.id)
            if (live) collectEngineGarbageSoon()
        }
        if (live) liveMulti[t.id] = bank to t.multi.gain
    }

    private var garbageJob: kotlinx.coroutines.Job? = null

    /**
     * An instrument's zones were let go: their memory (up to 100 MB) is freed once the audio thread
     * has dropped them, and again once the notes still ringing on them are over (10 s of release at most).
     */
    private fun collectEngineGarbageSoon() {
        if (!NativeEngine.isNative) return
        garbageJob?.cancel()
        garbageJob = viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            NativeEngine.collectGarbage()
            kotlinx.coroutines.delay(10_500)
            NativeEngine.collectGarbage()
        }
    }

    /** The memory the instruments being read will take, by pack: counted against the budget at once. */
    private val reservedMulti = HashMap<String, Long>()

    /** Tests: how an installed instrument's manifest and zones are read (the phone's files by default). */
    @androidx.annotation.VisibleForTesting
    internal var multiManifest: (Application, String) -> com.pocketstudio.model.PackManifest? = { app, pack -> com.pocketstudio.app.instruments.MultiBank.manifest(app, pack) }
    @androidx.annotation.VisibleForTesting
    internal var multiLoader: (Application, String) -> com.pocketstudio.app.instruments.MultiBank = { app, pack -> com.pocketstudio.app.instruments.MultiBank.load(app, pack) }

    /** Reads into memory the instruments the tracks play that are on the phone but not loaded yet. */
    private fun loadMultiBanks() {
        val app = getApplication<Application>()
        project.value.multiPacks.filter { it !in multiBanks && it !in loadingMulti }.forEach { pack ->
            loadingMulti += pack
            _multiLoading.value = loadingMulti.toSet()
            viewModelScope.launch {
                try {
                    val manifest = withContext(Dispatchers.IO) { multiManifest(app, pack) }
                    val bytes = manifest?.instrument?.memoryBytes ?: 0L
                    // The ones being read count too: two instruments opened together never pass the budget.
                    val used = multiBanks.values.sumOf { it.instrument.memoryBytes } + reservedMulti.values.sum()
                    val bank = when {
                        manifest?.instrument == null -> null  // not downloaded here: the track plays its synth sound
                        used + bytes > com.pocketstudio.app.instruments.MultiBank.BUDGET_BYTES -> {
                            _notice.value = tr("Trop d'instruments à la fois : « %s » joue son son de synthé. Retire un instrument d'une autre piste.", manifest?.name ?: pack)
                            null
                        }
                        else -> {
                            reservedMulti[pack] = bytes
                            try {
                                // Read to the end even when the screen closes meanwhile, then freed (never left in the engine's memory).
                                finishOrRelease(Dispatchers.IO, com.pocketstudio.app.instruments.MultiBank::close) {
                                    runCatching { multiLoader(app, pack) }.onFailure { AppLog.add("Instrument $pack : échec (${it.message})") }.getOrNull()
                                }
                            } finally {
                                reservedMulti.remove(pack)
                            }
                        }
                    }
                    if (bank == null) return@launch
                    // Not needed any more (the track changed meanwhile), or already there.
                    if (pack !in project.value.multiPacks || multiBanks.containsKey(pack)) {
                        bank.close()
                        return@launch
                    }
                    multiBanks[pack] = bank
                    AppLog.add("Instrument « ${manifest?.name} » : ${bank.zones.size} zones, ${bank.instrument.memoryBytes / 1_000_000} Mo")
                    project.value.instrumentTracks.filter { it.engine == InstrumentTrack.ENGINE_MULTI && it.multi.pack == pack }.forEach { pushMulti(id = it.id) }
                    // G1b: and the layers playing it (until now on their synth stand-in, live).
                    project.value.instrumentTracks.forEach { t ->
                        t.layers.forEachIndexed { k, l ->
                            if (l.sound.engine == InstrumentTrack.ENGINE_MULTI && l.sound.multi.pack == pack) {
                                pushMultiSound(l.sound.asTrack(NativeEngine.layerSound(t.id, k)))
                            }
                        }
                    }
                } finally {
                    loadingMulti -= pack
                    _multiLoading.value = loadingMulti.toSet()
                }
            }
        }
    }

    /** Instruments were downloaded or deleted: the tracks waiting for theirs get them. */
    fun instrumentsChanged() {
        releaseUnusedMulti()
        loadMultiBanks()
    }

    /** Lets go of the instruments no track plays any more (the engine keeps its own references while they ring). */
    private fun releaseUnusedMulti() {
        val used = project.value.multiPacks
        multiBanks.keys.filter { it !in used }.forEach { multiBanks.remove(it)?.close() }
    }

    /**
     * Track [id] plays the downloaded multizone instrument [pack] (its synth sound becomes the
     * instrument's fallback, heard while it loads and on a phone without it). One undo step.
     */
    fun useMultiInstrument(pack: String, name: String, fallback: String, id: Int = track) {
        if (!hasTrack(id)) return
        editTrack(id = id) {
            it.copy(
                engine = InstrumentTrack.ENGINE_MULTI, instrument = null,
                multi = com.pocketstudio.model.MultiPatch(pack, name),
                synth = com.pocketstudio.model.MultiInstruments.fallbackSynth(fallback),
            )
        }
        pushInstrument()
        pushSynth(project.value.track(id).synth, id)
        pushMulti(id = id)
        pushPlaits(id = id)
        releaseUnusedFonts()
        releaseUnusedMulti()
        loadMultiBanks()
        AppLog.add("Piste ${id + 1} : instrument « $name »")
    }

    /** Track [id] plays the 808 (glide on). One undo step. */
    fun use808(id: Int = track) {
        if (!hasTrack(id)) return
        editTrack(id = id) {
            it.copy(
                engine = InstrumentTrack.ENGINE_808, instrument = null,
                voice808 = it.voice808.with(com.pocketstudio.model.PadVoice.GLIDE, it.voice808.param(com.pocketstudio.model.PadVoice.GLIDE) ?: com.pocketstudio.model.NewTracks.GLIDE_808),
            )
        }
        pushInstrument()
        pushMulti(id = id)
        pushPlaits(id = id)
        releaseUnusedFonts()
        releaseUnusedMulti()
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
            NativeEngine.setDrumLaneSteps(lane, l.length)
        }
    }

    // --- Scenes and song ------------------------------------------------------------------------

    private val _sessionUi = MutableStateFlow(SessionUi())
    val sessionUi: StateFlow<SessionUi> = _sessionUi.asStateFlow()
    private var swapsSeen = 0
    private var swapWatch: kotlinx.coroutines.Job? = null

    /** The song part still playing after it was taken out of the song (see [SessionUi.removed]). */
    private val removedPlaying: SongPart? get() = _sessionUi.value.removed

    private val _loopPart = MutableStateFlow(project.value.loopPart)
    /**
     * « ⟳ Partie »: ▶ loops the part being edited (the scene shown) rather than playing the song.
     * Off: once there is a song, ▶ plays it on every tab, from [playFrom]. Kept with the project.
     */
    val loopPart: StateFlow<Boolean> = _loopPart.asStateFlow()

    /**
     * Turns « ⟳ Partie » on or off (kept with the project, no undo step). While the music plays it
     * takes effect at once: the song part playing goes on as a loop (staying on another scene: that
     * one takes over at the next bar), or the song takes over at the next bar from the part of the
     * scene heard.
     */
    fun setLoopPart(on: Boolean) {
        if (_loopPart.value == on) return
        _loopPart.value = on
        if (!on) _loopArmedBySelf.value = false
        keepListenChoice()
        if (!NativeEngine.isAvailable || !_transport.value.playing) return
        val song = project.value.song
        if (on && songPlaying()) {
            endSongFollow()
        } else if (!on && song.isNotEmpty() && _sessionUi.value.songIndex < 0) {
            val heard = heardScene ?: project.value.scene
            playSongPart(song.indexOfFirst { it.scene == heard }.takeIf { it >= 0 } ?: playFromNow())
        }
    }

    private val _loopArmedBySelf = MutableStateFlow(false)
    /**
     * « ⟳ Partie » went on by itself: a grid, a pad or a note of the part shown was touched while
     * the song played (or another part was picked), so that part loops under the fingers instead
     * of the screens following the song. The editors say so above their grids. Off again when
     * « ⟳ Partie » goes off or the music stops.
     */
    val loopArmedBySelf: StateFlow<Boolean> = _loopArmedBySelf.asStateFlow()

    /** The project as last seen by [loopOnEdit]. */
    private var lastEdited: Project = project.value

    /**
     * Editing must not move under the finger: the song plays, the screens follow it, and the
     * part shown just changed in content (a hit, a note, an acid step), so « ⟳ Partie » goes on
     * for that part (it loops from the next bar, the edits are heard at once). Not during a take.
     */
    private fun loopOnEdit(now: Project) {
        val before = lastEdited
        lastEdited = now
        if (before === now || _loopPart.value || !_followSong.value || !songPlaying()) return
        if (_recording.value || _audioRecording.value != null || _take.value.running) return
        if (!editedShownScene(before, now)) return
        _loopArmedBySelf.value = true
        setLoopPart(true)
    }

    private val _playFrom = MutableStateFlow(0)
    /**
     * Where ▶ starts the song (a part): where it was stopped, the part chosen to play from or the
     * part of the scene picked to edit; back to the start once the song played to its end.
     */
    val playFrom: StateFlow<Int> = _playFrom.asStateFlow()

    /** [playFrom] if it is still a part of the song (parts removed since: the start). */
    private fun playFromNow(): Int = _playFrom.value.takeIf { it in project.value.song.indices } ?: 0

    /** Tests: what ▶ started ("song <part>" or "part <scene>"), engine or not. */
    internal var playStartsForTest: MutableList<String>? = null

    private val _playBar = MutableStateFlow(-1)
    /**
     * The bar playing (from 0): of the song while it plays, else of the scene's loop; -1 stopped.
     * Changes once a bar at most; up to date while a screen polls it ([pollPosition]).
     */
    val playBar: StateFlow<Int> = _playBar.asStateFlow()

    /** Keeps [playBar] up to date a few times a second, while the caller runs (a screen showing it). */
    suspend fun pollPosition() {
        if (!NativeEngine.isAvailable) return
        while (true) {
            _playBar.value = barNow()
            kotlinx.coroutines.delay(120)
        }
    }

    private fun barNow(): Int {
        if (!_transport.value.playing) return -1
        val p = project.value
        val spb = p.stepsPerBar.coerceAtLeast(1)
        if (_sessionUi.value.songIndex >= 0) return NativeEngine.songStep().let { if (it < 0) -1 else it / spb }
        val steps = NativeEngine.absoluteStep()
        if (steps < 0) return -1
        return steps % p.sceneSteps(heardScene ?: p.scene).coerceAtLeast(1) / spb
    }

    /**
     * « Lancer à » and the Live page's effect settings are not undo steps: Annuler / Rétablir of
     * something else keep them as they were ([before]: the project before the step).
     */
    private fun keepLiveSettings(before: Project) {
        val p = project.value
        if (p.liveQuantum == before.liveQuantum && p.liveQuantumFine == before.liveQuantumFine && p.liveFx == before.liveFx) return
        session.editQuietly { it.copy(liveQuantum = before.liveQuantum, liveQuantumFine = before.liveQuantumFine, liveFx = before.liveFx) }
        pushLiveFxSettings(project.value)
    }

    /** The project keeps « ⟳ Partie » as set (also after Annuler / Rétablir, which do not change it). */
    private fun keepListenChoice() {
        val loop = _loopPart.value
        if (project.value.loopPart != loop) session.editQuietly { it.copy(loopPart = loop) }
    }

    /** True when ▶ plays the song: there is one, and « ⟳ Partie » is off. */
    val listensToSong: Boolean get() = project.value.song.isNotEmpty() && !_loopPart.value

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
            // ▶ then plays the song from where this scene first plays in it (the start point said
            // under the top bar), unless the start point already is one of its parts.
            val song = project.value.song
            if (song.getOrNull(playFromNow())?.scene != index) song.indexOfFirst { it.scene == index }.takeIf { it >= 0 }?.let { _playFrom.value = it }
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
        // Following the song, the scene picked loops from the next bar: « ⟳ Partie » says so, and
        // the editors say above their grids that it went on by itself.
        if (songPlaying() && !_loopPart.value) {
            _loopPart.value = true
            _loopArmedBySelf.value = true
            keepListenChoice()
        }
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
            _notice.value = tr("Arrête d'abord l'enregistrement pour déplacer une partie.")
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
            _notice.value = tr("Les %d parties sont déjà prises : vide-en une pour recommencer.", Scene.MAX)
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
            _notice.value = tr("Les %d parties sont déjà prises : plus de place pour une copie.", Scene.MAX)
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

    private val _done = MutableStateFlow<String?>(null)
    /** « Fait : … » after an intention (the part sheet, a kit): said with « Annuler » beside it; null: nothing. */
    val done: StateFlow<String?> = _done.asStateFlow()

    fun dismissDone() {
        _done.value = null
    }

    /** « Annuler » beside « Fait : … »: the intention undone (it was one step). */
    fun undoDone() {
        _done.value = null
        undo()
    }

    /**
     * The part sheet: [intent] on song part [index], one undo step. What plays follows as after
     * Annuler (the part on now and the next one sent again). Nothing changed (no room for a
     * scene, a part or a curve): said why. « Toutes les 3 » shows the shared scene to edit it.
     */
    fun partIntent(index: Int, intent: com.pocketstudio.app.session.PartIntent) {
        val p = project.value
        val part = p.song.getOrNull(index) ?: return
        if (intent == com.pocketstudio.app.session.PartIntent.EditAll) {
            selectScene(part.scene)
            return
        }
        if (_recording.value || _audioRecording.value != null || _takeSaving.value || performanceTaking) {
            _notice.value = tr("Arrête d'abord l'enregistrement pour transformer une partie.")
            return
        }
        if (intent is com.pocketstudio.app.session.PartIntent.Energy && p.partEnergy(index) == intent.level) return
        val title = p.sceneTitle(part.scene)
        val heard = sceneHeard()
        session.edit { intent.applyTo(it, index) }
        if (project.value == p) {
            _notice.value = intent.refusal()
            return
        }
        if (heard == null) _sessionUi.value = SessionUi()
        followPlaybackAfterHistory(heard, p.song)
        AppLog.add("Partie ${index + 1} : $intent")
        _done.value = intent.doneText(title)
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

    /**
     * The timeline's « Couper ici »: song part [index] cut in two at [atBar] bars from its start
     * (see [splitSongPart]; one undo step). The song plays the same; playing it, the part on now
     * and the one chosen to come next keep their place.
     */
    fun splitSongPart(index: Int, atBar: Int) {
        val before = project.value.song
        if (index !in before.indices) return
        if (before.size >= SongPart.MAX_PARTS) {
            _notice.value = tr("Le morceau a déjà %d parties : retires-en une pour en ajouter une autre.", SongPart.MAX_PARTS)
            return
        }
        val following = songPlaying()
        session.edit { it.splitSongPart(index, atBar) }
        if (project.value.song == before) return
        if (following) followSongEdit { i, _ -> if (i > index) i + 1 else i }
    }

    private val _songLoop = MutableStateFlow<IntRange?>(null)
    /**
     * The song parts the timeline loops over (« Boucle »), null: none. Not saved: a way of
     * listening, like « suivre le morceau ». By place in the song (moving parts moves what it covers).
     */
    val songLoop: StateFlow<IntRange?> = _songLoop.asStateFlow()

    /** The loop if it still fits the song (parts removed since: none). */
    private fun loopNow(): IntRange? = _songLoop.value?.takeIf { !it.isEmpty() && it.first >= 0 && it.last < project.value.song.size }

    /** Loops the song over parts [range] (null: plays on to the end again); the part that comes next follows at once. */
    fun setSongLoop(range: IntRange?) {
        val r = range?.takeIf { !it.isEmpty() && it.first >= 0 && it.last < project.value.song.size }
        if (r == _songLoop.value) return
        _songLoop.value = r
        if (songPlaying() && removedPlaying == null) stageSongPart()
    }

    /**
     * The timeline's « Ouvrir dans … »: shows scene [index] to edit it. The song playing goes on
     * (its edits wait for that scene's turn, as when staying on a scene); else as [selectScene].
     */
    fun showSceneToEdit(index: Int) {
        if (index !in project.value.allScenes.indices) return
        if (songPlaying()) {
            if (index != project.value.scene) session.editQuietly { it.switchScene(index) }
            return
        }
        selectScene(index)
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

    /** Plays the song from its first part (or stops it); « ⟳ Partie » goes off (the song was asked for). */
    fun toggleSong() {
        if (_transport.value.playing) {
            toggleTransport()
            return
        }
        loopPartOff()
        _playFrom.value = 0
        playStartsForTest?.add("song 0")
        playSongFrom(0)
    }

    /**
     * « Écouter » on « Ton morceau est prêt ! » (decision 2A, S6): stopped, the song plays from about
     * 8 bars before its first Drop (or Refrain, [com.pocketstudio.model.FirstListen.startPart]), on a
     * part's start: the Drop after about 15 s, even in a techno whose Drop is two minutes in. Playing,
     * it stops. A song without a Drop or Refrain plays from its start.
     */
    fun listenFromDrop() {
        if (_transport.value.playing) {
            toggleTransport()
            return
        }
        val start = com.pocketstudio.model.FirstListen.startPart(project.value)
        playStartsForTest?.add("listen $start")
        if (start <= 0) toggleSong() else playSongPart(start)
    }

    /** « Depuis le début », next to « Écouter »: the whole song from its first bar (playing: from there again). */
    fun listenFromStart() {
        if (_transport.value.playing) toggleTransport()
        toggleSong()
    }

    /** The song was asked for (« Jouer le morceau », a part touched): « ⟳ Partie » goes off. */
    private fun loopPartOff() {
        _loopArmedBySelf.value = false
        if (!_loopPart.value) return
        _loopPart.value = false
        keepListenChoice()
    }

    /**
     * A touch on song part [index] (Morceau, « Jouer depuis… »): stopped, the song plays from there;
     * playing the song, that part comes next (a touch on the part already chosen goes back to the
     * usual order); playing a scene, the song starts from that part at the next bar. « ⟳ Partie »
     * goes off, and ▶ starts from there next time.
     */
    fun playSongPart(index: Int) {
        val song = project.value.song
        if (index !in song.indices) return
        loopPartOff()
        _playFrom.value = index
        if (!NativeEngine.isAvailable) return
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
        if (_followSong.value) session.showQuietly { it.switchScene(part.scene) }
        syncEngine()
        pushPart(project.value, part.scene, part)  // the part's scene without its muted tracks
        applyAutomationState(project.value, project.value.automationBefore(index))
        heardScene = part.scene
        _sessionUi.value = SessionUi(songIndex = index)
        // S6 (G5): another song made in this project is estimated again (applied when found); the drive
        // found is given once more with the target syncEngine just sent (a target left by another
        // project would have moved it).
        estimateLiveDrive()
        if (!liveDriveFromExport && liveDriveKey != null && liveDrivePlayedKey != liveDriveKey) {
            liveDriveDb?.let { giveKnownDrive(project.value, it, liveDriveLevel) }
            liveDrivePlayedKey = liveDriveKey
        }
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
            ui.pending.let { now -> if (screensFollow(ui)) session.showQuietly { it.switchScene(now) } }
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
                AutoTarget.SYNTH -> t.track?.let { NativeEngine.setSynthParam(t.param, v, it) }
                AutoTarget.PLAITS -> t.track?.let { NativeEngine.setPlaitsParam(t.param, v, it) }
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
            // Phase 37: a frozen track's notes are silent in the song (its sound plays from its audio track).
            val frozen = if (part != null) com.pocketstudio.model.Freeze.mutedKeys(p) else emptySet()
            val view = if (part != null) p.partView(if (frozen.isEmpty()) part else part.copy(muted = part.muted + frozen)) else p.sceneView(index)
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
        // Recording a take over the whole song: no loop either (the take follows the song).
        val next = songNextIndexLooped(ui.songIndex, ui.queued, song.size, p.songLoops && !recordingSong, if (recordingSong) null else loopNow())
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
        beforeSwap = slotScenesNow()
        heardScene = now
        val song = project.value.song
        val index = when {
            song.isEmpty() -> -1
            ui.songIndex >= 0 -> songNextIndexLooped(ui.songIndex, ui.queued, song.size, loops = true, loop = if (recordingSong) null else loopNow())
            else -> ui.queued.takeIf { it in song.indices } ?: -1  // the song starts from the part chosen
        }
        // A whole scene (a song part) took over: the live grid plays it as the engine does, each
        // track on its scene of the part (a muted one stopped), from the step it really started on.
        _liveGrid.value = gridOfPart(song.getOrNull(index)?.takeIf { it.scene == now }, now, liveTransportStep(), slotStatesNow())
        stagedFrom.fill(null)
        // The engine already plays it: only the screens follow (no undo step).
        if (screensFollow(ui)) session.showQuietly { it.switchScene(now) }
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
        // Played to its end: ▶ starts it from the top again.
        _playFrom.value = 0
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
        beforeSwap = slotScenesNow()
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

    /** Tests (no engine): song part [index] chosen to come next (« ensuite »), as [playSongPart] does while the song plays. */
    internal fun queueSongPartForTest(index: Int) {
        _sessionUi.value = _sessionUi.value.copy(queued = index)
    }

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

    /**
     * « Lancer à » (kept with the project, no undo step): 1, 2 or 4 bars, or [code] -1 a beat and
     * -2 half a bar (see [liveQuantumCode]). During a take, launches still fall on bar lines.
     */
    fun setLiveQuantum(code: Int) {
        val p = project.value
        val next = when (code) {
            -1 -> p.copy(liveQuantumFine = LiveSlots.QUANTUM_BEAT)
            -2 -> p.copy(liveQuantumFine = LiveSlots.QUANTUM_HALF_BAR)
            in LiveSlots.QUANTA -> p.copy(liveQuantum = code, liveQuantumFine = 0)
            else -> return
        }
        if (next.liveQuantum == p.liveQuantum && next.liveQuantumFine == p.liveQuantumFine) return
        session.editQuietly { it.copy(liveQuantum = next.liveQuantum, liveQuantumFine = next.liveQuantumFine) }
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
        else -> LiveSlots.audioOf(slot)?.let(AudioTrack::mixerChannel) ?: InstrumentTrack.mixerChannel(slot - LiveSlots.INST)
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
        val saved = project.value.acid.sound.values()[param]
        liveCommandsForTest?.add("acid $param $saved")
        if (NativeEngine.isAvailable) NativeEngine.setAcidParam(param, saved)
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
        if (ch !in 0 until Mixer.CHANNELS || Mixer.isPad(ch)) return
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
        if (ch !in 0 until Mixer.CHANNELS || Mixer.isPad(ch)) return
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
     * Leaving the Live page: nothing it set stays on unseen. The tracks muted with M play again; the
     * whole-mix effects (the Montée, the DJ filter, what the lock kept), the returns, the tracks' kills and choppers go back to rest
     * (written into a take as heard, at once); a Drop already waiting still drops on its bar line.
     */
    fun leaveLive() {
        // M of the Live page: heard again (it was never written in the project, nothing else shows it).
        _liveMuted.value.toList().forEach(::toggleLiveMute)
        _liveFx.value.locks.toList().forEach { setLiveLock(it, false) }
        for (param in AutoTarget.LIVE_GESTURES) {
            if (AutoTarget.isTrigger(param)) continue
            val rest = AutoTarget.liveRest(param)
            if (livePerf[param] != rest) livePerformance(param, rest)
        }
        for (bus in 0..1) if (_liveReturns.value[bus] != 0.5f) setLiveReturn(bus, 0.5f)
        _liveFx.value.trackKills.forEach { (ch, bands) -> bands.forEach { liveKill(it, false, ch) } }
        _liveChops.value.toList().forEach { liveTrackChop(it, false) }
        // The 303 as the page's sliders left it: back to the sound saved (Sons › Basse acide shows it).
        _liveAcid.value.keys.toList().forEach(::resetLiveAcid)
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
        if (ch !in 0 until Mixer.CHANNELS || Mixer.isPad(ch) || ch == MASTER || (ch in _liveChops.value) == on) return
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
                for (ch in Mixer.TRACK_STRIPS) if (ch != MASTER) engineMacro(ch, AutoTarget.MACRO_CHOP, 0f)
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
        if (others != 0 && NativeEngine.isAvailable) NativeEngine.armLaunch(others, launchSteps(), -1)
        toggleTransport(songStep = aloneSongStep())
        if (others != 0) _liveGrid.value = liveNow().armed(others, -1, launchSteps())
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
            NativeEngine.armLaunch(mask, launchSteps(), scene, launchGraceSteps())
        }
        for (k in 0 until LiveSlots.COUNT) if (mask and (1 shl k) != 0) stagedFrom[k] = p.takeIf { scene >= 0 }
        _liveGrid.value = liveNow().armed(mask, scene, launchSteps())
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
                in LiveSlots.AUDIO until LiveSlots.AUDIO + 4, in LiveSlots.AUDIO2 until LiveSlots.AUDIO2 + AudioTrack.MAX - 4 -> {
                    val id = LiveSlots.audioOf(slot)!!
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
        ch == 0 || Mixer.isPad(ch) -> LiveSlots.DRUMS
        ch == 1 -> LiveSlots.ACID
        ch == 2 -> LiveSlots.INST
        ch == 3 -> LiveSlots.REMIX
        ch in 5..11 -> LiveSlots.INST + ch - 4
        else -> AudioTrack.ofChannel(ch)?.let(LiveSlots::audio)
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
            _notice.value = tr("Le morceau est plein : choisis « Nouvelle version » pour enregistrer.")
            return
        }
        _takeAppend.value = append
        // Armed: the take's song position moves with the choice (the song's end, or its start).
        if (_take.value !is com.pocketstudio.app.live.TakeUi.Armed || performance == null) return
        performanceAppend = append && p.song.isNotEmpty()
        val start = performance?.start ?: return
        if (NativeEngine.isAvailable && NativeEngine.songMarkStep() < start) NativeEngine.markSong(p.liveLaunchSteps(taking = true), if (performanceAppend) p.song.sumOf { p.partSteps(it) } else 0)
    }

    /** No room after song [p] for a take « à la suite » (its parts all used). */
    private fun songFullForTake(p: Project): Boolean = p.song.isNotEmpty() && p.song.size >= SongPart.MAX_PARTS

    /**
     * How late after a launch line a tap still launches on it (in steps): what is heard lags what
     * is rendered by the output latency, and a finger on the « one » lands a little after it.
     * At most half the quantum (a tap late in the bar always waits for the next line).
     */
    private fun launchGraceSteps(): Double {
        val latencyMs = Metrics.streamInfo.value.outputLatencyMs.takeIf { it > 0 } ?: 0.0
        return ((latencyMs + LAUNCH_GRACE_MS) / 1000.0 * project.value.tempoBpm / 15.0).coerceAtMost(launchSteps() / 2.0)
    }

    /** The steps a launch waits for now: « Lancer à », whole bars during a take (it writes whole-bar parts). */
    private fun launchSteps(): Int = project.value.liveLaunchSteps(_take.value.running)

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
            is com.pocketstudio.app.live.TakeUi.Recording -> finishPerformance(performanceEndHeard(takePosition()))
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
            _notice.value = tr("Le morceau est plein : choisis « Nouvelle version » pour enregistrer.")
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
        val q = p.liveLaunchSteps(taking = true)
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
        performancePadTaps = ArrayList()
        performancePadsAtStart = livePadsSoundingForTest ?: _liveStates.value.withIndex().filter { it.value == 2 }.map { it.index }.toSet()
        _take.value = com.pocketstudio.app.live.TakeUi.Armed(start, (start - takeClock().coerceAtLeast(0)).coerceAtLeast(0))
        AppLog.add("Live : prise armée (mesure ${start / p.stepsPerBar + 1}${if (append) ", à la suite" else ""})")
        watchPerformance()
    }

    // « Notes » on the Live page: an instrument track played live, written into a take on the sixteenth.

    private class LiveHeld(val at: Double, val velocity: Float)
    private val liveKeysHeld = HashMap<Long, LiveHeld>()
    private fun liveKey(id: Int, pitch: Int): Long = id.toLong() shl 8 or pitch.toLong()

    /** Tests: the grid's state as the take reads it (null: [liveGrid]). */
    internal fun liveNoteHeldForTest(id: Int, pitch: Int): Boolean = liveKeysHeld.containsKey(liveKey(id, pitch))

    /**
     * A key down on the Live page's « Notes »: it sounds on instrument track [id]; while a take
     * records, it is held until let go, then written into the scene the track plays.
     */
    fun liveNoteOn(id: Int, pitch: Int, velocity: Float) {
        if (project.value.instrumentTracks.none { it.id == id }) return
        if (NativeEngine.isAvailable) NativeEngine.synthNoteOn(pitch, velocity, id)
        playedNoteOn(id, pitch, velocity)
        if (performance != null && _take.value is com.pocketstudio.app.live.TakeUi.Recording) liveKeysHeld[liveKey(id, pitch)] = LiveHeld(takePosition(), velocity)
    }

    fun liveNoteOff(id: Int, pitch: Int) {
        if (NativeEngine.isAvailable) NativeEngine.synthNoteOff(pitch, id)
        playedNoteOff(id, pitch)
        val held = liveKeysHeld.remove(liveKey(id, pitch)) ?: return
        writeLiveNote(id, pitch, held.at, takePosition(), held.velocity)
    }

    /**
     * A note of the Live page's keys into the take: on the nearest sixteenth of the loop of the
     * scene track [id] plays (counted from where its part was launched), its length kept (half a
     * sixteenth at least). A track stopped on the grid: nothing written. Folded into the take's
     * undo step; the song version kept before the take has the scene as it was.
     */
    private fun writeLiveNote(id: Int, pitch: Int, from: Double, to: Double, velocity: Float) {
        val p = project.value
        if (p.instrumentTracks.none { it.id == id }) return
        val slot = LiveSlots.INST + id
        val scene = sceneOfSlot(p, slot)?.takeIf { it in p.allScenes.indices } ?: return
        val start = _liveGrid.value?.slot(slot)?.start ?: 0
        val clip = p.sceneView(scene).track(id).notes
        val step = NoteClip.TICKS_PER_STEP
        val tick = Math.floorMod(Math.round(from - start).toInt() * step, clip.ticks.coerceAtLeast(step))
        val length = Math.round((to - from) * step).toInt().coerceIn(step / 2, NoteClip.MAX_TICKS)
        session.editQuietly { q ->
            inScene(q, scene) { r ->
                val t = r.track(id)
                r.withTrack(t.copy(notes = t.notes.copy(notes = t.notes.notes.filterNot { it.start == tick && it.pitch == pitch } + Note(tick, length, pitch, velocity)).sanitized()))
            }
        }
        if (playsNow(slot, scene)) pushNotesNow(project.value.sceneView(scene).track(id).notes, id)
    }

    // The live pads played during the take: one audio track (« Pads live ») once it is written.

    /** Taps on the live pads during the take (armed or recording), on its clock. */
    private var performancePadTaps = ArrayList<com.pocketstudio.model.LivePadTake.Tap>()
    /** The pads already sounding when the take was armed. */
    private var performancePadsAtStart: Set<Int> = emptySet()

    /** Tests: what the last take's live pads played. */
    internal var padSpansForTest: List<com.pocketstudio.model.LivePadTake.Span>? = null

    /** Tests: the pads sounding as the engine says (null: [liveStates]). */
    internal var livePadsSoundingForTest: Set<Int>? = null

    private fun livePadTapped(pad: Int) {
        if (performance == null || !_take.value.running) return
        performancePadTaps += com.pocketstudio.model.LivePadTake.Tap(pad, takePosition())
    }

    /**
     * The live pads the take played, as one sound on a new audio track « Pads live », a song clip
     * where the take begins in the song ([songStart]): folded into the take's undo step when
     * nothing was done since. No free audio track: said, the take keeps the rest.
     */
    private fun bouncePerformancePads(spans: List<com.pocketstudio.model.LivePadTake.Span>, start: Int, end: Int, songStart: Int, before: Project) {
        if (spans.isEmpty()) return
        val p = project.value
        if (p.audioTracks.size >= AudioTrack.MAX) {
            _notice.value = tr("Les pistes audio sont toutes prises : les pads live de cette prise n'ont pas été gardés.")
            return
        }
        val clips = p.live.clips
        val bpm = p.tempoBpm
        audioJob(tr("Écriture des pads live…"), always = true) {
            val sounds = withContext(Dispatchers.IO) {
                spans.map { it.pad }.distinct().mapNotNull { pad ->
                    clips.getOrNull(pad)?.let { c -> runCatching { pad to padSamples.read(c.file) }.getOrNull() }
                }.toMap()
            }
            val pcm = withContext(Dispatchers.Default) { com.pocketstudio.app.audio.PadBounce.render(spans, clips, sounds, start, end, bpm) } ?: return@audioJob
            val name = "Pads live"
            val (sample, prepared) = withContext(Dispatchers.IO) { padSamples.add(pcm, name) }
            padSamples.register(prepared)
            val add: (Project) -> Project = { q ->
                val grown = q.addAudioTrack(name)
                val id = grown?.audioTracks?.firstOrNull { t -> q.audioTracks.none { it.id == t.id } }?.id
                if (grown == null || id == null) q
                else grown.withAudioTrack(grown.audioTracks.first { it.id == id }.copy(
                    clip = AudioClip(sample.file, name, pcm.seconds, start = songStart, song = true, bpm = bpm),
                ))
            }
            if (session.undoDepth == performanceDoneDepth && performanceDoneDepth >= 0) {
                // Nothing done since the take: the pads join its undo step.
                session.editQuietly(add)
                session.commitQuietEdits(before, performanceDoneDepth - 1)
                performanceDoneDepth = session.undoDepth
            } else {
                session.edit(transform = add)
            }
            pushAudio()
            pushMixer()
            AppLog.add("Live : pads gardés (${spans.size} passages, ${"%.1f".format(pcm.seconds)} s)")
            _notice.value = tr("Les pads live de ta prise sont sur la piste audio « %s ».", name)
        }
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

    /**
     * The bar line ■ Fin ends the take on, pressed where [heard] is heard: in a bar's first beat,
     * that bar's line (a ■ on the « one » ends there, it does not add the bar just begun); later
     * in the bar, the bar being played is kept.
     */
    private fun performanceEndHeard(heard: Double): Int {
        val bar = project.value.stepsPerBar
        val at = heard.coerceAtLeast(0.0)
        val line = (at / bar).toInt() * bar
        // Never before the take's first bar is over (a ■ right after it began keeps that bar). 4: a beat.
        val first = performance?.start ?: 0
        return if (line > first && at - line < 4) line else performanceEnd(at.toInt())
    }

    /** The take ends on [end]: written into the song in one undo step. */
    private fun finishPerformance(end: Int) {
        // A Drop's bar line passed before the watch saw it: what it cleared is written first.
        settleDropIfPassed()
        val t = performance ?: return
        // Keys of « Notes » still held: written up to the take's end.
        liveKeysHeld.keys.toList().forEach { k -> liveKeysHeld.remove(k)?.let { h -> writeLiveNote((k shr 8).toInt(), (k and 0xFF).toInt(), h.at, end.toDouble(), h.velocity) } }
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
        val now = System.currentTimeMillis()
        session.editQuietly { p ->
            // The song it replaces (or writes after) is kept as a version, as it was when the take
            // began (notes played live went into the scenes since): never lost.
            val w = before.keepingVersion(p.writeTake(t, end, performanceAppend), now).also { written = it }
            if (w.parts > 0) w.project.copy(liveTake = t.id) else w.project
        }
        if (written.parts == 0) {
            dropTakeJournal(t)
            _take.value = com.pocketstudio.app.live.TakeUi.Ready
            _notice.value = if (written.cut) tr("Le morceau est plein : choisis « Nouvelle version » pour enregistrer.") else tr("Prise trop courte : rien n'a été écrit.")
            return
        }
        // Its undo step takes the take back, not the tempo, mixer or effects changed meanwhile.
        val undoTo = project.value.takeUndoTarget(before, t.gestureTargets)
        session.commitQuietEdits(undoTo, performanceDepth)
        performanceDoneDepth = session.undoDepth
        dropTakeJournalOnceSaved(t.id)
        // The live pads it played: one more audio track, written in the background.
        val padSpans = com.pocketstudio.model.LivePadTake.spans(project.value.live.clips, performancePadsAtStart, performancePadTaps, t.start, end, project.value.beatsPerBar)
        performancePadTaps = ArrayList()
        padSpansForTest = padSpans
        bouncePerformancePads(padSpans, t.start, end, if (performanceAppend) before.song.sumOf { before.partSteps(it) } else 0, undoTo)
        // Never silently: the song full (128 parts) or no lane left for a setting moved.
        val lost = listOfNotNull(
            tr("Le morceau est plein (%d parties) : la fin de la prise n'a pas été écrite.", SongPart.MAX_PARTS).takeIf { written.cut },
            (if (written.skipped == 1) tr("Plus de place pour d'autres réglages (16 au plus) : 1 n'a pas été écrit.")
            else tr("Plus de place pour d'autres réglages (16 au plus) : %d n'ont pas été écrits.", written.skipped)).takeIf { written.skipped > 0 },
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
            partsLeft <= 0 -> tr("Plus aucune partie possible : la prise va s'arrêter.")
            partsLeft == 1 -> tr("Plus qu'une partie possible : la prise s'arrêtera ensuite.")
            partsLeft <= TAKE_PARTS_WARNING -> tr("Plus que %d parties possibles : la prise s'arrêtera ensuite.", partsLeft)
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

    /**
     * Leaving the Live page, or the full-screen live pads opened from Morceau: a take never goes on
     * unseen behind another page. Armed, it is cancelled; recording, it ends and is written (the
     * song before it kept as a version), and said, since its « Morceau écrit » line goes with the
     * page. Nothing the page set stays on ([leaveLive]).
     */
    fun leaveLiveEndingTake() {
        if (performanceTaking) {
            togglePerformance()
            if (_take.value is com.pocketstudio.app.live.TakeUi.Done && _notice.value == null) {
                _notice.value = tr("Prise terminée : le morceau est écrit (↶ pour l'annuler).")
            }
        }
        leaveLive()
        dismissPerformance()
    }

    /** The « Morceau écrit » line goes. */
    fun dismissPerformance() {
        if (_take.value is com.pocketstudio.app.live.TakeUi.Done) _take.value = com.pocketstudio.app.live.TakeUi.Ready
    }

    /**
     * « Versions du morceau »: version [index] becomes the song, and the song it replaces is kept
     * in its place (a second restore brings it back). One undo step; the song stops first.
     */
    fun restoreSongVersion(index: Int) {
        if (performanceTaking || _recording.value || _audioRecording.value != null) return
        if (index !in project.value.songVersions.indices) return
        if (_transport.value.playing) toggleTransport()
        if (_take.value is com.pocketstudio.app.live.TakeUi.Done) _take.value = com.pocketstudio.app.live.TakeUi.Ready
        session.edit { it.restoreVersion(index, System.currentTimeMillis()) }
        _playFrom.value = 0
        syncEngine()
        // Its pads' sounds (on disk, not in memory while it was put aside) come back to the pads.
        reloadMissingSamples()
        pushPads()
        AppLog.add("Version du morceau remise (${index + 1})")
        _done.value = tr("Fait : version remise. L'autre est gardée dans les versions.")
    }

    /** Version [index] forgotten (one undo step). */
    fun dropSongVersion(index: Int) {
        if (index !in project.value.songVersions.indices) return
        session.edit { it.dropVersion(index) }
    }

    /** Stop during a take: it ends at the bar being played (armed: dropped). */
    private fun endPerformanceOnStop() {
        when (_take.value) {
            is com.pocketstudio.app.live.TakeUi.Armed -> cancelPerformance()
            is com.pocketstudio.app.live.TakeUi.Recording -> finishPerformance(performanceEndHeard(takePosition()))
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
        NativeEngine.setAutomationMode(i, lane.mode, curve.size, lane.songPerBeat, lane.hold, if (lane.mode == AutomationLane.SONG) 0 else lane.loopSteps)
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

    /**
     * Audit 06: loop lane [index] over [steps] from the part's start (the whole part), or over the
     * drum loop (0). Points past the new length stay (a longer one later finds them).
     */
    fun setAutomationLoopSteps(index: Int, steps: Int) {
        val lane = project.value.automation.getOrNull(index) ?: return
        val want = steps.takeIf { it in 1..AutomationLane.STEPS } ?: 0
        if (lane.loopSteps == want) return
        session.edit { p -> p.copy(automation = p.automation.mapIndexed { i, l -> if (i == index) l.copy(loopSteps = want) else l }) }
        pushAutomation()
        pushMixer()
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

    /**
     * One edit of lane [index] (a ramp, smoothing, a point of a beat curve): one undo step, strokes
     * with the same [key] merged; the lane's points stay where they always were (same fields).
     */
    fun editAutomationLane(index: Int, key: String?, transform: (AutomationLane, Project) -> AutomationLane) {
        if (index !in project.value.automation.indices) return
        session.edit(key) { p -> p.copy(automation = p.automation.mapIndexed { i, l -> if (i == index) transform(l, p).sanitized() else l }) }
        val lane = project.value.automation.getOrNull(index) ?: return
        if (!NativeEngine.isAvailable) return
        if (lane.mode == AutomationLane.SONG) pushSongCurve(project.value, index, lane) else pushAutomation()
    }

    /** « Filtre (balayage) »: a filter on the track's first free slot, its cutoff automated. */
    fun addFilterAutomation(ch: Int) {
        val c = project.value.mixer.channels.getOrNull(ch) ?: return
        val slot = c.inserts.indexOfFirst { it.effect == com.pocketstudio.model.EffectType.NONE }
        if (slot < 0 || project.value.automation.size >= AutomationLane.MAX_LANES) return
        setInsertType(ch, slot, com.pocketstudio.model.EffectType.FILTER)
        addAutomation(AutoTarget(AutoTarget.INSERT_PARAM, ch, slot, 0))
    }

    fun addAutomation(target: AutoTarget) {
        if (!target.isValid || project.value.automation.size >= AutomationLane.MAX_LANES || project.value.automation.any { it.target == target }) return
        // A volume is moved along the song (louder at the start, a fade at the end), not every loop.
        val song = target.kind == AutoTarget.VOLUME && project.value.song.isNotEmpty()
        val lane = if (song) AutomationLane(target, mode = AutomationLane.SONG) else AutomationLane(target)
        session.edit { p -> p.copy(automation = p.automation + lane) }
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
            // A curve over the whole part: on its step from the part's start (audit 06).
            val own = project.value.automation[index].loopSteps
            setAutomationPoint(index, if (own > 0) Math.floorMod(NativeEngine.absoluteStep(), own) else step, value)
        }
    }

    /** After a stop, settings left where the automation put them go back to the project's values. */
    private fun restoreAfterAutomation() {
        if (project.value.automation.isNotEmpty()) pushMixer(project.value)
    }

    // --- Remix: a separated song played along with the project ---------------------------------

    private var stems: NativeStems? = null
    private var stemsId: String? = null
    /** Length in seconds of the stems loaded ([stemsId]): the export's end for a project saved before it kept it. */
    private val remixSeconds = MutableStateFlow(0.0)
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
        // The engine keeps its own reference until it lets go; freeing a whole song is not for Main.
        stretched?.let { old -> viewModelScope.launch(Dispatchers.Default) { old.close() } }
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
                pushRemix()  // back to the song as is, not the copy for another tempo
                return@launch
            }
            val old = stretched
            stretched = copy
            stretchedFor = id to ratio
            pushRemix()  // the engine goes on from the same musical point in the new copy, crossfading
            // Freeing a whole song takes long: never on Main (the freeze of phase 16).
            if (old != null) viewModelScope.launch(Dispatchers.Default) { old.close() }
            AppLog.add("Remix : calé à ${"%.1f".format(p.tempoBpm)} BPM (x${"%.3f".format(ratio)})")
        }
    }

    internal object RemixCopy {
        /**
         * Stretch of the copy to play for [wanted] (1.0 = the song as is): the one made for it once
         * ready; meanwhile the copy for the previous tempo goes on (no swap back to the song's own
         * tempo during a tempo drag); the song as is when it cannot be fitted.
         */
        fun playRatio(canFit: Boolean, setId: String?, wanted: Double, stretchedFor: Pair<String?, Double>?): Double =
            if (!canFit || wanted == 1.0) 1.0 else stretchedFor?.takeIf { it.first == setId }?.second ?: 1.0
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
        // Following the project's tempo: the stretched copy once it is ready, the previous copy meanwhile.
        val ratio = p.remix.stretchFor(p.tempoBpm)
        val canFit = loaded != null && ratio in 0.5..2.0 && (p.remix.setId to ratio) !in stretchFailed
        val playRatioNow = RemixCopy.playRatio(canFit, p.remix.setId, ratio, stretchedFor)
        val fitted = stretched?.takeIf { loaded != null && stretchedFor == (p.remix.setId to ratio) }
        NativeEngine.setStems(if (playRatioNow != 1.0) stretched else loaded)
        playRatio = playRatioNow
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
            remixSeconds.value = loaded.frames.toDouble() / OfflineExport.SAMPLE_RATE
            // A project saved before the remix kept its length learns it (not an undo step).
            if (wanted.seconds <= 0) session.editQuietly { p -> if (p.remix.setId == id) p.copy(remix = p.remix.copy(seconds = remixSeconds.value)) else p }
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
                remix = Remix(set.id, set.name, info.bpm, info.firstBeat, seconds = set.seconds),
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
            _sampleTask.value = SampleTask.Failed(tr("Les 16 pads live sont pris : vides-en un dans Live › Pads."))
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
        // G1b: the layers' sounds too (as sounds of their own), and the slots of the layers gone back to none.
        val layerSounds = (0 until InstrumentTrack.MAX).flatMap { id ->
            val layers = if (id == 0 || p.tracks.any { it.id == id }) p.track(id).layers else emptyList()
            (0 until NativeEngine.LAYERS).map { k -> (layers.getOrNull(k)?.sound ?: com.pocketstudio.model.LayerSound()).asTrack(NativeEngine.layerSound(id, k)) }
        }
        (p.instrumentTracks + layerSounds).forEach { t ->
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
        val wanted = project.value.allTrackSounds.mapNotNull { it.instrument?.bankId }.toSet()
        wanted.filter { it !in fonts && it !in loadingBanks }.forEach { id ->
            val bank = InstrumentLibrary.bank(id)
            if (!InstrumentLibrary.isInstalled(app, bank)) return@forEach
            InstrumentLibrary.memoryShortfall(app, bank)?.let { needed ->
                AppLog.add("Banque ${bank.name} non ouverte : mémoire libre insuffisante")
                _notice.value = bankMemoryMessage(needed)
                return@forEach
            }
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

    private fun bankMemoryMessage(needed: Long) =
        tr("Pas assez de mémoire libre pour ouvrir cette banque de sons (il faut environ %d Mo). Ferme des applis, ou choisis une banque plus légère.", (needed / 1_000_000).toInt())

    /** Closes banks nobody plays or browses any more (the engine keeps its own instances). */
    private fun releaseUnusedFonts() {
        // An export may be copying a font on its worker (TinySoundFont's count is not atomic).
        if (running != null || exportJob != null) return
        val used = project.value.allTrackSounds.mapNotNull { it.instrument?.bankId }.toSet() + _bankId.value
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
        InstrumentLibrary.memoryShortfall(app, bank)?.let { needed ->
            AppLog.add("Banque ${bank.name} non ouverte : mémoire libre insuffisante")
            _library.value = LibraryState.Failed(bankMemoryMessage(needed))
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
    fun loadKit(kit: Preset, whole: Boolean = false) {
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
                if (whole) putKit(pads) else session.edit { p -> p.copy(drums = Slicer.apply(p.drums, pads)) }
                pushPads()
                AppLog.add("Kit ${kit.name} : ${pads.size} sons sur les pads")
                _sampleTask.value = SampleTask.Idle
                if (NativeEngine.isAvailable) NativeEngine.triggerPad(0, 0.9f)
                // From Rythme's « Kit ▾ »: said under the pads, with Annuler.
                if (whole) {
                    _done.value = tr("Fait : kit %s sur les 16 pads", tr(kit.name))
                    return@launch
                }
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

    private val _packsOnPhone = MutableStateFlow<List<com.pocketstudio.model.PackManifest>>(emptyList())
    /** The sound packs downloaded, read again when « Kit ▾ » or a pad's sheet opens. */
    val packsOnPhone: StateFlow<List<com.pocketstudio.model.PackManifest>> = _packsOnPhone.asStateFlow()

    fun refreshPacks() {
        viewModelScope.launch {
            _packsOnPhone.value = withContext(Dispatchers.IO) {
                runCatching { com.pocketstudio.app.library.PackStore.installed(getApplication()) }.getOrDefault(emptyList())
            }
        }
    }

    /**
     * What « Kit ▾ » offers: the two kits built in (Studio, the assistant's; Classique, the first
     * one), the General MIDI kits once the instrument bank is on the phone, and each downloaded
     * pack that has drums.
     */
    fun kitChoices(packs: List<com.pocketstudio.model.PackManifest>, lib: LibraryState): List<KitChoice> = buildList {
        add(KitChoice("builtin:${Project.KIT_V3}", tr("Studio"), KitChoice.BUILT_IN))
        add(KitChoice("builtin:${Project.KIT_V1}", tr("Classique"), KitChoice.BUILT_IN))
        // Lot 2: trap and drill without a pack, the studio kit with the engine's 808 on its pad.
        add(KitChoice("kit808:trap", tr("Trap (808)"), KitChoice.BUILT_IN))
        add(KitChoice("kit808:drill", tr("Drill (808)"), KitChoice.BUILT_IN))
        (lib as? LibraryState.Ready)?.presets?.filter { it.bank == 128 }?.sortedBy { it.program }?.forEach { kit ->
            add(KitChoice("gm:${kit.program}", tr(kit.name), KitChoice.GM))
        }
        packs.filter(com.pocketstudio.model.PackKits::hasDrums).forEach { add(KitChoice("pack:${it.id}", it.name, KitChoice.PACK)) }
    }

    /**
     * « Kit ▾ »: kit [id] (see [kitChoices]) on the 16 pads, the whole kit replaced (the pads'
     * own sounds too), one undo step. A kit built in is the engine's voices; the others are sounds
     * prepared first (the busy line under the pads says so).
     */
    fun chooseKit(id: String) {
        val kind = id.substringBefore(':')
        val rest = id.substringAfter(':', "")
        when (kind) {
            "builtin" -> {
                val version = rest.toIntOrNull()?.coerceIn(Project.KIT_V1, Project.KIT_V3) ?: return
                // The kit's kicks replace the studio kicks; the 808 and the rumble (the song's bass) stay.
                session.edit { p ->
                    p.copy(
                        kitVersion = version, drums = p.drums.copy(lanes = p.drums.lanes.map { it.copy(sample = null) }),
                        padVoices = p.padVoices.filterValues { !it.isKick },
                    )
                }
                pushPads()
                if (NativeEngine.isAvailable) {
                    NativeEngine.setKitVersion(version)
                    pushPadVoices(project.value)
                    NativeEngine.triggerPad(0, 0.9f)
                }
                _done.value = tr("Fait : kit %s sur les 16 pads", if (version >= Project.KIT_V2) tr("Studio") else tr("Classique"))
            }
            "gm" -> {
                val program = rest.toIntOrNull() ?: return
                val kit = (_library.value as? LibraryState.Ready)?.presets?.firstOrNull { it.bank == 128 && it.program == program } ?: return
                loadKit(kit, whole = true)
            }
            "pack" -> _packsOnPhone.value.firstOrNull { it.id == rest }?.let(::loadPackKit)
            "kit808" -> {
                if (com.pocketstudio.model.StudioDrums.with808Kit(project.value, rest) == null) return
                session.edit { p -> com.pocketstudio.model.StudioDrums.with808Kit(p, rest) ?: p }
                pushPads()
                pushMixer()
                if (NativeEngine.isAvailable) {
                    NativeEngine.setKitVersion(Project.KIT_V3)
                    pushPadVoices(project.value)
                    NativeEngine.triggerPad(com.pocketstudio.model.StudioDrums.LANE_808, 0.9f)
                }
                val kitName = if (rest == "drill") tr("Drill (808)") else tr("Trap (808)")
                // A « Basse 808 » track already plays the bass: said, so two 808s are not a surprise.
                _done.value = if (project.value.tracks.any { it.engine == com.pocketstudio.model.InstrumentTrack.ENGINE_808 }) {
                    tr("Fait : kit %s sur les 16 pads. Ta piste Basse 808 joue aussi : coupe-la si tu n'en veux qu'une.", kitName)
                } else {
                    tr("Fait : kit %s sur les 16 pads", kitName)
                }
            }
        }
    }

    /** The 16 pads with [pads]' sounds (pad, sound), the others back to the kit's voices: one undo step. */
    private fun putKit(pads: List<Pair<Int, PadSample>>) {
        val byLane = pads.toMap()
        session.edit { p -> p.copy(drums = p.drums.copy(lanes = p.drums.lanes.mapIndexed { i, l -> l.copy(sample = byLane[i]) })) }
    }

    /** A downloaded pack as a kit ([com.pocketstudio.model.PackKits]): each pad's sound decoded, then all placed at once. */
    private fun loadPackKit(m: com.pocketstudio.model.PackManifest) {
        if (_sampleTask.value is SampleTask.Busy || _sampleTask.value is SampleTask.Recording) {
            _notice.value = tr("Patiente un instant, un son est en cours de préparation.")
            return
        }
        _sampleTask.value = SampleTask.Busy(tr("Kit %s…", m.name))
        viewModelScope.launch {
            try {
                val app = getApplication<Application>()
                val pads = ArrayList<Pair<Int, PadSample>>()
                for ((lane, sound) in com.pocketstudio.model.PackKits.sounds(m)) {
                    val pcm = withContext(Dispatchers.IO) {
                        runCatching { com.pocketstudio.app.audio.AudioDecoder.decode(app, Uri.fromFile(com.pocketstudio.app.library.PackStore.file(app, m.id, sound))) }.getOrNull()
                    } ?: continue
                    val (sample, prepared) = withContext(Dispatchers.IO) { padSamples.add(pcm.withoutTail(), com.pocketstudio.model.DefaultNames.kitPad(m.name, lane)) }
                    padSamples.register(prepared)
                    // The two hats cut each other, as on a drum machine.
                    pads += lane to sample.copy(choke = if (lane == 5 || lane == 6) 1 else 0)
                }
                if (pads.isEmpty()) {
                    _sampleTask.value = SampleTask.Failed(tr("Ce pack n'a pas pu être lu."))
                    return@launch
                }
                putKit(pads)
                pushPads()
                AppLog.add("Kit du pack ${m.id} : ${pads.size} sons sur les pads")
                _sampleTask.value = SampleTask.Idle
                if (NativeEngine.isAvailable) NativeEngine.triggerPad(0, 0.9f)
                _done.value = tr("Fait : kit %s sur les 16 pads", m.name)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _sampleTask.value = SampleTask.Failed(tr("Le kit n'a pas pu être chargé (%s).", e.message))
            }
        }
    }

    // --- Piano roll ------------------------------------------------------------------------

    /** The engine plays the clip as performed: arpeggios and strums expanded here. */
    private fun pushNotes(clip: NoteClip, id: Int = track) {
        if (!NativeEngine.isAvailable || waitsFor(LiveSlots.INST + id)) return
        pushNotesNow(clip, id)
    }

    /** [clip] to track [id] in the engine, whatever scene is shown. */
    private fun pushNotesNow(clip: NoteClip, id: Int) {
        if (!NativeEngine.isAvailable) return
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
        // Phase 37: a frozen track plays its audio in the song; its notes wait for « Dégeler ».
        if (com.pocketstudio.model.Freeze.isFrozen(project.value, id)) {
            _notice.value = tr("Piste gelée : « Dégeler » pour modifier ses notes.")
            return
        }
        editTrackNotes(key, id, transform)
    }

    /**
     * Track [id]'s notes [transform]ed (one undo step); the assistant's Sub and Basse 2 that double it
     * follow ([com.pocketstudio.model.LinkedNotes]), in the engine too.
     */
    private fun editTrackNotes(key: String?, id: Int, transform: (NoteClip) -> NoteClip) {
        session.edit(key) { p ->
            if (p.instrumentTracks.none { it.id == id }) p
            else com.pocketstudio.model.LinkedNotes.sync(p, p.withTrack(p.track(id).let { it.copy(notes = transform(it.notes)) }.sanitized()), id)
        }
        pushNotes(project.value.track(id).notes, id)
        com.pocketstudio.model.LinkedNotes.followers(project.value, id).forEach { pushNotes(project.value.track(it.id).notes, it.id) }
    }

    /**
     * After a take (quiet edits since [before]): the Sub and Basse 2 doubling a track it wrote in
     * follow its new notes, in the same undo step; heard at once where their scene plays.
     */
    private fun followTakeNotes(before: Project) {
        val led = project.value.instrumentTracks.map { it.id }.filter { com.pocketstudio.model.LinkedNotes.followers(before, it).isNotEmpty() }
        if (led.isEmpty()) return
        val now = project.value
        session.editQuietly { p -> led.fold(p) { acc, id -> com.pocketstudio.model.LinkedNotes.sync(before, acc, id) } }
        if (project.value === now) return
        val scenes = takeNotes.keys.map { it.first }.toSet() + project.value.scene
        for (id in led) for (f in com.pocketstudio.model.LinkedNotes.followers(before, id)) for (scene in scenes) {
            if (scene in project.value.allScenes.indices && playsNow(LiveSlots.INST + f.id, scene)) pushNotesNow(project.value.sceneView(scene).track(f.id).notes, f.id)
        }
    }

    /** A whole-clip edit from the piano roll's selection tools. */
    fun replaceNoteClip(clip: NoteClip, key: String?) = editNotes(key) { clip.sanitized() }

    fun addNote(note: Note) {
        editNotes { it.add(note) }
        if (NativeEngine.isAvailable && !_transport.value.playing) previewNote(note.pitch)
    }

    fun removeNote(note: Note) = editNotes { it.remove(note) }

    /** A note dragged elsewhere forgets where it was played (« Comme joué » would pull it back). */
    fun changeNote(old: Note, new: Note) = editNotes { it.replace(old, if (new.start != old.start) new.copy(played = null) else new) }

    fun quantizeNotes(gridTicks: Int, strength: Float) = editNotes { it.quantized(gridTicks, strength) }

    /** « Comme joué » in the piano roll: every note put in time back where it was played (one undo step). */
    fun notesAsPlayed() = editNotes { it.asPlayed { true } }

    fun setNoteClipSteps(steps: Int) = editNotes { it.withSteps(steps) }

    fun clearNotes() = editNotes { it.cleared() }

    /** Empties track [id]'s notes in the scene shown (one undo step), whichever track is open. */
    fun clearTrackNotes(id: Int) {
        if (project.value.instrumentTracks.none { it.id == id }) return
        editTrackNotes(null, id) { it.cleared() }
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
        // The pads of the song versions kept aside too (« Ramener » loads them).
        val previousTakes = project.value.previousTakeFiles + project.value.versionSampleFiles
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
            // A song the assistant just made: its live pads are prepared now that its sounds are in.
            if (project.value.livePadsPending) fillLivePads()
        }
    }

    /** The live pads being prepared from the song ([fillLivePads]). */
    private var liveFill: kotlinx.coroutines.Job? = null

    /**
     * A song the assistant made ([Project.livePadsPending]): each track's loop of the scene where it
     * plays fullest, the riser's impact and the whole scene go onto the free live pads
     * ([com.pocketstudio.model.LivePadPlan]), rendered offline from the song as written, one by one
     * on a private engine (as « Rééchantillonner » records the song). One quiet step at the end (an
     * « Annuler » of the user's own edits never meets it); pads already set stay. Interrupted (the
     * project closed), it starts over next time; failed, the pads stay empty and it is not tried again.
     */
    private fun fillLivePads() {
        if (!NativeEngine.isAvailable || liveFill?.isActive == true) return
        liveFill = viewModelScope.launch {
            // Banks and multizone instruments still opening: waited for (else their track renders
            // silent, or its synth stand-in).
            awaitSoundsLoaded()
            // The song as it is once the sounds are in (edits made while waiting included).
            val p = project.value
            val plan = com.pocketstudio.model.LivePadPlan.of(p)
            val made = ArrayList<LiveClip>()
            try {
                for (spec in plan) {
                    val view = p.stemView(spec.key, spec.scene)
                    val frames = OfflineExport.framesFor(spec.bars, p.tempoBpm, p.beatsPerBar)
                    val tail = if (spec.oneShot) OfflineExport.SAMPLE_RATE * 2L else OfflineExport.SAMPLE_RATE / 2L
                    // A private engine costs tens of milliseconds to build: off the main thread.
                    val job = finishOrRelease(Dispatchers.IO, OfflineExport::close) { OfflineExport() }!!
                    val data = try {
                        NativeEngine.captureInto(job) {
                            syncEngine(view)
                            NativeEngine.setMetronome(false)
                            // The view has no target (stemView); said again here: the pad is
                            // brought to the song's loudness once, live, in the master chain.
                            NativeEngine.setLoudnessTarget(null)
                        }
                        withContext(Dispatchers.Default) { job.renderToArray(frames, tail) }
                    } finally {
                        // Native instruments share reference counts: freed on the main thread only.
                        withContext(NonCancellable + Dispatchers.Main) { job.close() }
                    }
                    if (data.isEmpty()) continue
                    val audio = withContext(Dispatchers.Default) {
                        if (spec.oneShot) com.pocketstudio.app.library.SoundLibraryViewModel.trimSilence(data)
                        else com.pocketstudio.app.library.SoundLibraryViewModel.loopOf(data, frames)
                    }
                    // A track the scene leaves silent (a riser held shut): no pad for it.
                    if (audio.none { kotlin.math.abs(it) > SILENT }) continue
                    val (sample, prepared) = withContext(Dispatchers.IO) { padSamples.add(Pcm(audio, 2, OfflineExport.SAMPLE_RATE), spec.name) }
                    padSamples.register(prepared)
                    made += LiveClip(sample.file, sample.name, (spec.bars * p.beatsPerBar).coerceAtMost(LivePads.MAX_BEATS), spec.oneShot)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.add("Pads live du morceau : échec (${e.message ?: e.javaClass.simpleName})")
            } catch (e: OutOfMemoryError) {
                AppLog.add("Pads live du morceau : mémoire insuffisante")
            }
            session.editQuietly { q ->
                var live = q.live
                var next = 0
                for (clip in made) {
                    while (next < LivePads.MAX && live.clips.getOrNull(next) != null) next++
                    if (next >= LivePads.MAX) break
                    live = live.with(next, clip.sanitized())
                    next++
                }
                q.copy(live = live, livePadsPending = false)
            }
            pushLive()
            AppLog.add("Pads live du morceau : ${made.size} sons préparés")
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
        val busy = tr("Enregistrement du morceau…")
        resampleCancelled = false
        sampleJob(busy, cancellable = true) {
            // A private engine costs tens of milliseconds to build: here, off the main thread (this
            // work runs on IO); described on the main thread, like every other command.
            val job = finishOrRelease(Dispatchers.IO, OfflineExport::close) { OfflineExport() }!!
            val data = try {
                withContext(Dispatchers.Main) {
                    NativeEngine.captureInto(job) {
                        syncEngine()
                        NativeEngine.setMetronome(false)
                    }
                    resampling = job
                }
                kotlinx.coroutines.coroutineScope {
                    val poll = launch {
                        while (!resampleCancelled) {
                            _sampleTask.value = SampleTask.Busy("$busy ${com.pocketstudio.model.i18n.percent((job.progress * 100).roundToInt())}", cancellable = true)
                            kotlinx.coroutines.delay(100)
                        }
                    }
                    try {
                        job.renderToArray(frames, 0)
                    } finally {
                        poll.cancel()
                    }
                }
            } finally {
                // Native instruments share reference counts: free them on the main thread only.
                withContext(kotlinx.coroutines.NonCancellable + Dispatchers.Main) {
                    resampling = null
                    job.close()
                }
            }
            if (resampleCancelled) throw kotlinx.coroutines.CancellationException("annulé")
            if (data.isEmpty()) throw UserMessage(tr("Le rendu a été interrompu."))
            AppLog.add("Rééchantillonnage : $bars mesures à ${p.tempoBpm.toInt()} BPM")
            padSamples.add(Pcm(data, 2, OfflineExport.SAMPLE_RATE), if (bars > 1) "Morceau $bars mesures" else "Morceau 1 mesure")  // shown translated (DefaultNames)
        }
    }

    /** The song being recorded onto the pad ([resampleSong]), its engine and whether it was stopped. */
    private var resampleJob: kotlinx.coroutines.Job? = null
    private var resampling: OfflineExport? = null
    @Volatile private var resampleCancelled = false

    /** « Annuler » while the song is recorded onto the pad: nothing is written. */
    fun cancelResample() {
        if (resampleJob?.isActive != true) return
        resampleCancelled = true
        resampling?.cancel()
        resampleJob?.cancel()
        _sampleTask.value = SampleTask.Idle
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
    private fun sampleJob(
        busy: String?, live: Int? = null, beats: Int? = null, cancellable: Boolean = false,
        work: suspend () -> Pair<PadSample, PadSamples.Prepared>,
    ) {
        val lane = _transport.value.selectedPad
        if (busy != null) _sampleTask.value = SampleTask.Busy(busy, cancellable)
        // The one that can be cancelled is the song recorded onto the pad ([cancelResample]).
        val job = viewModelScope.launch {
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
        if (cancellable) resampleJob = job
    }

    /** The file's name as the phone shows it; for plain file links, the last part of the path. */
    private fun displayName(app: Application, uri: Uri): String? = runCatching {
        app.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull() ?: uri.lastPathSegment

    // --- Mixer -----------------------------------------------------------------------------

    // The limiter's slot starts at 1 (no limiting): 0 read as a gain was « retenu de 214748368 dB » (lot 2).
    private val _meters = MutableStateFlow(FloatArray(NativeEngine.METERS).also { it[NativeEngine.METER_LIMITER] = 1f })
    /** Peak per mixer channel (the master's at [MASTER]), the limiter gain, the master's level into the limiter. */
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
    private val meterPeaks = FloatArray(NativeEngine.METERS).also { it[NativeEngine.METER_LIMITER] = 1f }
    private val meterFresh = FloatArray(NativeEngine.METERS)
    private val reductionShown = FloatArray(NativeEngine.INSERT_METERS)
    private val reductionFresh = FloatArray(NativeEngine.INSERT_METERS)
    private val _reductions = MutableStateFlow(FloatArray(NativeEngine.INSERT_METERS))
    /** Each insert's compressor gain reduction in dB (channel * 8 + slot), polled with the meters. */
    val reductions: StateFlow<FloatArray> = _reductions.asStateFlow()
    private val loudnessFresh = FloatArray(NativeEngine.LOUDNESS)
    private val _loudness = MutableStateFlow(FloatArray(NativeEngine.LOUDNESS) { NativeEngine.LOUDNESS_NONE })
    /**
     * The master's loudness and true peak ([NativeEngine.LOUDNESS_MOMENTARY]…), polled with [meters];
     * the peak since the last reading ([NativeEngine.LOUDNESS_RECENT_PEAK]) is held a second (a clip light).
     */
    val loudness: StateFlow<FloatArray> = _loudness.asStateFlow()
    private val recentPeak = com.pocketstudio.app.mixer.PeakHold()

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
                    // The compressors' reduction: at once when it deepens, back up by 1 dB a reading (20 dB/s).
                    NativeEngine.insertReductions(reductionFresh)
                    var moved = false
                    for (i in reductionShown.indices) {
                        val next = if (reductionFresh[i] < reductionShown[i]) reductionFresh[i] else minOf(0f, reductionShown[i] + 1f)
                        if (next != reductionShown[i]) {
                            reductionShown[i] = next
                            moved = true
                        }
                    }
                    if (moved) _reductions.value = reductionShown.copyOf()
                    NativeEngine.loudnessMeters(loudnessFresh)
                    loudnessFresh[NativeEngine.LOUDNESS_RECENT_PEAK] = recentPeak.update(loudnessFresh[NativeEngine.LOUDNESS_RECENT_PEAK])
                    if (!loudnessFresh.contentEquals(_loudness.value)) _loudness.value = loudnessFresh.copyOf()
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

    /** « Solo actif : tout désactiver »: every track heard again, in one undo step. */
    fun clearSolos() {
        val soloed = project.value.mixer.channels.indices.filter { project.value.mixer.channels[it].solo }
        if (soloed.isEmpty()) return
        session.edit { p -> p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.map { if (it.solo) it.copy(solo = false) else it })) }
        soloed.forEach { ch ->
            mixerPushesForTest?.add("strip $ch")
            if (NativeEngine.isAvailable && ch < NativeEngine.MIXER_CHANNELS) NativeEngine.setChannelSolo(ch, false)
        }
    }

    /** « Tout réentendre » (Prêt à partager ?): strips [chs] no longer muted, in one undo step. */
    fun unmuteChannels(chs: List<Int>) {
        val before = project.value.mixer.channels
        val muted = chs.filter { before.getOrNull(it)?.mute == true }.toSet()
        if (muted.isEmpty()) return
        session.edit { p -> p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.mapIndexed { i, c -> if (i in muted) c.copy(mute = false) else c })) }
        muted.forEach { pushStrip(it, before[it]) }
    }

    private fun editChannel(ch: Int, key: String?, transform: (com.pocketstudio.model.Channel) -> com.pocketstudio.model.Channel) {
        val before = project.value.mixer.channels.getOrNull(ch)
        session.edit(key?.let { "mix-$ch-$it" }) { p ->
            val channels = p.mixer.channels.toMutableList()
            channels[ch] = transform(channels[ch])
            p.copy(mixer = p.mixer.copy(channels = channels))
        }
        pushStrip(ch, before)
    }

    /** Tests: the mixer pushes made ("all", or "strip <ch>" for one strip's changes). */
    internal var mixerPushesForTest: MutableList<String>? = null

    /**
     * Strip [ch] changed from [before]: only what changed there goes to the engine (a fader moved
     * live sends its volume, not the whole mixer). The master, a strip not in use, or a pad strip
     * that starts or stops having a sound of its own: the whole mixer, as [pushMixer] does it.
     */
    private fun pushStrip(ch: Int, before: com.pocketstudio.model.Channel?) {
        val p = project.value
        val c = p.mixer.channels.getOrNull(ch) ?: return
        val pad = Mixer.isPad(ch)
        if (before == null || ch == MASTER || ch >= NativeEngine.MIXER_CHANNELS || ch !in usedStrips(p) || (pad && before.routesPad() != c.routesPad())) {
            pushMixer(p)
            return
        }
        mixerPushesForTest?.add("strip $ch")
        if (!NativeEngine.isAvailable) return
        if (c.volume != before.volume) NativeEngine.setChannelVolume(ch, c.volume)
        if (c.pan != before.pan) NativeEngine.setChannelPan(ch, c.pan)
        if (c.mute != before.mute) NativeEngine.setChannelMute(ch, c.mute || (!NativeEngine.isCapturing && ch in liveMutedChannels()))
        if (c.solo != before.solo) NativeEngine.setChannelSolo(ch, c.solo)
        if (c.inserts != before.inserts) pushInserts(ch, c.inserts)
        c.sends.forEachIndexed { bus, level -> if (before.sends.getOrNull(bus) != level) NativeEngine.setSend(ch, bus, level) }
        c.macros.forEachIndexed { m, v -> if (before.macros.getOrNull(m) != v) NativeEngine.setMacro(ch, m, v) }
    }

    private fun pushMixer(p: Project = project.value) {
        mixerPushesForTest?.add("all")
        if (!NativeEngine.isAvailable) return
        // Only the strips in use: the engine's command queue has room for whole projects.
        val pads = (0 until DrumPattern.PAD_COUNT).filter { p.mixer.channels.getOrNull(Mixer.padChannel(it))?.routesPad() == true }
        val used = usedStrips(p)
        // First the strips of tracks gone (deleted, undone, another project): their effects go in
        // one command, giving their echo, reverb and tape stop memory back before the strips in
        // use ask for it (else an effect there would stay silent live while the export plays it).
        // No solo or mute may linger in the engine either, where a solo would silence the rest.
        stripsToClear(p).forEach { i ->
            NativeEngine.clearInserts(i)
            if (!Mixer.isPad(i)) {
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
        NativeEngine.setLoudnessTarget(loudnessLufs(p))
        NativeEngine.setMonoBass(p.mixer.monoBassHz)
        pushSidechain(p)
        p.mixer.buses.forEachIndexed { bus, params -> params.forEachIndexed { k, v -> NativeEngine.setBusParam(bus, k, v) } }
        // Pads with effects of their own go through their strip, the others straight into the drums.
        (0 until DrumPattern.PAD_COUNT).forEach { NativeEngine.setPadStrip(it, it in pads) }
    }

    /**
     * S5: the sidechain's key and the « Musique » bus. Every strip's membership is said (a strip
     * left in the bus by another project must not stay there); a project without the bus sends
     * none, its depth 0: the engine mixes as before.
     */
    private fun pushSidechain(p: Project) {
        // A pump on « Le son du kick » without a key of its own follows the kick's pad (audit 06).
        val key = p.mixer.heardSidechainKey
        if (!NativeEngine.isCapturing) liveSidechainKey = key
        NativeEngine.setSidechainKey(key)
        val bus = p.mixer.musicBus
        NativeEngine.setMusicBusDuck(bus?.depth ?: 0f, bus?.lowDepth ?: 0f, bus?.release ?: 0.35f)
        for (ch in Mixer.TRACK_STRIPS) if (ch != MASTER) NativeEngine.setMusicBusMember(ch, bus != null && ch in bus.members)
        for (b in 0 until com.pocketstudio.model.Buses.COUNT) NativeEngine.setMusicBusReturn(b, bus?.returns == true)
    }

    /** The sidechain key the live engine was last given (pushSidechain). */
    private var liveSidechainKey = Int.MIN_VALUE

    /** An effect changed: a pump switched to « Le son du kick » (or away) changes the key at once. */
    private fun pushSidechainKeyIfChanged() {
        val key = project.value.mixer.heardSidechainKey
        if (key == liveSidechainKey || NativeEngine.isCapturing) return
        liveSidechainKey = key
        NativeEngine.setSidechainKey(key)
    }

    /** After a change to a pad's strip: it plays through it only while something there changes the sound. */
    private fun routePad(ch: Int) {
        if (!Mixer.isPad(ch) || !NativeEngine.isAvailable) return
        val c = project.value.mixer.channels.getOrNull(ch) ?: return
        if (c.routesPad()) {
            // Its whole strip first (a strip left from another project must not linger).
            NativeEngine.setChannelVolume(ch, c.volume)
            NativeEngine.setChannelPan(ch, c.pan)
            NativeEngine.setChannelMute(ch, c.mute)
            pushInserts(ch, c.inserts)
            c.sends.forEachIndexed { bus, level -> NativeEngine.setSend(ch, bus, level) }
            c.macros.forEachIndexed { m, v -> NativeEngine.setMacro(ch, m, v) }
        }
        NativeEngine.setPadStrip(ch - Mixer.FIRST_PAD, c.routesPad())
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
        if (Mixer.isPad(ch) && !heard.routesPad()) {
            NativeEngine.setPadStrip(ch - Mixer.FIRST_PAD, false)
            return
        }
        pushInserts(ch, heard.inserts)
        heard.sends.forEachIndexed { bus, level -> NativeEngine.setSend(ch, bus, level) }
        heard.macros.forEachIndexed { m, v -> NativeEngine.setMacro(ch, m, v) }
        if (Mixer.isPad(ch)) {
            NativeEngine.setChannelVolume(ch, heard.volume)
            NativeEngine.setChannelPan(ch, heard.pan)
            NativeEngine.setChannelMute(ch, heard.mute)
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
        if (NativeEngine.isAvailable) pushSidechainKeyIfChanged()
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
        if (press.alreadyHeld) playedNoteOff(press.track, note)
        playedNoteOn(press.track, note, velocity)
        // A note another chord already holds sounds again: the take ends it there and starts it anew.
        if (press.alreadyHeld) recordNoteOff(note)
        recordNoteOn(note, velocity)
    }

    fun synthNoteOff(note: Int) {
        // Still held by another key (overlapping chords): it keeps sounding.
        val on = keysDown.release(note, track) ?: return
        if (NativeEngine.isAvailable) NativeEngine.synthNoteOff(note, on)
        playedNoteOff(on, note)
        recordNoteOff(note)
    }

    // --- Live recording: what is played (keyboard, MIDI, pads) is written into the loop --------

    private val _recording = MutableStateFlow(false)
    /** True while armed: notes go into the piano roll, pad hits into the drum steps. */
    val recording: StateFlow<Boolean> = _recording.asStateFlow()

    /**
     * Recording while another scene than the one shown plays (« Rester sur ma scène » during the
     * song): that scene, where the take goes (said by the ●); else null.
     */
    val recordInto: StateFlow<Int?> = combine(_recording, project, _sessionUi, _transport) { rec, _, _, t ->
        if (rec && t.playing && editsWait) heardScene else null
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private var recordBefore: Project? = null
    /** Undo steps when the take started: an edit made during it folds into the take's step. */
    private var recordDepth = 0
    private val heldNotes = HashMap<Int, HeldNote>()  // by pitch

    /** A key held while recording: where it started, on which track and in which scene. */
    private data class HeldNote(val tick: Int, val velocity: Float, val track: Int, val scene: Int)

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
            com.pocketstudio.app.synth.TakeLoop.grown(currentTrack.notes, project.value.stepsPerBar)?.let { grown ->
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
        if (!_transport.value.playing) beforeSwap = null
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
        sustained.clear()
        heldNotes.keys.toList().forEach { writeNoteOff(it) }
        if (takeSpreads) spreadMelody()
        _recording.value = false
        val before = recordBefore
        before?.let { followTakeNotes(it) }
        before?.let { session.commitQuietEdits(it, recordDepth) }
        recordBefore = null
        takeEnded(before)
    }

    /** The take is the Mode simple's melody (« Ma mélodie »): it is heard through the song once written. */
    private var takeSpreads = false

    /** « Ma mélodie »'s take written: every part where it has nothing plays it (with the take, one undo step). */
    private fun spreadMelody() {
        val id = com.pocketstudio.model.SimpleMelody.trackOf(project.value) ?: return
        val scenes = takeNotes.filter { (k, v) -> k.second == id && v.isNotEmpty() }.keys.map { it.first }.distinct()
        if (scenes.isEmpty()) return
        scenes.forEach { scene -> session.editQuietly { com.pocketstudio.model.SimpleMelody.spread(it, id, scene) } }
        if (!NativeEngine.isCapturing) playingPart()?.let { pushPart(project.value, it.scene, it) }
    }

    /** Tests (no engine): the place a gesture is heard at, in sixteenths of the playing scene (see [recordPosition]). */
    internal var recordPositionForTest: Double? = null

    /**
     * The scene each grid slot played before the engine's last scene swap, and that part's length
     * in steps: a gesture heard before the swap but read after it goes there. Null: no swap since play.
     */
    private var beforeSwap: Pair<IntArray, Int>? = null

    /** What each grid slot plays now (-1: stopped), and how long the part or scene playing lasts. */
    private fun slotScenesNow(): Pair<IntArray, Int> {
        val p = project.value
        val ui = _sessionUi.value
        val part = (ui.removed ?: p.song.getOrNull(ui.songIndex))?.takeIf { ui.songIndex >= 0 }
        val heard = engineScene(p)
        return IntArray(LiveSlots.COUNT) { s -> sceneOfSlot(p, s) ?: -1 } to (part?.let(p::partSteps) ?: p.sceneSteps(heard))
    }

    /** The place a gesture is heard at, in sixteenths of the playing scene, or null when nothing is recorded (not armed, stopped). */
    private fun recordPosition(): Double? {
        if (!_recording.value || !_transport.value.playing) return null
        recordPositionForTest?.let { return it }
        if (!NativeEngine.isAvailable) return null
        // What is heard lags what is rendered by the output latency (much more over Bluetooth).
        val latencyMs = Metrics.streamInfo.value.outputLatencyMs.takeIf { it > 0 } ?: 0.0
        return NativeEngine.playPosition() - latencyMs / 1000.0 * project.value.tempoBpm / 15.0
    }

    /**
     * The scene a gesture of grid slot [slot]'s track is written into and its tick in that scene's
     * loop ([loopTicks] of a scene), or null when nothing is recorded (not armed, stopped, counting
     * in). The scene is the one the engine plays for that track (staying on another scene during
     * the song: the scene heard, not the one shown). Heard more than half a sixteenth before a part
     * change the engine already made: the part before's scene, at its end.
     */
    private fun recordAt(slot: Int, loopTicks: (scene: Int) -> Int): Pair<Int, Int>? {
        sceneHeard()  // a swap the 40 ms watch has not seen yet is followed first
        val pos = recordPosition() ?: return null
        val step = NoteClip.TICKS_PER_STEP
        val before = beforeSwap
        val old = before?.first?.getOrNull(slot)?.takeIf { it >= 0 }
        if (pos < -0.5 && before != null && old != null) {
            val ticks = loopTicks(old).coerceAtLeast(1).toLong()
            return old to Math.floorMod(((before.second + pos) * step).toLong(), ticks).toInt()
        }
        val p = project.value
        val scene = sceneOfSlot(p, slot) ?: engineScene(p)
        // Before the first step (end of the count-in, or the latency pulling a note played on the
        // "1" just before it): up to a sixteenth early lands on the "1", earlier is not recorded.
        if (pos < 0) return if (pos >= -EARLY_STEPS) scene to 0 else null
        return scene to Math.floorMod((pos * step).toLong(), loopTicks(scene).coerceAtLeast(1).toLong()).toInt()
    }

    /** [p] with [edit] made in scene [scene] (shown or not), the scene shown kept. */
    private fun inScene(p: Project, scene: Int, edit: (Project) -> Project): Project =
        if (scene == p.scene || scene !in p.allScenes.indices) edit(p) else edit(p.switchScene(scene)).switchScene(p.scene)

    /** The engine plays scene [scene] for grid slot [slot]'s track now: an edit there is heard at once. */
    private fun playsNow(slot: Int, scene: Int): Boolean = !NativeEngine.isCapturing && sceneOfSlot(project.value, slot) == scene

    /** A note played live (keyboard or MIDI) while recording: remembered until released. */
    /** Track [id]'s loop in scene [scene], in ticks. */
    private fun clipTicks(id: Int, scene: Int): Int = project.value.sceneView(scene).track(id).notes.ticks

    fun recordNoteOn(pitch: Int, velocity: Float) {
        val id = track
        if (_noteTake.value != null && _recording.value) takeKeys++
        val at = recordAt(LiveSlots.INST + id) { clipTicks(id, it) }
        if (at == null) {
            takeDropped { "key $pitch: ${whyNotRecorded()}" }
            return
        }
        // The same key again while the pedal held the one before: that one ends here.
        if (sustained.remove(pitch)) writeNoteOff(pitch)
        heldNotes[pitch] = HeldNote(at.second, velocity, id, at.first)
    }

    /** The sustain pedal is down: keys released meanwhile are written when it comes up. */
    private var sustainDown = false
    /** Keys released while the pedal is down, still sounding (by pitch). */
    private val sustained = HashSet<Int>()

    /**
     * The sustain pedal (MIDI CC 64) down or up: while it is down, a key released keeps its note
     * going, written in the take with the length heard (up to the pedal's release).
     */
    fun recordSustain(down: Boolean) {
        sustainDown = down
        if (down) return
        val released = sustained.toList()
        sustained.clear()
        released.forEach { writeNoteOff(it) }
    }

    /** Why a gesture is not recorded now (the take's journal line). */
    private fun whyNotRecorded(): String = when {
        !_recording.value -> "not armed"
        !_transport.value.playing -> "transport stopped"
        else -> recordPosition()?.let { String.format(java.util.Locale.ROOT, "position %.2f (before the take's first step)", it) }
            ?: "no position (engine ${if (NativeEngine.isAvailable) "on" else "off"})"
    }

    private fun takeDropped(why: () -> String) {
        if (_noteTake.value == null || !_recording.value) return
        if (takeDrops.size < 4) takeDrops += why()
    }

    fun recordNoteOff(pitch: Int) {
        if (sustainDown && pitch in heldNotes) {
            sustained += pitch
            return
        }
        writeNoteOff(pitch)
    }

    private fun writeNoteOff(pitch: Int) {
        // Written on the track and in the scene it started on, even if another one came meanwhile.
        val (start, velocity, id, scene) = heldNotes.remove(pitch) ?: return
        if (project.value.instrumentTracks.none { it.id == id } || scene !in project.value.allScenes.indices) {
            takeDropped { "key $pitch: track $id or scene $scene gone" }
            return
        }
        val ticks = clipTicks(id, scene)
        val step = NoteClip.TICKS_PER_STEP
        // Released after a part change into another scene: held to its loop's end.
        val end = recordAt(LiveSlots.INST + id) { clipTicks(id, it) }?.let { (s, t) -> if (s == scene) t else ticks } ?: start + step
        // Kept where it was played (« Remettre en rythme » puts it on the sixteenths afterwards,
        // without losing that); the length too (at least half a sixteenth). The same key played
        // again on a later pass of the loop (within half a sixteenth) replaces the one before.
        val at = Math.floorMod(start, ticks)
        val length = maxOf(step / 2, Math.floorMod(end - start, ticks).let { if (it == 0 && end != start) ticks else it })
        val note = Note(at, minOf(length, NoteClip.MAX_TICKS), pitch, velocity)
        val near = { n: Note -> n.pitch == pitch && kotlin.math.abs(n.playedStart - at).let { d -> minOf(d, ticks - d) } < step / 2 }
        // Notes there before the take that this one replaces: kept aside for « Refaire » (not the take's own).
        if (_noteTake.value != null) {
            val key = scene to id
            val mine = takeNotes[key].orEmpty()
            val replaced = project.value.sceneView(scene).track(id).notes.notes.filter { near(it) && it !in mine }
            if (replaced.isNotEmpty()) takeReplaced[key] = takeReplaced[key].orEmpty() + replaced
        }
        session.editQuietly { p ->
            inScene(p, scene) { q ->
                val t = q.track(id)
                q.withTrack(t.copy(notes = t.notes.copy(notes = t.notes.notes.filterNot(near) + note).sanitized()))
            }
        }
        noteTaken(scene, id, note)
        if (playsNow(LiveSlots.INST + id, scene)) pushNotesNow(project.value.sceneView(scene).track(id).notes, id)
    }

    /** A pad hit while recording becomes a step of the drum loop (nearest sixteenth) of the scene heard. */
    private fun recordPad(lane: Int, velocity: Float) {
        fun steps(scene: Int) = project.value.sceneView(scene).drums.steps
        val (scene, tick) = recordAt(LiveSlots.DRUMS) { steps(it) * NoteClip.TICKS_PER_STEP } ?: return
        val steps = steps(scene)
        val step = Math.floorMod(Math.round(tick.toDouble() / NoteClip.TICKS_PER_STEP).toInt(), steps)
        val old = project.value.sceneView(scene).drums.lanes[lane].hits[step]
        val fresh = old == null
        // A hit there before the take: its strength kept aside for « Refaire » (not one the take added).
        val where = Triple(scene, lane, step)
        if (old != null && _noteTake.value != null && where !in takeHits && where !in takeReplacedHits) takeReplacedHits[where] = old
        session.editQuietly { p ->
            inScene(p, scene) { q ->
                val l = q.drums.lanes[lane]
                // A hit already there keeps its roll, pitch, glide…: only its strength is played again.
                val hit = l.hits[step]?.copy(velocity = velocity) ?: Hit(velocity)
                q.copy(drums = q.drums.copy(lanes = q.drums.lanes.toMutableList().also { it[lane] = l.copy(hits = l.hits + (step to hit)) }))
            }
        }
        if (fresh) hitTaken(scene, lane, step)
        if (playsNow(LiveSlots.DRUMS, scene) && NativeEngine.isAvailable) {
            val hits = project.value.sceneView(scene).drums.lanes[lane].hits
            for (s in 0 until DrumPattern.MAX_STEPS) pushHitNow(lane, s, hits[s])
        }
    }

    /** MIDI notes arrive here too (they already sound): recorded when armed. */
    fun midiNote(channel: Int, pitch: Int, velocity: Float, on: Boolean) {
        if (channel == 9) {
            if (on && pitch in 36 until 36 + DrumPattern.PAD_COUNT) {
                playedPad(pitch - 36, velocity)
                recordPad(pitch - 36, velocity)
            }
        } else if (on) {
            playedNoteOn(track, pitch, velocity)
            recordNoteOn(pitch, velocity)
        } else {
            playedNoteOff(track, pitch)
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

    /**
     * A sound of the synth's list on the selected track. Audit 01 (G1): a track played by a sound
     * file, a pack's instrument, the 808 or a General MIDI instrument goes to the synth too (in
     * the same undo step), or the sound chosen would never be heard.
     */
    fun applySynthPreset(patch: SynthPatch) {
        val other = currentTrack.engine != InstrumentTrack.ENGINE_SYNTH || currentTrack.instrument != null
        editTrack { it.copy(synth = patch, engine = InstrumentTrack.ENGINE_SYNTH, instrument = null) }
        pushSynth(currentTrack.synth)
        if (other) {
            pushInstrument()
            pushSampled()
            pushMulti()
            pushPlaits()
            releaseUnusedFonts()
            releaseUnusedMulti()
        }
    }

    // --- Phase 35: « ● Enregistrer » in each editor, what comes after a take, the last bars played --

    private val _noteTake = MutableStateFlow<com.pocketstudio.app.record.NoteTakeUi?>(null)
    /**
     * The take of an editor's record bar: counting in, recording, then done (Garder, Remettre en
     * rythme, Refaire). Null: none (the record bar shows « ● Enregistrer »).
     */
    val noteTake: StateFlow<com.pocketstudio.app.record.NoteTakeUi?> = _noteTake.asStateFlow()

    private val _takeEmpty = MutableStateFlow(false)
    /** The last take ended with nothing played: « Rien n'a été joué » under its button, until the next one. */
    val takeEmpty: StateFlow<Boolean> = _takeEmpty.asStateFlow()

    /** The notes the last take wrote, by (scene, track), as they are now in the project. */
    private var takeNotes: MutableMap<Pair<Int, Int>, List<Note>> = HashMap()
    /** The hits the last take added, as (scene, lane, step). */
    private var takeHits: MutableList<Triple<Int, Int, Int>> = ArrayList()
    /** The notes there before that the last take replaced (the same key played on them), by (scene, track): « Refaire » gives them back. */
    private var takeReplaced: MutableMap<Pair<Int, Int>, List<Note>> = HashMap()
    /** The hits there before whose strength the last take changed, by (scene, lane, step): « Refaire » gives them back. */
    private var takeReplacedHits: MutableMap<Triple<Int, Int, Int>, com.pocketstudio.model.Hit> = HashMap()
    /** The project when the last take began (Refaire of a 303 take puts its curves back). */
    private var takeStartProject: Project? = null
    private var takeMetronomeOn = false
    private var takeAcidRecord = false
    /** Keys pressed during the take, and those it could not keep with why (the first few): its journal line. */
    private var takeKeys = 0
    private val takeDrops = ArrayList<String>()

    /**
     * What the last take heard and kept, as its journal line says (« Prise clavier : 4 touches,
     * 4 notes »; a key it could not keep, with why and where): a take that keeps nothing of keys
     * played is told apart from one where nothing was played. Null before the first take.
     */
    var lastTakeReport: String? = null
        private set
    private var takeWatch: kotlinx.coroutines.Job? = null

    /**
     * « ● Enregistrer » in an editor (also the top bar's ● outside the Audio tab): a take of
     * [source] starts (stopped: after the count-in, the shown part looping; the metronome on, as
     * Réglages says) or the one recording ends and says what it wrote. A take done and not yet
     * kept is kept first.
     */
    fun recordTake(source: com.pocketstudio.app.record.TakeSource, melody: Boolean = false) {
        if (_recording.value) {
            stopRecordingTake()
            return
        }
        if (_audioRecording.value != null || performanceTaking) return
        keepNoteTake()
        takeSpreads = melody
        _takeEmpty.value = false
        takeNotes = HashMap()
        takeHits = ArrayList()
        takeReplaced = HashMap()
        takeReplacedHits = HashMap()
        takeKeys = 0
        takeDrops.clear()
        takeStartProject = project.value
        // The metronome during the take (Réglages › Son), given back as it was at its end.
        takeMetronomeOn = !_transport.value.metronome && com.pocketstudio.app.settings.SettingsStore.state.value.metronomeInTakes
        if (takeMetronomeOn) toggleMetronome()
        takeAcidRecord = source == com.pocketstudio.app.record.TakeSource.ACID && !_automationRecord.value
        if (takeAcidRecord) _automationRecord.value = true
        toggleRecord(notes = source == com.pocketstudio.app.record.TakeSource.KEYS)
        if (!_recording.value) {
            endTakeExtras()
            return
        }
        val counting = NativeEngine.isAvailable && NativeEngine.playPosition() < 0
        _noteTake.value = com.pocketstudio.app.record.NoteTakeUi(source, if (counting) com.pocketstudio.app.record.TakePhase.COUNT_IN else com.pocketstudio.app.record.TakePhase.RECORDING, beatsLeft = if (counting) project.value.beatsPerBar else 0)
        watchTake()
    }

    /** The count-in counted down beat by beat (« 4 · 3 · 2 · 1 »), then the take records. */
    private fun watchTake() {
        takeWatch?.cancel()
        if (!NativeEngine.isAvailable) return
        takeWatch = viewModelScope.launch {
            while (_recording.value) {
                val ui = _noteTake.value ?: break
                val pos = NativeEngine.playPosition()
                val next = if (pos < 0 && _transport.value.playing) {
                    ui.copy(phase = com.pocketstudio.app.record.TakePhase.COUNT_IN, beatsLeft = kotlin.math.ceil(-pos / 4.0).toInt().coerceAtLeast(1))
                } else {
                    ui.copy(phase = com.pocketstudio.app.record.TakePhase.RECORDING, beatsLeft = 0)
                }
                if (next != ui) _noteTake.value = next
                kotlinx.coroutines.delay(40)
            }
        }
    }

    /** The metronome and the 303's recording back as they were before the take. */
    private fun endTakeExtras() {
        if (takeMetronomeOn && _transport.value.metronome) toggleMetronome()
        takeMetronomeOn = false
        if (takeAcidRecord) _automationRecord.value = false
        takeAcidRecord = false
        takeWatch?.cancel()
    }

    /** The take just ended ([stopRecordingTake]): what it wrote, and the choice that follows. */
    private fun takeEnded(before: Project?) {
        val ui = _noteTake.value ?: return
        endTakeExtras()
        val notes = takeNotes.values.sumOf { it.size }
        val moves = ui.source == com.pocketstudio.app.record.TakeSource.ACID && before != null && before.automation != project.value.automation
        val report = "Prise ${ui.source.name.lowercase()} : $takeKeys touches, $notes notes, ${takeHits.size} coups" +
            if (takeDrops.isEmpty()) "" else " (non gardé : ${takeDrops.joinToString("; ")})"
        lastTakeReport = report
        AppLog.add(report)
        if (notes == 0 && takeHits.isEmpty() && !moves) {
            // Nothing played: no card to answer, a short line where the take was (not a window).
            _noteTake.value = null
            _takeEmpty.value = true
            com.pocketstudio.app.report.Journal.action(if (takeKeys == 0) "Prise vide : rien n'a été joué" else "Prise vide : $takeKeys touches non gardées")
            return
        }
        _noteTake.value = ui.copy(phase = com.pocketstudio.app.record.TakePhase.DONE, beatsLeft = 0, notes = notes, hits = takeHits.size, moves = moves)
    }

    /** « Garder »: the take stays as it is (it already is in the project); the card goes. */
    fun keepNoteTake() {
        if (_noteTake.value?.phase == com.pocketstudio.app.record.TakePhase.DONE) _noteTake.value = null
    }

    /** The take's notes [transform]ed where they are (one undo step, [key] gathers the strengths). */
    private fun editTakeNotes(key: String?, transform: (Note, Int) -> Note) {
        val p = project.value
        val updated = HashMap<Pair<Int, Int>, List<Note>>()
        session.edit(key) { q ->
            var out = q
            takeNotes.forEach { (where, notes) ->
                val (scene, id) = where
                if (out.instrumentTracks.none { it.id == id } || scene !in out.allScenes.indices) return@forEach
                out = inScene(out, scene) { r ->
                    val t = r.track(id)
                    val ticks = t.notes.ticks
                    val mine = notes.toMutableList()
                    val next = mutableListOf<Note>()
                    val clip = t.notes.copy(notes = t.notes.notes.map { n ->
                        val i = mine.indexOf(n)
                        if (i < 0) n else transform(n, ticks).also { mine.removeAt(i); next += it }
                    })
                    updated[where] = next
                    r.withTrack(t.copy(notes = clip.sanitized()))
                }
            }
            out
        }
        if (project.value === p) return
        takeNotes = HashMap(updated)
        pushTakeNotes(updated.keys)
    }

    /**
     * « Remettre en rythme »: the take's notes onto the sixteenths by [strength] (0.5, 0.75, 1),
     * from where they were played (nothing is lost: [takeAsPlayed] brings them back). One undo step.
     */
    fun takeInTime(strength: Float = 1f) {
        val ui = _noteTake.value?.takeIf { it.phase == com.pocketstudio.app.record.TakePhase.DONE && it.canPutInTime } ?: return
        editTakeNotes("take-in-time") { n, ticks -> n.inTime(NoteClip.TICKS_PER_STEP, strength, ticks) }
        _noteTake.value = ui.copy(strength = strength, tuned = true)
    }

    /** « Comme joué »: the take's notes back where they were played. One undo step. */
    fun takeAsPlayed() {
        val ui = _noteTake.value?.takeIf { it.phase == com.pocketstudio.app.record.TakePhase.DONE } ?: return
        editTakeNotes("take-in-time") { n, _ -> n.played?.let { at -> n.copy(start = at, played = null) } ?: n }
        _noteTake.value = ui.copy(strength = null)
    }

    /**
     * « Refaire »: what the take wrote goes (one undo step) and a new take of the same kind starts
     * at once, with its count-in. « Enlever » for what « Garder ce que je viens de jouer » kept.
     */
    fun redoTake() {
        val ui = _noteTake.value?.takeIf { it.phase == com.pocketstudio.app.record.TakePhase.DONE } ?: return
        removeTake()
        _noteTake.value = null
        if (ui.source != com.pocketstudio.app.record.TakeSource.CAPTURE) recordTake(ui.source, takeSpreads)
    }

    private fun removeTake() {
        val acidBefore = takeStartProject?.takeIf { _noteTake.value?.moves == true }
        val hits = takeHits.toList()
        val replaced = takeReplaced
        val replacedHits = takeReplacedHits
        session.edit { q ->
            var out = q
            (takeNotes.keys + replaced.keys).forEach { where ->
                val (scene, id) = where
                if (out.instrumentTracks.none { it.id == id } || scene !in out.allScenes.indices) return@forEach
                out = inScene(out, scene) { r ->
                    val t = r.track(id)
                    val mine = takeNotes[where].orEmpty().toMutableList()
                    // The take's notes go; the ones it had replaced come back.
                    val kept = t.notes.notes.filter { n -> !mine.remove(n) }
                    val back = replaced[where].orEmpty().filter { it !in kept }
                    r.withTrack(t.copy(notes = t.notes.copy(notes = kept + back).sanitized()))
                }
            }
            (hits.map { it.first } + replacedHits.keys.map { it.first }).distinct().forEach { scene ->
                if (scene !in out.allScenes.indices) return@forEach
                out = inScene(out, scene) { r ->
                    var d = r.drums
                    hits.filter { it.first == scene }.forEach { (_, lane, step) -> d.lanes.getOrNull(lane)?.let { l -> d = d.copy(lanes = d.lanes.toMutableList().also { it[lane] = l.copy(hits = l.hits - step) }) } }
                    replacedHits.filterKeys { it.first == scene }.forEach { (k, hit) ->
                        d.lanes.getOrNull(k.second)?.let { l -> d = d.copy(lanes = d.lanes.toMutableList().also { it[k.second] = l.copy(hits = l.hits + (k.third to hit)) }) }
                    }
                    r.copy(drums = d)
                }
            }
            if (acidBefore != null) out = out.copy(automation = acidBefore.automation)
            out
        }
        val scenes = takeNotes.keys + replaced.keys
        takeNotes = HashMap()
        takeHits = ArrayList()
        takeReplaced = HashMap()
        takeReplacedHits = HashMap()
        pushTakeNotes(scenes)
        if ((hits.isNotEmpty() || replacedHits.isNotEmpty()) && NativeEngine.isAvailable) {
            (hits.map { it.first } + replacedHits.keys.map { it.first }).distinct().forEach { scene ->
                if (playsNow(LiveSlots.DRUMS, scene) || scene == project.value.scene) {
                    val drums = project.value.sceneView(scene).drums
                    drums.lanes.forEachIndexed { lane, l -> for (s in 0 until DrumPattern.MAX_STEPS) pushHitNow(lane, s, l.hits[s]) }
                }
            }
        }
        if (acidBefore != null) pushAutomation()
    }

    /** Notes of tracks in scenes changed by a take to the engine, where it plays them now. */
    private fun pushTakeNotes(keys: Collection<Pair<Int, Int>>) {
        val p = project.value
        keys.forEach { (scene, id) ->
            if (p.instrumentTracks.none { it.id == id } || scene !in p.allScenes.indices) return@forEach
            val clip = p.sceneView(scene).track(id).notes
            if (playsNow(LiveSlots.INST + id, scene)) pushNotesNow(clip, id) else if (scene == p.scene) pushNotes(clip, id)
        }
    }

    /** A note written by the take recording now (see [recordNoteOff]). */
    private fun noteTaken(scene: Int, id: Int, note: Note) {
        if (_noteTake.value == null) return
        val key = scene to id
        takeNotes[key] = (takeNotes[key] ?: emptyList()) + note
    }

    /** A hit added by the take recording now (see [recordPad]). */
    private fun hitTaken(scene: Int, lane: Int, step: Int) {
        if (_noteTake.value == null) return
        takeHits += Triple(scene, lane, step)
    }

    // « Garder ce que je viens de jouer »: the keys and pads played lately, recording or not.

    /** The last notes and hits played (a ring made once, see [com.pocketstudio.model.PlayedMemory]). */
    private val played = com.pocketstudio.model.PlayedMemory()
    private val _canKeepPlayed = MutableStateFlow(false)
    /** Something was played lately that « Garder ce que je viens de jouer » can keep. */
    val canKeepPlayed: StateFlow<Boolean> = _canKeepPlayed.asStateFlow()

    /** Tests: the clock the memory reads, in sixteenths (null: the transport's or the wall's). */
    internal var playedClockForTest: Double? = null
    private val wallOrigin = android.os.SystemClock.uptimeMillis()

    /** Where a key or pad is played on the memory's clock: the music's while it plays, else the wall's at the tempo. */
    private fun playedClock(): Double {
        playedClockForTest?.let { return it }
        if (_transport.value.playing && NativeEngine.isAvailable) {
            val latencyMs = Metrics.streamInfo.value.outputLatencyMs.takeIf { it > 0 } ?: 0.0
            return NativeEngine.transportPosition() - latencyMs / 1000.0 * project.value.tempoBpm / 15.0
        }
        return (android.os.SystemClock.uptimeMillis() - wallOrigin) / 1000.0 * project.value.tempoBpm / 15.0
    }

    /** The clock changes (play, stop): the memory starts counting on the new one. */
    private fun playedClockChanged(playing: Boolean) {
        played.newEpoch(playing)
        if (_canKeepPlayed.value) _canKeepPlayed.value = false
    }

    private fun playedNoteOn(id: Int, pitch: Int, velocity: Float) {
        played.noteOn(id, pitch, velocity, playedClock())
        if (!_canKeepPlayed.value && !_recording.value) _canKeepPlayed.value = true
    }

    private fun playedNoteOff(id: Int, pitch: Int) = played.noteOff(id, pitch, playedClock())

    private fun playedPad(lane: Int, velocity: Float) {
        played.pad(lane, velocity, playedClock())
        if (!_canKeepPlayed.value && !_recording.value) _canKeepPlayed.value = true
    }

    /**
     * « Garder ce que je viens de jouer »: the last [com.pocketstudio.model.PlayedMemory.BARS] bars
     * played on the keys and pads (recorded or not) go into the shown part, each note on the track
     * it was played on, as it was played (one undo step); the card then offers « Remettre en rythme ».
     */
    fun keepPlayed() {
        if (_recording.value || _audioRecording.value != null || performanceTaking) return
        keepNoteTake()
        val p = project.value
        val capture = played.capture(playedClock(), com.pocketstudio.model.PlayedMemory.BARS, p.stepsPerBar)
        if (capture.isEmpty) {
            _canKeepPlayed.value = false
            _notice.value = tr("Rien à garder : joue quelques notes d'abord.")
            return
        }
        val scene = p.scene
        val added = HashMap<Pair<Int, Int>, List<Note>>()
        val hits = ArrayList<Triple<Int, Int, Int>>()
        session.edit { q ->
            var out = q
            capture.notes.map { it.track }.distinct().forEach { id ->
                if (out.instrumentTracks.none { it.id == id }) return@forEach
                val t = out.track(id)
                val next = t.notes.withCaptured(capture, id, out.stepsPerBar) ?: return@forEach
                val left = t.notes.notes.toMutableList()
                added[scene to id] = next.notes.filter { n -> !left.remove(n) }
                out = out.withTrack(t.copy(notes = next))
            }
            out.drums.withCaptured(capture, out.stepsPerBar)?.let { d ->
                d.lanes.forEachIndexed { lane, l -> l.hits.keys.filter { it !in out.drums.lanes[lane].hits }.forEach { hits += Triple(scene, lane, it) } }
                out = out.copy(drums = d)
            }
            out
        }
        played.clear()
        _canKeepPlayed.value = false
        takeNotes = added
        takeHits = hits
        takeReplaced = HashMap()
        takeReplacedHits = HashMap()
        takeStartProject = p
        pushTakeNotes(added.keys)
        if (hits.isNotEmpty() && NativeEngine.isAvailable) pushDrumPattern(project.value.drums)
        AppLog.add("Garder ce que je viens de jouer : ${added.values.sumOf { it.size }} notes, ${hits.size} coups")
        _noteTake.value = com.pocketstudio.app.record.NoteTakeUi(
            com.pocketstudio.app.record.TakeSource.CAPTURE, com.pocketstudio.app.record.TakePhase.DONE,
            notes = added.values.sumOf { it.size }, hits = hits.size,
        )
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

    /**
     * ▶ / ■, the same on every tab: plays the song from [playFrom] once there is one, unless
     * « ⟳ Partie » loops the scene shown ([listensToSong]); without a song, the scene shown.
     */
    fun togglePlay() {
        if (_transport.value.playing) toggleTransport(songStep = aloneSongStep()) else startPlaying(null)
    }

    /** ▶ from where it starts: the song from its part ([playFromNow]), else the scene on its own; [countInBars] null: the switch. */
    private fun startPlaying(countInBars: Int?) {
        if (listensToSong) {
            val from = playFromNow()
            playStartsForTest?.add("song $from")
            if (NativeEngine.isAvailable) {
                playSongFrom(from, countInBars)
                return
            }
        }
        if (!listensToSong) playStartsForTest?.add("part ${project.value.scene}")
        heardScene = project.value.scene
        // The scene on its own: song curves hold the value they have where it first plays.
        if (NativeEngine.isAvailable) applySongCurves(project.value, project.value.scene)
        toggleTransport(countInBars, songStep = aloneSongStep())
    }

    /** Tests: the count-in (bars) the last start played, engine or not. */
    internal var countInForTest: Int = -1

    /**
     * A MIDI Start ([fromTop]) or Continue from the machine whose clock is followed: never a count-in
     * (it is already on its first beat). Start plays from the top, the song's first part (while
     * playing too: both machines line up again on the 1); Continue carries on from the part it stopped in.
     */
    fun midiStart(fromTop: Boolean) {
        if (_transport.value.playing) {
            if (!fromTop) return
            toggleTransport(songStep = aloneSongStep())
        }
        if (fromTop) _playFrom.value = 0
        startPlaying(countInBars = 0)
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
        if (!playing) {
            _loopArmedBySelf.value = false
            // A take ends before the transport does: its last launches are read while they still count.
            endPerformanceOnStop()
            // Stopped during the song: ▶ goes on from the part it stopped in (the song's end sets the start again).
            _sessionUi.value.songIndex.takeIf { it >= 0 && _sessionUi.value.removed == null }?.let { _playFrom.value = it }
            _playBar.value = -1
        }
        _transport.value = t.copy(playing = playing)
        playedClockChanged(playing)
        beforeSwap = null
        // The live grid starts from the scene heard (a song part: as it plays it, each track from
        // where it carries on); Stop ends what the Live page did.
        if (playing) {
            val part = _sessionUi.value.songIndex.takeIf { it >= 0 }?.let { project.value.song.getOrNull(it) }
            _liveGrid.value = gridOfPart(part, heardScene ?: project.value.scene, 0, null)
            countInForTest = countInBars ?: if (t.countIn) 1 else 0
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

    /** The engine must keep running in the background only while the music plays (▶, or live pads on their own). */
    val keepsRunningInBackground: Boolean get() = _transport.value.playing || _padsAlone.value

    init {
        viewModelScope.launch { project.collect { restageIfEditingUpcoming(); loopOnEdit(it) } }
        // A project with more long effects than the engine holds (made elsewhere, or by hand): said at once.
        if (_notice.value == null) warnLongFxOverLimit()
        loadPadSamples()
        if (project.value.instrument != null) openLibrary()
        loadTrackFonts()
        if (NativeEngine.isAvailable) NativeEngine.liveTrack = 0
        // The engine outlives a project: its loudness target forgets the last one's music.
        if (NativeEngine.isAvailable) NativeEngine.resetLoudnessEstimate()
        // S6 (G5): the live gain this song's export will use, found before ▶ (in the background).
        estimateLiveDrive()
        watchSoundForLiveDrive()
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
        playedPad(lane, 0.9f)
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
    fun setPadPan(pan: Float) = setPadPanOf(_transport.value.selectedPad, pan)

    /** The selected pad's pan gesture ended (its journal line). */
    fun padPanDone() = padPanDoneOf(_transport.value.selectedPad)

    /** Chooses pad [lane] without sounding it (its sheet opens on it, an appui long). */
    fun selectPad(lane: Int) {
        if (lane !in 0 until DrumPattern.PAD_COUNT) return
        if (lane != _transport.value.selectedPad) closeSlicer()
        _transport.value = _transport.value.copy(selectedPad = lane)
    }

    /** Pad [lane]'s own volume (its strip's fader, 0.8 = as the kit plays it); a drag is one undo step. */
    fun setPadVolume(lane: Int, volume: Float) {
        if (lane !in 0 until DrumPattern.PAD_COUNT) return
        editChannel(Mixer.padChannel(lane), "vol") { it.copy(volume = volume.coerceIn(0f, 1f)) }
    }

    /** Pad [lane]'s place in the stereo field, -1..1 (a drag is one undo step). */
    fun setPadPanOf(lane: Int, pan: Float) {
        if (lane !in 0 until DrumPattern.PAD_COUNT) return
        if (padPanFrom?.first != lane) padPanFrom = lane to project.value.drums.lanes[lane].pan
        session.edit("pad-pan-$lane") { it.copy(drums = it.drums.withPadPan(lane, pan)) }
        if (NativeEngine.isAvailable) NativeEngine.setPadPan(lane, project.value.drums.lanes[lane].pan)
    }

    /** Pad and pan when its pan gesture started: one journal line per gesture, from what to what. */
    private var padPanFrom: Pair<Int, Float>? = null

    /**
     * Pad [lane]'s pan gesture ended: one line in the bug report's journal when it moved
     * (« Pan du pad 1 · Kick : centre → G 100 »), so a pan nobody meant to touch shows there.
     */
    fun padPanDoneOf(lane: Int) {
        val from = padPanFrom?.takeIf { it.first == lane }?.second ?: return
        padPanFrom = null
        val to = project.value.drums.lanes.getOrNull(lane)?.pan ?: return
        if (from != to) com.pocketstudio.app.report.Journal.action("Pan du pad ${lane + 1} · ${padName(project.value, lane)} : ${journalPan(from)} → ${journalPan(to)}")
    }

    /** Pad [lane] silent or heard again (« Couper ce pad »); its hits stay. */
    fun togglePadMute(lane: Int) {
        if (lane !in 0 until DrumPattern.PAD_COUNT) return
        editChannel(Mixer.padChannel(lane), null) { it.copy(mute = !it.mute) }
    }

    /** Pad [lane] back to the kit's own voice (« Son d'origine »). */
    fun removeSampleOf(lane: Int) {
        if (project.value.drums.lanes.getOrNull(lane)?.sample == null) return
        session.edit { p -> p.copy(drums = p.drums.withSample(lane, null)) }
        pushPads()
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
        pushTempo()
    }

    /** An exact tempo from outside (MIDI clock): one undo step while it keeps moving. */
    fun setTempo(bpm: Double) {
        session.edit("tempo") { it.copy(tempoBpm = bpm) }
        pushTempo()
    }

    /**
     * The tempo of the MIDI clock followed: not an undo step of its own (« Annuler » never steps
     * through the clock's small moves), saved with the project.
     */
    fun followClockTempo(bpm: Double) {
        if (bpm == project.value.tempoBpm) return
        session.editQuietly { it.copy(tempoBpm = bpm) }
        pushTempo()
    }

    fun tapTempo() {
        val bpm = tapTempo.tap(System.currentTimeMillis()) ?: return
        session.edit("tempo") { it.copy(tempoBpm = Math.round(bpm).toDouble()) }
        pushTempo()
    }

    /** The project's tempo to the engine, and to what follows it (the remix, pads fitted to beats). */
    private fun pushTempo() {
        if (NativeEngine.isAvailable) NativeEngine.setTempo(project.value.tempoBpm)
        if (project.value.remix.followTempo) pushRemix()
        if (project.value.drums.lanes.any { (it.sample?.fitBeats ?: 0) > 0 }) pushPads()
    }

    /** The undo depth right after the tempo sheet's first change (-1: none yet): its next changes join that step. */
    private var tempoSheetDepth = -1

    /** The tempo sheet opens: whatever it changes until it closes is one undo step. */
    fun beginTempoSheet() {
        tempoSheetDepth = -1
    }

    /**
     * A tempo chosen in the tempo sheet (typed, ready-made, ½, ×2, tapped), heard at once: a whole
     * BPM within the tempos the app plays. The sheet's first change makes the undo step, the next
     * ones join it (unless something else made a step in between).
     */
    fun setSheetTempo(bpm: Double) {
        if (!bpm.isFinite()) return
        val clean = Math.round(bpm).toDouble().coerceIn(Project.MIN_TEMPO, Project.MAX_TEMPO)
        if (clean == project.value.tempoBpm) return
        if (tempoSheetDepth >= 0 && tempoSheetDepth == session.undoDepth && !session.canRedo.value) {
            session.editQuietly { it.copy(tempoBpm = clean) }
        } else {
            session.edit { it.copy(tempoBpm = clean) }
            tempoSheetDepth = session.undoDepth
        }
        pushTempo()
    }

    /** « Taper » in the tempo sheet: the tempo of the taps so far (from the second), in the sheet's undo step. */
    fun tapSheetTempo() {
        tapTempo.tap(System.currentTimeMillis())?.let(::setSheetTempo)
    }

    fun setSwing(swing: Double) {
        session.edit("swing") { it.copy(swing = swing) }
        if (NativeEngine.isAvailable) NativeEngine.setSwing(project.value.swing)
    }

    fun undo() {
        // A take ends first: a live one then undoes in one step; a mic take keeps its track.
        if (_recording.value || _audioRecording.value != null || _takeSaving.value || performanceTaking) return
        padPanFrom = null  // a pan gesture's start belongs to the project before the undo
        keepNoteTake()  // the card's notes may be gone
        val heard = sceneHeard()
        val before = project.value
        // A scene move undone: the scene playing goes on from its old place (not the one now there).
        val move = session.undo() as? SceneMove
        move?.let { followSceneMove(it::back) }
        keepListenChoice()
        keepLiveSettings(before)
        followPlaybackAfterHistory(if (move != null) heard?.let(move::back) else heard, before.song)
        warnLongFxOverLimit(before)
        loadTrackFonts()
        reloadMissingSamples()
    }

    fun redo() {
        if (_recording.value || _audioRecording.value != null || _takeSaving.value || performanceTaking) return
        padPanFrom = null
        keepNoteTake()
        val heard = sceneHeard()
        val before = project.value
        val move = session.redo() as? SceneMove
        move?.let { followSceneMove(it::forward) }
        keepListenChoice()
        keepLiveSettings(before)
        followPlaybackAfterHistory(if (move != null) heard?.let(move::forward) else heard, before.song)
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
    private fun followPlaybackAfterHistory(heard: Int?, songBefore: List<SongPart>? = null) {
        if (heard == null) {
            syncEngine()
            return
        }
        val ui = _sessionUi.value
        // Staying on a scene during the song: the screens keep the scene the step brought back.
        if (screensFollow(ui) && project.value.scene != heard && heard in project.value.allScenes.indices) session.showQuietly { it.switchScene(heard) }
        if (ui.songIndex >= 0) {
            val index = followSongIndex(project.value.song, ui.songIndex, heard)
            if (index < 0) {
                endSongFollow()
                syncEngine()
                return
            }
            // The part chosen to come next (« ensuite ») stays chosen when the step left it where it
            // was (a part added, removed or changed by the step: the usual order again).
            val song = project.value.song
            val chosen = songBefore?.getOrNull(ui.queued)?.takeIf { ui.removed == null && songBefore.size == song.size }
            val queued = if (chosen != null && song[ui.queued] == chosen) ui.queued else -1
            _sessionUi.value = SessionUi(songIndex = index, queued = queued)
            syncEngine()  // the part on now, with its muted tracks
            if (NativeEngine.isAvailable) stageSongPart()
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
        pushHitNow(lane, step, hit)
    }

    private fun pushHitNow(lane: Int, step: Int, hit: Hit?) {
        NativeEngine.setDrumHit(lane, step, hit?.velocity ?: 0f, hit?.repeats ?: 1, hit?.ramp ?: 0)
        if (hit == null) return
        // Only hits with their own pan or pitch, timing, 808 slide or chance need more (the hit command resets them).
        if (!hit.isPlain) NativeEngine.setDrumStepDetail(lane, step, hit.pan, hit.pitch)
        if (hit.offset != 0f) NativeEngine.setDrumStepOffset(lane, step, hit.offset)
        if (hit.glide > 0) NativeEngine.setDrumStepGlide(lane, step, hit.glide.toFloat())
        // A hit with a chance (phase 37): the hit command made it « always ».
        if (hit.chance < 100) NativeEngine.setDrumStepChance(lane, step, hit.chance / 100f)
    }

    /** The studio voices last sent to the live engine (null: unknown, everything is sent). */
    private var pushedVoices: Map<Int, PadVoice>? = null

    /**
     * The pads' studio voices ([Project.padVoices]), after the kit's version (which resets the kit's
     * own kicks): a pad that changes voice goes back to its kit voice first, so that its new voice
     * starts from its defaults, then takes its settings. An export's engine gets all of them.
     */
    private fun pushPadVoices(p: Project) {
        val capturing = NativeEngine.isCapturing
        val before = if (capturing) null else pushedVoices
        if (before == p.padVoices) return
        for (lane in 0 until DrumPattern.PAD_COUNT) {
            val voice = p.padVoices[lane]
            if (before != null && before[lane] == voice) continue
            NativeEngine.setPadVoice(lane, PadVoice.KIT)
            if (voice == null) continue
            NativeEngine.setPadVoice(lane, voice.kind)
            voice.params.toSortedMap().forEach { (param, value) -> NativeEngine.setPadVoiceParam(lane, param, value) }
        }
        if (!capturing) pushedVoices = p.padVoices
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

    private var paintBefore: Project? = null
    private var paintDepth = 0

    /**
     * A finger drawn across the steps: [step] gets a hit ([on]) or loses it on the selected pad.
     * The stroke is one undo step ([paintEnd]); each cell is heard at once.
     */
    fun paintStep(step: Int, on: Boolean) {
        val lane = _transport.value.selectedPad
        if (paintBefore == null) {
            paintBefore = project.value
            paintDepth = session.undoDepth
        }
        val before = project.value
        session.editQuietly { it.copy(drums = it.drums.setHit(lane, step, on)) }
        if (project.value != before && NativeEngine.isAvailable) pushHit(lane, step, project.value.drums.lanes[lane].hits[step])
    }

    /** The finger lifted: the stroke becomes one undo step. */
    fun paintEnd() {
        val before = paintBefore ?: return
        paintBefore = null
        if (project.value != before) session.commitQuietEdits(before, paintDepth)
    }

    /** A hit's roll on the selected pad: [repeats] hits in its step (×1 to ×8), [ramp] 1 rising, -1 falling. */
    fun setRoll(step: Int, repeats: Int, ramp: Int) {
        val lane = _transport.value.selectedPad
        if (project.value.drums.lanes[lane].hits[step] == null) return
        session.edit { it.copy(drums = it.drums.withRoll(lane, step, repeats, ramp)) }
        if (NativeEngine.isAvailable) pushHit(lane, step, project.value.drums.lanes[lane].hits[step])
    }

    /** 32nds over the beat of [step] on the selected pad ([ramp] as in [setRoll]): one undo step. */
    fun burstAt(step: Int, ramp: Int) {
        val lane = _transport.value.selectedPad
        session.edit { it.copy(drums = it.drums.burst(lane, step, ramp)) }
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
        if (!waitsFor(LiveSlots.DRUMS)) NativeEngine.setDrumLaneSteps(lane, project.value.drums.lanes[lane].length)
    }

    /**
     * Phase 37: the selected pad loops over [length] steps of its own (a polyrhythm: 3 against 4),
     * 0 = the pattern's loop. One undo step (taps of − / + in a row merge).
     */
    fun setPadLength(length: Int) {
        val lane = _transport.value.selectedPad
        session.edit("pad-length-$lane") { it.copy(drums = it.drums.withLaneLength(lane, length)) }
        pushLane(lane)
    }

    /** Phase 37: the chance (5..100 %) the hit on [step] of the selected pad plays on each pass. */
    fun setHitChance(step: Int, chance: Int) {
        val lane = _transport.value.selectedPad
        if (project.value.drums.lanes[lane].hits[step] == null) return
        session.edit { it.copy(drums = it.drums.withHitChance(lane, step, chance)) }
        if (NativeEngine.isAvailable) pushHit(lane, step, project.value.drums.lanes[lane].hits[step])
    }

    /** « Autre tirage »: the same chances, drawn again (another seed, the same in the export). One undo step. */
    fun rerollChances() {
        session.edit { it.copy(drumSeed = com.pocketstudio.model.DrumChance.next(it.drumSeed)) }
        if (NativeEngine.isAvailable) NativeEngine.setDrumSeed(project.value.drumSeed)
    }

    // --- Phase 37: « Écrire comme un pro » ------------------------------------------------------

    /**
     * « Accords du morceau » being listened to before « Appliquer »: the chords tried (scene index to
     * one chord a bar), the follow choices made meanwhile, and the progression picked (if any).
     */
    data class ChordTry(val chords: Map<Int, List<Int>>, val follows: Map<Int, Boolean> = emptyMap(), val progression: Progression? = null)

    private val _chordTry = MutableStateFlow<ChordTry?>(null)
    /** The chords being tried (heard, not yet applied), null: none. */
    val chordTry: StateFlow<ChordTry?> = _chordTry.asStateFlow()
    /** The project before the chords tried, and the undo depth then (« Appliquer » makes one step of it). */
    private var chordBefore: Project? = null
    private var chordDepth = 0
    /** The project as the last try left it: while it is still exactly that, a save writes [chordBefore]. */
    private var chordCandidate: Project? = null

    /**
     * The chords tried are heard, never saved on their own (the app killed in the background, the
     * screen closed): the disk keeps the song as it was before the tries until « Appliquer ». An
     * edit made meanwhile (a take, a knob) is saved as it is, the chords tried with it.
     */
    private fun holdChordTrial(on: Boolean) {
        session.saveAs = if (!on) null else { p -> chordBefore?.takeIf { p === chordCandidate } ?: p }
    }

    /** The project the chords are tried on (the one before, or the current when nothing is tried). */
    val chordBase: Project get() = chordBefore ?: project.value

    /** The tracks the chords move: those that follow, but the frozen ones (their notes wait). */
    private fun chordFollowers(p: Project): Set<Int> =
        com.pocketstudio.model.ChordTrack.followers(p) - com.pocketstudio.model.Freeze.frozenIds(p)

    /**
     * Tries [next] (heard at once, the music started if it was stopped): a quiet edit from the
     * project as it was before the first try, so tries never pile up and « Annuler » gives it back.
     * A setting changed between two tries (a knob by MIDI) goes back with the next try: the sheet
     * is a moment of listening, its tries start from the song as it was (« Appliquer » keeps them).
     */
    private fun tryChords(next: ChordTry) {
        if (_recording.value || _audioRecording.value != null || _takeSaving.value || performanceTaking) {
            _notice.value = tr("Arrête d'abord l'enregistrement pour changer les accords.")
            return
        }
        val before = chordBefore ?: project.value.also {
            chordBefore = it
            chordDepth = session.undoDepth
        }
        val heard = sceneHeard()
        val songBefore = project.value.song
        val base = next.follows.entries.fold(before) { q, (id, on) -> com.pocketstudio.model.ChordTrack.withFollow(q, id, on) }
        val candidate = com.pocketstudio.model.ChordTrack.withChordsOf(base, next.chords, chordFollowers(base))
        _chordTry.value = next
        session.editQuietly { candidate }
        chordCandidate = project.value
        holdChordTrial(true)
        followPlaybackAfterHistory(heard, songBefore)
        if (NativeEngine.isAvailable && !_transport.value.playing) togglePlay()
    }

    /** « Accords du morceau »: the whole song on [progression], heard before « Appliquer ». */
    fun tryProgression(progression: Progression) {
        val p = chordBase
        val degrees = com.pocketstudio.model.ChordTrack.degreesOf(p, progression)
        tryChords(ChordTry(com.pocketstudio.model.ChordTrack.songScenes(p).associateWith { degrees }, _chordTry.value?.follows.orEmpty(), progression))
    }

    /** One bar's chord of scene [scene] changed to [degree] (the other chords tried stay). */
    fun tryChord(scene: Int, bar: Int, degree: Int) {
        val p = chordBase
        val current = _chordTry.value
        val chords = current?.chords?.get(scene) ?: com.pocketstudio.model.ChordTrack.chordsOf(p, scene)
        val bars = com.pocketstudio.model.ChordTrack.bars(p, scene)
        if (bar !in 0 until bars || chords.isEmpty()) return
        val full = List(bars) { chords[it % chords.size] }.toMutableList().also { it[bar] = Math.floorMod(degree, 7) }
        tryChords(ChordTry(current?.chords.orEmpty() + (scene to full), current?.follows.orEmpty(), null))
    }

    /** Track [id] follows the song's chords ([on]) or keeps its notes: tried with the chords when some are, else one undo step. */
    fun setFollowsChords(id: Int, on: Boolean) {
        val current = _chordTry.value
        if (current != null) {
            tryChords(current.copy(follows = current.follows + (id to on)))
        } else {
            session.edit { com.pocketstudio.model.ChordTrack.withFollow(it, id, on) }
        }
    }

    /** « Appliquer à tout le morceau »: the chords tried become one undo step (« Fait : … Annuler »). */
    fun applyChords() {
        val before = chordBefore ?: return
        val tried = _chordTry.value
        chordBefore = null
        chordCandidate = null
        holdChordTrial(false)
        _chordTry.value = null
        if (project.value == before) return
        session.commitQuietEdits(before, chordDepth)
        val p = project.value
        val english = com.pocketstudio.app.settings.SettingsStore.state.value.englishNotes
        val names = tried?.progression?.let { pr -> com.pocketstudio.model.ChordTrack.degreesOf(p, pr).joinToString(" ") { com.pocketstudio.model.ChordTrack.name(p, it, english) } }
        _done.value = if (names != null) tr("Fait : accords du morceau %s", names) else tr("Fait : accords du morceau changés")
        AppLog.add("Accords du morceau : ${tried?.progression ?: tried?.chords}")
    }

    /** « Annuler » in the chords' sheet: the song as it was before the tries. */
    fun cancelChords() {
        val before = chordBefore ?: return
        val heard = sceneHeard()
        val songBefore = project.value.song
        chordBefore = null
        chordCandidate = null
        holdChordTrial(false)
        _chordTry.value = null
        session.editQuietly { before }
        followPlaybackAfterHistory(heard, songBefore)
    }

    private val _noteClipboard = MutableStateFlow<com.pocketstudio.model.NoteClipboard?>(null)
    /** Notes copied in a piano roll: kept here, so they paste into any track and any part. */
    val noteClipboard: StateFlow<com.pocketstudio.model.NoteClipboard?> = _noteClipboard.asStateFlow()

    /** Copies [board] (said as coming from the track and part shown). */
    fun copyNotes(board: com.pocketstudio.model.NoteClipboard) {
        val p = project.value
        _noteClipboard.value = board.copy(from = "${com.pocketstudio.model.DefaultNames.shown(p.track(track).name)} · ${p.sceneTitle(p.scene)}")
    }

    private val _freezing = MutableStateFlow<Int?>(null)
    /** The instrument track being frozen (rendered), null: none. */
    val freezing: StateFlow<Int?> = _freezing.asStateFlow()

    /**
     * « Geler la piste »: track [id] rendered offline over the whole song (its strip alone: its
     * effects, sends and curves, not the master's) onto a new audio track; its notes stay, silent in
     * the song (the CPU of its sound is saved). One undo step; « Dégeler » brings it back.
     */
    fun freezeTrack(id: Int) {
        val p = project.value
        if (com.pocketstudio.model.Freeze.isFrozen(p, id) || p.instrumentTracks.none { it.id == id } || _freezing.value != null) return
        if (p.song.isEmpty()) {
            _notice.value = tr("Pour geler une piste, il faut un morceau : ajoute tes parties dans Structure.")
            return
        }
        if (p.audioTracks.size >= AudioTrack.MAX) {
            _notice.value = tr("Les huit pistes audio sont prises : libères-en une pour geler cette piste.")
            return
        }
        val frames = songFrames(p)
        val seconds = frames.toDouble() / OfflineExport.SAMPLE_RATE
        if (!com.pocketstudio.model.Freeze.fits(seconds)) {
            _notice.value = tr("Morceau trop long pour geler une piste (%d minutes au plus).", com.pocketstudio.model.Freeze.MAX_SECONDS / 60)
            return
        }
        // Past two minutes, rendered straight to mono (the stereo never held in full: memory).
        val mono = com.pocketstudio.model.Freeze.mono(seconds)
        if (!NativeEngine.isAvailable) return
        val name = "❄ " + com.pocketstudio.model.DefaultNames.shown(p.track(id).name)
        // The whole mix's volume (fades, the silence before a Drop) is the master's: it stays on the mix.
        val rendered = p.copy(automation = p.automation.filterNot { it.target.isMasterVolume })
        _freezing.value = id
        audioJob(tr("Gel de la piste…"), always = true) {
            try {
                // Its multizone instrument still being read: the frozen sound would be the synth stand-in.
                awaitSoundsLoaded()
                val job = finishOrRelease(Dispatchers.IO, OfflineExport::close) { OfflineExport() }!!
                val data = try {
                    describe(job, rendered, song = true, soloChannel = InstrumentTrack.mixerChannel(id), limited = false, masterFx = false)
                    withContext(Dispatchers.Default) {
                        if (mono) job.renderToArrayMono(frames, OfflineExport.SAMPLE_RATE.toLong()) else job.renderToArray(frames, OfflineExport.SAMPLE_RATE.toLong())
                    }
                } finally {
                    withContext(NonCancellable + Dispatchers.Main) { job.close() }
                }
                if (data.isEmpty()) throw UserMessage(tr("Le rendu a été interrompu."))
                // Like an audio track's file: mono past two minutes (half the memory).
                val pcm = Pcm(data, if (mono) 1 else 2, OfflineExport.SAMPLE_RATE)
                val (sample, prepared) = withContext(Dispatchers.IO) { padSamples.add(pcm, name) }
                padSamples.register(prepared)
                val heard = sceneHeard()
                val songBefore = project.value.song
                val signature = com.pocketstudio.model.Freeze.signature(p, id)
                session.edit { q ->
                    com.pocketstudio.model.Freeze.frozen(q, id, sample.file, pcm.seconds, name)?.let { r ->
                        r.withAudioTrack(r.audioTracks.first { it.frozenFrom == id }.copy(frozenSignature = signature))
                    } ?: q
                }
                pushMixer()
                followPlaybackAfterHistory(heard, songBefore)
                if (com.pocketstudio.model.Freeze.isFrozen(project.value, id)) {
                    _done.value = tr("Fait : piste gelée (%s)", name)
                    AppLog.add("Piste gelée : $name, ${"%.1f".format(pcm.seconds)} s")
                }
            } finally {
                _freezing.value = null
            }
        }
    }

    /** « Dégeler »: the frozen sound's audio track goes, the notes play again. One undo step. */
    fun unfreezeTrack(id: Int) {
        val p = project.value
        if (!com.pocketstudio.model.Freeze.isFrozen(p, id)) return
        val heard = sceneHeard()
        session.edit { com.pocketstudio.model.Freeze.unfrozen(it, id) }
        pushMixer()
        followPlaybackAfterHistory(heard, p.song)
        _done.value = tr("Fait : piste dégelée (%s)", com.pocketstudio.model.DefaultNames.shown(p.track(id).name))
    }

    private val _automateTarget = MutableStateFlow<AutoTarget?>(null)
    /**
     * The control whose lane « Automatiser » opened from a knob (shown over the screen), null: none.
     * Its target, not its index: an undo or a lane removed meanwhile never shows another lane.
     */
    val automateTarget: StateFlow<AutoTarget?> = _automateTarget.asStateFlow()

    /**
     * « Automatiser » on a knob (appui long): its lane, made when there is none (along the song with
     * a point per beat, a quarter of a bar in 4/4, when there is a song; else on the loop), then
     * opened. A lane already there is opened as it is.
     */
    fun automateKnob(target: AutoTarget) {
        if (!target.isValid) return
        val index = project.value.automation.indexOfFirst { it.target == target }
        if (index < 0) {
            if (project.value.automation.size >= AutomationLane.MAX_LANES) {
                _notice.value = tr("%d réglages bougent déjà : retires-en un dans Mix pour en automatiser un autre.", AutomationLane.MAX_LANES)
                return
            }
            val song = project.value.song.isNotEmpty()
            val lane = if (song) AutomationLane(target, mode = AutomationLane.SONG, songPerBeat = true) else AutomationLane(target)
            session.edit { p -> p.copy(automation = p.automation + lane) }
            pushAutomation()
        }
        _automateTarget.value = target
    }

    fun closeAutomateLane() {
        _automateTarget.value = null
    }

    /** A song lane's points: one a bar, or one a beat (a quarter of a bar in 4/4). One undo step. */
    fun setLaneResolution(index: Int, perBeat: Boolean) {
        val lane = project.value.automation.getOrNull(index) ?: return
        if (lane.songPerBeat == perBeat) return
        session.edit { p ->
            p.copy(automation = p.automation.mapIndexed { i, l ->
                if (i != index) l else if (perBeat) l.perBeat(p.beatsPerBar) else com.pocketstudio.model.perBar(l, p.beatsPerBar)
            })
        }
        if (NativeEngine.isAvailable) project.value.automation.getOrNull(index)?.let { pushSongCurve(project.value, index, it) }
    }

    // --- Export ----------------------------------------------------------------------------

    private val _export = MutableStateFlow(ExportUi())
    /** The export settings, « La scène » as long as the current scene unless a length was picked. */
    val export: StateFlow<ExportUi> = combine(_export, project, remixSeconds) { e, p, _ -> e.forProject(sceneBars(p), p.song.isNotEmpty(), exportEnd(p)) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ExportUi().forProject(sceneBars(project.value), project.value.song.isNotEmpty()))
    private var running: OfflineExport? = null

    private fun sceneBars(p: Project) = (p.currentSceneContent().stepsFor(p.stepsPerBar) / p.stepsPerBar).coerceIn(1, MAX_EXPORT_BARS)

    /** Where the audio clips and the remix's song end (its length from the stems loaded when the project does not know it). */
    private fun exportEnd(p: Project): Int =
        p.exportEndBars(p.remix.seconds.takeIf { it > 0 } ?: remixSeconds.value.takeIf { stemsId == p.remix.setId } ?: 0.0)

    fun setExportBars(bars: Int) {
        val max = maxOf(MAX_EXPORT_BARS, exportEnd(project.value).coerceAtMost(MAX_LONG_EXPORT_BARS))
        _export.value = _export.value.copy(bars = bars.coerceIn(1, max), barsChosen = true)
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

    /** « 44,1 kHz » for the next exports (48 kHz otherwise). */
    fun setExportCdRate(on: Boolean) {
        _export.value = _export.value.copy(cdRate = on)
    }

    /** « 16 bits » for the next WAV and FLAC exports (24 otherwise). */
    fun setExportBits16(on: Boolean) {
        _export.value = _export.value.copy(bits16 = on)
    }

    /** The master's « Cible » (saved with the project): the engine brings the mix to its loudness. */
    fun setLoudnessTarget(target: com.pocketstudio.model.LoudnessTarget) {
        session.edit("mix-loudness-target") { p -> p.copy(mixer = p.mixer.copy(target = target)) }
        if (NativeEngine.isAvailable) NativeEngine.setLoudnessTarget(loudnessLufs(project.value))
    }

    private fun loudnessLufs(p: Project): Float? = com.pocketstudio.model.LoudnessTargets.lufs(p.mixer.target, p.style, p.mixVersion)

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
     * « Partager mon morceau »: the whole song (or the scene looped when there is none) in M4A
     * (or WAV, as « Prêt à partager ? » chose), with a short fade, then the share sheet. The
     * export card's own choices stay as they were.
     */
    fun shareSong(format: ExportFormat = ExportFormat.M4a) {
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
                // « Annuler » touched while the take was written (lot 1: it always works).
                if (exportCancelled) {
                    exportCancelled = false
                    _export.value = _export.value.copy(sharing = false)
                    return@launch
                }
            }
            val now = project.value
            startExport(com.pocketstudio.app.export.ShareSong.settings(now.song.isNotEmpty(), sceneBars(now), exportEnd(now), format), share = true)
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
            // A track alone is not brought to the song's loudness (it would be as loud as the mix).
            if (soloChannel != null) NativeEngine.setLoudnessTarget(null)
            // The Live page's own sounds (riser, build-up noise): only in the « Effets live » file.
            if (soloChannel != null) NativeEngine.setLiveNoises(if (soloChannel == LIVE_FX_STEM) NativeEngine.LIVE_NOISES_ONLY else NativeEngine.LIVE_NOISES_OFF)
            if (soloChannel != null && soloChannel != LIVE_FX_STEM) {
                // A stem is its strip alone, whatever solos and mutes the mix was left with.
                for (ch in Mixer.TRACK_STRIPS) if (ch != MASTER) NativeEngine.setChannelSolo(ch, false)
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
     * The song bench (app test `SongBenchExport`, with a recording [NativeEngine.sink]): the song
     * as « Exporter › Morceau » renders it with the card's first settings, once the sounds are in
     * (same waits, lengths and calls as [startExport]) into [file].
     */
    internal suspend fun renderSongForBench(job: OfflineExport, file: File): ExportResult {
        awaitSoundsLoaded()
        val p = project.value
        val settings = ExportUi().forProject(sceneBars(p), p.song.isNotEmpty()).copy(what = ExportWhat.Song)
        val frames = songFrames(p)
        val fadeFrames = settings.fadeFrames(frames)
        val tailFrames = if (fadeFrames > 0) 0L else MAX_TAIL_FRAMES
        describe(job, p, song = true, soloChannel = null, limited = settings.limited, masterFx = settings.masterFx)
        return job.render(file, frames, tailFrames, fadeFrames, autoTail = true, floatWav = !settings.limited)
    }

    /**
     * Waits (15 s at most) until the project's sounds are in: pad sounds, General MIDI banks,
     * multizone instruments still being read, the remix's stems and stretches. An offline render
     * started before would have the kit and the synth stand-in in their place.
     */
    private suspend fun awaitSoundsLoaded() {
        samplesLoad?.join()
        // The instruments the project plays (its tracks' and their layers') asked for first: a render
        // started before anything looked for them (the project just opened, the stream not started
        // yet) would find none being read and go ahead with the synth stand-in.
        loadTrackFonts()
        loadMultiBanks()
        var waited = 0
        while ((loadingBanks.isNotEmpty() || loadingMulti.isNotEmpty() || stemsJob?.isActive == true || remixStretchJob?.isActive == true || stretchJob?.isActive == true) &&
            waited++ < 300
        ) kotlinx.coroutines.delay(50)
    }

    /** Tests and the live bench: waits as an export does until the project's sounds are in. */
    @androidx.annotation.VisibleForTesting
    internal suspend fun awaitSoundsLoadedForTest() = awaitSoundsLoaded()

    // --- S6 son (G5): « ce que j'entends = ce que j'envoie » -----------------------------------
    // The live loudness gain used to start at 0 dB and rise for 11 to 32 s (an Intro 6.5 LU low, a
    // first Drop 1.4 LU under the export). For an assistant's song whose target is on its Drop (mix
    // version 2 and later), the drive is found offline as the export finds it (FirstListen.loudPart:
    // the fullest Drop rendered on its own by a private engine, its loud part brought to the target
    // within the chain's 3 LU) and given to the live engine before ▶ (NativeEngine.setKnownDrive: it
    // glides there and is settled, coming down only on a loss past the cap). Projects aimed at the
    // whole song (mix version 1) keep the live estimate as before.
    // (its state is declared next to [projectId]: the init blocks start it)

    private fun liveDriveWanted(p: Project) = p.mixVersion >= Project.MIX_V2 && p.song.isNotEmpty() && p.style != null

    /**
     * The song's shape and sound as the estimate heard it: another song made here, or (REVIEW S6)
     * another sound or mix on its tracks, estimates again.
     */
    private fun liveDriveKeyOf(p: Project): Int = java.util.Objects.hash(p.song, p.style, p.tempoBpm, p.mixVersion, p.allScenes.size, liveSoundKeyOf(p))

    /**
     * What the tracks sound like: their sounds and layers, each strip's effects, sends and quick
     * controls, the master's effects and buses, the 303's sound. Not the faders, pans, mutes nor
     * solos (son 8 review: soloing the hi-hat while playing must not estimate the drive on it).
     */
    internal fun liveSoundKeyOf(p: Project): Int = java.util.Objects.hash(
        p.instrumentTracks.map { listOf(it.engine, it.synth, it.plaits, it.instrument, it.sampled, it.multi, it.voice808, it.layers) },
        p.mixer.channels.map { listOf(it.inserts, it.sends, it.macros) }, p.mixer.master, p.mixer.buses, p.mixer.masterDb, p.mixer.target,
        p.acid.sound,
    )

    /**
     * REVIEW S6: a change of sound or mix (a track's sound, an effect, a fader) makes the live gain
     * found before it stale (a softer sound kept a gain set for a louder one): once the edits have
     * stopped for [LIVE_DRIVE_SETTLE_MS], it is estimated again (an export's own gain, found on the
     * sound before, gives way to it too).
     */
    private fun watchSoundForLiveDrive() {
        viewModelScope.launch {
            var last: Int? = null
            project.map { liveSoundKeyOf(it) }.distinctUntilChanged().collect { key ->
                val before = last
                last = key
                if (before == null) return@collect
                liveDriveSettle?.cancel()
                liveDriveSettle = viewModelScope.launch {
                    kotlinx.coroutines.delay(LIVE_DRIVE_SETTLE_MS)
                    if (liveDriveKey == null && liveDriveTriedKey == null) return@launch  // never estimated: ▶ will
                    liveDriveJob?.join()  // one estimated for the sound before: this one after it
                    // An export's gain gives way only when an estimate really starts for the new sound.
                    val p = project.value
                    val now = liveDriveKeyOf(p)
                    if (!NativeEngine.isAvailable || !liveDriveWanted(p) || now == liveDriveKey || now == liveDriveTriedKey) return@launch
                    liveDriveFromExport = false
                    estimateLiveDrive()
                }
            }
        }
    }

    /** Starts the estimate when this song has none yet (worker side; posted when found). */
    private fun estimateLiveDrive() {
        val p0 = project.value
        if (!NativeEngine.isAvailable || !liveDriveWanted(p0) || liveDriveFromExport) return
        if (liveDriveJob?.isActive == true || liveDriveKey == liveDriveKeyOf(p0) || liveDriveTriedKey == liveDriveKeyOf(p0)) return
        // Dispatched (never run inside the constructor that may start it: the sounds' loaders come later).
        liveDriveJob = viewModelScope.launch(Dispatchers.Main) {
            awaitSoundsLoaded()
            val p = project.value
            if (!liveDriveWanted(p)) return@launch
            val key = liveDriveKeyOf(p)
            liveDriveTriedKey = key
            val index = com.pocketstudio.model.FirstListen.loudPart(p)
            val part = p.song.getOrNull(index) ?: return@launch
            val frames = Math.round(com.pocketstudio.model.FirstListen.loudSteps(p, index) * 15.0 / p.tempoBpm * OfflineExport.SAMPLE_RATE)
            val job = finishOrRelease(Dispatchers.Default, OfflineExport::close) { OfflineExport() } ?: return@launch
            val report = try {
                NativeEngine.captureInto(job) {
                    // The part on its own, as ▶ plays it: the song's curves where it first plays.
                    syncEngine(p.sceneView(part.scene))
                    pushPart(p, part.scene, part)
                    NativeEngine.setMetronome(false)
                    applySongCurves(p, part.scene)
                    NativeEngine.captureAt(0)
                    NativeEngine.leaveSong()
                    NativeEngine.captureAt(-1)
                }
                withContext(Dispatchers.Default) {
                    // Its report only: no multi-MB array of samples thrown away (S6 review).
                    if (job.renderForReport(frames, 0)) job.report() else null
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.add("Gain du direct : estimation impossible (${e.message})")
                null
            } finally {
                withContext(kotlinx.coroutines.NonCancellable + Dispatchers.Main) { job.close() }
            }
            // Only a Drop-aimed render knows the loud part; an export that came in meanwhile wins.
            val found = report?.takeIf { it.loudLufs != null } ?: return@launch
            val drive = found.driveDb.toFloat()
            if (liveDriveFromExport) return@launch
            liveDriveDb = drive
            liveDriveLevel = knownLevelOf(p, found)
            liveDriveKey = key
            // Taken at once while the song plays; before ▶ the engine keeps it until the target comes on.
            // Son 8 review: a new estimate (the sound changed) that would push the drive up while the
            // song plays or a take records waits for the next ▶; one that brings it down is taken.
            val busy = _transport.value.playing || _recording.value
            val given = liveDriveGiven
            if (busy && given != null && drive > given) return@launch
            giveKnownDrive(p, drive, liveDriveLevel)
            if (_transport.value.playing) liveDrivePlayedKey = key
            AppLog.add("Gain du direct : ${"%+.1f".format(drive)} dB (estimé sur « ${com.pocketstudio.model.video.VideoPlanner.partLabel(p, part.scene)} »)")
        }
    }

    /**
     * S7 son (mix version 4): the level, before the drive, of the loud part [report]'s drive was found
     * on (its loud part's loudness, minus the drive, plus what the chain took off); NaN before.
     */
    private fun knownLevelOf(p: Project, report: com.pocketstudio.engine.ExportReport): Float {
        val loud = report.loudLufs ?: return Float.NaN
        return if (p.mixVersion >= Project.MIX_V4) (loud - report.driveDb + report.lossDb).toFloat() else Float.NaN
    }

    /**
     * The known drive to the live engine; with mix version 4 its level first (MasterChain::setKnownLevel:
     * an Intro quieter than that, limited harder, leaves the drive where the export has it). The
     * projects made before send the drive alone, as in S6.
     */
    private fun giveKnownDrive(p: Project, drive: Float, level: Float) {
        liveDriveGiven = drive
        if (p.mixVersion >= Project.MIX_V4) NativeEngine.setKnownLevel(level)
        NativeEngine.setKnownDrive(drive)
    }

    /** The live bench and tests: the estimate started (if any) is done. */
    @androidx.annotation.VisibleForTesting
    internal suspend fun awaitLiveDriveForTest() {
        estimateLiveDrive()
        liveDriveJob?.join()
    }

    /** Tests: [pack] counts as an instrument still being read into memory (null: none any more). */
    @androidx.annotation.VisibleForTesting
    internal fun holdMultiLoading(pack: String?) {
        if (pack == null) loadingMulti.clear() else loadingMulti += pack
        _multiLoading.value = loadingMulti.toSet()
    }

    /** The « vidéo 9:16 » is being made ([com.pocketstudio.app.export.video.VideoExportViewModel]): no export meanwhile. */
    @Volatile internal var videoBusy = false

    /** An export is being made (the card's, « Partager », the video): one private render at a time. */
    val exportBusy: Boolean get() = running != null || exportJob != null || videoBusy

    /**
     * The « vidéo 9:16 » (phase 39): the song (or the scene looped when there is none) as
     * « Partager » renders it, from its start for [frames] frames, into the 24-bit WAV [file], the
     * master held under the M4A's ceiling (the video's sound is AAC too). Once the sounds are in
     * (same waits as [startExport]); the export itself is not changed. Worker side: call from a
     * coroutine, [job] is cancelled through [OfflineExport.cancel].
     */
    internal suspend fun renderForVideo(job: OfflineExport, file: File, frames: Long, onRenderStart: () -> Unit = {}): ExportResult {
        awaitSoundsLoaded()
        val p = project.value
        describe(job, p, song = p.song.isNotEmpty(), soloChannel = null)
        NativeEngine.captureInto(job) {
            NativeEngine.setMasterCeiling(AacEncoder.CEILING_DBTP)
            // S6 (G6): as loud as the song's WAV (its drive searched at −1 dBTP).
            if (p.mixVersion >= Project.MIX_V2) NativeEngine.setDriveCeiling(MASTER_CEILING_DBTP)
        }
        // The sounds are in and the song described: the render itself starts (the stall watch arms here).
        onRenderStart()
        return withContext(Dispatchers.IO) { job.render(file, frames, 0L) }
    }

    /**
     * Renders the project offline (never the live engine): the loop, the whole song or each track,
     * into WAV (or M4A), then copies the files to Music › Pocket Studio.
     */
    fun exportWav() {
        val p = project.value
        startExport(_export.value.forProject(sceneBars(p), p.song.isNotEmpty(), exportEnd(p)), share = false)
    }

    /** Renders with [settings] (the card's, or « Partager mon morceau »'s with [share]). */
    private fun startExport(settings: ExportUi, share: Boolean) {
        if (!NativeEngine.isAvailable || running != null || exportJob != null) return
        if (videoBusy) {
            _export.value = _export.value.copy(error = tr("Une vidéo est en cours : attends qu'elle finisse."), files = emptyList())
            return
        }
        exportCancelled = false
        val app = getApplication<Application>()
        val p = project.value
        // The card keeps its own choices; only the run's state (progress, files, error) is shown.
        val shown = _export.value.copy(sharing = share)
        val song = settings.what == ExportWhat.Song || (settings.what == ExportWhat.Stems && p.song.isNotEmpty())
        if (settings.what == ExportWhat.Song && p.song.isEmpty()) {
            _export.value = shown.copy(error = tr("Le morceau est vide : ajoute des parties dans Structure."), files = emptyList())
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
        _export.value = shown.copy(running = true, progress = 0f, files = emptyList(), error = null, stage = null, loudness = null,
            truePeakDb = null, seconds = null, lossDb = null, decodedPeakDb = null)
        // A foreground service keeps a long export going while the screen is elsewhere (notification, « Annuler »).
        com.pocketstudio.app.export.ExportRun.begin(app, tr("Export de « %s »", com.pocketstudio.model.DefaultNames.shown(p.name)), ::cancelExport)
        exportJob = viewModelScope.launch {
            // How it ended, for the notification when the app is not on screen (null: cancelled).
            var ending: String? = null
            val done = ArrayList<File>()
            var musicOk = true
            var loudness: Double? = null
            var truePeak: Double? = null
            var seconds: Double? = null
            var loss: Double? = null
            var decodedPeak: Double? = null
            val writtenRate = if (settings.cdRate) OfflineExport.CD_RATE else OfflineExport.SAMPLE_RATE
            val writtenBits = if (!settings.limited && settings.format == ExportFormat.Wav) 32 else if (settings.bits16 || settings.format == ExportFormat.M4a) 16 else 24
            try {
                // Just after the project opened, its sounds, banks and song may still be loading: the
                // file would have the kit and the synth in their place.
                awaitSoundsLoaded()
                renders.forEachIndexed { index, (label, solo) ->
                    // Lot 1: what this render may still spend on its M4A check (a new render of the
                    // song, a further lowering); each stem its own, counted once the sounds are in.
                    val budget = com.pocketstudio.app.export.ExportBudget()
                    val wav = runCatching { WavExporter.newFile(app, "${com.pocketstudio.model.DefaultNames.shown(p.name).take(28)}-$label") }.getOrElse { throw UserMessage(tr("Impossible de préparer le fichier d'export.")) }
                    _export.value = _export.value.copy(stage = if (renders.size > 1) "${stageName(label)} (${index + 1}/${renders.size})" else null)
                    // The mix into [wav], its limiter at [ceiling] dBTP (an M4A's; null: the default −1). Again
                    // for an M4A that went over −1 dBTP once decoded (M4aGuard): the limiter, lower, takes the peaks down.
                    suspend fun renderMix(ceiling: Float?): com.pocketstudio.engine.ExportReport? {
                        // A private engine costs tens of milliseconds to build: not on the main thread (never lost if cancelled).
                        val job = finishOrRelease(Dispatchers.Default, OfflineExport::close) { OfflineExport() }!!
                        var poll: kotlinx.coroutines.Job? = null
                        var rendered: com.pocketstudio.engine.ExportReport? = null
                        // Lot 1: a render that stops moving is stopped, and the export says so (never a bar stuck for good).
                        var stalled = false
                        val result = try {
                            // In the try: a describe that fails (a sound gone) still closes the private engine.
                            describe(job, p, song, solo, settings.limited, settings.masterFx)
                            if (ceiling != null) NativeEngine.captureInto(job) {
                                NativeEngine.setMasterCeiling(ceiling)
                                // S6 (G6): a Drop-aimed song's M4A keeps its WAV's drive (searched at −1 dBTP), the
                                // lower ceiling only limits: as loud as the WAV instead of 3 LU under it.
                                if (p.mixVersion >= Project.MIX_V2) NativeEngine.setDriveCeiling(MASTER_CEILING_DBTP)
                            }
                            running = job
                            val watch = com.pocketstudio.app.export.StallWatch()
                            poll = launch {
                                while (true) {
                                    val done = job.progress
                                    _export.value = _export.value.copy(progress = (index + done) / renders.size)
                                    com.pocketstudio.app.export.ExportRun.progress(_export.value.progress, _export.value.stage)
                                    // « Annuler » touched before this render began: it stops at once.
                                    if (exportCancelled) job.cancel()
                                    if (!stalled && watch.stalled(done)) {
                                        stalled = true
                                        AppLog.add("Export : rendu bloqué à ${(done * 100).roundToInt()} %")
                                        job.cancel()
                                    }
                                    kotlinx.coroutines.delay(100)
                                }
                            }
                            withContext(Dispatchers.IO) {
                                job.render(
                                    wav, frames, tailFrames, fadeFrames, autoTail = true, floatWav = !settings.limited,
                                    sampleRate = if (settings.cdRate) OfflineExport.CD_RATE else OfflineExport.SAMPLE_RATE,
                                    // M4A is encoded from 16 bits, dithered there: its WAV stays 24-bit.
                                    bits = if (settings.bits16 && settings.format != ExportFormat.M4a) 16 else 24,
                                ).also { rendered = job.report() }
                            }
                        } finally {
                            poll?.cancel()
                            // Closed on the main thread, after the render returned.
                            job.close()
                            running = null
                        }
                        when (result) {
                            ExportResult.Cancelled -> {
                                wav.delete()
                                if (stalled && !exportCancelled) throw UserMessage(tr("La fabrication du son s'est bloquée : réessaie dans un instant."))
                                throw kotlinx.coroutines.CancellationException("annulé")
                            }
                            ExportResult.IoError -> { wav.delete(); throw UserMessage(tr("L'écriture du fichier a échoué (stockage plein ?).")) }
                            ExportResult.Ok -> Unit
                        }
                        return rendered
                    }
                    // An M4A's limiter holds −2 dBTP: decoded, the AAC stays under −1 dBTP (checked below).
                    var report = budget.took(com.pocketstudio.app.export.M4aGuard.Step.Kind.Render) {
                        renderMix(if (settings.format == ExportFormat.M4a) AacEncoder.CEILING_DBTP else null)
                    }
                    // Measured on the full-quality mix, before any compression (the engine's own report,
                    // as written; read again here only if it is missing).
                    if (solo == null) {
                        loudness = report?.lufs ?: withContext(Dispatchers.IO) { runCatching { Loudness.integrated(wav) }.getOrNull() }
                        truePeak = report?.truePeakDb
                        seconds = report?.seconds
                        loss = report?.takeIf { it.driveDb != 0.0 }?.lossDb
                        // S5: the live target settles on the drive the whole song needed.
                        report?.takeIf { p.mixVersion >= Project.MIX_V2 && it.loudLufs != null && settings.what == ExportWhat.Song }
                            ?.let {
                                giveKnownDrive(p, it.driveDb.toFloat(), knownLevelOf(p, it))
                                // S6 (G5): the export's own drive, better than any estimate.
                                liveDriveFromExport = true
                                liveDriveDb = it.driveDb.toFloat()
                                liveDriveLevel = knownLevelOf(p, it)
                            }
                    }
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
                        com.pocketstudio.app.export.ExportRun.progress(_export.value.progress, name + part)
                        val packed = File(wav.parentFile, wav.nameWithoutExtension + "." + settings.format.extension)
                        withContext(Dispatchers.IO) {
                            try {
                                if (settings.format == ExportFormat.Flac) {
                                    FlacEncoder.encode(wav, packed, { exportCancelled || !isActive }) { p ->
                                        _export.value = _export.value.copy(stage = "$name ${com.pocketstudio.model.i18n.percent((p * 100).roundToInt())}$part")
                                    }
                                }
                            } finally {
                                if (settings.format == ExportFormat.Flac) wav.delete()
                            }
                        }
                        if (settings.format == ExportFormat.M4a) {
                            try {
                                // Each step named and its own bar (lot 1): « Compression M4A », « Vérification »,
                                // « Nouvel essai 2/3 »… never a full bar while minutes of work go on.
                                fun stepDone(fraction: Float) {
                                    // From the encoder's thread: an atomic update, never a lost write against the main thread.
                                    _export.update { it.copy(progress = (index + fraction.coerceIn(0f, 1f)) / renders.size) }
                                }
                                val cancelled = { exportCancelled || !isActive }
                                // Encoded, decoded again as a player would: over −1 dBTP, the mix is rendered again
                                // with its limiter lower by what it went over (the loudness stays) while that fits
                                // in the export's time budget, else encoded lower (M4aGuard).
                                val checked = com.pocketstudio.app.export.M4aGuard.run(
                                    report?.truePeakDb,
                                    render = { ceiling ->
                                        budget.took(com.pocketstudio.app.export.M4aGuard.Step.Kind.Render) { renderMix(ceiling) }.also { r ->
                                            report = r
                                            if (solo == null) {
                                                loudness = r?.lufs ?: loudness
                                                truePeak = r?.truePeakDb
                                                // S5: the « tassé » line follows the mix kept (its limiter lower).
                                                loss = r?.takeIf { it.driveDb != 0.0 }?.lossDb ?: loss
                                            }
                                        }?.truePeakDb
                                    },
                                    encode = { g ->
                                        budget.took(com.pocketstudio.app.export.M4aGuard.Step.Kind.Encode) {
                                            withContext(Dispatchers.IO) { AacEncoder.encode(wav, packed, g, cancelled, ::stepDone) }
                                        }
                                    },
                                    decodedPeak = {
                                        budget.took(com.pocketstudio.app.export.M4aGuard.Step.Kind.Check) {
                                            withContext(Dispatchers.IO) { decodedTruePeak(app, packed, cancelled, ::stepDone) }
                                        }
                                    },
                                    step = { s ->
                                        _export.value = _export.value.copy(stage = s.label() + part, progress = index.toFloat() / renders.size)
                                        com.pocketstudio.app.export.ExportRun.progress(_export.value.progress, s.label() + part)
                                    },
                                    mayGoOn = budget::allows,
                                )
                                if (exportCancelled) throw kotlinx.coroutines.CancellationException("annulé")
                                if (checked.encodings > 1) {
                                    AppLog.add("M4A : crête décodée trop haute, ${checked.renders} rendu(s), ${checked.encodings} encodage(s), limiteur à " +
                                        "${"%.1f".format(checked.ceiling)} dBTP, gain ${"%.2f".format(checked.gain)}")
                                }
                                if (solo == null) decodedPeak = checked.decodedPeakDb
                            } catch (e: Exception) {
                                // A file half written is never shared.
                                packed.delete()
                                throw e
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
                _export.value = _export.value.copy(running = false, files = done, savedInMusic = musicOk, stage = null, loudness = loudness,
                    truePeakDb = truePeak, seconds = seconds, lossDb = loss, decodedPeakDb = decodedPeak, writtenRate = writtenRate,
                    writtenBits = writtenBits, writtenFormat = settings.format, sharing = false)
                ending = tr("Export terminé : %s", done.joinToString(", ") { it.name })
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
                    error = when (e) {
                        is UserMessage -> e.message
                        // Lot 1: the phone's codec stopped answering; the WAV needs none.
                        is com.pocketstudio.app.export.CodecStalledException -> tr("La compression ne répond pas sur ce téléphone : réessaie, ou partage en WAV.")
                        else -> tr("L'export a échoué : %s", e.message ?: e.javaClass.simpleName)
                    })
                ending = _export.value.error
            } finally {
                exportCancelled = false
                exportJob = null
                com.pocketstudio.app.export.ExportRun.end(ending)
            }
        }
    }

    /**
     * The true peak (dBTP) of [file] decoded as a player would (null when it cannot be read: the
     * file, already encoded, is kept as it is; a stuck decoder too, review M1). Read as it is
     * decoded: nothing written to disk. A cancel throws. Blocking.
     */
    private fun decodedTruePeak(app: Application, file: File, isCancelled: () -> Boolean, progress: (Float) -> Unit = {}): Double? =
        com.pocketstudio.app.export.M4aGuard.checkOrKeep({ AppLog.add("M4A : vérification impossible ($it), fichier gardé") }) {
            var meter: com.pocketstudio.app.export.TruePeakMeter? = null
            try {
                AudioDecoder.scan(app, Uri.fromFile(file), MAX_CHECK_SECONDS, isCancelled, progress) { channels, v ->
                    (meter ?: com.pocketstudio.app.export.TruePeakMeter(channels).also { meter = it }).add(v)
                }
            } catch (e: AudioDecoder.CancelledException) {
                throw kotlinx.coroutines.CancellationException("annulé")
            }
            meter?.peakDb
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
                _export.value = _export.value.copy(files = listOf(file), savedInMusic = music, error = null, loudness = null, truePeakDb = null, seconds = null)
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
        // Lot 1: whatever step it is in (waiting for the sounds, between two steps), the export stops now.
        exportJob?.cancel()
    }

    override fun onCleared() {
        exportCancelled = true  // a conversion in progress stops and leaves no file behind
        resampleCancelled = true
        resampling?.cancel()
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
        multiBanks.values.forEach { it.close() }
        multiBanks.clear()
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

        /** The longest export whose M4A is decoded again to check its peak (an hour). */
        private const val MAX_CHECK_SECONDS = 3600.0

        /** Below this a rendered pad is silence (about -80 dBFS): no pad for it. */
        private const val SILENT = 1e-4f

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
        /** A launch tapped this long (ms) after its line, beyond the output latency, still goes on it. */
        const val LAUNCH_GRACE_MS = 60.0
        const val TAKE_BARS_WARNING = 16
        /** Stems export: one render per mixer channel. */
        /** A song automation lane spans at least this many bars (the song can be shorter or empty). */
        const val SONG_LANE_MIN_BARS = 8
        /** Longest microphone take (34 MB of memory). */
        /** A take lasts 5 minutes at most (a whole song: 57 MB of mono float in memory). */
        const val MAX_AUDIO_SECONDS = 300
        const val MAX_EXPORT_BARS = 64
        /** A loop export may run on to the end of an audio clip or of the remix's song, up to this. */
        const val MAX_LONG_EXPORT_BARS = 1024
        private val AUDIO_EXTENSIONS = setOf("wav", "mp3", "flac", "m4a", "aac", "ogg", "opus", "aif", "aiff")
        /** After the last bar, releases, reverbs and echoes ring out until silent (−90 dBFS), ten seconds at most. */
        const val MAX_TAIL_FRAMES = 10L * OfflineExport.SAMPLE_RATE
        /** The master limiter's ceiling of a WAV (dBTP, the engine's MasterChain::kDefaultCeilingDb): an M4A searches its drive there (S6). */
        const val MASTER_CEILING_DBTP = -1f

        /** How long the sound and mix stay unchanged before the live gain is estimated again (watchSoundForLiveDrive). */
        const val LIVE_DRIVE_SETTLE_MS = 1500L
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
            val pads = (0 until DrumPattern.PAD_COUNT).filter { p.mixer.channels.getOrNull(Mixer.padChannel(it))?.routesPad() == true }
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
        /**
         * [a] and [b] (interleaved stereo at [OfflineExport.SAMPLE_RATE]) at the same integrated
         * loudness: the louder one is brought down to the other's (never up: no clipping). Silence
         * is left as it is.
         */
        /** G4: the reference's loudest passage kept (s), each section measured over (s), each track alone over (s). */
        const val REFERENCE_SECONDS = 16.0
        const val MEASURE_SECONDS = 8.0
        const val SOLO_SECONDS = 5.0

        internal fun matchLoudness(a: FloatArray, b: FloatArray): Pair<FloatArray, FloatArray> {
            fun lufs(x: FloatArray) = Loudness.Meter(OfflineExport.SAMPLE_RATE, 2).apply { add(x, x.size / 2) }.integrated()
            val la = lufs(a) ?: return a to b
            val lb = lufs(b) ?: return a to b
            val gain = Math.pow(10.0, -kotlin.math.abs(la - lb) / 20.0).toFloat()
            return if (la > lb) FloatArray(a.size) { a[it] * gain } to b else a to FloatArray(b.size) { b[it] * gain }
        }

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

/** A pan as the journal says it, in French whatever the app's language (the report is read in French). */
internal fun journalPan(pan: Float): String = when {
    pan < -0.05f -> "G ${(-pan * 100).roundToInt()}"
    pan > 0.05f -> "D ${(pan * 100).roundToInt()}"
    else -> "centre"
}

/** A melody track's name, as saved (French) or translated: « Lead », « Mélodie », « Lead 2 »… */
internal fun isMelodyName(name: String): Boolean {
    val french = com.pocketstudio.model.i18n.I18n.frenchOf(name.trim())
    val words = listOf("Lead", "Mélodie", com.pocketstudio.model.TrackKind.Melody.trackName)
    return words.any { w -> french == w || french.startsWith("$w ") }
}
