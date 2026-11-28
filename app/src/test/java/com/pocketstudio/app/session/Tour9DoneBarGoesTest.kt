package com.pocketstudio.app.session

import com.pocketstudio.app.human.HumanTester
import com.pocketstudio.app.human.SeatOptions
import com.pocketstudio.model.i18n.Lang
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 9, lot L12 (F111, seats t9-24, t9-49, t9-55, t9-59, t9-45): « Le message « nouvelle longueur » disparaît après
 * quelques secondes ou au geste suivant. » After a part's new length, « Fait : nouvelle longueur pour C · Couplet ·
 * Annuler » stayed over four more touches (about a second each) and into the next tutorial chapter, where it seemed to
 * speak of what the seat had just done (« Ustaw zalecane długości »).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class Tour9DoneBarGoesTest : HumanTester("t9-done-bar") {
    @Before
    fun notOnTheBench() = assumeTrue("not while the bench runs seats", System.getProperty("ps.human.dir").isNullOrBlank())

    private fun tap(tag: String) {
        val at = reachTag(tag)
        assertNotNull("nothing to touch: $tag\n" + screenLines().joinToString("\n"), at)
        act("touche ${at!!.first} ${at.second}")
    }

    private fun waitFor(what: String, done: () -> Boolean) {
        val until = System.currentTimeMillis() + 60_000
        while (!done()) {
            assertTrue("$what never came\n" + screenLines().joinToString("\n"), System.currentTimeMillis() < until)
            act("attends 0.5")
        }
    }

    private fun screen() = screenLines().joinToString("\n")

    /** The song's length in bars, as the bar under the tabs shows it (« Intro 2 mesure 1/176 »). */
    private fun songBars(): Int? = screenLines().firstNotNullOfOrNull { Regex("1/(\\d+)").find(it)?.groupValues?.get(1)?.toInt() }

    /** Into a new techno song's parts (Mode simple), then a part made one repeat longer from its « ⋮ » sheet. */
    private fun aPartMadeLonger(lang: Lang): Int {
        begin(SeatOptions(lang = lang, device = "phone", font = 100, simple = true))
        waitFor("the home's choices") { centerOf("welcome_guided") != null || centerOf("launch_new") != null }
        if (centerOf("welcome_guided") != null) tap("welcome_guided") else { tap("launch_new"); tap("starter_wizard") }
        if (centerOf("mode_card_simple") != null) tap("mode_card_simple")
        for (t in listOf("wizard_family_Techno", "wizard_style_techno", "wizard_style_choose_techno", "wizard_create_now")) {
            if (reachTag(t) != null) tap(t)
        }
        waitFor("the song's tabs") { centerOf("tab_simple_Morceau") != null }
        tap("tab_simple_Morceau")
        centerOf("guide_later")?.let { act("touche ${it.first} ${it.second}") }
        act("attends 1")
        val before = songBars()
        assertNotNull("no song length shown\n" + screen(), before)
        tap("part_menu_0")
        waitFor("the part's sheet") { centerOf("part_longer") != null }
        tap("part_longer")
        waitFor("the sheet closing") { centerOf("part_sheet") == null }
        assertNotNull("no « Fait : nouvelle longueur » after a part made longer\n" + screen(), centerOf("done_bar"))
        assertTrue("the song is no longer ($before bars before)\n" + screen(), (songBars() ?: 0) > before!!)
        return before
    }

    @Test fun frenchTheNextTouchLetsTheMessageGo() {
        aPartMadeLonger(Lang.FR)
        // The next gesture, on the page (the tab already open: nothing else changes), well before six seconds.
        tap("tab_simple_Morceau")
        assertNull("« Fait : … Annuler » still shown after the next touch\n" + screen(), centerOf("done_bar"))
    }

    @Test fun polishTheNextSwipeLetsTheMessageGo() {
        aPartMadeLonger(Lang.PL)
        // The next gesture: a swipe up the song's page (a part's « ⋮ » would land on the message's « Anuluj » here).
        act("glisse 294 700 294 400")
        assertNull("« Gotowe: nowa długość … Anuluj » still shown after a swipe\n" + screen(), centerOf("done_bar"))
    }

    @Test fun withoutATouchItGoesAfterAFewSeconds() {
        aPartMadeLonger(Lang.FR)
        act("attends 7")
        assertNull("« Fait : … Annuler » still shown seven seconds later\n" + screen(), centerOf("done_bar"))
    }

    @Test fun itsAnnulerStillUndoes() {
        val before = aPartMadeLonger(Lang.FR)
        tap("done_undo")
        act("attends 0.5")
        assertNull("« Fait : … Annuler » still shown after « Annuler »\n" + screen(), centerOf("done_bar"))
        assertEquals("« Annuler » did not bring the song back to its length\n" + screen(), before, songBars())
    }
}
