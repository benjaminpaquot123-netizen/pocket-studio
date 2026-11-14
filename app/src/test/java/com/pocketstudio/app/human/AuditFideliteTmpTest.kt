package com.pocketstudio.app.human

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.rhythm.RecordTarget
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.AutomationLane
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.partSteps
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.time.Duration

/**
 * Audit « banc crédible » (2026-11-09), annex A: the six probes the audit wrote (AuditFideliteTmpTest, never run: the
 * Gradle queue was held, and the file left the tree) rewritten from the annex for PLAN-RESTE A.1. Each one plays the
 * finding's own scenario through the app (RhythmViewModel, the bench's engine as NativeEngine's sink, as on a seat) and
 * reads what the app reads (NativeEngine's facade, the project). They were red on the bench of bebe0c9 by construction
 * (a1 F1, a2/a3 F5, b1/b2 F4, b3 F2: the lines quoted in the annex); the lots « faux-moteur » and « pads-live » repaired
 * the bench, so they are green now, and red again under mutations that put the old lines back (the report of the lot
 * « banc-fin » lists them, and their JUnit outputs are kept under docs/audits/ultra/audit-banc/).
 */
@RunWith(RobolectricTestRunner::class)
class AuditFideliteTmpTest {
    /** PLAN-RESTE 4.5: the state inherited from the test before must be pristine (else: red, naming that test). */
    @get:Rule
    val pristine = BenchPristineRule()
    private val app: Application get() = ApplicationProvider.getApplicationContext()

