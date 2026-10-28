package com.pocketstudio.app.generate

import com.pocketstudio.ui.pressScale
import com.pocketstudio.model.i18n.tr

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
import com.pocketstudio.ui.ProSlider
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
    tr("Kick") to "punchy kick drum one shot",
    tr("Caisse claire") to "crisp snare drum one shot",
    tr("Boucle batterie") to "$bpm BPM drum loop, tight groove",
    tr("Boucle house") to "$bpm BPM house drum loop with hi-hats",
    tr("Basse") to "deep synth bass loop, $bpm BPM",
    tr("Nappe") to "warm ambient synth pad, soft",
    tr("Guitare") to "acoustic guitar strumming loop, $bpm BPM",
    tr("Piano") to "lo-fi piano chords loop, $bpm BPM",
    tr("Pluie") to "rain falling on a window, ambience",
    tr("Effet whoosh") to "riser whoosh sound effect",
    // Current genres and sounds.
    tr("Boucle drum & bass") to "$bpm BPM drum and bass breakbeat loop, punchy",
    tr("Boucle UK garage") to "$bpm BPM UK garage 2-step drum loop, shuffled hi-hats",
    tr("Boucle drill") to "$bpm BPM UK drill drum loop, sliding 808, dark",
    tr("Boucle afro house") to "$bpm BPM afro house percussion loop, congas and shakers",
    tr("Log drum amapiano") to "amapiano log drum bass loop, $bpm BPM",
    tr("Boucle jersey club") to "$bpm BPM jersey club drum loop, bouncy kicks",
    tr("Cloche phonk") to "phonk cowbell melody loop, distorted, $bpm BPM",
    tr("808 saturée") to "distorted 808 bass one shot, long sub, note C",
    tr("Basse Reese") to "dark reese bass loop, drum and bass, $bpm BPM",
    tr("Accords future bass") to "future bass supersaw chords, sidechained, $bpm BPM",
    tr("Lead hyperpop") to "hyperpop glitchy bright synth lead, $bpm BPM",
    tr("Voix découpée") to "chopped vocal sample, pitched, catchy",
    tr("Montée") to "white noise riser build-up, tension",
    tr("Impact") to "cinematic impact boom hit",
)

/** Text to sound: describe it, wait a few seconds, listen, put it on a pad or a track. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GenerateScreen(ui: GenerateUi, actions: GenerateActions) {
    val main: @Composable () -> Unit = { if (!ui.installed) ModelCard(ui, actions) else PromptCard(ui, actions) }
    if (ui.result != null) {
        // On a tablet: the phrase on the left, the sound made on the right.
        com.pocketstudio.ui.AdaptivePanes(
            spacing = 12.dp,
            first = { main() },
            second = {
                ResultCard(ui, actions)
                GenerateFooter(ui, actions)
            },
        )
    } else {
        com.pocketstudio.ui.ReadingWidth {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                main()
                GenerateFooter(ui, actions)
            }
        }
    }
}

/** The credit of the model, and the way to remove it. */
@Composable
private fun GenerateFooter(ui: GenerateUi, actions: GenerateActions) {
    Text(
        "Powered by Stability AI · Stable Audio Open Small, " + tr("licence Stability AI Community.") + " " +
            tr("Le son est créé sur l'appareil, sans internet."),
        style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
        modifier = Modifier.testTag("generate_credit"),
    )
    if (ui.installed) {
        val busy = ui.state is GenerateState.Running || ui.state is GenerateState.Checking
        TextButton(onClick = actions.removeModel, enabled = !busy, modifier = Modifier.testTag("generate_remove")) {
            Text(tr("Supprimer le modèle (libère %s)", GenerationModel.SIZE_LABEL), color = Neon.TextSecondary)
        }
    }
}

