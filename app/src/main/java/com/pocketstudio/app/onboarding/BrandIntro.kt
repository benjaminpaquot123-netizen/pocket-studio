package com.pocketstudio.app.onboarding

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The BenStudio mark: four drum pads laid out as a « B » (two for the stem, two « D » pads for
 * the bowls, the lower one lit as a pad being hit). Same geometry and colours as
 * the launcher icon and the splash (scripts/brand/render_brand.py), on the 108-unit icon canvas.
 */
object BrandMark {
    /** The splash's background, under the intro too (res/values/themes.xml splash_background). */
    val Background = Color(0xFF1E2126)
    val Orange = Color(0xFFFF9F43)
    private val Amber = Color(0xFFFFC46B)
    private val Magenta = Color(0xFFFF5E7A)
    val Ring = Color(0xFFFFB35C)

    /** What a struck pad flashes with: a warm white, never a cold one. */
    val FlashTint = Color(0xFFFFF1D6)

    private const val SCALE = 0.9f

    /** x, y, w, h, then top-left, top-right, bottom-right, bottom-left radii, before [SCALE]. */
    private val RAW = arrayOf(
        floatArrayOf(28.5f, 26f, 19f, 25f, 5f, 5f, 5f, 5f),
        floatArrayOf(28.5f, 54.5f, 19f, 27.5f, 5f, 5f, 5f, 5f),
        floatArrayOf(51f, 26f, 22f, 25f, 5f, 12.5f, 12.5f, 5f),
        floatArrayOf(51f, 54.5f, 28f, 27.5f, 5f, 13.75f, 13.75f, 5f),
    )
    const val PADS = 4
    const val LIT = 3

    /** Top, middle, bottom of each pad's diagonal gradient: orange to amber, a hint of magenta. */
    private val tones = arrayOf(
        listOf(Color(0xFFFFA54A), Color(0xFFF2652C), Color(0xFFE04F5F)),
        listOf(Color(0xFFFFA54A), Color(0xFFF2652C), Color(0xFFE04F5F)),
        listOf(Color(0xFFFFC46B), Color(0xFFFF8C3A), Color(0xFFF0604F)),
        listOf(Color(0xFFFFE09A), Color(0xFFFFA347), Color(0xFFFF6A6A)),
    )

    /** Pad [i] on a canvas of [side] px standing for the 108 units. */
    fun pad(i: Int, side: Float): RoundRect {
        val r = RAW[i]
        val u = side / 108f * SCALE
        val left = side / 2 + (r[0] - 54f) * u
        val top = side / 2 + (r[1] - 54f) * u
        fun c(v: Float) = CornerRadius(minOf(v * u, r[3] * u / 2), minOf(v * u, r[3] * u / 2))
        return RoundRect(left, top, left + r[2] * u, top + r[3] * u, c(r[4]), c(r[5]), c(r[6]), c(r[7]))
    }

    fun mid(r: RoundRect) = Offset((r.left + r.right) / 2, (r.top + r.bottom) / 2)

    /** What the mark draws with, made once per size (no allocation while it animates). */
    class Kit(side: Float) {
        val u = side / 108f
        val pads = Array(PADS) { pad(it, side) }
        val paths = Array(PADS) { i ->
            Path().apply {
                fillType = PathFillType.EvenOdd
                addRoundRect(pads[i])
            }
        }
        val edges = Array(PADS) { i ->
            val p = pads[i]
            val h = 0.5f * u
            Path().apply {
                addRoundRect(RoundRect(p.left + h, p.top + h, p.right - h, p.bottom - h,
                    shrink(p.topLeftCornerRadius, h), shrink(p.topRightCornerRadius, h), shrink(p.bottomRightCornerRadius, h), shrink(p.bottomLeftCornerRadius, h)))
            }
        }
        val fills = Array(PADS) { i ->
            val p = pads[i]
            Brush.linearGradient(0f to tones[i][0], 0.65f to tones[i][1], 1f to tones[i][2],
                start = Offset(p.left, p.top), end = Offset(p.left + p.width * 0.35f, p.bottom))
        }
        val inner = Array(PADS) { i ->
            val p = pads[i]
            Brush.radialGradient(listOf(Color.White.copy(alpha = 0.28f), Color.Transparent),
                center = Offset(p.left + p.width * 0.45f, p.top + p.height * 0.25f), radius = maxOf(p.width, p.height) * 0.7f)
        }
        val edgeLight = Array(PADS) { i ->
            val p = pads[i]
            Brush.verticalGradient(0f to Color.White.copy(alpha = 0.7f), 0.35f to Color.White.copy(alpha = 0.08f), 1f to Color.Transparent,
                startY = p.top, endY = p.bottom)
        }
        val litCenter = mid(pads[LIT])
        val litRadius = pads[LIT].width * 0.95f
        val lit = Brush.radialGradient(listOf(Color(0xFFFFB347).copy(alpha = 0.55f), Color(0xFFFF6A6A).copy(alpha = 0f)), litCenter, litRadius)
        val haloCenter = Offset(58f * u, 58f * u)
        val halo = Brush.radialGradient(
            0f to Orange.copy(alpha = 0.42f), 0.5f to Magenta.copy(alpha = 0.12f), 1f to Magenta.copy(alpha = 0f),
            center = haloCenter, radius = 44f * u,
        )
        val edgeStroke = Stroke(width = u)

        /** 12 strokes from 0.6 to 1.8 u: Stroke is a class, never made while drawing; 12 steps over 3 dp are invisible. */
        val ringStrokes = Array(12) { Stroke(width = (0.6f + 1.2f * it / 11f) * u) }
        fun ringStroke(widthU: Float): Stroke = ringStrokes[((widthU - 0.6f) / 1.2f * 11f).roundToInt().coerceIn(0, 11)]

