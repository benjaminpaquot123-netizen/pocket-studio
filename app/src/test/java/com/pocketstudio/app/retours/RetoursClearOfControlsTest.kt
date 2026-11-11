package com.pocketstudio.app.retours

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
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
 * Audit ultra round 1, n° 20 (six seats, nine sightings): the « Mode retours » bubble (REC / Noter / ⋯) sat on
 * ■ Arrêter, ▶ Jouer, the tempo, « ? », Partager, the instruments' tabs… (a touch on ▶ opened the note). The
 * rule, on the main screens of the app as a person opens them (home, the Mode simple's tabs, the Pro's spaces,
 * the Remix), on a phone, a small phone at 200 %, a phone lying down and a tablet lying down: the bubble's
 * bounds meet nothing a finger can touch.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class RetoursClearOfControlsTest : HumanTester("retours-clear") {
    @Before
    fun notOnTheBench() = assumeTrue("not while the bench runs seats", System.getProperty("ps.human.dir").isNullOrBlank())

    private val any = SemanticsMatcher("any node") { true }

    private fun tap(tag: String) {
        val at = centerOf(tag)
        assertNotNull("nothing to touch: $tag\n" + screenLines().joinToString("\n"), at)
        act("touche ${at!!.first} ${at.second}")
    }

    private fun tapText(text: String) {
        val at = centerOfText(text, substring = true)
        assertNotNull("nothing to touch: « $text »\n" + screenLines().joinToString("\n"), at)
        act("touche ${at!!.first} ${at.second}")
    }

    private fun waitFor(what: String, done: () -> Boolean) {
        val until = System.currentTimeMillis() + 60_000
        while (!done()) {
            assertTrue("$what never came\n" + screenLines().joinToString("\n"), System.currentTimeMillis() < until)
            act("attends 0.5")
        }
    }

    private fun inside(n: SemanticsNode, of: SemanticsNode): Boolean {
        var p: SemanticsNode? = n
        while (p != null) { if (p.id == of.id) return true; p = p.parent }
        return false
    }

    /** Nothing to touch under the bubble, in its own window (a menu or a dialog above is another window). */
    private fun clear(where: String) {
        compose.waitForIdle()
        val nodes = compose.onAllNodes(any, useUnmergedTree = true).fetchSemanticsNodes(atLeastOneRootRequired = false)
        val bubble = nodes.firstOrNull { it.config.getOrNull(SemanticsProperties.TestTag) == "retours_bubble" }
        assertNotNull("$where: no « Mode retours » bubble\n" + screenLines().joinToString("\n"), bubble)
        val b: Rect = bubble!!.boundsInWindow
        val window = bubble.layoutInfo.coordinates.findRootCoordinates()
        val under = nodes.filter { n ->
            n.config.contains(SemanticsActions.OnClick) && !inside(n, bubble) && !inside(bubble, n) &&
                n.layoutInfo.coordinates.findRootCoordinates() === window &&
                n.boundsInWindow.width > 0f && n.boundsInWindow.height > 0f &&
                n.boundsInWindow.intersect(b).let { it.width > 1f && it.height > 1f }
        }
        assertTrue(
            "$where: the bubble ($b) covers " + under.joinToString { n ->
                val name = n.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" ")
                    ?: n.config.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text }
                "[${n.config.getOrNull(SemanticsProperties.TestTag) ?: "#" + n.id}] « ${name.orEmpty().take(40)} » ${n.boundsInWindow}"
            },
            under.isEmpty(),
        )
    }

    /** Home, then the Mode simple's song: each of its tabs, and the Remix from « ⋯ ». */
    private fun simpleTour(o: SeatOptions) {
        begin(o.copy(simple = true))
        clear("home")
        if (o.device != "phone") {
            // Lying down, a small phone at 200 %: the welcome's « Partir de zéro » (the assistant's steps are its own screens).
            compose.onNodeWithTag("welcome_empty").performScrollTo()
            tap("welcome_empty")
            if (centerOf("mode_card_simple") != null) tap("mode_card_simple")
        } else {
            tapText("Fais-moi un morceau")
            if (centerOf("mode_card_simple") != null) tap("mode_card_simple")
            tap("wizard_family_Techno")
            tap("wizard_style_techno")
            tap("wizard_style_choose_techno")
            tap("wizard_create_now")
        }
        waitFor("the song's tabs") { centerOf("tab_simple_Morceau") != null }
        tap("tab_simple_Morceau")
        clear("Mon morceau")
        for (t in listOf("Rythme", "Clavier", "Mix", "Envoyer")) {
            if (centerOf("tab_simple_$t") == null) continue
            tap("tab_simple_$t")
            clear("tab $t")
        }
        tap("tab_simple_Morceau")
        tap("top_play")
        clear("Mon morceau, playing")
        tap("top_play")
    }

    /** The Pro's spaces. */
    private fun proTour(o: SeatOptions) {
        begin(o.copy(simple = false))
        // The home's choices once the project behind it is read (« Ouverture du projet… » first on a loaded machine).
        waitFor("the home's choices") { centerOf("launch_new") != null }
        tap("launch_new")
        tap("starter_wizard")
        if (centerOf("mode_card_pro") != null) tap("mode_card_pro")
        tap("wizard_family_Techno")
        tap("wizard_style_techno")
        tap("wizard_style_choose_techno")
        tap("wizard_create_now")
        waitFor("the song's spaces") { screenRoute().endsWith("tab_Structure") }
        clear("Structure")
        for (t in listOf("Sons", "Mix", "Plus", "Structure")) {
            if (centerOf("tab_$t") == null) continue
            tap("tab_$t")
            clear("space $t")
        }
    }

    @Test fun phoneSimple() = simpleTour(SeatOptions())
    @Test fun phonePro() = proTour(SeatOptions())
    @Test fun smallPhoneAt200() = simpleTour(SeatOptions(device = "phone360", font = 200))
    @Test fun phoneLyingDown() = simpleTour(SeatOptions(device = "phoneLand"))
    @Test fun tabletLyingDownPro() = proTour(SeatOptions(device = "tablet11Land", font = 130))

    /**
     * Tour 5, chantier 4 (BenchPlayThenTabsTest): « Mode retours » hidden, its message (4 s at the screen's foot) lay over
     * « ▶ le morceau » and took the next touch (it closed the message, nothing played). Right after it: the message clear of
     * the thumb's bar, and ▶ plays at the first touch.
     */
    @Test
    fun theMessageLeavesTheSongsPlayFree() {
        begin(SeatOptions(simple = true))
        tapText("Fais-moi un morceau")
        if (centerOf("mode_card_simple") != null) tap("mode_card_simple")
        tap("wizard_family_Techno")
        tap("wizard_style_techno")
        tap("wizard_style_choose_techno")
        tap("wizard_create_now")
        waitFor("the song's tabs") { centerOf("tab_simple_Morceau") != null }
        tap("retours_more")
        tap("retours_menu_hide")
        compose.waitForIdle()
        val nodes = compose.onAllNodes(any, useUnmergedTree = true).fetchSemanticsNodes(atLeastOneRootRequired = false)
        fun bounds(tag: String) = nodes.firstOrNull { it.config.getOrNull(SemanticsProperties.TestTag) == tag }?.boundsInWindow
        val message = bounds("retours_message")
        val bar = bounds("thumb_bar")
        assertNotNull("no message after « Masquer »\n" + screenLines().joinToString("\n"), message)
        assertNotNull("no thumb's bar", bar)
        val over = message!!.intersect(bar!!).let { it.width > 1f && it.height > 1f }
        tap("thumb_play")
        val playing = rhythm()!!.transport.value.playing
        assertTrue("the message $message over the thumb's bar $bar: $over; ▶ le morceau at the first touch: $playing", !over && playing)
    }
}
