package com.pocketstudio.model.tuto

import com.pocketstudio.model.allScenes
import com.pocketstudio.model.switchScene
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 6, lot « tuto4 », the leftovers of the techno fixes said again in the acid techno course and in the Mode simple's
 * words: what the techno course now says right (the filter's « Par partie » curve, a pad named in the singular) is said
 * right here too, in every language.
 */
class Tour6Tuto4AcidTechnoTest {
    private val course = TutoCourse.of("acid_techno")!!
    private val langs = Lang.entries - Lang.FR
    private val steps = course.chapters.flatMap { it.steps }

    private fun step(id: String): TutoStep = steps.first { it.id == id }

    /** [fr] in [lang], never the French shown in its place. */
    private fun tr(lang: Lang, fr: String): String = I18n.table(lang)[fr] ?: "(no ${lang.code} translation of « $fr »)"

    private fun report(what: String, bad: List<String>) = assertTrue("${bad.size} $what:\n" + bad.joinToString("\n"), bad.isEmpty())

    /**
     * As the techno's (screen 222): the 303's « Par partie » curve is drawn on the Drop's loop (its drum loop, one bar),
     * so it opens and closes the filter each bar; « jusqu'au milieu du Drop » promised a curve as long as the Drop.
     */
    @Test
    fun theFilterCurveIsSaidAsLongAsTheDropsLoop() {
        val bad = ArrayList<String>()
        val target = TutoTarget.of("acid_techno")!!
        var p = target.blank()
        for (s in steps.takeWhile { it.id != "filter" }.filter { s -> listOf("lane:", "notes:", "label:", "part:").any { s.check.startsWith(it) } }) {
            if (!TutoChecks.done(s, p, target)) p = TutoChecks.show(s, p, target) ?: error("${s.id}: no Montre-moi")
        }
        val drop = p.switchScene(p.allScenes.indexOfFirst { it.label == "Drop" })
        val bars = drop.drums.steps / drop.stepsPerBar
        val filter = step("filter").text
        if ("milieu du Drop" in filter) bad += "fr filter: « milieu du Drop » while the curve « Par partie » is the Drop's loop: $filter"
        if (!Regex("boucle du Drop \\($bars mesures?\\)").containsMatchIn(filter)) bad += "fr filter: not « boucle du Drop ($bars mesure) »: $filter"
        for (l in langs) tr(l, filter).let { t -> if (!Regex("(?<!\\d)$bars(?!\\d)").containsMatchIn(t)) bad += "${l.code} filter: no $bars: $t" }
        report("curves said otherwise than drawn", bad)
    }
}