        /** The pads' lift at the full rise, in px (the halo and the dust follow only [IntroTimeline.HALO_RISE] of it). */
        val liftPx = side * IntroTimeline.LIFT

        // The opening's extras (none on the still mark, none before the splash hands over).
        /** A light from the top on each pad, for relief. */
        val gloss = Array(PADS) { i ->
            val p = pads[i]
            Brush.verticalGradient(0f to Color.White.copy(alpha = 0.2f), 1f to Color.Transparent, startY = p.top, endY = p.top + p.height * 0.48f)
        }
        /** The shadow's drop under each pad at rest ([IntroTimeline.shadowLift] stretches it on the beats and the rise). */
        val shadowDrop = 1.6f * u
        /** The beat's own glow over the halo: hot, small. */
        val hotHalo = Brush.radialGradient(
            0f to Color(0xFFFFE09A).copy(alpha = 0.4f), 0.4f to Orange.copy(alpha = 0.18f), 1f to Orange.copy(alpha = 0f),
            center = haloCenter, radius = 34f * u,
        )
        /** The light that runs over the pads after the beats: a diagonal band through (0, 0), moved along x. */
        val sheenBand = 16f * u
        val sheen = Brush.linearGradient(
            0f to Color.Transparent, 0.5f to Color.White.copy(alpha = 0.55f), 1f to Color.Transparent,
            start = Offset(-sheenBand / 2, -sheenBand * 0.3f), end = Offset(sheenBand / 2, sheenBand * 0.3f),
        )
        /** Where the band's line (x + 0.6 y = c) starts and ends: just off the pads on both sides. */
        val sheenFrom = pads[0].left + 0.6f * pads[0].top - sheenBand
        val sheenTo = pads[3].right + 0.6f * pads[3].bottom + sheenBand


        private fun shrink(c: CornerRadius, by: Float) = CornerRadius(max(0f, c.x - by), max(0f, c.y - by))
    }

    val wordAccent = Brush.horizontalGradient(listOf(Amber, Color(0xFFFF7A3D)))
    val line = Brush.horizontalGradient(listOf(Color(0xFFFFA347), Magenta))
}

/**
 * The mark drawn in a square box ([size] stands for the icon's 108 units: the pads take about
 * half of it). With [time] (ms on [IntroTimeline]), the pads pop in, sink and flash on the beats,
 * the halo comes up and pulses, the pads take relief (their shadow follows the punch) and a light
 * runs over them; as the mark rises the halo gives back part of the lift (parallax). Read while
 * drawing only, so an animation redraws without composing again (and allocates nothing).
 */
@Composable
fun BenStudioMark(size: Dp, modifier: Modifier = Modifier, glow: Boolean = true, time: (() -> Float)? = null) {
    Box(
        modifier.size(size).drawWithCache {
            val kit = BrandMark.Kit(this.size.minDimension)
            onDrawBehind {
                val t = time?.invoke()
                val rise = if (t == null) 0f else IntroTimeline.rise(t)
                if (glow) {
                    val haloAlpha = if (t == null) 1f else IntroTimeline.halo(t)
                    val haloScale = if (t == null) 1f else IntroTimeline.haloScale(t)
                    // Parallax: the box rises with the pads; the halo gives back (1 - HALO_RISE) of that lift.
                    translate(top = kit.liftPx * (1f - IntroTimeline.HALO_RISE) * rise) {
                        scale(haloScale, pivot = kit.haloCenter) {
                            drawCircle(kit.halo, 44f * kit.u, kit.haloCenter, alpha = haloAlpha)
                            val hot = if (t == null) 0f else IntroTimeline.beatGlow(t)
                            if (hot > 0.002f) drawCircle(kit.hotHalo, 34f * kit.u, kit.haloCenter, alpha = hot.coerceAtMost(1f))
                        }
                    }
                }
                val relief = if (t == null) 0f else IntroTimeline.relief(t)
                val sheen = if (t == null) -1f else IntroTimeline.sheen(t)
                // The four shadows first, under every pad (they lengthen as the mark punches out, come back on the spring).
                val shadowA = if (t == null) 0f else IntroTimeline.shadowAlpha(t)
                if (shadowA > 0.002f && t != null) {
                    val drop = kit.shadowDrop * IntroTimeline.shadowLift(t)
                    for (i in 0 until BrandMark.PADS) {
                        val pop = IntroTimeline.pop(i, t)
                        if (pop <= 0.001f) continue
                        scale(pop * IntroTimeline.press(i, t), pivot = BrandMark.mid(kit.pads[i])) {
                            translate(top = drop) { drawPath(kit.paths[i], Color.Black, alpha = shadowA) }
                        }
                    }
                }
                for (i in 0 until BrandMark.PADS) {
                    val pop = if (t == null) 1f else IntroTimeline.pop(i, t)
                    if (pop <= 0.001f) continue
                    val press = if (t == null) 1f else IntroTimeline.press(i, t)
                    val p = kit.pads[i]
                    scale(pop * press, pivot = BrandMark.mid(p)) {
                        if (i == BrandMark.LIT) drawCircle(kit.lit, kit.litRadius, kit.litCenter)
                        drawPath(kit.paths[i], kit.fills[i])
                        drawPath(kit.paths[i], kit.inner[i])
                        drawPath(kit.edges[i], kit.edgeLight[i], style = kit.edgeStroke)
                        if (relief > 0f) drawPath(kit.paths[i], kit.gloss[i], alpha = relief)
                        val flash = if (t == null) 0f else IntroTimeline.flash(i, t)
                        if (flash > 0f) drawPath(kit.paths[i], BrandMark.FlashTint, alpha = flash)
                        if (sheen in 0f..1f) {
                            val x = kit.sheenFrom + (kit.sheenTo - kit.sheenFrom) * sheen
                            clipPath(kit.paths[i]) {
                                translate(left = x) { drawRect(kit.sheen, topLeft = Offset(-x, 0f), size = this.size) }
                            }
                        }
                    }
                }
            }
        },
    )
}

