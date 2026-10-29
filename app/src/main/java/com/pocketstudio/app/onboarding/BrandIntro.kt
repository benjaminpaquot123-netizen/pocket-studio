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
import androidx.compose.ui.graphics.StrokeCap
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
        val ringStroke = Stroke(width = 1.6f * u)

        // The opening's extras (none on the still mark, none before the splash hands over).
        /** A light from the top on each pad, for relief. */
        val gloss = Array(PADS) { i ->
            val p = pads[i]
            Brush.verticalGradient(0f to Color.White.copy(alpha = 0.2f), 1f to Color.Transparent, startY = p.top, endY = p.top + p.height * 0.48f)
        }
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
 * half of it). With [time] (ms on [IntroTimeline]), the pads pop in, flash on the beats, the halo
 * comes up and pulses, the pads take relief and a light runs over them; read while drawing only,
 * so an animation redraws without composing again (and allocates nothing).
 */
@Composable
fun BenStudioMark(size: Dp, modifier: Modifier = Modifier, glow: Boolean = true, time: (() -> Float)? = null) {
    Box(
        modifier.size(size).drawWithCache {
            val kit = BrandMark.Kit(this.size.minDimension)
            onDrawBehind {
                val t = time?.invoke()
                if (glow) {
                    val haloAlpha = if (t == null) 1f else IntroTimeline.halo(t)
                    val haloScale = if (t == null) 1f else IntroTimeline.haloScale(t)
                    scale(haloScale, pivot = kit.haloCenter) {
                        drawCircle(kit.halo, 44f * kit.u, kit.haloCenter, alpha = haloAlpha)
                        val hot = if (t == null) 0f else IntroTimeline.beatGlow(t)
                        if (hot > 0.002f) drawCircle(kit.hotHalo, 34f * kit.u, kit.haloCenter, alpha = hot.coerceAtMost(1f))
                    }
                }
                val relief = if (t == null) 0f else IntroTimeline.relief(t)
                val sheen = if (t == null) -1f else IntroTimeline.sheen(t)
                for (i in 0 until BrandMark.PADS) {
                    val pop = if (t == null) 1f else IntroTimeline.pop(i, t)
                    if (pop <= 0.001f) continue
                    val p = kit.pads[i]
                    scale(pop, pivot = BrandMark.mid(p)) {
                        if (relief > 0f) translate(top = kit.shadowDrop) { drawPath(kit.paths[i], Color.Black, alpha = 0.32f * relief) }
                        if (i == BrandMark.LIT) drawCircle(kit.lit, kit.litRadius, kit.litCenter)
                        drawPath(kit.paths[i], kit.fills[i])
                        drawPath(kit.paths[i], kit.inner[i])
                        drawPath(kit.edges[i], kit.edgeLight[i], style = kit.edgeStroke)
                        if (relief > 0f) drawPath(kit.paths[i], kit.gloss[i], alpha = relief)
                        val flash = if (t == null) 0f else IntroTimeline.flash(i, t)
                        if (flash > 0f) drawPath(kit.paths[i], Color.White, alpha = flash)
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
 * What goes out of the mark on each heartbeat (same box as [BenStudioMark]): a soft shock wave of
 * light, a fine ring, and a few sparks. Everything is made once per size; nothing is allocated
 * while it draws.
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
            val sparkFrom = 30f * u
            val sparks = IntroTimeline.Sparks
            onDrawBehind {
                val t = time()
                for (k in 0 until IntroTimeline.BEAT_COUNT) {
                    val w = IntroTimeline.shockProgress(k, t)
                    if (w > 0f && w < 1f) {
                        val s = 0.7f + 1.2f * IntroTimeline.easeOut(w)
                        scale(s, pivot = center) { drawCircle(shock, shockRadius, center, alpha = IntroTimeline.shockAlpha(k) * (1f - w) * (1f - w)) }
                    }
                    val x = IntroTimeline.ringProgress(k, t)
                    if (x > 0f && x < 1f) {
                        val s = 0.85f + 1.05f * IntroTimeline.easeOut(x)
                        drawCircle(BrandMark.Ring, 31f * u * s, center, alpha = IntroTimeline.ringAlpha(k) * (1f - x), style = kit.ringStroke)
                    }
                    for (j in 0 until sparks.count(k)) {
                        val q = sparks.progress(k, j, t)
                        if (q <= 0f || q >= 1f) continue
                        val travel = sparkFrom + sparks.reach(k, j) * u * IntroTimeline.easeOut(q)
                        val trail = sparks.length(k, j) * u * (1f - q)
                        val dx = sparks.dx(k, j)
                        val dy = sparks.dy(k, j)
                        drawLine(
                            sparks.color(j),
                            Offset(center.x + dx * (travel - trail), center.y + dy * (travel - trail)),
                            Offset(center.x + dx * travel, center.y + dy * travel),
                            strokeWidth = sparks.width(j) * u * (1f - 0.5f * q),
                            cap = StrokeCap.Round,
                            alpha = IntroTimeline.sparkAlpha(k) * (1f - q) * (1f - q),
                        )
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
 * The opening's timeline, in ms from the splash's start (scripts/brand/render_brand.py has the
 * same, scripts/brand/render_heartbeat.py the sound's times). The splash (at most [SPLASH_MS])
 * pops the pads in and draws a breath in; then, drawn by the app, in step with its sound: a double
 * heartbeat (the mark punches out on a spring, a shock wave of light, a ring and sparks on each
 * beat, the pads flashing in turn, the whole screen warming), a light over the pads, the mark
 * rises, « BenStudio » comes in under it and a light runs over it; the halo breathes; a fade.
 * 4.7 s from the tap on the icon (3.8 s before 2026-10-29).
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
    private val BEAT_FLASH = floatArrayOf(0.6f, 0.38f)
    private val BEAT_GLOW = floatArrayOf(1f, 0.62f)
    private val RING_ALPHA = floatArrayOf(0.9f, 0.6f)
    private val SHOCK_ALPHA = floatArrayOf(0.85f, 0.5f)
    private val SPARK_ALPHA = floatArrayOf(0.85f, 0.6f)

    /** The punch: up in [BEAT_UP], then a damped spring (period [SPRING_MS], decay [SPRING_DECAY]). */
    const val BEAT_UP = 55f
    const val SPRING_MS = 300f
    const val SPRING_DECAY = 85f
    const val SPRING_END = 700f
    const val GLOW_UP = 40f
    const val GLOW_DECAY = 240f
    const val RING_MS = 480f
    const val SHOCK_MS = 480f
    const val FLASH_STAGGER = 35f
    const val FLASH_UP = 40f
    const val FLASH_DOWN = 240f

    /** The pads take relief once the app draws (the splash's pads are flat: no jump at the hand-over). */
    const val RELIEF_START = 1000f
    const val RELIEF_END = 1300f
    const val AMBIENT_END = 1500f
    const val SHEEN_START = 1480f
    const val SHEEN_END = 1980f
    const val RISE_START = 1650f
    const val RISE_END = 2050f
    const val NAME_START = 1750f
    const val NAME_END = 2850f
    const val NAME_SHINE_START = 2350f
    const val NAME_SHINE_END = 3050f
    const val LINE_START = 2550f
    const val LINE_END = 3150f
    const val BREATH_START = 2050f
    const val BREATH_MS = 1800f
    const val FADE_START = 4400f
    const val TOTAL = 4700f

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

    /** The light over the pads: 0..1 across them while it runs, -1 otherwise. */
    fun sheen(t: Float): Float = if (t <= SHEEN_START || t >= SHEEN_END) -1f else ease(ramp(t, SHEEN_START, SHEEN_END))

    /** The light over the name: 0..1 across it while it runs, -1 otherwise. */
    fun nameShine(t: Float): Float = if (t <= NAME_SHINE_START || t >= NAME_SHINE_END) -1f else ease(ramp(t, NAME_SHINE_START, NAME_SHINE_END))

    /** How lit pad [i] is: struck in turn on each beat, the second softer. */
    fun flash(i: Int, t: Float): Float {
        var v = 0f
        for (k in 0 until BEAT_COUNT) {
            val u = t - BEAT_START[k] - i * FLASH_STAGGER
            val peak = BEAT_FLASH[k]
            v = when {
                u < 0f -> v
                u < FLASH_UP -> max(v, peak * u / FLASH_UP)
                u < FLASH_UP + FLASH_DOWN -> max(v, peak * (1f - (u - FLASH_UP) / FLASH_DOWN))
                else -> v
            }
        }
        return v
    }

    fun ringProgress(k: Int, t: Float): Float = ramp(t, BEAT_START[k], BEAT_START[k] + RING_MS)

    fun ringAlpha(k: Int): Float = RING_ALPHA[k]

    fun shockProgress(k: Int, t: Float): Float = ramp(t, BEAT_START[k], BEAT_START[k] + SHOCK_MS)

    fun shockAlpha(k: Int): Float = SHOCK_ALPHA[k]

    fun sparkAlpha(k: Int): Float = SPARK_ALPHA[k]

    /**
     * The sparks of each beat, laid out once (a fixed seed: the same every time): a direction,
     * a reach, a trail, a width, a small delay; fewer and shorter on the second beat.
     */
    object Sparks {
        private val COUNT = intArrayOf(12, 7)
        private val LIFE = floatArrayOf(560f, 440f)
        private const val MAX = 12
        private val dx = FloatArray(BEAT_COUNT * MAX)
        private val dy = FloatArray(BEAT_COUNT * MAX)
        private val reach = FloatArray(BEAT_COUNT * MAX)
        private val length = FloatArray(BEAT_COUNT * MAX)
        private val delay = FloatArray(BEAT_COUNT * MAX)
        private val widths = FloatArray(MAX)
        private val colors = arrayOf(Color(0xFFFFE09A), Color(0xFFFFB35C), Color(0xFFFFF4E0), Color(0xFFFF8C5A))

        init {
            val r = java.util.Random(0xB3A7L)
            for (k in 0 until BEAT_COUNT) for (j in 0 until COUNT[k]) {
                val n = k * MAX + j
                // All around but uneven (sparks, not a clock's ticks).
                val a = (j + 0.5f + (r.nextFloat() - 0.5f) * 0.95f) / COUNT[k] * 2f * PI.toFloat() + k * 0.4f
                dx[n] = cos(a)
                dy[n] = sin(a)
                reach[n] = (if (k == 0) 10f else 7f) + r.nextFloat() * (if (k == 0) 20f else 12f)
                length[n] = 2f + r.nextFloat() * 3.5f
                delay[n] = r.nextFloat() * 60f
            }
            for (j in 0 until MAX) widths[j] = 0.6f + r.nextFloat() * 0.6f
        }

        fun count(k: Int): Int = COUNT[k]

        /** 0..1 over the spark's life (outside: not drawn). */
        fun progress(k: Int, j: Int, t: Float): Float {
            val from = BEAT_START[k] + delay[k * MAX + j]
            return ramp(t, from, from + LIFE[k])
        }

        fun dx(k: Int, j: Int): Float = dx[k * MAX + j]

        fun dy(k: Int, j: Int): Float = dy[k * MAX + j]

        /** How far it flies, in icon units, beyond the pads. */
        fun reach(k: Int, j: Int): Float = reach[k * MAX + j]

        fun length(k: Int, j: Int): Float = length[k * MAX + j]

        fun width(j: Int): Float = widths[j]

        fun color(j: Int): Color = colors[j % colors.size]
    }
}

/** The phone's « Supprimer les animations » (animator duration scale 0). */
fun animationsOff(context: android.content.Context): Boolean =
    runCatching { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }.getOrDefault(false)

/**
 * The opening, after the system splash (the same pads, at the same place, on the same colour,
 * on the same timeline: it goes on from where the splash was, [Intro.splashElapsedMs]): the pads
 * pop in and draw a breath, beat twice like a heart (a shock wave of light, a ring and sparks on
 * each beat, the pads flashing in turn, the screen warming), a light runs over the pads, the mark
 * rises and « BenStudio » comes in under it, a light over it, the halo breathes, and everything
 * fades into the app. 4.7 s from the tap on the icon; a touch skips it. [cue] hears (or feels)
 * the two beats, in step ([IntroCue], decided by MainActivity). It is drawn over the app, which
 * starts underneath meanwhile (the project loads, the audio engine starts): nothing waits for it.
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
            clock.animateTo(IntroTimeline.TOTAL, tween((IntroTimeline.TOTAL - from).toInt(), easing = LinearEasing))
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
                alpha = if (reducedMotion) stillAlpha else 1f - IntroTimeline.ease(IntroTimeline.ramp(clock.value, IntroTimeline.FADE_START, IntroTimeline.TOTAL))
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
        val lift = { box.value * IntroTimeline.LIFT * IntroTimeline.ease(IntroTimeline.ramp(clock.value, IntroTimeline.RISE_START, IntroTimeline.RISE_END)) }
        if (!reducedMotion) {
            // The warm light over the whole screen, behind the mark (none before the first beat).
            Box(
                Modifier.fillMaxSize().drawWithCache {
                    val c = Offset(size.width / 2, size.height / 2)
                    val r = max(size.width, size.height) * 0.6f
                    val light = Brush.radialGradient(
                        0f to BrandMark.Orange.copy(alpha = 0.26f), 0.35f to Color(0xFFFF5E7A).copy(alpha = 0.09f), 1f to Color.Transparent,
                        center = c, radius = r,
                    )
                    onDrawBehind {
                        val a = IntroTimeline.ambient(clock.value)
                        if (a > 0.002f) translate(top = -lift().dp.toPx()) { drawCircle(light, r, c, alpha = a) }
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
        // The name under the mark, centred: it fades in and settles, a light runs over it; then a short line grows under it.
        Column(
            Modifier.align(Alignment.TopCenter).padding(top = maxHeight / 2 + box * IntroTimeline.NAME_TOP).graphicsLayer {
                val show = IntroTimeline.ease(IntroTimeline.ramp(clock.value, IntroTimeline.NAME_START, IntroTimeline.NAME_END))
                alpha = show
                translationY = (1f - show) * 18.dp.toPx()
                val s = 0.96f + 0.04f * show
                scaleX = s
                scaleY = s
            },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BenStudioWordmark(
                Modifier.testTag("intro_name")
                    // Its own layer, so the light paints over the letters only.
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithCache {
                        val band = size.height * 1.4f
                        val shine = Brush.linearGradient(
                            0f to Color.Transparent, 0.5f to Color.White.copy(alpha = 0.85f), 1f to Color.Transparent,
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
                    scaleX = IntroTimeline.ease(IntroTimeline.ramp(clock.value, IntroTimeline.LINE_START, IntroTimeline.LINE_END))
                }.background(BrandMark.line, androidx.compose.foundation.shape.RoundedCornerShape(2.dp)),
            )
        }
    }
}
