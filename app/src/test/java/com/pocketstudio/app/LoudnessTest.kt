package com.pocketstudio.app

import com.pocketstudio.app.export.Loudness
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class LoudnessTest {
    private fun sine(amplitude: Double, seconds: Double, hz: Double = 997.0, rate: Int = 48000): FloatArray {
        val frames = (seconds * rate).toInt()
        return FloatArray(frames * 2) { i -> (amplitude * sin(2 * PI * hz * (i / 2) / rate)).toFloat() }
    }

    private fun measure(stereo: FloatArray): Double? {
        val m = Loudness.Meter(48000, 2)
        m.add(stereo, stereo.size / 2)
        return m.integrated()
    }

    @Test
    fun aSineReadsAsTheStandardSays() {
        // BS.1770: a 997 Hz sine at amplitude A on both channels reads about 20·log10(A) LUFS.
        assertEquals(-20.0, measure(sine(0.1, 5.0))!!, 0.15)
        assertEquals(-6.0, measure(sine(0.5012, 5.0))!!, 0.15)
        // 20 dB quieter reads 20 LU lower.
        assertEquals(20.0, measure(sine(0.5, 5.0))!! - measure(sine(0.05, 5.0))!!, 0.05)
    }

    @Test
    fun theHighPassAndTheRelativeGateMatchTheReference() {
        // Reference values: ffmpeg's ebur128 filter on the same signals.
        assertEquals(-15.0, measure(sine(0.5, 10.0, hz = 30.0))!!, 0.15)
        val loud = sine(0.5, 10.0)
        val quiet = FloatArray(loud.size) { loud[it] * 0.031623f }  // 30 dB quieter
        // The quiet half falls under the relative gate (it would read -9 without it).
        assertEquals(-6.1, measure(loud + quiet)!!, 0.15)
    }

    @Test
    fun silenceAndGatesAreHandled() {
        assertNull(measure(FloatArray(48000 * 2 * 2)))
        assertNull(measure(sine(0.5, 0.2)))  // shorter than one 400 ms block
        // Long silence around the music does not pull the result down (gating).
        val music = sine(0.1, 10.0)
        val padded = FloatArray(48000 * 2 * 10) + music + FloatArray(48000 * 2 * 10)
        assertEquals(measure(music)!!, measure(padded)!!, 0.2)
        assertTrue(Loudness.describe(-14.0).contains("streaming"))
    }
}