/**
 * What goes out of the mark on the heartbeats (same box as [BenStudioMark]): a soft shock wave
 * of light on each beat, the one ring (struck by the first beat, taut, plucked by the second, let
 * go as the mark rises) and the dust of light that lives in the halo until the end. Everything is
 * made once per size; nothing is allocated while it draws. Not clipped: the ring and the dust
 * overflow the box on purpose.
 */
@Composable
private fun BeatBurst(size: Dp, modifier: Modifier, time: () -> Float) {
    Box(
        modifier.size(size).drawWithCache {
            val kit = BrandMark.Kit(this.size.minDimension)
            val u = kit.u
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val shockRadius = 36f * u
            val shock = Brush.radialGradient(
                0f to BrandMark.Ring.copy(alpha = 0f), 0.74f to BrandMark.Ring.copy(alpha = 0f), 0.88f to BrandMark.Ring.copy(alpha = 0.42f),
                0.94f to BrandMark.Orange.copy(alpha = 0.16f), 1f to BrandMark.Orange.copy(alpha = 0f),
                center = center, radius = shockRadius,
            )
            val dust = IntroTimeline.Dust
            onDrawBehind {
                val t = time()
                for (k in 0 until IntroTimeline.BEAT_COUNT) {
                    val w = IntroTimeline.shockProgress(k, t)
                    if (w > 0f && w < 1f) {
                        val s = 0.7f + 1.2f * IntroTimeline.easeOut(w)
                        scale(s, pivot = center) { drawCircle(shock, shockRadius, center, alpha = IntroTimeline.shockAlpha(k) * (1f - w) * (1f - w)) }
                    }
                }
                // The one ring: struck, taut, plucked, let go.
                val ra = IntroTimeline.ringAlpha(t)
                if (ra > 0.003f) {
                    drawCircle(BrandMark.Ring, IntroTimeline.ringRadius(t) * u, center, alpha = ra, style = kit.ringStroke(IntroTimeline.ringWidth(t)))
                }
                // The dust lives in the halo: it follows the halo's lesser rise (this box rises with the pads).
                val back = kit.liftPx * (1f - IntroTimeline.HALO_RISE) * IntroTimeline.rise(t)
                val up = dust.lift(t) * u
                translate(top = back) {
                    for (j in 0 until IntroTimeline.Dust.COUNT) {
                        val a = dust.alpha(j, t)
                        if (a <= 0.003f) continue
                        val r = dust.radius(j, t) * u
                        drawCircle(dust.color(j), dust.size(j) * u, Offset(center.x + dust.dx(j) * r, center.y + dust.dy(j) * r - up), alpha = a)
                    }
                }
            }
        },
    )
}

/** « BenStudio »: « Ben » in the accent gradient (Inter ExtraBold), « Studio » in white (Medium). */
@Composable
fun BenStudioWordmark(modifier: Modifier = Modifier, sizeSp: Int = 34) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(brush = BrandMark.wordAccent, fontWeight = FontWeight.ExtraBold)) { append("Ben") }
            withStyle(SpanStyle(color = Color(0xFFF6F7F9), fontWeight = FontWeight.Medium)) { append("Studio") }
        },
        style = MaterialTheme.typography.headlineLarge.copy(fontSize = sizeSp.sp, letterSpacing = 0.sp),
        modifier = modifier,
    )
}

/**
 * Whether the opening plays, and how far the system splash got: the app's own windows (the
 * welcome, « Que veux-tu faire ? ») wait for it, as they would cover it; the app itself starts
 * underneath meanwhile.
 */
object Intro {
    var playing by mutableStateOf(false)

    /** Where the splash's animation was when it handed over (ms; 0 when there was none). */
    var splashElapsedMs by mutableLongStateOf(0L)
}

/**
 * The opening's timeline, in ms from the splash's start (scripts/brand/render_brand.py keeps its
 * own copy of the first second for its SVG preview; the truth of the whole opening is
 * BrandTest.previewFrames). The splash (at most [SPLASH_MS]) pops the pads in and draws a breath
 * in; then, drawn by the app, in step with its felt beats: a double heartbeat (the mark punches
 * out on a spring, the struck pads sink and flash in turn, their shadow follows the punch, a shock
 * wave of light; the first beat strikes one ring of light that tautens around the mark, the
 * second plucks it; a dust of light wakes in the halo; the screen warms and its corners darken),
 * a light runs over the pads, the mark rises (the halo and the dust less: parallax) and the ring
 * lets go, « BenStudio » is set down under it and a light runs over it, a line signs it; the halo
 * breathes; then we go into the app (a fade with a slight zoom). 5 s of timeline, played 30 %
 * slower after a 0.3 s hold: about 6.5 s from the tap on the icon (2026-11-04). After the first
 * beat, every later time sits on a 240 ms grid (1100 + n × 240): the tempo is felt, nothing more
 * is drawn.
 */
object IntroTimeline {
    const val POP_STAGGER = 60f
    const val POP_MS = 260f
    const val HALO_START = 120f
    const val HALO_END = 520f

    /** Before the first beat, the mark draws a breath in (a little smaller, the halo a little dimmer). */
    const val INHALE_START = 560f
    const val INHALE_DEPTH = 0.035f
    const val INHALE_DIM = 0.2f
    const val RELEASE_MS = 70f

    const val BEAT_COUNT = 2
    private val BEAT_START = floatArrayOf(1100f, 1340f)
    private val BEAT_SWELL = floatArrayOf(0.15f, 0.085f)
    private val BEAT_FLASH = floatArrayOf(0.5f, 0.32f)
    private val BEAT_GLOW = floatArrayOf(1f, 0.62f)
    private val SHOCK_ALPHA = floatArrayOf(0.7f, 0.4f)
    private val SHOCK_MS = floatArrayOf(480f, 420f)

