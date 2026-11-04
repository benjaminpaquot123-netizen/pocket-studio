package com.pocketstudio.app

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pocketstudio.app.live.RemixCellMenu
import com.pocketstudio.app.padlive.ChallengeActions
import com.pocketstudio.app.padlive.ChallengePanel
import com.pocketstudio.app.padlive.ChallengeUi
import com.pocketstudio.app.padlive.PadLiveActions
import com.pocketstudio.app.padlive.PadLiveScreen
import com.pocketstudio.app.padlive.PadLiveUi
import com.pocketstudio.app.remix.BOARD_RATE
import com.pocketstudio.app.remix.CutModel
import com.pocketstudio.app.remix.CutScreen
import com.pocketstudio.app.remix.NoStemBoardActions
import com.pocketstudio.app.remix.StemBoardActions
import com.pocketstudio.app.remix.StemBoardState
import com.pocketstudio.app.remix.StemBoardUi
import com.pocketstudio.app.remix.StemPeakSource
import com.pocketstudio.app.remix.StemPeaks
import com.pocketstudio.app.remix.rememberStemBoardView
import com.pocketstudio.model.CutPiece
import com.pocketstudio.model.LivePads
import com.pocketstudio.model.Project
import com.pocketstudio.model.RemixCell
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

/**
 * Fix 2, n° 7: the small buttons the review found are 48 dp to the finger (their look unchanged),
 * and a cut piece's tile says whether it is ticked.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-xxhdpi")
class A11yTargetsFixTest {
    @get:Rule
    val compose = createComposeRule()

    /** What the finger (and TalkBack) gets: the node's touch bounds, its circle may stay 40 dp. */
    private fun big(vararg tags: String) = tags.forEach { tag ->
        val n = compose.onNodeWithTag(tag).fetchSemanticsNode()
        val min = 48 * n.layoutInfo.density.density - 0.5f
        assertTrue("$tag: ${n.touchBoundsInRoot}", n.touchBoundsInRoot.width >= min && n.touchBoundsInRoot.height >= min)
    }

    private fun tall(tag: String) {
        val n = compose.onNodeWithTag(tag).fetchSemanticsNode()
        assertTrue("$tag: ${n.touchBoundsInRoot}", n.touchBoundsInRoot.height >= 48 * n.layoutInfo.density.density - 0.5f)
    }

    @Test
    fun padLiveBarAndTakeButtons() {
        compose.setContent {
            PocketStudioTheme {
                PadLiveScreen(
                    PadLiveUi(LivePads(), List(LivePads.ALL) { 0 }, page = 0, pack = "", playing = false, tempoBpm = 120.0, beatsPerBar = 4, takeDone = true, perfSeconds = 30),
                    PadLiveActions(), clockOf = { 0 }, still = true,
                )
            }
        }
        compose.waitForIdle()
        big("padlive_back", "padlive_tempo_less", "padlive_tempo_more", "padlive_perf_close")
        tall("padlive_tempo")
    }

    @Test
    fun challengeClose() {
        compose.setContent { PocketStudioTheme { ChallengePanel(ChallengeUi.NoShots, LivePads(), ChallengeActions()) } }
        compose.waitForIdle()
        big("padlive_challenge_close")
    }

    @Test
    fun remixCellMenuChips() {
        val p = Project().let { it.copy(remix = it.remix.copy(setId = "s", songBpm = 120.0, seconds = 60.0)) }
        compose.setContent { PocketStudioTheme { RemixCellMenu(p, "Remix", 0, busy = false, choose = { _, _, _ -> }, clear = {}, cancel = {}) } }
        compose.waitForIdle()
        tall("remix_cell_stem_${RemixCell.WHOLE}")
        tall("remix_cell_bars_${RemixCell.LENGTHS.first()}")
        big("remix_cell_earlier", "remix_cell_later")
    }

    private class Shown(private val cuts: SongCuts) : StemBoardActions by NoStemBoardActions {
        override val state = MutableStateFlow(StemBoardState())
        override fun savedCuts(setId: String, frames: Long) = cuts
    }

    @Test
    fun cutPieceTileSaysItsTickAndPlaysBig() {
        val frames = 20L * BOARD_RATE
        val cuts = SongCuts("s", listOf(StemCuts(3, listOf(CutPiece(0, 4L * BOARD_RATE), CutPiece(4L * BOARD_RATE, 8L * BOARD_RATE)), chosen = listOf(1), auto = "Phrases")))
        val peaks = StemPeakSource { _ -> StemPeaks.of(frames, 1) { _, count -> FloatArray(count) } }
        val ui = StemBoardUi("s", "Chanson", frames, 120.0, 0.0, peaks, peaksContext = EmptyCoroutineContext)
        lateinit var model: CutModel
        compose.setContent {
            PocketStudioTheme {
                model = androidx.compose.runtime.remember { CutModel("s", frames, 120.0, 0.0, cuts, mode = com.pocketstudio.model.AutoCut.Phrases) }
                CutScreen(ui, androidx.compose.runtime.remember { Shown(cuts) }, onClose = {}, model = model, view = rememberStemBoardView(ui))
            }
        }
        compose.waitForIdle()
        fun state(i: Int) = compose.onNodeWithTag("cut_piece_$i").fetchSemanticsNode().config.getOrNull(SemanticsProperties.ToggleableState)
        assertEquals(ToggleableState.Off, state(0))
        assertEquals(ToggleableState.On, state(1))
        compose.onNodeWithTag("cut_piece_0").performClick()
        compose.waitForIdle()
        assertEquals(ToggleableState.On, state(0))
        assertEquals(listOf(0, 1), model.chosen)
        big("cut_piece_play_0")
    }
}
