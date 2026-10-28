package com.pocketstudio.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.floor
import kotlin.math.round

/**
 * Where a level bar turns amber and red, as fractions of its length (the middle of each blend):
 * a meter in decibels passes its own (the « −12 » and « −1 dBFS » of the Mix).
 */
class MeterZones(val amber: Float = 0.65f, val red: Float = 0.85f)

/**
 * Every level meter of the app, the same (owner's request): green, amber, red in a soft
 * gradient along the bar, its tip dissolving into fine pixels (an 8×8 ordered dither, sparser
 * towards the tip, fixed on the bar: it never shimmers), and the highest level lately held as a
 * fine line that falls back gently ([rememberPeakHold]). Drawn in any Canvas ([draw]): a meter
 * beside a fader draws it in the fader's own canvas. Its gradient and clip are made when the
 * bar's place changes, never per frame; a frame only draws rectangles.
 */
@Stable
class LevelBar internal constructor(
    private val low: Color,
    private val mid: Color,
    private val high: Color,
    private val zones: MeterZones,
    /** The side of a pixel, and the length over which the tip dissolves, in pixels. */
    private val pixel: Float,
    private val dissolve: Float,
) {
    private var brush: Brush? = null
    private val clip = Path()
    private var key0 = Float.NaN
    private var key1 = Float.NaN
    private var key2 = Float.NaN
    private var key3 = Float.NaN
    private var keyVertical = false

    /** The blend's stops along the bar (0 = its start, the quiet end). */
    private val blend = 0.07f
    private val s1 = (zones.amber - blend).coerceIn(0f, 1f)
    private val s2 = (zones.amber + blend).coerceIn(s1, 1f)
    private val s3 = (zones.red - blend).coerceIn(s2, 1f)
    private val s4 = (zones.red + blend).coerceIn(s3, 1f)

    /** The colour at [f] along the bar (0..1): the same as the gradient's. */
    fun colorAt(f: Float): Color = when {
        f <= s1 -> low
        f < s2 -> mix(low, mid, (f - s1) / (s2 - s1))
        f <= s3 -> mid
        f < s4 -> mix(mid, high, (f - s3) / (s4 - s3))
        else -> high
    }

    /** Straight sRGB blend, as the gradient's (no colour-space conversion, nothing allocated). */
    private fun mix(a: Color, b: Color, t: Float): Color = Color(
        a.red + (b.red - a.red) * t, a.green + (b.green - a.green) * t, a.blue + (b.blue - a.blue) * t, a.alpha + (b.alpha - a.alpha) * t,
    )

    /**
     * The bar at [level] (0..1 of its length) in the rectangle [topLeft] [size], lengthwise up from
     * the bottom when [vertical], else from the start; [peak] (0..1, negative: none) as a fine line.
     * The track under it is the caller's.
     */
    fun draw(scope: DrawScope, level: Float, topLeft: Offset, size: Size, vertical: Boolean, peak: Float = -1f) {
        if (size.width <= 0f || size.height <= 0f) return
        with(scope) { drawBar(level, topLeft, size, vertical, peak) }
    }

    private fun DrawScope.drawBar(level: Float, topLeft: Offset, size: Size, vertical: Boolean, peak: Float) {
        prepare(topLeft, size, vertical)
        val length = if (vertical) size.height else size.width
        val across = if (vertical) size.width else size.height
        val lit = level.coerceIn(0f, 1f) * length
        clipPath(clip) {
            if (lit > 0f) {
                val fade = minOf(dissolve, lit)
                // Solid up to a whole pixel under the dissolving tip.
                val solid = floor((lit - fade) / pixel) * pixel
                if (solid > 0f) {
                    // Its end on a whole screen pixel, as the pixels above it.
                    if (vertical) clipRect(topLeft.x, round(topLeft.y + length - solid), topLeft.x + across, topLeft.y + length) { drawRect(brush!!, topLeft, size) }
                    else clipRect(topLeft.x, topLeft.y, round(topLeft.x + solid), topLeft.y + across) { drawRect(brush!!, topLeft, size) }
                }
                // The tip: pixels on a grid fixed on the bar, denser near the solid part.
                val cols = maxOf(1, round(across / pixel).toInt())
                val first = (solid / pixel).toInt()
                val last = kotlin.math.ceil(lit / pixel).toInt()
                val span = (lit - solid).coerceAtLeast(1f)
                for (k in first until last) {
                    val a0 = k * pixel
                    val a1 = minOf((k + 1) * pixel, lit)
                    if (a1 <= a0) continue
                    val t = ((a0 + a1) / 2f - solid) / span
                    // Smooth: full at the solid part, thinning out slowly then quickly towards the tip.
                    val u = 1f - t.coerceIn(0f, 1f)
                    val density = u * u * (3f - 2f * u)
                    val color = colorAt(((a0 + a1) / 2f) / length)
                    for (c in 0 until cols) {
                        if ((BAYER8[(k and 7) * 8 + (c and 7)] + 0.5f) / 64f >= density) continue
                        // On whole screen pixels: no seam between two squares.
                        if (vertical) {
                            val x0 = round(topLeft.x + c * across / cols)
                            val x1 = round(topLeft.x + (c + 1) * across / cols)
                            val y0 = round(topLeft.y + length - a1)
                            val y1 = round(topLeft.y + length - a0)
                            drawRect(color, Offset(x0, y0), Size(x1 - x0, y1 - y0))
                        } else {
                            val y0 = round(topLeft.y + c * across / cols)
                            val y1 = round(topLeft.y + (c + 1) * across / cols)
                            val x0 = round(topLeft.x + a0)
                            val x1 = round(topLeft.x + a1)
                            drawRect(color, Offset(x0, y0), Size(x1 - x0, y1 - y0))
                        }
                    }
                }
            }
            // The peak held, once clearly above the level: a fine line in the colour of where it stands.
            val t = maxOf(pixel, 1f)
            if (peak > 0.004f && peak * length > lit + 1.5f * t) {
                val p = peak.coerceIn(0f, 1f) * length
                val color = colorAt(peak.coerceIn(0f, 1f))
                if (vertical) drawRect(color, Offset(topLeft.x, topLeft.y + length - p), Size(across, t))
                else drawRect(color, Offset(topLeft.x + p - t, topLeft.y), Size(t, across))
            }
        }
    }

    private fun DrawScope.prepare(topLeft: Offset, size: Size, vertical: Boolean) {
        if (brush != null && key0 == topLeft.x && key1 == topLeft.y && key2 == size.width && key3 == size.height && keyVertical == vertical) return
        key0 = topLeft.x; key1 = topLeft.y; key2 = size.width; key3 = size.height; keyVertical = vertical
        val stops = arrayOf(0f to low, s1 to low, s2 to mid, s3 to mid, s4 to high, 1f to high)
        brush = if (vertical) {
            Brush.linearGradient(*stops, start = Offset(topLeft.x, topLeft.y + size.height), end = topLeft)
        } else {
            Brush.linearGradient(*stops, start = topLeft, end = Offset(topLeft.x + size.width, topLeft.y))
        }
        val r = minOf(size.width, size.height) / 2f
        clip.reset()
        clip.addRoundRect(RoundRect(topLeft.x, topLeft.y, topLeft.x + size.width, topLeft.y + size.height, CornerRadius(minOf(r, 2.dp.toPx()))))
    }
}

