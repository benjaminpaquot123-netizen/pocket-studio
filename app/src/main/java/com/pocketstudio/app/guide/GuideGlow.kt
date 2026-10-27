package com.pocketstudio.app.guide

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color

/**
 * The Guide's window beats like a heart (owner's request, after APK 224): a faint cyan edge that
 * swells twice, « boum-boum » as the BenStudio opening does, then rests; [PERIOD_MS] between double
 * beats. Low on purpose: a presence, never a call. The phone's « Supprimer les animations »: a
 * still accent edge ([STILL]).
 */
object GuideHeartbeat {
    const val PERIOD_MS = 1400
    /** The second beat comes 160 ms after the first, softer (the intro's timing). */
    private val BEAT_START = floatArrayOf(0f, 160f)
    private val BEAT_PEAK = floatArrayOf(1f, 0.6f)
    private const val UP = 70f
    private const val DOWN = 260f

    /** The edge's alpha between beats, and how much a beat adds. */
    const val REST = 0.22f
    const val SWELL = 0.30f
    /** The still edge (animations off). */
    const val STILL = 0.4f

    /** 0..1: how lit the edge is [t] ms into a period. */
    fun beat(t: Float): Float {
        var v = 0f
        for (k in BEAT_START.indices) {
            val u = t - BEAT_START[k]
            val x = when {
                u < 0f || u > UP + DOWN -> 0f
                u < UP -> u / UP
                else -> 1f - (u - UP) / DOWN
            }
            v = maxOf(v, BEAT_PEAK[k] * x * x * (3f - 2f * x))
        }
        return v
    }
}

/** Tests: whether the Guide's edge beats ("beating") or stays still ("still"). */
val GuideGlowKey = SemanticsPropertyKey<String>("GuideGlow")
private var SemanticsPropertyReceiver.guideGlow by GuideGlowKey

/** The time in the heartbeat's period (null when it stays still); read only when drawing: nothing recomposes. */
@Composable
fun rememberGuideBeat(animated: Boolean): State<Float>? {
    if (!animated) return null
    val transition = rememberInfiniteTransition(label = "guide_heartbeat")
    return transition.animateFloat(
        0f, GuideHeartbeat.PERIOD_MS.toFloat(),
        infiniteRepeatable(tween(GuideHeartbeat.PERIOD_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "guide_beat",
    )
}

/** The heartbeat's edge drawn over the window's border ([beat] null: the still accent edge). */
fun Modifier.guideEdge(beat: State<Float>?, color: Color): Modifier = this
    .semantics { guideGlow = if (beat != null) "beating" else "still" }
    .drawWithContent {
        drawContent()
        val lit = beat?.let { GuideHeartbeat.REST + GuideHeartbeat.SWELL * GuideHeartbeat.beat(it.value) } ?: GuideHeartbeat.STILL
        val w = 1.5.dp.toPx()
        val r = CornerRadius(10.dp.toPx())
        // A soft glow inside the edge (wider, fainter), then the edge itself.
        val glow = 6.dp.toPx()
        drawRoundRect(color.copy(alpha = lit * 0.25f), Offset(glow / 2, glow / 2), Size(size.width - glow, size.height - glow), r, style = Stroke(glow))
        drawRoundRect(color.copy(alpha = lit), Offset(w / 2, w / 2), Size(size.width - w, size.height - w), r, style = Stroke(w))
    }
