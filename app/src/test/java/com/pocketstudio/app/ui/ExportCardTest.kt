package com.pocketstudio.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pocketstudio.app.mixer.ExportCard
import com.pocketstudio.app.mixer.MixerActions
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
    fun theProChoicesWaitUnderAdvancedAndFitWhatIsExported() {
        var noLimiter: Boolean? = null
        var withMaster: Boolean? = null
        var ui by androidx.compose.runtime.mutableStateOf(ExportUi().forProject(sceneBars = 4, hasSong = true))
        compose.setContent {
            PocketStudioTheme {
                ExportCard(ui, MixerActions(setExportNoLimiter = { noLimiter = it }, setStemsWithMaster = { withMaster = it }), hasSong = true)
            }
        }
        compose.onNodeWithTag("export_no_limiter").assertDoesNotExist()
        compose.onNodeWithTag("export_advanced").performClick()
        // The mix: « Sans limiteur », no stems choice.
        compose.onNodeWithTag("export_stems_master").assertDoesNotExist()
        compose.onNodeWithTag("export_no_limiter").performClick()
        assertEquals(true, noLimiter)
        // Separate tracks: the master's processing is the choice, off by default.
        ui = ui.copy(what = ExportWhat.Stems, whatChosen = true)
        compose.onNodeWithTag("export_no_limiter").assertDoesNotExist()
        compose.onNodeWithTag("export_stems_master").performClick()
        assertEquals(true, withMaster)
    }

    @Test
    fun onlyTheMixWithItsLimiterIsLimited() {
        assertEquals(true, ExportUi().limited)
        assertEquals(false, ExportUi(noLimiter = true).limited)
        // Stems skip the master (and so its limiter) unless asked; « Sans limiteur » is the mix's own.
        assertEquals(false, ExportUi(what = ExportWhat.Stems).limited)
        assertEquals(false, ExportUi(what = ExportWhat.Stems, noLimiter = false).limited)
        assertEquals(true, ExportUi(what = ExportWhat.Stems, stemsWithMaster = true).limited)
    }

    @Test
    fun m4aAndFlacKeepTheLimiterRatherThanClip() {
        // Their samples cannot go over 0 dBFS: without the limiter the peaks would be cut flat.
        listOf(com.pocketstudio.app.rhythm.ExportFormat.M4a, com.pocketstudio.app.rhythm.ExportFormat.Flac).forEach { f ->
            assertEquals(true, ExportUi(noLimiter = true, format = f).limited)
            assertEquals(true, ExportUi(what = ExportWhat.Stems, format = f).limited)
        }
        // Stems still skip the master's effects (they add back up to the mix), whatever the format.
        assertEquals(false, ExportUi(what = ExportWhat.Stems, format = com.pocketstudio.app.rhythm.ExportFormat.M4a).masterFx)
        assertEquals(true, ExportUi(what = ExportWhat.Stems, stemsWithMaster = true).masterFx)
        assertEquals(true, ExportUi(noLimiter = true).masterFx)
    }
}
