package com.pocketstudio.app.mixer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.rhythm.ExportUi
import com.pocketstudio.app.rhythm.ExportWhat
import com.pocketstudio.model.TargetChoice
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Audit ultra round 1 (n° 16, seats A c, E d, F c: « Exporter » in the Mixeur does nothing), tested
 * without the bench: the ViewModel makes the file at once ([com.pocketstudio.app.export.OneTargetExportTest]);
 * what the seats missed is its result, written under the button where the screen ends (the bench renders
 * in a second, the button back before the next look). Now the file and its report come into view.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h640dp-xxhdpi")
class ExportResultInViewTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theFileAndItsReportComeIntoView() {
        val export = mutableStateOf(ExportUi(what = ExportWhat.Song))
        compose.setContent {
            PocketStudioTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    // The console, the master and the buses above, as in the Mixeur.
                    Spacer(Modifier.height(1200.dp))
                    ExportCard(export.value, MixerActions(), hasSong = true)
                }
            }
        }
        // The button at the screen's bottom edge, where the seats touched it.
        compose.onNodeWithTag("export").performScrollTo()
        compose.waitForIdle()
        export.value = export.value.copy(
            files = listOf(File("Ma-Mix-club-20261104-201500.wav")), savedInMusic = true, loudness = -8.1, truePeakDb = -1.2, seconds = 158.0,
            writtenFormat = com.pocketstudio.app.rhythm.ExportFormat.Wav, targetLufs = -8.0, targetChoice = TargetChoice.CLUB, targetWholeSong = false,
        )
        // The scroll there glides (about half a second).
        repeat(10) { compose.mainClock.advanceTimeBy(200); compose.waitForIdle() }
        compose.onNodeWithTag("export_summary").assertIsDisplayed()
        compose.onNodeWithTag("export_report").assertIsDisplayed()
        compose.onNodeWithTag("export_target").assertIsDisplayed()
        compose.onNodeWithTag("share").assertIsDisplayed()
    }
}