    /** The punch: up in [BEAT_UP], then a damped spring (period [SPRING_MS], decay [SPRING_DECAY]). */
    const val BEAT_UP = 55f
    const val SPRING_MS = 300f
    const val SPRING_DECAY = 85f
    const val SPRING_END = 700f
    const val GLOW_UP = 40f
    const val GLOW_DECAY = 240f
    const val FLASH_STAGGER = 35f
    const val FLASH_UP = 40f
    const val FLASH_DOWN = 240f
    /** The lit pad (the one being hit) flashes a little brighter: the eye ends on it. */
    const val FLASH_LIT_BOOST = 1.2f

    /** A struck pad sinks (the app's own pad gesture), in the flash's cascade, and comes back. */
    const val PRESS_DEPTH = 0.05f
    const val PRESS_UP = 40f
    const val PRESS_DOWN = 200f

    /** The pads' shadow: longer on the punch, a little longer on the rise, a little lighter on the punch. */
    const val SHADOW_PUNCH = 2.2f
    const val SHADOW_RISE = 0.5f
    const val SHADOW_ALPHA = 0.32f
    const val SHADOW_DIM = 0.3f

    /** The pads take relief once the app draws (the splash's pads are flat: no jump at the hand-over). */
    const val RELIEF_START = 1000f
    const val RELIEF_END = 1200f
    const val AMBIENT_END = 1500f

    /** The screen's corners darken (a spot on a dark stage); 0 as VIGNETTE_MAX: no vignette. */
    const val VIGNETTE_START = 1000f
    const val VIGNETTE_END = 1580f
    const val VIGNETTE_MAX = 0.25f

    /** The one ring, in icon units: born on the first beat, taut at [RING_TAUT] after [RING_OUT_MS]. */
    const val RING_BASE = 31f
    const val RING_TAUT = 40f
    const val RING_OUT_MS = 420f
    const val RING_ALPHA_BORN = 0.9f
    const val RING_ALPHA_TAUT = 0.30f
    const val RING_WIDTH_BORN = 1.8f
    const val RING_WIDTH_TAUT = 0.8f

    /** The second beat plucks it: a damped radial vibration (0 as PLUCK_AMP: none), a flare of alpha and width. */
    const val PLUCK_AMP = 0.04f
    const val PLUCK_MS = 120f
    const val PLUCK_DECAY = 170f
    const val PLUCK_FLARE = 0.45f
    const val PLUCK_FLARE_DECAY = 200f
    const val PLUCK_WIDTH = 0.5f

    /** Let go as the mark rises: a little wider, gone. */
    const val RING_RELEASE_START = 1820f
    const val RING_RELEASE_END = 2180f
    const val RING_RELEASE_GROW = 3f

    const val SHEEN_START = 1480f
    const val SHEEN_END = 1980f
    const val RISE_START = 1820f
    const val RISE_END = 2300f

    /** Parallax of the rise: the halo (and the dust in it) and the ambient light rise less than the pads. */
    const val HALO_RISE = 0.6f
    const val AMBIENT_RISE = 0.35f

    /** The name: lit by [NAME_ALPHA_END], set down (from [NAME_DROP_DP] below, a little stretched) by [NAME_END]. */
    const val NAME_START = 2060f
    const val NAME_ALPHA_END = 2540f
    const val NAME_END = 2780f
    const val NAME_DROP_DP = 10f
    const val NAME_STRETCH_X = 0.045f
    const val NAME_STRETCH_Y = 0.01f
    const val NAME_SHINE_START = 2540f
    const val NAME_SHINE_END = 3260f
    const val NAME_SHINE_ALPHA = 0.7f
    const val LINE_START = 2780f
    const val LINE_END = 3260f
    const val BREATH_START = 2300f
    const val BREATH_MS = 2400f

    /** The dust of light in the halo: lit on the first beat, pushed by both, twinkling, drifting up, half gone at the end. */
    const val DUST_IGNITE_MS = 60f
    /** Icon units per ms. */
    const val DUST_DRIFT = 0.0035f
    const val DUST_GLOW = 0.6f
    /** Its brightest grain, ever; 0: no dust. */
    const val DUST_MAX = 0.6f
    const val DUST_DIM_START = 3980f
    const val DUST_DIM_END = 4700f
    private val DUST_PUSH = floatArrayOf(7f, 3f)
    private val DUST_PUSH_MS = floatArrayOf(600f, 560f)

    /** The fade into the app, with this much zoom: we go into it, the screen does not switch off. */
    const val FADE_START = 4700f
    const val TOTAL = 5000f
    const val ZOOM = 0.04f

    /**
     * Benjamin (2026-11-04): at the hand-over the app is still loading and the first frames
     * stuttered. The intro now waits [HOLD_MS] on the splash's frame before moving, then plays its
     * timeline [SLOW] times slower (30 % longer). The times above stay the timeline's own.
     */
    const val HOLD_MS = 300f
    const val SLOW = 1.3f

    /**
     * Wall-clock ms after the intro's start at which timeline time [t] is drawn (started at [from]).
     * The hold only plays at the splash's hand-over ([from] > 0): from 0 nothing is on screen yet to hold.
     */
    fun wallMs(t: Float, from: Float = 0f): Float = (if (from > 0f) HOLD_MS else 0f) + (t - from).coerceAtLeast(0f) * SLOW

    /** The splash's longest time on screen: the intro takes over by then at the latest. */
    const val SPLASH_MS = 1000L

    /** The sound (assets/intro/ting.wav, the heartbeat itself is felt): its frame 0 plays at [SOUND_START]; 48 kHz. */
    const val SOUND_START = 1000f
    const val SAMPLE_RATE = 48_000

