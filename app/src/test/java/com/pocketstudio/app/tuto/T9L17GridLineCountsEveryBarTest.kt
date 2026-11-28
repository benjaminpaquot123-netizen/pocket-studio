package com.pocketstudio.app.tuto

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 9, F046 (t9-64, t9-66): the line over the drum grid said « Encore 3 cases ici », then at 0, 11 more cells
 * showed up in bar 2 with nothing having said so. The rule: from the start, the line says the cells of every bar,
 * the other bars' too, while cells are still left on this page.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w392dp-h852dp")
class T9L17GridLineCountsEveryBarTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun has(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun cellsLeftHereAndInBar2BothSaid() {
        compose.setContent {
            PocketStudioTheme { TutoGridLine(bar = 0, bars = 2, pad = "Charley", here = 3, elsewhere = 1 to 11, go = {}, copy = null) }
        }
        compose.waitForIdle()
        assertTrue("the 3 cells here said", has("tuto_grid_status"))
        assertTrue("bar 2's 11 cells said from the start", has("tuto_bar_go"))
    }
}
