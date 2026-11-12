package com.pocketstudio.model

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/*
 * Lot C (Benjamin's n° 24, mockup docs/maquettes/decoupe-deux.png): « Découper la chanson ». Each
 * stem of the separated song is cut into numbered pieces, like a sampler's slices: proposed by the
 * auto cuts (phrases, attacks, bars), added at the play head (« Couper ici »), trimmed (« Début »,
 * « Fin »), removed (« Enlever »), made a clean loop (« Boucle »), each cut moved by a finger with
 * a magnet. Pure rules here (frames of the stem at 48 kHz); the screen and its undo are the app's
 * (remix/CutModel.kt). Saved in the project ([Project.songCuts]): the song's file never changes.
 */

/** One piece of a stem: frames [start] until [end] of the stem; [loop]: « Boucle » made it a loop. */
@Serializable
data class CutPiece(val start: Long, val end: Long, val loop: Boolean = false) {
    val frames: Long get() = end - start
    operator fun contains(frame: Long): Boolean = frame >= start && frame < end
}

/** Stem [stem]'s pieces in time order (never overlapping, gaps where a piece was removed), the ones ticked ([chosen], indices). */
@Serializable
data class StemCuts(
    val stem: Int,
    val pieces: List<CutPiece> = emptyList(),
    val chosen: List<Int> = emptyList(),
    /** The auto cuts last asked for ([AutoCut] name), empty: none. */
    val auto: String = "",
    /** « Un bout » (Benjamin, 2026-11-04): the one piece between « Début » and « Fin »; null: not placed yet. */
    val span: CutPiece? = null,
)

/** The cuts of separated song [setId], one [StemCuts] per stem that was cut. */
@Serializable
data class SongCuts(val setId: String? = null, val stems: List<StemCuts> = emptyList()) {
    fun of(stem: Int): StemCuts? = stems.firstOrNull { it.stem == stem }

    fun with(cuts: StemCuts): SongCuts = copy(stems = (stems.filter { it.stem != cuts.stem } + cuts).sortedBy { it.stem })

    /**
     * Kept as saved when it is song [setId]'s and fits its [frames]: pieces in order, inside the
     * stem, at least [Cuts.MIN_FRAMES] long, at most [Cuts.MAX_PIECES]; anything else is dropped.
     */
    fun sanitized(setId: String, frames: Long): SongCuts {
        if (this.setId != setId || frames <= 0) return SongCuts(setId)
        return SongCuts(setId, stems.filter { it.stem in 0 until STEMS }.distinctBy { it.stem }.map { s ->
            val pieces = Cuts.ordered(s.pieces.map { CutPiece(it.start.coerceIn(0, frames), it.end.coerceIn(0, frames), it.loop) })
            s.copy(
                pieces = pieces, chosen = s.chosen.filter { it in pieces.indices }.distinct().sorted(), auto = s.auto.take(16),
                span = s.span?.let { Cuts.span(it.start, it.end, frames) },
            )
        })
    }

    companion object {
        const val STEMS = 4
    }
}

/**
 * The cut modes of the « Coupes » menu: [Selection] (« Un bout », first and the default: two bars,
 * « Début » and « Fin », around the one piece to send), then the auto cuts into many pieces: the
 * voice's phrases, the attacks (drums), every bar (needs the tempo).
 */
enum class AutoCut { Selection, Phrases, Attacks, Bars }

/** What a cut sticks to when moved or made: the beats, the bars, nothing. */
enum class CutMagnet { Beat, Bar, Free }

/** The cut screen's pure rules. */
object Cuts {
    /** The shortest piece: 50 ms at 48 kHz (a single hit). */
    const val MIN_FRAMES = 2_400L

    /** Pieces a stem holds at most (the screen lists them all). */
    const val MAX_PIECES = 64

    // --- « Un bout »: one piece between two bars ----------------------------------------------

    /** [start] until [end] as « Un bout » inside a stem of [frames]: at least [MIN_FRAMES] long; null when the stem is shorter. */
    fun span(start: Long, end: Long, frames: Long): CutPiece? {
        if (frames < MIN_FRAMES) return null
        val s = start.coerceIn(0L, frames - MIN_FRAMES)
        return CutPiece(s, end.coerceIn(s + MIN_FRAMES, frames))
    }

