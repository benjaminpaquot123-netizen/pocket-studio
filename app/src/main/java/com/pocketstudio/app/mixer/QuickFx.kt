package com.pocketstudio.app.mixer

import com.pocketstudio.ui.fadingHorizontalScroll
import com.pocketstudio.ui.WithHelp
import com.pocketstudio.ui.HelpDot
import com.pocketstudio.app.help.Glossary
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.Channel
import com.pocketstudio.model.FxPresets
import com.pocketstudio.model.Macros
import com.pocketstudio.model.SoundKind
import com.pocketstudio.model.matchingFx
import com.pocketstudio.ui.Knob
import com.pocketstudio.ui.KnobColors
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.pillColors
import kotlin.math.roundToInt
import com.pocketstudio.model.i18n.tr

class QuickFxActions(
    val setMacro: (ch: Int, macro: Int, value: Float) -> Unit = { _, _, _ -> },
    val setMacroPair: (ch: Int, x: Int, xValue: Float, y: Int, yValue: Float) -> Unit = { _, _, _, _, _ -> },
    val applyFx: (ch: Int, presetId: String) -> Unit = { _, _ -> },
)

/**
 * Sound play for one track, no technical knobs: ready-made effects named after what they sound
 * like (those that suit [kind] first), then, unless [compact], a pad XY to sweep two of the
 * track's macros at once and the four macros as big knobs.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickFxPanel(ch: Int, channel: Channel, actions: QuickFxActions, kind: SoundKind? = null, compact: Boolean = false) {
    val chipColors = pillColors(Neon.Magenta)
    val current = channel.matchingFx()
    val suggested = kind?.let { FxPresets.suggestedFor(it) }.orEmpty()
    // With suggestions, the families wait behind « Toutes » (unless the one in use is elsewhere).
    val startAll = suggested.isEmpty() || (current != null && current.id != FxPresets.NONE.id && current !in suggested)
    var category by rememberSaveable(ch, kind) {
        mutableStateOf(if (startAll) current?.category?.takeIf { it.isNotEmpty() } ?: FxPresets.CATEGORIES.first() else SUGGESTED)
    }
    Column(Modifier.fillMaxWidth().testTag("quick_fx_$ch"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        WithHelp(Glossary.FX_PRESET) { Text(tr("Ambiances toutes prêtes"), style = MaterialTheme.typography.labelLarge, color = Neon.Magenta) }
        Row(Modifier.fadingHorizontalScroll(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (kind != null) {
                FilterChip(selected = category == SUGGESTED, onClick = { category = SUGGESTED }, label = { Text(tr("Pour : %s", tr(kind.label))) },
                    modifier = Modifier.testTag("fx_category_suggested"))
            }
            FxPresets.CATEGORIES.forEach { c ->
                FilterChip(selected = category == c, onClick = { category = c }, label = { Text(tr(c)) },
                    modifier = Modifier.testTag("fx_category_$c"))
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (if (category == SUGGESTED) suggested else FxPresets.ALL.filter { it.category == category }).forEach { p ->
                FilterChip(selected = current?.id == p.id, onClick = { actions.applyFx(ch, p.id) }, label = { Text(tr(p.name)) },
                    colors = chipColors, modifier = Modifier.testTag("fx_preset_${p.id}"))
            }
            FilterChip(selected = current?.id == FxPresets.NONE.id, onClick = { actions.applyFx(ch, FxPresets.NONE.id) },
                label = { Text(tr(FxPresets.NONE.name)) }, colors = chipColors, modifier = Modifier.testTag("fx_preset_none"))
        }
        Text(current?.description?.let { tr(it) } ?: tr("Touche une ambiance pour l'essayer : tu peux toujours revenir avec Annuler."),
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        if (!compact) {
            XyPad(ch, channel.macros, actions)
            MacroKnobs(ch, channel.macros, actions)
        }
    }
}

/** The suggestions' tab (not a preset category). */
private const val SUGGESTED = "suggested"

