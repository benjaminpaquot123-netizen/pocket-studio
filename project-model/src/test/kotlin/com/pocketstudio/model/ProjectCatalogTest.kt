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
        val b = catalog.importZip(bytes.inputStream()).id
        assertNotEquals(a, b)
        assertEquals("Voyage", catalog.store.load(b)!!.name)
        assertEquals("boum", File(catalog.store.folder(b), "samples/kick.wav").readText())
        assertFalse(File(catalog.store.folder(b), "samples/old.wav").exists())  // unused sounds stay home
        // Not a project: refused, nothing left behind.
        val before = catalog.list().size
        assertTrue(runCatching { catalog.importZip("pas un zip".byteInputStream()) }.isFailure)
        val noSound = java.io.ByteArrayOutputStream().also { out ->
            java.util.zip.ZipOutputStream(out).use { z ->
                z.putNextEntry(java.util.zip.ZipEntry("project.json")); z.write(ProjectJson.encode(p).toByteArray()); z.closeEntry()
                z.putNextEntry(java.util.zip.ZipEntry("../../evil.wav")); z.write(1); z.closeEntry()
            }
        }.toByteArray()
        // A project missing its sound comes in without it (it used to refuse the whole .zip); a path is never followed.
        val holed = catalog.importZip(noSound.inputStream())
        assertEquals(1, holed.missingSounds)
        assertFalse(File(dir, "evil.wav").exists())
        catalog.store.delete(holed.id)
        assertEquals(before, catalog.list().size)
        // Twice the same sound, or a giant project.json: refused.
        val twice = java.io.ByteArrayOutputStream().also { out ->
            java.util.zip.ZipOutputStream(out).use { z ->
                z.putNextEntry(java.util.zip.ZipEntry("project.json")); z.write(ProjectJson.encode(p).toByteArray()); z.closeEntry()
                // Java refuses two identical names: the second spelling becomes the same path once read.
                listOf("samples/kick.wav", "samples\\kick.wav").forEach { z.putNextEntry(java.util.zip.ZipEntry(it)); z.write(1); z.closeEntry() }
            }
        }.toByteArray()
        assertTrue(runCatching { catalog.importZip(twice.inputStream()) }.isFailure)
        val giant = java.io.ByteArrayOutputStream().also { out ->
            java.util.zip.ZipOutputStream(out).use { z ->
                z.putNextEntry(java.util.zip.ZipEntry("project.json")); z.write(ByteArray((ProjectCatalog.MAX_PROJECT_JSON + 1).toInt()) { ' '.code.toByte() }); z.closeEntry()
            }
        }.toByteArray()
        assertEquals("projet trop gros", runCatching { catalog.importZip(giant.inputStream()) }.exceptionOrNull()?.message)
        assertEquals(before, catalog.list().size)
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

    @Test
    fun aFailedDuplicateLeavesNoHalfCopy() {
        val a = catalog.create(Project(name = "Source"))
        val samples = File(catalog.store.folder(a), "samples").apply { mkdirs() }
        samples.resolve("a.wav").writeText("son")
        // A sound that cannot be read stops the copy halfway.
        Files.createSymbolicLink(samples.resolve("b.wav").toPath(), File(dir, "nowhere.wav").toPath())
        assertTrue(runCatching { catalog.duplicate(a, "Copie") }.isFailure)
        assertEquals(listOf(a), File(dir, "projects").list()!!.toList())
        dir.deleteRecursively()
    }

    @Test
    fun theTrashListsDeletedProjectsToRestore() {
        val a = catalog.create(Project(name = "Premier"))
        val b = catalog.create(Project(name = "Second"))
        catalog.delete(a)
        Thread.sleep(5)
        catalog.delete(b)
        val bin = catalog.trashed()
        assertEquals(listOf("Second", "Premier"), bin.map { it.name })  // most recently deleted first
        catalog.restore(bin.last().entry)
        assertEquals(listOf("Premier"), catalog.list().map { it.name })
        assertEquals(listOf("Second"), catalog.trashed().map { it.name })
        dir.deleteRecursively()
    }

    @Test
    fun stemSetsUsedByAnyProjectAreFound() {
        catalog.create(Project(name = "A", remix = Remix(setId = "s1", name = "Chanson")))
        catalog.create(Project(name = "B", remix = Remix(setId = "s1", name = "Chanson")))
        catalog.create(Project(name = "Sans remix"))
        catalog.delete(catalog.create(Project(name = "Jeté", remix = Remix(setId = "s2"))))
        // Unreadable, but its file still names its song: that song is protected too.
        File(catalog.store.folder("p-broken"), ProjectStore.MAIN).apply { parentFile.mkdirs() }
            .writeText("{ \"remix\": { \"setId\": \"s3\", pas du json")
        val users = catalog.stemSetUsers()
        assertEquals(setOf("s1", "s2", "s3"), users.keys)
        assertEquals(setOf("A", "B"), users.getValue("s1").toSet())
        assertEquals(listOf("Jeté (corbeille)"), users.getValue("s2"))
        dir.deleteRecursively()
    }

    @Test
    fun aBrokenZipGivesAFrenchMessage() {
        val bad = java.io.ByteArrayOutputStream().also { out ->
            java.util.zip.ZipOutputStream(out).use { z ->
                z.putNextEntry(java.util.zip.ZipEntry("project.json")); z.write("{ pas du json".toByteArray()); z.closeEntry()
            }
        }.toByteArray()
        val e = runCatching { catalog.importZip(bad.inputStream()) }.exceptionOrNull()
        assertEquals("fichier illisible ou abîmé", e?.message)
        dir.deleteRecursively()
    }
}
