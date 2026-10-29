package com.pocketstudio.app.library

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.pocketstudio.model.MySound
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * « Ma bibliothèque » in the sound library: the user's sounds in their own family, before the
 * app's, as the same tiles with an « À moi » badge, their tempo and key said; renamed, filed
 * elsewhere, deleted (asked first).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class MySoundsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val voice = MySound("a1", "Voix · Ma chanson", "vocal", "a1.wav", seconds = 2.4, bpm = 98.0, root = 9, minor = true)
    private val loop = MySound("b2", "Boucle du refrain", "loop", "b2.wav", seconds = 4.9, bpm = 98.0)

    private fun exists(tag: String) = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun theUsersSoundsComeFirstInTheirFamilyWithABadge() {
        val used = mutableListOf<SoundOption>()
        compose.setContent {
            PocketStudioTheme {
                LibraryScreen(LibraryUi(PacksUi(), target = "le pad 1 (Kick)", startKind = "vocal", mine = listOf(voice, loop)), LibraryActions(use = { used += it }))
            }
        }
        // The families that hold the user's sounds first; « Boucles » shown since one is a loop.
        assertEquals(listOf("vocal", "loop"), libraryKinds(listOf(voice, loop)).take(2))
        assertTrue(exists("library_kind_loop"))
        compose.onNodeWithTag("library_source_mine").assertExists()
        compose.onNodeWithTag("library_mine_0").assertTextContains("Voix · Ma chanson", substring = true)
            .assertTextContains("À moi", substring = true).assertTextContains("98 BPM · La mineur", substring = true)
        compose.onNodeWithTag("library_mine_use_0").performClick()
        assertEquals(SoundOption(SoundRef.Mine("a1"), "Voix · Ma chanson"), used.single())
        // Another family: its own sounds only.
        compose.onNodeWithTag("library_kind_loop").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("library_mine_0").assertTextContains("Boucle du refrain", substring = true)
    }

    @Test
    fun renamedFiledElsewhereOrDeleted() {
        val renamed = mutableListOf<Pair<String, String>>()
        val kinds = mutableListOf<Pair<String, String>>()
        val deleted = mutableListOf<String>()
        compose.setContent {
            PocketStudioTheme {
                LibraryScreen(
                    LibraryUi(PacksUi(), target = "le pad 1 (Kick)", startKind = "vocal", mine = listOf(voice)),
                    LibraryActions(
                        renameMine = { id, n -> renamed += id to n },
                        setMineKind = { id, k -> kinds += id to k },
                        deleteMine = { deleted += it },
                    ),
                )
            }
        }
        compose.onNodeWithTag("library_mine_edit_0").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("library_mine_name").performTextReplacement("Refrain chanté")
        compose.onNodeWithTag("library_mine_kind_loop").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("library_mine_save").performClick()
        compose.waitForIdle()
        assertEquals(listOf("a1" to "Refrain chanté"), renamed)
        assertEquals(listOf("a1" to "loop"), kinds)
        compose.onNodeWithTag("library_mine_edit_0").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("library_mine_delete").performClick()
        compose.waitForIdle()
        assertTrue(deleted.isEmpty())
        compose.onNodeWithTag("library_mine_delete_confirm").performClick()
        compose.waitForIdle()
        assertEquals(listOf("a1"), deleted)
    }
}
