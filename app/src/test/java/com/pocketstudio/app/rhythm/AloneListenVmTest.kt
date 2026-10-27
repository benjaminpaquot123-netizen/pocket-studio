package com.pocketstudio.app.rhythm

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.addTrack
import com.pocketstudio.model.guide.GuideRoles
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.time.Duration

/**
 * « Écouter seul » (bug report, APK 224: « je n'arrive pas à écouter le kick en boucle »), through the
 * ViewModel with an engine stand-in that keeps the engine's last word per strip: the kick alone is
 * the kick's pad soloed inside the kit (not the drums' strip), the part loops, and « Arrêter » gives
 * back the project's own mutes and solos, with the project, its undo and its file untouched.
 */
@RunWith(RobolectricTestRunner::class)
class AloneListenVmTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    private val sink = AloneEngineSink()

    @After
    fun tearDown() {
        NativeEngine.sink = null
        com.pocketstudio.app.projects.CurrentProject.set(app, RhythmViewModel.PROJECT_ID)
    }

    private fun idle(ms: Long = 50) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
    }

    private fun waitUntil(what: String, check: () -> Boolean) {
        val until = System.currentTimeMillis() + 60_000
        while (!check()) {
            assertTrue(what, System.currentTimeMillis() < until)
            idle()
            Thread.sleep(5)
        }
    }

    private fun open(p: Project): RhythmViewModel {
        val id = "alone-${System.nanoTime()}"
        ProjectStore(File(app.filesDir, "projects")).save(id, p)
        com.pocketstudio.app.projects.CurrentProject.set(app, id)
        NativeEngine.sink = sink
        val vm = RhythmViewModel(app)
        waitUntil("the project never opened") { vm.project.value.tracks.size == p.tracks.size && vm.project.value.song.size == p.song.size }
        vm.engineStarted()
        idle()
        return vm
    }

    private val bassCh = InstrumentTrack.mixerChannel(1)

    /** A kick (pad 0), a snare (pad 2), a hat (pad 5) and a bass on track 1 the project keeps soloed; the snare cut. */
    private fun song(): Project {
        var p = Project(song = listOf(SongPart(0))).addTrack("Basse")!!
        p = p.copy(tracks = p.tracks.map { it.copy(notes = NoteClip(notes = listOf(Note(0, 36, 40)))) })
        p = p.copy(drums = p.drums.setHit(0, 0, true).setHit(0, 8, true).setHit(2, 4, true).setHit(5, 2, true))
        return p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.mapIndexed { i, c ->
            when (i) {
                bassCh -> c.copy(solo = true)
                Mixer.padChannel(2) -> c.copy(mute = true)
                else -> c
            }
        }))
    }

    @Test
    fun theKicksCardHearsOnlyTheKickPadLoopedThenGivesEverythingBack() {
        val vm = open(song())
        val before = vm.project.value
        val undo = vm.session.canUndo.value
        // As the project says: the bass alone (its solo), the drums silent.
        assertTrue(sink.heardTrack(bassCh))
        assertFalse(sink.heardPad(0))

        vm.toggleAlone(AloneTarget.ofRole(vm.project.value, GuideRoles.KICK), from = "card:sound.kick")
        idle()
        assertEquals(AloneTarget(pads = setOf(0), parts = setOf(SongPart.DRUMS)), vm.alone.value?.target)
        assertTrue("the loop plays at once", sink.playing && vm.transport.value.playing)
        assertTrue("the kick is heard", sink.heardPad(0))
        assertFalse("the hat is not", sink.heardPad(5))
        assertFalse("the snare is not", sink.heardPad(2))
        assertFalse("the bass is not", sink.heardTrack(bassCh))
        assertTrue("never the whole drums' strip soloed", sink.solo[0] != true)

        vm.toggleAlone(AloneTarget.ofRole(vm.project.value, GuideRoles.KICK), from = "card:sound.kick")
        idle()
        assertNull(vm.alone.value)
        assertFalse(sink.playing)
        // The project's own state again: the bass soloed, the snare cut, the kick back in the drums.
        assertTrue(sink.heardTrack(bassCh))
        assertEquals(true, sink.solo[bassCh])
        assertEquals(true, sink.mute[Mixer.padChannel(2)])
        assertTrue(sink.solo[Mixer.padChannel(0)] != true)
        assertTrue(sink.padStrip[0] != true)
        // Nothing changed in the song: no undo step, the same project.
        assertSame(before, vm.project.value)
        assertEquals(undo, vm.session.canUndo.value)
    }

    @Test
    fun aTrackAloneAndTheMainStopEndIt() {
        val vm = open(song().copy(mixer = song().mixer.copy(channels = song().mixer.channels.mapIndexed { i, c -> if (i == bassCh) c.copy(solo = false, mute = true) else c })))
        vm.toggleAlone(AloneTarget.ofSongKey("inst:1"))
        idle()
        assertTrue("a cut track is heard alone all the same", sink.heardTrack(bassCh))
        assertFalse(sink.heardTrack(0))
        // The top bar's ■: the loop ends and the cut comes back.
        vm.togglePlay()
        idle()
        assertNull(vm.alone.value)
        assertEquals(true, sink.mute[bassCh])
    }

    @Test
    fun aMixerPushWhileItPlaysKeepsTheSoundAlone() {
        val vm = open(song())
        vm.toggleAlone(AloneTarget.ofKey("pad:5"))
        idle()
        assertTrue(sink.heardPad(5))
        // An edit that sends the whole mixer again (a pad's volume): still the hat alone.
        vm.setPadVolume(0, 0.5f)
        idle()
        assertTrue(sink.heardPad(5))
        assertFalse(sink.heardPad(0))
        vm.stopAlone()
        idle()
        assertFalse(sink.playing)
    }

    @Test
    fun leavingEveryScreenThatHoldsItStopsIt() {
        val vm = open(song())
        vm.toggleAlone(AloneTarget.ofKey("pad:0"), from = "card:sound.kick")
        val card = vm.holdAlone()
        idle()
        // The card folds as a sheet opens (« Autre idée de son »): it goes on.
        val sheet = vm.holdAlone()
        card()
        idle(RhythmViewModel.ALONE_GRACE_MS + 100)
        assertTrue(vm.alone.value != null)
        // The sheet closes too: it stops once the grace is over.
        sheet()
        idle(RhythmViewModel.ALONE_GRACE_MS / 2)
        assertTrue(vm.alone.value != null)
        idle(RhythmViewModel.ALONE_GRACE_MS)
        assertNull(vm.alone.value)
        assertFalse(sink.playing)
        assertTrue(sink.solo[Mixer.padChannel(0)] != true)
    }

    companion object {
        const val TRANSPORT = 5
        const val CHANNEL_MUTE = 22
        const val CHANNEL_SOLO = 23
        const val PAD_STRIP = 66
    }
}

