package com.pocketstudio.app.human

import com.pocketstudio.app.remix.SeparationJob
import com.pocketstudio.app.remix.SeparationState
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Banc fiable 1.4 (round 5, seat C b): the Remix's model download and separation are the bench's stand-ins, sleeping in
 * real time; « attends N » moved only the simulated clock, and a tester waited 13 minutes and 6 gestures for nothing.
 * A person who asks for the model then waits twice 30 s gets the separated song, as on a phone.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class BenchSeparationWaitTest : HumanTester("attente") {
    @Before
    fun notOnTheBench() = assumeTrue("not while the bench runs seats", System.getProperty("ps.human.dir").isNullOrBlank())

    /** The stand-in's pace (1: a tester's); [aStuckStandInGivesTheHandBackWithinTwentySeconds] makes it never end. */
    private var scale = 1.0
    override val remixTimeScale: Double get() = scale

    @Test
    fun twoWaitsOfThirtySecondsGiveTheSeparatedSong() {
        begin(SeatOptions(amenee = AMENEE_REMIX))
        amenee(AMENEE_REMIX, File.createTempFile("amenee", ".txt").apply { deleteOnExit() })
        fun tap(tag: String) = act("touche ${centerOf(tag)!!.first} ${centerOf(tag)!!.second}")
        tap("remix_pick")
        assertNotNull(screenLines().joinToString("\n"), centerOf("remix_model_ask_ok"))
        tap("remix_model_ask_ok")
        // What the person reads once the song is separated: its card, with its length, tempo and key (and no progress line).
        fun separated() = screenLines().any { "0:40 · 120 BPM" in it } && screenLines().none { "Séparation de" in it }
        var waits = 0
        while (!separated() && waits < 2) {
            act("attends 30")
            waits++
        }
        assertTrue("separated after $waits « attends 30 »:\n" + screenLines().joinToString("\n"), separated())
    }

    /**
     * PLAN-RESTE 4.4: « attends » gave the bench's slow work up to 120 s of real time, and held on to the separation's own
     * flag alone. When the app is stuck (its flag says « busy », nothing works) or the stand-in never ends, a tester's
     * « attends 2 » cost 2 minutes of nothing. Now it gives the hand back once nothing has moved for 15 s.
     */
    @Test
    fun aStuckStandInGivesTheHandBackWithinTwentySeconds() {
        scale = 100_000.0
        begin(SeatOptions(amenee = AMENEE_REMIX))
        fun timed(what: String): Long {
            val t = System.currentTimeMillis()
            act("attends 2")
            return (System.currentTimeMillis() - t).also { println("PLAN-RESTE 4.4 mesure : « attends 2 », $what : $it ms réelles") }
        }
        // 1. The app says it separates, nothing runs (no thread in the stand-in, no worker busy).
        SeparationJob.set(SeparationState.Separating("figée", 0.37f))
        val flagOnly = timed("drapeau seul")
        // Lot « verdicts »: a random red at 28.9 s could not be explained; the threads that held the wait are named.
        val heldFlag = waitHeldBy
        SeparationJob.set(SeparationState.Idle)
        // 2. The model's download asked for, the stand-in's server stuck after its first megabyte.
        amenee(AMENEE_REMIX, File.createTempFile("amenee", ".txt").apply { deleteOnExit() })
        fun tap(tag: String) = act("touche ${centerOf(tag)!!.first} ${centerOf(tag)!!.second}")
        tap("remix_pick")
        assertNotNull(screenLines().joinToString("\n"), centerOf("remix_model_ask_ok"))
        tap("remix_model_ask_ok")
        val stuck = timed("remplaçant bloqué")
        assertTrue("« attends 2 » gave the hand back after $flagOnly ms (flag alone) and $stuck ms (stand-in stuck): 20 s at most; " +
            "held by: $heldFlag / $waitHeldBy", flagOnly < 20_000 && stuck < 20_000)
    }

    /**
     * Review of « banc6a » (mineur): the other side of [aStuckStandInGivesTheHandBackWithinTwentySeconds]. Work that runs
     * (a worker computing for 25 s while the app says it separates) holds « attends » until it is over, then 15 s of
     * stillness: no guard proved it (the busy worker taken out of the wait, both tests stayed green).
     */
    @Test
    fun aBusyWorkerHoldsTheWaitUntilItsWorkIsOver() {
        begin()
        val stop = java.util.concurrent.atomic.AtomicBoolean(false)
        SeparationJob.set(SeparationState.Separating("calcul", 0.5f))
        com.pocketstudio.app.rhythm.ClosingSaves.launch(60_000) {
            val end = System.nanoTime() + 25_000_000_000L
            while (System.nanoTime() < end && !stop.get()) Thread.onSpinWait()
        }
        try {
            val t = System.currentTimeMillis()
            act("attends 2")
            val took = System.currentTimeMillis() - t
            println("banc6b mesure : « attends 2 », un fil occupé 25 s : $took ms réelles")
            // The work's 25 s, then 15 s of stillness (40 s measured); the busy worker left out of the wait: 25,2 s (the
            // settle's own wait for the work), so the bound is 35 s.
            assertTrue("a worker busy for 25 s held « attends 2 » $took ms: its work, then 15 s of stillness (more than 35 s, less than the 120 s cap)", took in 35_000..100_000)
            // Lot « verdicts » (the random red of the test above at 28.9 s, unexplained): the wait names the thread that
            // held it and where it was, the worker and the frame of the work it ran.
            assertTrue("the wait names the busy worker that held it: « $waitHeldBy »", "DefaultDispatcher-worker" in waitHeldBy)
            assertTrue("and where it was (the work's frame): « $waitHeldBy »", "BenchSeparationWaitTest" in waitHeldBy)
        } finally {
            stop.set(true)
            SeparationJob.set(SeparationState.Idle)
        }
    }
}
