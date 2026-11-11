package com.pocketstudio.app.human

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.onAllNodesWithTag
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.instrumentTracks
import com.pocketstudio.model.sceneView
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.sceneTitle
import org.junit.Assert.assertEquals
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
 * Round 5's three blockers (lot « bloquants », PLAN-ACTION-AUDITS step 2), each replayed with a person's gestures:
 * - A a #27: Mode simple, the song playing, the editor's part menu › C (or B, or a new part): the editor stayed on A;
 * - E a #117: « Enregistrer ma mélodie », Retour, Pad Live, the project opened again: the take of 4 notes was gone;
 * - B c #23: Pro tutorial, chapter 1, « Swing »: the setting the step asks for was nowhere to be found.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class Tour5BloquantsTest : HumanTester("bloquants") {
    private val app: android.app.Application get() = androidx.test.core.app.ApplicationProvider.getApplicationContext()

    @Before
    fun notOnTheBench() = assumeTrue("not while the bench runs seats", System.getProperty("ps.human.dir").isNullOrBlank())

    private fun tap(tag: String): String {
        val at = reachTag(tag) ?: centerOf(tag)
        assertNotNull("nothing to touch: $tag\n" + screenLines().joinToString("\n"), at)
        return act("touche ${at!!.first} ${at.second}")
    }

    private fun tapText(text: String) {
        val at = centerOfText(text, substring = true)
        assertNotNull("nothing to touch: « $text »\n" + screenLines().joinToString("\n"), at)
        act("touche ${at!!.first} ${at.second}")
    }

    private fun waitFor(what: String, seconds: Int = 60, done: () -> Boolean) {
        val until = System.currentTimeMillis() + seconds * 1_000L
        while (!done()) {
            assertTrue("$what never came\n" + screenLines().joinToString("\n"), System.currentTimeMillis() < until)
            act("attends 0.5")
        }
    }

    private fun playing() = rhythm()!!.transport.value.playing

    private fun simpleSong(family: String = "Techno", style: String = "techno", seat: SeatOptions? = null) {
        if (seat == null) {
            begin()
            tapText("Fais-moi un morceau")
        } else {
            begin(seat)
            tap("welcome_guided")
        }
        tap("mode_card_simple")
        tap("wizard_family_$family")
        tap("wizard_style_$style")
        tap("wizard_style_choose_$style")
        tap("wizard_create_now")
        waitFor("the song's tabs") { screenRoute().endsWith("tab_simple_Morceau") }
    }

    /** The line under the editor's header (« Elle tourne en boucle… »), or "". */
    private fun headerNote(): String = compose.onAllNodesWithTag("part_header_note", useUnmergedTree = true).fetchSemanticsNodes()
        .firstOrNull()?.config?.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text } ?: ""

    /** The editor's header as drawn (« Partie : C · Refrain »), whatever the language. */
    private fun headerTitle(): String = compose.onAllNodesWithTag("part_header_title", useUnmergedTree = true).fetchSemanticsNodes()
        .firstOrNull()?.config?.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }
        ?: ("(no part header)\n" + screenLines().joinToString("\n"))

    /** A a #27: the song plays (the seat had pressed the Guide's « Écouter »), Clavier, the part menu › another part. */
    @Test
    fun simplePartPickedWhileTheSongPlaysIsTheOneEdited() {
        simpleSong()
        val p = project()!!
        assertTrue("a song of several scenes: ${p.allScenes.size}", p.allScenes.size >= 2)
        tap("thumb_play")
        assertTrue("▶ plays", playing())
        tap("tab_simple_Clavier")
        // The scene shown follows the song; the seat picked one the song was not on.
        val shown = project()!!.scene
        val target = project()!!.allScenes.indices.first { it != shown && it != shown + 1 }
        tap("part_header_pick")
        tap("part_pick_$target")
        // A person looks at the header right after the touch, then a moment later.
        act("attends 1")
        val name = project()!!.sceneTitle(target)
        assertTrue("the header names the part picked ($name): ${headerTitle()}", headerTitle().contains(name))
        act("attends 4")
        assertEquals("the part picked is the one edited\n" + screenLines().joinToString("\n"), target, project()!!.scene)
        assertTrue("the header still names it: ${headerTitle()}", headerTitle().contains(name))
    }
    /**
     * A a #27 as the seat played it: Lo-fi, the Guide's « Écouter » (the song from its intro, A), Clavier, the part
     * menu › C; then › B; then « + Nouvelle partie »: each time the header and the grid stayed on A.
     */
    @Test
    fun simplePartsPickedAfterTheGuidesListenAreEdited() = partsPickedAfterTheGuidesListen(null)

    /** The same on the seat's phone: Hindi, 360 dp, font at 130 %. */
    @Test
    fun simplePartsPickedOnTheSeatsPhone() = partsPickedAfterTheGuidesListen(SeatOptions(lang = Lang.HI, device = "phone360", font = 130, simple = true))

    private fun partsPickedAfterTheGuidesListen(seat: SeatOptions?) {
        simpleSong(family = "calm", style = "lofi", seat = seat)
        waitFor("the Guide's « Écouter »") { reachTag("guide_option_drop") != null }
        tap("guide_option_drop")
        assertTrue("the Guide's « Écouter » plays the song", playing())
        tap("tab_simple_Clavier")
        fun pick(item: String, what: String): Int {
            tap("part_header_pick")
            val before = project()!!.allScenes.size
            tap(item)
            val want = if (item == "part_new") before else item.removePrefix("part_pick_").toInt()
            val name = project()!!.sceneTitle(want)
            // Right after the touch (the part takes over at the next bar line, up to 3 s at 84 BPM), the header says the
            // choice is taken: its title, or its note while the part waits for its bar.
            val atOnce = headerTitle() + " / " + headerNote()
            assertTrue("$what: right after the touch, the header names $name: $atOnce", atOnce.contains(name))
            act("attends 4")
            assertEquals("$what: the part edited\n" + screenLines().joinToString("\n"), want, project()!!.scene)
            assertTrue("$what: the header still names $name: ${headerTitle()}", headerTitle().contains(name))
            return want
        }
        pick("part_pick_2", "C")
        pick("part_pick_1", "B")
        pick("part_new", "a new part")
    }
    private fun notesIn(p: com.pocketstudio.model.Project): Int =
        p.allScenes.indices.sumOf { s -> p.sceneView(s).instrumentTracks.sumOf { it.notes.notes.size } }

    /**
     * E a #117: Mode simple, Clavier, « Enregistrer ma mélodie », four keys while it records, Retour (the take still
     * running), « Pad Live » on the home screen, Retour, « Mes projets » › the song: the four notes must be there.
     */
    @Test
    fun aMelodyTakeLeftForPadLiveIsKept() {
        simpleSong()
        takeLeftForPadLive(rhythm()!!.projectId)
    }

    /**
     * E a #117 as the seat played it: a second song (« Créer et écouter »), deleted from « Mes projets » while open,
     * restored from the trash and opened again; then the take, Retour, Pad Live, and the song opened again.
     */
    @Test
    fun aMelodyTakeLeftForPadLiveIsKeptInASongBackFromTheTrash() {
        // A first song of another style (the seat's « Mon projet 1 »), to tell the two apart.
        simpleSong(family = "calm", style = "lofi")
        // Retour until the home screen (a first one may close a panel).
        repeat(4) { if (centerOf("launch_new") == null) act("retour") }
        waitFor("the home screen") { centerOf("launch_new") != null }
        tap("launch_new")
        tap("starter_wizard")
        if (centerOf("mode_card_simple") != null) tap("mode_card_simple")
        tap("wizard_family_Techno")
        tap("wizard_style_techno")
        tap("wizard_style_play_techno")
        waitFor("the second song") { screenRoute().contains("tab_simple_Morceau") }
        val id = rhythm()!!.projectId
        // Retour until the home screen (the seat needed two: the first one closed what « Créer et écouter » opened).
        repeat(4) { if (centerOf("launch_continue") == null) act("retour") }
        waitFor("the home screen after « Créer et écouter »") { centerOf("launch_continue") != null }
        tap("launch_continue")
        tap("top_more")
        tap("plus_projects")
        tap("project_menu_$id")
        tap("project_delete_$id")
        waitFor("« dans la corbeille »") { centerOf("project_deleted") != null }
        tapText("OK")
        act("retour")
        waitFor("the home screen after the trash") { centerOf("launch_projects") != null }
        tap("launch_projects")
        tap("project_trash")
        val restore = compose.onAllNodes(
            androidx.compose.ui.test.SemanticsMatcher("restore_$id…") { it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("restore_$id") == true },
        ).fetchSemanticsNodes()
        assertTrue("the song in the trash\n" + screenLines().joinToString("\n"), restore.isNotEmpty())
        tap(restore.first().config[SemanticsProperties.TestTag])
        tap("project_$id")
        waitFor("the song back from the trash") { rhythm()?.projectId == id && screenRoute().contains("tab_simple") }
        takeLeftForPadLive(id)
    }

    private fun takeLeftForPadLive(id: String) {
        val before = notesIn(project()!!)
        tap("tab_simple_Clavier")
        tap("record_start")
        waitFor("the take records") { rhythm()!!.noteTake.value?.phase == com.pocketstudio.app.record.TakePhase.RECORDING }
        for (key in listOf("key_60", "key_61", "key_72", "key_60")) {
            tap(key)
            act("attends 0.3")
        }
        val taken = notesIn(project()!!) - before
        assertTrue("the take wrote notes: ${rhythm()!!.noteTake.value}", taken >= 3)
        act("retour")
        waitFor("the home screen after the take") { centerOf("launch_padlive") != null }
        tap("launch_padlive")
        waitFor("Pad Live") { screenRoute().startsWith("padlive") }
        act("retour")
        waitFor("the home screen after Pad Live") { centerOf("launch_projects") != null }
        tap("launch_projects")
        tap("project_$id")
        waitFor("the song open again") { rhythm()?.projectId == id && screenRoute().contains("tab_simple") }
        act("attends 1")
        assertEquals("the take's notes are still in the song", before + taken, notesIn(project()!!))
        // What the seat looked at (its screens 125 and 149): Clavier, the melody's track, its notes in the part shown.
        tap("tab_simple_Clavier")
        val melody = com.pocketstudio.model.SimpleMelody.trackOf(project()!!)
        if (melody != null && centerOf("track_$melody") != null) tap("track_$melody")
        tap("mode_notes")
        act("attends 1")
        // Lot « réalisme » (R1): the screen in text writes the grid as drawn (no TalkBack words). In sight: the screen in
        // text has the grid's line where the grid is drawn; its notes: the grid node's own count (the notes it draws).
        // Where the screen in text puts the grid: the middle of what is drawn of it (it scrolls sideways, cut by its frame).
        val at = runCatching { compose.onAllNodesWithTag("piano_roll").fetchSemanticsNodes().firstOrNull()
            ?.let { BenchWindows.drawnBounds(it, androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>().resources.displayMetrics).center }
            ?.let { it.x to it.y } }.getOrNull()
        val lines = screenLines()
        val grid = at?.let { (x, y) -> lines.firstOrNull { l -> Regex("@ (\\d+),(\\d+)$").find(l)?.groupValues?.let { g ->
            kotlin.math.abs(g[1].toInt() - x) <= 2f && kotlin.math.abs(g[2].toInt() - y) <= 2f } == true } } ?: "(no grid)"
        assertTrue("the melody's grid in sight on the screen in text: $grid\n" + lines.joinToString("\n"), grid != "(no grid)")
        val said = runCatching { compose.onAllNodesWithTag("piano_roll").fetchSemanticsNodes().firstOrNull()
            ?.config?.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.ContentDescription)?.joinToString(" ") }.getOrNull()
        // Review m3: as many notes as the take wrote, not merely « not 0 ».
        val shown = said?.let { Regex("""Grille des notes : (\d+) notes?""").find(it)?.groupValues?.get(1)?.toInt() }
        assertEquals("the take's notes drawn on the melody's grid: $said ($grid)", taken, shown)
    }

    /** The node showing exactly [text], drawn whole above the tutorial's panel (a finger can touch it there, no swipe). */
    private fun inSightAboveTheTuto(text: String): Boolean {
        val nodes = compose.onAllNodes(androidx.compose.ui.test.hasText(text), useUnmergedTree = true).fetchSemanticsNodes()
        val panelTop = compose.onAllNodesWithTag("tuto_panel").fetchSemanticsNodes().firstOrNull()
            ?.let { BenchWindows.drawnBounds(it, app.resources.displayMetrics).top } ?: Float.MAX_VALUE
        return nodes.any { n -> shownWhole(n) && BenchWindows.drawnBounds(n, app.resources.displayMetrics).bottom <= panelTop }
    }

    /**
     * B c #23: Pro tutorial (Techno) in German on an 8-inch tablet, chapter 1 as the seat played it (Weiter, tempo 132,
     * F Phrygian), then the step « Gib ein ganz kleines bisschen Swing dazu (5 %) » › « Ich mach's »: the screen it opens
     * must show the Swing setting (round 5: the drum grid at the top of Sounds, Swing far below, never found).
     */
    @Test
    fun theTutorialsSwingStepShowsTheSwingSetting() {
        begin(SeatOptions(lang = Lang.DE, device = "tablet8", simple = false))
        tap("launch_learn")
        if (centerOf("mode_card_pro") != null) tap("mode_card_pro")
        tap("tuto_style_techno")
        val tuto = { com.pocketstudio.app.tuto.PrefsTutoStore(app).load(rhythm()!!.projectId)?.current }
        waitFor("the tutorial's first step") { centerOf("tuto_next") != null || centerOf("tuto_do") != null }
        tap("tuto_next")
        // Tempo: « Ich mach's », 1 3 2, OK, « Geschafft ».
        tap("tuto_do")
        for (k in listOf("tempo_key_1", "tempo_key_3", "tempo_key_2", "tempo_key_ok")) tap(k)
        tap("tuto_next_folded")
        // Key: « Ich mach's », Phrygisch, F, back to the tutorial, « Geschafft ».
        tap("tuto_do")
        tap("tuto_scale_Phrygian")
        tap("tuto_root_5")
        tap("tuto_key_continue")
        tap("tuto_next_folded")
        assertEquals("the swing step", "swing", tuto())
        repeat(2) { if (centerOf("tuto_kit_later") != null) tap("tuto_kit_later") }
        tap("tuto_do")
        act("attends 2")
        assertTrue("« Ich mach's » shows the Swing setting\n" + screenLines().joinToString("\n"), inSightAboveTheTuto("Swing"))
    }
    /** The waiting line's words (« … arrive à la prochaine mesure… ») in the header's note, in French. */
    private fun waitingLineShown(): String? = headerNote().takeIf { "arrive à la prochaine mesure" in it }

    /** Watches the header for [seconds] while the song plays on, nothing picked: never the waiting line. */
    private fun neverTheWaitingLine(seconds: Int, what: String): Set<String> {
        val notes = HashSet<String>()
        repeat(seconds * 2) {
            act("attends 0.5")
            assertTrue("$what: still playing", playing())
            waitingLineShown()?.let { error("$what: « ${headerTitle()} / $it » with nothing picked (sessionUi ${rhythm()!!.sessionUi.value})") }
            notes += headerNote()
        }
        return notes
    }

    /**
     * Review G1 (lot « bloquants »): the song playing, nothing picked, the waiting line named the song's next part (its
     * part change staged ahead) for the whole part. Mode simple, the Guide's « Écouter », Clavier.
     */
    @Test
    fun noWaitingLineWhileTheSongJustPlaysInSimple() {
        simpleSong(family = "calm", style = "lofi")
        waitFor("the Guide's « Écouter »") { reachTag("guide_option_drop") != null }
        tap("guide_option_drop")
        assertTrue("the song plays", playing())
        tap("tab_simple_Clavier")
        val notes = neverTheWaitingLine(8, "Simple")
        assertTrue("« Le morceau joue… » said meanwhile: $notes", notes.any { it.startsWith("Le morceau joue") })
    }

    /** Review G1, Pro: ▶ of the top bar plays the song, Sons › Drums. */
    @Test
    fun noWaitingLineWhileTheSongJustPlaysInPro() {
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
        tap("tab_Sons")
        tap("top_play")
        assertTrue("▶ plays", playing())
        neverTheWaitingLine(10, "Pro")
    }
}
