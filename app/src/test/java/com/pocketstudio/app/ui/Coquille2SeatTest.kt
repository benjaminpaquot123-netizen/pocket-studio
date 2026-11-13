package com.pocketstudio.app.ui

import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.model.remixOverrunSeconds
import com.pocketstudio.app.human.HumanTester
import com.pocketstudio.app.human.SeatOptions
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Lot « coquille2 » (tour 6): the acceptance phrases left in the shared files, each proven by the bench in the conditions
 * of its seat, the whole app. Seat A c (exports): fr, phone, font 100, Mode simple.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class Coquille2SeatTest : HumanTester("coquille2") {
    private fun tap(tag: String): String {
        val at = reachTag(tag) ?: error("no $tag on:\n" + screenLines().joinToString("\n"))
        return act("touche ${at.first} ${at.second}")
    }

    private fun screen() = screenLines().joinToString("\n")

    /** The first node tagged [tag] is drawn whole (not cut by a parent that scrolls, nor by an edge). */
    private fun whole(tag: String): Boolean =
        compose.onAllNodesWithTag(tag).fetchSemanticsNodes().firstOrNull()?.let { shownWhole(it) } == true

    /** Seat A c's start: « Fais-moi un morceau », Simple, Hip-hop › Trap, « Créer et écouter ». */
    private fun trapSong() {
        begin(SeatOptions.parse(mapOf("lang" to "fr", "device" to "phone", "font" to "100", "mode" to "simple")).first)
        tap("welcome_guided"); tap("mode_card_simple"); tap("wizard_family_Hip-hop"); tap("wizard_style_trap"); tap("wizard_style_play_trap")
        repeat(5) { act("attends 1") }
    }

    /** Seat A c, geste 18 (E2): a file made, the song not touched: never « Le morceau a changé depuis cet export ». */
    @Test
    fun e2_noSongChangeSaidWhenOnlyTheFilesVolumeIsChosen() {
        assumeTrue(System.getProperty("ps.human.dir").isNullOrBlank())
        trapSong()
        tap("tab_simple_Envoyer")
        tap("share_go")
        repeat(40) { if (rhythm()?.export?.value?.running == true) act("attends 1") }
        act("touche 294 543")  // the phone's share sheet: WhatsApp
        tap("share_bilan_ok")
        // « Autres fichiers » (the Pro export, as the seat went): its row is half under the format buttons (E3, lot exports2).
        compose.onNodeWithTag("share_other").performClick()
        repeat(2) { act("attends 1") }
        assertNull("« a changé » right after the file:\n" + screen(), centerOf("export_stale"))
        // The volume of the next file chosen (the seat's « Streaming »): the song itself is the same.
        tap("export_target_STREAMING")
        act("attends 1")
        assertNull("« Le morceau a changé » though only the file's volume was chosen:\n" + screen(), centerOf("export_stale"))
    }

    /** Seat A b (fr, phone, 100 %, Mode simple), P5: the Live project opened again from « Mes projets » opens on Pad Live. */
    @Test
    fun p5_theLiveProjectOpensAgainOnPadLive() {
        assumeTrue(System.getProperty("ps.human.dir").isNullOrBlank())
        begin(SeatOptions.parse(mapOf("lang" to "fr", "device" to "phone", "font" to "100", "mode" to "simple")).first)
        amenee("premier-morceau", java.io.File("build/coquille2-journal.txt"))
        val song = rhythm()!!.projectId
        act("retour")
        tap("launch_padlive")
        repeat(5) { act("attends 1") }
        assertNotNull("Pad Live not open:\n" + screen(), centerOf("padlive"))
        val live = rhythm()!!.projectId
        tap("padlive_back")
        val projects = centerOfText("Mes projets", substring = true) ?: error("no « Mes projets »:\n" + screen())
        act("touche ${projects.first} ${projects.second}")
        tap("project_$song")
        repeat(4) { act("attends 1") }
        tap("top_more"); tap("top_projects")
        tap("project_$live")
        repeat(4) { act("attends 1") }
        assertTrue("the Live project is not the one open", rhythm()!!.projectId == live)
        assertNotNull("the Live project opened again elsewhere than Pad Live:\n" + screen(), centerOf("padlive"))
    }

    /**
     * Seat B a (es, phone, 100 %, Mode simple, left hand), A6: a song (0:40) longer than the project (1 bar) put in it: the app
     * says its end will be cut AND offers to lengthen, in the same window.
     */
    @Test
    fun a6_aSongLongerThanTheProjectIsSaidWithTheOfferToLengthen() {
        assumeTrue(System.getProperty("ps.human.dir").isNullOrBlank())
        begin(SeatOptions.parse(mapOf("lang" to "es", "device" to "phone", "font" to "100", "mode" to "simple", "main" to "gauche")).first)
        amenee("remix", java.io.File("build/coquille2-journal.txt"))
        tap("remix_download_thumb")
        runCatching { tap("remix_model_ask_ok") }
        repeat(80) { if (com.pocketstudio.app.remix.SeparationJob.state.value is com.pocketstudio.app.remix.SeparationState.Done) return@repeat; act("attends 1") }
        runCatching { tap("remix_pick_thumb") }
        repeat(80) { if (com.pocketstudio.app.remix.SeparationJob.state.value is com.pocketstudio.app.remix.SeparationState.Done) return@repeat; act("attends 1") }
        val id = com.pocketstudio.app.remix.StemStore.list(ApplicationProvider.getApplicationContext()).first().id
        // As the seat: « Cortar lo que quiero » puts the song in the project (kept as a store of sounds, the project a 1-bar
        // loop), then the cut screen closed, the song played with the project.
        tap("cut_song_$id")
        repeat(20) { act("attends 1") }
        repeat(10) { if (centerOf("remix_project") != null) return@repeat; runCatching { tap("cut_close") }; act("attends 1") }
        // The voice switched on (the seat's speaker, screen 41): the song is heard with a 1-bar project.
        tap("stem_mute_3")
        repeat(2) { act("attends 1") }
        // Said (« su final se cortará ») with the offer to lengthen (« Alargar … »), in that window, before anything else.
        assertNotNull("no word that the end will be cut:\n" + screen(), centerOfText("se cortará", substring = true))
        assertNotNull("no offer to lengthen with the warning:\n" + screen(), centerOf("notice_lengthen"))
        tap("notice_lengthen")
        act("attends 1")
        assertTrue("the song still goes past the project's end", rhythm()!!.project.value.remixOverrunSeconds() <= 0)
    }

    /**
     * Seat B a (es, phone, 100 %, Mode simple, left hand), A4: « Cortar la canción » in « Frases »: the mode menus
     * (« ✂ Frases ▾ », « En el tiempo ▾ ») stay whole on the screen, not cut under the panel of actions.
     */
    @Test
    fun a4_thePhrasesModeMenusStayWholeInView() {
        assumeTrue(System.getProperty("ps.human.dir").isNullOrBlank())
        begin(SeatOptions.parse(mapOf("lang" to "es", "device" to "phone", "font" to "100", "mode" to "simple", "main" to "gauche")).first)
        amenee("remix", java.io.File("build/coquille2-journal.txt"))
        tap("remix_download_thumb")
        runCatching { tap("remix_model_ask_ok") }
        repeat(80) { if (com.pocketstudio.app.remix.SeparationJob.state.value is com.pocketstudio.app.remix.SeparationState.Done) return@repeat; act("attends 1") }
        runCatching { tap("remix_pick_thumb") }
        repeat(80) { if (com.pocketstudio.app.remix.SeparationJob.state.value is com.pocketstudio.app.remix.SeparationState.Done) return@repeat; act("attends 1") }
        val id = com.pocketstudio.app.remix.StemStore.list(ApplicationProvider.getApplicationContext()).first().id
        tap("cut_song_$id")
        repeat(20) { act("attends 1") }
        // The seat's gesture 19: « ✂ Un trozo ▾ », then « Frases ».
        tap("cut_auto"); tap("cut_auto_1")
        repeat(3) { act("attends 1") }
        assertTrue("« Frases ▾ » cut on:\n" + screen(), whole("cut_auto"))
        assertTrue("« En el tiempo ▾ » cut on:\n" + screen(), whole("cut_magnet"))
    }
}
