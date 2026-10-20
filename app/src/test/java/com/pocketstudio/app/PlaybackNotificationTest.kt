package com.pocketstudio.app

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.playback.PlaybackService
import com.pocketstudio.app.playback.PlaybackStatus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLooper

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
}
