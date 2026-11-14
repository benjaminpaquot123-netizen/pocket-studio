package com.pocketstudio.app.remix

import com.pocketstudio.app.audio.Pcm
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * The edits of the stem board (B2): cutting a stretch out of a stem with click-free fades, finding
 * the phrases of a voice, and the peaks the board draws. Pure Kotlin on plain samples: worker
 * thread only (a 3-minute stem takes tens of milliseconds), never the audio thread.
 */
object StemEditor : StemEdits {
    /** Even « Fondu : aucun » keeps this ramp at each end: inaudible as a fade, it stops the click. */
    const val DECLICK_MS = 1.0

    /** Energy windows of the phrase finder. */
    const val WINDOW_MS = 10

    /** Silences shorter than this inside a phrase (a breath between two words) do not split it. */
    const val MERGE_GAP_MS = 250

    /** Shorter sounds (a click, a breath, a bleed) are not phrases. */
    const val MIN_PHRASE_MS = 300

    /** Kept before and after the detected sound: a soft consonant, the end of a note dying away. */
    const val PRE_ROLL_MS = 10
    const val POST_ROLL_MS = 30

    /** A beat this close after a phrase's start (a singer slightly early) still counts as its start. */
    const val SNAP_TOLERANCE_MS = 50

    /** Under this (dBFS, loudest windows), the stem holds no voice: only the separation's bleed. */
    const val QUIETEST_VOICE_DB = -45.0

    /** The floor is never taken lower than this under the loudest windows (digital silence). */
    private const val FLOOR_RANGE_DB = 60.0

    /** Where the threshold sits between the floor and the loudest windows (0 = floor, 1 = loudest). */
    private const val THRESHOLD_POSITION = 0.5

    /** A phrase starts above the threshold and lasts while it stays above the threshold minus this. */
    private const val RELEASE_DB = 10.0

    private const val FLOOR_PERCENTILE = 0.10
    private const val LOUD_PERCENTILE = 0.995
    private const val SILENT_DB = -120.0

    // --- Cut -------------------------------------------------------------------------------

    override fun cut(stem: Pcm, sel: StemSelection): Pcm {
        val ch = stem.channels
        val total = stem.frames.toLong()
        val start = sel.startFrame.coerceIn(0L, total)
        val end = sel.endFrame.coerceIn(start, total)
        val out = stem.data.copyOfRange((start * ch).toInt(), (end * ch).toInt())
        val n = (end - start).toInt()
        if (n == 0) return Pcm(out, ch, stem.sampleRate)
        var fadeIn = fadeFrames(sel.fadeInMs, stem.sampleRate)
        var fadeOut = fadeFrames(sel.fadeOutMs, stem.sampleRate)
        if (fadeIn + fadeOut > n) {  // a stretch shorter than its fades: they share it
            fadeIn = (n.toLong() * fadeIn / (fadeIn + fadeOut)).toInt()
            fadeOut = n - fadeIn
        }
        for (i in 0 until fadeIn) {
            val g = ramp(i, fadeIn)
            for (c in 0 until ch) out[i * ch + c] *= g
        }
        for (i in 0 until fadeOut) {
            val g = ramp(i, fadeOut)
            val f = n - 1 - i
            for (c in 0 until ch) out[f * ch + c] *= g
        }
        return Pcm(out, ch, stem.sampleRate)
    }

    private fun fadeFrames(ms: Int, rate: Int): Int =
        (max(ms.toDouble(), DECLICK_MS) * rate / 1000.0).roundToLong().toInt().coerceAtLeast(1)

    /**
     * Raised cosine from 0 (step 0) towards 1 (step [len]): no step in level at the edge and none
     * in slope where it joins the sound, so neither a click nor a thump.
     */
    private fun ramp(i: Int, len: Int): Float = (0.5 - 0.5 * cos(PI * i / len)).toFloat()

    // --- Phrases ---------------------------------------------------------------------------

    override fun phrases(stem: Pcm, beatFrames: Long?, max: Int): List<LongRange> =
        phrases(stem, beatFrames, max, 0L)

