package com.pocketstudio.app.remix

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.pocketstudio.app.ScreenshotTest
import com.pocketstudio.app.library.MINE
import com.pocketstudio.app.library.ShelfBrowser
import com.pocketstudio.app.library.ShelfBrowserActions
import com.pocketstudio.app.library.ShelfBrowserUi
import com.pocketstudio.app.library.SoundOption
import com.pocketstudio.app.library.SoundRef
import com.pocketstudio.model.CutPiece
import com.pocketstudio.model.SongCuts
import com.pocketstudio.model.StemCuts
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Lot C, n° 24: « Découper la chanson » as on the mockup (docs/maquettes/decoupe-deux.png): the
 * voice of a 2:25 song cut in six pieces over bars 13 to 21, three ticked; a cut moved with its
 * magnifier; « Ma bibliothèque » filed by song in the one browser (app/screenshots/).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class CutScreenshotTest : ScreenshotTest() {
    override fun wanted(name: String) = name in SHOTS

    private val frames = 145L * BOARD_RATE
    private val bpm = 113.0
    private val bar = 4 * 60.0 / bpm * BOARD_RATE

    /** A voice singing in phrases (two bars on, one off from bar 5), the other stems all along. */
    private fun level(stem: Int, f: Long): Float {
        val b = f / bar
        val i = f / 480
        return when (stem) {
            3 -> if (b < 4 || (b % 3) >= 2.2) 0.01f else (0.3 + 0.6 * abs(sin(b * PI * 3.1)) * (0.6 + 0.4 * sin(i * 0.7))).toFloat()
            0 -> (0.3 + 0.7 * (1 - ((b * 4) % 1.0)).let { it * it * it }).toFloat()
            else -> (0.3 + 0.3 * abs(sin(b * PI / 4)) + 0.1 * abs(sin(i * 0.05))).toFloat()
        }
    }

    private val peaks = StemPeakSource { stem ->
        StemPeaks.of(frames, 1) { start, count -> FloatArray(count) { k -> val f = start + k; if (f % 480 == 0L) level(stem, f) else 0f } }
    }

    private fun ui() = StemBoardUi("boys", "ADÉLA - Boys", frames, bpm, 0.0, peaks, peaksContext = EmptyCoroutineContext)

    /** Six pieces between bars 13 and 21 (and the song's other phrases), the first, second and fourth ticked. */
    private val cuts = run {
        fun at(b: Double) = Math.round(b * bar)
        val inView = listOf(12.4 to 13.6, 13.6 to 14.7, 14.7 to 15.7, 15.7 to 17.0, 17.0 to 18.3, 18.3 to 19.4)
        val pieces = (listOf(4.0 to 6.2, 7.0 to 9.2) + inView + listOf(22.0 to 24.2)).map { (a, z) -> CutPiece(at(a), at(z)) }
        SongCuts("boys", listOf(StemCuts(3, pieces, chosen = listOf(2, 3, 5), auto = "Phrases")))
    }

    private class Shown(private val cuts: SongCuts) : StemBoardActions by NoStemBoardActions {
        override val state = MutableStateFlow(StemBoardState())
        override fun savedCuts(setId: String, frames: Long) = cuts
    }

    @androidx.compose.runtime.Composable
    private fun screen(moving: Boolean) {
        val actions = remember { Shown(cuts) }
        val model = remember { CutModel("boys", frames, bpm, 0.0, cuts) }
        val view = rememberStemBoardView(ui())
        CutScreen(ui(), actions, onClose = {}, model = model, view = view)
        LaunchedEffect(view.width) {
            if (view.width > 0f && !view.zoomedIn) {
                view.zoomBy((frames / (7.2 * bar)).toFloat(), 0f)
                view.scrollBy(-view.x(Math.round(12.2 * bar)))
                model.cursor = Math.round(14.2 * bar)
                if (moving) {
                    val line = model.pieces[3].start
                    model.beginMove(line, 4_000)
                    model.moveTo(line + Math.round(bar / 4))
                }
            }
        }
    }

    @Test
    fun cutScreen() = shoot("cut_screen") { screen(moving = false) }

    @Test
    fun cutScreenMagnifier() = shoot("cut_screen_magnifier") { screen(moving = true) }

    /** « Ma bibliothèque » of the one browser: a folder per song the sounds were kept from. */
    @Test
    fun libraryMineFolders() = shoot("library_mine_folders") {
        val origin = mapOf("1" to "ADÉLA - Boys", "2" to "ADÉLA - Boys", "3" to "ADÉLA - Boys", "4" to "Mon acidcore", "5" to "")
        val mine = listOf("Voix · ADÉLA - Boys", "Voix · ADÉLA - Boys 2", "Voix · ADÉLA - Boys 3", "Kick de ma voix", "Cri").mapIndexed { i, n ->
            SoundOption(SoundRef.Mine("${i + 1}"), n)
        }
        ShelfBrowser(
            ShelfBrowserUi(emptyList(), "vocal", tag = "library", start = MINE, mine = mine, mineGroupOf = { origin[(it.ref as SoundRef.Mine).id] }),
            ShelfBrowserActions(),
        )
    }

    companion object {
        val SHOTS = setOf("cut_screen", "cut_screen_magnifier", "library_mine_folders")
    }
}
