package com.pocketstudio.app.audiotracks

import com.pocketstudio.ui.fadingHorizontalScroll
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import com.pocketstudio.ui.ProSlider
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
import com.pocketstudio.app.audio.Listening
import com.pocketstudio.app.rhythm.AudioRecordingUi
import com.pocketstudio.app.rhythm.RecordTarget
import com.pocketstudio.app.rhythm.SampleInfo
import com.pocketstudio.app.rhythm.Waveform
import com.pocketstudio.model.AudioClip
import com.pocketstudio.model.AudioTrack
import com.pocketstudio.ui.LevelMeter
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.pillColors
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.SectionLabel
import com.pocketstudio.ui.TrackColors
import com.pocketstudio.ui.trackColor
import kotlin.math.roundToInt
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.tr

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
    /** What the music is heard on (advice: headphones, Bluetooth delay). */
    val listening: Listening = Listening.Wired,
    /** « Enregistrer sur »: the scene shown or the whole song. */
    val target: RecordTarget = RecordTarget(),
    /** The scene shown ("B · Couplet"). */
    val sceneTitle: String = "A",
    /** Each song part as shown ("A · Intro"); empty without a song. */
    val songParts: List<String> = emptyList(),
    /** Bars of the whole song (song clips start on one of them). */
    val songBars: Int = 0,
    val tempoBpm: Double = 120.0,
    /** The stopped take is still being written (Annuler waits). */
    val saving: Boolean = false,
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
    val setTarget: (RecordTarget) -> Unit = {},
    /** Plays once per part (true) or loops (false). */
    val setOnce: (Int, Boolean) -> Unit = { _, _ -> },
    /** Its start is a bar of the song (true) or of the scene (false). */
    val setSong: (Int, Boolean) -> Unit = { _, _ -> },
    /** « Décaler la prise », in ms. */
    val setOffset: (Int, Float) -> Unit = { _, _ -> },
)

private val LOOP_BARS = listOf(1, 2, 4, 8, 16)

/** Audio tracks: record your voice or an instrument over the song, or bring a sound file. */
@Composable
fun AudioTracksScreen(ui: AudioTracksUi, actions: AudioTracksActions, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp).testTag("audio_tracks"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ui.recording?.let { RecordingCard(it, ui.tracks.firstOrNull { t -> t.id == it.track }, ui, actions) }
        ui.task?.let { Text(it, color = Neon.Amber, style = MaterialTheme.typography.bodySmall) }
        if (ui.tracks.isEmpty()) {
            NeonCard {
                SectionLabel(tr("Pistes audio"), help = Glossary.AUDIO_TRACK)
                Text(
                    tr("Enregistre ta voix, une guitare, des claquements de mains… par-dessus ta musique, ou ajoute un fichier son.") + " " +
                        tr("Une prise sur une scène joue une fois à chaque partie de cette scène ; une prise sur tout le morceau joue d'un bout à l'autre."),
                    style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary,
                )
                ListeningAdvice(ui.listening)
            }
        } else if (ui.recording == null) {
            TargetCard(ui, actions)
        }
        // The tracks, then the button for one more: in two columns on a tablet.
        val cards = ui.tracks.map { t -> @Composable { TrackCard(t, ui, actions) } } +
            listOfNotNull(
                if (ui.tracks.size < AudioTrack.MAX) {
                    @Composable {
                        Button(onClick = actions.add, modifier = Modifier.fillMaxWidth().height(48.dp).testTag("audio_add")) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Text("  " + tr("Piste audio"))
                        }
                    }
                } else null,
            )
        com.pocketstudio.ui.AdaptiveColumns(cards, minCellWidth = 360.dp, spacing = 10.dp, columnSpacing = 10.dp, masonry = true)
        if (!ui.latencyMeasured && ui.tracks.isNotEmpty()) {
            Text(
                tr("Astuce : mesure la latence une fois (Outils › Latence) pour que tes prises tombent pile sur le temps."),
                style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
            )
        }
    }
}

