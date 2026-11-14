package com.pocketstudio.app.library

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.model.PackEntry
import com.sun.net.httpserver.HttpServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Audit 18: a sound pack downloaded, checked, unpacked flat, and swapped in only when whole. */
@RunWith(RobolectricTestRunner::class)
class PackInstallTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private lateinit var server: HttpServer
    private val zips = java.util.concurrent.ConcurrentHashMap<String, ByteArray>()

    @Before
    fun start() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { ex ->
            val bytes = zips[ex.requestURI.path.substringAfterLast('/')]
            if (bytes == null) ex.sendResponseHeaders(404, -1) else {
                ex.sendResponseHeaders(200, bytes.size.toLong())
                ex.responseBody.write(bytes)
            }
            ex.close()
        }
        server.start()
        PackStore.releasesBase = "http://127.0.0.1:${server.address.port}/"
        PackStore.dir(app).deleteRecursively()
    }

    @After
    fun stop() {
        server.stop(0)
        PackStore.releasesBase = "https://github.com/benjaminpaquot123-netizen/pocket-studio/releases/download/"
    }

    private fun zip(vararg files: Pair<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z ->
            for ((name, bytes) in files) {
                z.putNextEntry(ZipEntry(name))
                z.write(bytes)
                z.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private fun manifest(id: String, vararg sounds: String) =
        """{"id":"$id","name":"Test","sounds":[${sounds.joinToString(",") { """{"file":"$it","kind":"kick","name":"$it"}""" }}]}""".toByteArray()

    /** [bytes] published as [file], and its index entry. */
    private fun publish(file: String, bytes: ByteArray, id: String = "test-pack", version: Int = 1): PackEntry {
        zips[file] = bytes
        val sha = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        return PackEntry(id, "Test", file, sha, bytes.size.toLong(), version = version)
    }

    @Test
    fun aPackIsInstalledFlatWithOnlyWhatItsManifestLists() {
        val sound = ByteArray(2000) { it.toByte() }
        val entry = publish(
            "test-1.zip",
            zip(
                "pack.json" to manifest("test-pack", "kick.flac"),
                "kick.flac" to sound,
                "extra.bin" to ByteArray(10),
                // A path out of the folder: only its plain name is ever written, inside the pack.
                "../evil.flac" to ByteArray(10),
                "sub/" to ByteArray(0),
            ),
        )
        PackStore.install(app, entry, {}, { false })
        val dir = File(PackStore.dir(app), "test-pack")
        assertTrue(File(dir, "kick.flac").readBytes().contentEquals(sound))
        assertEquals(setOf("kick.flac", "pack.json"), dir.list()!!.toSet())
        assertFalse(File(PackStore.dir(app), "evil.flac").exists())
        assertEquals(listOf("test-pack"), PackStore.installed(app).map { it.id })
        // No leftovers: the .part folder and the zip are gone.
        assertTrue(PackStore.dir(app).list()!!.none { it.endsWith(".part") || it.endsWith(".old") })
        assertTrue(app.cacheDir.list()!!.none { it.startsWith("pack-test-pack") })
    }

    @Test
    fun aBrokenUpdateLeavesThePackThereAsItWas() {
        val v1 = publish("test-1.zip", zip("pack.json" to manifest("test-pack", "kick.flac"), "kick.flac" to ByteArray(100) { 1 }))
        PackStore.install(app, v1, {}, { false })
        val kick = File(PackStore.dir(app), "test-pack/kick.flac")
        // The new version's manifest names another pack: refused, the old one stays.
        val wrong = publish("test-2.zip", zip("pack.json" to manifest("other-pack", "kick.flac"), "kick.flac" to ByteArray(100) { 2 }), version = 2)
        try {
            PackStore.install(app, wrong, {}, { false })
            fail("another pack's manifest")
        } catch (_: IOException) {
            // expected
        }
        assertEquals(1, kick.readBytes()[0].toInt())
        // A pack whose sounds are all missing: refused too.
        val empty = publish("test-3.zip", zip("pack.json" to manifest("test-pack", "snare.flac")), version = 3)
        try {
            PackStore.install(app, empty, {}, { false })
            fail("empty pack")
        } catch (_: IOException) {
            // expected
        }
        assertEquals(1, kick.readBytes()[0].toInt())
        assertTrue(PackStore.dir(app).list()!!.none { it.endsWith(".part") || it.endsWith(".old") })
        // A whole update replaces it, and says its version.
        val v2 = publish("test-4.zip", zip("pack.json" to manifest("test-pack", "kick.flac"), "kick.flac" to ByteArray(100) { 2 }), version = 2)
        PackStore.install(app, v2, {}, { false })
        assertEquals(2, kick.readBytes()[0].toInt())
        assertEquals(2, PackStore.installed(app).single().version)
    }

    @Test
    fun aZipNotTheOneTheIndexNamesIsNeverUnpacked() {
        val entry = publish("test-1.zip", zip("pack.json" to manifest("test-pack", "kick.flac"), "kick.flac" to ByteArray(100)))
        zips["test-1.zip"] = zip("pack.json" to manifest("test-pack", "kick.flac"), "kick.flac" to ByteArray(100) { 9 })
        try {
            PackStore.install(app, entry, {}, { false })
            fail("wrong SHA-256")
        } catch (_: IOException) {
            // expected
        }
        assertFalse(File(PackStore.dir(app), "test-pack").exists())
    }

    @Test
    fun leftoversOfAKilledInstallAreCleanedAndAnOldCopyPutBack() {
        val root = PackStore.dir(app).apply { mkdirs() }
        File(root, "half.part").mkdirs()
        File(root, "kept.old").mkdirs()
        File(root, "kept.old/pack.json").writeText("{}")
        PackStore.cleanLeftovers(app)
        assertFalse(File(root, "half.part").exists())
        assertTrue(File(root, "kept/pack.json").isFile)
    }
}
