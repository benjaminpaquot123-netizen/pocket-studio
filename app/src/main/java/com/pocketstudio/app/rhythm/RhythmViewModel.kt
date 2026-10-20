package com.pocketstudio.app.rhythm

import com.pocketstudio.app.report.AppLog
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import android.provider.OpenableColumns
import com.pocketstudio.app.audio.AudioDecoder
import com.pocketstudio.app.audio.MicRecorder
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.export.WavExporter
import com.pocketstudio.app.instruments.InstrumentLibrary
import com.pocketstudio.engine.Preset
import com.pocketstudio.engine.SoundFont
import com.pocketstudio.model.InstrumentChoice
import com.pocketstudio.app.playback.PlaybackService
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.engine.NativeStems
import com.pocketstudio.app.remix.StemSet
import com.pocketstudio.app.remix.StemStore
import com.pocketstudio.model.Remix
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.copySceneTo
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
import com.pocketstudio.model.PadSample
import com.pocketstudio.model.HeardNote
import com.pocketstudio.model.Transcription
import com.pocketstudio.separation.BasicPitch
import com.pocketstudio.model.AcidSound
import com.pocketstudio.model.Favorites
import com.pocketstudio.model.NamedSound
import com.pocketstudio.model.PlaitsPatch
import com.pocketstudio.model.ProjectJson
import com.pocketstudio.model.SoundCategories
import com.pocketstudio.model.SoundLibrary
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.LivePads
import com.pocketstudio.model.sampleFiles
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectSession
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.SynthPatch
import com.pocketstudio.model.TapTempo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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

sealed interface SampleTask {
    data object Idle : SampleTask
    data class Busy(val message: String) : SampleTask
    data class Recording(val seconds: Float, val level: Float) : SampleTask
    data class Failed(val message: String) : SampleTask
}

data class ExportUi(
    val bars: Int = 4,
    val running: Boolean = false,
    val progress: Float = 0f,
    val file: File? = null,
    val savedInMusic: Boolean = false,
    val error: String? = null,
)

/** What the audio engine is doing right now, polled for the playhead and pad lights. */
data class LiveUi(val currentStep: Int = -1, val litPads: Set<Int> = emptySet(), val absoluteStep: Int = -1)

/** Peaks (0..1) of each stem for the waveform view, and the song length in frames at 48 kHz. */
class StemOverview(val peaks: List<FloatArray>, val frames: Long)

