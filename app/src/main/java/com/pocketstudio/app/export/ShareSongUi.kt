package com.pocketstudio.app.export

import com.pocketstudio.ui.ScaledAlertDialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.pocketstudio.app.rhythm.ExportUi
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.ui.Neon
import com.pocketstudio.model.i18n.tr

/**
 * The song being shared, wherever « Partager » was touched (« Prêt à partager ? » asks the name
 * and the format first): the export's progress, then Android's share sheet. [wav]: the song goes
 * out as a WAV (no compression step to announce).
 */
@Composable
fun ShareSongProgress(rhythmVm: RhythmViewModel, wav: Boolean = false) {
    val context = LocalContext.current
    val export by rhythmVm.export.collectAsStateWithLifecycle()
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(rhythmVm, lifecycle) {
        shareWhileShown(lifecycle, rhythmVm.shareRequests) { files ->
            // The song is exported and the share sheet is open: the last step of the card is done
            // (not before: a cancelled export, or a sheet that could not open, leaves it to do).
            if (WavExporter.share(context, files, tr("Partager mon morceau"))) {
                // G5: the Guide's « Prêt à partager ? » is decided (« C'est partagé »).
                rhythmVm.guide.markDone("share.ready", "keep")
                // Remix testers (Mehdi): the share ended with no word from the app; the file is said ready, and where its copy is.
                android.widget.Toast.makeText(context, shareReadyText(files, rhythmVm.export.value.savedInMusic), android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
    if (export.sharing) ShareProgressDialog(export, cancel = rhythmVm::cancelExport, dismiss = rhythmVm::dismissShare, wav = wav)
}

/** « C'est prêt » once the song is made and its share menu open: its file, and its copy in Musique when there is one. */
internal fun shareReadyText(files: List<java.io.File>, savedInMusic: Boolean): String {
    val name = files.joinToString(", ") { it.name }
    return if (savedInMusic) tr("C'est prêt : %s, une copie est aussi dans Musique › BenStudio.", name)
    else tr("C'est prêt : %s. Choisis l'appli pour l'envoyer.", name)
}

/**
 * Opens each finished song's share sheet only while the app is on screen: Android does not open
 * one for an app in the background. A song finished while the user was in another app waits (the
 * request stays queued) and its sheet opens when they come back.
 */
internal suspend fun shareWhileShown(lifecycle: androidx.lifecycle.Lifecycle, requests: kotlinx.coroutines.flow.Flow<List<java.io.File>>, share: (List<java.io.File>) -> Unit) {
    lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) { requests.collect { share(it) } }
}

/** The song being prepared for sharing: progress and « Annuler », or what went wrong. */
@Composable
fun ShareProgressDialog(export: ExportUi, cancel: () -> Unit, dismiss: () -> Unit, wav: Boolean = false) {
    val failed = !export.running && export.error != null
    ScaledAlertDialog(
        onDismissRequest = { if (!export.running) dismiss() },
        title = { Text(if (failed) tr("Partage impossible") else tr("Je prépare ton morceau…")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("share_progress")) {
                if (failed) {
                    Text(export.error.orEmpty(), color = Neon.RedText, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(
                        export.stage ?: if (wav) tr("Fabrication du son en WAV, qualité studio.") else tr("Fabrication du son, puis compression en M4A."),
                        style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary,
                    )
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
