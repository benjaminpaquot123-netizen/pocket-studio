package com.pocketstudio.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pocketstudio.app.nav.AppHarness
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 9, D10: « Depuis le menu « A ▾ » je peux renommer la partie, comme le tuto le demande. » The whole app (AppRoot's
 * own TopBarActions), not the bar alone with a lambda given by the test: the entry shows only when AppRoot wires it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class Tour9RenameWiredTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val h = AppHarness(compose)

    @After
    fun end() = h.finish()

    @Test
    fun theAppsSceneMenuOffersRename() {
        h.start(simple = false)
        // The chip shows on the parts' tabs (« Sons », Rythme first, as in the tutorial).
        compose.onNodeWithTag("tab_Sons").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("top_scene").performClick()
        compose.waitForIdle()
        assertTrue("« Nouvelle partie vide » shows: the menu is open", compose.onAllNodesWithTag("scene_new").fetchSemanticsNodes().isNotEmpty())
        assertTrue(
            "« Renommer » in the app's « A ▾ » menu (AppRoot passes renameScene)",
            compose.onAllNodesWithTag("scene_rename").fetchSemanticsNodes().isNotEmpty(),
        )
    }
}
