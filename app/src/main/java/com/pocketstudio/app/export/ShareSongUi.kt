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
import androidx.compose.runtime.setValue
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
fun ShareSongProgress(rhythmVm: RhythmViewModel, wav: Boolean = false, simple: Boolean = false) {
    val context = LocalContext.current
    val export by rhythmVm.export.collectAsStateWithLifecycle()
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    // Audit ultra round 1 (n° 15): what was shared, in numbers, waiting under the share menu (null: nothing to say).
    var done by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf<List<String>?>(null) }
    LaunchedEffect(rhythmVm, lifecycle) {
        shareWhileShown(lifecycle, rhythmVm.shareRequests) { files ->
            // The song is exported and the share sheet is open: the last step of the card is done
            // (not before: a cancelled export, or a sheet that could not open, leaves it to do).
            if (WavExporter.share(context, files, tr("Partager mon morceau"))) {
                // G5: the Guide's « Prêt à partager ? » is decided (« C'est partagé »).
                rhythmVm.guide.markDone("share.ready", "keep")
                // Remix testers (Mehdi): the share ended with no word from the app; the file is said ready, and where its copy is,
                // and (audit ultra round 1) its loudness, peak, format, length and target, seen when the share menu closes.
                val now = rhythmVm.export.value
                done = listOf(shareReadyText(files, now.savedInMusic)) + shareReportLines(now, simple)
            }
        }
    }
    if (export.sharing) ShareProgressDialog(export, cancel = rhythmVm::cancelExport, dismiss = rhythmVm::dismissShare, wav = wav)
    else done?.let { lines -> ShareBilanDialog(lines) { done = null } }
}

/** « C'est prêt » after « Partager »: the file, then its report in numbers (audit ultra round 1, n° 15). */
@Composable
internal fun ShareBilanDialog(lines: List<String>, dismiss: () -> Unit) {
    ScaledAlertDialog(
        onDismissRequest = dismiss,
        title = { Text(tr("C'est prêt")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.testTag("share_bilan")) {
                lines.forEachIndexed { i, line ->
                    Text(line, style = if (i == 0) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                        color = if (i == 0) Neon.TextPrimary else Neon.TextSecondary)
                }
            }
        },
        confirmButton = { TextButton(onClick = dismiss, modifier = Modifier.testTag("share_bilan_ok")) { Text("OK") } },
    )
}

/**
 * What « C'est prêt » says under the file: the report in numbers (audit ultra round 1, n° 15), or in everyday
 * words in the Mode simple (audit ultra round 3, n° 14: LUFS, dBTP, kHz and kb/s were shown there).
 */
internal fun shareReportLines(export: ExportUi, simple: Boolean): List<String> =
    if (simple) export.summary?.plainLines().orEmpty() + listOfNotNull(export.bilan?.levelWords())
    else export.bilan?.lines().orEmpty()

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
        // Testers (lot « tuto 5 »): the back gesture stops it as « Annuler » does; a touch beside it (a second
        // touch on « Partager ») does nothing.
        onDismissRequest = { if (export.running) cancel() else dismiss() },
        properties = androidx.compose.ui.window.DialogProperties(dismissOnClickOutside = !export.running),
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
                    // Testers (lot « tuto 5 »): a bar at 0 is a still dot that looks stuck; until the render moves,
                    // the bar runs (the sounds are being read), then it fills with its percentage beside it.
                    val shown = export.progress.coerceIn(0f, 1f)
                    if (shown <= 0f) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).testTag("share_progress_waiting"),
                            color = Neon.Acid, trackColor = Neon.SurfaceHigh,
                        )
                    } else {
                        androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            LinearProgressIndicator(
                                progress = { shown },
                                modifier = Modifier.weight(1f).padding(vertical = 6.dp).testTag("share_progress_bar"),
                                color = Neon.Acid,
                                trackColor = Neon.SurfaceHigh,
                            )
                            Text(
                                "  " + com.pocketstudio.model.i18n.percent((shown * 100).toInt()),
                                style = MaterialTheme.typography.labelLarge, color = Neon.TextSecondary,
                                modifier = Modifier.testTag("share_progress_percent"),
                            )
                        }
                    }
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
