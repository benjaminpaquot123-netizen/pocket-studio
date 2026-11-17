package com.pocketstudio.app.remix

import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import com.pocketstudio.model.AutoCut
import com.pocketstudio.model.CutPiece
import com.pocketstudio.model.SongCuts
import com.pocketstudio.model.StemCuts
import com.pocketstudio.ui.PocketStudioTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.coroutines.EmptyCoroutineContext

/** Review of tour 7 (m2): the play head (a new state every 33 ms) does not compose the whole cut screen again. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp-xxhdpi")
class CutPlayHeadTest {
    @get:Rule
    val compose = createComposeRule()

    private val frames = 60L * BOARD_RATE
    private val bar = 4 * 24_000L

    private class Shown(private val cuts: SongCuts) : StemBoardActions by NoStemBoardActions {
        override val state = MutableStateFlow(StemBoardState())
        override fun savedCuts(setId: String, frames: Long) = cuts
    }

    @Test
    fun inPhrases() = thePlayHeadDoesNotComposeTheScreenAgain(AutoCut.Phrases)

    @Test
    fun inOnePiece() = thePlayHeadDoesNotComposeTheScreenAgain(AutoCut.Selection)

    private fun thePlayHeadDoesNotComposeTheScreenAgain(mode: AutoCut) {
        run {
            val cuts = SongCuts("song", listOf(StemCuts(VOCALS_STEM, (0 until 6).map { CutPiece(it * 2 * bar, (it * 2 + 1) * bar) }, auto = "Phrases")))
            val ui = StemBoardUi("song", "Ma chanson", frames, 120.0, 0.0, StemPeakSource { StemPeaks.of(frames, 1) { _, n -> FloatArray(n) } }, peaksContext = EmptyCoroutineContext)
            val actions = Shown(cuts)
            compose.setContent {
                PocketStudioTheme {
                    CutScreen(ui, actions, onClose = {}, model = remember { CutModel("song", frames, 120.0, 0.0, cuts, mode = mode) })
                }
            }
            val heard = StemSelection("song", VOCALS_STEM, 0L, 2 * bar)
            actions.state.value = StemBoardState(playing = heard, playFrame = 0L)
            compose.waitForIdle()
            val before = cutCurveCompositions
            assertTrue("the curve was never composed", before > 0)
            repeat(10) { k ->
                actions.state.value = actions.state.value.copy(playFrame = (k + 1) * 1600L)
                compose.waitForIdle()
            }
            assertEquals("$mode", before, cutCurveCompositions)
        }
    }
}
