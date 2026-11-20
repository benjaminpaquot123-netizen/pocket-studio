package com.pocketstudio.app.rhythm

import android.app.Application
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.tuto.TutoHints
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Tour 9, lot L4, F056: a touch on a full cell takes the hit away on the first touch. The « Force » tool, a Pro
 * tool, never stays on where it cannot be seen: in the Mode simple (no tool chips), nor into a new tutorial step
 * (testers: a touch to erase set the hit to 55 % instead). Force stays one long press away (the hit's sheet).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w392dp-h852dp")
class Tour9L4FirstTouchErasesTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    @get:Rule
    val compose = createComposeRule()

    @After
    fun tidy() {
        TutoHints.active = false
        TutoHints.step = null
    }

    private fun screen(simple: () -> Boolean): RhythmViewModel {
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        vm.selectPad(1)
        vm.toggleStep(4)
        compose.setContent {
            PocketStudioTheme {
                val p by vm.project.collectAsState()
                val t by vm.transport.collectAsState()
                RhythmScreen(
                    p, t, { LiveUi() }, canUndo = true, canRedo = false, simple = simple(),
                    actions = RhythmActions(toggleStep = vm::toggleStep, setHitVelocity = vm::setHitVelocity, selectPad = vm::selectPad),
                )
            }
        }
        return vm
    }

    @Test
    fun theSimpleModeErasesOnTheFirstTouchAfterForceWasChosenInPro() {
        var simple by mutableStateOf(false)
        val vm = screen { simple }
        assertTrue("hit 5 not there first", vm.project.value.drums.lanes[1].hits[4] != null)
        compose.onNodeWithTag("velocity_tool").performScrollTo().performClick()
        simple = true
        compose.waitForIdle()
        compose.onNodeWithTag("step_4").performScrollTo().performClick()
        compose.waitForIdle()
        assertNull("the first touch did not erase the hit (Force still on, hidden)", vm.project.value.drums.lanes[1].hits[4])
    }

    @Test
    fun aNewTutorialStepErasesOnTheFirstTouch() {
        val vm = screen { false }
        compose.onNodeWithTag("velocity_tool").performScrollTo().performClick()
        TutoHints.active = true
        TutoHints.step = "tour9-l4-next"
        compose.waitForIdle()
        compose.onNodeWithTag("step_4").performScrollTo().performClick()
        compose.waitForIdle()
        assertNull("the first touch did not erase the hit in a new step", vm.project.value.drums.lanes[1].hits[4])
    }
}
