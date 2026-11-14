package com.pocketstudio.app.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
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
 * à une simple phrase »), chantier 2 (G3): the shared [com.pocketstudio.ui.LinkRow], a folder before it, the chevron after.
 */
@Composable
fun MyExportsButton(tag: String, modifier: Modifier = Modifier, /** Its name alone (little room); what it is for stays in its « ? ». */ compact: Boolean = false) {
    var open by rememberSaveable { mutableStateOf(false) }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        com.pocketstudio.ui.LinkRow(
            tr("Mes fichiers"), onClick = { open = true }, modifier = Modifier.weight(1f).testTag(tag), icon = Icons.Filled.Folder,
            subtitle = if (compact) null else tr("Retrouver et renvoyer : M4A, WAV, vidéo, .zip"),
        )
        // Round 3 integration: where these files are, in its « ? ».
        com.pocketstudio.ui.HelpDot(
            com.pocketstudio.ui.Help(tr("Mes fichiers"), tr("Retrouver et renvoyer : M4A, WAV, vidéo, .zip") + "\n" + MyExports.where()), about = listOf(tag),
        )
    }
    if (open) MyExportsDialog { open = false }
}

/**
 * The list of [MyExports], each with « Renvoyer ». [shown]: the files to show (previews and captures); null: [read] from
 * the device (off the main thread), a sign of waiting meanwhile.
 */
