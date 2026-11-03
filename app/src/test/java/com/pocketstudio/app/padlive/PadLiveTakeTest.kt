package com.pocketstudio.app.padlive

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.PadLive
import com.pocketstudio.model.PadLiveSet
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.time.Duration
import kotlin.math.abs

/**
 * « Enregistrer ce que je joue » (n° 25, lot B2) through the ViewModel: a take of whole bars put on
 * an empty pad is one undo step, saved and read back by a new ViewModel; put in a brick of the
 * Live, it is in the song's export; too short or let go, nothing is left. The engine's recorder is
 * a stand-in here ([ResampleSource]); the engine's own bar lines are tested on Linux
 * (engine/host-tests/padlive_tests.inc, padlive_resample_from_bar_to_bar).
 */
@RunWith(RobolectricTestRunner::class)
class PadLiveTakeTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    /** In the engine's place: the sounds made (their size) and the audio clips an export installs. */
    private class Sink : CommandSink {
        private var next = 1L
        val created = HashMap<Long, Int>()
        val exportClips = ArrayList<Long>()
        override fun post(target: Long, type: Int, value: Float, a: Int, b: Int) = Unit
        override fun scheduleFrom(target: Long, frame: Long) = Unit
        @Synchronized override fun install(target: Long, kind: CommandSink.Install, index: Int, sample: Long) {
            if (target != 0L && kind == CommandSink.Install.AudioClip && sample != 0L) exportClips += sample
        }
        @Synchronized override fun sampleCreate(data: FloatArray, channels: Int, sampleRate: Int): Long = next++.also { created[it] = data.size }
        @Synchronized override fun sampleFrames(handle: Long): Long = (created[handle] ?: 96_000).toLong() / 2
        override fun sampleRelease(handle: Long) = Unit
        @Synchronized override fun exportCreate(): Long = next++
        override fun exportRender(target: Long, path: String, frames: Long, tailFrames: Long, fadeOutFrames: Long, autoTail: Boolean, floatWav: Boolean, sampleRate: Int, bits: Int) =
            ExportResult.Cancelled
        override fun exportRenderFloat(target: Long, frames: Long, tailFrames: Long): FloatArray = FloatArray(0)
        override fun exportDestroy(target: Long) = Unit
    }

    /** The engine's recorder as it behaves: armed, recording, ended on the bar line asked for, [frames] recorded. */
    private class Recorder(private val frames: Int) : ResampleSource {
        var state = 0
        var started = 0
        @Volatile var finished = 0
        val stopped = ArrayList<Long>()
        override fun start(seconds: Int): Long {
            started++
            state = 0
            return 7L
        }
        override fun status(handle: Long): LongArray = longArrayOf(state.toLong(), if (state >= 1) frames.toLong() else 0L)
        override fun stop(handle: Long) {
            stopped += handle
            state = 2
        }
        @Synchronized override fun finish(handle: Long): FloatArray {
            finished++
            val was = state
            state = 2
            return if (was == 0) FloatArray(0) else FloatArray(frames * 2) { i -> 0.25f * kotlin.math.sin(i * 0.003f) }
        }
    }

    private val sink = Sink()

    /** Two bars at 124 BPM, as the engine cuts them (bar line to bar line), and a few frames of a third. */
    private val twoBars = Math.round(2 * 4 * 60.0 / 124.0 * 48_000).toInt()

    private fun waitFor(what: String, done: () -> Boolean) {
        val until = System.currentTimeMillis() + 20_000
        while (!done()) {
            assertTrue("never: $what", System.currentTimeMillis() < until)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
            Thread.sleep(5)
        }
    }

    private fun liveVm(recorder: ResampleSource? = null): RhythmViewModel = RhythmViewModel(app).also { vm ->
        vm.livePadsSoundingForTest = emptySet()
        vm.padLive.played.sourceForTest = recorder
    }

    /** The open project made a « Live » one at 124 BPM, page A's first pad taken. */
    private fun RhythmViewModel.asLive() = session.edit { p ->
        PadLive.newProject().let { n ->
            p.copy(name = n.name, tempoBpm = 124.0, live = n.live.withAt(0, LiveClip("k.wav", "Kick", 1)), padLive = PadLiveSet(home = true))
        }
    }

    private fun record(vm: RhythmViewModel, recorder: Recorder) {
        vm.padLive.played.press()
        assertEquals(PlayedTakeUi.Armed, vm.padLive.played.ui.value)
        recorder.state = 1
        waitFor("recording") { vm.padLive.played.ui.value is PlayedTakeUi.Recording }
        assertTrue(vm.padLive.played.ui.value is PlayedTakeUi.Recording)
        vm.padLive.played.press()
        waitFor("the take") { vm.padLive.played.ui.value is PlayedTakeUi.Done }
    }

    @Before
    fun setUp() {
        File(app.filesDir, "projects").deleteRecursively()
        NativeEngine.sink = sink
    }

    @After
    fun tearDown() {
        NativeEngine.sink = null
    }

    @Test
    fun aTakeOnAnEmptyPadIsOneUndoStepSavedAndReadBack() {
        val recorder = Recorder(twoBars + 300)
        val vm = liveVm(recorder)
        vm.asLive()
        record(vm, recorder)
        assertEquals(PlayedTakeUi.Done(2), vm.padLive.played.ui.value)
        // Whole bars: the frames past the last bar line are cut.
        assertEquals(twoBars, vm.padLive.played.takeForTest!!.frames)
        assertEquals(1, vm.padLive.played.emptyPad())
        assertTrue(vm.padLive.played.placeOnPad())
        waitFor("the take on pad A2") { vm.project.value.live.at(1) != null }
        val clip = vm.project.value.live.at(1)!!
        assertEquals("a loop of its two bars", 8, clip.beats)
        assertEquals(false, clip.oneShot)
        assertEquals("it follows the tempo it was played at", 124.0, clip.bpm, 1e-9)
        assertEquals(PlayedTakeUi.Ready, vm.padLive.played.ui.value)
        assertTrue(File(File(File(app.filesDir, "projects"), vm.projectId), "samples/${clip.file}").isFile)
        // One undo step, and back.
        vm.undo()
        assertNull(vm.project.value.live.at(1))
        vm.session.redo()
        assertEquals(clip, vm.project.value.live.at(1))
        // Saved, read back as the app reopened finds it.
        var saved: Boolean? = null
        vm.viewModelScope.launch { saved = vm.flushNow() }
        waitFor("saved") { saved != null }
        assertTrue(saved!!)
        val again = liveVm()
        assertEquals(clip, again.project.value.live.at(1))
    }

    @Test
    fun aTakeInABrickIsInTheSongsExport() {
        val recorder = Recorder(twoBars)
        val vm = liveVm(recorder)
        vm.asLive()
        record(vm, recorder)
        assertTrue(vm.padLive.played.placeInBrick())
        waitFor("the take in a brick") { vm.project.value.audioTracks.any { it.clip != null } }
        val track = vm.project.value.audioTracks.first { it.clip != null }
        val clip = track.clip!!
        assertEquals("in the scene shown", vm.project.value.scene, clip.scene)
        assertEquals("two bars", 2 * vm.project.value.stepsPerBar, clip.loopSteps)
        assertEquals(PlayedTakeUi.Ready, vm.padLive.played.ui.value)
        // The export plays it: its sound is installed on the export's engine.
        val made = sink.created.filterValues { it == twoBars * 2 }.keys
        assertTrue("the take's sound was made", made.isNotEmpty())
        vm.exportWav()
        waitFor("the export's audio clips") { sink.exportClips.isNotEmpty() }
        assertTrue("the take is in the export", sink.exportClips.any { it in made })
        // One undo step.
        vm.undo()
        assertTrue(vm.project.value.audioTracks.none { it.clip != null })
    }

    /** FIX-1: the project closed while the take is armed, or recording: its engine buffer is freed, once. */
    @Test
    fun closingTheProjectFreesTheTakesBuffer() {
        for (recording in listOf(false, true)) {
            val owner = androidx.lifecycle.ViewModelStore()
            val vm = androidx.lifecycle.ViewModelProvider(owner, androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory(app))[RhythmViewModel::class.java]
            vm.livePadsSoundingForTest = emptySet()
            val recorder = Recorder(twoBars)
            vm.padLive.played.sourceForTest = recorder
            vm.asLive()
            vm.padLive.played.press()
            assertEquals(PlayedTakeUi.Armed, vm.padLive.played.ui.value)
            if (recording) {
                recorder.state = 1
                waitFor("recording") { vm.padLive.played.ui.value is PlayedTakeUi.Recording }
            }
            owner.clear()  // onCleared: the screen's scope is gone
            waitFor("freed (recording: $recording)") { recorder.finished >= 1 }
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))
            Thread.sleep(50)
            assertEquals("freed once", 1, recorder.finished)
        }
    }

    /** FIX-1: the stop names the take it ends (the engine leaves a take armed since alone). */
    @Test
    fun theStopNamesItsTake() {
        val recorder = Recorder(twoBars)
        val vm = liveVm(recorder)
        vm.asLive()
        record(vm, recorder)
        assertEquals(listOf(7L), recorder.stopped)
    }

    @Test
    fun tooShortOrLetGoLeavesNothing() {
        // Less than half a bar: « Trop court ».
        val short = Recorder(20_000)
        val vm = liveVm(short)
        vm.asLive()
        val before = vm.project.value
        vm.padLive.played.press()
        short.state = 1
        waitFor("recording") { vm.padLive.played.ui.value is PlayedTakeUi.Recording }
        vm.padLive.played.press()
        waitFor("ready again") { vm.padLive.played.ui.value == PlayedTakeUi.Ready }
        assertNotNull(vm.padLive.played.message.value)
        assertEquals(before, vm.project.value)
        // Armed, touched again before any sound: let go, the engine's buffer freed.
        val armed = Recorder(twoBars)
        vm.padLive.played.sourceForTest = armed
        vm.padLive.played.press()
        vm.padLive.played.press()
        waitFor("freed") { armed.finished == 1 }
        assertEquals(PlayedTakeUi.Ready, vm.padLive.played.ui.value)
        // Recorded, then thrown away: nothing changes.
        val thrown = Recorder(twoBars)
        vm.padLive.played.sourceForTest = thrown
        record(vm, thrown)
        vm.padLive.played.cancel()
        assertEquals(PlayedTakeUi.Ready, vm.padLive.played.ui.value)
        assertEquals(before, vm.project.value)
    }

    @Test
    fun tapTempoFromPadLiveIsOneUndoStepAndTheLoopsFollow() {
        // Pad Live › the tempo (« 124 ») › « Taper »: four taps 480 ms apart, 125 BPM.
        val vm = liveVm()
        vm.asLive()
        vm.session.edit { it.copy(live = it.live.withAt(2, LiveClip("l.wav", "Boucle", 8, bpm = 124.0))) }
        vm.beginTempoSheet()
        val start = System.currentTimeMillis()
        for (i in 0 until 4) {
            while (System.currentTimeMillis() - start < i * 480L) Thread.onSpinWait()
            vm.tapSheetTempo()
        }
        assertEquals(125.0, vm.project.value.tempoBpm, 1.0)
        // The loop made at 124 follows the new tempo (it keeps its own, stretched live).
        assertEquals(124.0, vm.project.value.live.at(2)!!.bpm, 1e-9)
        assertTrue(vm.project.value.live.at(2)!!.follows)
        // The taps are one undo step.
        vm.undo()
        assertEquals(124.0, vm.project.value.tempoBpm, 1e-9)
    }

    @Test
    fun aPadsOwnSoundIsBakedAsTheEnginePlaysIt() {
        // A ramp: frame i holds i / 1000 * 0.5 (as the engine's padlive_pad_pitch_reverse_attack_release).
        val ramp = Pcm(FloatArray(1000) { 0.5f * it / 1000f }, 1, 48_000)
        fun v(i: Double) = (0.5 * i / 1000.0).toFloat()
        val clip = LiveClip("r.wav", "Rampe", 1, oneShot = true)
        assertTrue(PadSound.shaped(ramp, clip) === ramp)
        val up = PadSound.shaped(ramp, clip.copy(pitch = 12))
        assertEquals(500, up.frames)
        assertEquals(v(200.0), up.data[100], 1e-5f)
        val down = PadSound.shaped(ramp, clip.copy(pitch = -12))
        assertEquals(v(100.5), down.data[201], 1e-5f)
        val back = PadSound.shaped(ramp, clip.copy(reverse = true))
        assertEquals(v(999.0), back.data[0], 1e-5f)
        assertEquals(v(899.0), back.data[100], 1e-5f)
        val soft = PadSound.shaped(ramp, clip.copy(attack = 0.01f))
        assertEquals(0.5f * v(240.0), soft.data[240], 1e-4f)
        assertEquals(v(700.0), soft.data[700], 1e-5f)
        assertTrue(soft.data.all { abs(it) <= 0.5f })
    }
}
