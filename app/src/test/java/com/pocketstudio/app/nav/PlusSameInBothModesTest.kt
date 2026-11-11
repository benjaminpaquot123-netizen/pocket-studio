package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pocketstudio.app.settings.ProjectModes
import com.pocketstudio.app.settings.SettingsStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 5, chantier 5 (hands-on audit G15, PLAN-ACTION-AUDITS step 4 line 5): the same « Plus » in both modes, the
 * Recette included (Benjamin, 2026-11-08: « en mode Simple, pas de chemin vers la Recette »). And answer 3A: « Simple
 * ou Pro ? » is not asked again for a second song; it takes the last choice, with « Changer » in the assistant.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class PlusSameInBothModesTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)

    @After
    fun end() = h.finish()

    private fun exists(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun tap(tag: String) {
        val node = compose.onNodeWithTag(tag)
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
        compose.waitForIdle()
    }

    /** The Mode simple's ⋮ opens the Plus page (cards, not a menu of bare lines), with the Recette; Retour goes back. */
    @Test
    fun theSimpleMoreOpensThePlusPageWithTheRecette() {
        h.start(simple = true)
        tap("top_more")
        assertTrue("⋮ opens the Plus page in the Mode simple", exists("plus"))
        assertFalse("no menu of bare lines any more", exists("top_more_menu"))
        // What the old menu held, as cards (« Mon premier morceau » and « Les mots » are in Aide).
        for (tag in listOf("plus_home", "plus_projects", "plus_live", "plus_remix", "plus_voice", "plus_library", "plus_mine",
            "tool_Aide", "tool_Reglages", "plus_mode", "plus_bug", "tool_Recette")) {
            assertTrue("« $tag » on the Mode simple's Plus", exists(tag))
        }
        tap("tool_Recette")
        compose.onNodeWithTag("recette").assertExists()
        // « ← Plus · Recette »: back on the list, then Retour back on the song.
        tap("plus_back")
        assertTrue(exists("tool_Recette"))
        androidx.test.espresso.Espresso.pressBack()
        compose.waitForIdle()
        assertFalse(exists("plus"))
        compose.onNodeWithTag("tab_simple_Morceau").assertIsDisplayed()
        assertTrue(SettingsStore.state.value.simpleMode)
    }

    /** The thumb's « Plus » is the same page. */
    @Test
    fun theThumbsMoreOpensTheSamePlusPage() {
        h.start(simple = true)
        tap("thumb_more")
        assertTrue("the thumb's Plus opens the Plus page", exists("plus"))
        assertTrue(exists("tool_Recette"))
        assertFalse(exists("thumb_more_menu"))
    }

    /** 3A: a choice already made, a new song starts in it at once, and the assistant says it with « Changer ». */
    @Test
    fun aSecondSongTakesTheLastModeAndTheAssistantCanChangeIt() {
        h.start(simple = false)
        ProjectModes.choose(h.app, false)
        tap("top_projects")
        h.awaitTag("project_new")
        tap("project_new")
        tap("starter_wizard")
        val until = System.currentTimeMillis() + 10_000
        while (!exists("wizard") && !exists("mode_step") && System.currentTimeMillis() < until) { Thread.sleep(50); compose.waitForIdle() }
        assertFalse("« Simple ou Pro ? » not asked again", exists("mode_step"))
        assertFalse(SettingsStore.state.value.simpleMode)
        compose.onNodeWithTag("wizard_mode", useUnmergedTree = true).assertExists()
        tap("wizard_mode_change")
        tap("mode_card_simple")
        assertTrue("« Changer » in the assistant sets the mode of the song to come", SettingsStore.state.value.simpleMode)
        assertEquals(true, ProjectModes.last(h.app))
        assertTrue("still in the assistant", exists("wizard"))
    }

    /** 3A on the app's own starts too (Plus › « Mon premier morceau »): the last choice, no step. */
    @Test
    fun aGuideStartTakesTheLastModeWithoutAsking() {
        h.start(simple = false)
        ProjectModes.choose(h.app, false)
        tap("tab_Plus")
        tap("plus_missions")
        assertFalse("« Simple ou Pro ? » not asked again", exists("mode_step"))
        assertFalse(SettingsStore.state.value.simpleMode)
    }
}
