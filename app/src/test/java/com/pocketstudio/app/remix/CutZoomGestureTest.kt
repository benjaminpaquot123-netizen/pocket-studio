package com.pocketstudio.app.remix

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.CutPiece
import com.pocketstudio.model.SongCuts
import com.pocketstudio.model.StemCuts
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Fix 2, n° 1: « Découper la chanson » on a whole 3-minute song cut every bar (a line every 2 s):
 * a touch places the play head instead of grabbing a cut, a pinch zooms even when the first finger
 * lands on a line, and none of it leaves an undo step or a save behind.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp-xxhdpi")
class CutZoomGestureTest {
    @get:Rule
    val compose = createComposeRule()

    private val song = 180L * BOARD_RATE
    private val bar = 4 * 24_000L  // 120 BPM

    private fun setUp(): Triple<CutModel, StemBoardView, MutableList<SongCuts>> {
        val bars = (0 until song / bar).map { CutPiece(it * bar, (it + 1) * bar) }
        val saved = mutableListOf<SongCuts>()
        val m = CutModel("song", song, 120.0, 0.0, SongCuts("song", listOf(StemCuts(VOCALS_STEM, bars))), onChange = { saved += it }, mode = com.pocketstudio.model.AutoCut.Phrases)
        val v = StemBoardView(song)
        compose.setContent {
            PocketStudioTheme {
                CutZoom(m, v, BoardPeaks(), mutableStateOf(StemBoardState()), onTouched = {}, modifier = Modifier.fillMaxWidth().height(210.dp))
            }
        }
        compose.waitForIdle()
        return Triple(m, v, saved)
    }

    @Test
    fun aTouchNearALinePlacesThePlayHeadAndLeavesTheCutsAlone() {
        val (m, v, saved) = setUp()
        val before = m.pieces
        // A few px off the line of bar 30 (6 dp): the old 28 dp zone grabbed it.
        val x = v.x(30 * bar) + 18f
        compose.onNodeWithTag("cut_wave").performTouchInput { click(Offset(x, centerY)) }
        compose.waitForIdle()
        assertEquals(v.frameAt(x).toDouble(), m.cursor.toDouble(), v.framesPerPx * 2)
        assertEquals(before, m.pieces)
        assertFalse(m.canUndo)
        assertTrue(saved.isEmpty())
    }

    @Test
    fun aSecondFingerZoomsEvenWhenTheFirstLandedOnALine() {
        val (m, v, saved) = setUp()
        // Zoomed in until bar 30's line stands alone and is grabbed.
        v.zoomBy(60f, v.x(30 * bar))
        compose.waitForIdle()
        val line = v.x(30 * bar)
        assertTrue(m.beginMove(v.frameAt(line), (12 * 3 * v.framesPerPx).toLong()).also { m.endMove() })
        val zoomBefore = v.framesPerPx
        compose.onNodeWithTag("cut_wave").performTouchInput {
            down(0, Offset(line, centerY))
            down(1, Offset(line + 200f, centerY))
            moveTo(0, Offset(line - 150f, centerY))
            moveTo(1, Offset(line + 350f, centerY))
            moveTo(0, Offset(line - 300f, centerY))
            moveTo(1, Offset(line + 500f, centerY))
            up(0)
            up(1)
        }
        compose.waitForIdle()
        assertTrue("pinched: ${v.framesPerPx} vs $zoomBefore", v.framesPerPx < zoomBefore * 0.9)
        assertEquals(30 * bar, m.pieces[29].end)
        assertFalse(m.canUndo)
        assertTrue(saved.isEmpty())
    }
}