    override fun phrases(stem: Pcm, beatFrames: Long?, max: Int, firstBeatFrame: Long): List<LongRange> {
        val frames = stem.frames.toLong()
        if (max <= 0 || frames == 0L) return emptyList()
        val rate = stem.sampleRate
        val win = max(1, rate * WINDOW_MS / 1000)
        val db = windowDb(stem, win)
        val sorted = db.sortedArray()
        val loud = percentile(sorted, LOUD_PERCENTILE)
        if (loud < QUIETEST_VOICE_DB) return emptyList()
        val floor = max(percentile(sorted, FLOOR_PERCENTILE), loud - FLOOR_RANGE_DB)
        // At least 6 dB under the loudest windows: a voice that never stops is one phrase.
        val open = min(floor + (loud - floor) * THRESHOLD_POSITION, loud - 6.0)
        val hold = min(open, max(floor + 3.0, open - RELEASE_DB))

        // Stretches above [hold] that reach [open] somewhere (hysteresis both ways), in windows.
        val regions = ArrayList<LongArray>()
        var w = 0
        while (w < db.size) {
            if (db[w] < hold) { w++; continue }
            val a = w
            var reached = false
            while (w < db.size && db[w] >= hold) { if (db[w] >= open) reached = true; w++ }
            if (reached) regions += longArrayOf(a.toLong(), w.toLong())
        }

        // Breaths between words do not split a phrase; then the short ones go.
        val mergeGap = msToWindows(MERGE_GAP_MS)
        val merged = ArrayList<LongArray>()
        for (r in regions) {
            val last = merged.lastOrNull()
            if (last != null && r[0] - last[1] < mergeGap) last[1] = r[1] else merged += r
        }
        val minFrames = MIN_PHRASE_MS.toLong() * rate / 1000
        val pre = PRE_ROLL_MS.toLong() * rate / 1000
        val post = POST_ROLL_MS.toLong() * rate / 1000
        var found = merged
            .filter { (it[1] - it[0]) * win >= minFrames }
            .map { longArrayOf(max(0L, it[0] * win - pre), min(frames, it[1] * win + post)) }

        if (beatFrames != null && beatFrames > 0) found = snapped(found, beatFrames, firstBeatFrame, frames, rate)

        return found
            .sortedByDescending { it[1] - it[0] }
            .take(max)
            .sortedBy { it[0] }
            .map { it[0] until it[1] }
    }

    /**
     * Edges on the beats without cutting the voice: the start goes to the beat at or before it
     * (or one just after, within [SNAP_TOLERANCE_MS]), the end to the beat at or after it (or one
     * just before). Two phrases with no beat between them become one.
     */
    private fun snapped(found: List<LongArray>, beat: Long, first: Long, frames: Long, rate: Int): List<LongArray> {
        val tol = SNAP_TOLERANCE_MS.toLong() * rate / 1000
        val out = ArrayList<LongArray>()
        for (p in found) {
            val s = max(0L, first + Math.floorDiv(p[0] + tol - first, beat) * beat)
            val e = min(frames, first + Math.floorDiv(p[1] - tol - first + beat - 1, beat) * beat)
            if (e <= s) continue
            val last = out.lastOrNull()
            if (last != null && s < last[1]) last[1] = max(last[1], e) else out += longArrayOf(s, e)
        }
        return out
    }

    private fun msToWindows(ms: Int): Long = ((ms + WINDOW_MS - 1) / WINDOW_MS).toLong()

    /** Mean power of each window, all channels, in dB (digital silence: [SILENT_DB]). */
    private fun windowDb(pcm: Pcm, win: Int): DoubleArray {
        val ch = pcm.channels
        val count = (pcm.frames + win - 1) / win
        val out = DoubleArray(count)
        val data = pcm.data
        for (w in 0 until count) {
            val from = w * win * ch
            val to = min(data.size, (w + 1) * win * ch)
            var sum = 0.0
            for (i in from until to) { val x = data[i].toDouble(); sum += x * x }
            val power = sum / (to - from)
            out[w] = if (power <= 1e-12) SILENT_DB else 10.0 * log10(power)
        }
        return out
    }

    private fun percentile(sorted: DoubleArray, p: Double): Double =
        sorted[((sorted.size - 1) * p).toInt().coerceIn(0, sorted.size - 1)]

    // --- Peaks for the board -----------------------------------------------------------------

    /** The peak (0..1, the loudest channel) of each of [buckets] slices of the whole of [pcm]. */
    fun peaks(pcm: Pcm, buckets: Int): FloatArray = peaks(pcm, 0L, pcm.frames.toLong(), buckets)

