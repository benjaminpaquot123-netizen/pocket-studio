package com.pocketstudio.app.remix

import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.model.addAudioTrack
import com.pocketstudio.model.addTrack
import com.pocketstudio.model.instrumentTracks
import com.pocketstudio.model.DefaultNames
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.LivePads
import com.pocketstudio.model.Project
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.i18n.trCount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * A separated song as the board's actions read it (a worker thread, inside [StemBoardHost.withSong]):
 * frames at [StemSelection.SAMPLE_RATE], stereo.
 */
interface StemSource {
    /** The song's length in frames. */
    val frames: Long

    /** The song's tempo (0: unknown) and the frame of its first beat: the beat grid of the phrases. */
    val songBpm: Double
    val firstBeatFrame: Long

    /** The tempo of what [read] gives when fitted: the project's while the song follows it, else the song's. */
    val fittedBpm: Double

    /**
     * Stereo of stem [stem], [count] frames of the song from [start] (fewer past its end).
     * [fitted]: as the project plays the song (stretched to its tempo when it follows it, pitch kept).
     */
    fun read(stem: Int, start: Long, count: Long, fitted: Boolean): FloatArray
}

/** One cut for a drum pad: [pcm] onto pad [lane] (0..15), named [name] (French, shown translated). */
class PadCut(val lane: Int, val pcm: Pcm, val name: String)

/**
 * What [StemBoardRouter] needs of the app (the drum machine's ViewModel in the app, a stand-in in
 * the tests). Every « put » is one undo step and never changes the project's tempo nor its key.
 */
interface StemBoardHost {
    /** The open project, as it is now. */
    fun project(): Project

    /** Runs [block] on a worker with song [setId]; null when its stems are gone from the phone. */
    suspend fun <T> withSong(setId: String, block: (StemSource) -> T): T?

    /** A pad's sound is being prepared or recorded elsewhere (another one would race it). */
    fun busy(): Boolean

    /** Each cut onto its pad, in one undo step. */
    suspend fun putOnPads(cuts: List<PadCut>)

    /** [pcm] played across the keys of a new instrument track named [name]; its id, null when there are eight. */
    suspend fun putOnNewInstrument(pcm: Pcm, name: String): Int?

    /** [pcm] on a new audio track named [name], from song step [atStep] ([bpm]: its tempo); its id, null when there are eight. */
    suspend fun putOnNewAudioTrack(pcm: Pcm, name: String, atStep: Int, bpm: Double): Int?

    /** [pcm] on live pad [pad], named [name]. */
    suspend fun putOnLivePad(pcm: Pcm, name: String, pad: Int)

    /** The song step the transport plays now, -1 when it is stopped. */
    fun songStepNow(): Int

    /** The project stops playing (listening to a stretch alone: never two sounds at once). */
    fun pauseMusic()
}

/**
 * Where a stretch of a stem goes (B3), behind [StemBoardActions]: a free pad (never one with a
 * sound of its own unless the user named it), a new instrument track, a new audio track at its
 * place, a free live pad; a voice cut into its phrases, one a free pad. Each in one undo step,
 * said in one short sentence ([state]). The work (reading the song, cutting, writing the sounds)
 * runs off the main thread; calls come from the main thread.
 */
