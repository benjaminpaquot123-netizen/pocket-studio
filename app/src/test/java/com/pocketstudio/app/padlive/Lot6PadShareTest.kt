package com.pocketstudio.app.padlive

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.LivePads
import com.pocketstudio.model.PadLive
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Audit lot 6, R10: a Live take just kept shows « Perf gardée · Écouter · Partager »; « Plus » open, its own « Partager »
 * did the same thing: two « Partager » on one screen. One only, whatever is open, and it still opens the share.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-port-hdpi", fontScale = 1.3f)
class Lot6PadShareTest {
    @get:Rule
    val compose = createComposeRule()

    private val pads: LivePads = PadLive.placed(LivePads(), 0, listOf(LiveClip("s.wav", "Kick", 1, oneShot = true)))

    private fun shareButtons() = compose.onAllNodes(hasText("Partager")).fetchSemanticsNodes().size

    @Test
    fun oneShareButtonPerScreen() {
        var shared = 0
        compose.setContent {
            PocketStudioTheme {
                PadLiveScreen(
                    PadLiveUi(pads, List(LivePads.ALL) { 0 }, page = 0, pack = "fete", playing = false, tempoBpm = 124.0, beatsPerBar = 4, takeDone = true, perfSeconds = 42),
                    PadLiveActions(share = { shared++ }), clockOf = { -1 }, still = true,
                )
            }
        }
        assertEquals("« Perf gardée »: one « Partager »", 1, shareButtons())
        compose.onNodeWithTag("padlive_more").performClick()
        compose.waitForIdle()
        assertEquals("« Plus » open beside « Perf gardée »: still one « Partager »", 1, shareButtons())
        compose.onNode(hasText("Partager")).performClick()
        assertEquals(1, shared)
    }

    @Test
    fun withoutAKeptTakeTheShareStaysUnderPlus() {
        compose.setContent {
            PocketStudioTheme {
                PadLiveScreen(
                    PadLiveUi(pads, List(LivePads.ALL) { 0 }, page = 0, pack = "fete", playing = false, tempoBpm = 124.0, beatsPerBar = 4),
                    PadLiveActions(), clockOf = { -1 }, still = true,
                )
            }
        }
        assertEquals(0, shareButtons())
        compose.onNodeWithTag("padlive_more").performClick()
        compose.waitForIdle()
        assertEquals(1, shareButtons())
        compose.onNodeWithTag("padlive_share").assertExists()
    }
}
