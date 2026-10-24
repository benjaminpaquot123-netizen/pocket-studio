package com.pocketstudio.app.library

import com.pocketstudio.app.audio.finishOrRelease
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.app.report.forUser
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
import com.pocketstudio.model.AcidPattern
import com.pocketstudio.model.AcidSound
import com.pocketstudio.model.PackIndex
import com.pocketstudio.model.WizardAcid
import com.pocketstudio.model.automatedAcidKnobs
import com.pocketstudio.model.PackManifest
import com.pocketstudio.model.PackSound
import com.pocketstudio.model.PadSample
import com.pocketstudio.model.Project
import com.pocketstudio.model.SoundPacks
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.StarterWizard.Role
import com.pocketstudio.model.StudioDrums
import com.pocketstudio.model.Harmony
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
    /** A style with its advised sounds (drums, its parts, its 303), a few bars at its tempo: heard before choosing it. */
    data class StyleDemo(val styleId: String) : SoundRef
    /** The style's 303 line over the beat chosen so far, in the chosen mood. */
    data object AcidDemo : SoundRef
    /** One of the demos of « Nouveau projet » ([com.pocketstudio.model.Demos.ALL] index): its first scene, heard without opening a project. */
    data class Demo(val index: Int) : SoundRef
    /** A multizone instrument's preview (phase 38): from its pack when downloaded, else fetched from the catalogue. */
    data class Instrument(val packId: String) : SoundRef
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
    /** Ids of the index the user has already been offered (« Tout compléter » brings the others). */
    val seen: Set<String> = emptySet(),
    /** « Tout télécharger » / « Tout compléter » running, if any. */
    val queue: QueueUi? = null,
) {
    private val ready get() = (index as? IndexState.Ready)?.index
    /** Every pack not on the phone or older there. */
    val toDownload: List<com.pocketstudio.model.PackEntry> get() = ready?.let { com.pocketstudio.model.PackCatalog.downloadAll(it, installed) }.orEmpty()
    /** Updates and new packs: the badge's count. */
    val toComplete: List<com.pocketstudio.model.PackEntry> get() = ready?.let { com.pocketstudio.model.PackCatalog.complete(it, installed, seen) }.orEmpty()
    /** Packs whose newer version is published. */
    val outdated: Set<String> get() = ready?.let { com.pocketstudio.model.PackCatalog.outdated(it, installed) }.orEmpty().map { it.id }.toSet()
}

/** The download-all queue: [done] of [count] packs, [fraction] of all the bytes, the pack [current]ly coming. */
data class QueueUi(val done: Int, val count: Int, val current: String?, val fraction: Float)

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
    /** The style's 303 line is kept (acid styles; its own step). */
    val acid: Boolean = true,
    val creating: Boolean = false,
    val error: String? = null,
    /** The 303 line as written on its step (null: the style's, in the chosen mood). */
    val acidNotes: AcidPattern? = null,
    /** The 303's sound as set on its step (null: the style's). */
    val acidSound: AcidSound? = null,
    /** The quick sound picked for the 303 ([WizardAcid.SOUNDS] label), if any. */
    val acidSoundName: String? = null,
    /** How busy a generated 303 line is (0..1), as on the Acide tab. */
    val acidDensity: Float = AcidPattern().density,
) {
    val style: StarterWizard.Style? get() = styleId?.let(StarterWizard::style)
    /** Without a melody ([mood] null) the melodic parts' steps are left out; the mood is chosen before them, so [step] stays put. */
    val steps: List<StarterWizard.Step> get() = StarterWizard.steps(style, melody = mood != null)
    val lastStep: Int get() = steps.size - 1
    val current: StarterWizard.Step get() = steps[step.coerceIn(0, lastStep)]

    /**
     * The 303 line the song gets with [mood] (null: the style has none): the one written on its
     * step, or the style's in that mood, with the sound set there.
     */
    fun acidIn(mood: StarterWizard.Mood): AcidPattern? {
        val recipe = style?.let { WizardAcid.recipe(it, mood) } ?: return null
        return (acidNotes ?: recipe).copy(sound = acidSound ?: recipe.sound, density = acidDensity)
    }

    /** The 303 line as it is now (in the chosen mood, else the style's own). */
    val acidPattern: AcidPattern? get() = style?.let { acidIn(mood ?: it.melody.mood) }

    /** The 303 really plays in the song: a mood, the line kept, and at least one note in it. */
    val acidPlays: Boolean get() = mood != null && acid && acidPattern?.let(WizardAcid::plays) == true

    /** The song's key for the 303's notes. */
    val acidHarmony: com.pocketstudio.model.Harmony? get() = style?.let { StarterWizard.harmonyOf(it, mood ?: it.melody.mood) }

    /**
     * Which sections (Intro, Couplet, Refrain…) each chosen line and part will play in (what
     * "Créer le morceau" builds): only what really plays, a line with its sound, a part with a
     * mood, the 303 with a note.
     */
    val plan: StarterWizard.ScenePlan get() {
        val lines = style?.lines.orEmpty().map { it.role }.filter { r -> rows[r]?.let { it.enabled && it.sound != null } == true }.toSet()
        val used = if (mood == null) emptySet() else parts.filterValues { it.enabled }.keys
        return style?.let { StarterWizard.planFor(it, lines, used, acidPlays) } ?: StarterWizard.scenePlan(lines, used, acidPlays)
    }
}

/**
 * « Nouveau projet › Partir d'un style »: the whole song made in one touch with the assistant's
 * advised choices. [creating]: the style whose song is being prepared; [error]: what went wrong.
 */
data class QuickUi(
    val creating: String? = null, val error: String? = null,
    /** The demo (index in [com.pocketstudio.model.Demos.ALL]) playing, and the one being rendered before it plays. */
    val demoPlaying: Int? = null, val demoPreparing: Int? = null,
)

/** Fader of the sound being tried in an assistant excerpt (+3 dB) and of the song around it (−9 dB). */
private const val FOCUS_VOLUME = 0.95f
private const val BACKGROUND_VOLUME = 0.48f

const val KIT_CATEGORY = "kit"
private const val PACK_PREFIX = "pack:"

