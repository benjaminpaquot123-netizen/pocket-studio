package com.pocketstudio.model

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

/** Final audit, data lot (reports 14, 16, 19): the versions kept, the cloud backup, the unreadable projects. */
@OptIn(ExperimentalCoroutinesApi::class)
class FinalAuditDataTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun twoScenes(): Project = Project(name = "Deux").switchScene(1).copy(tempoBpm = 120.0).switchScene(0)

    @Test
    fun followingThePartsWhilePlayingWritesNothingUntilTheFlush() = runTest {
        val root = tmp.newFolder("p")
        val store = ProjectStore(root)
        val io = StandardTestDispatcher(testScheduler)
        val session = ProjectSession("p", twoScenes(), store, this, io, clock = { testScheduler.currentTime })
        session.edit { it.copy(name = "Deux bis") }
        advanceUntilIdle()
        val file = File(root, "p/project.json")
        val text = file.readText()
        repeat(15) { i -> session.showQuietly { it.switchScene((i + 1) % 2) } }
        advanceUntilIdle()
        assertEquals("a part followed is not a save", text, file.readText())
        assertFalse("nothing is said unsaved for it", session.unsaved.value)
        assertFalse("but the session is not settled: a closing screen still writes it", session.settled)
        val shown = session.project.value.scene
        assertEquals(1, shown)
        assertEquals(0, store.load("p")!!.scene)
        assertTrue(session.flush())
        assertEquals("the flush writes it", shown, store.load("p")!!.scene)
        assertTrue(session.settled)
        // A real edit after a part followed writes both.
        session.showQuietly { it.switchScene(1 - shown) }
        session.edit { it.copy(name = "Trois") }
        advanceUntilIdle()
        assertEquals(1 - shown, store.load("p")!!.scene)
        assertEquals("Trois", store.load("p")!!.name)
    }

    @Test
    fun theVersionBeforeTheSessionIsKeptOnceAndCopiedBackAsANewProject() = runTest {
        val root = tmp.newFolder("projects")
        val catalog = ProjectCatalog(root, File(tmp.root, "trash"))
        val id = catalog.create(Project(name = "Ma chanson", tempoBpm = 100.0))
        File(root, "$id/samples").mkdirs()
        File(root, "$id/samples/kick.wav").writeText("son")
        val io = StandardTestDispatcher(testScheduler)
        val session = ProjectSession(id, catalog.store.load(id)!!, catalog.store, this, io, clock = { testScheduler.currentTime })
        assertFalse("none before a session saved", catalog.info(id)!!.hasOpening)
        session.edit { it.copy(tempoBpm = 130.0) }
        advanceUntilIdle()
        session.edit { it.copy(tempoBpm = 140.0) }
        advanceUntilIdle()
        assertEquals(140.0, catalog.store.load(id)!!.tempoBpm, 0.0)
        assertEquals("the file as it was when the session began", 100.0, catalog.store.loadOpening(id)!!.tempoBpm, 0.0)
        assertTrue(catalog.info(id)!!.hasOpening)
        val copy = catalog.restoreOpening(id, ProjectCatalog.beforeSessionName("Ma chanson"))
        assertEquals(100.0, catalog.store.load(copy)!!.tempoBpm, 0.0)
        assertEquals("Ma chanson (avant la séance)", catalog.store.load(copy)!!.name)
        assertEquals("its sounds go with it", "son", File(root, "$copy/samples/kick.wav").readText())
        assertEquals("the project itself untouched", 140.0, catalog.store.load(id)!!.tempoBpm, 0.0)
        // The next session keeps the version it began with.
        val next = ProjectSession(id, catalog.store.load(id)!!, catalog.store, this, io, clock = { testScheduler.currentTime })
        next.edit { it.copy(tempoBpm = 150.0) }
        advanceUntilIdle()
        assertEquals(140.0, catalog.store.loadOpening(id)!!.tempoBpm, 0.0)
    }

    @Test
    fun theCloudBackupGetsCompactCopiesAndAPhoneSetUpAgainGetsThemBack() {
        val backup = File(tmp.root, "projects-backup")
        val root = tmp.newFolder("projects")
        val catalog = ProjectCatalog(root, File(tmp.root, "trash"), backup = backup)
        val sample = PadSample("kick-1.wav", "Kick")
        val p = Project(name = "Avec un son", drums = DrumPattern().let { d -> d.copy(lanes = d.lanes.mapIndexed { i, l -> if (i == 0) l.copy(sample = sample) else l }) })
        val kept = catalog.create(p)
        val gone = catalog.create(Project(name = "Jetée"))
        File(root, "$kept/samples").mkdirs()
        File(root, "$kept/samples/kick-1.wav").writeBytes(ByteArray(1_000_000))
        val mirror = File(backup, "$kept${ProjectStore.MIRROR}")
        assertTrue("a copy for the backup", mirror.isFile)
        assertTrue("compressed, and never the sounds", mirror.length() < File(root, "$kept/project.json").length())
        catalog.delete(gone)
        assertFalse("a deleted project is not in the backup", File(backup, "$gone${ProjectStore.MIRROR}").exists())

        // A new phone: Android brought back the copies only.
        val phone = File(tmp.root, "phone").apply { mkdirs() }
        backup.copyRecursively(File(phone, "projects-backup"))
        val restored = ProjectCatalog(File(phone, "projects"), File(phone, "trash"), backup = File(phone, "projects-backup"))
        assertEquals(1, restored.restoreFromBackup())
        val back = restored.store.load(kept)!!
        assertEquals("Avec un son", back.name)
        assertNull("its sound is not on this phone: the pad goes back to the kit", back.drums.lanes[0].sample)
        assertEquals("never twice", 0, restored.restoreFromBackup())
    }

    @Test
    fun anUnreadableProjectIsSaidSentAsItIsAndNeverPurged() {
        val root = tmp.newFolder("projects")
        val trash = File(tmp.root, "trash")
        val catalog = ProjectCatalog(root, trash)
        val id = "projet-1${ProjectCatalog.UNREADABLE}123"
        File(root, "$id/samples").mkdirs()
        File(root, "$id/project.json").writeText("{ pas du json")
        File(root, "$id/samples/voix.wav").writeText("voix")
        val info = catalog.info(id)!!
        assertTrue(info.unreadable)
        assertTrue(assertThrows { catalog.exportZip(id, ByteArrayOutputStream()) })
        val zip = ByteArrayOutputStream().also { catalog.exportRawZip(id, it) }.toByteArray()
        val names = java.util.zip.ZipInputStream(zip.inputStream()).use { z -> generateSequence { z.nextEntry?.name }.toList() }
        assertEquals(listOf("project.json", "samples/voix.wav"), names.sorted())
        // In the trash for months: still there.
        val entry = catalog.delete(id)
        catalog.purgeTrash(maxAgeMs = 0, now = System.currentTimeMillis() + 90L * 24 * 3600 * 1000)
        assertTrue(File(trash, entry).isDirectory)
        // Another unreadable one without the mark, and a readable one: only the readable one goes.
        val other = catalog.create(Project(name = "Lisible"))
        val broken = catalog.create(Project(name = "Abîmé"))
        File(root, "$broken/project.json").writeText("{")
        File(root, "$broken/project.json.bak").writeText("{")
        val e1 = catalog.delete(other)
        val e2 = catalog.delete(broken)
        catalog.purgeTrash(maxAgeMs = 0, now = System.currentTimeMillis() + 90L * 24 * 3600 * 1000)
        assertFalse(File(trash, e1).exists())
        assertTrue(File(trash, e2).exists())
    }

    @Test
    fun theCopyBeforeTheSessionIsNamedInEveryLanguage() {
        val name = ProjectCatalog.beforeSessionName("Mon projet 2")
        com.pocketstudio.model.i18n.I18n.lang = com.pocketstudio.model.i18n.Lang.EN
        try {
            assertEquals("My project 2 (before the session)", DefaultNames.shown(name))
        } finally {
            com.pocketstudio.model.i18n.I18n.lang = com.pocketstudio.model.i18n.Lang.FR
        }
        assertNotNull(name)
    }

    private fun assertThrows(block: () -> Unit): Boolean = try {
        block()
        false
    } catch (e: IOException) {
        true
    }
}
