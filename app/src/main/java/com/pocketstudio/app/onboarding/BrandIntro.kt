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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
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
import kotlin.math.max

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

        private fun shrink(c: CornerRadius, by: Float) = CornerRadius(max(0f, c.x - by), max(0f, c.y - by))
    }

    val wordAccent = Brush.horizontalGradient(listOf(Amber, Color(0xFFFF7A3D)))
    val line = Brush.horizontalGradient(listOf(Color(0xFFFFA347), Magenta))
}

/**
 * The mark drawn in a square box ([size] stands for the icon's 108 units: the pads take about
 * half of it). With [time] (ms on [IntroTimeline]), the pads pop in, flash on the beats and the
 * halo comes up; read while drawing only, so an animation redraws without composing again.
 */
@Composable
fun BenStudioMark(size: Dp, modifier: Modifier = Modifier, glow: Boolean = true, time: (() -> Float)? = null) {
    Box(
        modifier.size(size).drawWithCache {
            val kit = BrandMark.Kit(this.size.minDimension)
            onDrawBehind {
                val t = time?.invoke()
                if (glow) drawCircle(kit.halo, 44f * kit.u, kit.haloCenter, alpha = if (t == null) 1f else IntroTimeline.ramp(t, 120f, 520f))
                for (i in 0 until BrandMark.PADS) {
                    val pop = if (t == null) 1f else IntroTimeline.pop(i, t)
                    if (pop <= 0.001f) continue
                    val p = kit.pads[i]
                    scale(pop, pivot = BrandMark.mid(p)) {
                        if (i == BrandMark.LIT) drawCircle(kit.lit, kit.litRadius, kit.litCenter)
                        drawPath(kit.paths[i], kit.fills[i])
                        drawPath(kit.paths[i], kit.inner[i])
                        drawPath(kit.edges[i], kit.edgeLight[i], style = kit.edgeStroke)
                        val flash = if (t == null) 0f else IntroTimeline.flash(i, t)
                        if (flash > 0f) drawPath(kit.paths[i], Color.White, alpha = flash)
                    }
                }
            }
        },
    )
}

