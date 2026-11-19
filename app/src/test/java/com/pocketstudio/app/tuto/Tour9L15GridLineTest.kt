package com.pocketstudio.app.tuto

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 9, lot L15, the tutorial's line over the drum grid (TutoGridLine):
 * - F048: the pad being edited is on the line right over the cells, even while a message shows there (at 130 % its
 *   first line, « Compás 7/8 · Palmada », went under the screen's fixed head and only the message stayed in sight);
 * - F050: every word of it reads in full at 200 % on a 360 dp phone (no « … », no text squeezed or cut in height).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp")
class Tour9L15GridLineTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun tidy() {
        RuntimeEnvironment.setFontScale(1f)
        TutoHints.reset()
    }

    private fun line(here: Int, elsewhere: Pair<Int, Int>?, erasing: Boolean = false, bars: Int = 8) = compose.setContent {
        PocketStudioTheme {
            // The card's inner width on a 360 dp phone (16 dp of page and of card on each side).
            Box(Modifier.width(296.dp)) {
                TutoGridLine(bar = 6, bars = bars, pad = "Claquement de mains", here = here, elsewhere = elsewhere, go = {}, copy = null, erasing = erasing)
            }
        }
    }

    private fun statusText(): String =
        compose.onNodeWithTag("tuto_grid_status", useUnmergedTree = true).fetchSemanticsNode().config
            .getOrNull(androidx.compose.ui.semantics.SemanticsProperties.Text)?.joinToString { it.text }.orEmpty()

    @Test fun f048PadOverTheCellsWithItsCount() {
        line(here = 2, elsewhere = null)
        assertTrue(statusText(), statusText().contains("Claquement de mains"))
    }

    @Test fun f048PadOverTheCellsWhileErasing() {
        line(here = 2, elsewhere = null, erasing = true)
        assertTrue(statusText(), statusText().contains("Claquement de mains"))
    }

    @Test fun f048PadOverTheCellsWhileAMessageShows() {
        TutoHints.banner = tutoBarBanner(6, forward = true)
        line(here = 2, elsewhere = null)
        assertTrue(statusText(), statusText().contains("Claquement de mains") && statusText().contains(tutoBarBanner(6, forward = true)))
    }

    private fun cutWords(): List<String> =
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true).fetchSemanticsNodes().mapNotNull { n ->
            val layouts = mutableListOf<TextLayoutResult>()
            n.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
            val r = layouts.firstOrNull() ?: return@mapNotNull null
            // Squeezed: a word split over two lines. (didOverflowWidth is left out: under Robolectric it is true for a
            // one-line text far narrower than its room.)
            val words = r.layoutInput.text.text.split(' ').filter { it.isNotBlank() }
            val split = words.any { w -> (0 until r.lineCount).none { r.layoutInput.text.text.substring(r.getLineStart(it), r.getLineEnd(it)).contains(w) } }
            if ((0 until r.lineCount).any { r.isLineEllipsized(it) } || r.didOverflowHeight || split) "«${r.layoutInput.text}» ${r.size} lines=${r.lineCount} ell=${(0 until r.lineCount).any { r.isLineEllipsized(it) }} oh=${r.didOverflowHeight} split=$split c=${r.layoutInput.constraints}" else null
        }

    @Test fun f050ReadsInFullAt200WithCellsHere() {
        RuntimeEnvironment.setFontScale(2f)
        line(here = 3, elsewhere = null)
        assertTrue(cutWords().toString(), cutWords().isEmpty())
    }

    @Test fun f050ReadsInFullAt200WithCellsElsewhere() {
        RuntimeEnvironment.setFontScale(2f)
        line(here = 0, elsewhere = 3 to 5)
        assertTrue(cutWords().toString(), cutWords().isEmpty())
    }

    @Test fun f050ReadsInFullAt200WhileErasing() {
        RuntimeEnvironment.setFontScale(2f)
        line(here = 4, elsewhere = null, erasing = true)
        assertTrue(cutWords().toString(), cutWords().isEmpty())
    }
}
