package com.pocketstudio.app.rhythm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.down
import androidx.compose.ui.test.up
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

    private fun screen(project: Project, actions: RhythmActions = RhythmActions(), listenSong: Boolean? = null) = compose.setContent {
        PocketStudioTheme {
            RhythmScreen(project, TransportUi(), LiveUi(), canUndo = false, canRedo = false, actions = actions, listenSong = listenSong)
        }
    }

    @Test
    fun theFirstRowOfStepsIsOnAPhoneScreenWithoutScrolling() {
        screen(RhythmViewModel.starterProject())
        // About 700 dp between the top bar and the tabs of a 890 dp phone (status bar, top bar, tabs).
        val first = compose.onNodeWithTag("step_0").getBoundsInRoot()
        assertTrue("first step row ends at ${first.bottom}", first.bottom <= 700.dp)
        compose.onNodeWithTag("pads_hint").assertIsDisplayed()
    }

    @Test
    fun playChoosesTheSectionOrTheWholeSong() {
        val asked = mutableListOf<Boolean>()
        var song by mutableStateOf<Boolean?>(null)
        compose.setContent {
            PocketStudioTheme {
                RhythmScreen(Project(), TransportUi(), LiveUi(), false, false, RhythmActions(setListenSong = { asked += it; song = it }), listenSong = song)
            }
        }
        // No song: ▶ plays the section, nothing to choose.
        compose.onNodeWithTag("rhythm_listen").assertDoesNotExist()
        song = true
        compose.onNodeWithText("tout le morceau").assertExists()
        compose.onNodeWithTag("rhythm_listen").performClick()
        compose.onNodeWithText("cette section").assertExists()
        compose.onNodeWithTag("rhythm_listen").performClick()
        assertEquals(listOf(false, true), asked)
    }

    @Test
    fun holdingTempoPlusRepeats() {
        var steps = 0
        screen(Project(), RhythmActions(nudgeTempo = { steps += it }))
        compose.mainClock.autoAdvance = false
        val plus = compose.onNodeWithContentDescription("Tempo plus")
        plus.performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(1500)
        plus.performTouchInput { up() }
        compose.mainClock.advanceTimeBy(100)
        assertTrue("held 1.5 s: $steps steps", steps >= 5)
        // A short touch is one step.
        val before = steps
        plus.performClick()
        compose.mainClock.advanceTimeBy(100)
        assertEquals(before + 1, steps)
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