/**
 * The line's sound categories: the pack the style takes this line from (a machine such as the
 * TR-909 for rave techno) when downloaded, then the style's own pack, then the built-in kit, then
 * the other packs that have that kind of sound.
 */
fun soundCategories(role: Role, styleId: String?, installed: List<PackManifest>): List<SoundCategory> {
    val label = tr(SoundPacks.KIND_LABELS[role.packKind] ?: role.title)
    // The kit voice the style asks for this line (a shaker, a cowbell) is offered first.
    val voice = styleId?.let(StarterWizard::style)?.lines?.firstOrNull { it.role == role }?.voice
    val voices = (listOfNotNull(voice) + role.builtIn).distinct()
    val kit = SoundCategory(KIT_CATEGORY, tr("Sons intégrés"), voices.map { SoundOption(SoundRef.Kit(it), DrumKitInfo.names[it]) })
    val packs = installed.mapNotNull { m ->
        val sounds = m.ofKind(role.packKind)
        if (sounds.isEmpty()) null
        else SoundCategory(PACK_PREFIX + m.id, "$label · ${m.name}", sounds.map { SoundOption(SoundRef.Pack(m.id, it), "${it.name} · ${m.name}") })
    }
    val machine = styleId?.let(StarterWizard::style)?.kit?.get(role)
    val first = packs.filter { it.id == PACK_PREFIX + machine }
    val own = packs.filter { it.id == PACK_PREFIX + styleId } - first.toSet()
    // A line that asks for its own kit voice (the afro house shaker, the congas) keeps it even with
    // the style's pack downloaded: the pack's hats would not be the sound the style announces.
    if (voice != null) return listOf(kit) + first + own + (packs - own.toSet() - first.toSet())
    return first + own + kit + (packs - own.toSet() - first.toSet())
}

