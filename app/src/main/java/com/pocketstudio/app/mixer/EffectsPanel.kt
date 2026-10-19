package com.pocketstudio.app.mixer

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.Buses
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.Insert
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.SectionLabel

/** Short names for the slot chips. */
private val SHORT = mapOf(
    EffectType.NONE to "—", EffectType.EQ to "EQ", EffectType.COMPRESSOR to "Comp",
    EffectType.SATURATION to "Satu", EffectType.CHORUS to "Chorus", EffectType.FILTER to "Filtre",
)

/** One-line summary of a channel's effects, e.g. "EQ · Comp · Réverbe". */
fun effectsSummary(inserts: List<Insert>, sends: List<Float> = emptyList()): String {
    val parts = inserts.filter { it.effect != EffectType.NONE }.map { SHORT.getValue(it.effect) } +
        sends.mapIndexedNotNull { b, v -> if (v > 0.01f) Buses.NAMES[b] else null }
    return parts.joinToString(" · ").ifEmpty { "Aucun" }
}

/**
 * The 4 insert slots of a channel (or the master): pick a slot, choose its effect, then shape
 * it with up to 4 sliders. Effects run in slot order.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InsertRack(ch: Int, inserts: List<Insert>, actions: MixerActions) {
    var slot by rememberSaveable(ch) { mutableIntStateOf(0) }
    val chipColors = FilterChipDefaults.filterChipColors(selectedContainerColor = Neon.Cyan.copy(alpha = 0.25f), selectedLabelColor = Neon.Cyan)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        inserts.forEachIndexed { i, ins ->
            FilterChip(
                selected = slot == i, onClick = { slot = i },
                label = { Text("${i + 1} ${SHORT.getValue(ins.effect)}") },
                colors = chipColors,
                modifier = Modifier.testTag("slot_${ch}_$i"),
            )
        }
    }
    val current = inserts.getOrNull(slot) ?: return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        EffectType.entries.forEach { type ->
            FilterChip(
                selected = current.effect == type,
                onClick = { if (current.effect != type) actions.setInsertType(ch, slot, type) },
                label = { Text(type.label) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Neon.Magenta.copy(alpha = 0.25f), selectedLabelColor = Neon.Magenta),
                modifier = Modifier.testTag("fx_${ch}_${slot}_${type.name}"),
            )
        }
    }
    current.effect.params.forEachIndexed { k, param ->
        LabeledSlider(param.label, param.format(current.params[k]), current.params[k], "fxp_${ch}_${slot}_$k") {
            actions.setInsertParam(ch, slot, k, it)
        }
    }
}

@Composable
fun SendSliders(ch: Int, sends: List<Float>, actions: MixerActions) {
    sends.forEachIndexed { bus, level ->
        LabeledSlider("Envoi ${Buses.NAMES[bus].lowercase()}", "${(level * 100).toInt()} %", level, "send_${ch}_$bus") {
            actions.setSend(ch, bus, it)
        }
    }
}

/** The reverb and the delay shared by every channel (their sends set how much each gets). */
@Composable
fun BusesCard(buses: List<List<Float>>, actions: MixerActions) {
    NeonCard(accent = Neon.Magenta) {
        SectionLabel("Réverbe et écho", color = Neon.Magenta)
        Text(
            "Communs à toutes les voies : règle combien chaque voie y envoie dans ses effets. L'écho suit le tempo.",
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
        )
        buses.forEachIndexed { bus, params ->
            Text(Buses.NAMES[bus], style = MaterialTheme.typography.titleSmall, color = Neon.Magenta)
            Buses.PARAMS[bus].forEachIndexed { k, p ->
                LabeledSlider(p.label, p.format(params[k]), params[k], "bus_${bus}_$k") { actions.setBusParam(bus, k, it) }
            }
        }
    }
}

@Composable
private fun LabeledSlider(label: String, value: String, v: Float, tag: String, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MonoStyle, color = Neon.Cyan)
    }
    Slider(value = v, onValueChange = onChange, modifier = Modifier.testTag(tag))
}
