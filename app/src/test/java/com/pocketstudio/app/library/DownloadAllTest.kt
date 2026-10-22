package com.pocketstudio.app.library

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertTextContains
import com.pocketstudio.model.PackEntry
import com.pocketstudio.model.PackIndex
import com.pocketstudio.model.PackManifest
import com.pocketstudio.model.PackSound
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** « Tout télécharger », « Tout compléter » with its badge, updates and the search, in the library. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h2400dp-xxhdpi")
class DownloadAllTest {
    @get:Rule
    val compose = createComposeRule()

    private fun entry(id: String, size: Long, version: Int = 1) = PackEntry(id, "Pack $id", "$id.zip", "a".repeat(64), size, 10, version)

    private val index = PackIndex(listOf(entry("house", 2_000_000, 2), entry("bruitages", 8_000_000), entry("jazz", 1_000_000)))
    private val snaps = PackManifest(
        "bruitages", "Bruitages", version = 1,
        sounds = listOf(PackSound("clap_doigts_01.flac", "clap", "Claquement de doigts 1", listOf("doigts", "snap"))),
    )
    private val house = PackManifest("house", "House", version = 1, sounds = listOf(PackSound("kick_01.flac", "kick", "Kick 1")))

    private fun show(packs: PacksUi, metered: Boolean = false, actions: LibraryActions) = compose.setContent {
        PocketStudioTheme { LibraryScreen(LibraryUi(packs, target = "le pad 1 (Kick)", metered = metered), actions) }
    }

    @Test
    fun completeShowsTheBadgeThenAsksBeforeDownloading() {
        val asked = mutableListOf<Boolean>()
        // House is installed in version 1 (2 published); « bruitages » is new, « jazz » was offered before.
        show(PacksUi(IndexState.Ready(index), listOf(house), seen = setOf("house", "jazz")), metered = true, LibraryActions(downloadAll = { asked += it }))
        compose.onNodeWithTag("library_news_badge").assertTextContains("2")
        compose.onNodeWithTag("library_complete_all").performClick()
        // On mobile data the size comes with a warning; nothing starts before the confirmation.
        compose.onNodeWithTag("library_all_metered").assertExists()
        compose.onNodeWithText("2 packs, 10,0 Mo", substring = true).assertExists()
        assertEquals(emptyList<Boolean>(), asked)
        compose.onNodeWithTag("library_all_confirm").performClick()
        assertEquals(listOf(true), asked)
    }

    @Test
    fun downloadAllTakesEveryMissingOrOlderPack() {
        val asked = mutableListOf<Boolean>()
        val updates = mutableListOf<String>()
        show(PacksUi(IndexState.Ready(index), listOf(house), seen = setOf("house", "jazz", "bruitages")), actions = LibraryActions(downloadAll = { asked += it }, download = { updates += it }))
        // No new pack since the last look: the badge counts the update only; the whole library is one touch away.
        compose.onNodeWithTag("library_news_badge").assertTextContains("1")
        compose.onNodeWithTag("library_download_all").performScrollTo().assertTextContains("3 packs", substring = true).performClick()
        compose.onNodeWithTag("library_all_metered").assertDoesNotExist()
        compose.onNodeWithTag("library_all_confirm").performClick()
        assertEquals(listOf(false), asked)
        // The installed pack with a richer version is updated by its own button too.
        compose.onNodeWithTag("library_update_house").performScrollTo().performClick()
        assertEquals(listOf("house"), updates)
    }

    @Test
    fun nothingNewNoBadge() {
        val all = listOf(house.copy(version = 2), snaps, PackManifest("jazz", "Jazz", sounds = house.sounds))
        show(PacksUi(IndexState.Ready(index), all, seen = emptySet()), actions = LibraryActions())
        compose.onNodeWithTag("library_news").assertDoesNotExist()
        compose.onNodeWithTag("library_download_all").assertDoesNotExist()
    }

    @Test
    fun theQueueShowsItsProgressAndStops() {
        var stopped = 0
        show(
            PacksUi(IndexState.Ready(index), queue = QueueUi(1, 3, "Pack jazz", 0.3f)),
            actions = LibraryActions(cancelAll = { stopped++ }),
        )
        compose.onNodeWithText("Pack 2 sur 3 : Pack jazz").assertExists()
        compose.onNodeWithTag("library_download_all").assertDoesNotExist()
        compose.onNodeWithTag("library_queue_stop").performClick()
        assertEquals(1, stopped)
    }

    @Test
    fun theSearchFindsAFingerSnapAndPlacesIt() {
        val used = mutableListOf<SoundOption>()
        show(PacksUi(IndexState.Ready(index), listOf(house, snaps)), actions = LibraryActions(use = { used += it }))
        compose.onNodeWithTag("library_search").performTextInput("snap")
        compose.onNodeWithText("Claquement de doigts 1").assertExists()
        compose.onNodeWithTag("library_result_use_0").performClick()
        assertEquals("clap_doigts_01.flac", (used.single().ref as SoundRef.Pack).sound.file)
    }
}