    /** « Début » moved to [at]: never past « Fin » less [MIN_FRAMES], never before the stem. */
    fun spanStart(span: CutPiece, at: Long): CutPiece = span.copy(start = at.coerceIn(0L, max(0L, span.end - MIN_FRAMES)), loop = false)

    /** « Fin » moved to [at]: never before « Début » plus [MIN_FRAMES], never past the stem's [frames]. */
    fun spanEnd(span: CutPiece, at: Long, frames: Long): CutPiece =
        span.copy(end = at.coerceIn(span.start + MIN_FRAMES, max(span.start + MIN_FRAMES, frames)), loop = false)

    /**
     * « Début ici » at [at] (the play head): as [spanStart], but at or past « Fin » the piece moves
     * there whole, its length kept, instead of shrinking to a 50 ms sliver nobody sees.
     */
    fun spanStartHere(span: CutPiece, at: Long, frames: Long): CutPiece =
        if (at <= span.end - MIN_FRAMES) spanStart(span, at) else span(at, at + span.frames, frames) ?: span

    /** « Fin ici » at [at]: as [spanEnd], but at or before « Début » the piece moves there whole (it ends at [at]). */
    fun spanEndHere(span: CutPiece, at: Long, frames: Long): CutPiece {
        if (at >= span.start + MIN_FRAMES) return spanEnd(span, at, frames)
        val end = at.coerceIn(MIN_FRAMES, max(MIN_FRAMES, frames))
        return span(max(0L, end - span.frames), end, frames) ?: span
    }

    /**
     * [span] with both bars on the [magnet]'s grid (beats of [beat] frames from [firstBeat]): what
     * « Un bout » is first placed on, so that its length reads in beats or bars; never shorter than
     * one beat (nor than [MIN_FRAMES]); as it is without a tempo or with the magnet « Libre ».
     */
    fun spanOnGrid(span: CutPiece, magnet: CutMagnet, beat: Double, firstBeat: Long, frames: Long): CutPiece {
        if (magnet == CutMagnet.Free || beat <= 0.0) return span
        val unit = if (magnet == CutMagnet.Bar) beat * 4 else beat
        val s = magnet(span.start, magnet, beat, firstBeat).coerceIn(0L, frames)
        var e = magnet(span.end, magnet, beat, firstBeat).coerceAtMost(frames)
        if (e - s < max(MIN_FRAMES, Math.round(unit))) e = min(frames, s + Math.round(unit))
        return span(s, e, frames) ?: span
    }

    /** Seconds « Un bout » takes without a tempo (its bars cannot be counted). */
    const val SPAN_SECONDS = 4

    /**
     * Where « Un bout » is first placed, to be useful at once: around the first of the auto cuts
     * [found] (the first phrase of a voice), else the first two bars from [from] (the part shown;
     * [SPAN_SECONDS] without a tempo), inside the stem's [frames] at [rate].
     */
    fun firstSpan(found: List<LongRange>, from: Long, frames: Long, beat: Double, firstBeat: Long, rate: Int = 48_000, beatsPerBar: Int = 4): CutPiece? {
        found.firstOrNull { it.last + 1 - it.first >= MIN_FRAMES }?.let { return span(it.first, it.last + 1, frames) }
        if (beat <= 0.0) return span(from, from + SPAN_SECONDS.toLong() * rate, frames)
        val bar = beat * beatsPerBar.coerceAtLeast(1)
        // The first bar line at or after [from] (the song's first beat may come late).
        var s = firstBeat + Math.ceil((from - firstBeat) / bar) * bar
        if (s + 2 * bar > frames) s = max(0.0, frames - 2 * bar)
        return span(s.roundToLong(), (s + 2 * bar).roundToLong(), frames)
    }

    /** [pieces] sorted, without empty or too short ones, without overlaps (a later one starts where the earlier ends), at most [MAX_PIECES]. */
    fun ordered(pieces: List<CutPiece>): List<CutPiece> {
        val out = ArrayList<CutPiece>()
        for (p in pieces.sortedBy { it.start }) {
            val start = max(p.start, out.lastOrNull()?.end ?: 0L)
            if (p.end - start >= MIN_FRAMES) out += p.copy(start = start)
            if (out.size == MAX_PIECES) break
        }
        return out
    }

