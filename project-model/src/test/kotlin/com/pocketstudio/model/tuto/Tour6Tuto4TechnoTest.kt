package com.pocketstudio.model.tuto

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 6, lot « tuto4 » (seats D a: Japanese, phone, Mode simple, tuto Facile; B b: German, tablet, 130 %, Mode pro,
 * tuto Pro), the techno course: a big beginner, or the musician of the Pro tutorial, reads the step and knows what to do
 * without guessing. Each test reads the course in French and in the languages concerned, from the catalogs.
 */
class Tour6Tuto4TechnoTest {
    private val course = TutoCourse.of("techno")!!
    private val langs = Lang.entries - Lang.FR
    private val steps = course.chapters.flatMap { it.steps }

    private fun step(id: String): TutoStep = steps.first { it.id == id }
    private fun chapter(id: String): TutoChapter = course.chapters.first { it.id == id }

    /** A step's words at both levels (the Pro's own when it has some). */
    private fun texts(s: TutoStep): List<String> = listOfNotNull(s.text, s.textPro)

    /** [fr] in [lang], never the French shown in its place. */
    private fun tr(lang: Lang, fr: String): String = I18n.table(lang)[fr] ?: "(no ${lang.code} translation of « $fr »)"

    /** [text] names [name] between quotes, in any language's quotes, as a pad or a button is named. */
    private fun quotes(text: String, name: String): Boolean =
        Regex("[$OPEN][\\s  ]?${Regex.escape(name.trim())}[\\s  ]?[$CLOSE]").containsMatchIn(text)

    private fun report(what: String, bad: List<String>) = assertTrue("${bad.size} $what:\n" + bad.joinToString("\n"), bad.isEmpty())

    /**
     * M4 (seats D a, B b): no contradiction: the Break keeps the hats and the « Rim »; the Montée « Créer la partie » makes
     * still plays its last beat (its silence is the transitions' step); ch. 10 says which button makes each part.
     */
    @Test
    fun noContradiction() {
        val bad = mutableListOf<String>()
        for (fr in texts(step("make_montee"))) if ("se tait" in fr) bad += "make_montee: says the end is silent, the gap step makes it: $fr"
        for (id in listOf("map_break", "make_break")) step(id).text.let { fr ->
            listOf("charleys", "« Rim »", "stab").filter { it !in fr }.forEach { bad += "$id: says nothing of the $it it keeps: $fr" }
        }
        for (s in chapter("parts").steps.filter { it.check.startsWith("part:") }) {
            val label = s.check.removePrefix("part:").substringBefore(':')
            for (fr in texts(s)) {
                if ("« Créer la partie « $label » »" !in fr) bad += "fr ${s.id}: not « Créer la partie « $label » »: $fr"
                for (l in langs) {
                    val button = tr(l, "Créer la partie « %s »")
                    val shown = Regex(Regex.escape(button.substringBefore("%s")) + ".+?" + Regex.escape(button.substringAfter("%s")))
                    tr(l, fr).let { t -> if (!shown.containsMatchIn(t)) bad += "${l.code} ${s.id}: not its button « $button »: $t" }
                }
            }
        }
        report("steps that contradict the screen", bad)
    }

    companion object {
        private const val OPEN = "«“„「『\"‘"
        private const val CLOSE = "»”“」』\"’"
    }
}