@Composable
fun MyExportsDialog(
    shown: List<MyExport>? = null,
    read: suspend (Context) -> List<MyExport> = { c -> withContext(Dispatchers.IO) { MyExports.list(c) } },
    close: () -> Unit,
) {
    val context = LocalContext.current
    var items by remember { mutableStateOf(shown) }
    // Lot coquille3 (control seat Cq3, portal of batch 8): the list read off the main thread is written on the main thread.
    // Where the effect resumes on the reading thread (seen in the tests' clock), a write from there never reached the
    // screen: « Chargement de la liste… » for good.
    LaunchedEffect(Unit) { if (shown == null) { val read = read(context); withContext(Dispatchers.Main) { items = read } } }
    ScaledAlertDialog(
        onDismissRequest = close,
        title = { Text(tr("Mes fichiers")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("my_exports")) {
                val list = items
                when {
                    // Being read: a sign of waiting and where the files are, meanwhile (never an empty window). Tour 6, U22
                    // (seat A c #27): the place alone read as « nothing, not even "no file" ».
                    list == null -> {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.testTag("my_exports_wait")) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Neon.Cyan)
                            Text(tr("Chargement de la liste…"), style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary,
                                modifier = Modifier.padding(start = 8.dp))
                        }
                        Text(MyExports.where(), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                    }
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

/** The files, newest first: the name (two lines at most, its extension in sight), its date and size, what it is when not a sound, « Renvoyer ». */
@Composable
internal fun MyExportsList(list: List<MyExport>, send: (MyExport) -> Unit) {
    // Tour 6, R14: the name's room beside « Renvoyer »: 16 figures of its font (a name's word or two a line); less (a long
    // word for « Renvoyer », the phone's font at 200 %), « Renvoyer » goes under the name and the name gets the whole width.
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.bodyMedium
    val nameRoom = remember(measurer, style) { measurer.measure("0".repeat(16), style, softWrap = false, maxLines = 1).size.width }
    LazyColumn(Modifier.heightIn(max = 360.dp)) {
        items(list, key = { it.uri.toString() }) { e ->
            FileRow(
                nameRoom,
                name = {
                    Column {
                        FileName(e.name, style = style)
                        val kind = when (e.kind) { MyExport.Kind.Sound -> null; MyExport.Kind.Video -> tr("vidéo"); MyExport.Kind.Project -> tr("projet") }
                        Text(listOfNotNull(kind, MyExports.detail(e)).joinToString(" · "), style = MaterialTheme.typography.bodySmall,
                            color = Neon.TextSecondary, modifier = Modifier.testTag("my_export_detail"))
                    }
                },
                send = { TextButton(onClick = { send(e) }, modifier = Modifier.heightIn(min = 48.dp).testTag("my_export_send")) { Text(tr("Renvoyer")) } },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("my_export"),
            )
        }
    }
}

/** [name] beside [send] (centred) when [nameRoom] pixels are left to it, else above it, [send] at the end. */
@Composable
private fun FileRow(nameRoom: Int, name: @Composable () -> Unit, send: @Composable () -> Unit, modifier: Modifier = Modifier) {
    Layout(contents = listOf(name, send), modifier = modifier) { (names, sends), constraints ->
        val width = constraints.maxWidth
        val button = sends.first().measure(constraints.copy(minWidth = 0, minHeight = 0))
        if (width - button.width >= nameRoom) {
            val text = names.first().measure(Constraints.fixedWidth(width - button.width))
            val height = maxOf(text.height, button.height)
            layout(width, height) {
                text.placeRelative(0, (height - text.height) / 2)
                button.placeRelative(width - button.width, (height - button.height) / 2)
            }
        } else {
            val text = names.first().measure(Constraints.fixedWidth(width))
            layout(width, text.height + button.height) {
                text.placeRelative(0, 0)
                button.placeRelative(width - button.width, text.height)
            }
        }
    }
}

/**
 * Tour 6, R14 (seats A c #43, E b #16): a file's name on two lines at most, its words whole and its end in sight (.wav,
 * .m4a, .zip, .mp4 say which file it is). Compose broke a name with no spaces anywhere (« pocketstudio.z / ip ») and its
 * closing « … » hid the extension (« …21361… »): the lines are cut here instead, between the name's words (after « - »,
 * « _ » or a space, before a « . »), and when two lines are not enough, the second ends with « … » and the extension
 * (« Jump-up-DnB-bootleg- / Mehdi-morceau-club….wav »). TalkBack reads the whole name.
 */
@Composable
internal fun FileName(name: String, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.bodyMedium) {
    val measurer = rememberTextMeasurer()
    // Each line in the name's own direction (its first letter's), whatever the interface's: a Latin name's second line
    // in Arabic read « zip….20261108 », its first « -Nuit-sur-la-ville ».
    val own = remember(name, style) { style.copy(textDirection = if (startsRightToLeft(name)) TextDirection.Rtl else TextDirection.Ltr) }
    BoxWithConstraints(modifier) {
        val room = constraints.maxWidth
        val shown = remember(name, room, own, measurer) {
            if (!constraints.hasBoundedWidth) name
            else fileNameLines(name) { measurer.measure(it, own, softWrap = false, maxLines = 1).size.width <= room }
        }
        Text(shown, style = own, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { contentDescription = name })
    }
}

/** [text]'s first letter that has a direction is written right to left (Arabic, Hebrew). */
private fun startsRightToLeft(text: String): Boolean = text.firstNotNullOfOrNull { c ->
    when (Character.getDirectionality(c)) {
        Character.DIRECTIONALITY_LEFT_TO_RIGHT -> false
        Character.DIRECTIONALITY_RIGHT_TO_LEFT, Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC -> true
        else -> null
    }
} ?: false

/**
 * [name] on two lines at most, each one [fits]: whole on one line, else broken at its last break that fits (after « - »,
 * « _ » or a space, before a « . »; between letters only when no break fits), else its second line shortened: its start,
 * « … » and the extension.
 */
internal fun fileNameLines(name: String, fits: (String) -> Boolean): String {
    if (name.isEmpty() || fits(name)) return name
    // The longest start of [s] that fits once [then] is added: up to one of its breaks, else as many letters as fit.
    fun start(s: String, then: String): String {
        val breaks = (1 until s.length).filter { s[it - 1] in BREAK_AFTER || s[it] == '.' }
        breaks.lastOrNull { fits(s.substring(0, it).trimEnd() + then) }?.let { return s.substring(0, it).trimEnd() }
        var lo = 0
        var hi = s.length
        while (lo < hi) { val mid = (lo + hi + 1) / 2; if (fits(s.substring(0, mid) + then)) lo = mid else hi = mid - 1 }
        return s.substring(0, lo)
    }
    val first = start(name, "").ifEmpty { name.take(1) }
    val rest = name.substring(first.length).trimStart()
    if (fits(rest)) return "$first\n$rest"
    val dot = rest.lastIndexOf('.')
    val extension = if (dot > 0 && rest.length - dot in 2..6) rest.substring(dot) else ""
    val tail = "…$extension"
    return "$first\n" + start(rest.dropLast(extension.length), tail).trimEnd(*BREAK_AFTER.toCharArray()) + tail
}

private const val BREAK_AFTER = "-_ "
