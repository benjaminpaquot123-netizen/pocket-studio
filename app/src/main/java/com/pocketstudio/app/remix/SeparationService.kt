package com.pocketstudio.app.remix

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.pocketstudio.app.MainActivity
import com.pocketstudio.app.R
import com.pocketstudio.app.audio.AudioDecoder
import com.pocketstudio.app.download.VerifiedDownload
import com.pocketstudio.app.report.AppLog
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.separation.Demucs
import com.pocketstudio.separation.SeparationResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

/**
 * Splits a song into stems (or downloads the model) in the foreground: it takes minutes, so it
 * must survive the screen going off and the user switching apps. One job at a time.
 */
class SeparationService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val cancelled = AtomicBoolean(false)
    private var running = false
    private var wakeLock: PowerManager.WakeLock? = null
    private var lastPercent = -1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Always honour the foreground promise first, even for a request we then ignore.
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification("Préparation…", null), foregroundType())
        when (intent?.action) {
            ACTION_CANCEL -> {
                cancelled.set(true)
                if (!running) finish()
            }
            ACTION_DOWNLOAD_MODEL, ACTION_SEPARATE -> if (!running) {
                running = true
                cancelled.set(false)
                val uri = intent.data
                val name = intent.getStringExtra(EXTRA_NAME) ?: "Morceau"
                wakeLock = getSystemService(PowerManager::class.java)
                    .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PocketStudio:separation")
                    .apply { acquire(4 * 60 * 60 * 1000L) }
                scope.launch {
                    val end = try {
                        ensureModel()
                        if (intent.action == ACTION_SEPARATE && uri != null) separate(uri, name) else SeparationState.Idle
                    } catch (_: VerifiedDownload.CancelledException) {
                        SeparationState.Idle
                    } catch (_: AudioDecoder.CancelledException) {
                        SeparationState.Idle
                    } catch (e: AudioDecoder.TooLongException) {
                        SeparationState.Failed("Morceau trop long (${(e.seconds / 60).roundToInt()} min) : ${MAX_SECONDS / 60} minutes au maximum.")
                    } catch (e: IOException) {
                        AppLog.add("Séparation : ${e.message}")
                        SeparationState.Failed("Échec : ${e.message ?: "erreur de lecture ou de réseau"}.")
                    } catch (_: OutOfMemoryError) {
                        SeparationState.Failed("Mémoire insuffisante : essaie un morceau plus court.")
                    }
                    SeparationJob.set(end)
                    notifyEnd(end)
                    withContext(Dispatchers.Main) { finish() }
                }
            }
            else -> if (!running) finish()
        }
        return START_NOT_STICKY
    }

    // Android 15+ caps media processing at 6 h a day: stop cleanly instead of being killed.
    override fun onTimeout(startId: Int, fgsType: Int) {
        cancelled.set(true)
    }

    override fun onDestroy() {
        cancelled.set(true)
        scope.cancel()
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }

    private fun finish() {
        running = false
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun ensureModel() {
        if (SeparationModel.isInstalled(this)) return
        SeparationJob.set(SeparationState.DownloadingModel(0f))
        VerifiedDownload.fetch(SeparationModel.SPEC, SeparationModel.file(this), { p ->
            SeparationJob.set(SeparationState.DownloadingModel(p))
            progress("Téléchargement du modèle", p)
        }, cancelled::get)
    }

    private fun separate(uri: Uri, name: String): SeparationState {
        if (!Demucs.isAvailable) return SeparationState.Failed("La séparation n'est pas disponible sur ce téléphone.")
        SeparationJob.set(SeparationState.Decoding(name))
        progress("Lecture de « $name »", null)
        StemStore.prune(this)
        val work = File(cacheDir, "separation").apply { deleteRecursively(); mkdirs() }
        try {
            val raw = AudioDecoder.decodeToFile(this, uri, File(work, "input.raw"), MAX_SECONDS.toDouble(), cancelled::get)
            if (raw.seconds < 1.0) return SeparationState.Failed("Son trop court (1 seconde au minimum).")
            val key = (VerifiedDownload.sha256(raw.file) + "|${raw.channels}|${raw.sampleRate}|${SeparationModel.VERSION}")
                .let { VerifiedDownload.sha256Of(it) }.take(24)
            val final = StemStore.dirFor(this, key)
            StemStore.read(final)?.let { return SeparationState.Done(it) }
            // Four stereo float stems at 44.1 kHz, plus margin.
            val needed = (raw.seconds * Demucs.SAMPLE_RATE * 2 * 4 * 4 * 1.1).toLong()
            if (filesDir.usableSpace < needed) {
                return SeparationState.Failed("Stockage plein : il faut ${needed / 1_000_000} Mo libres.")
            }
            // A corrupt model could crash ONNX Runtime: check it each time (a second or two).
            if (!VerifiedDownload.verify(SeparationModel.SPEC, SeparationModel.file(this))) {
                SeparationModel.file(this).delete()
                return SeparationState.Failed("Modèle abîmé, supprimé : relance pour le retélécharger.")
            }
            val part = File(final.parentFile, "$key.part").apply { deleteRecursively() }
            SeparationJob.set(SeparationState.Separating(name, 0f))
            val result = Demucs.separateFile(SeparationModel.file(this), raw.file, raw.channels, raw.sampleRate, part) { p ->
                SeparationJob.set(SeparationState.Separating(name, p))
                progress("Séparation de « $name »", p)
                !cancelled.get()
            }
            if (result != SeparationResult.Ok) {
                part.deleteRecursively()
                return when (result) {
                    SeparationResult.Cancelled -> SeparationState.Idle
                    SeparationResult.BadInput -> SeparationState.Failed("Son illisible.")
                    SeparationResult.IoError -> SeparationState.Failed("Écriture impossible (stockage plein ou mémoire insuffisante).")
                    else -> SeparationState.Failed("Le modèle de séparation a échoué.")
                }
            }
            SeparationJob.set(SeparationState.Analysing(name))
            progress("Tempo et tonalité de « $name »", null)
            val info = analyse(part)
            final.deleteRecursively()
            if (!part.renameTo(final)) throw IOException("rename failed")
            val set = StemStore.commit(final, name, raw.seconds, info)
            StemStore.prune(this)
            AppLog.add("Séparation : « $name » (${raw.seconds.roundToInt()} s) terminée")
            return SeparationState.Done(set)
        } finally {
            work.deleteRecursively()
        }
    }

    // Tempo from the drums alone, key from bass + other: each is cleaner than the full mix.
    private fun analyse(dir: File): SongInfo {
        if (!NativeEngine.isAvailable) return SongInfo()
        val tempo = NativeEngine.analyzeTempo(File(dir, "drums.wav").absolutePath)
        val key = NativeEngine.analyzeKey(arrayOf(File(dir, "bass.wav").absolutePath, File(dir, "other.wav").absolutePath))
        val root = key[0].toInt().takeIf { it in 0..11 }
        return SongInfo(tempo[0].toDouble(), tempo[2].toDouble(), root, key[1] > 0.5f)
    }

    private fun progress(title: String, fraction: Float?) {
        val percent = fraction?.let { (it * 100).roundToInt() } ?: -1
        if (percent == lastPercent) return
        lastPercent = percent
        if (canNotify()) NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification(title, fraction))
    }

    private fun notifyEnd(state: SeparationState) {
        val text = when (state) {
            is SeparationState.Done -> "« ${state.set.name} » est séparé : touche pour l'ouvrir."
            is SeparationState.Failed -> state.message
            else -> return
        }
        if (!canNotify()) return
        NotificationManagerCompat.from(this).notify(
            DONE_NOTIFICATION_ID,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Pocket Studio")
                .setContentText(text)
                .setContentIntent(openApp())
                .setAutoCancel(true)
                .build(),
        )
    }

    private fun canNotify() = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun openApp() = PendingIntent.getActivity(
        this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE,
    )

    private fun notification(title: String, fraction: Float?): Notification {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Séparation", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Progression de la séparation des pistes"
                setShowBadge(false)
            },
        )
        val cancel = PendingIntent.getService(
            this, 2, Intent(this, SeparationService::class.java).setAction(ACTION_CANCEL), PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(fraction?.let { "${(it * 100).roundToInt()} %" } ?: "Patiente un peu…")
            .setProgress(100, fraction?.let { (it * 100).roundToInt() } ?: 0, fraction == null)
            .setContentIntent(openApp())
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .addAction(0, "Annuler", cancel)
            .build()
    }

    private fun foregroundType() = when {
        Build.VERSION.SDK_INT >= 35 -> ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
        else -> ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
    }

    companion object {
        /** Whole songs; the model needs about 1 GB of native memory for 7 minutes. */
        const val MAX_SECONDS = 7 * 60
        private const val CHANNEL_ID = "separation"
        private const val NOTIFICATION_ID = 3
        private const val DONE_NOTIFICATION_ID = 4
        private const val ACTION_SEPARATE = "com.pocketstudio.SEPARATE"
        private const val ACTION_DOWNLOAD_MODEL = "com.pocketstudio.DOWNLOAD_MODEL"
        private const val ACTION_CANCEL = "com.pocketstudio.CANCEL_SEPARATION"
        private const val EXTRA_NAME = "name"

        fun separate(context: Context, uri: Uri, name: String) = start(
            context,
            Intent(context, SeparationService::class.java).setAction(ACTION_SEPARATE).setData(uri)
                .putExtra(EXTRA_NAME, name).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
        )

        fun downloadModel(context: Context) =
            start(context, Intent(context, SeparationService::class.java).setAction(ACTION_DOWNLOAD_MODEL))

        fun cancel(context: Context) {
            if (SeparationJob.state.value.busy) start(context, Intent(context, SeparationService::class.java).setAction(ACTION_CANCEL))
        }

        private fun start(context: Context, intent: Intent) = ContextCompat.startForegroundService(context, intent)
    }
}
