package com.pocketstudio.model.i18n

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 5, lot « langues » (promises t5-Aa-4 in Hindi, t5-Bd-1 in Arabic, t5-Fc-ecran-1 in Japanese: « everyday words »
 * in the Mode simple): the Arabic, Hindi and Chinese catalogs keep the trade's English words in Latin letters (« Snare »,
 * « Loop », « Bridge », « Groove », « Kick »…), which a beginner reading that script cannot read. Every text of every
 * catalog, shown in the Mode simple, in the five languages whose script is not Latin: no such word left. The Mode pro
 * keeps the trade's words.
 */
class SimpleWordsNonLatinTest {
    @After
    fun back() {
        I18n.everyday = false
        I18n.lang = Lang.FR
    }

    private val trade = Regex(
        "(?i)(?<![A-Za-z])(Kicks?|Snares?|Bridge|Loops?|Groove|Swing|Intro|Outro|Drops?|Break|Build-up|Pads?|Solo|Crash|Fills?|" +
            "Hi-hats?|Sub bass|Claps?|Ride|Hook|Bass music|Mix)(?![A-Za-z])",
    )

    /** Names of the app and its features, the same in every language (« Pad Live », the 303's « Acid 303 »). */
    private val names = Regex("Pad Live|Pocket Studio")

    @Test
    fun theModeSimpleShowsNoTradeWordInLatinLettersInANonLatinScript() {
        val left = ArrayList<String>()
        for (lang in listOf(Lang.AR, Lang.HI, Lang.ZH_CN, Lang.JA, Lang.KO)) {
            I18n.lang = lang
            I18n.everyday = true
            val keys = I18n.AREAS.flatMap { I18n.area(lang, it).keys }.distinct()
            for (k in keys) {
                val shown = I18n.t(k)
                trade.find(names.replace(shown, ""))?.let { left += "${lang.code}: « ${it.value} » in « ${shown.take(70)} »" }
            }
        }
        assertTrue("${left.size} texts:\n" + left.take(40).joinToString("\n"), left.isEmpty())
    }

    @Test
    fun theWordsAreThePersonsOwn() {
        I18n.everyday = true
        I18n.lang = Lang.AR
        assertEquals("سنير", tr("Caisse claire"))
        I18n.lang = Lang.HI
        assertTrue(tr("Caisse claire"), tr("Caisse claire").none { it in 'A'..'z' })
    }

    @Test
    fun theModeProKeepsTheTradesWords() {
        I18n.everyday = false
        I18n.lang = Lang.HI
        assertEquals("Snare", tr("Caisse claire"))
    }
}