/** The live engine's state as the commands left it (strip → solo, mute; pad → own strip; transport). */
internal class AloneEngineSink : CommandSink {
    val solo = HashMap<Int, Boolean>()
    val mute = HashMap<Int, Boolean>()
    val padStrip = HashMap<Int, Boolean>()
    var playing = false
    private var next = 1L
    @Synchronized override fun post(target: Long, type: Int, value: Float, a: Int, b: Int) {
        if (target != 0L) return
        when (type) {
            AloneListenVmTest.CHANNEL_SOLO -> solo[a] = value > 0.5f
            AloneListenVmTest.CHANNEL_MUTE -> mute[a] = value > 0.5f
            AloneListenVmTest.PAD_STRIP -> padStrip[a] = value > 0.5f
            AloneListenVmTest.TRANSPORT -> playing = value > 0.5f
        }
    }
    override fun scheduleFrom(target: Long, frame: Long) = Unit
    override fun install(target: Long, kind: CommandSink.Install, index: Int, sample: Long) = Unit
    @Synchronized override fun sampleCreate(data: FloatArray, channels: Int, sampleRate: Int): Long = next++
    override fun sampleFrames(handle: Long): Long = 0L
    override fun sampleRelease(handle: Long) = Unit
    @Synchronized override fun exportCreate(): Long = 1000L + next++
    override fun exportRender(target: Long, path: String, frames: Long, tailFrames: Long, fadeOutFrames: Long, autoTail: Boolean, floatWav: Boolean, sampleRate: Int, bits: Int) =
        ExportResult.Ok
    override fun exportRenderFloat(target: Long, frames: Long, tailFrames: Long): FloatArray = FloatArray(2 * 48_000)
    override fun exportDestroy(target: Long) = Unit

    /** Whether strip [ch] is heard by the engine's mixer rules (Mixer::updateTargets), drums pads as the engine routes them. */
    fun heardPad(lane: Int): Boolean {
        val anyPadSolo = (0 until Mixer.PADS).any { padStrip[it] == true && solo[Mixer.padChannel(it)] == true }
        val drumsSolo = solo[0] == true
        val anyTrackSolo = Mixer.TRACK_STRIPS.any { it != Mixer.MASTER && solo[it] == true }
        if (mute[0] == true) return false
        if (padStrip[lane] == true) {
            val s = Mixer.padChannel(lane)
            if (mute[s] == true) return false
            return if (anyPadSolo) solo[s] == true || drumsSolo else !anyTrackSolo || drumsSolo
        }
        // Through the drums' own sound: quiet while a pad is soloed.
        return (!anyPadSolo || drumsSolo) && (!(anyTrackSolo || anyPadSolo) || drumsSolo || anyPadSolo)
    }

    fun heardTrack(ch: Int): Boolean {
        val anySolo = Mixer.TRACK_STRIPS.any { it != Mixer.MASTER && solo[it] == true } ||
            (0 until Mixer.PADS).any { padStrip[it] == true && solo[Mixer.padChannel(it)] == true }
        return mute[ch] != true && (!anySolo || solo[ch] == true)
    }
}
