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
        pushPads(p)
        pushNotes(p.synthNotes)
        pushInstrument(p)
        pushRemix(p)
    }

    // --- Remix: a separated song played along with the project ---------------------------------

    private var stems: NativeStems? = null
    private var stemsId: String? = null
    private var stemsJob: kotlinx.coroutines.Job? = null
    private val _remixLoading = MutableStateFlow(false)
    val remixLoading: StateFlow<Boolean> = _remixLoading.asStateFlow()

    private fun pushRemix(p: Project = project.value) {
        if (!NativeEngine.isAvailable) return
        val loaded = stems?.takeIf { stemsId == p.remix.setId }
        NativeEngine.setStems(loaded)
        NativeEngine.setStemStart(p.remix.startFrame(OfflineExport.SAMPLE_RATE))
        p.remix.stems.forEachIndexed { i, m ->
            NativeEngine.setStemParam(i, 0, m.volume)
            NativeEngine.setStemParam(i, 1, if (m.mute) 1f else 0f)
            NativeEngine.setStemParam(i, 2, if (m.solo) 1f else 0f)
        }
        val wanted = p.remix.setId
        if (loaded == null && stems != null) {
            // Another song (or none): the engine let go of the old one, free it now, not after the next load.
            stems?.close()
            stems = null
            stemsId = null
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
        session.edit("remix") { p -> p.copy(remix = Remix()) }
        StemStore.pin(getApplication(), null)
        pushRemix()
        stems?.close()
        stems = null
        stemsId = null
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
        val font = soundFont ?: return
        if (_sampleTask.value is SampleTask.Busy) return
        _sampleTask.value = SampleTask.Busy("Kit ${kit.name}…")
        viewModelScope.launch {
            try {
                val pads = ArrayList<Pair<Int, PadSample>>()
                InstrumentLibrary.KIT_NOTES.forEachIndexed { pad, note ->
                    // Rendering shares the font's reference count: main thread, one note at a time.
                    val audio = font.renderNote(kit, note)
                    kotlinx.coroutines.yield()
                    if (audio.size < 480) return@forEachIndexed
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
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _sampleTask.value = SampleTask.Failed("Le kit n'a pas pu être chargé (${e.message}).")
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
        val files = project.value.drums.lanes.mapNotNull { it.sample?.file }.toSet()
        // After a failed load the folder may still hold the user's sounds: never clean it then.
        val cleanUp = _notice.value == null
        viewModelScope.launch {
            val prepared = withContext(Dispatchers.IO) { padSamples.loadAll(files, cleanUp) }
            prepared.forEach(padSamples::register)
            pushPads()
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
    fun importFolder(tree: Uri) {
        if (_sampleTask.value is SampleTask.Busy || _sampleTask.value is SampleTask.Recording) return
        val first = _transport.value.selectedPad
        val app = getApplication<Application>()
        _sampleTask.value = SampleTask.Busy("Lecture du dossier…")
        viewModelScope.launch {
            try {
                val files = withContext(Dispatchers.IO) { audioFilesIn(app, tree) }.take(DrumPattern.PAD_COUNT - first)
                if (files.isEmpty()) throw UserMessage("Aucun son dans ce dossier (MP3, WAV, FLAC, M4A, OGG).")
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
                _sampleTask.value = SampleTask.Failed("Le dossier n'a pas pu être lu (${e.message}).")
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

    private fun sampleJob(busy: String?, work: suspend () -> Pair<PadSample, PadSamples.Prepared>) {
        val lane = _transport.value.selectedPad
        if (busy != null) _sampleTask.value = SampleTask.Busy(busy)
        viewModelScope.launch {
            try {
                val (sample, prepared) = withContext(Dispatchers.IO) { work() }
                padSamples.register(prepared)
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
        if (project.value.drums.lanes.any { (it.sample?.fitBeats ?: 0) > 0 }) pushPads()
    }

    fun tapTempo() {
        val bpm = tapTempo.tap(System.currentTimeMillis()) ?: return
        session.edit("tempo") { it.copy(tempoBpm = Math.round(bpm).toDouble()) }
        if (NativeEngine.isAvailable) NativeEngine.setTempo(project.value.tempoBpm)
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
        if (NativeEngine.isAvailable) NativeEngine.stopTransport()
        PlaybackService.stop(getApplication())
    }

    companion object {
        const val PROJECT_ID = "projet-1"
        private val AUDIO_EXTENSIONS = setOf("wav", "mp3", "flac", "m4a", "aac", "ogg", "opus", "aif", "aiff")
        /** Two seconds after the last bar so releases and the acid tail ring out. */
        const val TAIL_FRAMES = 2L * OfflineExport.SAMPLE_RATE
        private val STEM_LABELS = listOf("Batterie", "Basse", "Autres", "Voix")
    }
}
