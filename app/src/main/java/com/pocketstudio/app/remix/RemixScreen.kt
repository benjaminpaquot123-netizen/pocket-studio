@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.pocketstudio.app.remix

import com.pocketstudio.ui.ScaledAlertDialog

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import com.pocketstudio.app.help.Glossary
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.sp
import androidx.compose.material3.FilterChip
import com.pocketstudio.ui.ProSlider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.height
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import com.pocketstudio.app.rhythm.StemOverview
import com.pocketstudio.model.Remix
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.separation.Demucs
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.pillColors
import com.pocketstudio.ui.SectionLabel
import kotlin.math.roundToInt
import com.pocketstudio.model.i18n.tr

class RemixUi(
    val modelInstalled: Boolean,
    val state: SeparationState,
    val sets: List<StemSet>,
    /** The song whose stems are being listened to together, if any. */
    val preview: StemPreview? = null,
    /** The song played along with the project (setId null = none). */
    val remix: Remix = Remix(),
    val remixLoading: Boolean = false,
    /** The song is being fitted to the project's tempo. */
    val stretching: Boolean = false,
    val transportPlaying: Boolean = false,
    val projectBpm: Double = 120.0,
    /** What the last « → Pad » is doing or why it failed. */
    val padTask: String? = null,
    /** Waveforms of the stems in the project, and where the playhead is (-1 = stopped). */
    val overview: StemOverview? = null,
    val position: Long = -1,
    /** Why this device cannot separate a song (not enough memory), said before any download. */
    val memoryProblem: String? = null,
    /** [memoryProblem] is the device's size: checking again would change nothing. */
    val memoryNever: Boolean = false,
)

class RemixActions(
    val downloadModel: () -> Unit = {},
    val pickSong: () -> Unit = {},
    val cancel: () -> Unit = {},
    val dismiss: () -> Unit = {},
    /** Listening: all four stems of a song together, apart from the project. */
    val playAll: (StemSet) -> Unit = {},
    /** Listening: one stem on or off, the others keep playing. */
    val toggleStem: (StemSet, Int) -> Unit = { _, _ -> },
    val pause: () -> Unit = {},
    val delete: (StemSet) -> Unit = {},
    /** The project plays the song along, at its tempo and in its key. */
    val useSong: (StemSet) -> Unit = {},
    val clearRemix: () -> Unit = {},
    val togglePlay: () -> Unit = {},
    val setStartBar: (Int) -> Unit = {},
    val setStemVolume: (Int, Float) -> Unit = { _, _ -> },
    val toggleStemMute: (Int) -> Unit = {},
    val toggleStemSolo: (Int) -> Unit = {},
    val stemToPad: (Int, Int) -> Unit = { _, _ -> },
    /** [bars] bars of a stem onto the first free live pad (Morceau tab). */
    val stemToLive: (Int, Int) -> Unit = { _, _ -> },
    /** The song follows the project's tempo (stretched, pitch kept). */
    val setFollowTempo: (Boolean) -> Unit = {},
    /** Touching a waveform: start the remix at that bar (fraction 0..1 of the song). */
    val setStartAt: (Float) -> Unit = {},
    /** Reads the free memory again. */
    val checkMemory: () -> Unit = {},
    /** The effects box of the remix's strip (the live pads play there too); null: no button. */
    val effects: (() -> Unit)? = null,
)

/** Remix: split a song into drums, bass, other and vocals, then listen to each part. */
@Composable
fun RemixScreen(ui: RemixUi, actions: RemixActions) {
    // On a tablet: the remix in the project and a new separation on the left, the separated songs on the right.
    // Nothing separated yet: one column at a reading width.
    if (ui.sets.isEmpty()) {
        com.pocketstudio.ui.ReadingWidth {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) { RemixStart(ui, actions) }
        }
    } else {
        com.pocketstudio.ui.AdaptivePanes(
            spacing = 12.dp,
            first = { RemixStart(ui, actions) },
            second = { ui.sets.forEach { set -> StemCard(set, ui.preview?.takeIf { it.setId == set.id }, ui.remix.setId == set.id, actions) } },
        )
    }
}

