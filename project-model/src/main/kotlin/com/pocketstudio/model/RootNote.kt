package com.pocketstudio.model

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max

/**
 * The note a recorded sound plays at, so it can be played across the keys in tune: YIN pitch
 * detection at a few points after the attack, 25 Hz (an 808's sub) to 2 kHz.
 */
object RootNote {
    /** A fractional MIDI note (A4 = 69), or null when no steady pitch is found (noise, a drum hit). */
    fun detect(mono: FloatArray, rate: Int): Float? {
        if (rate <= 0) return null
        val maxLag = rate / 25
        val minLag = max(2, rate / 2000)
        val window = maxLag
        // Skip the attack (often a click or a pitch drop) and stop where the sound has faded.
        var peakAt = 0
        var peak = 0f
        for (i in mono.indices) if (abs(mono[i]) > peak) { peak = abs(mono[i]); peakAt = i }
        if (peak < 1e-3f) return null
        val step = rate / 20  // 50 ms
        val estimates = ArrayList<Double>()
        var start = peakAt + rate / 40
        while (start + window + maxLag < mono.size && estimates.size < 8) {
            var energy = 0.0
            for (i in start until start + window) energy += mono[i].toDouble() * mono[i]
            if (energy / window < (peak * 0.03) * (peak * 0.03)) break  // -30 dB under the peak: faded out
            yin(mono, start, window, minLag, maxLag)?.let { lag -> estimates += rate / lag }
            start += step
        }
        if (estimates.size < 2) return null
        estimates.sort()
        val median = estimates[estimates.size / 2]
        // Most estimates must agree (within half a semitone): a chord or a noise does not.
        val agree = estimates.count { abs(12 * ln(it / median) / ln(2.0)) < 0.5 }
        if (agree * 3 < estimates.size * 2) return null
        val note = 69 + 12 * ln(median / 440.0) / ln(2.0)
        return note.toFloat().takeIf { it in 0f..127f }
    }

    // The lag of the first clear dip of the cumulative mean normalised difference, refined.
    private fun yin(x: FloatArray, from: Int, window: Int, minLag: Int, maxLag: Int): Double? {
        val d = DoubleArray(maxLag + 2)
        for (lag in 1..maxLag + 1) {
            var sum = 0.0
            for (i in from until from + window) {
                val diff = x[i] - x[i + lag]
                sum += diff * diff
            }
            d[lag] = sum
        }
        val cmnd = DoubleArray(maxLag + 2) { 1.0 }
        var running = 0.0
        for (lag in 1..maxLag + 1) {
            running += d[lag]
            cmnd[lag] = if (running > 0) d[lag] * lag / running else 1.0
        }
        var lag = -1
        for (t in minLag..maxLag) {
            if (cmnd[t] < 0.15) {
                var best = t
                while (best + 1 <= maxLag && cmnd[best + 1] < cmnd[best]) best++
                lag = best
                break
            }
        }
        if (lag < 0) return null
        val a = cmnd[lag - 1]
        val b = cmnd[lag]
        val c = cmnd[lag + 1]
        val shift = if (a - 2 * b + c != 0.0) 0.5 * (a - c) / (a - 2 * b + c) else 0.0
        return lag + shift.coerceIn(-1.0, 1.0)
    }

    /** "Do 2"-style name of a MIDI note (C4 = 60 = Do 3, the French convention). */
    fun name(note: Float): String {
        val n = Math.round(note)
        return NOTE_NAMES[Math.floorMod(n, 12)] + " " + (Math.floorDiv(n, 12) - 2)
    }
}
