package com.pocketstudio.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

/** What a [PanSlider] does with a value: tenths (or not), then the centre caught within [CENTRE_SNAP]. */
object PanSliderDefaults {
    /** Inside this distance from the centre a value is the centre (« Centre » is easy to find again). */
    const val CENTRE_SNAP = 0.05f

    /**
     * Space kept free on each side of the track: with a card's own padding the track starts
     * 40 dp or more from the screen's edge, out of the system's back-gesture band.
     */
    val Margin: Dp = 28.dp

    /** How far from the thumb's centre a finger still takes it (a 48 dp target). */
    val ThumbReach: Dp = 24.dp

    /** [v] in -1..1, rounded to [step] when > 0, the centre caught within [CENTRE_SNAP]. */
    fun shape(v: Float, step: Float = 0f): Float {
        var r = if (v.isFinite()) v.coerceIn(-1f, 1f) else 0f
        if (step > 0f) r = ((r / step).roundToInt() * step).coerceIn(-1f, 1f)
        return if (abs(r) < CENTRE_SNAP) 0f else r
    }
}

/**
 * A pan (or any -1..1 value) as a slider that cannot be moved by accident from the screen's edge.
 *
 * Benjamin's kick ended hard left: a back swipe from the left edge landed on a full-width
 * Material slider, which jumps to the finger as it goes down (its left end, -1) before the
 * system takes the gesture. Here:
 * - the track keeps [margin] free on each side and asks the system not to take gestures over
 *   itself ([systemGestureExclusion]);
 * - a drag moves the value only when it starts on the thumb (never a jump under the finger);
 *   a plain touch elsewhere (down and up, no slide) puts the thumb there;
 * - a gesture the system takes back (a cancel) puts the value back where it started;
 * - the centre is caught within ±0.05, and two touches bring the thumb back to the centre;
 * - [onGestureEnd] once per gesture (an undo step, a journal line), after its last change.
 */
