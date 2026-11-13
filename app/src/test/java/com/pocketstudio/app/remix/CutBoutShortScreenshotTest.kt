package com.pocketstudio.app.remix

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.pocketstudio.model.AutoCut
import com.pocketstudio.model.CutPiece
import com.pocketstudio.model.SongCuts
import com.pocketstudio.model.StemCuts
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.PocketStudioTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.abs
import kotlin.math.sin

/**
 * Tour 6, seat E c #17: « Un bout » on the room the app's bars, the Guide and the thumb bar leave it on a 411 dp phone
 * (about 430 dp at 200 %, 480 dp at 100 %), in Brazilian Portuguese as the seat saw it and in French: the curve, its
 * bars and the send buttons, the screen scrolled to its top.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-xxhdpi")
class CutBoutShortScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val frames = 40L * BOARD_RATE
    private val bar = 4 * 24_000L

    @After
    fun french() {
        I18n.lang = Lang.FR
    }

    private class Shown(private val cuts: SongCuts) : StemBoardActions by NoStemBoardActions {
        override val state = MutableStateFlow(StemBoardState())
        override fun savedCuts(setId: String, frames: Long) = cuts
    }

    private fun shoot(name: String, lang: Lang, height: Dp, fontScale: Float) {
        I18n.lang = lang
        val cuts = SongCuts("song", listOf(StemCuts(VOCALS_STEM, listOf(CutPiece(2 * bar, 4 * bar)), auto = "Selection")))
        val peaks = StemPeakSource { _ -> StemPeaks.of(frames, 1) { start, n -> FloatArray(n) { k -> (0.3 + 0.5 * abs(sin((start + k) / 9000.0))).toFloat() } } }
        val ui = StemBoardUi("song", "Ma chanson préférée", frames, 120.0, 0.0, peaks, peaksContext = EmptyCoroutineContext)
        compose.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale)) {
                PocketStudioTheme {
                    Box(Modifier.fillMaxWidth().height(height).background(Neon.Background)) {
                        val model = remember { CutModel("song", frames, 120.0, 0.0, cuts, mode = AutoCut.Selection) }
                        CutScreen(ui, remember { Shown(cuts) }, onClose = {}, model = model)
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("screenshots/remix/$name.png")
    }

    @Test
    fun ptBrAt200() = shoot("cut_bout_short_ptbr_200", Lang.PT_BR, 430.dp, 2f)

    @Test
    fun frAt130() = shoot("cut_bout_short_fr_130", Lang.FR, 450.dp, 1.3f)

    @Test
    fun frAt100() = shoot("cut_bout_short_fr_100", Lang.FR, 480.dp, 1f)
}
