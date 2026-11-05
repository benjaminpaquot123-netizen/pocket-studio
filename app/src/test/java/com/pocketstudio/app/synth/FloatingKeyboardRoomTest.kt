package com.pocketstudio.app.synth

import android.os.Looper
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import com.pocketstudio.model.Project
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * Audit ultra round 2, n° 25 (seat B c, screens 20, 23): the keyboard that follows, over the page, covered the
 * ready-made sounds (« Au hasard », « Garder ce son ») and its folded « Clavier » covered « Unisson »'s value.
 * It has its own room under the page now: whatever the page shows can be seen and touched.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h700dp")
class FloatingKeyboardRoomTest {
    @get:Rule
    val compose = createComposeRule()

    private fun idle() = repeat(6) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(60)); compose.waitForIdle() }
    private fun has(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    private fun seen(tag: String): Rect = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    @Test
    fun theFollowingKeyboardNeverCoversTheSounds() {
        compose.setContent {
            PocketStudioTheme {
                SynthScreen(
                    Project().synth, 4, SynthActions(),
                    browser = com.pocketstudio.app.sounds.SoundBrowserUi(
                        com.pocketstudio.model.SoundCategories.SYNTH, com.pocketstudio.model.SoundLibrary.SYNTH, emptyList(),
                        current = com.pocketstudio.model.SoundLibrary.SYNTH[1].name,
                    ),
                )
            }
        }
        idle()
        // Down to the ready-made sounds' buttons: the real keyboard out of sight, the one that follows comes.
        repeat(2) {
            compose.onNodeWithTag("sounds_save").performScrollTo()
            idle()
        }
        assertTrue("the keyboard that follows", has("float_keyboard"))
        val keys = seen("float_keyboard")
        for (tag in listOf("sounds_random", "sounds_save")) {
            val b = seen(tag)
            assertTrue("$tag out of sight ($b)", b.height > 1f)
            assertTrue("$tag under the keyboard ($b, keyboard $keys)", !b.overlaps(keys))
        }
    }
}
