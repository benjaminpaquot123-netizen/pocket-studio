package com.pocketstudio.app.rhythm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pocketstudio.app.tuto.TutoHints
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.Project
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 9, lot L4. S56 (grave): during a tutorial step, « Remplir » places the step's dotted cells on this pad (those
 * on its grid), never wipes the hits already there nor adds hits the step did not ask for. F055: a fill on a muted
 * pad says why nothing is heard.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w392dp-h852dp")
class Tour9L4FillTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun tidy() {
        TutoHints.active = false
        TutoHints.step = null
        TutoHints.cells = null
    }

    private fun screen(start: Project): () -> Project {
        var project by mutableStateOf(start)
        compose.setContent {
            PocketStudioTheme {
                RhythmScreen(
                    project = project, transport = TransportUi(selectedPad = 2), live = { LiveUi() }, canUndo = true, canRedo = false,
                    actions = RhythmActions(
                        paintStep = { s, on -> project = project.copy(drums = project.drums.setHit(2, s, on)) },
                        fill = { every -> project = project.copy(drums = project.drums.fill(2, every, 0.8f)) },
                    ),
                )
            }
        }
        return { project }
    }

    @Test
    fun fillDuringATutorialStepPlacesOnlyItsDottedCells() {
        TutoHints.active = true
        TutoHints.step = "tour9-l4-fill"
        TutoHints.cells = 2 to setOf(0, 2, 4, 6, 13)
        val project = screen(Project(drums = DrumPattern().toggle(2, 9)))
        compose.onNodeWithTag("fill_2").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals("the fill broke the step", listOf(0, 2, 4, 6, 9), project().drums.lanes[2].hits.keys.sorted())
        compose.onNodeWithTag("fill_1").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals("« tout » did not place the step's cells only", listOf(0, 2, 4, 6, 9, 13), project().drums.lanes[2].hits.keys.sorted())
    }

    @Test
    fun fillOnAMutedPadSaysWhyNothingIsHeard() {
        val start = Project()
        val channels = start.mixer.channels.toMutableList()
        channels[Mixer.padChannel(2)] = channels[Mixer.padChannel(2)].copy(mute = true)
        val project = screen(start.copy(mixer = start.mixer.copy(channels = channels)))
        compose.onNodeWithTag("fill_1").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(16, project().drums.lanes[2].hits.size)
        compose.onNodeWithText(com.pocketstudio.model.i18n.tr("%s coupé", padName(project(), 2))).assertIsDisplayed()
    }
}
