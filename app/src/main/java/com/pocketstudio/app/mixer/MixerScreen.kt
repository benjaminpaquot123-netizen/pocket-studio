package com.pocketstudio.app.mixer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.rhythm.ExportUi
import com.pocketstudio.app.rhythm.ExportWhat
import com.pocketstudio.app.rhythm.ExportFormat
import com.pocketstudio.model.Buses
import com.pocketstudio.model.Channel
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.Insert
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.pocketstudio.ui.LevelMeter
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.SectionLabel
import kotlin.math.log10
import kotlin.math.roundToInt

val CHANNEL_NAMES = listOf("Batterie", "Basse acide", "Synthé", "Remix")

data class MixerActions(
    val setVolume: (Int, Float) -> Unit = { _, _ -> },
    val setPan: (Int, Float) -> Unit = { _, _ -> },
    val toggleMute: (Int) -> Unit = {},
    val toggleSolo: (Int) -> Unit = {},
    val setExportBars: (Int) -> Unit = {},
    val export: () -> Unit = {},
    val setExportWhat: (ExportWhat) -> Unit = {},
    val setExportFormat: (ExportFormat) -> Unit = {},
    val cancelExport: () -> Unit = {},
    val share: () -> Unit = {},
    val setInsertType: (ch: Int, slot: Int, type: EffectType) -> Unit = { _, _, _ -> },
    val setInsertParam: (ch: Int, slot: Int, param: Int, value: Float) -> Unit = { _, _, _, _ -> },
    val setSend: (ch: Int, bus: Int, level: Float) -> Unit = { _, _, _ -> },
    val setBusParam: (bus: Int, param: Int, value: Float) -> Unit = { _, _, _ -> },
)

val EXPORT_BARS = listOf(1, 2, 4, 8, 16)

/** Fader position (0.8 = unity, quadratic law, as in the engine) shown in dB. */
fun faderDb(v: Float): String {
    if (v <= 0.001f) return "−∞ dB"
    val gain = (v / 0.8f) * (v / 0.8f)
    val db = 20 * log10(gain)
    return (if (db >= 0) "+" else "") + String.format(java.util.Locale.FRANCE, "%.1f dB", db)
}

@Composable
fun MixerScreen(
    channels: List<Channel>,
    meters: FloatArray,
    actions: MixerActions,
    modifier: Modifier = Modifier,
    export: ExportUi = ExportUi(),
    master: List<Insert> = List(4) { Insert() },
    buses: List<List<Float>> = Buses.DEFAULTS,
    automation: AutomationUi = AutomationUi(),
    automationActions: AutomationActions = AutomationActions(),
    hasSong: Boolean = false,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp).testTag("mixer"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CHANNEL_NAMES.forEachIndexed { i, name ->
            Strip(name, channels[i], meters.getOrElse(i) { 0f }, i, actions)
        }
        NeonCard(accent = Neon.Cyan) {
            SectionLabel("Sortie générale")
            LevelMeter(meters.getOrElse(CHANNEL_NAMES.size) { 0f } / 0.89f)
            val gain = meters.getOrElse(CHANNEL_NAMES.size + 1) { 1f }
            Text(
                if (gain < 0.98f) "Le limiteur protège la sortie (−${String.format(java.util.Locale.FRANCE, "%.1f", -20 * log10(gain))} dB)"
                else "Pas de saturation : tout va bien.",
                style = MaterialTheme.typography.bodySmall,
                color = if (gain < 0.7f) Neon.Amber else Neon.TextSecondary,
            )
            var open by rememberSaveable { mutableStateOf(false) }
            EffectsToggle(effectsSummary(master), open) { open = !open }
            if (open) InsertRack(CHANNEL_NAMES.size, master, actions)
        }
        BusesCard(buses, actions)
        AutomationCard(automation, channels, master, automationActions)
        ExportCard(export, actions, hasSong)
    }
}

@Composable
private fun Strip(name: String, c: Channel, meter: Float, index: Int, actions: MixerActions) {
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = Neon.Cyan.copy(alpha = 0.25f),
        selectedLabelColor = Neon.Cyan,
    )
    NeonCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            FilterChip(
                selected = c.mute,
                onClick = { actions.toggleMute(index) },
                label = { Text("Muet") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Neon.Red.copy(alpha = 0.25f), selectedLabelColor = Neon.Red),
                modifier = Modifier.testTag("mute_$index"),
            )
            FilterChip(selected = c.solo, onClick = { actions.toggleSolo(index) }, label = { Text("Solo") }, colors = chipColors,
                modifier = Modifier.testTag("solo_$index"))
        }
        LevelMeter(meter / 0.89f)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Volume", Modifier.weight(1f))
            Text(faderDb(c.volume), style = MonoStyle, color = Neon.Cyan)
        }
        Slider(value = c.volume, onValueChange = { actions.setVolume(index, it) }, modifier = Modifier.testTag("volume_$index"))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Panoramique", Modifier.weight(1f))
            val pan = (c.pan * 100).roundToInt()
            Text(
                when {
                    pan < -2 -> "Gauche ${-pan}"
                    pan > 2 -> "Droite $pan"
                    else -> "Centre"
                },
                style = MonoStyle, color = Neon.Cyan,
            )
        }
        Slider(value = c.pan, onValueChange = { v -> actions.setPan(index, if (kotlin.math.abs(v) < 0.04f) 0f else v) }, valueRange = -1f..1f)
        var open by rememberSaveable(index) { mutableStateOf(false) }
        EffectsToggle(effectsSummary(c.inserts, c.sends), open, Modifier.testTag("effects_$index")) { open = !open }
        if (open) {
            InsertRack(index, c.inserts, actions)
            SendSliders(index, c.sends, actions)
        }
    }
}

