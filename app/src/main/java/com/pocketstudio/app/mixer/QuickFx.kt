package com.pocketstudio.app.mixer

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
import androidx.compose.material3.FilterChipDefaults
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
import com.pocketstudio.model.matchingFx
import com.pocketstudio.ui.Knob
import com.pocketstudio.ui.KnobColors
import com.pocketstudio.ui.Neon
import kotlin.math.roundToInt

class QuickFxActions(
    val setMacro: (ch: Int, macro: Int, value: Float) -> Unit = { _, _, _ -> },
    val setMacroPair: (ch: Int, x: Int, xValue: Float, y: Int, yValue: Float) -> Unit = { _, _, _, _, _ -> },
    val applyFx: (ch: Int, presetId: String) -> Unit = { _, _ -> },
)

/**
 * Sound play for one track, no technical knobs: ready-made effects named after what they sound
 * like, a pad XY to sweep two of the track's macros at once, and the four macros as big knobs.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickFxPanel(ch: Int, channel: Channel, actions: QuickFxActions) {
    val chipColors = FilterChipDefaults.filterChipColors(selectedContainerColor = Neon.Magenta.copy(alpha = 0.25f), selectedLabelColor = Neon.Magenta)
    val current = channel.matchingFx()
    var category by rememberSaveable(ch) { mutableStateOf(current?.category?.takeIf { it.isNotEmpty() } ?: FxPresets.CATEGORIES.first()) }
    Column(Modifier.fillMaxWidth().testTag("quick_fx_$ch"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Ambiances toutes prêtes", style = MaterialTheme.typography.labelLarge, color = Neon.Magenta)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FxPresets.CATEGORIES.forEach { c ->
                FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c) },
                    modifier = Modifier.testTag("fx_category_$c"))
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FxPresets.ALL.filter { it.category == category }.forEach { p ->
                FilterChip(selected = current?.id == p.id, onClick = { actions.applyFx(ch, p.id) }, label = { Text(p.name) },
                    colors = chipColors, modifier = Modifier.testTag("fx_preset_${p.id}"))
            }
            FilterChip(selected = current?.id == FxPresets.NONE.id, onClick = { actions.applyFx(ch, FxPresets.NONE.id) },
                label = { Text(FxPresets.NONE.name) }, colors = chipColors, modifier = Modifier.testTag("fx_preset_none"))
        }
        Text(current?.description ?: "Touche une ambiance pour l'essayer : tu peux toujours revenir avec Annuler.",
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        XyPad(ch, channel.macros, actions)
        MacroKnobs(ch, channel.macros, actions)
    }
}

/** X: the colour (dark ↔ bright); Y: a macro of choice. Double tap: both back to rest. */
@Composable
private fun XyPad(ch: Int, macros: List<Float>, actions: QuickFxActions) {
    var yMacro by rememberSaveable(ch) { mutableIntStateOf(Macros.SPACE) }
    val x = macros[Macros.COLOR]
    val y = macros[yMacro]
    val currentY by rememberUpdatedState(yMacro)
    val set by rememberUpdatedState(actions.setMacroPair)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Pad XY · haut-bas :", style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        listOf(Macros.SPACE, Macros.DIRT, Macros.MOTION).forEach { m ->
            FilterChip(selected = yMacro == m, onClick = { yMacro = m }, label = { Text(Macros.NAMES[m]) },
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
        val (dark, bright) = Macros.ENDS[Macros.COLOR]
        val (low, high) = Macros.ENDS[yMacro]
        Text("← $dark", Modifier.align(Alignment.BottomStart).padding(8.dp), style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary)
        Text("$bright →", Modifier.align(Alignment.BottomEnd).padding(8.dp), style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary)
        Text("↑ $high", Modifier.align(Alignment.TopStart).padding(8.dp), style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary)
        Text("↓ $low", Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = 26.dp), style = MaterialTheme.typography.labelSmall,
            color = Neon.TextSecondary)
    }
    Text("Glisse le doigt pour triturer le son en direct. Deux touchers : retour au son de départ.",
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
                label = name,
                default = Macros.DEFAULTS[m],
                size = 52.dp,
                colors = colors,
                valueText = macroText(m, v),
                modifier = Modifier.weight(1f).testTag("macro_${ch}_$m"),
            )
        }
    }
}

private fun macroText(m: Int, v: Float): String {
    if (m == Macros.COLOR) {
        val c = ((v - 0.5f) * 200).roundToInt()
        return when {
            c < -2 -> "Sombre ${-c}"
            c > 2 -> "Brillant $c"
            else -> "Neutre"
        }
    }
    return if (v < 0.01f) "0" else "${(v * 100).roundToInt()} %"
}

/** One line for a closed strip: the matching ready-made effect, or what is on. */
fun quickFxSummary(c: Channel): String {
    c.matchingFx()?.let { return if (it.id == FxPresets.NONE.id) "Aucun" else it.name }
    val macros = c.macros.mapIndexedNotNull { m, v -> if (v != Macros.DEFAULTS[m]) Macros.NAMES[m] else null }
    return (listOf(effectsSummary(c.inserts, c.sends)).filter { it != "Aucun" } + macros).joinToString(" · ").ifEmpty { "Aucun" }
}

/** The detailed effect slots and sends, behind a button (for those who want every setting). */
@Composable
fun DetailedFx(ch: Int, channel: Channel, actions: MixerActions) {
    var open by rememberSaveable(ch) { mutableStateOf(false) }
    TextButton(onClick = { open = !open }, modifier = Modifier.testTag("fx_detail_$ch")) {
        Text(if (open) "Masquer les réglages détaillés" else "Réglages détaillés (effets un par un, envois)", color = Neon.TextSecondary)
    }
    if (open) {
        InsertRack(ch, channel.inserts, actions)
        SendSliders(ch, channel.sends, actions)
    }
}