/** X: the colour (dark ↔ bright); Y: a macro of choice. Double tap: both back to rest. */
@Composable
private fun XyPad(ch: Int, macros: List<Float>, actions: QuickFxActions) {
    var yMacro by rememberSaveable(ch) { mutableIntStateOf(Macros.SPACE) }
    val x = macros[Macros.COLOR]
    val y = macros[yMacro]
    val currentY by rememberUpdatedState(yMacro)
    val set by rememberUpdatedState(actions.setMacroPair)
    Row(Modifier.fadingHorizontalScroll(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(tr("Pad XY, haut-bas :"), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, maxLines = 1)
        HelpDot(Glossary.XY_PAD)
        listOf(Macros.SPACE, Macros.DIRT, Macros.MOTION).forEach { m ->
            FilterChip(selected = yMacro == m, onClick = { yMacro = m }, label = { Text(tr(Macros.NAMES[m])) },
                modifier = Modifier.testTag("xy_axis_$m"))
        }
    }
    val accent = Neon.Magenta
    val grid = Neon.Outline
    Box(
        Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(12.dp)).background(Neon.Surface)
            .pointerInput(ch) {
                fun send(at: Offset) {
                    val nx = (at.x / size.width).coerceIn(0f, 1f)
                    val ny = 1f - (at.y / size.height).coerceIn(0f, 1f)
                    set(ch, Macros.COLOR, nx, currentY, ny)
                }
                detectDragGestures(onDragStart = ::send) { change, _ -> send(change.position) }
            }
            .pointerInput(ch) {
                detectTapGestures(
                    onTap = { at ->
                        set(ch, Macros.COLOR, (at.x / size.width).coerceIn(0f, 1f), currentY, 1f - (at.y / size.height).coerceIn(0f, 1f))
                    },
                    onDoubleTap = { set(ch, Macros.COLOR, Macros.DEFAULTS[Macros.COLOR], currentY, Macros.DEFAULTS[currentY]) },
                )
            }
            .testTag("xy_pad_$ch"),
    ) {
        Canvas(Modifier.matchParentSize()) {
            val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            drawLine(grid, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), strokeWidth = 2f, pathEffect = dash)
            drawLine(grid, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 2f)
            val at = Offset(x * size.width, (1f - y) * size.height)
            drawCircle(accent.copy(alpha = 0.25f), radius = 34f, center = at)
            drawCircle(accent, radius = 16f, center = at)
        }
        val (dark, bright) = Macros.ENDS[Macros.COLOR].let { (a, b) -> tr(a) to tr(b) }
        val (low, high) = Macros.ENDS[yMacro].let { (a, b) -> tr(a) to tr(b) }
        Text("← $dark", Modifier.align(Alignment.BottomStart).padding(8.dp), style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary)
        Text("$bright →", Modifier.align(Alignment.BottomEnd).padding(8.dp), style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary)
        Text("↑ $high", Modifier.align(Alignment.TopStart).padding(8.dp), style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary)
        Text("↓ $low", Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = 26.dp), style = MaterialTheme.typography.labelSmall,
            color = Neon.TextSecondary)
    }
    Text(tr("Glisse le doigt pour triturer le son en direct. Deux touchers : retour au son de départ."),
        style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
}

@Composable
private fun MacroKnobs(ch: Int, macros: List<Float>, actions: QuickFxActions) {
    val colors = KnobColors(
        cap = Neon.SurfaceHigh, capEdge = Neon.Outline, pointer = Neon.TextPrimary,
        track = Neon.Outline, value = Neon.Magenta, label = Neon.TextSecondary,
    )
    Row(Modifier.fillMaxWidth()) {
        Macros.NAMES.forEachIndexed { m, name ->
            val v = macros[m]
            Knob(
                value = v,
                onValueChange = { actions.setMacro(ch, m, it) },
                label = tr(name),
                default = Macros.DEFAULTS[m],
                size = 52.dp,
                colors = colors,
                valueText = macroText(m, v),
                modifier = Modifier.weight(1f).testTag("macro_${ch}_$m"),
            )
        }
    }
}

internal fun macroText(m: Int, v: Float): String {
    if (m == Macros.COLOR) {
        val c = ((v - 0.5f) * 200).roundToInt()
        return when {
            c < -2 -> tr("Sombre %d", -c)
            c > 2 -> tr("Brillant %d", c)
            else -> tr("Neutre")
        }
    }
    return if (v < 0.01f) "0" else com.pocketstudio.model.i18n.percent((v * 100).roundToInt())
}

/** One line for a closed strip: the matching ready-made effect, or what is on. */
fun quickFxSummary(c: Channel): String {
    val none = tr("Aucun")
    c.matchingFx()?.let { return if (it.id == FxPresets.NONE.id) none else tr(it.name) }
    val macros = c.macros.mapIndexedNotNull { m, v -> if (v != Macros.DEFAULTS[m]) tr(Macros.NAMES[m]) else null }
    return (listOf(effectsSummary(c.inserts, c.sends)).filter { it != none } + macros).joinToString(" · ").ifEmpty { none }
}
