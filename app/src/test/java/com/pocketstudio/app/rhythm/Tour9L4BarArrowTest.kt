package com.pocketstudio.app.rhythm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.Project
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 9, lot L4, F053 (grave): touching ▶ to go to bar 2 only changes the bar; no hit moves (testers saw every
 * hi-hat shifted by one cell: the ▶ they touched was the pad's « rotate »).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w392dp-h852dp")
class Tour9L4BarArrowTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun everyRightArrowOnTheGridLeavesTheHitsWhereTheyAre() {
        var drums = DrumPattern().withSteps(32)
        (0 until 32 step 2).forEach { drums = drums.toggle(2, it) }
        var project by mutableStateOf(Project(tempoBpm = 124.0, drums = drums))
        val before = project.drums.lanes[2].hits
        compose.setContent {
            PocketStudioTheme {
                RhythmScreen(
                    project = project, transport = TransportUi(selectedPad = 2), live = { LiveUi() }, canUndo = true, canRedo = false,
                    actions = RhythmActions(rotate = { by -> project = project.copy(drums = project.drums.rotate(2, by)) }),
                )
            }
        }
        val arrows = compose.onAllNodesWithText("▶").fetchSemanticsNodes()
        assertTrue("no ▶ to go to the next bar", arrows.isNotEmpty())
        arrows.indices.forEach { i ->
            compose.onAllNodesWithText("▶")[i].performScrollTo().performClick()
            compose.waitForIdle()
            assertEquals("a ▶ moved the hits", before, project.drums.lanes[2].hits)
        }
        compose.onNodeWithTag("drum_page_1").assertIsSelected()
    }
}