    /**
     * The peak (0..1, the loudest channel) of each of [buckets] slices of frames [fromFrame] until
     * [toFrame] of [pcm]; slices outside the sound are 0. The same scale as the Remix overview
     * (`NativeStems.peaks`). For drawing at 120 Hz, build a [StemPeaks] once instead.
     */
    fun peaks(pcm: Pcm, fromFrame: Long, toFrame: Long, buckets: Int): FloatArray {
        val out = FloatArray(max(0, buckets))
        val span = toFrame - fromFrame
        if (buckets <= 0 || span <= 0) return out
        val ch = pcm.channels
        val frames = pcm.frames.toLong()
        for (b in 0 until buckets) {
            val a = fromFrame + span * b / buckets
            val z = max(a + 1, fromFrame + span * (b + 1) / buckets)
            val lo = max(0L, a)
            val hi = min(frames, z)
            var peak = 0f
            for (i in (lo * ch).toInt() until (hi * ch).toInt()) peak = max(peak, abs(pcm.data[i]))
            out[b] = min(1f, peak)
        }
        return out
    }
}

/**
 * A stem's peaks at every zoom, computed once (worker thread): level 0 holds the peak of each
 * [BASE] frames, each next level the peak of two of the level before. [fill] then draws any
 * stretch at any width by reading the level just finer than a screen slice, without allocating:
 * fit for every frame of a pinch at 120 Hz. About 4 floats per 100 frames (1.5 MB for 3 minutes
 * at 48 kHz).
 */
class StemPeaks private constructor(
    /** Length of the stem, in frames. */
    val frames: Long,
    private val levels: Array<FloatArray>,
) {
    /**
     * [out] filled with the peaks (0..1) of frames [fromFrame] until [toFrame], one per slot;
     * slots outside the stem are 0. A slot may also show up to its own width (at least [BASE]
     * frames) of its neighbours: a crest is never missed, at worst drawn one slot wider.
     */
    fun fill(fromFrame: Long, toFrame: Long, out: FloatArray) {
        val n = out.size
        val span = toFrame - fromFrame
        if (n == 0) return
        if (span <= 0 || levels.isEmpty()) { out.fill(0f); return }
        // The coarsest level whose cells still fit in one slot.
        var level = 0
        while (level + 1 < levels.size && (BASE.toLong() shl (level + 1)) <= span / n) level++
        val cells = levels[level]
        val cell = BASE.toLong() shl level
        for (b in 0 until n) {
            val a = fromFrame + span * b / n
            val z = max(a + 1, fromFrame + span * (b + 1) / n)
            if (z <= 0 || a >= frames) { out[b] = 0f; continue }
            val i0 = (max(0L, a) / cell).toInt()
            val i1 = min(cells.size.toLong(), (min(frames, z) + cell - 1) / cell).toInt()
            var peak = 0f
            for (i in i0 until i1) if (cells[i] > peak) peak = cells[i]
            out[b] = peak
        }
    }

    companion object {
        /** Frames per peak at the finest level (0.67 ms at 48 kHz). */
        const val BASE = 32

        /** Read in pieces this long by [of] (frames). */
        private const val CHUNK = BASE * 4096

        fun of(pcm: Pcm): StemPeaks = of(pcm.frames.toLong(), pcm.channels) { start, count ->
            val from = (start * pcm.channels).toInt()
            pcm.data.copyOfRange(from, min(pcm.data.size, from + count * pcm.channels))
        }

        /**
         * Built from [read] (interleaved samples of [count] frames from frame [start], [channels]
         * channels), piece by piece: the whole stem is never held, e.g.
         * `StemPeaks.of(stems.frames, 2) { s, c -> stems.extract(stem, s, c.toLong()) }`.
         */
        fun of(frames: Long, channels: Int, read: (start: Long, count: Int) -> FloatArray): StemPeaks {
            if (frames <= 0 || channels <= 0) return StemPeaks(0, emptyArray())
            val base = FloatArray(((frames + BASE - 1) / BASE).toInt())
            var start = 0L
            while (start < frames) {
                val count = min(CHUNK.toLong(), frames - start).toInt()
                val data = read(start, count)
                val got = min(count, data.size / channels)
                for (f in 0 until got) {
                    val cellIndex = ((start + f) / BASE).toInt()
                    for (c in 0 until channels) {
                        val v = min(1f, abs(data[f * channels + c]))
                        if (v > base[cellIndex]) base[cellIndex] = v
                    }
                }
                start += count
            }
            val levels = ArrayList<FloatArray>()
            levels += base
            while (levels.last().size > 1) {
                val prev = levels.last()
                levels += FloatArray((prev.size + 1) / 2) { i ->
                    max(prev[2 * i], if (2 * i + 1 < prev.size) prev[2 * i + 1] else 0f)
                }
            }
            return StemPeaks(frames, levels.toTypedArray())
        }
    }
}
