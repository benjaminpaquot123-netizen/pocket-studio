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
 * [onChange] (the project keeps it) and can be undone.
 */
@Stable
class CutModel(
    val setId: String,
    val frames: Long,
    val bpm: Double,
    firstBeatSeconds: Double,
    saved: SongCuts,
    private val onChange: (SongCuts) -> Unit = {},
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

    /** The auto cuts the menu shows (the last asked for this stem, else the stem's own). */
    val auto: AutoCut get() = cuts.of(stem)?.auto?.let { a -> AutoCut.entries.firstOrNull { it.name == a } } ?: defaultAuto(stem)

    private val history = ArrayDeque<SongCuts>()
    var undoCount by mutableIntStateOf(0)
        private set
    val canUndo: Boolean get() = undoCount > 0

    /** The pieces of the stem shown: the whole stem as one piece while it was never cut. */
    val pieces: List<CutPiece> get() = cuts.of(stem)?.pieces ?: Cuts.whole(frames)

    /** Whether the stem shown was ever cut (else its auto cuts are made when it is first shown). */
    val cutYet: Boolean get() = cuts.of(stem) != null

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

    /** « Début »: the piece under the play head starts there. */
    fun setStart(): Boolean {
        val i = current.takeIf { it >= 0 } ?: return false
        return edit { s -> withPieces(s, Cuts.setStart(s.pieces, i, stuck(cursor))) }
    }

    /** « Fin »: the piece under the play head ends there. */
    fun setEnd(): Boolean {
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

    /** « Tout choisir », or none when all are. */
    fun chooseAll() {
        val all = pieces.indices.toList()
        cuts = cuts.with(stemCuts().copy(chosen = if (chosen.size == all.size) emptyList() else all))
        onChange(cuts)
    }

    // --- A cut moved by a finger: one undo step for the whole move, saved when it lifts ---------

    private var moving: Cuts.Line? = null
    private var movingFrom: List<CutPiece>? = null

    /** Whether the moving cut really moved yet: the undo step and the save wait for that. */
    private var moved = false

    /**
     * The cut line within [slop] frames of [frame] starts following the finger; false when none is
     * there, or when another line is within twice [slop] of it (the whole song shown, cut every bar:
     * a touch places the play head, the lines are grabbed once zoomed in).
     */
    fun beginMove(frame: Long, slop: Long): Boolean {
        val line = grabbable(frame, slop) ?: return false
        moving = line
        movingFrom = pieces
        moved = false
        return true
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

    /** The finger lifted (or a second one came): saved once when the cut really moved. */
    fun endMove() {
        if (moving == null) return
        moving = null
        movingFrom = null
        movedTo = null
        if (moved) onChange(cuts)
        moved = false
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

    /** The ticked pieces, in time order. */
    fun chosenSelections(): List<StemSelection> = chosen.sorted().mapNotNull { selection(it) }

    /** Their total length, in frames. */
    val chosenFrames: Long get() = chosen.sumOf { pieces.getOrNull(it)?.frames ?: 0L }

    companion object {
        const val MAX_UNDO = 50
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
