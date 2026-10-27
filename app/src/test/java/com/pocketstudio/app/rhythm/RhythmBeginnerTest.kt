package com.pocketstudio.app.rhythm

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.down
import androidx.compose.ui.test.up
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.PadSample
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

/** The Rythme tab as a beginner meets it on a phone held upright. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class RhythmBeginnerTest {
    @get:Rule
    val compose = createComposeRule()

    private fun screen(project: Project, actions: RhythmActions = RhythmActions()) = compose.setContent {
        PocketStudioTheme {
            RhythmScreen(project, TransportUi(), LiveUi(), canUndo = false, canRedo = false, actions = actions)
        }
    }

    /**
     * The Rythme tab right after the assistant (G5: the Guide's bar, 48 dp, under it instead of the
     * « Prochaine étape » card on top), the song's Play choice. [room]: the height between the top
     * bar and the Guide's bar (status bar 24, top bar 56, the Guide's bar 48, tabs 80, gesture bar 24 dp).
     */
    private fun firstRowBottomAfterTheAssistant(room: androidx.compose.ui.unit.Dp) {
        val project = RhythmViewModel.starterProject()
        compose.setContent {
            PocketStudioTheme {
                RhythmScreen(project, TransportUi(), LiveUi(), canUndo = false, canRedo = false, actions = RhythmActions())
            }
        }
        val first = compose.onNodeWithTag("step_0").getBoundsInRoot()
        assertTrue("first step row ends at ${first.bottom}, the screen shows $room", first.bottom <= room)
        compose.onNodeWithTag("pads_hint").assertIsDisplayed()
    }

    @Test
    fun theFirstRowOfStepsIsOnAPhoneScreenWithoutScrolling() = firstRowBottomAfterTheAssistant(890.dp - 232.dp)

    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun theFirstRowOfStepsIsOnASmallPhoneScreenToo() = firstRowBottomAfterTheAssistant(800.dp - 232.dp)

    /** One transport for the whole app (the top bar and the strip under it): Rythme has no ▶, no tempo, no ↶ of its own. */
    @Test
    fun theTransportIsNotRepeatedOnTheRhythmTab() {
        screen(Project(song = listOf(com.pocketstudio.model.SongPart(0))).sanitized())
        compose.onNodeWithTag("rhythm_listen").assertDoesNotExist()
        compose.onAllNodesWithText("tout le morceau").assertCountEquals(0)
        compose.onNodeWithTag("transport_play").assertDoesNotExist()
        compose.onNodeWithTag("tempo").assertDoesNotExist()
        compose.onNodeWithTag("tap").assertDoesNotExist()
        compose.onAllNodesWithText("Jouer", substring = true).assertCountEquals(0)
    }

    /** The tempo sheet's − and + (the only ones left) repeat while held. */
    @Test
    fun holdingTempoPlusInTheSheetRepeats() {
        // The tempo as the app feeds it back: a state, so that each step counts from the last.
        var tempo by androidx.compose.runtime.mutableStateOf(120.0)
        compose.setContent {
            PocketStudioTheme {
                com.pocketstudio.app.ui.TempoPad(tempo, com.pocketstudio.app.ui.TempoActions(set = { tempo = it }), close = {})
            }
        }
        compose.mainClock.autoAdvance = false
        val plus = compose.onNodeWithContentDescription("Tempo plus")
        plus.performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(1500)
        plus.performTouchInput { up() }
        compose.mainClock.advanceTimeBy(100)
        assertTrue("held 1.5 s: $tempo", tempo >= 125.0)
        // A short touch is one step.
        val before = tempo
        plus.performClick()
        compose.mainClock.advanceTimeBy(100)
        assertEquals(before + 1, tempo, 0.0)
        compose.onNodeWithContentDescription("Tempo moins").performClick()
        compose.mainClock.advanceTimeBy(100)
        assertEquals(before, tempo, 0.0)
    }

    @Test
    fun aPadShowsTheSoundPlacedOnIt() {
        val project = Project(drums = DrumPattern().withSample(2, PadSample("c.wav", "Clap de cuisine")))
        assertEquals("Clap de cuisine", padName(project, 2))
        assertEquals(DrumKitInfo.names[0], padName(project, 0))
    }

    @Test
    fun thePadSoundCardStartsWithChangingTheSound() {
        var library = 0
        val wave = FloatArray(4800) { i -> kotlin.math.sin(i * 0.05).toFloat() }
        val info = SampleInfo(0.1, PadSamples.peaks(com.pocketstudio.app.audio.Pcm(wave, 1, 48000)))
        val project = Project(drums = DrumPattern().withSample(0, PadSample("k.wav", "Kick 2")))
        compose.setContent {
            PocketStudioTheme { PadSoundCard(project, 0, PadSoundUi(mapOf("k.wav" to info)), RhythmActions(openLibrary = { library++ })) }
        }
        compose.onNodeWithText("SON DU PAD").assertExists()
        compose.onNodeWithTag("open_library").performClick()
        assertEquals(1, library)
        // The sample's editor is folded: a beginner sees the sound, not its knobs.
        compose.onNodeWithText("Début").assertDoesNotExist()
        compose.onNodeWithTag("edit_sample").performClick()
        compose.onNodeWithText("Début").assertExists()
    }
}
