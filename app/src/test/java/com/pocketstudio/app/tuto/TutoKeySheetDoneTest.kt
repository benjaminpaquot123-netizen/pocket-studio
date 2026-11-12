package com.pocketstudio.app.tuto

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pocketstudio.model.Harmony
import com.pocketstudio.model.Scale
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Audit ultra round 2, n° 24 (seat A a, screen 22): « Phrygienne » then « Fa » touched in the key sheet, it
 * stayed as it was; « Fermer » had to be guessed. The key asked for reached is said, and « Revenir au tuto » closes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class TutoKeySheetDoneTest {
    @get:Rule
    val compose = createComposeRule()

    private fun has(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun theKeyAskedForReachedIsSaid() {
        val wanted = Harmony(root = 5, scale = Scale.Phrygian, lock = true)
        var harmony by mutableStateOf(Harmony())
        var closed = false
        compose.setContent { PocketStudioTheme { TutoKeySheetContent(harmony, wanted, { harmony = it }, { closed = true }) } }
        assertFalse(has("tuto_key_done"))
        compose.onNodeWithTag("tuto_scale_${Scale.Phrygian.name}").performClick()
        assertFalse("said done with the scale alone", has("tuto_key_done"))
        compose.onNodeWithTag("tuto_root_5").performClick()
        compose.waitForIdle()
        assertTrue("« Réussi » in the key sheet", has("tuto_key_done"))
        compose.onNodeWithTag("tuto_key_continue").performClick()
        assertTrue(closed)
    }
}
