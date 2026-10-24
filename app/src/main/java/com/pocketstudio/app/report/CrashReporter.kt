package com.pocketstudio.app.report

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import com.pocketstudio.app.AppInfo
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Remembers how the previous session ended so the next launch can offer a report:
 * Kotlin exceptions are written to disk, native crashes (C++ engine) and freezes (ANR)
 * come from the system's exit history.
 */
object CrashReporter {
    private const val FILE_NAME = "last_crash.txt"
    private const val KEY_SEEN = "exit_seen_at"
    private lateinit var appContext: Context
    private lateinit var crashFile: File

    fun install(context: Context) {
        appContext = context.applicationContext
        crashFile = File(context.filesDir, FILE_NAME)
        AppLog.persistTo(context.filesDir)
        val filesDir = context.filesDir
        JournalFiles.startSession(filesDir)
        // Installed once per process: a second Application (Robolectric gives each test its own)
        // only points the handler at its files. The handler keeps no Context: a chain of handlers
        // each holding an Application would keep every test's app in memory.
        crashDir = filesDir
        val current = Thread.getDefaultUncaughtExceptionHandler()
        if (current === handler) return
        previous = current
        Thread.setDefaultUncaughtExceptionHandler(handler)
    }

    @Volatile private var crashDir: File? = null
    @Volatile private var previous: Thread.UncaughtExceptionHandler? = null
    private val handler = Thread.UncaughtExceptionHandler { thread, throwable ->
        runCatching { crashFile.writeText(format(thread, throwable)) }
        // The bug report offered at the next launch: what led there, with the stack.
        crashDir?.let { dir -> runCatching { JournalFiles.saveCrash(dir, thread.name, throwable) } }
        previous?.uncaughtException(thread, throwable)
    }

    fun pendingCrash(): String? {
        if (!::crashFile.isInitialized) return null
        if (crashFile.exists()) return crashFile.readText()
        return runCatching { systemExit() }.getOrNull()
    }

    /** The journal of the session that crashed (journal.json), null if none was kept. */
    fun crashJournal(): String? = if (!::appContext.isInitialized) null else JournalFiles.crashJournal(appContext.filesDir)

    fun clear() {
        if (!::crashFile.isInitialized) return
        crashFile.delete()
        JournalFiles.clearCrash(appContext.filesDir)
        prefs().edit().putLong(KEY_SEEN, System.currentTimeMillis()).apply()
    }

    private fun prefs() = appContext.getSharedPreferences("crash", Context.MODE_PRIVATE)

    private fun systemExit(): String? {
        val prefs = prefs()
        val seen = prefs.getLong(KEY_SEEN, -1L)
        if (seen < 0) {
            // First launch with this feature: older exits are not ours to report.
            prefs.edit().putLong(KEY_SEEN, System.currentTimeMillis()).apply()
            return null
        }
        val am = appContext.getSystemService(ActivityManager::class.java)
        val exit = am.getHistoricalProcessExitReasons(appContext.packageName, 0, 5)
            .firstOrNull {
                it.timestamp > seen &&
                    (it.reason == ApplicationExitInfo.REASON_CRASH_NATIVE || it.reason == ApplicationExitInfo.REASON_ANR)
            } ?: return null
        val kind = if (exit.reason == ApplicationExitInfo.REASON_ANR) "Blocage (ANR)" else "Plantage natif (moteur audio)"
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.FRANCE).format(Date(exit.timestamp))
        // ANR traces are text; native ones are binary tombstones (protobuf): keep their readable
        // strings (signal, abort message, libraries and functions of the crashing stack).
        val trace = if (exit.reason == ApplicationExitInfo.REASON_ANR) {
            exit.traceInputStream?.bufferedReader()?.useLines { it.take(120).joinToString("\n") }.orEmpty()
        } else {
            runCatching { exit.traceInputStream?.use { tombstoneStrings(it.readBytes()) } }.getOrNull().orEmpty()
        }
        val memory = "Mémoire : pss ${exit.pss / 1024} Mo, rss ${exit.rss / 1024} Mo"
        val log = AppLog.previous(appContext.filesDir)
        return "Version ${AppInfo.versionLabel} · $date\n$kind : ${exit.description ?: "sans description"}\n$memory\n" +
            "--- Trace\n${LogTail.redact(trace)}\n--- Journal de la session\n${LogTail.redact(log)}"
    }

    private fun format(thread: Thread, throwable: Throwable): String {
        val trace = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.FRANCE).format(Date())
        return "Version ${AppInfo.versionLabel} · $date · thread ${thread.name}\n${LogTail.redact(trace)}"
    }

    /**
     * The printable strings of a tombstone, in order, without duplicates: enough to see the
     * signal, the abort message and which library / function crashed. Capped in size.
     */
    internal fun tombstoneStrings(bytes: ByteArray, maxLines: Int = 80): String {
        val out = LinkedHashSet<String>()
        val run = StringBuilder()
        fun flush() {
            val s = run.toString().trim()
            if (s.length >= 4 && (s.contains("lib") || s.contains("::") || s.contains("SIG") || s.contains("abort", ignoreCase = true) ||
                    s.contains("Abort") || s.contains("pocketstudio") || s.contains("Exception") || s.contains("signal") ||
                    s.contains("fault") || s.contains("_Z") || s.contains("onnx"))
            ) {
                out += s.take(200)
            }
            run.setLength(0)
        }
        for (b in bytes) {
            val c = b.toInt() and 0xFF
            if (c in 0x20..0x7E) run.append(c.toChar()) else flush()
            if (out.size >= maxLines) break
        }
        flush()
        return out.take(maxLines).joinToString("\n")
    }
}
