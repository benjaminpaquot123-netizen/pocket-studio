package com.pocketstudio.app.export

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.test.core.app.ApplicationProvider
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

/** A long export runs in a foreground service: progress and « Annuler » in a notification, stopped at the end. */
@RunWith(RobolectricTestRunner::class)
class ExportServiceTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    @After
    fun end() = ExportRun.end(null)

    private fun shown(): Notification {
        ShadowLooper.idleMainLooper()
        return shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications.last()
    }

    @Test
    fun anExportStartsTheServiceWhichShowsItsProgressAndStopsAtTheEnd() {
        shadowOf(app).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        var cancelled = 0
        ExportRun.begin(app, "Export de « Rave »") { cancelled++ }
        val started = shadowOf(app).nextStartedService
        assertEquals(ExportService::class.java.name, started.component!!.className)
        val controller = Robolectric.buildService(ExportService::class.java, started).create()
        val service = controller.get()
        service.onStartCommand(started, 0, 1)
        // In the foreground with a type Android 14+ accepts, the title and « Annuler ».
        val first = shadowOf(service).lastForegroundNotification
        assertNotNull(first)
        assertEquals("Export de « Rave »", first.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("Annuler", first.actions.single().title.toString())
        assertTrue(service.foregroundServiceType and (ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING) != 0)
        ExportRun.progress(0.42f, "Batterie (1/3)")
        val later = shown()
        assertEquals("Batterie (1/3) · 42 %", later.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        assertEquals(42, later.extras.getInt(Notification.EXTRA_PROGRESS))
        // « Annuler » in the notification reaches the export.
        service.onStartCommand(Intent(app, ExportService::class.java).setAction("com.pocketstudio.CANCEL_EXPORT"), 0, 2)
        assertEquals(1, cancelled)
        assertFalse(shadowOf(service).isStoppedBySelf)
        // The export ends (done, failed or cancelled): the service goes.
        ExportRun.end(null)
        ShadowLooper.idleMainLooper()
        assertTrue(shadowOf(service).isStoppedBySelf)
        controller.destroy()
    }

    @Test
    fun aServiceStartedAfterTheEndStopsAtOnce() {
        val service = Robolectric.buildService(ExportService::class.java).create().get()
        service.onStartCommand(Intent(app, ExportService::class.java), 0, 1)
        assertTrue(shadowOf(service).isStoppedBySelf)
        service.onDestroy()
    }
}