@Composable
private fun ModelCard(ui: GenerateUi, actions: GenerateActions) {
    NeonCard(modifier = Modifier.testTag("generate_model")) {
        SectionLabel(tr("Créer un son avec une phrase"), help = Glossary.GENERATE, foldKey = "Créer un son avec une phrase")
        Text(
            tr("Écris ce que tu veux entendre (« drum loop », « warm pad »…) et l'appli le fabrique.") + " " +
                tr("Il faut d'abord le modèle : %s, une seule fois. Wi-Fi conseillé.", GenerationModel.SIZE_LABEL),
            style = MaterialTheme.typography.bodyMedium,
        )
        when (val d = ui.download) {
            is ModelDownload.Running -> {
                Text(tr("Téléchargement… %d %%", (d.progress * 100).roundToInt()), modifier = Modifier.testTag("generate_download_progress"))
                LinearProgressIndicator(progress = { d.progress }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), color = Neon.Cyan)
                OutlinedButton(onClick = actions.cancelDownload, modifier = Modifier.fillMaxWidth()) { Text(tr("Annuler")) }
            }
            else -> {
                if (d is ModelDownload.Failed) Text(d.message, color = Neon.Amber, style = MaterialTheme.typography.bodyMedium)
                Button(onClick = actions.download, modifier = Modifier.pressScale().fillMaxWidth().testTag("generate_download")) {
                    Text(tr("Télécharger le modèle (%s)", GenerationModel.SIZE_LABEL))
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
        SectionLabel(tr("Décris le son"), foldKey = "Décris le son")
        OutlinedTextField(
            value = ui.prompt,
            onValueChange = actions.setPrompt,
            enabled = !busy,
            placeholder = { Text(tr("ex. 120 BPM drum loop, warm pad…")) },
            supportingText = { Text(tr("En anglais, c'est mieux compris. Touche un exemple pour commencer.")) },
            modifier = Modifier.fillMaxWidth().testTag("generate_prompt"),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            generateExamples(ui.projectBpm).forEachIndexed { i, (label, text) ->
                AssistChip(onClick = { actions.setPrompt(text) }, enabled = !busy, label = { Text(label) }, modifier = Modifier.testTag("generate_example_$i"))
            }
        }
        Text(tr("Durée : %d s", ui.seconds.roundToInt()), style = MaterialTheme.typography.bodyMedium)
        ProSlider(
            value = ui.seconds, onValueChange = { actions.setSeconds(it.roundToInt().toFloat()) },
            valueRange = 1f..StableAudio.MAX_SECONDS, steps = StableAudio.MAX_SECONDS.toInt() - 2, enabled = !busy,
            modifier = Modifier.testTag("generate_seconds"),
        )
        when (val s = ui.state) {
            is GenerateState.Checking -> {
                Text(tr("Vérification du modèle…"))
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 4.dp), color = Neon.Cyan)
            }
            is GenerateState.Running -> {
                Text(tr("Création du son… %d %%", (s.progress * 100).roundToInt()), modifier = Modifier.testTag("generate_progress"))
                LinearProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), color = Neon.Magenta)
                OutlinedButton(onClick = actions.cancel, modifier = Modifier.fillMaxWidth().testTag("generate_cancel")) { Text(tr("Annuler")) }
            }
            else -> {
                if (s is GenerateState.Failed) {
                    Text(s.message, color = Neon.Amber, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("generate_error"))
                }
                Button(onClick = actions.generate, enabled = ui.prompt.isNotBlank(), modifier = Modifier.pressScale(enabled = ui.prompt.isNotBlank()).fillMaxWidth().testTag("generate_go")) {
                    Text(if (ui.result == null) tr("Générer") else tr("Générer un autre essai"))
                }
                Text(tr("Quelques secondes à une minute selon l'appareil."), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultCard(ui: GenerateUi, actions: GenerateActions) {
    NeonCard(modifier = Modifier.testTag("generate_result")) {
        SectionLabel(tr("Résultat : %s", ui.result), color = Neon.Magenta, foldKey = "Résultat")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = if (ui.playing) actions.stop else actions.play, modifier = Modifier.pressScale().testTag("generate_play")) {
                Icon(if (ui.playing) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null)
                Text(if (ui.playing) tr(" Arrêter") else tr(" Écouter"))
            }
            Text(tr("%.1f s", ui.resultSeconds), color = Neon.TextSecondary)
        }
        Text(tr("Le poser :"), style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = actions.toPad, modifier = Modifier.testTag("generate_to_pad")) { Text(tr("Pad %d", ui.selectedPad + 1)) }
            OutlinedButton(onClick = actions.toLive, modifier = Modifier.testTag("generate_to_live")) { Text(tr("Pad live")) }
            OutlinedButton(onClick = actions.toAudioTrack, modifier = Modifier.testTag("generate_to_track")) { Text(tr("Piste audio")) }
        }
        ui.placeTask?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary) }
        TextButton(onClick = actions.discard, modifier = Modifier.testTag("generate_discard")) { Text(tr("Jeter ce son")) }
    }
}
