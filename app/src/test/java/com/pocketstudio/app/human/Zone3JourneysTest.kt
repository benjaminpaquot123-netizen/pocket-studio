package com.pocketstudio.app.human

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Audit ultra round 1, zone 3 (navigation, getting started, safety), as the seats did it in the whole app:
 * - n° 22 (A c, C e, F d): « Mes projets » said « Aucun projet » while « Mon premier rythme » was open and « Sauvegardé »;
 * - n° 24 (B b): no way to make a second part (B) in the Mode simple;
 * - n° 26 (F d): Retour on the « Noter » window closed the studio behind it; ↷ Rétablir nowhere after ↶ in the Mode simple.
 * n° 27 (D b, C a: the song turned into a loop of its Intro on opening a track's sounds, or « Envoyer ») is the bench's:
 * on a gesture that needs no engine it stops the song, lets its engine go and plays again with ▶ alone, which plays the
 * part shown in a loop ([HumanTester] letTheEngineGo, seen in a trace of this class); a phone's engine never goes.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class Zone3JourneysTest : HumanTester("zone3") {
    @Before
    fun notOnTheBench() = assumeTrue("not while the bench runs seats", System.getProperty("ps.human.dir").isNullOrBlank())

    private fun tap(tag: String): String {
        val at = centerOf(tag)
        assertNotNull("nothing to touch: $tag\n" + screenLines().joinToString("\n"), at)
        return act("touche ${at!!.first} ${at.second}")
    }

    private fun tapText(text: String) {
        val at = centerOfText(text, substring = true)
        assertNotNull("nothing to touch: « $text »\n" + screenLines().joinToString("\n"), at)
        act("touche ${at!!.first} ${at.second}")
    }

    private fun waitFor(what: String, done: () -> Boolean) {
        val until = System.currentTimeMillis() + 60_000
        while (!done()) {
            assertTrue("$what never came\n" + screenLines().joinToString("\n"), System.currentTimeMillis() < until)
            act("attends 0.5")
        }
    }

    private fun shows(text: String) = screenLines().any { text in it }

    private fun simpleSong() {
        begin(SeatOptions(simple = true))
        tapText("Créer mon premier morceau")
        if (centerOf("mode_card_simple") != null) tap("mode_card_simple")
        tap("wizard_family_Techno")
        tap("wizard_style_techno")
        tap("wizard_style_choose_techno")
        tap("wizard_create_now")
        waitFor("the song's tabs") { screenRoute().endsWith("tab_simple_Morceau") }
        centerOf("guide_later")?.let { act("touche ${it.first} ${it.second}") }
    }

    /** n° 22 (seat F d): the app just installed, « Mes projets » from home lists the project « Continuer » offers. */
    @Test
    fun myProjectsListsTheProjectOfContinue() {
        begin(SeatOptions(simple = false))
        assertTrue("home offers « Continuer »\n" + screenLines().joinToString("\n"), centerOf("launch_continue") != null)
        tap("launch_projects")
        act("attends 1")
        assertFalse("« Aucun projet » while one is open\n" + screenLines().joinToString("\n"), shows("Aucun projet"))
        assertTrue("the open project in the list\n" + screenLines().joinToString("\n"), shows("Mon premier rythme"))
    }

    /** The welcome's « Partir de zéro », in the Mode simple, on « Mon morceau ». */
    private fun emptySimpleProject() {
        begin(SeatOptions(simple = true))
        compose.onNodeWithTag("welcome_empty").performScrollTo()
        tap("welcome_empty")
        if (centerOf("mode_card_simple") != null) tap("mode_card_simple")
        waitFor("the song's tabs") { centerOf("tab_simple_Morceau") != null }
        tap("tab_simple_Morceau")
        centerOf("guide_later")?.let { act("touche ${it.first} ${it.second}") }
    }

    /** n° 24 (seat B b: « + A », then « seule la partie A est proposée »): « Mon morceau » of the Mode simple makes a part B. */
    @Test
    fun theModeSimpleMakesASecondPart() {
        emptySimpleProject()
        // The rows lower in « Mon morceau » brought into view as a finger would (the bench touches what it sees).
        compose.onNodeWithTag("add_part_0").performScrollTo()
        tap("add_part_0")
        val before = project()!!
        assertEquals(listOf(0), before.song.map { it.scene })
        compose.onNodeWithTag("add_part_new_copy").performScrollTo()
        tap("add_part_new_copy")
        val after = project()!!
        assertEquals("a part more in the song (${rhythm()!!.notice.value})", 2, after.song.size)
        val added = after.song.last().scene
        assertTrue("a part of its own (B), not A again: ${after.song}", added != 0)
        assertEquals("the screens show the new part", added, after.scene)
        rhythm()!!.undo()
        assertEquals("one undo step", before.song, project()!!.song)
        // The editors' « Partie : A ▾ » makes one too (the seat looked there).
        tap("tab_simple_Rythme")
        tap("part_header_pick")
        tap("part_new")
        assertEquals(2, project()!!.song.size)
    }

    /** n° 26 (seat F d): Retour on « Noter » closes the note only, the studio stays. */
    @Test
    fun backOnTheNoteClosesTheNoteOnly() {
        simpleSong()
        val route = screenRoute()
        tap("retours_note")
        assertTrue(centerOf("retours_note_sheet") != null)
        act("retour")
        assertTrue("the note closed", centerOf("retours_note_sheet") == null)
        // The tab (the page's biggest card changes with the Guide's panel once it has read the song: round 2, n° 11).
        assertEquals("the studio stays", route.substringAfterLast('/'), screenRoute().substringAfterLast('/'))
    }

    /** n° 26 (seat F d): ↶ in the Mode simple, then ↷ Rétablir brings it back (top bar and the thumb's bar). */
    @Test
    fun redoAfterUndoInTheModeSimple() {
        simpleSong()
        val vm = rhythm()!!
        val before = project()!!.tempoBpm
        vm.setTempo(before + 7)
        act("attends 0.5")
        // Audit ultra round 2 (n° 15): the Mode simple's ↶ is at the thumb when its bar is there.
        tap(if (centerOf("top_undo") != null) "top_undo" else "thumb_undo")
        assertEquals(before, project()!!.tempoBpm, 0.01)
        assertNotNull("« Rétablir » offered\n" + screenLines().joinToString("\n"), centerOf("top_redo") ?: centerOf("thumb_redo"))
        tap(if (centerOf("thumb_redo") != null) "thumb_redo" else "top_redo")
        assertEquals(before + 7, project()!!.tempoBpm, 0.01)
    }
}
