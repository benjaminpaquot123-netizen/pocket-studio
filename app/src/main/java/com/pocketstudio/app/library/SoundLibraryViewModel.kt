package com.pocketstudio.app.library

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocketstudio.app.audio.AudioDecoder
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.audio.PcmPlayer
import com.pocketstudio.app.download.VerifiedDownload
import com.pocketstudio.app.report.AppLog
import com.pocketstudio.app.rhythm.DrumKitInfo
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.PackIndex
import com.pocketstudio.model.PackManifest
import com.pocketstudio.model.PackSound
import com.pocketstudio.model.PadSample
import com.pocketstudio.model.Project
import com.pocketstudio.model.SoundPacks
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.StarterWizard.Role
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

/** A sound of the library: a voice of the built-in kit, or a file of a downloaded pack. */
sealed interface SoundRef {
    data class Kit(val voice: Int) : SoundRef
    data class Pack(val packId: String, val sound: PackSound) : SoundRef
    /** A melodic part of the assistant played with [sound], over the beat and the parts before it. */
    data class Melodic(val part: StarterWizard.Part, val sound: StarterWizard.PartSound) : SoundRef
    /** A mood of the assistant: its four chords (bass and pad) over the style's beat. */
    data class MoodDemo(val mood: StarterWizard.Mood) : SoundRef
    /** The beat chosen so far, up to [role] played with [sound]: the line heard in its groove. */
    data class LineDemo(val role: Role, val sound: SoundOption) : SoundRef
}

data class SoundOption(val ref: SoundRef, val name: String)

/** "Kicks · House" (a pack's sounds of one kind), or the built-in kit's voices for that line. */
data class SoundCategory(val id: String, val title: String, val options: List<SoundOption>)

sealed interface IndexState {
    data object Loading : IndexState
    data object Unavailable : IndexState
    data class Ready(val index: PackIndex) : IndexState
    data class Failed(val message: String) : IndexState
}

data class PacksUi(
    val index: IndexState = IndexState.Loading,
    val installed: List<PackManifest> = emptyList(),
    /** Pack id → download progress 0..1. */
    val downloading: Map<String, Float> = emptyMap(),
    val message: String? = null,
)

/** One line of the beat in the assistant: used or not, the category shown, the chosen sound ([chosen]: by the user). */
data class WizardRow(val enabled: Boolean, val category: String, val sound: SoundOption?, val chosen: Boolean = false)

/** One melodic part in the assistant: used or not, and its sound ([chosen]: by the user). */
data class PartRow(val enabled: Boolean, val sound: StarterWizard.PartSound, val chosen: Boolean = false)

data class WizardUi(
    val open: Boolean = false,
    /** Index in [steps]. */
    val step: Int = 0,
    val styleId: String? = null,
    val rows: Map<Role, WizardRow> = emptyMap(),
    /** null: no melody, the beat only (the key stays free). */
    val mood: StarterWizard.Mood? = null,
    val parts: Map<StarterWizard.Part, PartRow> = emptyMap(),
    val creating: Boolean = false,
    val error: String? = null,
) {
    val style: StarterWizard.Style? get() = styleId?.let(StarterWizard::style)
    val steps: List<StarterWizard.Step> get() = StarterWizard.steps(style)
    val lastStep: Int get() = steps.size - 1
    val current: StarterWizard.Step get() = steps[step.coerceIn(0, lastStep)]

    /** Which scene each chosen line and part will land in (what "Créer le morceau" builds). */
    val plan: StarterWizard.ScenePlan get() {
        val lines = style?.lines.orEmpty().map { it.role }.filter { r -> rows[r]?.let { it.enabled && it.sound != null } == true }.toSet()
        val used = if (mood == null) emptySet() else parts.filterValues { it.enabled }.keys
        return StarterWizard.scenePlan(lines, used, acid = mood != null && style?.melody?.acid != null)
    }
}

const val KIT_CATEGORY = "kit"
private const val PACK_PREFIX = "pack:"

/**
 * The line's sound categories: the pack the style takes this line from (a machine such as the
 * TR-909 for rave techno) when downloaded, then the style's own pack, then the built-in kit, then
 * the other packs that have that kind of sound.
 */
