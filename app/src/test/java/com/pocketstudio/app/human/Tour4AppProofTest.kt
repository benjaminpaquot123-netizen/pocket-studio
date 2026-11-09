package com.pocketstudio.app.human

import com.pocketstudio.model.allScenes
import com.pocketstudio.model.instrumentTracks
import com.pocketstudio.model.sceneView
import com.pocketstudio.model.switchScene
import com.pocketstudio.model.track
import android.app.Application
import android.os.Looper
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.record.TakePhase
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.LiveSlots
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.SimpleMelody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.time.Duration

/**
 * Audit ultra round 4 (lot banc): app or bench? Without the bench (no HumanTester, no BenchEngine), the app's own view
 * model with a small engine that behaves like a phone's (its play position moves with the clock from ▶, after the
 * count-in; its launch lines report what they launched), for what testers saw go wrong:
 * - « Enregistrer ma mélodie », three keys, « Arrêter »: « Rien n'a été joué pendant la prise » (seats A b, C b, C d, D a, D d);
 * - Live's « ■ Tout » and a column's ■ stopped nothing (seats B c, C c).
 * The app keeps the notes and sends the stops: the bench let its engine go under a keyboard take (and stopped the
 * transport to do so, which ends the take), and had nothing to play the launch lines (see BenchTour4Test).
 */
@RunWith(RobolectricTestRunner::class)
class Tour4AppProofTest {
    /** PLAN-RESTE 4.5: the state inherited from the test before must be pristine (else: red, naming that test). */
    @get:Rule
    val pristine = BenchPristineRule()
    private val app: Application get() = ApplicationProvider.getApplicationContext()

    /** A phone's engine as far as the view model reads it: the play position from ▶ (count-in first), the launch lines. */
    private class PhoneEngine : CommandSink {
        private var next = 1L
        val posts = ArrayList<List<Number>>()
        private var since = -1L
        private var countIn = 0
        private var tempo = 120.0
        private var beats = 4
        /** Launches the « engine » made, (slot, scene, step) each, read once by the view model. */
        val launches = java.util.ArrayDeque<IntArray>()

        @Synchronized override fun post(target: Long, type: Int, value: Float, a: Int, b: Int) {
            if (target != 0L) return
            posts += listOf(type, value, a, b)
            when (type) {
                CMD_SET_TRANSPORT -> { since = if (value > 0f) SystemClock.uptimeMillis() else -1L; countIn = if (value > 0f) a else 0 }
                CMD_SET_TEMPO -> if (value > 0f) tempo = value.toDouble()
                CMD_BEATS_PER_BAR -> if (a > 0) beats = a
            }
        }
        override fun playPosition(): Double = if (since < 0) -1.0 else (SystemClock.uptimeMillis() - since) * tempo / 15_000.0 - countIn * beats * 4.0
        override fun transportStep(): Int = playPosition().let { if (it < 0) -1 else it.toInt() }
        override fun transportPosition(): Double = playPosition().let { if (it < 0) -1.0 else it }
        @Synchronized override fun drainLaunchEvents(out: IntArray): Int {
            var n = 0
            while (launches.isNotEmpty() && (n + 1) * 3 <= out.size) {
                val e = launches.poll()!!
                out[n * 3] = e[0]; out[n * 3 + 1] = e[1]; out[n * 3 + 2] = e[2]
                n++
            }
            return n
        }
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
        @Synchronized fun of(type: Int): List<List<Number>> = posts.filter { it[0] == type }
    }

    @After
    fun clear() {
        NativeEngine.sink = null
    }

    private fun idle(ms: Long = 20) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    private fun open(p: Project = Project()): RhythmViewModel {
        val id = "tour4-${System.nanoTime()}"
        ProjectStore(File(app.filesDir, "projects")).save(id, p)
        CurrentProject.set(app, id)
        val vm = RhythmViewModel(app)
        val until = System.currentTimeMillis() + 10_000
        // The project read from disk (its song too: a default project has the same tracks and no song).
        while (vm.project.value.instrumentTracks.size != p.instrumentTracks.size || vm.project.value.song.size != p.song.size) {
            assertTrue("the project never opened: song ${vm.project.value.song.size}/${p.song.size}, scenes ${vm.project.value.allScenes.size}/${p.allScenes.size}, " +
                "tracks ${vm.project.value.instrumentTracks.size}/${p.instrumentTracks.size}", System.currentTimeMillis() < until)
            idle(); Thread.sleep(5)
        }
        return vm
    }

