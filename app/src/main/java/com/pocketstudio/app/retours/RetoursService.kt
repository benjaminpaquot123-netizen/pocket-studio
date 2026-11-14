package com.pocketstudio.app.retours

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import com.pocketstudio.app.MainActivity
import com.pocketstudio.app.R
import com.pocketstudio.app.report.AppLog
import com.pocketstudio.model.i18n.tr
import java.io.File

/**
 * « Mode retours »: the screen recording's foreground service (type mediaProjection, which
 * Android 14+ wants started BEFORE the projection is taken, after each consent). It goes on with the
 * app in the background; the system's « Arrêter le partage » or the notification's « Arrêter »
 * finish the file cleanly.
 */
class RetoursService : Service() {
    private var recorder: ScreenRecorder? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopRecording(null)
            return START_NOT_STICKY
        }
        if (recorder != null) return START_NOT_STICKY
        val code = intent?.getIntExtra(EXTRA_CODE, Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val data = intent?.let { IntentCompat.getParcelableExtra(it, EXTRA_DATA, Intent::class.java) }
        if (code != Activity.RESULT_OK || data == null) return fail(tr("L'enregistrement de l'écran a été refusé."))
        val promised = runCatching { startForeground(NOTIFICATION_ID, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION) }
        if (promised.isFailure) {
            AppLog.add("Retours : service refusé (${promised.exceptionOrNull()?.javaClass?.simpleName})")
            return fail(tr("L'enregistrement n'a pas pu démarrer."))
        }
        val projection = runCatching { getSystemService(MediaProjectionManager::class.java).getMediaProjection(code, data) }.getOrNull()
            ?: return fail(tr("L'enregistrement n'a pas pu démarrer."))
        // Android 14+: a callback before the virtual display; it also hears the system stop the sharing.
        projection.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() = stopRecording(tr("L'enregistrement s'est arrêté."))
        }, Handler(Looper.getMainLooper()))
        val dir = Retours.videosDir() ?: File(File(filesDir, Retours.DIR), "videos")
        val r = ScreenRecorder(this, projection, dir) { why -> Handler(Looper.getMainLooper()).post { ended(why) } }
        recorder = r
        if (!r.start()) {
            recorder = null
            runCatching { projection.stop() }
            if (Retours.rec.value != RecState.Idle) Retours.recordingEnded(tr("L'enregistrement n'a pas pu démarrer."))
            stopSelfNow()
        }
        return START_NOT_STICKY
    }

    private fun fail(why: String): Int {
        Retours.recordingEnded(why)
        stopSelfNow()
        return START_NOT_STICKY
    }

    private fun stopRecording(why: String?) {
        val r = recorder
        if (r == null) {
            if (Retours.rec.value != RecState.Idle) Retours.recordingEnded(why)
            stopSelfNow()
        } else {
            r.stopAsync(why)
        }
    }

    private fun ended(why: String?) {
        recorder = null
        Retours.recordingEnded(why)
        stopSelfNow()
    }

    private fun stopSelfNow() {
        runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
        stopSelf()
    }

    override fun onDestroy() {
        // Killed or stopped from outside: the last file is still finished.
        // (on its own thread: never blocks the main one), then ended() says so.
        recorder?.stopAsync(null)
        super.onDestroy()
    }

    private fun notification(): Notification {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, tr("Mode retours"), NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) },
        )
        val stop = PendingIntent.getService(this, 3, Intent(this, RetoursService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE)
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(tr("Mode retours"))
            .setContentText(tr("L'écran est filmé avec le son de l'appli."))
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
            .addAction(0, tr("Arrêter"), stop)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "retours"
        private const val NOTIFICATION_ID = 31
        private const val EXTRA_CODE = "code"
        private const val EXTRA_DATA = "data"
        private const val ACTION_STOP = "com.pocketstudio.RETOURS_STOP"

        /** After the consent ([code], [data] from the system's dialog). Never throws. */
        fun start(context: Context, code: Int, data: Intent?) {
            val intent = Intent(context, RetoursService::class.java).putExtra(EXTRA_CODE, code).putExtra(EXTRA_DATA, data)
            runCatching { ContextCompat.startForegroundService(context, intent) }.onFailure {
                AppLog.add("Retours : service non démarré (${it.javaClass.simpleName})")
                Retours.recordingEnded(tr("L'enregistrement n'a pas pu démarrer."))
            }
        }

        fun stop(context: Context) {
            runCatching { context.startService(Intent(context, RetoursService::class.java).setAction(ACTION_STOP)) }
                .onFailure { if (Retours.rec.value != RecState.Idle) Retours.recordingEnded(null) }
        }
    }
}
