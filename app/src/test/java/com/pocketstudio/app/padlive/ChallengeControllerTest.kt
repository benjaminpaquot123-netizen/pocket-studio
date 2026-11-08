package com.pocketstudio.app.padlive

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.app.human.KnownDefect
import com.pocketstudio.app.human.KnownDefectRule
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.PadChallenge
import com.pocketstudio.model.PadLive
import com.pocketstudio.model.PadLiveSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.time.Duration

/**
 * Plan 4.1 (P3 « tape », round 5 C a #32: the testers got 0 or 1 star tapping « in rhythm »): the challenge's own logic,
 * WITHOUT the bench (no HumanTester, no BenchEngine): [ChallengeController] on a [RhythmViewModel], its clock ([ChallengeController.now])
 * injected, the touches given through [PadLiveController.touch] as the screen's pads give them.
 * - the challenge's beat ([PadChallenge.Challenge.beatMs]) after « À moi ! »: 3 stars;
 * - cause (d): the beat is the PROJECT's tempo (80 to 120 BPM), not a fixed pulse: 500 ms is right at 120 BPM, too fast at 90;
 * - cause (c'): touches while the example plays (« Écoute ») are not counted;
 * - the owner (phone, 2026-11-08, « tant que le clap n'était pas fini il n'acceptait pas mon appui suivant »): touches closer
 *   than the pad's sound, the pad still sounding, are each counted and each sent to the engine (the sound itself is the engine's,
 *   see docs/audits/ultra/tour-5-banc41-rapport.md).
 */
@RunWith(RobolectricTestRunner::class)
class ChallengeControllerTest {
    @get:Rule
    val knownDefects: TestRule = KnownDefectRule()

    private val app: Application = ApplicationProvider.getApplicationContext()

    /** The example's player: [heard] frames went out, [end] says it is over. */
    private class Ears : GuideListenPlayer {
        var heard = 0L
        private var ended: (() -> Unit)? = null
        override fun play(pcm: Pcm, ended: () -> Unit): Boolean { heard = 0; this.ended = ended; return true }
        override fun stop() { ended = null }
        override fun heard(): Long = heard
        fun end() { ended?.invoke(); ended = null }
    }

    private var clock = 0L
    /** The pads the controller sent to the engine (each touch that plays). */
    private val sent = ArrayList<Int>()

    private fun idle(ms: Long = 60) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    /** A project at [bpm] with one « coup » pad (A9) and one more (A10), as a pack gives them; the clock at 0. */
    private fun vm(bpm: Double, sounding: Set<Int> = emptySet()): RhythmViewModel = RhythmViewModel(app).also { vm ->
        vm.livePadsSoundingForTest = sounding
        vm.padLive.toggleForTest = { sent += it }
        vm.session.edit { p ->
            p.copy(
                tempoBpm = bpm, padLive = PadLiveSet(home = true),
                live = PadLive.newProject().live
                    .withAt(8, LiveClip("c.wav", "Clap", 1, oneShot = true)).withAt(9, LiveClip("s.wav", "Snap", 1, oneShot = true)),
            )
        }
        vm.padLive.challenge.now = { clock }
    }

    /** Taps the hits of [c] in order, the first at [from], each [gapMs] after the previous one ([gapMs] null: at [c]'s own places). */
    private fun RhythmViewModel.tap(c: PadChallenge.Challenge, from: Long, gapMs: Double? = null, count: Int = c.hits.size) {
        for (i in 0 until count) {
            val h = c.hits[i % c.hits.size]
            clock = from + Math.round(gapMs?.let { it * i } ?: ((h.beat - c.hits[0].beat) * c.beatMs))
            padLive.touch(h.pad, true)
            padLive.touch(h.pad, false)
        }
    }

    @Before
    fun setUp() {
        File(app.filesDir, "projects").deleteRecursively()
        clock = 0L
        sent.clear()
    }

    /** « À moi ! » then the four hits of « Boum boum » one challenge beat apart (666.7 ms at 90 BPM): perfect. */
    @Test
    fun tapsAtTheChallengesBeatAfterAMoiGiveThreeStars() {
        val vm = vm(90.0)
        val ctl = vm.padLive.challenge
        ctl.open(1)
        val c = (ctl.ui.value as ChallengeUi.Ready).c
        assertEquals(90.0, c.bpm, 0.0)
        assertEquals(666.67, c.beatMs, 0.01)
        ctl.yourTurn()
        vm.tap(c, from = 10_000)
        val done = ctl.ui.value as ChallengeUi.Done
        assertEquals("4 of 4", 4, done.result.right)
        assertEquals(3, done.result.stars)
        assertEquals(listOf(3), vm.project.value.padLive.stars)
    }

