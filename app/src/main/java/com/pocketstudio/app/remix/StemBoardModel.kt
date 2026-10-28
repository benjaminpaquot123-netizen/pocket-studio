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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/*
 * The stem board's arithmetic (B1), apart from Compose so that it is tested on its own: the peaks of
 * each stem at every zoom level, the beat grid of the song, the part of the song on screen and the
 * stretch the finger chose.
 */

/** Where the board gets each stem's peaks at every zoom ([StemEditor]'s [StemPeaks], built once). */
fun interface StemPeakSource {
    /** The peaks of stem [stem], read from its audio. Blocking: called on a worker thread, never on the main one. */
    fun peaks(stem: Int): StemPeaks
}

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
 * The four stems' peaks, read once when the board opens, off the main thread, one stem after the
 * other (each shows as soon as it is ready). Each stem is scaled so that its loudest peak reaches
 * the top: a quiet voice reads as well as the drums. Drawing then only reads arrays.
 */
@Stable
class BoardPeaks(val stems: Int = 4) {
    private val peaks = arrayOfNulls<StemPeaks>(stems)
    private val scales = FloatArray(stems) { 1f }

    /** How many stems are done (read while drawing: they appear one after the other). */
    var ready by mutableIntStateOf(0)
        private set

    /** The peaks of [stem], null until read (or when they could not be). */
    fun of(stem: Int): StemPeaks? = peaks.getOrNull(stem)

    /** What [stem]'s peaks are multiplied by to fill the height. */
    fun scale(stem: Int): Float = scales.getOrElse(stem) { 1f }

    /** Reads every stem's peaks from [source] on [context]. A stem that cannot be read stays flat. */
    suspend fun load(source: StemPeakSource, context: CoroutineContext) {
        while (ready < stems) {
            val stem = ready
            val read = withContext(context) {
                runCatching {
                    val p = source.peaks(stem)
                    val top = FloatArray(1)
                    p.fill(0L, p.frames, top)
                    p to top[0]
                }.getOrNull()
            }
            if (read != null) {
                peaks[stem] = read.first
                scales[stem] = scaleFor(read.second)
            }
            ready = stem + 1
        }
    }

    companion object {
        /** Scales [top] to 1; silence (or nearly) is not blown up into noise. */
        fun scaleFor(top: Float): Float = if (top > 0.02f) 1f / top else 1f
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
class StemBoardView(val frames: Long) {
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
    /** The finger went from [anchor] to [current]: the stretch between them, at least a beat (or [StemBoardView.MIN_FRAMES]). */
    fun drag(anchor: Long, current: Long, grid: BeatGrid?, frames: Long): LongRange {
        if (grid == null) {
            var lo = min(anchor, current).coerceIn(0L, frames)
            var hi = max(anchor, current).coerceIn(0L, frames)
            if (hi - lo < StemBoardView.MIN_FRAMES) {
                if (current >= anchor) hi = min(frames, lo + StemBoardView.MIN_FRAMES) else lo = max(0L, hi - StemBoardView.MIN_FRAMES)
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
     * is given; the stretch keeps at least a beat (or [StemBoardView.MIN_FRAMES]).
     */
    fun moveEdge(from: Long, to: Long, edge: Int, frame: Long, grid: BeatGrid?, frames: Long): LongRange {
        val f = (if (grid != null) grid.snap(frame) else frame).coerceIn(0L, frames)
        val least = if (grid != null) grid.beat.roundToLong().coerceAtLeast(1L) else StemBoardView.MIN_FRAMES
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
const val BOARD_RATE = StemSelection.SAMPLE_RATE
