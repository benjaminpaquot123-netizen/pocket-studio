package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 5, chantier 4 (audit de prise en main, G6): with the text made bigger (Réglages › Taille 130 %, or the phone's own
 * font size) the thumb's bar went away on a common phone (411 x 914 dp: 914 / 1.3 = 703 dp, under the 720 it asked for),
 * on a small one and on Benjamin's Tab A11 upright (934 / 1.3 = 718): ▶, Annuler, Tempo and Partager left the thumb.
 * Read as a person sees it: on every Simple tab and every Pro space, the bar is there and its ▶ wholly on the screen.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-xxhdpi")
open class ThumbBarBigTextTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)

    /** Réglages › Taille. */
    protected open val uiScale = 1.3f
    /** The phone's own font size. */
    protected open val fontScale = 1f

    @After
    fun back() {
        RuntimeEnvironment.setFontScale(1f)
        h.finish()
    }

    private val simpleTabs = listOf("tab_simple_Morceau", "tab_simple_Rythme", "tab_simple_Clavier", "tab_simple_Mix", "tab_simple_Envoyer")
    private val proSpaces = listOf("tab_Structure", "tab_Sons", "tab_Mix", "tab_Plus")

    /** Where the bar is missing or its ▶ not wholly on the screen, after a touch on each of [tabs]. */
    private fun missing(simple: Boolean, tabs: List<String>): List<String> {
        if (fontScale != 1f) RuntimeEnvironment.setFontScale(fontScale)
        h.start(simple = simple, scale = uiScale)
        val screen = compose.onRoot().fetchSemanticsNode().boundsInRoot
        return tabs.mapNotNull { tab ->
            compose.onNodeWithTag(tab).performClick()
            compose.waitForIdle()
            val play = compose.onAllNodesWithTag("thumb_play").fetchSemanticsNodes().firstOrNull()?.boundsInRoot
            when {
                play == null -> "$tab: no thumb's bar"
                play.top < screen.top || play.bottom > screen.bottom + 0.5f -> "$tab: ▶ at $play, off the screen $screen"
                else -> null
            }
        }
    }

    /** What the app reads of the screen (the height it is given at the interface's size, the font size). */
    private fun seen(): String {
        val c = compose.activity.resources.configuration
        val high = (c.screenHeightDp / uiScale).toInt()
        return "ui $uiScale, font ${c.fontScale}: the app reads $high dp, thumbBarFits = ${com.pocketstudio.app.ui.thumbBarFits(high, c.fontScale)}"
    }

    @Test
    fun theThumbBarStaysOnEverySimpleTab() {
        val gone = missing(simple = true, simpleTabs)
        assertTrue(seen() + "\n" + gone.joinToString("\n"), gone.isEmpty())
    }

    @Test
    fun theThumbBarStaysInEveryProSpace() {
        val gone = missing(simple = false, proSpaces)
        assertTrue(seen() + "\n" + gone.joinToString("\n"), gone.isEmpty())
    }
}

/** A small phone (360 x 780 dp) at Taille 130 %. */
@Config(qualifiers = "w360dp-h780dp-xxhdpi")
class ThumbBarBigTextSmallPhoneTest : ThumbBarBigTextTest()

/** Benjamin's Tab A11 upright, at his own size (130 %): 934 / 1.3 = 718 dp. */
@Config(qualifiers = "sw600dp-w600dp-h934dp-port-tvdpi")
class ThumbBarBigTextTabA11Test : ThumbBarBigTextTest()

/** The phone's own font size at its largest (200 %), the app at 100 %. */
@Config(qualifiers = "w411dp-h914dp-xxhdpi")
class ThumbBarBigSystemFontTest : ThumbBarBigTextTest() {
    override val uiScale = 1f
    override val fontScale = 2f
}
