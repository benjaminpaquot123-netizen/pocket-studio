package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Audit ultra round 6, G1 (five seats): « la barre ou la carte Guide cache le contenu ». Three things, each read here
 * by comparing rectangles, as a finger meets them:
 * - seat A a #16: the Guide open, the thumb's « ⋮ Plus » touched: the Plus page came up under the open panel, its
 *   second row of tiles cut, the others behind it. The panel now folds when the user goes to another screen himself
 *   (the Guide's own « Voir le réglage » keeps its rules);
 * - seat F a #23: the open panel cut its own buttons at its lower edge, with no sign that more waited below. Where
 *   its card is cut, « ↓ La suite » shows and brings the rest up;
 * - seat F b #10 and the 130 % seats: on the keys' page of a short screen the Guide's bar took the keys' last room
 *   (16 dp of keys in view on a 360 x 780 phone at 130 %). There the bar gives its line to the keys, as the thumb's
 *   bar already does.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-xhdpi")
class Tour6GuideCoversTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val h = AppHarness(compose)
    private val shell = Tour6Shell(compose)

    @After
    fun back() = h.finish()

    private fun start(simple: Boolean, scale: Float = 1f) {
        h.start(simple = simple, scale = scale)
        h.awaitGuide()
        assertTrue("the Guide has read the song", shell.reached { !h.vm.guide.ui.value.loading })
        shell.idle(300)
    }

    private fun openGuide() {
        shell.tap("guide_bar")
        assertTrue("the Guide's panel opens", shell.reached(3_000) { shell.has("guide_panel") })
        shell.idle(600)
    }

    /** The Guide's rectangle on screen: its open panel, else its bar. */
    private fun guide(): Rect? = shell.bounds("guide_panel") ?: shell.bounds("guide_bar")

    /** What a touch can reach on the page that the Guide's open panel cuts or hides (laid out where the panel is). */
    private fun underThePanel(): List<String> {
        val panel = shell.bounds("guide_panel") ?: return emptyList()
        val shellTags = setOf("guide_panel", "guide_bar", "thumb_bar", "done_bar")
        return shell.touchables().filter { !Tour6Shell.within(it, shellTags) }
            .filter { Tour6Shell.whole(it).overlaps(panel) }
            .map { Tour6Shell.name(it) + " laid out at " + Tour6Shell.whole(it) + ", the panel from " + panel.top }
    }

    /** Seat A a #16: the Guide open on « Mon morceau », then the thumb's « ⋮ Plus ». */
    @Test
    fun thePlusPageOpenedFromTheThumbIsNotUnderTheOpenGuide() {
        start(simple = true)
        openGuide()
        shell.tap("thumb_more")
        assertTrue("the Plus page shows", shell.reached(3_000) { shell.has("plus_projects") })
        shell.idle(600)
        val under = underThePanel()
        assertTrue("the Guide's open panel covers the Plus page the user just asked for:\n" + under.joinToString("\n"), under.isEmpty())
        // Its bar is still there, one touch from the card.
        assertTrue("the Guide's bar stays", shell.has("guide_bar"))
        // The first tiles whole, as without the Guide.
        listOf("plus_home", "plus_projects", "plus_live", "plus_remix").forEach { tag ->
            val node = shell.touchables().first { Tour6Shell.tagOf(it) == tag }
            assertTrue("$tag is cut: ${node.boundsInRoot} of ${Tour6Shell.whole(node)}", node.boundsInRoot.height >= Tour6Shell.whole(node).height - 1f)
        }
    }

    /**
     * Review of the lot (grave): a choice of the card that changes no screen (« Écouter » of « Ton morceau est prêt »), then
     * the thumb's « ⋮ Plus »: the panel still folds (the Guide's own way had made the next screen its home).
     */
    @Test
    fun afterAChoiceThatChangesNoScreenThePanelStillFoldsOnLeaving() {
        val style = com.pocketstudio.model.StarterWizard.style("house")!!
        val song = com.pocketstudio.model.StarterWizard.build(
            style, style.lines.associate { it.role to null as com.pocketstudio.model.PadSample? }, style.melody.mood,
            com.pocketstudio.model.StarterWizard.defaultParts(style).associateWith { com.pocketstudio.model.StarterWizard.soundsFor(it, style).first() },
        )
        val made = com.pocketstudio.model.guide.GuideStart.begin(song.copy(name = "House du soir"), com.pocketstudio.model.guide.GuideStart.SONG,
            listOf(com.pocketstudio.model.guide.GuidePageText("Ton morceau House est prêt", listOf("Intro, Groove, Drop."))))
        h.start(simple = true) { session.edit { made } }
        assertTrue("the Guide opens on « Ton morceau est prêt »", shell.reached(30_000) { shell.has("guide_card_start.ready") })
        // « Voir le guide du morceau »: a window over the page, no other screen.
        shell.tap("guide_open_guide")
        assertTrue("the song's guide shows", shell.reached(5_000) { shell.has("song_guide") })
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        shell.idle(600)
        assertTrue("the Guide's panel is still open on the same screen", shell.reached(5_000) { shell.has("guide_panel") })
        shell.tap("thumb_more")
        assertTrue("the Plus page shows", shell.reached(3_000) { shell.has("plus_projects") })
        shell.idle(600)
        val under = underThePanel()
        assertTrue("after a choice of the card, the Guide's open panel covers the Plus page the user just asked for:\n" + under.joinToString("\n"), under.isEmpty())
    }

    /** The same from the top bar's « ⋮ », with big text. */
    @Test
    fun thePlusPageOpenedFromTheTopBarIsNotUnderTheOpenGuideWithBigText() {
        start(simple = true, scale = 1.3f)
        openGuide()
        shell.tap("top_more")
        assertTrue("the Plus page shows", shell.reached(3_000) { shell.has("plus_projects") })
        shell.idle(600)
        val under = underThePanel()
        assertTrue("the Guide's open panel covers the Plus page the user just asked for:\n" + under.joinToString("\n"), under.isEmpty())
    }

    /** A page of Plus opened under the open Guide (Pro: Plus › Réglages). */
    @Test
    fun aPageOfPlusIsNotUnderTheOpenGuideInPro() {
        start(simple = false)
        shell.tap("tab_Plus")
        openGuide()
        shell.tap("tool_Reglages")
        shell.idle(600)
        assertFalse("the Guide's panel stays open over the page the user just asked for (Réglages)", shell.has("guide_panel"))
        assertTrue("the Guide's bar stays", shell.has("guide_bar"))
    }

    /** The Guide's own « Voir le réglage » is not the user leaving: the screen it opens keeps the Guide at hand. */
    @Test
    fun theGuidesOwnWayToAScreenKeepsItsBar() {
        start(simple = true)
        openGuide()
        val card = h.vm.guide.ui.value.next!!
        val open = card.options.firstOrNull { it.opens != null }
        org.junit.Assume.assumeTrue("the step opens a screen", open != null)
        shell.tap("guide_open_${open!!.id}")
        shell.idle(600)
        assertTrue("the Guide is still on screen after its own « Voir le réglage »", shell.has("guide_bar") || shell.has("guide_panel") || shell.has("guide_spot"))
    }

    /** Seat F a #23: the open panel's card cut at its lower edge says so, and a touch brings the rest. */
    private fun theCutCardSaysMoreWaits(simple: Boolean, scale: Float) {
        start(simple, scale)
        openGuide()
        val panel = shell.bounds("guide_panel")!!
        val shellTags = setOf("guide_panel")
        val cut = shell.touchables().filter { Tour6Shell.within(it, shellTags) && Tour6Shell.tagOf(it) != "guide_bar" && Tour6Shell.tagOf(it) != "guide_more_below" }
            .filter { Tour6Shell.whole(it).bottom > panel.bottom + 1f }
        org.junit.Assume.assumeTrue("the card is taller than the panel here", cut.isNotEmpty())
        assertTrue(
            "the Guide's panel cuts its own buttons (${cut.joinToString { Tour6Shell.name(it) }}) and nothing says more waits below",
            shell.has("guide_more_below"),
        )
        // The sign is whole and inside the panel.
        val sign = shell.bounds("guide_more_below")!!
        assertTrue("« La suite » at $sign is not inside the panel $panel", sign.bottom <= panel.bottom + 1f && sign.top >= panel.top)
        // A touch on it brings the rest up, to the end; then it goes.
        var touches = 0
        while (shell.has("guide_more_below") && touches < 40) {
            shell.tap("guide_more_below")
            shell.idle(600)
            touches++
        }
        assertFalse("« La suite » still shows after $touches touches", shell.has("guide_more_below"))
        val last = shell.touchables().filter { Tour6Shell.within(it, shellTags) }.maxByOrNull { Tour6Shell.whole(it).bottom }!!
        assertTrue("the card's last button ${Tour6Shell.name(last)} is still cut", Tour6Shell.whole(last).bottom <= panel.bottom + 1f)
    }

    @Test fun theOpenGuidesCutCardSaysMoreWaitsInSimple() = theCutCardSaysMoreWaits(simple = true, scale = 1f)

    @Test fun theOpenGuidesCutCardSaysMoreWaitsInPro() = theCutCardSaysMoreWaits(simple = false, scale = 1f)

    @Test fun theOpenGuidesCutCardSaysMoreWaitsWithBigText() = theCutCardSaysMoreWaits(simple = true, scale = 1.3f)

    /** Seat F b #10 and the 130 % seats: on a short screen the Guide's bar does not sit on the keys. */
    @Test
    fun theGuidesBarLeavesTheKeysTheirRoomOnAShortScreen() {
        start(simple = true, scale = 1.3f)
        shell.tap("tab_simple_Clavier")
        shell.idle(600)
        val keys = shell.nodes().firstOrNull { Tour6Shell.tagOf(it) == "keyboard" }
        assertNotNull("the keys are on the Clavier page", keys)
        val bar = guide()
        val dp = compose.density.density * 1.3f
        val said = "keys laid out at ${Tour6Shell.whole(keys!!)}, in view ${keys.boundsInRoot} (${keys.boundsInRoot.height / dp} dp high), the Guide's bar at $bar"
        assertTrue("the Guide's bar sits on the keys: $said", bar == null || Tour6Shell.whole(keys).bottom <= bar.top + 1f)
        assertTrue("less than a finger of keys in view: $said", keys.boundsInRoot.height / dp >= 48f)
        // Elsewhere the bar is back.
        shell.tap("tab_simple_Morceau")
        shell.idle(300)
        assertTrue("the Guide's bar is back on « Mon morceau »", shell.has("guide_bar"))
    }

    /** On a tall enough screen the bar stays on the keys' page, the keys whole above it. */
    @Test
    @Config(qualifiers = "w411dp-h914dp-xhdpi")
    fun onATallScreenTheGuidesBarStaysOnTheKeysPage() {
        start(simple = true)
        shell.tap("tab_simple_Clavier")
        shell.idle(600)
        val keys = shell.nodes().first { Tour6Shell.tagOf(it) == "keyboard" }
        val bar = shell.bounds("guide_bar")
        assertNotNull("the Guide's bar on the keys' page of a tall screen", bar)
        assertTrue("the keys ${Tour6Shell.whole(keys)} are cut by the Guide's bar $bar", Tour6Shell.whole(keys).bottom <= bar!!.top + 1f)
    }
}
