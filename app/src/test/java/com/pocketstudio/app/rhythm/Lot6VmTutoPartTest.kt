package com.pocketstudio.app.rhythm

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.switchScene
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import android.os.Looper
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.time.Duration

/**
 * Lot 6, N25 (seat B b-suite #178): the tutorial's « Aller dans la partie « Build-up » » while the song plays shows that
 * part, or says it comes at the next bar: the screens following the song took it straight back to the part playing.
 */
@RunWith(RobolectricTestRunner::class)
class Lot6VmTutoPartTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    private class Sink : CommandSink {
        private var next = 1L
        @Volatile var swaps = 0
        override fun sceneSwaps(): Int = swaps
        override fun post(target: Long, type: Int, value: Float, a: Int, b: Int) = Unit
        override fun scheduleFrom(target: Long, frame: Long) = Unit
        override fun install(target: Long, kind: CommandSink.Install, index: Int, sample: Long) = Unit
        @Synchronized override fun sampleCreate(data: FloatArray, channels: Int, sampleRate: Int): Long = next++
        override fun sampleFrames(handle: Long): Long = 48_000L
        override fun sampleRelease(handle: Long) = Unit
        @Synchronized override fun exportCreate(): Long = next++
        override fun exportRender(target: Long, path: String, frames: Long, tailFrames: Long, fadeOutFrames: Long, autoTail: Boolean, floatWav: Boolean, sampleRate: Int, bits: Int) =
            ExportResult.Cancelled
        override fun exportRenderFloat(target: Long, frames: Long, tailFrames: Long): FloatArray = FloatArray(0)
        override fun exportDestroy(target: Long) = Unit
    }

    @After
    fun tearDown() {
        NativeEngine.sink = null
    }

    @Test
    fun goingToAPartWhileTheSongPlaysShowsItOrSaysItComes() {
        File(app.filesDir, "projects").deleteRecursively()
        val sink = Sink()
        NativeEngine.sink = sink
        val vm = RhythmViewModel(app)
        vm.toggleHit(0, 0)
        vm.duplicateScene()
        vm.toggleHit(0, 4)
        vm.duplicateScene()
        vm.toggleHit(0, 8)
        vm.selectScene(0)
        // The song A B; the tutorial's « Build-up » is C, not in it yet.
        listOf(0, 1).forEach(vm::addSongPart)
        vm.togglePlay()
        assertTrue(vm.transport.value.playing)
        assertTrue("the song plays", vm.sessionUi.value.songIndex == 0)
        // « Aller dans la partie … » (TutoChecks.fulfil: the part switched to, through tutoEdit).
        vm.tutoEdit { switchScene(2) }
        val waiting = vm.sessionUi.value.pending
        // The engine goes on to what waits (the song's next part, B, unless C was put first).
        sink.swaps++
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))
        val shown = vm.project.value.scene
        assertEquals("on part $shown after the next swap (waiting then: $waiting)", 2, shown)
    }
}
