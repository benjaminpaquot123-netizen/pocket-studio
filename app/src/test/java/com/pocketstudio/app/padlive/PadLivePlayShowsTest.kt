package com.pocketstudio.app.padlive

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
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
 * Tour 6, lot « padlive2 », P4 (control seat A b, defect #11: « le ▶ du haut reste ▶ avec 3 boucles en route ;
 * on croit que rien ne joue »): the bar's ▶ says what really plays. Loops started with the transport stopped (the
 * pads on their own) show it as ■ « Arrêter », and one touch stops them (« Tout couper »); the transport playing
 * keeps its own ■ (its stop also cuts the pads); nothing playing: ▶ « Jouer » starts the music.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class PadLivePlayShowsTest {
    @get:Rule
    val compose = createComposeRule()

    private val pads: LivePads = PadLive.placed(LivePads(), 0, LiveStarter.PADS.mapIndexed { i, p ->
        LiveClip("s$i.wav", p.name, if (p.oneShot) 1 else p.beats, p.oneShot, p.gain)
    })

    private fun said(text: String) = SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(text))

    /** The bar's ▶ with [states] and the transport [playing]; returns what one touch on it asked. */
    private fun touchPlay(states: List<Int>, playing: Boolean, expect: String): List<String> {
        val done = ArrayList<String>()
        compose.setContent {
            PocketStudioTheme {
                PadLiveScreen(
                    PadLiveUi(pads, states, page = 0, pack = "fete", playing = playing, tempoBpm = 124.0, beatsPerBar = 4),
                    PadLiveActions(togglePlay = { done += "play" }, stopAll = { done += "stop all" }),
                    clockOf = { -1 }, still = true,
                )
            }
        }
        compose.onNodeWithTag("padlive_play").assert(said(expect)).performClick()
        compose.waitForIdle()
        return done
    }

    @Test
    fun loopsOnTheirOwnShowTheSquareAndOneTouchStopsThem() {
        // Three loops playing (the kick, the 303, the bass: pads 0, 2, 3), the transport stopped.
        val states = List(LivePads.ALL) { if (it == 0 || it == 2 || it == 3) 2 else 0 }
        assertEquals(listOf("stop all"), touchPlay(states, playing = false, expect = "Arrêter"))
    }

    @Test
    fun aPadWaitingForItsLineAlreadyShowsTheSquare() {
        val states = List(LivePads.ALL) { if (it == 2) 1 else 0 }
        assertEquals(listOf("stop all"), touchPlay(states, playing = false, expect = "Arrêter"))
    }

    @Test
    fun theTransportKeepsItsOwnStopAndNothingPlayingStarts() {
        assertEquals(listOf("play"), touchPlay(List(LivePads.ALL) { if (it == 0) 2 else 0 }, playing = true, expect = "Arrêter"))
    }

    @Test
    fun nothingPlayingShowsPlay() {
        assertEquals(listOf("play"), touchPlay(List(LivePads.ALL) { 0 }, playing = false, expect = "Jouer"))
    }
}
