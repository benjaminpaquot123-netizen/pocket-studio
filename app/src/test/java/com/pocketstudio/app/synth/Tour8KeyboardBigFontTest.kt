package com.pocketstudio.app.synth

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.hasTestTag
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.human.BenchWindows
import com.pocketstudio.app.human.HumanTester
import com.pocketstudio.app.human.SeatOptions
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
 * Tour 8, lot clavier200 (defect t4-26): « en fr, deux mains, phone à 200 %, Mode Simple : depuis le Clavier, je touche
 * les touches 62 et 65 puis « Enregistrer ma mélodie » et la prise démarre. » The Clavier's head (the track bar, the
 * mode switch, the take's card, the keyboard's title and its chips) pushed the keys under the fold: a finger could not
 * reach them without scrolling. Here, as the person sees it: the keys 62 and 65 and the take's button are whole on the
 * screen, above the Guide's bar when it is there, without a scroll.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class Tour8KeyboardBigFontTest : HumanTester("t8-clavier200") {
    @Before
    fun notOnTheBench() = assumeTrue("not while the bench runs seats", System.getProperty("ps.human.dir").isNullOrBlank())

    private fun tap(tag: String) {
        val at = reachTag(tag) ?: centerOf(tag)
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

    private fun node(tag: String): SemanticsNode? =
        compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes(atLeastOneRootRequired = false).firstOrNull()

    /** The Mode simple's Clavier of a new song on [device] at [font] %: the keys and « Enregistrer ma mélodie » need no scroll. */
    private fun simpleClavier(device: String, font: Int) {
        begin(SeatOptions(device = device, font = font, simple = true))
        waitFor("the welcome") { centerOf("welcome_guided") != null || centerOf("launch_new") != null }
        if (centerOf("welcome_guided") != null) tap("welcome_guided") else { tap("launch_new"); tap("starter_wizard") }
        if (centerOf("mode_card_simple") != null) tap("mode_card_simple")
        tap("wizard_family_Techno")
        tap("wizard_style_techno")
        tap("wizard_style_choose_techno")
        tap("wizard_create_now")
        waitFor("the song's tabs") { screenRoute().endsWith("tab_simple_Morceau") }
        tap("tab_simple_Clavier")
        waitFor("the keys") { node("key_62") != null && node("record_start") != null }
        act("attends 1")
        val m = ApplicationProvider.getApplicationContext<android.app.Application>().resources.displayMetrics
        val barTop = node("guide_bar")?.let { BenchWindows.drawnBounds(it, m).top } ?: m.heightPixels.toFloat()
        val cut = listOf("key_62", "key_65", "record_start").mapNotNull { tag ->
            val n = node(tag) ?: return@mapNotNull "$tag missing"
            val b = BenchWindows.drawnBounds(n, m)
            if (shownWhole(n) && b.height >= n.size.height - 1f && b.bottom <= barTop + 1f) null
            else "$tag cut ($b, its size ${n.size}, the Guide's bar at $barTop)"
        }
        assertTrue("$device $font %: ${cut.joinToString("; ")}\n" + screenLines().joinToString("\n"), cut.isEmpty())
    }

    @Test
    fun phone360At200() = simpleClavier("phone360", 200)

    @Test
    fun phone360At130() = simpleClavier("phone360", 130)

    @Test
    fun phone360At100() = simpleClavier("phone360", 100)

    @Test
    fun tablet8At200() = simpleClavier("tablet8", 200)
}
