package com.pocketstudio.app.ui

import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import com.pocketstudio.app.human.SeatOptions
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 7, lot 3, D5: in the Mode simple's « Más » (es, left hand, a phone at 100 %), « Live: tocar en directo » sat in
 * the page's top rows, out of the left thumb's reach. Its tile is in the screen's lower half, within either thumb.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h914dp-port-hdpi")
class PlusLiveThumbTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun tearDown() {
        I18n.lang = Lang.FR
    }

    private fun liveWithinReach(lang: Lang) {
        I18n.lang = lang
        compose.setContent {
            PocketStudioTheme {
                val entries = plusEntries(
                    home = {}, projects = {}, glossary = {}, missions = {}, simple = true, setSimple = {}, bugReport = {},
                    simpleWays = SimplePlusWays({}, {}, {}, {}, {}),
                )
                PlusSpace(entries, page = null, open = {}, back = {}, widePage = ToolPage.Aide, leave = {}, tiles = true) {}
            }
        }
        compose.waitForIdle()
        val root = compose.onRoot().getBoundsInRoot()
        val density = compose.density.density
        val w = (root.right.value * density).toInt()
        val h = (root.bottom.value * density).toInt()
        val b = compose.onNodeWithTag("plus_live").getBoundsInRoot()
        val x = (b.left.value + b.right.value) / 2 * density
        val y = (b.top.value + b.bottom.value) / 2 * density
        assertTrue("Live (${lang.code}) at ${x.toInt()},${y.toInt()} on ${w}x$h: not in the lower half", y > h / 2f)
        for (hand in listOf(SeatOptions.Hand.LEFT, SeatOptions.Hand.RIGHT)) {
            assertTrue("Live (${lang.code}) at ${x.toInt()},${y.toInt()}: out of the ${hand.word} thumb", SeatOptions.reachable(hand, x, y, w, h, density))
        }
    }

    @Test
    fun liveWithinEitherThumbInSpanish() = liveWithinReach(Lang.ES)

    @Test
    fun liveWithinEitherThumbInFrench() = liveWithinReach(Lang.FR)
}
