package com.pocketstudio.app.library

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Audit ultra round 1, n° 3 (two testers): a pad's library showed only the app's sounds, never the
 * song they had just separated. Its songs are there, each one touch from its cut screen.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w392dp-h852dp")
class LibrarySongsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theSeparatedSongsAreInTheLibraryOneTouchFromTheirCut() {
        val asked = mutableListOf<String>()
        compose.setContent {
            PocketStudioTheme {
                LibraryScreen(
                    LibraryUi(PacksUi(), target = "le pad live A1", startMine = true, songs = listOf("s1" to "Ma chanson préférée", "s2" to "Boys")),
                    LibraryActions(cutSong = { asked += it }),
                )
            }
        }
        compose.onNodeWithTag("library_songs").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Tes chansons séparées", ignoreCase = true).assertIsDisplayed()
        compose.onNodeWithText("Découper « Ma chanson préférée »", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("library_cut_s2").performScrollTo().performClick()
        assertEquals(listOf("s2"), asked)
    }

    @Test
    fun noSongNoCard() {
        compose.setContent { PocketStudioTheme { LibraryScreen(LibraryUi(PacksUi(), target = "le pad 1 (Kick)"), LibraryActions()) } }
        compose.onNodeWithTag("library_songs").assertDoesNotExist()
    }
}
