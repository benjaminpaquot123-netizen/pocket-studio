package com.pocketstudio.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The three waves of the synth and the 303 (decision of 2026-11-04, « Scie / Triangle / Carré »): a small
 * drawing of each beside the sound, the same in the Mode simple (with its ear word: « Brillant », « Creux »,
 * « Doux ») and in the Pro (with the wave's name). A vector of two periods, drawn in the theme's colour.
 */
enum class WaveShape {
    Saw, Square, Triangle;

    /** Two periods in a 24 x 24 box, as a line (the stroke takes the tint). */
    val icon: ImageVector by lazy {
        ImageVector.Builder(name = "wave_$name", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                when (this@WaveShape) {
                    Saw -> { moveTo(2f, 17f); lineTo(12f, 7f); lineTo(12f, 17f); lineTo(22f, 7f); lineTo(22f, 17f) }
                    Square -> { moveTo(2f, 17f); lineTo(2f, 7f); lineTo(7f, 7f); lineTo(7f, 17f); lineTo(12f, 17f); lineTo(12f, 7f); lineTo(17f, 7f); lineTo(17f, 17f); lineTo(22f, 17f) }
                    Triangle -> { moveTo(2f, 17f); lineTo(7f, 7f); lineTo(12f, 17f); lineTo(17f, 7f); lineTo(22f, 17f) }
                }
            }
        }.build()
    }

    companion object {
        /** The synth's oscillator wave (0: saw, 0.5: square, 1: triangle), as its knob reads. */
        fun ofSynth(wave: Float): WaveShape = if (wave < 0.25f) Saw else if (wave < 0.75f) Square else Triangle

        /** The 303's (below 0.5: saw, else square). */
        fun ofAcid(waveform: Float): WaveShape = if (waveform < 0.5f) Saw else Square
    }
}

/** [shape]'s drawing, decorative: its word beside it is what TalkBack reads. */
@Composable
fun WaveGlyph(shape: WaveShape, modifier: Modifier = Modifier, tint: Color = Neon.TextSecondary, size: Dp = 18.dp) {
    Icon(shape.icon, contentDescription = null, tint = tint, modifier = modifier.size(size))
}
