package com.pocketstudio.app.rhythm

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.AudioClip
import com.pocketstudio.model.AudioTrack
import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.LiveSlots
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.addAudioTrack
import com.pocketstudio.model.withAudioTrack
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.time.Duration

/**
 * Eight audio tracks through the ViewModel: the fifth to eighth get their strips (32..35) in the
 * engine, their curves (the channel's high bits), their stems, and « Geler la piste » still finds
 * a track free with seven in use; the ninth is refused, said.
 */
@RunWith(RobolectricTestRunner::class)
class EightAudioTracksTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    private class Sink : CommandSink {
        val posts = ArrayList<IntArray>()  // type, a, b
        @Volatile var renders = 0
        private var next = 1L
        @Synchronized override fun post(target: Long, type: Int, value: Float, a: Int, b: Int) {
            if (target == 0L) posts += intArrayOf(type, a, b)
        }
        override fun scheduleFrom(target: Long, frame: Long) = Unit
        override fun install(target: Long, kind: CommandSink.Install, index: Int, sample: Long) = Unit
        @Synchronized override fun sampleCreate(data: FloatArray, channels: Int, sampleRate: Int): Long = next++
        override fun sampleFrames(handle: Long): Long = 0L
        override fun sampleRelease(handle: Long) = Unit
        @Synchronized override fun exportCreate(): Long = next++
        override fun exportRender(target: Long, path: String, frames: Long, tailFrames: Long, fadeOutFrames: Long, autoTail: Boolean, floatWav: Boolean, sampleRate: Int, bits: Int) =
            ExportResult.Ok
        @Synchronized override fun exportRenderFloat(target: Long, frames: Long, tailFrames: Long): FloatArray {
            renders++
            return FloatArray(2 * 4800) { 0.1f * kotlin.math.sin(it * 0.05f) }
        }
        override fun exportDestroy(target: Long) = Unit
    }

    private val sink = Sink()

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
        val id = "eight-${System.nanoTime()}"
        ProjectStore(File(app.filesDir, "projects")).save(id, p)
        com.pocketstudio.app.projects.CurrentProject.set(app, id)
        NativeEngine.sink = sink
        val vm = RhythmViewModel(app)
        waitUntil("the project never opened") { vm.project.value.song.size == p.song.size && vm.project.value.audioTracks.size == p.audioTracks.size }
        return vm
    }

    @Test
    fun eightTracksTheirStripsCurvesAndStems() {
        val vm = open(Project(song = listOf(SongPart(0))))
        repeat(8) { vm.addAudioTrack() }
        assertEquals((0..7).toList(), vm.project.value.audioTracks.map { it.id })
        vm.addAudioTrack()
        assertEquals(8, vm.project.value.audioTracks.size)
        assertTrue(vm.notice.value!!.contains("Huit"))
        // The eighth's strip in the engine: its volume sent on channel 35.
        sink.posts.clear()
        vm.setChannelVolume(35, 0.5f)
        assertTrue("volume of strip 35", sink.posts.any { it[0] == 20 && it[1] == 35 })
        // A curve on track 6's volume: its packed target carries channel 33 (high bits at 24).
        vm.addAutomation(AutoTarget(AutoTarget.VOLUME, AudioTrack.mixerChannel(5)))
        assertEquals(33, vm.project.value.automation.last().target.ch)
        assertTrue("curve target", sink.posts.any { it[0] == 37 && it[2] == ((2 shl 24) or (1 shl 12)) })
        // A clip on track 7: its stem is the strip 34; its live slot is 17.
        var p = vm.project.value
        p = p.withAudioTrack(p.audioTracks.first { it.id == 6 }.copy(clip = AudioClip("x.wav", "Voix", 2.0)))
        assertTrue(stripSounds(p, song = false).any { (label, heard) -> label.second == 34 && heard && label.first.startsWith("audio7") })
        assertEquals(17, LiveSlots.of(SongPart.audio(6)))
    }

    @Test
    fun aTrackFreezesWithSevenAudioTracksInUse() {
        var p = Project(
            synthNotes = NoteClip(notes = listOf(Note(0, 96, 60))),
            song = listOf(SongPart(0)),
        )
        repeat(7) { p = p.addAudioTrack()!! }
        val vm = open(p)
        vm.freezeTrack(0)
        waitUntil("never frozen (${vm.notice.value})") { vm.freezing.value == null && com.pocketstudio.model.Freeze.isFrozen(vm.project.value, 0) }
        val frozen = com.pocketstudio.model.Freeze.audioOf(vm.project.value, 0)
        assertNotNull(frozen)
        assertEquals(7, frozen!!.id)
        assertEquals(Mixer.FIRST_AUDIO2 + 3, AudioTrack.mixerChannel(frozen.id))
        // All eight taken now: the next freeze is refused, said.
        vm.unfreezeTrack(0)
        vm.addAudioTrack()
        vm.freezeTrack(0)
        assertTrue(vm.notice.value!!.contains("huit pistes audio"))
        assertEquals(InstrumentTrack.MAX, 8)
    }
}
