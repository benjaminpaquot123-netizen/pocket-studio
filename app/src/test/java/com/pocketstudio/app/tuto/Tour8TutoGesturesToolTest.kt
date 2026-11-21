package com.pocketstudio.app.tuto

import android.app.Application
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.rhythm.LiveUi
import com.pocketstudio.app.rhythm.RhythmActions
import com.pocketstudio.app.rhythm.RhythmScreen
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Tour 8, lot T0b (seat e in de, Pro, ch.12: after the fill made with « Roulements », the next step's crosses were
 * touched with that tool still on, and a touch rolled the hit ×2 instead of taking it away): when a step begins
 * that asks for cells or crosses on the pad shown, the grid's tool goes back to « Coups », so one touch does the
 * one thing the step asks.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w392dp-h852dp")
class Tour8TutoGesturesToolTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    @get:Rule
    val compose = createComposeRule()

    @After
    fun end() {
        TutoHints.step = null
        TutoHints.erase = null
        TutoHints.cells = null
    }

    @Test
    fun aStepAskingForCrossesOpensTheGridOnHits() {
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        val lane = 4
        vm.selectPad(lane)
        vm.toggleStep(14)
        compose.setContent {
            PocketStudioTheme {
                val p by vm.project.collectAsState()
                val t by vm.transport.collectAsState()
                RhythmScreen(
                    p, t, { LiveUi() }, canUndo = true, canRedo = false,
                    actions = RhythmActions(toggleStep = vm::toggleStep, selectPad = vm::selectPad, cycleRoll = vm::cycleRoll),
                )
            }
        }
        // The step before (the fill) was made with « Roulements ».
        compose.onNodeWithTag("roll_tool").performScrollTo().performClick()
        compose.waitForIdle()
        // The next step: take the hit away.
        TutoHints.erase = mapOf(lane to setOf(14))
        TutoHints.step = "gap"
        compose.waitForIdle()
        assertNotNull("cell 15 holds a hit first", vm.project.value.drums.lanes[lane].hits[14])
        compose.onNodeWithTag("step_14").performScrollTo().performClick()
        compose.waitForIdle()
        val hit = vm.project.value.drums.lanes[lane].hits[14]
        assertEquals("the touch rolled the hit instead of taking it away", null, hit?.repeats?.takeIf { it > 1 })
        assertNull("the crossed hit is still there after one touch", hit)
    }
}
