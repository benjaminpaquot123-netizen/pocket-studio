package com.pocketstudio.app.mixer

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.hasTestTag
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.human.BenchWindows
import com.pocketstudio.app.human.HumanTester
import com.pocketstudio.app.human.SeatOptions
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
 * Tour 7, D3 (seat 3b, screen 43): « en fr, deux mains, phone à 100 %, Mode Pro : depuis Mix, je vois « Voir les
 * courbes » et « Ce qui joue » entiers au-dessus de la barre du Guide ». The console's faders took the page's
 * height and the two buttons stood cut at the fold, the Guide's bar right under them.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class Tour7MixEntryAboveGuideTest : HumanTester("t7-mix-entry") {
    @Before
    fun notOnTheBench() = assumeTrue("not while the bench runs seats", System.getProperty("ps.human.dir").isNullOrBlank())

    private fun tap(tag: String) {
        val at = centerOf(tag)
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

    private fun node(tag: String): SemanticsNode? =
        compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes(atLeastOneRootRequired = false).firstOrNull()

    private fun proMix(device: String, font: Int) {
        begin(SeatOptions(device = device, font = font, simple = false))
        waitFor("the home's choices") { centerOf("launch_new") != null }
        tap("launch_new")
        tap("starter_wizard")
        if (centerOf("mode_card_pro") != null) tap("mode_card_pro")
        tap("wizard_family_Techno")
        tap("wizard_style_techno")
        tap("wizard_style_choose_techno")
        tap("wizard_create_now")
        waitFor("the song's spaces") { centerOf("tab_Mix") != null }
        tap("tab_Mix")
        waitFor("the mixer") { node("automation_entry_overview") != null }
        val m = ApplicationProvider.getApplicationContext<android.app.Application>().resources.displayMetrics
        val bar = node("guide_bar")
        assertNotNull("no Guide bar on Mix\n" + screenLines().joinToString("\n"), bar)
        val barTop = BenchWindows.drawnBounds(bar!!, m).top
        for (tag in listOf("automation_entry_open", "automation_entry_overview")) {
            val n = node(tag)
            assertNotNull("$tag missing", n)
            val b = BenchWindows.drawnBounds(n!!, m)
            assertTrue(
                "$device $font %: $tag cut ($b, its size ${n.size}, the Guide's bar at $barTop)\n" + screenLines().joinToString("\n"),
                shownWhole(n) && b.height >= n.size.height - 1f && b.bottom <= barTop + 1f,
            )
        }
    }

    @Test
    fun phoneAt100() = proMix("phone", 100)

    // phone360 at 130 %: the console and the entry cannot both stand above the bar even at 80 dp faders; the page
    // scrolls there as before (left, docs/decisions/tour-7-guide2.md).
}
