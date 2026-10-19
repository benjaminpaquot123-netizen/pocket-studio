package com.pocketstudio.app.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.pocketstudio.app.MainActivity
import com.pocketstudio.app.R
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Keeps the process in the foreground while the transport plays, so the music goes on
 * with the screen off or in another app. It holds no audio itself: the engine keeps running.
 */
class PlaybackService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    // startForeground in onCreate: it runs even when play/stop are tapped so fast that
    // stopService arrives before onStartCommand, so Android never sees a broken promise.
    override fun onCreate() {
        super.onCreate()
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            _stopRequests.tryEmit(Unit)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    // The user swiped the app away: stop like a music player would.
    override fun onTaskRemoved(rootIntent: Intent?) {
        _stopRequests.tryEmit(Unit)
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Lecture", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Affichée pendant que la musique joue"
                setShowBadge(false)
            },
        )
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, PlaybackService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Pocket Studio joue")
            .setContentText("Touchez pour revenir au studio")
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .addAction(0, "Arrêter", stop)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "playback"
        private const val NOTIFICATION_ID = 7
        private const val ACTION_STOP = "com.pocketstudio.app.STOP"

        private val _stopRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        /** "Arrêter" pressed in the notification (or app swiped away). */
        val stops: SharedFlow<Unit> = _stopRequests

        fun start(context: Context) {
            runCatching { ContextCompat.startForegroundService(context, Intent(context, PlaybackService::class.java)) }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PlaybackService::class.java))
        }
    }
}
