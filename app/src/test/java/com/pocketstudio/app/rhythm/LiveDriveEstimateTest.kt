package com.pocketstudio.app.rhythm

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.SongPart
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.time.Duration

/**
 * S6 son (G5) and its review: the live gain is estimated offline only for an assistant's song aimed
 * at its Drop (mix version 2 and later); a project made before (mix version 1) keeps the live
 * estimate as before and is never rendered for it. S7: the estimate renders for its report only
 * (no array of samples handed back).
 */
@RunWith(RobolectricTestRunner::class)
class LiveDriveEstimateTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    /** Counts the private engines and their renders; reports a Drop-aimed render with a +3.5 dB drive. */
    private class EstimateSink : CommandSink {
        @Volatile var exports = 0
        @Volatile var reportOnly = 0
        @Volatile var arrays = 0
        @Volatile var knownDrive: Float? = null
        @Volatile var knownLevels = 0
        @Volatile var knownLevel: Float? = null
        private var next = 1L
        override fun post(target: Long, type: Int, value: Float, a: Int, b: Int) {
            if (target == 0L && type == CMD_MASTER && a == 8) knownDrive = value
            if (target == 0L && type == CMD_MASTER && a == 10) {
                knownLevels++
                knownLevel = value
            }
        }
        override fun scheduleFrom(target: Long, frame: Long) = Unit
        override fun install(target: Long, kind: CommandSink.Install, index: Int, sample: Long) = Unit
        @Synchronized override fun sampleCreate(data: FloatArray, channels: Int, sampleRate: Int): Long = next++
        override fun sampleFrames(handle: Long): Long = 0L
        override fun sampleRelease(handle: Long) = Unit
        @Synchronized override fun exportCreate(): Long {
            exports++
            return next++
        }
        override fun exportRender(target: Long, path: String, frames: Long, tailFrames: Long, fadeOutFrames: Long, autoTail: Boolean, floatWav: Boolean, sampleRate: Int, bits: Int) =
            ExportResult.Ok
        override fun exportRenderFloat(target: Long, frames: Long, tailFrames: Long): FloatArray {
            arrays++
            return FloatArray(2 * 4800)
        }
        override fun exportRenderReportOnly(target: Long, frames: Long, tailFrames: Long): Boolean {
            reportOnly++
            return true
        }
        override fun exportReport(target: Long): FloatArray = floatArrayOf(-9f, -1.2f, 8f, 48000f, 3.5f, 0.5f, -8f, 1f)
        override fun exportDestroy(target: Long) = Unit
    }

    private val sink = EstimateSink()

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
        val id = "drive-${System.nanoTime()}"
        ProjectStore(File(app.filesDir, "projects")).save(id, p)
        com.pocketstudio.app.projects.CurrentProject.set(app, id)
        NativeEngine.sink = sink
        val vm = RhythmViewModel(app)
        waitUntil("the project never opened") { vm.project.value.song.size == p.song.size && vm.project.value.mixVersion == p.mixVersion }
        return vm
    }

    /** An assistant's song (a style, a song) of mix version [mix]. */
    private fun song(mix: Int) = Project(
        style = "techno",
        mixVersion = mix,
        synthNotes = NoteClip(notes = listOf(Note(0, 4, 60))),
        song = listOf(SongPart(0), SongPart(0)),
    )

    private fun estimate(vm: RhythmViewModel) {
        val done = Thread { runBlocking { vm.awaitLiveDriveForTest() } }
        done.start()
        waitUntil("the estimate never ended") { !done.isAlive }
    }

    @Test
    fun aProjectOfMixVersion1IsNeverEstimated() {
        val vm = open(song(Project.MIX_V1))
        estimate(vm)
        assertEquals("a private engine made for a mix version 1 project", 0, sink.exports)
        assertEquals(0, sink.reportOnly + sink.arrays)
        assertNull(vm.liveDriveDb)
        assertNull(sink.knownDrive)
        assertEquals(0, sink.knownLevels)
    }

    @Test
    fun aSongOfMixVersion2GetsItsDriveAloneAsInS6() {
        val vm = open(song(Project.MIX_V2))
        estimate(vm)
        waitUntil("no drive estimated") { vm.liveDriveDb != null }
        assertEquals(3.5f, sink.knownDrive!!, 1e-4f)
        assertEquals("a known level sent for a mix version 2 song", 0, sink.knownLevels)
    }

    @Test
    fun aDropAimedSongIsEstimatedFromItsReportOnly() {
        val vm = open(song(Project.MIX_V4))
        estimate(vm)
        waitUntil("no drive estimated") { vm.liveDriveDb != null }
        assertEquals(3.5f, vm.liveDriveDb!!, 1e-4f)
        assertEquals(3.5f, sink.knownDrive!!, 1e-4f)
        assertTrue("${sink.reportOnly} report-only renders", sink.reportOnly >= 1)
        assertEquals("an array of samples rendered for the estimate", 0, sink.arrays)
        // S7: the loud part's level before the drive goes first (loud −8 − drive 3.5 + loss 0.5).
        assertEquals(-11f, sink.knownLevel!!, 1e-4f)
        assertEquals(-11f, vm.liveDriveLevel, 1e-4f)
    }

    /**
     * REVIEW S6: a sound changed after the estimate (here the synth's cut-off) is estimated again once
     * the edits stop for a moment, not before; a second change meanwhile makes one estimate, not two.
     */
    @Test
    fun aChangeOfSoundIsEstimatedAgainOnceSettled() {
        val vm = open(song(Project.MIX_V4))
        estimate(vm)
        waitUntil("no drive estimated") { vm.liveDriveDb != null }
        val first = sink.reportOnly + sink.arrays
        vm.setSynthParam(com.pocketstudio.model.SynthPatch.CUTOFF, 0.3f)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500))
        vm.setSynthParam(com.pocketstudio.model.SynthPatch.CUTOFF, 0.35f)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500))
        assertEquals("estimated again while the sound was still being edited", first, sink.reportOnly + sink.arrays)
        waitUntil("the new sound was never estimated") { sink.reportOnly + sink.arrays > first }
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(3000))
        assertEquals("one estimate for the two edits", first + 1, sink.reportOnly + sink.arrays)
    }

    private companion object {
        /** NativeEngine's CMD_MASTER (setKnownDrive: its a = 8). */
        const val CMD_MASTER = 67
    }
}
