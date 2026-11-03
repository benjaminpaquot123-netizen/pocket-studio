package com.pocketstudio.app.padlive

import android.app.Application
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.pocketstudio.app.audio.AudioDecoder
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.audio.finishOrRelease
import com.pocketstudio.app.library.SoundLibraryViewModel
import com.pocketstudio.app.report.AppLog
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.LivePadPlan
import com.pocketstudio.model.LivePads
import com.pocketstudio.model.LiveStarter
import com.pocketstudio.model.PadLive
import com.pocketstudio.model.PadMode
import com.pocketstudio.model.PadTouch
import com.pocketstudio.model.PartySounds
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.stemView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

/** One sound a pack puts on its page: [slot] (0..15), its audio, its name, and its clip once stored in [file]. */
class PackSound(val slot: Int, val pcm: Pcm, val name: String, val clip: (file: String) -> LiveClip)

/** Makes a pack's sounds at a tempo (the engine's renders on the phone; a test gives its own). */
fun interface PackMaker {
    suspend fun make(pack: PadLive.Pack, bpm: Double, beatsPerBar: Int): List<PackSound>
}

/**
 * Pad Live's logic (n° 25, lot B1), held by [RhythmViewModel.padLive] in one field: a touch on a
 * pad by its mode (coup, boucle, tenir) and its « un seul à la fois » group, the page shown, the
 * pad settings, the packs by style. Every change is one undo step of the project (saved with it);
 * the page shown is not an edit (never undone).
 */
class PadLiveController(private val vm: RhythmViewModel) {
    private val _busy = MutableStateFlow<String?>(null)
    /** « Préparation du pack Trap… » while a pack is made (null: ready). */
    val busy: StateFlow<String?> = _busy.asStateFlow()

    private var packJob: Job? = null

    /** Tests: the pads the controller switched (in the engine's place). */
    internal var toggleForTest: ((Int) -> Unit)? = null

    /** Tests: the pack maker (no audio engine there). */
    internal var makerForTest: PackMaker? = null

    private fun toggle(pad: Int) {
        toggleForTest?.let { it(pad); return }
        vm.toggleLive(pad)
    }

    /**
     * A finger [down] on pad [pad] (any page), or up from it: what its mode says (see
     * [PadLive.touch]); starting, it first stops the others of its group.
     */
    fun touch(pad: Int, down: Boolean) {
        val pads = vm.project.value.live
        val clip = pads.at(pad) ?: return
        val sounding = vm.padsSoundingNow()
        if (PadLive.touch(clip, down, pad in sounding) != PadTouch.TOGGLE) return
        PadLive.choked(pads, pad, sounding).forEach(::toggle)
        toggle(pad)
    }

    /** The page shown (A to D): not an edit, never undone, kept with the project. */
    fun setPage(page: Int) {
        val p = page.coerceIn(0, PadLive.PAGES - 1)
        if (vm.project.value.padLive.page == p) return
        vm.session.editQuietly { it.copy(padLive = it.padLive.copy(page = p)) }
    }

    fun setMode(pad: Int, mode: PadMode) {
        // A held pad switched to another mode while it sounds: stopped first (no finger to let go).
        if (vm.project.value.live.at(pad)?.hold == true && pad in vm.padsSoundingNow()) toggle(pad)
        vm.editLive(pad) { PadLive.withMode(it, mode) }
    }

    fun setGroup(pad: Int, group: Int) = vm.editLive(pad) { PadLive.withGroup(it, group) }

