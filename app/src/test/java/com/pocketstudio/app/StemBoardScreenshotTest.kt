package com.pocketstudio.app

import androidx.compose.ui.unit.dp
import com.pocketstudio.app.remix.BOARD_RATE
import com.pocketstudio.app.remix.NoStemBoardActions
import com.pocketstudio.app.remix.StemBoard
import com.pocketstudio.app.remix.StemBoardState
import com.pocketstudio.app.remix.StemBoardUi
import com.pocketstudio.app.remix.StemPeaks
import com.pocketstudio.app.remix.StemSelection
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/** « Tableau des pistes » (Remix): the four stems' curves, a stretch of the voice chosen and playing (app/screenshots/). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
open class StemBoardScreenshotTest : ScreenshotTest() {
    override fun wanted(name: String) = name in SHOTS

    /** A song of 3:20 at 124 BPM: drums all along, the bass from bar 9, the voice in phrases with silences between. */
    private val frames = 200L * BOARD_RATE
    private val bar = 4 * 60.0 / 124 * BOARD_RATE

    private val peaks = StemPeaks { stem, fpb ->
        FloatArray(((frames + fpb - 1) / fpb).toInt()) { i ->
            val f = i.toDouble() * fpb
            val b = (f - 0.25 * BOARD_RATE) / bar
            val beatPhase = (b * 4) % 1.0
            when (stem) {
                0 -> (0.25 + 0.75 * (1 - beatPhase).let { it * it * it } * (0.8 + 0.2 * sin(i * 0.37))).toFloat()
                1 -> if (b < 8) 0.02f else (0.35 + 0.45 * abs(sin(b * PI * 2)) + 0.1 * sin(i * 0.11)).toFloat()
                2 -> (0.3 + 0.25 * abs(sin(b * PI / 8)) + 0.15 * abs(sin(i * 0.05))).toFloat()
                else -> {
                    // Two bars sung, one bar of silence.
                    val inPhrase = b >= 16 && (b % 3) < 2
                    if (!inPhrase) 0.015f else (0.35 + 0.55 * abs(sin(b * PI * 1.5)) * (0.7 + 0.3 * sin(i * 0.9))).toFloat()
                }
            }
        }
    }

    private fun ui(playing: StemSelection? = null, at: Long = -1L) =
        StemBoardUi("abc", "Ma chanson", frames, 124.0, 0.25, peaks, playing = playing, position = { at }, peaksContext = EmptyCoroutineContext)

    /** The whole song: four curves, bars on the ruler, how to choose a stretch. */
    @Test
    fun stemBoard() = shoot("stem_board") {
        StemBoard(ui(), NoStemBoardActions, onClose = {})
    }

    /** Zoomed in on the voice's first phrases: a stretch of two bars chosen, its handles, listening (green play head). */
    @Test
    fun stemBoardSelection() = shoot("stem_board_selection") {
        val state = StemBoardState(frames)
        val start = Math.round(0.25 * BOARD_RATE + 16 * bar)
        val end = Math.round(0.25 * BOARD_RATE + 18 * bar)
        val sel = StemSelection("abc", 3, start, end)
        StemBoard(ui(playing = sel, at = start + (end - start) / 3), NoStemBoardActions, onClose = {}, state = state)
        androidx.compose.runtime.LaunchedEffect(state.width) {
            if (state.width > 0f && !state.hasSelection) {
                state.zoomBy((frames / (6 * bar)).toFloat(), 0f)
                state.scrollBy(-state.x(start - Math.round(bar)))
                state.select(3, start, end)
            }
        }
    }

    /** The Remix card of the song in the project, with « Ouvrir le tableau des pistes ». */
    @Test
    fun remixBoardButton() = shoot("remix_board_button") {
        val set = com.pocketstudio.app.remix.StemSet(
            File("/tmp/stems/abc"), "Ma chanson", 200.0, 0L, com.pocketstudio.app.remix.SongInfo(124.0, 0.25, 9, true),
        )
        com.pocketstudio.app.remix.RemixScreen(
            com.pocketstudio.app.remix.RemixUi(
                true, com.pocketstudio.app.remix.SeparationState.Idle, listOf(set),
                remix = com.pocketstudio.model.Remix("abc", "Ma chanson", 124.0, 0.25, startBar = 8),
                projectBpm = 124.0,
                overview = com.pocketstudio.app.rhythm.StemOverview(List(4) { st -> peaks.peaks(st, (frames / 400).toInt()) }, frames),
                board = ui(),
            ),
            com.pocketstudio.app.remix.RemixActions(),
        )
    }

    companion object {
        val SHOTS = setOf("stem_board", "stem_board_selection", "remix_board_button")
    }
}

/** The board on Benjamin's tablet lying down (926 x 472 dp left once the bars are taken): the actions on the side. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1006dp-h600dp-land-tvdpi")
class StemBoardTabletScreenshotTest : StemBoardScreenshotTest() {
    override val prefix = "tablet8/land_"
    override fun wanted(name: String) = name == "stem_board_selection"
    override val chrome = androidx.compose.foundation.layout.PaddingValues(start = 80.dp, top = 80.dp, bottom = 48.dp)
}