class StemBoardRouter(
    private val scope: CoroutineScope,
    private val host: StemBoardHost,
    private val edits: StemEdits,
    private val player: GuideListenPlayer,
) : StemBoardActions {
    private val _state = MutableStateFlow(StemBoardState())
    override val state: StateFlow<StemBoardState> = _state.asStateFlow()

    private var work: Job? = null
    private var listening: Job? = null

    override fun dismissMessage() = _state.update { it.copy(message = null) }

    // --- Listening -------------------------------------------------------------------------------

    override fun play(sel: StemSelection) {
        if (_state.value.playing == sel) {
            stop()
            return
        }
        stop()
        if (sel.frames <= 0) return
        host.pauseMusic()
        _state.update { it.copy(playing = sel, playFrame = sel.startFrame) }
        val job = scope.launch(start = kotlinx.coroutines.CoroutineStart.LAZY) {
            // A long stretch is heard from its start, the first minute only (memory).
            val heard = sel.copy(endFrame = minOf(sel.endFrame, sel.startFrame + PLAY_MAX_SECONDS * StemSelection.SAMPLE_RATE.toLong()))
            val pcm = try {
                host.withSong(sel.setId) { cutOf(it, heard, fitted = false) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Throwable) {
                null
            }
            if (pcm == null || pcm.frames == 0) {
                _state.update { it.copy(playing = null, playFrame = -1, message = tr("Ce passage n'a pas pu être lu.")) }
                return@launch
            }
            val me = coroutineContext[Job]
            val started = player.play(pcm) { if (listening === me) ended() }
            if (!started) {
                ended()
                return@launch
            }
            // The play head follows what really went out, about 30 times a second.
            while (true) {
                val h = player.heard()
                if (h >= 0) _state.update { if (it.playing == sel) it.copy(playFrame = sel.startFrame + h) else it }
                delay(33)
            }
        }
        listening = job
        job.start()
    }

    private fun ended() {
        listening?.cancel()
        listening = null
        _state.update { it.copy(playing = null, playFrame = -1) }
    }

    override fun stop() {
        listening?.cancel()
        listening = null
        player.stop()
        _state.update { it.copy(playing = null, playFrame = -1) }
    }

    // --- Sending ----------------------------------------------------------------------------------

    override fun send(sel: StemSelection, dest: StemDestination) {
        if (refusedBusy()) return
        if (sel.frames <= 0) return say(tr("Choisis d'abord un morceau de la piste : glisse le doigt sur sa courbe."))
        val p = host.project()
        val french = StemRouting.stemFrench(sel.stem)
        val label = tr(french)
        val seconds = sel.frames / StemSelection.SAMPLE_RATE.toDouble()
        val short = dest !is StemDestination.Track
        if (short && seconds > PAD_MAX_SECONDS) {
            return say(tr("Trop long pour un pad : %d secondes au plus. Choisis un morceau plus court.", PAD_MAX_SECONDS))
        }
        if (!short && seconds > TRACK_MAX_SECONDS) {
            return say(tr("Trop long pour une piste : %d minutes au plus. Choisis un morceau plus court.", TRACK_MAX_SECONDS / 60))
        }
        when (dest) {
            is StemDestination.Pad -> {
                val lane = StemRouting.padFor(p, dest.pad)
                    ?: return say(tr("Tous les pads ont déjà un son : choisis le pad à remplacer, ou vide-en un dans Batterie."))
                val name = StemRouting.nextName(p.drums.lanes.mapNotNull { it.sample?.name }, french)
                val replaced = p.drums.lanes[lane].sample?.name
                launchWork(tr("%s vers le pad…", label)) {
                    val pcm = cut(sel, fitted = true) ?: return@launchWork null
                    host.putOnPads(listOf(PadCut(lane, pcm, name)))
                    if (replaced != null) {
                        tr("« %1\$s » remplace « %2\$s » sur le pad %3\$d (la flèche retour le remet).", DefaultNames.shownSample(name), DefaultNames.shownSample(replaced), lane + 1)
                    } else {
                        tr("« %1\$s » est sur le pad %2\$d.", DefaultNames.shownSample(name), lane + 1)
                    }
                }
            }
            is StemDestination.Instrument -> {
                if (p.addTrack() == null) {
                    return say(tr("Huit pistes d'instruments au maximum : supprime-en une pour en ajouter une autre."))
                }
                val name = StemRouting.nextName(p.instrumentTracks.map { it.name }, french).take(InstrumentTrack.MAX_NAME)
                launchWork(tr("%s vers une piste d'instrument…", label)) {
                    val pcm = cut(sel, fitted = true) ?: return@launchWork null
                    host.putOnNewInstrument(pcm, name)
                        ?: return@launchWork tr("Huit pistes d'instruments au maximum : supprime-en une pour en ajouter une autre.")
                    tr("« %s » est une nouvelle piste d'instrument : joue-la au clavier.", DefaultNames.shown(name))
                }
            }
            is StemDestination.Track -> {
                if (p.addAudioTrack() == null) {
                    return say(tr("Les huit pistes audio sont prises : vide-en une ou pose le son sur un pad."))
                }
                val name = StemRouting.nextName(p.audioTracks.map { it.name }, french).take(InstrumentTrack.MAX_NAME)
                val step = (dest.atStep ?: host.songStepNow().takeIf { it >= 0 } ?: StemRouting.stepOf(p, sel)).coerceAtLeast(0)
                launchWork(tr("%s vers une piste audio…", label)) {
                    var bpm = 0.0
                    val pcm = cut(sel, fitted = true) { bpm = it } ?: return@launchWork null
                    host.putOnNewAudioTrack(pcm, name, step, bpm)
                        ?: return@launchWork tr("Les huit pistes audio sont prises : vide-en une ou pose le son sur un pad.")
                    tr("« %1\$s » est sur une nouvelle piste audio, à la mesure %2\$d.", DefaultNames.shown(name), step / p.stepsPerBar.coerceAtLeast(1) + 1)
                }
            }
            is StemDestination.Live -> {
                val pad = p.live.clips.indexOfFirst { it == null }.takeIf { it in 0 until LivePads.MAX }
                    ?: return say(tr("Les 16 pads live sont pris : vides-en un dans Live › Pads."))
                val name = StemRouting.nextName(p.live.clips.mapNotNull { it?.name }, french)
                launchWork(tr("%1\$s vers le pad live %2\$d…", label, pad + 1)) {
                    val pcm = cut(sel, fitted = true) ?: return@launchWork null
                    host.putOnLivePad(pcm, name, pad)
                    tr("« %1\$s » est sur le pad live %2\$d.", DefaultNames.shownSample(name), pad + 1)
                }
            }
        }
    }

    // --- Phrases ----------------------------------------------------------------------------------

    override fun chop(setId: String, stem: Int, max: Int) {
        if (refusedBusy()) return
        val p = host.project()
        val free = StemRouting.freePads(p)
        if (free.isEmpty()) return say(tr("Tous les pads ont déjà un son : vide un pad dans Batterie, puis recommence."))
        val french = StemRouting.stemFrench(stem)
        val first = StemRouting.nextNumber(p.drums.lanes.mapNotNull { it.sample?.name }, french)
        launchWork(tr("Découpe en phrases…")) {
            class Found(val cuts: List<Pcm>, val left: Int)
            val found = host.withSong(setId) { src ->
                val energy = StemRouting.energy(src, stem, ENERGY_FACTOR)
                val beat = if (src.songBpm > 0) Math.round(60.0 / src.songBpm * StemSelection.SAMPLE_RATE / ENERGY_FACTOR) else null
                val all = edits.phrases(energy, beat, max.coerceAtLeast(1), src.firstBeatFrame / ENERGY_FACTOR)
                    .map { (it.first * ENERGY_FACTOR) until minOf(src.frames, (it.last + 1) * ENERGY_FACTOR) }
                    .filter { !it.isEmpty() }
                // More phrases than free pads: the longest ones, in song order.
                val kept = all.sortedByDescending { it.last - it.first }.take(free.size).sortedBy { it.first }
                Found(kept.map { r -> cutOf(src, StemSelection(setId, stem, r.first, r.last + 1), fitted = true) }, all.size - kept.size)
            } ?: return@launchWork gone()
            if (found.cuts.isEmpty()) return@launchWork tr("Aucune phrase trouvée sur cette piste : elle est presque muette.")
            val cuts = found.cuts.mapIndexed { k, pcm -> PadCut(free[k], pcm, "$french ${first + k}") }
            host.putOnPads(cuts)
            val pads = StemRouting.padsText(cuts.map { it.lane })
            trCount(cuts.size, "1 phrase sur le pad %2\$s.", "%1\$d phrases sur les pads %2\$s.", cuts.size, pads) +
                if (found.left > 0) " " + trCount(found.left, "Plus de pad libre pour la dernière.", "Plus de pad libre pour les %d dernières.", found.left) else ""
        }
    }

    // --- Work -------------------------------------------------------------------------------------

    /** The stretch cut out with its fades ([edits]); null (said) when the song is gone or silent there. */
    private suspend fun cut(sel: StemSelection, fitted: Boolean, tempo: (Double) -> Unit = {}): Pcm? {
        val pcm = host.withSong(sel.setId) { src ->
            tempo(if (fitted) src.fittedBpm else src.songBpm)
            cutOf(src, sel, fitted)
        }
        if (pcm == null) {
            say(gone())
            return null
        }
        if (StemRouting.isSilent(pcm)) {
            say(tr("Ce morceau de la piste est muet : rien n'a été posé."))
            return null
        }
        return pcm
    }

    private fun cutOf(src: StemSource, sel: StemSelection, fitted: Boolean): Pcm {
        val data = src.read(sel.stem, sel.startFrame, sel.frames, fitted)
        val region = Pcm(data, 2, StemSelection.SAMPLE_RATE)
        return edits.cut(region, sel.copy(startFrame = 0, endFrame = region.frames.toLong()))
    }

    private fun gone() = tr("Les pistes de ce morceau ne sont plus sur l'appareil : sépare-le à nouveau.")

    /** [block] off the main thread's way; what it returns is said (null: it said why itself). */
    private fun launchWork(busy: String, block: suspend () -> String?) {
        _state.update { it.copy(busy = busy, message = null) }
        work = scope.launch {
            val said = try {
                block()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: OutOfMemoryError) {
                tr("Pas assez de mémoire pour ce son : essaie plus court.")
            } catch (e: Exception) {
                com.pocketstudio.app.report.AppLog.add("Tableau des pistes : échec (${e.message})")
                tr("Ça n'a pas marché : %s", e.message ?: e.javaClass.simpleName)
            }
            _state.update { it.copy(busy = null, message = said ?: it.message) }
            work = null
        }
    }

    private fun refusedBusy(): Boolean {
        if (work?.isActive != true && !host.busy()) return false
        say(tr("Patiente un instant, un son est en cours de préparation."))
        return true
    }

    private fun say(message: String) = _state.update { it.copy(message = message) }

    companion object {
        /** A pad's or a live pad's sound, an instrument's: as long as an imported sound may be. */
        const val PAD_MAX_SECONDS = com.pocketstudio.app.audio.AudioDecoder.MAX_SECONDS
        /** An audio track's clip: as long as an imported file on a track may be. */
        const val TRACK_MAX_SECONDS = com.pocketstudio.app.audio.AudioDecoder.TRACK_MAX_SECONDS
        /** Listening to a stretch alone: its first minute (a longer one would weigh hundreds of MB in the engine). */
        const val PLAY_MAX_SECONDS = 60
        /** The voice's energy for the phrases: one value every this many frames (6 kHz). */
        const val ENERGY_FACTOR = 8
    }
}

/** The pure rules of [StemBoardRouter]: which pad, which name, which step, how it is said. */
object StemRouting {
    /** The parts of a separated song, in stem order, as their names are saved (French, shown translated). */
    val STEMS = listOf("Batterie", "Basse", "Autres", "Voix")

    fun stemFrench(stem: Int): String = STEMS.getOrElse(stem) { "Piste" }

    /**
     * Pad [lane] is free for a cut: no sound of its own, no studio voice, no hit in any scene. A
     * kit's sound nobody plays may go; a kick the beat plays never does. (The same rule as R2's
     * `RemixVoice.padIsFree`: at the merge, one of the two stays.)
     */
    fun padIsFree(p: Project, lane: Int): Boolean {
        val own = p.drums.lanes.getOrNull(lane) ?: return false
        if (own.sample != null || own.hits.isNotEmpty() || p.padVoices.containsKey(lane)) return false
        return p.scenes.withIndex().none { (i, s) -> i != p.scene && s.drums.lanes.getOrNull(lane)?.hits?.isNotEmpty() == true }
    }

    fun freePads(p: Project): List<Int> = (0 until DrumPattern.PAD_COUNT).filter { padIsFree(p, it) }

    /** The pad a stretch goes to: the one the user named (even with a sound: the user chose it), else the first free one. */
    fun padFor(p: Project, named: Int?): Int? =
        named?.takeIf { it in 0 until DrumPattern.PAD_COUNT } ?: freePads(p).firstOrNull()

    /** One more than the highest « [stem] N » among [names] (1 when none). */
    fun nextNumber(names: List<String>, stem: String): Int {
        val re = Regex("^${Regex.escape(stem)} (\\d{1,4})$")
        return (names.mapNotNull { re.matchEntire(it)?.groupValues?.get(1)?.toIntOrNull() }.maxOrNull() ?: 0) + 1
    }

    /** « Voix 3 » when « Voix 1 » and « Voix 2 » are there. */
    fun nextName(names: List<String>, stem: String): String = "$stem ${nextNumber(names, stem)}"

    /** « 9 à 14 » for pads in a row, else « 3, 5 et 9 » (pads counted from 1). */
    fun padsText(lanes: List<Int>): String {
        val n = lanes.sorted().map { it + 1 }
        if (n.size == 1) return n[0].toString()
        if (n.zipWithNext().all { (a, b) -> b == a + 1 }) return tr("%1\$d à %2\$d", n.first(), n.last())
        return tr("%1\$s et %2\$d", n.dropLast(1).joinToString(", "), n.last())
    }

    /**
     * The song step where frame [sel] starts is heard when the project plays its remix (the song
     * at its place from [com.pocketstudio.model.Remix.startFrame], stretched when it follows the
     * tempo); 0 for another song, before that place, or with no known tempo.
     */
    fun stepOf(p: Project, sel: StemSelection): Int {
        val r = p.remix
        if (r.setId != sel.setId || p.tempoBpm <= 0) return 0
        val rate = StemSelection.SAMPLE_RATE
        val seconds = (sel.startFrame - r.startFrame(rate)).toDouble() / rate * r.stretchFor(p.tempoBpm)
        if (seconds <= 0) return 0
        return kotlin.math.floor(seconds * p.tempoBpm / 15.0 + 1e-6).toInt()  // a sixteenth = 15/bpm s
    }

    /**
     * Stem [stem]'s loudness, mono, one value every [factor] frames: the root mean square of those
     * frames' samples (all channels), so a window's mean power is the same as in the sound itself.
     * Read in pieces of ten seconds: a whole song never sits in memory twice.
     */
    fun energy(src: StemSource, stem: Int, factor: Int): Pcm {
        val out = FloatArray(((src.frames + factor - 1) / factor).toInt())
        val piece = 10L * StemSelection.SAMPLE_RATE / factor * factor
        var start = 0L
        while (start < src.frames) {
            val data = src.read(stem, start, minOf(piece, src.frames - start), fitted = false)
            val frames = data.size / 2
            var f = 0
            while (f < frames) {
                val end = minOf(frames, f + factor)
                var sum = 0.0
                for (i in f * 2 until end * 2) sum += data[i].toDouble() * data[i]
                val k = ((start + f) / factor).toInt()
                if (k < out.size) out[k] = kotlin.math.sqrt(sum / ((end - f) * 2)).toFloat()
                f = end
            }
            if (frames == 0) break
            start += frames
        }
        return Pcm(out, 1, StemSelection.SAMPLE_RATE / factor)
    }

    /** Nothing above -80 dB: the separation left only silence there. */
    fun isSilent(pcm: Pcm): Boolean = pcm.data.none { kotlin.math.abs(it) > 1e-4f }
}

/** These actions, [hook] first whenever a stretch is to be heard (the Remix's own listening stops: one preview player). */
fun StemBoardActions.beforePlay(hook: () -> Unit): StemBoardActions {
    val actions = this
    return object : StemBoardActions by actions {
        override fun play(sel: StemSelection) {
            hook()
            actions.play(sel)
        }
    }
}
