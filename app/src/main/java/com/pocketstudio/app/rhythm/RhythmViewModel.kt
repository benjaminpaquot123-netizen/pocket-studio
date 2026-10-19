package com.pocketstudio.app.rhythm

import com.pocketstudio.app.report.AppLog
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import android.provider.OpenableColumns
import com.pocketstudio.app.audio.AudioDecoder
import com.pocketstudio.app.audio.MicRecorder
import com.pocketstudio.app.export.WavExporter
import com.pocketstudio.app.playback.PlaybackService
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.AcidGenerator
import com.pocketstudio.model.AcidPattern
import com.pocketstudio.model.AcidStep
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.Hit
import com.pocketstudio.model.PadSample
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

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
data class LiveUi(val currentStep: Int = -1, val litPads: Set<Int> = emptySet())

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
            _live.value = LiveUi(NativeEngine.currentStep(), lit)
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
        val p = project.value
        NativeEngine.setTempo(p.tempoBpm)
        NativeEngine.setSwing(p.swing)
        NativeEngine.setPatternSteps(p.drums.steps)
        p.drums.lanes.forEachIndexed { lane, l ->
            for (step in 0 until DrumPattern.MAX_STEPS) {
                NativeEngine.setDrumHit(lane, step, l.hits[step]?.velocity ?: 0f)
            }
        }
        NativeEngine.setMetronome(_transport.value.metronome)
        pushAcid(p.acid)
        pushSynth(p.synth)
        pushMixer(p)
        padSamples.pushPads(p.drums.lanes.map { it.sample })
    }

    // --- Pad samples (import, microphone) --------------------------------------------------

    val padSamples = PadSamples(File(store.folder(PROJECT_ID), "samples"))
    val sampleInfo: StateFlow<Map<String, SampleInfo>> = padSamples.info
    private val _sampleTask = MutableStateFlow<SampleTask>(SampleTask.Idle)
    val sampleTask: StateFlow<SampleTask> = _sampleTask.asStateFlow()
    private var recorder: MicRecorder? = null

    private fun loadPadSamples() {
        val files = project.value.drums.lanes.mapNotNull { it.sample?.file }.toSet()
        viewModelScope.launch {
            val prepared = withContext(Dispatchers.IO) { padSamples.loadAll(files) }
            prepared.forEach(padSamples::register)
            padSamples.pushPads(project.value.drums.lanes.map { it.sample })
        }
    }

    /** Decodes an audio file from the phone and puts it on the selected pad. */
    fun importSample(uri: Uri) = sampleJob("Lecture du fichier…") {
        val app = getApplication<Application>()
        val name = PadSamples.nameFrom(displayName(app, uri))
        val pcm = try {
            AudioDecoder.decode(app, uri)
        } catch (e: AudioDecoder.TooLongException) {
            throw UserMessage("Ce fichier dure plus d'une minute : choisis un son plus court (la découpe des morceaux arrive bientôt).")
        } catch (e: Exception) {
            throw UserMessage("Ce fichier n'a pas pu être lu (${e.message ?: "format inconnu"}).")
        }
        AppLog.add("Import « $name » : ${pcm.channels} canal(aux), ${pcm.sampleRate} Hz, ${"%.1f".format(pcm.seconds)} s")
        padSamples.add(pcm, name)
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
        padSamples.pushPads(project.value.drums.lanes.map { it.sample })
    }

    fun dismissSampleError() {
        if (_sampleTask.value is SampleTask.Failed) _sampleTask.value = SampleTask.Idle
    }

    private class UserMessage(message: String) : Exception(message)

    private fun sampleJob(busy: String?, work: suspend () -> Pair<PadSample, PadSamples.Prepared>) {
        val lane = _transport.value.selectedPad
        if (busy != null) _sampleTask.value = SampleTask.Busy(busy)
        viewModelScope.launch {
            try {
                val (sample, prepared) = withContext(Dispatchers.IO) { work() }
                padSamples.register(prepared)
                session.edit { p -> p.copy(drums = p.drums.withSample(lane, sample)) }
                padSamples.pushPads(project.value.drums.lanes.map { it.sample })
                _sampleTask.value = SampleTask.Idle
                if (NativeEngine.isAvailable) NativeEngine.triggerPad(lane, 0.9f)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                recorder = null
                AppLog.add("Son du pad : échec (${e.message})")
                _sampleTask.value = SampleTask.Failed(
                    if (e is UserMessage) e.message!! else "Ça n'a pas marché : ${e.message ?: e.javaClass.simpleName}",
                )
            }
        }
    }

    private fun displayName(app: Application, uri: Uri): String? = runCatching {
        app.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull()

    // --- Mixer -----------------------------------------------------------------------------

    private val _meters = MutableStateFlow(FloatArray(5))
    val meters: StateFlow<FloatArray> = _meters.asStateFlow()

    /** Polls meter peaks about 20 times a second while the mixer is visible. */
    suspend fun pollMeters() {
        if (!NativeEngine.isAvailable) return
        val decayed = FloatArray(5)
        while (true) {
            val fresh = FloatArray(5)
            NativeEngine.mixerMeters(fresh)
            // Peak hold with a gentle fall so the bars are readable.
            for (i in 0 until 4) decayed[i] = maxOf(fresh[i], decayed[i] * 0.85f)
            decayed[4] = fresh[4]
            _meters.value = decayed.copyOf()
            kotlinx.coroutines.delay(50)
        }
    }

    fun setChannelVolume(ch: Int, v: Float) = editChannel(ch, "vol") { it.copy(volume = v) }
    fun setChannelPan(ch: Int, pan: Float) = editChannel(ch, "pan") { it.copy(pan = pan) }
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
        if (playing) NativeEngine.play(if (t.countIn) 1 else 0) else NativeEngine.stopTransport()
        val app = getApplication<Application>()
        AppLog.add(if (playing) "Lecture (${project.value.tempoBpm.toInt()} BPM)" else "Stop")
        if (playing) PlaybackService.start(app) else PlaybackService.stop(app)
    }

    /** True while the activity is on screen; set by the UI. */
    var visible = true

    /** The engine must keep running in the background only while the transport plays. */
    val keepsRunningInBackground: Boolean get() = _transport.value.playing

    init {
        loadPadSamples()
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
    }

    fun tapTempo() {
        val bpm = tapTempo.tap(System.currentTimeMillis()) ?: return
        session.edit("tempo") { it.copy(tempoBpm = Math.round(bpm).toDouble()) }
        if (NativeEngine.isAvailable) NativeEngine.setTempo(project.value.tempoBpm)
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
        running?.cancel()
        recorder?.stop()
        padSamples.close()
        if (NativeEngine.isAvailable) NativeEngine.stopTransport()
        PlaybackService.stop(getApplication())
    }

    companion object {
        const val PROJECT_ID = "projet-1"
        /** Two seconds after the last bar so releases and the acid tail ring out. */
        const val TAIL_FRAMES = 2L * OfflineExport.SAMPLE_RATE
    }
}
