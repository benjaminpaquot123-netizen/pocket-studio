package com.pocketstudio.app.library

import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 5, lot « langues » (promise t5-Bd-2, seat B d in Arabic: « الأغنية: Intro → لازمة → مقطع … » with « Kick, سنير, هاي هات »):
 * a right-to-left sentence reads from the right, so its « → » points back to the start, and its lists take the Arabic comma
 * « ، ». The song's plan and the parts' lines of the assistant's guide, in Arabic: « ← » and « ، »; French keeps « → » and « , ».
 * The « from → to » texts of the catalog (« Effet changé : %1$s → %2$s ») follow in Arabic.
 */
class RtlPunctuationTest {
    @After
    fun back() {
        I18n.lang = Lang.FR
    }

    private val house = StarterWizard.style("house")!!

    private fun plan(): StarterWizard.ScenePlan {
        val rows = house.lines.associate { l ->
            val cats = soundCategories(l.role, house.id, emptyList())
            l.role to WizardRow(true, cats.first().id, cats.first().options.first())
        }
        val parts = StarterWizard.Part.entries.associateWith { PartRow(it.byDefault, StarterWizard.soundsFor(it, house).first()) }
        return WizardUi(true, 0, house.id, rows, StarterWizard.Mood.Happy, parts).plan
    }

    @Test
    fun anArabicSentencePointsAndListsTheWayItReads() {
        I18n.lang = Lang.AR
        val p = plan()
        val summary = songSummary(house, p)
        assertFalse(summary, "→" in summary)
        assertTrue(summary, "←" in summary)
        val lines = songGuide("House", p, inKey = true, song = summary).first().lines
        val parts = lines.filter { " · " in it && ":" in it }
        assertTrue(lines.joinToString("\n"), parts.isNotEmpty())
        parts.forEach { assertFalse("« , » in an Arabic list: $it", Regex("[^0-9], ").containsMatchIn(it)) }
    }

    @Test
    fun theArabicCatalogsFromToTextsPointBack() {
        val table = I18n.table(Lang.AR)
        val wrong = table.filterKeys { Regex("""%\d\$s → %\d\$s""").containsMatchIn(it) }.filterValues { "→" in it }
        assertTrue(wrong.entries.joinToString("\n"), wrong.isEmpty())
    }

    @Test
    fun frenchKeepsItsArrowAndComma() {
        I18n.lang = Lang.FR
        val summary = songSummary(house, plan())
        assertTrue(summary, summary.startsWith("Intro → "))
    }
}
