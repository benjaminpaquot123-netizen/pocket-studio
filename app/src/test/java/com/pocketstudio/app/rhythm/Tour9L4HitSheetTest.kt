package com.pocketstudio.app.rhythm

import android.app.Application
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
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
 * Tour 9, lot L4, D04 (bloquant): the long press on a hit opens its sheet with « Décalage du coup », so the
 * uk_garage step « snare_late » (check offset:snare:late) can be done from where the testers looked.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w392dp-h852dp")
class Tour9L4HitSheetTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theLongPressSheetSetsTheHitLate() {
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        vm.selectPad(1)
        compose.setContent {
            PocketStudioTheme {
                val p by vm.project.collectAsState()
                val t by vm.transport.collectAsState()
                RhythmScreen(
                    p, t, { LiveUi() }, canUndo = true, canRedo = false,
                    actions = RhythmActions(
                        toggleStep = vm::toggleStep, cycleVelocity = vm::cycleVelocity, setHitVelocity = vm::setHitVelocity,
                        selectPad = vm::selectPad, setRoll = vm::setRoll, setHitOffset = vm::setHitOffset,
                    ),
                )
            }
        }
        compose.onNodeWithTag("step_6").performScrollTo().performTouchInput { longClick() }
        compose.waitForIdle()
        assertTrue("the long press opened no sheet", compose.onAllNodesWithTag("roll_menu").fetchSemanticsNodes().isNotEmpty())
        assertTrue("no « Décalage du coup » in the sheet", compose.onAllNodesWithTag("roll_offset").fetchSemanticsNodes().isNotEmpty())
        compose.onNodeWithTag("roll_offset").performSemanticsAction(SemanticsActions.SetProgress) { it(20f) }
        compose.waitForIdle()
        val hit = vm.project.value.drums.lanes[1].hits[6]
        assertTrue("the hit is not late: ${hit?.offset}", hit != null && hit.offset > 0f)
    }
}
