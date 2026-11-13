package com.pocketstudio.app.tuto

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

/**
 * Tour 6, N13: at the course's end « Fermer le tuto » was grey words with no frame under two framed buttons:
 * nothing said it could be touched. It is drawn as a button, framed like its neighbours.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h852dp-xxhdpi")
class Tour6TutoPanelCloseTest {
    @get:Rule
    val compose = createComposeRule()

    private fun far(a: Color, b: Color) = abs(a.red - b.red) + abs(a.green - b.green) + abs(a.blue - b.blue) > 0.12f

    /** Whether [tag]'s left edge, at its middle height, is drawn (a frame or a fill) over the panel's colour beside it. */
    private fun framed(tag: String): Boolean {
        val px = compose.onRoot().captureToImage().toPixelMap()
        val b = compose.onNodeWithTag(tag, useUnmergedTree = true).getBoundsInRoot()
        val d = compose.density.density
        val left = (b.left.value * d).toInt()
        val y = ((b.top.value + b.bottom.value) / 2 * d).toInt()
        val outside = px[(left - (4 * d).toInt()).coerceAtLeast(0), y]
        return (left until left + (3 * d).toInt()).any { x -> far(px[x, y], outside) }
    }

    @Test
    fun closeTheTutorialIsADrawnButton() {
        val chapters = TutoCourse.of("techno")!!.chaptersFor(TutoLevel.Facile)
        val all = chapters.flatMap { it.steps }.map { it.id }.toSet()
        val ui = TutoUi(TutoProgress("techno", TutoLevel.Facile, done = all, current = chapters.last().steps.last().id), chapters, chapters.lastIndex, chapters.last().steps.lastIndex)
        assertTrue(ui.finished)
        compose.setContent { PocketStudioTheme { TutoPanel(ui, canShow = false, actions = TutoActions(), reducedMotion = true, mine = true) } }
        compose.waitForIdle()
        assertTrue("its neighbour « Recommencer le tuto » is framed", framed("tuto_restart_finished"))
        assertTrue("« Fermer le tuto » has no frame: nothing says it can be touched", framed("tuto_end_close"))
    }
}
