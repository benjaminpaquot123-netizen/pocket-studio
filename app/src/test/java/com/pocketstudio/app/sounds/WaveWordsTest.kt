package com.pocketstudio.app.sounds

import com.pocketstudio.model.AcidSound
import com.pocketstudio.model.SynthPatch
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.WaveShape
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Decision of 2026-11-04 (« Scie / Triangle / Carré »): in the Mode simple a wave is said by how it
 * sounds (« Brillant », « Creux », « Doux »), in the Pro by its name; both with the same drawing; in
 * the 15 languages.
 */
class WaveWordsTest {
    @After
    fun back() {
        I18n.lang = Lang.FR
        I18n.everyday = false
    }

    private fun synth(wave: Float) = SynthPatch().with(SynthPatch.OSC1_WAVE, wave)

    @Test
    fun theModeSimpleSaysHowItSoundsAndTheProNamesTheWave() {
        I18n.everyday = true
        assertEquals(listOf("Brillant", "Creux", "Doux"), listOf(0f, 0.5f, 1f).map { SoundLooks.describe(synth(it)).substringBefore(" · ") })
        assertTrue(SoundLooks.describe(AcidSound(waveform = 0f)).startsWith("Brillant"))
        assertTrue(SoundLooks.describe(AcidSound(waveform = 1f)).startsWith("Creux"))
        I18n.everyday = false
        assertEquals(listOf("Scie", "Carré", "Triangle"), listOf(0f, 0.5f, 1f).map { SoundLooks.describe(synth(it)).substringBefore(" · ") })
        assertTrue(SoundLooks.describe(AcidSound(waveform = 0f)).startsWith("Scie"))
        // The Mode simple's words of the assistant are the same ear words.
        assertEquals("Brillant", SoundLooks.describePlain(synth(0f)).substringBefore(" · "))
    }

    @Test
    fun theSameDrawingInBothModes() {
        assertEquals(WaveShape.Saw, SoundLooks.waveOf(synth(0f)))
        assertEquals(WaveShape.Square, SoundLooks.waveOf(synth(0.5f)))
        assertEquals(WaveShape.Triangle, SoundLooks.waveOf(synth(1f)))
        assertEquals(WaveShape.Square, SoundLooks.waveOf(AcidSound(waveform = 1f)))
        assertEquals(null, SoundLooks.waveOf("a sample"))
        WaveShape.entries.forEach { assertTrue(it.icon.root.iterator().hasNext()) }
    }

    @Test
    fun everyLanguageHasItsSixWordsAndTellsThemApart() {
        for (l in Lang.entries) {
            I18n.lang = l
            val plain = WaveShape.entries.map { SoundLooks.waveWord(it, plain = true) }
            val pro = WaveShape.entries.map { SoundLooks.waveWord(it, plain = false) }
            assertEquals("$l: three ear words", 3, plain.distinct().size)
            assertEquals("$l: three wave names", 3, pro.distinct().size)
            if (l != Lang.FR) {
                I18n.lang = Lang.FR
                val french = WaveShape.entries.map { SoundLooks.waveWord(it, plain = true) }
                I18n.lang = l
                assertNotEquals("$l: the ear words are translated", french, plain)
            }
        }
    }
}
