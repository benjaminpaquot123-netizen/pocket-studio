package com.pocketstudio.app.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.ScaledAlertDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Audit ultra round 3 (n° 11, seat A d 45): « Mes fichiers » (« Mes exports » until the owner's answer 1A), the files the
 * app copied into the phone's shared folders (the app sees its own copies there without any permission), newest first,
 * each to send again. Before, the only trace of an export was the card's « Sauvegardé dans… » line, gone with the next export.
 *
 * T5-ZIP (recette tel-03): the sounds are in Musique › BenStudio, and so were the only files listed; a project's .zip
 * (Documents › BenStudio: Android's Music collection takes sound only) and a video (Films › BenStudio) are listed too.
 */
data class MyExport(val name: String, val uri: Uri, val bytes: Long, val addedSeconds: Long, val mime: String?, val kind: Kind = Kind.Sound) {
    enum class Kind { Sound, Video, Project }
}

object MyExports {
    /** Every file the app put in the shared folders (sounds, videos, projects' .zip), newest first. Blocking: off the main thread. */
    fun list(context: Context): List<MyExport> = (
        query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, WavExporter.MUSIC_FOLDER, MyExport.Kind.Sound) +
            query(context, com.pocketstudio.app.export.video.VideoExportViewModel.MOVIES_COLLECTION, com.pocketstudio.app.export.video.VideoExportViewModel.MOVIES_FOLDER, MyExport.Kind.Video) +
            // Documents › BenStudio also holds the exports' text reports: only the projects are files to send again.
            query(context, WavExporter.DOCUMENTS_COLLECTION, WavExporter.DOCUMENTS_FOLDER, MyExport.Kind.Project).filter { it.name.endsWith(".zip", ignoreCase = true) }
        ).sortedByDescending { it.addedSeconds }

    private fun query(context: Context, collection: Uri, folder: String, kind: MyExport.Kind): List<MyExport> = runCatching {
        val columns = arrayOf(
            MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_ADDED, MediaStore.MediaColumns.MIME_TYPE,
        )
        context.contentResolver.query(
            collection, columns, "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?", arrayOf("$folder%"),
            "${MediaStore.MediaColumns.DATE_ADDED} DESC",
        )?.use { c ->
            buildList {
                while (c.moveToNext()) {
                    val id = c.getLong(0)
                    add(MyExport(c.getString(1) ?: "", android.content.ContentUris.withAppendedId(collection, id), c.getLong(2), c.getLong(3), c.getString(4), kind))
                }
            }
        }.orEmpty()
    }.getOrDefault(emptyList())

    /** « 5 nov. 2026 10:02 · 1,2 Mo ». */
    fun detail(e: MyExport): String {
        val date = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(com.pocketstudio.model.i18n.I18n.lang.locale)
            .format(Instant.ofEpochSecond(e.addedSeconds).atZone(ZoneId.systemDefault()))
        val mb = e.bytes / 1_048_576.0
        val size = if (mb >= 1) tr("%s Mo", String.format(com.pocketstudio.model.i18n.I18n.lang.locale, "%.1f", mb)) else tr("%d Ko", (e.bytes / 1024).coerceAtLeast(1))
        return "$date · $size"
    }

    /** Sends [e] again through Android's share menu; false when it could not open. */
    fun share(context: Context, e: MyExport): Boolean = runCatching {
        val send = Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, e.uri).apply {
            type = e.mime ?: when (e.kind) { MyExport.Kind.Sound -> "audio/*"; MyExport.Kind.Video -> "video/mp4"; MyExport.Kind.Project -> "application/zip" }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, tr("Partager le morceau")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess

    /** Where these files are on the device, for the list's first line and its « ? ». */
    fun where(): String =
        tr("Les sons sont dans Musique › BenStudio, les vidéos dans Films › BenStudio, les .zip dans Documents › BenStudio, sur cet appareil. Les plus récents d'abord.")
}

/**
 * « Mes fichiers » as a line that opens the list ([tag]). Chantier 1 of the hands-on audit (G2, recette tel-03: « ressemble
 * à une simple phrase »): drawn as the lines beside it that open something (a title, what it is for under it, the arrow),
 * with a folder before it; the shared `LinkRow` of the next lot takes its place.
 */
@Composable
fun MyExportsButton(tag: String, modifier: Modifier = Modifier, /** Its name alone (little room); what it is for stays in its « ? ». */ compact: Boolean = false) {
    var open by rememberSaveable { mutableStateOf(false) }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { open = true }, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag(tag)) {
            Icon(Icons.Filled.Folder, contentDescription = null, tint = Neon.Cyan, modifier = Modifier.padding(end = 12.dp).size(22.dp))
            Column(Modifier.weight(1f)) {
                Text(tr("Mes fichiers"), color = Neon.Cyan)
                if (!compact) Text(tr("Retrouver et renvoyer : M4A, WAV, vidéo, .zip"), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Neon.Cyan)
        }
        // Round 3 integration: where these files are, in its « ? ».
        com.pocketstudio.ui.HelpDot(
            com.pocketstudio.ui.Help(tr("Mes fichiers"), tr("Retrouver et renvoyer : M4A, WAV, vidéo, .zip") + "\n" + MyExports.where()), about = listOf(tag),
        )
    }
    if (open) MyExportsDialog { open = false }
}

/** The list of [MyExports], each with « Renvoyer ». [shown]: the files to show (previews and captures); null: read from the device. */
@Composable
fun MyExportsDialog(shown: List<MyExport>? = null, close: () -> Unit) {
    val context = LocalContext.current
    var items by androidx.compose.runtime.remember { mutableStateOf(shown) }
    LaunchedEffect(Unit) { if (shown == null) items = withContext(Dispatchers.IO) { MyExports.list(context) } }
    ScaledAlertDialog(
        onDismissRequest = close,
        title = { Text(tr("Mes fichiers")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("my_exports")) {
                val list = items
                when {
                    // Being read: where the files are, meanwhile (never an empty window).
                    list == null -> Text(MyExports.where(), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                    // G2: never « aucun export… Partager en crée un » to someone who has just shared.
                    list.isEmpty() -> Text(tr("Rien ici pour l'instant. Tout ce que tu partages s'ajoute ici, dans Musique › BenStudio sur cet appareil."), modifier = Modifier.testTag("my_exports_empty"))
                    else -> {
                        Text(MyExports.where(), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                        MyExportsList(list) { MyExports.share(context, it) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = close, modifier = Modifier.testTag("my_exports_close")) { Text(tr("Fermer")) } },
    )
}

/** The files, newest first: the name whole (two lines at most), its date and size, what it is when not a sound, « Renvoyer ». */
@Composable
internal fun MyExportsList(list: List<MyExport>, send: (MyExport) -> Unit) {
    LazyColumn(Modifier.heightIn(max = 360.dp)) {
        items(list, key = { it.uri.toString() }) { e ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("my_export")) {
                Column(Modifier.weight(1f)) {
                    Text(e.name, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    val kind = when (e.kind) { MyExport.Kind.Sound -> null; MyExport.Kind.Video -> tr("vidéo"); MyExport.Kind.Project -> tr("projet") }
                    Text(listOfNotNull(kind, MyExports.detail(e)).joinToString(" · "), style = MaterialTheme.typography.bodySmall,
                        color = Neon.TextSecondary, modifier = Modifier.testTag("my_export_detail"))
                }
                TextButton(onClick = { send(e) }, modifier = Modifier.heightIn(min = 48.dp).testTag("my_export_send")) { Text(tr("Renvoyer")) }
            }
        }
    }
}
