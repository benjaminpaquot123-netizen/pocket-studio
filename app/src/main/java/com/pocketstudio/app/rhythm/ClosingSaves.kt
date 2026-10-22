package com.pocketstudio.app.rhythm

import com.pocketstudio.app.report.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicInteger

/**
 * The last saves of a screen that goes away (its ViewModel cleared: the app swiped away, another
 * project opened): they run on their own scope, never the screen's (cancelled by then), so the
 * changes of the last moments reach the disk. The next screen waits for them before it reads the
 * project ([awaitIdleBlocking]).
 */
object ClosingSaves {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val busy = AtomicInteger(0)

    /** Runs [save] (at most [timeoutMs]) away from any screen. */
    fun launch(timeoutMs: Long = 10_000, save: suspend () -> Unit) {
        busy.incrementAndGet()
        scope.launch {
            try {
                withTimeoutOrNull(timeoutMs) { save() } ?: AppLog.add("Sauvegarde à la fermeture : trop longue")
            } catch (e: Exception) {
                AppLog.add("Sauvegarde à la fermeture : ${e.message}")
            } finally {
                busy.decrementAndGet()
            }
        }
    }

    /** Waits (at most [timeoutMs]) for the saves launched to end. */
    fun awaitIdleBlocking(timeoutMs: Long) {
        if (busy.get() == 0) return
        runBlocking { awaitIdle(timeoutMs) }
    }

    /** Waits (at most [timeoutMs]) for the saves launched to end; false when some still run. */
    suspend fun awaitIdle(timeoutMs: Long): Boolean =
        busy.get() == 0 || withTimeoutOrNull(timeoutMs) { while (busy.get() > 0) delay(10) } != null
}
