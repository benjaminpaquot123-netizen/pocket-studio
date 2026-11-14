package com.pocketstudio.app.remix

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pocketstudio.model.Remix
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Audit ultra round 1, n° 1 (three testers, two of them went to the Mode pro for nothing): right after
 * the split, the song's card has « Découper ce que je veux », before any « Jouer avec le projet »;
 * touched, the song goes into the project behind the scenes and the cut screen opens once its stems are in.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w392dp-h852dp")
class RemixCutFromCardTest {
    @get:Rule
    val compose = createComposeRule()

    private val song = StemSet(File("/tmp/stems/cut"), "Ma chanson préférée", 40.0, 1L, SongInfo(bpm = 120.0))

    private fun board() = StemBoardUi(
        song.id, song.name, 48_000L * 40, 120.0, 0.0,
        StemPeakSource { StemPeaks.of(48_000L * 40, 2) { _, c -> FloatArray(c * 2) } },
        peaksContext = EmptyCoroutineContext,
    )

    @Test
    fun theSplitSongsCardCutsItWithNothingBefore() {
        var remix by mutableStateOf(Remix())
        var board by mutableStateOf<StemBoardUi?>(null)
        var asked: StemSet? = null
        compose.setContent {
            PocketStudioTheme {
                RemixScreen(RemixUi(true, SeparationState.Idle, listOf(song), remix = remix, board = board), RemixActions(cutSong = { asked = it }))
            }
        }
        // On the card, first, with its line; « Jouer avec le projet » is still there, under it.
        compose.onNodeWithTag("cut_song_${song.id}").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Découper ce que je veux", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("cut_song_hint_${song.id}").assertIsDisplayed()
        compose.onNodeWithTag("use_${song.id}").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("cut_song_${song.id}").performScrollTo().performClick()
        assertEquals(song, asked)
        // The song came in (silent, behind the scenes): its stems load, said on the button.
        remix = Remix(setId = song.id, name = song.name, songBpm = 120.0)
        compose.waitForIdle()
        compose.onNodeWithTag("cut_song_${song.id}").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Chargement des pistes…", substring = true).assertExists()
        compose.onNodeWithTag("cut_screen").assertDoesNotExist()
        // Its stems are in: the cut screen opens by itself.
        board = board()
        compose.waitForIdle()
        compose.onNodeWithTag("cut_screen").assertIsDisplayed()
    }

    @Test
    fun aSongAlreadyInTheProjectOpensItsCutScreenAtOnce() {
        var asked: StemSet? = null
        compose.setContent {
            PocketStudioTheme {
                RemixScreen(
                    RemixUi(true, SeparationState.Idle, listOf(song), remix = Remix(setId = song.id, name = song.name, songBpm = 120.0), board = board()),
                    RemixActions(cutSong = { asked = it }),
                )
            }
        }
        // Control Ba-2 (A8): its card no longer repeats the cut; the thumb row's one button opens it at once.
        compose.onNodeWithTag("cut_song_${song.id}").assertDoesNotExist()
        compose.onNodeWithTag("remix_cut_thumb").performClick()
        compose.onNodeWithTag("cut_screen").assertIsDisplayed()
        assertNull(asked)
    }

    @Test
    fun theHomesRemixHasNoCutButton() {
        compose.setContent {
            PocketStudioTheme { RemixScreen(RemixUi(true, SeparationState.Idle, listOf(song), home = true), RemixActions()) }
        }
        compose.onNodeWithTag("use_${song.id}").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("cut_song_${song.id}").assertDoesNotExist()
    }
}
