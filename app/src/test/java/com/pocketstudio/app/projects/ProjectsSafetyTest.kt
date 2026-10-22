package com.pocketstudio.app.projects

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.model.Project
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/** « Dupliquer » and « Envoyer (.zip) » of the open project never go on from an older version when its save failed. */
@RunWith(RobolectricTestRunner::class)
class ProjectsSafetyTest {
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
    fun aCopyOrAZipIsRefusedWhenTheOpenProjectCouldNotBeSaved() {
        val catalog = projectCatalog(app)
        val id = catalog.create(Project(name = "Ouvert"))
        val vm = ProjectsViewModel(app)
        val info = catalog.info(id)!!
        vm.duplicate(info, saveFirst = { false })
        waitFor("the refusal") { vm.state.value.message != null }
        assertTrue(vm.state.value.message!!.startsWith("Le projet n'a pas pu être sauvegardé"))
        assertEquals(1, catalog.list().size)
        vm.dismissMessage()
        var shared = false
        vm.exportZip(info, saveFirst = { false }) { shared = true }
        waitFor("the refusal") { vm.state.value.message != null }
        assertTrue(!shared)
        // Saved: the copy is made.
        vm.duplicate(info, saveFirst = { true })
        waitFor("the copy") { catalog.list().size == 2 }
    }

    @Test
    fun theTrashIsEmptiedForGood() {
        val catalog = projectCatalog(app)
        catalog.delete(catalog.create(Project(name = "Un")))
        val two = catalog.delete(catalog.create(Project(name = "Deux")))
        val vm = ProjectsViewModel(app)
        vm.refresh()
        waitFor("the list") { vm.state.value.trash.size == 2 }
        vm.deleteForever(vm.state.value.trash.first { it.entry == two })
        waitFor("one gone") { vm.state.value.trash.size == 1 }
        vm.emptyTrash()
        waitFor("all gone") { vm.state.value.trash.isEmpty() }
        assertTrue(catalog.trashed().isEmpty())
    }

    /**
     * Audit 28 review M4: « Restaurer » tapped while the trash is being emptied waits for it (the
     * buttons are off meanwhile): it never renames a folder half deleted.
     */
    @Test
    fun aRestoreWaitsForTheTrashBeingEmptied() {
        val catalog = projectCatalog(app)
        catalog.trashed().forEach { catalog.deleteForever(it.entry) }
        catalog.delete(catalog.create(Project(name = "Un")))
        val vm = ProjectsViewModel(app)
        vm.refresh()
        waitFor("the list") { vm.state.value.trash.size == 1 }
        val item = vm.state.value.trash.single()
        val emptying = java.util.concurrent.CountDownLatch(1)
        val go = kotlinx.coroutines.CompletableDeferred<Unit>()
        vm.beforeOperationForTest = { what -> if (what == "Suppression") { emptying.countDown(); go.await() } }
        vm.emptyTrash()
        assertTrue(emptying.await(5, java.util.concurrent.TimeUnit.SECONDS))
        assertTrue(vm.state.value.busy)
        vm.restore(item)
        Thread.sleep(100)
        // The restore waits: the project is still in the trash, untouched.
        assertEquals(1, catalog.trashed().size)
        assertTrue(catalog.list().isEmpty())
        go.complete(Unit)
        waitFor("both done") { !vm.state.value.busy }
        // The trash went first: nothing half restored, and it says so.
        assertTrue(catalog.trashed().isEmpty())
        assertTrue(catalog.list().isEmpty())
        assertTrue(vm.state.value.message!!.startsWith("Restauration impossible"))
    }

    /**
     * m5: a project trashed while the last save of its screen (another project just opened) is
     * still being written waits for it: the save never brings the folder back beside the trash.
     */
    @Test
    fun aDeletionWaitsForTheLastSavesOfTheScreenThatClosed() {
        val catalog = projectCatalog(app)
        val id = catalog.create(Project(name = "Fermé"))
        val vm = ProjectsViewModel(app)
        val saving = kotlinx.coroutines.CompletableDeferred<Unit>()
        com.pocketstudio.app.rhythm.ClosingSaves.launch {
            saving.await()
            catalog.store.save(id, Project(name = "Fermé, sauvé"))
        }
        vm.delete(catalog.info(id)!!)
        Thread.sleep(150)
        assertTrue("not trashed while its save runs", catalog.store.folder(id).exists())
        saving.complete(Unit)
        waitFor("trashed") { vm.state.value.deleted != null }
        assertTrue(catalog.list().none { it.id == id })
        assertEquals("Fermé, sauvé", catalog.trashed().single { it.entry.startsWith(id) }.name)
    }

    /** m2: the Live take journal of a project deleted for good goes with it; the others stay. */
    @Test
    fun theJournalOfAProjectDeletedForGoodGoes() {
        val catalog = projectCatalog(app)
        catalog.trashed().forEach { catalog.deleteForever(it.entry) }
        val kept = catalog.create(Project(name = "Gardé"))
        val gone = catalog.create(Project(name = "Parti"))
        val journals = com.pocketstudio.app.rhythm.LiveTakeJournals.of(app)
        fun journal(id: String) = com.pocketstudio.model.TakeJournal(projectId = id, takeId = "t-$id", start = 0, row = 0, playing = emptyList(), starts = emptyList(), end = 16)
        journals.write(journal(kept))
        journals.write(journal(gone))
        assertTrue(journals.awaitIdle(5000))
        catalog.delete(gone)
        val vm = ProjectsViewModel(app)
        vm.refresh()
        waitFor("the list") { vm.state.value.trash.size == 1 }
        assertTrue("kept while in the trash", journals.file(gone).exists())
        vm.emptyTrash()
        waitFor("the trash emptied") { vm.state.value.trash.isEmpty() && !vm.state.value.busy }
        assertTrue(journals.awaitIdle(5000))
        assertTrue(journals.file(kept).exists())
        assertTrue(!journals.file(gone).exists())
    }
}
