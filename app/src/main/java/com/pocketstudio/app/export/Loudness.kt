package com.pocketstudio.app.export

import com.pocketstudio.model.i18n.tr
import java.io.File
import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.tan

/**
 * Integrated loudness in LUFS (ITU-R BS.1770-4, as streaming services measure it): K-weighting,
 * 400 ms blocks every 100 ms, absolute gate at -70 LUFS and relative gate 10 LU below.
 */
object Loudness {
    /** Of a WAV file; null when it is silent or too short (under 400 ms). Background thread. */
    fun integrated(wav: File): Double? = WavPcm.open(wav).use { pcm ->
        val meter = Meter(pcm.sampleRate, pcm.channels)
        val buf = FloatArray(8192 * pcm.channels)
        while (true) {
            val n = pcm.readFloat(buf, 8192)
            if (n == 0) break
            meter.add(buf, n)
        }
        meter.integrated()
    }

    /** What a number means, for someone who is not a sound engineer. */
    fun describe(lufs: Double): String = when {
        lufs > -9.0 -> tr("très fort (les plateformes le baisseront)")
        lufs > -12.0 -> tr("fort, comme la musique de club")
        lufs > -16.0 -> tr("comme la musique en streaming (−14)")
        lufs > -23.0 -> tr("assez doux")
        else -> tr("très doux : monte un peu le volume")
    }

    /** 3 s of 100 ms segments: EBU R 128's short-term window. */
    private const val SHORT_TERM_SEGMENTS = 30

    class Meter(private val rate: Int, private val channels: Int) {
        private val shelf = Array(channels) { Biquad.highShelf(rate) }
        private val highPass = Array(channels) { Biquad.highPass(rate) }
        private val segment = (rate / 10).coerceAtLeast(1)  // 100 ms
        private val segments = ArrayList<Double>()  // summed channel energy per 100 ms
        private var energy = 0.0
        private var inSegment = 0

        fun add(interleaved: FloatArray, frames: Int) {
            for (f in 0 until frames) {
                var e = 0.0
                for (c in 0 until channels) {
                    val y = highPass[c].process(shelf[c].process(interleaved[f * channels + c].toDouble()))
                    e += y * y
                }
                energy += e
                if (++inSegment == segment) {
                    segments += energy / segment
                    energy = 0.0
                    inSegment = 0
                }
            }
        }

        /**
         * The loudest short-term loudness (LUFS): the K-weighted energy over 3 s windows, every
         * 100 ms; a sound shorter than 3 s is measured over its whole length. Null when silent.
         * The level previews are brought to (lot 1, [com.pocketstudio.app.library.PreviewLevel]).
         */
        fun shortTermMax(): Double? {
            val parts = if (inSegment > 0) segments + energy / inSegment else segments
            if (parts.isEmpty()) return null
            val window = minOf(SHORT_TERM_SEGMENTS, parts.size)
            var sum = 0.0
            for (i in 0 until window) sum += parts[i]
            var best = sum
            for (i in window until parts.size) {
                sum += parts[i] - parts[i - window]
                if (sum > best) best = sum
            }
            val z = best / window
            return if (z <= 0.0) null else -0.691 + 10 * log10(z)
        }

        fun integrated(): Double? {
            // 400 ms blocks = four consecutive segments, one block every 100 ms.
            val blocks = (0..segments.size - 4).map { i -> (segments[i] + segments[i + 1] + segments[i + 2] + segments[i + 3]) / 4 }
            fun lufs(z: Double) = -0.691 + 10 * log10(z)
            val loud = blocks.filter { it > 0 && lufs(it) > -70.0 }
            if (loud.isEmpty()) return null
            val gate = lufs(loud.average()) - 10.0
            val kept = loud.filter { lufs(it) > gate }
            return lufs(kept.average())
        }
    }

    /** Direct form I biquad; the two K-weighting stages, as libebur128 derives them for any rate. */
    private class Biquad(val b0: Double, val b1: Double, val b2: Double, val a1: Double, val a2: Double) {
        private var x1 = 0.0
        private var x2 = 0.0
        private var y1 = 0.0
        private var y2 = 0.0

        fun process(x: Double): Double {
            val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1; x1 = x; y2 = y1; y1 = y
            return y
        }

        companion object {
            // At 48 kHz these give exactly the coefficients printed in BS.1770-4.
            fun highShelf(rate: Int): Biquad {
                val q = 0.7071752369554196
                val k = tan(PI * 1681.974450955533 / rate)
                val vh = 10.0.pow(3.999843853973347 / 20)
                val vb = vh.pow(0.4996667741545416)
                val a0 = 1 + k / q + k * k
                return Biquad(
                    (vh + vb * k / q + k * k) / a0,
                    2 * (k * k - vh) / a0,
                    (vh - vb * k / q + k * k) / a0,
                    2 * (k * k - 1) / a0,
                    (1 - k / q + k * k) / a0,
                )
            }

            fun highPass(rate: Int): Biquad {
                val q = 0.5003270373238773
                val k = tan(PI * 38.13547087602444 / rate)
                val a0 = 1 + k / q + k * k
                return Biquad(1.0, -2.0, 1.0, 2 * (k * k - 1) / a0, (1 - k / q + k * k) / a0)
            }
        }
    }
}
