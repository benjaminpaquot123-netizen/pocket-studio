package com.pocketstudio.app.human

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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
 * Audit ultra round 1, lot A, seats a and d: ▶ played the song (the bench's engine with it), then a tab, and every
 * touch after left the screen as it was (tabs, « Retour », ■, « Mes projets »): the bench let its engine go under the
 * position's poll ([HumanTester.letTheEngineGo]). With the song played by ▶, by the guide's « Écouter » and with
 * « Enregistrer ce que je joue » touched while it plays, each tab touched is the one shown, « Retour » and ■ too,
 * and the screen's work never fails.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class BenchPlayThenTabsTest : HumanTester("play-tabs") {
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

    /** The screen's work never failed (a failure stops its updates: the frozen screen). */
    private fun alive(what: String) = assertTrue("$what: the screen's work failed\n${problems.joinToString("\n")}", problems.isEmpty())

    private fun goesTo(tag: String, what: String) {
        val before = screenRoute()
        tap(tag)
        val now = screenRoute()
        alive(what)
        assertTrue("$what: « $tag » touched, the screen shows it ($before → $now)", now.endsWith(tag))
    }

    private fun playing() = rhythm()!!.transport.value.playing

    /** The assistant's song in Mode simple, as seat a made it (Techno), on its « Morceau » tab. */
    private fun simpleSong(family: String, style: String) {
        begin()
        tapText("Créer mon premier morceau")
        tap("mode_card_simple")
        tap("wizard_family_$family")
        tap("wizard_style_$style")
        tap("wizard_style_choose_$style")
        tap("wizard_create_now")
        waitFor("the song's tabs") { screenRoute().endsWith("tab_simple_Morceau") }
    }

    /** Then « Retour », ■ and one more tab: each changes the screen. */
    private fun backStopAndATab(next: String) {
        val before = screenRoute()
        act("retour")
        alive("« Retour »")
        assertNotEquals("« Retour » changes the screen", before, screenRoute())
        // Audit ultra round 1 (n° 25): a tab touched folds the Guide's panel, so « Retour » now leaves the project for
        // « Que veux-tu faire ? » (which stops the music); « Continuer » brings the song back, its ▶ and tabs working.
        if (centerOf("launch_continue") != null) {
            assertFalse("the home screen plays nothing", playing())
            tap("launch_continue")
            alive("« Continuer »")
            waitFor("the song again") { centerOf("top_play") != null }
        }
        if (!playing()) tap("top_play")
        assertTrue(playing())
        tap("top_play")
        alive("■")
        assertFalse("■ stops", playing())
        goesTo(next, "after ■")
    }

    @Test
    fun theSongOfPlayThenTabsBackAndStop() {
        simpleSong("Techno", "techno")
        val heard = tap("top_play")
        assertTrue(heard, "le morceau joue (partie" in heard)
        act("attends 1")
        goesTo("tab_simple_Rythme", "▶ then a tab (seat a, #21)")
        goesTo("tab_simple_Clavier", "a second tab")
        assertTrue("still playing", playing())
        backStopAndATab("tab_simple_Rythme")
    }

    @Test
    fun theGuidesListenThenATab() {
        simpleSong("House", "house")
        val until = System.currentTimeMillis() + 60_000
        while (centerOf("guide_option_drop") == null) {
            assertTrue("no guide « Écouter »\n" + screenLines().joinToString("\n"), System.currentTimeMillis() < until)
            centerOfText("Compris")?.let { act("touche ${it.first} ${it.second}") } ?: act("attends 0.5")
        }
        val drop = tap("guide_option_drop").let { if ("le morceau joue" in it) it else act("attends 1") }
        assertTrue(drop, "le morceau joue (partie" in drop)
        act("attends 1")
        goesTo("tab_simple_Clavier", "the guide's « Écouter » then a tab")
        backStopAndATab("tab_simple_Morceau")
    }

    /** Seat d: Pro, ▶, then « Enregistrer ce que je joue » (the top bar's ●) while it plays, then « Plus ». */
    @Test
    fun recordWhileTheSongPlaysThenTabs() {
        begin(SeatOptions(simple = false))
        tap("launch_new")
        tap("starter_wizard")
        if (centerOf("mode_card_pro") != null) tap("mode_card_pro")
        tap("wizard_family_Techno")
        tap("wizard_style_techno")
        tap("wizard_style_choose_techno")
        tap("wizard_create_now")
        waitFor("the song's spaces") { screenRoute().endsWith("tab_Structure") }
        centerOf("guide_later")?.let { act("touche ${it.first} ${it.second}") }
        // The « Mode retours » bubble out of the way: on the Sons tab it sits on the top bar's ■ (not this test's matter).
        if (centerOf("retours_more") != null) {
            tap("retours_more")
            tap("retours_menu_hide")
        }
        val heard = tap("top_play")
        assertTrue(heard, "le morceau joue (partie" in heard)
        tap("top_record")
        // Audit ultra round 1 (n° 30): the ● asks what to record; the pads, as seat d's take.
        tap("record_choice_pads")
        alive("« Enregistrer ce que je joue » (seat d, #17)")
        goesTo("tab_Plus", "then « Plus » (seat d, #18)")
        goesTo("tab_Sons", "then « Sons »")
        assertTrue("still playing", playing())
        val stop = tap("top_play")
        alive("■")
        assertFalse("■ stops: $stop\n" + screenLines().joinToString("\n"), playing())
        goesTo("tab_Structure", "after ■")
        // « Retour » (seat d, #23): the project left for « Que veux-tu faire ? ».
        val before = screenRoute()
        act("retour")
        alive("« Retour »")
        assertNotEquals("« Retour » changes the screen", before, screenRoute())
    }
}