/** « Mets des écouteurs » on the speaker; a short delay warning over Bluetooth. */
@Composable
private fun ListeningAdvice(listening: Listening) {
    when (listening) {
        Listening.Speaker -> Text(
            tr("Mets des écouteurs (filaires de préférence) : sinon la musique s'enregistre avec ta voix."),
            style = MaterialTheme.typography.bodySmall, color = Neon.Amber, modifier = Modifier.testTag("audio_headphones"),
        )
        Listening.Bluetooth -> Text(
            tr("Écouteurs Bluetooth : ils ajoutent du retard. Des écouteurs filaires sont plus précis ; sinon, recale la prise avec « Décaler la prise »."),
            style = MaterialTheme.typography.bodySmall, color = Neon.Amber, modifier = Modifier.testTag("audio_bluetooth"),
        )
        Listening.Wired -> Unit
    }
}

/** « Enregistrer sur »: the scene shown, or the whole song (from a part). */
@Composable
private fun TargetCard(ui: AudioTracksUi, actions: AudioTracksActions) {
    val chip = pillColors()
    val song = ui.target.song && ui.songParts.isNotEmpty()
    NeonCard(modifier = Modifier.testTag("audio_target")) {
        Text(tr("Enregistrer sur"), style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fadingHorizontalScroll()) {
            FilterChip(
                selected = !song, onClick = { actions.setTarget(ui.target.copy(song = false)) },
                label = { Text(tr("La scène %s", ui.sceneTitle)) }, colors = chip, modifier = Modifier.testTag("audio_target_scene"),
            )
            FilterChip(
                selected = song, enabled = ui.songParts.isNotEmpty(), onClick = { actions.setTarget(ui.target.copy(song = true)) },
                label = { Text(tr("Tout le morceau")) }, colors = chip, modifier = Modifier.testTag("audio_target_song"),
            )
        }
        if (ui.songParts.isEmpty()) {
            Text(tr("Pour enregistrer sur tout le morceau, crée d'abord un morceau (onglet Morceau)."), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        } else if (song) {
            Text(tr("À partir de la partie"), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fadingHorizontalScroll()) {
                ui.songParts.forEachIndexed { i, title ->
                    FilterChip(selected = ui.target.fromPart == i, onClick = { actions.setTarget(ui.target.copy(fromPart = i)) }, label = { Text("${i + 1}. $title") }, colors = chip)
                }
            }
            Text(tr("Le morceau joue avec le décompte ; ta prise se pose à sa place dans le morceau et joue une seule fois."), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        } else {
            Text(tr("La scène %s tourne en boucle ; ta prise joue une fois à chaque partie de cette scène.", ui.sceneTitle), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        }
        ListeningAdvice(ui.listening)
    }
}

@Composable
private fun RecordingCard(rec: AudioRecordingUi, track: AudioTrack?, ui: AudioTracksUi, actions: AudioTracksActions) {
    var confirm by remember { mutableStateOf(false) }
    NeonCard(accent = Neon.Red, modifier = Modifier.testTag("audio_recording")) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.FiberManualRecord, contentDescription = null, tint = Neon.Red)
            Text(
                if (rec.waiting) tr("Décompte… la prise commence sur le premier temps") else tr("Enregistrement : %.1f s", rec.seconds),
                style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f),
            )
        }
        val where = if (rec.song) tr("tout le morceau") else tr("la scène %s", ui.sceneTitle)
        Text(listOfNotNull(track?.name, tr("sur %s", where)).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        LevelMeter(rec.level)
        ListeningAdvice(ui.listening)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = actions.stop, colors = ButtonDefaults.buttonColors(containerColor = Neon.Red), modifier = Modifier.weight(1f).testTag("audio_stop")) {
                Text(tr("Arrêter et garder"))
            }
            // A long take is not thrown away by a slip of the finger.
            OutlinedButton(
                onClick = { if (rec.seconds > CONFIRM_CANCEL_SECONDS) confirm = true else actions.cancel() },
                modifier = Modifier.weight(1f).testTag("audio_cancel"),
            ) { Text(tr("Annuler")) }
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(tr("Jeter cette prise ?")) },
            text = { Text(tr("Elle dure %d s et ne sera pas gardée.", rec.seconds.roundToInt())) },
            confirmButton = { TextButton(onClick = { confirm = false; actions.cancel() }, modifier = Modifier.testTag("audio_cancel_confirm")) { Text(tr("Jeter la prise")) } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(tr("Continuer")) } },
        )
    }
}