/** Bayer's 8×8 ordered dither: a pixel of the tip is lit when its threshold is under the density there. */
private val BAYER8 = intArrayOf(
    0, 32, 8, 40, 2, 34, 10, 42,
    48, 16, 56, 24, 50, 18, 58, 26,
    12, 44, 4, 36, 14, 46, 6, 38,
    60, 28, 52, 20, 62, 30, 54, 22,
    3, 35, 11, 43, 1, 33, 9, 41,
    51, 19, 59, 27, 49, 17, 57, 25,
    15, 47, 7, 39, 13, 45, 5, 37,
    63, 31, 55, 23, 61, 29, 53, 21,
)

/** A [LevelBar] in the theme's colours: pixels of 1.5 dp, a tip dissolving over [dissolve]. */
@Composable
fun rememberLevelBar(zones: MeterZones = DefaultZones, dissolve: Dp = 24.dp): LevelBar {
    val low = Neon.Acid
    val mid = Neon.Amber
    val high = Neon.Red
    val density = androidx.compose.ui.platform.LocalDensity.current
    return remember(low, mid, high, zones, dissolve, density) {
        with(density) { LevelBar(low, mid, high, zones, 1.5.dp.toPx(), dissolve.toPx()) }
    }
}

private val DefaultZones = MeterZones()

