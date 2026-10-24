package com.pocketstudio.app.library

import com.pocketstudio.app.audio.Pcm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.sin

/**
 * Lot 1 (APK 181: the assistant's « Nappe / accords » previews far louder than the others): every
 * kind of sound a chooser previews comes out at [PreviewLevel.REFERENCE_LUFS] ± 1.5 LU, whatever
 * level it was rendered or recorded at. The sounds here stand for each family: a held chord (a
 * pad, dense and long), a bass line, a lead's notes, a pluck that dies away, keys, a pack's
 * sample in mono, a quiet General MIDI or multizone piano, a 5-second excerpt (a demo).
 * The real sounds, rendered by the engine, are checked by the preview bench (PreviewLevelBench,
 * scripts/live-bench.sh).
 */
class PreviewLevelTest {
    private val rate = 48_000

    private fun stereo(seconds: Double, f: (Double) -> Double): Pcm {
        val n = (seconds * rate).toInt()
        val data = FloatArray(2 * n)
        for (i in 0 until n) {
            val v = f(i.toDouble() / rate).toFloat()
            data[2 * i] = v
            data[2 * i + 1] = v * 0.9f
        }
        return Pcm(data, 2, rate)
    }

    private fun tone(hz: Double, t: Double) = sin(2 * PI * hz * t)

    /** Notes of [length] s every [every] s, each with an exponential decay [decay] (s). */
    private fun notes(t: Double, every: Double, length: Double, decay: Double, hz: Double, gain: Double): Double {
        val at = t % every
        return if (at > length) 0.0 else gain * exp(-at / decay) * (tone(hz, t) + 0.3 * tone(2 * hz, t))
    }

    private val families: Map<String, Pcm> = mapOf(
        // A held chord at full level: the loud one of APK 181.
        "nappe tenue" to stereo(2.8) { t -> 0.22 * (tone(220.0, t) + tone(277.2, t) + tone(329.6, t) + tone(440.0, t)) },
        "accords" to stereo(2.8) { t -> notes(t, 0.35, 0.3, 0.4, 261.6, 0.25) + notes(t, 0.35, 0.3, 0.4, 329.6, 0.25) + notes(t, 0.35, 0.3, 0.4, 392.0, 0.25) },
        "basse" to stereo(2.8) { t -> notes(t, 0.345, 0.3, 0.5, 55.0, 0.5) },
        "lead" to stereo(2.8) { t -> notes(t, 0.17, 0.15, 0.3, 880.0, 0.12) },
        "pluck" to stereo(2.8) { t -> notes(t, 0.69, 0.6, 0.15, 659.3, 0.5) },
        "clavier" to stereo(2.8) { t -> notes(t, 0.69, 0.6, 0.6, 523.3, 0.05) },
        "sample de pack (mono)" to Pcm(FloatArray(48_000 * 2) { i -> (0.5 * exp(-i / 30_000.0) * tone(196.0, i / 48_000.0)).toFloat() }, 1, rate),
        "piano GM / multizone (doux)" to stereo(3.0) { t -> notes(t, 0.5, 0.45, 0.5, 440.0, 0.05) },
        "démo de 5 s" to stereo(5.0) { t -> (if (t > 2.5) 0.3 else 0.05) * tone(110.0, t) + notes(t, 0.47, 0.1, 0.05, 2000.0, 0.1) },
    )

    @Test
    fun everyFamilyIsPreviewedAtTheSameLevel() {
        val before = families.mapValues { PreviewLevel.loudness(it.value)!! }
        // As they come: far apart (the held chord well over the lead and the soft piano).
        assertTrue("spread before: $before", before.values.max() - before.values.min() > 15)
        for ((name, pcm) in families) {
            val heard = PreviewLevel.normalize(pcm)
            val l = PreviewLevel.loudness(heard)!!
            val peak = 20 * log10(heard.data.maxOf { abs(it) }.toDouble())
            assertTrue("$name: peak $peak dBFS", peak <= PreviewLevel.CEILING_DBFS + 1e-3)
            assertEquals("$name at the preview level", PreviewLevel.REFERENCE_LUFS, l, PreviewLevel.TOLERANCE_LU)
        }
    }

    @Test
    fun aSoundAlreadyAtTheLevelIsLeftAsItIsAndSilenceStaysSilent() {
        val pad = PreviewLevel.normalize(families.getValue("nappe tenue"))
        assertSame(pad, PreviewLevel.normalize(pad))
        val silence = Pcm(FloatArray(9600), 2, rate)
        assertSame(silence, PreviewLevel.normalize(silence))
    }

    @Test
    fun aVeryPeakySoundIsNeverClipped() {
        // A click: 3 samples at full scale in half a second of near silence.
        val data = FloatArray(48_000) { if (it in 100..102) 1f else 0f }
        val heard = PreviewLevel.normalize(Pcm(data, 2, rate))
        assertTrue(heard.data.maxOf { abs(it) } <= 0.8913f + 1e-4f)
    }
}
