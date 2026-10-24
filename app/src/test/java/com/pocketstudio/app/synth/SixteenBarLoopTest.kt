package com.pocketstudio.app.synth

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.time.Duration

/**
 * The piano roll's loop goes up to 16 bars, and its notes reach the engine whole: a start past
 * 4095 ticks and a note longer than that are sent with the extended SetSynthNote format
 * (NoteTrack::packIndex), which the engine decodes as below.
 */
@RunWith(RobolectricTestRunner::class)
class SixteenBarLoopTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    private class NoteSink : CommandSink {
        val notes = ArrayList<Triple<Int, Int, Int>>()  // start, length, pitch as the engine reads them
        var steps = 0
        @Synchronized override fun post(target: Long, type: Int, value: Float, a: Int, b: Int) {
            if (target != 0L) return
            when (type) {
                26 -> {  // CMD_SYNTH_NOTE: NoteTrack::setNote
                    val high = (a shr 10) and 0x3F
                    val start = (b and 0xFFF) or ((high and 0x7) shl 12)
                    val length = ((b shr 12) and 0xFFF) or (((high shr 3) and 0x7) shl 12)
                    notes += Triple(start, length, (b shr 24) and 0x7F)
                }
                28 -> steps = a and 0xFFFF  // CMD_SYNTH_STEPS
            }
        }
        override fun scheduleFrom(target: Long, frame: Long) = Unit
        override fun install(target: Long, kind: CommandSink.Install, index: Int, sample: Long) = Unit
        override fun sampleCreate(data: FloatArray, channels: Int, sampleRate: Int): Long = 1L
        override fun sampleFrames(handle: Long): Long = 0L
        override fun sampleRelease(handle: Long) = Unit
        override fun exportCreate(): Long = 2L
        override fun exportRender(target: Long, path: String, frames: Long, tailFrames: Long, fadeOutFrames: Long, autoTail: Boolean, floatWav: Boolean, sampleRate: Int, bits: Int) =
            ExportResult.Ok
        override fun exportRenderFloat(target: Long, frames: Long, tailFrames: Long): FloatArray = FloatArray(0)
        override fun exportDestroy(target: Long) = Unit
    }

    @After
    fun tearDown() {
        NativeEngine.sink = null
    }

    @Test
    fun aSixteenBarLoopReachesTheEngineWhole() {
        File(app.filesDir, "projects").deleteRecursively()
        val sink = NoteSink()
        NativeEngine.sink = sink
        val vm = RhythmViewModel(app)
        repeat(20) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50)) }
        // The « + » of the loop: bar after bar up to 16.
        repeat(20) { vm.setNoteClipSteps(minOf(NoteClip.MAX_STEPS, vm.project.value.synthNotes.steps + 16)) }
        assertEquals(256, vm.project.value.synthNotes.steps)
        // A note in bar 16, and one held for 12 bars and a half.
        vm.addNote(Note(15 * 384, 96, 69))
        vm.addNote(Note(0, 4800, 57))
        assertEquals(listOf(0 to 4800, 15 * 384 to 96), vm.project.value.synthNotes.notes.map { it.start to it.length })
        assertEquals(256, sink.steps)
        assertTrue("sent ${sink.notes}", sink.notes.contains(Triple(15 * 384, 96, 69)))
        assertTrue("sent ${sink.notes}", sink.notes.contains(Triple(0, 4800, 57)))
        // Past 16 bars, a note goes; the loop stays at 16.
        assertEquals(256, NoteClip(steps = 999).sanitized().steps)
    }
}
