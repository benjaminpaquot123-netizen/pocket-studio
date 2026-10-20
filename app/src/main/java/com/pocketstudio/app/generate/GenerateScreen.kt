package com.pocketstudio.app.generate

import com.pocketstudio.app.help.Glossary
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pocketstudio.separation.StableAudio
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.SectionLabel
import kotlin.math.roundToInt

class GenerateUi(
    val installed: Boolean,
    val download: ModelDownload,
    val state: GenerateState,
    val prompt: String,
    val seconds: Float,
    /** Title and seconds of the sound waiting to be placed, if any. */
    val result: String? = null,
    val resultSeconds: Double = 0.0,
    val playing: Boolean = false,
    val projectBpm: Int = 120,
    val selectedPad: Int = 0,
    /** What placing the sound is doing or why it failed (shared pad-sound card). */
    val placeTask: String? = null,
)

class GenerateActions(
    val download: () -> Unit = {},
    val cancelDownload: () -> Unit = {},
    val setPrompt: (String) -> Unit = {},
    val setSeconds: (Float) -> Unit = {},
    val generate: () -> Unit = {},
    val cancel: () -> Unit = {},
    val dismissError: () -> Unit = {},
    val play: () -> Unit = {},
    val stop: () -> Unit = {},
    val toPad: () -> Unit = {},
    val toLive: () -> Unit = {},
    val toAudioTrack: () -> Unit = {},
    val discard: () -> Unit = {},
    val removeModel: () -> Unit = {},
)

/** Examples in French for the button, in English for the model (it only reads English well). */
fun generateExamples(bpm: Int): List<Pair<String, String>> = listOf(
    "Kick" to "punchy kick drum one shot",
    "Caisse claire" to "crisp snare drum one shot",
    "Boucle batterie" to "$bpm BPM drum loop, tight groove",
    "Boucle house" to "$bpm BPM house drum loop with hi-hats",
    "Basse" to "deep synth bass loop, $bpm BPM",
    "Nappe" to "warm ambient synth pad, soft",
    "Guitare" to "acoustic guitar strumming loop, $bpm BPM",
    "Piano" to "lo-fi piano chords loop, $bpm BPM",
    "Pluie" to "rain falling on a window, ambience",
    "Effet whoosh" to "riser whoosh sound effect",
)