@Composable
fun PanSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onGestureEnd: () -> Unit = {},
    /** 0.1: tenths (« G 30 »); 0: any value. */
    step: Float = 0f,
    enabled: Boolean = true,
    accent: Color = Neon.Cyan,
    margin: Dp = PanSliderDefaults.Margin,
    /** What it sets, for TalkBack (« Pan du pad »). */
    label: String? = null,
    /** Its value in words, for TalkBack (« G 30 », « Centre ») rather than « 35 % ». */
    stateLabel: ((Float) -> String)? = null,
) {
    val current by rememberUpdatedState(value)
    val keyed = remember { floatArrayOf(value) }
    keyed[0] = value
    val change by rememberUpdatedState(onValueChange)
    val end by rememberUpdatedState(onGestureEnd)
    var active by remember { mutableStateOf(false) }
    val grow by animateFloatAsState(if (active) 1.25f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "pan-thumb")
    val ink = if (enabled) accent else Neon.TextSecondary.copy(alpha = 0.5f)
    val rail = Neon.SurfaceHigh
    val steps = if (step > 0f) (2f / step).roundToInt() - 1 else 0
    Box(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = margin)
            // A hardware keyboard (or a switch): ← → move it a step, the slider takes the focus.
            .onKeyEvent { e ->
                if (!enabled || e.type != KeyEventType.KeyDown) return@onKeyEvent false
                val by = if (step > 0f) step else 0.05f
                val dir = when (e.key) {
                    Key.DirectionLeft, Key.Minus -> -1
                    Key.DirectionRight, Key.Plus, Key.Equals -> 1
                    else -> return@onKeyEvent false
                }
                // Keys pressed faster than a frame add up (the value given back comes a frame later).
                val v = PanSliderDefaults.shape(keyed[0] + dir * by, step)
                if (v != keyed[0]) change(v)
                keyed[0] = v
                end()
                true
            }
            .focusable(enabled)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(value.coerceIn(-1f, 1f), -1f..1f, steps)
                label?.let { contentDescription = it }
                stateLabel?.let { stateDescription = it(value) }
                if (!enabled) disabled()
                if (enabled) setProgress { target ->
                    change(PanSliderDefaults.shape(target, step))
                    end()
                    true
                }
            }
            // Android 10+: the system leaves this strip's touches to the app (at most 200 dp of
            // each edge's height can be excluded: a 48 dp slider is well inside).
            .systemGestureExclusion()
            .pointerInput(enabled, step) {
                if (!enabled) return@pointerInput
                var lastTapAt = 0L
                var lastTapX = -1f
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val width = size.width.toFloat().coerceAtLeast(1f)
                    fun valueAt(x: Float) = PanSliderDefaults.shape(x / width * 2f - 1f, step)
                    val start = current
                    val thumbX = (start.coerceIn(-1f, 1f) + 1f) / 2f * width
                    val onThumb = abs(down.position.x - thumbX) <= PanSliderDefaults.ThumbReach.toPx()
                    val grab = down.position.x - thumbX
                    val slop = viewConfiguration.touchSlop
                    var dragging = false
                    var moved = false
                    var changed = false
                    var last = start
                    var cancelled = false
                    var upAt = down.uptimeMillis
                    var upX = down.position.x
                    if (onThumb) active = true
                    while (true) {
                        val event = awaitPointerEvent()
                        val c = event.changes.firstOrNull { it.id == down.id }
                        if (c == null) {
                            cancelled = true
                            break
                        }
                        if (!c.pressed) {
                            // Compose turns a cancel (the system took the gesture) into an up already consumed.
                            cancelled = c.isConsumed
                            upAt = c.uptimeMillis
                            upX = c.position.x
                            break
                        }
                        val dx = c.position.x - down.position.x
                        val dy = c.position.y - down.position.y
                        if (!moved && (abs(dx) > slop || abs(dy) > slop)) {
                            moved = true
                            // Mostly sideways from the thumb: the thumb follows; anything else
                            // (the page scrolled, a swipe from the edge) is not for the slider.
                            dragging = onThumb && abs(dx) >= abs(dy)
                            if (!dragging) active = false
                        }
                        if (!dragging && c.isConsumed) {
                            // Someone else (the page's scroll) took it.
                            moved = true
                        }
                        if (dragging) {
                            val v = valueAt(c.position.x - grab)
                            if (v != last) {
                                change(v)
                                last = v
                                changed = true
                            }
                            c.consume()
                        }
                    }
                    active = false
                    when {
                        cancelled -> {
                            if (changed) {
                                change(start)
                                end()
                            }
                        }
                        dragging -> end()
                        !moved -> {
                            val again = upAt - lastTapAt <= viewConfiguration.doubleTapTimeoutMillis &&
                                abs(upX - lastTapX) <= PanSliderDefaults.ThumbReach.toPx() * 2
                            val v = when {
                                again -> 0f
                                // A touch on the thumb leaves it where it is (only two touches move it: to the centre).
                                onThumb -> start
                                else -> valueAt(upX)
                            }
                            lastTapAt = if (again) 0L else upAt
                            lastTapX = upX
                            if (v != start) change(v)
                            end()
                        }
                    }
                }
            },
    ) {
        Canvas(Modifier.fillMaxWidth().height(48.dp)) {
            val track = 6.dp.toPx()
            val y = size.height / 2f
            val r = CornerRadius(track / 2f)
            drawRoundRect(color = rail, topLeft = Offset(0f, y - track / 2f), size = Size(size.width, track), cornerRadius = r)
            val v = value.coerceIn(-1f, 1f)
            val mid = size.width / 2f
            val x = (v + 1f) / 2f * size.width
            // Filled from the centre to the thumb: left or right shows at a glance.
            if (v != 0f) {
                val left = minOf(mid, x)
                val w = abs(x - mid)
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(lerp(ink, Color.Black, 0.25f), ink), startX = left, endX = left + w),
                    topLeft = Offset(left, y - track / 2f), size = Size(w, track), cornerRadius = r,
                )
            }
            // The centre's mark.
            drawRoundRect(
                color = Color.White.copy(alpha = 0.55f), topLeft = Offset(mid - 1.dp.toPx(), y - 7.dp.toPx()),
                size = Size(2.dp.toPx(), 14.dp.toPx()), cornerRadius = CornerRadius(1.dp.toPx()),
            )
            val thumb = 9.dp.toPx() * grow
            drawCircle(ink.copy(alpha = if (active) 0.35f else 0.18f), radius = thumb * 1.8f, center = Offset(x, y))
            drawCircle(Brush.radialGradient(listOf(Color.White, lerp(ink, Color.White, 0.35f)), center = Offset(x, y), radius = thumb), radius = thumb, center = Offset(x, y))
            drawCircle(ink, radius = thumb, center = Offset(x, y), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
        }
    }
}
