package com.pocketstudio.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pocketstudio.app.mixer.ExportCard
import com.pocketstudio.app.mixer.MixerActions
import com.pocketstudio.app.mixer.ShareSongCard
import com.pocketstudio.app.rhythm.ExportFade
import com.pocketstudio.app.rhythm.ExportUi
import com.pocketstudio.app.rhythm.ExportWhat
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h1600dp")
class ExportCardTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun thePlainChoicesShowAndTheRestWaitsUnderAdvanced() {
        var what: ExportWhat? = null
        var fade: ExportFade? = null
        compose.setContent {
            PocketStudioTheme {
                ExportCard(
                    ExportUi().forProject(sceneBars = 4, hasSong = true),
                    MixerActions(setExportWhat = { what = it }, setExportFade = { fade = it }),
                    hasSong = true,
                )
            }
        }
        // The song by default, with a short fade; WAV and M4A in plain sight.
        compose.onNodeWithTag("export_what_Song").assertIsSelected()
        compose.onNodeWithTag("export_fade_Short").assertIsSelected()
        compose.onNodeWithTag("export_format_M4a").assertIsDisplayed()
        compose.onNodeWithTag("export_format_Wav").assertIsSelected()
        // Separate tracks, FLAC and MIDI are under « Avancé », folded.
        compose.onNodeWithTag("export_what_Stems").assertDoesNotExist()
        compose.onNodeWithTag("export_format_Flac").assertDoesNotExist()
        compose.onNodeWithTag("export_midi").assertDoesNotExist()
        compose.onNodeWithTag("export_advanced").performClick()
        compose.onNodeWithTag("export_what_Stems").assertIsDisplayed()
        compose.onNodeWithTag("export_format_Flac").assertIsDisplayed()
        compose.onNodeWithTag("export_midi").assertIsDisplayed()
        compose.onNodeWithTag("export_what_Stems").performClick()
        assertEquals(ExportWhat.Stems, what)
        compose.onNodeWithTag("export_fade_Long").performClick()
        assertEquals(ExportFade.Long, fade)
    }

    @Test
    fun theShareCardSharesInOneTouch() {
        var shared = 0
        var scrolled = 0
        compose.setContent {
            PocketStudioTheme { ShareSongCard(ExportUi(), hasSong = true, share = { shared++ }) { scrolled++ } }
        }
        compose.onNodeWithTag("share_song").performClick()
        assertEquals(1, shared)
        compose.onNodeWithTag("to_export").performClick()
        assertEquals(1, scrolled)
    }
}