    /**
     * Pack [id] on the page shown: its sounds made at the project's tempo, then put on the page in
     * one undo step (the page's sounds before come back with « Annuler »). [fete]: the ready-to-play
     * set ([LiveStarter], « Fête »), made by the sound library.
     */
    fun loadPack(id: String, fete: suspend (Double) -> Map<Int, Pair<Pcm, Int>>) {
        val pack = PadLive.pack(id) ?: return
        if (packJob?.isActive == true) return
        val page = vm.project.value.padLive.shownPage
        packJob = vm.viewModelScope.launch {
            _busy.value = tr("Préparation du pack %s…", packName(pack))
            try {
                val p = vm.project.value
                val maker = makerForTest ?: EnginePackMaker(vm, fete)
                val sounds = maker.make(pack, p.tempoBpm, p.beatsPerBar)
                if (sounds.isEmpty()) {
                    AppLog.add("Pack ${pack.id} : aucun son")
                    return@launch
                }
                val clips = arrayOfNulls<LiveClip>(PadLive.PAGE_SIZE)
                for (s in sounds) {
                    if (s.slot !in 0 until PadLive.PAGE_SIZE) continue
                    val (sample, prepared) = withContext(Dispatchers.IO) { vm.padSamples.add(s.pcm, s.name) }
                    vm.padSamples.register(prepared)
                    clips[s.slot] = s.clip(sample.file)
                }
                // The page's pads that sound stop: their sounds are being replaced.
                val sounding = vm.padsSoundingNow()
                (0 until PadLive.PAGE_SIZE).map { PadLive.pad(page, it) }.filter { it in sounding }.forEach(::toggle)
                vm.session.edit { q -> q.copy(live = PadLive.placed(q.live, page, clips.toList()), padLive = q.padLive.copy(pack = id)) }
                vm.pushLive()
                AppLog.add("Pack ${pack.id} sur la page ${PadLive.PAGE_NAMES[page]} : ${sounds.size} sons")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.add("Pack ${pack.id} : échec (${e.message ?: e.javaClass.simpleName})")
            } catch (e: OutOfMemoryError) {
                AppLog.add("Pack ${pack.id} : mémoire insuffisante")
            } finally {
                _busy.value = null
            }
        }
    }

    /**
     * A sound chosen in the sound browser for pad [pad] of pages B to D (page A goes through the
     * Live page's own path): stored, then on the pad in one undo step, a loop or a hit by its length.
     */
    fun placeSound(pad: Int, pcm: Pcm, name: String, oneShot: Boolean) {
        if (pad !in LivePads.MAX until LivePads.ALL) return
        vm.viewModelScope.launch {
            val (sample, prepared) = withContext(Dispatchers.IO) { vm.padSamples.add(pcm, name) }
            vm.padSamples.register(prepared)
            val beats = if (oneShot) 1 else LivePads.beatsFor(pcm.seconds, vm.project.value.tempoBpm)
            vm.session.edit { q -> q.copy(live = q.live.withAt(pad, LiveClip(sample.file, sample.name, beats, oneShot).sanitized())) }
            vm.pushLive()
        }
    }

    private var welcomeJob: Job? = null

    /**
     * Opened from the home screen's card: the first hit of the page shown sounds as soon as its
     * sound is in (Benjamin's « ça sonne tout de suite »: the first sound in one touch), its pad
     * lit so the ear knows where it came from. Nothing when the page has no hit, or after [WELCOME_WAIT_MS].
     */
    fun welcome() {
        if (welcomeJob?.isActive == true) return
        welcomeJob = vm.viewModelScope.launch {
            var waited = 0L
            while (waited <= WELCOME_WAIT_MS) {
                val p = vm.project.value
                val page = p.padLive.shownPage
                val pad = (0 until PadLive.PAGE_SIZE).map { PadLive.pad(page, it) }
                    .firstOrNull { p.live.at(it)?.let { c -> PadLive.modeOf(c) == PadMode.SHOT } == true }
                if (pad != null && (toggleForTest != null || vm.padSamples.native(p.live.at(pad)!!.file) != null)) {
                    if (pad !in vm.padsSoundingNow()) toggle(pad)
                    return@launch
                }
                delay(WELCOME_POLL_MS)
                waited += WELCOME_POLL_MS
            }
        }
    }

    companion object {
        const val WELCOME_WAIT_MS = 8_000L
        private const val WELCOME_POLL_MS = 100L
    }
}

/**
 * The packs as the phone makes them: « Fête » is the ready-to-play set of the sound library
 * ([fete]); a style's pack renders one loop per track of a song the assistant builds in that style
 * (as [LivePadPlan] does for a song), offline on a private engine, then its party hits.
 */
