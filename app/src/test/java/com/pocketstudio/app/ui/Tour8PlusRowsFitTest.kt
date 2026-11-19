package com.pocketstudio.app.ui

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.hasTestTag
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.human.BenchWindows
import com.pocketstudio.app.human.HumanTester
import com.pocketstudio.app.human.SeatOptions
import com.pocketstudio.model.i18n.Lang
import org.junit.Assert.assertEquals
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
 *
 * Tour 9, L9: each case names the tiles it must get. Its eleven first cases all got compact tiles or the list, and the
 * test returned before looking at anything (a « full » choice owed to a wrong guess was never seen): the screens where
 * full tiles are owed (phone411 and tablet8) are added, and with compact tiles or the list no row stands across the fold.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class Tour8PlusRowsFitTest : HumanTester("t8-plus-rows") {
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

    private fun nodes(match: (String) -> Boolean): List<SemanticsNode> =
        compose.onAllNodes(androidx.compose.ui.test.SemanticsMatcher("tag") { n ->
            n.config.getOrElseNullable(SemanticsProperties.TestTag) { null }?.let(match) == true
        }, useUnmergedTree = true).fetchSemanticsNodes(atLeastOneRootRequired = false)

    private fun plus(lang: Lang, font: Int, device: String, layout: String) {
        begin(SeatOptions(lang = lang, device = device, font = font, simple = true))
        waitFor("the home's choices") { centerOf("welcome_guided") != null || centerOf("launch_new") != null }
        if (centerOf("welcome_guided") != null) tap("welcome_guided") else { tap("launch_new"); tap("starter_wizard") }
        if (centerOf("mode_card_simple") != null) tap("mode_card_simple")
        for (t in listOf("wizard_family_Techno", "wizard_style_techno", "wizard_style_choose_techno", "wizard_create_now")) {
            if (reachTag(t) != null) tap(t)
        }
        waitFor("the song's tabs") { centerOf("thumb_more") != null }
        tap("thumb_more")
        waitFor("the Plus page") { nodes { it == "plus_mine" }.isNotEmpty() }
        act("attends 1")
        val shown = nodes { it.startsWith("plus_tiles_") }.map { it.config[SemanticsProperties.TestTag] }
        val m = ApplicationProvider.getApplicationContext<android.app.Application>().resources.displayMetrics
        val bar = compose.onAllNodes(hasTestTag("guide_bar"), useUnmergedTree = true).fetchSemanticsNodes(atLeastOneRootRequired = false).firstOrNull()
        assertNotNull("no Guide bar over Plus\n" + screenLines().joinToString("\n"), bar)
        val barTop = BenchWindows.drawnBounds(bar!!, m).top
        val tiles = nodes { (it.startsWith("plus_") || it.startsWith("tool_")) && it != "plus_leave" && !it.startsWith("plus_tiles") }
        assertTrue("no tiles\n" + screenLines().joinToString("\n"), tiles.size >= 6)
        for (n in tiles) {
            val top = n.positionInRoot.y
            val bottom = top + n.size.height
            val tag = n.config[SemanticsProperties.TestTag]
            if (shown == listOf(FULL)) {
                assertTrue(
                    "$lang $device $font %: full tiles, yet $tag stands under the fold (bottom $bottom, the Guide's bar at $barTop): compact ones were owed\n" +
                        screenLines().joinToString("\n"),
                    bottom <= barTop + 1f,
                )
            } else {
                // Compact tiles or the list scroll: a row stands whole above the fold or starts below it, never across it.
                assertTrue(
                    "$lang $device $font %: $tag stands across the fold (top $top, bottom $bottom, the Guide's bar at $barTop)\n" + screenLines().joinToString("\n"),
                    bottom <= barTop + 1f || top >= barTop - 1f,
                )
            }
        }
        // Then the choice itself: full tiles where they all stand above the fold, compact ones (or the list) where not.
        assertEquals("$lang $device $font %: the Plus page's tiles\n" + screenLines().joinToString("\n"), listOf(layout), shown)
    }

    @Test fun french100() = plus(Lang.FR, 100, "phone360", COMPACT)
    @Test fun french130() = plus(Lang.FR, 130, "phone360", COMPACT)
    @Test fun french160() = plus(Lang.FR, 160, "phone360", LIST)
    @Test fun french200() = plus(Lang.FR, 200, "phone360", LIST)
    @Test fun german130() = plus(Lang.DE, 130, "phone360", COMPACT)
    @Test fun hindi130() = plus(Lang.HI, 130, "phone360", COMPACT)
    @Test fun hindi160() = plus(Lang.HI, 160, "phone360", LIST)
    @Test fun phoneFrench130() = plus(Lang.FR, 130, "phone", COMPACT)
    @Test fun phoneFrench160() = plus(Lang.FR, 160, "phone", COMPACT)
    @Test fun phoneHindi160() = plus(Lang.HI, 160, "phone", COMPACT)
    @Test fun phoneGerman200() = plus(Lang.DE, 200, "phone", LIST)

    // The screens where every row of full tiles stands above the fold: the case the measure decides.
    @Test fun phone411French100() = plus(Lang.FR, 100, "phone411", FULL)
    @Test fun phone411Hindi100() = plus(Lang.HI, 100, "phone411", FULL)
    @Test fun phone411German115() = plus(Lang.DE, 115, "phone411", COMPACT)
    @Test fun tabletFrench100() = plus(Lang.FR, 100, "tablet8", FULL)
    @Test fun tabletHindi160() = plus(Lang.HI, 160, "tablet8", FULL)
    @Test fun tabletFrench200() = plus(Lang.FR, 200, "tablet8", COMPACT)

    private companion object {
        const val FULL = "plus_tiles_full"
        const val COMPACT = "plus_tiles_compact"
        const val LIST = "plus_tiles_list"
    }
}
