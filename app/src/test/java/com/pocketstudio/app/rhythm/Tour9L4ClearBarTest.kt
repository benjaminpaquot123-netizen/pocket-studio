package com.pocketstudio.app.rhythm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.Project
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tour 9, lot L4, S76 (grave): I can empty one bar in a single gesture (the other bar keeps its hits; one undo step). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w392dp-h852dp")
class Tour9L4ClearBarTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun oneTouchEmptiesTheBarShownOnly() {
        var drums = DrumPattern().withSteps(32)
        (0 until 32 step 2).forEach { drums = drums.toggle(2, it) }
        var project by mutableStateOf(Project(tempoBpm = 124.0, drums = drums))
        var strokes = 0
        compose.setContent {
            PocketStudioTheme {
                RhythmScreen(
                    project = project, transport = TransportUi(selectedPad = 2), live = { LiveUi() }, canUndo = true, canRedo = false,
                    actions = RhythmActions(
                        paintStep = { s, on -> project = project.copy(drums = project.drums.setHit(2, s, on)) },
                        paintEnd = { strokes++ },
                    ),
                )
            }
        }
        compose.onNodeWithTag("drum_page_1").performScrollTo().performClick()
        compose.onNodeWithTag("clear_bar").performScrollTo().performClick()
        compose.waitForIdle()
        val left = project.drums.lanes[2].hits.keys.sorted()
        assertEquals("bar 2 not emptied, or bar 1 touched", (0 until 16 step 2).toList(), left)
        assertEquals("not one undo step", 1, strokes)
    }
}
