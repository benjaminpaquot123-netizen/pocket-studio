package com.pocketstudio.app

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.playback.PlaybackService
import com.pocketstudio.app.playback.PlaybackStatus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLooper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@RunWith(RobolectricTestRunner::class)
class PlaybackNotificationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @After
    fun reset() = PlaybackService.update(PlaybackStatus())

    private fun shown(): Notification {
        ShadowLooper.idleMainLooper()
        return shadowOf(context.getSystemService(NotificationManager::class.java)).allNotifications.last()
    }

    @Test
    fun playingIsAMediaPlayerWithTheProjectAndStop() {
        PlaybackService.update(PlaybackStatus("Rave du samedi", 132.0))
        val service = Robolectric.buildService(PlaybackService::class.java).create().get()
        val n = shadowOf(service).lastForegroundNotification
        assertNotNull(n)
        assertEquals("Rave du samedi", n.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("Pocket Studio · 132 BPM", n.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        // A media session is attached: the phone shows it as a player (control centre, lock screen).
        assertNotNull(n.extras.getParcelable(Notification.EXTRA_MEDIA_SESSION, android.media.session.MediaSession.Token::class.java))
        assertEquals("Arrêter", n.actions.single().title.toString())
        service.onDestroy()
    }

    @Test
    fun recordingBecomesALiveActivityWithRecAndAChronometer() {
        PlaybackService.update(PlaybackStatus("Rave du samedi", 132.0))
        val service = Robolectric.buildService(PlaybackService::class.java).create().get()
        PlaybackService.update(PlaybackStatus("Rave du samedi", 132.0, recording = true, recordingSince = 1_000L))
        val n = shown()
        assertEquals("● Enregistrement", n.extras.getString(Notification.EXTRA_TITLE))
        assertTrue(n.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertEquals(1_000L, n.`when`)
        // Asks to be promoted (Android 16 live update: the system grants it), "REC" in the chip.
        assertTrue(n.extras.getBoolean(Notification.EXTRA_REQUEST_PROMOTED_ONGOING))
        assertTrue(n.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertEquals("REC", n.shortCriticalText)
        service.onDestroy()
    }

    @Test
    fun stopInTheNotificationOrThePlayerStopsTheSongAndTheService() {
        val got = mutableListOf<String>()
        val listener = CoroutineScope(Dispatchers.Unconfined)
        listener.launch { PlaybackService.stops.collect { got += it } }
        PlaybackService.update(PlaybackStatus("Rave du samedi", 132.0))
        val service = Robolectric.buildService(PlaybackService::class.java).create().get()
        service.onStartCommand(Intent(context, PlaybackService::class.java).setAction("com.pocketstudio.app.STOP"), 0, 1)
        assertEquals(listOf("notification"), got)
        assertTrue(shadowOf(service).isStoppedBySelf)

        // The control centre's pause, its "Arrêter" or the headset's button (the media session's callback).
        service.player.onPause()
        service.player.onCustomAction("com.pocketstudio.app.STOP", null)
        assertEquals(listOf("notification", "lecteur du téléphone", "lecteur du téléphone"), got)
        listener.cancel()
        service.onDestroy()
    }

    @Test
    fun aTakeHoldsTheMicrophoneAndItsChronometerWaitsForTheCountIn() {
        shadowOf(ApplicationProvider.getApplicationContext<android.app.Application>()).grantPermissions(android.Manifest.permission.RECORD_AUDIO)
        PlaybackService.update(PlaybackStatus("Prise", 120.0, recording = true, recordingSince = 0L, microphone = true))
        val service = Robolectric.buildService(PlaybackService::class.java).create().get()
        // Without the microphone type, Android cuts the microphone of an app in the background.
        assertTrue(service.foregroundServiceType and ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE != 0)
        val counting = shadowOf(service).lastForegroundNotification
        assertEquals("● Prise au micro", counting.extras.getString(Notification.EXTRA_TITLE))
        assertFalse(counting.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        service.onDestroy()
    }

    @Test
    fun anUpdateLateForTheStopLeavesNothingBehind() {
        PlaybackService.update(PlaybackStatus("Rave du samedi", 132.0))
        val service = Robolectric.buildService(PlaybackService::class.java).create().get()
        ShadowLooper.idleMainLooper()
        // A change posted just before Stop: Android takes the service's notification away first,
        // then the update runs and posts a new one, then the service is destroyed.
        PlaybackService.update(PlaybackStatus("Rave du samedi", 132.0, recording = true, recordingSince = 1_000L))
        service.stopForeground(android.app.Service.STOP_FOREGROUND_REMOVE)
        ShadowLooper.idleMainLooper()
        val manager = shadowOf(context.getSystemService(NotificationManager::class.java))
        assertTrue(manager.allNotifications.isNotEmpty())
        service.onDestroy()
        assertTrue(manager.allNotifications.isEmpty())
    }
}
