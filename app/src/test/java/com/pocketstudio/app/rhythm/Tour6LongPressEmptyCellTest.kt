package com.pocketstudio.app.rhythm

import android.app.Application
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Tour 6, seats Bb-7 and Bb-8 (H3, decided): a long press on an EMPTY cell puts the hit AND opens its menu at once
 * (« Force » with « Doux », « Roulement », « Triolet »): « Doux » is one long press away, as the tutorial says.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w392dp-h852dp")
class Tour6LongPressEmptyCellTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun aLongPressOnAnEmptyCellPutsTheHitAndOpensItsMenu() {
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        vm.selectPad(4)
        compose.setContent {
            PocketStudioTheme {
                val p by vm.project.collectAsState()
                val t by vm.transport.collectAsState()
                RhythmScreen(
                    p, t, { LiveUi() }, canUndo = true, canRedo = false,
                    actions = RhythmActions(
                        toggleStep = vm::toggleStep, cycleVelocity = vm::cycleVelocity, setHitVelocity = vm::setHitVelocity,
                        selectPad = vm::selectPad, setRoll = vm::setRoll,
                    ),
                )
            }
        }
        assertTrue("cell 15 empty first", vm.project.value.drums.lanes[4].hits[14] == null)
        compose.onNodeWithTag("step_14").performScrollTo().performTouchInput { longClick() }
        compose.waitForIdle()
        assertTrue("no hit put by the long press", vm.project.value.drums.lanes[4].hits[14] != null)
        assertTrue("the hit's menu not open after one long press", compose.onAllNodesWithTag("roll_menu").fetchSemanticsNodes().isNotEmpty())
    }
}
