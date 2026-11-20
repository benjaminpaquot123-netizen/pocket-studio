package com.pocketstudio.app.rhythm

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.LayoutDirection
import com.pocketstudio.model.Project
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 9, lot L4, F051: in Arabic (right to left) the drum grid still reads left to right, like every sequencer; it
 * says so: « 1 → » over its first cell, so cell 1 is found at once.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class Tour9L4RtlGridTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theArabicGridShowsCellOneAndItsDirection() {
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                PocketStudioTheme {
                    RhythmScreen(project = Project(), transport = TransportUi(selectedPad = 0), live = { LiveUi() }, canUndo = false, canRedo = false, actions = RhythmActions())
                }
            }
        }
        compose.onNodeWithTag("grid_reading_dir", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        val mark = compose.onNodeWithTag("grid_reading_dir", useUnmergedTree = true).getBoundsInRoot()
        val first = compose.onNodeWithTag("step_0").getBoundsInRoot()
        val second = compose.onNodeWithTag("step_1").getBoundsInRoot()
        assertTrue("cell 1 is not the leftmost: $first / $second", first.left < second.left)
        assertTrue("« 1 → » is not over cell 1: $mark / $first", mark.left < first.right && mark.bottom <= first.top + 1.0.let { androidx.compose.ui.unit.Dp(it.toFloat()) })
    }
}