@Composable
private fun EffectsToggle(summary: String, open: Boolean, modifier: Modifier = Modifier, toggle: () -> Unit) {
    TextButton(onClick = toggle, modifier = modifier.fillMaxWidth()) {
        Text("Effets : $summary", Modifier.weight(1f), color = Neon.Cyan)
        Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null, tint = Neon.Cyan)
    }
}

@Composable
internal fun ExportCard(export: ExportUi, actions: MixerActions, hasSong: Boolean = true) {
    val chipColors = FilterChipDefaults.filterChipColors(selectedContainerColor = Neon.Acid.copy(alpha = 0.25f), selectedLabelColor = Neon.Acid)
    NeonCard(accent = Neon.Acid) {
        SectionLabel("Exporter", color = Neon.Acid)
        Text("Quoi", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(ExportWhat.Loop to "La boucle", ExportWhat.Song to "Le morceau", ExportWhat.Stems to "Piste par piste").forEach { (what, label) ->
                FilterChip(
                    selected = export.what == what, onClick = { actions.setExportWhat(what) }, enabled = !export.running,
                    label = { Text(label) }, colors = chipColors, modifier = Modifier.testTag("export_what_${what.name}"),
                )
            }
        }
        Text(
            when (export.what) {
                ExportWhat.Loop -> "La scène en cours, sur la longueur choisie."
                ExportWhat.Song -> if (hasSong) "Toutes les parties de l'onglet Morceau, dans l'ordre." else "Le morceau est vide : ajoute des scènes dans l'onglet Morceau."
                ExportWhat.Stems -> "Un fichier par piste (batterie, acide, synthé, remix) pour les reprendre ailleurs." +
                    if (hasSong) " Sur tout le morceau." else ""
            },
            style = MaterialTheme.typography.bodySmall, color = if (export.what == ExportWhat.Song && !hasSong) Neon.Amber else Neon.TextSecondary,
        )
        if (export.what == ExportWhat.Loop || (export.what == ExportWhat.Stems && !hasSong)) {
            Text("Longueur", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EXPORT_BARS.forEach { bars ->
                    FilterChip(
                        selected = export.bars == bars,
                        onClick = { actions.setExportBars(bars) },
                        enabled = !export.running,
                        label = { Text(if (bars == 1) "1 mesure" else "$bars") },
                        colors = chipColors,
                    )
                }
            }
        }
        Text("Format", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(ExportFormat.Wav to "WAV (qualité studio)", ExportFormat.M4a to "M4A (léger)").forEach { (format, label) ->
                FilterChip(
                    selected = export.format == format, onClick = { actions.setExportFormat(format) }, enabled = !export.running,
                    label = { Text(label) }, colors = chipColors, modifier = Modifier.testTag("export_format_${format.name}"),
                )
            }
        }
        when {
            export.running -> {
                export.stage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary) }
                LinearProgressIndicator(
                    progress = { export.progress },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    color = Neon.Acid,
                    trackColor = Neon.SurfaceHigh,
                )
                OutlinedButton(onClick = actions.cancelExport, modifier = Modifier.fillMaxWidth()) { Text("Annuler") }
            }
            else -> {
                Button(
                    onClick = actions.export,
                    enabled = export.what != ExportWhat.Song || hasSong,
                    modifier = Modifier.fillMaxWidth().testTag("export"),
                    colors = ButtonDefaults.buttonColors(containerColor = Neon.Acid, contentColor = Color.Black),
                ) {
                    Text(
                        when (export.what) {
                            ExportWhat.Loop -> "Exporter ${export.bars} mesure${if (export.bars > 1) "s" else ""}"
                            ExportWhat.Song -> "Exporter le morceau"
                            ExportWhat.Stems -> "Exporter les pistes"
                        },
                    )
                }
            }
        }
        if (export.files.isNotEmpty()) {
            Text(
                (if (export.savedInMusic) "Enregistré dans Musique › Pocket Studio : " else "Prêt : ") + export.files.joinToString(", ") { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = Neon.TextSecondary,
            )
            OutlinedButton(onClick = actions.share, modifier = Modifier.fillMaxWidth().testTag("share")) {
                Text(if (export.files.size > 1) "Partager les ${export.files.size} fichiers" else "Partager")
            }
        }
        export.error?.let { Text(it, color = Neon.Red, style = MaterialTheme.typography.bodySmall) }
    }
}
