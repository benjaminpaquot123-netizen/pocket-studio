package com.pocketstudio.app.update

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/** The install confirmation: opened at once with the app on screen, a notification when it is elsewhere. */
@RunWith(RobolectricTestRunner::class)
class InstallResultReceiverTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val saved = InstallResultReceiver.foreground

    @After
    fun restore() {
        InstallResultReceiver.foreground = saved
    }

    private fun pending(): Intent = Intent()
        .putExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_PENDING_USER_ACTION)
        .putExtra(Intent.EXTRA_INTENT, Intent("android.content.pm.action.CONFIRM_INSTALL"))

    @Test
    fun inTheBackgroundTheConfirmationWaitsInANotification() {
        shadowOf(app).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        InstallResultReceiver.foreground = { false }
        InstallResultReceiver().onReceive(app, pending())
        assertNull("no screen opened from the background", shadowOf(app).nextStartedActivity)
        val n = shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications.single()
        assertEquals("Mise à jour prête : touche pour l'installer.", n.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        assertTrue(n.flags and Notification.FLAG_AUTO_CANCEL != 0)
        val opens = shadowOf(n.contentIntent).savedIntent
        assertEquals("android.content.pm.action.CONFIRM_INSTALL", opens.action)
    }

    @Test
    fun onScreenTheConfirmationOpensAtOnce() {
        InstallResultReceiver.foreground = { true }
        InstallResultReceiver().onReceive(app, pending())
        val opened = shadowOf(app).nextStartedActivity
        assertNotNull(opened)
        assertEquals("android.content.pm.action.CONFIRM_INSTALL", opened.action)
        assertTrue(shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications.isEmpty())
    }
}
