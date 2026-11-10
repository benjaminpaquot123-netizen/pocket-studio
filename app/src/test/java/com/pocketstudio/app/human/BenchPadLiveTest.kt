package com.pocketstudio.app.human

import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.engine.NativeSample
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

/**
 * Audit « banc crédible » A.16 (F3, G2): the bench's engine plays the Live pads as the phone's ClipLauncher does, from the
 * commands the app sends it (NativeEngine.setClip, setClipParams, toggleClip, launchClipRow; their states read back by
 * NativeEngine.clipStatesNow, as the app reads them). Every number here is the real engine's: the same commands posted to
 * ps::Engine (libps_core.a built on the host, 1 ms blocks), see docs/audits/ultra/tour-5-pads-live-rapport.md.
 * States: 0 off, 1 waiting for its line, 2 playing.
 */
@RunWith(RobolectricTestRunner::class)
class BenchPadLiveTest {
    /** PLAN-RESTE 4.5: the state inherited from the test before must be pristine (else: red, naming that test). */
    @get:Rule
    val pristine = BenchPristineRule()

    @After
    fun unplug() {
        NativeEngine.sink = null
    }

    /** A new bench engine as the app's (its pads' clock starts with it), at [bpm]. */
    private fun engine(bpm: Double = 120.0): BenchEngine = BenchEngine().also {
        NativeEngine.sink = it
        NativeEngine.setTempo(bpm)
        NativeEngine.setBeatsPerBar(4)
    }

    private fun ms(n: Long) = ShadowSystemClock.advanceBy(Duration.ofMillis(n))

    /** Pad [pad] holds a sound of [ms] ms at 48 kHz, a loop of [beats] beats or a hit ([oneShot]). */
    private fun pad(pad: Int, ms: Int, beats: Int, oneShot: Boolean) {
        NativeEngine.setClip(pad, NativeSample.create(FloatArray(ms * 48) { 0.2f }, 1, 48_000))
        NativeEngine.setClipParams(pad, beats, oneShot, 1f)
    }

    private fun state(pad: Int): Int = IntArray(NativeEngine.LIVE_PADS).also(NativeEngine::clipStatesNow)[pad]

    /**
     * The mirror of the engine's own test of T5-PADRETRIG (engine/host-tests/padlive_tests.inc, known_defect_t5_padretrig:
     * same tempos, sounds and touches). Transport stopped, a « coup » pad (a hit of 1 beat) touched again while its sound
     * still plays waits for the next beat line of the project's grid, a touch during that wait cancels it, and once all is
     * off the next touch restarts the clock and sounds at once. This is the phone's behaviour TODAY (recette tel-17): when
     * the engine is fixed, its host test and this one are inverted together.
     */
    @Test
    fun aHitRetouchedWaitsForTheGrid() {
        // 90 BPM (a beat: 667 ms), a clap of 800 ms: the second touch a hair after beat 1.
        engine(90.0)
        pad(0, 800, 1, true)
        NativeEngine.toggleClip(0)
        ms(1)
        assertEquals("the first touch sounds at once", 2, state(0))
        ms(666)
        NativeEngine.toggleClip(0)
        assertEquals("touched again while it sounds: it waits for the grid (T5-PADRETRIG)", 1, state(0))
        ms(664)
        assertEquals("still silent at 1331 ms", 1, state(0))
        ms(4)
        assertEquals("heard a whole beat late, on beat 2 (1333 ms)", 2, state(0))

        // 90 BPM, eighth notes (333 ms) with a 400 ms clap: the 2nd touch waits, the 3rd cancels it, the 4th sounds at once.
        engine(90.0)
        pad(0, 400, 1, true)
        NativeEngine.toggleClip(0)
        ms(333)
        NativeEngine.toggleClip(0)
        assertEquals("the 2nd touch waits for beat 1 (667 ms)", 1, state(0))
        ms(333)
        NativeEngine.toggleClip(0)
        assertEquals("the waiting hit is cancelled, never heard", 0, state(0))
        ms(333)
        NativeEngine.toggleClip(0)
        ms(1)
        assertEquals("everything off: the clock restarts, this one sounds at once", 2, state(0))

        // A 140 BPM project (a beat: 429 ms): the wait follows the project's grid. Retouched at 100 ms, the hit comes at 429 ms.
        engine(140.0)
        pad(0, 800, 1, true)
        NativeEngine.toggleClip(0)
        ms(100)
        NativeEngine.toggleClip(0)
        assertEquals(1, state(0))
        ms(328)
        assertEquals("still waiting at 428 ms", 1, state(0))
        ms(2)
        assertEquals("on the project's beat 1 (429 ms)", 2, state(0))
    }

