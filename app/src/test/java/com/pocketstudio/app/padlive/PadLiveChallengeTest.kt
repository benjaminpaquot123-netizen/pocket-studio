package com.pocketstudio.app.padlive

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.PadChallenge
import com.pocketstudio.model.PadLive
import com.pocketstudio.model.PadLiveSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.time.Duration

/**
 * « Défi » (n° 25, lot B3, « refais ce rythme ») through the ViewModel: the rhythm on the page's
 * « coup » pads, its example heard with its pads lit in turn, the player's touches on the pads
 * scored, the best stars kept with the project (not undone) and the next challenge offered.
 */
@RunWith(RobolectricTestRunner::class)
class PadLiveChallengeTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    /** Plays the example as the phone would: [heard] frames went out, [end] says it is over. */
    private class Ears : GuideListenPlayer {
        var played: Pcm? = null
        var heard = 0L
        private var ended: (() -> Unit)? = null
        override fun play(pcm: Pcm, ended: () -> Unit): Boolean { played = pcm; heard = 0; this.ended = ended; return true }
        override fun stop() { ended = null }
        override fun heard(): Long = heard
        fun end() { ended?.invoke(); ended = null }
    }

    private fun idle(ms: Long = 60) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    private fun vm(): RhythmViewModel = RhythmViewModel(app).also { vm ->
        vm.livePadsSoundingForTest = emptySet()
        vm.padLive.toggleForTest = {}
        // Page A: two loops, then « coup » pads on A9 and A10.
        vm.session.edit { p ->
            p.copy(
                tempoBpm = 100.0, padLive = PadLiveSet(home = true),
                live = PadLive.newProject().live.withAt(0, LiveClip("k.wav", "Kick", 4)).withAt(1, LiveClip("b.wav", "Basse", 4))
                    .withAt(8, LiveClip("h.wav", "Klaxon", 1, oneShot = true)).withAt(9, LiveClip("l.wav", "Laser", 1, oneShot = true)),
            )
        }
    }

    @Before
    fun setUp() {
        File(app.filesDir, "projects").deleteRecursively()
    }

    @Test
    fun theRhythmIsHeardWithItsPadsLitThenPlayedBackAndScored() {
        val vm = vm()
        val ears = Ears()
        val c = vm.padLive.challenge
        c.player = ears
        c.exampleForTest = { ch -> Pcm(FloatArray(PadChallenge.exampleFrames(ch, 48_000) * 2), 2, 48_000) }
        var clock = 0L
        c.now = { clock }
        val depth = vm.session.undoDepth
        c.open()
        val ready = c.ui.value as ChallengeUi.Ready
        assertEquals(1, ready.c.level)
        assertEquals("only the « coup » pads", setOf(8), ready.c.hits.map { it.pad }.toSet())
        // « Écouter »: its first hit lights A9, between the hits nothing is lit.
        c.listen()
        idle()
        assertTrue(c.ui.value is ChallengeUi.Listening)
        assertEquals(8, c.litPad())
        ears.heard = 48_000L * 3 / 10  // 0.3 s in: past the light of the first hit (beat = 0.6 s)
        idle()
        assertEquals(null, c.litPad())
        ears.heard = 48_000L * 6 / 10 + 10  // the second hit
        idle()
        assertEquals(8, c.litPad())
        ears.end()
        idle()
        assertEquals(ChallengeUi.YourTurn(ready.c, 0), c.ui.value)
        // Played back on the pads, in time: three stars, kept with the project, the next one offered.
        ready.c.hits.forEachIndexed { i, h ->
            clock = 5_000L + Math.round((h.beat - ready.c.hits[0].beat) * ready.c.beatMs)
            vm.padLive.touch(h.pad, true)
            vm.padLive.touch(h.pad, false)
            if (i < ready.c.hits.lastIndex) assertEquals(i + 1, (c.ui.value as ChallengeUi.YourTurn).taps)
        }
        val done = c.ui.value as ChallengeUi.Done
        assertEquals(3, done.result.stars)
        assertEquals(listOf(3), vm.project.value.padLive.stars)
        assertEquals(2, vm.project.value.padLive.nextChallenge)
        assertEquals("the stars are not an edit to undo", depth, vm.session.undoDepth)
        c.next()
        val second = c.ui.value as ChallengeUi.Ready
        assertEquals(2, second.c.level)
        assertEquals("two « coup » pads in turn", listOf(8, 9, 8, 9), second.c.hits.map { it.pad })
    }

    @Test
    fun aMissedRhythmIsSaidAndOffersAnotherTry() {
        val vm = vm()
        val c = vm.padLive.challenge
        var clock = 0L
        c.now = { clock }
        c.open(2)
        c.yourTurn()
        // The loops' pads are not the rhythm: wrong touches, then « J'ai fini ».
        vm.padLive.touch(0, true)
        clock += 600
        vm.padLive.touch(1, true)
        c.finish()
        val done = c.ui.value as ChallengeUi.Done
        assertEquals(0, done.result.stars)
        assertTrue(resultWords(done.result).isNotBlank())
        assertEquals("nothing won, nothing kept", emptyList<Int>(), vm.project.value.padLive.stars)
        assertEquals("the first one still to win", 1, vm.project.value.padLive.nextChallenge)
        c.again()
        assertEquals(2, (c.ui.value as ChallengeUi.Ready).c.level)
        c.close()
        assertEquals(ChallengeUi.Off, c.ui.value)
    }

    @Test
    fun aPageWithoutCoupPadsSaysSo() {
        val vm = vm()
        vm.padLive.setPage(1)
        vm.padLive.challenge.open()
        assertEquals(ChallengeUi.NoShots, vm.padLive.challenge.ui.value)
    }
}
