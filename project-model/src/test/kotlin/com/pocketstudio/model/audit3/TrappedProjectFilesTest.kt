package com.pocketstudio.model.audit3

import com.pocketstudio.model.PadSample
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectCatalog
import com.pocketstudio.model.ProjectJson
import com.pocketstudio.model.ProjectStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Audit stage 3, « fichiers piégés »: a project .zip that lies (paths out of the folder, a bomb,
 * a truncated archive, a broken or crazy project.json) fails with a message or imports cleaned;
 * never a crash, never a file written outside the new project's folder, nothing left behind.
 */
class TrappedProjectFilesTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun zip(vararg entries: Pair<String, ByteArray>, method: Int = ZipEntry.DEFLATED): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z ->
            z.setMethod(method)
            for ((name, data) in entries) {
                z.putNextEntry(ZipEntry(name))
                z.write(data)
                z.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private val goodJson = ProjectJson.encode(Project(name = "Piège").let { it.copy(drums = it.drums.withSample(0, PadSample("kick.wav", "Kick"))) }).toByteArray()

    private fun everyFile(dir: File) = dir.walkTopDown().filter { it.isFile }.map { it.relativeTo(dir).path }.toSet()

    /** Imports [bytes] in a fresh phone; returns the catalog, the result or the error, and checks nothing escaped. */
    private fun import(bytes: InputStream): Pair<ProjectCatalog, Result<com.pocketstudio.model.ImportedProject>> {
        val phone = tmp.newFolder()
        val catalog = ProjectCatalog(File(phone, "projects"), File(phone, "trash"), stems = File(phone, "stems"))
        val result = runCatching { catalog.importZip(bytes) }
        result.exceptionOrNull()?.let { assertTrue("a clean message, not ${it.javaClass}", it is IOException && !it.message.isNullOrBlank()) }
        val escaped = everyFile(phone).filterNot { it.startsWith("projects/") }
        assertTrue("nothing written outside the projects: $escaped", escaped.isEmpty())
        if (result.isFailure) assertTrue("nothing half-imported: ${everyFile(phone)}", everyFile(phone).isEmpty())
        // Nothing beside the project's own files (no .stems, no .project.json left).
        result.getOrNull()?.let { r ->
            val left = everyFile(catalog.store.folder(r.id)).filter { it.startsWith(".") }
            assertTrue("temporary files left: $left", left.isEmpty())
        }
        return catalog to result
    }

    @Test
    fun pathsOutOfTheFolderAreIgnored() {
        val (catalog, r) = import(
            ByteArrayInputStream(
                zip(
                    "project.json" to goodJson,
                    "samples/kick.wav" to ByteArray(100),
                    "../evil.txt" to "x".toByteArray(),
                    "samples/../../evil.wav" to ByteArray(10),
                    "/etc/evil" to ByteArray(10),
                    "samples\\..\\..\\evil2.wav" to ByteArray(10),
                    "stems/../x/y.wav" to ByteArray(10),
                    "samples/.hidden.wav" to ByteArray(10),
                ),
            ),
        )
        val id = r.getOrThrow().id
        assertEquals(setOf("project.json", "samples/kick.wav"), everyFile(catalog.store.folder(id)) - setOf("project.json.bak"))
        assertEquals("Piège", catalog.store.load(id)!!.name)
    }

    @Test
    fun aCompressionBombStopsAtTheLimitWithAMessage() {
        // One entry claiming far more than a sound may weigh: zeros compress 1000 to 1; streamed, never all in memory.
        val bomb = object : InputStream() {
            val pipe = java.io.PipedInputStream(1 shl 16)
            val out = java.io.PipedOutputStream(pipe)
            val writer = Thread {
                runCatching {
                    ZipOutputStream(out).use { z ->
                        z.putNextEntry(ZipEntry("project.json")); z.write(goodJson); z.closeEntry()
                        z.putNextEntry(ZipEntry("samples/bomb.wav"))
                        val zeros = ByteArray(1 shl 20)
                        repeat(((ProjectCatalog.MAX_ZIP_ENTRY shr 20) + 4).toInt()) { z.write(zeros) }
                        z.closeEntry()
                    }
                }
            }.also { it.isDaemon = true; it.start() }
            override fun read(): Int = pipe.read()
            override fun read(b: ByteArray, off: Int, len: Int): Int = pipe.read(b, off, len)
            override fun close() = pipe.close()
        }
        val (_, r) = import(bomb)
        assertTrue(r.isFailure)
    }

    @Test
    fun aHugeProjectJsonIsRefused() {
        val big = ByteArray((ProjectCatalog.MAX_PROJECT_JSON + 10).toInt()) { ' '.code.toByte() }
        val (_, r) = import(ByteArrayInputStream(zip("project.json" to big)))
        assertTrue(r.isFailure)
    }

    @Test
    fun brokenArchivesAndProjectsFailCleanly() {
        val whole = zip("project.json" to goodJson, "samples/kick.wav" to ByteArray(4000) { it.toByte() })
        val cases = listOf(
            "empty" to ByteArray(0),
            "not a zip" to "hello".toByteArray(),
            "truncated" to whole.copyOf(whole.size / 2),
            "no project" to zip("samples/kick.wav" to ByteArray(10)),
            "duplicate" to zip("project.json" to goodJson, "samples/a.wav" to ByteArray(1), "samples\\a.wav" to ByteArray(1)),
            "truncated json" to zip("project.json" to goodJson.copyOf(goodJson.size / 2)),
            "not json" to zip("project.json" to ByteArray(300) { (it * 7).toByte() }),
            "json array" to zip("project.json" to "[1,2,3]".toByteArray()),
            "deeply nested" to zip("project.json" to ("{\"x\":" + "[".repeat(200_000) + "]".repeat(200_000) + "}").toByteArray()),
        )
        for ((what, bytes) in cases) {
            val (_, r) = import(ByteArrayInputStream(bytes))
            // A truncated archive may still hold a whole project.json: then it imports (its sound left out).
            when {
                what == "truncated" && r.isSuccess -> assertEquals(1, r.getOrThrow().missingSounds)
                // Unknown keys are skipped (no stack overflow): the project itself is a blank one.
                what == "deeply nested" -> assertTrue(r.isSuccess)
                else -> assertTrue("$what: refused", r.isFailure)
            }
        }
    }

    @Test
    fun crazyValuesAreCleanedOnImport() {
        val crazy = """
            {"name":"${"x".repeat(5000)}","tempoBpm":1e308,"swing":-50,"stepsPerBar":0,"beatsPerBar":-3,
             "drums":{"steps":99999,"lanes":[{"pan":1e9,"length":-5,"hits":{"-1":{"velocity":-4},"999999":{"velocity":2},"3":{"velocity":1e30,"repeats":1000000,"chance":-9,"span":77}},
               "sample":{"file":"../../x.wav","name":"x"}}]},
             "song":[{"scene":-7,"repeats":-1},{"scene":1000000}],
             "mixer":{"channels":[{"volume":1e30,"pan":-1e30}]}}
        """.trimIndent().toByteArray()
        // A strength out of 0..1 is kept as saved (the engine clamps it, SetDrumHit); only NaN goes.
        val (catalog, r) = import(ByteArrayInputStream(zip("project.json" to crazy)))
        val p = catalog.store.load(r.getOrThrow().id)!!
        assertTrue("tempo ${p.tempoBpm}", p.tempoBpm.isFinite() && p.tempoBpm in Project.MIN_TEMPO..Project.MAX_TEMPO)
        assertEquals(ProjectCatalog.MAX_NAME, p.name.length)
        assertTrue("lanes ${p.drums.lanes.map { it.sample to it.pan }}", p.drums.lanes.all { it.sample == null && it.pan in -1f..1f })
        assertTrue("hits ${p.drums.lanes.map { it.hits }} steps ${p.drums.steps}", p.drums.lanes.flatMap { it.hits.entries }.all { (s, h) -> s in 0 until com.pocketstudio.model.DrumPattern.MAX_STEPS && h.velocity.isFinite() && h.repeats in 1..com.pocketstudio.model.DrumPattern.MAX_REPEATS })
        assertTrue("song ${p.song}", p.song.all { it.scene >= 0 })
        assertTrue("mixer", p.mixer.channels.all { it.volume.isFinite() && it.volume in 0f..2f && it.pan in -1f..1f })
        // And it opens again as it was saved.
        assertEquals(p, ProjectJson.decode(ProjectJson.encode(p)))
    }

    @Test
    fun aTruncatedProjectOnTheDiskFallsBackToItsBackupOrSaysSo() {
        val root = tmp.newFolder("projects")
        val store = ProjectStore(root)
        store.save("a", Project(name = "Un"))
        store.save("a", Project(name = "Deux"))
        val main = File(root, "a/${ProjectStore.MAIN}")
        main.writeBytes(main.readBytes().copyOf(main.length().toInt() / 3))
        assertEquals("the backup opens", "Un", store.load("a")!!.name)
        File(root, "a/${ProjectStore.BACKUP}").writeText("{\"name\":")
        try {
            store.load("a")
            fail("a project nobody can read says so")
        } catch (e: IOException) {
            assertFalse(e.message.isNullOrBlank())
        }
    }
}
