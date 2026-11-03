package com.pocketstudio.app.padlive

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import com.pocketstudio.app.ScreenshotTest
import com.pocketstudio.app.onboarding.LaunchActions
import com.pocketstudio.app.onboarding.LaunchScreen
import com.pocketstudio.app.onboarding.LaunchUi
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.LivePads
import com.pocketstudio.model.LiveStarter
import com.pocketstudio.model.PadLive
import com.pocketstudio.model.PadMode
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Pad Live (n° 25, lot B1, docs/maquettes/padlive-trois.png): the home screen's card, the pads
 * ready to play (two loops playing, one waiting for its beat), « Plus » open with a held effect's
 * veil on what plays, a pad's settings (app/screenshots/).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class PadLiveScreenshotTest : ScreenshotTest() {
    override fun wanted(name: String) = name in SHOTS

    /** Page A as a new « Live » project gets it, and four more hits. */
    private val pads: LivePads = PadLive.placed(
        LivePads(), 0,
        LiveStarter.PADS.mapIndexed { i, p -> LiveClip("s$i.wav", p.name, if (p.oneShot) 1 else p.beats, p.oneShot, p.gain) } +
            listOf(
                PadLive.withMode(LiveClip("m.wav", "Montée", 1, oneShot = true), PadMode.HOLD),
                LiveClip("i.wav", "Impact", 1, oneShot = true),
                LiveClip("c.wav", "Chute", 1, oneShot = true, group = 1),
                LiveClip("w.wav", "Sifflet", 1, oneShot = true, group = 1),
            ),
    )

    /** The kick's and the 303's loops play, the bass waits for its beat. */
    private val states = List(LivePads.ALL) { if (it == 0 || it == 2) 2 else if (it == 3) 1 else 0 }

    private fun ui(effect: Boolean = false) =
        PadLiveUi(pads, states, page = 0, pack = "fete", playing = true, tempoBpm = 124.0, beatsPerBar = 4, effectHeld = effect, canUndo = true)

    @Test
    fun padLiveHome() = shoot("padlive_home") {
        LaunchScreen(LaunchUi("Mon projet 3", style = "Techno", length = "2:40", edited = "modifié à l'instant"), LaunchActions())
    }

    @Test
    fun padLive() = shoot("padlive", fullScreen = true) {
        PadLiveScreen(ui(), PadLiveActions(), clockOf = { 6 }, record = { RecordDot() }, still = true)
    }

    @Test
    fun padLiveMore() = shoot("padlive_more", fullScreen = true, prepare = {
        compose.onNodeWithTag("padlive_more").performClick()
        compose.waitForIdle()
    }) {
        PadLiveScreen(ui(effect = true), PadLiveActions(), clockOf = { 6 }, record = { RecordDot() }, still = true)
    }

    @Test
    fun padLiveSettings() = shoot("padlive_settings", fullScreen = true, prepare = {
        compose.onNodeWithTag("padlive_mode_10").performClick()
        compose.waitForIdle()
    }) {
        PadLiveScreen(ui(), PadLiveActions(), clockOf = { 6 }, record = { RecordDot() }, still = true)
    }

    // --- Lot B2: the held effects' panels, a pad's own sound, the take recorded -------------------

    /** The finger on [tag]'s button, then slid up into the panel to its place [x], [y] (0..1 of the grid). */
    private fun holdInPanel(tag: String, x: Float, y: Float) {
        compose.onNodeWithTag(tag).performTouchInput { down(center) }
        compose.waitForIdle()
        val area = compose.onNodeWithTag("padlive_held_area").fetchSemanticsNode().boundsInRoot
        val button = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag(tag).performTouchInput {
            moveTo(androidx.compose.ui.geometry.Offset(area.left + area.width * x - button.left, area.top + area.height * (1f - y) - button.top))
        }
        compose.waitForIdle()
    }

    @Test
    fun padLiveFilter() = shoot("padlive_filter", fullScreen = true, prepare = { holdInPanel("padlive_fx_filter", 0.78f, 0.6f) }) {
        PadLiveScreen(ui(effect = true), PadLiveActions(), clockOf = { 6 }, record = { RecordDot() }, still = true)
    }

    @Test
    fun padLiveRoll() = shoot("padlive_roll", fullScreen = true, prepare = { holdInPanel("padlive_fx_roll", 0.42f, 0.5f) }) {
        PadLiveScreen(ui(effect = true), PadLiveActions(), clockOf = { 6 }, record = { RecordDot() }, still = true)
    }

    @Test
    fun padLiveSound() = shoot("padlive_sound", fullScreen = true, prepare = {
        compose.onNodeWithTag("padlive_mode_2").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("padlive_release").performScrollTo()
        compose.waitForIdle()
    }) {
        val tuned = pads.withAt(2, pads.at(2)!!.copy(pitch = -3, reverse = true, attack = 0.12f, release = 0.8f))
        PadLiveScreen(
            PadLiveUi(tuned, states, page = 0, pack = "fete", playing = true, tempoBpm = 124.0, beatsPerBar = 4, canUndo = true),
            PadLiveActions(), clockOf = { 6 }, record = { RecordDot() }, still = true,
        )
    }

    @Test
    fun padLiveTake() = shoot("padlive_take", fullScreen = true) {
        PadLiveScreen(
            PadLiveUi(pads, states, page = 0, pack = "fete", playing = true, tempoBpm = 124.0, beatsPerBar = 4, canUndo = true,
                played = PlayedTakeUi.Done(2), emptyPad = "A16"),
            PadLiveActions(), clockOf = { 6 }, record = { RecordDot() }, still = true,
        )
    }

    @Test
    fun padLiveRecording() = shoot("padlive_recording", fullScreen = true) {
        PadLiveScreen(
            PadLiveUi(pads, states, page = 0, pack = "fete", playing = true, tempoBpm = 124.0, beatsPerBar = 4, canUndo = true,
                played = PlayedTakeUi.Recording(3), message = "Ta prise est sur le pad A16."),
            PadLiveActions(), clockOf = { 6 }, record = { RecordDot() }, still = true,
        )
    }

    companion object {
        val SHOTS = setOf(
            "padlive_home", "padlive", "padlive_more", "padlive_settings",
            "padlive_filter", "padlive_roll", "padlive_sound", "padlive_take", "padlive_recording",
        )
    }
}

/** The ● of the Live take, as the bar shows it ready (the app gives the take's own button). */
@androidx.compose.runtime.Composable
private fun RecordDot() = com.pocketstudio.app.live.RecordButton(com.pocketstudio.app.live.TakeUi.Ready) {}
