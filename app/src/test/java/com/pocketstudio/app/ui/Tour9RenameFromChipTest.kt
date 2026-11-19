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
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.testTag
import com.pocketstudio.model.Scene
import org.junit.Assert.assertEquals
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

    private var asked: Pair<Int, String>? = null

    /** Structure's naming dialog without its window (a dialog holding a text field never goes idle under Robolectric). */
    @Composable
    private fun NamingFields(index: Int, shown: String, done: (String) -> Unit) {
        LaunchedEffect(index) { asked = index to shown }
        var name by remember(index) { mutableStateOf(com.pocketstudio.app.session.shownSection(shown)) }
        Column {
            Text("Nommer la partie " + Scene.NAMES[index])
            com.pocketstudio.app.session.RenameSceneFields(name) { name = it }
            TextButton(onClick = { done(com.pocketstudio.app.session.sectionKeyOf(name)) }, modifier = Modifier.testTag("scene_name_ok")) { Text("Valider") }
        }
    }

    @Test
    fun theSceneMenuOffersRenameAndOpensTheNamingDialog() {
        var renamed: Pair<Int, String>? = null
        compose.setContent {
            PocketStudioTheme { Box(Modifier.width(411.dp)) { TopBar(TopBarUi(projectName = "Mon morceau", playing = false, canUndo = true, canRedo = true, scene = scenes), TopBarActions(renameScene = { i, l -> renamed = i to l }), nameDialog = { i, shown, done, _ -> NamingFields(i, shown, done) }) } }
        }
        compose.onNodeWithTag("top_scene").performClick()
        compose.onNodeWithTag("scene_rename").assertIsDisplayed()
        compose.onNodeWithText("Renommer", substring = true, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("scene_rename").performClick()
        compose.onNodeWithTag("scene_menu").assertDoesNotExist()
        // The same naming as Structure's « Renommer »: its title, the part's current name and its sections in one touch.
        assertEquals(1 to "Refrain", asked)
        compose.onNodeWithText("Nommer la partie B").assertIsDisplayed()
        compose.onNodeWithTag("section_Pont").assertIsDisplayed()
        // The name given goes to the project's rename (AppRoot wires renameScene to Structure's).
        compose.onNodeWithTag("section_Pont").performClick()
        compose.onNodeWithTag("scene_name_ok").performClick()
        compose.onNodeWithText("Nommer la partie B").assertDoesNotExist()
        assertEquals(1 to "Pont", renamed)
    }

    /** Relecture G1: the part's saved name goes to the dialog, not its title in the interface's language. */
    @Test
    fun inItalianAnUnchangedNameIsKeptAsSaved() {
        com.pocketstudio.model.i18n.I18n.lang = com.pocketstudio.model.i18n.Lang.IT
        try {
            var renamed: Pair<Int, String>? = null
            val italian = SceneChipUi(current = 1, titles = listOf("A · Intro", "B · " + Scene.shownLabel("Refrain 2"), "C · " + Scene.shownLabel("Drop +")))
            compose.setContent {
                PocketStudioTheme { Box(Modifier.width(411.dp)) { TopBar(TopBarUi(projectName = "Mon morceau", playing = false, canUndo = true, canRedo = true, scene = italian), TopBarActions(renameScene = { i, l -> renamed = i to l }), nameDialog = { i, shown, done, _ -> NamingFields(i, shown, done) }) } }
            }
            compose.onNodeWithTag("top_scene").performClick()
            compose.onNodeWithTag("scene_rename").performClick()
            compose.onNodeWithTag("scene_name_ok").performClick()
            assertEquals(1 to "Refrain 2", renamed)
        } finally {
            com.pocketstudio.model.i18n.I18n.lang = com.pocketstudio.model.i18n.Lang.FR
        }
    }
}