/** Session state: the scene waiting to take over, and the song part playing (-1 = not in song mode). */
data class SessionUi(val pending: Int? = null, val songIndex: Int = -1)

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
    fun dismissNotice() { _notice.value = null }

    val session: ProjectSession = ProjectSession(
        id = PROJECT_ID,
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
            _live.value = LiveUi(NativeEngine.currentStep(), lit, NativeEngine.absoluteStep())
            kotlinx.coroutines.delay(33)
        }
    }

    private fun loadOrCreate(): Project {
        try {
            val loaded = runBlocking(Dispatchers.IO) { store.load(PROJECT_ID) }
            if (loaded != null) return loaded
        } catch (e: Exception) {
            AppLog.add("Projet illisible : ${e.message} / ${e.cause?.message}")
            // Never let the first autosave overwrite files we could not read: move them aside.
            val aside = File(store.folder(PROJECT_ID).parentFile, "$PROJECT_ID-illisible-${System.currentTimeMillis()}")
            val moved = store.folder(PROJECT_ID).renameTo(aside)
            _notice.value = if (moved) {
                "Le projet n'a pas pu être relu. Il est mis de côté (${aside.name}) et un nouveau projet commence."
            } else {
                "Le projet n'a pas pu être relu ; un nouveau projet commence."
            }
        }
        var drums = DrumPattern()
        DrumKitInfo.starterHits.forEach { (lane, steps) -> steps.forEach { drums = drums.toggle(lane, it) } }
        val acid = AcidGenerator().generate(AcidPattern(), AcidGenerator.NOTE_SETS[5])
        return Project(name = "Mon premier rythme", drums = drums, acid = acid)
    }

    /** Sends the whole project to the engine, e.g. after the audio stream (re)started. */
    fun syncEngine() {
        if (!NativeEngine.isAvailable) return
        NativeEngine.setStaging(false)  // whatever happened before, edits go to the playing scene
        val p = project.value
        NativeEngine.setTempo(p.tempoBpm)
        NativeEngine.setSwing(p.swing)
        pushDrumPattern(p.drums)
        NativeEngine.setMetronome(_transport.value.metronome)
        pushAcid(p.acid)
        pushSynth(p.synth)
        pushMixer(p)
        pushPads(p)
        pushNotes(p.synthNotes)
        pushInstrument(p)
        pushPlaits(p)
        pushRemix(p)
        pushAutomation(p)
        pushLive(p)
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
    fun importLive(pad: Int, uri: Uri) = sampleJob("Lecture du fichier…", live = pad) {
        val app = getApplication<Application>()
        val name = PadSamples.nameFrom(displayName(app, uri))
        val pcm = try {
            AudioDecoder.decode(app, uri)
        } catch (e: AudioDecoder.TooLongException) {
            throw UserMessage("Ce fichier est trop long : choisis un son plus court.")
        } catch (e: Exception) {
            throw UserMessage("Ce fichier n'a pas pu être lu (${e.message ?: "format inconnu"}).")
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

    // --- Plaits (Mutable Instruments) voice for the synth track -------------------------------

    private fun pushPlaits(p: Project = project.value) {
        if (!NativeEngine.isAvailable) return
        p.plaits.engineValues().forEachIndexed { i, v -> NativeEngine.setPlaitsParam(i, v) }
        NativeEngine.setSynthEngine(p.synthEngine)
    }

    /** 0 = subtractive synth, 1 = Plaits; leaves any General MIDI instrument. */
    fun selectSynthEngine(engine: Int) {
        session.edit { p -> p.copy(synthEngine = engine, instrument = null) }
        pushInstrument()
        pushPlaits()
    }

    fun setPlaitsModel(model: Int) {
        session.edit { p -> p.copy(plaits = p.plaits.copy(model = model)) }
        pushPlaits()
        if (NativeEngine.isAvailable && !_transport.value.playing) {
            viewModelScope.launch {
                kotlinx.coroutines.delay(30)
                previewNote(60)  // hear the new model right away
            }
        }
    }

    fun setPlaitsParam(param: Int, value: Float) {
        session.edit("plaits-$param") { p -> p.copy(plaits = p.plaits.with(param, value)) }
        if (NativeEngine.isAvailable) NativeEngine.setPlaitsParam(param, value.coerceIn(0f, 1f))
    }

    private fun pushDrumPattern(drums: DrumPattern) {
        NativeEngine.setPatternSteps(drums.steps)
        drums.lanes.forEachIndexed { lane, l ->
            for (step in 0 until DrumPattern.MAX_STEPS) NativeEngine.setDrumHit(lane, step, l.hits[step]?.velocity ?: 0f)
        }
    }

    // --- Scenes and song ------------------------------------------------------------------------

    private val _sessionUi = MutableStateFlow(SessionUi())
    val sessionUi: StateFlow<SessionUi> = _sessionUi.asStateFlow()
    private var swapsSeen = 0
    private var swapWatch: kotlinx.coroutines.Job? = null

    /** Stopped: shows and edits [index]. Playing: [index] takes over at the next bar. */
    fun selectScene(index: Int) {
        if (index !in 0 until Scene.MAX) return
        if (!_transport.value.playing || !NativeEngine.isAvailable) {
            _sessionUi.value = _sessionUi.value.copy(pending = null, songIndex = -1)
            session.edit { it.switchScene(index) }
            syncEngine()
            return
        }
        if (index == project.value.scene && _sessionUi.value.pending == null) return
        _sessionUi.value = _sessionUi.value.copy(pending = index, songIndex = -1)
        stageScene(index, 0)
    }

    /** Copies the current scene to the first empty slot and shows it: the usual way to make a variation. */
    fun duplicateScene() {
        val p = project.value
        val scenes = p.allScenes
        val target = scenes.indices.firstOrNull { it != p.scene && scenes[it].isEmpty } ?: scenes.size.takeIf { it < Scene.MAX } ?: return
        session.edit { it.copySceneTo(target) }
        selectScene(target)
    }

    fun addSongPart(scene: Int) {
        if (project.value.song.size >= SongPart.MAX_PARTS) return
        session.edit { p -> p.copy(song = p.song + SongPart(scene, 1)) }
    }

    fun removeSongPart(index: Int) = session.edit { p -> p.copy(song = p.song.filterIndexed { i, _ -> i != index }) }

    fun setSongRepeats(index: Int, repeats: Int) = session.edit("song-$index") { p ->
        p.copy(song = p.song.mapIndexed { i, part -> if (i == index) part.copy(repeats = repeats.coerceIn(1, SongPart.MAX_REPEATS)) else part })
    }

    /** Plays the song from its first part (or stops it). */
    fun toggleSong() {
        if (_transport.value.playing) {
            togglePlay()
            return
        }
        val song = project.value.song
        if (song.isEmpty() || !NativeEngine.isAvailable) return
        session.edit { it.switchScene(song[0].scene) }
        syncEngine()
        _sessionUi.value = SessionUi(songIndex = 0)
        togglePlay()
        // The whole project just went to the engine: let the queue drain before the next scene.
        viewModelScope.launch {
            kotlinx.coroutines.delay(150)
            if (_transport.value.playing && _sessionUi.value.songIndex == 0) stageSongPart(1)
        }
    }

    /**
     * In song mode the next part is sent ahead; if it is the scene being edited (a part
     * repeated, or A then A), edits must reach that waiting copy too, or the swap would undo them.
     */
    private var restageJob: kotlinx.coroutines.Job? = null

    private fun restageIfEditingUpcoming() {
        val ui = _sessionUi.value
        if (ui.songIndex < 0 || ui.pending != project.value.scene || !_transport.value.playing) return
        restageJob?.cancel()
        restageJob = viewModelScope.launch {
            kotlinx.coroutines.delay(150)  // one resend after a burst of edits
            val ui2 = _sessionUi.value
            if (ui2.songIndex >= 0 && ui2.pending == project.value.scene && _transport.value.playing) stageSongPart(ui2.songIndex + 1)
        }
    }

    /** The engine swapped scenes but the 40 ms watch had not seen it yet: follow it now. */
    private fun catchUpMissedSwap() {
        if (!NativeEngine.isAvailable) return
        if (_sessionUi.value.pending != null && NativeEngine.sceneSwaps() != swapsSeen) {
            swapsSeen = NativeEngine.sceneSwaps()
            _sessionUi.value.pending?.let { now -> session.editQuietly { it.switchScene(now) } }
        }
    }

    /** Sends scene [index] to the engine's waiting slot and arms the swap after [afterSteps] (0 = next bar). */
    private fun stageScene(index: Int, afterSteps: Int) {
        val view = project.value.sceneView(index)
        NativeEngine.setStaging(true)
        pushDrumPattern(view.drums)
        pushAcid(view.acid)
        pushNotes(view.synthNotes)
        NativeEngine.setStaging(false)
        NativeEngine.armSceneSwap(afterSteps)
        watchSwaps()
    }

    /** Prepares the song part after the one now playing ([next] wraps around: the song loops). */
    private fun stageSongPart(next: Int) {
        val song = project.value.song
        if (song.isEmpty()) return
        val playing = song[(next - 1 + song.size) % song.size]
        val upcoming = song[next % song.size]
        val steps = project.value.allScenes.getOrNull(playing.scene)?.steps ?: 16
        _sessionUi.value = _sessionUi.value.copy(pending = upcoming.scene)
        stageScene(upcoming.scene, steps * playing.repeats)
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
        val now = ui.pending ?: return
        // The engine already plays it: only the screens follow (no undo step).
        session.editQuietly { it.switchScene(now) }
        val song = project.value.song
        if (ui.songIndex >= 0 && song.isNotEmpty()) {
            val index = (ui.songIndex + 1) % song.size
            _sessionUi.value = SessionUi(songIndex = index)
            stageSongPart(index + 1)
        } else {
            _sessionUi.value = SessionUi()
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
        for (i in 0 until AutomationLane.MAX_LANES) {
            val lane = p.automation.getOrNull(i)
            NativeEngine.setAutomationTarget(i, lane?.target?.pack() ?: -1)
            lane?.values?.forEachIndexed { step, v -> if (v != null) NativeEngine.setAutomationPoint(i, step, v) }
        }
    }

    fun addAutomation(target: AutoTarget) {
        if (project.value.automation.size >= AutomationLane.MAX_LANES || project.value.automation.any { it.target == target }) return
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
        session.edit("auto-$index") { p -> p.copy(automation = p.automation.mapIndexed { i, l -> if (i == index) l.withPoint(step, value) else l }) }
        if (NativeEngine.isAvailable) NativeEngine.setAutomationPoint(index, step, value?.coerceIn(0f, 1f))
    }

    private fun recordAutomation(target: AutoTarget, value: Float) {
        if (!_automationRecord.value || !_transport.value.playing || !NativeEngine.isAvailable) return
        val step = NativeEngine.currentStep()
        if (step !in 0 until AutomationLane.STEPS) return
        var index = project.value.automation.indexOfFirst { it.target == target }
        if (index < 0) {
            if (project.value.automation.size >= AutomationLane.MAX_LANES) return
            addAutomation(target)
            index = project.value.automation.indexOfFirst { it.target == target }
        }
        setAutomationPoint(index, step, value)
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
    private fun stretchStems(source: NativeStems, id: String, ratio: Double) {
        if (remixStretchJob?.isActive == true && stretchPending == id to ratio) return  // already on it
        remixStretchJob?.cancel()
        stretchPending = id to ratio
        _remixStretching.value = true
        remixStretchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(600)
            // Not cancellable once started: the copy must be closed below if no longer wanted.
            val copy = withContext(kotlinx.coroutines.NonCancellable + Dispatchers.Default) {
                runCatching { source.stretched(ratio) }.getOrNull()
            }
            val p = project.value
            if (!isActive || p.remix.setId != id || p.remix.stretchFor(p.tempoBpm) != ratio || stems !== source) {
                copy?.close()
                return@launch
            }
            _remixStretching.value = false
            if (copy == null) {
                AppLog.add("Remix : calage au tempo impossible (mémoire ?)")
                _notice.value = "Le morceau n'a pas pu être calé au tempo (mémoire insuffisante) : il garde son tempo."
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
            // Not cancellable: a finished load must reach the code below, which closes it if unwanted.
            val loaded = withContext(kotlinx.coroutines.NonCancellable + Dispatchers.IO) {
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
                    _notice.value = "Les pistes de « ${wanted.name} » ne sont plus sur le téléphone (ou la mémoire manque). Sépare à nouveau le morceau."
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
            _sampleTask.value = SampleTask.Failed("Trop long pour un pad : choisis moins de mesures.")
            return
        }
        val label = STEM_LABELS.getOrElse(stem) { "Piste" }
        sampleJob("$label vers le pad…") {
            val audio = source.extract(stem, r.startFrame(OfflineExport.SAMPLE_RATE), count)
            if (audio.isEmpty()) throw UserMessage("Ce passage est après la fin du morceau.")
            padSamples.add(Pcm(audio, 2, OfflineExport.SAMPLE_RATE), "$label ${r.name}".take(40))
        }
    }

    /** [bars] bars of one stem onto the first empty live pad: switch the parts on and off in time. */
    fun stemToLive(stem: Int, bars: Int) {
        val source = stems ?: return
        if (_sampleTask.value is SampleTask.Busy || _sampleTask.value is SampleTask.Recording) return
        val pad = project.value.live.clips.indexOfFirst { it == null }
        if (pad < 0) {
            _sampleTask.value = SampleTask.Failed("Les 16 pads live sont pris : vides-en un dans l'onglet Morceau.")
            return
        }
        val r = project.value.remix
        val count = r.barFrames(OfflineExport.SAMPLE_RATE) * bars
        if (count <= 0) return
        if (count > AudioDecoder.MAX_SECONDS * OfflineExport.SAMPLE_RATE) {
            _sampleTask.value = SampleTask.Failed("Trop long pour un pad : choisis moins de mesures.")
            return
        }
        val label = STEM_LABELS.getOrElse(stem) { "Piste" }
        if (project.value.live.size <= pad) setLiveSize(LivePads.SIZES.first { it > pad })
        sampleJob("$label vers le pad live ${pad + 1}…", live = pad, beats = bars * 4) {
            val audio = source.extract(stem, r.startFrame(OfflineExport.SAMPLE_RATE), count)
            if (audio.isEmpty()) throw UserMessage("Ce passage est après la fin du morceau.")
            padSamples.add(Pcm(audio, 2, OfflineExport.SAMPLE_RATE), "$label ${r.name}".take(40))
        }
    }

    // --- Instrument library (General MIDI banks) -------------------------------------------

    private val _library = MutableStateFlow<LibraryState>(LibraryState.Missing)
    val library: StateFlow<LibraryState> = _library.asStateFlow()
    /** The bank shown in the browser (and loaded once opened). */
    private val _bankId = MutableStateFlow(project.value.instrument?.bankId ?: InstrumentLibrary.BANKS[0].id)
    val bankId: StateFlow<String> = _bankId.asStateFlow()
    private var soundFont: SoundFont? = null
    private var loadedBankId: String? = null
    @Volatile private var downloadCancelled = false

    private fun pushInstrument(p: Project = project.value) {
        if (!NativeEngine.isAvailable) return
        val font = soundFont?.takeIf { loadedBankId == p.instrument?.bankId }
        val index = p.instrument?.let { font?.indexOf(it.bank, it.program) } ?: -1
        if (font != null && index >= 0) NativeEngine.setInstrument(font, index) else NativeEngine.setInstrument(null, 0)
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
        if (soundFont != null && loadedBankId == bank.id) {
            _library.value = LibraryState.Ready(soundFont!!.presets)
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
                _library.value = LibraryState.Failed("La bibliothèque était abîmée : télécharge-la à nouveau.")
                return@launch
            }
            // One bank in memory at a time: the engine keeps its own copy of the old one until replaced.
            val old = soundFont
            soundFont = font
            loadedBankId = bank.id
            pushInstrument()
            old?.close()
            if (_bankId.value == bank.id) _library.value = LibraryState.Ready(font.presets)
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
                else LibraryState.Failed("Le téléchargement a échoué (${e.message}). Vérifie la connexion et réessaie.")
            }
        }
    }

    fun cancelLibraryDownload() {
        downloadCancelled = true
    }

    /** null: back to the subtractive synth. */
    fun selectInstrument(choice: InstrumentChoice?) {
        session.edit { p -> p.copy(instrument = choice) }
        pushInstrument()
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
            android.widget.Toast.makeText(getApplication(), "Patiente un instant, un son est en cours de préparation.", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        _sampleTask.value = SampleTask.Busy("Kit ${kit.name}…")
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
                        padSamples.add(Pcm(audio, 1, OfflineExport.SAMPLE_RATE), "${kit.name.take(18)} ${InstrumentLibrary.KIT_PAD_NAMES[pad]}")
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
                    getApplication(), "Kit « ${kit.name} » sur les pads : va dans Rythme (Annuler possible).", android.widget.Toast.LENGTH_LONG,
                ).show()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _sampleTask.value = SampleTask.Failed("Le kit n'a pas pu être chargé (${e.message}).")
                android.widget.Toast.makeText(getApplication(), "Le kit n'a pas pu être chargé.", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    // --- Piano roll ------------------------------------------------------------------------

    /** The engine plays the clip as performed: arpeggios and strums expanded here. */
    private fun pushNotes(clip: NoteClip) {
        if (!NativeEngine.isAvailable) return
        val played = clip.performance.expand(clip)
        NativeEngine.setSynthSteps(clip.steps)
        played.forEachIndexed { i, n -> NativeEngine.setSynthNote(i, n.start, n.length, n.pitch, n.velocity) }
        NativeEngine.setSynthNoteCount(played.size)
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

    fun setPerformance(performance: Performance) = editNotes("perf") { it.copy(performance = performance).sanitized() }

    /** Keyboard in chord mode: the chord of the scale on the key. */
    fun synthChordOn(pitch: Int) = project.value.harmony.chord(pitch).forEach { synthNoteOn(it) }
    fun synthChordOff(pitch: Int) = project.value.harmony.chord(pitch).forEach { synthNoteOff(it) }

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
            app.assets.open("basic-pitch/nmp.onnx").use { input -> file.outputStream().use { input.copyTo(it) } }
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
    fun transcribeFile(uri: Uri) = transcribeJob("Lecture du fichier…") {
        try {
            AudioDecoder.decode(getApplication(), uri)
        } catch (e: AudioDecoder.TooLongException) {
            throw UserMessage("Ce fichier est trop long : choisis un passage plus court.")
        }
    }

    /** Sing, whistle or play near the phone; [stopNoteRecording] ends it (30 s at most). */
    fun recordNotes() {
        if (_transcription.value is SampleTask.Busy || _transcription.value is SampleTask.Recording) return
        val rec = MicRecorder().also { noteRecorder = it }
        _transcription.value = SampleTask.Recording(0f, 0f)
        transcribeJob(null) {
            val pcm = rec.record { sec -> _transcription.value = SampleTask.Recording(sec, rec.level) }
            noteRecorder = null
            if (pcm.frames < 4800 || PadSamples.isSilent(pcm)) throw UserMessage("Rien n'a été entendu : rapproche-toi du micro et réessaie.")
            pcm
        }
    }

    fun stopNoteRecording() {
        noteRecorder?.stop()
    }

    fun dismissTranscription() {
        if (_transcription.value is SampleTask.Failed) _transcription.value = SampleTask.Idle
    }

    private fun transcribeJob(busy: String?, read: suspend () -> Pcm) {
        if (busy != null) _transcription.value = SampleTask.Busy(busy)
        viewModelScope.launch {
            try {
                val heard = withContext(Dispatchers.Default) {
                    val pcm = read()
                    _transcription.value = SampleTask.Busy("Écoute des notes…")
                    val mono = FloatArray(pcm.frames) { f ->
                        var v = 0f
                        for (c in 0 until pcm.channels) v += pcm.data[f * pcm.channels + c]
                        v / pcm.channels
                    }
                    BasicPitch.transcribe(pitchModel(), mono, pcm.sampleRate).map { HeardNote(it.start, it.end, it.pitch, it.amplitude) }
                }
                if (heard.isEmpty()) throw UserMessage("Aucune note reconnue : essaie une mélodie plus nette, une note à la fois.")
                AppLog.add("Audio vers notes : ${heard.size} notes")
                editNotes { Transcription.toClip(heard, project.value.tempoBpm, base = it) }
                _transcription.value = SampleTask.Idle
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                noteRecorder = null
                AppLog.add("Audio vers notes : échec (${e.message})")
                _transcription.value = SampleTask.Failed(if (e is UserMessage) e.message!! else "Ça n'a pas marché : ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }

    private fun editNotes(key: String? = null, transform: (NoteClip) -> NoteClip) {
        session.edit(key) { p -> p.copy(synthNotes = transform(p.synthNotes)) }
        pushNotes(project.value.synthNotes)
    }

    fun addNote(note: Note) {
        editNotes { it.add(note) }
        if (NativeEngine.isAvailable && !_transport.value.playing) previewNote(note.pitch)
    }

    fun removeNote(note: Note) = editNotes { it.remove(note) }

    fun changeNote(old: Note, new: Note) = editNotes { it.replace(old, new) }

    fun quantizeNotes(gridTicks: Int, strength: Float) = editNotes { it.quantized(gridTicks, strength) }

    fun setNoteClipSteps(steps: Int) = editNotes { it.copy(steps = steps).sanitized() }

    fun clearNotes() = editNotes { it.copy(notes = emptyList()) }

    /** A short note so the user hears what they placed. */
    private fun previewNote(pitch: Int) {
        NativeEngine.synthNoteOn(pitch, 0.8f)
        viewModelScope.launch {
            kotlinx.coroutines.delay(250)
            NativeEngine.synthNoteOff(pitch)
        }
    }

    // --- Pad samples (import, microphone) --------------------------------------------------

    private var stretchJob: kotlinx.coroutines.Job? = null

    /** Gives every pad its sound; tempo-fitted pads get their stretched copy as soon as it is ready. */
    private fun pushPads(p: Project = project.value) {
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
                // NonCancellable: a copy made while the tempo moved again is kept (and freed later
                // by pushPads), never lost on the way.
                val copy = withContext(kotlinx.coroutines.NonCancellable + Dispatchers.Default) { padSamples.computeStretch(source, s, bpm) }
                if (copy == null) AppLog.add("Calage au tempo impossible pour « ${s.name} » (son trop court)")
                padSamples.registerStretch(key, copy)
                if (!isActive) break
            }
            if (project.value.tempoBpm == bpm) padSamples.pushPads(project.value.drums.lanes.map { it.sample }, bpm)
        }
    }

    val padSamples = PadSamples(File(store.folder(PROJECT_ID), "samples"))
    val sampleInfo: StateFlow<Map<String, SampleInfo>> = padSamples.info
    private val _sampleTask = MutableStateFlow<SampleTask>(SampleTask.Idle)
    val sampleTask: StateFlow<SampleTask> = _sampleTask.asStateFlow()
    private var recorder: MicRecorder? = null

    private fun loadPadSamples() {
        val files = project.value.sampleFiles
        // After a failed load the folder may still hold the user's sounds: never clean it then.
        val cleanUp = _notice.value == null
        viewModelScope.launch {
            val prepared = withContext(Dispatchers.IO) { padSamples.loadAll(files, cleanUp) }
            prepared.forEach(padSamples::register)
            pushPads()
            pushLive()
        }
    }

    /** Decodes an audio file from the phone and puts it on the selected pad. */
    fun importSample(uri: Uri) = sampleJob("Lecture du fichier…") {
        val app = getApplication<Application>()
        val name = PadSamples.nameFrom(displayName(app, uri))
        val pcm = try {
            AudioDecoder.decode(app, uri)
        } catch (e: AudioDecoder.TooLongException) {
            throw UserMessage("Ce fichier est trop long : choisis un son plus court.")
        } catch (e: Exception) {
            throw UserMessage("Ce fichier n'a pas pu être lu (${e.message ?: "format inconnu"}).")
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
        val frames = OfflineExport.framesFor(bars, p.tempoBpm)
        if (frames > AudioDecoder.MAX_SECONDS * OfflineExport.SAMPLE_RATE) {
            _sampleTask.value = SampleTask.Failed("Trop long à ce tempo : choisis moins de mesures.")
            return
        }
        val job = OfflineExport()
        NativeEngine.captureInto(job) {
            syncEngine()
            NativeEngine.setMetronome(false)
        }
        sampleJob("Enregistrement du morceau…") {
            val data = try {
                job.renderToArray(frames, 0)
            } finally {
                // Native instruments share reference counts: free them on the main thread only.
                withContext(kotlinx.coroutines.NonCancellable + Dispatchers.Main) { job.close() }
            }
            if (data.isEmpty()) throw UserMessage("Le rendu a été interrompu.")
            AppLog.add("Rééchantillonnage : $bars mesures à ${p.tempoBpm.toInt()} BPM")
            padSamples.add(Pcm(data, 2, OfflineExport.SAMPLE_RATE), "Morceau $bars mesure${if (bars > 1) "s" else ""}")
        }
    }

    /**
     * A sample pack: the audio files of a folder (by name) go onto the pads from the selected
     * one on, one undo step. Files that cannot be read are skipped.
     */
    fun importFolder(tree: Uri) = importPack("Lecture du dossier…", "Aucun son dans ce dossier (MP3, WAV, FLAC, M4A, OGG).") { app, max ->
        audioFilesIn(app, tree).take(max)
    }

    /** A sample pack as a .zip: its audio files (any folder inside) go onto the pads in name order. */
    fun importZip(zip: Uri) = importPack("Ouverture du pack…", "Aucun son dans ce .zip (MP3, WAV, FLAC, M4A, OGG).") { app, max ->
        val dir = File(app.cacheDir, "zip-pack")
        val stream = app.contentResolver.openInputStream(zip) ?: throw UserMessage("Le .zip n'a pas pu être ouvert.")
        try {
            stream.use { ZipPack.extractAudio(it, dir, max, AUDIO_EXTENSIONS) }.map { (file, name) -> Uri.fromFile(file) to name }
        } catch (e: ZipPack.TooBigException) {
            throw UserMessage("Ce pack est trop gros (plus de 256 Mo de sons).")
        } catch (e: java.util.zip.ZipException) {
            throw UserMessage("Ce fichier n'est pas un .zip lisible.")
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
                    _sampleTask.value = SampleTask.Busy("Import ${i + 1}/${files.size} : $name")
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
                else SampleTask.Failed("${files.size - added.size} fichier(s) illisible(s) ignoré(s).")
            } catch (e: UserMessage) {
                _sampleTask.value = SampleTask.Failed(e.message!!)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _sampleTask.value = SampleTask.Failed("Le pack n'a pas pu être lu (${e.message}).")
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
            if (pcm.frames < 480 || PadSamples.isSilent(pcm)) throw UserMessage("Rien n'a été entendu : rapproche-toi du micro et réessaie.")
            AppLog.add("Enregistrement micro : ${"%.1f".format(pcm.seconds)} s")
            padSamples.add(pcm, "Micro ${java.time.LocalTime.now().withNano(0)}")
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
                _sampleTask.value = SampleTask.Failed("Pas assez de mémoire pour ce son : essaie plus court.")
            } catch (e: Exception) {
                recorder = null
                AppLog.add("Son du pad : échec (${e.message})")
                _sampleTask.value = SampleTask.Failed(
                    if (e is UserMessage) e.message!! else "Ça n'a pas marché : ${e.message ?: e.javaClass.simpleName}",
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

    private val _meters = MutableStateFlow(FloatArray(NativeEngine.MIXER_CHANNELS + 2))
    val meters: StateFlow<FloatArray> = _meters.asStateFlow()

    /** Polls meter peaks about 20 times a second while the mixer is visible. */
    suspend fun pollMeters() {
        if (!NativeEngine.isAvailable) return
        val n = NativeEngine.MIXER_CHANNELS
        val decayed = FloatArray(n + 2)
        while (true) {
            val fresh = FloatArray(n + 2)
            NativeEngine.mixerMeters(fresh)
            // Peak hold with a gentle fall so the bars are readable.
            for (i in 0..n) decayed[i] = maxOf(fresh[i], decayed[i] * 0.85f)
            decayed[n + 1] = fresh[n + 1]
            _meters.value = decayed.copyOf()
            kotlinx.coroutines.delay(50)
        }
    }

    fun setChannelVolume(ch: Int, v: Float) {
        editChannel(ch, "vol") { it.copy(volume = v) }
        recordAutomation(AutoTarget(AutoTarget.VOLUME, ch), v)
    }

    fun setChannelPan(ch: Int, pan: Float) {
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

    private fun pushMixer(p: Project) {
        if (!NativeEngine.isAvailable) return
        p.mixer.channels.take(NativeEngine.MIXER_CHANNELS).forEachIndexed { i, c ->
            NativeEngine.setChannelVolume(i, c.volume)
            NativeEngine.setChannelPan(i, c.pan)
            NativeEngine.setChannelMute(i, c.mute)
            NativeEngine.setChannelSolo(i, c.solo)
            pushInserts(i, c.inserts)
            c.sends.forEachIndexed { bus, level -> NativeEngine.setSend(i, bus, level) }
        }
        pushInserts(NativeEngine.MIXER_CHANNELS, p.mixer.master)
        p.mixer.buses.forEachIndexed { bus, params -> params.forEachIndexed { k, v -> NativeEngine.setBusParam(bus, k, v) } }
    }

    private fun pushInserts(ch: Int, inserts: List<Insert>) {
        inserts.forEachIndexed { slot, ins ->
            NativeEngine.setInsertType(ch, slot, ins.type)
            ins.params.forEachIndexed { k, v -> NativeEngine.setInsertParam(ch, slot, k, v) }
        }
    }

    // --- Effects (inserts on channels and master, sends, buses) ------------------------------

    /** [ch] 0..3 = a channel, [MASTER] = the master. A new type starts from its defaults. */
    fun setInsertType(ch: Int, slot: Int, type: EffectType) = editInserts(ch, null) { list ->
        list.mapIndexed { i, ins -> if (i == slot) Insert.of(type) else ins }
    }

    // Sliders send only the value that moved (the whole mixer is ~130 commands).
    fun setInsertParam(ch: Int, slot: Int, param: Int, value: Float) {
        val v = value.coerceIn(0f, 1f)
        editInserts(ch, "fx-$slot-$param", push = false) { list ->
            list.mapIndexed { i, ins -> if (i == slot) ins.copy(params = ins.params.mapIndexed { k, old -> if (k == param) v else old }) else ins }
        }
        if (NativeEngine.isAvailable) NativeEngine.setInsertParam(ch, slot, param, v)
        recordAutomation(AutoTarget(AutoTarget.INSERT_PARAM, ch, slot, param), v)
    }

    fun setSend(ch: Int, bus: Int, level: Float) {
        val v = level.coerceIn(0f, 1f)
        session.edit("mix-$ch-send-$bus") { p ->
            val channels = p.mixer.channels.toMutableList()
            channels[ch] = channels[ch].let { c -> c.copy(sends = c.sends.mapIndexed { b, old -> if (b == bus) v else old }) }
            p.copy(mixer = p.mixer.copy(channels = channels))
        }
        if (NativeEngine.isAvailable) NativeEngine.setSend(ch, bus, v)
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
        session.edit(key?.let { "fx-$ch-$it" }) { p ->
            if (ch == MASTER) {
                p.copy(mixer = p.mixer.copy(master = transform(p.mixer.master)))
            } else {
                val channels = p.mixer.channels.toMutableList()
                channels[ch] = channels[ch].let { c -> c.copy(inserts = transform(c.inserts)) }
                p.copy(mixer = p.mixer.copy(channels = channels))
            }
        }
        if (push && NativeEngine.isAvailable) {
            pushInserts(ch, if (ch == MASTER) project.value.mixer.master else project.value.mixer.channels[ch].inserts)
        }
    }

    // --- Subtractive synth -------------------------------------------------------------------

    private val _keyboardOctave = MutableStateFlow(4)
    val keyboardOctave: StateFlow<Int> = _keyboardOctave.asStateFlow()

    fun shiftKeyboardOctave(delta: Int) {
        _keyboardOctave.value = (_keyboardOctave.value + delta).coerceIn(1, 7)
    }

    fun synthNoteOn(note: Int) {
        if (NativeEngine.isAvailable) NativeEngine.synthNoteOn(note, 0.85f)
    }

    fun synthNoteOff(note: Int) {
        if (NativeEngine.isAvailable) NativeEngine.synthNoteOff(note)
    }

    /** Safety net: releases every synth voice (leaving the synth tab or the app). */
    fun synthAllOff() {
        if (NativeEngine.isAvailable) NativeEngine.synthAllOff()
    }

    fun setSynthParam(index: Int, value: Float) {
        session.edit("synth-$index") { p -> p.copy(synth = p.synth.with(index, value)) }
        if (NativeEngine.isAvailable) NativeEngine.setSynthParam(index, project.value.synth.get(index))
    }

    fun applySynthPreset(patch: SynthPatch) {
        session.edit { p -> p.copy(synth = patch) }
        pushSynth(project.value.synth)
    }

    // --- Sound library: built-in sounds, favourites, random ---------------------------------------

    private val favoritesFile = File(app.filesDir, "favorites.json")
    private val _favorites = MutableStateFlow(
        runCatching { if (favoritesFile.exists()) ProjectJson.decodeFavorites(favoritesFile.readText()) else Favorites() }
            .onFailure { AppLog.add("Favoris illisibles : ${it.message}") }.getOrDefault(Favorites()),
    )
    /** The user's own sounds, shared by every project. */
    val favorites: StateFlow<Favorites> = _favorites.asStateFlow()

    private fun editFavorites(transform: (Favorites) -> Favorites) {
        val next = transform(_favorites.value).sanitized()
        _favorites.value = next
        val text = ProjectJson.encodeFavorites(next)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val tmp = File(favoritesFile.path + ".tmp")
                tmp.writeText(text)
                if (!tmp.renameTo(favoritesFile)) error("renommage impossible")
            }.onFailure { AppLog.add("Favoris non enregistrés : ${it.message}") }
        }
    }

    fun saveSynthFavorite() = editFavorites { f ->
        f.copy(synth = f.synth + NamedSound(Favorites.nextName(f.synth.map { it.name }), SoundCategories.FAVORITES, project.value.synth.sanitized()))
    }
    fun savePlaitsFavorite() = editFavorites { f ->
        f.copy(plaits = f.plaits + NamedSound(Favorites.nextName(f.plaits.map { it.name }), SoundCategories.FAVORITES, project.value.plaits))
    }
    fun saveAcidFavorite() = editFavorites { f ->
        f.copy(acid = f.acid + NamedSound(Favorites.nextName(f.acid.map { it.name }), SoundCategories.FAVORITES, project.value.acid.sound))
    }
    fun deleteSynthFavorite(i: Int) = editFavorites { f -> f.copy(synth = f.synth.filterIndexed { k, _ -> k != i }) }
    fun deletePlaitsFavorite(i: Int) = editFavorites { f -> f.copy(plaits = f.plaits.filterIndexed { k, _ -> k != i }) }
    fun deleteAcidFavorite(i: Int) = editFavorites { f -> f.copy(acid = f.acid.filterIndexed { k, _ -> k != i }) }

    fun randomSynth() = applySynthPreset(SoundLibrary.randomSynth())

    fun applyPlaits(patch: PlaitsPatch) {
        session.edit { p -> p.copy(plaits = patch.sanitized()) }
        pushPlaits()
        if (NativeEngine.isAvailable && !_transport.value.playing) {
            viewModelScope.launch {
                kotlinx.coroutines.delay(30)
                previewNote(60)  // hear it right away
            }
        }
    }

    fun randomPlaits() = applyPlaits(SoundLibrary.randomPlaits().copy(level = project.value.plaits.level))

    fun applyAcidSound(sound: AcidSound) {
        session.edit { p -> p.copy(acid = p.acid.copy(sound = sound.sanitized())) }
        if (NativeEngine.isAvailable) project.value.acid.sound.values().forEachIndexed { i, v -> NativeEngine.setAcidParam(i, v) }
    }

    /** New character, same tuning and volume. */
    fun randomAcid() = applyAcidSound(SoundLibrary.randomAcid(keep = project.value.acid.sound))

    private fun pushSynth(patch: SynthPatch) {
        if (!NativeEngine.isAvailable) return
        patch.sanitized().values.forEachIndexed { i, v -> NativeEngine.setSynthParam(i, v) }
    }

    // --- Acid bass -------------------------------------------------------------------------

    private val acidGenerator = AcidGenerator()

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
        session.edit { it.copy(acid = acidGenerator.generate(it.acid)) }
        pushAcid(project.value.acid)
    }

    fun editAcidStep(transform: (AcidStep) -> AcidStep) {
        val index = _selectedAcidStep.value
        session.edit { p -> p.copy(acid = p.acid.withStep(index, transform(p.acid.steps[index]))) }
        if (NativeEngine.isAvailable) NativeEngine.setAcidStep(index, project.value.acid.steps[index].pack())
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
    }

    fun shiftAcidRoot(semitones: Int) {
        session.edit { p -> p.copy(acid = p.acid.copy(root = p.acid.root + semitones)) }
        if (NativeEngine.isAvailable) NativeEngine.setAcidRoot(project.value.acid.root)
    }

    fun setAcidDensity(density: Float) {
        session.edit("acid-density") { p -> p.copy(acid = p.acid.copy(density = density)) }
    }

    private fun pushAcid(acid: AcidPattern) {
        if (!NativeEngine.isAvailable) return
        NativeEngine.setAcidLength(acid.length)
        NativeEngine.setAcidRoot(acid.root)
        acid.steps.forEachIndexed { i, s -> NativeEngine.setAcidStep(i, s.pack()) }
        acid.sound.values().forEachIndexed { i, v -> NativeEngine.setAcidParam(i, v) }
    }

    fun togglePlay() {
        val t = _transport.value
        val playing = !t.playing
        _transport.value = t.copy(playing = playing)
        if (!NativeEngine.isAvailable) return
        if (playing) NativeEngine.play(if (t.countIn) 1 else 0) else {
            NativeEngine.stopTransport()
            catchUpMissedSwap()
            _sessionUi.value = SessionUi()
            // Engine and screens agree again on the scene, whatever the swaps did.
            syncEngine()
        }
        val app = getApplication<Application>()
        AppLog.add(if (playing) "Lecture (${project.value.tempoBpm.toInt()} BPM)" else "Stop")
        if (playing) PlaybackService.start(app) else PlaybackService.stop(app)
    }

    /** True while the activity is on screen; set by the UI. */
    var visible = true

    /** The engine must keep running in the background only while the transport plays. */
    val keepsRunningInBackground: Boolean get() = _transport.value.playing

    init {
        viewModelScope.launch { project.collect { restageIfEditingUpcoming() } }
        loadPadSamples()
        if (project.value.instrument != null) openLibrary()
        viewModelScope.launch {
            session.lastSaveError.collect { e -> if (e != null) AppLog.add("Sauvegarde impossible : ${e.message}") }
        }
        viewModelScope.launch {
            PlaybackService.stops.collect {
                AppLog.add("Stop depuis la notification")
                if (_transport.value.playing) togglePlay()
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
            lanes[lane] = lanes[lane].copy(hits = lanes[lane].hits + (step to Hit(next)))
            p.copy(drums = p.drums.copy(lanes = lanes))
        }
        pushLane(lane)
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
        session.undo()
        syncEngine()
    }

    fun redo() {
        session.redo()
        syncEngine()
    }

    /** Writes pending changes now (the app is going to the background). */
    fun flush() {
        viewModelScope.launch { withContext(NonCancellable) { session.flush() } }
    }

    private fun pushLane(lane: Int) {
        if (!NativeEngine.isAvailable) return
        val hits = project.value.drums.lanes[lane].hits
        for (step in 0 until DrumPattern.MAX_STEPS) NativeEngine.setDrumHit(lane, step, hits[step]?.velocity ?: 0f)
    }

    // --- Export ----------------------------------------------------------------------------

    private val _export = MutableStateFlow(ExportUi())
    val export: StateFlow<ExportUi> = _export.asStateFlow()
    private var running: OfflineExport? = null

    fun setExportBars(bars: Int) {
        _export.value = _export.value.copy(bars = bars.coerceIn(1, 64))
    }

    /** Renders the project offline (not the live engine) into a WAV, then copies it to Music. */
    fun exportWav() {
        if (!NativeEngine.isAvailable || running != null) return
        val app = getApplication<Application>()
        val p = project.value
        val bars = _export.value.bars
        val file = runCatching { WavExporter.newFile(app, p.name) }.getOrElse {
            _export.value = ExportUi(bars = bars, error = "Impossible de préparer le fichier d'export.")
            return
        }
        val job = OfflineExport()
        NativeEngine.captureInto(job) {
            syncEngine()
            NativeEngine.setMetronome(false)
        }
        running = job
        _export.value = ExportUi(bars = bars, running = true)
        viewModelScope.launch {
            // Progress is polled on the main thread, and the job is closed on it too, so
            // the native renderer is never read after it is freed.
            val poll = launch {
                while (true) {
                    _export.value = _export.value.copy(progress = job.progress)
                    kotlinx.coroutines.delay(100)
                }
            }
            try {
                val result = withContext(Dispatchers.IO) {
                    val r = job.render(file, OfflineExport.framesFor(bars, p.tempoBpm), TAIL_FRAMES)
                    r to (r == ExportResult.Ok && WavExporter.copyToMusic(app, file))
                }
                _export.value = when (result.first) {
                    ExportResult.Ok -> ExportUi(bars = bars, file = file, savedInMusic = result.second)
                    ExportResult.Cancelled -> ExportUi(bars = bars)
                    ExportResult.IoError -> ExportUi(bars = bars, error = "L'écriture du fichier a échoué (stockage plein ?).")
                }
                AppLog.add("Export $bars mesures : ${result.first}, copie Musique = ${result.second}")
                if (result.first != ExportResult.Ok) file.delete()
            } finally {
                poll.cancel()
                job.close()
                running = null
            }
        }
    }

    fun cancelExport() {
        running?.cancel()
    }

    override fun onCleared() {
        downloadCancelled = true
        soundFont?.close()
        running?.cancel()
        recorder?.stop()
        padSamples.close()
        stems?.close()
        stretched?.close()
        if (NativeEngine.isAvailable) NativeEngine.stopTransport()
        PlaybackService.stop(getApplication())
    }

    companion object {
        const val PROJECT_ID = "projet-1"
        private val AUDIO_EXTENSIONS = setOf("wav", "mp3", "flac", "m4a", "aac", "ogg", "opus", "aif", "aiff")
        /** Two seconds after the last bar so releases and the acid tail ring out. */
        const val TAIL_FRAMES = 2L * OfflineExport.SAMPLE_RATE
        private val STEM_LABELS = listOf("Batterie", "Basse", "Autres", "Voix")
        private const val OVERVIEW_BUCKETS = 400
        /** Insert "channel" index of the master. */
        const val MASTER = NativeEngine.MIXER_CHANNELS
    }
}
