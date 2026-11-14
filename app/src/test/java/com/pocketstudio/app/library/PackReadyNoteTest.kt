package com.pocketstudio.app.library

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.model.PackEntry
import com.pocketstudio.model.PackIndex
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Audit ultra tour 0 (seat a): « Télécharger les sons du style » asked for four packs at once and the note said
 * only « C'est prêt : le pack « Roland Juno-106 » est sur ton téléphone. », the last one to arrive. The packs that
 * come together are named together.
 */
@RunWith(RobolectricTestRunner::class)
class PackReadyNoteTest {
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

    private fun publish(id: String, name: String): PackEntry {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z ->
            z.putNextEntry(ZipEntry("pack.json"))
            z.write("""{"id":"$id","name":"$name","sounds":[{"file":"kick.flac","kind":"kick","name":"Kick"}]}""".toByteArray())
            z.closeEntry()
            z.putNextEntry(ZipEntry("kick.flac"))
            z.write(ByteArray(200) { it.toByte() })
            z.closeEntry()
        }
        val bytes = out.toByteArray()
        zips["$id.zip"] = bytes
        val sha = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        return PackEntry(id, name, "$id.zip", sha, bytes.size.toLong())
    }

    @Test
    fun packsThatArriveTogetherAreNamedTogether() {
        val entries = listOf(publish("tr909", "Roland TR-909"), publish("juno", "Roland Juno-106"), publish("house", "House"))
        val vm = SoundLibraryViewModel(app)
        @Suppress("UNCHECKED_CAST")
        val packs = SoundLibraryViewModel::class.java.getDeclaredField("_packs").apply { isAccessible = true }.get(vm) as MutableStateFlow<PacksUi>
        packs.value = packs.value.copy(index = IndexState.Ready(PackIndex(entries)))
        entries.forEach { vm.downloadPack(it.id) }
        val until = System.currentTimeMillis() + 30_000
        while (vm.packs.value.downloading.isNotEmpty() && System.currentTimeMillis() < until) {
            Thread.sleep(20)
            shadowOf(Looper.getMainLooper()).idle()
        }
        assertEquals(setOf("tr909", "juno", "house"), vm.packs.value.installed.map { it.id }.toSet())
        val note = vm.packs.value.readyNote.orEmpty()
        assertTrue(note, entries.all { it.name in note })

        // One alone, later: named alone, as before.
        val one = publish("techno", "Techno")
        packs.value = packs.value.copy(index = IndexState.Ready(PackIndex(entries + one)))
        vm.downloadPack("techno")
        while (vm.packs.value.downloading.isNotEmpty() && System.currentTimeMillis() < until) {
            Thread.sleep(20)
            shadowOf(Looper.getMainLooper()).idle()
        }
        assertEquals("C'est prêt : le pack « Techno » est sur ton téléphone.", vm.packs.value.readyNote)
    }
}
