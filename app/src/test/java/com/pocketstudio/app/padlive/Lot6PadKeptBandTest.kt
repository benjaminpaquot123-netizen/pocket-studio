package com.pocketstudio.app.padlive

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.LivePads
import com.pocketstudio.model.PadLive
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Audit lot 6, U21: the « Perf gardée » band (« Set kept ») came above the pads: the grid got shorter, its pads smaller
 * and lower, under the finger that plays. On a 360 dp phone at 130 %, the pads keep their size and place when the band
 * (or a one-time message) shows.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-port-hdpi", fontScale = 1.3f)
class Lot6PadKeptBandTest {
    @get:Rule
    val compose = createComposeRule()

    private val pads: LivePads = PadLive.placed(
        LivePads(), 0, listOf("Kick", "Snare", "Hat", "Clap").map { LiveClip("s.wav", it, 1, oneShot = true) },
    )

    private fun box(tag: String): Rect {
        val r = compose.onNodeWithTag(tag).getBoundsInRoot()
        return Rect(r.left.value, r.top.value, r.right.value, r.bottom.value)
    }

    private fun padsNow(): List<Rect> = listOf(0, 3, 12, 15).map { box("padlive_pad_$it") }

    @Test
    fun padsKeepSizeAndPlaceWhenTheKeptBandShows() {
        var kept by mutableStateOf(false)
        var message by mutableStateOf<String?>(null)
        compose.setContent {
            PocketStudioTheme {
                PadLiveScreen(
                    PadLiveUi(
                        pads, List(LivePads.ALL) { 0 }, page = 0, pack = "fete", playing = false, tempoBpm = 124.0, beatsPerBar = 4,
                        takeDone = kept, perfSeconds = 42, message = message,
                    ),
                    PadLiveActions(), clockOf = { -1 }, still = true,
                )
            }
        }
        compose.waitForIdle()
        val before = padsNow()
        kept = true
        compose.waitForIdle()
        compose.onNodeWithTag("padlive_perf").assertExists()
        assertEquals("« Perf gardée » shown: the pads did not move nor shrink", before, padsNow())
        kept = false
        message = "Prise posée sur le pad A5"
        compose.waitForIdle()
        compose.onNodeWithTag("padlive_message").assertExists()
        assertEquals("a message shown: the pads did not move nor shrink", before, padsNow())
        // The band is whole on the screen, clear of the pads and of the controls under it.
        kept = true
        message = null
        compose.waitForIdle()
        val band = box("padlive_perf")
        val lastPad = padsNow().last()
        val more = box("padlive_more")
        assertTrue("band $band over the pads ($lastPad)", band.top >= lastPad.bottom)
        assertTrue("band $band over « Plus » ($more)", band.bottom <= more.top)
    }
}
