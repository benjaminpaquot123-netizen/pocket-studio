package com.pocketstudio.bentest

import kotlin.math.PI
import kotlin.math.cos

import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Made-up sounds for the microphone's analysis (T1b): known filters (RBJ biquads), pink and white noise, and what a
 * third-octave or octave band of them must read, worked out from the filter's own response (not from the analysis).
 */
object SonsFabriques {
    const val RATE = 48_000

    /** A biquad (RBJ cookbook), run sample by sample; [gain] gives its power response at [f]. */
    class Biquad(private val b0: Double, private val b1: Double, private val b2: Double, private val a1: Double, private val a2: Double) {
        private var x1 = 0.0; private var x2 = 0.0; private var y1 = 0.0; private var y2 = 0.0

        fun run(x: Double): Double {
            val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1; x1 = x; y2 = y1; y1 = y
            return y
        }

        /** |H(f)|² at [f] Hz. */
        fun power(f: Double): Double {
            val w = 2 * PI * f / RATE
            val nr = b0 + b1 * cos(w) + b2 * cos(2 * w); val ni = -(b1 * sin(w) + b2 * sin(2 * w))
            val dr = 1 + a1 * cos(w) + a2 * cos(2 * w); val di = -(a1 * sin(w) + a2 * sin(2 * w))
            return (nr * nr + ni * ni) / (dr * dr + di * di)
        }

        fun fresh() = Biquad(b0, b1, b2, a1, a2)

        companion object {
            private fun make(b0: Double, b1: Double, b2: Double, a0: Double, a1: Double, a2: Double) = Biquad(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)

            fun highPass(f: Double, q: Double = 0.7071): Biquad {
                val w = 2 * PI * f / RATE; val alpha = sin(w) / (2 * q); val c = cos(w)
                return make((1 + c) / 2, -(1 + c), (1 + c) / 2, 1 + alpha, -2 * c, 1 - alpha)
            }

            fun lowPass(f: Double, q: Double = 0.7071): Biquad {
                val w = 2 * PI * f / RATE; val alpha = sin(w) / (2 * q); val c = cos(w)
                return make((1 - c) / 2, 1 - c, (1 - c) / 2, 1 + alpha, -2 * c, 1 - alpha)
            }

            fun peak(f: Double, db: Double, q: Double = 1.0): Biquad {
                val a = 10.0.pow(db / 40); val w = 2 * PI * f / RATE; val alpha = sin(w) / (2 * q); val c = cos(w)
                return make(1 + alpha * a, -2 * c, 1 - alpha * a, 1 + alpha / a, -2 * c, 1 - alpha / a)
            }
        }
    }

    /** A chain of biquads, as one filter. */
    class Chaine(private vararg val parts: Biquad) {
        fun power(f: Double) = parts.fold(1.0) { p, b -> p * b.power(f) }

        fun filtrer(x: FloatArray): FloatArray {
            val run = parts.map { it.fresh() }
            return FloatArray(x.size) { i -> run.fold(x[i].toDouble()) { v, b -> b.run(v) }.toFloat() }
        }
    }

    /** Pink noise (Paul Kellet's filter, within 0.05 dB above 9 Hz) of [n] samples, RMS about [rms]. */
    fun rose(n: Int, rms: Double = 0.1, seed: Long = 1): FloatArray {
        val r = java.util.Random(seed)
        var b0 = 0.0; var b1 = 0.0; var b2 = 0.0; var b3 = 0.0; var b4 = 0.0; var b5 = 0.0; var b6 = 0.0
        val out = DoubleArray(n) {
            val w = r.nextGaussian()
            b0 = 0.99886 * b0 + w * 0.0555179; b1 = 0.99332 * b1 + w * 0.0750759; b2 = 0.96900 * b2 + w * 0.1538520
            b3 = 0.86650 * b3 + w * 0.3104856; b4 = 0.55000 * b4 + w * 0.5329522; b5 = -0.7616 * b5 - w * 0.0168980
            val p = b0 + b1 + b2 + b3 + b4 + b5 + b6 + w * 0.5362
            b6 = w * 0.115926
            p
        }
        val now = sqrt(out.sumOf { it * it } / n)
        return FloatArray(n) { (out[it] * rms / now).toFloat() }
    }

    fun blanc(n: Int, rms: Double, seed: Long = 2): FloatArray {
        val r = java.util.Random(seed)
        return FloatArray(n) { (r.nextGaussian() * rms).toFloat() }
    }

    fun plus(a: FloatArray, b: FloatArray) = FloatArray(maxOf(a.size, b.size)) { (a.getOrElse(it) { 0f } + b.getOrElse(it) { 0f }) }

    fun silence(n: Int) = FloatArray(n)

    /**
     * What a band from [lo] to [hi] Hz of a 1/f signal (a log sweep, pink noise) through [power] reads, in power:
     * the mean of |H|² weighted by 1/f over the band.
     */
    fun bande(power: (Double) -> Double, lo: Double, hi: Double): Double {
        val n = 400
        var num = 0.0; var den = 0.0
        for (i in 0 until n) {
            val f = lo * (hi / lo).pow((i + 0.5) / n)
            // Log-spaced points: each already carries the 1/f weight (df/f is constant).
            num += power(f); den += 1.0
        }
        return num / den
    }

    /** Third-octave band [k] of [Tiers.CENTRES]'s edges. */
    fun tiers(k: Int) = Tiers.CENTRES[k] * 2.0.pow(-1.0 / 6) to Tiers.CENTRES[k] * 2.0.pow(1.0 / 6)

    /** The expected curve of [power] by third-octave, 0 dB = the power mean of 500 Hz … 2 kHz (as [Courbe.mesurer]). */
    fun courbeAttendue(power: (Double) -> Double): List<Double> {
        val lin = Tiers.CENTRES.indices.map { k -> tiers(k).let { (lo, hi) -> bande(power, lo, hi) } }
        val ref = Tiers.CENTRES.indices.filter { Tiers.CENTRES[it] in 440.0..2250.0 }.map { lin[it] }.average()
        return lin.map { 10 * log10(it / ref) }
    }

    /** The expected octaves of pink noise through [power], 0 dB = the power mean of 500 Hz … 4 kHz (as [Empreinte.octaves]). */
    fun octavesAttendues(power: (Double) -> Double): List<Double> {
        val lin = Empreinte.OCTAVES.map { nominal ->
            val fc = 1000.0 * 2.0.pow(Math.round(3 * kotlin.math.log2(nominal / 1000.0)) / 3.0)
            bande(power, fc * 2.0.pow(-0.5), fc * 2.0.pow(0.5))
        }
        val ref = Empreinte.OCTAVES.indices.filter { Empreinte.OCTAVES[it] in 500..4000 }.map { lin[it] }.average()
        return lin.map { 10 * log10(it / ref) }
    }

}
