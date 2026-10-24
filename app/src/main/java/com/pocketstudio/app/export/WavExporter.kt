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

/** Where exported files go and how they leave the app. */
object WavExporter {
    const val MUSIC_FOLDER = "Music/Pocket Studio"

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

    fun fileName(projectName: String, time: LocalDateTime): String {
        val safe = projectName.trim()
            .replace(Regex("[^\\p{L}\\p{N} _-]"), "")
            .replace(' ', '-')
            .take(40)
            .ifEmpty { "pocket-studio" }
        return "$safe-${time.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))}.wav"
    }

    /** Copies [file] into the shared Music folder (no permission needed). Blocking: call off the main thread. */
    fun copyToMusic(context: Context, file: File, mime: String = "audio/wav"): Boolean = runCatching {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Audio.Media.MIME_TYPE, mime)
            put(MediaStore.Audio.Media.RELATIVE_PATH, MUSIC_FOLDER)
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        try {
            resolver.openOutputStream(uri)!!.use { out -> file.inputStream().use { it.copyTo(out) } }
            values.clear()
            values.put(MediaStore.Audio.Media.IS_PENDING, 0)
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
        }
        return runCatching { context.startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
    }
}
