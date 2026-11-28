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
    // Tour 8, banc9 (point g): the audit's screens are copied as test resources (app/src/test/resources/bench/).
    private fun seen(name: String) = javaClass.getResource("/bench/$name")!!.readText().removeSuffix("\n").lines().drop(1)

    @Test
    fun aScreenWhoseWordsOnlyMovedIsTheOriginsForAReplayHandingBack() {
        val was = seen("tour-6-lotB-a-screen-2.txt")
        val now = seen("tour-7-lotB-c-screen-2.txt")
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

    @Test
    fun theAuditScreensReadAreTestResources() {
        // Tour 8, banc9 (point g): the bench's tests read copies under app/src/test/resources/bench/, not the audit folders.
        for (name in listOf("tour-6-lotB-a-screen-2.txt", "tour-7-lotB-c-screen-2.txt"))
            assertNotNull(name, javaClass.getResource("/bench/$name"))
    }

    @Test
    fun aHalfDrawnTargetIsAimedAtInItsDrawnPart() {
        // Tour 8, banc9 (points a, b): a button whose bottom half is cut by its parent: the finger aims at the middle of
        // the drawn half, no swipe; a sliver too thin to touch says null (then, and only then, the bench scrolls).
        val node = SeatOptions.Box(100f, 900f, 400f, 1100f)
        val drawn = SeatOptions.Box(100f, 900f, 400f, 1000f)
        val at = SeatOptions.drawnAim(node, drawn, 1080, 2340, 3f)
        assertNotNull(at)
        assertEquals(250f, at!!.first, 0.5f)
        assertEquals(950f, at.second, 0.5f)
        assertNull("a 20 px sliver at density 3", SeatOptions.drawnAim(node, SeatOptions.Box(100f, 900f, 400f, 920f), 1080, 2340, 3f))
        // A small node wholly drawn stays aimable, however small.
        val dot = SeatOptions.Box(10f, 10f, 40f, 40f)
        assertNotNull(SeatOptions.drawnAim(dot, dot, 1080, 2340, 3f))
        // Off the window: the window clips the drawn part.
        val low = SeatOptions.drawnAim(node, SeatOptions.Box(100f, 2200f, 400f, 2500f), 1080, 2340, 3f)
        assertNotNull(low)
        assertEquals(2270f, low!!.second, 0.5f)
    }

    @Test
    fun aReplayWithNoBoundIsFaithfulWhenTheOriginIsPlayedOut() {
        // Tour 8, banc9 (point f): rejeu.n=0 (no bound) used to read « non fidèle (… sur 0 rejoués) » even when every
        // gesture of the origin had been played.
        val done = SeatOptions.handBackNote(9, 0, "plus de geste à l'origine (pas 9)", null, originDone = true)
        assertFalse(done, done.contains("non fidèle"))
        val early = SeatOptions.handBackNote(3, 0, "écran 3 différent de l'origine (après le geste 2)", "origine « a », rejeu « b »")
        assertTrue(early, early.contains("rejeu non fidèle (2 gestes de l'origine rejoués)"))
        assertTrue(early, early.contains("écran changé : origine « a », rejeu « b »"))
    }

    @Test
    fun anUntaggedButtonThatMovedGivesTheSeatBack() {
        // Tour 8, banc9 (point f): the origin's finger was on an untagged button; at the replay the same pixels are not.
        assertEquals("Ok", SeatOptions.goneWords("#12 « Ok »", "rien"))
        assertEquals("Ok", SeatOptions.goneWords("désactivé #12 « Ok »", "#13 « Annuler »"))
        assertEquals("Lancer", SeatOptions.goneWords("vue « Lancer »", "rien"))
        assertEquals("Nos pads", SeatOptions.goneWords("dans:cut_to_pads « Nos pads »", "rien"))
        assertNull("still there", SeatOptions.goneWords("#12 « Ok »", "#14 « Ok »"))
        assertNull("a tagged button is re-aimed, not handed back", SeatOptions.goneWords("mode_card_simple « Simples »", "rien"))
        assertNull("nothing under the origin's finger", SeatOptions.goneWords("rien (texte « a »)", "rien"))
        assertNull(SeatOptions.goneWords(null, "rien"))
        val why = SeatOptions.handBackWhy(4, 0, diverged = false, originHasGesture = true, gone = "Ok")
        assertNotNull(why)
        assertTrue(why!!, why.contains("« Ok »"))
    }

    @Test
    fun aContainerTagWithManyButtonsIsNotReaimed() {
        // Tour 8, banc9 (point f): « dans:list_x » names a container; re-aiming at its middle hits any of its 12 buttons.
        assertNull(SeatOptions.reaimTag("dans:list_x « Kick »", "rien") { 12 })
        assertEquals("cut_to_pads", SeatOptions.reaimTag("dans:cut_to_pads « Nos pads »", "rien") { 1 })
        assertEquals("mode_card_simple", SeatOptions.reaimTag("mode_card_simple « Simples »", "rien") { 12 })
    }

    @Test
    fun aTouchOnATagDrawnInsideTheTargetIsOnTheTarget() {
        // Tour 8, banc9 (point f): remix_pick holds remix_pick_thumb; the finger under the thumb is on remix_pick.
        assertTrue(SeatOptions.fingerOn("remix_pick", "remix_pick_thumb « Démo »", setOf("remix_pick_thumb")))
        assertFalse(SeatOptions.fingerOn("remix_pick", "remix_pick_thumb « Démo »"))
        assertTrue(SeatOptions.fingerOn("remix_pick", "dans:remix_pick « Démo »"))
        assertFalse(SeatOptions.fingerOn("remix_pick", "tab_Sons « Sons »", setOf("remix_pick_thumb")))
        assertTrue(SeatOptions.fingerOn("vue_Ok", "vue « Ok » (bouton)"))
        assertTrue(SeatOptions.fingerOn("tuto_chapter_*", "tuto_chapter_8 « 9 »"))
    }

    @Test
    fun aScreenThatSticksAgainHasNoDiffNow() {
        // Tour 8, banc9 (point f): lastDiff was never reset: a hand back at a later, sticking screen quoted an old diff.
        val d = ReplayDiffs()
        d.turn("origine « a », rejeu « b »")
        d.turn(null)
        assertNull(d.now)
        assertEquals("origine « a », rejeu « b »", d.last)
    }
}
