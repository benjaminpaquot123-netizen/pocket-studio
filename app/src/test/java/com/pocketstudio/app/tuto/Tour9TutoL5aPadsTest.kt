package com.pocketstudio.app.tuto

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 9 lot L5a, F060: a drum step said « le pad du shaker », « le pad de percussion », « le pad du kick » while the pad
 * itself reads « Shaker », « Rim », « Kick ». The reported steps send the user to the pad by the name the pad shows.
 */
class Tour9TutoL5aPadsTest {
    /** Style, step and the name its pad shows (DefaultNames.KIT_VOICES of the step's lane, no studio voice on it). */
    private val reported = listOf(
        Triple("trance", "perc2_do", "Shaker"),
        Triple("neurofunk", "kick_do", "Kick"),
        Triple("neurofunk", "perc_do", "Rim"),
        Triple("riddim", "perc_do", "Rim"),
        Triple("reggaeton", "guira_do", "Shaker"),
    )

    @Test
    fun aReportedDrumStepNamesItsPadAsThePadShows() {
        val bad = ArrayList<String>()
        for ((style, id, name) in reported) {
            val course = TutoCourse.of(style) ?: run { bad += "no course $style"; continue }
            for (level in TutoLevel.entries) {
                val step = course.chaptersFor(level).flatMap { it.steps }.firstOrNull { it.id == id } ?: continue
                val fr = step.textFor(level)
                if (!fr.contains("« $name »")) bad += "fr $style/$id/$level wants « $name »: $fr"
                for (lang in Lang.entries.filter { it != Lang.FR }) {
                    val table = I18n.table(lang)
                    val shown = table[fr]?.takeIf { it.isNotBlank() } ?: run { bad += "${lang.code} $style/$id/$level untranslated"; null } ?: continue
                    val want = table[name]?.takeIf { it.isNotBlank() } ?: name
                    if (!shown.contains(want)) bad += "${lang.code} $style/$id/$level wants $want: $shown"
                }
            }
        }
        assertTrue("a drum step names its pad otherwise than the pad (F060):\n" + bad.distinct().joinToString("\n"), bad.isEmpty())
    }
}
