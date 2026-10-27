package com.pocketstudio.app.guide

import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.export.Loudness
import com.pocketstudio.engine.OfflineExport
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin

/**
 * G4: the checks of stage 6 (docs/PLAN-GUIDE.md §3.5) on rendered sound: the song folded to mono,
 * heard through a phone's speaker, and a reference brought to the song's loudness. Interleaved
 * stereo at [OfflineExport.SAMPLE_RATE] in and out. Pure: a worker thread, tested on the JVM.
 */
object GuideChecks {
    private const val RATE = OfflineExport.SAMPLE_RATE

    /** Where a phone's speaker stops (the song bench's model, docs/son/BANC.md B2: 4th-order Butterworth high-pass). */
    const val PHONE_LOW_HZ = 300.0

    /** And its top (a small speaker rolls off there). */
    const val PHONE_HIGH_HZ = 9000.0

    /** [stereo]'s integrated loudness (LUFS), null for silence. */
    fun lufs(stereo: FloatArray): Double? = Loudness.Meter(RATE, 2).apply { add(stereo, stereo.size / 2) }.integrated()

    /** Both channels as their mean, on both sides: a single speaker, a mono radio. */
    fun mono(stereo: FloatArray): FloatArray = FloatArray(stereo.size) { i ->
        val f = i / 2
        0.5f * (stereo[2 * f] + stereo[2 * f + 1])
    }

    /**
     * What [stereo] loses folded to mono (LU, 0 when nothing: the same in both channels). A wide sound
     * loses up to 3; past that, something is out of phase and cancels.
     */
    fun monoLossDb(stereo: FloatArray): Float? {
        val s = lufs(stereo) ?: return null
        val m = lufs(mono(stereo)) ?: return 60f  // all of it cancels
        return (s - m).toFloat().coerceAtLeast(0f)
    }

    /** [stereo] as a phone's speaker plays it: mono, nothing under [PHONE_LOW_HZ], rolled off over [PHONE_HIGH_HZ]. */
    fun phone(stereo: FloatArray): FloatArray {
        val m = mono(stereo)
        val chain = phoneChain()
        val out = FloatArray(m.size)
        for (f in 0 until m.size / 2) {
            var y = m[2 * f].toDouble()
            for (b in chain) y = b.process(y)
            out[2 * f] = y.toFloat()
            out[2 * f + 1] = y.toFloat()
        }
        return out
    }

    /** What a phone keeps of [stereo] (dB: its energy over [PHONE_LOW_HZ] against its own; nearer 0 is better), null for silence. */
    fun phoneDb(stereo: FloatArray): Float? {
        val m = mono(stereo)
        val high = highPass()
        var all = 0.0
        var kept = 0.0
        for (f in 0 until m.size / 2) {
            val x = m[2 * f].toDouble()
            var y = x
            for (b in high) y = b.process(y)
            all += x * x
            kept += y * y
        }
        if (all <= 1e-12) return null
        return (10 * log10((kept / all).coerceAtLeast(1e-9))).toFloat()
    }

    /**
     * The loudest [seconds] of [pcm] (3 s steps), as stereo at the engine's rate: a reference's
     * chorus or Drop, the part to compare with. Linear resampling: a comparison, not a mastering.
     */
    fun loudestExcerpt(pcm: Pcm, seconds: Double): FloatArray {
        val stereo = toStereo48(pcm)
        val frames = stereo.size / 2
        val want = (seconds * RATE).toInt().coerceIn(RATE, frames.coerceAtLeast(RATE))
        if (frames <= want) return stereo
        val step = 3 * RATE
        var best = 0
        var bestEnergy = -1.0
        var start = 0
        while (start + want <= frames) {
            var e = 0.0
            var i = start
            // Every 8th frame is enough to tell the loud passage.
            while (i < start + want) { val l = stereo[2 * i]; val r = stereo[2 * i + 1]; e += l * l + r * r; i += 8 }
            if (e > bestEnergy) { bestEnergy = e; best = start }
            start += step
        }
        return stereo.copyOfRange(2 * best, 2 * (best + want))
    }

    /** [pcm] (any rate, mono or stereo) as stereo at the engine's rate. */
    fun toStereo48(pcm: Pcm): FloatArray {
        val ch = pcm.channels.coerceAtLeast(1)
        val inFrames = pcm.data.size / ch
        fun at(f: Int, c: Int): Float = pcm.data[f * ch + if (ch == 1) 0 else c.coerceAtMost(ch - 1)]
        if (pcm.sampleRate == RATE) return FloatArray(inFrames * 2) { i -> at(i / 2, i % 2) }
        val ratio = pcm.sampleRate.toDouble() / RATE
        val outFrames = (inFrames / ratio).toInt()
        val out = FloatArray(outFrames * 2)
        for (f in 0 until outFrames) {
            val x = f * ratio
            val i = x.toInt().coerceAtMost(inFrames - 1)
            val j = (i + 1).coerceAtMost(inFrames - 1)
            val t = (x - i).toFloat()
            for (c in 0..1) out[2 * f + c] = at(i, c) * (1 - t) + at(j, c) * t
        }
        return out
    }

    /** [x] by [db] dB. */
    fun gain(x: FloatArray, db: Double): FloatArray {
        val k = 10.0.pow(db / 20).toFloat()
        return FloatArray(x.size) { x[it] * k }
    }

    private fun highPass(): List<Biquad> = listOf(Biquad.highPass(PHONE_LOW_HZ, 0.5411961), Biquad.highPass(PHONE_LOW_HZ, 1.3065630))

    private fun phoneChain(): List<Biquad> = highPass() + Biquad.lowPass(PHONE_HIGH_HZ, 0.7071068)

    /** RBJ biquads (Direct Form I). */
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
            fun highPass(hz: Double, q: Double): Biquad {
                val w = 2 * PI * hz / RATE
                val alpha = sin(w) / (2 * q)
                val c = cos(w)
                val a0 = 1 + alpha
                return Biquad((1 + c) / 2 / a0, -(1 + c) / a0, (1 + c) / 2 / a0, -2 * c / a0, (1 - alpha) / a0)
            }

            fun lowPass(hz: Double, q: Double): Biquad {
                val w = 2 * PI * hz / RATE
                val alpha = sin(w) / (2 * q)
                val c = cos(w)
                val a0 = 1 + alpha
                return Biquad((1 - c) / 2 / a0, (1 - c) / a0, (1 - c) / 2 / a0, -2 * c / a0, (1 - alpha) / a0)
            }
        }
    }
}
