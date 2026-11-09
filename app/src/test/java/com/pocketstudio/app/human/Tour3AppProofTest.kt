package com.pocketstudio.app.human

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.AudioClip
import com.pocketstudio.model.AudioTrack
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.PadLive
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.SongPart
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
 * Audit ultra round 3 (lot banc): app or bench? Without the bench (no HumanTester, no BenchEngine), the app's own view
 * models with a plain recording engine, for what testers saw do nothing:
 * - Pad Live's row ▶ / ■ and « Tout couper » (seat C b: « rien ne part », « les boucles continuent »);
 * - the song played from a part, from « ▶ Depuis ma prise », « Tout le morceau, du début » (seats A b, C a).
 * The app gives the engine the right commands and plays as soon as an engine is there (a phone always has one);
 * with none, nothing plays: the bench had let its engine go (see BenchTour3Test).
 */
@RunWith(RobolectricTestRunner::class)
class Tour3AppProofTest {
    /** PLAN-RESTE 4.5: the state inherited from the test before must be pristine (else: red, naming that test). */
    @get:Rule
    val pristine = BenchPristineRule()
    private val app: Application get() = ApplicationProvider.getApplicationContext()

    /** A plain engine: the live engine's commands recorded, nothing else. */
    private class Sink : CommandSink {
        private var next = 1L
        val posts = ArrayList<List<Number>>()
        @Synchronized override fun post(target: Long, type: Int, value: Float, a: Int, b: Int) {
            if (target == 0L) posts += listOf(type, value, a, b)
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

    private fun idle() = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(20))

    private fun open(p: Project): RhythmViewModel {
        val id = "tour3-${System.nanoTime()}"
        ProjectStore(File(app.filesDir, "projects")).save(id, p)
        CurrentProject.set(app, id)
        val vm = RhythmViewModel(app)
        val until = System.currentTimeMillis() + 10_000
        while (vm.project.value.song.size != p.song.size || vm.project.value.audioTracks.size != p.audioTracks.size) {
            assertTrue("the project never opened", System.currentTimeMillis() < until)
            idle(); Thread.sleep(5)
        }
        return vm
    }

    /** Row 1 of page A: two loops and a hit. */
    private fun livePads(): Project = Project().let { p ->
        p.copy(live = p.live.withAt(0, LiveClip("a.wav", "Kick", 4)).withAt(1, LiveClip("b.wav", "Basse", 4)).withAt(2, LiveClip("c.wav", "Clap", 1, oneShot = true)))
    }

    @Test
    fun padLiveRowsAndStopAllGoToTheEngine() {
        val sink = Sink()
        NativeEngine.sink = sink
        val vm = open(livePads())
        // Nothing plays (as the engine says): the row's ▶ starts all three on the next bar line, in one command.
        vm.livePadsSoundingForTest = emptySet()
        var plan: Pair<List<Int>, List<Int>>? = null
        vm.padLive.rowForTest = { _, starting, stopping -> plan = starting to stopping }
        vm.padLive.launchRow(0, 0)
        assertEquals(listOf(0, 1, 2) to emptyList<Int>(), plan)
        vm.padLive.rowForTest = null
        vm.padLive.launchRow(0, 0)
        assertEquals(listOf(listOf(LAUNCH_CLIP_ROW, 0f, 0, PadLive.ROW_SIZE)), sink.of(LAUNCH_CLIP_ROW))
        // Its loops playing: the same ▶ (now ■) stops them, the hit left alone.
        vm.livePadsSoundingForTest = setOf(0, 1)
        vm.padLive.rowForTest = { _, starting, stopping -> plan = starting to stopping }
        vm.padLive.launchRow(0, 0)
        assertEquals(emptyList<Int>() to listOf(0, 1), plan)
        // « Tout couper »: every pad off, in one command.
        vm.stopLive()
        assertEquals(listOf(listOf(TOGGLE_CLIP, 1f, -1, 0)), sink.of(TOGGLE_CLIP))
    }

    /** Two parts, and a voice recorded on the second (a song clip from its bar 2). */
    private fun songWithATake(): Project = Project(song = listOf(SongPart(0), SongPart(0)))
        .copy(audioTracks = listOf(AudioTrack(0, "Voix", clip = AudioClip("voix.wav", "Voix", seconds = 2.0, start = 16, song = true))))

    @Test
    fun theSongPlaysFromAPartOrATakeWithAnEngine() {
        NativeEngine.sink = Sink()
        val vm = open(songWithATake())
        // A part touched (Morceau, « Jouer depuis… »): the song from there.
        vm.playSongPart(1)
        idle()
        assertTrue("▶ from part 2", vm.transport.value.playing)
        assertEquals(1, vm.sessionUi.value.songIndex)
        vm.togglePlay()
        idle()
        assertFalse(vm.transport.value.playing)
        // « ▶ Depuis ma prise »: the song from the part the take starts in, never a loop of one bar.
        vm.playFromTake(0)
        idle()
        assertTrue("▶ from the take", vm.transport.value.playing)
        assertEquals(1, vm.sessionUi.value.songIndex)
        // « Tout le morceau, du début » while it plays: the song again from its first part.
        vm.listenFromStart()
        idle()
        assertTrue(vm.transport.value.playing)
        assertEquals(0, vm.sessionUi.value.songIndex)
    }

    /** The same touches with no engine at all: nothing plays. On the bench, that is the bench's engine let go. */
    @Test
    fun withoutAnEngineNothingPlays() {
        val vm = open(songWithATake())
        vm.playSongPart(1)
        vm.playFromTake(0)
        vm.listenFromStart()
        idle()
        assertFalse(vm.transport.value.playing)
        assertEquals(-1, vm.sessionUi.value.songIndex)
    }

    private companion object {
        const val TOGGLE_CLIP = 46
        const val LAUNCH_CLIP_ROW = 150
    }
}
