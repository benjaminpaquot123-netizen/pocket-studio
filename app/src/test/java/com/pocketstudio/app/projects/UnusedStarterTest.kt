package com.pocketstudio.app.projects

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.rhythm.LiveTakeJournals
import com.pocketstudio.app.rhythm.ProjectOpening
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.PendingTake
import com.pocketstudio.model.PendingTakes
import com.pocketstudio.model.Project
import com.pocketstudio.model.TakeJournal
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File

/**
 * Relecture tour 2 (n° 14 of round 2): the install's first project « left untouched » is deleted for good once another
 * one is open. Its project.json is not all its work: a take written while it closed waits in its folder (takes/ and
 * samples/) and a Live take cut short in its journal, both put in at its next opening. Such a project is kept.
 */
@RunWith(RobolectricTestRunner::class)
class UnusedStarterTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val store = ProjectOpening.projectStore(app)
    private val starterId = "starter-" + System.nanoTime()

    @After
    fun end() {
        CurrentProject.set(app, RhythmViewModel.PROJECT_ID)
        LiveTakeJournals.of(app).file(starterId).delete()
    }

    private fun waitFor(what: String, condition: () -> Boolean) {
        val until = System.currentTimeMillis() + 15_000
        while (!condition()) {
            check(System.currentTimeMillis() < until) { "waited too long for $what" }
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
    }

    /** The first project made and remembered as made, then another project opened. */
    private fun starterLeft() {
        store.save(starterId, RhythmViewModel.starterProject().sanitized())
        CurrentProject.rememberStarter(app, starterId, store.load(starterId)!!)
        val other = "other-" + System.nanoTime()
        store.save(other, Project(name = "Techno"))
        CurrentProject.set(app, other)
    }

    /** « Mes projets » starts (the app's next launch): its first work done. */
    private fun projectsStart() {
        val vm = ProjectsViewModel(app)
        vm.refresh()
        waitFor("the list") { !vm.state.value.loading }
    }

    @Test
    fun anUntouchedFirstProjectGoes() {
        starterLeft()
        projectsStart()
        assertNull("nobody used it: gone", store.load(starterId))
    }

    /**
     * CI 43137620831 (63 ms, the starter still there): « Mes projets »'s first work and the list asked right after it
     * each started on a thread of their own, and the list could take the catalog first and say « loaded » before the
     * first work had run. Asked in that order, they run in that order, every time.
     */
    @Test
    fun anUntouchedFirstProjectGoesBeforeTheListIsShown() {
        repeat(ROUNDS) { round ->
            starterLeft()
            projectsStart()
            assertNull("round $round: the list loaded before the first project went", store.load(starterId))
        }
    }

    @Test
    fun aTakeWaitingInItsFolderKeepsIt() {
        starterLeft()
        val folder = store.folder(starterId)
        File(folder, "samples").mkdirs()
        File(folder, "samples/voix.wav").writeBytes(ByteArray(64))
        PendingTakes.write(folder, PendingTake(0, "voix.wav", "Prise 21:14", 3.0, 120.0))
        projectsStart()
        assertNotNull("a take recorded on it, not yet on its track: kept", store.load(starterId))
        assertNotNull("the take is still there", PendingTakes.read(folder).singleOrNull())
    }

    @Test
    fun aLiveTakesJournalKeepsIt() {
        starterLeft()
        val journal = LiveTakeJournals.of(app).file(starterId)
        journal.parentFile?.mkdirs()
        journal.writeText(TakeJournal(starterId, start = 0, row = 0, playing = emptyList(), starts = emptyList(), end = 16).encode())
        projectsStart()
        assertNotNull("a Live take cut short waits for it: kept", store.load(starterId))
    }

    private companion object {
        const val ROUNDS = 200
    }
}
