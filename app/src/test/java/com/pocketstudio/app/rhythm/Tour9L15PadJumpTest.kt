package com.pocketstudio.app.rhythm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pocketstudio.app.tuto.TutoHints
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
 * Tour 9, lot L15, F054 (seat t9-57): « Le repère vers le pad demandé m'y emmène d'un toucher. » The tutorial's step
 * asks for cells (or hits to take away) on another pad than the one shown: the line over the grid has a button with
 * that pad's name, and one touch shows its cells. On the asked pad, no such button.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h1800dp")
class Tour9L15PadJumpTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun tearDown() = TutoHints.reset()

    private var selected by mutableIntStateOf(0)

    private fun show(project: Project) {
        TutoHints.active = true
        compose.setContent {
            PocketStudioTheme {
                RhythmScreen(project = project, transport = TransportUi(selectedPad = selected), live = { LiveUi() }, canUndo = false, canRedo = false,
                    actions = RhythmActions(selectPad = { selected = it }))
            }
        }
        compose.waitForIdle()
    }

    private fun jumpsTo(project: Project, lane: Int) {
        compose.onNodeWithTag("tuto_pad_go").performScrollTo().assertTextContains(padName(project, lane), substring = true)
        compose.onNodeWithTag("tuto_pad_go").performClick()
        compose.waitForIdle()
        assertEquals("one touch on the line's pad button shows the asked pad", lane, selected)
        assertEquals("on the asked pad, the line no longer points elsewhere", 0, compose.onAllNodesWithTag("tuto_pad_go", useUnmergedTree = true).fetchSemanticsNodes().size)
    }

    @Test
    fun f054CellsAskedOnAnotherPad() {
        TutoHints.cells = 4 to setOf(4, 12)
        val project = Project()
        show(project)
        jumpsTo(project, 4)
    }

    @Test
    fun f054HitsToEraseOnAnotherPad() {
        val start = Project()
        val project = start.copy(drums = start.drums.setHit(2, 0, true).setHit(2, 8, true))
        TutoHints.erase = mapOf(2 to setOf(8))
        show(project)
        jumpsTo(project, 2)
    }

    @Test
    fun f054NoJumpWhenTheAskedPadsCellsAreDone() {
        val start = Project()
        val project = start.copy(drums = start.drums.setHit(4, 4, true))
        TutoHints.cells = 4 to setOf(4)
        show(project)
        assertEquals("nothing left on the asked pad: no button", 0, compose.onAllNodesWithTag("tuto_pad_go", useUnmergedTree = true).fetchSemanticsNodes().size)
    }
}
