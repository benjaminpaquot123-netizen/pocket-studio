package com.pocketstudio.app.library

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Audit ultra round 2, zone 1, n° 6 and 7. A pad's « Changer le son » (a pad held): its title says so and the sound on
 * the pad now is marked in the list (owner: « toujours le même », the sound now not marked). Seat A b: a swipe along
 * the families' row selected the family under the finger.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class LibraryRound2Zone1Test {
    @get:Rule
    val compose = createComposeRule()

    private fun show(ui: LibraryUi) = compose.setContent { PocketStudioTheme { LibraryScreen(ui, LibraryActions()) } }

    @Test
    fun aPadsChangeTheSoundSaysItAndMarksTheSoundOnThePad() {
        show(LibraryUi(PacksUi(), target = "le pad 1 (Kick)", startKind = "kick", title = "Changer le son", current = "Kick"))
        compose.waitForIdle()
        val title = compose.onNodeWithTag("library_title").fetchSemanticsNode().config[SemanticsProperties.Text].joinToString { it.text }
        assertEquals("Changer le son", title)
        // The built-in sounds' folder, opened: « Kick » there, marked.
        compose.onNodeWithTag("library_folder_Sons intégrés", useUnmergedTree = true).performTouchInput { down(center); up() }
        compose.waitForIdle()
        assertTrue("the pad's sound marked in its list", compose.onAllNodesWithText("✓ Sur le pad maintenant").fetchSemanticsNodes().size == 1)
    }

    @Test
    fun aSwipeAlongTheFamiliesScrollsWithoutChoosingOne() {
        show(LibraryUi(PacksUi(), target = "le pad 1 (Kick)", startKind = "kick"))
        compose.waitForIdle()
        compose.onNodeWithTag("library_kind_kick").assertIsSelected()
        compose.onNodeWithTag("library_kind_snare").performTouchInput { swipeLeft(startX = right - 2f, endX = left - 400f) }
        compose.waitForIdle()
        compose.onNodeWithTag("library_kind_kick").assertIsSelected()
    }
}
