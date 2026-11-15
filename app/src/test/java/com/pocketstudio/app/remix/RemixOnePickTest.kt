package com.pocketstudio.app.remix

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 7, lot 2 D3 (pt-BR at 200 %): « Escolher uma música » was shown twice on the Remix page,
 * on the card and on the thumb row. One button per action: it is shown once, wherever it sits.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class RemixOnePickTest {
    @get:Rule
    val compose = createComposeRule()

    private fun picksShown() = compose.onAllNodesWithText("Choisir une chanson", substring = true).fetchSemanticsNodes().size

    @Test
    fun theSongIsChosenFromOneButton() {
        compose.setContent { PocketStudioTheme { RemixScreen(RemixUi(true, SeparationState.Idle, emptyList()), RemixActions()) } }
        assertEquals("« Choisir une chanson » buttons", 1, picksShown())
        compose.onNodeWithTag("remix_pick").assertIsDisplayed()
        compose.onNodeWithTag("remix_pick_thumb").assertIsDisplayed()
    }

    @Test
    fun withoutTheModelTheCardPicksAndTheThumbDownloads() {
        compose.setContent { PocketStudioTheme { RemixScreen(RemixUi(false, SeparationState.Idle, emptyList()), RemixActions()) } }
        assertEquals("« Choisir une chanson » buttons", 1, picksShown())
        compose.onNodeWithTag("remix_download_thumb").assertIsDisplayed()
    }
}