/** « Annuler » asks first beyond this. */
private const val CONFIRM_CANCEL_SECONDS = 10f

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
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = tr("Options de %s", t.name)) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(tr("Renommer")) }, onClick = { menu = false; renaming = true })
                    DropdownMenuItem(text = { Text(tr("Couleur")) }, onClick = { menu = false; colouring = true })
                    if (t.clip != null) DropdownMenuItem(text = { Text(tr("Vider la piste")) }, onClick = { menu = false; actions.clear(t.id) })
                    DropdownMenuItem(text = { Text(tr("Supprimer la piste (Annuler possible)")) }, onClick = { menu = false; actions.remove(t.id) })
                }
            }
        }
        val busy = ui.recording != null || ui.saving
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { actions.record(t.id) }, enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = Neon.Red),
                modifier = Modifier.weight(1f).testTag("audio_record_${t.id}"),
            ) {
                Icon(Icons.Filled.FiberManualRecord, contentDescription = null, Modifier.size(16.dp))
                Text(" " + if (t.clip == null) tr("Enregistrer") else tr("Nouvelle prise"))
            }
            OutlinedButton(onClick = { actions.import(t.id) }, enabled = !busy, modifier = Modifier.weight(1f)) { Text(tr("Fichier son")) }
        }
        TextButton(onClick = { actions.effects(t.id) }, modifier = Modifier.testTag("audio_effects_${t.id}")) { Text(tr("Effets de cette piste")) }
        val clip = t.clip ?: return@NeonCard
        ClipEditor(t.id, clip, ui.info[clip.file], color, actions, ui)
    }
    if (renaming) {
        var name by remember(t.id) { mutableStateOf(t.name) }
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text(tr("Renommer la piste")) },
            text = { OutlinedTextField(value = name, onValueChange = { name = it.take(24) }, singleLine = true, label = { Text(tr("Nom")) }) },
            confirmButton = { TextButton(onClick = { actions.rename(t.id, name); renaming = false }, enabled = name.isNotBlank()) { Text("OK") } },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text(tr("Annuler")) } },
        )
    }
    if (colouring) {
        AlertDialog(
            onDismissRequest = { colouring = false },
            title = { Text(tr("Couleur de « %s »", t.name)) },
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
            confirmButton = { TextButton(onClick = { colouring = false }) { Text(tr("Fermer")) } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClipEditor(id: Int, clip: AudioClip, info: SampleInfo?, color: androidx.compose.ui.graphics.Color, actions: AudioTracksActions, ui: AudioTracksUi) {
    val per = ui.stepsPerBar
    val chip = pillColors()
    var range by remember(clip.file) { mutableStateOf(0f..1f) }
    Text("${com.pocketstudio.model.DefaultNames.shownSample(clip.name)} · ${String.format(I18n.lang.locale, "%.1f", clip.seconds)} s", style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
    if (clip.tempoDiffers(ui.tempoBpm)) {
        Text(
            tr("Enregistrée à %1\$d BPM, le morceau est à %2\$d BPM : elle ne tombe plus pile sur le temps.", clip.bpm.roundToInt(), ui.tempoBpm.roundToInt()),
            style = MaterialTheme.typography.bodySmall, color = Neon.Amber, modifier = Modifier.testTag("audio_tempo_$id"),
        )
    }
    info?.let { Waveform(it.peaks, color, region = range.start to range.endInclusive) }
    // Cut: keep the part between the two handles.
    RangeSlider(value = range, onValueChange = { range = it }, modifier = Modifier.testTag("audio_trim_$id"))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fadingHorizontalScroll()) {
        OutlinedButton(onClick = { actions.trim(id, range.start, range.endInclusive) }, enabled = range.start > 0.001f || range.endInclusive < 0.999f) {
            Text(tr("Garder la sélection"))
        }
        OutlinedButton(onClick = { actions.normalize(id) }, modifier = Modifier.testTag("audio_normalize_$id")) { Text(tr("Normaliser")) }
        OutlinedButton(onClick = { actions.reverse(id) }) { Text(tr("Inverser")) }
    }
    // Where it plays: in each part of a scene, or once along the whole song.
    if (ui.songParts.isNotEmpty() || clip.song) {
        Text(tr("Joue avec"), style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fadingHorizontalScroll()) {
            FilterChip(selected = !clip.song, onClick = { actions.setSong(id, false) }, label = { Text(tr("Chaque partie")) }, colors = chip)
            FilterChip(selected = clip.song, onClick = { actions.setSong(id, true) }, label = { Text(tr("Tout le morceau")) }, colors = chip, modifier = Modifier.testTag("audio_song_$id"))
        }
    }
    if (clip.song) {
        val bar = clip.startBar(per)
        val last = maxOf(ui.songBars, bar)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(tr("Commence à la mesure du morceau"), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { actions.setStart(id, bar - 1) }, enabled = bar > 1) { Text("−") }
            Text("$bar", style = MonoStyle, color = Neon.Cyan, modifier = Modifier.testTag("audio_song_bar_$id"))
            OutlinedButton(onClick = { actions.setStart(id, bar + 1) }, enabled = bar < last) { Text("+") }
        }
    } else {
        Text(tr("Commence à la mesure"), style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fadingHorizontalScroll()) {
            (1..8).forEach { bar ->
                FilterChip(selected = clip.start == (bar - 1) * per, onClick = { actions.setStart(id, bar) }, label = { Text("$bar") }, colors = chip)
            }
        }
        Text(tr("Revient"), style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fadingHorizontalScroll()) {
            FilterChip(selected = clip.once, onClick = { actions.setOnce(id, true) }, label = { Text(tr("Une fois par partie")) }, colors = chip, modifier = Modifier.testTag("audio_once_$id"))
            LOOP_BARS.forEach { bars ->
                FilterChip(selected = !clip.once && clip.loopSteps == bars * per, onClick = { actions.setLoopBars(id, bars) },
                    label = { Text(if (bars == 1) tr("toutes les mesures") else tr("toutes les %d mesures", bars)) }, colors = chip)
            }
        }
    }
    LabeledSlider(tr("Volume"), "${(clip.gain * 100).roundToInt()} %", clip.gain / 2f) { actions.setGain(id, it * 2f) }
    LabeledSlider(tr("Fondu au début"), "${String.format(I18n.lang.locale, "%.2f", clip.fadeIn)} s", clip.fadeIn / 2f) { actions.setFadeIn(id, it * 2f) }
    LabeledSlider(tr("Fondu à la fin"), "${String.format(I18n.lang.locale, "%.2f", clip.fadeOut)} s", clip.fadeOut / 2f) { actions.setFadeOut(id, it * 2f) }
    // « Décaler la prise »: a fine nudge when the voice is a little early or late.
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(tr("Décaler la prise"), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        OutlinedButton(
            onClick = { actions.setOffset(id, clip.offsetMs - AudioClip.OFFSET_STEP_MS) }, enabled = clip.offsetMs > -AudioClip.MAX_OFFSET_MS,
            modifier = Modifier.testTag("audio_earlier_$id"),
        ) { Text(tr("Plus tôt")) }
        Text(
            if (clip.offsetMs == 0f) "0 ms" else String.format(I18n.lang.locale, "%+d ms", clip.offsetMs.roundToInt()),
            style = MonoStyle, color = Neon.Cyan, modifier = Modifier.clickable { actions.setOffset(id, 0f) }.testTag("audio_offset_$id"),
        )
        OutlinedButton(
            onClick = { actions.setOffset(id, clip.offsetMs + AudioClip.OFFSET_STEP_MS) }, enabled = clip.offsetMs < AudioClip.MAX_OFFSET_MS,
            modifier = Modifier.testTag("audio_later_$id"),
        ) { Text(tr("Plus tard")) }
    }
}

@Composable
private fun LabeledSlider(label: String, value: String, position: Float, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MonoStyle, color = Neon.Cyan)
    }
    ProSlider(value = position.coerceIn(0f, 1f), onValueChange = onChange)
}
