package com.pocketstudio.app.synth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.rhythm.SampleTask
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.SectionLabel

class TranscribeUi(
    val task: SampleTask = SampleTask.Idle,
    val pickFile: () -> Unit = {},
    val record: () -> Unit = {},
    val stop: () -> Unit = {},
    val pickMidi: () -> Unit = {},
)

/** Audio to notes: hum, sing or play a melody, it lands in the piano roll. */
@Composable
fun TranscribeCard(ui: TranscribeUi) {
    NeonCard(modifier = Modifier.testTag("transcribe")) {
        SectionLabel("Importer des notes")
        Text(
            "Chante, siffle ou joue une mélodie (ou choisis un enregistrement, ou un fichier MIDI) : ses notes remplacent celles du piano roll. Annuler les ramène.",
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
        )
        when (val t = ui.task) {
            is SampleTask.Recording -> {
                Text("Enregistrement… ${"%.0f".format(t.seconds)} s", color = Neon.Acid)
                LinearProgressIndicator(progress = { t.level.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = Neon.Acid)
                Button(
                    onClick = ui.stop, modifier = Modifier.fillMaxWidth().testTag("transcribe_stop"),
                    colors = ButtonDefaults.buttonColors(containerColor = Neon.Acid),
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null)
                    Text("  Terminé")
                }
            }
            is SampleTask.Busy -> {
                Text(t.message, color = Neon.TextSecondary)
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = Neon.Cyan)
            }
            else -> {
                if (t is SampleTask.Failed) Text(t.message, color = Neon.Amber, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = ui.record, modifier = Modifier.weight(1f).testTag("transcribe_record")) {
                        Icon(Icons.Filled.Mic, contentDescription = null)
                        Text("  Au micro")
                    }
                    OutlinedButton(onClick = ui.pickFile, modifier = Modifier.weight(1f).testTag("transcribe_file")) {
                        Icon(Icons.Filled.FileOpen, contentDescription = null)
                        Text("  Un son")
                    }
                }
                OutlinedButton(onClick = ui.pickMidi, modifier = Modifier.fillMaxWidth().testTag("import_midi")) {
                    Icon(Icons.Filled.FileOpen, contentDescription = null)
                    Text("  Un fichier MIDI (.mid)")
                }
            }
        }
    }
}
