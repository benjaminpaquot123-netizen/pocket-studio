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
 * Round 5, chantier 6: the home screen's promise « Nouveau morceau : un style en une touche » kept, the whole app played
 * with a person's touches (P009, P046; T3 F a 58 « 4 écrans », T4 D d 12 « 5 touchers »). From « Que veux-tu faire ? »:
 * « Nouveau morceau », then one touch on a style; the song is made, opens and plays (« prêt à écouter »). The very first
 * time « Simple ou Pro ? » comes in between, once.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class OneTouchSongPlaysTest : HumanTester("accueil") {
    @Before
    fun notOnTheBench() = assumeTrue("not while the bench runs seats", System.getProperty("ps.human.dir").isNullOrBlank())

    private var touches = 0

    private fun tap(tag: String) {
        val at = reachTag(tag) ?: centerOf(tag)
        assertNotNull("nothing to touch: $tag\n" + screenLines().joinToString("\n"), at)
        touches++
        act("touche ${at!!.first} ${at.second}")
    }

    private fun waitFor(what: String, seconds: Int = 60, done: () -> Boolean) {
        val until = System.currentTimeMillis() + seconds * 1_000L
        while (!done()) {
            assertTrue("$what never came\n" + screenLines().joinToString("\n"), System.currentTimeMillis() < until)
            act("attends 0.5")
        }
    }

    private fun home() {
        repeat(5) { if (centerOf("launch_new") == null) act("retour") }
        waitFor("« Que veux-tu faire ? »") { centerOf("launch_new") != null }
    }

    /** From the home screen: « Nouveau morceau », one touch on [style]; the song of that style plays. */
    private fun oneTouch(style: String, modeOnce: Boolean) {
        home()
        touches = 0
        tap("launch_new")
        tap("one_touch_$style")
        if (modeOnce) tap("mode_card_simple")
        waitFor("the $style song, playing") { project()?.style == style && rhythm()!!.transport.value.playing }
        assertEquals("touches from the home screen to the $style song playing", if (modeOnce) 3 else 2, touches)
    }

    @Test
    fun aStyleInOneTouchFromTheHomeScreenPlaysItsSong() {
        begin()
        tap("welcome_later")
        // The first time on this phone: « Simple ou Pro ? » once.
        oneTouch("house", modeOnce = true)
        // Then: one touch on the style.
        oneTouch("techno", modeOnce = false)
    }
}
