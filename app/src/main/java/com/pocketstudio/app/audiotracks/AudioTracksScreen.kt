package com.pocketstudio.app.audiotracks

import com.pocketstudio.app.help.Glossary
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.rhythm.AudioRecordingUi
import com.pocketstudio.app.rhythm.SampleInfo
import com.pocketstudio.app.rhythm.Waveform
import com.pocketstudio.model.AudioClip
import com.pocketstudio.model.AudioTrack
import com.pocketstudio.ui.LevelMeter
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.SectionLabel
import com.pocketstudio.ui.TrackColors
import com.pocketstudio.ui.trackColor
import kotlin.math.roundToInt

class AudioTracksUi(
    val tracks: List<AudioTrack>,
    val recording: AudioRecordingUi? = null,
    val info: Map<String, SampleInfo> = emptyMap(),
    /** A clip edit or import in progress, or its error. */
    val task: String? = null,
    /** The round trip was measured (Outils › Latence): takes are placed exactly. */
    val latencyMeasured: Boolean = false,
    /** Sixteenths in a bar (time signature). */
    val stepsPerBar: Int = 16,
)

class AudioTracksActions(
    /** Opens a track's quick effects. */
    val effects: (Int) -> Unit = {},
    val add: () -> Unit = {},
    val remove: (Int) -> Unit = {},
    val rename: (Int, String) -> Unit = { _, _ -> },
    val setColor: (Int, Int) -> Unit = { _, _ -> },
    val record: (Int) -> Unit = {},
    val stop: () -> Unit = {},
    val cancel: () -> Unit = {},
    val import: (Int) -> Unit = {},
    val setStart: (Int, Int) -> Unit = { _, _ -> },
    val setLoopBars: (Int, Int) -> Unit = { _, _ -> },
    val setGain: (Int, Float) -> Unit = { _, _ -> },
    val setFadeIn: (Int, Float) -> Unit = { _, _ -> },
    val setFadeOut: (Int, Float) -> Unit = { _, _ -> },
    val normalize: (Int) -> Unit = {},
    val reverse: (Int) -> Unit = {},
    val trim: (Int, Float, Float) -> Unit = { _, _, _ -> },
    val clear: (Int) -> Unit = {},
)

private val LOOP_BARS = listOf(1, 2, 4, 8, 16)

/** Audio tracks: record your voice or an instrument over the song, or bring a sound file. */
@Composable
fun AudioTracksScreen(ui: AudioTracksUi, actions: AudioTracksActions, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp).testTag("audio_tracks"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ui.recording?.let { RecordingCard(it, ui.tracks.firstOrNull { t -> t.id == it.track }, actions) }
        ui.task?.let { Text(it, color = Neon.Amber, style = MaterialTheme.typography.bodySmall) }
        if (ui.tracks.isEmpty()) {
            NeonCard {
                SectionLabel("Pistes audio", help = Glossary.AUDIO_TRACK)
                Text(
                    "Enregistre ta voix, une guitare, des claquements de mains… par-dessus ta musique, ou ajoute un fichier son. " +
                        "Chaque prise se cale sur la mesure et tourne en boucle avec le morceau.",
                    style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary,
                )
            }
        }
        ui.tracks.forEach { t -> TrackCard(t, ui, actions) }
        if (ui.tracks.size < AudioTrack.MAX) {
            Button(onClick = actions.add, modifier = Modifier.fillMaxWidth().height(48.dp).testTag("audio_add")) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  Piste audio")
            }
        }
        if (!ui.latencyMeasured && ui.tracks.isNotEmpty()) {
            Text(
                "Astuce : mesure la latence une fois (Outils › Latence) pour que tes prises tombent pile sur le temps.",
                style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
            )
        }
    }
}

@Composable
private fun RecordingCard(rec: AudioRecordingUi, track: AudioTrack?, actions: AudioTracksActions) {
    NeonCard(accent = Neon.Red, modifier = Modifier.testTag("audio_recording")) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.FiberManualRecord, contentDescription = null, tint = Neon.Red)
            Text(
                if (rec.waiting) "Décompte… la prise commence sur le premier temps" else "Enregistrement : ${String.format(java.util.Locale.FRANCE, "%.1f", rec.seconds)} s",
                style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f),
            )
        }
        track?.let { Text(it.name, style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary) }
        LevelMeter(rec.level)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = actions.stop, colors = ButtonDefaults.buttonColors(containerColor = Neon.Red), modifier = Modifier.weight(1f).testTag("audio_stop")) {
                Text("Arrêter et garder")
            }
            OutlinedButton(onClick = actions.cancel, modifier = Modifier.weight(1f)) { Text("Annuler") }
        }
    }
}