    /** Three keys of the keyboard as a finger plays them (down, a moment, up), 400 ms apart. */
    private fun threeKeys(vm: RhythmViewModel) {
        for (key in listOf(62, 65, 69)) {
            vm.synthNoteOn(key)
            idle(150)
            vm.synthNoteOff(key)
            idle(250)
        }
    }

    private fun melodyNotes(vm: RhythmViewModel): Int {
        val p = vm.project.value
        val id = SimpleMelody.trackOf(p) ?: return 0
        return p.sceneView(p.scene).track(id).notes.notes.size
    }

    /** Transport stopped: « Enregistrer ma mélodie » counts in, records, the keys are kept (Garder, Remettre en rythme, Refaire). */
    @Test
    fun aMelodyTakeFromStoppedKeepsItsKeys() {
        NativeEngine.sink = PhoneEngine()
        val vm = open()
        vm.recordMelodyTake()
        assertTrue("the take is on", vm.recording.value)
        // The count-in (one bar at the project's tempo) goes by: the take records.
        val until = SystemClock.uptimeMillis() + 10_000
        while (NativeEngine.playPosition() < 0.5) { assertTrue(SystemClock.uptimeMillis() < until); idle(50) }
        idle(100)
        assertEquals(TakePhase.RECORDING, vm.noteTake.value?.phase)
        threeKeys(vm)
        vm.recordMelodyTake()  // « ■ Arrêter »
        idle()
        assertFalse("« Rien n'a été joué » (${vm.lastTakeReport})", vm.takeEmpty.value)
        assertEquals(vm.lastTakeReport, TakePhase.DONE, vm.noteTake.value?.phase)
        assertEquals(vm.lastTakeReport, 3, vm.noteTake.value?.notes)
        assertEquals("« Ma mélodie » holds the three notes", 3, melodyNotes(vm))
    }

    /** The music already playing (the seats' case: « la musique du projet joue en boucle »): no count-in, the keys are kept. */
    @Test
    fun aMelodyTakeWhileTheMusicPlaysKeepsItsKeys() {
        NativeEngine.sink = PhoneEngine()
        val vm = open()
        vm.togglePlay()
        idle(500)
        assertTrue(vm.transport.value.playing)
        vm.recordMelodyTake()
        idle(100)
        assertTrue(vm.recording.value)
        threeKeys(vm)
        vm.recordMelodyTake()
        idle()
        assertFalse("« Rien n'a été joué » (${vm.lastTakeReport})", vm.takeEmpty.value)
        assertEquals(vm.lastTakeReport, 3, vm.noteTake.value?.notes)
        assertEquals(3, melodyNotes(vm))
        assertTrue("the music goes on after the take", vm.transport.value.playing)
    }

    /**
     * Coordinator's lead (CI 243 logcat: « take: 4 notes » written into the scene the ENGINE plays): a song of two parts
     * on two scenes, the second one playing, the first one shown. Where do the keys go, what does the screen say?
     */
    @Test
    fun aMelodyTakeDuringTheSongGoesToThePartHeard() {
        NativeEngine.sink = PhoneEngine()
        // Two parts that are not empty (an empty part is dropped as the project is read): a kick on each.
        val two = Project().let { it.copy(drums = it.drums.toggle(0, 0)) }.switchScene(1)
            .let { it.copy(drums = it.drums.toggle(0, 4)) }.switchScene(0)
            .copy(song = listOf(com.pocketstudio.model.SongPart(0), com.pocketstudio.model.SongPart(1)))
        val vm = open(two)
        vm.playSongPart(1)
        // The song starts once its sounds are loaded (off the main thread): awaited, as a phone's ▶ is.
        val until = System.currentTimeMillis() + 10_000
        while (!vm.transport.value.playing && System.currentTimeMillis() < until) { idle(); Thread.sleep(5) }
        idle(500)
        assertTrue("the song plays from its second part", vm.transport.value.playing)
        val shown = vm.project.value.scene
        vm.recordMelodyTake()
        idle(100)
        val shownDuring = vm.project.value.scene
        threeKeys(vm)
        vm.recordMelodyTake()
        idle()
        val p = vm.project.value
        val id = SimpleMelody.trackOf(p)!!
        val perScene = p.allScenes.indices.map { s -> p.sceneView(s).track(id).notes.notes.size }
        val facts = "song part ${vm.sessionUi.value.songIndex}, shown before $shown, during $shownDuring, after ${p.scene}; " +
            "notes per scene $perScene; ${vm.lastTakeReport}; empty=${vm.takeEmpty.value}"
        println("TOUR4 several parts: $facts")
        // The take is not « Rien n'a été joué », and its notes are where the screen looks (T4-26, CI 243: they went to the
        // part heard while the screen showed another one, « 4 notes » said and none seen).
        assertFalse(facts, vm.takeEmpty.value)
        assertTrue(facts, perScene.sum() >= 3)
        assertTrue("the notes are in the part shown: $facts", perScene[p.scene] >= 3)
    }

