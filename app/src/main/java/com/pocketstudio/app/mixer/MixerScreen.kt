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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
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

/** A strip on screen: its mixer channel, its name and, for an instrument track, its colour. */
data class StripInfo(val ch: Int, val name: String, val color: androidx.compose.ui.graphics.Color? = null)

/** The mixer channel index of the master (inserts, automation, meters). */
const val MASTER_CHANNEL = 4

/** Drums, acid, the first instrument track, the remix: a project with no added track. */
val BASIC_STRIPS = listOf(StripInfo(0, "Batterie"), StripInfo(1, "Basse acide"), StripInfo(2, "Synthé"), StripInfo(3, "Remix"))

/** Index of the limiter gain in the meters (after one peak per mixer channel). */
const val LIMITER_METER = 16

data class MixerActions(
    val setVolume: (Int, Float) -> Unit = { _, _ -> },
    val setPan: (Int, Float) -> Unit = { _, _ -> },
    val toggleMute: (Int) -> Unit = {},
    val toggleSolo: (Int) -> Unit = {},
    val setExportBars: (Int) -> Unit = {},
    val export: () -> Unit = {},
    val setExportWhat: (ExportWhat) -> Unit = {},
    val setExportFormat: (ExportFormat) -> Unit = {},
    val exportMidi: () -> Unit = {},
    val cancelExport: () -> Unit = {},
    val share: () -> Unit = {},
    val setInsertType: (ch: Int, slot: Int, type: EffectType) -> Unit = { _, _, _ -> },
    val setInsertParam: (ch: Int, slot: Int, param: Int, value: Float) -> Unit = { _, _, _, _ -> },
    val setSend: (ch: Int, bus: Int, level: Float) -> Unit = { _, _, _ -> },
    val setBusParam: (bus: Int, param: Int, value: Float) -> Unit = { _, _, _ -> },
    val quick: QuickFxActions = QuickFxActions(),
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
    strips: List<StripInfo> = BASIC_STRIPS,
    /** The output spectrum while the analyser is open (null: closed or not yet measured). */
    spectrum: FloatArray? = null,
    analyserOpen: Boolean = false,
    setAnalyser: (Boolean) -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp).testTag("mixer"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Many tracks: each strip shows its volume on one line; ▾ opens pan and effects.
        val compact = strips.size > BASIC_STRIPS.size
        strips.forEach { st ->
            Strip(st, channels.getOrElse(st.ch) { Channel() }, meters.getOrElse(st.ch) { 0f }, actions, compact)
        }
        NeonCard(accent = Neon.Cyan) {
            SectionLabel("Sortie générale")
            LevelMeter(meters.getOrElse(MASTER_CHANNEL) { 0f } / 0.89f)
            val gain = meters.getOrElse(LIMITER_METER) { 1f }
            Text(
                if (gain < 0.98f) "Le limiteur protège la sortie (−${String.format(java.util.Locale.FRANCE, "%.1f", -20 * log10(gain))} dB)"
                else "Pas de saturation : tout va bien.",
                style = MaterialTheme.typography.bodySmall,
                color = if (gain < 0.7f) Neon.Amber else Neon.TextSecondary,
            )
            var open by rememberSaveable { mutableStateOf(false) }
            EffectsToggle(effectsSummary(master), open) { open = !open }
            if (open) InsertRack(MASTER_CHANNEL, master, actions)
            FilterChip(selected = analyserOpen, onClick = { setAnalyser(!analyserOpen) }, label = { Text("Analyseur de spectre") },
                modifier = Modifier.testTag("analyser"))
            if (analyserOpen) com.pocketstudio.app.analysis.SpectrumView(spectrum)
        }
        BusesCard(buses, actions)
        AutomationCard(automation, channels, master, automationActions, strips)
        ExportCard(export, actions, hasSong)
    }
}

@Composable
private fun Strip(info: StripInfo, c: Channel, meter: Float, actions: MixerActions, compact: Boolean = false) {
    val name = info.name
    val index = info.ch
    var expanded by rememberSaveable(index) { mutableStateOf(false) }
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = Neon.Cyan.copy(alpha = 0.25f),
        selectedLabelColor = Neon.Cyan,
    )
    NeonCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            info.color?.let { androidx.compose.foundation.layout.Box(Modifier.size(12.dp).background(it, androidx.compose.foundation.shape.CircleShape)) }
            Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
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
        if (compact) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Slider(value = c.volume, onValueChange = { actions.setVolume(index, it) }, modifier = Modifier.weight(1f).testTag("volume_$index"))
                Text(faderDb(c.volume), style = MonoStyle, color = Neon.Cyan, modifier = Modifier.padding(start = 6.dp))
                androidx.compose.material3.IconButton(onClick = { expanded = !expanded }, modifier = Modifier.testTag("strip_more_$index")) {
                    Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Moins de réglages" else "Panoramique et effets", tint = Neon.Cyan)
                }
            }
            if (!expanded) {
                val fx = androidx.compose.runtime.remember(c) { quickFxSummary(c) }
                if (c.pan != 0f || fx != "Aucun") {
                    Text("Effets : $fx", style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                }
                return@NeonCard
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Volume", Modifier.weight(1f))
                Text(faderDb(c.volume), style = MonoStyle, color = Neon.Cyan)
            }
            Slider(value = c.volume, onValueChange = { actions.setVolume(index, it) }, modifier = Modifier.testTag("volume_$index"))
        }
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
        val summary = androidx.compose.runtime.remember(c) { quickFxSummary(c) }
        EffectsToggle(summary, open, Modifier.testTag("effects_$index")) { open = !open }
        if (open) {
            QuickFxPanel(index, c, actions.quick)
            DetailedFx(index, c, actions)
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
        if (!export.running) {
            TextButton(onClick = actions.exportMidi, modifier = Modifier.fillMaxWidth().testTag("export_midi")) {
                Text("Exporter les notes en fichier MIDI" + if (hasSong) " (tout le morceau)" else "")
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