@Composable
private fun RemixStart(ui: RemixUi, actions: RemixActions) {
    if (ui.remix.setId != null) InProject(ui, actions)
    NeonCard(modifier = Modifier.testTag("remix_start")) {
        SectionLabel(tr("Séparer une chanson"), help = Glossary.REMIX)
        Text(
            tr("Choisis un morceau de l'appareil : l'appli le découpe en batterie, basse, voix et le reste.") + " " +
                tr("Compte quelques minutes ; tu peux éteindre l'écran."),
            style = MaterialTheme.typography.bodyMedium,
        )
        when (val s = ui.state) {
            is SeparationState.DownloadingModel -> Progress(tr("Téléchargement du modèle… %d %%", (s.progress * 100).roundToInt()), s.progress, actions.cancel)
            is SeparationState.Decoding -> Progress(tr("Lecture de « %s »…", s.name), null, actions.cancel)
            is SeparationState.Separating -> Progress(tr("Séparation de « %s »… %d %%", s.name, (s.progress * 100).roundToInt()), s.progress, actions.cancel)
            is SeparationState.Analysing -> Progress(tr("Tempo et tonalité de « %s »…", s.name), null, actions.cancel)
            else -> {
                if (s is SeparationState.Failed) {
                    Text(s.message, color = Neon.Amber, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("remix_error"))
                }
                // Said before the model's download, not after it: no song can be separated here now.
                if (ui.memoryProblem != null) {
                    Text(ui.memoryProblem, color = Neon.Amber, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("remix_memory"))
                    if (!ui.memoryNever) {
                        OutlinedButton(onClick = actions.checkMemory, modifier = Modifier.fillMaxWidth().testTag("remix_memory_check")) {
                            Text(tr("Vérifier à nouveau"))
                        }
                    }
                    return@NeonCard
                }
                if (!ui.modelInstalled) {
                    Text(
                        tr("Il faut d'abord le modèle de séparation (%s, une seule fois, Wi-Fi conseillé).", SeparationModel.SPEC.sizeLabel),
                        style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
                    )
                    OutlinedButton(onClick = actions.downloadModel, modifier = Modifier.fillMaxWidth().testTag("remix_download")) {
                        Text(tr("Télécharger le modèle (%s)", SeparationModel.SPEC.sizeLabel))
                    }
                }
                Button(onClick = actions.pickSong, modifier = Modifier.fillMaxWidth().testTag("remix_pick")) {
                    Text(tr("Choisir une chanson"))
                }
                Text(
                    tr("%d minutes au maximum. Les %d derniers morceaux séparés sont gardés, et ceux qu'un projet utilise.", SeparationService.MAX_SECONDS / 60, StemStore.KEEP),
                    style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun Progress(text: String, fraction: Float?, cancel: () -> Unit) {
    Text(text, modifier = Modifier.testTag("remix_progress"))
    if (fraction == null) {
        LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 4.dp), color = Neon.Cyan)
    } else {
        LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), color = Neon.Cyan)
    }
    OutlinedButton(onClick = cancel, modifier = Modifier.fillMaxWidth().testTag("remix_cancel")) { Text(tr("Annuler")) }
}

@Composable
private fun InProject(ui: RemixUi, actions: RemixActions) {
    val r = ui.remix
    var padBars by rememberSaveable { mutableIntStateOf(1) }
    NeonCard(modifier = Modifier.testTag("remix_project")) {
        SectionLabel(tr("Dans le projet : %s", r.name), color = Neon.Magenta, foldKey = "Dans le projet")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = actions.togglePlay, modifier = Modifier.testTag("remix_play")) {
                Icon(if (ui.transportPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null)
                Text(" " + if (ui.transportPlaying) tr("Arrêter") else tr("Jouer"))
            }
            Text(tr("Mesure %d", r.startBar + 1), Modifier.weight(1f), maxLines = 1)
            actions.effects?.let { open ->
                TextButton(onClick = open, modifier = Modifier.testTag("remix_effects")) { Text(tr("Effets")) }
            }
            IconButton(onClick = { actions.setStartBar(r.startBar - 1) }, enabled = r.startBar > 0) {
                Icon(Icons.Filled.Remove, contentDescription = tr("Mesure précédente"))
            }
            IconButton(onClick = { actions.setStartBar(r.startBar + 1) }, modifier = Modifier.testTag("remix_bar_next")) {
                Icon(Icons.Filled.Add, contentDescription = tr("Mesure suivante"))
            }
        }
        ui.overview?.let { ov ->
            val now = if (ui.position >= 0) ui.position else r.startFrame(SAMPLE_RATE)
            Text(
                "${clock(now)} / ${clock(ov.frames)}" + if (ui.position < 0) " · " + tr("départ") else "",
                style = MonoStyle, color = Neon.TextSecondary,
            )
        }
        if (ui.remixLoading) {
            Text(tr("Chargement des pistes…"), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = Neon.Magenta)
        }
        if (r.songBpm > 0 && kotlin.math.abs(ui.projectBpm - r.songBpm) > 0.05) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (r.followTempo) tr("Le morceau (%.1f BPM) suit le tempo du projet (%.1f BPM), sans changer de hauteur.", r.songBpm, ui.projectBpm)
                    else tr("Le projet est à %.1f BPM, le morceau à %.1f : les pistes gardent leur vitesse et se décalent.", ui.projectBpm, r.songBpm),
                    style = MaterialTheme.typography.bodySmall, color = if (r.followTempo) Neon.TextSecondary else Neon.Amber,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = r.followTempo, onCheckedChange = actions.setFollowTempo, modifier = Modifier.testTag("remix_follow_tempo"))
            }
            if (ui.stretching) {
                Text(tr("Calage du morceau au tempo…"), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = Neon.Magenta)
            }
        }
        FlowRow(itemVerticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(tr("Vers le pad :"), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
            listOf(1, 2, 4).forEach { b ->
                FilterChip(
                    selected = padBars == b, onClick = { padBars = b },
                    label = { Text(com.pocketstudio.app.ui.barsShort(b)) },
                    colors = pillColors(),
                )
            }
        }
        ui.padTask?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Neon.Amber) }
        Demucs.STEMS.forEachIndexed { i, stem ->
            val m = r.stems[i]
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(StemStore.label(stem), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                FilterChip(
                    selected = m.mute, onClick = { actions.toggleStemMute(i) }, label = { Text("M") },
                    colors = pillColors(Neon.Amber),
                    modifier = Modifier.testTag("stem_mute_$i"),
                )
                FilterChip(
                    selected = m.solo, onClick = { actions.toggleStemSolo(i) }, label = { Text("S") },
                    colors = pillColors(Neon.Acid),
                    modifier = Modifier.testTag("stem_solo_$i"),
                )
                TextButton(onClick = { actions.stemToPad(i, padBars) }, modifier = Modifier.testTag("stem_pad_$i")) { Text("→ Pad") }
                TextButton(onClick = { actions.stemToLive(i, padBars) }, modifier = Modifier.testTag("stem_live_$i")) { Text("→ Live") }
            }
            ui.overview?.let { ov ->
                Waveform(
                    peaks = ov.peaks[i],
                    playhead = if (ui.position >= 0) ui.position.toFloat() / ov.frames else -1f,
                    start = r.startFrame(SAMPLE_RATE).toFloat() / ov.frames,
                    dimmed = m.mute,
                    onTap = actions.setStartAt,
                    modifier = Modifier.testTag("waveform_$i"),
                )
            }
            ProSlider(value = m.volume, onValueChange = { actions.setStemVolume(i, it) }, modifier = Modifier.testTag("stem_volume_$i"))
        }
        Text(
            tr("Les pistes démarrent avec le Rythme, à partir de la mesure choisie (trait vert). Touche une forme d'onde pour partir de là.") + " " +
                tr("« → Pad » copie les mesures choisies sur le pad sélectionné, « → Live » sur un pad live libre (Live › Pads) pour lancer et couper chaque partie en rythme."),
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
        )
        TextButton(onClick = actions.clearRemix, modifier = Modifier.testTag("remix_clear")) { Text(tr("Retirer du projet")) }
    }
}

