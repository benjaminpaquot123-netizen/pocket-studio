package com.pocketstudio.app.rhythm

import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import com.pocketstudio.app.tuto.TutoHints
import com.pocketstudio.model.DrumLane
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.Hit
import com.pocketstudio.model.Project
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 9, lot L4, F140: in « Tous les pads », the tutorial's « Touche ici » ring goes round a cell the person can
 * touch: never a cell a spread roll (a triplet) already covers, as on the pad's own grid.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Tour9L4RingRollTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun end() {
        TutoHints.cells = null
    }

    private fun rings(cell: String) =
        compose.onNodeWithTag(cell, useUnmergedTree = true).onChildren().filter(hasTestTag("tuto_hint_ring")).fetchSemanticsNodes().size

    @Test
    fun theRingSkipsACellARollCovers() {
        val lanes = List(DrumPattern().lanes.size) { DrumLane() }.toMutableList()
        lanes[2] = DrumLane(hits = mapOf(0 to Hit(1f, span = 3)))
        val p = Project(drums = DrumPattern(steps = 16, lanes = lanes))
        TutoHints.cells = 2 to setOf(1, 4)
        compose.setContent { PocketStudioTheme { AllPadsGrid(p, 0, -1, RhythmActions()) } }
        compose.waitForIdle()
        assertEquals("the ring sits on a cell the roll covers", 0, rings("cell_2_1"))
        assertEquals("the ring is not on the first cell left to touch", 1, rings("cell_2_4"))
    }
}
