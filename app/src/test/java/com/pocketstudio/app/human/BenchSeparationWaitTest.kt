package com.pocketstudio.app.human

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

    @Test
    fun twoWaitsOfThirtySecondsGiveTheSeparatedSong() {
        begin(SeatOptions(amenee = AMENEE_REMIX))
        amenee(AMENEE_REMIX, File.createTempFile("amenee", ".txt").apply { deleteOnExit() })
        fun tap(tag: String) = act("touche ${centerOf(tag)!!.first} ${centerOf(tag)!!.second}")
        tap("remix_pick")
        assertNotNull(screenLines().joinToString("\n"), centerOf("remix_model_ask_ok"))
        tap("remix_model_ask_ok")
        var waits = 0
        while (centerOf("remix_open_board") == null && waits < 2) {
            act("attends 30")
            waits++
        }
        assertTrue("separated after $waits « attends 30 »:\n" + screenLines().joinToString("\n"), centerOf("remix_open_board") != null)
    }
}
