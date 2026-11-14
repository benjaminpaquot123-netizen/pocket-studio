package com.pocketstudio.app.record

import android.app.Application
import android.os.Looper
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.rhythm.RecordTarget
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.SongPart
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.time.Duration
import kotlin.math.PI
import kotlin.math.sin

/**
 * T5-Ab-27 (round 5, seat A b: « la prise micro continue après la fin du morceau »; audit « banc crédible » A.7, F1), the
 * app's side, without the bench: a take of the microphone on the whole song, left running, is KEPT when the engine ends the
 * song by itself. The app arms the song's end (a song take never loops), the engine stops its transport on the last bar line
 * and ends the recording there (Engine.cpp: onStep → stopTransport → finishRecording, state 2), and the app, which reads
 * that state, writes the take onto its track.
 *
 * The engine here is scripted by the test, not the bench's: it only says what a phone's says at each moment.
 */
@RunWith(RobolectricTestRunner::class)
class TakeAtTheSongsEndTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    /** A phone's engine as far as an audio take reads it; [endTheSong] is the song's last bar line. */
    private class EndingEngine : CommandSink {
        private var next = 1L
        @Volatile private var since = -1L
        @Volatile private var preroll = 0
        @Volatile private var ended = -1L
        @Volatile var endArmed = false
            private set
        @Volatile private var swaps = 0
        @Volatile var finished = 0
            private set

        private fun frames(): Long = if (ended >= 0) ended else if (since < 0) 0L else (SystemClock.uptimeMillis() - since) * 48L

        /** The last bar line: the transport stops, the swap is counted, the recording is over with what it holds. */
        fun endTheSong() {
            ended = frames()
            since = -1L
            swaps++
        }

        override fun post(target: Long, type: Int, value: Float, a: Int, b: Int) {
            if (target != 0L) return
            if (type == CMD_SET_TRANSPORT && value > 0f) since = SystemClock.uptimeMillis()
            if (type == CMD_SET_TRANSPORT && value <= 0f) {
                // ■ ends a recording that runs, as the song's end does.
                if (ended < 0 && since >= 0) ended = frames()
                since = -1L
            }
            if (type == CMD_ARM_SCENE_SWAP && a >= 0 && b == 2) endArmed = true
        }

        override fun micStart(onStart: Boolean, seconds: Int, prerollFrames: Int): Long {
            preroll = prerollFrames
            return 7L
        }

        override fun micStatus(handle: Long): FloatArray = when {
            ended >= 0 -> floatArrayOf(2f, ended.toFloat(), 0.01f)
            since < 0 -> floatArrayOf(0f, 0f, 0f)
            else -> floatArrayOf(1f, frames().toFloat(), 0.2f)
        }

        override fun micFinish(handle: Long): FloatArray {
            finished++
            return FloatArray(preroll + frames().toInt()) { i -> if (i < preroll) 0f else (0.25 * sin(2 * PI * 220.0 * i / 48_000)).toFloat() }
        }

        override fun sceneSwaps(): Int = swaps
        override fun playPosition(): Double = if (since < 0) -1.0 else (SystemClock.uptimeMillis() - since) * 8.0 / 1000
        override fun scheduleFrom(target: Long, frame: Long) = Unit
        override fun install(target: Long, kind: CommandSink.Install, index: Int, sample: Long) = Unit
        @Synchronized override fun sampleCreate(data: FloatArray, channels: Int, sampleRate: Int): Long = next++
        override fun sampleFrames(handle: Long): Long = 48_000L
        override fun sampleRelease(handle: Long) = Unit
        @Synchronized override fun exportCreate(): Long = next++
        override fun exportRender(
            target: Long, path: String, frames: Long, tailFrames: Long, fadeOutFrames: Long, autoTail: Boolean, floatWav: Boolean,
            sampleRate: Int, bits: Int,
        ): ExportResult = ExportResult.Cancelled
        override fun exportRenderFloat(target: Long, frames: Long, tailFrames: Long): FloatArray = FloatArray(0)
        override fun exportDestroy(target: Long) = Unit

        private companion object {
            const val CMD_SET_TRANSPORT = 5
            const val CMD_ARM_SCENE_SWAP = 40
        }
    }

    @After
    fun end() {
        NativeEngine.sink = null
    }

    private fun pass(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    private fun until(what: String, ok: () -> Boolean) {
        val end = System.currentTimeMillis() + 30_000
        while (!ok()) {
            check(System.currentTimeMillis() < end) { what }
            pass(20)
            Thread.sleep(5)
        }
    }

    @Test
    fun aTakeLeftRunningIsKeptWhenTheEngineEndsTheSong() {
        val engine = EndingEngine()
        NativeEngine.sink = engine
        val id = "take-end-${System.nanoTime()}"
        ProjectStore(File(app.filesDir, "projects")).save(id, Project(song = listOf(SongPart(0))))
        CurrentProject.set(app, id)
        val vm = RhythmViewModel(app)
        until("the project never opened") { vm.project.value.song.size == 1 }
        vm.setRecordTarget(RecordTarget(song = true, fromPart = 0))
        val track = vm.micTakeTrack()!!
        vm.recordAudio(track)
        until("the take never recorded") { vm.audioRecording.value?.waiting == false }
        assertTrue("a take on the whole song", vm.audioRecording.value!!.song)
        until("the app never armed the song's end (a song take never loops)") { engine.endArmed }
        pass(3_000)
        assertTrue("left running: still recording", vm.audioRecording.value != null && vm.transport.value.playing)
        engine.endTheSong()
        until("the app never ended the take") { vm.audioRecording.value == null }
        until("the take was never written onto its track (notice: ${vm.notice.value})") {
            vm.project.value.audioTracks.firstOrNull { it.id == track }?.clip != null && !vm.takeSaving.value
        }
        val clip = vm.project.value.audioTracks.first { it.id == track }.clip!!
        assertTrue("a clip of the song", clip.song)
        assertTrue("about the 3 s recorded before the end: ${clip.seconds} s", clip.seconds in 2.5..3.6)
        assertFalse("the app stopped with the song", vm.transport.value.playing)
        assertEquals("the take was read once from the engine", 1, engine.finished)
        assertNull("no « trop courte », no « rien entendu »: ${vm.notice.value}", vm.notice.value?.takeIf { "prise" in it.lowercase() || "micro" in it.lowercase() })
    }
}