@Composable
private fun TrackCard(t: AudioTrack, ui: AudioTracksUi, actions: AudioTracksActions) {
    val color = trackColor(t.color)
    var menu by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var colouring by remember { mutableStateOf(false) }
    NeonCard(modifier = Modifier.testTag("audio_track_${t.id}")) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(12.dp).background(color, CircleShape))
            Text(t.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Options de ${t.name}") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Renommer") }, onClick = { menu = false; renaming = true })
                    DropdownMenuItem(text = { Text("Couleur") }, onClick = { menu = false; colouring = true })
                    if (t.clip != null) DropdownMenuItem(text = { Text("Vider la piste") }, onClick = { menu = false; actions.clear(t.id) })
                    DropdownMenuItem(text = { Text("Supprimer la piste (Annuler possible)") }, onClick = { menu = false; actions.remove(t.id) })
                }
            }
        }
        val busy = ui.recording != null
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { actions.record(t.id) }, enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = Neon.Red),
                modifier = Modifier.weight(1f).testTag("audio_record_${t.id}"),
            ) {
                Icon(Icons.Filled.FiberManualRecord, contentDescription = null, Modifier.size(16.dp))
                Text(if (t.clip == null) " Enregistrer" else " Nouvelle prise")
            }
            OutlinedButton(onClick = { actions.import(t.id) }, enabled = !busy, modifier = Modifier.weight(1f)) { Text("Fichier son") }
        }
        TextButton(onClick = { actions.effects(t.id) }, modifier = Modifier.testTag("audio_effects_${t.id}")) { Text("Effets de cette piste") }
        val clip = t.clip ?: return@NeonCard
        ClipEditor(t.id, clip, ui.info[clip.file], color, actions, ui.stepsPerBar)
    }
    if (renaming) {
        var name by remember(t.id) { mutableStateOf(t.name) }
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text("Renommer la piste") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it.take(24) }, singleLine = true, label = { Text("Nom") }) },
            confirmButton = { TextButton(onClick = { actions.rename(t.id, name); renaming = false }, enabled = name.isNotBlank()) { Text("OK") } },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text("Annuler") } },
        )
    }
    if (colouring) {
        AlertDialog(
            onDismissRequest = { colouring = false },
            title = { Text("Couleur de « ${t.name} »") },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TrackColors.forEachIndexed { i, c ->
                        Box(
                            Modifier.size(30.dp).clip(CircleShape).background(c)
                                .border(BorderStroke(if (i == t.color) 3.dp else 0.dp, Neon.TextPrimary), CircleShape)
                                .clickable { actions.setColor(t.id, i); colouring = false },
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { colouring = false }) { Text("Fermer") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClipEditor(id: Int, clip: AudioClip, info: SampleInfo?, color: androidx.compose.ui.graphics.Color, actions: AudioTracksActions, per: Int) {
    val chip = FilterChipDefaults.filterChipColors(selectedContainerColor = Neon.Cyan.copy(alpha = 0.25f), selectedLabelColor = Neon.Cyan)
    var range by remember(clip.file) { mutableStateOf(0f..1f) }
    Text("${clip.name} · ${String.format(java.util.Locale.FRANCE, "%.1f", clip.seconds)} s", style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
    info?.let { Waveform(it.peaks, color, region = range.start to range.endInclusive) }
    // Cut: keep the part between the two handles.
    RangeSlider(value = range, onValueChange = { range = it }, modifier = Modifier.testTag("audio_trim_$id"))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
        OutlinedButton(onClick = { actions.trim(id, range.start, range.endInclusive) }, enabled = range.start > 0.001f || range.endInclusive < 0.999f) {
            Text("Garder la sélection")
        }
        OutlinedButton(onClick = { actions.normalize(id) }, modifier = Modifier.testTag("audio_normalize_$id")) { Text("Normaliser") }
        OutlinedButton(onClick = { actions.reverse(id) }) { Text("Inverser") }
    }
    Text("Commence à la mesure", style = MaterialTheme.typography.bodyMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
        (1..8).forEach { bar ->
            FilterChip(selected = clip.start == (bar - 1) * per, onClick = { actions.setStart(id, bar) }, label = { Text("$bar") }, colors = chip)
        }
    }
    Text("Revient toutes les", style = MaterialTheme.typography.bodyMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
        LOOP_BARS.forEach { bars ->
            FilterChip(selected = clip.loopSteps == bars * per, onClick = { actions.setLoopBars(id, bars) },
                label = { Text(if (bars == 1) "1 mesure" else "$bars mesures") }, colors = chip)
        }
    }
    LabeledSlider("Volume", "${(clip.gain * 100).roundToInt()} %", clip.gain / 2f) { actions.setGain(id, it * 2f) }
    LabeledSlider("Fondu au début", "${String.format(java.util.Locale.FRANCE, "%.2f", clip.fadeIn)} s", clip.fadeIn / 2f) { actions.setFadeIn(id, it * 2f) }
    LabeledSlider("Fondu à la fin", "${String.format(java.util.Locale.FRANCE, "%.2f", clip.fadeOut)} s", clip.fadeOut / 2f) { actions.setFadeOut(id, it * 2f) }
}

@Composable
private fun LabeledSlider(label: String, value: String, position: Float, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MonoStyle, color = Neon.Cyan)
    }
    Slider(value = position.coerceIn(0f, 1f), onValueChange = onChange)
}
