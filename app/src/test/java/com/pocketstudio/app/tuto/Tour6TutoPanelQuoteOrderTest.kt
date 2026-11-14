package com.pocketstudio.app.tuto

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.tuto.TutoCourse
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 6, lot 2, fresh review (grave): [tutoKitQuotes] took the k-th quote of a translation for the k-th quote of the French
 * consigne; a language that orders its words otherwise had another button's name replaced by the pad's (techno « reverb » in
 * Chinese: « Envoi réverbe » became « Clap », the step named the pad twice and the setting to raise no more). Every consigne of
 * every course that quotes several things keeps each of its translated quotes, in the 14 languages.
 */
class Tour6TutoPanelQuoteOrderTest {
    @After
    fun end() { I18n.lang = Lang.FR }

    private val quote = Regex("[«“„「『\"‘][\\s  ]?([^«“„「『\"‘»”“」』\"’]+?)[\\s  ]?[»”“」』\"’]")

    @Test
    fun noQuoteIsTakenForAnotherInAConsigneQuotingSeveralThings() {
        val bad = mutableListOf<String>()
        for (style in TutoCourse.STYLES) for (s in TutoCourse.of(style)!!.chapters.flatMap { it.steps }) {
            for (fr in listOfNotNull(s.text, s.textPro, s.why)) {
                if (quote.findAll(fr).count() < 2) continue
                for (lang in Lang.entries - Lang.FR) {
                    I18n.lang = lang
                    val shown = I18n.table(lang)[fr] ?: continue
                    val after = tutoKitQuotes(fr, shown)
                    if (after != shown) bad += "${lang.code} $style/${s.id}: $shown\n   → $after"
                }
            }
        }
        assertTrue("${bad.size} consignes changed:\n" + bad.take(15).joinToString("\n"), bad.isEmpty())
    }
}
