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

    // Tour 7, banc B2: the tour 6 replays gave the seat back at gesture 1 of 7 (B/c) and 4 of 17 (B/b): the app had moved
    // « Simple ou Pro ? » down the screen (same words), the bench compared the places too.
    private fun seen(path: String) = java.io.File("../docs/audits/ultra/sieges/$path").readLines().drop(1)

    @Test
    fun aScreenWhoseWordsOnlyMovedIsTheOriginsForAReplayHandingBack() {
        val was = seen("tour-6/lotB/a/screen-2.txt")
        val now = seen("tour-7/lotB/c/screen-2.txt")
        assertFalse("the places did move", ScreenText.fingerprintOf(was) == ScreenText.fingerprintOf(now))
        assertEquals(ScreenText.fingerprintOf(was, places = false), ScreenText.fingerprintOf(now, places = false))
        val other = now.map { it.replace("Simple", "Sencillo") }
        assertFalse("changed words still differ", ScreenText.fingerprintOf(was, places = false) == ScreenText.fingerprintOf(other, places = false))
    }

    @Test
    fun aReplayedTouchOnAMovedButtonAimsAtItsTag() {
        assertEquals("mode_card_simple", SeatOptions.reaimTag("mode_card_simple « Simples »", "rien"))
        assertEquals("mode_card_simple", SeatOptions.reaimTag("mode_card_simple « Simples »", "rien (texte « Como você quer ver »)"))
        assertEquals("cut_to_pads", SeatOptions.reaimTag("dans:cut_to_pads « Nos pads »", "rien"))
        assertEquals("help_x", SeatOptions.reaimTag("désactivé help_x « ? »", "rien"))
        // ScreenText.under's order: « désactivé » first, then « dans: ».
        assertEquals("cut_to_pads", SeatOptions.reaimTag("désactivé dans:cut_to_pads « Nos pads »", "rien"))
        assertNull("still under the finger", SeatOptions.reaimTag("mode_card_simple « Simples »", "mode_card_simple « Simples »"))
        assertNull("nothing aimed at", SeatOptions.reaimTag("rien (texte « a »)", "mode_card_pro « Pro »"))
        assertNull("no tag", SeatOptions.reaimTag("#12 « Ok »", "rien"))
        assertNull("a view", SeatOptions.reaimTag("vue « Ok »", "rien"))
        assertNull("no origin line", SeatOptions.reaimTag(null, "rien"))
    }

    @Test
    fun aSeatGivenBackEarlySaysTheLineAndIsNoFaithfulReplay() {
        val early = SeatOptions.handBackNote(2, 7, "écran 2 différent de l'origine (après le geste 1)", "origine « a », rejeu « b »")
        assertTrue(early, early.contains("rejeu non fidèle"))
        assertTrue(early, early.contains("origine « a », rejeu « b »"))
        val full = SeatOptions.handBackNote(8, 7, "les 7 gestes du rejeu joués", null)
        assertFalse(full, full.contains("non fidèle"))
        assertTrue(full, full.startsWith("main rendue au pas 8"))
    }
}
