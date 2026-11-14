package com.pocketstudio.app.padlive

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.down
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.up
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
 * Audit ultra round 1, n° 8 (a DJ, an ado, a scout): « Montée » touched, nothing on the screen said it
 * was going on for its four bars. Its button stays lit (« en cours ») with a bar emptying, then goes out.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp")
class PadLiveRiserShownTest {
    @get:Rule
    val compose = createComposeRule()

    private val going = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "en cours")

    @Test
    fun theRiserIsSeenWhileItRises() {
        var risers = 0
        compose.setContent {
            PocketStudioTheme {
                PadLiveScreen(
                    PadLiveUi(PadLive.newProject().live, List(LivePads.ALL) { 0 }, page = 0, pack = "", playing = true, tempoBpm = 120.0, beatsPerBar = 4, riserBars = 4),
                    PadLiveActions(riser = { risers++ }),
                    clockOf = { -1 }, record = {}, still = true,
                )
            }
        }
        compose.onNodeWithTag("padlive_more").performClick()
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("padlive_fx_riser").performTouchInput { down(center); up() }
        compose.waitForIdle()
        repeat(10) { compose.mainClock.advanceTimeBy(100); compose.waitForIdle() }
        assertEquals(1, risers)
        // 4 bars at 120 BPM: 8 s. One second in, lit, its bar there.
        compose.onNodeWithTag("padlive_fx_riser_progress").assertExists()
        compose.onNodeWithTag("padlive_fx_riser").assert(going)
        repeat(80) { compose.mainClock.advanceTimeBy(100); compose.waitForIdle() }
        compose.onNodeWithTag("padlive_fx_riser").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
        compose.onNodeWithTag("padlive_fx_riser_progress").assertDoesNotExist()
    }
}
