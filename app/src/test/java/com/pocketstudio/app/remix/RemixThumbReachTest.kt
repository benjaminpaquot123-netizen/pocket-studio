package com.pocketstudio.app.remix

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Audit ultra round 1 (n° 21, lot C e: a left thumb): « Télécharger le modèle » and « Choisir une chanson »
 * were only at the top of the Remix page, the bottom half empty. They are at the bottom too, in the thumb's reach.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class RemixThumbReachTest {
    @get:Rule
    val compose = createComposeRule()

    private fun lowThird(tag: String) {
        val root = compose.onRoot().getBoundsInRoot()
        val b = compose.onNodeWithTag(tag).assertIsDisplayed().getBoundsInRoot()
        assertTrue("$tag at ${b.top}..${b.bottom} of ${root.bottom}", b.top.value > root.bottom.value * 2 / 3)
    }

    @Test
    fun theModelThenTheSongAtTheThumb() {
        var installed by androidx.compose.runtime.mutableStateOf(false)
        var downloads = 0
        var picks = 0
        compose.setContent {
            PocketStudioTheme {
                RemixScreen(RemixUi(installed, SeparationState.Idle, emptyList()), RemixActions(downloadModel = { downloads++ }, pickSong = { picks++ }))
            }
        }
        lowThird("remix_download_thumb")
        compose.onNodeWithTag("remix_download_thumb").performClick()
        assertEquals(1, downloads)
        installed = true
        compose.waitForIdle()
        lowThird("remix_pick_thumb")
        compose.onNodeWithTag("remix_pick_thumb").performClick()
        assertEquals(1, picks)
    }

    @Test
    fun aSongInTheProjectIsCutFromTheThumb() {
        val song = StemSet(java.io.File("/tmp/stems/thumb"), "Boys", 40.0, 1L, SongInfo(bpm = 120.0))
        val board = StemBoardUi(song.id, song.name, 48_000L * 40, 120.0, 0.0,
            StemPeakSource { StemPeaks.of(48_000L * 40, 2) { _, c -> FloatArray(c * 2) } }, peaksContext = kotlin.coroutines.EmptyCoroutineContext)
        compose.setContent {
            PocketStudioTheme {
                RemixScreen(RemixUi(true, SeparationState.Idle, listOf(song), remix = com.pocketstudio.model.Remix(setId = song.id, name = song.name, songBpm = 120.0), board = board), RemixActions())
            }
        }
        lowThird("remix_cut_thumb")
        compose.onNodeWithTag("remix_cut_thumb").performClick()
        compose.onNodeWithTag("cut_screen").assertIsDisplayed()
    }

    @Test
    fun notWhileASongIsSeparated() {
        compose.setContent {
            PocketStudioTheme { RemixScreen(RemixUi(true, SeparationState.Separating("Boys", 0.4f), emptyList()), RemixActions()) }
        }
        compose.onNodeWithTag("remix_thumb").assertDoesNotExist()
    }
}
