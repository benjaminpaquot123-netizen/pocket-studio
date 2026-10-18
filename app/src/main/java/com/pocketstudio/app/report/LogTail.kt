package com.pocketstudio.app.report

import android.os.Process

/** Reads the end of this process' own logcat (no special permission needed). */
object LogTail {
    fun read(maxLines: Int = 60): String = runCatching {
        val process = ProcessBuilder(
            "logcat", "-d", "-t", maxLines.toString(), "--pid=${Process.myPid()}", "-v", "time",
        ).redirectErrorStream(true).start()
        val text = process.inputStream.bufferedReader().use { it.readText() }
        process.waitFor()
        redact(text.trim())
    }.getOrDefault("(journal indisponible)")

    /** Strips anything that looks like a URL query or an e-mail before it leaves the phone. */
    fun redact(text: String): String = text
        .replace(Regex("[\\w.+-]+@[\\w-]+\\.[\\w.]+"), "<email>")
        .replace(Regex("\\?[^\\s]+"), "?…")
}
