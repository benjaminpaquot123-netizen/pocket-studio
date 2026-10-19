package com.pocketstudio.app.remix

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pocketstudio.separation.Demucs
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.SectionLabel
import kotlin.math.roundToInt

class RemixUi(
    val modelInstalled: Boolean,
    val state: SeparationState,
    val sets: List<StemSet>,
    /** (set id, stem) being heard. */
    val playing: Pair<String, String>? = null,
)

class RemixActions(
    val downloadModel: () -> Unit = {},
    val pickSong: () -> Unit = {},
    val cancel: () -> Unit = {},
    val dismiss: () -> Unit = {},
    val play: (StemSet, String) -> Unit = { _, _ -> },
    val stop: () -> Unit = {},
    val delete: (StemSet) -> Unit = {},
    /** The project takes the song's tempo and key. */
    val matchProject: (StemSet) -> Unit = {},
)

/** Remix: split a song into drums, bass, other and vocals, then listen to each part. */
@Composable
fun RemixScreen(ui: RemixUi, actions: RemixActions) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NeonCard(modifier = Modifier.testTag("remix_start")) {
            SectionLabel("Séparer une chanson")
            Text(
                "Choisis un morceau du téléphone : l'appli le découpe en batterie, basse, voix et le reste. " +
                    "Compte quelques minutes ; tu peux éteindre l'écran.",
                style = MaterialTheme.typography.bodyMedium,
            )
            when (val s = ui.state) {
                is SeparationState.DownloadingModel -> Progress("Téléchargement du modèle… ${(s.progress * 100).roundToInt()} %", s.progress, actions.cancel)
                is SeparationState.Decoding -> Progress("Lecture de « ${s.name} »…", null, actions.cancel)
                is SeparationState.Separating -> Progress("Séparation de « ${s.name} »… ${(s.progress * 100).roundToInt()} %", s.progress, actions.cancel)
                is SeparationState.Analysing -> Progress("Tempo et tonalité de « ${s.name} »…", null, actions.cancel)
                else -> {
                    if (s is SeparationState.Failed) {
                        Text(s.message, color = Neon.Amber, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("remix_error"))
                    }
                    if (!ui.modelInstalled) {
                        Text(
                            "Il faut d'abord le modèle de séparation (${SeparationModel.SPEC.sizeLabel}, une seule fois, Wi-Fi conseillé).",
                            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
                        )
                        OutlinedButton(onClick = actions.downloadModel, modifier = Modifier.fillMaxWidth().testTag("remix_download")) {
                            Text("Télécharger le modèle (${SeparationModel.SPEC.sizeLabel})")
                        }
                    }
                    Button(onClick = actions.pickSong, modifier = Modifier.fillMaxWidth().testTag("remix_pick")) {
                        Text("Choisir une chanson")
                    }
                    Text(
                        "${SeparationService.MAX_SECONDS / 60} minutes au maximum. Les ${StemStore.KEEP} derniers morceaux séparés sont gardés.",
                        style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
                    )
                }
            }
        }
        ui.sets.forEach { set -> StemCard(set, ui.playing, actions) }
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
    OutlinedButton(onClick = cancel, modifier = Modifier.fillMaxWidth().testTag("remix_cancel")) { Text("Annuler") }
}

@Composable
private fun StemCard(set: StemSet, playing: Pair<String, String>?, actions: RemixActions) {
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
            IconButton(onClick = { actions.delete(set) }) { Icon(Icons.Filled.Delete, contentDescription = "Supprimer") }
        }
        if (set.info.bpm > 0 || set.info.root != null) {
            OutlinedButton(onClick = { actions.matchProject(set) }, modifier = Modifier.fillMaxWidth().testTag("match_${set.id}")) {
                Text("Caler le projet sur ce morceau")
            }
        }
        Demucs.STEMS.forEach { stem ->
            val on = playing == set.id to stem
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(StemStore.LABELS[stem] ?: stem, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                TextButton(
                    onClick = { if (on) actions.stop() else actions.play(set, stem) },
                    modifier = Modifier.testTag("stem_${set.id}_$stem"),
                ) {
                    Icon(if (on) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null, tint = if (on) Neon.Magenta else Neon.Cyan)
                    Text(if (on) " Arrêter" else " Écouter", color = if (on) Neon.Magenta else Neon.Cyan)
                }
            }
        }
    }
}
