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
import com.pocketstudio.model.firstFreeRemixCell
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

    /** [pcm] on live pad [pad], named [name]; [bpm]: its tempo (a loop then follows the project's, 0: unknown). */
    suspend fun putOnLivePad(pcm: Pcm, name: String, pad: Int, bpm: Double = 0.0)

    /**
     * [pcm] kept in « Ma bibliothèque » named [name], filed under [kind], at [bpm] with song
     * [setId]'s key; the sound as kept (null: the library is not available here).
     */
    suspend fun keepInLibrary(pcm: Pcm, name: String, kind: String, bpm: Double, setId: String): com.pocketstudio.model.MySound? = null

    /** The song step the transport plays now, -1 when it is stopped. */
    fun songStepNow(): Int

    /** The project stops playing (listening to a stretch alone: never two sounds at once). */
    fun pauseMusic()

    /** Lot C: the cut screen's pieces kept in the project ([Project.songCuts]), no undo step; a stand-in may ignore them. */
    fun saveCuts(cuts: com.pocketstudio.model.SongCuts) {}

    /**
     * Lot D2: [pcm] (cut at [bpm], the song's tempo) in brick [scene]'s cell of the Live grid's remix
     * column, named [name], taken from stem [stem] at [fromSeconds] into the song; one undo step.
     */
    suspend fun putInRemixCell(pcm: Pcm, name: String, scene: Int, bpm: Double, stem: Int, fromSeconds: Double) {}
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
    /** Lot C: several pieces sent one after the other (live pads, instruments). */
    private var batch: Job? = null
    private var listening: Job? = null

    override fun dismissMessage() = _state.update { it.copy(message = null) }

    // --- Listening -------------------------------------------------------------------------------

    override fun play(sel: StemSelection) = play(sel, loop = false)

    override fun play(sel: StemSelection, loop: Boolean) {
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
            // « Boucle »: once it has played to its end, it starts again (off the player's callback).
            lateinit var again: () -> Unit
            again = {
                if (listening === me) {
                    if (!loop) ended() else scope.launch { if (listening === me && !player.play(pcm, again)) ended() }
                }
            }
            val started = player.play(pcm, again)
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
        route(sel, dest)
    }

    /** [send] without the busy check (a batch of pieces sends them one after the other). */
    private fun route(sel: StemSelection, dest: StemDestination) {
        if (sel.frames <= 0) return say(tr("Choisis d'abord un morceau de la piste : glisse le doigt sur sa courbe."))
        val p = host.project()
        val french = StemRouting.stemFrench(sel.stem)
        val label = StemRouting.stemLabel(sel.stem)
        val seconds = sel.frames / StemSelection.SAMPLE_RATE.toDouble()
        val short = dest !is StemDestination.Track
        if (short && seconds > PAD_MAX_SECONDS) {
            return say(tr("Trop long pour un pad : %d secondes au plus. Choisis un morceau plus court.", PAD_MAX_SECONDS))
        }
        if (!short && seconds > TRACK_MAX_SECONDS) {
            return say(tr("Trop long pour une piste : %d minutes au plus. Choisis un morceau plus court.", TRACK_MAX_SECONDS / 60))
        }
        when (dest) {
            // Audit ultra round 3, zone 1, n° 4 (seat E d, back from round 2): « Sur les pads » took Tom médium, then Tom
            // aigu, while eight live pads were empty. A drum pad always holds a kit sound: a piece goes on an EMPTY pad (a
            // live pad, Pad Live and Live › Pads play it), never over a drum sound unasked; none empty: which drum pad, asked.
            is StemDestination.Pad if dest.pad == null -> {
                val pad = freeLivePad(p) ?: return ask(listOf(sel))
                toLivePad(sel, pad, p)
            }
            is StemDestination.Pad -> {
                val lane = StemRouting.padFor(p, dest.pad) ?: return ask(listOf(sel))
                val name = StemRouting.nextName(p.drums.lanes.mapNotNull { it.sample?.name }, french)
                // Audit ultra round 2, zone 1, n° 5: the kit's own sound it replaces said too (« Tom médium »), not only a sample.
                val replaced = com.pocketstudio.app.rhythm.padName(p, lane)
                launchWork(tr("%s vers le pad…", label)) {
                    val pcm = cut(sel, fitted = true) ?: return@launchWork null
                    host.putOnPads(listOf(PadCut(lane, pcm, name)))
                    tr("« %1\$s » remplace « %2\$s » sur le pad %3\$d (la flèche retour le remet).", DefaultNames.shownSample(name), replaced, lane + 1)
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
            is StemDestination.Mine -> {
                val song = p.remix.takeIf { it.setId == sel.setId }?.name.orEmpty().trim()
                val name = if (song.isEmpty()) french else "$french · $song"
                launchWork(tr("%s vers ma bibliothèque…", label)) {
                    var bpm = 0.0
                    val pcm = cut(sel, fitted = false) { bpm = it } ?: return@launchWork null
                    val kept = host.keepInLibrary(pcm, name, com.pocketstudio.model.MySounds.kindOfStem(sel.stem, pcm.seconds), bpm, sel.setId)
                        ?: return@launchWork tr("Ce son n'a pas pu être gardé (%s).", tr("bibliothèque indisponible"))
                    com.pocketstudio.app.library.keptNotice(kept)
                }
            }
            // Lot D2: a passage of the project's own song goes in the Live grid's remix column, the
            // first brick without a passage (a song of another set still goes on a live pad).
            // Round 3, zone 1, n° 4 (seat E d 33): the cut screen opened from the library « pour le pad live 1 »: that pad.
            is StemDestination.Live if liveTarget?.let { p.live.at(it) == null } == true -> {
                val pad = liveTarget!!
                liveTarget = null
                toLivePad(sel, pad, p)
            }
            is StemDestination.Live if p.remix.setId != null && p.remix.setId == sel.setId -> {
                val scene = p.firstFreeRemixCell()
                    ?: return say(tr("Chaque brique a déjà un passage dans la colonne Remix : enlèves-en un dans Live."))
                val letter = com.pocketstudio.model.Scene.NAMES.getOrElse(scene) { "?" }
                launchWork(tr("%1\$s vers la case Remix de la brique %2\$s…", label, letter)) {
                    var bpm = 0.0
                    val pcm = cut(sel, fitted = false) { bpm = it } ?: return@launchWork null
                    host.putInRemixCell(pcm, french, scene, bpm, sel.stem, sel.startFrame / StemSelection.SAMPLE_RATE.toDouble())
                    tr("« %1\$s » est dans la case Remix de la brique %2\$s.", label, letter)
                }
            }
            is StemDestination.Live -> {
                val pad = freeLivePad(p) ?: return say(tr("Les 16 pads live sont pris : vides-en un dans Live › Pads."))
                toLivePad(sel, pad, p)
            }
        }
    }

    /**
     * Round 3, zone 1, n° 4: the live pad the cut screen was opened for (« Ma bibliothèque » of live pad N › « Découper »):
     * « Dans le Live » puts the piece there (it went to the remix column's brick A, the whole song playing there).
     */
    var liveTarget: Int? = null

    /** The first empty live pad, or null. */
    private fun freeLivePad(p: Project): Int? = (0 until LivePads.MAX).firstOrNull { p.live.at(it) == null }

    /** [sel] on live pad [pad] (empty), said. */
    private fun toLivePad(sel: StemSelection, pad: Int, p: Project) {
        val french = StemRouting.stemFrench(sel.stem)
        val label = StemRouting.stemLabel(sel.stem)
        val name = StemRouting.nextName(p.live.clips.mapNotNull { it?.name }, french)
        launchWork(tr("%1\$s vers le pad live %2\$d…", label, pad + 1)) {
            var bpm = 0.0
            val pcm = cut(sel, fitted = true) { bpm = it } ?: return@launchWork null
            host.putOnLivePad(pcm, name, pad, bpm)
            tr("« %1\$s » est sur le pad live %2\$d.", DefaultNames.shownSample(name), pad + 1)
        }
    }

    // --- Lot C: « Découper la chanson » ---------------------------------------------------------

    override fun savedCuts(setId: String, frames: Long): com.pocketstudio.model.SongCuts =
        host.project().songCuts.sanitized(setId, frames)

    override fun saveCuts(cuts: com.pocketstudio.model.SongCuts) = host.saveCuts(cuts)

    override suspend fun autoCuts(setId: String, stem: Int, mode: com.pocketstudio.model.AutoCut, from: Long, to: Long): List<LongRange>? =
        try {
            host.withSong(setId) { src -> SongCutFinder.find(src, edits, stem, mode, from, to) }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            com.pocketstudio.app.report.AppLog.add("Découpe : coupes auto impossibles (${e.message})")
            null
        }

    override fun sendPieces(sels: List<StemSelection>, dest: StemDestination) {
        val pieces = sels.filter { it.frames > 0 }.sortedBy { it.startFrame }
        if (pieces.isEmpty()) return say(tr("Coche d'abord un ou plusieurs bouts."))
        if (pieces.size == 1 && dest !is StemDestination.Mine) return send(pieces.single(), dest)
        if (refusedBusy()) return
        val p = host.project()
        val french = StemRouting.stemFrench(pieces.first().stem)
        val label = StemRouting.stemLabel(pieces.first().stem)
        val longest = pieces.maxOf { it.frames } / StemSelection.SAMPLE_RATE.toDouble()
        if (dest !is StemDestination.Track && longest > PAD_MAX_SECONDS) {
            return say(tr("Trop long pour un pad : %d secondes au plus. Choisis un morceau plus court.", PAD_MAX_SECONDS))
        }
        when (dest) {
            // Round 3, zone 1, n° 4: the empty live pads, one piece each (by the batch below); none empty: asked.
            is StemDestination.Pad if freeLivePad(p) == null -> ask(pieces)
            is StemDestination.Track -> {
                val span = pieces.last().endFrame - pieces.first().startFrame
                if (span / StemSelection.SAMPLE_RATE.toDouble() > TRACK_MAX_SECONDS) {
                    return say(tr("Trop long pour une piste : %d minutes au plus. Choisis un morceau plus court.", TRACK_MAX_SECONDS / 60))
                }
                if (p.addAudioTrack() == null) return say(tr("Les huit pistes audio sont prises : vide-en une ou pose le son sur un pad."))
                val name = StemRouting.nextName(p.audioTracks.map { it.name }, french).take(InstrumentTrack.MAX_NAME)
                val step = StemRouting.stepOf(p, pieces.first())
                launchWork(tr("%s vers une piste audio…", label)) {
                    var bpm = 0.0
                    val joined = host.withSong(pieces.first().setId) { src ->
                        bpm = src.fittedBpm
                        joinedAtTheirPlace(src, pieces)
                    } ?: return@launchWork gone()
                    if (StemRouting.isSilent(joined)) return@launchWork tr("Ce morceau de la piste est muet : rien n'a été posé.")
                    host.putOnNewAudioTrack(joined, name, step, bpm)
                        ?: return@launchWork tr("Les huit pistes audio sont prises : vide-en une ou pose le son sur un pad.")
                    tr("%1\$d bouts sur la nouvelle piste audio « %2\$s », à leur place dans la chanson.", pieces.size, DefaultNames.shown(name))
                }
            }
            is StemDestination.Mine -> {
                val song = p.remix.takeIf { it.setId == pieces.first().setId }?.name.orEmpty().trim()
                val name = if (song.isEmpty()) french else "$french · $song"
                launchWork(tr("%s vers ma bibliothèque…", label)) {
                    var kept = 0
                    var last: com.pocketstudio.model.MySound? = null
                    for (sel in pieces) {
                        var bpm = 0.0
                        val pcm = cut(sel, fitted = false) { bpm = it } ?: continue
                        last = host.keepInLibrary(pcm, name, com.pocketstudio.model.MySounds.kindOfStem(sel.stem, pcm.seconds), bpm, sel.setId)
                            ?: return@launchWork tr("Ce son n'a pas pu être gardé (%s).", tr("bibliothèque indisponible"))
                        kept++
                    }
                    when {
                        kept == 0 -> null
                        kept == 1 -> last?.let { com.pocketstudio.app.library.keptNotice(it) }
                        song.isEmpty() -> tr("%d bouts gardés dans Ma bibliothèque.", kept)
                        else -> tr("%1\$d bouts gardés dans Ma bibliothèque, dans le dossier %2\$s.", kept, song)
                    }
                }
            }
            else -> {
                // The live pads and the instruments take one piece at a time, each by its own route.
                batch = scope.launch {
                    val said = ArrayList<String>()
                    try {
                        for (sel in pieces) {
                            _state.update { it.copy(message = null) }
                            route(sel, dest)
                            work?.join()
                            _state.value.message?.let { if (it !in said) said += it }
                        }
                    } finally {
                        _state.update { it.copy(busy = null, message = said.joinToString("\n").ifEmpty { null }) }
                        batch = null
                    }
                }
            }
        }
    }

    /** [pieces] (one stem) on [lanes] in order, one undo step; those past the last lane are said, not placed. */
    private fun putPiecesOn(pieces: List<StemSelection>, lanes: List<Int>) {
        val p = host.project()
        val french = StemRouting.stemFrench(pieces.first().stem)
        val label = StemRouting.stemLabel(pieces.first().stem)
        val first = StemRouting.nextNumber(p.drums.lanes.mapNotNull { it.sample?.name }, french)
        val placed = pieces.take(lanes.size)
        launchWork(tr("%s vers les pads…", label)) {
            val cuts = placed.mapIndexedNotNull { k, sel -> cut(sel, fitted = true)?.let { PadCut(lanes[k], it, "$french ${first + k}") } }
            if (cuts.isEmpty()) return@launchWork null
            host.putOnPads(cuts)
            val left = pieces.size - placed.size
            trCount(cuts.size, "1 bout sur le pad %2\$s.", "%1\$d bouts sur les pads %2\$s.", cuts.size, StemRouting.padsText(cuts.map { it.lane })) +
                if (left > 0) " " + trCount(left, "Plus de pad libre pour le dernier bout.", "Plus de pad libre pour les %d derniers bouts.", left) else ""
        }
    }

    /** No pad free for [pieces]: the user is asked which pad they go on ([pickPad]). */
    private fun ask(pieces: List<StemSelection>) {
        val p = host.project()
        _state.update { it.copy(message = null, pick = PadPick(pieces, List(DrumPattern.PAD_COUNT) { lane -> com.pocketstudio.app.rhythm.padName(p, lane) })) }
    }

    override fun pickPad(lane: Int) {
        val pick = _state.value.pick ?: return
        _state.update { it.copy(pick = null) }
        if (lane !in 0 until DrumPattern.PAD_COUNT || refusedBusy()) return
        if (pick.sels.size == 1) route(pick.sels.single(), StemDestination.Pad(lane))
        // Relecture tour 1, M1: only the pad touched is replaced (as the question says), the other
        // pieces take the pads freed since, the rest is said, never the kit's next pads overwritten.
        // Round 3, zone 1, n° 4: only that one (a « free » kit pad still holds a drum sound): the rest is said.
        else putPiecesOn(pick.sels, listOf(lane))
    }

    override fun dismissPick() = _state.update { it.copy(pick = null) }

    /**
     * [pieces] of one stem as one sound from the first's start to the last's end, each at its own
     * place (silence between them, their fades kept), as the project plays the song (fitted).
     */
    private fun joinedAtTheirPlace(src: StemSource, pieces: List<StemSelection>): Pcm {
        val from = pieces.first().startFrame
        val span = pieces.last().endFrame - from
        val ratio = if (src.fittedBpm > 0 && src.songBpm > 0) src.songBpm / src.fittedBpm else 1.0
        val out = FloatArray((Math.round(span * ratio) * 2).toInt())
        for (sel in pieces) {
            val pcm = cutOf(src, sel, fitted = true)
            val at = (Math.round((sel.startFrame - from) * ratio) * 2).toInt()
            val n = minOf(pcm.data.size, out.size - at)
            if (n > 0) System.arraycopy(pcm.data, 0, out, at, n)
        }
        return Pcm(out, 2, StemSelection.SAMPLE_RATE)
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
        if (work?.isActive != true && batch?.isActive != true && !host.busy()) return false
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
    val STEMS = listOf("Batterie", "Basse", "Reste", "Voix")

    fun stemFrench(stem: Int): String = STEMS.getOrElse(stem) { "Piste" }

    /** [stemFrench] in the interface's language. */
    fun stemLabel(stem: Int): String = when (stem) {
        0 -> tr("Batterie")
        1 -> tr("Basse")
        2 -> tr("Reste")
        3 -> tr("Voix")
        else -> tr("Piste")
    }

    /**
     * Pad [lane] is free for a cut: no sound of its own, no studio voice, no hit in any scene (one
     * rule for the Remix page's « → Pad » and the board: [com.pocketstudio.model.RemixVoice.padIsFree]).
     */
    fun padIsFree(p: Project, lane: Int): Boolean = com.pocketstudio.model.RemixVoice.padIsFree(p, lane)

    fun freePads(p: Project): List<Int> = com.pocketstudio.model.RemixVoice.freePads(p)

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
