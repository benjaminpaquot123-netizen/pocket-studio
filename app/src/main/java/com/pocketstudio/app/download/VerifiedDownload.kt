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

    /**
     * Blocking (background). [onProgress] 0..1. Throws on network error, cancel or a wrong file.
     * [resume]: what a cut connection already brought stays in `<target>.part` and the next call
     * asks the server for the rest only (HTTP Range); a cancel or a wrong file still starts over.
     */
    fun fetch(
        spec: VerifiedFile, target: File, onProgress: (Float) -> Unit, isCancelled: () -> Boolean = { false }, resume: Boolean = false,
    ): File {
        target.parentFile!!.mkdirs()
        val part = File(target.parentFile, target.name + ".part")
        var have = if (resume && part.isFile && part.length() in 1 until spec.sizeBytes) part.length() else 0L
        if (have == 0L) part.delete()
        val connection = (URL(spec.url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            if (have > 0) setRequestProperty("Range", "bytes=$have-")
        }
        // A network error keeps what came (resume); a refused range or a wrong file does not.
        var keep = resume
        try {
            val code = connection.responseCode
            if (code == 416) keep = false
            if (code == 200) have = 0L // the server sends it all again
            else if (code != 206 || have == 0L) throw IOException("HTTP $code")
            val digest = MessageDigest.getInstance("SHA-256")
            if (have > 0) part.inputStream().use { input ->
                val buffer = ByteArray(256 * 1024)
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    digest.update(buffer, 0, n)
                }
            }
            var done = have
            connection.inputStream.use { input ->
                java.io.FileOutputStream(part, have > 0).use { out ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        if (isCancelled()) {
                            keep = false
                            throw CancelledException()
                        }
                        val n = input.read(buffer)
                        if (n < 0) break
                        out.write(buffer, 0, n)
                        digest.update(buffer, 0, n)
                        done += n
                        if (done > spec.sizeBytes) {
                            keep = false
                            throw IOException(tr("fichier abîmé (empreinte différente)"))
                        }
                        onProgress((done.toFloat() / spec.sizeBytes).coerceAtMost(1f))
                    }
                    out.fd.sync()
                }
            }
            // The connection ended early without an error: what came is kept for the next try.
            if (done < spec.sizeBytes) throw IOException(tr("connexion coupée"))
            keep = false
            if (hex(digest.digest()) != spec.sha256) throw IOException(tr("fichier abîmé (empreinte différente)"))
            if (!part.renameTo(target)) throw IOException(tr("écriture impossible"))
            return target
        } finally {
            connection.disconnect()
            if (!keep) part.delete()
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