/** Asks before deleting a separated song; refuses while a project plays it along. */
@Composable
private fun DeleteStemsDialog(set: StemSet, inProject: Boolean, onDelete: () -> Unit, onDismiss: () -> Unit) {
    val users = (listOfNotNull(tr("le projet ouvert").takeIf { inProject }) + set.usedBy.map { tr("« %s »", com.pocketstudio.model.DefaultNames.shown(it)) }).distinct()
    if (users.isNotEmpty()) {
        ScaledAlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(tr("Morceau utilisé")) },
            text = {
                Text(tr("« %s » joue dans %s. Retire-le d'abord de ce projet (« Retirer du projet ») pour pouvoir le supprimer.", set.name, users.joinToString(", ")))
            },
            confirmButton = { TextButton(onClick = onDismiss, modifier = Modifier.testTag("delete_stems_ok")) { Text("OK") } },
            modifier = Modifier.testTag("delete_stems_dialog"),
        )
    } else {
        ScaledAlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(tr("Supprimer ce morceau ?")) },
            text = { Text(tr("Les pistes séparées de « %s » seront effacées. Pour les retrouver, il faudra séparer la chanson à nouveau.", set.name)) },
            confirmButton = {
                TextButton(onClick = { onDismiss(); onDelete() }, modifier = Modifier.testTag("delete_stems_confirm")) { Text(tr("Supprimer"), color = Neon.RedText) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Annuler")) } },
            modifier = Modifier.testTag("delete_stems_dialog"),
        )
    }
}

