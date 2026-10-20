package com.pocketstudio.app.analysis

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Spectrum of a block of sound: [bands] levels in dB, log-spaced from 30 Hz to 18 kHz. */
object Spectrum {
    const val MIN_DB = -72f

    fun bands(samples: FloatArray, sampleRate: Int, bands: Int = 40, size: Int = 2048): FloatArray {
        val n = minOf(size, Integer.highestOneBit(samples.size))
        val re = DoubleArray(n)
        val im = DoubleArray(n)
        val from = samples.size - n
        for (i in 0 until n) re[i] = samples[from + i] * (0.5 - 0.5 * cos(2 * PI * i / (n - 1)))  // Hann
        fft(re, im)
        val mags = DoubleArray(n / 2) { k -> sqrt(re[k] * re[k] + im[k] * im[k]) * 4.0 / n }
        val out = FloatArray(bands)
        val lo = ln(30.0)
        val hi = ln(18000.0)
        for (b in 0 until bands) {
            val f0 = kotlin.math.exp(lo + (hi - lo) * b / bands)
            val f1 = kotlin.math.exp(lo + (hi - lo) * (b + 1) / bands)
            val k0 = (f0 * n / sampleRate).toInt().coerceIn(1, n / 2 - 1)
            val k1 = max(k0 + 1, (f1 * n / sampleRate).toInt()).coerceAtMost(n / 2)
            var peak = 0.0
            for (k in k0 until k1) peak = max(peak, mags[k])
            out[b] = (20 * log10(peak + 1e-9)).toFloat().coerceAtLeast(MIN_DB)
        }
        return out
    }

    /** In-place radix-2 FFT (size a power of two). */
    fun fft(re: DoubleArray, im: DoubleArray) {
        val n = re.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }
            j = j xor bit
            if (i < j) {
                var t = re[i]; re[i] = re[j]; re[j] = t
                t = im[i]; im[i] = im[j]; im[j] = t
            }
        }
        var len = 2
        while (len <= n) {
            val ang = -2 * PI / len
            for (i in 0 until n step len) {
                for (k in 0 until len / 2) {
                    val wr = cos(ang * k); val wi = sin(ang * k)
                    val ur = re[i + k]; val ui = im[i + k]
                    val vr = re[i + k + len / 2] * wr - im[i + k + len / 2] * wi
                    val vi = re[i + k + len / 2] * wi + im[i + k + len / 2] * wr
                    re[i + k] = ur + vr; im[i + k] = ui + vi
                    re[i + k + len / 2] = ur - vr; im[i + k + len / 2] = ui - vi
                }
            }
            len = len shl 1
        }
    }
}

/** What the tuner shows: the nearest note, how far off in cents, the frequency. */
data class TunerReading(val hz: Double, val midi: Int, val cents: Double)

/** Pitch of a sung or played note (YIN difference function, 60 Hz .. 1.5 kHz). */
object PitchDetector {
    fun detect(samples: FloatArray, sampleRate: Int, a4: Double = 440.0): TunerReading? {
        val window = minOf(samples.size / 2, 2048)
        val from = samples.size - 2 * window
        var energy = 0.0
        for (i in 0 until window) energy += samples[from + i].toDouble().pow(2)
        if (sqrt(energy / window) < 0.003) return null  // too quiet
        val minLag = sampleRate / 1500
        val maxLag = minOf(window - 1, sampleRate / 60)
        val d = DoubleArray(maxLag + 1)
        for (lag in 1..maxLag) {
            var s = 0.0
            for (i in 0 until window) {
                val x = samples[from + i] - samples[from + i + lag]
                s += x * x
            }
            d[lag] = s
        }
        // Cumulative mean normalised difference; first dip under the threshold.
        var running = 0.0
        val cmnd = DoubleArray(maxLag + 1) { 1.0 }
        for (lag in 1..maxLag) {
            running += d[lag]
            cmnd[lag] = if (running > 0) d[lag] * lag / running else 1.0
        }
        var best = -1
        for (lag in minLag..maxLag) {
            if (cmnd[lag] < 0.15) {
                var l = lag
                while (l + 1 <= maxLag && cmnd[l + 1] < cmnd[l]) l++
                best = l
                break
            }
        }
        if (best < 0) return null
        // Parabolic refinement.
        val a = cmnd.getOrElse(best - 1) { cmnd[best] }
        val b = cmnd[best]
        val c = cmnd.getOrElse(best + 1) { cmnd[best] }
        val shift = if (a - 2 * b + c != 0.0) 0.5 * (a - c) / (a - 2 * b + c) else 0.0
        val hz = sampleRate / (best + shift)
        val note = 69 + 12 * ln(hz / a4) / ln(2.0)
        val midi = kotlin.math.round(note).toInt()
        return TunerReading(hz, midi, (note - midi) * 100)
    }
}
