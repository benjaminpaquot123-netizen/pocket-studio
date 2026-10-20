package com.pocketstudio.app.report

import java.io.File
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
    @Volatile private var file: File? = null

    /**
     * Also writes every line to [dir]/applog.txt, so a native crash (which kills the process
     * with no chance to save) can still be reported with what the app was doing. The previous
     * session's file becomes [previous].
     */
    fun persistTo(dir: File) {
        val current = File(dir, "applog.txt")
        runCatching {
            File(dir, "applog-previous.txt").delete()
            current.renameTo(File(dir, "applog-previous.txt"))
        }
        file = current
    }

    /** Last lines of the previous session (empty if none). */
    fun previous(dir: File, maxLines: Int = 40): String =
        runCatching {
            // Crash reports go to a public issue: song and file names (between « ») stay on the phone.
            File(dir, "applog-previous.txt").readLines().takeLast(maxLines).joinToString("\n").replace(Regex("«[^»]*»"), "«…»")
        }.getOrDefault("")

    fun add(message: String) {
        val line = "${LocalTime.now().format(time)} ${LogTail.redact(message)}"
        synchronized(lines) {
            if (lines.size == CAPACITY) lines.removeFirst()
            lines.addLast(line)
            // Small appends, a few per minute at most; never from the audio thread.
            file?.let { f ->
                runCatching {
                    // Keep the newest lines: halve the file when it grows past 256 KB.
                    if (f.length() > 256_000) f.writeText(f.readText().takeLast(128_000).substringAfter('\n'))
                    f.appendText(line + "\n")
                }
            }
        }
    }

    fun dump(maxLines: Int = 80): String = synchronized(lines) { lines.takeLast(maxLines).joinToString("\n") }

    internal fun clear() = synchronized(lines) { lines.clear() }
}
