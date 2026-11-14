package com.pocketstudio.app.ui

import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pocketstudio.app.human.HumanTester
import com.pocketstudio.app.human.SeatOptions
import org.junit.Assert.assertNotNull
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
        val said = compose.onAllNodesWithTag("projects_back", useUnmergedTree = true).fetchSemanticsNodes().flatMap { n ->
            n.children.flatMap { it.config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.ContentDescription) { emptyList() } } +
                n.config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.ContentDescription) { emptyList() }
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

}