    /**
     * What the bench did (round 4): no engine under the take (it let its engine go at the touch before), the keys go
     * nowhere: « Rien n'a été joué ». A phone always has its engine: this is the bench's « Rien n'a été joué ».
     */
    @Test
    fun withoutAnEngineTheKeysOfATakeAreNotKept() {
        val vm = open()
        vm.togglePlay()
        idle(500)
        vm.recordMelodyTake()
        idle(100)
        threeKeys(vm)
        vm.recordMelodyTake()
        idle()
        assertTrue(vm.takeEmpty.value)
        assertTrue(vm.lastTakeReport, vm.lastTakeReport!!.contains("3 touches, 0 notes"))
        assertTrue(vm.lastTakeReport, vm.lastTakeReport!!.contains("engine off"))
    }

    /**
     * And the bench's other way to lose it: its engine let go DURING the take (stopped, then ▶ again without it). A stop
     * ends a take (by design: ■, a call, the headphones out), here as soon as it began: « Rien n'a été joué » at once, and
     * with it the end of the « first recording », after which the app asks for the notifications (seat D d, screen 20).
     */
    @Test
    fun aStopEndsTheTakeAtOnce() {
        NativeEngine.sink = PhoneEngine()
        val vm = open()
        vm.togglePlay()
        idle(500)
        vm.recordMelodyTake()
        idle(100)
        vm.togglePlay()  // the bench's stop as it lets its engine go
        idle()
        assertFalse(vm.recording.value)
        assertTrue("« Rien n'a été joué » right after « Enregistrer ma mélodie »", vm.takeEmpty.value)
    }

    /** Live's « ■ Tout » and a column's ■: the stop goes to the engine on the launch line, and the grid follows its launches. */
    @Test
    fun liveStopAllAndATracksStopGoToTheEngineAndTheGridFollows() {
        val engine = PhoneEngine()
        NativeEngine.sink = engine
        val vm = open()
        vm.togglePlay()
        idle(300)
        val grid = vm.liveGrid.value ?: error("no Live grid while it plays")
        assertTrue("every block plays", (0 until LiveSlots.COUNT).all { grid.slot(it).playing >= 0 })
        // A column's ■ (the drums): armed as a stop (-1) for its slot alone, on the launch line.
        vm.stopLiveTrack(com.pocketstudio.model.SongPart.DRUMS)
        val arm1 = engine.of(CMD_ARM_LAUNCH).last()
        assertEquals(-1f, arm1[1].toFloat())
        assertEquals(1 shl LiveSlots.DRUMS, arm1[2].toInt())
        assertEquals(-1, vm.liveGrid.value!!.slot(LiveSlots.DRUMS).pending)
        // The engine reaches the line and stops it: the grid shows it stopped, the rest playing.
        synchronized(engine) { engine.launches += intArrayOf(LiveSlots.DRUMS, -1, 16) }
        idle(200)
        assertEquals(-1, vm.liveGrid.value!!.slot(LiveSlots.DRUMS).playing)
        assertEquals(null, vm.liveGrid.value!!.slot(LiveSlots.DRUMS).pending)
        assertTrue(vm.liveGrid.value!!.slot(LiveSlots.INST).playing >= 0)
        // « ■ Tout »: every slot armed as a stop, in one command.
        vm.stopLiveAll()
        val arm2 = engine.of(CMD_ARM_LAUNCH).last()
        assertEquals(-1f, arm2[1].toFloat())
        assertEquals(LiveSlots.ALL, arm2[2].toInt())
        synchronized(engine) { for (k in 0 until LiveSlots.COUNT) engine.launches += intArrayOf(k, -1, 32) }
        idle(200)
        assertTrue("every block stopped: ${vm.liveGrid.value}", (0 until LiveSlots.COUNT).all { vm.liveGrid.value!!.slot(it).playing == -1 })
        assertFalse(vm.liveGrid.value!!.hasPending)
        // « the transport goes on » (stopLiveAll's own rule): ▶ is still on, nothing sounds.
        assertTrue(vm.transport.value.playing)
    }

    private companion object {
        const val CMD_SET_TRANSPORT = 5
        const val CMD_SET_TEMPO = 6
        const val CMD_BEATS_PER_BAR = 57
        const val CMD_ARM_LAUNCH = 71
    }
}
