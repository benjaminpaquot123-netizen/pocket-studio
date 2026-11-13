package com.pocketstudio.app.padlive

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.LivePads
import com.pocketstudio.model.PadLive
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Audit lot 6, U18: « Plus » open, its button says « Moins » (« Tutup », « 閉じる »: close) but kept the ⋯ of « Plus »: it
 * looked like a menu still to open. Open, it wears the fold (^); closed, the ⋯ again.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-port-hdpi", fontScale = 1.3f)
class Lot6PadMoreIconTest {
    @get:Rule
    val compose = createComposeRule()

    private val pads: LivePads = PadLive.placed(LivePads(), 0, listOf(LiveClip("s.wav", "Kick", 1, oneShot = true)))

    @Test
    fun closeHasNoDotsIcon() {
        compose.setContent {
            PocketStudioTheme {
                PadLiveScreen(
                    PadLiveUi(pads, List(LivePads.ALL) { 0 }, page = 0, pack = "fete", playing = false, tempoBpm = 124.0, beatsPerBar = 4),
                    PadLiveActions(), clockOf = { -1 }, still = true,
                )
            }
        }
        compose.onNodeWithTag("padlive_more_dots", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("padlive_more").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("padlive_more_dots", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("padlive_more_fold", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("padlive_more").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("padlive_more_dots", useUnmergedTree = true).assertExists()
    }
}
