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
import com.pocketstudio.app.generate.GenerationJob
import com.pocketstudio.app.generate.GenerationModel
import com.pocketstudio.app.generate.ModelDownload
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
import com.pocketstudio.model.i18n.tr

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
        // Platform call on purpose: androidx ServiceCompat (core 1.19) masks out types newer than
        // its table, turning mediaProcessing into "none", which Android 14+ refuses with a crash.
        startForeground(NOTIFICATION_ID, notification(tr("Préparation…"), null), foregroundType())
        when (intent?.action) {
            ACTION_CANCEL -> {
                cancelled.set(true)
                if (!running) finish()
            }
            ACTION_DOWNLOAD_GENERATION -> if (!running) {
                running = true
                cancelled.set(false)
                wakeLock = getSystemService(PowerManager::class.java)
                    .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PocketStudio:separation")
                    .apply { acquire(4 * 60 * 60 * 1000L) }
                scope.launch {
                    val end = try {
                        downloadGeneration()
                        ModelDownload.Idle
                    } catch (_: VerifiedDownload.CancelledException) {
                        ModelDownload.Idle
                    } catch (e: IOException) {
                        AppLog.add("Modèle de génération : ${e.message}")
                        ModelDownload.Failed(tr("Échec du téléchargement : %s. Relance pour reprendre.", e.message ?: tr("erreur de réseau")))
                    } catch (e: Exception) {
                        AppLog.add("Modèle de génération : ${e.javaClass.simpleName} ${e.message}")
                        ModelDownload.Failed(tr("Échec du téléchargement (%s).", e.javaClass.simpleName))
                    }
                    GenerationJob.set(end)
                    withContext(Dispatchers.Main) { finish() }
                }
            } else {
                GenerationJob.set(ModelDownload.Failed(tr("Une séparation est en cours : attends qu'elle finisse.")))
            }
            ACTION_DOWNLOAD_MODEL, ACTION_SEPARATE -> if (!running) {
                running = true
                cancelled.set(false)
                val uri = intent.data
                val name = intent.getStringExtra(EXTRA_NAME) ?: tr("Morceau")
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
                        SeparationState.Failed(tr("Morceau trop long (%d min) : %d minutes au maximum.", (e.seconds / 60).roundToInt(), MAX_SECONDS / 60))
                    } catch (e: IOException) {
                        AppLog.add("Séparation : ${e.message}")
                        SeparationState.Failed(tr("Échec : %s.", e.message ?: tr("erreur de lecture ou de réseau")))
                    } catch (_: OutOfMemoryError) {
                        SeparationState.Failed(tr("Mémoire insuffisante : essaie un morceau plus court."))
                    } catch (e: Exception) {
                        // Codec errors, revoked file access…: report, never crash the app mid-job.
                        AppLog.add("Séparation : ${e.javaClass.simpleName} ${e.message}")
                        SeparationState.Failed(tr("Ce fichier n'a pas pu être lu (%s).", e.javaClass.simpleName))
                    } finally {
                        if (uri != null) {
                            runCatching { contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                        }
                    }
                    SeparationJob.set(end)
                    notifyEnd(end)
                    withContext(Dispatchers.Main) { finish() }
                }
            } else {
                // Another job (the generation model download) holds the service: say so, keep nothing.
                intent.data?.let { runCatching { contentResolver.releasePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
                if (!SeparationJob.state.value.busy) {
                    SeparationJob.set(SeparationState.Failed(tr("Un téléchargement de modèle est en cours : réessaie quand il sera fini.")))
                }
            }
            else -> if (!running) finish()
        }
        return START_NOT_STICKY
    }

    // Android 15+ caps media processing at 6 h a day: stop cleanly instead of being killed.
    override fun onTimeout(startId: Int, fgsType: Int) {
        cancelled.set(true)
        if (GenerationJob.download.value is ModelDownload.Running) {
            GenerationJob.set(ModelDownload.Failed(tr("Android a arrêté le téléchargement (temps maximum par jour atteint).")))
        } else {
            SeparationJob.set(SeparationState.Failed(tr("Android a arrêté la séparation (temps maximum par jour atteint).")))
        }
        finish()
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
            progress(tr("Téléchargement du modèle"), p)
        }, cancelled::get)
    }

    /** Files already there (checked by size) are kept: a cut download resumes file by file. */
    private fun downloadGeneration() {
        val specs = GenerationModel.SPECS
        var done = 0L
        GenerationJob.set(ModelDownload.Running(0f))
        for (spec in specs) {
            val target = GenerationModel.file(this, spec)
            if (target.length() != spec.sizeBytes) {
                VerifiedDownload.fetch(spec, target, { p ->
                    val total = (done + p * spec.sizeBytes) / GenerationModel.TOTAL_BYTES
                    GenerationJob.set(ModelDownload.Running(total))
                    progress(tr("Téléchargement du modèle de génération"), total)
                }, cancelled::get)
            }
            done += spec.sizeBytes
        }
    }

    private fun separate(uri: Uri, name: String): SeparationState {
        if (!Demucs.isAvailable) return SeparationState.Failed(tr("La séparation n'est pas disponible sur cet appareil."))
        if (GenerationJob.generating.get()) return SeparationState.Failed(tr("Une génération de son est en cours : relance quand elle sera finie."))
        SeparationJob.set(SeparationState.Decoding(name))
        progress(tr("Lecture de « %s »", name), null)
        StemStore.prune(this)
        val work = File(cacheDir, "separation").apply { deleteRecursively(); mkdirs() }
        try {
            val raw = AudioDecoder.decodeToFile(this, uri, File(work, "input.raw"), MAX_SECONDS.toDouble(), cancelled::get)
            if (raw.seconds < 1.0) return SeparationState.Failed(tr("Son trop court (1 seconde au minimum)."))
            val key = (VerifiedDownload.sha256(raw.file) + "|${raw.channels}|${raw.sampleRate}|${SeparationModel.VERSION}")
                .let { VerifiedDownload.sha256Of(it) }.take(24)
            val final = StemStore.dirFor(this, key)
            StemStore.read(final)?.let { return SeparationState.Done(StemStore.touch(it)) }
            // Four stereo float stems at 44.1 kHz, plus margin.
            val needed = (raw.seconds * Demucs.SAMPLE_RATE * 2 * 4 * 4 * 1.1).toLong()
            if (filesDir.usableSpace < needed) {
                return SeparationState.Failed(tr("Stockage plein : il faut %d Mo libres.", needed / 1_000_000))
            }
            // The model needs about 2.6 GB while it runs (measured), plus the song and its stems.
            val memory = android.app.ActivityManager.MemoryInfo()
            getSystemService(android.app.ActivityManager::class.java).getMemoryInfo(memory)
            val neededRam = RemixMemory.needed(raw.seconds)
            AppLog.add("Séparation : mémoire libre ${memory.availMem / 1_000_000} Mo, besoin ~${neededRam / 1_000_000} Mo")
            if (memory.availMem < neededRam) {
                return SeparationState.Failed(
                    tr("Pas assez de mémoire libre (%.1f Go, il en faut %.1f). Ferme des applis ou prends un morceau plus court.", memory.availMem / 1e9, neededRam / 1e9),
                )
            }
            // A corrupt model could crash ONNX Runtime: check it each time (a second or two).
            if (!VerifiedDownload.verify(SeparationModel.SPEC, SeparationModel.file(this))) {
                SeparationModel.file(this).delete()
                return SeparationState.Failed(tr("Modèle abîmé, supprimé : relance pour le retélécharger."))
            }
            val part = File(final.parentFile, "$key.part").apply { deleteRecursively() }
            SeparationJob.set(SeparationState.Separating(name, 0f))
            AppLog.add("Séparation : modèle lancé (${raw.seconds.roundToInt()} s, ${raw.channels} canaux, ${raw.sampleRate} Hz)")
            val result = Demucs.separateFile(SeparationModel.file(this), raw.file, raw.channels, raw.sampleRate, part) { p ->
                SeparationJob.set(SeparationState.Separating(name, p))
                progress(tr("Séparation de « %s »", name), p)
                !cancelled.get()
            }
            if (result != SeparationResult.Ok) {
                part.deleteRecursively()
                return when (result) {
                    SeparationResult.Cancelled -> SeparationState.Idle
                    SeparationResult.BadInput -> SeparationState.Failed(tr("Son illisible."))
                    SeparationResult.IoError -> SeparationState.Failed(tr("Écriture impossible (stockage plein ou mémoire insuffisante)."))
                    else -> SeparationState.Failed(tr("Le modèle de séparation a échoué."))
                }
            }
            SeparationJob.set(SeparationState.Analysing(name))
            progress(tr("Tempo et tonalité de « %s »", name), null)
            val info = analyse(part)
            final.deleteRecursively()
            if (!part.renameTo(final)) throw IOException(tr("écriture impossible"))
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
            is SeparationState.Done -> tr("« %s » est séparé : touche pour l'ouvrir.", state.set.name)
            is SeparationState.Failed -> state.message
            else -> return
        }
        if (!canNotify()) return
        NotificationManagerCompat.from(this).notify(
            DONE_NOTIFICATION_ID,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("BenStudio")
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
            NotificationChannel(CHANNEL_ID, tr("Séparation"), NotificationManager.IMPORTANCE_LOW).apply {
                description = tr("Progression de la séparation des pistes")
                setShowBadge(false)
            },
        )
        val cancel = PendingIntent.getService(
            this, 2, Intent(this, SeparationService::class.java).setAction(ACTION_CANCEL), PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(fraction?.let { com.pocketstudio.model.i18n.percent((it * 100).roundToInt()) } ?: tr("Patiente un peu…"))
            .setProgress(100, fraction?.let { (it * 100).roundToInt() } ?: 0, fraction == null)
            .setContentIntent(openApp())
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .addAction(0, tr("Annuler"), cancel)

            .build()
    }

    private fun foregroundType() = when {
        Build.VERSION.SDK_INT >= 35 -> ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
        else -> ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
    }

    companion object {
        /** Whole songs; 7 minutes need about 3.9 GB free (the model, then the song and its stems: see RemixMemory). */
        const val MAX_SECONDS = 7 * 60
        private const val CHANNEL_ID = "separation"
        private const val NOTIFICATION_ID = 3
        private const val DONE_NOTIFICATION_ID = 4
        private const val ACTION_SEPARATE = "com.pocketstudio.SEPARATE"
        private const val ACTION_DOWNLOAD_MODEL = "com.pocketstudio.DOWNLOAD_MODEL"
        private const val ACTION_DOWNLOAD_GENERATION = "com.pocketstudio.DOWNLOAD_GENERATION_MODEL"
        private const val ACTION_CANCEL = "com.pocketstudio.CANCEL_SEPARATION"
        private const val EXTRA_NAME = "name"

        fun separate(context: Context, uri: Uri, name: String) = start(
            context,
            Intent(context, SeparationService::class.java).setAction(ACTION_SEPARATE).setData(uri)
                .putExtra(EXTRA_NAME, name).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
        )

        fun downloadModel(context: Context) =
            start(context, Intent(context, SeparationService::class.java).setAction(ACTION_DOWNLOAD_MODEL))

        fun downloadGenerationModel(context: Context) =
            start(context, Intent(context, SeparationService::class.java).setAction(ACTION_DOWNLOAD_GENERATION))

        fun cancel(context: Context) {
            if (SeparationJob.state.value.busy || GenerationJob.download.value is ModelDownload.Running) start(context, Intent(context, SeparationService::class.java).setAction(ACTION_CANCEL))
        }

        private fun start(context: Context, intent: Intent) = ContextCompat.startForegroundService(context, intent)
    }
}
