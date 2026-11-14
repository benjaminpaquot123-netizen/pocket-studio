package com.pocketstudio.app.padlive

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
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
 * Fix 2, n° 2: a held effect or a « tenir » pad whose button goes away under the finger lets go
 * all the same: « Chute » held then « Moins » touched, the screen gone (turning, « Fête ») while an
 * effect is held, a pad held on page A while page B comes, a party pad held while « Fête » closes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-xxhdpi")
class PadLiveLetGoTest {
    @get:Rule
    val compose = createComposeRule()

    private val done = ArrayList<String>()

    private val pads: LivePads = (0 until 2).fold(LivePads()) { p, page ->
        PadLive.placed(p, page, List(PadLive.PAGE_SIZE) { LiveClip("p${page}_$it.wav", "Son $page $it", 1, oneShot = true) })
    }

    private var page by mutableIntStateOf(0)
    private var shown by mutableStateOf(true)

    private fun actions() = PadLiveActions(
        touch = { pad, down -> done += "touch $pad $down" },
        brake = { done += "brake $it" },
        echo = { done += "echo $it" },
    )

    private fun show() = compose.setContent {
        PocketStudioTheme {
            if (shown) {
                PadLiveScreen(
                    PadLiveUi(pads, List(LivePads.ALL) { 0 }, page = page, pack = "", playing = true, tempoBpm = 120.0, beatsPerBar = 4),
                    actions(), clockOf = { 0 }, still = true,
                )
            }
        }
    }

    @Test
    fun theBrakeHeldThenLessTouchedLetsGo() {
        show()
        compose.onNodeWithTag("padlive_more").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("padlive_fx_brake").performTouchInput { down(0, center) }
        compose.waitForIdle()
        assertEquals(listOf("brake true"), done)
        // Another finger on « Moins »: the row of « Chute » goes, under the first finger.
        compose.onNodeWithTag("padlive_more").performTouchInput { down(1, center); up(1) }
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        assertEquals(listOf("brake true", "brake false"), done)
    }

    @Test
    fun theEchoHeldWhileTheScreenGoesLetsGo() {
        show()
        compose.onNodeWithTag("padlive_fx_echo").performTouchInput { down(center) }
        compose.waitForIdle()
        shown = false
        compose.waitForIdle()
        assertEquals(listOf("echo true", "echo false"), done)
    }

    @Test
    fun aPadHeldOnPageAWhilePageBComesLetsGoOfItsOwnPad() {
        show()
        // Below its corner (the corner's touch opens its settings).
        compose.onNodeWithTag("padlive_pad_5").performTouchInput { down(androidx.compose.ui.geometry.Offset(width * 0.3f, height * 0.85f)) }
        compose.waitForIdle()
        page = 1
        compose.waitForIdle()
        assertEquals(listOf("touch 5 true", "touch 5 false"), done)
    }

    @Test
    fun aPartyPadHeldWhilePageBComesLetsGoOfItsOwnPad() {
        show()
        compose.onNodeWithTag("padlive_more").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("padlive_party_open").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("padlive_party_pad_2").performTouchInput { down(center) }
        compose.waitForIdle()
        page = 1
        compose.waitForIdle()
        assertEquals(listOf("touch 2 true", "touch 2 false"), done)
    }
}
