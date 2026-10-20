package com.pocketstudio.app.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
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
 * Like a music player, it stops the song when the headphones come out (not full blast on the
 * speaker) and when a call or another player takes the sound.
 */
class PlaybackService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    private val noisy = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) _stopRequests.tryEmit("casque débranché")
        }
    }

    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
        .setOnAudioFocusChangeListener { change ->
            // A notification may lower us a moment (duck): keep playing. A call or another player: stop.
            if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                _stopRequests.tryEmit("son pris par un appel ou une autre appli")
            }
        }
        .build()

    // startForeground in onCreate: it runs even when play/stop are tapped so fast that
    // stopService arrives before onStartCommand, so Android never sees a broken promise.
    override fun onCreate() {
        super.onCreate()
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )
        ContextCompat.registerReceiver(this, noisy, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), ContextCompat.RECEIVER_NOT_EXPORTED)
        getSystemService(AudioManager::class.java)?.requestAudioFocus(focus)
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(noisy) }
        getSystemService(AudioManager::class.java)?.abandonAudioFocusRequest(focus)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            _stopRequests.tryEmit("notification")
            stopSelf()
        }
        return START_NOT_STICKY
    }

    // The user swiped the app away: stop like a music player would.
    override fun onTaskRemoved(rootIntent: Intent?) {
        _stopRequests.tryEmit("appli fermée")
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

        private val _stopRequests = MutableSharedFlow<String>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        /** Why the song must stop: "Arrêter" in the notification, app swiped away, headphones out, a call. */
        val stops: SharedFlow<String> = _stopRequests

        fun start(context: Context) {
            runCatching { ContextCompat.startForegroundService(context, Intent(context, PlaybackService::class.java)) }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PlaybackService::class.java))
        }
    }
}
