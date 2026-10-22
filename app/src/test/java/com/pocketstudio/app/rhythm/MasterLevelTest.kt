package com.pocketstudio.app.rhythm

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.projects.ProjectsViewModel
import com.pocketstudio.app.projects.projectCatalog
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.Project
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File

/** The master's level: new projects start with headroom, the Mixer's hint is one undo step. */
@RunWith(RobolectricTestRunner::class)
class MasterLevelTest {
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
    fun newProjectsStartSixDecibelsDownAndOldOnesKeepTheirLevel() {
        val catalog = projectCatalog(app)
        val old = catalog.create(Project(name = "Ancien"))  // saved before: 0 dB
        val vm = ProjectsViewModel(app)
        var blank: String? = null
        var wizard: String? = null
        vm.create(Project(name = "")) { blank = it }
        vm.create(Project(name = "Assistant"), emptyMap()) { wizard = it }
        waitFor("both projects") { blank != null && wizard != null }
        assertEquals(Mixer.NEW_PROJECT_DB, catalog.store.load(blank!!)!!.mixer.masterDb)
        assertEquals(Mixer.NEW_PROJECT_DB, catalog.store.load(wizard!!)!!.mixer.masterDb)
        assertEquals(0f, catalog.store.load(old)!!.mixer.masterDb)
    }

    @Test
    fun lowerBySixDecibelsIsOneUndoStep() {
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        val before = vm.project.value.mixer.masterDb
        vm.toggleHit(0, 0)
        val hits = vm.project.value.drums
        vm.setMasterDb(before - Mixer.HINT_STEP_DB)
        assertEquals(before - 6f, vm.project.value.mixer.masterDb)
        vm.undo()
        assertEquals(before, vm.project.value.mixer.masterDb)
        assertEquals("only the master went back", hits, vm.project.value.drums)
    }
}
