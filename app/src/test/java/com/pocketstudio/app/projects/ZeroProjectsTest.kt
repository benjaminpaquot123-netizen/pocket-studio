package com.pocketstudio.app.projects

import android.app.Application
import android.os.Looper
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectInfo
import com.pocketstudio.model.TrashedProject
import com.pocketstudio.model.asNewProject
import com.pocketstudio.ui.PocketStudioTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Lot 1 (APK 181, « Mes projets »): every project can go, down to none (the open one included:
 * « Ce projet est vide et neuf : il reste là » kept the last one for good), « Vider la corbeille »
 * beside its heading, no stray « ▌ » after the open project's name.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class ZeroProjectsTest {
    @get:Rule
    val compose = createComposeRule()

    private val app = ApplicationProvider.getApplicationContext<Application>()

    private fun waitFor(what: String, condition: () -> Boolean) {
        val until = System.currentTimeMillis() + 10_000
        while (!condition()) {
            check(System.currentTimeMillis() < until) { "waited too long for $what" }
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
    }

    @Test
    fun theLastProjectDeletedLeavesNone() {
        File(app.filesDir, "projects").deleteRecursively()
        File(app.filesDir, "projects-trash").deleteRecursively()
        val catalog = projectCatalog(app)
        val touched = catalog.create(Project(name = "Mon morceau", tempoBpm = 174.0))
        // The open project deleted: the app closes it (no project open), then trashes it.
        CurrentProject.set(app, CurrentProject.NONE)
        CurrentProject.deleteLater(app, touched)
        val vm = ProjectsViewModel(app)
        waitFor("the deletion") { catalog.list().isEmpty() && vm.state.value.deleted != null }
        assertEquals(1, catalog.trashed().size)
        // A blank one nobody touched goes for good, never to the trash.
        val blank = catalog.create(Project(name = "").asNewProject())
        CurrentProject.deleteLater(app, blank)
        ProjectsViewModel(app)
        waitFor("the blank one gone") { catalog.list().isEmpty() }
        assertEquals(1, catalog.trashed().size)
        CurrentProject.set(app, RhythmViewModel.PROJECT_ID)
    }

    @Test
    fun withNoProjectOpenNothingIsEverWritten() {
        File(app.filesDir, "projects").deleteRecursively()
        CurrentProject.set(app, CurrentProject.NONE)
        try {
            val store = ViewModelStore()
            // Made with this test's application (the default factory keeps the first test's one).
            val factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = modelClass.getConstructor(Application::class.java).newInstance(app)
            }
            val vm = ViewModelProvider(store, factory)[RhythmViewModel::class.java]
            assertEquals(CurrentProject.NONE, vm.projectId)
            vm.setTempo(140.0)
            vm.toggleHit(0, 3)
            assertTrue(runBlocking { vm.flushNow() })
            shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(5))
            assertFalse("no folder for the stand-in", File(File(app.filesDir, "projects"), CurrentProject.NONE).exists())
            assertTrue(projectCatalog(app).list().isEmpty())
            store.clear()
        } finally {
            CurrentProject.set(app, RhythmViewModel.PROJECT_ID)
        }
    }

    @Test
    fun anEmptyListOffersANewSongAndAnImport() {
        var imported = 0
        compose.setContent {
            PocketStudioTheme {
                ProjectsScreen(ProjectsUi(loading = false), CurrentProject.NONE, emptyList(), ProjectsActions(importZip = { imported++ }))
            }
        }
        compose.onNodeWithTag("projects_empty").assertExists()
        compose.onNodeWithTag("projects_empty_import").performClick()
        assertEquals(1, imported)
        // « Nouveau morceau »: the page of styles, demos and the empty project.
        compose.onNodeWithTag("projects_empty_new").performClick()
        compose.onNodeWithTag("new_project").assertExists()
    }

    @Test
    fun theTrashEmptiesFromItsHeadingAfterAConfirmation() {
        val now = 1_800_000_000_000L
        val trashed = listOf(TrashedProject("p-1@${now - 1000}", "Vieux", now - 1000), TrashedProject("p-2@${now - 2000}", "Brouillon", now - 2000))
        var emptied = 0
        compose.setContent {
            PocketStudioTheme {
                ProjectsScreen(ProjectsUi(loading = false, trash = trashed), CurrentProject.NONE, emptyList(), ProjectsActions(emptyTrash = { emptied++ }), now)
            }
        }
        // Beside « Corbeille (2) », the trash still folded.
        compose.onNodeWithTag("restore_${trashed[0].entry}").assertDoesNotExist()
        compose.onNodeWithTag("project_empty_trash").performClick()
        assertEquals("asked first", 0, emptied)
        compose.onNodeWithTag("project_delete_forever_ok").performClick()
        assertEquals(1, emptied)
    }

    @Test
    fun theOpenProjectsBadgeIsPlainText() {
        val open = ProjectInfo("p-open", "Mon projet 2", 1_800_000_000_000L, 120.0, 1, 1, false)
        compose.setContent {
            PocketStudioTheme {
                ProjectsScreen(ProjectsUi(loading = false, projects = listOf(open)), open.id, emptyList(), ProjectsActions())
            }
        }
        // The badge alone (a section heading drew its accent bar right after the name).
        compose.onNodeWithTag("project_open_badge", useUnmergedTree = true).assertTextEquals("OUVERT")
    }
}
