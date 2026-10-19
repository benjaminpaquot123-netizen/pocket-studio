package com.pocketstudio.app.report

import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * The app's own journal, joined to checklist and bug reports. Logcat is not enough:
 * MagicOS (Honor) encrypts it, so reports only showed an unreadable HKS…HKE block.
 * Keeps the last [CAPACITY] lines in memory. Never call from the audio thread.
 */
object AppLog {
    const val CAPACITY = 200
    private val lines = ArrayDeque<String>()
    private val time = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

    fun add(message: String) {
        val line = "${LocalTime.now().format(time)} ${LogTail.redact(message)}"
        synchronized(lines) {
            if (lines.size == CAPACITY) lines.removeFirst()
            lines.addLast(line)
        }
    }

    fun dump(maxLines: Int = 80): String = synchronized(lines) { lines.takeLast(maxLines).joinToString("\n") }

    internal fun clear() = synchronized(lines) { lines.clear() }
}