    /** Later than this, the sound is not started at all (it would miss its first beat). */
    const val SOUND_LATEST = 1070f

    /** A beat's tap (phone on vibrate) is not given later than this after it. */
    const val BEAT_LATE_MS = 60f

    /** Of the 288 dp box: how far the mark goes up, where the name's top sits under the centre. */
    const val LIFT = 0.13f
    const val NAME_TOP = 0.19f

    /** Asked to skip (a touch): the fade only. */
    const val SKIP_FADE_MS = 180

    /** With the phone's animations off: still, then a short fade. */
    const val STILL_MS = 450L
    const val STILL_FADE_MS = 200L

    fun beatStart(k: Int): Float = BEAT_START[k]

    /** The sound file's frame of beat [k] (where the WAV's cue point is). */
    fun beatFrame(k: Int): Int = soundFrame(BEAT_START[k])

    /** The file's frame playing when the timeline is at [ms]. */
    fun soundFrame(ms: Float): Int = ((ms - SOUND_START) * SAMPLE_RATE / 1000f).roundToInt()

    fun ramp(t: Float, from: Float, to: Float): Float = ((t - from) / (to - from)).coerceIn(0f, 1f)

    fun ease(x: Float): Float = x * x * (3f - 2f * x)

    fun easeOut(x: Float): Float = 1f - (1f - x) * (1f - x) * (1f - x)

    /** Android's accelerate_decelerate interpolator, as the splash's breath. */
    fun easeCos(x: Float): Float = (1f - cos(PI.toFloat() * x)) / 2f

    /** Out fast, then a long settle (1 - (1-x)^5): the ring, the dust, the name, the line. */
    fun easeOutQuint(x: Float): Float {
        val p = 1f - x
        val p2 = p * p
        return 1f - p2 * p2 * p
    }

    /** Android's overshoot interpolator (tension 2), as the splash's. */
    private fun overshoot(x: Float): Float {
        val y = x - 1f
        return y * y * (3f * y + 2f) + 1f
    }

    fun pop(i: Int, t: Float): Float {
        val x = ramp(t, i * POP_STAGGER, i * POP_STAGGER + POP_MS)
        return if (x < 1f) overshoot(x) else 1f
    }

    /** The breath in before the first beat (1 → 1 - [INHALE_DEPTH]), let go at once on the beat. */
    private fun inhale(t: Float): Float = if (t < BEAT_START[0]) easeCos(ramp(t, INHALE_START, BEAT_START[0]))
    else 1f - easeOut(ramp(t, BEAT_START[0], BEAT_START[0] + RELEASE_MS))

    /** A beat's punch: out fast, back on a damped spring (a little under, then still). */
    fun spring(u: Float): Float = when {
        u <= 0f -> 0f
        u < BEAT_UP -> easeOut(u / BEAT_UP)
        u > SPRING_END -> 0f
        else -> {
            val v = u - BEAT_UP
            cos(2f * PI.toFloat() * v / SPRING_MS) * exp(-v / SPRING_DECAY)
        }
    }

    /** The slow breathing once the name is there (-1..1, 0 before). */
    fun breath(t: Float): Float = if (t <= BREATH_START) 0f
    else sin(2f * PI.toFloat() * (t - BREATH_START) / BREATH_MS) * ramp(t, BREATH_START, BREATH_START + 550f)

    /** The mark's size: the breath in, the double heartbeat, then the slow breathing. */
    fun heartbeat(t: Float): Float {
        var s = 1f - INHALE_DEPTH * inhale(t)
        for (k in 0 until BEAT_COUNT) s *= 1f + BEAT_SWELL[k] * spring(t - BEAT_START[k])
        return s * (1f + 0.006f * breath(t))
    }

    /** The beats as one number: 1 at the first beat's height, 0.57 at the second's, 0 before and once settled. */
    fun punch(t: Float): Float {
        var v = 0f
        for (k in 0 until BEAT_COUNT) v += BEAT_SWELL[k] * spring(t - BEAT_START[k])
        return v / BEAT_SWELL[0]
    }

    /** Pad [i]'s scale when struck: it sinks by [PRESS_DEPTH] (the app's own pad gesture), in the flash's cascade; 1 otherwise. */
    fun press(i: Int, t: Float): Float {
        var v = 0f
        for (k in 0 until BEAT_COUNT) {
            val u = t - BEAT_START[k] - i * FLASH_STAGGER
            val depth = PRESS_DEPTH * BEAT_FLASH[k] / BEAT_FLASH[0]
            v = max(
                v,
                when {
                    u < 0f -> 0f
                    u < PRESS_UP -> depth * easeOut(u / PRESS_UP)
                    else -> depth * (1f - ease(ramp(u, PRESS_UP, PRESS_UP + PRESS_DOWN)))
                },
            )
        }
        return 1f - v
    }

    /** How much the beats light things up (halo, screen): a flare on each, the second softer. */
    fun beatGlow(t: Float): Float {
        var v = 0f
        for (k in 0 until BEAT_COUNT) {
            val u = t - BEAT_START[k]
            v += BEAT_GLOW[k] * when {
                u < 0f -> 0f
                u < GLOW_UP -> u / GLOW_UP
                else -> exp(-(u - GLOW_UP) / GLOW_DECAY)
            }
        }
        return v
    }

    /** The halo's light: up after the pads, a little dimmer while breathing in. */
    fun halo(t: Float): Float = ramp(t, HALO_START, HALO_END) * (1f - INHALE_DIM * inhale(t))

    fun haloScale(t: Float): Float = 1f + 0.16f * beatGlow(t).coerceAtMost(1.2f) + 0.05f * breath(t)

    /** The warm light over the whole screen: from the first beat, flaring on each, then breathing. */
    fun ambient(t: Float): Float =
        (0.5f * ease(ramp(t, BEAT_START[0], AMBIENT_END)) * (1f + 0.2f * breath(t)) + 0.45f * beatGlow(t)).coerceIn(0f, 1f)

