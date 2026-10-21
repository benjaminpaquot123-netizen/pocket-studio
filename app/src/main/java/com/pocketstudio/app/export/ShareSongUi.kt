package com.pocketstudio.app.export

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pocketstudio.app.rhythm.ExportUi
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.ProjectCatalog
import com.pocketstudio.ui.Neon
import com.pocketstudio.model.i18n.tr

/**
 * « Partager mon morceau », wherever it is asked from: the name first when the project still has a
 * default one, the export's progress, then Android's share sheet. Returns what starts it.
 */
@Composable
fun rememberShareSong(rhythmVm: RhythmViewModel): () -> Unit {
    val context = LocalContext.current
    val export by rhythmVm.export.collectAsStateWithLifecycle()
    // The name being typed (null: not asking).
    var naming by rememberSaveable { mutableStateOf<String?>(null) }
    // A name the user confirmed (even a template's): not asked again while the app is open.
    var confirmed by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(rhythmVm) {
        rhythmVm.shareRequests.collect { files ->
            WavExporter.share(context, files, tr("Partager mon morceau"))
            // The song is exported and the share sheet is open: the last step of the card is done
            // (not before: a cancelled name or export leaves it to do).
            com.pocketstudio.app.onboarding.NextStepPrefs(context, rhythmVm.projectId).mark(com.pocketstudio.app.onboarding.StepGoal.Share)
        }
    }
    naming?.let { name ->
        NameSongDialog(
            name = name,
            onName = { naming = it.take(ProjectCatalog.MAX_NAME) },
            confirm = {
                val clean = name.trim()
                naming = null
                rhythmVm.renameProject(clean)
                confirmed = clean
                rhythmVm.shareSong()
            },
            dismiss = { naming = null },
        )
    }
    if (export.sharing) ShareProgressDialog(export, cancel = rhythmVm::cancelExport, dismiss = rhythmVm::dismissShare)
    return {
        val name = rhythmVm.project.value.name
        when {
            export.running -> Toast.makeText(context, tr("Un export est déjà en cours : attends qu'il finisse."), Toast.LENGTH_SHORT).show()
            ShareSong.isDefaultName(name) && name.trim() != confirmed -> naming = ShareSong.suggestedName(com.pocketstudio.model.DefaultNames.shown(name))
            else -> rhythmVm.shareSong()
        }
    }
}

/** « Nomme ton morceau »: the name the shared file (and the project) will carry. */
@Composable
fun NameSongDialog(name: String, onName: (String) -> Unit, confirm: () -> Unit, dismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(tr("Nomme ton morceau")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(tr("C'est le nom que verront ceux qui l'écoutent."), style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary)
                OutlinedTextField(
                    value = name, onValueChange = onName, singleLine = true, label = { Text(tr("Nom du morceau")) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) confirm() }),
                    modifier = Modifier.fillMaxWidth().testTag("share_name_field"),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = confirm, enabled = name.isNotBlank(), modifier = Modifier.testTag("share_name_ok")) { Text(tr("Partager")) }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text(tr("Annuler")) } },
    )
}

/** The song being prepared for sharing: progress and « Annuler », or what went wrong. */
@Composable
fun ShareProgressDialog(export: ExportUi, cancel: () -> Unit, dismiss: () -> Unit) {
    val failed = !export.running && export.error != null
    AlertDialog(
        onDismissRequest = { if (!export.running) dismiss() },
        title = { Text(if (failed) tr("Partage impossible") else tr("Je prépare ton morceau…")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("share_progress")) {
                if (failed) {
                    Text(export.error.orEmpty(), color = Neon.Red, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(export.stage ?: tr("Fabrication du son, puis compression en M4A."), style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary)
                    LinearProgressIndicator(
                        progress = { export.progress },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        color = Neon.Acid,
                        trackColor = Neon.SurfaceHigh,
                    )
                    Text(tr("Ensuite, choisis l'appli pour l'envoyer."), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                }
            }
        },
        confirmButton = {
            if (export.running) TextButton(onClick = cancel, modifier = Modifier.testTag("share_cancel")) { Text(tr("Annuler")) }
            else
 TextButton(onClick = dismiss, modifier = Modifier.testTag("share_dismiss")) { Text("OK") }
        },
    )
}
