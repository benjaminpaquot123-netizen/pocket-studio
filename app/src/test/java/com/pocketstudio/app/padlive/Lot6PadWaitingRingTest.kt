package com.pocketstudio.app.padlive

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
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
 * Audit lot 6, R8, checked without the bench (the bench reads the screen about 1.1 s late): a loop pad touched while
 * another plays waits for its line (the engine says 1, [com.pocketstudio.app.rhythm.RhythmViewModel.liveStates]); the
 * screen draws its pale ring (« anneau pâle ») and says « en attente » all that time, then lights it when it plays.
 * The engine's part (it waits, at most a bar) is BenchPadLiveTest.aLoopWaitsForItsGridAndATouchWhileItWaitsCancelsIt.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-port-hdpi", fontScale = 1.3f)
class Lot6PadWaitingRingTest {
    @get:Rule
    val compose = createComposeRule()

    private val pads: LivePads = PadLive.placed(
        LivePads(), 0, listOf("Beat", "Bass").map { LiveClip("s.wav", it, 4, oneShot = false) },
    )

    /** The pad's top edge in its middle, where its ring is drawn (2 dp in: the stroke's middle). */
    private fun ringPixel(pad: Int): Int {
        val img = compose.onNodeWithTag("padlive_pad_$pad").captureToImage().toPixelMap()
        val y = (2f * 1.5f).toInt()
        return listOf(-4, 0, 4).sumOf { dx ->
            val c = img[img.width / 2 + dx, y]
            ((c.red + c.green + c.blue) * 255).toInt()
        }
    }

    private fun said(pad: Int): String =
        compose.onNodeWithTag("padlive_pad_$pad").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.ContentDescription].joinToString()

    @Test
    fun aPadTouchedWhileAnotherPlaysShowsThePaleRingUntilItsLine() {
        val states = mutableStateListOf(*Array(LivePads.ALL) { 0 })
        states[0] = 2
        compose.mainClock.autoAdvance = false
        compose.setContent {
            PocketStudioTheme {
                PadLiveScreen(
                    PadLiveUi(pads, states.toList(), page = 0, pack = "fete", playing = true, tempoBpm = 124.0, beatsPerBar = 4),
                    // As the engine does (ClipLauncher::toggle): a loop touched while another plays waits for its line.
                    PadLiveActions(touch = { pad, down -> if (down && states[pad] == 0) states[pad] = if (states.any { it == 2 }) 1 else 2 }),
                    clockOf = { 0 }, still = true,
                )
            }
        }
        repeat(10) { compose.mainClock.advanceTimeBy(100); compose.waitForIdle() }
        val dark = ringPixel(1)
        compose.onNodeWithTag("padlive_pad_1").performTouchInput { down(center); up() }
        // The touch's ripple fades; the line (a bar at 124 BPM: 1.94 s) is not there yet.
        repeat(10) { compose.mainClock.advanceTimeBy(100); compose.waitForIdle() }
        assertEquals("the engine was told and waits", 1, states[1])
        val waiting = ringPixel(1)
        assertTrue("touched while pad A1 plays: its pale ring ($dark → $waiting)", waiting - dark > 30)
        assertTrue(said(1), said(1).contains("en attente"))
        repeat(8) { compose.mainClock.advanceTimeBy(100); compose.waitForIdle() }
        assertEquals("still waiting 1.8 s on: still its ring", waiting, ringPixel(1))
        // Its line: it plays, the light comes.
        states[1] = 2
        repeat(5) { compose.mainClock.advanceTimeBy(100); compose.waitForIdle() }
        assertTrue(said(1), said(1).contains("joue"))
    }
}
