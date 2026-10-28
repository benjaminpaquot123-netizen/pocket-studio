package com.pocketstudio.app.remix

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/*
 * The stem board's arithmetic (B1), apart from Compose so that it is tested on its own: the peaks of
 * each stem at every zoom level, the beat grid of the song, the part of the song on screen and the
 * stretch the finger chose.
 */

/** Where the board reads the stems' peaks. */
fun interface StemPeaks {
    /**
     * The peak (0..1) of each slice of [framesPerBucket] frames of stem [stem], slice i covering
     * frames [i * framesPerBucket, (i + 1) * framesPerBucket) (the last one may be shorter).
     * Blocking: called on a worker thread, never on the main one.
     */
    fun peaks(stem: Int, framesPerBucket: Int): FloatArray
}

/**
 * Peaks read from [read] (interleaved audio of [channels] channels, frames [start] until
 * [start] + count) a few seconds at a time: minutes of a stem never sit whole in memory.
 */
fun stemPeaksFrom(frames: Long, channels: Int = 2, read: (stem: Int, start: Long, count: Long) -> FloatArray): StemPeaks =
    StemPeaks { stem, framesPerBucket ->
        val fpb = framesPerBucket.coerceAtLeast(1).toLong()
        val out = FloatArray(((frames + fpb - 1) / fpb).toInt())
        // Whole buckets per read, about 2.7 s (1 MB of floats).
        val chunk = max(1L, READ_FRAMES / fpb) * fpb
        var at = 0L
        while (at < frames) {
            val want = min(chunk, frames - at)
            val data = read(stem, at, want)
            val got = data.size / channels
            if (got <= 0) break
            var i = 0
            while (i < got) {
                val b = ((at + i) / fpb).toInt()
                if (b >= out.size) break
                val bucketEnd = min(got.toLong(), (b + 1) * fpb - at).toInt()
                var peak = out[b]
                var s = i * channels
                val end = bucketEnd * channels
                while (s < end) {
                    val a = abs(data[s])
                    if (a > peak) peak = a
                    s++
                }
                out[b] = peak
                i = bucketEnd
            }
            at += got
            if (got < want) break
        }
        for (k in out.indices) if (out[k] > 1f) out[k] = 1f
        out
    }

private const val READ_FRAMES = 1L shl 17

/**
 * A song's beats: one every [beat] frames from frame [origin] (its first beat), [BEATS_PER_BAR]
 * to a bar. Null ([of]) when the song's tempo is unknown.
 */
class BeatGrid(bpm: Double, firstBeatSeconds: Double, rate: Int) {
    val beat: Double = 60.0 / bpm * rate
    val origin: Double = firstBeatSeconds * rate
    val bar: Double get() = beat * BEATS_PER_BAR

    /** The beat nearest to [frame] (it may be before 0 or after the end: callers clamp). */
    fun snap(frame: Long): Long = (origin + Math.round((frame - origin) / beat) * beat).roundToLong()

    /** Frame of beat [index] (0: the first beat). */
    fun beatFrame(index: Long): Double = origin + index * beat

    /** Index of the first beat at or after [frame]. */
    fun firstBeatFrom(frame: Double): Long = ceil((frame - origin) / beat - 1e-9).toLong()

    /** The bar (1 = the first) that beat [index] opens, or 0 when it opens none (or is before the song's first bar). */
    fun barOpenedBy(index: Long): Int = if (index >= 0 && index % BEATS_PER_BAR == 0L) (index / BEATS_PER_BAR + 1).toInt() else 0

    companion object {
        const val BEATS_PER_BAR = 4

        fun of(bpm: Double, firstBeatSeconds: Double, rate: Int): BeatGrid? =
            if (bpm > 0 && bpm.isFinite()) BeatGrid(bpm, firstBeatSeconds.coerceAtLeast(0.0), rate) else null
    }
}

/**
 * The peaks of every stem at every zoom level, computed once when the board opens, off the main
 * thread: the finest level is read from the audio, each coarser one (twice as many frames a
 * bucket) is the previous one halved. Drawing then only reads arrays.
 */
@Stable
class PeakLevels(val frames: Long, val stems: Int = 4) {
    /** Frames a bucket of the finest level: a power of 2, at least [MIN_BUCKET], at most [MAX_BUCKETS] buckets a stem. */
    val base: Int = run {
        var b = MIN_BUCKET
        while (frames / b > MAX_BUCKETS) b *= 2
        b
    }

    /** Number of levels: the coarsest still has more than [MIN_COARSE] buckets. */
    val count: Int = run {
        var n = 1
        while (n < 24 && frames / (base.toLong() shl n) > MIN_COARSE) n++
        n
    }

    private val levels = arrayOfNulls<Array<FloatArray>>(stems)

    /** How many stems are ready to draw (read while drawing: they appear one after the other). */
    var ready by mutableIntStateOf(0)
        private set

    fun framesPerBucket(level: Int): Long = base.toLong() shl level

    fun has(stem: Int): Boolean = levels.getOrNull(stem) != null

