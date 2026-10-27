package com.pocketstudio.app.remix

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Looper
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.audio.AudioDecoder
import com.pocketstudio.app.download.VerifiedFile
import com.pocketstudio.app.report.AppLog
import com.pocketstudio.separation.Demucs
import com.pocketstudio.separation.SeparationResult
import com.pocketstudio.ui.PocketStudioTheme
import com.sun.net.httpserver.HttpServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.net.InetSocketAddress
import java.security.MessageDigest
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Audit 18 and 02: the separation service run with a stand-in for the decoder and the model
 * (progress, result, cancel, the model's errors in the bug log, an interrupted job found again,
 * the model's download resumed after a cut).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class SeparationServiceTest {
    @get:Rule
    val compose = createComposeRule()

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val originalSpec = SeparationModel.SPEC
    private val modelBytes = ByteArray(50_000) { (it % 251).toByte() }

    /** The stand-in: [result] from the model, a song of [seconds]; hooks to hold it mid-way. */
    private inner class FakeWork(
        val result: Pair<SeparationResult, String> = SeparationResult.Ok to "",
        val seconds: Double = 12.0,
    ) : SeparationService.Work {
        @Volatile var markerDuringModel = false
        val progressSeen = java.util.Collections.synchronizedList(ArrayList<SeparationState>())
        var onAnalyse: () -> Unit = {}
        override val available = true

        override fun decode(context: Context, uri: Uri, target: File, maxSeconds: Double, isCancelled: () -> Boolean): AudioDecoder.RawAudio {
            val frames = (seconds * 44_100).toLong()
            target.parentFile?.mkdirs()
            target.writeBytes(ByteArray(4096) { it.toByte() })
            return AudioDecoder.RawAudio(target, 2, 44_100, frames)
        }

        override fun separate(model: File, raw: AudioDecoder.RawAudio, outDir: File, progress: Demucs.Progress): Pair<SeparationResult, String> {
            markerDuringModel = SeparationJob.marker(app).isFile
            outDir.mkdirs()
            for (p in listOf(0.25f, 0.5f, 1f)) {
                progress.onProgress(p)
                progressSeen += SeparationJob.state.value
            }
            if (result.first == SeparationResult.Ok) Demucs.STEMS.forEach { File(outDir, "$it.wav").writeBytes(ByteArray(64)) }
            return result
        }

        override fun analyse(dir: File): SongInfo {
            onAnalyse()
            return SongInfo(bpm = 124.0, firstBeat = 0.1, root = 9, minor = true)
        }

        override fun availableMemory(context: Context) = Long.MAX_VALUE
    }

    private fun installFakeModel() {
        val sha = MessageDigest.getInstance("SHA-256").digest(modelBytes).joinToString("") { "%02x".format(it) }
        SeparationModel.SPEC = VerifiedFile("http://127.0.0.1:1/htdemucs.onnx", "htdemucs.onnx", sha, modelBytes.size.toLong())
        SeparationModel.file(app).apply { parentFile!!.mkdirs() }.writeBytes(modelBytes)
    }

    @Before
    fun setUp() {
        SeparationJob.set(SeparationState.Idle)
        AppLog.clear()
    }

    @After
    fun tearDown() {
        SeparationService.work = realWork
        SeparationModel.SPEC = originalSpec
        SeparationJob.set(SeparationState.Idle)
        listOf("stems", "models", "separation", "projects").forEach { File(app.filesDir, it).deleteRecursively() }
    }

    private val realWork = SeparationService.work

    private fun waitUntil(what: String, check: () -> Boolean) {
        val until = System.currentTimeMillis() + 30_000
        while (!check()) {
            assertTrue(what, System.currentTimeMillis() < until)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(20))
            Thread.sleep(5)
        }
    }

    /** The intent the app sent to the service (SeparationService.separate…), run on a fresh service. */
    private fun runNextStart(): ServiceController<SeparationService> {
        val intent = shadowOf(app).nextStartedService
        return Robolectric.buildService(SeparationService::class.java, intent).create().startCommand(0, 1)
    }

    private fun separate(work: FakeWork): ServiceController<SeparationService> {
        SeparationService.work = work
        SeparationService.separate(app, Uri.parse("content://test/chanson.mp3"), "Chanson")
        return runNextStart()
    }

    @Test
    fun aSongIsSeparatedWithItsProgressAndKept() {
        installFakeModel()
        val work = FakeWork()
        separate(work)
        waitUntil("the separation never ended") { SeparationJob.state.value is SeparationState.Done }
        val done = SeparationJob.state.value as SeparationState.Done
        assertEquals("Chanson", done.set.name)
        assertEquals(12.0, done.set.seconds, 0.01)
        assertEquals(124.0, done.set.info.bpm, 0.0)
        assertTrue(work.progressSeen.any { it is SeparationState.Separating && it.progress == 0.5f })
        // While it ran the marker said so; once done, nothing is left to be « interrupted ».
        assertTrue(work.markerDuringModel)
        assertFalse(SeparationJob.marker(app).exists())
        assertEquals(listOf("Chanson"), StemStore.list(app).map { it.name })
    }

    @Test
    fun theModelsOwnErrorReachesTheBugLog() {
        installFakeModel()
        separate(FakeWork(SeparationResult.ModelError to "Ort: Non-zero status code returned while running Conv node"))
        waitUntil("the failure never came") { SeparationJob.state.value is SeparationState.Failed }
        assertEquals("Le modèle de séparation a échoué.", (SeparationJob.state.value as SeparationState.Failed).message)
        assertTrue(AppLog.dump(), AppLog.dump().contains("Non-zero status code"))
        assertTrue(StemStore.list(app).isEmpty())
        assertFalse(SeparationJob.marker(app).exists())
    }

    @Test
    fun runningOutOfMemorySaysSo() {
        installFakeModel()
        separate(FakeWork(SeparationResult.OutOfMemory to "mémoire insuffisante"))
        waitUntil("the failure never came") { SeparationJob.state.value is SeparationState.Failed }
        assertTrue((SeparationJob.state.value as SeparationState.Failed).message.startsWith("Mémoire insuffisante pendant la séparation"))
        assertTrue(AppLog.dump().contains("OutOfMemory"))
    }

    @Test
    fun cancelDuringTheTempoAndKeyKeepsNothing() {
        // Audit 02 M12: « Annuler » shown during « Tempo et tonalité… » is obeyed.
        installFakeModel()
        val analysing = CountDownLatch(1)
        val proceed = CountDownLatch(1)
        val work = FakeWork().apply {
            onAnalyse = {
                analysing.countDown()
                proceed.await(20, TimeUnit.SECONDS)
            }
        }
        val service = separate(work)
        waitUntil("the analysis never started") { analysing.count == 0L }
        assertTrue(SeparationJob.state.value is SeparationState.Analysing)
        SeparationService.cancel(app)
        service.withIntent(shadowOf(app).nextStartedService).startCommand(0, 2)
        proceed.countDown()
        waitUntil("the job never ended") { !SeparationJob.state.value.busy }
        assertEquals(SeparationState.Idle, SeparationJob.state.value)
        assertTrue(StemStore.list(app).isEmpty())
        assertTrue(File(app.filesDir, "stems").listFiles().orEmpty().none { it.name.endsWith(".part") })
    }

    @Test
    fun aSeparationAndroidStoppedIsSaidAndCanStartAgain() {
        // Audit 02 G2: the app was killed mid-separation: its marker is still there on the next start.
        SeparationJob.markRunning(app, "Ma chanson", "content://test/ma-chanson.mp3")
        SeparationJob.recover(app)
        val s = SeparationJob.state.value as SeparationState.Interrupted
        assertEquals("Ma chanson", s.name)
        assertEquals("content://test/ma-chanson.mp3", s.uri)
        assertFalse(SeparationJob.marker(app).exists())
        // Once only.
        SeparationJob.set(SeparationState.Idle)
        SeparationJob.recover(app)
        assertEquals(SeparationState.Idle, SeparationJob.state.value)
        // A job running in this process is never taken for an interrupted one.
        SeparationJob.markRunning(app, "En cours", null)
        SeparationJob.set(SeparationState.Separating("En cours", 0.3f))
        SeparationJob.recover(app)
        assertTrue(SeparationJob.state.value is SeparationState.Separating)
        SeparationJob.clearRunning(app)

        var retried = 0
        compose.setContent {
            PocketStudioTheme {
                RemixScreen(RemixUi(true, s, emptyList()), RemixActions(retry = { retried++ }))
            }
        }
        compose.onNodeWithTag("remix_interrupted").assertExists()
        compose.onNodeWithTag("remix_retry").performScrollTo().performClick()
        assertEquals(1, retried)
    }

    @Test
    fun theModelsDownloadResumesAfterACut() {
        // Audit 02 M3: 174 MB never start over from zero after the Wi-Fi drops.
        val ranges = java.util.Collections.synchronizedList(ArrayList<String?>())
        var cut = true
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/htdemucs.onnx") { ex ->
            val range = ex.requestHeaders.getFirst("Range")
            ranges += range
            if (range != null) {
                val from = range.removePrefix("bytes=").removeSuffix("-").toInt()
                ex.sendResponseHeaders(206, (modelBytes.size - from).toLong())
                ex.responseBody.write(modelBytes, from, modelBytes.size - from)
            } else {
                ex.sendResponseHeaders(200, modelBytes.size.toLong())
                ex.responseBody.write(modelBytes, 0, if (cut) modelBytes.size / 2 else modelBytes.size)
                ex.responseBody.flush()
            }
            ex.close()
        }
        server.start()
        try {
            val sha = MessageDigest.getInstance("SHA-256").digest(modelBytes).joinToString("") { "%02x".format(it) }
            SeparationModel.SPEC = VerifiedFile("http://127.0.0.1:${server.address.port}/htdemucs.onnx", "htdemucs.onnx", sha, modelBytes.size.toLong())
            SeparationService.downloadModel(app)
            runNextStart()
            waitUntil("the cut never came") { SeparationJob.state.value is SeparationState.Failed }
            assertFalse(SeparationModel.isInstalled(app))
            cut = false
            SeparationJob.set(SeparationState.Idle)
            SeparationService.downloadModel(app)
            runNextStart()
            waitUntil("the download never ended") { SeparationModel.isInstalled(app) && !SeparationJob.state.value.busy }
            assertEquals("bytes=${modelBytes.size / 2}-", ranges.last())
            assertTrue(SeparationModel.file(app).readBytes().contentEquals(modelBytes))
        } finally {
            server.stop(0)
        }
    }
}