    /** The whole stem as one piece (a stem never cut yet). */
    fun whole(frames: Long): List<CutPiece> = if (frames >= MIN_FRAMES) listOf(CutPiece(0, frames)) else emptyList()

    /** The piece [frame] is in, -1 when none (a gap or outside). */
    fun pieceAt(pieces: List<CutPiece>, frame: Long): Int = pieces.indexOfFirst { frame in it }

    /** « Couper ici »: the piece under [at] split in two there; unchanged when [at] is too near an edge, in a gap, or the stem is full. */
    fun split(pieces: List<CutPiece>, at: Long): List<CutPiece> {
        val i = pieceAt(pieces, at)
        if (i < 0 || pieces.size >= MAX_PIECES) return pieces
        val p = pieces[i]
        if (at - p.start < MIN_FRAMES || p.end - at < MIN_FRAMES) return pieces
        return pieces.toMutableList().apply {
            set(i, p.copy(end = at, loop = false))
            add(i + 1, CutPiece(at, p.end))
        }
    }

    /** « Début »: piece [index] starts at [at] (what was before it in the piece goes); kept at least [MIN_FRAMES] long. */
    fun setStart(pieces: List<CutPiece>, index: Int, at: Long): List<CutPiece> {
        val p = pieces.getOrNull(index) ?: return pieces
        val floor = pieces.getOrNull(index - 1)?.end ?: 0L
        val s = at.coerceIn(floor, p.end - MIN_FRAMES)
        if (s == p.start || s < floor) return pieces
        return pieces.toMutableList().apply { set(index, p.copy(start = s)) }
    }

    /** « Fin »: piece [index] ends at [at] (what was after it in the piece goes); kept at least [MIN_FRAMES] long. */
    fun setEnd(pieces: List<CutPiece>, index: Int, at: Long, frames: Long): List<CutPiece> {
        val p = pieces.getOrNull(index) ?: return pieces
        val ceiling = pieces.getOrNull(index + 1)?.start ?: frames
        val e = at.coerceIn(p.start + MIN_FRAMES, ceiling)
        if (e == p.end || e > ceiling) return pieces
        return pieces.toMutableList().apply { set(index, p.copy(end = e)) }
    }

    /** « Enlever »: pieces [indices] go (a gap is left: the others keep their place in the song). */
    fun remove(pieces: List<CutPiece>, indices: Collection<Int>): List<CutPiece> = pieces.filterIndexed { i, _ -> i !in indices }

    /**
     * A cut line on screen: at [frame], the end of piece [left] and/or the start of piece [right]
     * (-1: none on that side). Two pieces that touch share one line: moving it moves both edges.
     */
    data class Line(val frame: Long, val left: Int, val right: Int)

    /** Every cut line of [pieces], in time order. */
    fun lines(pieces: List<CutPiece>): List<Line> {
        val out = ArrayList<Line>()
        pieces.forEachIndexed { i, p ->
            val last = out.lastOrNull()
            if (last != null && last.left == i - 1 && last.frame == p.start) out[out.size - 1] = last.copy(right = i)
            else out += Line(p.start, -1, i)
            out += Line(p.end, i, -1)
        }
        return out
    }

    /** The line nearest to [frame] within [slop] frames, null when none. */
    fun lineNear(pieces: List<CutPiece>, frame: Long, slop: Long): Line? =
        lines(pieces).filter { abs(it.frame - frame) <= slop }.minByOrNull { abs(it.frame - frame) }

    /**
     * [line] moved to [to]: the edges it holds follow, each piece keeping [MIN_FRAMES] and none
     * crossing its neighbour; the line found again at its new place is [movedLine].
     */
    fun moveLine(pieces: List<CutPiece>, line: Line, to: Long, frames: Long): List<CutPiece> {
        val left = pieces.getOrNull(line.left)
        val right = pieces.getOrNull(line.right)
        if (left == null && right == null) return pieces
        val lo = when {
            left != null -> left.start + MIN_FRAMES
            else -> pieces.getOrNull(line.right - 1)?.end ?: 0L
        }
        val hi = when {
            right != null -> right.end - MIN_FRAMES
            else -> pieces.getOrNull(line.left + 1)?.start ?: frames
        }
        if (hi < lo) return pieces
        val f = to.coerceIn(lo, hi)
        return pieces.toMutableList().apply {
            if (left != null) set(line.left, left.copy(end = f))
            if (right != null) set(line.right, right.copy(start = f))
        }
    }

