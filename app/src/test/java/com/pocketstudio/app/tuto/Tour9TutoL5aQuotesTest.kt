package com.pocketstudio.app.tuto

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 9 lot L5a, F058 and F137 (techno, Pro): « touche « Créer la partie « Intro » » » read as a pile of quote marks,
 * and the Pro « Intro 2 » left out the cymbal while the Facile one left out only the riff.
 */
class Tour9TutoL5aQuotesTest {
    private fun texts(): List<Pair<String, String>> = buildList {
        for (style in TutoCourse.STYLES) {
            val course = TutoCourse.of(style) ?: continue
            for (level in TutoLevel.entries) course.chaptersFor(level).forEach { ch ->
                add("$style/${ch.id}/title/$level" to ch.titleFor(level))
                add("$style/${ch.id}/why" to ch.why)
                ch.steps.forEach { s ->
                    add("$style/${s.id}/text/$level" to s.textFor(level))
                    s.why?.let { add("$style/${s.id}/why" to it) }
                    add("$style/${s.id}/skip" to s.skip)
                }
            }
        }
    }.filter { it.second.isNotBlank() }.distinct()

    /** A quote opened again before its own closing mark (« a « b » c »), in the marks the text uses. */
    private fun nested(t: String): Boolean {
        val families = buildList {
            add(Regex("«[^»]*«"))
            add(Regex("「[^」]*「"))
            if ('„' in t) add(Regex("„[^“”]*„")) else add(Regex("“[^”]*“"))
            add(Regex("‘[^’]*‘"))
        }
        return families.any { it.containsMatchIn(t) }
    }

    @Test
    fun noCourseTextNestsAQuoteInsideTheSameMarks() {
        val bad = ArrayList<String>()
        val all = texts()
        for (lang in Lang.entries) {
            val table = I18n.table(lang)
            all.forEach { (where, fr) ->
                val shown = if (lang == Lang.FR) fr else table[fr]?.takeIf { it.isNotBlank() } ?: return@forEach
                if (nested(shown)) bad += "${lang.code} $where: $shown"
            }
        }
        assertTrue("quotes inside the same quotes (F058):\n" + bad.take(20).joinToString("\n"), bad.isEmpty())
    }

    @Test
    fun aPartLeavesOutTheSameThingsAtBothLevels() {
        val bad = ArrayList<String>()
        for (style in TutoCourse.STYLES) {
            val course = TutoCourse.of(style) ?: continue
            val facile = course.chaptersFor(TutoLevel.Facile).flatMap { it.steps }.associateBy { it.id }
            course.chaptersFor(TutoLevel.Pro).flatMap { it.steps }.filter { it.check.startsWith("part:") }.forEach { s ->
                val f = facile[s.id] ?: return@forEach
                fun but(t: String) = t.substringAfter(" sauf ", "").substringBefore('.').trim()
                val pro = but(s.textFor(TutoLevel.Pro))
                val easy = but(f.textFor(TutoLevel.Facile))
                if (pro.isNotEmpty() && easy.isNotEmpty() && pro != easy) bad += "$style/${s.id}: Facile « $easy » / Pro « $pro »"
            }
        }
        assertTrue("a part leaves out different things at each level (F137):\n" + bad.joinToString("\n"), bad.isEmpty())
    }
}
