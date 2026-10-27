package com.pocketstudio.app.download

import com.sun.net.httpserver.HttpServer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.security.MessageDigest

/**
 * Audit 18: the big downloads (the separation model's 174 MB, the sound packs) checked by size and
 * SHA-256, resumed with HTTP Range after a cut, never kept when wrong. A stand-in server on this machine.
 */
class VerifiedDownloadTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val body = ByteArray(300_000) { (it * 31 + it / 7).toByte() }
    private val sha = MessageDigest.getInstance("SHA-256").digest(body).joinToString("") { "%02x".format(it) }
    private lateinit var server: HttpServer

    /** How the server answers: cut the next full answer half-way, honour Range or not, refuse it. */
    @Volatile private var cutNext = false
    @Volatile private var honourRange = true
    @Volatile private var refuseRange = false
    private val ranges = java.util.Collections.synchronizedList(ArrayList<String?>())

    @Before
    fun start() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/model.onnx") { ex ->
            val range = ex.requestHeaders.getFirst("Range")
            ranges += range
            try {
                if (range != null && refuseRange) {
                    ex.sendResponseHeaders(416, -1)
                } else if (range != null && honourRange) {
                    val from = range.removePrefix("bytes=").removeSuffix("-").toInt()
                    ex.responseHeaders.add("Content-Range", "bytes $from-${body.size - 1}/${body.size}")
                    ex.sendResponseHeaders(206, (body.size - from).toLong())
                    ex.responseBody.write(body, from, body.size - from)
                } else {
                    ex.sendResponseHeaders(200, body.size.toLong())
                    if (cutNext) {
                        cutNext = false
                        // Half of it, then the connection drops (the phone left the Wi-Fi).
                        ex.responseBody.write(body, 0, body.size / 2)
                        ex.responseBody.flush()
                        Thread.sleep(50)
                        ex.close()
                        return@createContext
                    }
                    ex.responseBody.write(body)
                }
            } catch (_: IOException) {
                // The client went away (a cancel): nothing to do.
            }
            ex.close()
        }
        server.start()
    }

    @After
    fun stop() = server.stop(0)

    private fun spec(sha256: String = sha) =
        VerifiedFile("http://127.0.0.1:${server.address.port}/model.onnx", "model.onnx", sha256, body.size.toLong())

    @Test
    fun aWholeDownloadIsCheckedThenInPlace() {
        val target = File(tmp.root, "models/model.onnx")
        val progress = ArrayList<Float>()
        VerifiedDownload.fetch(spec(), target, { progress += it })
        assertArrayEquals(body, target.readBytes())
        assertEquals(1f, progress.last(), 0f)
        assertTrue(VerifiedDownload.verify(spec(), target))
        assertFalse(File(target.parentFile, "model.onnx.part").exists())
    }

    @Test
    fun aCutDownloadResumesWithARange() {
        val target = File(tmp.root, "model.onnx")
        val part = File(tmp.root, "model.onnx.part")
        cutNext = true
        try {
            VerifiedDownload.fetch(spec(), target, {}, resume = true)
            fail("the cut must be reported")
        } catch (_: IOException) {
            // expected: « Relance pour reprendre »
        }
        assertFalse(target.exists())
        val kept = part.length()
        assertTrue("what came is kept ($kept)", kept in 1 until body.size)
        // The next try asks for the rest only and ends with the whole, right file.
        VerifiedDownload.fetch(spec(), target, {}, resume = true)
        assertEquals("bytes=$kept-", ranges.last())
        assertArrayEquals(body, target.readBytes())
        assertFalse(part.exists())
    }

    @Test
    fun withoutResumeACutStartsOver() {
        val target = File(tmp.root, "model.onnx")
        cutNext = true
        runCatching { VerifiedDownload.fetch(spec(), target, {}) }
        assertFalse(File(tmp.root, "model.onnx.part").exists())
    }

    @Test
    fun aServerIgnoringTheRangeSendsItAllAgain() {
        val target = File(tmp.root, "model.onnx")
        File(tmp.root, "model.onnx.part").writeBytes(body.copyOf(1000))
        honourRange = false
        VerifiedDownload.fetch(spec(), target, {}, resume = true)
        assertEquals("bytes=1000-", ranges.last())
        assertArrayEquals(body, target.readBytes())
    }

    @Test
    fun aRefusedRangeDropsWhatCame() {
        val target = File(tmp.root, "model.onnx")
        val part = File(tmp.root, "model.onnx.part")
        part.writeBytes(body.copyOf(1000))
        refuseRange = true
        try {
            VerifiedDownload.fetch(spec(), target, {}, resume = true)
            fail("416 must fail")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("416"))
        }
        assertFalse(part.exists())
        // The next try starts from zero and works.
        refuseRange = false
        VerifiedDownload.fetch(spec(), target, {}, resume = true)
        assertArrayEquals(body, target.readBytes())
    }

    @Test
    fun aWrongFileIsNeverKept() {
        val target = File(tmp.root, "model.onnx")
        try {
            VerifiedDownload.fetch(spec("0".repeat(64)), target, {}, resume = true)
            fail("a wrong SHA-256 must fail")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("empreinte"))
        }
        assertFalse(target.exists())
        assertFalse(File(tmp.root, "model.onnx.part").exists())
        // A file on disk damaged since: verify says so.
        target.writeBytes(body.copyOf().also { it[10] = (it[10] + 1).toByte() })
        assertFalse(VerifiedDownload.verify(spec(), target))
    }

    @Test
    fun aCancelStopsAndDropsWhatCame() {
        val target = File(tmp.root, "model.onnx")
        var calls = 0
        try {
            VerifiedDownload.fetch(spec(), target, { calls++ }, isCancelled = { calls > 0 }, resume = true)
            fail("cancelled")
        } catch (_: VerifiedDownload.CancelledException) {
            // expected
        }
        assertFalse(target.exists())
        assertFalse(File(tmp.root, "model.onnx.part").exists())
    }
}
