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

    /** Strips anything that looks like a URL query or an e-mail before it leaves the phone. */
    fun redact(text: String): String = text
        .replace(Regex("[\\w.+-]+@[\\w-]+\\.[\\w.]+"), "<email>")
        .replace(Regex("\\?[^\\s]+"), "?…")
}
