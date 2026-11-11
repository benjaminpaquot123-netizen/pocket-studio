package com.pocketstudio.app.remix

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pocketstudio.app.export.ShareLevels
import com.pocketstudio.app.export.ShareSheetActions
import com.pocketstudio.app.export.ShareSheetContent
import com.pocketstudio.app.export.ShareSheetUi
import com.pocketstudio.app.export.shareChecks
import com.pocketstudio.app.ui.mixerStrips
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectInfo
import com.pocketstudio.model.Remix
import com.pocketstudio.model.SongPart
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Chantier 10 of the hands-on audit (G16, recette tel-15 « trop compliqué »): Remix → song → shared file in a few
 * touches. One recommended way, one clear button on the song, and « the end will be cut » with its two fixes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h1600dp")
class RemixChantier10Test {
    @get:Rule
    val compose = createComposeRule()

    private val set = StemSet(File("/tmp/stems/abc"), "Ma chanson", 214.0, 0L, SongInfo(98.0, 0.25, 9, true))

    /** A song of 8 bars (16 s at 120 BPM) and a remix's song of 60 s coming in on bar 2 (T5-Ec-28). */
    private fun overrun() = Project(
        name = "Remix coupé", drums = DrumPattern().toggle(0, 0),
        song = listOf(SongPart(0, bars = 4), SongPart(0, bars = 4)),
        remix = Remix(setId = "s", name = "Boys", songBpm = 120.0, seconds = 60.0, atStep = 16, placed = true),
    )

    /** « La fin sera coupée » comes with its two fixes: « Allonger le morceau » and « Couper la chanson à la fin ». */
    @Test
    fun theCutEndHasItsTwoFixes() {
        val p = overrun()
        val checks = shareChecks(p, mixerStrips(p), ShareLevels(), loopBars = 8)
        compose.setContent {
            PocketStudioTheme { ShareSheetContent(ShareSheetUi("Remix coupé", seconds = 16, parts = 2, checks = checks), ShareSheetActions()) }
        }
        compose.onNodeWithTag("share_check_remix_overrun").assertExists()
        compose.onNodeWithTag("share_fix_remix_longer").assertExists()
        compose.onNodeWithTag("share_fix_remix_cut").assertExists()
    }
}
