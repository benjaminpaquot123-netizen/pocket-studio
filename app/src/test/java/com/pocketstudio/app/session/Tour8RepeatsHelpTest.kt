package com.pocketstudio.app.session

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.onNodeWithTag
import com.pocketstudio.app.human.HumanTester
import com.pocketstudio.app.human.SeatOptions
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
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
 * Tour 8, lot T0c (seat 3a #14): « en hi, phone360 à 130 %, Mode Simple : dans Mon morceau, je lis pourquoi − / + sont
 * grisés. » The buttons were greyed (one repeat left, or a part played in Live) with no word saying why.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class Tour8RepeatsHelpTest : HumanTester("t8-repeats-help") {
    private val help = "− / + : répétitions de la partie (une partie jouée en Live garde sa longueur)."

    @Before
    fun notOnTheBench() = assumeTrue("not while the bench runs seats", System.getProperty("ps.human.dir").isNullOrBlank())

    @Test
    fun theHelpLineIsTranslatedInEveryLanguage() {
        val missing = (Lang.entries - Lang.FR).filter { I18n.table(it)[help].isNullOrBlank() }
        assertTrue("no translation of the − / + help in: $missing", missing.isEmpty())
    }

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

    @Test
    fun hindiSmallPhoneAt130SaysWhyMinusPlusAreGreyed() {
        begin(SeatOptions(lang = Lang.HI, device = "phone360", font = 130, simple = true))
        waitFor("the home's choices") { centerOf("welcome_guided") != null || centerOf("launch_new") != null }
        if (centerOf("welcome_guided") != null) tap("welcome_guided") else { tap("launch_new"); tap("starter_wizard") }
        if (centerOf("mode_card_simple") != null) tap("mode_card_simple")
        for (t in listOf("wizard_family_Techno", "wizard_style_techno", "wizard_style_choose_techno", "wizard_create_now")) {
            if (reachTag(t) != null) tap(t)
        }
        waitFor("the song's tabs") { centerOf("tab_simple_Morceau") != null }
        tap("tab_simple_Morceau")
        centerOf("guide_later")?.let { act("touche ${it.first} ${it.second}") }
        tap("add_part_new_empty")
        act("attends 1")
        val node = compose.onNodeWithTag("song_repeats_help", useUnmergedTree = true).fetchSemanticsNode()
        val text = node.config[SemanticsProperties.Text].joinToString("") { it.text }
        assertEquals("the − / + help, in Hindi\n" + screenLines().joinToString("\n"), I18n.table(Lang.HI)[help], text)
    }
}
