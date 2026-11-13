package com.pocketstudio.app.projects

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Lot 6, U23 (seat F a #36, Italian): « I miei progetti » showed nothing, neither the list nor « Aucun projet »: the
 * catalogue was still being read (its queue may wait up to 2 × 10 s for a save closing on a phone). While it reads and
 * nothing is listed yet, the screen says it is loading; once read, the list or the empty card, never both.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-port-hdpi", fontScale = 1.3f)
class Lot6UiProjectsLoadingTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun french() { I18n.lang = Lang.FR }

    private fun count(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes(atLeastOneRootRequired = false).size

    @Test
    fun whileTheCatalogueIsReadTheScreenSaysItWaits() {
        I18n.lang = Lang.IT
        val ui = mutableStateOf(ProjectsUi(loading = true))
        compose.setContent { PocketStudioTheme { ProjectsScreen(ui.value, "p-0", emptyList(), ProjectsActions()) } }
        compose.waitForIdle()
        compose.onNodeWithTag("projects_loading").assertIsDisplayed()
        compose.onNodeWithText(tr("Chargement de la liste…"), useUnmergedTree = true).assertIsDisplayed()
        // Read, nothing on the phone: the empty card, the wait gone.
        ui.value = ProjectsUi(loading = false)
        compose.waitForIdle()
        assertEquals(0, count("projects_loading"))
    }

    @Test
    fun aListAlreadyShownCarriesNoWait() {
        I18n.lang = Lang.FR
        val listed = com.pocketstudio.model.ProjectInfo(id = "p-1", name = "Nuit trap", modified = 0L, tempoBpm = 140.0, bars = 16, scenes = 2, hasRemix = false)
        compose.setContent { PocketStudioTheme { ProjectsScreen(ProjectsUi(loading = true, projects = listOf(listed)), "p-1", emptyList(), ProjectsActions()) } }
        compose.waitForIdle()
        assertEquals(0, count("projects_loading"))
    }
}
