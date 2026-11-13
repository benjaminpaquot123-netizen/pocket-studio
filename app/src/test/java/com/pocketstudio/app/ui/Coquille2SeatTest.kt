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
     * Seat B a (es, phone, 100 %, Mode simple, left hand), A6: a song (0:40) longer than the project (a 1-bar loop) put in
     * it: the app says so truly (a loop lets the song ring on: no « su final se cortará ») AND offers to lengthen, in the
     * same window; « Alargar el tema » gives the project a song that holds the whole song.
     */
    @Test
    fun a6_aSongLongerThanTheLoopIsSaidTrulyWithTheOfferToLengthen() {
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
        // Said truly (« sigue sonando mientras el bucle se repite », nothing cut in a loop), with the offer to lengthen.
        assertNull("« se cortará » said of a loop, which lets the song ring on:\n" + screen(), centerOfText("se cortará", substring = true))
        assertNotNull("no word that the song is longer than the loop:\n" + screen(), centerOfText("sigue sonando", substring = true))
        assertNotNull("no offer to lengthen with the warning:\n" + screen(), centerOf("notice_lengthen"))
        tap("notice_lengthen")
        act("attends 1")
        val p = rhythm()!!.project.value
        // Fresh review: with no song the overrun reads 0 whatever happens; the song made must be there and hold it all.
        assertTrue("« Alargar el tema » made no song", p.song.isNotEmpty())
        assertTrue("the song still goes past the project's end", p.remixOverrunSeconds() <= 0)
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

    /**
     * Seat A c (fr, phone, 100 %, Mode simple), N2 (gesture 21, screen 23): during « Fabrication du son » of the Pro
     * export, its « Annuler » is whole on the screen, not cut under the « Guide » bar.
     */
    @Test
    fun n2_theExportsCancelIsWholeWhileTheSoundIsMade() {
        assumeTrue(System.getProperty("ps.human.dir").isNullOrBlank())
        trapSong()
        tap("tab_simple_Envoyer")
        compose.onNodeWithTag("share_other").performClick()
        repeat(2) { act("attends 1") }
        tap("export")
        act("attends 1")
        assertTrue("the export is not running", rhythm()?.export?.value?.running == true)
        // The export's own « Annuler » (fresh review: not the first « Annuler » of the screen).
        val cancel = compose.onAllNodesWithTag("export_cancel").fetchSemanticsNodes().firstOrNull()
            ?: error("no « Annuler » while the sound is made:\n" + screen())
        assertTrue("« Annuler » cut while the sound is made:\n" + screen(), shownWhole(cancel))
    }

    /**
     * Seat D a (ja, phone, 100 %, Mode simple, the techno tutorial « Facile »), chapter 12 « トランジション », step « crash »
     * (screen 177): the line over the grid said « → スタジオのバスドラム » on the cymbal's grid. Its « → » names the pad shown.
     */
    @Test
    fun cymbalGridLineNamesThePadShown() {
        assumeTrue(System.getProperty("ps.human.dir").isNullOrBlank())
        begin(SeatOptions.parse(mapOf("lang" to "ja", "device" to "phone", "font" to "100", "mode" to "simple")).first)
        tap("welcome_learn"); runCatching { tap("mode_card_simple") }; tap("tuto_style_techno")
        repeat(3) { act("attends 1") }
        // Chapter 12, its step « gap », from the chapters' list (as the seat's « 次の章 »).
        tap("tuto_chapters"); tap("tuto_chapter_11")
        act("attends 1")
        val v = rhythm()!!
        fun saysThePadShown(at: String) {
            val shown = com.pocketstudio.app.rhythm.padName(v.project.value, v.transport.value.selectedPad)
            val said = screenLines().mapNotNull { Regex("« → ([^»]+) »").find(it)?.groupValues?.get(1)?.trim() }
            // Fresh review: a line with no « → » at all would pass unseen; the moment checked is the banner's.
            assertTrue("$at: no « → » on the screen:\n" + screen(), said.isNotEmpty())
            assertTrue("$at: « → ${said.joinToString()} » over the grid of « $shown »:\n" + screen(), said.all { it == shown })
        }
        // The seat's screens 175-177: a hit left on the kick's last beat while another pad is shown, the app turns the grid
        // to the kick and says « → スタジオのバスドラム »; within that moment the next step (« crash ») puts the cymbal on the
        // grid, as the app does at each step (`tutoUi.pad`). The line must name the cymbal, not the kick.
        v.selectPad(1)
        v.toggleHit(0, v.project.value.drums.steps - 1)
        compose.mainClock.advanceTimeBy(com.pocketstudio.app.tuto.TUTO_TURN_MS + 100)
        compose.waitForIdle()
        assertTrue("the grid not turned to the kick", v.transport.value.selectedPad == 0)
        saysThePadShown("turned to the kick")
        v.selectPad(com.pocketstudio.model.tuto.TutoChecks.CRASH_LANE)
        compose.waitForIdle()
        saysThePadShown("the cymbal on the grid")
    }

    /**
     * Seat A b (fr, phone, 100 %, Mode simple), P6 (padlive2, path « Enregistrer ce que je joue » › « Dans une brique du
     * Live »): after a take of several bars put in a brick, the screen and the transport say the length one hears, not
     * « A mesure 1/1 ».
     */
    @Test
    fun p6_aTakeInABrickIsCountedAtTheLengthHeard() {
        assumeTrue(System.getProperty("ps.human.dir").isNullOrBlank())
        begin(SeatOptions.parse(mapOf("lang" to "fr", "device" to "phone", "font" to "100", "mode" to "simple")).first)
        amenee("padlive", java.io.File("build/coquille2-journal.txt"))
        // The take: the bench has no microphone (the recorder answers « Pas assez de mémoire »), so the take the seat's
        // « Enregistrer ce que je joue » made (4 bars and more) is put where « Dans une brique du Live » puts it
        // (`PlayedTake.placeInBrick` → `placeInCell(SongPart.audio(-1), scene, …)`), 4 bars at the project's tempo (the seat's
        // « 4 mesures »).
        val v0 = rhythm()!!
        val taken = 4
        val frames = (taken * v0.project.value.beatsPerBar * 60.0 / v0.project.value.tempoBpm * 48_000).toInt()
        val pcm = com.pocketstudio.app.audio.Pcm(FloatArray(frames * 2) { i -> if ((i / 2) % 12_000 < 600) 0.3f else 0f }, 2, 48_000)
        assertTrue("the take not put in a brick", v0.placeInCell(com.pocketstudio.model.SongPart.audio(-1), v0.project.value.scene, pcm, "Ma prise 1", known = v0.project.value.tempoBpm))
        repeat(4) { act("attends 1") }
        // What one hears: the brick loops on its own length (whole bars, `LiveCells.loopBars`).
        val bars = rhythm()!!.project.value.let { p -> p.audioTracks.mapNotNull { it.clip }.first { !it.song && it.scene == p.scene }.loopSteps / p.stepsPerBar }
        assertTrue("a take of one bar only: $bars", bars > 1)
        val v = rhythm()!!
        // As the message says (« Plus › Briques »): the Live's bricks, played; the counter under the top bar says « mes. x/<bars> ».
        runCatching { tap("padlive_more") }
        tap("padlive_live")
        repeat(2) { act("attends 1") }
        // Back to the project (the seat's « lecture « A mesure 1/1 » »), its part played in a loop: « ▶ cette partie ».
        repeat(3) { if (centerOf("top_play") == null) act("retour") }
        if (!v.transport.value.playing) tap("top_play")
        repeat(3) { act("attends 1") }
        val counted = screenLines().firstNotNullOfOrNull { Regex("mes(?:\\.|ure) \\d+/(\\d+)").find(it)?.groupValues?.get(1)?.toInt() }
        assertTrue("the brick loops on $bars bars, the transport says ${counted ?: "nothing"}:\n" + screen(), counted == bars)
    }
}
