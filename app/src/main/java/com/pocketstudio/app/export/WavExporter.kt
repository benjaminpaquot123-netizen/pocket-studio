package com.pocketstudio.app.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Where exported files go and how they leave the app. */
object WavExporter {
    const val MUSIC_FOLDER = "Music/Pocket Studio"

    /** A fresh file in the private export folder; exports older than an hour are removed (they live on in Music). */
    fun newFile(context: Context, projectName: String): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val cutoff = System.currentTimeMillis() - 3_600_000
        dir.listFiles()?.filter { it.lastModified() < cutoff }?.forEach { it.delete() }
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
    fun copyToMusic(context: Context, file: File): Boolean = runCatching {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
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

    fun share(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "audio/wav"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Partager le morceau").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