fun soundCategories(role: Role, styleId: String?, installed: List<PackManifest>): List<SoundCategory> {
    val label = SoundPacks.KIND_LABELS[role.id] ?: role.title
    val kit = SoundCategory(KIT_CATEGORY, "Sons intégrés", role.builtIn.map { SoundOption(SoundRef.Kit(it), DrumKitInfo.names[it]) })
    val packs = installed.mapNotNull { m ->
        val sounds = m.ofKind(role.id)
        if (sounds.isEmpty()) null
        else SoundCategory(PACK_PREFIX + m.id, "$label · ${m.name}", sounds.map { SoundOption(SoundRef.Pack(m.id, it), "${it.name} · ${m.name}") })
    }
    val machine = styleId?.let(StarterWizard::style)?.kit?.get(role)
    val first = packs.filter { it.id == PACK_PREFIX + machine }
    val own = packs.filter { it.id == PACK_PREFIX + styleId } - first.toSet()
    return first + own + kit + (packs - own.toSet() - first.toSet())
}

/** Every category of a kind of sound for the library: the built-in kit's voices when the kit has that kind, then each pack. */
fun libraryCategories(kind: String, installed: List<PackManifest>): List<SoundCategory> {
    val role = Role.entries.firstOrNull { it.id == kind }
    if (role != null) return soundCategories(role, null, installed)
    val label = SoundPacks.KIND_LABELS[kind] ?: kind
    return installed.mapNotNull { m ->
        val sounds = m.ofKind(kind)
        if (sounds.isEmpty()) null
        else SoundCategory(PACK_PREFIX + m.id, "$label · ${m.name}", sounds.map { SoundOption(SoundRef.Pack(m.id, it), "${it.name} · ${m.name}") })
    }
}

/**
 * The sound library (built-in kit and downloaded packs, with previews) and the beginner's
 * assistant that builds a new song from it.
 */
class SoundLibraryViewModel(app: Application) : AndroidViewModel(app) {
    private val _packs = MutableStateFlow(PacksUi())
    val packs: StateFlow<PacksUi> = _packs.asStateFlow()

    private val _wizard = MutableStateFlow(WizardUi())
    val wizard: StateFlow<WizardUi> = _wizard.asStateFlow()

    private val _playing = MutableStateFlow<SoundRef?>(null)
    /** The sound being previewed, if any. */
    val playing: StateFlow<SoundRef?> = _playing.asStateFlow()

    private val _preparing = MutableStateFlow<SoundRef?>(null)
    /** The excerpt being rendered before it plays (a second or so), if any. */
    val preparing: StateFlow<SoundRef?> = _preparing.asStateFlow()
    private var previewJob: Job? = null

    private val player = PcmPlayer { _playing.value = null }
    private val kitCache = HashMap<Int, Pcm>()
    private val cancelled = HashMap<String, AtomicBoolean>()

    // --- Packs ---------------------------------------------------------------------------------

