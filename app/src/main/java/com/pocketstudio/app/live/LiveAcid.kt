package com.pocketstudio.app.live

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.i18n.percent
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.LiveSlots
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.fadingVerticalEdges
import kotlin.math.roundToInt

/** The 303's parameters played on the Live page (AcidTrack::Param). */
object LiveAcidParams {
    const val CUTOFF = 0
    const val RESONANCE = 1
    const val ENV = 2
    const val DECAY = 3
    const val ACCENT = 4
    const val DRIVE = 7
}

/**
 * The 303 under the fingers: a big pad XY, cutoff (left to right) × resonance (bottom to top),
 * that stays where it is let go (two quick touches: back to the saved sound), and four wide
 * sliders. While a finger is on a control, it wins over the song's curves (touch take-over).
 */
@Composable
fun LiveAcid(ui: LiveScreenUi, actions: LiveActions, modifier: Modifier) {
    val saved = ui.project.acid.sound.values()
    fun value(param: Int) = ui.acid[param] ?: saved[param]
    val stopped = ui.playing && ui.grid?.slot(LiveSlots.ACID)?.playing == -1
    // The pad gives up height first: the four sliders always fit under it (it scrolls only when
    // even its smallest size does not fit).
    BoxWithConstraints(modifier) {
        // The pad's ends are written above and under it (about 20 dp each).
        val sliders = 56.dp * 2 + 8.dp * 2 + 40.dp + if (stopped) 48.dp else 0.dp
        val padHeight = (maxHeight - sliders - 8.dp).coerceIn(140.dp, 220.dp)
    val scroll = rememberScrollState()
    Column(Modifier.fillMaxSize().fadingVerticalEdges(scroll).verticalScroll(scroll).testTag("live_acid"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!ui.hasAcid) {
            // No 303 line yet: nothing to play here, say where it is written.
            Text(
                tr("Pas encore de ligne 303 dans ce morceau : écris-en une dans Sons › Basse acide, elle se jouera ici."),
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Neon.SurfaceHigh).padding(12.dp).testTag("live_acid_none"),
                style = MaterialTheme.typography.bodyMedium,
            )
            return@Column
        }
        if (stopped) {
            Text(
                tr("Basse arrêtée : relance-la depuis la colonne Acide."),
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Neon.Amber.copy(alpha = 0.22f)).padding(8.dp).testTag("live_acid_stopped"),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        AcidXy(ui, actions, padHeight)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BigSlider(tr("Enveloppe"), LiveAcidParams.ENV, value(LiveAcidParams.ENV), actions, Modifier.weight(1f))
            BigSlider(tr("Decay"), LiveAcidParams.DECAY, value(LiveAcidParams.DECAY), actions, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BigSlider(tr("Accent"), LiveAcidParams.ACCENT, value(LiveAcidParams.ACCENT), actions, Modifier.weight(1f))
            BigSlider(tr("Saturation"), LiveAcidParams.DRIVE, value(LiveAcidParams.DRIVE), actions, Modifier.weight(1f))
        }
    }
    }
}

/** Two touches within this many milliseconds: back to the saved setting. */
private const val DOUBLE_TAP_MS = 300L

/** ... and this close to each other (fast drumming across the pad is not a double touch). */
private val DOUBLE_TAP_SLOP = 48.dp

/** A second touch at [at] after one let go at [last] ([lastAt] where): a double touch. */
internal fun isDoubleTouch(now: Long, last: Long, at: Offset, lastAt: Offset, slopPx: Float): Boolean =
    last > 0L && now - last < DOUBLE_TAP_MS && (at - lastAt).getDistance() <= slopPx

/**
 * The 303's pad: cutoff (left to right) × resonance (bottom to top); it stays where it is let go,
 * two touches bring back the saved sound. While a finger is on it, it wins over the song's curves.
 */
