package com.pocketstudio.app.guide

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.hasTestTag
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
 * Tour 7, D3 (seat 3a, screen 63): « en hi, phone360 à 130 %, Mode Simple : depuis Mon morceau › ⋮ « और », aucune
 * tuile coupée par la barre du Guide » (« मेरी लाइब्रेरी » and « मदद » stood cut at the fold, the bar right under them).
 * Every tile of the page is either whole above the Guide's bar or wholly under the fold, a scroll away.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class Tour7PlusTilesAboveGuideTest : HumanTester("t7-plus-tiles") {
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

    private fun nodes(match: (String) -> Boolean): List<SemanticsNode> =
        compose.onAllNodes(androidx.compose.ui.test.SemanticsMatcher("tile") { n ->
            n.config.getOrElseNullable(androidx.compose.ui.semantics.SemanticsProperties.TestTag) { null }?.let(match) == true
        }, useUnmergedTree = true).fetchSemanticsNodes(atLeastOneRootRequired = false)

    private fun simplePlus(lang: Lang, device: String, font: Int) {
        begin(SeatOptions(lang = lang, device = device, font = font, simple = true))
        waitFor("the home's choices") { centerOf("welcome_guided") != null || centerOf("launch_new") != null }
        if (centerOf("welcome_guided") != null) tap("welcome_guided") else { tap("launch_new"); tap("starter_wizard") }
        if (centerOf("mode_card_simple") != null) tap("mode_card_simple")
        tap("wizard_family_Techno")
        tap("wizard_style_techno")
        tap("wizard_style_choose_techno")
        tap("wizard_create_now")
        waitFor("the song's tabs") { centerOf("thumb_more") != null }
        tap("thumb_more")
        waitFor("the Plus page") { nodes { it == "plus_mine" }.isNotEmpty() }
        val m = ApplicationProvider.getApplicationContext<android.app.Application>().resources.displayMetrics
        val bar = compose.onAllNodes(hasTestTag("guide_bar"), useUnmergedTree = true).fetchSemanticsNodes(atLeastOneRootRequired = false).firstOrNull()
        assertNotNull("no Guide bar over Plus\n" + screenLines().joinToString("\n"), bar)
        val barTop = BenchWindows.drawnBounds(bar!!, m).top
        val tiles = nodes { it.startsWith("plus_") || it.startsWith("tool_") }.filter { it.config.getOrElseNullable(androidx.compose.ui.semantics.SemanticsProperties.TestTag) { null } != "plus_leave" }
        assertTrue("no tiles\n" + screenLines().joinToString("\n"), tiles.size >= 6)
        for (n in tiles) {
            val tag = n.config[androidx.compose.ui.semantics.SemanticsProperties.TestTag]
            val top = n.positionInRoot.y
            val bottom = top + n.size.height
            val hidden = top >= barTop - 1f
            assertTrue(
                "$lang $device $font %: $tag cut at the fold (top $top, bottom $bottom, the Guide's bar at $barTop)\n" + screenLines().joinToString("\n"),
                hidden || (shownWhole(n) && bottom <= barTop + 1f),
            )
        }
    }

    @Test
    fun hindiSmallPhoneAt130() = simplePlus(Lang.HI, "phone360", 130)

    @Test
    fun frenchPhoneAt100() = simplePlus(Lang.FR, "phone", 100)
}
