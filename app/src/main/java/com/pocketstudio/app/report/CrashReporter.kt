package com.pocketstudio.app.report

import android.content.Context
import com.pocketstudio.app.AppInfo
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Writes uncaught exceptions to disk so the next launch can offer to report them.
 */
object CrashReporter {
    private const val FILE_NAME = "last_crash.txt"
    private lateinit var crashFile: File

    fun install(context: Context) {
        crashFile = File(context.filesDir, FILE_NAME)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { crashFile.writeText(format(thread, throwable)) }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun pendingCrash(): String? =
        if (::crashFile.isInitialized && crashFile.exists()) crashFile.readText() else null

    fun clear() {
        if (::crashFile.isInitialized) crashFile.delete()
    }

    private fun format(thread: Thread, throwable: Throwable): String {
        val trace = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.FRANCE).format(Date())
        return "Version ${AppInfo.versionLabel} · $date · thread ${thread.name}\n${LogTail.redact(trace)}"
    }
}
