package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Audit ultra round 6, N15 (seat C b #13): « Mes fichiers » was « caché sous le bandeau « Nouveau son : Basse ronde » ».
 * « Fait : … Annuler » floated six seconds over the bottom of the page, on whatever button was there: a touch meant for
 * it landed on the line, or on « Annuler ». The line now has its own place above the bars: it covers nothing a touch
 * can reach.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-xhdpi")
class Tour6DoneBarTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val h = AppHarness(compose)
    private val shell = Tour6Shell(compose)

    @After
    fun back() = h.finish()

    /** « Fait : … Annuler » brought by a choice of the Guide (« Sauter »), as a sound changed brings it. */
    private fun sayDone() {
        val card = h.vm.guide.ui.value.next ?: h.vm.guide.ui.value.cards.values.first { !it.status.settled }
        h.vm.guide.skip(card.id)
        assertTrue("« Fait : … » shows", shell.reached(5_000) { shell.has("done_bar") })
        shell.idle(300)
    }

    /** What a touch can reach under the line, on each of [tabs]. */
    private fun covered(simple: Boolean, scale: Float, tabs: List<String>): List<String> {
        h.start(simple = simple, scale = scale)
        h.awaitGuide()
        assertTrue("the Guide has read the song", shell.reached { !h.vm.guide.ui.value.loading })
        val own = setOf("done_bar")
        return tabs.flatMap { tab ->
            shell.tap(tab)
            sayDone()
            val line = shell.bounds("done_bar")!!
            val under = shell.touchables().filter { !Tour6Shell.within(it, own) && it.boundsInRoot.overlaps(line) && it.boundsInRoot.height > 1f }
            h.vm.dismissDone()
            shell.idle(100)
            under.map { "$tab: " + Tour6Shell.name(it) + " under the line $line" }
        }
    }

    private val simpleTabs = listOf("tab_simple_Morceau", "tab_simple_Rythme", "tab_simple_Clavier", "tab_simple_Mix", "tab_simple_Envoyer")
    private val proSpaces = listOf("tab_Structure", "tab_Sons", "tab_Mix", "tab_Plus")

    @Test
    fun theDoneLineCoversNothingToTouchInSimple() {
        val under = covered(simple = true, scale = 1f, tabs = simpleTabs)
        assertTrue("« Fait : … Annuler » covers what a touch can reach:\n" + under.joinToString("\n"), under.isEmpty())
    }

    @Test
    fun theDoneLineCoversNothingToTouchInPro() {
        val under = covered(simple = false, scale = 1f, tabs = proSpaces)
        assertTrue("« Fait : … Annuler » covers what a touch can reach:\n" + under.joinToString("\n"), under.isEmpty())
    }

    @Test
    fun theDoneLineCoversNothingToTouchWithBigText() {
        val under = covered(simple = true, scale = 1.3f, tabs = simpleTabs)
        assertTrue("« Fait : … Annuler » covers what a touch can reach:\n" + under.joinToString("\n"), under.isEmpty())
    }
}
