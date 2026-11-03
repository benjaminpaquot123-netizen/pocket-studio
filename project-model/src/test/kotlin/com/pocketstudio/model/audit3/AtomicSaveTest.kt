package com.pocketstudio.model.audit3

import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectJson
import com.pocketstudio.model.ProjectStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

/**
 * Audit stage 3, « écriture atomique »: the app killed at each step of a save leaves either the
 * project before or after it, never half of one; a save that fails leaves no temporary file, and
 * the next save goes through.
 */
class AtomicSaveTest {
    @get:Rule val tmp = TemporaryFolder()

    private val before = Project(name = "Avant", tempoBpm = 100.0)
    private val after = Project(name = "Après", tempoBpm = 140.0)

    private fun phone(): Pair<ProjectStore, File> {
        val root = tmp.newFolder()
        val store = ProjectStore(root)
        store.save("p", Project(name = "Premier"))
        store.save("p", before)
        return store to File(root, "p")
    }

    private fun bytes(p: Project) = ProjectJson.encode(p).toByteArray()

    /** What a kill leaves at each step of [ProjectStore.save] (files as the step left them). */
    private val killedAt: List<Pair<String, (File) -> Unit>> = listOf(
        "writing the temporary file" to { d -> File(d, ProjectStore.TMP).writeBytes(bytes(after).copyOf(bytes(after).size / 2)) },
        "before its sync (empty)" to { d -> File(d, ProjectStore.TMP).writeBytes(ByteArray(0)) },
        "copying the backup" to { d ->
            File(d, ProjectStore.TMP).writeBytes(bytes(after))
            File(d, ProjectStore.BACKUP).writeBytes(bytes(before).copyOf(10))
        },
        "before the rename" to { d ->
            File(d, ProjectStore.TMP).writeBytes(bytes(after))
            File(d, ProjectStore.BACKUP).writeBytes(bytes(before))
        },
    )

    @Test
    fun aKillAtAnyStepLeavesTheProjectBeforeAndTheNextSaveIsClean() {
        for ((step, kill) in killedAt) {
            val (store, dir) = phone()
            kill(dir)
            assertEquals("killed $step: the project as before", before, store.load("p"))
            store.save("p", after)
            assertEquals("$step: saved again", after, store.load("p"))
            assertFalse("$step: no temporary file left", File(dir, ProjectStore.TMP).exists())
            assertEquals("$step: the backup is the project before", before, ProjectJson.decode(File(dir, ProjectStore.BACKUP).readText()))
        }
    }

    @Test
    fun aKillAfterTheRenameHasTheNewProject() {
        val (store, dir) = phone()
        // Rename done, the cloud copy and the folder's sync not yet.
        File(dir, ProjectStore.BACKUP).writeBytes(bytes(before))
        File(dir, ProjectStore.MAIN).writeBytes(bytes(after))
        assertEquals(after, store.load("p"))
    }

    @Test
    fun aMainFileLostOpensTheBackup() {
        val (store, dir) = phone()
        File(dir, ProjectStore.MAIN).delete()
        assertEquals(Project(name = "Premier"), store.load("p"))
    }

    @Test
    fun aSaveThatFailsLeavesNoTemporaryFile() {
        val (store, dir) = phone()
        // The save cannot complete (stands for a full phone at the backup or rename step).
        File(dir, ProjectStore.BACKUP).delete()
        File(dir, ProjectStore.BACKUP).mkdirs()
        File(dir, "${ProjectStore.BACKUP}/x").writeText("x")
        val e = runCatching { store.save("p", after) }.exceptionOrNull()
        assertTrue("an IOException: $e", e is IOException)
        assertFalse("no half file left taking the room", File(dir, ProjectStore.TMP).exists())
        assertEquals("the project as before", before, store.load("p"))
    }
}
