package com.pocketstudio.app.instruments

import android.content.Context
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * The General MIDI bank (GeneralUser GS 2.0.3 by S. Christian Collins, free licence):
 * downloaded once from our own GitHub Release (as the author asks), checked against its
 * SHA-256 before it is ever parsed, kept in the app's files.
 */
object InstrumentLibrary {
    const val URL = "https://github.com/benjaminpaquot123-netizen/pocket-studio/releases/download/sons-generaluser-gs-2.0.3/GeneralUser-GS.sf2"
    const val SHA256 = "9575028c7a1f589f5770fccc8cff2734566af40cd26ed836944e9a5152688cfe"
    const val SIZE_BYTES = 32_319_396L

    fun file(context: Context) = File(File(context.filesDir, "sounds"), "GeneralUser-GS.sf2")

    fun isInstalled(context: Context) = file(context).length() == SIZE_BYTES

    /** Blocking (background). [onProgress] 0..1. Throws on network error or a wrong file. */
    fun download(context: Context, onProgress: (Float) -> Unit, isCancelled: () -> Boolean = { false }): File {
        val target = file(context)
        target.parentFile!!.mkdirs()
        val part = File(target.parentFile, target.name + ".part")
        val connection = (URL(URL).openConnection() as HttpURLConnection).apply {
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
                        if (isCancelled()) throw IOException("annulé")
                        val n = input.read(buffer)
                        if (n < 0) break
                        out.write(buffer, 0, n)
                        digest.update(buffer, 0, n)
                        done += n
                        onProgress((done.toFloat() / SIZE_BYTES).coerceAtMost(1f))
                    }
                    out.fd.sync()
                }
            }
            if (hex(digest.digest()) != SHA256) throw IOException("fichier abîmé (empreinte différente)")
            if (!part.renameTo(target)) throw IOException("rename failed")
            return target
        } finally {
            connection.disconnect()
            part.delete()
        }
    }

    /** Re-checks an installed file (once per launch, before parsing it). */
    fun verify(context: Context): Boolean {
        val f = file(context)
        if (f.length() != SIZE_BYTES) return false
        val digest = MessageDigest.getInstance("SHA-256")
        f.inputStream().use { input ->
            val buffer = ByteArray(256 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        return hex(digest.digest()) == SHA256
    }

    private fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it) }

    /** The 16 General MIDI families (8 programs each), in French. */
    val FAMILIES = listOf(
        "Pianos", "Percussions mélodiques", "Orgues", "Guitares", "Basses", "Cordes solo", "Ensembles", "Cuivres",
        "Anches", "Flûtes", "Synthé lead", "Synthé nappe", "Effets synthé", "Ethnique", "Percussions", "Bruitages",
    )
}
