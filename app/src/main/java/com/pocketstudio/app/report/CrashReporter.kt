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
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { crashFile.writeText(format(thread, throwable)) }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun pendingCrash(): String? {
        if (!::crashFile.isInitialized) return null
        if (crashFile.exists()) return crashFile.readText()
        return runCatching { systemExit() }.getOrNull()
    }

    fun clear() {
        if (!::crashFile.isInitialized) return
        crashFile.delete()
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
        // ANR traces are text; native traces are binary tombstones, so only the summary is kept.
        val trace = if (exit.reason == ApplicationExitInfo.REASON_ANR) {
            exit.traceInputStream?.bufferedReader()?.useLines { it.take(120).joinToString("\n") }.orEmpty()
        } else ""
        return "Version ${AppInfo.versionLabel} · $date\n$kind : ${exit.description ?: "sans description"}\n${LogTail.redact(trace)}"
    }

    private fun format(thread: Thread, throwable: Throwable): String {
        val trace = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.FRANCE).format(Date())
        return "Version ${AppInfo.versionLabel} · $date · thread ${thread.name}\n${LogTail.redact(trace)}"
    }
}
