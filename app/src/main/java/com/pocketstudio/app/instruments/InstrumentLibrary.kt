package com.pocketstudio.app.instruments

import android.content.Context
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** A downloadable SoundFont bank, hosted on our own GitHub Release and pinned by SHA-256. */
data class Bank(
    val id: String,
    val name: String,
    val description: String,
    val tag: String,
    val fileName: String,
    val sha256: String,
    val sizeBytes: Long,
) {
    val url get() = "https://github.com/benjaminpaquot123-netizen/pocket-studio/releases/download/$tag/$fileName"
    val sizeLabel get() = "${sizeBytes / 1_000_000} Mo"
}

/**
 * The instrument banks (General MIDI, free licences): each is downloaded once from our own
 * GitHub Release, checked against its SHA-256 before it is ever parsed, and kept in the app's files.
 */
object InstrumentLibrary {
    val BANKS = listOf(
        Bank(
            "generaluser", "GeneralUser GS", "Léger et équilibré : 287 sons, 13 kits de batterie.",
            "sons-generaluser-gs-2.0.3", "GeneralUser-GS.sf2",
            "9575028c7a1f589f5770fccc8cff2734566af40cd26ed836944e9a5152688cfe", 32_319_396L,
        ),
        Bank(
            "fluidr3", "FluidR3 GM", "Le classique : 189 sons, 31 kits de batterie.",
            "sons-fluidr3-gm-3.1", "FluidR3_GM.sf2",
            "74594e8f4250680adf590507a306655a299935343583256f3b722c48a1bc1cb0", 148_398_306L,
        ),
        Bank(
            "musescore", "MuseScore General", "Le plus riche : 309 sons, 37 kits de batterie.",
            "sons-musescore-general-0.2", "MuseScore_General.sf2",
            "ee51d2c4b1525e70f19a45909c4fd7a2e26d91d115fa89dbf5a6bc413d8b9bf3", 215_614_036L,
        ),
    )

    fun bank(id: String) = BANKS.firstOrNull { it.id == id } ?: BANKS[0]

    fun file(context: Context, bank: Bank) = File(File(context.filesDir, "sounds"), bank.fileName)

    fun isInstalled(context: Context, bank: Bank) = file(context, bank).length() == bank.sizeBytes

    /** Blocking (background). [onProgress] 0..1. Throws on network error or a wrong file. */
    fun download(context: Context, bank: Bank, onProgress: (Float) -> Unit, isCancelled: () -> Boolean = { false }): File {
        val target = file(context, bank)
        target.parentFile!!.mkdirs()
        val part = File(target.parentFile, target.name + ".part")
        val connection = (URL(bank.url).openConnection() as HttpURLConnection).apply {
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
                        onProgress((done.toFloat() / bank.sizeBytes).coerceAtMost(1f))
                    }
                    out.fd.sync()
                }
            }
            if (hex(digest.digest()) != bank.sha256) throw IOException("fichier abîmé (empreinte différente)")
            if (!part.renameTo(target)) throw IOException("rename failed")
            return target
        } finally {
            connection.disconnect()
            part.delete()
        }
    }

    /** Re-checks an installed file before parsing it. */
    fun verify(context: Context, bank: Bank): Boolean {
        val f = file(context, bank)
        if (f.length() != bank.sizeBytes) return false
        val digest = MessageDigest.getInstance("SHA-256")
        f.inputStream().use { input ->
            val buffer = ByteArray(256 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        return hex(digest.digest()) == bank.sha256
    }

    private fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it) }

    /** The 16 General MIDI families (8 programs each), in French. */
    val FAMILIES = listOf(
        "Pianos", "Percussions mélodiques", "Orgues", "Guitares", "Basses", "Cordes solo", "Ensembles", "Cuivres",
        "Anches", "Flûtes", "Synthé lead", "Synthé nappe", "Effets synthé", "Ethnique", "Percussions", "Bruitages",
    )

    /** General MIDI drum notes for our 16 pads, in pad order (DrumKitInfo.names). */
    val KIT_NOTES = listOf(36, 35, 38, 40, 39, 42, 46, 41, 45, 48, 37, 56, 49, 70, 63, 54)
    val KIT_PAD_NAMES = listOf(
        "Kick", "Kick 2", "Caisse", "Caisse 2", "Clap", "Charley", "Charley ouvert", "Tom grave",
        "Tom médium", "Tom aigu", "Rim", "Cloche", "Crash", "Maracas", "Conga", "Tambourin",
    )
}
