package com.pocketstudio.app.rhythm

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.Freeze
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.MultiPatch
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.SongPart
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
 * « Geler la piste » and the live pads of an assistant's song render offline like an export: they
 * wait for a multizone instrument still being read into memory, so they never render the synth
 * stand-in in its place.
 */
@RunWith(RobolectricTestRunner::class)
class RenderWaitsForInstrumentsTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    /** Counts the offline renders into memory and gives back a short silence for each. */
    private class RenderSink : CommandSink {
        @Volatile var renders = 0
        @Volatile var lastFrames = 0L
        private var next = 1L
        override fun post(target: Long, type: Int, value: Float, a: Int, b: Int) = Unit
        override fun scheduleFrom(target: Long, frame: Long) = Unit
        override fun install(target: Long, kind: CommandSink.Install, index: Int, sample: Long) = Unit
        @Volatile var lastChannels = 0
        @Synchronized override fun sampleCreate(data: FloatArray, channels: Int, sampleRate: Int): Long {
            lastChannels = channels
            return next++
        }
        override fun sampleFrames(handle: Long): Long = 0L
        override fun sampleRelease(handle: Long) = Unit
        @Synchronized override fun exportCreate(): Long = next++
        override fun exportRender(target: Long, path: String, frames: Long, tailFrames: Long, fadeOutFrames: Long, autoTail: Boolean, floatWav: Boolean, sampleRate: Int, bits: Int) =
            ExportResult.Ok
        @Synchronized override fun exportRenderFloat(target: Long, frames: Long, tailFrames: Long): FloatArray {
            renders++
            lastFrames = frames
            // A faint tone: not silent, so a live pad is kept.
            return FloatArray(2 * 4800) { 0.1f * kotlin.math.sin(it * 0.05f) }
        }
        override fun exportDestroy(target: Long) = Unit
    }

    private val sink = RenderSink()

    @After
    fun tearDown() {
        NativeEngine.sink = null
        com.pocketstudio.app.projects.CurrentProject.set(app, RhythmViewModel.PROJECT_ID)
    }

    /** [ms] of the main looper's clock (the waits count 15 s at most), letting worker threads run meanwhile. */
    private fun idle(ms: Long) {
        repeat((ms / 50).toInt()) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
            Thread.sleep(10)
        }
    }

    private fun waitUntil(what: String, check: () -> Boolean) {
        val until = System.currentTimeMillis() + 60_000
        while (!check()) {
            assertTrue(what, System.currentTimeMillis() < until)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
            Thread.sleep(5)
        }
    }

    /** A song whose first track plays the multizone instrument « piano-test » (not on this phone). */
    private fun open(p: Project, hold: String? = null): RhythmViewModel {
        // A project of its own: a ViewModel of an earlier test may still write its own.
        val id = "wait-${System.nanoTime()}"
        ProjectStore(File(app.filesDir, "projects")).save(id, p)
        com.pocketstudio.app.projects.CurrentProject.set(app, id)
        NativeEngine.sink = sink
        val vm = RhythmViewModel(app)
        // Before anything the project opening started has run (the main looper is paused here).
        vm.holdMultiLoading(hold)
        // The project read.
        waitUntil("the project never opened") { vm.project.value.song.size == p.song.size }
        return vm
    }

    private fun song(): Project = Project(
        synthEngine = InstrumentTrack.ENGINE_MULTI,
        multi = MultiPatch("piano-test", "Piano test"),
        synthNotes = NoteClip(notes = listOf(Note(0, 96, 60))),
        song = listOf(SongPart(0)),
    )

    @Test
    fun freezingWaitsForTheInstrumentBeingRead() {
        val vm = open(song())
        // The phone has no such instrument: its first reading ends at once; then it is read again.
        waitUntil("the first reading never ended") { vm.multiLoading.value.isEmpty() }
        vm.holdMultiLoading("piano-test")
        vm.freezeTrack(0)
        idle(3000)
        // Still waiting (this ViewModel's own state: a ViewModel left over from another test may
        // render into the same sink meanwhile).
        assertEquals("frozen before the instrument was in", 0, vm.freezing.value)
        assertTrue(!Freeze.isFrozen(vm.project.value, 0))
        vm.holdMultiLoading(null)
        waitUntil("the track was never frozen") { vm.freezing.value == null && Freeze.isFrozen(vm.project.value, 0) }
        assertTrue(sink.renders >= 1)
    }

    @Test
    fun theSongsLivePadsWaitForTheInstrumentBeingRead() {
        // Another instrument still being read when the project opens (the pads start once its sounds are in).
        val vm = open(song().copy(livePadsPending = true), hold = "strings-test")
        vm.engineStarted()
        idle(3000)
        // Still waiting: the pads are written (and the flag cleared) only once all are rendered.
        assertTrue("prepared before the instrument was in", vm.project.value.livePadsPending)
        assertTrue(vm.project.value.live.clips.all { it == null })
        vm.holdMultiLoading(null)
        waitUntil("the pads were never prepared") { !vm.project.value.livePadsPending }
        assertTrue("${sink.renders} renders", sink.renders >= 1)
    }

    /** 6 minutes of song (a techno track): frozen, in mono (past two minutes); 7.5 minutes: refused, said. */
    @Test
    fun aSixMinuteSongFreezesInMonoAndALongerOneIsRefused() {
        // 120 BPM: a bar is 2 s; 12 parts of 15 bars = 360 s.
        val six = song().copy(song = List(12) { SongPart(0, repeats = 15) })
        val vm = open(six)
        waitUntil("the first reading never ended") { vm.multiLoading.value.isEmpty() }
        vm.freezeTrack(0)
        waitUntil("the track was never frozen (${vm.notice.value}, ${vm.sampleTask.value}, ${sink.renders})") { vm.freezing.value == null && Freeze.isFrozen(vm.project.value, 0) }
        assertEquals(360L * 48_000, sink.lastFrames)
        val audio = Freeze.audioOf(vm.project.value, 0)!!
        // The sink's 4800 stereo frames, folded to mono: 0.1 s, one channel.
        assertEquals(0.1, audio.clip!!.seconds, 1e-6)
        assertEquals(1, sink.lastChannels)

        val long = song().copy(song = List(15) { SongPart(0, repeats = 15) })  // 450 s
        val vm2 = open(long)
        val before = sink.renders
        vm2.freezeTrack(0)
        idle(1000)
        assertEquals(before, sink.renders)
        assertTrue(vm2.notice.value!!.contains("7 minutes"))
    }
}
