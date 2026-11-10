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
        play(songStep = 0)
        val take = e.micStart(true, 60, 0)
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

    /**
     * Audit « banc crédible » A.9 (F4): one clock. The transport's own step (NativeEngine.transportStep: where a Pad Live take
     * and the launch lines count from) is the engine's, as Engine.cpp stores it on each step: a tempo change keeps the steps
     * played (Sequencer.h only changes the step's length). It came from the bench's play head, which counted the whole time
     * since ▶ at the new tempo (16 steps at 120 BPM read 8 at 60 BPM) and never from the engine itself.
     */
    @Test
    fun transportStepKeepsItsStepsAcrossATempoChange() {
        start()
        play()
        steps(16)
        assertEquals("16 steps played at 120 BPM", 16, e.transportStep())
        e.post(0L, TEMPO, 60f, 0, 0)
        assertEquals("no jump back at 60 BPM", 16, e.transportStep())
        ShadowSystemClock.advanceBy(Duration.ofMillis(1_000))  // 4 steps at 60 BPM
        assertEquals(20, e.transportStep())
        assertEquals(20.0, e.transportPosition(), 0.01)
        stop()
        assertEquals("stopped", -1, e.transportStep())
        assertEquals(-1.0, e.transportPosition(), 0.0)
    }

    /** A.9 (F4): during the count-in the transport has no step yet (Engine.cpp's onStep returns before storing one). */
    @Test
    fun transportStepIsMinusOneDuringTheCountIn() {
        start()
        play(countInBars = 1)
        steps(8)
        assertEquals("counting in: no transport step", -1, e.transportStep())
        assertEquals(-1.0, e.transportPosition(), 0.0)
        assertEquals("half a bar before the « 1 »", -8.0, e.playPosition(), 0.01)
        steps(8 + 8)
        assertEquals("one second after the « 1 »", 8, e.transportStep())
        assertEquals(8.0, e.transportPosition(), 0.01)
    }

    /**
     * A.9 (F4), the count-in too: a take armed « on start » records from the transport's step 0 (Engine.cpp's onStep:
     * recordOnStart_ ends there), so after the count-in the app asked for with ▶. The bench's microphone had a count-in of its
     * own (2 s unless the bench set it): with two bars asked, it recorded a bar early.
     */
    @Test
    fun aTakeWaitsForTheTransportsOwnCountIn() {
        start()
        val take = e.micStart(true, 60, 0)
        assertEquals("armed before ▶: waiting", 0f, e.micStatus(take)[0], 0f)
        play(countInBars = 2)
        steps(24)  // 3 s: one bar and a half of the two counted in
        assertEquals("still counting in: waiting, nothing recorded", 0f, e.micStatus(take)[0], 0f)
        assertEquals(0f, e.micStatus(take)[1], 0f)
        steps(8 + 8)  // the « 1 », then one second
        assertEquals("recording since the « 1 »", 1f, e.micStatus(take)[0], 0f)
        assertEquals("one second of it", 48_000f, e.micStatus(take)[1], 48_000f / 100)
    }

    /**
     * Audit « banc crédible » A.7 (F1): the song that ends by itself is a stop like ■ (Engine.cpp: onStep calls stopTransport
     * on the last bar line). A take armed is kept with what it recorded up to that line (state 2, 32 steps = 32 × 6000
     * frames at 120 BPM), and a song mark still waiting is dropped (stopTransport: markQuantum_ = 0), as ■ drops it. The
     * bench's engine kept that mark: it came true at the next ▶, on a line nobody asked for.
     */
    @Test
    fun aTakeArmedIsKeptWhenTheSongEnds() {
        start()
        val take = e.micStart(true, 60, 0)
        play(songStep = 0)
        steps(8)
        e.post(0L, MARK, 0f, 64, 0)  // a performance take waiting for the launch line of step 64
        arm(32, 2)                   // the song ends at step 32
        steps(30)
        assertEquals("the song's end stopped the transport", -1, e.transportStep())
        val st = e.micStatus(take)
        assertEquals("the take is over", 2f, st[0], 0f)
        assertEquals("kept, with what it recorded up to the last bar line", 32 * 6_000f, st[1], 6_000f)
        assertEquals("nothing marked", -1, e.songMarkStep())
        play(songStep = -1)
        steps(70)
        assertEquals("the mark the song's end dropped does not come true at the next ▶", -1, e.songMarkStep())
        assertEquals("and the song position stays out of the song", -1, e.songStep())
    }

    /**
     * A.7 (F1), the other side of « a stop ends a take »: Engine.cpp's stopTransport ends a recording that runs
     * (« if (record_ && !recordOnStart_) finishRecording() »), not one still waiting for its step 0. ■ during the count-in
     * leaves the take armed: it starts on the « 1 » of the next ▶. The bench's engine ended it there with 0 frames.
     */
    @Test
    fun aTakeStillCountingInIsNotEndedByStop() {
        start()
        val take = e.micStart(true, 60, 0)
        play(countInBars = 1)
        steps(8)
        stop()
        assertEquals("stopped during the count-in: still waiting", 0f, e.micStatus(take)[0], 0f)
        assertEquals(0f, e.micStatus(take)[1], 0f)
        play(countInBars = 1)
        steps(16 + 8)
        assertEquals("it records from the « 1 » of the next ▶", 1f, e.micStatus(take)[0], 0f)
        assertEquals(48_000f, e.micStatus(take)[1], 48_000f / 100)
        stop()
        assertEquals("■ while it records ends it", 2f, e.micStatus(take)[0], 0f)
    }

    /**
     * A.7, the song mark as Engine.cpp plays it (MarkSong, onStep, SetTransport, stopTransport): it comes true on the first
     * step from the next one that its quantum divides, puts the song there (song step [value] on that line), stays readable
     * after ■ until the next ▶, survives ▶ when asked before it (the line is then step 0), and a quantum of 0 cancels it.
     */
    @Test
    fun aSongMarkComesTrueOnItsLineAndPutsTheSongThere() {
        start()
        play(songStep = -1)  // a scene on its own, outside the song
        steps(5)
        e.post(0L, MARK, 32f, 16, 0)
        steps(5)
        assertEquals("before its line", -1, e.songMarkStep())
        assertEquals(-1, e.songStep())
        steps(8)
        assertEquals("marked on step 16", 16, e.songMarkStep())
        assertEquals("the song is at step 32 on that line: 34 two steps later", 34, e.songStep())
        stop()
        assertEquals("still readable after ■", 16, e.songMarkStep())
        play(songStep = -1)
        steps(20)
        assertEquals("▶ forgets the last mark, and marks nothing by itself", -1, e.songMarkStep())
        stop()
        e.post(0L, MARK, 0f, 16, 0)  // asked before ▶
        play(songStep = -1)
        steps(2)
        assertEquals("the mark asked before ▶ comes true on step 0", 0, e.songMarkStep())
        assertEquals(2, e.songStep())
        stop()
        e.post(0L, MARK, 0f, 16, 0)
        e.post(0L, MARK, 0f, 0, 0)  // cancelled (the app's cancelPerformance)
        play(songStep = -1)
        steps(20)
        assertEquals("a cancelled mark never comes", -1, e.songMarkStep())
    }

    /**
     * Audit « banc crédible » A.15 (F5): the positions the screens read are local to the scene playing. Engine.cpp's
     * absoluteStep_ and playPosition_ count from the scene's start (onStep: local = step - sceneStart_; swapScene:
     * sceneStart_ = step), where the transport's own step goes on from ▶. The bench's engine counted both from ▶: after a part
     * took over at step 32, step 40 read 40 instead of 8, and a note played there was written 32 steps too far in its part.
     */
    @Test
    fun absoluteStepRestartsAtTheSwap() {
        start()
        play(songStep = 0)
        steps(20)
        assertEquals("before the swap: the scene started with ▶", 20, e.absoluteStep())
        arm(32, 1, songStep = 32)  // the next part takes over after 32 steps of this one
        steps(20)
        assertEquals(1, e.sceneSwaps())
        assertEquals("8 steps into the part that took over at 32", 8, e.absoluteStep())
        assertEquals("the play position is local too", 8.0, e.playPosition(), 0.01)
        assertEquals("the song goes on", 40, e.songStep())
        assertEquals("and so does the transport's own step", 40, e.transportStep())
        assertEquals(40.0, e.transportPosition(), 0.01)
    }

    /**
     * A.8 and A.15, the drum pattern's own step across a swap, from the app's commands as Engine.cpp plays them: the
     * pattern's length staged for the waiting scene (SetStaging, SetPatternSteps) is taken at the swap, not before; the
     * drums' slot counts from the swap, or from further in when the part starts later in (SetSlotOffset); no step while
     * stopped or counting in.
     */
    @Test
    fun theDrumPatternsStepFollowsTheSceneTakingOver() {
        start()
        e.post(0L, PATTERN_STEPS, 0f, 16, 0)
        play(songStep = 0)
        steps(40)
        assertEquals("step 40 of the transport: step 8 of a 16-step pattern", 8, e.currentStep())
        e.post(0L, STAGING, 1f, 0, 0)
        e.post(0L, PATTERN_STEPS, 0f, 32, 0)   // the next part's drums: 32 steps
        e.post(0L, SLOT_OFFSET, 0f, 0, 16)     // started one bar in
        e.post(0L, STAGING, 0f, 0, 0)
        arm(64, 1, songStep = 64)
        steps(20)
        assertEquals("the staged pattern waits for the swap: step 60 is still step 12 of 16", 12, e.currentStep())
        steps(8)
        assertEquals("4 steps into the part that took over at 64", 4, e.absoluteStep())
        assertEquals("its drums started 16 steps in: step 20 of 32", 20, e.currentStep())
        stop()
        assertEquals("stopped", -1, e.currentStep())
        play(countInBars = 1)
        steps(4)
        assertEquals("counting in", -1, e.currentStep())
        steps(12 + 35)
        assertEquals("▶ starts the pattern from its first step: step 35 is step 3 of 32", 3, e.currentStep())
    }

    private companion object {
        const val PATTERN_STEPS = 9
        const val STAGING = 39
        const val SLOT_OFFSET = 73
        const val TRANSPORT = 5
        const val MARK = 75
        const val TEMPO = 6
        const val ARM = 40
        const val BEATS_PER_BAR = 57
    }
}
