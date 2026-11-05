package com.pocketstudio.model.guide

import com.pocketstudio.model.EffectType
import com.pocketstudio.model.Insert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Audit ultra round 2, n° 25 (seat B c, screen 14): the track's sheet said of its first effect « Filtre ·
 * Coupure 364 Hz » « Un passe-haut vers 80 Hz : le sub garde le tout grave. » (the style's words for a
 * bass) over a low-pass at 364 Hz. A filter is said as it is set; an EQ whose style words give figures,
 * by its own band; the other effects keep the style's words.
 */
class GuideEffectWhyTest {
    private val trap = GuideLibrary.forStyle("trap")!!

    private fun filter(cutoff: Float, type: Float) = Insert(EffectType.FILTER.ordinal, listOf(cutoff, 0.2f, type, 1f))

    @Test
    fun aFilterIsSaidAsItIsSet() {
        assertTrue("the style's words for a bass's filter", trap.why("bass", EffectType.FILTER).contains("passe-haut vers 80 Hz"))
        // A low-pass (type 0) whose cutoff shows « 364 Hz » on the sheet.
        val low = filter(0.3f, 0f)
        val cutoff = EffectType.FILTER.params[0].format(0.3f)
        val said = GuideSheets.effectWhy(trap, "bass", low)
        assertTrue(said, said.startsWith("Un passe-bas") && said.contains(cutoff))
        assertFalse(said, said.contains("passe-haut") || said.contains("80 Hz"))
        // A high-pass is said a high-pass, at its own cutoff.
        val high = GuideSheets.effectWhy(trap, "bass", filter(0.15f, 1f))
        assertTrue(high, high.startsWith("Un passe-haut") && high.contains(EffectType.FILTER.params[0].format(0.15f)))
        assertTrue(GuideSheets.effectWhy(trap, "pad", filter(0.5f, 0.5f)).startsWith("Un passe-bande"))
    }

    @Test
    fun anEqWithFiguresIsSaidByItsOwnBandAndTheOthersKeepTheStylesWords() {
        val eq = Insert(EffectType.PARAM_EQ.ordinal, listOf(0.5f, 0.5f, 0.4f, 0f))
        val said = GuideSheets.effectWhy(trap, "bass", eq)
        assertFalse(said, said.contains("120 Hz") && !said.contains(EffectType.PARAM_EQ.params[0].format(0.5f)))
        assertTrue(said, said.contains(EffectType.PARAM_EQ.params[0].format(0.5f)))
        val comp = Insert(EffectType.COMPRESSOR.ordinal, EffectType.COMPRESSOR.defaults)
        assertEquals(trap.why("bass", EffectType.COMPRESSOR), GuideSheets.effectWhy(trap, "bass", comp))
    }
}
