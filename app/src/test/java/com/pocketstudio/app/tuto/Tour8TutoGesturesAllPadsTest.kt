package com.pocketstudio.app.tuto

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.filter
import com.pocketstudio.app.rhythm.AllPadsGrid
import com.pocketstudio.app.rhythm.RhythmActions
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
 * Tour 8, lot T0b (seat c in ko, ch.12: « Tous les pads » open, no red cross on the hits to take away, no dotted
 * cell): the grid of all the pads draws the tutorial's marks as the pad's own grid does, a cross on each hit the
 * step takes away, a dotted outline on each empty cell it asks for, and none elsewhere.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Tour8TutoGesturesAllPadsTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun end() {
        TutoHints.cells = null
        TutoHints.erase = null
    }

    private fun count(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().size

    private fun marks(cell: String, tag: String) =
        compose.onNodeWithTag(cell, useUnmergedTree = true).onChildren().filter(hasTestTag(tag)).fetchSemanticsNodes().size

    @Test
    fun theCrossesAndTheDottedCellsAreDrawnOnAllThePads() {
        val lanes = List(DrumPattern().lanes.size) { DrumLane() }.toMutableList()
        lanes[1] = DrumLane(hits = mapOf(4 to Hit(1f), 12 to Hit(1f)))
        val p = Project(drums = DrumPattern(steps = 16, lanes = lanes))
        TutoHints.erase = mapOf(1 to setOf(12))
        TutoHints.cells = 2 to setOf(0, 8)
        compose.setContent { PocketStudioTheme { AllPadsGrid(p, 0, -1, RhythmActions()) } }
        compose.waitForIdle()
        assertEquals(1, count("tuto_erase"))
        assertEquals(1, marks("cell_1_12", "tuto_erase"))
        assertEquals(0, marks("cell_1_4", "tuto_erase"))
        assertEquals(2, count("tuto_hint"))
        assertEquals(1, marks("cell_2_0", "tuto_hint"))
        assertEquals(1, marks("cell_2_8", "tuto_hint"))
    }

    // Tour 8, control seat d (a-V1-anneau, ch.3 in « Tous les pads »): only the dotted cells, no « Touche ici » ring
    // round the one to touch first. The ring goes round the first dotted cell of the bar shown not yet filled, alone.
    @Test
    fun theFirstDottedCellIsRingedOnAllThePads() {
        val lanes = List(DrumPattern().lanes.size) { DrumLane() }.toMutableList()
        lanes[2] = DrumLane(hits = mapOf(4 to Hit(1f)))
        val p = Project(drums = DrumPattern(steps = 32, lanes = lanes))
        TutoHints.cells = 2 to setOf(4, 12, 20, 28)
        val bar = androidx.compose.runtime.mutableStateOf(0)
        compose.setContent { PocketStudioTheme { AllPadsGrid(p, bar.value, -1, RhythmActions()) } }
        compose.waitForIdle()
        assertEquals(1, count("tuto_hint_ring"))
        assertEquals(1, marks("cell_2_12", "tuto_hint_ring"))
        bar.value = 1
        compose.waitForIdle()
        assertEquals(1, count("tuto_hint_ring"))
        assertEquals(1, marks("cell_2_20", "tuto_hint_ring"))
    }

    // The same ring on the pad's own grid (the view the tutorial opens first), on the same cell.
    @Test
    fun theFirstDottedCellIsRingedOnThePadsOwnGrid() {
        val lanes = List(DrumPattern().lanes.size) { DrumLane() }.toMutableList()
        lanes[2] = DrumLane(hits = mapOf(4 to Hit(1f)))
        val p = Project(drums = DrumPattern(steps = 32, lanes = lanes))
        TutoHints.cells = 2 to setOf(4, 12, 20, 28)
        compose.setContent {
            PocketStudioTheme {
                com.pocketstudio.app.rhythm.RhythmScreen(
                    p, com.pocketstudio.app.rhythm.TransportUi(selectedPad = 2), { com.pocketstudio.app.rhythm.LiveUi() }, canUndo = false, canRedo = false,
                    actions = RhythmActions(),
                )
            }
        }
        compose.waitForIdle()
        assertEquals(1, count("tuto_hint_ring"))
        assertEquals(1, marks("step_12", "tuto_hint_ring"))
    }
}
