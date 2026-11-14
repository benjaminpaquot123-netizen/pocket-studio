package com.pocketstudio.app.remix

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.AutoCut
import com.pocketstudio.model.CutPiece
import com.pocketstudio.model.SongCuts
import com.pocketstudio.model.StemCuts
import com.pocketstudio.ui.PocketStudioTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Audit ultra round 3, zone 1, n° 9 (seat B c, Arabic, Mode simple, phone): « Coupes › Phrases » under the app's bars left
 * the cut screen ~520 dp; its curve's fixed 210 dp pushed « Coupes », the tools and the list of pieces out: no way back to
 * « Un bout », no piece to tick, the send buttons grey. Every control stays reachable, right to left as left to right.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp-xxhdpi")
class CutScreenTightTest {
    @get:Rule
    val compose = createComposeRule()

    private val frames = 60L * BOARD_RATE
    private val bar = 4 * 24_000L

    private class Shown(private val cuts: SongCuts) : StemBoardActions by NoStemBoardActions {
        override val state = MutableStateFlow(StemBoardState())
        override fun savedCuts(setId: String, frames: Long) = cuts
    }

    private fun show(rtl: Boolean): CutModel {
        val cuts = SongCuts("song", listOf(StemCuts(VOCALS_STEM, (0 until 6).map { CutPiece(it * 2 * bar, (it * 2 + 1) * bar) }, auto = "Phrases")))
        val ui = StemBoardUi("song", "Ma chanson préférée", frames, 120.0, 0.0, StemPeakSource { StemPeaks.of(frames, 1) { _, n -> FloatArray(n) } }, peaksContext = EmptyCoroutineContext)
        lateinit var model: CutModel
        compose.setContent {
            PocketStudioTheme {
                CompositionLocalProvider(LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                    Box(Modifier.fillMaxWidth().height(520.dp)) {
                        model = remember { CutModel("song", frames, 120.0, 0.0, cuts, mode = AutoCut.Phrases) }
                        CutScreen(ui, remember { Shown(cuts) }, onClose = {}, model = model)
                    }
                }
            }
        }
        compose.waitForIdle()
        return model
    }

    private fun reachable(tag: String) {
        compose.onNodeWithTag(tag, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
    }

    private fun everythingStays(rtl: Boolean) {
        val model = show(rtl)
        // Tour 6 (A4, lot coquille2): the mode menus stay in view under the curve, out of its scroll.
        compose.onNodeWithTag("cut_auto", useUnmergedTree = true).assertIsDisplayed()
        reachable("cut_here")
        reachable("cut_count")
        compose.onNodeWithTag("cut_to_pads").assertIsDisplayed()
        // A piece ticked in the list: it is chosen, ready to send.
        compose.onNodeWithTag("cut_all").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(6, model.chosen.size)
    }

    @Test
    fun leftToRight() = everythingStays(rtl = false)

    @Test
    fun rightToLeft() = everythingStays(rtl = true)
}
