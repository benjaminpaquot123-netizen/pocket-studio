package com.pocketstudio.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.rhythm.RhythmViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class ProjectRecoveryTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val projects = File(app.filesDir, "projects")

    @Test
    fun unreadableProjectIsSetAsideNotOverwritten() {
        val dir = File(projects, RhythmViewModel.PROJECT_ID).apply { mkdirs() }
        File(dir, "project.json").writeText("{ pas du json")
        File(dir, "project.json.bak").writeText("{ pas du json non plus")

        val vm = RhythmViewModel(app)

        assertNotNull("the user is told", vm.notice.value)
        val aside = projects.listFiles()!!.single { it.name.startsWith("${RhythmViewModel.PROJECT_ID}-illisible-") }
        assertEquals("{ pas du json", File(aside, "project.json").readText())
        assertEquals("{ pas du json non plus", File(aside, "project.json.bak").readText())
    }

    @Test
    fun missingProjectStartsQuietly() {
        projects.deleteRecursively()
        val vm = RhythmViewModel(app)
        assertNull(vm.notice.value)
        assertEquals("Mon premier rythme", vm.project.value.name)
    }
}
