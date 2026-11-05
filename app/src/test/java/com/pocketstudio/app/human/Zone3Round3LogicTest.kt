package com.pocketstudio.app.human

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.record.TakePhase
import com.pocketstudio.app.record.TakeSource
import com.pocketstudio.app.rhythm.RhythmViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import android.os.Looper
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectCatalog
import com.pocketstudio.model.ProjectStore
import org.robolectric.Shadows.shadowOf

/** Audit ultra round 3, zone 3 (n° 15 to 22): the view model's part, without the bench. */
@RunWith(RobolectricTestRunner::class)
class Zone3Round3LogicTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    private fun vm(): RhythmViewModel {
        File(app.filesDir, "projects").deleteRecursively()
        return RhythmViewModel(app).also { it.playingForTest = true }
    }

    private fun RhythmViewModel.play(pitch: Int, from: Double, to: Double) {
        recordPositionForTest = from
        playedClockForTest = from
        synthNoteOn(pitch, 0.8f)
        recordPositionForTest = to
        playedClockForTest = to
        synthNoteOff(pitch)
    }

    /**
     * n° 18 (seat D a, écran 16): Retour during « Enregistrer ma mélodie » goes home, the way a microphone take is kept
     * ([RhythmViewModel.keepTake], then [RhythmViewModel.stop]): Do and Sol played must stay, never « Rien n'a été joué ».
     */
    @Test
    fun backDuringAKeyboardTakeKeepsItsNotes() {
        for (melody in listOf(false, true)) {
            val vm = vm()
            vm.recordTake(TakeSource.KEYS, melody = melody)
            assertEquals(TakePhase.RECORDING, vm.noteTake.value!!.phase)
            vm.play(60, 1.2, 2.9)
            vm.play(67, 3.8, 5.0)
            // AppRoot's goHome: the take kept, then the music stopped.
            runBlocking { vm.keepTake() }
            vm.stop()
            assertFalse("melody=$melody: « Rien n'a été joué »", vm.takeEmpty.value)
            assertFalse(vm.recording.value)
            assertEquals("melody=$melody", 2, vm.project.value.synthNotes.notes.size)
        }
    }

    /**
     * n° 17 (seat B a, « Mes projets » said « à l'instant » for a song left 3 days ago): opening a project and leaving it
     * untouched must not change its last edit (each save wrote the same bytes again, so the file's date was « now »).
     */
    @Test
    fun anOpenedProjectKeepsItsLastEdit() {
        File(app.filesDir, "projects").deleteRecursively()
        val root = File(app.filesDir, "projects")
        val store = ProjectStore(root)
        store.save("old", Project(name = "Révisions"))
        val threeDaysAgo = System.currentTimeMillis() - 3 * 24 * 3600_000L
        root.resolve("old").listFiles()!!.forEach { it.setLastModified(threeDaysAgo) }
        CurrentProject.set(app, "old")
        val vm = RhythmViewModel(app)
        val until = System.currentTimeMillis() + 10_000
        while (vm.project.value.name != "Révisions") {
            check(System.currentTimeMillis() < until) { "never opened" }
            shadowOf(Looper.getMainLooper()).idle(); Thread.sleep(5)
        }
        runBlocking { vm.flushNow() }
        vm.stop()
        runBlocking { vm.flushNow() }
        val modified = ProjectCatalog(root, File(app.filesDir, "projects-trash")).list().first { it.id == "old" }.modified
        assertEquals("still 3 days ago", threeDaysAgo / 1000, modified / 1000)
    }

    /**
     * n° 19 (seat E b): « J'apprends » › Facile › House again, the course at chapter 4 in « Mijn project 2 »: that project is
     * taken up (most recent first), not a new one at chapter 1. Another style, another level or a course finished: a new one.
     */
    @Test
    fun learningAgainTakesUpTheCourseUnderWay() {
        fun info(id: String) = com.pocketstudio.model.ProjectInfo(id, id, 0L, 120.0, 4, 1, false)
        val facile = com.pocketstudio.model.tuto.TutoLevel.Facile
        val steps = com.pocketstudio.model.tuto.TutoCourse.of("house")!!.chaptersFor(facile).flatMap { c -> c.steps.map { it.id } }
        val saved = mapOf(
            "done" to com.pocketstudio.model.tuto.TutoProgress("house", facile, done = steps.toSet()),
            "mine" to com.pocketstudio.model.tuto.TutoProgress("house", facile, done = steps.take(5).toSet(), active = false),
            "older" to com.pocketstudio.model.tuto.TutoProgress("house", facile),
            "techno" to com.pocketstudio.model.tuto.TutoProgress("techno", facile),
        )
        val list = listOf("done", "techno", "mine", "older", "none").map(::info)
        val pick = com.pocketstudio.app.tuto.TutoResume.pick(list, saved::get, "house", facile)
        assertEquals("mine", pick?.first)
        assertEquals(null, com.pocketstudio.app.tuto.TutoResume.pick(list, saved::get, "house", com.pocketstudio.model.tuto.TutoLevel.Pro))
        assertEquals(null, com.pocketstudio.app.tuto.TutoResume.pick(listOf(info("done")), saved::get, "house", facile))
    }
}
