package com.pocketstudio.app.live

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.LiveState
import com.pocketstudio.model.Project
import com.pocketstudio.model.Scene
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Lot 4c: the Briques grid in style B, « Cases − 8 + », the first loop's invitation, the animations off. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class LiveGridStyleTest {
    @get:Rule
    val compose = createComposeRule()

    private fun twoBricks(rows: Int = 0) = Project(
        scenes = listOf(Scene("A", DrumPattern().toggle(0, 0), label = "Intro"), Scene("B", DrumPattern().toggle(1, 4))),
        scene = 0, liveRows = rows,
    ).sanitized()

    @Test
    fun theCasesStepperSetsHowManyBricksTheGridShows() {
        var project by mutableStateOf(twoBricks())
        val asked = mutableListOf<Int>()
        compose.setContent {
            PocketStudioTheme {
                LiveScreen(LiveScreenUi(project), LiveActions(setRows = { asked += it; project = project.copy(liveRows = it) }))
            }
        }
        // 8 by default: rows A..H, the ones past B new bricks to fill.
        compose.onNodeWithTag("live_cases_count").assertTextEquals("8")
        compose.onNodeWithTag("live_row_7").assertExists()
        compose.onNodeWithTag("live_pad_drums_7").assertExists()
        compose.onNodeWithTag("live_row_8").assertDoesNotExist()
        compose.onNodeWithTag("live_cases_more").performClick()
        compose.onNodeWithTag("live_cases_count").assertTextEquals("12")
        compose.onNodeWithTag("live_row_11").assertExists()
        compose.onNodeWithTag("live_cases_more").performClick()
        compose.onNodeWithTag("live_cases_more").assertIsNotEnabled()
        repeat(4) { compose.onNodeWithTag("live_cases_less").performClick() }
        compose.onNodeWithTag("live_cases_count").assertTextEquals("4")
        compose.onNodeWithTag("live_cases_less").assertIsNotEnabled()
        compose.onNodeWithTag("live_row_4").assertDoesNotExist()
        assertEquals(listOf(12, 16, 12, 8, 6, 4), asked)
    }

    @Test
    fun anEmptyDotOfANewBrickOpensItsMenu() {
        compose.setContent { PocketStudioTheme { LiveScreen(LiveScreenUi(twoBricks()), LiveActions()) } }
        // Brick F (past the song's two): its row scrolled into view, then its drum dot.
        compose.onNodeWithTag("live_row_5").performScrollTo()
        compose.onNodeWithTag("live_pad_drums_5").performClick()
        compose.onNodeWithTag("live_cell_menu").assertIsDisplayed()
    }

    @Test
    fun theFirstLoopAloneInvitesToTouchADot() {
        val one = Project(scenes = emptyList(), drums = DrumPattern().toggle(0, 0)).sanitized()
        compose.setContent { PocketStudioTheme { LiveScreen(LiveScreenUi(one, LiveState.on(0), clock = 3, playing = true, reducedMotion = true), LiveActions()) } }
        compose.onNodeWithTag("live_first_loop").assertIsDisplayed()
    }

    @Test
    fun withTheAnimationsOffNothingMovesOnItsOwn() {
        var reduced by mutableStateOf(true)
        compose.setContent {
            PocketStudioTheme { LiveScreen(LiveScreenUi(twoBricks(), LiveState.on(0), clock = 3, playing = true, reducedMotion = reduced), LiveActions()) }
        }
        compose.waitForIdle()
        assertFalse(liveGridClockRunning)
        // Animations on: the grid's one clock runs (on each frame of the phone), and recomposes no pad.
        reduced = false
        compose.waitForIdle()
        assertTrue(liveGridClockRunning)
        val composed = livePadCompositions
        compose.mainClock.advanceTimeBy(500)
        assertEquals(composed, livePadCompositions)
    }
}
