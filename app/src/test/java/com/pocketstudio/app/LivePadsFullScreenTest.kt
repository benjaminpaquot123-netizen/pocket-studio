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
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.session.LivePadsActions
import com.pocketstudio.app.session.LivePadsCard
import com.pocketstudio.app.session.LivePadsFullScreen
import com.pocketstudio.app.session.LivePadsUi
import com.pocketstudio.app.session.StageTempo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
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
                    setSize = { calls += "size $it" },
                    browse = { pad, mine -> calls += "browse $pad ${if (mine) "mine" else "app"}" },
                    record = { calls += "record $it" },
                )
                if (full) {
                    LivePadsFullScreen(
                        ui, actions, close = { full = false },
                        tempoBpm = 174.0, beatsPerBar = 4, playing = true, clockOf = { 45 },
                        effects = { m -> Box(m.testTag("test_effects")) },
                        tempo = StageTempo(nudge = { calls += "tempo $it" }, open = { calls += "tempo sheet" }),
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

    /** Lot 2 (audit 35): narrow (a phone, the A11 upright with its margins), « Tout couper » keeps its word. */
    @Test
    fun toutCouperIsNamedOnANarrowScreen() {
        show(mutableListOf())
        compose.onNodeWithTag("live_pads_fullscreen").performClick()
        compose.onNodeWithTag("live_pads_full_stop", useUnmergedTree = true)
            .assert(androidx.compose.ui.test.hasAnyDescendant(androidx.compose.ui.test.hasText("Tout couper")))
    }

    @Test
    fun aPadPlaysTouchStopsAllAndAnEmptyPadAsksWhatGoesOnIt() {
        val calls = mutableListOf<String>()
        show(calls)
        compose.onNodeWithTag("live_pads_fullscreen").performClick()
        compose.onNodeWithTag("live_full_pad_1").performClick()
        compose.onNodeWithTag("live_pads_full_stop").performClick()
        // An empty pad: the question, here, with the one browser and the microphone.
        compose.onNodeWithTag("live_full_pad_5").performClick()
        compose.onNodeWithTag("live_pads_full").assertIsDisplayed()
        compose.onNodeWithText("Pad 6 : qu'est-ce que tu mets dessus ?").assertIsDisplayed()
        compose.onNodeWithTag("live_chooser_settings").assertDoesNotExist()
        compose.onNodeWithTag("live_chooser_mine").performClick()
        compose.onNodeWithTag("live_pad_chooser").assertDoesNotExist()
        compose.onNodeWithTag("live_full_pad_6").performClick()
        compose.onNodeWithTag("live_chooser_app").performClick()
        compose.onNodeWithTag("live_full_pad_7").performClick()
        compose.onNodeWithTag("live_chooser_record").performClick()
        assertEquals(listOf("tap 1", "stop all", "browse 5 mine", "browse 6 app", "record 7"), calls)
    }

    /** A long press changes a pad's sound and never switches its loop on; a one-shot plays from the finger's down. */
    @Test
    fun aLongPressChangesTheSoundWithoutPlayingTheLoop() {
        val calls = mutableListOf<String>()
        show(calls)
        compose.onNodeWithTag("live_pads_fullscreen").performClick()
        compose.onNodeWithTag("live_full_pad_0").performTouchInput { longClick() }
        compose.onNodeWithText("Pad 1 : qu'est-ce que tu mets dessus ?").assertIsDisplayed()
        compose.onNodeWithTag("live_chooser_settings").assertIsDisplayed()
        assertEquals(emptyList<String>(), calls)
        compose.onNodeWithTag("live_chooser_settings").performClick()
        // Its settings are on the page below.
        compose.onNodeWithTag("live_pads_full").assertDoesNotExist()
        assertEquals(listOf("edit 0"), calls)
        calls.clear()
        compose.onNodeWithTag("live_pads_fullscreen").performClick()
        // The one-shot (pad 3) held: it played once, then the question.
        compose.onNodeWithTag("live_full_pad_2").performTouchInput { longClick() }
        assertEquals(listOf("tap 2"), calls)
        compose.onNodeWithTag("live_pad_chooser").assertIsDisplayed()
    }

    /** « Pads − 8 + »: 6 and 12 around 8. « − 174 + » and the figure opens the tempo sheet. */
    @Test
    fun padsStepperAndTempoStepper() {
        val calls = mutableListOf<String>()
        show(calls)
        compose.onNodeWithTag("live_pads_fullscreen").performClick()
        compose.onNodeWithTag("live_pads_count").assert(androidx.compose.ui.test.hasText("8"))
        compose.onNodeWithTag("live_pads_less").performClick()
        compose.onNodeWithTag("live_pads_more").performClick()
        compose.onNodeWithTag("live_pads_tempo_minus").performClick()
        compose.onNodeWithTag("live_pads_tempo_plus").performClick()
        compose.onNodeWithTag("live_pads_tempo").performClick()
        assertEquals(listOf("size 6", "size 12", "tempo -1", "tempo 1", "tempo sheet"), calls)
    }

    /** Upright, the 8 pads are square or a little wide, never tall, the room left over above them. */
    @Test
    fun padsAreNeverTallerThanWide() {
        show(mutableListOf())
        compose.onNodeWithTag("live_pads_fullscreen").performClick()
        val pad = compose.onNodeWithTag("live_full_pad_0").fetchSemanticsNode().boundsInRoot
        val last = compose.onNodeWithTag("live_full_pad_7").fetchSemanticsNode().boundsInRoot
        val handle = compose.onNodeWithTag("live_pads_fx_toggle").fetchSemanticsNode().boundsInRoot
        val ratio = pad.width / pad.height
        assert(ratio >= 0.99f && ratio <= 1.36f) { "pad ratio $ratio" }
        // The last row sits right over the effects' handle: the pads stay under the thumb.
        assert(handle.top - last.bottom < 60f * compose.activity.resources.displayMetrics.density) { "gap ${handle.top - last.bottom}" }
    }
}
