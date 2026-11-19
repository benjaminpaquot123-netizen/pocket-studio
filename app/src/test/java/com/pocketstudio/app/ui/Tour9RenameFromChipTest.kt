package com.pocketstudio.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 9, D10 (seats 27, 37, 08, 23, 48, 60, 63): « impossible de renommer la partie depuis le menu « A ▾ » de la barre du
 * haut. » Acceptance: « A ▾ » offers « Renommer », which opens the same naming dialog as Structure.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class Tour9RenameFromChipTest {
    @get:Rule val compose = createComposeRule()

    private val scenes = SceneChipUi(current = 1, titles = listOf("A · Intro", "B · Refrain", "C"), empty = setOf(2))

    @Test
    fun theSceneMenuOffersRenameAndOpensTheNamingDialog() {
        compose.setContent {
            PocketStudioTheme { Box(Modifier.width(411.dp)) { TopBar(TopBarUi(projectName = "Mon morceau", playing = false, canUndo = true, canRedo = true, scene = scenes), TopBarActions()) } }
        }
        compose.onNodeWithTag("top_scene").performClick()
        compose.onNodeWithTag("scene_rename").assertIsDisplayed()
        compose.onNodeWithText("Renommer", substring = true, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("scene_rename").performClick()
        compose.onNodeWithTag("scene_menu").assertDoesNotExist()
        // The same dialog as Structure's « Renommer »: its title and its sections in one touch.
        compose.onNodeWithText("Nommer la partie B").assertIsDisplayed()
        compose.onNodeWithTag("section_Pont").assertIsDisplayed()
    }
}
