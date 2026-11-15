package com.pocketstudio.app.human

import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * METHODE-V4 §4.5 step 3 (« siège court ancré »): a seat brought straight to a chapter of the tutorial
 * (`amenee=tuto:<style>:<chapitre>`), and a replay that gives the seat back to its tester once the screen
 * no longer sticks to the origin's (`rejeu.puis=main`), instead of ending with a verdict.
 */
class BenchShortSeatTest {
    private val style = TutoCourse.STYLES.first()

    private fun parse(vararg kv: Pair<String, String>) = SeatOptions.parse(linkedMapOf(*kv))

    @Test
    fun aTutoAmeneeNamesItsStyleAndChapter() {
        val (o, warn) = parse("amenee" to "tuto:$style:3")
        assertEquals(emptyList<String>(), warn)
        assertEquals("tuto:$style:3", o.amenee)
        assertEquals(style to 3, SeatOptions.tutoAmenee(o.amenee!!))
        assertTrue(o.describe().contains("amenee=tuto:$style:3"))
        // The other amenées are not the tutorial's.
        assertNull(SeatOptions.tutoAmenee(SeatOptions.AMENEES.first()))
    }

    @Test
    fun aWrongTutoAmeneeIsSaidAndLeftOut() {
        for (bad in listOf("tuto:nulle-part:1", "tuto:$style:0", "tuto:$style:deux", "tuto:$style", "tuto::2")) {
            val (o, warn) = parse("amenee" to bad)
            assertNull(bad, o.amenee)
            assertEquals(bad, 1, warn.size)
            assertTrue(warn.single(), warn.single().contains("tuto:<style>:<chapitre>"))
        }
    }

    @Test
    fun aChapterPastTheCourseIsRefusedWithItsCount() {
        val chapters = TutoCourse.of(style)!!.chaptersFor(TutoLevel.Facile).size
        assertNull(SeatOptions.tutoAmeneeError("tuto:$style:1", TutoLevel.Facile))
        assertNull(SeatOptions.tutoAmeneeError("tuto:$style:$chapters", TutoLevel.Facile))
        val past = SeatOptions.tutoAmeneeError("tuto:$style:${chapters + 1}", TutoLevel.Facile)
        assertNotNull(past)
        assertTrue(past!!, past.contains("$chapters chapitres"))
    }

    @Test
    fun handBackIsAnOption() {
        assertFalse(parse().first.replayHandBack)
        val (o, warn) = parse("rejeu" to "/tmp/seat", "rejeu.puis" to "main")
        assertEquals(emptyList<String>(), warn)
        assertTrue(o.replayHandBack)
        assertTrue(o.describe().contains("rejeu.puis=main"))
        val (o2, warn2) = parse("rejeu.puis" to "fin")
        assertFalse(o2.replayHandBack)
        assertEquals(1, warn2.size)
    }

    @Test
    fun theReplayGivesTheSeatBackWhenTheScreenNoLongerSticks() {
        // The screen sticks, the origin has the gesture: the replay goes on.
        assertNull(SeatOptions.handBackWhy(n = 4, replayN = 0, diverged = false, originHasGesture = true))
        // Screen 4 differs from the origin's: given back, said with the gesture after which it differs.
        val diverged = SeatOptions.handBackWhy(n = 4, replayN = 0, diverged = true, originHasGesture = true)
        assertTrue(diverged!!, diverged.contains("écran 4") && diverged.contains("geste 3"))
        // The origin's gestures are over.
        assertTrue(SeatOptions.handBackWhy(n = 9, replayN = 0, diverged = false, originHasGesture = false)!!.contains("plus de geste"))
        // rejeu.n bounds the replay: past it, the seat is the tester's.
        assertNull(SeatOptions.handBackWhy(n = 5, replayN = 5, diverged = false, originHasGesture = true))
        assertTrue(SeatOptions.handBackWhy(n = 6, replayN = 5, diverged = false, originHasGesture = true)!!.contains("5 gestes"))
        // A tag the screen no longer has.
        assertTrue(SeatOptions.handBackWhy(n = 2, replayN = 0, diverged = false, originHasGesture = true, missingTag = "play")!!.contains("« play »"))
    }
}