    /**
     * Cause (d), confirmed: the pulse is the PROJECT's tempo (kept between 80 and 120 BPM). The same four touches 500 ms apart
     * (the bench's « tape … 500 », a 120 BPM pulse) are perfect on a 120 BPM project and 33 % too fast on a 90 BPM one:
     * 2 stars, not 3. The stars are right here (the example and the tempo light beat 90): the tester did
     * not know the pulse, see the bench's « pulsation N BPM = M ms ».
     */
    @Test
    fun fiveHundredMsTapsAreScoredOnTheProjectsTempo() {
        val fast = vm(120.0)
        fast.padLive.challenge.open(1)
        val c120 = (fast.padLive.challenge.ui.value as ChallengeUi.Ready).c
        assertEquals(500.0, c120.beatMs, 0.0)
        fast.padLive.challenge.yourTurn()
        fast.tap(c120, from = 10_000, gapMs = 500.0)
        assertEquals("500 ms on a 120 BPM challenge", 3, (fast.padLive.challenge.ui.value as ChallengeUi.Done).result.stars)

        clock = 0L
        val slow = vm(90.0)
        slow.padLive.challenge.open(1)
        val c90 = (slow.padLive.challenge.ui.value as ChallengeUi.Ready).c
        slow.padLive.challenge.yourTurn()
        slow.tap(c90, from = 10_000, gapMs = 500.0)
        val r = (slow.padLive.challenge.ui.value as ChallengeUi.Done).result
        println("500 ms on 90 BPM: ${r.right}/${r.total}, ${r.stars} star(s), extra ${r.extra}, tendency ${r.tendency} (« ${resultWords(r)} »)")
        assertEquals("3 of 4 (the second touch just within a sixteenth, the third none)", 3, r.right)
        assertEquals("500 ms on a 90 BPM challenge is not perfect", 2, r.stars)
        // Kept at the challenge's tempo: 80 to 120 BPM whatever the project's.
        assertEquals(120.0, PadChallenge.make(1, listOf(8), 160.0)!!.bpm, 0.0)
        assertEquals(80.0, PadChallenge.make(1, listOf(8), 60.0)!!.bpm, 0.0)
    }

    /**
     * Found on the way (cause d's case): four touches 500 ms apart on a 90 BPM challenge (667 ms) are 33 % too FAST, and the
     * score says « Un peu trop lent. ». [PadChallenge.score]'s tendency compares the span from the first to the last MATCHED
     * hit (hits 1 to 3, 2 beats) with the touches matched to them (touches 1 and 4, 2.25 beats): the fourth touch, taken for
     * the third hit, makes a rushed try look dragged. Right: a player who keeps every gap shorter is told too fast.
     */
    @Test
    @KnownDefect("T5-DEFI-TENDANCE", "four touches 500 ms apart on a 90 BPM challenge are said too slow")
    fun touchesTooCloseAreSaidTooFast() {
        val vm = vm(90.0)
        val ctl = vm.padLive.challenge
        ctl.open(1)
        val c = (ctl.ui.value as ChallengeUi.Ready).c
        ctl.yourTurn()
        vm.tap(c, from = 10_000, gapMs = 500.0)
        val r = (ctl.ui.value as ChallengeUi.Done).result
        assertEquals("four touches 500 ms apart on a 90 BPM challenge are said too slow (« ${resultWords(r)} »)", -1, r.tendency)
    }

    /**
     * Cause (c'), confirmed: while the example plays (« Écoute bien… »), the touches are not counted (they still sound): the
     * bench's « tape » given right after « Écouter » lost them. By design (the panel says to listen and offers « À moi ! »).
     */
    @Test
    fun touchesWhileTheExampleIsHeardAreNotCounted() {
        val vm = vm(90.0)
        val ctl = vm.padLive.challenge
        val ears = Ears()
        ctl.player = ears
        ctl.exampleForTest = { ch -> Pcm(FloatArray(PadChallenge.exampleFrames(ch, 48_000) * 2), 2, 48_000) }
        ctl.open(1)
        val c = (ctl.ui.value as ChallengeUi.Ready).c
        ctl.listen()
        idle()
        assertTrue(ctl.ui.value is ChallengeUi.Listening)
        vm.tap(c, from = 1_000)
        assertTrue("still listening: ${ctl.ui.value}", ctl.ui.value is ChallengeUi.Listening)
        assertEquals("every touch still plays its pad", 4, sent.size)
        ears.end()
        idle()
        assertEquals("nothing counted", ChallengeUi.YourTurn(c, 0), ctl.ui.value)
    }

