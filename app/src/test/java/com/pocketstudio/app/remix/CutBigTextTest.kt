package com.pocketstudio.app.remix

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.AutoCut
import com.pocketstudio.model.CutPiece
import com.pocketstudio.model.SongCuts
import com.pocketstudio.model.StemCuts
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.PocketStudioTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Tour 7, lot 2 (seat 2b, pt-BR, phone, text at 200 %, Mode simple): « Um pedaço » on the ~430 dp the cut screen gets
 * under the app's bars (as in [CutBoutBigTextTest]).
 */
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h852dp-port-hdpi")
class CutBigTextTest {
    @get:Rule
    val compose = createComposeRule()

    private val frames = 60L * BOARD_RATE
    private val bar = 4 * 24_000L

    private class Shown(private val cuts: SongCuts) : StemBoardActions by NoStemBoardActions {
        override val state = MutableStateFlow(StemBoardState())
        override fun savedCuts(setId: String, frames: Long) = cuts
    }

    @Before fun ptBr() { I18n.lang = Lang.PT_BR }
    @After fun french() { I18n.lang = Lang.FR }

    /**
     * The cut screen as the seat had it: [above] dp of the app's bars over it, [height] dp for it, its text at [fontScale].
     */
    private fun show(fontScale: Float = 2f, mode: AutoCut = AutoCut.Selection, above: Int = 0, height: Int = 430) {
        val cuts = SongCuts("song", listOf(StemCuts(VOCALS_STEM, listOf(CutPiece(2 * bar, 4 * bar)), auto = mode.name)))
        val ui = StemBoardUi("song", "Boys", frames, 120.0, 0.0, StemPeakSource { StemPeaks.of(frames, 1) { _, n -> FloatArray(n) } }, peaksContext = EmptyCoroutineContext)
        compose.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale)) {
                PocketStudioTheme {
                    androidx.compose.foundation.layout.Column(Modifier.fillMaxSize()) {
                        androidx.compose.foundation.layout.Spacer(Modifier.height(above.dp))
                        Box(Modifier.fillMaxWidth().height(height.dp)) {
                            val model = remember { CutModel("song", frames, 120.0, 0.0, cuts, mode = mode) }
                            CutScreen(ui, remember { Shown(cuts) }, onClose = {}, model = model)
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    /** D7 (« Resto » cut without a sign, 2b #14): every stem chip is drawn whole, none clipped by the row's edge. */
    @Test
    fun everyStemChipIsWhole() {
        show()
        for (stem in 0 until 4) {
            val tag = "cut_stem_$stem"
            val n = compose.onNodeWithTag(tag).fetchSemanticsNode()
            val shown = n.boundsInRoot.width
            assertTrue("$tag shows $shown of its ${n.size.width} px", shown >= n.size.width - 1f)
        }
    }

    /** Whole in view: the node's drawn bounds are its full size, inside the screen. */
    private fun whole(tag: String): String? {
        val n = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()
        val b = n.boundsInRoot
        return if (b.width >= n.size.width - 1f && b.height >= n.size.height - 1f) null else "$tag shows ${b.width} x ${b.height} of ${n.size}"
    }

    /** D1 (R3-EC17, 2b #18): with the playhead placed, the curve and Início, Fim and Ouvir are seen together without a scroll. */
    @Test
    fun theCurveAndItsThreeButtonsAreSeenTogether() {
        show()
        // However the screen is scrolled (to the buttons or to the curve), the four are in view together.
        for (first in listOf("cut_span_play", "cut_wave")) {
            runCatching { compose.onNodeWithTag(first, useUnmergedTree = true).performScrollTo() }
            compose.waitForIdle()
            val cut = listOf("cut_wave", "cut_start", "cut_end", "cut_span_play").mapNotNull { whole(it) }
            assertTrue("scrolled to $first: $cut", cut.isEmpty())
        }
    }

    /** In the lower half of the phone (a left thumb's reach), measured on the whole screen. */
    private fun low(tag: String): String? {
        val root = compose.onRoot().getBoundsInRoot()
        val b = compose.onNodeWithTag(tag, useUnmergedTree = true).getBoundsInRoot()
        return if ((b.top + b.bottom) / 2 > (root.top + root.bottom) / 2) null else "$tag at ${b.top}..${b.bottom} of ${root.bottom}"
    }

    /** D2 (R7, seat 2c #15): « ✂ Un trozo ▾ » sits in the lower half, under Inicio / Fin / Escuchar. */
    @Test
    fun theCutsMenuIsUnderTheThumb() {
        I18n.lang = Lang.ES
        show(fontScale = 1f, above = 213, height = 480)
        assertTrue(low("cut_auto") ?: "", low("cut_auto") == null)
    }
}
