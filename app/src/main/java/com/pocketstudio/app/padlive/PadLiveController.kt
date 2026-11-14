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
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.stemView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.NonCancellable
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

    /** [packJob] fills a page on its own ([fillShownPage]): a pack the user touches goes first. */
    private var packQuiet = false

    /** Lot B2: « Enregistrer ce que je joue ». */
    val played = PlayedTake(vm)

    /** Lot B3: the challenges (« refais ce rythme »). */
    val challenge = ChallengeController(vm)

    /** Lot B3: « Partager ma perf » (the sound, the pads one file each). */
    val perf = PerfShare(vm)

    /** Pad Live is on the screen (set by its route): a controller's pads play its pads. */
    @Volatile var shown: Boolean = false

    /**
     * Lot B2: a MIDI command while Pad Live is shown: its pads (hit, let go) play the page shown.
     * True when taken (else it goes where it went before Pad Live).
     */
    fun midi(c: com.pocketstudio.app.midi.MidiCommand): Boolean {
        if (!shown) return false
        return when (c) {
            is com.pocketstudio.app.midi.MidiCommand.Pad -> { midiPad(c.index, true, c.velocity); true }
            is com.pocketstudio.app.midi.MidiCommand.PadUp -> { midiPad(c.index, false); true }
            else -> false
        }
    }

    /**
     * Lot B2: a controller's pad [index] (0..15, as the screen's pads of the page shown) hit
     * ([down], [velocity]) or let go: played as a finger on that pad (its mode, its group).
     */
    fun midiPad(index: Int, down: Boolean, velocity: Float = 1f) {
        if (index !in 0 until PadLive.PAGE_SIZE) return
        touch(PadLive.pad(vm.project.value.padLive.shownPage, index), down, velocity)
    }

    /** Tests: the pads the controller switched (in the engine's place). */
    internal var toggleForTest: ((Int) -> Unit)? = null

    /** Tests: the pack maker (no audio engine there). */
    internal var makerForTest: PackMaker? = null

    private fun toggle(pad: Int, velocity: Float = 1f) {
        toggleForTest?.let { it(pad); return }
        vm.toggleLive(pad, velocity)
    }

    /**
     * A finger [down] on pad [pad] (any page), or up from it: what its mode says (see
     * [PadLive.touch]); starting, it first stops the others of its group. [velocity]: a
     * controller's pad strength (the screen: 1).
     */
    fun touch(pad: Int, down: Boolean, velocity: Float = 1f) {
        val pads = vm.project.value.live
        val clip = pads.at(pad) ?: return
        if (down) challenge.tapped(pad)
        val sounding = vm.padsSoundingNow()
        if (PadLive.touch(clip, down, pad in sounding) != PadTouch.TOGGLE) return
        PadLive.choked(pads, pad, sounding).forEach { toggle(it) }
        toggle(pad, velocity)
    }

    /** The engine's pads: 0 off, 1 waiting for its line, 2 playing (without an engine: as [RhythmViewModel.padsSoundingNow] says, playing). */
    private fun padStates(): IntArray {
        if (!NativeEngine.isNative) {
            val sounding = vm.padsSoundingNow()
            return IntArray(LivePads.ALL) { if (it in sounding) 2 else 0 }
        }
        return IntArray(NativeEngine.LIVE_PADS).also(NativeEngine::clipStates)
    }

    /** Tests: the rows launched (first pad, pads starting, pads stopping) in the engine's place. */
    internal var rowForTest: ((Int, List<Int>, List<Int>) -> Unit)? = null

    /**
     * Lot B2: row [row] (0..3) of page [page] launched together on the next bar line (its loops all
     * on: they stop). Starting, the others of their « un seul à la fois » groups stop first.
     */
    fun launchRow(page: Int, row: Int) {
        val pads = vm.project.value.live
        val first = PadLive.pad(page, row.coerceIn(0, PadLive.ROWS - 1) * PadLive.ROW_SIZE)
        val states = padStates()
        val playing = states.indices.filterTo(HashSet()) { states[it] == 2 }
        val waiting = states.indices.filterTo(HashSet()) { states[it] == 1 }
        val sounding = playing + waiting
        val plan = PadLive.rowLaunch(pads, first, playing, waiting)
        if (plan.starting.isEmpty() && plan.stopping.isEmpty()) return
        plan.starting.flatMap { PadLive.choked(pads, it, sounding) }.filter { it !in plan.starting }.distinct().forEach { toggle(it) }
        rowForTest?.let { it(first, plan.starting, plan.stopping); return }
        vm.launchLiveRow(first, PadLive.ROW_SIZE, plan.starting, plan.stopping)
    }

    /**
     * Lot B2: pad [pad]'s own sound: [pitch] semitones, [reverse], [attack] and [release] seconds
     * (null: unchanged). A slider's moves make one undo step.
     */
    fun setSound(pad: Int, pitch: Int? = null, reverse: Boolean? = null, attack: Float? = null, release: Float? = null) {
        val key = when {
            attack != null -> "padlive-attack-$pad"
            release != null -> "padlive-release-$pad"
            else -> null
        }
        vm.editLive(pad, key) { c ->
            c.copy(pitch = pitch ?: c.pitch, reverse = reverse ?: c.reverse, attack = attack ?: c.attack, release = release ?: c.release)
        }
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
    fun loadPack(id: String, quiet: Boolean = false, fete: suspend (Double) -> Map<Int, Pair<Pcm, Int>>) {
        val pack = PadLive.pack(id) ?: return
        // Relecture tour 1, M3: a pack touched while a page fills on its own is taken (that filling
        // stops), never dropped; one touched while another of the user's is made waits for nothing.
        val before = packJob?.takeIf { it.isActive }
        if (before != null && (quiet || !packQuiet)) return
        val page = vm.project.value.padLive.shownPage
        packQuiet = quiet
        packJob = vm.viewModelScope.launch {
            before?.cancelAndJoin()
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
                val put = { q: com.pocketstudio.model.Project -> q.copy(live = PadLive.placed(q.live, page, clips.toList()), padLive = q.padLive.withPack(page, id)) }
                // A page filled on its own ([fillShownPage]) is not an undo step: it is put in every
                // step (relecture tour 1, M3: « Annuler » of an older step took it away), wherever the
                // page is still empty and never had a pack.
                if (quiet) vm.session.editEverywhere { q -> if (PadLive.filled(q.live, page) > 0 || q.padLive.packOf(page).isNotEmpty()) q else put(q) }
                else vm.session.edit(transform = put)
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
     * Audit ultra round 1, n° 5 (four testers met pages of « + »): in the home's « Live » project, a
     * page B to D shown empty for the first time gets its pack ([PadLive.FIRST_PACKS]), not an undo
     * step. A page the user emptied keeps its pack's name: it is not filled again.
     */
    fun fillShownPage(fete: suspend (Double) -> Map<Int, Pair<Pcm, Int>>) {
        val p = vm.project.value
        val page = p.padLive.shownPage
        if (!p.padLive.home || page == 0 || PadLive.filled(p.live, page) > 0 || p.padLive.packOf(page).isNotEmpty()) return
        PadLive.FIRST_PACKS.getOrNull(page)?.let { loadPack(it, quiet = true, fete = fete) }
    }

    /**
     * Tour 6, padlive2 (P3, control seat A b #49: a first perf of 4 bars, then a take of 0:01, and the « Live » project
     * falls to « 1 mes. »): Pad Live's ● [record]. Pad Live has no « Nouvelle version » / « Ajouter à la suite » to
     * show, so a take after one already in the song goes after it and the project keeps its length; the first take
     * still replaces the new project's empty song. Ready (or a take done): the choice is made here, never while armed.
     */
    fun record(record: () -> Unit) {
        val take = vm.take.value
        if (take is com.pocketstudio.app.live.TakeUi.Ready || take is com.pocketstudio.app.live.TakeUi.Done) {
            val p = vm.project.value
            vm.setTakeAppend(p.liveTake.isNotEmpty() && p.song.isNotEmpty())
        }
        record()
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
        // Audit ultra round 1, n° 5: the style with its kit and its parts (built empty, a pack had only its hits).
        val song = PadLive.packSong(pack, bpm) ?: return out
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