    /**
     * Loops (real engine: 120 BPM, a loop of 4 beats on at 0 ms, then a loop of 2 beats and one of 8 touched at 300 ms): the
     * second waits for its own grid (2 beats, 1000 ms), a touch while it waits cancels it; the third for a bar at most
     * (2000 ms). A loop touched again stops; « Tout couper » (toggleClip(-1)) stops every pad.
     */
    @Test
    fun aLoopWaitsForItsGridAndATouchWhileItWaitsCancelsIt() {
        engine()
        pad(0, 40, 4, false)
        pad(1, 40, 2, false)
        pad(2, 40, 8, false)
        NativeEngine.toggleClip(0)
        ms(1)
        assertEquals("transport stopped, nothing on: at once", 2, state(0))
        ms(299)
        NativeEngine.toggleClip(1)
        NativeEngine.toggleClip(2)
        assertEquals(1, state(1))
        assertEquals(1, state(2))
        ms(300)
        NativeEngine.toggleClip(1)
        assertEquals("touched while it waits: cancelled", 0, state(1))
        ms(1399)
        assertEquals("a loop of 8 beats waits for the bar (2000 ms)", 1, state(2))
        ms(2)
        assertEquals(2, state(2))
        ms(299)
        NativeEngine.toggleClip(0)
        assertEquals("a loop touched again stops", 0, state(0))
        assertEquals(2, state(2))
        ms(200)
        NativeEngine.toggleClip(-1)
        assertEquals("« Tout couper »", 0, state(2))
    }

    /**
     * A row (real engine: 120 BPM, a loop of 1 beat playing since 0 ms; the row 4..7 = a loop of 4 beats, one of 2, a hit of
     * 200 ms, an empty pad, launched at 750 ms): all wait for the next bar line (2000 ms), not their own grid; the hit ends
     * after its sound (2200 ms); launched again while its loops play, they stop and the pads outside the row go on.
     */
    @Test
    fun aRowWaitsForTheBarLineAndAgainStopsItsLoops() {
        engine()
        pad(0, 40, 1, false)
        pad(4, 40, 4, false)
        pad(5, 40, 2, false)
        pad(6, 200, 1, true)
        NativeEngine.toggleClip(0)
        ms(750)
        NativeEngine.launchClipRow(4, 4)
        assertEquals(listOf(1, 1, 1, 0), (4..7).map(::state))
        ms(1249)
        assertEquals("beats 2 and 3 (the 2-beat loop's grid, a hit's beat) passed: still waiting", listOf(1, 1, 1), (4..6).map(::state))
        ms(2)
        assertEquals("together on the bar line", listOf(2, 2, 2), (4..6).map(::state))
        ms(200)
        assertEquals("the hit played once", 0, state(6))
        ms(299)
        NativeEngine.launchClipRow(4, 4)
        assertEquals("again: its loops stop", listOf(0, 0), (4..5).map(::state))
        assertEquals("the pad outside the row goes on", 2, state(0))
    }

    /** A row launched with the transport stopped and nothing on (real engine: at once), its hit ending after its sound. */
    @Test
    fun aRowAloneStartsAtOnce() {
        engine()
        pad(4, 40, 4, false)
        pad(5, 40, 2, false)
        pad(6, 200, 1, true)
        ms(300)
        NativeEngine.launchClipRow(4, 4)
        ms(1)
        assertEquals(listOf(2, 2, 2), (4..6).map(::state))
        ms(200)
        assertEquals(listOf(2, 2, 0), (4..6).map(::state))
    }

    /**
     * ▶ snaps the pads' clock to the transport's beat (ClipLauncher::align). Real engine, 120 BPM: a loop on at 0 ms, ▶ at
     * 700 ms (the clock at beat 1.4 goes back to 0, the loop starts over), a loop of 4 beats touched at 1000 ms waits for
     * the transport's bar line (2700 ms), not the pads' old one (2000 ms). ■ leaves the clock running: a hit touched at
     * 3100 ms while the loops play waits for the next beat (3200 ms) and plays 200 ms.
     */
    @Test
    fun theTransportsStartPutsThePadsOnItsBeat() {
        engine()
        pad(0, 40, 4, false)
        pad(1, 40, 4, false)
        pad(2, 200, 1, true)
        NativeEngine.toggleClip(0)
        ms(700)
        NativeEngine.play()
        ms(300)
        NativeEngine.toggleClip(1)
        ms(1699)
        assertEquals("on the transport's bar, not the pads' old one", 1, state(1))
        ms(2)
        assertEquals(2, state(1))
        ms(299)
        NativeEngine.stopTransport()
        ms(100)
        NativeEngine.toggleClip(2)
        assertEquals(1, state(2))
        ms(99)
        assertEquals(1, state(2))
        ms(2)
        assertEquals(2, state(2))
        ms(200)
        assertEquals(0, state(2))
    }

    /**
     * During the count-in the transport runs: no restart of the clock (real engine, 120 BPM, ▶ with a bar of count-in at
     * 1 ms, a loop touched at 501 ms waits for the clock's bar line, 2000 ms), then the « 1 » (2001 ms) snaps the clock: a
     * loop touched at 2301 ms starts on the transport's next bar (4001 ms).
     */
    @Test
    fun aPadTouchedDuringTheCountInStartsOnTheClocksBar() {
        engine()
        pad(0, 40, 4, false)
        pad(1, 40, 4, false)
        ms(1)
        NativeEngine.play(countInBars = 1)
        ms(500)
        NativeEngine.toggleClip(0)
        assertEquals(1, state(0))
        ms(1498)
        assertEquals(1, state(0))
        ms(2)
        assertEquals(2, state(0))
        ms(300)
        NativeEngine.toggleClip(1)
        ms(1699)
        assertEquals(1, state(1))
        ms(2)
        assertEquals(2, state(1))
    }
}
