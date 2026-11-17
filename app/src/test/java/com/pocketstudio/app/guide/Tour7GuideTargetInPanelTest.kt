package com.pocketstudio.app.guide

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.hasTestTag
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.human.BenchWindows
import com.pocketstudio.app.human.HumanTester
import com.pocketstudio.app.human.SeatOptions
import com.pocketstudio.model.i18n.Lang
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
 * Tour 7, D2 (seat 3a, screen 11): « en hi, deux mains, phone360 à 130 %, Mode Simple : depuis Mon morceau, Guide ouvert à
 * « एक साउंड बदलो », je vois « मेलोडी का साउंड » sans fermer le Guide ». The card said « touche « मेलोडी का साउंड » » and the
 * button stood far below the panel's fold, under « ↓ La suite ».
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class Tour7GuideTargetInPanelTest : HumanTester("t7-guide-target") {
    @Before
    fun notOnTheBench() = assumeTrue("not while the bench runs seats", System.getProperty("ps.human.dir").isNullOrBlank())

    private fun tap(tag: String) {
        val at = centerOf(tag)
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

    private fun soundCard(lang: Lang, device: String, font: Int) {
        begin(SeatOptions(lang = lang, device = device, font = font, simple = true))
        waitFor("the home's choices") { centerOf("welcome_guided") != null || centerOf("launch_new") != null }
        if (centerOf("welcome_guided") != null) tap("welcome_guided") else { tap("launch_new"); tap("starter_wizard") }
        if (centerOf("mode_card_simple") != null) tap("mode_card_simple")
        tap("wizard_family_Techno")
        tap("wizard_style_techno")
        tap("wizard_style_choose_techno")
        tap("wizard_create_now")
        waitFor("the Guide") { centerOf("guide_bar") != null }
        if (node("guide_panel") == null) tap("guide_bar")
        waitFor("the ready card") { node("guide_option_drop") != null }
        tap("guide_option_drop")
        waitFor("the sound card") { node("guide_card_start.sound") != null }
        act("attends 1")
        val m = ApplicationProvider.getApplicationContext<android.app.Application>().resources.displayMetrics
        val target = node("guide_open_melody")
        assertNotNull("no « Le son de la mélodie »\n" + screenLines().joinToString("\n"), target)
        val b = BenchWindows.drawnBounds(target!!, m)
        val floor = node("guide_more_below")?.let { BenchWindows.drawnBounds(it, m).top }
            ?: BenchWindows.drawnBounds(node("guide_panel")!!, m).bottom
        assertTrue(
            "$lang $device $font %: « Le son de la mélodie » not in the panel's view ($b, its size ${target.size}, the panel's floor at $floor)\n" +
                screenLines().joinToString("\n"),
            shownWhole(target) && b.height >= target.size.height - 1f && b.bottom <= floor + 1f,
        )
    }

    @Test
    fun hindiSmallPhoneAt130() = soundCard(Lang.HI, "phone360", 130)

    @Test
    fun frenchPhoneAt100() = soundCard(Lang.FR, "phone", 100)
}
