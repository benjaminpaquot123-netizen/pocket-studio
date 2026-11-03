package com.pocketstudio.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * The one light of every pad and every cell (Benjamin's design rule, n° 25, 2026-11-01): Rythme's
 * pads and steps, the Live page's bricks, the live pads, Pad Live. Subtle and the same everywhere:
 * - **inactive**: nothing moves, ever (no wave, no breath, no blink): it draws nothing at all;
 * - **playing**: a soft light inside the pad and a fine ring along its edge that fills as its loop
 *   goes round ([progress]; still and full without one);
 * - **waiting** for its beat: the ring drawn faintly, still;
 * - **touched**: a small ripple from the finger, 200 ms, then nothing;
 * - **held effect** on it: a light veil of colour on its edges ([veil]);
 * - lot B3, **the light follows the sound** ([level], read when drawn): a playing pad's inner light
 *   is as bright as its sound is loud, from a dim glow to full (a pad that does not play: nothing).
 * Lighting up takes 150 ms, going out 250 ms. Everything that moves is read when drawn: a playing
 * pad redraws, it never recomposes; nothing is allocated per frame.
 */
@Stable
class PadLight internal constructor(private val scope: CoroutineScope) {
    internal val ripple = Animatable(1f)
    internal val at = mutableStateOf(Offset.Unspecified)

    /** A finger came down at [position] (in the pad): the 200 ms ripple. */
    fun touch(position: Offset) {
        at.value = position
        scope.launch {
            ripple.snapTo(0f)
            ripple.animateTo(1f, tween(RIPPLE_MS, easing = LinearEasing))
        }
    }

    companion object {
        const val RIPPLE_MS = 200
        const val LIGHT_IN_MS = 150
        const val LIGHT_OUT_MS = 250
    }
}

@Composable
fun rememberPadLight(): PadLight {
    val scope = rememberCoroutineScope()
    return remember(scope) { PadLight(scope) }
}

/**
 * The pad's light (see [PadLight]), drawn over what is before it in the chain (its fill) and under
 * its content (its name). [playing]: its sound plays; [waiting]: it waits for its beat; [progress]
 * (0..1, read when drawn): where its loop is; [veil]: a held effect works on it; [touchable]: the
 * finger's ripple (the touch is seen, never taken: the pad's own gestures stay as they are).
 */
@Composable
fun Modifier.padLight(
    light: PadLight,
    color: Color,
    playing: Boolean,
    waiting: Boolean = false,
    progress: (() -> Float)? = null,
    veil: Boolean = false,
    corner: Dp = 12.dp,
    touchable: Boolean = true,
    /** Lot B3: how loud the pad sounds now (0..1, read when drawn); null: its light stays full while it plays. */
    level: (() -> Float)? = null,
): Modifier {
    val lit = animateFloatAsState(
        if (playing) 1f else 0f,
        tween(if (playing) PadLight.LIGHT_IN_MS else PadLight.LIGHT_OUT_MS),
        label = "pad-light",
    )
    val veiled = animateFloatAsState(if (veil) 1f else 0f, tween(if (veil) PadLight.LIGHT_IN_MS else PadLight.LIGHT_OUT_MS), label = "pad-veil")
    val touch = if (!touchable) Modifier else Modifier.pointerInput(light) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            light.touch(down.position)
        }
    }
    return this.then(touch).drawWithCache {
        val w = size.width
        val h = size.height
        val stroke = 2.dp.toPx()
        val inset = stroke / 2 + 1.dp.toPx()
        val r = (corner.toPx() - inset).coerceAtLeast(0f)
        // The ring: from the top's middle, clockwise, along the rounded edge.
        val ring = Path().apply {
            val l = inset
            val t = inset
            val rr = w - inset
            val b = h - inset
            moveTo(w / 2, t)
            lineTo(rr - r, t)
            arcTo(Rect(rr - 2 * r, t, rr, t + 2 * r), -90f, 90f, false)
            lineTo(rr, b - r)
            arcTo(Rect(rr - 2 * r, b - 2 * r, rr, b), 0f, 90f, false)
            lineTo(l + r, b)
            arcTo(Rect(l, b - 2 * r, l + 2 * r, b), 90f, 90f, false)
            lineTo(l, t + r)
            arcTo(Rect(l, t, l + 2 * r, t + 2 * r), 180f, 90f, false)
            close()
        }
        val measure = PathMeasure().apply { setPath(ring, false) }
        val length = measure.length
        val part = Path()
        val ringStyle = Stroke(stroke, cap = StrokeCap.Round)
        val ringColor = lerp(color, Color.White, 0.55f)
        val inner = Brush.radialGradient(
            0f to lerp(color, Color.White, 0.5f).copy(alpha = 0.55f), 1f to Color.Transparent,
            center = Offset(w / 2, h * 0.45f), radius = max(w, h) * 0.62f,
        )
        val veilBrush = Brush.radialGradient(
            0.55f to Color.Transparent, 1f to lerp(color, Color.White, 0.25f).copy(alpha = 0.5f),
            center = Offset(w / 2, h / 2), radius = max(w, h) * 0.72f,
        )
        val rippleMax = max(w, h) * 0.9f
        val round = CornerRadius(corner.toPx())
        onDrawWithContent {
            val on = lit.value
            if (on > 0.004f) {
                val loud = level?.invoke()?.takeIf { it.isFinite() }?.let { padLightFollow(it) } ?: 1f
                drawRoundRect(inner, size = Size(w, h), cornerRadius = round, alpha = on * loud)
                val p = progress?.invoke()?.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 1f
                drawPath(ring, ringColor.copy(alpha = 0.18f * on), style = ringStyle)
                if (p > 0f) {
                    part.reset()
                    measure.getSegment(0f, length * p, part, true)
                    drawPath(part, ringColor.copy(alpha = 0.9f * on), style = ringStyle)
                }
            } else if (waiting) {
                drawPath(ring, ringColor.copy(alpha = 0.35f), style = ringStyle)
            }
            val v = veiled.value
            if (v > 0.004f) drawRoundRect(veilBrush, size = Size(w, h), cornerRadius = round, alpha = v)
            val t = light.ripple.value
            val from = light.at.value
            if (t < 1f && from.isSpecified) {
                drawCircle(Color.White.copy(alpha = 0.28f * (1f - t)), rippleMax * (0.15f + 0.85f * t), from)
            }
            drawContent()
        }
    }
}

/**
 * Lot B3: a pad's inner light for its sound's [level] (a peak, 0..1): never below a dim glow while
 * it plays (a quiet tail still shows it plays), full from about −6 dB; the ear's curve in between.
 */
fun padLightFollow(level: Float): Float {
    val l = level.coerceIn(0f, 1f)
    return (PAD_LIGHT_FLOOR + (1f - PAD_LIGHT_FLOOR) * kotlin.math.sqrt((l * 2f).coerceAtMost(1f))).coerceIn(PAD_LIGHT_FLOOR, 1f)
}

/** The dimmest a playing pad's inner light gets (lot B3). */
const val PAD_LIGHT_FLOOR = 0.3f
