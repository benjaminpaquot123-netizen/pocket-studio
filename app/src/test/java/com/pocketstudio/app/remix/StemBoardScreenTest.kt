package com.pocketstudio.app.remix

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import com.pocketstudio.model.Remix
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.coroutines.EmptyCoroutineContext

/** The stem board's gestures and buttons, with fake actions (the real ones are B3's). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class StemBoardScreenTest {
    @get:Rule
    val compose = createComposeRule()

    /** One minute at 120 BPM: 30 bars, a beat every 24 000 frames from frame 0. */
    private val frames = 60L * BOARD_RATE
    private val beat = 24_000L

    private class FakeActions : StemBoardActions {
        val played = mutableListOf<StemSelection>()
        var stops = 0
        val sent = mutableListOf<Pair<StemSelection, StemDestination>>()
        val chopped = mutableListOf<Triple<String, Int, Int>>()
        override fun play(sel: StemSelection) { played += sel }
        override fun stop() { stops++ }
        override fun send(sel: StemSelection, dest: StemDestination) { sent += sel to dest }
        override fun chop(setId: String, stem: Int, max: Int) { chopped += Triple(setId, stem, max) }
    }

    private val peaks = StemPeaks { stem, fpb -> FloatArray(((frames + fpb - 1) / fpb).toInt()) { i -> 0.2f + 0.1f * ((i + stem) % 7) } }

    private fun ui(playing: StemSelection? = null, bpm: Double = 120.0) =
        StemBoardUi("song", "Ma chanson", frames, bpm, 0.0, peaks, playing = playing, peaksContext = EmptyCoroutineContext)

    private fun show(state: StemBoardState, actions: StemBoardActions, ui: StemBoardUi = ui(), onClose: () -> Unit = {}) {
        compose.setContent { PocketStudioTheme { StemBoard(ui, actions, onClose, state = state) } }
        compose.waitForIdle()
    }

    @Test
    fun slidingAlongTheVoiceChoosesWholeBeatsThenListensAndSendsToAFreePad() {
        val state = StemBoardState(frames)
        val actions = FakeActions()
        show(state, actions)
        assertTrue(state.width > 0f)
        compose.onNodeWithText(com.pocketstudio.model.i18n.tr("Glisse le doigt sur une courbe, de gauche à droite, pour choisir un morceau. Deux doigts pour zoomer ; appui long puis glisse pour défiler.")).assertExists()
        compose.onNodeWithTag("board_track_3").performTouchInput {
            down(Offset(width * 0.2f, centerY))
            moveTo(Offset(width * 0.3f, centerY))
            moveTo(Offset(width * 0.5f, centerY))
            up()
        }
        assertEquals(3, state.selStem)
        assertEquals(0L, state.selStart % beat)
        assertEquals(0L, state.selEnd % beat)
        // About the fifth to the half of the song (the magnet moves each edge half a beat at most).
        assertEquals(frames * 0.2, state.selStart.toDouble(), beat / 2.0 + 1)
        assertEquals(frames * 0.5, state.selEnd.toDouble(), beat / 2.0 + 1)

        compose.onNodeWithTag("board_play").performClick()
        val sel = StemSelection("song", 3, state.selStart, state.selEnd)
        assertEquals(listOf(sel), actions.played)
        compose.onNodeWithTag("board_to_pad").performClick()
        assertEquals(listOf(sel to StemDestination.Pad(null)), actions.sent)
    }

    @Test
    fun aLongPressThenASlideScrollsAndSelectsNothing() {
        val state = StemBoardState(frames)
        show(state, FakeActions())
        compose.runOnIdle { state.zoomBy(4f, 0f) }
        assertEquals(0.0, state.start, 0.0)
        compose.onNodeWithTag("board_track_1").performTouchInput {
            down(center)
            advanceEventTime(1_000)
            moveBy(Offset(-width / 4, 0f))
            moveBy(Offset(-width / 4, 0f))
            up()
        }
        assertFalse(state.hasSelection)
        // Half the screen to the left: the song moved on by half of what the screen shows.
        assertEquals(state.width / 2 * state.framesPerPx, state.start, state.width * 0.02 * state.framesPerPx)
    }

    @Test
    fun slidingTheRulerScrollsAndATouchOnACurveClearsTheSelection() {
        val state = StemBoardState(frames)
        show(state, FakeActions())
        compose.runOnIdle {
            state.zoomBy(4f, 0f)
            state.select(0, 4 * beat, 8 * beat)
        }
        compose.onNodeWithTag("board_ruler").performTouchInput {
            down(Offset(width * 0.8f, centerY))
            moveTo(Offset(width * 0.6f, centerY))
            moveTo(Offset(width * 0.4f, centerY))
            up()
        }
        assertTrue(state.start > 0.0)
        assertTrue(state.hasSelection)  // scrolling never touches the selection
        compose.onNodeWithTag("board_track_2").performTouchInput { click(center) }
        assertFalse(state.hasSelection)
    }

    @Test
    fun aHandleMovesItsEdgeToTheBeatUnderTheFinger() {
        val state = StemBoardState(frames)
        show(state, FakeActions())
        compose.runOnIdle { state.select(3, 8 * beat, 16 * beat) }
        val from = state.x(16 * beat)
        val to = state.x(28 * beat)
        compose.onNodeWithTag("board_track_3").performTouchInput {
            down(Offset(from + 4f, centerY))
            moveTo(Offset((from + to) / 2, centerY))
            moveTo(Offset(to, centerY))
            up()
        }
        assertEquals(8 * beat, state.selStart)
        assertEquals(28 * beat, state.selEnd)
    }

    @Test
    fun withoutTheMagnetTheStretchStartsExactlyUnderTheFinger() {
        val state = StemBoardState(frames)
        show(state, FakeActions())
        compose.onNodeWithTag("board_magnet").performClick()
        assertFalse(state.magnet)
        var x0 = 0f
        compose.onNodeWithTag("board_track_0").performTouchInput {
            x0 = width * 0.31f
            down(Offset(x0, centerY))
            moveTo(Offset(width * 0.4f, centerY))
            moveTo(Offset(width * 0.47f, centerY))
            up()
        }
        assertEquals(0, state.selStem)
        assertEquals(state.frameAt(x0), state.selStart)
    }

    @Test
    fun theFadeChosenGoesWithTheStretchToEachDestination() {
        val state = StemBoardState(frames)
        val actions = FakeActions()
        show(state, actions)
        compose.runOnIdle { state.select(1, 4 * beat, 12 * beat) }
        compose.onNodeWithTag("board_fade_medium").performClick()
        compose.onNodeWithTag("board_to_instrument").performClick()
        compose.onNodeWithTag("board_fade_none").performClick()
        compose.onNodeWithTag("board_to_track").performClick()
        compose.onNodeWithTag("board_fade_small").performClick()
        compose.onNodeWithTag("board_to_live").performClick()
        val base = StemSelection("song", 1, 4 * beat, 12 * beat)
        assertEquals(
            listOf(
                base.copy(fadeInMs = 15, fadeOutMs = 60) to StemDestination.Instrument,
                base.copy(fadeInMs = 0, fadeOutMs = 0) to StemDestination.Track(null),
                base to StemDestination.Live,
            ),
            actions.sent,
        )
    }

    @Test
    fun theVoiceIsCutIntoPhrasesInOneTouchAndTheButtonHidesOnOtherStems() {
        val state = StemBoardState(frames)
        val actions = FakeActions()
        show(state, actions)
        compose.onNodeWithTag("board_chop").performClick()
        assertEquals(listOf(Triple("song", VOCALS_STEM, CHOP_MAX)), actions.chopped)
        compose.runOnIdle { state.select(0, 0, 4 * beat) }
        compose.onNodeWithTag("board_chop").assertDoesNotExist()
        compose.runOnIdle { state.select(VOCALS_STEM, 0, 4 * beat) }
        compose.onNodeWithTag("board_chop").assertExists()
    }

    @Test
    fun pinchingZoomsInAndToutVoirShowsTheWholeSongAgain() {
        val state = StemBoardState(frames)
        show(state, FakeActions())
        val whole = state.framesPerPx
        compose.onNodeWithTag("board_fit").assertIsNotEnabled()
        compose.onNodeWithTag("board_track_2").performTouchInput {
            pinch(
                Offset(centerX - 20f, centerY), Offset(centerX - width * 0.4f, centerY),
                Offset(centerX + 20f, centerY), Offset(centerX + width * 0.4f, centerY),
            )
        }
        assertTrue("zoomed in: ${state.framesPerPx} < $whole", state.framesPerPx < whole / 4)
        assertFalse(state.hasSelection)  // a pinch is not a selection
        compose.onNodeWithTag("board_fit").assertIsEnabled().performClick()
        assertEquals(whole, state.framesPerPx, 1e-9)
        assertEquals(0.0, state.start, 0.0)
    }

    @Test
    fun whileTheStretchPlaysItsButtonStopsIt() {
        val state = StemBoardState(frames)
        val actions = FakeActions()
        state.select(2, 4 * beat, 12 * beat)
        show(state, actions, ui(playing = StemSelection("song", 2, 4 * beat, 12 * beat)))
        compose.onNodeWithText(com.pocketstudio.model.i18n.tr("Arrêter"), substring = true).assertExists()
        compose.onNodeWithTag("board_play").performClick()
        assertEquals(1, actions.stops)
        assertTrue(actions.played.isEmpty())
    }

    @Test
    fun withoutATempoTheMagnetIsOffAndTheRulerCountsSeconds() {
        val state = StemBoardState(frames)
        show(state, FakeActions(), ui(bpm = 0.0))
        compose.onNodeWithTag("board_magnet").assertIsNotEnabled()
        compose.onNodeWithTag("board_track_0").performTouchInput {
            down(Offset(width * 0.25f, centerY))
            moveTo(Offset(width * 0.35f, centerY))
            moveTo(Offset(width * 0.45f, centerY))
            up()
        }
        assertTrue(state.hasSelection)
    }

    @Test
    fun theRemixScreenOpensTheBoardAndBackCloses() {
        val board = ui()
        val remix = Remix("song", "Ma chanson", 120.0, 0.0)
        compose.setContent {
            PocketStudioTheme {
                RemixScreen(RemixUi(true, SeparationState.Idle, emptyList(), remix = remix, board = board), RemixActions())
            }
        }
        compose.onNodeWithTag("board").assertDoesNotExist()
        compose.onNodeWithTag("remix_open_board").performScrollTo().performClick()
        compose.onNodeWithTag("board").assertExists()
        compose.onNodeWithTag("remix_project").assertDoesNotExist()
        compose.onNodeWithTag("board_close").performClick()
        compose.onNodeWithTag("board").assertDoesNotExist()
        compose.onNodeWithTag("remix_project").assertExists()
    }

    @Test
    fun withoutLoadedStemsThereIsNoBoardButton() {
        compose.setContent {
            PocketStudioTheme {
                RemixScreen(RemixUi(true, SeparationState.Idle, emptyList(), remix = Remix("song", "Ma chanson", 120.0, 0.0)), RemixActions())
            }
        }
        compose.onNodeWithTag("remix_project").assertExists()
        compose.onNodeWithTag("remix_open_board").assertDoesNotExist()
    }
}
