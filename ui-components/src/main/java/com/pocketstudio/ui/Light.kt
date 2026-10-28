package com.pocketstudio.ui

import android.graphics.Bitmap
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.shadow.DropShadowPainter
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.PI
import kotlin.math.cos

/**
 * The phone's animations are turned off (Settings › Accessibility): the lights stay still (no
 * breathing, no falling peak). Read once per screen; the same test as the intro's.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
            .getOrDefault(false)
    }
}

/**
 * The music's breath, for the lights of what plays: 1 on each beat, 0 half a beat later, as a
 * cosine (no flash, no jump), 0 when stopped. Read [value] while drawing only: it changes every
 * frame during the song, so a composition reading it would recompose 120 times a second.
 */
@Stable
class Breath internal constructor() {
    internal val level = mutableFloatStateOf(0f)
    /** True while it breathes (playing, animations on): otherwise the lights stay still. */
    internal val breathing = mutableStateOf(false)
    val value: Float get() = level.floatValue
}

/** The app's [Breath] (null: none, the lights of what plays stay still, as in the tests). */
val LocalBreath = staticCompositionLocalOf<Breath?> { null }

/** Sixteenths in a beat: the engine's steps. */
private const val STEPS_PER_BEAT = 4

/**
 * One [Breath] for the whole app, locked on the engine's beat: [playing] and [step] (sixteenths
 * since play, negative while stopped or counting in) are polled on each frame of the song only,
 * [bpm] gives the length of a beat between two polls. Nothing runs while stopped, nor with the
 * phone's animations off. The poll is a read of two atomics; nothing is allocated per frame.
 */
@Composable
fun rememberBreath(playing: () -> Boolean, step: () -> Int, bpm: () -> Float): Breath {
    val breath = remember { Breath() }
    val still = rememberReducedMotion()
    val isPlaying by rememberUpdatedState(playing)
    val stepOf by rememberUpdatedState(step)
    val bpmOf by rememberUpdatedState(bpm)
    LaunchedEffect(still) {
        if (still) return@LaunchedEffect
        snapshotFlow { isPlaying() }.collectLatest { on ->
            breath.level.floatValue = 0f
            breath.breathing.value = on
            if (!on) return@collectLatest
            var beat = Int.MIN_VALUE
            var start = 0L
            while (true) {
                withInfiniteAnimationFrameNanos { now ->
                    val s = stepOf()
                    if (s < 0) {
                        breath.level.floatValue = 0f
                    } else {
                        val b = s / STEPS_PER_BEAT
                        if (b != beat) {
                            beat = b
                            start = now
                        }
                        val beatNanos = 60e9 / bpmOf().coerceIn(20f, 400f)
                        val phase = ((now - start) / beatNanos).toFloat().coerceIn(0f, 1f)
                        breath.level.floatValue = 0.5f + 0.5f * cos(2f * PI.toFloat() * phase)
                    }
                }
            }
        }
    }
    return breath
}

/**
 * A soft glow around what plays (the Play button, a pad that sounds, the scene playing), in
 * [color], never on anything else: it comes up in 150 ms and goes in 250 ms, and breathes very
 * slightly on the beat with the app's [Breath] (still without one, or with animations off).
 * Drawn behind the element, outside its bounds: put it before any clip. [inset] matches a
 * smaller disc drawn inside a larger touch target. Nothing recomposes while it breathes: the
 * shadow is blurred once and only its alpha changes.
 */
@Composable
fun Modifier.liveGlow(on: Boolean, color: Color, shape: Shape = CircleShape, radius: Dp = 12.dp, inset: Dp = 0.dp): Modifier {
    val fade = animateFloatAsState(if (on) 1f else 0f, tween(if (on) 150 else 250), label = "live-glow")
    val breath = LocalBreath.current
    val still = rememberReducedMotion()
    val glow = remember(shape, color, radius, inset) { DropShadowPainter(shape, Shadow(radius = radius, color = color, spread = -inset)) }
    return drawBehind {
        val f = fade.value
        if (f <= 0.004f) return@drawBehind
        val level = if (breath == null || still || !breath.breathing.value) GLOW_STILL else GLOW_LOW + GLOW_SWING * breath.value
        with(glow) { draw(size, alpha = f * level) }
    }
}

/** The glow's strength: still, and its breath between low and low + swing (a third, felt rather than seen). */
private const val GLOW_STILL = 0.85f
private const val GLOW_LOW = 0.62f
private const val GLOW_SWING = 0.38f

/**
 * A very fine grain over the panels (a 96 px tile of ±1.5 % light), so their gradients never
 * band and the graphite reads as a material, not a flat fill. Made once; drawing it is one
 * shader rectangle. Off in the screenshot tests (where it would only make the images ten times
 * heavier), [enabled] turns it back on for the one capture that shows it.
 */
object PanelGrain {
    var enabled: Boolean = Build.FINGERPRINT != "robolectric"

    val brush: ShaderBrush by lazy {
        val n = 96
        val random = java.util.Random(0x6B5)
        val pixels = IntArray(n * n) {
            // A third of the pixels, a little lighter or darker: sparse, so it stays a texture.
            when {
                random.nextInt(3) != 0 -> 0
                random.nextBoolean() -> ((2 + random.nextInt(4)) shl 24) or 0xFFFFFF
                else -> (4 + random.nextInt(6)) shl 24
            }
        }
        val bitmap = Bitmap.createBitmap(pixels, n, n, Bitmap.Config.ARGB_8888)
        ShaderBrush(ImageShader(bitmap.asImageBitmap(), TileMode.Repeated, TileMode.Repeated))
    }
}
