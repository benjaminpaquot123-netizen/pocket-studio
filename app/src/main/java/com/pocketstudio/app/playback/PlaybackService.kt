package com.pocketstudio.app.playback

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.pocketstudio.app.MainActivity
import com.pocketstudio.app.R
import com.pocketstudio.app.rhythm.TakeVault
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.pocketstudio.model.i18n.tr

/** What the phone shows while the app plays: the project, its tempo, and whether it records. */
data class PlaybackStatus(
    val title: String = "Pocket Studio",
    val bpm: Double = 0.0,
    /** Live recording or a microphone take (the notification becomes a live activity). */
    val recording: Boolean = false,
    /** When the recording started (System.currentTimeMillis), for the chronometer; 0 while counting in. */
    val recordingSince: Long = 0L,
    /** A microphone take: the service also holds the microphone (it keeps hearing, screen off). */
    val microphone: Boolean = false,
) {
    val subtitle: String get() = if (bpm > 0) "Pocket Studio · ${Math.round(bpm)} BPM" else "Pocket Studio"
}

/**
 * Keeps the process in the foreground while the transport plays, so the music goes on
 * with the screen off or in another app. It holds no audio itself: the engine keeps running.
 * Like a music player, it stops the song when the headphones come out (not full blast on the
 * speaker) and when a call or another player takes the sound.
 *
 * The phone sees a real media player (a media session: the project in the control centre and on
 * the lock screen, Stop, the headset's button). While recording, the notification becomes an
 * Android 16 live activity ("REC" and a chronometer at the top of the screen; Honor's Magic
 * Capsule shows those of the apps it chooses, there is no public way into it).
 */