@Composable
private fun StemCard(set: StemSet, preview: StemPreview?, inProject: Boolean, actions: RemixActions) {
    var confirmDelete by rememberSaveable(set.id) { mutableStateOf(false) }
    NeonCard(modifier = Modifier.testTag("stems_${set.id}")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SectionLabel(set.name)
                val total = set.seconds.roundToInt()
                val details = listOfNotNull(
                    "%d:%02d".format(total / 60, total % 60),
                    set.info.bpm.takeIf { it > 0 }?.let { "%.0f BPM".format(it) },
                    set.info.keyLabel,
                ).joinToString(" · ")
                Text(details, style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, modifier = Modifier.testTag("song_info_${set.id}"))
            }
            IconButton(onClick = { confirmDelete = true }, modifier = Modifier.testTag("delete_${set.id}")) {
                Icon(Icons.Filled.Delete, contentDescription = tr("Supprimer"))
            }
        }
        if (confirmDelete) DeleteStemsDialog(set, inProject, onDelete = { actions.delete(set) }, onDismiss = { confirmDelete = false })
        if (!inProject) {
            Button(onClick = { actions.useSong(set) }, modifier = Modifier.fillMaxWidth().testTag("use_${set.id}")) {
                Text(tr("Jouer avec le projet"))
            }
        }
        val playing = preview?.playing == true
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { if (playing) actions.pause() else actions.playAll(set) },
                enabled = preview?.loading != true,
                modifier = Modifier.testTag("listen_${set.id}"),
            ) {
                Icon(if (playing) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null, tint = if (playing) Neon.Acid else Neon.Cyan)
                Text(" " + if (playing) tr("Arrêter") else tr("Écouter"), color = if (playing) Neon.Acid else Neon.Cyan)
            }
            val clockText = when {
                preview?.loading == true -> tr("Chargement…")
                playing && preview.frames > 0 -> "${clock(preview.position.coerceAtLeast(0))} / ${clock(preview.frames)}"
                else -> tr("Allume ou coupe chaque partie, même en pleine lecture.")
            }
            Text(clockText, style = if (playing) MonoStyle else MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, modifier = Modifier.weight(1f))
        }
        if (playing && preview.frames > 0) {
            LinearProgressIndicator(
                progress = { (preview.position.coerceAtLeast(0).toFloat() / preview.frames).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(), color = Neon.Acid,
            )
        }
        // One big switch per part: lit = heard. Switching never stops the others.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Demucs.STEMS.forEachIndexed { i, stem ->
                val on = preview != null && preview.on.getOrElse(i) { false }
                val lit = on && playing
                Button(
                    onClick = { actions.toggleStem(set, i) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (lit) Neon.Acid else if (on) Neon.SurfaceHigh else Neon.Surface,
                        contentColor = if (lit) androidx.compose.ui.graphics.Color(0xFF14210F) else if (on) Neon.TextPrimary else Neon.TextSecondary,
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (on) Neon.Acid else Neon.Outline),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp),
                    modifier = Modifier.weight(1f).height(56.dp).testTag("stem_${set.id}_$stem"),
                ) {
                    Text(StemStore.label(stem), fontSize
 = 13.sp, maxLines = 1, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }
}

private const val SAMPLE_RATE = 48_000

private fun clock(frames: Long): String {
    val s = (frames / SAMPLE_RATE).toInt()
    return "%d:%02d".format(s / 60, s % 60)
}

/** A stem's waveform: played part bright, playhead in white, start point in green. Tap to move the start. */
@Composable
private fun Waveform(peaks: FloatArray, playhead: Float, start: Float, dimmed: Boolean, onTap: (Float) -> Unit, modifier: Modifier = Modifier) {
    val bright = if (dimmed) Neon.TextSecondary.copy(alpha = 0.4f) else Neon.Cyan
    val dark = bright.copy(alpha = 0.35f)
    val head = Neon.TextPrimary
    val mark = Neon.Acid
    Canvas(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .pointerInput(Unit) { detectTapGestures { offset -> onTap(offset.x / size.width) } },
    ) {
        if (peaks.isEmpty()) return@Canvas
        val step = size.width / peaks.size
        val mid = size.height / 2
        peaks.forEachIndexed { i, p ->
            val x = i * step
            val h = (p * mid).coerceAtLeast(1f)
            val played = playhead >= 0 && x / size.width <= playhead
            drawLine(if (played) bright else dark, Offset(x, mid - h), Offset(x, mid + h), strokeWidth = maxOf(1f, step * 0.7f))
        }
        if (start in 0f..1f) drawLine(mark, Offset(start * size.width, 0f), Offset(start * size.width, size.height), strokeWidth = 3f)
        if (playhead in 0f..1f) drawLine(head, Offset(playhead * size.width, 0f), Offset(playhead * size.width, size.height), strokeWidth = 4f)
    }
}