    @After
    fun clear() {
        NativeEngine.sink = null
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(20))
    private fun pass(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    /** [p] opened with the bench's engine there before the app, as on a seat. */
    private fun openWithEngine(p: Project): Pair<RhythmViewModel, BenchEngine> {
        val engine = BenchEngine()
        NativeEngine.sink = engine
        val id = "audit-${System.nanoTime()}"
        ProjectStore(File(app.filesDir, "projects")).save(id, p)
        CurrentProject.set(app, id)
        val vm = RhythmViewModel(app)
        val until = System.currentTimeMillis() + 10_000
        while (vm.project.value.song.size != p.song.size || vm.project.value.tempoBpm != p.tempoBpm) {
            assertTrue("the project never opened", System.currentTimeMillis() < until)
            idle(); Thread.sleep(5)
        }
        vm.engineStarted()
        return vm to engine
    }

    /**
     * F1: a take on the song (Mode simple › Mon morceau › Enregistrer ma voix), never stopped by hand: the song ends by
     * itself and the take is kept on its track, a clip as long as the song. bebe0c9: the song's end (kind 2) set
     * transportSince to -1 without freezing the take, micFinish gave 0 frames, TakeVault said Outcome.Nothing: lost.
     */
    @Test
    fun a1_micTakeKeptWhenTheSongEndsByItself() {
        val (vm, engine) = openWithEngine(Project(song = listOf(SongPart(0), SongPart(0))))
        val song = vm.project.value.let { p -> p.song.sumOf { p.partSteps(it) } * 15.0 / p.tempoBpm }
        vm.setRecordTarget(RecordTarget(song = true, fromPart = 0))
        val track = vm.micTakeTrack()!!
        vm.recordAudio(track)
        val until = System.currentTimeMillis() + 90_000
        while (vm.project.value.audioTracks.firstOrNull { it.id == track }?.clip == null || vm.takeSaving.value) {
            assertTrue("the take never reached its track (recording ${vm.audioRecording.value}, notice ${vm.notice.value})", System.currentTimeMillis() < until)
            pass(50); Thread.sleep(3)
        }
        val clip = vm.project.value.audioTracks.first { it.id == track }.clip!!
        assertTrue("kept, as long as the song ($song s): ${clip.seconds} s", clip.seconds in song - 0.6..song + 0.3)
        assertEquals(1, engine.micTakes)
    }

    /**
     * F5: positions local to the part playing. Three one-bar parts at 120 BPM: song step 40 is step 8 of the third part
     * (it took over at 32). bebe0c9: absoluteStep and playPosition counted from ▶ (40).
     */
    @Test
    fun a2_absoluteStepRestartsAtTheSwap() {
        val (vm, _) = openWithEngine(Project(song = listOf(SongPart(0), SongPart(0), SongPart(0))))
        assertEquals("one-bar parts", 16, vm.project.value.partSteps(vm.project.value.song[0]))
        vm.toggleSong()
        idle()
        pass(5_000 - 20)  // 40 steps at 120 BPM (125 ms each)
        assertEquals("the song is at step 40", 40, NativeEngine.songStep())
        assertEquals("the third part is at its step 8", 8, NativeEngine.absoluteStep())
        assertEquals("so is the play position", 8.0, NativeEngine.playPosition(), 0.6)
    }

    /**
     * F5 (the grid's head): under a melody of 64 steps the 16-step drums go on turning; the head the grid shows
     * (Engine.cpp's currentStep_) is the drum pattern's step: 8 at step 40. bebe0c9: the engine had no currentStep (-1) and
     * the bench's play head showed the part's position (40 of 16).
     */
    @Test
    fun a3_theGridsHeadIsTheDrumPatternsStep() {
        val (vm, _) = openWithEngine(Project(synthNotes = NoteClip(steps = 64), song = listOf(SongPart(0), SongPart(0))))
        vm.togglePlay()
        idle()
        pass(5_000 - 20)
        val head = BenchPlayhead()
        head.tick(vm, android.os.SystemClock.uptimeMillis())
        assertEquals("the part is at its step 40", 40, vm.live.value.absoluteStep)
        assertEquals("the drum grid's head is on its step 8", 8, vm.live.value.currentStep)
    }

    /**
     * F4 (a): one clock. 16 steps at 120 BPM, then the tempo goes to 60: the transport's step (where a Pad Live take and the
     * launch lines count from) keeps its 16. bebe0c9: the play head counted the whole time since ▶ at the new tempo (8).
     */
    @Test
    fun b1_transportStepKeepsItsStepsAcrossATempoChange() {
        val (vm, _) = openWithEngine(Project())  // a loop on its own: a song of one part would end at step 16
        if (vm.transport.value.countIn) vm.toggleCountIn()  // no count-in here (b2 is about it)
        vm.togglePlay()
        idle()
        assertEquals("▶ without a count-in", 0, vm.countInForTest)
        pass(2_000 - 20)
        assertEquals("16 steps played at 120 BPM", 16, NativeEngine.transportStep())
        vm.setTempo(60.0)
        assertEquals("no jump back at 60 BPM", 16, NativeEngine.transportStep())
        pass(1_000)
        assertEquals("4 steps more at 60 BPM", 20, NativeEngine.transportStep())
    }

    /**
     * F4 (b): with the « Décompte » chip (one bar), the transport has no step during the count-in (Engine.cpp: -1 before the
     * « 1 ») and counts from the « 1 ». bebe0c9: the play head counted from ▶, 8 in the middle of the count-in.
     */
    @Test
    fun b2_transportStepIsMinusOneDuringTheCountIn() {
        val (vm, _) = openWithEngine(Project())  // a loop on its own: a song of one part would end at step 16
        if (!vm.transport.value.countIn) vm.toggleCountIn()
        vm.togglePlay()
        idle()
        pass(1_000 - 20)
        assertEquals("the app asked for one bar of count-in", 1, vm.countInForTest)
        assertEquals("counting in: no transport step", -1, NativeEngine.transportStep())
        pass(2_000)  // the « 1 » at 2 s, then one second: 8 steps
        assertEquals("one second after the « 1 »", 8, NativeEngine.transportStep())
    }

    /**
     * F2: ● Automation on, the music playing, a channel's volume moved: one curve with one point on the step playing.
     * bebe0c9: BenchEngine had no currentStep (CommandSink's -1), recordGesture returned at once: nothing was ever written.
     */
    @Test
    fun b3_aKnobMovedWithAutomationRecordWritesAPoint() {
        val (vm, _) = openWithEngine(Project().copy(tempoBpm = 100.0))
        vm.togglePlay()
        idle()
        vm.toggleAutomationRecord()
        pass(800)  // 150 ms a step at 100 BPM: step 5
        vm.setChannelVolume(0, 0.3f)
        val lanes = vm.project.value.automation
        assertEquals("one curve, made by the gesture", 1, lanes.size)
        assertEquals(AutoTarget(AutoTarget.VOLUME, 0), lanes[0].target)
        assertEquals(AutomationLane.LOOP, lanes[0].mode)
        assertEquals("one point, on the step playing", listOf(5), lanes[0].values.indices.filter { lanes[0].values[it] != null })
    }
}
