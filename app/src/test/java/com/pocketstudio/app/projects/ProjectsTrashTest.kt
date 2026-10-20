package com.pocketstudio.app.projects

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pocketstudio.model.TrashedProject
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class ProjectsTrashTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun aTrashedProjectCanBeRestoredFromTheList() {
        val now = 1_800_000_000_000L
        val trashed = TrashedProject("p-1@${now - 86_400_000}", "Mon vieux rythme", now - 86_400_000)
        val restored = mutableListOf<TrashedProject>()
        compose.setContent {
            PocketStudioTheme {
                ProjectsScreen(
                    ProjectsUi(loading = false, trash = listOf(trashed)), "p-0", emptyList(),
                    ProjectsActions(restore = { restored += it }), now,
                )
            }
        }
        compose.onNodeWithTag("restore_${trashed.entry}").assertDoesNotExist()  // folded at first
        compose.onNodeWithTag("project_trash").performScrollTo().performClick()
        compose.onNodeWithTag("restore_${trashed.entry}").performScrollTo().performClick()
        assertEquals(listOf(trashed), restored)
    }
}
