package com.pocketstudio.app.human

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

/**
 * Banc fiable 3.2 and its review (the bench's song position was too naive: armed swaps added as anchors, a counter that
 * could go back, run past the song's end or start a part early during the count-in). The bench's engine plays the scene
 * swaps as Engine.cpp does: one waits at a time, on a bar line, a new arm replaces it, a negative one cancels it, the song's
 * end stops the transport, the count-in delays everything, a tempo change keeps the steps played. 120 BPM: 8 steps a second,
 * a 4/4 bar = 16 steps = 2 s.
 */
@RunWith(RobolectricTestRunner::class)
class BenchSongClockTest {
    /** PLAN-RESTE 4.5: the state inherited from the test before must be pristine (else: red, naming that test). */
    @get:Rule
    val pristine = BenchPristineRule()
    private val e = BenchEngine()

    private fun play(countInBars: Int = 0, songStep: Int = 0) = e.post(0L, TRANSPORT, 1f, countInBars, songStep)
    private fun stop() = e.post(0L, TRANSPORT, 0f, 0, 0)
    private fun arm(steps: Int, kind: Int, songStep: Int = 0) = e.post(0L, ARM, songStep.toFloat(), steps, kind)
    private fun steps(n: Int) = ShadowSystemClock.advanceBy(Duration.ofMillis(n * 125L))

    private fun start() {
        e.post(0L, TEMPO, 120f, 0, 0)
        e.post(0L, BEATS_PER_BAR, 0f, 4, 0)
    }

    @Test
    fun aSongPlaysOnAndARestartArmedAheadWaitsForItsBar() {
        start()
        play(songStep = 0)
        steps(8)
        assertEquals(8, e.songStep())
        arm(64, 1, songStep = 64)  // the next part (song step 64) after 64 steps of this one
        steps(24)
        assertEquals("not moved before its bar", 32, e.songStep())
        assertEquals(0, e.sceneSwaps())
        steps(34)
        assertEquals("taken over on step 64, the song goes on", 66, e.songStep())
        assertEquals(1, e.sceneSwaps())
    }

    @Test
    fun aNewArmReplacesTheOneWaitingAndANegativeOneCancelsIt() {
        start()
        play(songStep = 0)
        steps(8)
        arm(128, 1, songStep = 500)
        arm(96, 1, songStep = 96)  // armed again (an edit of the part): the second only
        steps(92)
        assertEquals(100, e.songStep())
        assertEquals(1, e.sceneSwaps())
        arm(32, 1, songStep = 900)
        arm(-1, 0)  // cancelled
        steps(64)
        assertEquals("nothing jumped", 164, e.songStep())
        assertEquals(1, e.sceneSwaps())
    }

    @Test
    fun theSongsEndStopsTheTransportOnItsBarLine() {
        start()
        play(songStep = 0)
        steps(4)
        arm(32, 2)
        steps(26)
        assertEquals(30, e.songStep())
        steps(4)
        assertEquals("stopped at step 32: never past the end", -1, e.songStep())
        assertEquals(-1.0, e.playPosition(), 0.0)
        assertEquals(1, e.sceneSwaps())
    }

    @Test
    fun aPartArmedDuringTheCountInWaitsForItsBarAfterIt() {
        start()
        play(countInBars = 1, songStep = 0)
        arm(16, 1, songStep = 16)
        steps(8)
        assertEquals("counting in: no song position yet", -1, e.songStep())
        steps(12)  // 4 steps after the count-in's 16
        assertEquals("the first part plays, not the next", 4, e.songStep())
        steps(16)
        assertEquals(20, e.songStep())
        assertEquals(1, e.sceneSwaps())
    }

    @Test
    fun aTempoChangeKeepsTheStepsPlayed() {
        start()
        play(songStep = 0)
        steps(16)
        e.post(0L, TEMPO, 60f, 0, 0)
        assertEquals("no jump back at 60 BPM", 16, e.songStep())
        ShadowSystemClock.advanceBy(Duration.ofMillis(1_000))  // 4 steps at 60 BPM
        assertEquals(20, e.songStep())
        stop()
        assertEquals(-1, e.songStep())
        assertTrue(e.absoluteStep() < 0)
    }

    /**
     * T5-Ab-27 (round 5, seat A b: « la prise micro continue après la fin du morceau »; recette tel-18 on the tablet: the take
     * stops at the song's end, 137 s, bar 96/96): the engine stops the transport on the song's last bar line and a take ends
     * there with what it recorded, as ■ ends it. The bench's engine stopped its transport but left the take open: back to
     * « waiting », 0 frames, which the seat reported as the app's.
     */
    @Test
    fun aMicTakeEndsWithTheSongsEnd() {
        start()
        e.countInMs = 0
        play(songStep = 0)
        val take = e.micStart(false, 60, 0)
        steps(8)
        assertEquals("recording after ▶", 1f, e.micStatus(take)[0], 0f)
        arm(32, 2)
        steps(30)
        assertEquals("the song's end stopped the transport", -1, e.songStep())
        val st = e.micStatus(take)
        assertEquals("the take is over, as ■ would end it", 2f, st[0], 0f)
        assertEquals("with what it recorded up to the last bar line (32 steps = 4 s)", 4 * 48_000f, st[1], 48_000f / 8)
        assertEquals("micFinish gives those frames", st[1].toInt(), e.micFinish(take)!!.size)
    }

    private companion object {
        const val TRANSPORT = 5
        const val TEMPO = 6
        const val ARM = 40
        const val BEATS_PER_BAR = 57
    }
}
