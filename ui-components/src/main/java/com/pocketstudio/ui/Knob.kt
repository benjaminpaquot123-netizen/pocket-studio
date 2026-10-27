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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.stateDescription
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
    /** A finger on the knob (true) and off it (false): see [holding]. */
    onHold: ((Boolean) -> Unit)? = null,
) {
    val current by rememberUpdatedState(value)
    val change by rememberUpdatedState(onValueChange)
    val haptics = LocalHapticFeedback.current
    val resetLabel = com.pocketstudio.model.i18n.tr("Remettre comme au départ")
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Canvas(
            Modifier.size(size)
                .holding(onHold)
                .semantics {
                    contentDescription = label
                    progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f)
                    // Audit final (22): the value as shown, and the double tap's reset as an action.
                    valueText?.let { stateDescription = it }
                    customActions = listOf(CustomAccessibilityAction(resetLabel) { change(default); true })
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
            // The cap: a flat disc with a thin rim.
            val capR = r * 0.68f
            drawCircle(colors.capEdge, capR, c)
            drawCircle(colors.cap, capR * 0.88f, c)
            val angle = Math.toRadians(135.0 + 270.0 * value)
            val dir = Offset(cos(angle).toFloat(), sin(angle).toFloat())
            drawLine(colors.pointer, c + dir * (capR * 0.25f), c + dir * (capR * 0.8f), strokeWidth = r * 0.1f, cap = StrokeCap.Round)
        }
        // A narrow knob (a tablet pane, a big interface size): the label shrinks a little rather
        // than lose its last letters (« Accordag »).
        WholeWordsText(
            label, color = colors.label, style = androidx.compose.ui.text.TextStyle(fontSize = 12.sp), fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center, maxLines = 1, minScale = 0.75f,
        )
        if (valueText != null) Text(valueText, color = colors.value, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 1)
    }
}
