package com.pocketstudio.app.human

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.projects.ProjectsViewModel
import com.pocketstudio.app.projects.projectCatalog
import com.pocketstudio.model.Project
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File

/**
 * Tour 7, banc B6 (seat B/d #23, #30, #31: « Mes projets » said « Chargement de la liste… » three screens long): proved
 * without the bench whether the app reads the list. The app's own model, a saved project, a new one made (as a course
 * starts), then the list asked again (as the dialog opens): the wait ends and the projects are listed, within seconds (15 s each: room for a busy CI).
 */
@RunWith(RobolectricTestRunner::class)
class BenchProjectsListTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()

    private fun waitFor(what: String, ms: Long, condition: () -> Boolean): Long {
        val start = System.currentTimeMillis()
        while (!condition()) {
            check(System.currentTimeMillis() - start < ms) { "waited more than $ms ms for $what" }
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        return System.currentTimeMillis() - start
    }

    @Test
    fun theProjectsListIsReadAndShownWithoutTheBench() {
        File(app.filesDir, "projects").deleteRecursively()
        projectCatalog(app).create(Project(name = "Mon projet 1"))
        val vm = ProjectsViewModel(app)
        vm.refresh() // as AppRoot does when « Mes projets » opens (the model's own start reads nothing)
        waitFor("the first reading", 15_000) { !vm.state.value.loading }
        var made: String? = null
        vm.create(Project(name = "")) { made = it }
        waitFor("the course's project", 15_000) { made != null }
        vm.refresh()
        waitFor("the list asked again", 15_000) { !vm.state.value.loading && !vm.state.value.busy }
        assertFalse(vm.state.value.loading)
        assertEquals(2, vm.state.value.projects.size)
        // A model made afresh (the screens rebuilt once the course's project opened): its first reading ends too.
        val again = ProjectsViewModel(app)
        again.refresh()
        waitFor("a new model's list", 15_000) { !again.state.value.loading }
        assertEquals(2, again.state.value.projects.size)
    }
}
