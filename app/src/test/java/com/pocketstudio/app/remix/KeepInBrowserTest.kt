package com.pocketstudio.app.remix

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pocketstudio.app.library.MINE
import com.pocketstudio.app.library.ShelfBrowser
import com.pocketstudio.app.library.ShelfBrowserActions
import com.pocketstudio.app.library.ShelfBrowserUi
import com.pocketstudio.app.library.SoundOption
import com.pocketstudio.app.library.SoundRef
import com.pocketstudio.app.library.mineFolders
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Lot C, rest of 7d: what « Garder » keeps from the Remix is found in the one sound browser, in
 * « Ma bibliothèque », filed under its song (« Ma bibliothèque › Boys »); a library of one song
 * only stays one list. The same browser everywhere a sound is chosen ([ShelfBrowser]).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class KeepInBrowserTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val origins = mapOf("a" to "Boys", "b" to "Boys", "c" to "Mon morceau", "d" to "")
    private fun mine(vararg ids: String) = ids.map { SoundOption(SoundRef.Mine(it), "Voix $it") }
    private val groupOf: (SoundOption) -> String? = { o -> origins[(o.ref as SoundRef.Mine).id] }

    @Test
    fun keptSoundsAreFiledBySongOnlyWhenThereIsAChoice() {
        val filed = mineFolders(mine("a", "b", "c", "d"), groupOf)!!
        assertEquals(listOf("Boys", "Mon morceau"), filed.first.map { it.first })
        assertEquals(listOf("a", "b"), filed.first[0].second.map { (it.ref as SoundRef.Mine).id })
        assertEquals(listOf("d"), filed.second.map { (it.ref as SoundRef.Mine).id })
        // One song only: no folder to open, the list as it is.
        assertNull(mineFolders(mine("a", "b"), groupOf))
        assertNull(mineFolders(mine("d"), groupOf))
    }

    @Test
    fun theBrowserOpensMaBibliothequeThenTheSongsFolder() {
        val shown = ArrayList<List<String>>()
        compose.setContent {
            PocketStudioTheme {
                ShelfBrowser(
                    ShelfBrowserUi(
                        emptyList(), "vocal", tag = "t", start = MINE,
                        mine = mine("a", "b", "c", "d"),
                        mineGroupOf = groupOf,
                        mineContentOf = { some ->
                            shown += some.map { (it.ref as SoundRef.Mine).id }
                            some.forEach { Text("son ${(it.ref as SoundRef.Mine).id}") }
                        },
                    ),
                    ShelfBrowserActions(),
                )
            }
        }
        compose.onNodeWithTag("t_mine_folder_Boys").assertExistsAndClick()
        compose.waitForIdle()
        compose.onNodeWithText("son a").assertExists()
        compose.onNodeWithText("son b").assertExists()
        compose.onNodeWithText("Ma bibliothèque › Boys").assertExists()
        // Back: Ma bibliothèque again, with the sound kept from nowhere in particular.
        compose.onNodeWithTag("t_back").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("t_mine_folder_Mon morceau").assertExists()
        compose.onNodeWithText("son d").assertExists()
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertExistsAndClick() {
        assertExists()
        performClick()
    }
}
