package com.pocketstudio.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/** Colours of a [Knob]: hardware-style panels pass their own. */
class KnobColors(
    val cap: Color,
    val capEdge: Color,
    val pointer: Color,
    val track: Color,
    val value: Color,
    val label: Color,
)

/**
 * A rotary knob (potard), 0..1 over a 270° sweep. Drag up or down to turn it (the whole sweep
 * is about 200 dp of finger travel), double-tap to put it back to [default].
 */
@Composable
fun Knob(
    value: Float,
    onValueChange: (Float) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    default: Float = 0.5f,
    size: Dp = 64.dp,
    colors: KnobColors,
    valueText: String? = null,
) {
    val current by rememberUpdatedState(value)
    val change by rememberUpdatedState(onValueChange)
    val haptics = LocalHapticFeedback.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Canvas(
            Modifier.size(size)
                .semantics {
                    contentDescription = label
                    progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f)
                    setProgress { v -> change(v.coerceIn(0f, 1f)); true }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        change(default)
                    })
                }
                .pointerInput(Unit) {
                    val travel = 200.dp.toPx()
                    var v = 0f
                    detectVerticalDragGestures(onDragStart = { v = current }) { c, dy ->
                        c.consume()
                        v = (v - dy / travel).coerceIn(0f, 1f)
                        change(v)
                    }
                },
        ) {
            val r = this.size.minDimension / 2f
            val c = Offset(this.size.width / 2f, this.size.height / 2f)
            val stroke = r * 0.12f
            val arc = Size(2 * (r - stroke / 2), 2 * (r - stroke / 2))
            val arcTopLeft = Offset(c.x - arc.width / 2, c.y - arc.height / 2)
            // Scale: grey track, value in colour, from 7:30 to 4:30 o'clock.
            drawArc(colors.track, 135f, 270f, false, arcTopLeft, arc, style = Stroke(stroke, cap = StrokeCap.Round))
            if (value > 0.005f) {
                drawArc(colors.value, 135f, 270f * value, false, arcTopLeft, arc, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            // The cap: a ribbed rim and a slightly lit top, like a moulded knob.
            val capR = r * 0.72f
            drawCircle(Color.Black.copy(alpha = 0.25f), capR, c + Offset(0f, r * 0.06f))
            drawCircle(colors.capEdge, capR, c)
            drawCircle(
                Brush.radialGradient(listOf(colors.cap.copy(alpha = 1f).compositeOver(Color.White, 0.18f), colors.cap), c - Offset(capR * 0.3f, capR * 0.3f), capR * 1.2f),
                capR * 0.84f, c,
            )
            val angle = Math.toRadians(135.0 + 270.0 * value)
            val dir = Offset(cos(angle).toFloat(), sin(angle).toFloat())
            drawLine(colors.pointer, c + dir * (capR * 0.25f), c + dir * (capR * 0.8f), strokeWidth = r * 0.1f, cap = StrokeCap.Round)
        }
        Text(label, color = colors.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
        if (valueText != null) Text(valueText, color = colors.label.copy(alpha = 0.7f), fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1)
    }
}

private fun Color.compositeOver(other: Color, amount: Float) = Color(
    red = red + (other.red - red) * amount,
    green = green + (other.green - green) * amount,
    blue = blue + (other.blue - blue) * amount,
    alpha = alpha,
)