class PlaybackService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    private val scope = MainScope()
    private var session: MediaSession? = null

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
        running = true
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, tr("Lecture"), NotificationManager.IMPORTANCE_LOW).apply {
                description = tr("Affichée pendant que la musique joue ou s'enregistre")
                setShowBadge(false)
            },
        )
        session = MediaSession(this, "PocketStudio").apply {
            setCallback(player)
            setPlaybackState(
                PlaybackState.Builder()
                    .setActions(PlaybackState.ACTION_STOP or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE)
                    // The control centre builds its buttons from here (Android 13+): a real "Arrêter".
                    .addCustomAction(PlaybackState.CustomAction.Builder(ACTION_STOP, tr("Arrêter"), R.drawable.ic_stop).build())
                    .setState(PlaybackState.STATE_PLAYING, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1f)
                    .build(),
            )
            setSessionActivity(openApp())
            isActive = true
        }
        // What is shown now, and what the updates below start from (a change in between is not lost).
        val first = status.value
        if (!foreground(first)) return
        ContextCompat.registerReceiver(this, noisy, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), ContextCompat.RECEIVER_NOT_EXPORTED)
        getSystemService(AudioManager::class.java)?.requestAudioFocus(focus)
        // The project, the tempo or a recording change: the notification follows, at most
        // four times a second (a tempo knob turned fast would flood it, and Android drops updates;
        // a StateFlow hands a slow collector only the latest status).
        scope.launch {
            var shown = first
            status.collect { s ->
                if (s == shown) return@collect
                // A take that starts needs the microphone type; anything else is a new notification.
                if (s.microphone != shown.microphone) foreground(s)
                else getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(s))
                shown = s
                delay(250)
            }
        }
    }

    /** The control centre's or the headset's buttons: the song stops, like the app's. */
    internal val player = object : MediaSession.Callback() {
        override fun onStop() = stopFromPlayer()

        override fun onPause() = stopFromPlayer()

        override fun onCustomAction(action: String, extras: android.os.Bundle?) {
            if (action == ACTION_STOP) stopFromPlayer()
        }
    }

    private fun stopFromPlayer() {
        _stopRequests.tryEmit("lecteur du téléphone")
        stopSelf()
    }

    /** Puts the service in the foreground for [s]; false (and the service stops) if Android refuses. */
    private fun foreground(s: PlaybackStatus): Boolean {
        val micAllowed = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK or
            if (s.microphone && micAllowed) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0
        val started = runCatching { ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(s), type) }
            .recoverCatching { ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(s), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK) }
        if (started.isFailure) stopSelf()
        return started.isSuccess
    }

    override fun onDestroy() {
        running = false
        scope.cancel()
        // An update posted just before the stop must not leave a player nobody owns.
        getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
        session?.run {
            isActive = false
            release()
        }
        session = null
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

    // The user swiped the app away: stop like a music player would. A microphone take is kept:
    // the service (and so the app) stays until it is written into the project.
    override fun onTaskRemoved(rootIntent: Intent?) {
        _stopRequests.tryEmit("appli fermée")
        scope.launch {
            TakeVault.app.awaitIdle(TAKE_WAIT_MS)
            stopSelf()
        }
    }

    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        this, 0,
        Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE,
    )

    private fun stopIntent(): PendingIntent = PendingIntent.getService(
        this, 1,
        Intent(this, PlaybackService::class.java).setAction(ACTION_STOP),
        PendingIntent.FLAG_IMMUTABLE,
    )

    internal fun buildNotification(s: PlaybackStatus): Notification {
        session?.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, s.title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, s.subtitle)
                .build(),
        )
        return if (s.recording) recordingNotification(s) else playingNotification(s)
    }

    /** Playing: a media player notification tied to the session (control centre, lock screen). */
    private fun playingNotification(s: PlaybackStatus): Notification {
        val stop = Notification.Action.Builder(Icon.createWithResource(this, R.drawable.ic_stop), tr("Arrêter"), stopIntent()).build()
        val style = Notification.MediaStyle().setShowActionsInCompactView(0)
        session?.let { style.setMediaSession(it.sessionToken) }
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(s.title)
            .setContentText(s.subtitle)
            .setContentIntent(openApp())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .addAction(stop)
            .setStyle(style)
            .build()
    }

    /**
     * Recording: an Android 16 live activity (a promoted ongoing notification): "REC" and the time
     * since the take began, in the status bar. Older Android: the same, as a plain notification.
     */
    private fun recordingNotification(s: PlaybackStatus): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (s.microphone) tr("● Prise au micro") else tr("● Enregistrement"))
            .setContentText(s.title)
            .setContentIntent(openApp())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // No chronometer while the take counts in (it starts with the recording itself).
            .setWhen(s.recordingSince)
            .setShowWhen(s.recordingSince > 0)
            .setUsesChronometer(s.recordingSince > 0)
            .setShortCriticalText("REC")
            .setRequestPromotedOngoing(true)
            .addAction(R.drawable.ic_stop, tr("Arrêter"), stopIntent())

            .build()

    companion object {
        private const val CHANNEL_ID = "playback"
        private const val NOTIFICATION_ID = 7
        private const val ACTION_STOP = "com.pocketstudio.app.STOP"

        private val _stopRequests = MutableSharedFlow<String>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        /** Why the song must stop: "Arrêter" in the notification, app swiped away, headphones out, a call. */
        val stops: SharedFlow<String> = _stopRequests

        private val status = MutableStateFlow(PlaybackStatus())

        /** True while the song plays (the service lives exactly that long): previews step back. */
        @Volatile
        var running = false
            private set

        /** What the notification shows; the app keeps it up to date (it costs nothing while stopped). */
        fun update(next: PlaybackStatus) {
            status.value = next
        }

        fun start(context: Context) {
            starts++
            runCatching { ContextCompat.startForegroundService(context, Intent(context, PlaybackService::class.java)) }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PlaybackService::class.java))
        }

        /** How long the app is kept for a take being written after it was closed. */
        const val TAKE_WAIT_MS = 15_000L

        /** [stop], once no microphone take is left to write (the app closing in the middle of one). */
        fun stopAfterTakes(context: Context) {
            val app = context.applicationContext
            if (TakeVault.app.busy.value <= 0) {
                stop(app)
                return
            }
            val since = starts
            MainScope().launch {
                TakeVault.app.awaitIdle(TAKE_WAIT_MS)
                if (starts == since) stop(app)  // not if the music started again meanwhile
            }
        }

        @Volatile
        private var starts = 0
    }
}
