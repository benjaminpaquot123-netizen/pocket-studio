package com.pocketstudio.app.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.takeWhole

/** Where exported files go and how they leave the app. */
object WavExporter {
    const val MUSIC_FOLDER = "Music/BenStudio"

    /**
     * A fresh file in the private export folder; exports older than an hour are removed (they live
     * on in Music), and so is any `.premaster` a mastered export left when the process died.
     */
    fun newFile(context: Context, projectName: String): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val cutoff = System.currentTimeMillis() - 3_600_000
        dir.listFiles()?.filter { it.lastModified() < cutoff || it.name.endsWith(".premaster") }?.forEach { it.delete() }
        return File(dir, fileName(projectName, LocalDateTime.now()))
    }

    /**
     * The file's name: the project's, its letters kept in every script (ó, ę, テクノ, हिंदी's vowel
     * signs: audit ultra round 1, n° 17), « & » kept, the rest dropped, one « - » between words.
     */
    fun fileName(projectName: String, time: LocalDateTime): String {
        val safe = projectName.trim()
            .replace(Regex("[^\\p{L}\\p{M}\\p{N}&_\\s-]"), "")
            .replace(Regex("\\s+"), "-")
            .replace(Regex("-{2,}"), "-")
            .trim('-')
            .takeWhole(40)
            .ifEmpty { "pocket-studio" }
        return "$safe-${time.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))}.wav"
    }

    /**
     * « Ma chanson » with the words after it (« morceau », « club »…), each left out when the name
     * already holds it (audit ultra round 1, B b: « Mon-morceau-morceau »).
     */
    fun nameWith(name: String, vararg words: String?): String {
        val have = name.lowercase().split(Regex("[^\\p{L}\\p{M}\\p{N}]+")).toMutableSet()
        val extra = words.filterNotNull().filter { it.isNotBlank() }.filter { w ->
            val parts = w.lowercase().split(Regex("[^\\p{L}\\p{M}\\p{N}]+")).filter { it.isNotEmpty() }
            (parts.isEmpty() || !have.containsAll(parts)).also { have += parts }
        }
        return (listOf(name) + extra).joinToString("-")
    }

    /** Where a text report goes (Musique only takes sound: MediaStore refuses text/plain there). */
    const val DOCUMENTS_FOLDER = "Documents/BenStudio"

    /**
     * Copies [file] into the shared Music folder (no permission needed). Blocking: call off the main thread.
     * Audit ultra round 2 (n° 10): the media store checks the MIME type against its own table (« audio/wav »
     * is « audio/x-wav » there): the platform's type for the file's extension is tried first, then [mime].
     */
    fun copyToMusic(context: Context, file: File, mime: String = "audio/wav"): Boolean =
        mimesFor(file, mime).any { copyInto(context, file, it, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MUSIC_FOLDER) }

    /** Android's collection for [DOCUMENTS_FOLDER] (read back by « Mes fichiers »). */
    val DOCUMENTS_COLLECTION: android.net.Uri get() = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

    /** A text file (an export's report) or a project's .zip into Documents › BenStudio. Blocking. */
    fun copyToDocuments(context: Context, file: File, mime: String = "text/plain"): Boolean =
        mimesFor(file, mime).any { copyInto(context, file, it, DOCUMENTS_COLLECTION, DOCUMENTS_FOLDER) }

    private fun mimesFor(file: File, mime: String): List<String> =
        listOfNotNull(android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase()), mime).distinct()

    private fun copyInto(context: Context, file: File, mime: String, collection: android.net.Uri, folder: String): Boolean = runCatching {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, folder)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values) ?: return false
        try {
            resolver.openOutputStream(uri)!!.use { out -> file.inputStream().use { it.copyTo(out) } }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }.getOrDefault(false)

    fun share(context: Context, file: File) = share(context, listOf(file))

    /**
     * One file or several (the tracks of a stems export) through Android's share menu, with a mail's
     * [subject] and [text] if given. False when
     * it could not open (no file, no app to share with).
     */
    fun share(context: Context, files: List<File>, title: String = tr("Partager le morceau"), subject: String? = null, text: String? = null): Boolean {
        if (files.isEmpty()) return false
        // A file outside the shared folders throws here: « could not open », never a crash.
        val uris = runCatching { ArrayList(files.map { FileProvider.getUriForFile(context, context.packageName + ".files", it) }) }.getOrNull() ?: return false
        val mime = when {
            files.all { it.extension == "m4a" } -> "audio/mp4"
            files.all { it.extension == "flac" } -> "audio/flac"
            files.all { it.extension == "wav" } -> "audio/wav"
            files.all { it.extension == "mid" } -> "audio/midi"
            files.all { it.extension == "zip" } -> "application/zip"
            files.all { it.extension == "mp4" } -> "video/mp4"
            else -> "*/*"
        }
        val send = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris[0])
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        }.apply {
            type = mime
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
            text?.let { putExtra(Intent.EXTRA_TEXT, it) }
            // The file's own name in Android's preview (tour 7 lot 5 D1); the menu's title stays the app's words.
            files.singleOrNull()?.let { putExtra(Intent.EXTRA_TITLE, it.name) }
        }
        return runCatching { context.startActivity(ShareSent.opening().let { Intent.createChooser(send, title, ShareSent.sender(context)) }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
    }
}

/** Android's share menu title for [names]: the file's own name when one file leaves (tour 7 lot 5 D1). */
internal fun chooserTitle(title: String, names: List<String>): String = names.singleOrNull()?.let { "$title · $it" } ?: title

/**
 * Tour 7 lot 5 D2: whether an app was picked in the last share menu opened by [WavExporter.share]. Android tells it
 * through the chooser's [android.content.IntentSender]; no word from it: nothing was sent (back with no app chosen).
 */
object ShareSent {
    private val action get() = "com.pocketstudio.app.SHARE_PICKED"
    private val _picked = kotlinx.coroutines.flow.MutableStateFlow(false)
    val picked: kotlinx.coroutines.flow.StateFlow<Boolean> = _picked
    @Volatile private var listening = false

    /** A new menu opens: nothing picked yet. */
    fun opening() { _picked.value = false }

    /** Android's word: an app was picked. */
    fun picked() { _picked.value = true }

    /** What Android calls back once an app is picked; null where it cannot be made (the menu opens all the same). */
    fun sender(context: Context): android.content.IntentSender? = runCatching {
        val app = context.applicationContext
        if (!listening) {
            androidx.core.content.ContextCompat.registerReceiver(app, object : android.content.BroadcastReceiver() {
                override fun onReceive(c: Context?, i: Intent?) = picked()
            }, android.content.IntentFilter(action), androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
            listening = true
        }
        val flags = android.app.PendingIntent.FLAG_UPDATE_CURRENT or
            (if (android.os.Build.VERSION.SDK_INT >= 31) android.app.PendingIntent.FLAG_MUTABLE else 0)
        android.app.PendingIntent.getBroadcast(app, 0, Intent(action).setPackage(app.packageName), flags).intentSender
    }.getOrNull()
}
