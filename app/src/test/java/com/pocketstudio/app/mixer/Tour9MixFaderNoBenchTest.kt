package com.pocketstudio.app.mixer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import com.pocketstudio.app.nav.acidTechnoSong
import com.pocketstudio.app.ui.simpleTracks
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.Project
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
 * Tour 9, lot L2 (Mix), with no bench at all: a plain Compose finger on a 360 dp phone, the phone's font at 200 %.
 * D01 (« le fader ne bouge pas », seats 04 14 18 24 28 34): a drag on a fader of the Mix (pro console and Mode
 * simple) moves its volume. D11 (seats 07 14 33 37): a touch on a strip's name opens its effects. S30 (seat 30,
 * « zone morte »): a fader follows the finger from the touch on, the first few millimetres included. D12 (seats 18
 * 49 59): a pad's sends (« Caisse ») are found from the Mix. S34 (seats 18 34): the Mix says that more tracks wait
 * on the right, and a touch shows them.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-port-hdpi", fontScale = 2.0f)
class Tour9MixFaderNoBenchTest {
    @get:Rule
    val compose = createComposeRule()

    private val volumes = mutableListOf<Pair<Int, Float>>()

    /** Five strips: on a 360 dp phone the fifth waits on the right (seat 18: « Melodia » off-screen). */
    private val five = BASIC_STRIPS + StripInfo(Mixer.TRACK_STRIPS[5], "Mélodie")

    private fun showPro(strips: List<StripInfo> = BASIC_STRIPS) {
        val channels = Project().mixer.channels
        val actions = MixerActions(setVolume = { ch, v -> volumes += ch to v })
        compose.setContent {
            PocketStudioTheme { Box(Modifier.fillMaxSize()) { MixerScreen(channels, { FloatArray(NativeEngine.METERS) }, actions, strips = strips) } }
        }
        compose.waitForIdle()
    }

    private fun showSimple() {
        val meters = FloatArray(NativeEngine.METERS)
        val loudness = FloatArray(NativeEngine.LOUDNESS) { NativeEngine.LOUDNESS_NONE }
        val ui = SimpleMixUi("Acid techno", simpleTracks(acidTechnoSong()), emptyMap(), 0f, false, null, null)
        compose.setContent {
            PocketStudioTheme {
                SimpleMixScreen(ui, { meters }, { loudness }, SimpleMixActions(setVolume = { ch, v -> volumes += ch to v }), words = { OutputWords.GOOD })
            }
        }
        compose.waitForIdle()
    }

    /** Drags [tag] down by [px] (slowly, as a finger), returns how far its value went (0..1 of the travel). */
    private fun dragDown(tag: String, px: Float, from: Float): Float {
        volumes.clear()
        compose.onNodeWithTag(tag).performScrollTo().performTouchInput {
            swipe(center, center + Offset(0f, px), durationMillis = 400)
        }
        compose.waitForIdle()
        return from - (volumes.lastOrNull()?.second ?: from)
    }

    @Test
    fun d01_aDragMovesTheProConsolesFader() {
        showPro()
        dragDown("volume_1", 60f, 0.8f)
        assertTrue("the pro Mix's fader did not move: $volumes", volumes.any { it.first == 1 })
    }

    @Test
    fun d01_aDragMovesTheSimpleMixsFader() {
        showSimple()
        val ch = simpleTracks(acidTechnoSong())[1].ch
        dragDown("mix_fader_$ch", 60f, 0.8f)
        assertTrue("the simple Mix's fader did not move: $volumes", volumes.any { it.first == ch })
    }

    @Test
    fun d11_aTouchOnAStripsNameOpensItsEffects() {
        showPro()
        compose.onNodeWithTag("strip_more_1").performScrollTo().performTouchInput { click() }
        compose.waitForIdle()
        compose.onNodeWithTag("strip_sheet").assertExists()
        compose.onNodeWithTag("effects_box_1", useUnmergedTree = true).assertExists()
    }

    /** S30: a 30 px slide moves the fader by 30 px of its travel (the touch slop is not lost on the way). */
    @Test
    fun s30_theSimpleMixsFaderFollowsTheFingerFromTheTouchOn() {
        showSimple()
        val t = simpleTracks(acidTechnoSong())[1]
        val node = compose.onNodeWithTag("mix_fader_${t.ch}").fetchSemanticsNode()
        val travel = node.size.height - 2 * 6 * node.layoutInfo.density.density
        val moved = dragDown("mix_fader_${t.ch}", 30f, t.volume)
        assertEquals("a 30 px slide on a ${travel}px travel: $volumes", 30f / travel, moved, 0.1f * 30f / travel)
    }

    @Test
    fun s30_theProConsolesFaderFollowsTheFingerFromTheTouchOn() {
        showPro()
        val travel = compose.onNodeWithTag("volume_1").fetchSemanticsNode().size.height.toFloat()
        val moved = dragDown("volume_1", 30f, 0.8f)
        assertEquals("a 30 px slide on a ${travel}px travel: $volumes", 30f / travel, moved, 0.1f * 30f / travel)
    }

    /** D12: from the Mix, « Effets du pad » in sight; one pad (pad 3, « Caisse ») and its sends are there. */
    @Test
    fun d12_aPadsSendsAreFoundFromTheMix() {
        showPro()
        compose.onNodeWithTag("console_pads").performScrollTo().assertIsDisplayed().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("pad_fx_2").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("effects_box_${Mixer.padChannel(2)}", useUnmergedTree = true).assertExists()
    }

    /** S34: five strips on a 360 dp phone: « Voir les autres pistes » in sight; a touch scrolls to the last one. */
    @Test
    fun s34_theProConsoleSaysMoreTracksWaitOnTheRight() {
        showPro(five)
        compose.onNodeWithTag("console_more_right").performScrollTo().assertIsDisplayed().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("strip_more_${five.last().ch}").assertIsDisplayed()
        compose.onNodeWithTag("console_more_right").assertDoesNotExist()
    }

    @Test
    fun s34_theSimpleMixSaysMoreTracksWaitOnTheRight() {
        showSimple()
        compose.onNodeWithTag("mix_more_right").performScrollTo().assertIsDisplayed().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("mix_out_fader").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("mix_more_right").assertDoesNotExist()
    }
}
