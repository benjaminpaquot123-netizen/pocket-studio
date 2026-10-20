package com.pocketstudio.app.rhythm

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
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.Macros
import com.pocketstudio.model.FxPresets
import com.pocketstudio.model.withFx
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
import com.pocketstudio.model.ProjectJson
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
import com.pocketstudio.model.sceneSteps
import com.pocketstudio.model.togglePartTrack
import com.pocketstudio.model.songBars
import com.pocketstudio.model.withBeatsPerBar
import com.pocketstudio.model.AudioTrack
import com.pocketstudio.model.AudioClip
import com.pocketstudio.model.addAudioTrack
import com.pocketstudio.model.removeAudioTrack
import com.pocketstudio.model.withAudioTrack
import com.pocketstudio.model.instrumentTracks
import com.pocketstudio.model.track
import com.pocketstudio.model.withTrack
import com.pocketstudio.model.addTrack
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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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

/** A microphone take on an audio track: [waiting] during the count-in. */
data class AudioRecordingUi(val track: Int, val waiting: Boolean = true, val seconds: Float = 0f, val level: Float = 0f)

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

data class ExportUi(
    val bars: Int = 4,
    val what: ExportWhat = ExportWhat.Loop,
    val format: ExportFormat = ExportFormat.Wav,
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
}

