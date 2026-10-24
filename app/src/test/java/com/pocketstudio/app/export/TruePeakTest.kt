package com.pocketstudio.app.export

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.sin

/**
 * The decoded M4A's check: its true peak reader ([TruePeakMeter], the one kept: S5's own reader,
 * TruePeak4x, and its encode-again loop were the same check twice) and the gain of an encode again
 * ([M4aGuard.encodeWithin]). Moved here from S5's tests with the code they covered.
 */
class TruePeakTest {
    @Test
    fun readsThePeakBetweenTheSamples() {
        // A sine at a quarter of the rate, 45 degrees off: every sample at 0.707, the wave at 1.
        val tp = TruePeakMeter(1)
        for (n in 0 until 4800) tp.add(sin(PI / 2 * n + PI / 4).toFloat())
        assertEquals(0.0, tp.peakDb, 0.15)
        // A 1 kHz sine at 0.5: its peak, not more.
        val low = TruePeakMeter(1)
        for (n in 0 until 48000) low.add((0.5 * sin(2 * PI * 1000.0 * n / 48000.0 + 0.3)).toFloat())
        assertEquals(-6.02, low.peakDb, 0.1)
    }

    @Test
    fun anEncodeAgainComesUnderTheCeiling() {
        // Decoded 2.6 dB over the -1 dBTP ceiling: the next encode lower by that and the margin.
        val gains = ArrayList<Float>()
        M4aGuard.encodeWithin(0.9f, encode = { gains += it }, decodedPeak = { if (gains.size == 1) 1.6 else -1.5 })
        assertEquals(2, gains.size)
        assertEquals(20 * log10(0.9) - 2.6 - M4aGuard.MARGIN_DB, 20 * log10(gains[1].toDouble()), 1e-3)
        // Under it already: the same gain, one encode.
        val once = ArrayList<Float>()
        M4aGuard.encodeWithin(0.9f, encode = { once += it }, decodedPeak = { -1.4 })
        assertEquals(listOf(0.9f), once)
    }
}
