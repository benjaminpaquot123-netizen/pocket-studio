package com.pocketstudio.app.remix

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
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

/**
 * Tour 6, R3 (seat B a, es, phone, screen 28): after « Frases » the cut screen's column scrolls, but its tools and its list
 * of pieces went under the bottom with nothing to say so (« plus aucun outil listé »). While something waits below, the
 * column's bottom fades (as the lists of tour 5, ScrollHintBox); scrolled to the end, the fade goes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp-xxhdpi")
class CutScrollSignTest {
    @get:Rule
    val compose = createComposeRule()

    private val frames = 60L * BOARD_RATE
    private val bar = 4 * 24_000L

    private class Shown(private val cuts: SongCuts) : StemBoardActions by NoStemBoardActions {
        override val state = MutableStateFlow(StemBoardState())
        override fun savedCuts(setId: String, frames: Long) = cuts
    }

    private fun signs() = compose.onAllNodesWithTag("cut_more", useUnmergedTree = true).fetchSemanticsNodes().size

    @Test
    fun aSignSaysTheToolsAreBelow() {
        val cuts = SongCuts("song", listOf(StemCuts(VOCALS_STEM, (0 until 6).map { CutPiece(it * 2 * bar, (it * 2 + 1) * bar) }, auto = "Phrases")))
        val ui = StemBoardUi("song", "Mi canción", frames, 120.0, 0.0, StemPeakSource { StemPeaks.of(frames, 1) { _, n -> FloatArray(n) } }, peaksContext = EmptyCoroutineContext)
        compose.setContent {
            PocketStudioTheme {
                Box(Modifier.fillMaxWidth().height(520.dp)) {
                    val model = remember { CutModel("song", frames, 120.0, 0.0, cuts, mode = AutoCut.Phrases) }
                    CutScreen(ui, remember { Shown(cuts) }, onClose = {}, model = model)
                }
            }
        }
        compose.waitForIdle()
        assertEquals("the tools and the pieces wait below: a sign says so", 1, signs())
        // The column scrolled to its end (the tallest scrolling node: not the chips' row nor the list of pieces), it is gone.
        val column = compose.onAllNodes(hasScrollAction(), useUnmergedTree = true).fetchSemanticsNodes()
            .maxBy { it.boundsInRoot.height }
        compose.onNode(SemanticsMatcher("the cut screen's column") { it.id == column.id }, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 100_000f) }
        compose.waitForIdle()
        assertTrue("scrolled to the end: no sign left", signs() == 0)
    }
}