/** Every category of a kind of sound for the library: the built-in kit's voices when the kit has that kind, then each pack. */
fun libraryCategories(kind: String, installed: List<PackManifest>): List<SoundCategory> {
    val role = Role.entries.firstOrNull { it.id == kind }
    if (role != null) return soundCategories(role, null, installed)
    val label = tr(SoundPacks.KIND_LABELS[kind] ?: kind)
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

    private val _quick = MutableStateFlow(QuickUi())
    /** The song made in one touch from « Nouveau projet » (the assistant stays closed). */
    val quick: StateFlow<QuickUi> = _quick.asStateFlow()

    private val _playing = MutableStateFlow<SoundRef?>(null)
    /** The sound being previewed, if any. */
    val playing: StateFlow<SoundRef?> = _playing.asStateFlow()

    private val _preparing = MutableStateFlow<SoundRef?>(null)
    /** The excerpt being rendered before it plays (a second or so), if any. */
    val preparing: StateFlow<SoundRef?> = _preparing.asStateFlow()
    private var previewJob: Job? = null

    private val player = PcmPlayer { _playing.value = null }
    /** Kit voices already rendered (internal: tests put a sound there as if heard before). */
    internal val kitCache = HashMap<Int, Pcm>()
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
            _packs.update { it.copy(installed = installed, seen = seenPacks() ?: it.seen) }
            // The assistant opened on a style before the packs were known: its lines move to them.
            refreshWizardDefaults()
            if (_packs.value.index is IndexState.Ready) return@launch
            _packs.update { it.copy(index = IndexState.Loading) }
            val index = withContext(Dispatchers.IO) {
                runCatching { PackStore.fetchIndex(app) }.fold(
                    { it?.let { i -> IndexState.Ready(i) } ?: IndexState.Unavailable },
                    { IndexState.Failed(tr("La liste des packs n'a pas pu être chargée (réseau ?).")) },
                )
            }
            // The first time: what is installed counts as seen; with nothing installed, the whole
            // list does (« Tout télécharger » is there for it): only later packs are « new ».
            if (index is IndexState.Ready && seenPacks() == null) {
                val ids = if (installed.isEmpty()) index.index.packs.map { it.id } else installed.map { it.id }
                markSeen(ids)
            }
            _packs.update { it.copy(index = index) }
        }
    }

    private fun prefs() = getApplication<Application>().getSharedPreferences("packs", android.content.Context.MODE_PRIVATE)

    private fun seenPacks(): Set<String>? = runCatching { prefs().getStringSet("seen", null)?.toSet() }.getOrNull()

    private fun markSeen(ids: Collection<String>) {
        val all = (seenPacks().orEmpty() + ids).toSet()
        runCatching { prefs().edit().putStringSet("seen", all).apply() }
        _packs.update { it.copy(seen = all) }
    }

    private val queueStop = AtomicBoolean(false)

    /**
     * « Tout télécharger » ([complete] false: every pack missing or older) or « Tout compléter »
     * (updates and new packs only), one pack after the other, each tried again on a network error.
     * Killed half-way, it starts again from what is missing (a cut file resumes where it stopped).
     */
    fun downloadAll(complete: Boolean) {
        val ui = _packs.value
        if (ui.index !is IndexState.Ready || ui.queue != null) return
        val entries = (if (complete) ui.toComplete else ui.toDownload).filter { it.id !in ui.downloading }
        if (entries.isEmpty()) return
        queueStop.set(false)
        _packs.update { it.copy(queue = QueueUi(0, entries.size, entries.first().name, 0f), message = null) }
        viewModelScope.launch {
            val app = getApplication<Application>()
            val queue = com.pocketstudio.model.PackQueue({ e, progress, cancelled -> PackStore.install(app, e, progress, cancelled) })
            val result = withContext(Dispatchers.IO + NonCancellable) {
                queue.run(entries, queueStop::get) { p ->
                    _packs.update { it.copy(queue = QueueUi(p.done, p.count, p.current?.name, p.fraction)) }
                }
            }
            val installed = withContext(Dispatchers.IO) { PackStore.installed(app) }
            // Only what got in: a pack that failed stays offered by « Tout compléter ».
            markSeen(installed.map { it.id })
            val names = entries.associate { it.id to it.name }
            _packs.update {
                it.copy(
                    installed = installed, queue = null,
                    message = when {
                        result.failed.isNotEmpty() -> {
                            val failed = result.failed.keys.joinToString(", ") { id -> names[id] ?: id }
                            tr("%1\$d packs installés ; %2\$d n'ont pas pu venir (%3\$s) : touche « Tout compléter » pour réessayer.", result.installed.size, result.failed.size, failed)
                                .also { m -> AppLog.add(m) }
                        }
                        result.cancelled -> tr("Téléchargement arrêté : %d packs installés, le reste viendra avec « Tout compléter ».", result.installed.size)
                        else -> tr("%d packs installés : toute la bibliothèque est à jour.", result.installed.size)
                    },
                )
            }
            if (result.installed.isNotEmpty()) {
                AppLog.add("Packs installés : ${result.installed.joinToString()}")
                refreshWizardDefaults()
            }
        }
    }

    fun cancelAll() = queueStop.set(true)

    fun downloadPack(id: String) {
        val entry = (_packs.value.index as? IndexState.Ready)?.index?.packs?.firstOrNull { it.id == id } ?: return
        if (id in _packs.value.downloading) return
        if (_packs.value.queue != null) return
        val stop = AtomicBoolean(false).also { cancelled[id] = it }
        markSeen(listOf(id))
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
                        else -> tr("Le pack « %s » n'a pas pu être téléchargé : %s.", entry.name, error.forUser()).also { m -> AppLog.add(m) }
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
        startPreview(ref)
    }

    /**
     * Renders [ref] and plays it, after [waitMs] (edits in a row make one render); what plays
     * goes on until the new excerpt is ready.
     */
    private fun startPreview(ref: SoundRef, waitMs: Long = 0) {
        // The last touch wins: an excerpt still rendering for another sound is dropped.
        previewJob?.cancel()
        // Started only once it is known as the current job: on the main thread a cached sound (a
        // kit voice already rendered) runs to its end at start(), before an assignment after
        // launch, and its finally would not see itself as the current render (a spinner forever).
        val job = viewModelScope.launch(start = kotlinx.coroutines.CoroutineStart.LAZY) {
            _preparing.value = ref
            if (waitMs > 0) kotlinx.coroutines.delay(waitMs)
            val pcm = try {
                previewOf(ref)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.add("Écoute impossible : ${e.javaClass.simpleName}")
                null
            } catch (e: OutOfMemoryError) {
                AppLog.add("Écoute impossible : mémoire pleine")
                null
            } finally {
                // Not when a newer render of the same excerpt took over (the 303 edited again).
                if (_preparing.value == ref && previewJob === coroutineContext[Job]) _preparing.value = null
            }
            if (pcm == null) {
                AppLog.add("Écoute impossible : $ref")
                return@launch
            }
            // A line tried with the beat (a drum line, the 303) goes round until stopped, to compare in time.
            if (player.play(pcm, loop = loops(ref))) _playing.value = ref
            else AppLog.add("Le téléphone a refusé de jouer l'extrait (${pcm.frames} images)")
        }
        previewJob = job
        job.start()
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
        val opened = openings
        viewModelScope.launch {
            val pcm = load(ref)
            // The library closed meanwhile (even across a rotation): the sound lands nowhere.
            if (opened != openings) return@launch
            if (pcm == null) {
                _packs.update { it.copy(message = tr("Ce son n'a pas pu être lu.")) }
                return@launch
            }
            then(pcm)
        }
    }

    /** Bumped each time the library closes: kept here, it outlives the screen's rotation. */
    private var openings = 0

    /** The library closed: a sound still loading is dropped, the preview stops. */
    fun closed() {
        openings++
        stopPreview()
    }

    /**
     * What ▶ plays for [ref]: the sound as audio, at the one preview level (lot 1, [PreviewLevel]:
     * a held chord as loud as a bass line or a pack's sample). Only what is heard: a sound put on a
     * pad or a track keeps its own level ([use]).
     */
    private suspend fun previewOf(ref: SoundRef): Pcm? {
        val pcm = load(ref) ?: return null
        // A short sound (a kit voice already rendered) right here, as it was played at once before;
        // a longer excerpt off the main thread.
        return if (pcm.data.size <= SHORT_PREVIEW_SAMPLES) PreviewLevel.normalize(pcm) else withContext(Dispatchers.Default) { PreviewLevel.normalize(pcm) }
    }

    /** Tests and the preview bench: what ▶ would play for [ref]. */
    @androidx.annotation.VisibleForTesting
    internal suspend fun previewForTest(ref: SoundRef): Pcm? = previewOf(ref)

    /** Tests and the preview bench: the sound as audio before the preview level. */
    @androidx.annotation.VisibleForTesting
    internal suspend fun loadForTest(ref: SoundRef): Pcm? = load(ref)

    /** The sound as audio: the kit voice rendered once by the engine, or the pack file decoded. */
    private suspend fun load(ref: SoundRef): Pcm? = when (ref) {
        is SoundRef.Kit -> kitVoice(ref.voice)
        // A multizone instrument (phase 38): its own preview (the downloaded pack's, else the
        // catalogue's), not the synth sound that stands in for it in a private engine.
        is SoundRef.Melodic -> previewSource(ref).let { if (it is SoundRef.Instrument) load(it) else partInContext(ref) }
        is SoundRef.MoodDemo -> moodDemo(ref.mood)
        is SoundRef.LineDemo -> lineDemo(ref)
        is SoundRef.StyleDemo -> styleDemo(ref.styleId)
        SoundRef.AcidDemo -> acidDemo()
        is SoundRef.Demo -> demoPreview(ref.index)
        is SoundRef.Instrument -> withContext(Dispatchers.IO) {
            val app = getApplication<Application>()
            val installed = _packs.value.installed.firstOrNull { it.id == ref.packId }
            val file = installed?.sounds?.firstOrNull()?.let { PackStore.file(app, installed.id, it) }
                ?: (_packs.value.index as? IndexState.Ready)?.index?.packs?.firstOrNull { it.id == ref.packId }?.let { PackStore.preview(app, it) }
            file?.let { f ->
                runCatching { AudioDecoder.decode(app, Uri.fromFile(f)) }.onFailure { AppLog.add("Aperçu illisible ${ref.packId} : ${it.message}") }.getOrNull()
            }
        }
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
        // A private engine costs tens of milliseconds to build: not on the main thread (never lost if cancelled).
        val job = finishOrRelease(Dispatchers.Default, OfflineExport::close) { OfflineExport() }!!
        val data = try {
            NativeEngine.captureInto(job) {
                // The kit of the assistant's songs (kit v3, each voice at kit v2's level for its role): WizardMix places these sounds by those levels.
                NativeEngine.setKitVersion(com.pocketstudio.model.Project.KIT_V3)
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

    /**
     * A sound played across the keys for a melodic part (a pack file, or a kit voice such as the
     * 808 cowbell): decoded, with the note it was recorded at (null: not readable).
     */
    private suspend fun packPartSound(sound: StarterWizard.PartSound): Pair<Pcm, Float?>? {
        val pcm = sound.kitVoice?.let { kitVoice(it) } ?: run {
            val (packId, file) = sound.packFile ?: return null
            withContext(Dispatchers.IO) {
                val f = PackStore.file(getApplication(), packId, com.pocketstudio.model.PackSound(file, "", ""))
                runCatching { AudioDecoder.decode(getApplication(), Uri.fromFile(f)) }.getOrNull()
            }
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
        val mood = w.mood ?: style.melody.mood
        return groove(style, chosenBeat(w, style), before + (ref.part to ref.sound), mood, bars = 2, acid = w.acidIn(mood)?.takeIf { w.acid }, focusPart = before.size)
    }

    /** The mood's four chords on the style's parts (its 303 line, or bass and pad), over the beat as chosen so far. */
    private suspend fun moodDemo(mood: StarterWizard.Mood): Pcm? {
        val w = _wizard.value
        val style = w.style ?: return null
        val parts = demoParts(style).map { p ->
            p to (w.parts[p]?.sound ?: StarterWizard.soundsFor(p, style, _packs.value.installed).first())
        }
        return groove(style, chosenBeat(w, style), parts, mood, bars = 4, acid = w.acidIn(mood)?.takeIf { w.acid })
    }

    /**
     * The parts that make a style heard: its own (and its signature), or bass and chords when the
     * recipe has none but no 303 either.
     */
    private fun demoParts(style: StarterWizard.Style): List<StarterWizard.Part> {
        val own = StarterWizard.Part.entries.filter { it in StarterWizard.defaultParts(style) || it == style.melody.signature }
        return own.ifEmpty { if (style.melody.acid != null) emptyList() else listOf(StarterWizard.Part.Bass, StarterWizard.Part.Pad) }
    }

    /** The style as the assistant would make it: its beat, its parts and its 303 with the advised sounds, in its mood. */
    private suspend fun styleDemo(id: String): Pcm? {
        val style = StarterWizard.style(id) ?: return null
        val installed = _packs.value.installed
        val beat = style.lines.filter { !it.off }.mapNotNull { line -> defaultRow(line.role, id, installed).sound?.let { line to it.ref } }
        val parts = demoParts(style).map { it to StarterWizard.soundsFor(it, style, installed).first() }
        return groove(style, beat, parts, style.melody.mood, bars = STYLE_DEMO_BARS, acid = WizardAcid.recipe(style, style.melody.mood))
    }

    /**
     * Describes a project to the engine under [captureInto][NativeEngine.captureInto] (main thread):
     * set by the screen that owns the open project's engine sync, so a demo can be rendered on a
     * private engine without touching the open project.
     */
    var demoSync: ((Project) -> Unit)? = null

    /** Stops the demo preview if it is the one playing or being rendered (other previews are left alone). */
    fun stopDemoPreview() {
        if (_playing.value is SoundRef.Demo || _preparing.value is SoundRef.Demo) stopPreview()
    }

    /** The first scene of a demo (drums, 303, synth) over [DEMO_BARS] bars, rendered offline: the open project is untouched. */
    private suspend fun demoPreview(index: Int): Pcm? {
        if (!NativeEngine.isAvailable) return null
        val sync = demoSync ?: return null
        val demo = com.pocketstudio.model.Demos.ALL.getOrNull(index)?.third?.invoke() ?: return null
        val job = finishOrRelease(Dispatchers.Default, OfflineExport::close) { OfflineExport() }!!
        val data = try {
            NativeEngine.captureInto(job) {
                sync(demo)
                NativeEngine.setMetronome(false)
            }
            coroutineScope {
                // A preview dropped for another one stops rendering at once.
                var rendered = false
                val stopper = launch { try { awaitCancellation() } finally { if (!rendered) job.cancel() } }
                withContext(Dispatchers.Default) {
                    job.renderToArray(OfflineExport.framesFor(DEMO_BARS, demo.tempoBpm, demo.beatsPerBar), OfflineExport.SAMPLE_RATE / 2L)
                }.also { rendered = true; stopper.cancel() }
            }
        } finally {
            withContext(NonCancellable + Dispatchers.Main) { job.close() }
        }
        if (data.isEmpty()) return null
        return Pcm(trimSilence(data), 2, OfflineExport.SAMPLE_RATE)
    }

    /** The 303 line as written on its step, with its sound, over the beat chosen so far: one loop of it. */
    private suspend fun acidDemo(): Pcm? {
        val w = _wizard.value
        val style = w.style ?: return null
        val line = w.acidPattern ?: return null
        val bars = (line.length + AcidPattern.STEPS_PER_PAGE - 1) / AcidPattern.STEPS_PER_PAGE
        return groove(style, chosenBeat(w, style), emptyList(), w.mood ?: style.melody.mood, bars = bars.coerceAtLeast(2), acid = line, loop = true)
    }

    /** The beat up to the line, that line played with the sound being tried. */
    private suspend fun lineDemo(ref: SoundRef.LineDemo): Pcm? {
        val w = _wizard.value
        val style = w.style ?: return null
        val upTo = style.lines.indexOfFirst { it.role == ref.role }
        if (upTo < 0) return null
        val beat = style.lines.take(upTo).mapNotNull { line -> enabledSound(w, line) } + (style.lines[upTo] to ref.sound.ref)
        return groove(style, beat, emptyList(), w.mood ?: style.melody.mood, bars = 2, acid = null, focusLane = style.lines[upTo].role.lane, loop = true)
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
        /** The 303 line to play too (null: none, e.g. while trying a drum line alone). */
        acid: AcidPattern?,
        /** The sound being tried, in front of the rest: part [focusPart] (index in [parts]) or the pad of [focusLane]. */
        focusPart: Int? = null,
        focusLane: Int? = null,
        /** Made to go round: exactly [bars] bars, the ringing end laid over the start. */
        loop: Boolean = false,
    ): Pcm? {
        if (!NativeEngine.isAvailable) return null
        // Audio first: the kit voice already on its pad needs nothing, the others are decoded or rendered.
        val harmonyKey = StarterWizard.harmonyOf(style, mood)
        // The studio voices the song gives its kit lines (the tuned kick, the rumble: S3), heard as they will be.
        val voices = beat.map { (line, ref) -> if (ref is SoundRef.Kit) StudioDrums.voiceFor(style, line.role, harmonyKey) else null }
        val padAudio = beat.mapIndexed { i, (line, ref) ->
            if (ref is SoundRef.Kit && ((ref.voice == line.role.lane && !StarterWizard.shapesKit(line.role)) || voices[i] != null)) null else (load(ref) ?: return null)
        }
        val partAudio = parts.map { (_, sound) -> if (sound.sampled != null) (packPartSound(sound) ?: return null) else null }
        val harmony = StarterWizard.harmonyOf(style, mood).copy(lock = false)
        val steps = StarterWizard.drumSteps(style)
        val ticks = bars * 16 * com.pocketstudio.model.NoteClip.TICKS_PER_STEP
        val natives = ArrayList<com.pocketstudio.engine.NativeSample?>()
        // A private engine costs tens of milliseconds to build: not on the main thread (never lost if cancelled).
        val job = finishOrRelease(Dispatchers.Default, OfflineExport::close) { OfflineExport() }!!
        val data = try {
            // Resampled to 48 kHz off the main thread, and always kept so that the finally below frees them.
            withContext(Dispatchers.Default + NonCancellable) {
                padAudio.forEach { pcm -> natives += pcm?.let { com.pocketstudio.engine.NativeSample.create(it.data, it.channels, it.sampleRate) } }
                partAudio.forEach { a -> natives += a?.let { (pcm, _) -> com.pocketstudio.engine.NativeSample.create(pcm.data, pcm.channels, pcm.sampleRate) } }
            }
            NativeEngine.captureInto(job) {
                NativeEngine.setKitVersion(com.pocketstudio.model.Project.KIT_V3)  // as the song will have it
                NativeEngine.setTempo(style.bpm)
                NativeEngine.setSwing(style.swing)
                NativeEngine.setPatternSteps(steps)
                beat.forEachIndexed { i, (line, _) ->
                    val lane = line.role.lane
                    natives[i]?.let { NativeEngine.setPadSample(lane, it) }
                    voices[i]?.let { v ->
                        NativeEngine.setPadVoice(lane, v.kind)
                        v.params.toSortedMap().forEach { (param, value) -> NativeEngine.setPadVoiceParam(lane, param, value) }
                    }
                    // Heard as the song will have it: a kit voice shaped for its line (the short ride).
                    if (natives[i] != null && beat[i].second is SoundRef.Kit && StarterWizard.shapesKit(line.role)) {
                        NativeEngine.setPadParams(lane, StarterWizard.kitShaped(line.role, PadSample("", "")).engineParams())
                    }
                    StarterWizard.hits(line, steps).forEach { (step, hit) -> NativeEngine.setDrumHit(lane, step, hit.velocity, hit.repeats, hit.ramp) }
                }
                // The 303 line under the chords (acid house, rave techno).
                if (acid != null) {
                    val line = acid.sanitized()
                    NativeEngine.setAcidLength(line.length)
                    NativeEngine.setAcidRoot(line.root)
                    line.steps.forEachIndexed { i, s -> NativeEngine.setAcidStep(i, s.pack()) }
                    line.sound.values().forEachIndexed { i, v -> NativeEngine.setAcidParam(i, v) }
                }
                parts.forEachIndexed { track, (part, sound) ->
                    NativeEngine.setTrackOn(track, true)  // a fresh engine only plays track 0
                    // As the song will have it: a gliding bass gets its portamento.
                    val synth = if (StarterWizard.glides(part, style)) sound.synth.with(com.pocketstudio.model.SynthPatch.GLIDE, StarterWizard.BASS_GLIDE) else sound.synth
                    synth.sanitized().values.forEachIndexed { i, v -> NativeEngine.setSynthParam(i, v, track) }
                    sound.plaits?.let { p -> p.sanitized().engineValues().forEachIndexed { i, v -> NativeEngine.setPlaitsParam(i, v, track) } }
                    val sample = natives[beat.size + track]
                    if (sample != null) {
                        val heard = StarterWizard.startingNotes(part, style, mood, harmony, sound).notes
                        val patch = sound.sampled!!.copy(
                            root = com.pocketstudio.model.SampledPatch.fitRoot(partAudio[track]?.second, heard),
                            glide = if (StarterWizard.isLine(heard)) sound.sampled!!.glide else 0f,
                        )
                        patch.engineValues().forEachIndexed { i, v -> NativeEngine.setTrackSampleParam(i, v, track) }
                        NativeEngine.setTrackSample(track, sample)  // the export engine keeps its own reference
                    }
                    NativeEngine.setSynthEngine(if (sample != null) 2 else if (sound.plaits != null) 1 else 0, track)
                    val clip = StarterWizard.startingNotes(part, style, mood, harmony, sound)
                    val notes = clip.performance.expand(clip).filter { it.start < ticks }
                    NativeEngine.setSynthSteps(16 * bars, track)
                    notes.forEachIndexed { i, n -> NativeEngine.setSynthNote(i, n.start, n.length, n.pitch, n.velocity, track) }
                    NativeEngine.setSynthNoteCount(notes.size, track)
                }
                // Each line through the style's effects on its pad, as in the song (the rumble is
                // only its preset: a kick copy without it is just a second kick).
                val fx = StarterWizard.effectsMixer(style, beat.map { it.first.role }.toSet(), acid = acid != null)
                for ((line, _) in beat) {
                    val ch = com.pocketstudio.model.Mixer.padChannel(line.role.lane)
                    val strip = fx.channels[ch]
                    com.pocketstudio.app.rhythm.RhythmViewModel.insertCommands(strip.inserts, { slot, type -> NativeEngine.setInsertType(ch, slot, type) }) { slot, k, v ->
                        NativeEngine.setInsertParam(ch, slot, k, v)
                    }
                    strip.sends.forEachIndexed { bus, v -> NativeEngine.setSend(ch, bus, v) }
                }
                // The 303 heard with the beat sounds as in the song: through the style's effects on
                // the acid line, the drums and the master (Acidcore's saturation and OTT).
                if (acid != null) {
                    for (ch in listOf(0, 1)) {
                        val strip = fx.channels[ch]
                        com.pocketstudio.app.rhythm.RhythmViewModel.insertCommands(strip.inserts, { slot, type -> NativeEngine.setInsertType(ch, slot, type) }) { slot, k, v ->
                            NativeEngine.setInsertParam(ch, slot, k, v)
                        }
                        strip.sends.forEachIndexed { bus, v -> NativeEngine.setSend(ch, bus, v) }
                        strip.macros.forEachIndexed { m, v -> NativeEngine.setMacro(ch, m, v) }
                    }
                    com.pocketstudio.app.rhythm.RhythmViewModel.insertCommands(fx.master, { slot, type -> NativeEngine.setInsertType(NativeEngine.MASTER, slot, type) }) { slot, k, v ->
                        NativeEngine.setInsertParam(NativeEngine.MASTER, slot, k, v)
                    }
                }
                // The sound being tried stands out (+3 dB), the song around it steps back (−9 dB):
                // it is the one to judge, the rest is only there to place it.
                if (focusPart != null) {
                    val focus = com.pocketstudio.model.InstrumentTrack.mixerChannel(focusPart)
                    for (ch in com.pocketstudio.model.Mixer.TRACK_STRIPS) {
                        if (ch != NativeEngine.MASTER) NativeEngine.setChannelVolume(ch, if (ch == focus) FOCUS_VOLUME else BACKGROUND_VOLUME)
                    }
                } else if (focusLane != null) {
                    for (lane in 0 until com.pocketstudio.model.DrumPattern.PAD_COUNT) {
                        NativeEngine.setChannelVolume(com.pocketstudio.model.Mixer.padChannel(lane), if (lane == focusLane) FOCUS_VOLUME else BACKGROUND_VOLUME)
                    }
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
        if (data.isEmpty()) return null
        return Pcm(if (loop) loopOf(data, OfflineExport.framesFor(bars, style.bpm)) else trimSilence(data), 2, OfflineExport.SAMPLE_RATE)
    }

    // --- The assistant -------------------------------------------------------------------------

    /** [styleId]: opened on that style's first step (« Personnaliser pas à pas »), else on the styles. */
    fun openWizard(styleId: String? = null) {
        _wizard.value = WizardUi(open = true)
        refreshPacks()
        styleId?.let(::chooseStyle)
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
        _wizard.update { withStyle(it, style, installed) }
    }

    /** null: the beat only, no melodic tracks. */
    fun setMood(mood: StarterWizard.Mood?) {
        _wizard.update { w ->
            // A 303 line written before (the 303 step comes after the mood's) moves into the new
            // key's scale, so that the song's 303 stays in tune with its chords.
            val style = w.style
            val notes = w.acidNotes
            val moved = if (notes != null && style != null && mood != null) WizardAcid.inScale(notes, StarterWizard.harmonyOf(style, mood)) else notes
            w.copy(mood = mood, acidNotes = moved)
        }
        val ref = mood?.let { SoundRef.MoodDemo(it) }
        if (ref == null) stopPreview() else if (_playing.value != ref && _preparing.value != ref) preview(ref)
    }

    /** Keeps the style's 303 line or leaves it out. */
    fun setAcidEnabled(enabled: Boolean) = _wizard.update { it.copy(acid = enabled) }

    /** A touch on 303 step [index]: the next note up the song's scale (a rest after the top one). */
    fun acidTapStep(index: Int) = editAcidNotes { p, h -> p.withStep(index, WizardAcid.nextNote(p.steps[index], h)) }

    /** A long touch on 303 step [index]: its accent, its slide, both, neither. */
    fun acidHoldStep(index: Int) = editAcidNotes { p, _ -> p.withStep(index, WizardAcid.nextMarks(p.steps[index])) }

    /** A new 303 line from the generator, at the density chosen, on the song's scale. */
    fun generateAcid() = editAcidNotes { p, h ->
        val feel = _wizard.value.styleId?.let { com.pocketstudio.model.AcidGenerator.Feel.forStyle(it) } ?: com.pocketstudio.model.AcidGenerator.Feel.CLASSIC
        WizardAcid.generate(p.copy(density = _wizard.value.acidDensity), h, feel = feel)
    }

    fun setAcidDensity(density: Float) = _wizard.update { it.copy(acidDensity = density.coerceIn(0f, 1f)) }

    /** The 303 line over one or two bars (a second bar starts as a copy of the first). */
    fun setAcidBars(bars: Int) = editAcidNotes { p, _ -> WizardAcid.withBars(p, bars) }

    /** Back to the style's own 303 line (the sound stays as set). */
    fun acidFromStyle() {
        _wizard.update { it.copy(acidNotes = null) }
        refreshAcidDemo()
    }

    /** One of the quick 303 sounds ([WizardAcid.SOUNDS]). */
    fun chooseAcidSound(label: String) {
        _wizard.update { w ->
            val sound = WizardAcid.sound(label, w.acidPattern?.sound ?: AcidSound()) ?: return@update w
            w.copy(acidSound = sound, acidSoundName = label)
        }
        refreshAcidDemo()
    }

    /** Turns one of the 303's knobs (index as in [AcidSound.values]: 0 cutoff, 1 resonance). */
    fun setAcidKnob(index: Int, value: Float) {
        _wizard.update { w -> w.acidPattern?.let { w.copy(acidSound = it.sound.with(index, value)) } ?: w }
        refreshAcidDemo()
    }

    private fun editAcidNotes(transform: (AcidPattern, com.pocketstudio.model.Harmony) -> AcidPattern) {
        _wizard.update { w ->
            val line = w.acidPattern ?: return@update w
            val harmony = w.acidHarmony ?: return@update w
            w.copy(acidNotes = transform(line, harmony).sanitized(), acid = true)
        }
        refreshAcidDemo()
    }

    /** The 303 heard with the beat (« Avec le rythme ») starts again with the change, still in a loop. */
    private fun refreshAcidDemo() {
        if (restartsOnEdit(_playing.value, _preparing.value)) startPreview(SoundRef.AcidDemo, waitMs = EDIT_SETTLE_MS)
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

    fun setRowEnabled(role: Role, enabled: Boolean) {
        editRow(role) { it.copy(enabled = enabled) }
        // The rumble rolls in the low end in the bass line's place (its tip says so): ticked, the
        // bass part goes off, or two low ends would play at once (it can be ticked back).
        if (role == Role.Rumble && enabled) setPartEnabled(StarterWizard.Part.Bass, false)
    }

    fun setCategory(role: Role, categoryId: String) {
        val category = soundCategories(role, _wizard.value.styleId, _packs.value.installed).firstOrNull { it.id == categoryId } ?: return
        editRow(role) { it.copy(category = categoryId, sound = category.options.firstOrNull(), chosen = true) }
    }

    /**
     * Picks the sound for the line and lets it be heard: alone, or, while the line is being heard
     * with the beat (« Avec le rythme »), with the beat again straight away with the new sound.
     */
    fun choose(role: Role, option: SoundOption) {
        editRow(role) { it.copy(sound = option, enabled = true, chosen = true) }
        val next = soundOnChoose(_playing.value ?: _preparing.value, role, option)
        if (_preparing.value != next && _playing.value != next) preview(next)
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
        // The built-in kit: the voice the style asks for (a shaker, a cowbell), else the one already
        // on that pad when it can play the line.
        val wanted = styleId?.let(StarterWizard::style)?.lines?.firstOrNull { it.role == role }?.voice ?: role.lane
        val option = if (first.id == KIT_CATEGORY) first.options.firstOrNull { (it.ref as SoundRef.Kit).voice == wanted } ?: first.options.first()
        else first.options.first()
        return WizardRow(enabled = true, category = first.id, sound = option)
    }

    /**
     * Builds the song from the choices as they are now and hands it to [then] with its sounds
     * (file name in its folder → audio) and the guide that describes it, on the main thread; the
     * assistant stays "creating" until it is closed or [createFailed].
     */
    fun create(then: (Project, Map<String, Pcm>, List<com.pocketstudio.app.settings.GuidePage>) -> Unit) {
        val w = _wizard.value
        if (w.style == null || w.creating) return
        stopPreview()
        _wizard.update { it.copy(creating = true, error = null) }
        viewModelScope.launch {
            try {
                val song = songFrom(w)
                if (song == null) {
                    _wizard.update { it.copy(creating = false, error = tr("Garde au moins une ligne pour commencer.")) }
                    return@launch
                }
                // The choices stay until the project is written (the caller closes the assistant then).
                then(song.project, song.files, song.guide)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _wizard.update { it.copy(creating = false, error = tr("Le morceau n'a pas pu être préparé (%s).", e.message ?: e.javaClass.simpleName)) }
            }
        }
    }

    /**
     * « Nouveau projet › Partir d'un style »: the whole song of [styleId] in one touch, exactly as
     * the assistant makes it with its advised choices (its lines and their sounds, its mood, its
     * parts, its 303, effects, automation and guide), without opening it. The downloaded packs are
     * read first as the assistant would see them; a pack sound that cannot be read gives way to
     * the built-in sounds. [then] as for [create]; [quick] says "creating" until [quickDone] or
     * [quickFailed].
     */
    fun quickCreate(styleId: String, then: (Project, Map<String, Pcm>, List<com.pocketstudio.app.settings.GuidePage>) -> Unit) {
        val style = StarterWizard.style(styleId) ?: return
        if (_quick.value.creating != null) return
        stopPreview()
        _quick.value = QuickUi(creating = styleId)
        viewModelScope.launch {
            try {
                val app = getApplication<Application>()
                val installed = withContext(Dispatchers.IO) { runCatching { PackStore.installed(app) }.getOrElse { _packs.value.installed } }
                _packs.update { it.copy(installed = installed) }
                val song = try {
                    songFrom(withStyle(WizardUi(), style, installed))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (installed.isEmpty()) throw e
                    AppLog.add("Morceau en un geste : un son de pack illisible (${e.message}), sons intégrés à la place")
                    songFrom(withStyle(WizardUi(), style, emptyList()))
                } ?: throw IllegalStateException(tr("aucun son à jouer"))
                then(song.project, song.files, song.guide)
            } catch (e: CancellationException) {
                _quick.value = QuickUi()
                throw e
            } catch (e: Exception) {
                AppLog.add("Morceau en un geste impossible : ${e.javaClass.simpleName}")
                _quick.value = QuickUi(error = tr("Le morceau n'a pas pu être préparé (%s).", e.message ?: e.javaClass.simpleName))
            } catch (e: OutOfMemoryError) {
                AppLog.add("Morceau en un geste impossible : mémoire pleine")
                _quick.value = QuickUi(error = tr("Le morceau n'a pas pu être préparé (%s).", tr("mémoire pleine")))
            }
        }
    }

    /** The song made in one touch is written and opened. */
    fun quickDone() {
        _quick.value = QuickUi()
    }

    /** Writing the song made in one touch failed: said, to try again. */
    fun quickFailed(message: String) {
        _quick.value = QuickUi(error = message)
    }

    fun dismissQuickError() = _quick.update { it.copy(error = null) }

    /** The assistant on [style] with its advised choices: what "Choisir ce style" sets and the one-touch song uses. */
    private fun withStyle(w: WizardUi, style: StarterWizard.Style, installed: List<PackManifest>): WizardUi = w.copy(
        styleId = style.id, step = 1, rows = style.lines.associate { it.role to defaultRow(it.role, style.id, installed).copy(enabled = !it.off) },
        mood = style.melody.mood,
        acidNotes = null, acidSound = null, acidSoundName = null, acidDensity = AcidPattern().density,
        // The recipe's parts on (the riddim's bass alone, no bass under the gabber's kick, the
        // acid styles' 303 alone); the others can be ticked on their step.
        parts = StarterWizard.Part.entries.associateWith { PartRow(it in StarterWizard.defaultParts(style), StarterWizard.soundsFor(it, style, installed).first()) },
        acid = true,
        error = null,
    )

    /** A song built by the assistant: the project, the sounds to store in its folder, its guide. */
    private class Song(val project: Project, val files: Map<String, Pcm>, val guide: List<com.pocketstudio.app.settings.GuidePage>)

    /** The song the choices [w] make (null: nothing would play); throws when a sound cannot be read. */
    private suspend fun songFrom(w: WizardUi): Song? {
        val style = w.style ?: return null
        val sounds = HashMap<Role, PadSample?>()
        val files = HashMap<String, Pcm>()
        for (line in style.lines) {
            val row = w.rows[line.role] ?: continue
            val option = row.sound ?: continue
            if (!row.enabled) continue
            val ref = option.ref
            // The kit voice the pad already has needs no file (unless the line shapes it: the ride); nor does a
            // line the style gives a studio voice of the engine (its tuned kick, the rumble: S3).
            if (ref is SoundRef.Kit && ((ref.voice == line.role.lane && !StarterWizard.shapesKit(line.role)) || StudioDrums.voiceFor(style, line.role, Harmony(style.melody.root)) != null)) {
                sounds[line.role] = null
                continue
            }
            val pcm = load(ref) ?: throw IllegalStateException(tr("son illisible : %s", option.name))
            val file = "${UUID.randomUUID().toString().take(12)}.wav"
            files[file] = pcm
            val sample = PadSample(file, option.name.substringBefore(" · ").take(40))
            sounds[line.role] = if (ref is SoundRef.Kit) StarterWizard.kitShaped(line.role, sample) else sample
        }
        if (sounds.isEmpty() && (w.mood == null || w.parts.values.none { it.enabled })) return null
        val chosen = if (w.mood == null) emptyMap() else w.parts.filterValues { it.enabled }.mapValues { it.value.sound }
        // Pack sounds and kit voices played across the keys: copied into the project, set on their own note.
        val parts = chosen.mapValues { (_, sound) ->
            if (sound.packFile == null && sound.kitVoice == null) return@mapValues sound
            val (pcm, root) = packPartSound(sound) ?: throw IllegalStateException(tr("son illisible : %s", tr(sound.name)))
            val file = "${UUID.randomUUID().toString().take(12)}.wav"
            files[file] = pcm
            sound.copy(sampled = sound.sampled!!.copy(file = file, root = root ?: 60f))
        }
        AppLog.add("Assistant : morceau ${style.name}, ${sounds.size} lignes, ${parts.size} pistes mélodiques")
        // Its live pads are prepared once it is open and its sounds loaded (RhythmViewModel.fillLivePads).
        val project = StarterWizard.build(style, sounds, w.mood, parts, acid = w.acidPlays, chosenAcid = w.acidPattern).copy(livePadsPending = true)
        // The song's 303 cutoff follows a curve (acid styles): the guide says how to take it over.
        val guide = songGuide(
            style.name, w.plan, inKey = w.mood != null, song = songSummary(style, w.plan), acid = w.acidPlays,
            acidMoves = 0 in project.automatedAcidKnobs,
            sweeps = StarterWizard.hasSweeps(project),
        )
        return Song(project, files, guide)
    }

    override fun onCleared() {
        player.stop()
        cancelled.values.forEach { it.set(true) }
    }

    companion object {
        /** A preview this short (2 s of stereo) is brought to its level on the spot. */
        private const val SHORT_PREVIEW_SAMPLES = 2 * 2 * OfflineExport.SAMPLE_RATE
        private const val KIT_FRAMES = OfflineExport.SAMPLE_RATE * 3L / 2
        /** Bars of a style heard on its card (a few seconds). */
        private const val STYLE_DEMO_BARS = 4
        private const val DEMO_BARS = 8

        /** Edits of the 303 in a row (a knob turned) wait this long before the loop is rendered again. */
        private const val EDIT_SETTLE_MS = 250L

        /**
         * One loop of exactly [frames] frames from a stereo render that rings on after them: the
         * ringing end is laid over the start, as it sounds when the loop goes round.
         */
        fun loopOf(stereo: FloatArray, frames: Long): FloatArray {
            val n = (frames * 2).coerceAtMost(stereo.size.toLong()).toInt().coerceAtLeast(2)
            val out = stereo.copyOf(n)
            for (i in n until stereo.size) out[(i - n) % n] += stereo[i]
            return out
        }

        /** Drops the silent end of a render (keeps 20 ms after the last audible frame). */
        fun trimSilence(stereo: FloatArray): FloatArray {
            var last = stereo.size / 2 - 1
            while (last > 0 && abs(stereo[last * 2]) < 1e-4f && abs(stereo[last * 2 + 1]) < 1e-4f) last--
            val keep = ((last + 1 + OfflineExport.SAMPLE_RATE / 50) * 2).coerceAtMost(stereo.size).coerceAtLeast(2)
            return stereo.copyOf(keep)
        }
    }
}

/**
 * What to play when the sound [option] is picked for line [role]: the line with the beat again
 * when that is what is being heard ([current]), else the sound alone.
 */
internal fun soundOnChoose(current: SoundRef?, role: Role, option: SoundOption): SoundRef =
    if (current is SoundRef.LineDemo && current.role == role) SoundRef.LineDemo(role, option) else option.ref

/** Excerpts that go round until stopped: a drum line with the beat, the 303 with the beat. */
/**
 * What ▶ really plays for [ref]: a part of the assistant on a multizone instrument (phase 38)
 * plays that instrument's own preview (the downloaded pack's, else the catalogue's), not the synth
 * sound that stands in for it in a private engine; anything else plays itself.
 */
internal fun previewSource(ref: SoundRef): SoundRef =
    (ref as? SoundRef.Melodic)?.sound?.multi?.pack?.let { SoundRef.Instrument(it) } ?: ref

internal fun loops(ref: SoundRef): Boolean = ref is SoundRef.LineDemo || ref == SoundRef.AcidDemo

/** An edit of the 303 restarts what is heard when it is the 303 with the beat ([playing] or being [preparing]). */
internal fun restartsOnEdit(playing: SoundRef?, preparing: SoundRef?): Boolean =
    playing == SoundRef.AcidDemo || preparing == SoundRef.AcidDemo
