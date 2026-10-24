package com.pocketstudio.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** « Geler la piste » over a whole techno or bass music song (5 to 6 minutes), mono past two. */
class FreezeLengthTest {
    @Test
    fun songsUpToSevenMinutesFreezeInMonoPastTwo() {
        assertTrue(Freeze.fits(6 * 60.0))
        assertTrue(Freeze.fits(7 * 60.0))
        assertFalse(Freeze.fits(7 * 60.0 + 1))
        assertFalse(Freeze.mono(2 * 60.0))
        assertTrue(Freeze.mono(2 * 60.0 + 0.5))
        // A 7-minute frozen sound in memory: mono float at 48 kHz stays under 81 MB.
        assertTrue(Freeze.MAX_SECONDS * 48_000L * 4 < 81_000_000)
    }
}
