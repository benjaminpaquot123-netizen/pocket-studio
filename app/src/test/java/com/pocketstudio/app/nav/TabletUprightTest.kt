package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Audit final (09, grave): Benjamin's Galaxy Tab A11 held upright (600 × 934 dp). At 130 % the
 * keyboard's box was wider than high and the keyboard took its lying-down layout: two octaves in
 * ~18 dp keys, « Octave » cut. The same with the Guide's panel open on an upright screen.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "sw600dp-w600dp-h934dp-port-tvdpi")
class TabletUprightTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)

    @After
    fun end() = h.finish()

    private fun tap(tag: String) {
        compose.onNodeWithTag(tag).performClick()
        compose.waitForIdle()
    }

    private fun assertUprightKeys(what: String) {
        assertEquals("$what: the lying-down row of keys", 0, compose.onAllNodesWithTag("synth_keys_row").fetchSemanticsNodes().size)
        val glass = compose.activity.resources.displayMetrics.density
        // A white key (C4): two octaves across the width, at least 30 dp on the glass (18 dp before; a phone gives 26).
        val w = compose.onNodeWithTag("key_60").fetchSemanticsNode().size.width / glass
        assertTrue("$what: a white key is $w dp wide on the glass", w >= 30f)
    }

    @Test
    fun theModeSimplesClavierKeepsUprightKeysAt130() {
        h.start(simple = true, scale = 1.3f)
        tap("tab_simple_Clavier")
        assertUprightKeys("A11 upright, 130 %")
    }

    @Test
    fun theSonsKeyboardKeepsUprightKeysAt130() {
        h.start(scale = 1.3f)
        tap("tab_Sons")
        tap("track_${h.trackId("Lead")}")
        assertUprightKeys("A11 upright, Sons, 130 %")
    }

    @Test
    fun theGuidesPanelOpenKeepsUprightKeys() {
        h.start(simple = true)
        tap("tab_simple_Clavier")
        tap("guide_bar")
        compose.onNodeWithTag("guide_panel").assertExists()
        assertUprightKeys("A11 upright, Guide open")
    }

    /** Audit final (09): upright at 130 %, the second row of steps above the fold. */
    @Test
    fun theSecondRowOfStepsShowsAt130() {
        h.start(scale = 1.3f)
        tap("tab_Sons")
        tap("sons_Rythme")
        fun bottomOf(tag: String) = compose.onNodeWithTag(tag).fetchSemanticsNode().let { it.positionInRoot.y + it.size.height }
        val pane = bottomOf("rhythm")
        assertTrue("the second row of steps ends at ${bottomOf("step_8")} (pane $pane)", bottomOf("step_8") <= pane + 1f)
        val glass = compose.activity.resources.displayMetrics.density
        val pad = compose.onNodeWithTag("pad_12").fetchSemanticsNode().size.height / glass
        assertTrue("a pad is $pad dp high on the glass", pad >= 47.5f)
    }
}