    /**
     * Cause (c'), its edge: the example ends with a beat to ring after its last hit ([PadChallenge.exampleFrames]); a player who
     * answers in time (the next bar's first beat, one beat after the last hit heard) touches while the panel still says
     * « Écoute »: the touch sounds and is lost, the try then starts one hit late. Right: once the example's last hit has been
     * heard, a touch on the rhythm's pad is the player's first.
     */
    @Test
    @KnownDefect("T5-DEFI-ECOUTE", "a touch on the beat after the example's last hit was not counted")
    fun aTouchOnTheBeatAfterTheExampleCounts() {
        val vm = vm(90.0)
        val ctl = vm.padLive.challenge
        val ears = Ears()
        ctl.player = ears
        ctl.exampleForTest = { ch -> Pcm(FloatArray(PadChallenge.exampleFrames(ch, 48_000) * 2), 2, 48_000) }
        ctl.open(1)
        val c = (ctl.ui.value as ChallengeUi.Ready).c
        ctl.listen()
        idle()
        // The last hit heard and past, the ring nearly over (a finger a hair early on the next beat, as people play).
        val lastHit = PadChallenge.frameOf(c, c.hits.lastIndex, 48_000).toLong()
        ears.heard = lastHit + 48_000L * 95 / 100 * c.beatMs.toLong() / 1000
        idle()
        clock = 20_000
        vm.padLive.touch(c.hits[0].pad, true)
        vm.padLive.touch(c.hits[0].pad, false)
        ears.end()
        idle()
        val taps = (ctl.ui.value as? ChallengeUi.YourTurn)?.taps
        assertEquals("a touch on the beat after the example's last hit was not counted (${ctl.ui.value})", 1, taps)
    }

    /**
     * The owner's remark (phone, 2026-11-08: « comme si tant que le clap n'était pas fini il n'acceptait pas mon appui suivant »):
     * at the challenge's level, every touch is taken, the pad still sounding or not, closer than its sound or not. « Le galop »
     * (eighth notes, 333 ms at 90 BPM) tapped on time with the pad sounding all along: 6 of 6, 3 stars, 6 hits sent to the
     * engine. The touch is not refused here: what the owner heard is the engine's (a « coup » hit again while it sounds waits for
     * the next beat line, a second touch while it waits cancels it), see the round's report.
     */
    @Test
    fun touchesCloserThanTheClapAreAllCountedAndSent() {
        val vm = vm(90.0, sounding = setOf(8, 9))
        val ctl = vm.padLive.challenge
        ctl.open(3)
        val c = (ctl.ui.value as ChallengeUi.Ready).c
        assertEquals("Le galop", c.name)
        assertEquals("eighth notes", 0.5, c.hits[1].beat - c.hits[0].beat, 0.0)
        ctl.yourTurn()
        vm.tap(c, from = 10_000)
        val done = ctl.ui.value as ChallengeUi.Done
        assertEquals(6, done.result.right)
        assertEquals(3, done.result.stars)
        assertEquals("each touch sent to the engine", c.hits.map { it.pad }, sent)
    }

    /**
     * The owner's case, the same with « Boum boum » tapped a little unevenly (± 40 ms, a person's hand) while the clap still
     * rings: nothing is refused, the score stays perfect (well within [PadChallenge.TIGHT_BEATS]).
     */
    @Test
    fun aHumanHandOnASoundingPadStillGetsThreeStars() {
        val vm = vm(90.0, sounding = setOf(8))
        val ctl = vm.padLive.challenge
        ctl.open(1)
        val c = (ctl.ui.value as ChallengeUi.Ready).c
        ctl.yourTurn()
        val wobble = longArrayOf(0, 40, -30, 25)
        c.hits.forEachIndexed { i, h ->
            clock = 10_000 + Math.round(i * c.beatMs) + wobble[i]
            vm.padLive.touch(h.pad, true)
            vm.padLive.touch(h.pad, false)
        }
        val r = (ctl.ui.value as ChallengeUi.Done).result
        assertEquals(4, r.right)
        assertEquals(3, r.stars)
        assertEquals(4, sent.size)
    }
}
