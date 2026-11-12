package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pocketstudio.app.ui.withinGestures
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Benjamin (2026-10-29, « ce n'est pas la première fois »): the 303 could not be found in the Mode
 * simple. Its chip « Basse acide · 303 » was the full mode's alone and « + Piste » did not offer
 * it. Now « Acid 303 », a yellow chip in both modes (the Mode simple's Clavier, Sons), in
 * « + Piste » in both modes, and its editor in the Mode simple's words.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class AcidFindableTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)

    @After
    fun end() = h.finish()

    private fun assertEditor() {
        compose.onNodeWithTag("acid").assertExists()
        compose.onNodeWithTag("acid_title").assertTextContains("Acid 303", substring = true)
    }

    @Test
    fun theModeSimpleShowsItsChipOnTheClavier() {
        h.start(simple = true)
        compose.withinGestures("Ouvrir l'Acid 303 (Mode simple)", 2) {
            tap("tab_simple_Clavier")
            compose.onNodeWithTag("sons_Acide").assertIsDisplayed().assertTextContains("Acid 303", substring = true)
            tap("sons_Acide")
        }
        assertEditor()
        // Its chip lit, and the bottom tab it belongs to.
        compose.onNodeWithTag("sons_Acide").assertIsSelected()
        compose.onNodeWithTag("tab_simple_Clavier").assertIsSelected()
        // The Mode simple's words.
        compose.onNodeWithText("Écris-moi une ligne").assertExists()
        compose.onNodeWithText("Génère-moi un pattern").assertDoesNotExist()
        compose.onNodeWithText("Nombre de notes").assertExists()
        compose.onNodeWithText("Plus fort").performScrollTo().assertExists()
        compose.onNodeWithText("Glisse").assertExists()
        compose.onNodeWithText("Slide").assertDoesNotExist()
        compose.onNodeWithTag("acid_record").assertDoesNotExist()
    }

    @Test
    fun theFullModeShowsItsChipInSons() {
        h.start()
        compose.withinGestures("Ouvrir l'Acid 303", 2) {
            tap("tab_Sons")
            compose.onNodeWithTag("sons_Acide").assertIsDisplayed().assertTextContains("Acid 303", substring = true)
            tap("sons_Acide")
        }
        assertEditor()
        compose.onNodeWithText("Génère-moi un pattern").assertExists()
        compose.onNodeWithText("Basse acide").assertDoesNotExist()
    }

    private fun addTrackOpensTheAcid() {
        compose.onNodeWithTag("track_add").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("track_add_acid").assertTextContains("Acid 303", substring = true).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("track_add_sheet").assertDoesNotExist()
        assertEditor()
    }

    @Test
    fun addTrackOffersItInTheModeSimple() {
        h.start(simple = true)
        compose.onNodeWithTag("tab_simple_Clavier").performClick()
        compose.waitForIdle()
        addTrackOpensTheAcid()
        compose.onNodeWithTag("tab_simple_Clavier").assertIsSelected()
    }

    @Test
    fun addTrackOffersItInTheFullMode() {
        h.start()
        compose.onNodeWithTag("tab_Sons").performClick()
        compose.waitForIdle()
        addTrackOpensTheAcid()
    }
}