    fun refreshPacks() {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val idle = _packs.value.downloading.isEmpty()
            val installed = withContext(Dispatchers.IO) {
                if (idle) PackStore.cleanLeftovers(app)
                PackStore.installed(app)
            }
            _packs.update { it.copy(installed = installed) }
            if (_packs.value.index is IndexState.Ready) return@launch
            _packs.update { it.copy(index = IndexState.Loading) }
            val index = withContext(Dispatchers.IO) {
                runCatching { PackStore.fetchIndex(app) }.fold(
                    { it?.let { i -> IndexState.Ready(i) } ?: IndexState.Unavailable },
                    { IndexState.Failed("La liste des packs n'a pas pu être chargée (réseau ?).") },
                )
            }
            _packs.update { it.copy(index = index) }
        }
    }

    fun downloadPack(id: String) {
        val entry = (_packs.value.index as? IndexState.Ready)?.index?.packs?.firstOrNull { it.id == id } ?: return
        if (id in _packs.value.downloading) return
        val stop = AtomicBoolean(false).also { cancelled[id] = it }
        _packs.update { it.copy(downloading = it.downloading + (id to 0f), message = null) }
        viewModelScope.launch {
            val app = getApplication<Application>()
            val error = withContext(Dispatchers.IO + NonCancellable) {
                runCatching {
                    PackStore.install(app, entry, { p -> _packs.update { it.copy(downloading = it.downloading + (id to p)) } }, stop::get)
                }.exceptionOrNull()
            }
            cancelled.remove(id)
            val installed = withContext(Dispatchers.IO) { PackStore.installed(app) }
            _packs.update {
                it.copy(
                    installed = installed,
                    downloading = it.downloading - id,
                    message = when (error) {
                        null -> null
                        is VerifiedDownload.CancelledException -> null
                        else -> "Le pack « ${entry.name} » n'a pas pu être téléchargé : ${error.message ?: "erreur"}.".also { m -> AppLog.add(m) }
                    },
                )
            }
            if (error == null) {
                AppLog.add("Pack « ${entry.name} » installé")
                refreshWizardDefaults()
            }
        }
    }

    fun cancelDownload(id: String) {
        cancelled[id]?.set(true)
    }

    /** Frees the space; projects keep their own copies of the sounds they use. */
    fun deletePack(id: String) {
        stopPreview()
        viewModelScope.launch {
            val app = getApplication<Application>()
            val installed = withContext(Dispatchers.IO) {
                PackStore.delete(app, id)
                PackStore.installed(app)
            }
            _packs.update { it.copy(installed = installed) }
            // Lines of the assistant that pointed at the deleted pack go back to a sound that exists.
            _wizard.update { w ->
                w.copy(rows = w.rows.mapValues { (role, row) ->
                    if (row.category == PACK_PREFIX + id) defaultRow(role, w.styleId, installed).copy(enabled = row.enabled) else row
                })
            }
        }
    }

    fun dismissMessage() = _packs.update { it.copy(message = null) }

    /** A line for the library's notice spot (shown at the top of the library). */
    fun notice(text: String) = _packs.update { it.copy(message = text) }

    // --- Previews ------------------------------------------------------------------------------

    fun preview(ref: SoundRef) {
        if (_playing.value == ref || _preparing.value == ref) {
            previewJob?.cancel()
            _preparing.value = null
            stopPreview()
            return
        }
        // The last touch wins: an excerpt still rendering for another sound is dropped.
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            _preparing.value = ref
            val pcm = try {
                load(ref)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.add("Écoute impossible : ${e.javaClass.simpleName}")
                null
            } catch (e: OutOfMemoryError) {
                AppLog.add("Écoute impossible : mémoire pleine")
                null
            } finally {
                if (_preparing.value == ref) _preparing.value = null
            }
            if (pcm == null) {
                AppLog.add("Écoute impossible : $ref")
                return@launch
            }
            if (player.play(pcm)) _playing.value = ref
            else AppLog.add("Le téléphone a refusé de jouer l'extrait (${pcm.frames} images)")
        }
    }

    fun stopPreview() {
        previewJob?.cancel()
        _preparing.value = null
        player.stop()
    }

    /** The sound as audio for a pad or a track ([then] on the main thread; nothing if unreadable). */
    fun use(ref: SoundRef, then: (Pcm) -> Unit) {
        stopPreview()
        dismissMessage()
        viewModelScope.launch {
            val pcm = load(ref)
            if (pcm == null) {
                _packs.update { it.copy(message = "Ce son n'a pas pu être lu.") }
                return@launch
            }
            then(pcm)
        }
    }

    /** The sound as audio: the kit voice rendered once by the engine, or the pack file decoded. */
    private suspend fun load(ref: SoundRef): Pcm? = when (ref) {
        is SoundRef.Kit -> kitVoice(ref.voice)
        is SoundRef.Melodic -> partInContext(ref)
        is SoundRef.MoodDemo -> moodDemo(ref.mood)
        is SoundRef.LineDemo -> lineDemo(ref)
        is SoundRef.Pack -> withContext(Dispatchers.IO) {
            val file = PackStore.file(getApplication(), ref.packId, ref.sound)
            runCatching { AudioDecoder.decode(getApplication(), Uri.fromFile(file)) }
                .onFailure { AppLog.add("Son de pack illisible ${ref.sound.file} : ${it.message}") }
                .getOrNull()
        }
    }

    private suspend fun kitVoice(voice: Int): Pcm? {
        kitCache[voice]?.let { return it }
        if (!NativeEngine.isAvailable) return null
        val job = OfflineExport()
        val data = try {
            NativeEngine.captureInto(job) {
                NativeEngine.setTempo(120.0)
                NativeEngine.setPatternSteps(16)
                NativeEngine.setDrumHit(voice, 0, 1f)
            }
            withContext(Dispatchers.Default) { job.renderToArray(KIT_FRAMES, 0) }
        } finally {
            withContext(NonCancellable + Dispatchers.Main) { job.close() }
        }
        if (data.isEmpty()) return null
        return Pcm(trimSilence(data), 2, OfflineExport.SAMPLE_RATE).also { kitCache[voice] = it }
    }

    /** A pack sound for a melodic part: decoded, with the note it was recorded at (null: not readable). */
    private suspend fun packPartSound(sound: StarterWizard.PartSound): Pair<Pcm, Float?>? {
        val (packId, file) = sound.packFile ?: return null
        val pcm = withContext(Dispatchers.IO) {
            val f = PackStore.file(getApplication(), packId, com.pocketstudio.model.PackSound(file, "", ""))
            runCatching { AudioDecoder.decode(getApplication(), Uri.fromFile(f)) }.getOrNull()
        } ?: return null
        val root = withContext(Dispatchers.Default) {
            val ch = pcm.channels
            val mono = FloatArray(pcm.data.size / ch) { i -> (0 until ch).sumOf { c -> pcm.data[i * ch + c].toDouble() }.toFloat() / ch }
            com.pocketstudio.model.RootNote.detect(mono, pcm.sampleRate)
        }
        return pcm to root
    }

    /** The part with that sound over the beat and the parts before it, two bars in the chosen mood. */
    private suspend fun partInContext(ref: SoundRef.Melodic): Pcm? {
        val w = _wizard.value
        val style = w.style ?: return null
        val before = StarterWizard.Part.entries.takeWhile { it != ref.part }
            .mapNotNull { p -> w.parts[p]?.takeIf { it.enabled }?.let { p to it.sound } }
        return groove(style, chosenBeat(w, style), before + (ref.part to ref.sound), w.mood ?: style.melody.mood, bars = 2)
    }

    /** The mood's four chords on the style's parts (its 303 line, or bass and pad), over the beat as chosen so far. */
    private suspend fun moodDemo(mood: StarterWizard.Mood): Pcm? {
        val w = _wizard.value
        val style = w.style ?: return null
        val own = StarterWizard.Part.entries.filter { it in style.melody.parts }
        val shown = own.ifEmpty { if (style.melody.acid != null) emptyList() else listOf(StarterWizard.Part.Bass, StarterWizard.Part.Pad) }
        val parts = shown.map { p ->
            p to (w.parts[p]?.sound ?: StarterWizard.soundsFor(p, style).first())
        }
        return groove(style, chosenBeat(w, style), parts, mood, bars = 4)
    }

    /** The beat up to the line, that line played with the sound being tried. */
    private suspend fun lineDemo(ref: SoundRef.LineDemo): Pcm? {
        val w = _wizard.value
        val style = w.style ?: return null
        val upTo = style.lines.indexOfFirst { it.role == ref.role }
        if (upTo < 0) return null
        val beat = style.lines.take(upTo).mapNotNull { line -> enabledSound(w, line) } + (style.lines[upTo] to ref.sound.ref)
        return groove(style, beat, emptyList(), w.mood ?: style.melody.mood, bars = 2, acid = false)
    }

    private fun enabledSound(w: WizardUi, line: StarterWizard.Line): Pair<StarterWizard.Line, SoundRef>? =
        w.rows[line.role]?.takeIf { it.enabled }?.sound?.let { line to it.ref }

    private fun chosenBeat(w: WizardUi, style: StarterWizard.Style) = style.lines.mapNotNull { enabledSound(w, it) }

    /**
     * An excerpt of the song being built, rendered offline: the [beat] lines with their sounds
     * (pads), the [parts] on synth tracks 0, 1, 2 with their first notes in [mood], [bars] bars
     * at the style's tempo and swing. Null when a sound cannot be read or the engine is missing.
     */
    private suspend fun groove(
        style: StarterWizard.Style,
        beat: List<Pair<StarterWizard.Line, SoundRef>>,
        parts: List<Pair<StarterWizard.Part, StarterWizard.PartSound>>,
        mood: StarterWizard.Mood,
        bars: Int,
        /** The style's 303 line too (not while trying a drum line alone). */
        acid: Boolean = true,
    ): Pcm? {
        if (!NativeEngine.isAvailable) return null
        // Audio first: the kit voice already on its pad needs nothing, the others are decoded or rendered.
        val padAudio = beat.map { (line, ref) -> if (ref is SoundRef.Kit && ref.voice == line.role.lane) null else (load(ref) ?: return null) }
        val partAudio = parts.map { (_, sound) -> if (sound.sampled != null) (packPartSound(sound) ?: return null) else null }
        val harmony = StarterWizard.harmonyOf(style, mood).copy(lock = false)
        val steps = StarterWizard.drumSteps(style)
        val ticks = bars * 16 * com.pocketstudio.model.NoteClip.TICKS_PER_STEP
        val natives = ArrayList<com.pocketstudio.engine.NativeSample?>()
        val job = OfflineExport()
        val data = try {
            // Resampled to 48 kHz off the main thread, and always kept so that the finally below frees them.
            withContext(Dispatchers.Default + NonCancellable) {
                padAudio.forEach { pcm -> natives += pcm?.let { com.pocketstudio.engine.NativeSample.create(it.data, it.channels, it.sampleRate) } }
                partAudio.forEach { a -> natives += a?.let { (pcm, _) -> com.pocketstudio.engine.NativeSample.create(pcm.data, pcm.channels, pcm.sampleRate) } }
            }
            NativeEngine.captureInto(job) {
                NativeEngine.setTempo(style.bpm)
                NativeEngine.setSwing(style.swing)
                NativeEngine.setPatternSteps(steps)
                beat.forEachIndexed { i, (line, _) ->
                    val lane = line.role.lane
                    natives[i]?.let { NativeEngine.setPadSample(lane, it) }
                    StarterWizard.hits(line, steps).forEach { (step, hit) -> NativeEngine.setDrumHit(lane, step, hit.velocity) }
                }
                // The style's 303 line under its chords (acid house, rave techno).
                val recipe = style.melody.acid
                if (recipe != null && acid) {
                    val line = com.pocketstudio.model.acidLine(harmony, recipe.pattern, recipe.accents, recipe.slides)
                    NativeEngine.setAcidLength(line.length)
                    NativeEngine.setAcidRoot(line.root)
                    line.steps.forEachIndexed { i, s -> NativeEngine.setAcidStep(i, s.pack()) }
                    line.sound.values().forEachIndexed { i, v -> NativeEngine.setAcidParam(i, v) }
                }
                parts.forEachIndexed { track, (part, sound) ->
                    NativeEngine.setTrackOn(track, true)  // a fresh engine only plays track 0
                    sound.synth.sanitized().values.forEachIndexed { i, v -> NativeEngine.setSynthParam(i, v, track) }
                    sound.plaits?.let { p -> p.sanitized().engineValues().forEachIndexed { i, v -> NativeEngine.setPlaitsParam(i, v, track) } }
                    val sample = natives[beat.size + track]
                    if (sample != null) {
                        val patch = sound.sampled!!.copy(root = partAudio[track]?.second ?: 60f)
                        patch.engineValues().forEachIndexed { i, v -> NativeEngine.setTrackSampleParam(i, v, track) }
                        NativeEngine.setTrackSample(track, sample)  // the export engine keeps its own reference
                    }
                    NativeEngine.setSynthEngine(if (sample != null) 2 else if (sound.plaits != null) 1 else 0, track)
                    val clip = StarterWizard.startingNotes(part, style, mood, harmony)
                    val notes = clip.performance.expand(clip).filter { it.start < ticks }
                    NativeEngine.setSynthSteps(16 * bars, track)
                    notes.forEachIndexed { i, n -> NativeEngine.setSynthNote(i, n.start, n.length, n.pitch, n.velocity, track) }
                    NativeEngine.setSynthNoteCount(notes.size, track)
                }
            }
            coroutineScope {
                // A preview dropped for another one stops rendering at once.
                var rendered = false
                val stopper = launch { try { awaitCancellation() } finally { if (!rendered) job.cancel() } }
                withContext(Dispatchers.Default) { job.renderToArray(OfflineExport.framesFor(bars, style.bpm), OfflineExport.SAMPLE_RATE / 2L) }
                    .also { rendered = true; stopper.cancel() }
            }
        } finally {
            withContext(NonCancellable + Dispatchers.Main) {
                job.close()
                natives.forEach { it?.close() }
            }
        }
        return if (data.isEmpty()) null else Pcm(trimSilence(data), 2, OfflineExport.SAMPLE_RATE)
    }

    // --- The assistant -------------------------------------------------------------------------

    fun openWizard() {
        _wizard.value = WizardUi(open = true)
        refreshPacks()
    }

    /** Writing the new project failed: the choices are still there, to try again. */
    fun createFailed(message: String) {
        _wizard.update { it.copy(creating = false, error = message) }
    }

    fun closeWizard() {
        stopPreview()
        _wizard.value = WizardUi()
    }

    fun chooseStyle(id: String) {
        val style = StarterWizard.style(id) ?: return
        val installed = _packs.value.installed
        _wizard.update { w ->
            w.copy(
                styleId = id, step = 1, rows = style.lines.associate { it.role to defaultRow(it.role, id, installed) },
                mood = style.melody.mood,
                // Every part on: the song's third scene is its melody (chords and lead); the user can untick them.
                parts = StarterWizard.Part.entries.associateWith { PartRow(true, StarterWizard.soundsFor(it, style, installed).first()) },
                error = null,
            )
        }
    }

    /** null: the beat only, no melodic tracks. */
    fun setMood(mood: StarterWizard.Mood?) {
        _wizard.update { it.copy(mood = mood) }
        val ref = mood?.let { SoundRef.MoodDemo(it) }
        if (ref == null) stopPreview() else if (_playing.value != ref && _preparing.value != ref) preview(ref)
    }

    fun setPartEnabled(part: StarterWizard.Part, enabled: Boolean) =
        _wizard.update { w -> w.parts[part]?.let { w.copy(parts = w.parts + (part to it.copy(enabled = enabled))) } ?: w }

    /** Picks the part's sound and lets its first bar be heard. */
    fun choosePartSound(part: StarterWizard.Part, sound: StarterWizard.PartSound) {
        _wizard.update { w -> w.parts[part]?.let { w.copy(parts = w.parts + (part to it.copy(sound = sound, enabled = true, chosen = true))) } ?: w }
        val ref = SoundRef.Melodic(part, sound)
        // A second touch while it prepares waits for it; while it plays, it stops it.
        if (_preparing.value != ref) preview(ref)
    }

    fun next() {
        stopPreview()
        _wizard.update { it.copy(step = (it.step + 1).coerceAtMost(it.lastStep)) }
    }

    fun back() {
        stopPreview()
        _wizard.update { it.copy(step = (it.step - 1).coerceAtLeast(0), error = null) }
    }

    fun setRowEnabled(role: Role, enabled: Boolean) = editRow(role) { it.copy(enabled = enabled) }

    fun setCategory(role: Role, categoryId: String) {
        val category = soundCategories(role, _wizard.value.styleId, _packs.value.installed).firstOrNull { it.id == categoryId } ?: return
        editRow(role) { it.copy(category = categoryId, sound = category.options.firstOrNull(), chosen = true) }
    }

    /** Picks the sound for the line and lets it be heard. */
    fun choose(role: Role, option: SoundOption) {
        editRow(role) { it.copy(sound = option, enabled = true, chosen = true) }
        if (_preparing.value != option.ref) preview(option.ref)
    }

    private fun editRow(role: Role, transform: (WizardRow) -> WizardRow) =
        _wizard.update { w -> w.rows[role]?.let { w.copy(rows = w.rows + (role to transform(it))) } ?: w }

    /** After a download: lines and parts not picked by hand move to the style's sounds just downloaded. */
    private fun refreshWizardDefaults() {
        val installed = _packs.value.installed
        _wizard.update { w ->
            val style = w.style ?: return@update w
            w.copy(
                rows = w.rows.mapValues { (role, row) ->
                    val better = defaultRow(role, w.styleId, installed)
                    if (!row.chosen && better.category != row.category) better.copy(enabled = row.enabled) else row
                },
                parts = w.parts.mapValues { (part, row) ->
                    if (row.chosen) row else row.copy(sound = StarterWizard.soundsFor(part, style, installed).first())
                },
            )
        }
    }

    private fun defaultRow(role: Role, styleId: String?, installed: List<PackManifest>): WizardRow {
        val first = soundCategories(role, styleId, installed).first()
        // The built-in kit: the voice already on that pad when it can play the line.
        val option = if (first.id == KIT_CATEGORY) first.options.firstOrNull { (it.ref as SoundRef.Kit).voice == role.lane } ?: first.options.first()
        else first.options.first()
        return WizardRow(enabled = true, category = first.id, sound = option)
    }

    /**
     * Builds the new song: [then] gets the project and the sounds to store in its folder
     * (file name → audio), on the main thread.
     */
    /**
     * Builds the song from the choices as they are now and hands it to [then] with its sounds and
     * the guide that describes it; the assistant stays "creating" until it is closed or [createFailed].
     */
    fun create(then: (Project, Map<String, Pcm>, List<com.pocketstudio.app.settings.GuidePage>) -> Unit) {
        val w = _wizard.value
        val style = w.style ?: return
        if (w.creating) return
        stopPreview()
        _wizard.update { it.copy(creating = true, error = null) }
        viewModelScope.launch {
            try {
                val sounds = HashMap<Role, PadSample?>()
                val files = HashMap<String, Pcm>()
                for (line in style.lines) {
                    val row = w.rows[line.role] ?: continue
                    val option = row.sound ?: continue
                    if (!row.enabled) continue
                    val ref = option.ref
                    // The kit voice the pad already has needs no file.
                    if (ref is SoundRef.Kit && ref.voice == line.role.lane) {
                        sounds[line.role] = null
                        continue
                    }
                    val pcm = load(ref) ?: throw IllegalStateException("son illisible : ${option.name}")
                    val file = "${UUID.randomUUID().toString().take(12)}.wav"
                    files[file] = pcm
                    sounds[line.role] = PadSample(file, option.name.substringBefore(" · ").take(40))
                }
                if (sounds.isEmpty() && (w.mood == null || w.parts.values.none { it.enabled })) {
                    _wizard.update { it.copy(creating = false, error = "Garde au moins une ligne pour commencer.") }
                    return@launch
                }
                val chosen = if (w.mood == null) emptyMap() else w.parts.filterValues { it.enabled }.mapValues { it.value.sound }
                // Pack sounds played across the keys: copied into the project, set on their own note.
                val parts = chosen.mapValues { (_, sound) ->
                    if (sound.packFile == null) return@mapValues sound
                    val (pcm, root) = packPartSound(sound) ?: throw IllegalStateException("son illisible : ${sound.name}")
                    val file = "${UUID.randomUUID().toString().take(12)}.wav"
                    files[file] = pcm
                    sound.copy(sampled = sound.sampled!!.copy(file = file, root = root ?: 60f))
                }
                AppLog.add("Assistant : morceau ${style.name}, ${sounds.size} lignes, ${parts.size} pistes mélodiques")
                // The choices stay until the project is written (the caller closes the assistant then).
                then(StarterWizard.build(style, sounds, w.mood, parts), files, songGuide(style.name, w.plan))
            } catch (e: Exception) {
                _wizard.update { it.copy(creating = false, error = "Le morceau n'a pas pu être préparé (${e.message}).") }
            }
        }
    }

    override fun onCleared() {
        player.stop()
        cancelled.values.forEach { it.set(true) }
    }

    companion object {
        private const val KIT_FRAMES = OfflineExport.SAMPLE_RATE * 3L / 2

        /** Drops the silent end of a render (keeps 20 ms after the last audible frame). */
        fun trimSilence(stereo: FloatArray): FloatArray {
            var last = stereo.size / 2 - 1
            while (last > 0 && abs(stereo[last * 2]) < 1e-4f && abs(stereo[last * 2 + 1]) < 1e-4f) last--
            val keep = ((last + 1 + OfflineExport.SAMPLE_RATE / 50) * 2).coerceAtMost(stereo.size).coerceAtLeast(2)
            return stereo.copyOf(keep)
        }
    }
}
