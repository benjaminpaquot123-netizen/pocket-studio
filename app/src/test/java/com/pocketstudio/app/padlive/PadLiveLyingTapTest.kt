package com.pocketstudio.app.padlive

import androidx.compose.ui.test.down
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.up
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.LivePads
import com.pocketstudio.model.LiveStarter
import com.pocketstudio.model.PadLive
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Audit ultra round 1, n° 7 (a DJ, phone lying down): a touch in the middle of a pad opened its
 * settings (the corner's « boucle » zone reached it on the low pads), only « Gros pads » played.
 * The middle of every pad plays it; its corner word still opens the settings.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w800dp-h360dp-land-hdpi")
class PadLiveLyingTapTest {
    @get:Rule
    val compose = createComposeRule()

    private val pads: LivePads = PadLive.placed(LivePads(), 0, LiveStarter.PADS.mapIndexed { i, p ->
        LiveClip("s$i.wav", p.name, if (p.oneShot) 1 else p.beats, p.oneShot, p.gain)
    })

    @Test
    fun theMiddleOfALowPadPlaysIt() {
        val done = ArrayList<String>()
        compose.setContent {
            PocketStudioTheme {
                PadLiveScreen(
                    PadLiveUi(pads, List(LivePads.ALL) { 0 }, page = 0, pack = "fete", playing = false, tempoBpm = 124.0, beatsPerBar = 4, canUndo = true),
                    PadLiveActions(touch = { pad, down -> done += "touch $pad ${if (down) "down" else "up"}" }),
                    clockOf = { -1 }, record = {}, still = true,
                )
            }
        }
        for (pad in listOf(0, 5, 15)) {
            done.clear()
            compose.onNodeWithTag("padlive_pad_$pad").performTouchInput { down(center); up() }
            compose.waitForIdle()
            assertEquals("pad $pad", listOf("touch $pad down", "touch $pad up"), done)
            compose.onNodeWithTag("padlive_settings").assertDoesNotExist()
        }
        // The corner's word opens the settings, as before.
        compose.onNodeWithTag("padlive_mode_0").performTouchInput { down(center); up() }
        compose.waitForIdle()
        compose.onNodeWithTag("padlive_settings").assertExists()
    }
}