/** The peak stays this long, then falls back this much of the bar a second. */
private const val PEAK_HOLD_NANOS = 700_000_000L
private const val PEAK_FALL_PER_SECOND = 0.7f

/**
 * The highest [level] (0..1) lately: held [PEAK_HOLD_NANOS], then falling back gently towards
 * the level. Runs only while it falls (one float written a frame, read where drawn); nothing is
 * allocated per frame. With the phone's animations off: no peak (-1).
 */
@Composable
fun rememberPeakHold(level: () -> Float): MutableFloatState {
    val peakState = remember { mutableFloatStateOf(-1f) }
    val still = rememberReducedMotion()
    val read by rememberUpdatedState(level)
    if (!still) {
        LaunchedEffect(Unit) {
            var peak = 0f
            var holdUntil = -1L
            snapshotFlow { read().coerceIn(0f, 1f) }.collectLatest { now ->
                if (now >= peak) {
                    peak = now
                    holdUntil = -1L
                }
                peakState.floatValue = peak
                var last = -1L
                while (peak > now) {
                    withFrameNanos { t ->
                        if (holdUntil < 0L) holdUntil = t + PEAK_HOLD_NANOS
                        if (t > holdUntil && last >= 0L) peak = maxOf(now, peak - PEAK_FALL_PER_SECOND * (t - last) / 1e9f)
                        last = t
                        peakState.floatValue = peak
                    }
                }
            }
        }
    }
    return peakState
}

/** Horizontal level meter: green to amber to red, its tip in fine pixels, its peak held. [level] is 0..1. */
@Composable
fun LevelMeter(level: Float, modifier: Modifier = Modifier) = LevelMeter({ level }, modifier)

/**
 * The same, [level] read only when drawn: a meter fed 20 times a second redraws itself alone,
 * nothing around it is composed again. [zones]: where it turns amber and red; [warnZone]: the red
 * part of the track shown dimly (where the level must not stay).
 */
@Composable
fun LevelMeter(
    level: () -> Float,
    modifier: Modifier = Modifier,
    zones: MeterZones = DefaultZones,
    thickness: Dp = 10.dp,
    warnZone: Boolean = false,
) {
    val track = lerp(Neon.SurfaceHigh, Neon.Background, 0.25f)
    val warn = Neon.Red.copy(alpha = 0.18f)
    val bar = rememberLevelBar(zones, dissolve = 20.dp)
    val peak = rememberPeakHold(level)
    Canvas(modifier = modifier.fillMaxWidth().height(thickness)) {
        val radius = CornerRadius(minOf(size.height / 2f, 2.dp.toPx()))
        drawRoundRect(track, cornerRadius = radius)
        if (warnZone) {
            val x = size.width * (zones.red - 0.03f).coerceIn(0f, 1f)
            clipRect(left = x) { drawRoundRect(warn, cornerRadius = radius) }
        }
        bar.draw(this, level(), Offset.Zero, size, vertical = false, peak = peak.floatValue)
    }
}