    /** [line] as it is after [moveLine] to [to] (same pieces, its new frame). */
    fun movedLine(pieces: List<CutPiece>, line: Line): Line =
        line.copy(frame = pieces.getOrNull(line.right)?.start ?: pieces.getOrNull(line.left)?.end ?: line.frame)

    /**
     * The auto cuts [found] (ranges `start until end`) put in place of every piece between [from]
     * and [to] (a piece across an edge keeps its outside part), in order.
     */
    fun replaceIn(pieces: List<CutPiece>, from: Long, to: Long, found: List<LongRange>): List<CutPiece> {
        val kept = ArrayList<CutPiece>()
        for (p in pieces) {
            if (p.end <= from || p.start >= to) { kept += p; continue }
            if (p.start < from) kept += p.copy(end = from, loop = false)
            if (p.end > to) kept += p.copy(start = to, loop = false)
        }
        val fresh = found.mapNotNull { r ->
            val s = max(from, r.first)
            val e = min(to, r.last + 1)
            if (e - s >= MIN_FRAMES) CutPiece(s, e) else null
        }
        return ordered(kept + fresh)
    }

    // --- Magnet ------------------------------------------------------------------------------

    /** The beat length in frames of a song at [bpm] ([rate] frames a second), 0 when unknown. */
    fun beatFrames(bpm: Double, rate: Int): Double = if (bpm > 0 && bpm.isFinite()) 60.0 / bpm * rate else 0.0

    /**
     * [frame] stuck to the nearest beat ([CutMagnet.Beat]) or bar ([CutMagnet.Bar], [beatsPerBar]
     * beats) of a song whose beats last [beat] frames from [firstBeat]; as it is when [magnet] is
     * [CutMagnet.Free] or the tempo is unknown ([beat] 0).
     */
    fun magnet(frame: Long, magnet: CutMagnet, beat: Double, firstBeat: Long, beatsPerBar: Int = 4): Long {
        if (magnet == CutMagnet.Free || beat <= 0.0) return frame
        val unit = if (magnet == CutMagnet.Bar) beat * beatsPerBar.coerceAtLeast(1) else beat
        return (firstBeat + Math.round((frame - firstBeat) / unit) * unit).roundToLong().coerceAtLeast(0L)
    }

    // --- Auto cuts ---------------------------------------------------------------------------

    /** Every bar between [from] and [to] as a piece (none without a tempo), at most [MAX_PIECES]. */
    fun bars(from: Long, to: Long, beat: Double, firstBeat: Long, beatsPerBar: Int = 4): List<LongRange> {
        if (beat <= 0.0 || to <= from) return emptyList()
        val bar = beat * beatsPerBar.coerceAtLeast(1)
        var k = Math.floorDiv(from - firstBeat, bar.roundToLong().coerceAtLeast(1L))
        val out = ArrayList<LongRange>()
        while (out.size < MAX_PIECES) {
            val s = (firstBeat + k * bar).roundToLong()
            val e = (firstBeat + (k + 1) * bar).roundToLong()
            if (s >= to) break
            val a = max(s, from)
            val z = min(e, to)
            if (z - a >= MIN_FRAMES) out += a until z
            k++
        }
        return out
    }

    /** Attacks this much louder than the window before them (dB) start a piece. */
    const val ATTACK_RISE_DB = 6.0

    /** Attacks nearer than this to the previous one (ms) belong to it (a flam, a double hit). */
    const val ATTACK_GAP_MS = 90

    /** Quieter than this under the loudest window (dB), a window is silence: no attack starts there. */
    const val ATTACK_FLOOR_DB = 40.0

