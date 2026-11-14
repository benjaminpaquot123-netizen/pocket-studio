package com.pocketstudio.app.mixer

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tour 5, review of the « textes » lot: the bottom of a fader in the Mode simple says « Muet » (never « Plus de son »,
 * which French reads as « davantage de son »), the rest in words and whole numbers.
 */
class LevelWordsTest {
    @Test
    fun theBottomOfAFaderSaysMuet() {
        assertEquals("Muet", faderWords(0f))
        assertEquals("Muet", levelWords(-80f))
        assertEquals("Muet", levelWords(Float.NaN))
    }

    @Test
    fun theRestIsWordsAndWholeNumbers() {
        assertEquals("Normal", faderWords(0.8f))
        assertEquals("Normal", levelWords(0.3f))
        assertEquals("+3", levelWords(3f))
        assertEquals("−6", levelWords(-6f))
    }
}
