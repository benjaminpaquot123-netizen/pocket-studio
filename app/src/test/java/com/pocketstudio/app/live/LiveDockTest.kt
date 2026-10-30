package com.pocketstudio.app.live

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.AcidPattern
import com.pocketstudio.model.AcidStep
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.LiveState
import com.pocketstudio.model.Project
import com.pocketstudio.model.Remix
import com.pocketstudio.model.Scene
import com.pocketstudio.model.addAudioTrack
import com.pocketstudio.model.addTrack
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Lot 4d: the Live page's tools. Folded, one dock (Briques · Pads · Effets · Notes · Acid 303, ︿);
 * open, a panel with the same tabs and its tools as icons; the six columns fit a phone's width;
 * the tempo is in the bar whatever the tab.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class LiveDockTest {
    @get:Rule
    val compose = createComposeRule()

    private val acid = AcidPattern(steps = List(AcidPattern.MAX_STEPS) { AcidStep(gate = it == 0) })

    /** Batterie, Acid 303, Synthé, Basse, Voix, Remix: the six columns of Benjamin's capture. */
    private val six: Project = Project(
        scenes = listOf(Scene("A", DrumPattern().toggle(0, 0), label = "Intro", acid = acid), Scene("B", DrumPattern().toggle(1, 4))),
        scene = 0, acid = acid, remix = Remix(setId = "x", name = "Chanson"),
    ).addTrack("Basse")!!.addAudioTrack("Voix")!!.sanitized()

    private fun show(p: Project = six, actions: LiveActions = LiveActions(), tab: LivePanelTab = LivePanelTab.Briques) = compose.setContent {
        PocketStudioTheme { LiveScreen(LiveScreenUi(p, LiveState.on(0), clock = 5, playing = true), actions, initialTab = tab) }
    }

    @Test
    fun foldedTheToolsAreOneDockWithItsTabs() {
        show()
        compose.onNodeWithTag("live_dock").assertIsDisplayed()
        compose.onNodeWithTag("live_panel").assertDoesNotExist()
        listOf("Briques", "Pads", "Effets", "Notes", "Acide").forEach { compose.onNodeWithTag("live_tab_$it").assertIsDisplayed() }
        compose.onNodeWithTag("live_tab_Briques").assertIsSelected()
        // The dock sits under the grid, in the screen.
        val dock = compose.onNodeWithTag("live_dock").getBoundsInRoot()
        val grid = compose.onNodeWithTag("live_grid").getBoundsInRoot()
        val screen = compose.onNodeWithTag("live_screen").getBoundsInRoot()
        assertTrue(dock.top >= grid.bottom - 0.5.dp && dock.bottom <= screen.bottom + 0.5.dp)
    }

    @Test
    fun aTabOpensThePanelAndReplierOrBriquesFoldsIt() {
        show()
        compose.onNodeWithTag("live_tab_Effets").performClick()
        compose.onNodeWithTag("live_panel").assertIsDisplayed()
        compose.onNodeWithTag("live_dock").assertDoesNotExist()
        compose.onNodeWithTag("live_tab_Effets").assertIsSelected()
        compose.onNodeWithTag("live_panel_fold").performClick()
        compose.onNodeWithTag("live_dock").assertIsDisplayed()
        // ︿ opens it again where it was.
        compose.onNodeWithTag("live_panel_fold").performClick()
        compose.onNodeWithTag("live_tab_Effets").assertIsSelected()
        compose.onNodeWithTag("live_fx").assertIsDisplayed()
        // « Briques » in the open panel folds it too; the handle as well.
        compose.onNodeWithTag("live_tab_Briques").performClick()
        compose.onNodeWithTag("live_panel").assertDoesNotExist()
        compose.onNodeWithTag("live_tab_Notes").performClick()
        compose.onNodeWithTag("live_notes").assertIsDisplayed()
        compose.onNodeWithTag("live_panel_handle").performClick()
        compose.onNodeWithTag("live_dock").assertIsDisplayed()
    }

    @Test
    fun everyToolIsReachable() {
        val calls = mutableListOf<String>()
        show(actions = LiveActions(resetAll = { calls += "reset" }))
        compose.onNodeWithTag("live_tab_Effets").performClick()
        // The header's tools, as icons.
        listOf("live_reset", "live_lock", "live_fx_settings", "live_fullscreen", "live_panel_fold").forEach { compose.onNodeWithTag(it).assertIsDisplayed() }
        compose.onNodeWithTag("live_reset").performClick()
        assertEquals(listOf("reset"), calls)
        compose.onNodeWithTag("live_lock").performClick()
        compose.onNodeWithTag("live_lock").assertIsSelected()
        compose.onNodeWithTag("live_fx_settings").performClick()
        compose.onNodeWithTag("live_fx_settings_page").assertIsDisplayed()
        // « L'effet »: the pad's modes and the tracks; « Comment il joue »: the five ways.
        compose.onNodeWithTag("live_xy_mode_DJ_FILTER").performClick()
        compose.onNodeWithTag("live_page_PAD").assertIsSelected()
        compose.onNodeWithTag("xy_pad").assertIsDisplayed()
        listOf("HOLDS" to "live_holds", "ROLLS" to "live_rolls").forEach { (page, tag) ->
            compose.onNodeWithTag("live_page_$page").performClick()
            compose.onNodeWithTag(tag).assertIsDisplayed()
        }
        listOf("DUB", "BUILD").forEach { compose.onNodeWithTag("live_page_$it").performClick().assertIsSelected() }
        compose.onNodeWithTag("live_fx_drums").performScrollTo().performClick()
        compose.onNodeWithTag("live_fx_drums").assertIsSelected()
        // The 303 (a plain tab, as the others) and the notes.
        compose.onNodeWithTag("live_tab_Acide").performClick()
        compose.onNodeWithTag("live_acid").assertIsDisplayed()
        compose.onNodeWithTag("live_tab_Notes").performClick()
        compose.onNodeWithTag("live_notes").assertIsDisplayed()
    }

    @Test
    fun withTheToolsOpenTheGridStillShowsSixBricks() {
        show(six.copy(liveRows = 8), tab = LivePanelTab.Effets)
        val grid = compose.onNodeWithTag("live_grid").getBoundsInRoot()
        val sixth = compose.onNodeWithTag("live_row_5").getBoundsInRoot()
        assertTrue("brick F ends at ${sixth.bottom}, the grid at ${grid.bottom}", sixth.bottom <= grid.bottom + 0.5.dp)
        compose.onNodeWithTag("xy_pad").assertIsDisplayed()
    }

    @Test
    fun sixColumnsFitAPhonesWidth() {
        assertEquals(6, liveColumnsOf(six).size)
        show()
        val screen = compose.onNodeWithTag("live_screen").getBoundsInRoot()
        liveColumnsOf(six).forEach { c ->
            // No scrolling: each column's M and ■ are within the width as the page opens.
            listOf("live_mute_${c.key}", "live_stop_${c.key}").forEach { tag ->
                val b = compose.onNodeWithTag(tag).getBoundsInRoot()
                assertTrue("$tag ends at ${b.right}", b.right <= screen.right + 0.5.dp)
            }
        }
        compose.onNodeWithTag("live_columns_more").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w360dp-h740dp-xxhdpi")
    fun sixColumnsFitASmallPhoneToo() {
        show()
        val screen = compose.onNodeWithTag("live_screen").getBoundsInRoot()
        liveColumnsOf(six).forEach { c ->
            val b = compose.onNodeWithTag("live_mute_${c.key}").getBoundsInRoot()
            assertTrue("${c.key} ends at ${b.right}", b.right <= screen.right + 0.5.dp)
        }
    }

    @Test
    @Config(qualifiers = "w316dp-h640dp-xxhdpi")
    fun tooManyColumnsSayTheyScroll() {
        var many = six
        repeat(2) { many = many.addTrack("Piste ${it + 2}")!! }
        show(many)
        val last = liveColumnsOf(many).last().key
        compose.onNodeWithTag("live_columns_more").assertIsDisplayed().performClick()
        compose.waitForIdle()
        val screen = compose.onNodeWithTag("live_screen").getBoundsInRoot()
        val b = compose.onNodeWithTag("live_mute_$last").getBoundsInRoot()
        assertTrue("$last ends at ${b.right}", b.right <= screen.right + 0.5.dp)
        compose.onNodeWithTag("live_columns_more").assertDoesNotExist()
    }

    @Test
    fun theTempoIsInTheBarWhateverTheTab() {
        val nudges = mutableListOf<Int>()
        var sheet = 0
        show(actions = LiveActions(nudgeTempo = { nudges += it }, openTempo = { sheet++ }))
        val bar = compose.onNodeWithTag("live_top_bar").getBoundsInRoot()
        for (t in listOf("Briques", "Effets", "Notes", "Acide")) {
            compose.onNodeWithTag("live_tab_$t").performClick()
            val tempo = compose.onNodeWithTag("live_tempo").assertIsDisplayed().getBoundsInRoot()
            assertTrue("$t: the tempo in the bar", tempo.top >= bar.top - 0.5.dp && tempo.bottom <= bar.bottom + 0.5.dp)
            compose.onNodeWithTag("live_tempo_minus").assertIsDisplayed()
            compose.onNodeWithTag("live_tempo_plus").assertIsDisplayed()
        }
        compose.onNodeWithTag("live_tempo_minus").performClick()
        compose.onNodeWithTag("live_tempo_plus").performClick()
        compose.onNodeWithTag("live_tempo").performClick()
        assertEquals(listOf(-1, 1), nudges)
        assertEquals(1, sheet)
        // Full screen too (the app's strip gives way there, not the bar's tempo).
        compose.onNodeWithTag("live_fullscreen").performClick()
        compose.onNodeWithTag("live_tempo").assertIsDisplayed()
    }
}
