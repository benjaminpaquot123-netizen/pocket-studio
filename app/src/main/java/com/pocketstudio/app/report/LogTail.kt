package com.pocketstudio.app.report

import android.os.Process

/** The journal joined to reports: the app's own [AppLog], plus logcat when it is readable. */
object LogTail {
    fun read(maxLines: Int = 60): String {
        val own = AppLog.dump().ifEmpty { "(journal vide)" }
        val system = readLogcat(maxLines)
        // MagicOS returns a single encrypted HKS…HKE block: useless in a report.
        return if (system.contains("(HKS)") || system.isBlank()) own else "$own\n\n--- logcat ---\n$system"
    }

    private fun readLogcat(maxLines: Int): String = runCatching {
        val process = ProcessBuilder(
            "logcat", "-d", "-t", maxLines.toString(), "--pid=${Process.myPid()}", "-v", "time",
        ).redirectErrorStream(true).start()
        val text = process.inputStream.bufferedReader().use { it.readText() }
        process.waitFor()
        redact(text.trim())
    }.getOrDefault("")

    private val email = Regex("[\\w.+-]+@[\\w-]+\\.[\\w.]+")
    private val query = Regex("\\?[^\\s]+")
    /**
     * A file on the phone (its folders and name say whose it is): /storage/…, /data/…, /sdcard/…,
     * /mnt/…, up to the end of the line or a separator (names often hold spaces and apostrophes: « Nuit d'été.wav »).
     * The app's own code (/data/app/…/libpocketstudio.so) stays: a native crash needs it.
     */
    private val path = Regex("/(storage|sdcard|mnt|data(?!/app/))/[^\\n,;:()\\[\\]«»\"]*")
    /** A shared document (content://…): its address can carry the file's name. */
    private val contentUri = Regex("content://[^\\s,;)»\"']+")

    /**
     * Strips what could say who the user is before it leaves the phone: e-mails, URL queries, file
     * paths (as <chemin>) and document addresses (as <document>).
     */
    fun redact(text: String): String = text
        .replace(email, "<email>")
        .replace(contentUri, "<document>")
        .replace(path, "<chemin>")
        .replace(query, "?…")
}
