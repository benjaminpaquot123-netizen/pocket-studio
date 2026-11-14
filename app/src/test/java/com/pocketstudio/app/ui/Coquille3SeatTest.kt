package com.pocketstudio.app.ui

import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
 * Lot « coquille3 » (tour 6): what coquille2 did not take, each proven by the bench in the conditions of its seat, the
 * whole app.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class Coquille3SeatTest : HumanTester("coquille3") {
    private fun tap(tag: String): String {
        val at = reachTag(tag) ?: error("no $tag on:\n" + screenLines().joinToString("\n"))
        return act("touche ${at.first} ${at.second}")
    }

    private fun screen() = screenLines().joinToString("\n")

    private fun whole(tag: String): Boolean =
        compose.onAllNodesWithTag(tag).fetchSemanticsNodes().firstOrNull()?.let { shownWhole(it) } == true

    /** Seat A c's start: « Fais-moi un morceau », Simple, Hip-hop › Trap, « Créer et écouter ». */
    private fun trapSong() {
        begin(SeatOptions.parse(mapOf("lang" to "fr", "device" to "phone", "font" to "100", "mode" to "simple")).first)
        tap("welcome_guided"); tap("mode_card_simple"); tap("wizard_family_Hip-hop"); tap("wizard_style_trap"); tap("wizard_style_play_trap")
        repeat(5) { act("attends 1") }
    }

    /**
     * Seat A c (E7, control Ac-2 screen 31), decided the owner's way (« ← Mes projets » goes to the welcome page,
     * 2026-10-29): the arrow says so before it is touched — « Accueil », not « Retour » — and it goes there.
     */
    @Test
    fun e7_myProjectsArrowSaysItGoesHome() {
        assumeTrue(System.getProperty("ps.human.dir").isNullOrBlank())
        trapSong()
        tap("top_more"); tap("top_projects")
        repeat(2) { act("attends 1") }
        val said = compose.onAllNodesWithTag("projects_back").fetchSemanticsNodes().flatMap { n ->
            n.children.flatMap { it.config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.ContentDescription) { emptyList() } } +
                n.config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.ContentDescription) { emptyList() } +
                n.config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.Text) { emptyList() }.map { it.text }
        }
        assertTrue("the arrow of « Mes projets » says $said, not where it goes:\n" + screen(), said.any { "Accueil" in it })
        tap("projects_back")
        repeat(2) { act("attends 1") }
        assertNotNull("the arrow did not go to the welcome page:\n" + screen(), centerOf("launch_projects"))
    }

    /** Seat A c, gestures 7-13: the song sent as M4A (the share page's choice), then « Autres fichiers » (Pro). */
    private fun sentThenPro() {
        trapSong()
        tap("tab_simple_Envoyer")
        tap("share_go")
        repeat(40) { if (rhythm()?.export?.value?.running == true) act("attends 1") }
        act("touche 294 543")  // the phone's share sheet: WhatsApp
        runCatching { tap("share_bilan_ok") }
        compose.onNodeWithTag("share_other").performScrollTo().performClick()
        repeat(2) { act("attends 1") }
    }

    /** Seat A c (gesture 13, screen 17): going Pro from the share page keeps the format chosen there (M4A), never WAV by itself. */
    @Test
    fun n1_proKeepsTheFormatChosenOnTheSharePage() {
        assumeTrue(System.getProperty("ps.human.dir").isNullOrBlank())
        sentThenPro()
        val format = rhythm()!!.export.value.format
        assertTrue("Pro's files are $format though M4A was chosen:\n" + screen(), format == com.pocketstudio.app.rhythm.ExportFormat.M4a)
    }

    /**
     * Control seat Ac-3 (gesture 43, screen 45), Pro: the share page's volume sentence is read whole, « détails » and its
     * « ? » after it — not « comme la musique de détails ? club ».
     */
    @Test
    fun n5_theVolumeSentenceIsWholeBeforeItsDetails() {
        assumeTrue(System.getProperty("ps.human.dir").isNullOrBlank())
        sentThenPro()
        tap("export_target_CLUB")
        tap("top_share")
        repeat(2) { act("attends 1") }
        val sentence = compose.onAllNodesWithTag("share_loudness").fetchSemanticsNodes().firstOrNull() ?: error("no volume sentence:\n" + screen())
        val details = compose.onAllNodesWithTag("share_details").fetchSemanticsNodes().firstOrNull() ?: error("no « détails »:\n" + screen())
        assertTrue("« détails » beside the sentence, read in its middle:\n" + screen(), details.boundsInRoot.top >= sentence.boundsInRoot.bottom - 1f)
    }

    /** Seat B a (es, Mode simple, left hand), A11 (control Ba-5, screen 6): « Remix » of « Más » is under the left thumb. */
    @Test
    fun a11_remixOfMoreIsUnderTheLeftThumb() {
        assumeTrue(System.getProperty("ps.human.dir").isNullOrBlank())
        begin(SeatOptions.parse(mapOf("lang" to "es", "device" to "phone", "font" to "100", "mode" to "simple", "main" to "gauche")).first)
        amenee("premier-morceau", java.io.File("build/coquille3-journal.txt"))
        tap("thumb_more")
        repeat(2) { act("attends 1") }
        // Its middle, where a thumb aims (control Ba-5: reached only by the card's edge).
        val n = compose.onAllNodesWithTag("plus_remix").fetchSemanticsNodes().firstOrNull() ?: error("no « Remix »:\n" + screen())
        val m = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>().resources.displayMetrics
        val p = com.pocketstudio.app.human.BenchWindows.drawnPosition(n, m)
        assertTrue("« Remix » of « Más » out of the left thumb (its middle):\n" + screen(),
            SeatOptions.reachable(SeatOptions.Hand.LEFT, p.x + n.size.width / 2f, p.y + n.size.height / 2f, m.widthPixels, m.heightPixels, m.density))
    }

    /**
     * Seat A c (gesture 27) and control Ac-3 (gesture 25): right after a song is sent, « Voir mes fichiers » lists it —
     * never an empty list.
     */
    @Test
    fun n4_myFilesListTheSongJustSent() {
        assumeTrue(System.getProperty("ps.human.dir").isNullOrBlank())
        trapSong()
        tap("tab_simple_Envoyer")
        tap("share_go")
        repeat(40) { if (rhythm()?.export?.value?.running == true) act("attends 1") }
        act("touche 294 543")  // the phone's share sheet: WhatsApp
        runCatching { tap("share_bilan_ok") }
        // The seat's gesture 27: « Mes fichiers » of the share page (control Ac-3: « Voir mes fichiers » of the bilan, the same list).
        tap("share_my_exports")
        // The list is read off the main thread: a person waits while « Chargement de la liste… » shows; the bench's
        // « attends » moves its clock, not the wall's (MyFilesBenchClockTest), so the wait is in real time, up to 20 s.
        act("attends 1")
        // As a person waits: the bench's « attends » (its clock: the read's answer is taken on the main thread as its clock
        // moves) and real time (the read itself runs off the main thread), until the list comes, up to 60 real seconds.
        // (A wait on real time alone, compose.waitUntil, never moved the bench's clock: the answer, read, stayed unread.)
        val t0 = System.nanoTime()
        var loaded = false
        while (!loaded && System.nanoTime() - t0 < 60_000_000_000L) {
            Thread.sleep(100)
            act("attends 1")
            loaded = centerOf("my_exports_wait") == null
        }
        val waitedMs = (System.nanoTime() - t0) / 1_000_000
        assertTrue("« Chargement de la liste… » still there after $waitedMs real ms:\n" + screen(), loaded)
        assertTrue("« Mes fichiers » without the song just sent (read in $waitedMs real ms):\n" + screen(),
            centerOf("my_exports_empty") == null && centerOf("my_export_detail") != null)
    }

    /** Seat B a (es, Mode simple, left hand), A10 (control Ba-2 screen 3): one « Descargar el modelo (174 MB) » on the panel. */
    @Test
    fun a10_oneDownloadButtonOnTheRemixPanel() {
        assumeTrue(System.getProperty("ps.human.dir").isNullOrBlank())
        begin(SeatOptions.parse(mapOf("lang" to "es", "device" to "phone", "font" to "100", "mode" to "simple", "main" to "gauche")).first)
        amenee("remix", java.io.File("build/coquille3-journal.txt"))
        repeat(2) { act("attends 1") }
        val buttons = screenLines().count { "Descargar el modelo" in it && it.startsWith("bouton") }
        assertTrue("$buttons « Descargar el modelo » on the panel:\n" + screen(), buttons == 1)
    }

    /**
     * Seat A b (fr, Mode simple), P1 (gesture 34, screen 43): a melody played after the first bar (bar 1 empty), « Notes »
     * touched on the keyboard: the grid shows the notes played, not an empty bar 1 that makes the melody look lost.
     */
    @Test
    fun p1_notesShowsEveryNotePlayed() {
        assumeTrue(System.getProperty("ps.human.dir").isNullOrBlank())
        begin(SeatOptions.parse(mapOf("lang" to "fr", "device" to "phone", "font" to "100", "mode" to "simple")).first)
        amenee("premier-morceau", java.io.File("build/coquille3-journal.txt"))
        tap("tab_simple_Clavier")
        repeat(2) { act("attends 1") }
        // The seat's melody, played with the keyboard (the bench has no recording of played notes): bars 2 to 4.
        val v = rhythm()!!
        val tick = com.pocketstudio.model.NoteClip.TICKS_PER_STEP
        val melody = (1 until 4).flatMap { bar -> (0 until 4).map { beat -> com.pocketstudio.model.Note((bar * 16 + beat * 4) * tick, 2 * tick, 64 + beat) } }
        v.replaceNoteClip(com.pocketstudio.model.NoteClip(steps = 64, notes = melody), null)
        repeat(2) { act("attends 1") }
        tap("mode_notes")
        repeat(2) { act("attends 1") }
        // Bars 2 and 4 (the melody's first and last) in sight on the grid's ruler.
        assertTrue("« Notes » opened on bar 1, the melody's bars out of sight:\n" + screen(), whole("roll_bar_1") && whole("roll_bar_3"))
    }


    /**
     * Fresh review of coquille3 (grave): the frame asked by « Notes » is one touch's, not the grid's for good: the zoom
     * the user chose after it stays when the grid comes back from another tab (nothing zooms by itself).
     */
    @Test
    fun p1_theZoomChosenAfterNotesStaysWhenTheGridComesBack() {
        assumeTrue(System.getProperty("ps.human.dir").isNullOrBlank())
        begin(SeatOptions.parse(mapOf("lang" to "fr", "device" to "phone", "font" to "100", "mode" to "simple")).first)
        amenee("premier-morceau", java.io.File("build/coquille3-journal.txt"))
        tap("tab_simple_Clavier")
        repeat(2) { act("attends 1") }
        val v = rhythm()!!
        val tick = com.pocketstudio.model.NoteClip.TICKS_PER_STEP
        val melody = (1 until 4).flatMap { bar -> (0 until 4).map { beat -> com.pocketstudio.model.Note((bar * 16 + beat * 4) * tick, 2 * tick, 64 + beat) } }
        v.replaceNoteClip(com.pocketstudio.model.NoteClip(steps = 64, notes = melody), null)
        repeat(2) { act("attends 1") }
        tap("mode_notes")
        repeat(2) { act("attends 1") }
        assertTrue("« Notes » did not frame the melody:\n" + screen(), whole("roll_bar_3"))
        // The user zooms in (one bar or so in sight), goes to another tab and comes back.
        repeat(6) { compose.onNodeWithTag("zoom_in").performScrollTo().performClick() }  // its row scrolls sideways on a phone
        act("attends 1")
        assertTrue("the zoom in left bar 4 in sight:\n" + screen(), !whole("roll_bar_3"))
        tap("tab_simple_Rythme"); act("attends 1")
        tap("tab_simple_Clavier"); repeat(2) { act("attends 1") }
        assertTrue("the grid framed again by itself when it came back:\n" + screen(), !whole("roll_bar_3"))
    }

    /** Fresh review of coquille3 (moyen): a FLAC chosen in Pro stays when « Autres fichiers » leads there again. */
    @Test
    fun n1_aFlacChosenInProStays() {
        assumeTrue(System.getProperty("ps.human.dir").isNullOrBlank())
        trapSong()
        rhythm()!!.setExportFormat(com.pocketstudio.app.rhythm.ExportFormat.Flac)
        tap("tab_simple_Envoyer")
        compose.onNodeWithTag("share_other").performScrollTo().performClick()
        repeat(2) { act("attends 1") }
        val format = rhythm()!!.export.value.format
        assertTrue("the FLAC chosen in Pro became $format:\n" + screen(), format == com.pocketstudio.app.rhythm.ExportFormat.Flac)
    }

    /**
     * Control seat of coquille3 (E7 again, screens 6 and 8): the house alone said nothing on the screen; « Accueil » is
     * written beside it, read without TalkBack.
     */
    @Test
    fun e7_theHomeWordIsWrittenBesideTheHouse() {
        assumeTrue(System.getProperty("ps.human.dir").isNullOrBlank())
        trapSong()
        tap("top_more"); tap("top_projects")
        repeat(2) { act("attends 1") }
        val back = compose.onAllNodesWithTag("projects_back").fetchSemanticsNodes().firstOrNull() ?: error("no way back:\n" + screen())
        val written = back.config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.Text) { emptyList() }.map { it.text }
        assertTrue("the house of « Mes projets » writes $written:\n" + screen(), written.any { "Accueil" in it })
        assertTrue("« Accueil » cut:\n" + screen(), whole("projects_back"))
    }

    /**
     * Control seat Cq3 (gestures 1-16, screens 14-16): a demo, « Mes fichiers » opened and closed BEFORE sending, the song
     * sent, then « Mes fichiers » again: the list comes, not « Chargement de la liste… » for good.
     */
    @Test
    fun n4_myFilesOpenedBeforeAndAfterASendLoadsInRealTime() {
        assumeTrue(System.getProperty("ps.human.dir").isNullOrBlank())
        begin(SeatOptions.parse(mapOf("lang" to "fr", "device" to "phone", "font" to "100", "mode" to "simple")).first)
        tap("welcome_demo"); runCatching { tap("mode_card_simple") }
        repeat(3) { act("attends 1") }
        centerOfText("Plus tard")?.let { act("touche ${it.first} ${it.second}") }
        tap("tab_simple_Envoyer")
        tap("share_format_M4a")
        tap("share_my_exports"); repeat(2) { act("attends 1") }; tap("my_exports_close")
        tap("share_go")
        repeat(40) { if (rhythm()?.export?.value?.running == true) act("attends 1") }
        act("touche 294 543")  // the phone's share sheet: WhatsApp
        runCatching { tap("share_bilan_ok") }
        tap("share_my_exports")
        // « Chargement de la liste… » is the list being read off the main thread (MediaStore, Dispatchers.IO). The bench's
        // « attends 5 » moves its clock, not the wall's (proof: far under 5 real seconds): a read slower than that, on a
        // loaded machine (the seats, a cold JVM: 1 run in 5 here), is still « Chargement » after any « attends ».
        val t0 = System.nanoTime()
        act("attends 5")
        val realMs = (System.nanoTime() - t0) / 1_000_000
        assertTrue("« attends 5 » took $realMs real ms: the bench's clock is the wall's", realMs < 5_000)
        // Given real time, the list always comes (the app never stays on « Chargement »): the seats' « 95 s » were the bench's.
        compose.waitUntil(20_000) { compose.onAllNodesWithTag("my_exports_wait").fetchSemanticsNodes().isEmpty() }
        assertNull("« Chargement de la liste… » still there 20 real seconds after « Mes fichiers »:\n" + screen(), centerOf("my_exports_wait"))
    }
}
