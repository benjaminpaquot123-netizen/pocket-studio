package com.pocketstudio.app.export

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.human.BenchEngine
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.LoudnessTarget
import com.pocketstudio.model.MasterPro
import com.pocketstudio.model.PadSample
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.TargetChoice
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Audit ultra round 1, n° 13 to 16 (seats A c, E d, F c), tested without the bench's seats:
 * - « Exporter » of the Mixeur's card (the ViewModel's [RhythmViewModel.exportWav]) makes the file
 *   and its report: the seats' « nothing happens » is not the app's (n° 16);
 * - a club export and a streaming export send the engine different targets (−8 on the Drop, −14 on
 *   the whole song), whatever the master pro held before (n° 13, 14; the bench's engine renders the
 *   same tone for both: its −12,9 LUFS);
 * - the report says the integrated loudness, the true peak (measured on the file when the engine
 *   gives no report: never « — »), the format, the length and the target by its one name (n° 15).
 */
@RunWith(RobolectricTestRunner::class)
class OneTargetExportTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    /** The bench engine, with what each private engine was told: its target (LUFS) and whole-song mode. */
    private class Sink(private val inner: BenchEngine = BenchEngine()) : CommandSink by inner {
        val target = ConcurrentHashMap<Long, Float>()
        val wholeSong = ConcurrentHashMap<Long, Boolean>()
        @Volatile var rendered = -1L

        override fun post(target: Long, type: Int, value: Float, a: Int, b: Int) {
            if (target != 0L && type == 67 && a == 4) this.target[target] = value
            if (target != 0L && type == 190 && a == 2) wholeSong[target] = value > 0.5f
            inner.post(target, type, value, a, b)
        }

        override fun exportRender(
            target: Long, path: String, frames: Long, tailFrames: Long, fadeOutFrames: Long, autoTail: Boolean, floatWav: Boolean,
            sampleRate: Int, bits: Int,
        ): ExportResult {
            rendered = target
            return inner.exportRender(target, path, frames, tailFrames, fadeOutFrames, autoTail, floatWav, sampleRate, bits)
        }
    }

    @After
    fun end() {
        NativeEngine.sink = null
        CurrentProject.set(app, RhythmViewModel.PROJECT_ID)
    }

    private fun until(what: String, ok: () -> Boolean) {
        val end = System.currentTimeMillis() + 60_000
        while (!ok()) {
            check(System.currentTimeMillis() < end) { what }
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(20))
            Thread.sleep(5)
        }
    }

    @Test
    fun clubAndStreamingExportsDifferAndSayIt() {
        val sink = Sink()
        NativeEngine.sink = sink
        val style = StarterWizard.style("house")!!
        val song = StarterWizard.build(style, style.lines.associate { it.role to null as PadSample? })
        // As seat F c left it: « Club » above, « Spotify −14 » in the master pro's row.
        val id = "one-target-" + System.nanoTime()
        ProjectStore(File(app.filesDir, "projects")).save(id, song.copy(name = "Ma Mix", mixer = song.mixer.copy(target = LoudnessTarget.CLUB, pro = MasterPro(targetLufs = -14f))))
        CurrentProject.set(app, id)
        val vm = RhythmViewModel(app)
        until("never opened") { vm.project.value.name == "Ma Mix" }

        fun export(): Long {
            sink.rendered = -1L
            val before = vm.export.value.files
            vm.exportWav()
            // « Exporter » starts (the card turns into its progress bar) and ends with a new file: never nothing said.
            until("never exported: ${vm.export.value.error}") { !vm.export.value.running && vm.export.value.files.isNotEmpty() && vm.export.value.files != before }
            assertTrue(sink.rendered > 0)
            return sink.rendered
        }

        vm.chooseLoudnessTarget(TargetChoice.CLUB)
        assertEquals("the pro value is gone with a chip", null, vm.project.value.mixer.pro?.targetLufs)
        val club = export()
        assertEquals(-8f, sink.target[club]!!, 0f)
        assertEquals(false, sink.wholeSong[club])
        val clubBilan = vm.export.value.bilan!!
        assertEquals("Cible : Club · −8 LUFS", clubBilan.targetLine())
        assertFalse(clubBilan.levelLine(), "—" in clubBilan.levelLine())
        assertTrue(clubBilan.levelLine(), "LUFS intégrés · crête −" in clubBilan.levelLine() && "dBTP" in clubBilan.levelLine())
        assertTrue(clubBilan.fileLine(), clubBilan.fileLine().startsWith("WAV") && " s" in clubBilan.fileLine())
        val clubFile = vm.export.value.files.single().name

        vm.chooseLoudnessTarget(TargetChoice.STREAMING)
        val streaming = export()
        assertEquals(-14f, sink.target[streaming]!!, 0f)
        assertEquals(true, sink.wholeSong[streaming])
        assertEquals("Cible : Streaming · −14 LUFS", vm.export.value.bilan!!.targetLine())
        // The two files say which is which.
        val streamingFile = vm.export.value.files.single().name
        assertTrue(clubFile, "-club-" in clubFile)
        assertTrue(streamingFile, "-streaming-" in streamingFile)
        assertNotEquals(clubFile, streamingFile)
    }
}