private class EnginePackMaker(private val vm: RhythmViewModel, private val fete: suspend (Double) -> Map<Int, Pair<Pcm, Int>>) : PackMaker {
    override suspend fun make(pack: PadLive.Pack, bpm: Double, beatsPerBar: Int): List<PackSound> {
        if (!NativeEngine.isAvailable) return emptyList()
        val style = pack.style ?: return fete(bpm).mapNotNull { (i, made) ->
            val pad = LiveStarter.PADS.getOrNull(i) ?: return@mapNotNull null
            val (pcm, beats) = made
            PackSound(i, pcm, pad.name) { file ->
                LiveClip(file, pad.name, if (pad.oneShot) 1 else beats, pad.oneShot, pad.gain, bpm = if (pad.oneShot) 0.0 else bpm)
            }
        }
        val out = ArrayList<PackSound>()
        val recipe = StarterWizard.style(style) ?: return out
        val song = StarterWizard.build(recipe.copy(bpm = bpm), emptyMap())
        val plan = LivePadPlan.of(song, max = PadLive.MAX_LOOPS)
        for (spec in plan) {
            val view = song.stemView(spec.key, spec.scene)
            val frames = OfflineExport.framesFor(spec.bars, song.tempoBpm, song.beatsPerBar)
            val tail = if (spec.oneShot) OfflineExport.SAMPLE_RATE * 2L else OfflineExport.SAMPLE_RATE / 2L
            val job = finishOrRelease(Dispatchers.IO, OfflineExport::close) { OfflineExport() }!!
            val data = try {
                NativeEngine.captureInto(job) {
                    vm.syncEngine(view)
                    NativeEngine.setMetronome(false)
                    NativeEngine.setLoudnessTarget(null)
                }
                withContext(Dispatchers.Default) { job.renderToArray(frames, tail) }
            } finally {
                // Native instruments share reference counts: freed on the main thread only.
                withContext(NonCancellable + Dispatchers.Main) { job.close() }
            }
            if (data.isEmpty()) continue
            val audio = withContext(Dispatchers.Default) {
                if (spec.oneShot) SoundLibraryViewModel.trimSilence(data) else SoundLibraryViewModel.loopOf(data, frames)
            }
            if (audio.none { abs(it) > SILENT }) continue
            val beats = (spec.bars * song.beatsPerBar).coerceAtMost(LivePads.MAX_BEATS)
            val slot = out.size
            out += PackSound(slot, Pcm(audio, 2, OfflineExport.SAMPLE_RATE), spec.name) { file ->
                LiveClip(file, spec.name, if (spec.oneShot) 1 else beats, spec.oneShot, bpm = if (spec.oneShot) 0.0 else song.tempoBpm)
            }
        }
        pack.hits.forEachIndexed { i, hit ->
            val pcm = partySound(vm.getApplication(), hit.party) ?: return@forEachIndexed
            out += PackSound(PadLive.HITS_FROM + i, pcm, hit.name) { file -> PadLive.hitClip(hit, file) }
        }
        return out
    }

    private companion object {
        const val SILENT = 1e-4f
    }
}

/** A party sound of the app ([PartySounds]): its asset copied once to the cache (the decoder reads files), then decoded. */
internal suspend fun partySound(app: Application, id: String): Pcm? = withContext(Dispatchers.IO) {
    val sound = PartySounds.byId(id) ?: return@withContext null
    runCatching {
        val dir = File(app.cacheDir, "party").apply { mkdirs() }
        val file = File(dir, "${sound.id}.ogg")
        if (!file.isFile || file.length() == 0L) {
            // Written aside then renamed: a copy cut short is never read as the sound.
            val part = File(dir, "${sound.id}.padlive.part")
            app.assets.open(sound.asset).use { input -> part.outputStream().use { input.copyTo(it) } }
            if (!part.renameTo(file)) error("rename")
        }
        AudioDecoder.decode(app, Uri.fromFile(file))
    }.onFailure { AppLog.add("Son de fête illisible ${sound.id} : ${it.message}") }.getOrNull()
}
