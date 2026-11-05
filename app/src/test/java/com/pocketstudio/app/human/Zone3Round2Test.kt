package com.pocketstudio.app.human

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextLayoutResult
import com.pocketstudio.app.ui.GUIDE_PANEL_READING_DP
import com.pocketstudio.app.ui.THUMB_BAR_HEIGHT_DP
import com.pocketstudio.app.ui.guidePanelHeightDp
import com.pocketstudio.model.instrumentTracks
import com.pocketstudio.model.track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowToast

/**
 * Audit ultra round 2, zone 3 (navigation, getting started, saving, one hand), as the seats did it in the whole app:
 * - n° 11 (B a): the Guide's open panel hid the thumb's bar (« Jouer » out of reach); its empty « Le Guide lit… » took half the screen;
 * - n° 12 (B a, A c): ten steps, about sixteen screens before the first sound: « Créer et écouter » on the style;
 * - n° 13 (B a): a step opened scrolled as far as the step before (no title, no « Créer tout de suite »);
 * - n° 14 (B a): « Mon premier rythme », never made by the person, in « Mes projets » next to their first song;
 * - n° 15 (B d): « Mon pr… » and a lone tick, no « Sauvegardé » in words, nothing said leaving the app (Retour on home
 *   leaves it: the bench cannot show a task moved back, [backOnHomeLeavesTheAppAndSaysItIsSaved] proves it);
 * - n° 16 (A c): the welcome's green hat (« J'apprends ») gone from the home screen;
 * - n° 17 (B d, B c): a track's new sound and a deleted track said nothing; the track kept « Synthé » / « Piste 2 ».
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class Zone3Round2Test : HumanTester("zone3r2") {
    @Before
    fun notOnTheBench() = assumeTrue("not while the bench runs seats", System.getProperty("ps.human.dir").isNullOrBlank())

    private fun screen() = screenLines().joinToString("\n")

    private fun tap(tag: String): String {
        val at = centerOf(tag)
        assertNotNull("nothing to touch: $tag\n" + screen(), at)
        return act("touche ${at!!.first} ${at.second}")
    }

    private fun tapText(text: String) {
        val at = centerOfText(text, substring = true)
        assertNotNull("nothing to touch: « $text »\n" + screen(), at)
        act("touche ${at!!.first} ${at.second}")
    }

    private fun waitFor(what: String, done: () -> Boolean) {
        val until = System.currentTimeMillis() + 60_000
        while (!done()) {
            assertTrue("$what never came\n" + screen(), System.currentTimeMillis() < until)
            act("attends 0.5")
        }
    }

    private fun shows(text: String) = screenLines().any { text in it }

    /** The welcome › « Créer mon premier morceau » › Simple › Techno, the card « Techno » open. */
    private fun techno(o: SeatOptions = SeatOptions(simple = true)) {
        begin(o)
        tapText("Créer mon premier morceau")
        if (centerOf("mode_card_simple") != null) tap("mode_card_simple")
        tap("wizard_family_Techno")
        tap("wizard_style_techno")
    }

    private fun simpleSong(o: SeatOptions = SeatOptions(simple = true)) {
        techno(o)
        tap("wizard_style_choose_techno")
        tap("wizard_create_now")
        waitFor("the song's tabs") { screenRoute().endsWith("tab_simple_Morceau") }
    }

    /** The welcome's « Partir de zéro », in the Mode simple, on « Mon morceau » (« Mon projet 1 »: Batterie and Synthé). */
    private fun emptySimpleProject(o: SeatOptions = SeatOptions(simple = true)) {
        begin(o)
        compose.onNodeWithTag("welcome_empty").performScrollTo()
        tap("welcome_empty")
        if (centerOf("mode_card_simple") != null) tap("mode_card_simple")
        waitFor("the song's tabs") { centerOf("tab_simple_Morceau") != null }
        tap("tab_simple_Morceau")
        centerOf("guide_later")?.let { act("touche ${it.first} ${it.second}") }
    }

    /** n° 11 (seat B a, a 360 x 780 phone held in one hand): the Guide open, ▶ still at the thumb. */
    @Test
    fun theOpenGuideNeverHidesTheThumbsBar() {
        simpleSong(SeatOptions(simple = true, device = "phone360"))
        if (centerOf("guide_panel") == null) tap("guide_bar")
        assertNotNull("the Guide's panel open\n" + screen(), centerOf("guide_panel"))
        val play = centerOf("thumb_play")
        assertNotNull("« Jouer » at the thumb under the open Guide\n" + screen(), play)
        // Above the tabs, in the thumb's half of the screen.
        assertTrue("the thumb's bar under the panel", play!!.second > centerOf("guide_panel")!!.second)
    }

    /** n° 11: the panel the bar's height lower when the bar is there; while it reads the song, a bar and a line only. */
    @Test
    fun theGuidesPanelMakesRoomForTheBarAndIsSmallWhileReading() {
        assertEquals(312, guidePanelHeightDp(780, 360, thumbBar = false, loading = false))
        assertEquals(312 - THUMB_BAR_HEIGHT_DP, guidePanelHeightDp(780, 360, thumbBar = true, loading = false))
        assertEquals(GUIDE_PANEL_READING_DP, guidePanelHeightDp(780, 360, thumbBar = true, loading = true))
        assertTrue("an empty panel never takes the thumb's half", GUIDE_PANEL_READING_DP < 780 / 6)
    }

    /** n° 12 (seats B a, A c): the style touched, one more touch and the song plays. */
    @Test
    fun aBeatPlaysOneTouchAfterTheStyle() {
        techno()
        tap("wizard_style_play_techno")
        waitFor("the song's tabs") { screenRoute().endsWith("tab_simple_Morceau") }
        waitFor("the song playing") { rhythm()?.transport?.value?.playing == true }
        assertTrue("a song with its parts", project()!!.song.isNotEmpty())
        // The steps are still there for whoever wants them.
        assertNotNull(project())
    }

    /** n° 12 / n° 13 (seat B a): « Choisir ce style » right under the description, never under the references. */
    @Test
    fun theStylesChoicesComeRightUnderItsDescription() {
        techno(SeatOptions(simple = true, device = "phone360"))
        val play = centerOf("wizard_style_play_techno")
        val choose = centerOf("wizard_style_choose_techno")
        assertNotNull(play)
        assertNotNull(choose)
        assertTrue("« Créer et écouter » first", play!!.second < choose!!.second)
        // The references' own line (the card's merged text holds it too).
        val reference = compose.onAllNodes(hasText("Pour l'idée :"), useUnmergedTree = true).fetchSemanticsNodes().firstOrNull()?.boundsInRoot
        val chosen = compose.onAllNodes(hasTestTag("wizard_style_choose_techno"), useUnmergedTree = true).fetchSemanticsNodes().first().boundsInRoot
        assertNotNull("the references shown\n" + screen(), reference)
        assertTrue("the choices above the references\n" + screen(), chosen.bottom <= reference!!.top)
    }

    /** n° 13 (seat B a: « Étape 3 sur 10 » with no title, the step scrolled as far as the step before). */
    @Test
    fun eachStepOpensAtItsTop() {
        techno()
        tap("wizard_style_choose_techno")
        act("glisse 300 1000 300 200")
        act("glisse 300 1000 300 200")
        tap("wizard_next")
        val step = centerOf("wizard_step")!!
        val now = centerOf("wizard_create_now")
        assertNotNull(now)
        assertTrue("« Créer tout de suite » in sight under the step's title\n" + screen(), now!!.second > step.second)
    }

    /** n° 14 (seat B a): the first song made at the first opening; « Mon premier rythme » is not in « Mes projets ». */
    @Test
    fun noProjectNobodyMadeInMyProjects() {
        techno()
        tap("wizard_style_play_techno")
        waitFor("the song's tabs") { screenRoute().endsWith("tab_simple_Morceau") }
        tap("top_projects")
        waitFor("the list") { shows("Techno") }
        act("attends 1")
        assertFalse("« Mon premier rythme », never made, in the list\n" + screen(), shows("Mon premier rythme"))
    }

    /** n° 15 (seat B d, the owner's phone): « Mon projet 1 » whole and « Sauvegardé » in words in the top bar. */
    @Test
    fun theTopBarSaysSavedInWords() {
        emptySimpleProject()
        waitFor("the project saved") { rhythm()?.session?.unsaved?.value == false }
        act("attends 1")
        fun cut(text: String): Boolean {
            val node = compose.onAllNodes(hasText(text) and hasAnyAncestor(hasTestTag("top_projects")), useUnmergedTree = true).fetchSemanticsNodes().firstOrNull()
            assertNotNull("« $text » in the top bar\n" + screen(), node)
            val layouts = mutableListOf<TextLayoutResult>()
            node!!.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(layouts)
            return layouts.first().hasVisualOverflow
        }
        assertFalse("the project's name whole", cut("Mon projet 1"))
        assertFalse("« Sauvegardé » whole, in words", cut("Sauvegardé"))
        // ↶ stays at the thumb.
        assertNotNull(centerOf("thumb_undo"))
    }

    /** n° 15 (seat B d: « Retour » on home « reste sur l'accueil »): it leaves the app, saying the song is saved. */
    @Test
    fun backOnHomeLeavesTheAppAndSaysItIsSaved() {
        emptySimpleProject()
        act("retour")
        waitFor("the home screen") { screenRoute() == "launch" }
        waitFor("the project saved") { rhythm()?.session?.unsaved?.value == false }
        assertTrue("« Sauvegardé » on « Continuer »\n" + screen(), shows("Sauvegardé à l'instant"))
        act("retour")
        var activity: android.app.Activity? = null
        androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
            .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).firstOrNull()?.let { activity = it }
        assertNotNull(activity)
        assertTrue("the app goes to the back (the phone's home)", shadowOf(activity).isTaskMovedToBack)
        assertEquals("Ton morceau est sauvegardé.", ShadowToast.getTextOfLatestToast())
    }

    /** n° 16 (seat A c): after Retour, the home screen still has the welcome's green hat (« J'apprends »). */
    @Test
    fun theHomeKeepsJapprends() {
        emptySimpleProject()
        act("retour")
        waitFor("the home screen") { screenRoute() == "launch" }
        compose.onNodeWithTag("launch_learn").performScrollTo()
        tap("launch_learn")
        if (centerOf("mode_card_simple") != null) tap("mode_card_simple")
        waitFor("the tutorial's chooser") { shows("J'apprends") && screenRoute() != "launch" }
    }

    /** n° 17 (seat B d): a new sound on « Synthé » names the track and says so; deleting it says so, « Annuler » brings it back. */
    @Test
    fun aTracksNewSoundAndItsDeletionAreSaid() {
        emptySimpleProject()
        val synth = project()!!.instrumentTracks.first()
        tap("simple_track_inst:${synth.id}")
        tapText("Basses")
        tapText("Basse sub")
        waitFor("the sheet closed") { centerOf("sound_chooser") == null }
        assertEquals("the track named after its sound", "Basse sub", project()!!.track(synth.id).name)
        assertNotNull("said\n" + screen(), centerOf("done_bar"))
        assertTrue(shows("Nouveau son : Basse sub"))
        tap("simple_volume_inst:${synth.id}")
        tap("simple_track_delete")
        assertTrue("said\n" + screen(), shows("Piste « Basse sub » supprimée."))
        tap("done_undo")
        assertEquals("back with « Annuler »", "Basse sub", project()!!.track(synth.id).name)
        assertTrue(project()!!.instrumentTracks.any { it.id == synth.id })
        assertTrue("the line gone with its undo", centerOf("done_bar") == null)
    }

    /** n° 17 (seat B c, Pro): a kept sound on « Piste 2 » names it; a name the user typed stays. */
    @Test
    fun aKeptSoundNamesATrackTheAppNamed() {
        begin(SeatOptions(simple = false))
        val vm = rhythm()!!
        vm.saveSynthFavorite("Sub House Ben")
        act("attends 0.5")
        vm.addTrack()
        act("attends 0.5")
        val added = vm.selectedTrack.value
        vm.applySynthFavorite(vm.favorites.value.synth.indexOfFirst { it.name == "Sub House Ben" })
        act("attends 0.5")
        assertEquals("Sub House Ben", project()!!.track(added).name)
        vm.renameTrack(added, "Ma basse")
        vm.applySynthPreset(com.pocketstudio.model.SoundLibrary.SYNTH.first().sound, name = com.pocketstudio.model.SoundLibrary.SYNTH.first().name)
        assertEquals("a name the user typed stays", "Ma basse", project()!!.track(added).name)
    }
}
