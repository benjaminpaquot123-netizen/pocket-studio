package com.pocketstudio.app.update

import com.pocketstudio.model.i18n.tr
import android.Manifest
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.pocketstudio.app.R

/**
 * Receives the installer status; shows the system confirmation screen when needed. The app no
 * longer on screen (the download went on while it was elsewhere), Android does not let it open
 * that screen: a notification « Mise à jour prête » opens it with a touch instead.
 */
class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT) ?: return
                confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val opened = foreground(context) && runCatching { context.startActivity(confirm) }.isSuccess
                if (!opened) notifyReady(context, confirm)
            }
            PackageInstaller.STATUS_SUCCESS -> Log.i("PocketStudio", "update installed")
            else -> {
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                Log.w("PocketStudio", "update failed: $status $message")
                Toast.makeText(context, tr("Mise à jour impossible : %s", message), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun notifyReady(context: Context, confirm: Intent) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("PocketStudio", "update ready, no notification allowed")
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, tr("Mises à jour"), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = tr("Une mise à jour téléchargée attend ta confirmation")
            },
        )
        val open = PendingIntent.getActivity(
            context, 0, confirm, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        manager.notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("BenStudio")
                .setContentText(tr("Mise à jour prête : touche pour l'installer."))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build(),
        )
    }

    companion object {
        private const val CHANNEL_ID = "updates"
        private const val NOTIFICATION_ID = 13

        /** Whether the app is on screen (it may then open the confirmation itself). Tests replace it. */
        @VisibleForTesting
        internal var foreground: (Context) -> Boolean = {
            val info = ActivityManager.RunningAppProcessInfo()
            ActivityManager.getMyMemoryState(info)
            info.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        }
    }
}
