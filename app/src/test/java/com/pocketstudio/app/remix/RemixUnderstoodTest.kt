package com.pocketstudio.app.remix

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextLayoutResult
import com.pocketstudio.model.Harmony
import com.pocketstudio.model.Remix
import com.pocketstudio.model.Scale
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Remix testers (lot « remix 1 », Sarah and Mehdi): what the Remix page says. « Mesure 3 » read « Me »,
 * the tempo switch had no name, the model's download ended in silence, the project's key was nowhere.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w392dp-h852dp")
class RemixUnderstoodTest {
    @get:Rule
    val compose = createComposeRule()

    private val song = StemSet(File("/tmp/stems/abc"), "Ma chanson préférée", 40.0, 1L, SongInfo(bpm = 120.0, root = 9, minor = true))
    private val inProject = Remix(setId = song.id, name = song.name, songBpm = 120.0, startBar = 2)

    private fun layout(tag: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action!!.invoke(results)
        return results.first()
    }

    @Test
    fun theSongInTheProjectSaysWhereItStartsAndBothKeys() {
        compose.setContent {
            PocketStudioTheme {
                RemixScreen(RemixUi(true, SeparationState.Idle, listOf(song), remix = inProject, projectBpm = 174.0, projectKey = "Fa mineur"), RemixActions(effects = {}))
            }
        }
        // Its own line, whole (it was « Me » between « Jouer » and « Effets » on this phone).
        compose.onNodeWithText("Partir de la mesure 3 de la chanson").assertIsDisplayed()
        assertFalse(layout("remix_start_bar").hasVisualOverflow)
        compose.onNodeWithTag("remix_keys").assertIsDisplayed()
        compose.onNodeWithText("Tonalité : projet en Fa mineur, chanson en La mineur").assertExists()
        // The switch that fits the song to the project's tempo has its name before it is on.
        compose.onNodeWithTag("remix_follow_tempo_label").assertIsDisplayed()
        compose.onNodeWithText("Caler au tempo").assertExists()
    }

    @Test
    fun beforePlayingItAlongTheSongCardSaysTheProjectsTempoAndKey() {
        compose.setContent {
            PocketStudioTheme { RemixScreen(RemixUi(true, SeparationState.Idle, listOf(song), projectBpm = 174.0, projectKey = "Fa mineur"), RemixActions()) }
        }
        compose.onNodeWithTag("song_info_${song.id}").assertExists()
        compose.onNodeWithText("0:40 · 120 BPM · La mineur").assertExists()
        compose.onNodeWithText("Ton projet : 174 BPM · Fa mineur").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun theModelsDownloadEndsWithItsReadyLine() {
        var state by mutableStateOf<SeparationState>(SeparationState.DownloadingModel(0f))
        var installed by mutableStateOf(false)
        compose.setContent { PocketStudioTheme { RemixScreen(RemixUi(installed, state, emptyList()), RemixActions()) } }
        compose.onNodeWithText("Téléchargement du modèle… 0 %").assertIsDisplayed()
        compose.onNodeWithTag("remix_model_ready").assertDoesNotExist()
        state = SeparationState.DownloadingModel(0.6f)
        compose.waitForIdle()
        installed = true
        state = SeparationState.Idle
        compose.waitForIdle()
        compose.onNodeWithTag("remix_model_ready").assertIsDisplayed()
        compose.onNodeWithText("C'est prêt : le modèle est sur ton téléphone. Choisis une chanson.").assertExists()
        compose.onNodeWithTag("remix_pick").assertIsDisplayed()
    }

    @Test
    fun noReadyLineWhenTheModelWasAlreadyThere() {
        compose.setContent { PocketStudioTheme { RemixScreen(RemixUi(true, SeparationState.Idle, emptyList()), RemixActions()) } }
        compose.onNodeWithTag("remix_pick").assertIsDisplayed()
        compose.onNodeWithTag("remix_model_ready").assertDoesNotExist()
    }

    @Test
    fun theProjectsKeyIsNamedAsTheSongsChordsSayIt() {
        assertEquals("Fa mineur", keyLabel(Harmony(root = 5, scale = Scale.Minor), english = false))
        assertEquals("F mineur", keyLabel(Harmony(root = 5, scale = Scale.Minor), english = true))
        assertEquals("Ré dorienne", keyLabel(Harmony(root = 2, scale = Scale.Dorian), english = false))
        assertEquals("La majeur", keyLabel(Harmony(root = 9, scale = Scale.Major), english = false))
        // « Libre »: no key to show.
        assertNull(keyLabel(Harmony(root = 0, scale = Scale.Off), english = false))
    }
}
