package com.pocketstudio.app.human

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
 * Audit ultra tour 0, seat a (parent pressé, premier morceau): every « Écouter » of the assistant said « Cette
 * écoute n'a pas pu être préparée » and the guide's « Écouter » only moved the song's place (« Montée mes. 25 »),
 * then ▶ played the first part's loop. A phone's engine renders the previews and plays the song from its part:
 * the bench now does both, as on a phone.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class BenchAssistantTest : HumanTester("assistant") {
    @Before
    fun notOnTheBench() = assumeTrue("not while the bench runs seats", System.getProperty("ps.human.dir").isNullOrBlank())

    /** Scrolled to first, as a person would (n° 33: the song's length above the moods put « Joyeux »'s ▶ under « Suivant »). */
    private fun tap(tag: String): String {
        val at = reachTag(tag)
        assertNotNull("nothing to touch: $tag\n" + screenLines().joinToString("\n"), at)
        return act("touche ${at!!.first} ${at.second}")
    }

    private fun failed() = screenLines().any { "n'a pas pu être préparée" in it }

    @Test
    fun theAssistantsListensPlayAndTheGuidePlaysTheSongFromItsPart() {
        begin()
        act("touche ${centerOfText("Fais-moi un morceau", substring = true)!!.first} ${centerOfText("Fais-moi un morceau", substring = true)!!.second}")
        tap("mode_card_simple")
        tap("wizard_family_House")
        val style = tap("wizard_style_listen_house")
        val heard = if ("extrait du style" in style) style else act("attends 1")
        assertTrue(heard + "\n" + screenLines().joinToString("\n"), "l'assistant fait écouter un extrait du style « House »" in heard)
        assertTrue(screenLines().joinToString("\n"), !failed())
        // It ends as the sound does (the button is « Écouter » again). Since round 3 the bench renders as long as asked: the
        // excerpt's 4 bars (about 8 s at the style's tempo), not a short stand-in; waited for, at most 20 s.
        val excerptEnd = System.currentTimeMillis() + 20_000
        var now = act("attends 1")
        while ("extrait du style" in now) {
            assertTrue("the excerpt never ended: $now", System.currentTimeMillis() < excerptEnd)
            now = act("attends 1")
        }

        tap("wizard_style_house")
        tap("wizard_style_choose_house")
        val mood = tap("wizard_mood_listen_happy").let { if ("ambiance" in it) it else act("attends 1") }
        assertTrue(mood + "\n" + screenLines().joinToString("\n"), "les accords de l'ambiance" in mood)
        assertTrue(screenLines().joinToString("\n"), !failed())

        tap("wizard_create_now")
        val until = System.currentTimeMillis() + 60_000
        while (centerOf("guide_option_drop") == null) {
            assertTrue("no guide « Écouter »\n" + screenLines().joinToString("\n"), System.currentTimeMillis() < until)
            centerOfText("Compris")?.let { act("touche ${it.first} ${it.second}") } ?: act("attends 0.5")
        }
        val drop = tap("guide_option_drop").let { if ("le morceau joue" in it) it else act("attends 1") }
        assertTrue(drop, "le morceau joue (partie" in drop)
        val vm = rhythm()!!
        assertTrue("the song plays", vm.transport.value.playing && vm.sessionUi.value.songIndex >= 0)
        assertEquals("from the part the guide named", vm.playFrom.value, vm.sessionUi.value.songIndex)
    }
}