/** Text to sound: describe it, wait a few seconds, listen, put it on a pad or a track. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GenerateScreen(ui: GenerateUi, actions: GenerateActions) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!ui.installed) ModelCard(ui, actions) else PromptCard(ui, actions)
        if (ui.result != null) ResultCard(ui, actions)
        Text(
            "Powered by Stability AI · Stable Audio Open Small, licence Stability AI Community. " +
                "Le son est créé sur le téléphone, sans internet.",
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
            modifier = Modifier.testTag("generate_credit"),
        )
        if (ui.installed) {
            val busy = ui.state is GenerateState.Running || ui.state is GenerateState.Checking
            TextButton(onClick = actions.removeModel, enabled = !busy, modifier = Modifier.testTag("generate_remove")) {
                Text("Supprimer le modèle (libère ${GenerationModel.SIZE_LABEL})", color = Neon.TextSecondary)
            }
        }
    }
}

@Composable
private fun ModelCard(ui: GenerateUi, actions: GenerateActions) {
    NeonCard(modifier = Modifier.testTag("generate_model")) {
        SectionLabel("Créer un son avec une phrase", help = Glossary.GENERATE)
        Text(
            "Écris ce que tu veux entendre (« drum loop », « warm pad »…) et l'appli le fabrique. " +
                "Il faut d'abord le modèle : ${GenerationModel.SIZE_LABEL}, une seule fois. Wi-Fi conseillé.",
            style = MaterialTheme.typography.bodyMedium,
        )
        when (val d = ui.download) {
            is ModelDownload.Running -> {
                Text("Téléchargement… ${(d.progress * 100).roundToInt()} %", modifier = Modifier.testTag("generate_download_progress"))
                LinearProgressIndicator(progress = { d.progress }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), color = Neon.Cyan)
                OutlinedButton(onClick = actions.cancelDownload, modifier = Modifier.fillMaxWidth()) { Text("Annuler") }
            }
            else -> {
                if (d is ModelDownload.Failed) Text(d.message, color = Neon.Amber, style = MaterialTheme.typography.bodyMedium)
                Button(onClick = actions.download, modifier = Modifier.fillMaxWidth().testTag("generate_download")) {
                    Text("Télécharger le modèle (${GenerationModel.SIZE_LABEL})")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PromptCard(ui: GenerateUi, actions: GenerateActions) {
    val busy = ui.state is GenerateState.Running || ui.state is GenerateState.Checking
    NeonCard(modifier = Modifier.testTag("generate_prompt_card")) {
        SectionLabel("Décris le son")
        OutlinedTextField(
            value = ui.prompt,
            onValueChange = actions.setPrompt,
            enabled = !busy,
            placeholder = { Text("ex. 120 BPM drum loop, warm pad…") },
            supportingText = { Text("En anglais, c'est mieux compris. Touche un exemple pour commencer.") },
            modifier = Modifier.fillMaxWidth().testTag("generate_prompt"),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            generateExamples(ui.projectBpm).forEachIndexed { i, (label, text) ->
                AssistChip(onClick = { actions.setPrompt(text) }, enabled = !busy, label = { Text(label) }, modifier = Modifier.testTag("generate_example_$i"))
            }
        }
        Text("Durée : ${ui.seconds.roundToInt()} s", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = ui.seconds, onValueChange = { actions.setSeconds(it.roundToInt().toFloat()) },
            valueRange = 1f..StableAudio.MAX_SECONDS, steps = StableAudio.MAX_SECONDS.toInt() - 2, enabled = !busy,
            modifier = Modifier.testTag("generate_seconds"),
        )
        when (val s = ui.state) {
            is GenerateState.Checking -> {
                Text("Vérification du modèle…")
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 4.dp), color = Neon.Cyan)
            }
            is GenerateState.Running -> {
                Text("Création du son… ${(s.progress * 100).roundToInt()} %", modifier = Modifier.testTag("generate_progress"))
                LinearProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), color = Neon.Magenta)
                OutlinedButton(onClick = actions.cancel, modifier = Modifier.fillMaxWidth().testTag("generate_cancel")) { Text("Annuler") }
            }
            else -> {
                if (s is GenerateState.Failed) {
                    Text(s.message, color = Neon.Amber, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("generate_error"))
                }
                Button(onClick = actions.generate, enabled = ui.prompt.isNotBlank(), modifier = Modifier.fillMaxWidth().testTag("generate_go")) {
                    Text(if (ui.result == null) "Générer" else "Générer un autre essai")
                }
                Text("Quelques secondes à une minute selon le téléphone.", style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultCard(ui: GenerateUi, actions: GenerateActions) {
    NeonCard(modifier = Modifier.testTag("generate_result")) {
        SectionLabel("Résultat : ${ui.result}", color = Neon.Magenta)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = if (ui.playing) actions.stop else actions.play, modifier = Modifier.testTag("generate_play")) {
                Icon(if (ui.playing) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null)
                Text(if (ui.playing) " Arrêter" else " Écouter")
            }
            Text("${"%.1f".format(ui.resultSeconds)} s", color = Neon.TextSecondary)
        }
        Text("Le poser :", style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = actions.toPad, modifier = Modifier.testTag("generate_to_pad")) { Text("Pad ${ui.selectedPad + 1}") }
            OutlinedButton(onClick = actions.toLive, modifier = Modifier.testTag("generate_to_live")) { Text("Pad live") }
            OutlinedButton(onClick = actions.toAudioTrack, modifier = Modifier.testTag("generate_to_track")) { Text("Piste audio") }
        }
        ui.placeTask?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary) }
        TextButton(onClick = actions.discard, modifier = Modifier.testTag("generate_discard")) { Text("Jeter ce son") }
    }
}
