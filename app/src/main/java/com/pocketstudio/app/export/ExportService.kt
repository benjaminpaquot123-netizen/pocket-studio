package com.pocketstudio.app.export

import android.Manifest
import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.pocketstudio.app.MainActivity
import com.pocketstudio.app.R
import com.pocketstudio.app.report.AppLog
import com.pocketstudio.model.i18n.tr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * The export running now (the ViewModel renders it; [ExportService] only shows it and keeps the
 * app alive meanwhile). Main thread only, apart from reading [state].
 */
object ExportRun {
    /** [title]: what is exported; [stage]: the step shown under it (null: the percentage); [fraction] 0..1. */
    data class Running(val title: String, val fraction: Float = 0f, val stage: String? = null)

    private val _state = MutableStateFlow<Running?>(null)
    val state: StateFlow<Running?> = _state.asStateFlow()

    /** How the last one ended, for the notification when the app is not on screen (null: cancelled). */
    @Volatile
    var ended: String? = null
        private set

    /** « Annuler » in the notification. */
    @Volatile
    private var cancel: (() -> Unit)? = null

    /** An export starts: the service shows it and keeps the app alive while the screen is elsewhere. */
    fun begin(context: Context, title: String, cancel: () -> Unit) {
        this.cancel = cancel
        ended = null
        _state.value = Running(title)
        ExportService.start(context)
    }

    fun progress(fraction: Float, stage: String?) {
        val now = _state.value ?: return
        _state.value = now.copy(fraction = fraction.coerceIn(0f, 1f), stage = stage)
    }

    /** It is over: [message] says how (shown when the app is not on screen), null when cancelled. */
    fun end(message: String?) {
        ended = message
        cancel = null
        _state.value = null
    }

    internal fun cancelNow() = cancel?.invoke()
}

/**
 * Keeps a long export going when the screen goes elsewhere (another app, the screen off): a
 * foreground service with the progress and « Annuler » in a notification. It stops by itself
 * when [ExportRun] ends. Refused by the system: the export still runs, only less safe.
 */
class ExportService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var watching = false
    private var lastPercent = -1
    private var lastStage: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Always honour the foreground promise first (platform call: see SeparationService).
        val running = ExportRun.state.value
        val promised = runCatching { startForeground(NOTIFICATION_ID, notification(running ?: ExportRun.Running(tr("Export"))), foregroundType()) }
        promised.exceptionOrNull()?.let { AppLog.add("Export : service au premier plan refusé (${it.javaClass.simpleName})") }
        if (intent?.action == ACTION_CANCEL) ExportRun.cancelNow()
        // Refused, or the export already over: nothing to keep alive (the export goes on regardless).
        if (promised.isFailure || ExportRun.state.value == null) {
            finish()
            return START_NOT_STICKY
        }
        if (!watching) {
            watching = true
            scope.launch {
                ExportRun.state.collect { now ->
                    if (now == null) {
                        notifyEnd()
                        finish()
                    } else {
                        show(now)
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    // Android 15+ caps media processing each day: the export stops cleanly rather than the app being killed.
    override fun onTimeout(startId: Int, fgsType: Int) {
        ExportRun.cancelNow()
        finish()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun finish() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun show(run: ExportRun.Running) {
        val percent = (run.fraction * 100).roundToInt()
        if (percent == lastPercent && run.stage == lastStage) return
        lastPercent = percent
        lastStage = run.stage
        if (canNotify(this)) getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(run))
    }

    /** Finished while the app is not on screen: says so (a touch brings it back). */
    private fun notifyEnd() {
        val text = ExportRun.ended ?: return
        if (inForeground() || !canNotify(this)) return
        getSystemService(NotificationManager::class.java).notify(
            DONE_NOTIFICATION_ID,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("BenStudio")
                .setContentText(text)
                .setContentIntent(openApp(this))
                .setAutoCancel(true)
                .build(),
        )
    }

    private fun notification(run: ExportRun.Running): Notification {
        channel(this)
        val cancel = PendingIntent.getService(
            this, 1, Intent(this, ExportService::class.java).setAction(ACTION_CANCEL), PendingIntent.FLAG_IMMUTABLE,
        )
        val percent = (run.fraction * 100).roundToInt()
        val shown = com.pocketstudio.model.i18n.percent(percent)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(run.title)
            .setContentText(run.stage?.let { "$it · $shown" } ?: shown)
            .setProgress(100, percent, false)
            .setContentIntent(openApp(this))
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .addAction(0, tr("Annuler"), cancel)
            .build()
    }

    private fun foregroundType() = when {
        Build.VERSION.SDK_INT >= 35 -> ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
        else -> ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
    }

    private fun inForeground(): Boolean {
        val info = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(info)
        return info.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
    }

    companion object {
        private const val CHANNEL_ID = "export"
        private const val NOTIFICATION_ID = 11
        private const val DONE_NOTIFICATION_ID = 12
        private const val ACTION_CANCEL = "com.pocketstudio.CANCEL_EXPORT"

        /** Started with the export; never fails the export (the system may refuse it). */
        internal fun start(context: Context) {
            runCatching { ContextCompat.startForegroundService(context, Intent(context, ExportService::class.java)) }
                .onFailure { AppLog.add("Export : service non démarré (${it.javaClass.simpleName})") }
        }

        private fun channel(context: Context) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, tr("Export"), NotificationManager.IMPORTANCE_LOW).apply {
                    description = tr("Progression des exports")
                    setShowBadge(false)
                },
            )
        }

        private fun canNotify(context: Context) = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

        private fun openApp(context: Context) = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
