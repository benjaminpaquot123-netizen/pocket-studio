package com.pocketstudio.app.remix

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.model.AutoCut
import com.pocketstudio.model.CutMagnet
import com.pocketstudio.model.CutPiece
import com.pocketstudio.model.Cuts
import com.pocketstudio.model.SongCuts
import com.pocketstudio.model.StemCuts
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/*
 * Lot C (n° 24): what « Découper la chanson » holds while it is open, apart from Compose so that it
 * is tested on its own: the stem shown, its numbered pieces ([SongCuts], pure rules in [Cuts]), the
 * pieces ticked, the play head the tools act at, the magnet, and the undo of every change.
 */

/**
 * The cut screen's state for song [setId] ([frames] long at [BOARD_RATE], [bpm] 0: tempo unknown,
 * first beat [firstBeatSeconds]), starting from the cuts [saved]; every change is handed to
 * [onChange] (the project keeps it) and can be undone. It opens in cut mode [mode]: « Un bout »
 * (Benjamin, 2026-11-04: « propre et sans fioriture », two bars only) unless told otherwise.
 */
@Stable
class CutModel(
    val setId: String,
    val frames: Long,
    val bpm: Double,
    firstBeatSeconds: Double,
    saved: SongCuts,
    private val onChange: (SongCuts) -> Unit = {},
    mode: AutoCut = AutoCut.Selection,
) {
    /** One beat in frames (0: no tempo, no magnet, no bars). */
    val beat: Double = Cuts.beatFrames(bpm, BOARD_RATE)
    val firstBeat: Long = Math.round(firstBeatSeconds.coerceAtLeast(0.0) * BOARD_RATE)
    val hasGrid: Boolean get() = beat > 0.0

    var cuts by mutableStateOf(saved.sanitized(setId, frames))
        private set

    /** The stem shown (0 drums, 1 bass, 2 other, 3 voice): the voice first, it is what is cut most. */
    var stem by mutableIntStateOf(VOCALS_STEM)

    /** The orange play head: where « Couper ici », « Début » and « Fin » act; a touch on the curve moves it. */
    var cursor by mutableLongStateOf(0L)

    var magnet by mutableStateOf(if (beat > 0.0) CutMagnet.Beat else CutMagnet.Free)

    /** The cut mode the menu shows: « Un bout » (two bars) or the pieces of the auto cuts. */
    var mode by mutableStateOf(mode)

    /** « Un bout »: the two bars « Début » and « Fin », no numbered pieces. */
    val single: Boolean get() = mode == AutoCut.Selection

    /** The auto cuts the pieces were last made with (the last asked for this stem, else the stem's own). */
    val auto: AutoCut get() = cuts.of(stem)?.auto?.let { a -> AutoCut.entries.firstOrNull { it.name == a && it != AutoCut.Selection } } ?: defaultAuto(stem)

    private val history = ArrayDeque<SongCuts>()
    var undoCount by mutableIntStateOf(0)
        private set
    val canUndo: Boolean get() = undoCount > 0

    /** The pieces of the stem shown: the whole stem as one piece while it was never cut. */
    val pieces: List<CutPiece> get() = cuts.of(stem)?.pieces ?: Cuts.whole(frames)

    /**
     * Whether the stem shown was ever cut (else its auto cuts are made when it is first shown): auto
     * cuts asked for, or pieces other than the whole stem (« Un bout » placed alone is no cut).
     */
    val cutYet: Boolean get() = cuts.of(stem)?.let { it.auto.isNotEmpty() || it.pieces != Cuts.whole(frames) } == true

    /** « Un bout » of the stem shown, null until placed. */
    val span: CutPiece? get() = cuts.of(stem)?.span

    val chosen: List<Int> get() = cuts.of(stem)?.chosen.orEmpty()

    /** The piece under the play head, -1 in a gap. */
    val current: Int get() = Cuts.pieceAt(pieces, cursor)

    /** The auto cuts a stem gets first: the voice's phrases, every bar of the others (their attacks without a tempo). */
    fun defaultAuto(stem: Int): AutoCut = when {
        stem == VOCALS_STEM -> AutoCut.Phrases
        hasGrid -> AutoCut.Bars
        else -> AutoCut.Attacks
    }

    /** [frame] as the magnet takes it. */
    fun stuck(frame: Long): Long = Cuts.magnet(frame, magnet, beat, firstBeat).coerceIn(0L, frames)

    /** Whether [frame] sits on a beat (or bar) of the song: « sur le temps ». */
    fun onBeat(frame: Long): Boolean = hasGrid && Cuts.magnet(frame, CutMagnet.Beat, beat, firstBeat) == frame

    private fun stemCuts(): StemCuts = cuts.of(stem) ?: StemCuts(stem, Cuts.whole(frames))

    /** One undoable change of the stem shown; false (nothing kept) when it changed nothing. */
    private fun edit(save: Boolean = true, change: (StemCuts) -> StemCuts): Boolean {
        val before = stemCuts()
        val after = change(before)
        if (after == before && cuts.of(stem) != null) return false
        pushUndo()
        cuts = cuts.with(after)
        if (save) onChange(cuts)
        return true
    }

    private fun pushUndo() {
        history.addLast(cuts)
        while (history.size > MAX_UNDO) history.removeFirst()
        undoCount = history.size
    }

    /** The ticks follow the pieces: a new piece is ticked when most of it was a ticked piece. */
    private fun keepChoice(before: StemCuts, pieces: List<CutPiece>): List<Int> {
        val was = before.chosen.mapNotNull { before.pieces.getOrNull(it) }
        return pieces.indices.filter { i ->
            val p = pieces[i]
            was.any { w -> min(w.end, p.end) - max(w.start, p.start) > p.frames / 2 }
        }
    }

    private fun withPieces(s: StemCuts, pieces: List<CutPiece>) = s.copy(pieces = pieces, chosen = keepChoice(s, pieces))

    /** « Couper ici »: a cut at [at] (the play head, or where the music plays), stuck by the magnet. */
    fun cutHere(at: Long = cursor): Boolean = edit { s -> withPieces(s, Cuts.split(s.pieces, stuck(at))) }

    /** « Début »: the piece under the play head starts there (« Un bout »: its « Début » goes to the play head). */
    fun setStart(): Boolean {
        if (single) return setSpanEdge(end = false, at = cursor)
        val i = current.takeIf { it >= 0 } ?: return false
        return edit { s -> withPieces(s, Cuts.setStart(s.pieces, i, stuck(cursor))) }
    }

    /** « Fin »: the piece under the play head ends there (« Un bout »: its « Fin » goes to the play head). */
    fun setEnd(): Boolean {
        if (single) return setSpanEdge(end = true, at = cursor)
        val i = current.takeIf { it >= 0 } ?: return false
        return edit { s -> withPieces(s, Cuts.setEnd(s.pieces, i, stuck(cursor), frames)) }
    }

    /** « Enlever »: the ticked pieces go, else the one under the play head. */
    fun remove(): Boolean {
        val which = chosen.ifEmpty { listOfNotNull(current.takeIf { it >= 0 }) }
        if (which.isEmpty()) return false
        return edit { s -> s.copy(pieces = Cuts.remove(s.pieces, which), chosen = emptyList()) }
    }

    /** « Boucle »: the piece under the play head (else the first ticked one) becomes a clean loop, or a plain piece again. */
    fun loop(): Int {
        val i = current.takeIf { it >= 0 } ?: chosen.firstOrNull() ?: return -1
        edit { s -> withPieces(s, Cuts.loopPiece(s.pieces, i, beat, firstBeat, frames)) }
        return i
    }

    /**
     * The first auto cuts of a stem, made by the screen when it is first shown (no act of the
     * user's: no undo step), and « Un bout » placed around the first of them when it was not yet
     * (else the first two bars from [shownFrom]).
     */
    fun firstCuts(mode: AutoCut, from: Long, to: Long, found: List<LongRange>, shownFrom: Long = 0L) {
        val s = stemCuts()
        val pieces = if (found.isEmpty()) s.pieces else Cuts.replaceIn(s.pieces, from, to, found)
        val auto = if (found.isEmpty()) s.auto else mode.name
        val span = s.span ?: Cuts.firstSpan(found, shownFrom, frames, beat, firstBeat, BOARD_RATE)?.let(::onGrid)
        cuts = cuts.with(s.copy(pieces = pieces, chosen = keepChoice(s, pieces), auto = auto, span = span))
        onChange(cuts)
    }

    /** « Un bout » placed when a stem cut before has none (a project of before): on its first piece, else two bars from [shownFrom]. */
    fun ensureSpan(shownFrom: Long = 0L) {
        if (span != null) return
        val s = stemCuts()
        val first = s.pieces.takeIf { it != Cuts.whole(frames) }.orEmpty().map { it.start until it.end }
        val placed = Cuts.firstSpan(first, shownFrom, frames, beat, firstBeat, BOARD_RATE)?.let(::onGrid) ?: return
        cuts = cuts.with(s.copy(span = placed))
        onChange(cuts)
    }

    /** « Un bout » first placed: on the magnet's beats, so that its length reads in beats or bars. */
    private fun onGrid(p: CutPiece): CutPiece = Cuts.spanOnGrid(p, magnet, beat, firstBeat, frames)

    /**
     * « Début ici » (or « Fin ici », [end]) of « Un bout » at [at], stuck by the magnet: one undo
     * step; past the other bar the piece moves there whole (see [Cuts.spanStartHere]).
     */
    fun setSpanEdge(end: Boolean, at: Long): Boolean {
        val now = span ?: return false
        val f = stuck(at)
        return edit { s -> s.copy(span = if (end) Cuts.spanEndHere(now, f, frames) else Cuts.spanStartHere(now, f, frames)) }
    }

    /**
     * What is heard when a bar of « Un bout » is let go (Début: [end] false, the piece from its new
     * start; Fin: its last [PREVIEW_SECONDS] up to the new end), so each move is judged by ear at once.
     */
    fun previewAfter(end: Boolean, fade: BoardFade = BoardFade.Small): StemSelection? {
        val sp = span ?: return null
        val from = if (end) max(sp.start, sp.end - PREVIEW_SECONDS * BOARD_RATE) else sp.start
        return StemSelection(setId, stem, from, sp.end, fade.inMs, fade.outMs)
    }

    /**
     * TalkBack's « avancer / reculer d'un temps » on « Début » (or « Fin », [end]): [beats] beats
     * later (earlier when negative), on the beats; a quarter of a second each without a tempo.
     */
    fun nudgeSpan(end: Boolean, beats: Int): Boolean {
        val now = span ?: return false
        val edge = if (end) now.end else now.start
        val to = if (hasGrid) Cuts.magnet(Math.round(edge + beats * beat), CutMagnet.Beat, beat, firstBeat)
        else edge + beats.toLong() * BOARD_RATE / 4
        return edit { s -> s.copy(span = if (end) Cuts.spanEnd(now, to, frames) else Cuts.spanStart(now, to)) }
    }

    /** Auto cuts [mode] found between [from] and [to] ([found]) in place of the pieces there. */
    fun applyAuto(mode: AutoCut, from: Long, to: Long, found: List<LongRange>): Boolean = edit { s ->
        val pieces = Cuts.replaceIn(s.pieces, from, to, found)
        s.copy(pieces = pieces, chosen = keepChoice(s, pieces).filter { pieces[it].end <= from || pieces[it].start >= to }, auto = mode.name)
    }

    fun toggleChosen(i: Int) {
        if (i !in pieces.indices) return
        val now = chosen.toMutableSet().apply { if (!add(i)) remove(i) }.sorted()
        // Ticking is no cut to undo: kept without an undo step.
        cuts = cuts.with(stemCuts().copy(chosen = now))
        onChange(cuts)
    }

    /**
     * Audit ultra round 3, zone 1, n° 9 (seat B c): a touch on a piece of the curve ticks it (or unticks it), as its line in
     * the list does: the pieces of « Phrases » could not be chosen where they are seen. Outside a piece: nothing.
     */
    fun touchPiece(at: Long): Boolean {
        if (single) return false
        val i = Cuts.pieceAt(pieces, at).takeIf { it in pieces.indices } ?: return false
        toggleChosen(i)
        return true
    }

    /**
     * Tour 6, control Ba-2 (A7): a touch on a piece's body in « Phrases », « Attaques », « Mesures » chooses it — it is
     * added, never taken out, so placing the play head again in a chosen piece keeps it chosen (its number or its tile
     * untick it). False in « Un bout » or off the pieces.
     */
    fun choosePieceAt(at: Long): Boolean {
        if (single) return false
        val i = Cuts.pieceAt(pieces, at).takeIf { it in pieces.indices } ?: return false
        if (i !in chosen) toggleChosen(i)
        return true
    }

    /** « Tout choisir », or none when all are. */
    fun chooseAll() {
        val all = pieces.indices.toList()
        cuts = cuts.with(stemCuts().copy(chosen = if (chosen.size == all.size) emptyList() else all))
        onChange(cuts)
    }

    // --- A cut moved by a finger: one undo step for the whole move, saved when it lifts ---------

    private var moving: Cuts.Line? = null
    private var movingFrom: List<CutPiece>? = null

    /** « Un bout »: the bar moving (false « Début », true « Fin »), null when none; the span before the move. */
    private var movingEdge: Boolean? = null
    private var spanFrom: CutPiece? = null

    /** Whether the moving cut really moved yet: the undo step and the save wait for that. */
    private var moved = false

    /**
     * The cut line within [slop] frames of [frame] starts following the finger; false when none is
     * there, or when another line is within twice [slop] of it (the whole song shown, cut every bar:
     * a touch places the play head, the lines are grabbed once zoomed in).
     */
    fun beginMove(frame: Long, slop: Long): Boolean {
        if (single) {
            val edge = spanEdgeNear(frame, slop) ?: return false
            movingEdge = edge
            spanFrom = span
            moved = false
            return true
        }
        val line = grabbable(frame, slop) ?: return false
        moving = line
        movingFrom = pieces
        moved = false
        return true
    }

    /**
     * « Un bout »: the bar a finger at [frame] grabs within [slop] frames (false « Début », true
     * « Fin »), the nearer one; both within reach (a short piece): the one on the finger's side.
     */
    fun spanEdgeNear(frame: Long, slop: Long): Boolean? {
        val sp = span ?: return null
        val toStart = abs(frame - sp.start)
        val toEnd = abs(frame - sp.end)
        return when {
            toStart > slop && toEnd > slop -> null
            toStart <= slop && toEnd <= slop -> if (frame > sp.end) true else if (frame < sp.start) false else toEnd < toStart
            else -> toEnd <= slop
        }
    }

    /** The line a finger at [frame] grabs with a zone of [slop] frames, null when none (or too crowded). */
    fun grabbable(frame: Long, slop: Long): Cuts.Line? {
        val all = Cuts.lines(pieces)
        val i = all.indices.filter { abs(all[it].frame - frame) <= slop }.minByOrNull { abs(all[it].frame - frame) } ?: return null
        val at = all[i].frame
        val near = listOfNotNull(all.getOrNull(i - 1), all.getOrNull(i + 1)).any { abs(it.frame - at) <= 2 * slop }
        return if (near) null else all[i]
    }

    /** Where the moved cut is now (stuck by the magnet), null when none moves. */
    var movedTo by mutableStateOf<Long?>(null)
        private set

    fun moveTo(frame: Long) {
        movingEdge?.let { end ->
            val from = spanFrom ?: return
            val now = if (end) Cuts.spanEnd(from, stuck(frame), frames) else Cuts.spanStart(from, stuck(frame))
            movedTo = if (end) now.end else now.start
            if (!moved) {
                if (now == from) return
                pushUndo()
                moved = true
            }
            cuts = cuts.with(stemCuts().copy(span = now))
            return
        }
        val line = moving ?: return
        val from = movingFrom ?: return
        val now = Cuts.moveLine(from, line, stuck(frame), frames)
        movedTo = Cuts.movedLine(now, line).frame
        if (!moved) {
            if (now == from) return
            pushUndo()  // one undo step for the whole move, on its first real change
            moved = true
        }
        val s = stemCuts()
        cuts = cuts.with(withPieces(s.copy(pieces = from), now))
    }

    /**
     * The finger lifted (or a second one came): saved once when the cut really moved. Returns the
     * bar of « Un bout » that really moved (false « Début », true « Fin »), null otherwise.
     */
    fun endMove(): Boolean? {
        if (moving == null && movingEdge == null) return null
        val edge = movingEdge.takeIf { moved }
        moving = null
        movingEdge = null
        spanFrom = null
        movingFrom = null
        movedTo = null
        if (moved) onChange(cuts)
        moved = false
        return edge
    }

    fun undo() {
        val before = history.removeLastOrNull() ?: return
        undoCount = history.size
        cuts = before
        onChange(cuts)
    }

    // --- What the actions take ---------------------------------------------------------------

    fun selection(i: Int, fade: BoardFade = BoardFade.Small): StemSelection? =
        pieces.getOrNull(i)?.let { StemSelection(setId, stem, it.start, it.end, fade.inMs, fade.outMs) }

    /** « Un bout » as the actions take it. */
    fun spanSelection(fade: BoardFade = BoardFade.Small): StemSelection? =
        span?.let { StemSelection(setId, stem, it.start, it.end, fade.inMs, fade.outMs) }

    /** What the send buttons send: « Un bout » by itself, else the ticked pieces. */
    fun toSend(): List<StemSelection> = if (single) listOfNotNull(spanSelection()) else chosenSelections()

    /** The ticked pieces, in time order. */
    fun chosenSelections(): List<StemSelection> = chosen.sorted().mapNotNull { selection(it) }

    /** Their total length, in frames. */
    val chosenFrames: Long get() = chosen.sumOf { pieces.getOrNull(it)?.frames ?: 0L }

    companion object {
        const val MAX_UNDO = 50

        /** « Fin » let go: this many seconds before it are heard. */
        const val PREVIEW_SECONDS = 2L
    }
}

