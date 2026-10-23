package com.pocketstudio.app

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.session.LivePadsActions
import com.pocketstudio.app.session.LivePadsCard
import com.pocketstudio.app.session.LivePadsFullScreen
import com.pocketstudio.app.session.LivePadsUi
import com.pocketstudio.app.session.bestColumns
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.LivePads
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Morceau › Pads live › « Plein écran »: the pads big, the effects a touch away, Back to leave. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class LivePadsFullScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val pads = LivePads(8)
        .with(0, LiveClip("a.wav", "Batterie", 8))
        .with(1, LiveClip("b.wav", "Basse", 16))
        .with(2, LiveClip("c.wav", "Impact", 4, oneShot = true))

    /** The card, and its full screen once « Plein écran » is touched (as AppRoot does). */
    private fun show(calls: MutableList<String>, states: List<Int> = List(8) { 0 }) {
        compose.setContent {
            PocketStudioTheme {
                var full by remember { mutableStateOf(false) }
                val ui = LivePadsUi(pads, states, editing = null)
                val actions = LivePadsActions(
                    tap = { calls += "tap $it" },
                    stopAll = { calls += "stop all" },
                    edit = { calls += "edit $it" },
                    fullScreen = { full = true },
                )
                if (full) {
                    LivePadsFullScreen(
                        ui, actions, close = { full = false },
                        tempoBpm = 174.0, beatsPerBar = 4, playing = true, clockOf = { 45 },
                        effects = { m -> Box(m.testTag("test_effects")) },
                    )
                } else {
                    LivePadsCard(ui, actions)
                }
            }
        }
    }

    @Test
    fun fullScreenShowsThePadsAndTheEffectsAndBackClosesIt() {
        show(mutableListOf(), states = listOf(2, 1, 0, 0, 0, 0, 0, 0))
        compose.onNodeWithTag("live_pads_fullscreen").performClick()
        compose.onNodeWithTag("live_pads_full").assertIsDisplayed()
        for (i in 0 until 8) compose.onNodeWithTag("live_full_pad_$i").assertIsDisplayed()
        compose.onNodeWithTag("live_pads_full_beat").assertIsDisplayed()
        // The effects slide in, and go with ✕.
        compose.onNodeWithTag("live_pads_fx_toggle").assertIsDisplayed()
        compose.onNodeWithTag("test_effects").assertDoesNotExist()
        compose.onNodeWithTag("live_pads_fx_toggle").performClick()
        compose.onNodeWithTag("test_effects").assertIsDisplayed()
        compose.onNodeWithTag("live_pads_fx_close").performClick()
        compose.onNodeWithTag("test_effects").assertDoesNotExist()
        // Back: the card again.
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("live_pads_full").assertDoesNotExist()
        compose.onNodeWithTag("live_pads").assertIsDisplayed()
    }

    @Test
    fun aPadPlaysTouchStopsAllAndAnEmptyPadIsSetUpBelow() {
        val calls = mutableListOf<String>()
        show(calls)
        compose.onNodeWithTag("live_pads_fullscreen").performClick()
        compose.onNodeWithTag("live_full_pad_1").performClick()
        compose.onNodeWithTag("live_pads_full_stop").performClick()
        // An empty pad: back to the card, its settings open.
        compose.onNodeWithTag("live_full_pad_5").performClick()
        compose.onNodeWithTag("live_pads_full").assertDoesNotExist()
        assertEquals(listOf("tap 1", "stop all", "edit 5"), calls)
    }

    @Test
    fun theGridGivesThePadsTheBiggestSize() {
        // A phone upright: 16 pads in 4 columns, 8 in 2 columns of big pads.
        assertEquals(4, bestColumns(16, 376.dp, 700.dp, 12.dp))
        assertEquals(2, bestColumns(8, 376.dp, 700.dp, 12.dp))
        // Lying down: two rows of 4, one row of 4.
        assertEquals(4, bestColumns(8, 800.dp, 330.dp, 12.dp))
        assertEquals(4, bestColumns(4, 800.dp, 330.dp, 12.dp))
    }
}
