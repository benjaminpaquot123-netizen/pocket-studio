package com.pocketstudio.app.tuto

import com.pocketstudio.app.human.HumanTester
import com.pocketstudio.app.human.SeatOptions
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 6, replay `t6-ha19-cases-panneau` (seat H a, French, the seats' phone, text at 100 %, Mode simple, house
 * « Facile »): « J'ai compris » on « Avant de commencer » brings the kick step, panel open; its first row of dotted
 * cells was cut under the tutorial's panel (lot tutogrille checked 360 dp at 130 % only, with « Je le fais » touched).
 * The seat's 19 gestures, then every dotted cell to touch is drawn whole (never « [coupé, état « vide, en pointillé »).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class Tour6TutoCellsPanel100Test : HumanTester("tuto4") {
    private fun tap(tag: String): String {
        val at = reachTag(tag) ?: error("no $tag on:\n" + screenLines().joinToString("\n"))
        return act("touche ${at.first} ${at.second}")
    }

    @Test
    fun theKickStepsDottedCellsAreWholeAboveThePanelAt100() {
        assumeTrue(System.getProperty("ps.human.dir").isNullOrBlank())
        begin(SeatOptions.parse(mapOf("lang" to "fr", "device" to "phone", "font" to "100", "mode" to "simple")).first)
        tap("welcome_learn"); tap("mode_card_simple"); tap("tuto_style_house"); tap("tuto_listen")
        act("attends 8")
        tap("tuto_next_big"); tap("tuto_do")
        for (k in listOf("tempo_key_1", "tempo_key_2", "tempo_key_2", "tempo_key_ok")) tap(k)
        tap("tuto_next_folded"); tap("tuto_do"); tap("tuto_scale_Dorian"); tap("tuto_root_9"); tap("tuto_key_continue")
        tap("tuto_chapter_end_folded"); tap("tuto_next_big"); tap("tuto_mark")
        val screen = screenLines()
        assertTrue("not at the kick step:\n" + screen.joinToString("\n"), screen.any { "Le kick" in it })
        val dotted = screen.filter { "en pointillé : à toucher" in it }
        assertTrue("no dotted cell in sight:\n" + screen.joinToString("\n"), dotted.isNotEmpty())
        val cut = dotted.filter { "coupé" in it }
        assertTrue("${cut.size} dotted cells cut:\n" + screen.joinToString("\n"), cut.isEmpty())
    }
}
