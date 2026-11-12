package com.pocketstudio.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import androidx.compose.ui.unit.dp
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Remix lot 1 (Sarah, Mehdi: « le gros ▶ ne joue que la boucle d'une partie »): the whole song is one
 * touch away under the top bar, and ▶ set to loop a part says so in a word, not an orange ⟳ alone.
 */
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h800dp-xxhdpi")
class WholeSongPlayTest {
    @get:Rule
    val compose = createComposeRule()

    private fun strip(loopPart: Boolean, width: Int = 392, actions: PlayStripActions = PlayStripActions()) = compose.setContent {
        PocketStudioTheme {
            Box(Modifier.width(width.dp)) {
                PlayStrip(
                    PlayStripUi(
                        174.0, hasSong = true, loopPart = loopPart, playing = false, position = { PlayPosition("Drop", 1, 8, loop = loopPart) },
                        parts = listOf("Intro 2", "Intro 3", "Montée", "Drop"), helpOn = false,
                    ),
                    actions,
                )
            }
        }
    }

    @Test
    fun jouerDepuisStartsWithTheWholeSongFromItsStart() {
        var whole = 0
        var from = -1
        strip(loopPart = true, actions = PlayStripActions(playFrom = { from = it }, playWholeSong = { whole++ }))
        compose.onNodeWithTag("top_position").performClick()
        compose.onNodeWithTag("play_from_whole").assertIsDisplayed()
        compose.onRoot().captureRoboImage("screenshots/play_from_whole.png")
        compose.onAllNodesWithText("Tout le morceau, du début").fetchSemanticsNodes().let { assertEquals(1, it.size) }
        // First in the list, above the parts.
        val wholeTop = compose.onNodeWithTag("play_from_whole").fetchSemanticsNode().boundsInRoot.top
        val firstPart = compose.onNodeWithTag("play_from_0").fetchSemanticsNode().boundsInRoot.top
        assertTrue(wholeTop < firstPart)
        compose.onNodeWithTag("play_from_whole").performClick()
        assertEquals(1, whole)
        assertEquals(-1, from)
    }

    @Test
    fun theLoopSaysItsWordWhenOnBesideTheHelpButton() {
        strip(loopPart = true)
        assertEquals(1, compose.onAllNodesWithText("Boucler").fetchSemanticsNodes().size)
        compose.onRoot().captureRoboImage("screenshots/play_strip_loop_on.png")
        // The position keeps its words beside it.
        compose.onNodeWithTag("top_position_bar", useUnmergedTree = true).assertIsDisplayed()
        val position = compose.onNodeWithTag("top_position").fetchSemanticsNode().boundsInRoot
        val loop = compose.onNodeWithTag("top_loop_part").fetchSemanticsNode().boundsInRoot
        val help = compose.onNodeWithTag("help_labels_button").fetchSemanticsNode().boundsInRoot
        assertTrue("the chips overlap: $position $loop $help", position.right <= loop.left && loop.right <= help.left && help.right <= 392f * compose.density.density + 0.5f)
    }

    @Test
    fun offTheLoopIsItsIconAloneOnAPhone() {
        strip(loopPart = false)
        assertEquals(0, compose.onAllNodesWithText("Boucler").fetchSemanticsNodes().size)
    }
}
