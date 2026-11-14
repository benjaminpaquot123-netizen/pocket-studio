package com.pocketstudio.app.ui

import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.getBoundsInRoot
import com.pocketstudio.ui.Palette
import com.pocketstudio.ui.Palettes
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Audit ultra round 2, n° 30 (seat B d, night theme): ↶ « Annuler » with nothing to undo was dark grey
 * on black, unreadable. Switched off, the thumb's words stay dimmed but readable (3:1 at least) in every theme.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class ThumbOffReadableTest {
    @get:Rule
    val compose = createComposeRule()

    private fun lum(c: androidx.compose.ui.graphics.Color): Double {
        fun ch(v: Float): Double = if (v <= 0.03928f) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        return 0.2126 * ch(c.red) + 0.7152 * ch(c.green) + 0.0722 * ch(c.blue)
    }

    /** The undo's word as drawn: the bar's colour behind it, the strongest 2 % of its pixels for the letters. */
    private fun undoContrast(palette: Palette): Double {
        compose.setContent {
            PocketStudioTheme(palette) {
                ThumbBar(ThumbBarUi(playing = false, canUndo = false, canRedo = false, tempoBpm = 120, redoAlways = false), ThumbBarActions({}, {}, {}, {}, null, {}))
            }
        }
        compose.waitForIdle()
        val px = compose.onRoot().captureToImage().toPixelMap()
        val b = compose.onNodeWithTag("thumb_undo").getBoundsInRoot()
        val d = compose.density.density
        // The lower half of the button: its word (the arrow above it).
        val left = (b.left.value * d).toInt(); val right = (b.right.value * d).toInt()
        val top = ((b.top.value + b.bottom.value) / 2 * d).toInt(); val bottom = (b.bottom.value * d).toInt()
        val pixels = (top until bottom).flatMap { y -> (left until right).map { x -> px[x, y] } }
        val bg = pixels.groupingBy { it }.eachCount().maxBy { it.value }.key
        val lb = lum(bg)
        val far = pixels.map { (max(lum(it), lb) + 0.05) / (min(lum(it), lb) + 0.05) }.sortedDescending()
        return far[(far.size * 0.02).toInt()]
    }

    @Test
    fun theNightThemesUndoReads() {
        val night = undoContrast(Palettes.Dark)
        assertTrue("night: %.2f:1".format(night), night >= 3.0)
    }

    @Test
    fun theStudioThemesUndoReads() {
        val studio = undoContrast(Palettes.Studio)
        assertTrue("studio: %.2f:1".format(studio), studio >= 3.0)
    }
}
