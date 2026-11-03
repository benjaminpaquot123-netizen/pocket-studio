package com.pocketstudio.app.audit3

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.human.BenchEngine
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.io.IOException
import java.time.Duration

/**
 * Audit stage 3, « écriture atomique » for the export: a render cut at each way it can end badly
 * (the engine's write error, a throw mid-render, the phone full) leaves no file behind, says so,
 * and the next export goes through.
 */
@RunWith(RobolectricTestRunner::class)
class AtomicExportTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    /** The bench engine, whose next render [fail]s as asked after writing half a file. */
    private class Failing(private val inner: CommandSink = BenchEngine()) : CommandSink by inner {
        var fail: String? = null
        override fun exportRender(
            target: Long, path: String, frames: Long, tailFrames: Long, fadeOutFrames: Long, autoTail: Boolean, floatWav: Boolean,
            sampleRate: Int, bits: Int,
        ): ExportResult {
            when (fail) {
                null -> return inner.exportRender(target, path, frames, tailFrames, fadeOutFrames, autoTail, floatWav, sampleRate, bits)
                "io" -> { File(path).writeBytes(ByteArray(1000)); return ExportResult.IoError }
                else -> { File(path).writeBytes(ByteArray(1000)); throw IOException("disque arraché") }
            }
        }
    }

    @After
    fun end() {
        NativeEngine.sink = null
        CurrentProject.set(app, RhythmViewModel.PROJECT_ID)
    }

    private fun until(what: String, ok: () -> Boolean) {
        val end = System.currentTimeMillis() + 30_000
        while (!ok()) {
            check(System.currentTimeMillis() < end) { what }
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(20))
            Thread.sleep(5)
        }
    }

    @Test
    fun aRenderCutLeavesNoFileAndTheNextGoesThrough() {
        val sink = Failing()
        NativeEngine.sink = sink
        val id = "atomic-export-" + System.nanoTime()
        ProjectStore(File(app.filesDir, "projects")).save(id, Project(name = "Coupure"))
        CurrentProject.set(app, id)
        val vm = RhythmViewModel(app)
        until("never opened") { vm.project.value.name == "Coupure" }
        val exports = File(app.cacheDir, "exports")
        for (way in listOf("io", "throw")) {
            sink.fail = way
            vm.exportWav()
            until("$way: never ended") { !vm.export.value.running && vm.export.value.error != null }
            assertNotNull(vm.export.value.error)
            assertTrue("$way: no file left: ${exports.list()?.toList()}", exports.listFiles().orEmpty().none { it.extension == "wav" })
            assertTrue(vm.export.value.files.isEmpty())
        }
        sink.fail = null
        vm.exportWav()
        until("never exported") { !vm.export.value.running && vm.export.value.files.isNotEmpty() }
        assertEquals(1, vm.export.value.files.size)
        assertTrue(vm.export.value.files.single().isFile)
    }
}
