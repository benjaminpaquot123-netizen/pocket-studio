package com.pocketstudio.app.human

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.onAllNodesWithTag
import com.pocketstudio.model.allScenes
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
            tapText("Créer mon premier morceau")
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
}
