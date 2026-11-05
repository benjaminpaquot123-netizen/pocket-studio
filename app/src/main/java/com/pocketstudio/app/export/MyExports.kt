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
 * Audit ultra round 3 (n° 11, seat A d 45): « Mes exports », the files the app copied into Musique › BenStudio
 * (the app sees its own copies there without any permission), newest first, each to send again. Before, the
 * only trace of an export was the card's « Sauvegardé dans… » line, gone with the next export.
 */
data class MyExport(val name: String, val uri: Uri, val bytes: Long, val addedSeconds: Long, val mime: String?)

object MyExports {
    /** Every sound the app put in Musique › BenStudio, newest first. Blocking: off the main thread. */
    fun list(context: Context): List<MyExport> = runCatching {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val columns = arrayOf(
            MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_ADDED, MediaStore.MediaColumns.MIME_TYPE,
        )
        context.contentResolver.query(
            collection, columns, "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?", arrayOf(WavExporter.MUSIC_FOLDER + "%"),
            "${MediaStore.MediaColumns.DATE_ADDED} DESC",
        )?.use { c ->
            buildList {
                while (c.moveToNext()) {
                    val id = c.getLong(0)
                    add(MyExport(c.getString(1) ?: "", android.content.ContentUris.withAppendedId(collection, id), c.getLong(2), c.getLong(3), c.getString(4)))
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
            type = e.mime ?: "audio/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, tr("Partager le morceau")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess
}

/** « Mes exports » as a link ([tag]) that opens the list. */
@Composable
fun MyExportsButton(tag: String, modifier: Modifier = Modifier) {
    var open by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick = { open = true }, modifier = modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(tag)) {
        Text(tr("Mes exports : retrouver et renvoyer un fichier"), color = Neon.Cyan, modifier = Modifier.weight(1f))
    }
    if (open) MyExportsDialog { open = false }
}

/** The list of [MyExports], each with « Envoyer ». */
@Composable
fun MyExportsDialog(close: () -> Unit) {
    val context = LocalContext.current
    var items by androidx.compose.runtime.remember { mutableStateOf<List<MyExport>?>(null) }
    LaunchedEffect(Unit) { items = withContext(Dispatchers.IO) { MyExports.list(context) } }
    ScaledAlertDialog(
        onDismissRequest = close,
        title = { Text(tr("Mes exports")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("my_exports")) {
                Text(tr("Dans Musique › BenStudio sur ce téléphone. Les plus récents d'abord."), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                val list = items
                when {
                    list == null -> Unit
                    list.isEmpty() -> Text(tr("Aucun export pour l'instant : « Partager » ou « Exporter » en crée un."), modifier = Modifier.testTag("my_exports_empty"))
                    else -> LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        items(list, key = { it.uri.toString() }) { e ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().testTag("my_export")) {
                                Column(Modifier.weight(1f)) {
                                    Text(e.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(MyExports.detail(e), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                                }
                                TextButton(onClick = { MyExports.share(context, e) }, modifier = Modifier.heightIn(min = 48.dp)) { Text(tr("Envoyer")) }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = close, modifier = Modifier.testTag("my_exports_close")) { Text(tr("Fermer")) } },
    )
}