    fun relief(t: Float): Float = ease(ramp(t, RELIEF_START, RELIEF_END))

    /** The mark's rise (0..1): the pads go up by [LIFT] of the box, the halo and the ambient light less. */
    fun rise(t: Float): Float = ease(ramp(t, RISE_START, RISE_END))

    /** The pads' shadow: longer as the mark punches out, a little longer as it rises (× the Kit's shadowDrop). */
    fun shadowLift(t: Float): Float = 1f + SHADOW_PUNCH * punch(t).coerceAtLeast(0f) + SHADOW_RISE * rise(t)

    fun shadowAlpha(t: Float): Float = SHADOW_ALPHA * relief(t) * (1f - SHADOW_DIM * punch(t).coerceAtLeast(0f))

    /** The screen's vignette (0..1 of [VIGNETTE_MAX]): with the relief, a little slower. */
    fun vignette(t: Float): Float = ease(ramp(t, VIGNETTE_START, VIGNETTE_END))

    /** The fade into the app (0..1): a touch jumps into it. */
    fun fade(t: Float): Float = ease(ramp(t, FADE_START, TOTAL))

    /** The light over the pads: 0..1 across them while it runs (at a constant speed, as a play head), -1 otherwise. */
    fun sheen(t: Float): Float = if (t <= SHEEN_START || t >= SHEEN_END) -1f else ramp(t, SHEEN_START, SHEEN_END)

    /** The light over the name: 0..1 across it while it runs, -1 otherwise. */
    fun nameShine(t: Float): Float = if (t <= NAME_SHINE_START || t >= NAME_SHINE_END) -1f else ease(ramp(t, NAME_SHINE_START, NAME_SHINE_END))

    fun nameAlpha(t: Float): Float = ease(ramp(t, NAME_START, NAME_ALPHA_END))

    /** The name's settle (0..1): from a little below and a little stretched to its place. */
    fun nameSettle(t: Float): Float = easeOutQuint(ramp(t, NAME_START, NAME_END))

    /** The line under the name (0..1 of its width, from the centre). */
    fun line(t: Float): Float = easeOutQuint(ramp(t, LINE_START, LINE_END))

    /** How lit pad [i] is: struck in turn on each beat, the second softer, the lit pad a little brighter. */
    fun flash(i: Int, t: Float): Float {
        var v = 0f
        for (k in 0 until BEAT_COUNT) {
            val u = t - BEAT_START[k] - i * FLASH_STAGGER
            val peak = BEAT_FLASH[k] * (if (i == BrandMark.LIT) FLASH_LIT_BOOST else 1f)
            v = when {
                u < 0f -> v
                u < FLASH_UP -> max(v, peak * u / FLASH_UP)
                u < FLASH_UP + FLASH_DOWN -> max(v, peak * (1f - (u - FLASH_UP) / FLASH_DOWN))
                else -> v
            }
        }
        return v
    }

    fun shockProgress(k: Int, t: Float): Float = ramp(t, BEAT_START[k], BEAT_START[k] + SHOCK_MS[k])

    fun shockAlpha(k: Int): Float = SHOCK_ALPHA[k]

    private fun ringOut(t: Float): Float = easeOutQuint(ramp(t, BEAT_START[0], BEAT_START[0] + RING_OUT_MS))

    private fun ringRelease(t: Float): Float = ease(ramp(t, RING_RELEASE_START, RING_RELEASE_END))

    /** The one ring, in icon units: born on the first beat, taut at [RING_TAUT], plucked by the second, let go as the mark rises; 0 outside. */
    fun ringRadius(t: Float): Float {
        if (t <= BEAT_START[0] || t >= RING_RELEASE_END) return 0f
        var r = RING_BASE + (RING_TAUT - RING_BASE) * ringOut(t) + RING_RELEASE_GROW * ringRelease(t)
        val v = t - BEAT_START[1]
        if (v > 0f) r *= 1f + PLUCK_AMP * sin(2f * PI.toFloat() * v / PLUCK_MS) * exp(-v / PLUCK_DECAY)
        return r
    }

    /** Its light: bright when struck, faint once taut, flaring on the pluck, gone when let go. */
    fun ringAlpha(t: Float): Float {
        if (t <= BEAT_START[0] || t >= RING_RELEASE_END) return 0f
        var a = RING_ALPHA_BORN - (RING_ALPHA_BORN - RING_ALPHA_TAUT) * ringOut(t)
        val v = t - BEAT_START[1]
        if (v > 0f) a += PLUCK_FLARE * exp(-v / PLUCK_FLARE_DECAY)
        return (a * (1f - ringRelease(t))).coerceAtMost(1f)
    }

    /** Its stroke, in icon units (quantised to the Kit's 12 strokes when drawn). */
    fun ringWidth(t: Float): Float {
        var w = RING_WIDTH_BORN - (RING_WIDTH_BORN - RING_WIDTH_TAUT) * ringOut(t)
        val v = t - BEAT_START[1]
        if (v > 0f) w += PLUCK_WIDTH * exp(-v / PLUCK_FLARE_DECAY)
        return w
    }

    /**
     * Grains of light in the halo, laid out once (a fixed seed): on the upper arc only, lit by the
     * first beat, pushed by both, twinkling and drifting up until the end. Never under the name.
     */
    object Dust {
        const val COUNT = 18
        private val dx = FloatArray(COUNT)
        private val dy = FloatArray(COUNT)
        private val r0 = FloatArray(COUNT)
        private val sizes = FloatArray(COUNT)
        private val peaks = FloatArray(COUNT)
        private val periods = FloatArray(COUNT)
        private val phases = FloatArray(COUNT)
        private val Warm = Color(0xFFFFE09A)
        private val Amber = Color(0xFFFFB35C)