@Composable
internal fun AcidXy(ui: LiveScreenUi, actions: LiveActions, height: androidx.compose.ui.unit.Dp) {
    val saved = ui.project.acid.sound.values()
    val cutoff = ui.acid[LiveAcidParams.CUTOFF] ?: saved[LiveAcidParams.CUTOFF]
    val resonance = ui.acid[LiveAcidParams.RESONANCE] ?: saved[LiveAcidParams.RESONANCE]
    LiveXyPad(
        tag = "live_acid_xy",
        description = tr("Pad 303 : coupure %1\$s, résonance %2\$s", percent((cutoff * 100).roundToInt()), percent((resonance * 100).roundToInt())),
        accent = Neon.Acid, height = height,
        top = tr("↑ Résonance"), left = tr("← Fermé"), bottom = tr("Coupure"), right = tr("Ouvert →"), topEnd = tr("Deux touchers : son enregistré"),
        at = { Offset(cutoff, resonance) }, spectrum = ui.spectrum,
        hold = { on ->
            // Let go (or the pad gone mid-gesture): the knobs stay where they are, the song's
            // curves drive them again from the next step.
            actions.holdAcid(LiveAcidParams.CUTOFF, on)
            actions.holdAcid(LiveAcidParams.RESONANCE, on)
        },
        move = { x, y ->
            actions.setAcid(LiveAcidParams.CUTOFF, x)
            actions.setAcid(LiveAcidParams.RESONANCE, y)
        },
        doubleTap = {
            actions.resetAcid(LiveAcidParams.CUTOFF)
            actions.resetAcid(LiveAcidParams.RESONANCE)
        },
    )
}

/**
 * A wide slider for a finger (56 dp tall): the bar fills from the left with the name and the
 * value written inside; it follows the finger, two quick touches bring back the saved setting.
 */
@Composable
private fun BigSlider(label: String, param: Int, value: Float, actions: LiveActions, modifier: Modifier) {
    val act by rememberUpdatedState(actions)
    val lastUp = remember { longArrayOf(0L) }
    val lastAt = remember { arrayOf(Offset.Zero) }
    val accent = Neon.Acid
    val rail = Neon.SurfaceHigh
    Box(
        modifier.height(56.dp).clip(RoundedCornerShape(14.dp)).testTag("live_acid_$param")
            // TalkBack: a slider it can read and move (like the knobs).
            .semantics {
                contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(value.coerceIn(0f, 1f), 0f..1f)
                setProgress { v -> act.setAcid(param, v.coerceIn(0f, 1f)); true }
            }
            .pointerInput(param) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    if (isDoubleTouch(down.uptimeMillis, lastUp[0], down.position, lastAt[0], DOUBLE_TAP_SLOP.toPx())) {
                        act.resetAcid(param)
                        lastUp[0] = 0L
                        return@awaitEachGesture
                    }
                    act.holdAcid(param, true)
                    try {
                        act.setAcid(param, (down.position.x / size.width).coerceIn(0f, 1f))
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                lastUp[0] = change.uptimeMillis
                                lastAt[0] = change.position
                                break
                            }
                            if (change.positionChange() != Offset.Zero) act.setAcid(param, (change.position.x / size.width).coerceIn(0f, 1f))
                            change.consume()
                        }
                    } finally {
                        act.holdAcid(param, false)
                    }
                }
            },
    ) {
        Canvas(Modifier.matchParentSize()) {
            val r = CornerRadius(14.dp.toPx())
            drawRoundRect(rail, cornerRadius = r)
            if (value > 0f) {
                drawRoundRect(
                    Brush.horizontalGradient(listOf(accent.copy(alpha = 0.35f), accent.copy(alpha = 0.8f)), endX = size.width * value),
                    size = Size(size.width * value.coerceIn(0f, 1f), size.height), cornerRadius = r,
                )
            }
        }
        Row(Modifier.matchParentSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Text(percent((value * 100).roundToInt()), style = MaterialTheme.typography.labelLarge, color = Neon.TextPrimary, maxLines = 1)
        }
    }
}
