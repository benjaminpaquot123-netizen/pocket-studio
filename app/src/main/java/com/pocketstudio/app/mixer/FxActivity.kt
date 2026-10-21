package com.pocketstudio.app.mixer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.Insert
import com.pocketstudio.ui.Neon
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.tanh

/** Points of an effect's mini view. */
private const val MINI_POINTS = 32

/**
 * The shape of what effect [insert] does, from its settings, as [points] values in -1..1 drawn
 * left to right (0 = the middle line): a filter or EQ its curve over the frequencies, an echo
 * its taps fading, a reverb its tail, a distortion its curve, a wobble or chorus its wave, a
 * chopper or gate its slices, a pump its breathing on the beats.
 */
fun miniCurve(insert: Insert, points: Int = MINI_POINTS): FloatArray {
    val p = insert.params
    fun at(k: Int) = p.getOrElse(k) { 0f }
    val type = insert.effect
    return FloatArray(points) { i ->
        val x = i / (points - 1f)
        val v = when (type) {
            EffectType.EQ, EffectType.PARAM_EQ -> (EqMath.responseDb(insert, 20.0 * 1000.0.pow(x.toDouble())) / 18.0).toFloat()
            EffectType.FILTER -> {
                val c = 0.1f + 0.8f * at(0)
                val bump = at(1) * 0.7f * exp(-((x - c) * 10).pow(2))
                when ((at(2) * 3).toInt().coerceIn(0, 2)) {
                    0 -> (if (x < c) 0.5f else 0.5f - (x - c) * 5f) + bump
                    2 -> (if (x > c) 0.5f else 0.5f - (c - x) * 5f) + bump
                    else -> -0.6f + 1.1f * exp(-((x - c) * 6).pow(2)) + bump
                }
            }
            EffectType.WAH -> -0.6f + 1.3f * exp(-((x - 0.3f - 0.4f * at(0)) * 7).pow(2))
            EffectType.ECHO, EffectType.PING_PONG -> {
                // Taps every eighth of the view, each one quieter (the feedback).
                val tap = floor(x * 6f)
                val near = abs(x * 6f - tap) < 0.18f
                if (near) (0.95f * (0.25f + 0.7f * at(1)).pow(tap)) * if (type == EffectType.PING_PONG && tap.toInt() % 2 == 1) -1f else 1f else 0f
            }
            EffectType.REVERB -> exp(-x * (6f - 4.5f * at(0))) * (if (i % 2 == 0) 0.9f else -0.9f)
            EffectType.DISTORTION, EffectType.SATURATION, EffectType.OTT, EffectType.COMPRESSOR -> {
                val drive = when (type) {
                    EffectType.COMPRESSOR -> 1f + 3f * (1f - at(0))
                    EffectType.OTT -> 1f + 4f * at(0)
                    else -> 1f + 8f * at(0)
                }
                (tanh(drive * (2f * x - 1f)) / tanh(drive)).toFloat()
            }
            EffectType.BITCRUSHER, EffectType.VINYL, EffectType.RADIO -> {
                val steps = 3f + (1f - at(0)) * 10f
                val s = sin(2 * PI * x).toFloat()
                floor(s * steps) / steps
            }
            EffectType.CHOPPER, EffectType.GATE -> if (floor(x * 8f).toInt() % 2 == 0) 0.8f else -0.1f
            EffectType.TREMOLO -> {
                val s = sin(2 * PI * x * (1f + 3f * at(0))).toFloat()
                (if (at(2) >= 0.66f) sign(s) else s) * (0.2f + 0.7f * at(1))
            }
            EffectType.PUMP -> {
                // Down on each beat, back up over the release.
                val beat = (x * 4f) - floor(x * 4f)
                0.8f - at(0) * 1.4f * exp(-beat * (8f - 6f * at(1)))
            }
            EffectType.WIDENER -> (2f * x - 1f) * (0.3f + 0.6f * at(0)) * if (i % 2 == 0) 1f else -1f
            EffectType.TAPE_STOP -> sin(2 * PI * 4 * x * (1f - 0.6f * x)).toFloat() * (1f - x)
            EffectType.NONE -> 0f
            // Chorus, flanger, phaser, auto-pan, ring modulator, frequency shifter: a wave.
            else -> sin(2 * PI * x * (1f + 3f * at(0))).toFloat() * (0.25f + 0.65f * at(1))
        }
        if (v.isFinite()) v.coerceIn(-1f, 1f) else 0f
    }
}

/**
 * An effect's activity light and mini view, in its family's [colour]: both light up with the
 * sound going through its strip ([level], the strip's meter, read only when drawn: the sound
 * redraws it, never recomposes it). Switched off, they stay grey and still.
 */
@Composable
internal fun FxActivity(insert: Insert, colour: Color, level: () -> Float, tag: String) {
    val curve = remember(insert.effect, insert.params) { miniCurve(insert) }
    val off = Neon.Outline
    val path = remember { Path() }
    Canvas(Modifier.size(width = 40.dp, height = 20.dp).testTag(tag)) {
        val lit = if (insert.bypass) 0f else level().coerceIn(0f, 1f)
        val ink = if (insert.bypass) off else colour
        // The light: a dot that brightens with the sound.
        val r = 3.5.dp.toPx()
        drawCircle(ink.copy(alpha = 0.25f + 0.75f * lit), r, Offset(r, size.height / 2))
        if (lit > 0.05f) drawCircle(ink.copy(alpha = 0.3f * lit), r * 2f, Offset(r, size.height / 2))
        // The mini view, taller with the sound.
        val left = r * 2 + 3.dp.toPx()
        val w = size.width - left
        val mid = size.height / 2
        val amp = mid * (0.55f + 0.45f * lit)
        path.reset()
        curve.forEachIndexed { i, v ->
            val x = left + w * i / (curve.size - 1)
            val y = mid - v * amp
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, ink.copy(alpha = 0.5f + 0.5f * lit), style = Stroke(1.5.dp.toPx()))
    }
}
