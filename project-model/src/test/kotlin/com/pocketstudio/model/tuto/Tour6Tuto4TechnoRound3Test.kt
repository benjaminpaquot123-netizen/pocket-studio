package com.pocketstudio.model.tuto

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 6, lot « tuto4 », round trip 3, seat Bb-7 (German, tablet 8", font 130 %, Mode pro, tuto Pro, Techno): the steps
 * say the gesture that really works and where the button really is, in every layout and every language.
 */
class Tour6Tuto4TechnoRound3Test {
    private val course = TutoCourse.of("techno")!!
    private val langs = Lang.entries - Lang.FR
    private val steps = course.chapters.flatMap { it.steps }

    private fun step(id: String): TutoStep = steps.first { it.id == id }

    private fun tr(lang: Lang, fr: String): String? = I18n.table(lang)[fr]?.takeIf { it.isNotBlank() }

    private fun word(lang: Lang, fr: String): String = I18n.table(lang)[fr] ?: fr

    private fun report(what: String, bad: List<String>) = assertTrue("${bad.size} $what:\n" + bad.joinToString("\n"), bad.isEmpty())

    /**
     * H3 (ch. 3, screen 44): a long press on an empty cell puts a strong hit and opens no menu (RhythmScreen's grid:
     * `onLongClick` on a cell without a hit calls `cycleVelocity`); the menu opens on a hit already there. The clap
     * step says to put the ghost hit first, then the long press on it, Facile and Pro, in every language.
     */
    @Test
    fun theLongPressComesOnceTheHitIsPut() {
        val bad = ArrayList<String>()
        val s = step("clap_do")
        for (fr in listOfNotNull(s.text, s.textPro)) {
            val press = fr.indexOf("appui long")
            val put = fr.indexOf("posé")
            if (press < 0) bad += "fr: no long press: $fr"
            else if (put !in 0 until press) bad += "fr: the long press not said on the hit once put: $fr"
            val v = langs.associateWith { tr(it, fr) }
            v.filterValues { it == null }.keys.forEach { bad += "${it.code}: no translation of « $fr »" }
        }
        // The Pro's ghost is put by a touch on its cell, said before the long press.
        val pro = s.textPro!!
        if (!Regex("""touche la dernière case[^.]*\.[^.]*appui long""").containsMatchIn(pro)) bad += "fr Pro: the ghost's cell not touched before the long press: $pro"
        report("clap steps that send a long press to an empty cell", bad)
    }
}