        init {
            val r = java.util.Random(0xB3A7L)
            for (j in 0 until COUNT) {
                // -190° → +10°: the upper arc and the sides, uneven (dust, not a clock's ticks).
                val a = (-190f + 200f * (j + 0.5f + (r.nextFloat() - 0.5f) * 0.9f) / COUNT) * PI.toFloat() / 180f
                dx[j] = cos(a)
                dy[j] = sin(a)
                r0[j] = 33f + r.nextFloat() * 17f          // 33..50 u from the centre: outside the pads, in the halo
                sizes[j] = 0.6f + r.nextFloat() * 0.4f     // 0.6..1.0 u (never under 1.5 dp: no shimmer)
                peaks[j] = 0.18f + r.nextFloat() * 0.24f   // 0.18..0.42
                periods[j] = 900f + r.nextFloat() * 800f
                phases[j] = r.nextFloat() * 2f * PI.toFloat()
            }
        }

        fun dx(j: Int): Float = dx[j]

        fun dy(j: Int): Float = dy[j]

        /** Its radius, in icon units. */
        fun size(j: Int): Float = sizes[j]

        /** Two warm tones only. */
        fun color(j: Int): Color = if (j % 2 == 0) Warm else Amber

        /** Distance from the centre, in icon units: pushed by each beat. */
        fun radius(j: Int, t: Float): Float = r0[j] +
            DUST_PUSH[0] * easeOutQuint(ramp(t, BEAT_START[0], BEAT_START[0] + DUST_PUSH_MS[0])) +
            DUST_PUSH[1] * easeOutQuint(ramp(t, BEAT_START[1], BEAT_START[1] + DUST_PUSH_MS[1]))

        /** How far up they have drifted, in icon units. */
        fun lift(t: Float): Float = DUST_DRIFT * (t - BEAT_START[0]).coerceAtLeast(0f)

        fun alpha(j: Int, t: Float): Float {
            if (t <= BEAT_START[0]) return 0f
            val twinkle = 0.65f + 0.35f * sin(2f * PI.toFloat() * t / periods[j] + phases[j])
            val a = peaks[j] * ramp(t, BEAT_START[0], BEAT_START[0] + DUST_IGNITE_MS) * twinkle *
                (1f + DUST_GLOW * beatGlow(t)) * (1f - 0.5f * ramp(t, DUST_DIM_START, DUST_DIM_END))
            return a.coerceAtMost(DUST_MAX)
        }
    }
}

/** The phone's « Supprimer les animations » (animator duration scale 0). */
fun animationsOff(context: android.content.Context): Boolean =
    runCatching { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }.getOrDefault(false)

/**
 * The opening, after the system splash (the same pads, at the same place, on the same colour,
 * on the same timeline: it goes on from where the splash was, [Intro.splashElapsedMs]): the pads
 * pop in and draw a breath, beat twice like a heart (the struck pads sink and flash in turn,
 * their shadow follows the punch, a shock wave of light on each beat; the first beat strikes one
 * ring of light that tautens around the mark, the second plucks it; a dust of light wakes in the
 * halo; the screen warms, its corners darken), a light runs over the pads, the mark rises and
 * the ring lets go, « BenStudio » is set down under it, a light over it, a line signs it, the halo
 * breathes, and we go into the app (a fade with a slight zoom). About 6.5 s from the tap on the
 * icon (a 0.3 s hold, then 30 % slower); a touch skips it. [cue] feels the two beats, in step
 * ([IntroCue], decided by MainActivity). It is drawn over the app, which starts underneath
 * meanwhile (the project loads, the audio engine starts): nothing waits for it.
 * With the phone's animations off: the mark and the name, still, then a short fade, silent.
 */
