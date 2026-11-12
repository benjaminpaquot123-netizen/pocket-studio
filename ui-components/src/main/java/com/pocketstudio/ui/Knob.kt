package com.pocketstudio.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
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
import kotlinx.coroutines.launch
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
 * is about 200 dp of finger travel), double-tap to put it back to [default]. Under the finger the
 * cap sinks a little; turning it ticks lightly at [default] and at both ends, as a notched pot.
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
    val haptics by rememberUpdatedState(LocalHapticFeedback.current)
    val notchAt by rememberUpdatedState(default)
    val reduced = LocalReducedMotion.current
    // 0 at rest, 1 under the finger: read only where the cap is drawn.
    val held = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val resetLabel = com.pocketstudio.model.i18n.tr("Remettre comme au départ")
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(0.dp)) {
        val shown = rememberUpdatedState(value)
        val face = remember(colors.cap, colors.capEdge, colors.pointer, colors.track, colors.value) { knobFace(colors, shown, held.asState()) }
        Spacer(
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
                .pointerInput(reduced) {
                    if (reduced) return@pointerInput
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        scope.launch { held.animateTo(1f, StudioMotion.pressIn()) }
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Final)
                        } while (event.changes.any { it.pressed })
                        scope.launch { held.animateTo(0f, StudioMotion.pressOut()) }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = {
                        haptics.hold()
                        change(default)
                    })
                }
                .pointerInput(Unit) {
                    val travel = 200.dp.toPx()
                    var v = 0f
                    detectVerticalDragGestures(onDragStart = { v = current }) { c, dy ->
                        c.consume()
                        val before = v
                        v = (v - dy / travel).coerceIn(0f, 1f)
                        if (passesNotch(before, v, notchAt) || passesNotch(before, v, 0f) || passesNotch(before, v, 1f)) haptics.notch()
                        change(v)
                    }
                }
                .then(face),
        )
        // A narrow knob (a tablet pane, a big interface size): the label shrinks a little rather
        // than lose its last letters (« Accordag »).
        WholeWordsText(
            label, color = colors.label, style = androidx.compose.ui.text.TextStyle(fontSize = 12.sp), fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center, maxLines = 1, minScale = 0.75f,
        )
        // Figures of one width: the value does not jiggle while the knob turns.
        if (valueText != null) Text(valueText, color = colors.value, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 1, style = KnobValueStyle)
    }
}

/** The value under a knob: tabular figures. */
private val KnobValueStyle = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")

/**
 * A knob's face, as on studio gear: its scale (grey track, value in colour, from 7:30 to 4:30
 * o'clock), then a cap that stands out (a soft shadow under it, lit from above, a bright rim on
 * its upper edge) with its pointer. Brushes are made when the size changes; turning the knob
 * only reads [value] while drawing.
 */
private fun knobFace(colors: KnobColors, value: State<Float>, held: State<Float>): Modifier = Modifier.drawWithCache {
    val r = size.minDimension / 2f
    val c = Offset(size.width / 2f, size.height / 2f)
    val stroke = r * 0.12f
    val arc = Size(2 * (r - stroke / 2), 2 * (r - stroke / 2))
    val arcTopLeft = Offset(c.x - arc.width / 2, c.y - arc.height / 2)
    val scale = Stroke(stroke, cap = StrokeCap.Round)
    val capR = r * 0.68f
    val shadow = Brush.radialGradient(
        0.0f to Color.Black.copy(alpha = 0.42f), 0.72f to Color.Black.copy(alpha = 0.22f), 1f to Color.Transparent,
        center = c + Offset(0f, capR * 0.12f), radius = capR * 1.16f,
    )
    val rim = Brush.verticalGradient(
        listOf(lerp(colors.capEdge, Color.White, 0.30f), colors.capEdge, lerp(colors.capEdge, Color.Black, 0.25f)),
        startY = c.y - capR, endY = c.y + capR,
    )
    val cap = Brush.verticalGradient(
        listOf(lerp(colors.cap, Color.White, 0.10f), colors.cap, lerp(colors.cap, Color.Black, 0.10f)),
        startY = c.y - capR, endY = c.y + capR,
    )
    val shine = Stroke(r * 0.035f, cap = StrokeCap.Round)
    val shineR = capR * 0.80f
    val shineSize = Size(shineR * 2, shineR * 2)
    val shineTopLeft = Offset(c.x - shineR, c.y - shineR)
    val shineColor = Color.White.copy(alpha = 0.16f)
    onDrawBehind {
        val v = value.value
        drawArc(colors.track, 135f, 270f, false, arcTopLeft, arc, style = scale)
        if (v > 0.005f) drawArc(colors.value, 135f, 270f * v, false, arcTopLeft, arc, style = scale)
        // The cap sinks a touch while a finger is on it (D1).
        scale(1f - (1f - StudioMotion.PRESSED_SCALE) * held.value, c) {
            drawCircle(shadow, capR * 1.16f, c + Offset(0f, capR * 0.12f))
            drawCircle(rim, capR, c)
            drawCircle(cap, capR * 0.88f, c)
            // Light from above on the cap's upper edge.
            drawArc(shineColor, 215f, 110f, false, shineTopLeft, shineSize, style = shine)
            val angle = Math.toRadians(135.0 + 270.0 * v)
            val dir = Offset(cos(angle).toFloat(), sin(angle).toFloat())
            drawLine(colors.pointer, c + dir * (capR * 0.25f), c + dir * (capR * 0.8f), strokeWidth = r * 0.1f, cap = StrokeCap.Round)
        }
    }
}