/** The rings that go out on each heartbeat, around the mark (same box as [BenStudioMark]). */
@Composable
private fun BeatRings(size: Dp, modifier: Modifier, time: () -> Float) {
    Box(
        modifier.size(size).drawWithCache {
            val kit = BrandMark.Kit(this.size.minDimension)
            val center = Offset(this.size.width / 2, this.size.height / 2)
            onDrawBehind {
                val t = time()
                for (k in 0 until IntroTimeline.BEAT_COUNT) {
                    val x = IntroTimeline.ringProgress(k, t)
                    if (x <= 0f || x >= 1f) continue
                    val s = 0.85f + 0.75f * IntroTimeline.easeOut(x)
                    drawCircle(BrandMark.Ring, 31f * kit.u * s, center, alpha = IntroTimeline.ringAlpha(k) * (1f - x), style = kit.ringStroke)
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
 * same): the pads pop in, a double heartbeat (two swells 160 ms apart, a ring out on each, the
 * pads flashing in turn), then the mark rises and « BenStudio » comes in under it; a fade.
 */
object IntroTimeline {
    const val POP_STAGGER = 60f
    const val POP_MS = 260f
    const val BEAT_COUNT = 2
    private val BEAT_START = floatArrayOf(480f, 640f)
    private val BEAT_SWELL = floatArrayOf(0.12f, 0.07f)
    private val BEAT_FLASH = floatArrayOf(0.55f, 0.35f)
    private val RING_ALPHA = floatArrayOf(0.85f, 0.6f)
    const val BEAT_UP = 70f
    const val BEAT_DOWN = 160f
    const val RING_MS = 360f
    const val FLASH_STAGGER = 35f
    const val FLASH_UP = 40f
    const val FLASH_DOWN = 220f
    const val RISE_START = 900f
    const val RISE_END = 1250f
    const val NAME_START = 1000f
    const val NAME_END = 1400f
    const val LINE_START = 1150f
    const val LINE_END = 1450f
    const val FADE_START = 1500f
    const val TOTAL = 1800f

    /** Of the 288 dp box: how far the mark goes up, where the name's top sits under the centre. */
    const val LIFT = 0.13f
    const val NAME_TOP = 0.19f

    /** Asked to skip (a touch): the fade only. */
    const val SKIP_FADE_MS = 180

    /** With the phone's animations off: still, then a short fade. */
    const val STILL_MS = 450L
    const val STILL_FADE_MS = 200L

    fun ramp(t: Float, from: Float, to: Float): Float = ((t - from) / (to - from)).coerceIn(0f, 1f)

    fun ease(x: Float): Float = x * x * (3f - 2f * x)

    fun easeOut(x: Float): Float = 1f - (1f - x) * (1f - x) * (1f - x)

    /** Android's overshoot interpolator (tension 2), as the splash's. */
    private fun overshoot(x: Float): Float {
        val y = x - 1f
        return y * y * (3f * y + 2f) + 1f
    }

    fun pop(i: Int, t: Float): Float {
        val x = ramp(t, i * POP_STAGGER, i * POP_STAGGER + POP_MS)
        return if (x < 1f) overshoot(x) else 1f
    }

    private fun pulse(u: Float): Float = when {
        u < 0f || u > BEAT_UP + BEAT_DOWN -> 0f
        u < BEAT_UP -> easeOut(u / BEAT_UP)
        else -> 1f - ease((u - BEAT_UP) / BEAT_DOWN)
    }

    /** The mark's size on the double heartbeat (1 between beats). */
    fun heartbeat(t: Float): Float {
        var s = 1f
        for (k in 0 until BEAT_COUNT) s *= 1f + BEAT_SWELL[k] * pulse(t - BEAT_START[k])
        return s
    }

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
}

/** The phone's « Supprimer les animations » (animator duration scale 0). */
fun animationsOff(context: android.content.Context): Boolean =
    runCatching { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }.getOrDefault(false)

/**
 * The opening, after the system splash (the same pads, at the same place, on the same colour,
 * on the same timeline: it goes on from where the splash was, [Intro.splashElapsedMs]): the pads
 * pop in, beat twice like a heart (a neon ring out on each beat, the pads flashing in turn), then
 * the mark rises and « BenStudio » comes in under it, and everything fades into the app. 1.8 s
 * from the tap on the icon; a touch skips it; silent. It is drawn over the app, which starts
 * underneath meanwhile (the project loads, the audio engine starts): nothing waits for it. With
 * the phone's animations off: the mark and the name, still, then a short fade.
 */
@Composable
fun BrandIntro(onDone: () -> Unit, reducedMotion: Boolean = animationsOff(LocalContext.current)) {
    // Progress in ms on the timeline; read in the drawing phase only.
    val clock = remember { Animatable(if (reducedMotion) IntroTimeline.FADE_START else 0f) }
    var stillAlpha by remember { mutableFloatStateOf(1f) }
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
    }
    val skip: () -> Unit = {
        if (reducedMotion) {
            stillAlpha = 0f
            onDone()
        } else scope.launch {
            val from = max(clock.value, IntroTimeline.FADE_START + (IntroTimeline.TOTAL - IntroTimeline.FADE_START) * 0.4f)
            clock.snapTo(from)
            clock.animateTo(IntroTimeline.TOTAL, tween(IntroTimeline.SKIP_FADE_MS, easing = LinearEasing))
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
        val rising = Modifier.graphicsLayer {
            translationY = -box.toPx() * IntroTimeline.LIFT * IntroTimeline.ease(IntroTimeline.ramp(clock.value, IntroTimeline.RISE_START, IntroTimeline.RISE_END))
        }
        BeatRings(box, rising, time)
        BenStudioMark(
            box,
            rising.graphicsLayer {
                val s = IntroTimeline.heartbeat(clock.value)
                scaleX = s
                scaleY = s
            },
            time = if (reducedMotion) null else time,
        )
        // The name under the mark, centred: it fades in and settles; then a short line grows under it.
        Column(
            Modifier.align(Alignment.TopCenter).padding(top = maxHeight / 2 + box * IntroTimeline.NAME_TOP).graphicsLayer {
                val show = IntroTimeline.ease(IntroTimeline.ramp(clock.value, IntroTimeline.NAME_START, IntroTimeline.NAME_END))
                alpha = show
                translationY = (1f - show) * 18.dp.toPx()
            },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BenStudioWordmark(Modifier.testTag("intro_name"))
            Box(
                Modifier.offset(y = 6.dp).width(64.dp).height(3.dp).graphicsLayer {
                    scaleX = IntroTimeline.ease(IntroTimeline.ramp(clock.value, IntroTimeline.LINE_START, IntroTimeline.LINE_END))
                }.background(BrandMark.line, androidx.compose.foundation.shape.RoundedCornerShape(2.dp)),
            )
        }
    }
}
