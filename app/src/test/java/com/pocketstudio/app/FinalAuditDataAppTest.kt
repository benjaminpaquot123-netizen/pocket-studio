package com.pocketstudio.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.instruments.BankMemory
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.rhythm.ProjectOpening
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.SoundLibrary
import com.pocketstudio.model.track
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** Final audit, data lot: opening a project (off the main thread, rescued from a crash loop), « Changer le son ». */
@RunWith(RobolectricTestRunner::class)
class FinalAuditDataAppTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val projects get() = File(app.filesDir, "projects")

    @Before
    fun clean() {
        projects.deleteRecursively()
        app.getSharedPreferences("opening", 0).edit().clear().commit()
    }

    @After
    fun forget() {
        ProjectOpening.take(CurrentProject.get(app))
        CurrentProject.set(app, RhythmViewModel.PROJECT_ID)
    }

    private fun saved(id: String, p: Project) = ProjectStore(projects).save(id, p)

    @Test
    fun theProjectIsReadBeforeTheScreenAndTheScreenDoesNotReadItAgain() {
        CurrentProject.set(app, "p-lu")
        saved("p-lu", Project(name = "Lu avant"))
        val opened = runBlocking { ProjectOpening.prepare(app) }
        assertEquals("Lu avant", opened.project.name)
        // The file changed after: the screen opens on what was prepared, it does not read again.
        saved("p-lu", Project(name = "Lu après"))
        val vm = RhythmViewModel(app)
        assertEquals("Lu avant", vm.project.value.name)
        // Taken once: another screen reads the disk itself.
        assertEquals("Lu après", RhythmViewModel(app).project.value.name)
    }

    @Test
    fun aProjectThatCrashedTheAppTwiceAsItOpenedIsNotOpenedAThirdTime() {
        CurrentProject.set(app, "p-plante")
        saved("p-plante", Project(name = "Qui plante"))
        val before = File(projects, "p-plante/project.json").readText()
        // Two openings that never settled (the app crashed each time).
        assertEquals(1, ProjectOpening.markOpening(app, "p-plante"))
        assertEquals(2, ProjectOpening.markOpening(app, "p-plante"))
        val opened = runBlocking { ProjectOpening.prepare(app) }
        assertTrue(opened.rescued)
        assertEquals(CurrentProject.NONE, opened.id)
        assertEquals(CurrentProject.NONE, CurrentProject.get(app))
        val vm = RhythmViewModel(app)
        assertEquals(CurrentProject.NONE, vm.projectId)
        assertTrue(vm.rescued)
        assertTrue("the user is told which one", vm.notice.value!!.contains("Qui plante"))
        assertEquals("the project untouched, not renamed", before, File(projects, "p-plante/project.json").readText())
        assertEquals(listOf("p-plante"), projects.list()!!.toList())
        // Opened again from « Mes projets »: it has its two tries again.
        CurrentProject.set(app, "p-plante")
        val again = runBlocking { ProjectOpening.prepare(app) }
        assertFalse(again.rescued)
        assertEquals("Qui plante", again.project.name)
    }

    @Test
    fun anOpeningThatSettledCountsNoMore() {
        CurrentProject.set(app, "p-ok")
        saved("p-ok", Project(name = "Calme"))
        repeat(4) {
            val opened = runBlocking { ProjectOpening.prepare(app) }
            assertFalse(opened.rescued)
            ProjectOpening.take("p-ok")
            ProjectOpening.settled(app)
        }
    }

    @Test
    fun changingTheSoundOfATrackPlayedByAnotherEngineGoesToTheSynth() {
        CurrentProject.set(app, RhythmViewModel.PROJECT_ID)
        val vm = RhythmViewModel(app)
        for (engine in listOf(InstrumentTrack.ENGINE_SAMPLED, InstrumentTrack.ENGINE_MULTI, InstrumentTrack.ENGINE_808)) {
            vm.selectSynthEngine(engine)
            val track = vm.selectedTrack.value
            assertEquals(engine, vm.project.value.track(track).engine)
            val sound = SoundLibrary.SYNTH[3].sound
            vm.applySynthPreset(sound)
            assertEquals("heard: the track plays the synth", InstrumentTrack.ENGINE_SYNTH, vm.project.value.track(track).engine)
            assertEquals(sound, vm.project.value.track(track).synth)
            vm.undo()
            assertEquals("one undo gives the sound back", engine, vm.project.value.track(track).engine)
        }
    }

    @Test
    fun aBankTooBigForTheFreeMemoryIsRefused() {
        val mb = 1024L * 1024
        assertTrue(BankMemory.fits(32 * mb, 1024 * mb))
        assertFalse("MuseScore's 215 MB on a phone with 400 MB free", BankMemory.fits(215 * mb, 400 * mb))
        assertTrue(BankMemory.fits(215 * mb, 1024 * mb))
        assertEquals(430 * mb, BankMemory.needed(215 * mb))
    }

    @Test
    fun noPreparedProjectIsTakenForAnother() {
        CurrentProject.set(app, "p-a")
        saved("p-a", Project(name = "A"))
        runBlocking { ProjectOpening.prepare(app) }
        assertNull(ProjectOpening.take("p-b"))
        assertNull("gone after a wrong take too", ProjectOpening.take("p-a"))
        assertNotNull(app)
    }
}
