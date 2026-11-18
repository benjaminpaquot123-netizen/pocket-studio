package com.pocketstudio.app.ui

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
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
 * Tour 8, lot R (review of guide2, mineur): « en fr, deux mains, phone360 à 200 %, Mode Simple : sur la page Plus, les
 * rangées de tuiles tiennent comme l'estimation le dit (pas de tuile coupée sous le pli). » The rows of full tiles were
 * guessed (58 + 36 × fontScale dp a row) where a title of two or three lines takes more: full tiles, pushed under the
 * Guide's bar, where compact ones would have stood above it. Now: full tiles only when every one stands above the fold.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class Tour8PlusRowsFitTest : HumanTester("t8-plus-rows") {
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
        compose.onAllNodes(androidx.compose.ui.test.SemanticsMatcher("tag") { n ->
            n.config.getOrElseNullable(SemanticsProperties.TestTag) { null }?.let(match) == true
        }, useUnmergedTree = true).fetchSemanticsNodes(atLeastOneRootRequired = false)

    private fun plus(lang: Lang, font: Int, device: String = "phone360") {
        begin(SeatOptions(lang = lang, device = device, font = font, simple = true))
        waitFor("the home's choices") { centerOf("welcome_guided") != null || centerOf("launch_new") != null }
        if (centerOf("welcome_guided") != null) tap("welcome_guided") else { tap("launch_new"); tap("starter_wizard") }
        if (centerOf("mode_card_simple") != null) tap("mode_card_simple")
        for (t in listOf("wizard_family_Techno", "wizard_style_techno", "wizard_style_choose_techno", "wizard_create_now")) {
            if (centerOf("thumb_more") != null) break
            if (centerOf(t) != null) tap(t)
        }
        waitFor("the song's tabs") { centerOf("thumb_more") != null }
        tap("thumb_more")
        waitFor("the Plus page") { nodes { it == "plus_mine" }.isNotEmpty() }
        act("attends 1")
        if (nodes { it == "plus_tiles_full" }.isEmpty()) return // compact tiles, or the list (one a row): nothing to guess
        val m = ApplicationProvider.getApplicationContext<android.app.Application>().resources.displayMetrics
        val bar = compose.onAllNodes(hasTestTag("guide_bar"), useUnmergedTree = true).fetchSemanticsNodes(atLeastOneRootRequired = false).firstOrNull()
        assertNotNull("no Guide bar over Plus\n" + screenLines().joinToString("\n"), bar)
        val barTop = BenchWindows.drawnBounds(bar!!, m).top
        val tiles = nodes { (it.startsWith("plus_") || it.startsWith("tool_")) && it != "plus_leave" && !it.startsWith("plus_tiles") }
        assertTrue("no tiles\n" + screenLines().joinToString("\n"), tiles.size >= 6)
        for (n in tiles) {
            val bottom = n.positionInRoot.y + n.size.height
            assertTrue(
                "$lang $device $font %: full tiles, yet ${n.config[SemanticsProperties.TestTag]} stands under the fold (bottom $bottom, the Guide's bar at $barTop): compact ones were owed\n" +
                    screenLines().joinToString("\n"),
                bottom <= barTop + 1f,
            )
        }
    }

    @Test fun french100() = plus(Lang.FR, 100)
    @Test fun french130() = plus(Lang.FR, 130)
    @Test fun french160() = plus(Lang.FR, 160)
    @Test fun french200() = plus(Lang.FR, 200)
    @Test fun german130() = plus(Lang.DE, 130)
    @Test fun hindi130() = plus(Lang.HI, 130)
    @Test fun hindi160() = plus(Lang.HI, 160)
    @Test fun phoneFrench130() = plus(Lang.FR, 130, "phone")
    @Test fun phoneFrench160() = plus(Lang.FR, 160, "phone")
    @Test fun phoneHindi160() = plus(Lang.HI, 160, "phone")
    @Test fun phoneGerman200() = plus(Lang.DE, 200, "phone")
}