/** The auto cuts, worked on a song's stem (a worker thread, inside [StemBoardHost.withSong]). */
object SongCutFinder {
    /** The loudness is read one value every this many frames (6 kHz), as the phrases of the stem board. */
    const val FACTOR = 8

    /** 10 ms windows for the attacks: this many loudness values each. */
    private const val ATTACK_VALUES = 60

    fun find(src: StemSource, edits: StemEdits, stem: Int, mode: AutoCut, from: Long, to: Long): List<LongRange> {
        val a = from.coerceIn(0L, src.frames)
        val z = to.coerceIn(a, src.frames)
        if (z - a < Cuts.MIN_FRAMES) return emptyList()
        val beat = Cuts.beatFrames(src.songBpm, StemSelection.SAMPLE_RATE)
        return when (mode) {
            // « Un bout » is placed, not found.
            AutoCut.Selection -> emptyList()
            AutoCut.Bars -> Cuts.bars(a, z, beat, src.firstBeatFrame)
            AutoCut.Phrases -> {
                val energy = energyIn(src, stem, a, z)
                val beatE = if (beat > 0) Math.round(beat / FACTOR) else null
                edits.phrases(energy, beatE, Cuts.MAX_PIECES, Math.floorDiv(src.firstBeatFrame - a, FACTOR.toLong()))
                    .map { (a + it.first * FACTOR) until min(z, a + (it.last + 1) * FACTOR) }
                    .filter { !it.isEmpty() }
            }
            AutoCut.Attacks -> {
                val e = energyIn(src, stem, a, z).data
                val windows = FloatArray(e.size / ATTACK_VALUES) { w ->
                    var sum = 0.0
                    for (i in w * ATTACK_VALUES until (w + 1) * ATTACK_VALUES) sum += e[i].toDouble() * e[i]
                    sqrt(sum / ATTACK_VALUES).toFloat()
                }
                Cuts.attacks(windows, ATTACK_VALUES * FACTOR, a, StemSelection.SAMPLE_RATE).map { it.first until min(z, it.last + 1) }
            }
        }
    }

    /**
     * Stem [stem]'s loudness between [from] and [to], mono, one root mean square every [FACTOR]
     * frames, read ten seconds at a time (the window of a phone's screen, or the whole song).
     */
    fun energyIn(src: StemSource, stem: Int, from: Long, to: Long): Pcm {
        val out = FloatArray(((to - from + FACTOR - 1) / FACTOR).toInt())
        val piece = 10L * StemSelection.SAMPLE_RATE
        var start = from
        while (start < to) {
            val data = src.read(stem, start, min(piece, to - start), fitted = false)
            val got = data.size / 2
            if (got == 0) break
            var f = 0
            while (f < got) {
                val end = min(got, f + FACTOR)
                var sum = 0.0
                for (i in f * 2 until end * 2) sum += data[i].toDouble() * data[i]
                val k = ((start - from + f) / FACTOR).toInt()
                if (k < out.size) out[k] = sqrt(sum / ((end - f) * 2)).toFloat()
                f = end
            }
            start += got
        }
        return Pcm(out, 1, StemSelection.SAMPLE_RATE / FACTOR)
    }
}
