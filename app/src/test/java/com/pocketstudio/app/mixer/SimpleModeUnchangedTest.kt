package com.pocketstudio.app.mixer

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import com.pocketstudio.app.export.ExportOptions
import com.pocketstudio.app.export.ShareSheetActions
import com.pocketstudio.app.export.ShareSheetContent
import com.pocketstudio.app.export.ShareSheetUi
import com.pocketstudio.app.rhythm.ExportFormat
import com.pocketstudio.app.rhythm.ExportUi
import com.pocketstudio.app.rhythm.ExportWhat
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectJson
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Lot E (« Master pro »): the Mode simple keeps its three clear choices. « Prêt à partager ? » offers
 * M4A, WAV and the video, nothing of the master pro nor of the file's final form; the export as
 * « Partager » and the Mode simple make it (ExportOptions at its defaults) writes the same rate and bits
 * as before the lot; a project without master pro settings saves as before.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h1600dp")
class SimpleModeUnchangedTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theShareSheetOffersItsThreeChoicesAndNothingMore() {
        compose.setContent {
            PocketStudioTheme {
                ShareSheetContent(ShareSheetUi("Mon tube", seconds = 180, parts = 3, checks = emptyList()), ShareSheetActions(), simple = true)
            }
        }
        compose.onNodeWithTag("share_format_M4a").assertExists()
        compose.onNodeWithTag("share_format_Wav").assertExists()
        compose.onNodeWithTag("share_video").assertExists()
        for (tag in listOf("master_pro", "master_pro_panel", "export_final", "pro_compare", "pro_reference", "export_rate_96000", "share_format_Flac")) {
            assertEquals(tag, 0, compose.onAllNodesWithTag(tag).fetchSemanticsNodes().size)
        }
    }

    @Test
    fun theDefaultOptionsWriteWhatTheExportWroteBefore() {
        val o = ExportOptions()
        for (format in ExportFormat.entries) for (cd in listOf(false, true)) for (b16 in listOf(false, true)) for (noLimiter in listOf(false, true)) {
            val ui = ExportUi(format = format, cdRate = cd, bits16 = b16, noLimiter = noLimiter, what = ExportWhat.Song)
            // The formulas of the export before lot E.
            val rateBefore = if (cd) OfflineExport.CD_RATE else OfflineExport.SAMPLE_RATE
            val bitsBefore = if (!ui.limited && format == ExportFormat.Wav) 32 else if (b16 || format == ExportFormat.M4a) 16 else 24
            assertEquals("$format cd=$cd", rateBefore, o.rateFor(format, cd))
            assertEquals("$format b16=$b16 noLimiter=$noLimiter", bitsBefore, o.bitsFor(format, ui.limited, b16))
            val f = o.finish(stems = false, rangeStart = 0, bits = bitsBefore)
            assertEquals(OfflineExport.Finish(), f)
        }
        assertFalse(o.ranged)
        assertEquals(null, o.songRange(Project()))
    }

    @Test
    fun aProjectWithoutMasterProSavesAsBefore() {
        val json = ProjectJson.encode(Project())
        assertFalse(json, json.contains("\"pro\""))
        assertEquals(null, Project().mixer.pro)
        // A default pro (nothing changed) is not kept either.
        assertEquals(null, Mixer(pro = com.pocketstudio.model.MasterPro()).sanitized().pro)
    }
}
