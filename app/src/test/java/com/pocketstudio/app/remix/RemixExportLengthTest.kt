package com.pocketstudio.app.remix

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.rhythm.ExportFormat
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.Remix
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong

/**
 * Audit 02 G1: « Partager mon morceau » and « Exporter » of a remixed song without a Structure
 * render the whole song (3:30 here), not the 8 looped bars (16 s) of the scene.
 */
@RunWith(RobolectricTestRunner::class)
class RemixExportLengthTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val store = ProjectStore(File(app.filesDir, "projects"))

    /** The engine's stand-in: notes how long each render was asked to be. */
    private class Sink : CommandSink {
        private var next = 1L
        val frames = AtomicLong(-1)
        override fun post(target: Long, type: Int, value: Float, a: Int, b: Int) = Unit
        override fun scheduleFrom(target: Long, frame: Long) = Unit
        override fun install(target: Long, kind: CommandSink.Install, index: Int, sample: Long) = Unit
        @Synchronized override fun sampleCreate(data: FloatArray, channels: Int, sampleRate: Int): Long = next++
        override fun sampleFrames(handle: Long): Long = 4800L
        override fun sampleRelease(handle: Long) = Unit
        @Synchronized override fun exportCreate(): Long = next++
        override fun exportRender(target: Long, path: String, frames: Long, tailFrames: Long, fadeOutFrames: Long, autoTail: Boolean, floatWav: Boolean, sampleRate: Int, bits: Int): ExportResult {
            this.frames.set(frames)
            return ExportResult.Cancelled
        }
        override fun exportRenderFloat(target: Long, frames: Long, tailFrames: Long): FloatArray {
            this.frames.set(frames)
            return FloatArray(0)
        }
        override fun exportDestroy(target: Long) = Unit
    }

    private val sink = Sink()

    @After
    fun tearDown() {
        NativeEngine.sink = null
        com.pocketstudio.app.projects.CurrentProject.set(app, RhythmViewModel.PROJECT_ID)
    }

    private fun waitUntil(what: String, check: () -> Boolean) {
        val until = System.currentTimeMillis() + 60_000
        while (!check()) {
            assertTrue(what, System.currentTimeMillis() < until)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
            Thread.sleep(5)
        }
    }

    private fun open(p: Project): RhythmViewModel {
        val id = "remix-export-${System.nanoTime()}"
        store.save(id, p.copy(name = id))
        com.pocketstudio.app.projects.CurrentProject.set(app, id)
        NativeEngine.sink = sink
        val vm = RhythmViewModel(app)
        waitUntil("the project never opened") { vm.project.value.name == id }
        return vm
    }

    private val remixed = Project(tempoBpm = 120.0, remix = Remix("abc", "Chanson", songBpm = 120.0, seconds = 210.0))

    @Test
    fun sharingARemixRendersTheWholeSong() {
        val vm = open(remixed)
        vm.shareSong(ExportFormat.Wav)
        waitUntil("nothing was rendered") { sink.frames.get() >= 0 }
        // 105 bars of 2 s: the whole 3:30, not 8 bars (16 s).
        assertEquals(OfflineExport.framesFor(105, 120.0), sink.frames.get())
    }

    @Test
    fun theExportCardOffersTheWholeSongFirst() {
        val vm = open(remixed)
        waitUntil("the export settings never knew the song") { vm.export.value.audioBars == 105 }
        assertEquals(105, vm.export.value.bars)
        vm.exportWav()
        waitUntil("nothing was rendered") { sink.frames.get() >= 0 }
        assertEquals(OfflineExport.framesFor(105, 120.0), sink.frames.get())
        // A length beyond the 64 bars of a loop can be picked, up to the song's end.
        vm.setExportBars(100)
        waitUntil("the length picked") { vm.export.value.bars == 100 }
    }
}