    /** Peaks of [stem] at [level] (null until computed). */
    fun level(stem: Int, level: Int): FloatArray? = levels.getOrNull(stem)?.getOrNull(level)

    /** The coarsest level whose buckets are no wider than [framesPerColumn]: each column reads one or two buckets. */
    fun levelFor(framesPerColumn: Double): Int {
        var l = 0
        while (l + 1 < count && framesPerBucket(l + 1) <= framesPerColumn) l++
        return l
    }

    /** The highest peak of [stem] at [level] between frames [from] and [to] (0 out of the song or not ready). */
    fun columnPeak(stem: Int, level: Int, from: Long, to: Long): Float {
        val data = level(stem, level) ?: return 0f
        if (to <= 0 || from >= frames || data.isEmpty()) return 0f
        val fpb = framesPerBucket(level)
        val b0 = (max(0L, from) / fpb).toInt()
        val b1 = min(data.size - 1L, (max(from + 1, to) - 1) / fpb).toInt()
        var p = 0f
        var b = b0
        while (b <= b1) {
            if (data[b] > p) p = data[b]
            b++
        }
        return p
    }

    /** Reads every stem's peaks from [source] (on [context]), one stem after the other. A stem that cannot be read stays flat. */
    suspend fun load(source: StemPeaks, context: CoroutineContext) {
        for (stem in 0 until stems) {
            if (levels[stem] != null) continue
            val built = withContext(context) {
                runCatching { build(source.peaks(stem, base), count) }.getOrNull()
            } ?: Array(count) { FloatArray(0) }
            levels[stem] = built
            ready++
        }
    }

    companion object {
        const val MIN_BUCKET = 64
        const val MAX_BUCKETS = 1L shl 17
        const val MIN_COARSE = 64L

        /**
         * [fine] and its [count] - 1 halvings, scaled so that the stem's loudest peak reaches 1: a
         * quiet voice reads as well as the drums. Silence stays flat.
         */
        fun build(fine: FloatArray, count: Int): Array<FloatArray> {
            var top = 0f
            for (v in fine) if (v > top) top = v
            val scale = if (top > 0.02f) 1f / top else 1f
            val out = arrayOfNulls<FloatArray>(count)
            out[0] = FloatArray(fine.size) { min(1f, fine[it] * scale) }
            for (l in 1 until count) {
                val prev = out[l - 1]!!
                out[l] = FloatArray((prev.size + 1) / 2) { i ->
                    val a = prev[2 * i]
                    val b = if (2 * i + 1 < prev.size) prev[2 * i + 1] else 0f
                    max(a, b)
                }
            }
            @Suppress("UNCHECKED_CAST")
            return out as Array<FloatArray>
        }
    }
}

/** The fades of a stretch, as « Fondu » offers them. */
enum class BoardFade(val inMs: Int, val outMs: Int) {
    Small(StemSelection.DEFAULT_FADE_IN_MS, StemSelection.DEFAULT_FADE_OUT_MS),
    Medium(15, 60),
    None(0, 0),
}

/**
 * What the board shows and what the finger chose: the part of the song on screen ([start], the
 * frame at the left edge, and [framesPerPx], the zoom) and the selected stretch ([selStem] -1:
 * none). Moves only on a gesture: nothing scrolls or zooms by itself.
 */
@Stable
class StemBoardState(val frames: Long) {
    var width by mutableFloatStateOf(0f)
        private set
    var framesPerPx by mutableDoubleStateOf(0.0)
        private set
    var start by mutableDoubleStateOf(0.0)
        private set

    var magnet by mutableStateOf(true)
    var fade by mutableStateOf(BoardFade.Small)

    var selStem by mutableIntStateOf(-1)
        private set
    var selStart by mutableLongStateOf(0L)
        private set
    var selEnd by mutableLongStateOf(0L)
        private set

    val hasSelection: Boolean get() = selStem >= 0 && selEnd > selStart

    /** The whole song on screen. */
    val fitFramesPerPx: Double get() = if (width > 0f) max(MIN_FRAMES_PER_PX, frames / width.toDouble()) else 0.0

    /** Zoomed in: « Tout voir » has something to do. */
    val zoomedIn: Boolean get() = width > 0f && framesPerPx < fitFramesPerPx * 0.999

    /** The board's width in pixels; the first one shows the whole song. */
    fun layout(widthPx: Float) {
        if (widthPx <= 0f || widthPx == width) return
        val first = width <= 0f || framesPerPx <= 0.0
        // Turning the phone keeps the same part of the song on screen.
        val shown = width * framesPerPx
        width = widthPx
        if (first) fit() else {
            framesPerPx = (shown / widthPx).coerceIn(MIN_FRAMES_PER_PX, fitFramesPerPx)
            clamp()
        }
    }

    fun fit() {
        framesPerPx = fitFramesPerPx
        start = 0.0
    }

