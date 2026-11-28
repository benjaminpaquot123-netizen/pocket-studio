package com.pocketstudio.app.rhythm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pocketstudio.model.Project
import com.pocketstudio.model.tuto.TutoChecks
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 9, lot L4, F057: a finger reaches the swing a step asks for within 1 %: « − » and « + » beside the slider move
 * it by one shown point, so 25 % (the house step) is reached exactly from 0 and from 27 %.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class Tour9L4SwingTest {
    @get:Rule
    val compose = createComposeRule()

    private fun screen(start: Project): () -> Project {
        var project by mutableStateOf(start)
        compose.setContent {
            PocketStudioTheme {
                RhythmScreen(
                    project = project, transport = TransportUi(selectedPad = 0), live = { LiveUi() }, canUndo = false, canRedo = false,
                    actions = RhythmActions(setSwing = { project = project.copy(swing = it) }),
                )
            }
        }
        return { project }
    }

    @Test
    fun plusAndMinusReachASwingToTheShownPoint() {
        val project = screen(Project(swing = 0.0))
        repeat(25) { compose.onNodeWithTag("swing_plus").performScrollTo().performClick() }
        compose.waitForIdle()
        assertEquals(25, TutoChecks.swingShown(project().swing))
        repeat(2) { compose.onNodeWithTag("swing_plus").performClick() }
        repeat(2) { compose.onNodeWithTag("swing_minus").performClick() }
        compose.waitForIdle()
        assertEquals(25, TutoChecks.swingShown(project().swing))
        assertEquals(0.25 * Project.MAX_SWING, project().swing, 1e-9)
    }
}
