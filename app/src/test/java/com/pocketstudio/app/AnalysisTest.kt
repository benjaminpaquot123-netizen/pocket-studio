package com.pocketstudio.app

import com.pocketstudio.app.analysis.PitchDetector
import com.pocketstudio.app.analysis.Spectrum
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class AnalysisTest {
    private fun tone(hz: Double, amp: Float = 0.3f, harmonics: Boolean = false) = FloatArray(4096) { i ->
        val t = 2 * PI * hz * i / 48000
        (amp * (sin(t) + if (harmonics) 0.5 * sin(2 * t) + 0.3 * sin(3 * t) else 0.0)).toFloat()
    }

    @Test
    fun theTunerFindsTheNote() {
        val a = PitchDetector.detect(tone(440.0), 48000)!!
        assertEquals(69, a.midi)
        assertEquals(0.0, a.cents, 2.0)
        // A guitar's low E, a bit sharp, with harmonics.
        val e = PitchDetector.detect(tone(82.41 * 1.01, harmonics = true), 48000)!!
        assertEquals(40, e.midi)
        assertEquals(17.0, e.cents, 4.0)
        assertNull(PitchDetector.detect(FloatArray(4096), 48000))
    }

    @Test
    fun theSpectrumPeaksAtTheTone() {
        val bands = Spectrum.bands(tone(1000.0), 48000)
        val loudest = bands.indices.maxBy { bands[it] }
        // 1 kHz on the 30 Hz .. 18 kHz log scale of 40 bands.
        val expected = (kotlin.math.ln(1000.0 / 30) / kotlin.math.ln(18000.0 / 30) * 40).toInt()
        assertTrue("band $loudest vs $expected", kotlin.math.abs(loudest - expected) <= 1)
        assertTrue(bands[loudest] > -20f && bands[0] < -50f)
    }
}