    /** Pinch: [factor] > 1 zooms in, the frame under [focusX] stays under the fingers. */
    fun zoomBy(factor: Float, focusX: Float) {
        if (width <= 0f || factor <= 0f || !factor.isFinite()) return
        val at = start + focusX * framesPerPx
        framesPerPx = (framesPerPx / factor).coerceIn(MIN_FRAMES_PER_PX, fitFramesPerPx)
        start = at - focusX * framesPerPx
        clamp()
    }

    /** The finger moved [dxPx] to the right: the song follows it (earlier frames come in from the left). */
    fun scrollBy(dxPx: Float) {
        start -= dxPx * framesPerPx
        clamp()
    }

    private fun clamp() {
        start = start.coerceIn(0.0, max(0.0, frames - width * framesPerPx))
    }

    fun x(frame: Long): Float = if (framesPerPx > 0) ((frame - start) / framesPerPx).toFloat() else 0f
    fun x(frame: Double): Float = if (framesPerPx > 0) ((frame - start) / framesPerPx).toFloat() else 0f

    fun frameAt(x: Float): Long = (start + x * framesPerPx).toLong().coerceIn(0L, frames)

    fun select(stem: Int, from: Long, to: Long) {
        selStem = stem
        selStart = min(from, to).coerceIn(0L, frames)
        selEnd = max(from, to).coerceIn(0L, frames)
    }

    fun clearSelection() {
        selStem = -1
        selStart = 0L
        selEnd = 0L
    }

    /** The selection as the board's actions take it. */
    fun selection(setId: String): StemSelection? =
        if (hasSelection) StemSelection(setId, selStem, selStart, selEnd, fade.inMs, fade.outMs) else null

    /** Which edge of the selection of [stem] is within [slopPx] of [x]: -1 start, 1 end, 0 none (the nearer one when both are). */
    fun edgeAt(stem: Int, x: Float, slopPx: Float): Int {
        if (!hasSelection || stem != selStem) return 0
        val ds = abs(x - x(selStart))
        val de = abs(x - x(selEnd))
        return when {
            ds > slopPx && de > slopPx -> 0
            de < ds || (de == ds && x > x(selEnd)) -> 1
            else -> -1
        }
    }

    companion object {
        /** The deepest zoom: about 1.5 s across a phone. */
        const val MIN_FRAMES_PER_PX = 48.0

        /** The shortest stretch without a beat grid: 10 ms. */
        const val MIN_FRAMES = 480L
    }
}

/** How a gesture turns into a stretch: snapped to the beats when [grid] is given (magnet on). */
object BoardSelect {
    /** The finger went from [anchor] to [current]: the stretch between them, at least a beat (or [StemBoardState.MIN_FRAMES]). */
    fun drag(anchor: Long, current: Long, grid: BeatGrid?, frames: Long): LongRange {
        if (grid == null) {
            var lo = min(anchor, current).coerceIn(0L, frames)
            var hi = max(anchor, current).coerceIn(0L, frames)
            if (hi - lo < StemBoardState.MIN_FRAMES) {
                if (current >= anchor) hi = min(frames, lo + StemBoardState.MIN_FRAMES) else lo = max(0L, hi - StemBoardState.MIN_FRAMES)
            }
            return lo until hi
        }
        val a = grid.snap(anchor)
        val c = grid.snap(current)
        val beat = grid.beat.roundToLong().coerceAtLeast(1L)
        var lo = min(a, c)
        var hi = max(a, c)
        if (hi == lo) { if (current >= anchor) hi = lo + beat else lo = hi - beat }
        lo = lo.coerceIn(0L, frames)
        hi = hi.coerceIn(0L, frames)
        if (hi <= lo) {
            hi = min(frames, lo + beat)
            lo = max(0L, hi - beat)
        }
        return lo until hi
    }

    /**
     * Moves one edge ([edge] -1 start, 1 end) of [from] until [to] to [frame], snapped when [grid]
     * is given; the stretch keeps at least a beat (or [StemBoardState.MIN_FRAMES]).
     */
    fun moveEdge(from: Long, to: Long, edge: Int, frame: Long, grid: BeatGrid?, frames: Long): LongRange {
        val f = (if (grid != null) grid.snap(frame) else frame).coerceIn(0L, frames)
        val least = if (grid != null) grid.beat.roundToLong().coerceAtLeast(1L) else StemBoardState.MIN_FRAMES
        return if (edge < 0) {
            val s = min(f, to - least).coerceAtLeast(0L)
            s until to
        } else {
            val e = max(f, from + least).coerceAtMost(frames)
            from until e
        }
    }
}

/** "1:05.3": a stretch's length or a time in the song, at [rate] frames a second. */
fun boardClock(frames: Long, rate: Int = BOARD_RATE): String {
    val tenths = (frames * 10 + rate / 2) / rate
    val s = tenths / 10
    return "%d:%02d.%d".format(s / 60, s % 60, tenths % 10)
}

/** The stems on the board are the engine's: 48 kHz frames, the unstretched song. */
const val BOARD_RATE = 48_000

/** Floor of a Double as Long, for grid indices. */
internal fun floorLong(v: Double): Long = floor(v).toLong()