    /**
     * The attacks of a sound given by its loudness [envelope] (root mean square of each window of
     * [windowFrames] frames, from frame [offset]) at [rate]: where the level jumps by
     * [ATTACK_RISE_DB] within two windows, above the floor, at least [ATTACK_GAP_MS] apart; the
     * strongest [MAX_PIECES] when there are more. Each piece runs to the next attack (the last
     * one to the end of the envelope), in frames of the stem.
     */
    fun attacks(envelope: FloatArray, windowFrames: Int, offset: Long, rate: Int): List<LongRange> {
        if (envelope.size < 3 || windowFrames <= 0) return emptyList()
        val db = DoubleArray(envelope.size) { val v = envelope[it].toDouble(); if (v > 1e-6) 20.0 * ln(v) / ln(10.0) else -120.0 }
        val loud = db.max()
        if (loud < -60.0) return emptyList()
        val floor = loud - ATTACK_FLOOR_DB
        val gap = max(1, (ATTACK_GAP_MS.toLong() * rate / 1000 / windowFrames).toInt())
        class Hit(val window: Int, val rise: Double)
        val hits = ArrayList<Hit>()
        // Before the envelope: silence (a sound already there at its start is an attack).
        fun at(w: Int) = if (w >= 0) db[w] else -120.0
        for (w in db.indices) {
            if (db[w] < floor) continue
            val rise = db[w] - min(at(w - 1), at(w - 2))
            if (rise < ATTACK_RISE_DB) continue
            // The top of a rise, not its slope: the next window rising more takes it.
            if (w + 1 < db.size && db[w + 1] - min(db[w], at(w - 1)) > rise) continue
            val last = hits.lastOrNull()
            if (last != null && w - last.window < gap) {
                if (rise > last.rise) hits[hits.size - 1] = Hit(w, rise)
                continue
            }
            hits += Hit(w, rise)
        }
        val kept = hits.sortedByDescending { it.rise }.take(MAX_PIECES).sortedBy { it.window }
        val end = offset + envelope.size.toLong() * windowFrames
        return kept.mapIndexed { i, h ->
            // A window starts just before the hit: the piece takes it whole.
            val s = offset + max(0, h.window - 1).toLong() * windowFrames
            val e = kept.getOrNull(i + 1)?.let { offset + max(0, it.window - 1).toLong() * windowFrames } ?: end
            s until e
        }.filter { it.last + 1 - it.first >= MIN_FRAMES }
    }

    // --- Boucle ------------------------------------------------------------------------------

    /**
     * « Boucle »: [piece] made a clean loop of the song at [beat] frames a beat from [firstBeat]:
     * its start on the nearest beat, its length a whole number of beats (1, 2, 4, 8, 16 or 32,
     * the nearest), within the stem; with no tempo, only marked a loop. Again: back to a plain piece.
     */
    fun loop(piece: CutPiece, beat: Double, firstBeat: Long, frames: Long): CutPiece {
        if (piece.loop) return piece.copy(loop = false)
        if (beat <= 0.0) return piece.copy(loop = true)
        val start = magnet(piece.start, CutMagnet.Beat, beat, firstBeat).coerceIn(0L, max(0L, frames - MIN_FRAMES))
        val beats = piece.frames / beat
        val count = LOOP_BEATS.minByOrNull { abs(ln(it / beats.coerceAtLeast(0.25))) } ?: 4
        var end = (start + count * beat).roundToLong()
        if (end > frames) end = frames
        if (end - start < MIN_FRAMES) return piece.copy(loop = true)
        return CutPiece(start, end, loop = true)
    }

    /** [loop] on piece [index] of [pieces], never over its neighbours. */
    fun loopPiece(pieces: List<CutPiece>, index: Int, beat: Double, firstBeat: Long, frames: Long): List<CutPiece> {
        val p = pieces.getOrNull(index) ?: return pieces
        val floor = pieces.getOrNull(index - 1)?.end ?: 0L
        val ceiling = pieces.getOrNull(index + 1)?.start ?: frames
        val l = loop(p, beat, firstBeat, frames)
        val fitted = l.copy(start = l.start.coerceIn(floor, ceiling), end = l.end.coerceIn(floor, ceiling))
        val made = if (fitted.frames >= MIN_FRAMES) fitted else p.copy(loop = l.loop)
        return pieces.toMutableList().apply { set(index, made) }
    }

    private val LOOP_BEATS = listOf(1, 2, 4, 8, 16, 32)
}
