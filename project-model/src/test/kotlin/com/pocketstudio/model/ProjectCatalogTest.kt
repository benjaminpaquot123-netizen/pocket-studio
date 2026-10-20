package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ProjectCatalogTest {
    private val dir = Files.createTempDirectory("catalog").toFile()
    private val catalog = ProjectCatalog(File(dir, "projects"), File(dir, "trash"))

    @Test
    fun createListRenameDuplicate() {
        val a = catalog.create(Project(name = "Da Funk", tempoBpm = 111.0))
        Thread.sleep(20)
        val b = catalog.create(Project(name = "Acide"))
        assertEquals(listOf(b, a), catalog.list().map { it.id })  // most recent first
        assertEquals(111.0, catalog.info(a)!!.tempoBpm, 0.0)
        catalog.rename(a, "Da Funk (remix)")
        assertEquals("Da Funk (remix)", catalog.store.load(a)!!.name)
        // A duplicate carries the project's own sounds.
        File(catalog.store.folder(a), "samples").apply { mkdirs() }.resolve("k.wav").writeText("kick")
        val c = catalog.duplicate(a, "Copie")
        assertNotEquals(a, c)
        assertEquals("kick", File(catalog.store.folder(c), "samples/k.wav").readText())
        assertEquals("Copie", catalog.store.load(c)!!.name)
        assertEquals("Mon projet 1", catalog.nextName())
        dir.deleteRecursively()
    }

    @Test
    fun deleteGoesToTheTrashAndComesBack() {
        val a = catalog.create(Project(name = "Précieux"))
        val entry = catalog.delete(a)
        assertTrue(catalog.list().isEmpty())
        val back = catalog.restore(entry)
        assertEquals(a, back)
        assertEquals("Précieux", catalog.store.load(back)!!.name)
        // Old trash is forgotten, recent trash is kept.
        val e2 = catalog.delete(back)
        catalog.purgeTrash(maxAgeMs = 1000, now = System.currentTimeMillis())
        assertTrue(File(dir, "trash/$e2").exists())
        catalog.purgeTrash(maxAgeMs = 1000, now = System.currentTimeMillis() + 5000)
        assertFalse(File(dir, "trash/$e2").exists())
        dir.deleteRecursively()
    }

    @Test
    fun zipRoundTripKeepsTheProjectAndItsSounds() {
        val p = Project(name = "Voyage", drums = DrumPattern().withSample(0, PadSample("kick.wav", "Kick")))
        val a = catalog.create(p)
        File(catalog.store.folder(a), "samples").apply { mkdirs() }.resolve("kick.wav").writeText("boum")
        File(catalog.store.folder(a), "samples/old.wav").writeText("plus utilisé")
        val bytes = java.io.ByteArrayOutputStream().also { catalog.exportZip(a, it) }.toByteArray()
        val b = catalog.importZip(bytes.inputStream())
        assertNotEquals(a, b)
        assertEquals("Voyage", catalog.store.load(b)!!.name)
        assertEquals("boum", File(catalog.store.folder(b), "samples/kick.wav").readText())
        assertFalse(File(catalog.store.folder(b), "samples/old.wav").exists())  // unused sounds stay home
        // Not a project, or a project missing its sound: refused, nothing left behind.
        val before = catalog.list().size
        assertTrue(runCatching { catalog.importZip("pas un zip".byteInputStream()) }.isFailure)
        val noSound = java.io.ByteArrayOutputStream().also { out ->
            java.util.zip.ZipOutputStream(out).use { z ->
                z.putNextEntry(java.util.zip.ZipEntry("project.json")); z.write(ProjectJson.encode(p).toByteArray()); z.closeEntry()
                z.putNextEntry(java.util.zip.ZipEntry("../../evil.wav")); z.write(1); z.closeEntry()
            }
        }.toByteArray()
        assertTrue(runCatching { catalog.importZip(noSound.inputStream()) }.isFailure)
        assertEquals(before, catalog.list().size)
        assertFalse(File(dir, "evil.wav").exists())
        dir.deleteRecursively()
    }

    @Test
    fun unreadableProjectsAreListedNotHidden() {
        val a = catalog.create(Project(name = "Ok"))
        File(catalog.store.folder("p-broken"), ProjectStore.MAIN).apply { parentFile.mkdirs() }.writeText("{ pas du json")
        val names = catalog.list().map { it.name }
        assertTrue("Ok" in names && "Projet illisible" in names)
        assertEquals(a, catalog.list().first { it.name == "Ok" }.id)
        dir.deleteRecursively()
    }
}
