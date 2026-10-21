package com.pocketstudio.app.download

import com.pocketstudio.model.i18n.tr
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** A big file hosted on a release, checked by size and SHA-256 before anything parses it. */
data class VerifiedFile(val url: String, val fileName: String, val sha256: String, val sizeBytes: Long) {
    val sizeLabel: String get() = tr("%d Mo", (sizeBytes + 500_000) / 1_000_000)
}

object VerifiedDownload {
    class CancelledException : IOException(tr("annulé"))

    /** Blocking (background). [onProgress] 0..1. Throws on network error, cancel or a wrong file. */
    fun fetch(spec: VerifiedFile, target: File, onProgress: (Float) -> Unit, isCancelled: () -> Boolean = { false }): File {
        target.parentFile!!.mkdirs()
        val part = File(target.parentFile, target.name + ".part")
        val connection = (URL(spec.url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
        }
        try {
            if (connection.responseCode != 200) throw IOException("HTTP ${connection.responseCode}")
            val digest = MessageDigest.getInstance("SHA-256")
            var done = 0L
            connection.inputStream.use { input ->
                part.outputStream().use { out ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        if (isCancelled()) throw CancelledException()
                        val n = input.read(buffer)
                        if (n < 0) break
                        out.write(buffer, 0, n)
                        digest.update(buffer, 0, n)
                        done += n
                        onProgress((done.toFloat() / spec.sizeBytes).coerceAtMost(1f))
                    }
                    out.fd.sync()
                }
            }
            if (hex(digest.digest()) != spec.sha256) throw IOException(tr("fichier abîmé (empreinte différente)"))
            if (!part.renameTo(target)) throw IOException(tr("écriture impossible"))
            return target
        } finally {
            connection.disconnect()
            part.delete()
        }
    }

    /** Re-checks an installed file before parsing it. */
    fun verify(spec: VerifiedFile, file: File): Boolean {
        if (file.length() != spec.sizeBytes) return false
        return sha256(file) == spec.sha256
    }

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(256 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        return hex(digest.digest())
    }

    fun sha256Of(text: String): String = hex(MessageDigest.getInstance("SHA-256").digest(text.toByteArray()))

    private fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it) }
}
