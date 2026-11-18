package com.pocketstudio.app.session

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.human.BenchWindows
import com.pocketstudio.app.human.HumanTester
import com.pocketstudio.app.human.SeatOptions
import com.pocketstudio.model.i18n.Lang
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 8, lot R (seat 3a, « piste T sans preuve »): « en hi, phone360 à 130 %, Mode Simple : dans Mon morceau, après
 * « + Nouvelle partie vide », je vois la partie ajoutée, entière, sans chercher. » After an add the column was brought to the
 * add buttons only: the new row, above them, could stay out of sight, and nothing showed the part was made.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class Tour8NewPartShownTest : HumanTester("t8-new-part") {
    @Before
    fun notOnTheBench() = assumeTrue("not while the bench runs seats", System.getProperty("ps.human.dir").isNullOrBlank())

    private fun tap(tag: String) {
        val at = reachTag(tag)
        assertNotNull("nothing to touch: $tag\n" + screenLines().joinToString("\n"), at)
        act("touche ${at!!.first} ${at.second}")
    }

    private fun waitFor(what: String, done: () -> Boolean) {
        val until = System.currentTimeMillis() + 60_000
        while (!done()) {
            assertTrue("$what never came\n" + screenLines().joinToString("\n"), System.currentTimeMillis() < until)
            act("attends 0.5")
        }
    }

    private fun parts(): List<SemanticsNode> =
        compose.onAllNodes(androidx.compose.ui.test.SemanticsMatcher("part") { n ->
            n.config.getOrElseNullable(SemanticsProperties.TestTag) { null }?.matches(Regex("part_\\d+")) == true
        }, useUnmergedTree = true).fetchSemanticsNodes(atLeastOneRootRequired = false)

    private fun addsShowTheNewPart(lang: Lang, device: String, font: Int) {
        begin(SeatOptions(lang = lang, device = device, font = font, simple = true))
        waitFor("the home's choices") { centerOf("welcome_guided") != null || centerOf("launch_new") != null }
        if (centerOf("welcome_guided") != null) tap("welcome_guided") else { tap("launch_new"); tap("starter_wizard") }
        if (centerOf("mode_card_simple") != null) tap("mode_card_simple")
        for (t in listOf("wizard_family_Techno", "wizard_style_techno", "wizard_style_choose_techno", "wizard_create_now")) {
            if (reachTag(t) != null) tap(t)
        }
        waitFor("the song's tabs") { centerOf("tab_simple_Morceau") != null }
        tap("tab_simple_Morceau")
        centerOf("guide_later")?.let { act("touche ${it.first} ${it.second}") }
        val m = ApplicationProvider.getApplicationContext<android.app.Application>().resources.displayMetrics
        repeat(3) { round ->
            val before = parts().size
            tap("add_part_new_empty")
            act("attends 1")
            val rows = parts()
            assertTrue("no part added (round $round)\n" + screenLines().joinToString("\n"), rows.size == before + 1)
            val last = rows.maxBy { it.config[SemanticsProperties.TestTag].removePrefix("part_").toInt() }
            val b = BenchWindows.drawnBounds(last, m)
            assertTrue(
                "$lang $device $font %, round $round: the new part ${last.config[SemanticsProperties.TestTag]} is not shown whole after « + Nouvelle partie vide » (drawn ${b.height} of ${last.size.height} px)\n" +
                    screenLines().joinToString("\n"),
                shownWhole(last) && b.height >= last.size.height - 1f,
            )
        }
    }

    @Test fun hindiSmallPhoneAt130() = addsShowTheNewPart(Lang.HI, "phone360", 130)
    @Test fun frenchPhoneAt100() = addsShowTheNewPart(Lang.FR, "phone", 100)
    @Test fun frenchSmallPhoneAt200() = addsShowTheNewPart(Lang.FR, "phone360", 200)
}
