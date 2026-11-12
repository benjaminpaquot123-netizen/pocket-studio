package com.pocketstudio.app.remix

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.AutoCut
import com.pocketstudio.model.CutPiece
import com.pocketstudio.model.SongCuts
import com.pocketstudio.model.StemCuts
import com.pocketstudio.ui.PocketStudioTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Tour 6, seat E c #17 (the round's one blocker, pt-BR, phone, text at 200 %, Mode simple): in « Un bout » the curve and
 * its Início / Fim bars never showed: the column under the chips only got what the send bar left, and it did not scroll,
 * so the curve fell to nothing. Also seen at 100 % (seat B a, screen 21: the curve squeezed to ~50 dp). The curve keeps a
 * height a finger can work with and every control of the piece stays reachable. The heights are those left to the cut
 * screen under the app's bars, the Guide and the thumb bar (E c screen 18: about 430 dp at 200 %).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h914dp-xxhdpi")
class CutBoutBigTextTest {
    @get:Rule
    val compose = createComposeRule()

    private val frames = 60L * BOARD_RATE
    private val bar = 4 * 24_000L

    private class Shown(private val cuts: SongCuts) : StemBoardActions by NoStemBoardActions {
        override val state = MutableStateFlow(StemBoardState())
        override fun savedCuts(setId: String, frames: Long) = cuts
    }

    /** « Un bout » on [height] of screen under the app's bars, its text at [fontScale]. */
    private fun show(height: Dp, fontScale: Float) {
        val cuts = SongCuts("song", listOf(StemCuts(VOCALS_STEM, listOf(CutPiece(2 * bar, 4 * bar)), auto = "Selection")))
        val ui = StemBoardUi("song", "Ma chanson préférée", frames, 120.0, 0.0, StemPeakSource { StemPeaks.of(frames, 1) { _, n -> FloatArray(n) } }, peaksContext = EmptyCoroutineContext)
        compose.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale)) {
                PocketStudioTheme {
                    Box(Modifier.fillMaxWidth().height(height)) {
                        val model = remember { CutModel("song", frames, 120.0, 0.0, cuts, mode = AutoCut.Selection) }
                        CutScreen(ui, remember { Shown(cuts) }, onClose = {}, model = model)
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    /** Scrolled into view when the screen scrolls; left as it is when nothing scrolls (it must then be in view already). */
    private fun bringIntoView(tag: String) {
        runCatching { compose.onNodeWithTag(tag, useUnmergedTree = true).performScrollTo() }
        compose.waitForIdle()
    }

    private fun curveAndControls(height: Dp, fontScale: Float) {
        show(height, fontScale)
        val wave = compose.onNodeWithTag("cut_wave", useUnmergedTree = true)
        bringIntoView("cut_wave")
        val b = wave.getBoundsInRoot()
        val h = b.bottom - b.top
        assertTrue("the curve is $h high at ${fontScale * 100} % on $height: a finger needs 120 dp", h >= 120.dp)
        for (tag in listOf("cut_start", "cut_end", "cut_span_play", "cut_to_pads", "cut_to_track")) {
            bringIntoView(tag)
            compose.onNodeWithTag(tag, useUnmergedTree = true).assertIsDisplayed()
        }
    }

    @Test
    fun aPieceAtTwiceTheTextOnAPhone() = curveAndControls(430.dp, 2f)

    @Test
    fun aPieceAtTheUsualTextOnAPhone() = curveAndControls(480.dp, 1f)
}