@Composable
fun BrandIntro(onDone: () -> Unit, reducedMotion: Boolean = animationsOff(LocalContext.current), cue: IntroCue = IntroCue.None) {
    // Progress in ms on the timeline; read in the drawing phase only.
    val clock = remember { Animatable(if (reducedMotion) IntroTimeline.FADE_START else 0f) }
    var stillAlpha by remember { mutableFloatStateOf(1f) }
    var skipped by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    if (reducedMotion) {
        LaunchedEffect(Unit) {
            delay(IntroTimeline.STILL_MS)
            // A fade by hand: the phone's animation scale (0) would make an animate* instant.
            val start = withFrameNanos { it }
            while (stillAlpha > 0f) {
                withFrameNanos { now -> stillAlpha = 1f - ((now - start) / 1e6f / IntroTimeline.STILL_FADE_MS).coerceIn(0f, 1f) }
            }
            onDone()
        }
    } else {
        // Started again from where the splash handed over, when it says so after the first frame.
        LaunchedEffect(Intro.splashElapsedMs) {
            val from = max(clock.value, Intro.splashElapsedMs.toFloat().coerceAtMost(IntroTimeline.FADE_START))
            clock.snapTo(from)
            // A touch already started the fade: it is not taken over.
            if (skipped) return@LaunchedEffect
            // At the splash's hand-over, still on its frame while the app finishes loading underneath:
            // the hold is the animation's own start delay (a coroutine delay stalls under test clocks).
            val hold = if (from > 0f) IntroTimeline.HOLD_MS.toInt() else 0
            clock.animateTo(
                IntroTimeline.TOTAL,
                tween(((IntroTimeline.TOTAL - from) * IntroTimeline.SLOW).toInt(), delayMillis = hold, easing = LinearEasing),
            )
        }
        LaunchedEffect(Unit) {
            snapshotFlow { clock.value >= IntroTimeline.TOTAL }.first { it }
            onDone()
        }
        // The sound starts on the timeline (at the frame that plays now), the taps on the beats.
        LaunchedEffect(cue) {
            snapshotFlow { clock.value >= IntroTimeline.SOUND_START }.first { it }
            if (!skipped && clock.value <= IntroTimeline.SOUND_LATEST) cue.start(clock.value)
            for (k in 0 until IntroTimeline.BEAT_COUNT) {
                val at = IntroTimeline.beatStart(k)
                snapshotFlow { clock.value >= at }.first { it }
                if (!skipped && clock.value < at + IntroTimeline.BEAT_LATE_MS) cue.beat(k)
            }
        }
        DisposableEffect(cue) { onDispose { cue.stop() } }
    }
    val skip: () -> Unit = {
        skipped = true
        if (reducedMotion) {
            stillAlpha = 0f
            onDone()
        } else {
            cue.stop()
            scope.launch {
                val from = max(clock.value, IntroTimeline.FADE_START + (IntroTimeline.TOTAL - IntroTimeline.FADE_START) * 0.4f)
                clock.snapTo(from)
                clock.animateTo(IntroTimeline.TOTAL, tween(IntroTimeline.SKIP_FADE_MS, easing = LinearEasing))
            }
        }
    }
    val time = { clock.value }
    BoxWithConstraints(
        Modifier.fillMaxSize()
            .graphicsLayer {
                if (reducedMotion) {
                    alpha = stillAlpha
                } else {
                    val f = IntroTimeline.fade(clock.value)
                    alpha = 1f - f
                    // We go into the app, the screen does not switch off (the background zooms with the scene).
                    val z = 1f + IntroTimeline.ZOOM * f
                    scaleX = z
                    scaleY = z
                }
            }
            .background(BrandMark.Background)
            .pointerInput(Unit) { detectTapGestures { skip() } }
            .semantics { contentDescription = "BenStudio" }
            .testTag("intro"),
        contentAlignment = Alignment.Center,
    ) {
        // The splash draws its icon on 288 dp: the same here, so the hand-over does not jump.
        // Smaller when the window is (a phone lying down).
        val box = minOf(288.dp, maxHeight * 0.75f, maxWidth * 0.8f)
        val lift = { box.value * IntroTimeline.LIFT * IntroTimeline.rise(clock.value) }
        if (!reducedMotion) {
            // The warm light over the whole screen, behind the mark (none before the first beat), and the
            // screen's own vignette (it does not rise: it is the screen's, not the mark's).
            Box(
                Modifier.fillMaxSize().drawWithCache {
                    val c = Offset(size.width / 2, size.height / 2)
                    val r = max(size.width, size.height) * 0.6f
                    val light = Brush.radialGradient(
                        0f to BrandMark.Orange.copy(alpha = 0.26f), 0.35f to Color(0xFFFF5E7A).copy(alpha = 0.09f), 1f to Color.Transparent,
                        center = c, radius = r,
                    )
                    val vignette = Brush.radialGradient(
                        0f to Color.Transparent, 0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = IntroTimeline.VIGNETTE_MAX),
                        center = c, radius = sqrt(size.width * size.width + size.height * size.height) / 2f,   // the corners are at 1
                    )
                    onDrawBehind {
                        val t = clock.value
                        val a = IntroTimeline.ambient(t)
                        if (a > 0.002f) translate(top = -lift().dp.toPx() * IntroTimeline.AMBIENT_RISE) { drawCircle(light, r, c, alpha = a) }
                        val v = IntroTimeline.vignette(t)
                        if (v > 0.002f && IntroTimeline.VIGNETTE_MAX > 0f) drawRect(vignette, alpha = v)
                    }
                },
            )
        }
        val rising = Modifier.graphicsLayer { translationY = -lift().dp.toPx() }
        if (!reducedMotion) BeatBurst(box, rising, time)
        BenStudioMark(
            box,
            rising.graphicsLayer {
                val s = if (reducedMotion) 1f else IntroTimeline.heartbeat(clock.value)
                scaleX = s
                scaleY = s
            },
            time = if (reducedMotion) null else time,
        )
        // The name under the mark, centred: the light sets it down (it comes up from a little below, a little
        // stretched), a light runs over it; then a short line grows under it from the centre.
        Column(
            Modifier.align(Alignment.TopCenter).padding(top = maxHeight / 2 + box * IntroTimeline.NAME_TOP).graphicsLayer {
                val t = clock.value
                val q = IntroTimeline.nameSettle(t)
                alpha = IntroTimeline.nameAlpha(t)
                translationY = (1f - q) * IntroTimeline.NAME_DROP_DP.dp.toPx()
                scaleX = 1f + IntroTimeline.NAME_STRETCH_X * (1f - q)
                scaleY = 1f + IntroTimeline.NAME_STRETCH_Y * (1f - q)
            },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BenStudioWordmark(
                Modifier.testTag("intro_name")
                    // Its own layer, so the light paints over the letters only.
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithCache {
                        val band = size.height
                        val shine = Brush.linearGradient(
                            0f to Color.Transparent, 0.5f to Color.White.copy(alpha = IntroTimeline.NAME_SHINE_ALPHA), 1f to Color.Transparent,
                            start = Offset(-band / 2, -band * 0.15f), end = Offset(band / 2, band * 0.15f),
                        )
                        onDrawWithContent {
                            drawContent()
                            val x = if (reducedMotion) -1f else IntroTimeline.nameShine(clock.value)
                            if (x in 0f..1f) {
                                val at = -band + (size.width + 2f * band) * x
                                translate(left = at) { drawRect(shine, topLeft = Offset(-at, 0f), size = size, blendMode = BlendMode.SrcAtop) }
                            }
                        }
                    },
            )
            Box(
                Modifier.offset(y = 6.dp).width(64.dp).height(3.dp).graphicsLayer {
                    scaleX = IntroTimeline.line(clock.value)
                }.background(BrandMark.line, androidx.compose.foundation.shape.RoundedCornerShape(2.dp)),
            )
        }
    }
}