/** What the audio engine is doing right now, polled for the playhead and pad lights. */
data class LiveUi(val currentStep: Int = -1, val litPads: Set<Int> = emptySet(), val absoluteStep: Int = -1, val songStep: Int = -1)

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
    fun dismissNotice() {
        _notice.value = null
        favoritesBeforeDelete = null
    }

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
            val loaded = runBlocking(Dispatchers.IO) { store.load(projectId) }
            if (loaded != null) return loaded
        } catch (e: Exception) {
            AppLog.add("Projet illisible : ${e.message} / ${e.cause?.message}")
            // Never let the first autosave overwrite files we could not read: move them aside.
            val aside = File(store.folder(projectId).parentFile, "$projectId-illisible-${System.currentTimeMillis()}")
            val moved = store.folder(projectId).renameTo(aside)
            _notice.value = if (moved) {
                "Le projet n'a pas pu être relu. Il est mis de côté (${aside.name}) et un nouveau projet commence."
            } else {
                "Le projet n'a pas pu être relu ; un nouveau projet commence."
            }
        }
        return starterProject()
    }

    /** The audio stream (re)started: everything is sent again, instruments included. */
    fun engineStarted() {
        liveInstruments.clear()
        syncEngine()
    }

    /** Sends the whole project to the engine, e.g. after the audio stream (re)started. */
    fun syncEngine(p: Project = project.value) {
        if (!NativeEngine.isAvailable) return
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
        // Playing the song: the part on now keeps its muted tracks (live only: an export renders
        // the scene or the song as written, whatever plays at that moment).
        if (p === project.value && !NativeEngine.isCapturing) playingPart()?.let { pushPart(p, it.scene, it) }
    }

    /** The song part playing now, or null outside song playback. */
    private fun playingPart(): SongPart? {
        val i = _sessionUi.value.songIndex
        return if (i >= 0 && _transport.value.playing) project.value.song.getOrNull(i) else null
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
            _notice.value = "Huit pistes d'instruments au maximum : supprime-en une pour en ajouter une autre."
            return
        }
        val id = next.tracks.first { t -> p.tracks.none { it.id == t.id } }.id
        session.edit { next }
        pushTracks()
        pushMixer()
        selectTrack(id)
        AppLog.add("Piste ajoutée : ${project.value.track(id).name}")
    }

    fun duplicateTrack(id: Int) {
        val p = project.value
        val next = p.duplicateTrack(id) ?: run {
            _notice.value = "Huit pistes d'instruments au maximum : supprime-en une pour en dupliquer une autre."
            return
        }
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
        for (id in 0 until AudioTrack.MAX) {
            NativeEngine.setAudioOn(id, part?.plays(SongPart.audio(id)) ?: true)
            val clip = p.audioTracks.firstOrNull { it.id == id }?.clip
            val native = clip?.let { padSamples.native(it.file) }
            if (clip != null && native != null) {
                NativeEngine.setAudioClipParams(
                    id, clip.start, clip.loopSteps, clip.gain,
                    (clip.fadeIn * OfflineExport.SAMPLE_RATE).toInt(), (clip.fadeOut * OfflineExport.SAMPLE_RATE).toInt(),
                )
            }
            NativeEngine.setAudioClip(id, native)
        }
    }

    fun addAudioTrack() {
        val next = project.value.addAudioTrack() ?: run {
            _notice.value = "Quatre pistes audio au maximum : supprime-en une pour en ajouter une autre."
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

    private fun editAudio(id: Int, key: String? = null, transform: (AudioTrack) -> AudioTrack) {
        val t = project.value.audioTracks.firstOrNull { it.id == id } ?: return
        session.edit(key) { it.withAudioTrack(transform(t)) }
        pushAudio()
    }

    private fun editClip(id: Int, key: String? = null, transform: (AudioClip) -> AudioClip) =
        editAudio(id, key) { t -> t.copy(clip = t.clip?.let(transform)) }

    /** The clip starts on bar [bar] (1 = the first). */
    fun setAudioStart(id: Int, bar: Int) = editClip(id) { it.copy(start = (bar - 1) * project.value.stepsPerBar) }
    fun setAudioLoopBars(id: Int, bars: Int) = editClip(id) { it.copy(loopSteps = bars * project.value.stepsPerBar) }
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
            prepared.forEach(padSamples::register)
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
        audioJob("Lecture du fichier…") {
            val app = getApplication<Application>()
            val name = PadSamples.nameFrom(displayName(app, uri))
            val pcm = try {
                withContext(Dispatchers.IO) { AudioDecoder.decode(app, uri) }  // 2 minutes at most (cut)
            } catch (e: Exception) {
                throw UserMessage("Ce fichier n'a pas pu être lu (${e.message ?: "format inconnu"}).")
            }
            AppLog.add("Piste audio ${id + 1} : « $name », ${"%.1f".format(pcm.seconds)} s")
            val loopSeconds = maxLoopSteps() * 15.0 / project.value.tempoBpm
            when {
                pcm.seconds >= AudioDecoder.MAX_SECONDS - 0.5 ->
                    _notice.value = "Ce fichier dure plus de ${AudioDecoder.MAX_SECONDS / 60} minutes : seul le début est gardé."
                pcm.seconds > loopSeconds ->
                    _notice.value = "À ce tempo, une piste audio boucle au bout de 64 mesures : la fin de « $name » ne sera pas jouée."
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
        sampleJob("Pose du son sur le pad ${_transport.value.selectedPad + 1}…") { padSamples.add(if (oneShot) pcm.withoutTail() else pcm, name) }
        return true
    }

    /** On the first free live pad, its length in beats fitted to the tempo. */
    fun placeOnLive(pcm: Pcm, name: String) {
        if (_sampleTask.value is SampleTask.Busy || _sampleTask.value is SampleTask.Recording) return
        val pad = project.value.live.clips.indexOfFirst { it == null }
        if (pad < 0) {
            _sampleTask.value = SampleTask.Failed("Les 16 pads live sont pris : vides-en un dans l'onglet Morceau.")
            return
        }
        if (project.value.live.size <= pad) setLiveSize(LivePads.SIZES.first { it > pad })
        sampleJob("Pose du son sur le pad live ${pad + 1}…", live = pad) { padSamples.add(pcm, name) }
    }

    /** On live pad [pad] (from the sound library), its length in beats fitted to the tempo; false when busy. */
    fun placeOnLivePad(pad: Int, pcm: Pcm, name: String): Boolean {
        if (_sampleTask.value is SampleTask.Busy || _sampleTask.value is SampleTask.Recording) return false
        if (pad !in 0 until LivePads.MAX) return true
        AppLog.add("Pad live ${pad + 1} : « $name » de la bibliothèque")
        sampleJob("Pose du son sur le pad live ${pad + 1}…", live = pad) { padSamples.add(pcm, name) }
        return true
    }

    /** On the first audio track without a clip; one is added when all are taken. */
    fun placeOnAudioTrack(pcm: Pcm, name: String) {
        if (_sampleTask.value is SampleTask.Busy || _sampleTask.value is SampleTask.Recording || _audioRecording.value != null) return
        val p = project.value
        val id = p.audioTracks.firstOrNull { it.clip == null }?.id ?: run {
            val next = p.addAudioTrack() ?: run {
                _notice.value = "Les quatre pistes audio sont prises : vide-en une ou pose le son sur un pad."
                return
            }
            session.edit { next }
            pushMixer()
            next.audioTracks.first { t -> p.audioTracks.none { it.id == t.id } }.id
        }
        AppLog.add("Son généré « $name » sur la piste audio ${id + 1}")
        audioJob("Pose du son…") { putClip(id, pcm, name) }
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
                _sampleTask.value = SampleTask.Failed("Pas assez de mémoire pour ce son : essaie plus court.")
            } catch (e: Exception) {
                AppLog.add("Piste audio : échec (${e.message})")
                _sampleTask.value = SampleTask.Failed(if (e is UserMessage) e.message!! else "Ça n'a pas marché : ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }

    /**
     * Records the microphone onto audio track [id]: the song starts (after the count-in) and the
     * take begins exactly on its first step. [stopAudioRecording] ends it.
     */
    fun recordAudio(id: Int) {
        if (!NativeEngine.isAvailable || _audioRecording.value != null) return
        if (_recording.value) stopRecordingTake()
        if (_transport.value.playing) togglePlay()
        _audioRecording.value = AudioRecordingUi(id, waiting = true)
        viewModelScope.launch {
            // Opening the microphone can take up to a second: never on the main thread.
            // No longer than a track can loop at this tempo (64 bars): the rest would never play.
            val seconds = minOf(MAX_AUDIO_SECONDS, (maxLoopSteps() * 15.0 / project.value.tempoBpm).toInt())
            // A microphone opened while the project closes is closed again, not left armed.
            val handle = finishOrRelease(Dispatchers.IO, { h: Long -> if (h != 0L) finishInBackground(h) }) {
                NativeEngine.recordStart(onStart = true, seconds = seconds)
            } ?: 0L
            if (_audioRecording.value?.track != id) {  // cancelled meanwhile
                if (handle != 0L) finishInBackground(handle)
                return@launch
            }
            if (handle == 0L) {
                _audioRecording.value = null
                _notice.value = "Le micro n'a pas pu être ouvert. Vérifie l'autorisation du micro et qu'aucune autre appli ne l'utilise."
                return@launch
            }
            recordHandle = handle
            // The output the take is heard on now (a headset unplugged later must not change it).
            takeLatencyMs = Metrics.recordLatencyMs()
            _transport.value = _transport.value.copy(playing = true)
            NativeEngine.play(com.pocketstudio.app.settings.SettingsStore.state.value.recordCountIn.coerceAtLeast(1))
            PlaybackService.start(getApplication())
            AppLog.add("Prise audio sur la piste ${id + 1}")
            recordPoll = launch {
                while (true) {
                    val st = NativeEngine.recordStatus(handle)
                    val ui = _audioRecording.value ?: break
                    _audioRecording.value = ui.copy(waiting = st[0] < 0.5f, seconds = st[1] / OfflineExport.SAMPLE_RATE.toFloat(), level = st[2])
                    if (st[0] > 1.5f) {  // full: the take ends by itself
                        stopAudioRecording()
                        break
                    }
                    kotlinx.coroutines.delay(50)
                }
            }
        }
    }

    /** Ends the take: the song stops and the recording becomes the track's clip (Annuler possible). */
    fun stopAudioRecording() {
        val ui = _audioRecording.value ?: return
        val handle = recordHandle
        recordHandle = 0L
        recordPoll?.cancel()
        _audioRecording.value = null
        if (_transport.value.playing) togglePlay()
        if (handle == 0L) return
        // What the microphone hears lags what was played by the round trip (speaker → micro).
        val latencyMs = takeLatencyMs
        _takeSaving.value = true
        takeJob = audioJob("Enregistrement de la prise…", always = true) {
            val pcm = withContext(Dispatchers.IO) {
                val audio = NativeEngine.recordFinish(handle) ?: throw UserMessage("La prise n'a pas pu être récupérée : réessaie.")
                if (audio.size < OfflineExport.SAMPLE_RATE / 10) null
                else {
                    val skip = (latencyMs / 1000.0 * OfflineExport.SAMPLE_RATE).toInt().coerceIn(0, audio.size / 2)
                    Pcm(audio.copyOfRange(skip, audio.size), 1, OfflineExport.SAMPLE_RATE)
                }
            }
            if (pcm == null) {
                if (!ui.waiting) throw UserMessage("La prise est trop courte ou le micro n'a rien donné.")
                return@audioJob  // stopped during the count-in: nothing was recorded
            }
            if (PadSamples.isSilent(pcm)) throw UserMessage("Le micro n'a rien entendu : rapproche-toi et réessaie.")
            putClip(ui.track, pcm, "Prise ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.FRANCE).format(java.util.Date())}")
            AppLog.add("Prise audio : ${"%.1f".format(pcm.seconds)} s, latence compensée ${latencyMs.toInt()} ms")
        }
        // Annuler waits until the take is on its track (or has failed).
        takeJob?.invokeOnCompletion { _takeSaving.value = false } ?: run { _takeSaving.value = false }
    }

    private var takeJob: kotlinx.coroutines.Job? = null
    private var takeLatencyMs = 0.0
    private val _takeSaving = MutableStateFlow(false)
    /** A stopped take is still being written to its track (Annuler waits for it). */
    val takeSaving: StateFlow<Boolean> = _takeSaving.asStateFlow()

    /** Before another project opens: a take in progress is stopped and kept on its track. */
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
        if (_transport.value.playing) togglePlay()
        if (handle != 0L) finishInBackground(handle)
    }

    /** Frees a take and closes the microphone off the main thread (also when the project closes). */
    private fun finishInBackground(handle: Long) {
        Thread({ NativeEngine.recordFinish(handle) }, "record-finish").start()
    }

    /** Clip edits: a new sound file each time, so Annuler brings the previous one back. */
    private fun editClipAudio(id: Int, label: String, transform: (Pcm) -> Pcm) {
        val clip = project.value.audioTracks.firstOrNull { it.id == id }?.clip ?: return
        audioJob("$label…") {
            val pcm = withContext(Dispatchers.Default) { transform(padSamples.read(clip.file)) }
            putClip(id, pcm, clip.name, keep = clip)
        }
    }

    /** Loudest point at -1 dBFS. */
    fun normalizeAudio(id: Int) = editClipAudio(id, "Normalisation") { AudioEdits.normalize(it) }
    fun reverseAudio(id: Int) = editClipAudio(id, "Inversion") { AudioEdits.reverse(it) }
    /** Keeps [from]..[to] (fractions of the clip). */
    fun trimAudio(id: Int, from: Float, to: Float) = editClipAudio(id, "Découpe") { AudioEdits.trim(it, from, to) }

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
            MidiMapping.SYNTH -> setSynthParam(t.param, v)
            MidiMapping.PLAITS -> setPlaitsParam(t.param, v)
            MidiMapping.PERFORMANCE -> if (NativeEngine.isAvailable) NativeEngine.setPerformance(t.param, v)
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
                    _trackSoundBusy.value = "La piste a été supprimée entre-temps : rien n'a changé."
                    return@launch
                }
                val slide = kind == "808"
                editTrack(id = id) {
                    it.copy(
                        engine = 2, instrument = null,
                        sampled = SampledPatch(
                            file = sample.file, name = name, root = root ?: 60f,
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
                _trackSoundBusy.value = if (root == null) "Note d'origine introuvable : règle-la à l'oreille avec − et +." else null
                if (NativeEngine.isAvailable && !_transport.value.playing) previewNote(root?.let { Math.round(it) }?.coerceIn(24, 96) ?: 60)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Throwable) {
                AppLog.add("Son au clavier : échec (${e.message})")
                _trackSoundBusy.value = "Ce son n'a pas pu être préparé."
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
                if (_trackSoundBusy.value != BUSY_TRACK_SOUND) _trackSoundBusy.value = "Ce son n'a pas pu être lu."
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
                if (_trackSoundBusy.value != BUSY_TRACK_SOUND) _trackSoundBusy.value = "Ce fichier n'a pas pu être lu."
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

    /** Time signature [beats]/4: the drum loop keeps its number of bars (up to 64 steps). */
    fun setBeatsPerBar(beats: Int) {
        session.edit { it.withBeatsPerBar(beats) }
        syncEngine()
    }

    /** The timeline: track [key] plays or stays silent in song part [index]. */
    fun togglePartTrack(index: Int, key: String) {
        session.edit { it.togglePartTrack(index, key).sanitized() }
        // Playing the song: the part on now changes at once, the next one is sent again.
        val ui = _sessionUi.value
        val song = project.value.song
        if (!NativeEngine.isAvailable || !_transport.value.playing || ui.songIndex < 0 || song.isEmpty()) return
        if (ui.songIndex == index) pushPart(project.value, song[index].scene, song[index])
        stageSongPart(ui.songIndex + 1)
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
        pushPart(project.value, song[0].scene, song[0])  // the first part without its muted tracks
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

    /**
     * Sends scene [index] to the engine's waiting slot and arms the swap after [afterSteps] (0 = next
     * bar). A song [part] plays without its muted tracks.
     */
    private fun stageScene(index: Int, afterSteps: Int, part: SongPart? = null, restartsSong: Boolean = false) {
        NativeEngine.setStaging(true)
        pushPart(project.value, index, part)
        NativeEngine.setStaging(false)
        NativeEngine.armSceneSwap(afterSteps, restartsSong)
        watchSwaps()
    }

    /** The patterns of scene [index] as [part] plays them (to the playing or the waiting slot). */
    private fun pushPart(p: Project, index: Int, part: SongPart?) {
        val view = if (part != null) p.partView(part) else p.sceneView(index)
        pushDrumPattern(view.drums)
        pushAcid(view.acid)
        view.instrumentTracks.forEach { pushNotes(it.notes, it.id) }
        for (id in 0 until AudioTrack.MAX) NativeEngine.setAudioOn(id, part?.plays(SongPart.audio(id)) ?: true)
        // Per-scene automation clips: the scene's own steps (all of them, empty ones included).
        p.automation.forEachIndexed { i, lane ->
            if (lane.mode == AutomationLane.PER_SCENE) lane.valuesFor(index).forEachIndexed { step, v -> NativeEngine.setAutomationPoint(i, step, v) }
        }
    }

    /** Prepares the song part after the one now playing ([next] wraps around: the song loops). */
    private fun stageSongPart(next: Int) {
        val song = project.value.song
        if (song.isEmpty()) return
        val playing = song[(next - 1 + song.size) % song.size]
        val upcoming = song[next % song.size]
        val steps = project.value.sceneSteps(playing.scene)
        _sessionUi.value = _sessionUi.value.copy(pending = upcoming.scene)
        stageScene(upcoming.scene, steps * playing.repeats, upcoming, restartsSong = next % song.size == 0)
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
            if (lane == null) continue
            pushSongCurve(p, i, lane)
            lane.valuesFor(p.scene).forEachIndexed { step, v -> if (v != null) NativeEngine.setAutomationPoint(i, step, v) }
        }
    }

    /** A song lane's curve over the bars the screen shows (the song, at least 8 bars). */
    private fun pushSongCurve(p: Project, i: Int, lane: AutomationLane) {
        val curve = if (lane.mode == AutomationLane.SONG) lane.songCurve(maxOf(SONG_LANE_MIN_BARS, p.songBars)) else emptyList()
        NativeEngine.setAutomationMode(i, lane.mode, curve.size)
        curve.forEachIndexed { bar, v -> NativeEngine.setAutomationSong(i, bar, v) }
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

    /** The song curve: [value] on bar [bar] (null erases). */
    fun setAutomationSongPoint(index: Int, bar: Int, value: Float?) {
        if (index !in project.value.automation.indices) return
        session.edit("auto-song-$index") { p -> p.copy(automation = p.automation.mapIndexed { i, l -> if (i == index) l.withSongPoint(bar, value) else l }) }
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
        if (NativeEngine.isAvailable && project.value.automation[index].mode != AutomationLane.SONG) {
            NativeEngine.setAutomationPoint(index, step, value?.coerceIn(0f, 1f))
        }
    }

    private fun recordAutomation(target: AutoTarget, value: Float) {
        if (!_automationRecord.value || !_transport.value.playing || !NativeEngine.isAvailable || !target.isValid) return
        val step = NativeEngine.currentStep()
        if (step !in 0 until AutomationLane.STEPS) return
        var index = project.value.automation.indexOfFirst { it.target == target }
        if (index < 0) {
            if (project.value.automation.size >= AutomationLane.MAX_LANES) return
            addAutomation(target)
            index = project.value.automation.indexOfFirst { it.target == target }
        }
        // A song curve records on the bar the song is at.
        if (project.value.automation[index].mode == AutomationLane.SONG) {
            // The bar the engine's song curve is reading now (it restarts when the song loops).
            val songStep = NativeEngine.songStep()
            if (songStep >= 0) setAutomationSongPoint(index, songStep / project.value.stepsPerBar, value)
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
            _notice.value = "Le tempo du projet est trop loin de celui du morceau pour le caler (de la moitié au double)."
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
                _library.value = LibraryState.Failed("La bibliothèque était abîmée : télécharge-la à nouveau.")
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
                else LibraryState.Failed("Le téléchargement a échoué (${e.message}). Vérifie la connexion et réessaie.")
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
    private fun pushNotes(clip: NoteClip, id: Int = track) {
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
    fun transcribeFile(uri: Uri) = transcribeJob("Lecture du fichier…") {
        try {
            AudioDecoder.decode(getApplication(), uri)
        } catch (e: AudioDecoder.TooLongException) {
            throw UserMessage("Ce fichier est trop long : choisis un passage plus court.")
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

    private var transcribing: kotlinx.coroutines.Job? = null

    private fun transcribeJob(busy: String?, read: suspend () -> Pcm) {
        if (transcribing?.isActive == true && busy != null) return  // one at a time
        if (busy != null) _transcription.value = SampleTask.Busy(busy)
        transcribing = viewModelScope.launch {
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

    fun clearNotes() = editNotes { it.copy(notes = emptyList()) }

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
        // After a failed load the folder may still hold the user's sounds: never clean it then.
        val cleanUp = _notice.value == null
        samplesLoad = viewModelScope.launch {
            // Sounds read while the project closes are freed, not lost on the way.
            val prepared = finishOrRelease(Dispatchers.IO, { list: List<PadSamples.Prepared> -> list.forEach { it.native.close() } }) {
                padSamples.loadAll(files, cleanUp)
            }.orEmpty()
            prepared.forEach(padSamples::register)
            pushPads()
            pushLive()
            pushAudio()
            project.value.instrumentTracks.forEach { pushSampled(id = it.id) }
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
        val frames = OfflineExport.framesFor(bars, p.tempoBpm, p.beatsPerBar)
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
            padSamples.add(pcm.withoutTail(), "Micro ${java.time.LocalTime.now().withNano(0)}")
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

    private val _meters = MutableStateFlow(FloatArray(NativeEngine.MIXER_CHANNELS + 1))
    /** Peak per mixer channel (the master's at [MASTER]), then the limiter gain. */
    val meters: StateFlow<FloatArray> = _meters.asStateFlow()

    /** Polls meter peaks about 20 times a second while the mixer is visible. */
    suspend fun pollMeters() {
        if (!NativeEngine.isAvailable) return
        val n = NativeEngine.MIXER_CHANNELS
        val decayed = FloatArray(n + 1)
        while (true) {
            val fresh = FloatArray(n + 1)
            NativeEngine.mixerMeters(fresh)
            // Peak hold with a gentle fall so the bars are readable.
            for (i in 0 until n) decayed[i] = maxOf(fresh[i], decayed[i] * 0.85f)
            decayed[n] = fresh[n]
            _meters.value = decayed.copyOf()
            kotlinx.coroutines.delay(50)
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
            if (open && _audioRecording.value == null) withContext(micThread + kotlinx.coroutines.NonCancellable) { NativeEngine.micClose() }
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

    private fun pushMixer(p: Project = project.value) {
        if (!NativeEngine.isAvailable) return
        // Only the strips in use: the engine's command queue has room for whole projects.
        val pads = (0 until DrumPattern.PAD_COUNT).filter { p.mixer.channels.getOrNull(Mixer.padChannel(it))?.hasOwnFx() == true }
        val used = setOf(0, 1, 3) + p.instrumentTracks.map { InstrumentTrack.mixerChannel(it.id) } + p.audioTracks.map { AudioTrack.mixerChannel(it.id) } +
            pads.map { Mixer.padChannel(it) }
        p.mixer.channels.take(NativeEngine.MIXER_CHANNELS).forEachIndexed { i, c ->
            if (i == MASTER) return@forEachIndexed  // the master's index: its settings are below
            if (i !in used) {
                // A strip of a track gone (deleted, undone, another project): no solo or mute may
                // linger in the engine, where a solo would silence everything else.
                if (i < Mixer.FIRST_PAD) {
                    NativeEngine.setChannelMute(i, false)
                    NativeEngine.setChannelSolo(i, false)
                }
                return@forEachIndexed
            }
            NativeEngine.setChannelVolume(i, c.volume)
            NativeEngine.setChannelPan(i, c.pan)
            NativeEngine.setChannelMute(i, c.mute)
            NativeEngine.setChannelSolo(i, c.solo)
            pushInserts(i, c.inserts)
            c.sends.forEachIndexed { bus, level -> NativeEngine.setSend(i, bus, level) }
            c.macros.forEachIndexed { m, v -> NativeEngine.setMacro(i, m, v) }
        }
        pushInserts(MASTER, p.mixer.master)
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

    // --- Quick sound play: macros, pad XY, ready-made effects -------------------------------

    /** One quick control of a track (a knob drag is one undo step). */
    fun setMacro(ch: Int, macro: Int, value: Float) = setMacros(ch, mapOf(macro to value), "macro-$ch-$macro")

    /** The pad XY: two macros at once (one drag, one undo step). */
    fun setMacroPair(ch: Int, x: Int, xValue: Float, y: Int, yValue: Float) = setMacros(ch, mapOf(x to xValue, y to yValue), "macro-xy-$ch")

    private fun setMacros(ch: Int, values: Map<Int, Float>, key: String) {
        if (ch !in 0 until Mixer.CHANNELS || ch == MASTER) return
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
        session.edit { p ->
            val channels = p.mixer.channels.toMutableList()
            channels[ch] = channels[ch].withFx(preset)
            p.copy(mixer = p.mixer.copy(channels = channels))
        }
        if (NativeEngine.isAvailable) {
            val c = project.value.mixer.channels[ch]
            pushInserts(ch, c.inserts)
            c.sends.forEachIndexed { bus, level -> NativeEngine.setSend(ch, bus, level) }
            c.macros.forEachIndexed { m, v -> NativeEngine.setMacro(ch, m, v) }
        }
        AppLog.add("Effet « ${preset.name} » sur la voie $ch")
        routePad(ch)
    }

    fun setSend(ch: Int, bus: Int, level: Float) {
        val v = level.coerceIn(0f, 1f)
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
        if (ch != MASTER) routePad(ch)
    }

    // --- Subtractive synth -------------------------------------------------------------------

    private val _keyboardOctave = MutableStateFlow(4)
    val keyboardOctave: StateFlow<Int> = _keyboardOctave.asStateFlow()

    fun shiftKeyboardOctave(delta: Int) {
        _keyboardOctave.value = (_keyboardOctave.value + delta).coerceIn(1, 7)
    }

    /** Keys down on the keyboard and the track each one plays (released there, whatever is selected since). */
    private val keysDown = HashMap<Int, Int>()

    fun synthNoteOn(note: Int, velocity: Float = 0.85f) {
        keysDown[note] = track
        if (NativeEngine.isAvailable) NativeEngine.synthNoteOn(note, velocity, track)
        recordNoteOn(note, velocity)
    }

    fun synthNoteOff(note: Int) {
        val on = keysDown.remove(note) ?: track
        if (NativeEngine.isAvailable) NativeEngine.synthNoteOff(note, on)
        recordNoteOff(note)
    }

    // --- Live recording: what is played (keyboard, MIDI, pads) is written into the loop --------

    private val _recording = MutableStateFlow(false)
    /** True while armed: notes go into the piano roll, pad hits into the drum steps. */
    val recording: StateFlow<Boolean> = _recording.asStateFlow()
    private var recordBefore: Project? = null
    private val heldNotes = HashMap<Int, Triple<Int, Float, Int>>()  // pitch -> (start tick, velocity, track)

    /** Starts (with a bar of count-in when stopped) or ends a take; the whole take is one undo step. */
    fun toggleRecord() {
        if (_recording.value) {
            stopRecordingTake()
            return
        }
        recordBefore = project.value
        heldNotes.clear()
        _recording.value = true
        AppLog.add("Enregistrement en direct")
        if (!_transport.value.playing) {
            _transport.value = _transport.value.copy(playing = true)
            if (NativeEngine.isAvailable) NativeEngine.play(com.pocketstudio.app.settings.SettingsStore.state.value.recordCountIn)
            PlaybackService.start(getApplication())
        }
    }

    private fun stopRecordingTake() {
        // Keys still down when the take ends keep the length they have so far.
        heldNotes.keys.toList().forEach { recordNoteOff(it) }
        _recording.value = false
        recordBefore?.let { session.commitQuietEdits(it) }
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

    private val favoritesFile = File(app.filesDir, "favorites.json")
    private val _favorites = MutableStateFlow(
        runCatching { if (favoritesFile.exists()) ProjectJson.decodeFavorites(favoritesFile.readText()) else Favorites() }
            .onFailure { AppLog.add("Favoris illisibles : ${it.message}") }.getOrDefault(Favorites()),
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
                runCatching {
                    val latest = _favorites.value
                    val tmp = File(favoritesFile.path + ".tmp")
                    java.io.FileOutputStream(tmp).use { out ->
                        out.write(ProjectJson.encodeFavorites(latest).toByteArray(Charsets.UTF_8))
                        out.fd.sync()
                    }
                    if (!tmp.renameTo(favoritesFile)) error("renommage impossible")
                }.onFailure { AppLog.add("Favoris non enregistrés : ${it.message}") }
            }
        }
    }

    private fun <T> addFavorite(list: List<T>, add: () -> Favorites) {
        if (list.size >= Favorites.MAX) {
            _notice.value = "Tu as déjà ${Favorites.MAX} favoris de ce son : supprime-en un pour en garder un nouveau."
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
        _notice.value = "Favori « $name » supprimé."
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
        if (!playing && _recording.value) stopRecordingTake()
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
                project.map { it.name to it.tempoBpm }.distinctUntilChanged(),
                _recording,
                _audioRecording.map { it != null }.distinctUntilChanged(),
            ) { song, live, take -> Triple(song, live, take) }.collect { (song, live, take) ->
                val recording = live || take
                if (!recording) since = 0L else if (since == 0L) since = System.currentTimeMillis()
                PlaybackService.update(PlaybackStatus(song.first, song.second, recording, since, microphone = take))
            }
        }
        viewModelScope.launch {
            PlaybackService.stops.collect { why ->
                AppLog.add("Arrêt : $why")
                // A microphone take ends and is kept; otherwise the song stops.
                if (_audioRecording.value != null) stopAudioRecording()
                else if (_transport.value.playing) togglePlay()
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
        if (_recording.value || _audioRecording.value != null || _takeSaving.value) return
        session.undo()
        syncEngine()
        loadTrackFonts()
        reloadMissingSamples()
    }

    fun redo() {
        if (_recording.value || _audioRecording.value != null || _takeSaving.value) return
        session.redo()
        syncEngine()
        loadTrackFonts()
        reloadMissingSamples()
    }

    /** Writes pending changes now (the app is going to the background). */
    fun flush() {
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
    val export: StateFlow<ExportUi> = _export.asStateFlow()
    private var running: OfflineExport? = null

    fun setExportBars(bars: Int) {
        _export.value = _export.value.copy(bars = bars.coerceIn(1, 64))
    }

    fun setExportWhat(what: ExportWhat) {
        _export.value = _export.value.copy(what = what)
    }

    fun setExportFormat(format: ExportFormat) {
        _export.value = _export.value.copy(format = format)
    }

    /** Frames of the whole song (each part: its scene's length × its repeats). */
    private fun songFrames(p: Project): Long {
        val scenes = p.allScenes
        val steps = p.song.sumOf { (scenes.getOrNull(it.scene)?.stepsFor(p.stepsPerBar) ?: p.stepsPerBar).toLong() * it.repeats }
        return Math.round(steps * 15.0 / p.tempoBpm * OfflineExport.SAMPLE_RATE)  // a sixteenth = 15/bpm s
    }

    /**
     * Describes the project to an export engine. For the song, the first part's scene plays first
     * and each next scene is staged half a step into the part before it, swapping on its last bar line.
     */
    private fun describe(job: OfflineExport, p: Project, song: Boolean, soloChannel: Int?) {
        NativeEngine.captureInto(job) {
            val first = if (song) p.sceneView(p.song[0].scene) else p
            syncEngine(first)
            if (song) pushPart(p, p.song[0].scene, p.song[0])
            NativeEngine.setMetronome(false)
            if (soloChannel != null) {
                // A stem is its strip alone, whatever solos and mutes the mix was left with.
                for (ch in 0 until Mixer.FIRST_PAD) if (ch != MASTER) NativeEngine.setChannelSolo(ch, false)
                NativeEngine.setChannelMute(soloChannel, false)
                NativeEngine.setChannelSolo(soloChannel, true)
            }
            if (song) {
                val sixteenth = 15.0 / p.tempoBpm * OfflineExport.SAMPLE_RATE
                var stepsSoFar = 0L
                p.song.forEachIndexed { i, part ->
                    val steps = p.sceneSteps(part.scene) * part.repeats
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
            }
        }
    }

    /**
     * Renders the project offline (never the live engine): the loop, the whole song or each track,
     * into WAV (or M4A), then copies the files to Music › Pocket Studio.
     */
    fun exportWav() {
        if (!NativeEngine.isAvailable || running != null || exportJob != null) return
        exportCancelled = false
        val app = getApplication<Application>()
        val p = project.value
        val settings = _export.value
        val song = settings.what == ExportWhat.Song || (settings.what == ExportWhat.Stems && p.song.isNotEmpty())
        if (settings.what == ExportWhat.Song && p.song.isEmpty()) {
            _export.value = settings.copy(error = "Le morceau est vide : ajoute des scènes dans l'onglet Morceau.", files = emptyList())
            return
        }
        val frames = if (song) songFrames(p) else OfflineExport.framesFor(settings.bars, p.tempoBpm, p.beatsPerBar)
        // Each render: (label for the file and the screen, solo channel or null for the mix).
        val renders: List<Pair<String, Int?>> = when (settings.what) {
            ExportWhat.Stems -> STEM_EXPORTS.filter { (_, ch) -> ch != 3 || p.remix.setId != null || p.live.used > 0 } +
                // One file per added instrument track, named after it.
                p.tracks.map { t -> "piste${t.id + 1}-" + t.name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').take(16) to InstrumentTrack.mixerChannel(t.id) } +
                p.audioTracks.filter { it.clip != null }.map { t ->
                    "audio${t.id + 1}-" + t.name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').take(16) to AudioTrack.mixerChannel(t.id)
                }
            ExportWhat.Song -> listOf("morceau" to null)
            ExportWhat.Loop -> listOf("boucle" to null)
        }
        _export.value = settings.copy(running = true, progress = 0f, files = emptyList(), error = null, stage = null, loudness = null)
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
                    val job = OfflineExport()
                    describe(job, p, song, solo)
                    running = job
                    val wav = runCatching { WavExporter.newFile(app, "${p.name.take(28)}-$label") }.getOrElse { throw UserMessage("Impossible de préparer le fichier d'export.") }
                    _export.value = _export.value.copy(stage = if (renders.size > 1) "${label.replaceFirstChar { it.uppercase() }} (${index + 1}/${renders.size})" else null)
                    val poll = launch {
                        while (true) {
                            _export.value = _export.value.copy(progress = (index + job.progress) / renders.size)
                            kotlinx.coroutines.delay(100)
                        }
                    }
                    val result = try {
                        withContext(Dispatchers.IO) { job.render(wav, frames, TAIL_FRAMES) }
                    } finally {
                        poll.cancel()
                        // Closed on the main thread, after the render returned.
                        job.close()
                        running = null
                    }
                    when (result) {
                        ExportResult.Cancelled -> { wav.delete(); throw kotlinx.coroutines.CancellationException("annulé") }
                        ExportResult.IoError -> { wav.delete(); throw UserMessage("L'écriture du fichier a échoué (stockage plein ?).") }
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
                        val name = if (settings.format == ExportFormat.M4a) "Compression M4A" else "Conversion FLAC"
                        val part = if (renders.size > 1) " (${index + 1}/${renders.size})" else ""
                        _export.value = _export.value.copy(stage = name + part)
                        val packed = File(wav.parentFile, wav.nameWithoutExtension + "." + settings.format.extension)
                        withContext(Dispatchers.IO) {
                            try {
                                if (settings.format == ExportFormat.M4a) {
                                    AacEncoder.encode(wav, packed) { exportCancelled || !isActive }
                                } else {
                                    FlacEncoder.encode(wav, packed, { exportCancelled || !isActive }) { p ->
                                        _export.value = _export.value.copy(stage = "$name ${(p * 100).roundToInt()} %$part")
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
                _export.value = _export.value.copy(running = false, files = done, savedInMusic = musicOk, stage = null, loudness = loudness)
                AppLog.add("Export ${settings.what} ${settings.format} : ${done.size} fichier(s), ${frames / OfflineExport.SAMPLE_RATE} s")
            } catch (e: kotlinx.coroutines.CancellationException) {
                done.forEach { it.delete() }
                _export.value = _export.value.copy(running = false, files = emptyList(), stage = null)
            } catch (e: Exception) {
                done.forEach { it.delete() }
                AppLog.add("Export : échec (${e.message})")
                _export.value = _export.value.copy(running = false, files = emptyList(), stage = null,
                    error = if (e is UserMessage) e.message else "L'export a échoué : ${e.message ?: e.javaClass.simpleName}")
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
                    val wav = WavExporter.newFile(app, p.name + if (p.song.isNotEmpty()) "-morceau" else "")
                    val mid = File(wav.parentFile, wav.nameWithoutExtension + ".mid")
                    mid.writeBytes(com.pocketstudio.model.MidiFile.export(p))
                    mid
                }
                val music = withContext(Dispatchers.IO) { WavExporter.copyToMusic(app, file, "audio/midi") }
                _export.value = _export.value.copy(files = listOf(file), savedInMusic = music, error = null, loudness = null)
                AppLog.add("Export MIDI : ${file.length()} octets")
            } catch (e: Exception) {
                _export.value = _export.value.copy(error = "L'export MIDI a échoué : ${e.message}")
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
                            if (out.size() > MAX_MIDI_BYTES) throw UserMessage("Ce fichier MIDI est trop gros.")
                        }
                        out.toByteArray()
                    } ?: throw UserMessage("Le fichier n'a pas pu être ouvert.")
                    com.pocketstudio.model.MidiFile.read(bytes)
                }
                if (content.notes.isEmpty()) throw UserMessage("Aucune note dans ce fichier MIDI.")
                session.edit { com.pocketstudio.model.MidiFile.importInto(it, content) }
                syncEngine()
                AppLog.add("Import MIDI : ${content.notes.size} notes")
                _transcription.value = SampleTask.Idle
            } catch (e: Exception) {
                _transcription.value = SampleTask.Failed(if (e is UserMessage) e.message!! else "Ce fichier MIDI n'a pas pu être lu (${e.message}).")
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
        // A take in progress: the microphone closes and its buffer is freed.
        recordHandle.takeIf { it != 0L }?.let { finishInBackground(it) }
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
        PlaybackService.stop(getApplication())
    }

    companion object {
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
        /** Stems export: one render per mixer channel. */
        /** A song automation lane spans at least this many bars (the song can be shorter or empty). */
        const val SONG_LANE_MIN_BARS = 8
        /** Longest microphone take (34 MB of memory). */
        const val MAX_AUDIO_SECONDS = 180
        private val STEM_EXPORTS = listOf("batterie" to 0, "acide" to 1, "synthe" to 2, "remix" to 3)
        private val AUDIO_EXTENSIONS = setOf("wav", "mp3", "flac", "m4a", "aac", "ogg", "opus", "aif", "aiff")
        /** Two seconds after the last bar so releases and the acid tail ring out. */
        const val TAIL_FRAMES = 2L * OfflineExport.SAMPLE_RATE
        private val STEM_LABELS = listOf("Batterie", "Basse", "Autres", "Voix")
        private const val OVERVIEW_BUCKETS = 400
        /** Insert "channel" index of the master. */
        const val MASTER = NativeEngine.MASTER
    }
}
