package com.pocketstudio.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 5, moyens M1: the Mode simple's bar (no ↶, « ⋮ », the chip « Simple ») cut the project's name to « M… » on a
 * 411 dp phone at Taille 130 % (a 316 dp bar). Drawn with real text (native graphics), on 360 and 411 dp phones, at
 * Taille 130 % and at the phone's font at 130 % and 200 %: the name keeps a few words, on its own line when the row has
 * no room for them, and the buttons stay.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w480dp-h900dp-xxhdpi")
class TopBarNameRoomTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theModeSimpleBarKeepsAFewWordsOfTheName() {
        // (bar width, phone's font scale): Taille 130 % divides the dp of a 360 / 411 dp phone by 1.3.
        var case by mutableStateOf(316 to 1f)
        compose.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, case.second)) {
                PocketStudioTheme {
                    Box(Modifier.width(case.first.dp)) {
                        TopBar(
                            TopBarUi("Mon premier rythme", playing = false, canUndo = true, canRedo = false, saved = true, mode = true, undo = false, redo = false),
                            TopBarActions(more = {}),
                        )
                    }
                }
            }
        }
        for ((width, fontScale) in listOf(316 to 1f, 277 to 1f, 360 to 1f, 411 to 1f, 360 to 1.3f, 411 to 1.3f, 360 to 2f, 411 to 2f)) {
            case = width to fontScale
            compose.waitForIdle()
            val name = compose.onNodeWithText("Mon premier rythme", useUnmergedTree = true).getBoundsInRoot()
            val shown = name.right - name.left
            // « Mon prem… » at least: 56 dp of text at the usual size, as much more as the text is larger.
            assertTrue("$width dp at ${fontScale}x: the name gets $shown", shown >= 56.dp * fontScale)
            for (tag in listOf("top_projects", "top_record", "top_play", "top_more")) compose.onNodeWithTag(tag).assertIsDisplayed()
            val more = compose.onNodeWithTag("top_more").getBoundsInRoot()
            assertTrue("$width dp at ${fontScale}x: « ⋮ » ends at ${more.right}", more.right <= width.dp + 0.5.dp)
        }
    }
}
