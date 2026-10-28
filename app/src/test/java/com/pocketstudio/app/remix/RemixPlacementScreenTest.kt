package com.pocketstudio.app.remix

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import com.pocketstudio.model.PadSample
import com.pocketstudio.model.Remix
import com.pocketstudio.model.RemixVoice
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Remix lot 1, R2, on the Remix page: where the song comes in is said in words and drawn over the
 * project's song; a touch on « Drop » or a drag of the song places it; a cut's start, end and fade
 * are at hand under it; every pad used, « → Pad » asks before covering one.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w392dp-h850dp-xxhdpi")
class RemixPlacementScreenTest {
    @get:Rule
    val rule = createComposeRule()

    /** Intro 8 bars, Montée 4, Drop 8, Drop 8 again (16 sixteenths a bar). */
    private val plan = SongPlan(
        listOf("Intro" to 128, "Montée" to 64, "Drop" to 128, "Drop" to 128), 16,
        listOf(RemixVoice.Place(0, "Intro", 0), RemixVoice.Place(1, "Montée", 128), RemixVoice.Place(2, "Drop", 192)),
    )

    private val remix = Remix("set", "Ma chanson", songBpm = 120.0, seconds = 40.0)

    @Test
    fun theSongIsPlacedOnTheDropByATouchOrADrag() {
        var at by mutableStateOf(0)
        val moves = ArrayList<Int>()
        rule.setContent {
            PocketStudioTheme {
                RemixScreen(
                    RemixUi(true, SeparationState.Idle, emptyList(), remix = remix.copy(atStep = at), plan = plan, songSteps = 320),
                    RemixActions(setAt = { moves += it; at = it }),
                )
            }
        }
        rule.onNodeWithTag("remix_at_text").assertTextContains("La chanson entre à la mesure 1, au début de « Intro ».")
        // « Drop »: the song comes in on bar 13, said so.
        rule.onNodeWithTag("remix_place_2").performScrollTo().performClick()
        assertEquals(192, at)
        rule.onNodeWithTag("remix_at_text").assertTextContains("La chanson entre à la mesure 13, au début de « Drop ».")
        rule.onNodeWithTag("remix_place_2").assertIsSelected()
        // A bar later: inside the drop.
        rule.onNodeWithTag("remix_at_next").performScrollTo().performClick()
        assertEquals(208, at)
        rule.onNodeWithTag("remix_at_text").assertTextContains("La chanson entre à la mesure 14, dans « Drop ».")
        rule.onNodeWithTag("remix_at_prev").performClick()
        assertEquals(192, at)
        // Dragged back towards the start: one move, on a bar line, earlier.
        moves.clear()
        rule.onNodeWithTag("remix_plan").performScrollTo().performTouchInput {
            val y = centerY
            val from = width * 192f / 512f + 4f
            down(androidx.compose.ui.geometry.Offset(from, y))
            repeat(10) { moveBy(androidx.compose.ui.geometry.Offset(-from / 12f, 0f)) }
            up()
        }
        assertEquals("one undo step for a drag: $moves", 1, moves.size)
        assertTrue("$moves", moves.single() < 192 && moves.single() % 16 == 0)
        // A touch on the strip puts its start there (here: the first half, the start of the song).
        rule.onNodeWithTag("remix_plan").performTouchInput { click(androidx.compose.ui.geometry.Offset(2f, centerY)) }
        assertEquals(0, at)
    }

    @Test
    fun theCutIsTrimmedUnderTheVoice() {
        var sample by mutableStateOf(PadSample("a.wav", "Voix 1", attack = RemixVoice.CUT_ATTACK, release = RemixVoice.CUT_RELEASE))
        var played = 0
        rule.setContent {
            PocketStudioTheme {
                RemixScreen(
                    RemixUi(
                        true, SeparationState.Idle, emptyList(), remix = remix, plan = plan, songSteps = 320,
                        cut = RemixCutUi(9, "« Voix 1 » est sur le pad 10. Pose-le dans Batterie, sur la partie de ton choix.", sample, 2.5, FloatArray(64) { 0.5f }),
                    ),
                    RemixActions(editCut = { _, t -> sample = t(sample) }, playCut = { played++ }),
                )
            }
        }
        rule.onNodeWithTag("remix_cut_message").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("remix_cut_start").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("remix_cut_end").performScrollTo().assertIsDisplayed()
        // « Court » is what a cut comes with; « Long » fades more.
        rule.onNodeWithTag("remix_cut_fade_1").performScrollTo().assertIsSelected()
        rule.onNodeWithTag("remix_cut_fade_2").performClick()
        assertEquals(0.3f, sample.release)
        rule.onNodeWithTag("remix_cut_fade_2").assertIsSelected()
        // The start slider moved: the cut starts later.
        rule.onNodeWithTag("remix_cut_start").performScrollTo().performTouchInput { click(androidx.compose.ui.geometry.Offset(width * 0.3f, centerY)) }
        assertTrue("${sample.start}", sample.start in 0.15f..0.45f)
        rule.onNodeWithTag("remix_cut_play").performClick()
        assertEquals(1, played)
    }

    @Test
    fun everyPadUsedAsksBeforeCoveringOne() {
        var asked by mutableStateOf<PadAsk?>(PadAsk(0, "Kick"))
        var confirmed = 0
        rule.setContent {
            PocketStudioTheme {
                RemixScreen(
                    RemixUi(true, SeparationState.Idle, emptyList(), remix = remix, padAsk = asked),
                    RemixActions(confirmPad = { confirmed++; asked = null }, dismissPadAsk = { asked = null }),
                )
            }
        }
        rule.onNodeWithText("Remplacer le son du pad 1 (« Kick ») par ce passage ? Partout où ton rythme joue ce pad, on entendra la chanson.").assertIsDisplayed()
        rule.onNodeWithTag("remix_pad_replace").performClick()
        assertEquals(1, confirmed)
        rule.onNodeWithTag("remix_pad_ask").assertDoesNotExist()
    }
}
