package com.pocketstudio.app.synth

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.pocketstudio.app.nav.AppHarness
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * Audit ultra round 2, n° 27 (seat B c, screen 66, a Fold's inner screen held upright): the piano roll
 * showed a grid of 300 dp (about 450 px of 1243) under the top bar, the thumb's bar with the same
 * buttons, the Guide and the tabs: 1,3 bars and 11 notes in sight. The grid takes the screen there
 * (the keyboard under it a scroll away), and the thumb's bar gives it its room (the top bar has them all).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w690dp-h829dp-port-hdpi")
class PianoRollFoldableTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val h = AppHarness(compose)

    @After
    fun end() = h.finish()

    private fun idle() = repeat(6) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50)); compose.waitForIdle() }
    private fun has(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun onAFoldableTheNotesGridTakesTheScreen() {
        h.start(simple = false)
        val lead = h.trackId("Lead")
        compose.onNodeWithTag("tab_Sons").performClick()
        idle()
        // The seat's way: the track's chip in the Sons space.
        h.awaitTag("track_$lead")
        compose.onNodeWithTag("track_$lead").performClick()
        idle()
        h.awaitTag("mode_notes")
        compose.onNodeWithTag("mode_notes").performClick()
        idle()
        h.awaitTag("roll_view")
        val window = compose.onRoot().fetchSemanticsNode().size.height.toFloat()
        val grid = compose.onNodeWithTag("roll_view", useUnmergedTree = true).fetchSemanticsNode().size.height.toFloat()
        assertTrue("the grid ${grid.toInt()} px high of ${window.toInt()}", grid >= window * 0.5f)
        assertFalse("the thumb's bar again under the top bar's same buttons", has("thumb_bar"))
        // Back on the keyboard, the thumb's bar is back.
        compose.onNodeWithTag("mode_keyboard").performClick()
        idle()
        assertTrue("the thumb's bar on the keyboard", has("thumb_bar"))
    }
}
