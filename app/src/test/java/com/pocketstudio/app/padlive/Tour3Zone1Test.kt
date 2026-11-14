package com.pocketstudio.app.padlive

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.LiveSlots
import com.pocketstudio.model.PadChallenge
import com.pocketstudio.model.PadLive
import com.pocketstudio.model.PadLiveSet
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.Remix
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.StemMix
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.time.Duration

/**
 * Audit ultra round 3, zone 1, without the bench (the app's view model and a plain engine that records its commands):
 * - n° 1: ← out of Pad Live (the home's « silence », vm.stop) left the live loops playing with the transport stopped;
 * - n° 3: a live pad touched goes to the engine (the seat's « listo » pad that never played: the bench), and a Live column
 *   whose sound Mix cuts shows muted, its speaker giving the sound back;
 * - n° 5: the score shows before the panel's buttons (or the level list) answer again.
 */
@RunWith(RobolectricTestRunner::class)
class Tour3Zone1Test {
    private val app: Application get() = ApplicationProvider.getApplicationContext()

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
        val id = "t3z1-${System.nanoTime()}"
        ProjectStore(File(app.filesDir, "projects")).save(id, p)
        CurrentProject.set(app, id)
        val vm = RhythmViewModel(app)
        val until = System.currentTimeMillis() + 10_000
        while (vm.project.value.song.size != p.song.size || vm.project.value.live.at(0)?.name != p.live.at(0)?.name) {
            assertTrue("the project never opened", System.currentTimeMillis() < until)
            idle(); Thread.sleep(5)
        }
        return vm
    }

    private fun pads(): Project = Project(song = listOf(SongPart(0), SongPart(0))).let { p ->
        p.copy(live = p.live.withAt(0, LiveClip("a.wav", "Boucle de kick", 4)).withAt(1, LiveClip("b.wav", "Basse", 4)))
    }

    @Test
    fun leavingPadLiveSilencesItsLoops() {
        val sink = Sink()
        NativeEngine.sink = sink
        val vm = open(pads())
        // Two loops started from Pad Live, the transport stopped (they play on their own).
        vm.livePadsSoundingForTest = setOf(0, 1)
        assertFalse(vm.transport.value.playing)
        sink.posts.clear()
        // ← (Pad Live's close goes home, and home's silence is vm.stop()): every pad off, in one command.
        vm.stop()
        assertEquals(listOf(listOf(TOGGLE_CLIP, 1f, -1, 0)), sink.of(TOGGLE_CLIP))
        // Nothing sounding: no command at all.
        vm.livePadsSoundingForTest = emptySet()
        sink.posts.clear()
        vm.stop()
        assertTrue(sink.of(TOGGLE_CLIP).isEmpty())
    }

    @Test
    fun aLivePadTouchedGoesToTheEngine() {
        val sink = Sink()
        NativeEngine.sink = sink
        val vm = open(pads())
        vm.livePadsSoundingForTest = emptySet()
        sink.posts.clear()
        // Live › Pads, the seat's « Voz · listo » pad: the app asks the engine to play it (the bench never heard it).
        vm.toggleLive(0)
        assertEquals(listOf(listOf(TOGGLE_CLIP, 1f, 0, 0)), sink.of(TOGGLE_CLIP))
    }

    @Test
    fun aColumnCutInMixShowsMutedAndItsSpeakerGivesTheSoundBack() {
        NativeEngine.sink = Sink()
        // A song put in « muette, pour la découper »: its four parts muted in Mix.
        val p = pads().copy(remix = Remix(setId = "set", name = "Ma chanson préférée", songBpm = 120.0, stems = List(Remix.STEMS) { StemMix(mute = true) }))
        val vm = open(p)
        val remix = LiveSlots.key(LiveSlots.REMIX)
        assertEquals(setOf(remix), vm.liveCutElsewhere())
        assertFalse("the Live page's own mute is not set", remix in vm.liveMuted.value)
        // Its speaker: the parts heard again, in one undo step, and said.
        val depth = vm.session.undoDepth
        vm.toggleLiveMute(remix)
        assertTrue(vm.project.value.remix.stems.none { it.mute })
        assertEquals(emptySet<String>(), vm.liveCutElsewhere())
        assertEquals(depth + 1, vm.session.undoDepth)
        assertEquals("« Ma chanson préférée » était coupé dans Mix : il joue de nouveau.", vm.notice.value)
        // A strip muted in Mix (the drums): shown muted too; the Live page's own M still works as before.
        vm.toggleMute(0)
        assertEquals(setOf(LiveSlots.key(LiveSlots.DRUMS)), vm.liveCutElsewhere())
        vm.toggleLiveMute(remix)
        assertTrue(remix in vm.liveMuted.value)
        vm.toggleLiveMute(remix)
        assertFalse(remix in vm.liveMuted.value)
    }

    @Test
    fun theScoreShowsBeforeTheButtonsAnswerAgain() {
        val vm = RhythmViewModel(app)
        vm.livePadsSoundingForTest = emptySet()
        vm.padLive.toggleForTest = {}
        vm.session.edit { p ->
            p.copy(tempoBpm = 100.0, padLive = PadLiveSet(home = true), live = PadLive.newProject().live.withAt(8, LiveClip("h.wav", "Claquement de mains", 1, oneShot = true)))
        }
        val c = vm.padLive.challenge
        var clock = 0L
        c.now = { clock }
        c.open()
        val ready = c.ui.value as ChallengeUi.Ready
        c.yourTurn()
        // Seat A a: four taps, the last one scores at once; the same finger lands on « Défi suivant » or on the list's 5.
        ready.c.hits.forEach { h ->
            clock += Math.round(ready.c.beatMs)
            vm.padLive.touch(h.pad, true)
            vm.padLive.touch(h.pad, false)
        }
        val done = c.ui.value as ChallengeUi.Done
        clock += 300
        c.next()
        c.open(5)
        c.again()
        assertEquals("the stars stay in sight", done, c.ui.value)
        clock += ChallengeController.SCORE_SHOWN_MS
        c.open(5)
        assertEquals(5, (c.ui.value as ChallengeUi.Ready).c.level)
        assertTrue(PadChallenge.LEVELS.size >= 5)
    }

    private companion object {
        const val TOGGLE_CLIP = 46
    }
}
