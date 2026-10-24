package com.pocketstudio.app.export.video

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocketstudio.app.audio.finishOrRelease
import com.pocketstudio.app.export.WavExporter
import com.pocketstudio.app.export.WavPcm
import com.pocketstudio.app.report.AppLog
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.DefaultNames
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.video.VideoLevels
import com.pocketstudio.model.video.VideoPlan
import com.pocketstudio.model.video.VideoPlanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/** Where « Créer la vidéo » is: [Stage.Sound] renders the song, [Stage.Pictures] draws and encodes. */
data class VideoExportUi(
    val running: Boolean = false,
    val stage: Stage = Stage.Idle,
    val progress: Float = 0f,
    val file: File? = null,
    val error: String? = null,
    /** Seconds it took (the log and the « Partager encore » line). */
    val seconds: Double? = null,
    /** The share sheet opened by itself for [file] (once: not again when the screen turns). */
    val autoShared: Boolean = false,
) {
    enum class Stage { Idle, Sound, Pictures, Done }
}

/**
 * The window of [plan] out of a WAV of the song from its start ([WavPcm], any bit depth): its
 * frames from [VideoPlan.startFrame], faded by [VideoPlan.gainAt]. Interleaved stereo floats.
 */
fun windowOf(pcm: WavPcm, plan: VideoPlan): FloatArray {
    val channels = pcm.channels
    val buf = FloatArray(16 * 1024)
    // The frames before the window: read and dropped (the WAV reader goes forward only).
    var skip = plan.startFrame
    while (skip > 0) {
        val n = pcm.readFloat(buf, minOf(skip, (buf.size / channels).toLong()).toInt())
        if (n == 0) break
        skip -= n
    }
    val out = FloatArray((plan.frames * 2).toInt())
    var at = 0L
    while (at < plan.frames) {
        val n = pcm.readFloat(buf, minOf(plan.frames - at, (buf.size / channels).toLong()).toInt())
        if (n == 0) break
        for (f in 0 until n) {
            val g = plan.gainAt(at + f)
            val l = buf[f * channels]
            val r = if (channels > 1) buf[f * channels + 1] else l
            out[((at + f) * 2).toInt()] = l * g
            out[((at + f) * 2 + 1).toInt()] = r * g
        }
        at += n
    }
    return out
}

/**
 * « Vidéo pour les réseaux » (phase 39): 30 seconds of the song around its Drop (or Refrain), in
 * 9:16 with the waveform, the parts' names, the title and the app's name, as an MP4 (H.264 +
 * AAC) shared like the song. The sound is the export's own render of the song from its start up
 * to the end of the window (nothing of the export changed), then the window is cut out of it.
 */
class VideoExportViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(VideoExportUi())
    val state: StateFlow<VideoExportUi> = _state.asStateFlow()

    private var job: Job? = null
    @Volatile private var cancelled = false
    @Volatile private var render: OfflineExport? = null

    /** The plan for the open project, for the sheet's preview and text. */
    fun plan(rhythmVm: RhythmViewModel): VideoPlan {
        val p = rhythmVm.project.value
        return VideoPlanner.plan(p, DefaultNames.shown(p.name))
    }

    fun start(rhythmVm: RhythmViewModel) {
        if (job?.isActive == true) return
        if (!NativeEngine.isAvailable) {
            _state.value = VideoExportUi(error = tr("Le moteur audio n'est pas prêt : réessaie dans un instant."))
            return
        }
        if (rhythmVm.exportBusy) {
            _state.value = VideoExportUi(error = tr("Un export est déjà en cours : attends qu'il finisse."))
            return
        }
        val app = getApplication<Application>()
        val plan = plan(rhythmVm)
        if (plan.frames <= 0) {
            _state.value = VideoExportUi(error = tr("Rien à mettre en vidéo : le morceau est vide."))
            return
        }
        cancelled = false
        _state.value = VideoExportUi(running = true, stage = VideoExportUi.Stage.Sound)
        val started = System.nanoTime()
        // One private render at a time (an export waits), kept going in the background like an export.
        rhythmVm.videoBusy = true
        val title = tr("Vidéo de « %s »", DefaultNames.shown(rhythmVm.project.value.name))
        com.pocketstudio.app.export.ExportRun.begin(app, title, ::cancel)
        job = viewModelScope.launch {
            var wav: File? = null
            var mp4: File? = null
            var ending: String? = null
            val shown = launch {
                while (isActive) {
                    com.pocketstudio.app.export.ExportRun.progress(_state.value.progress, null)
                    kotlinx.coroutines.delay(250)
                }
            }
            try {
                val w = runCatching { WavExporter.newFile(app, "${DefaultNames.shown(rhythmVm.project.value.name).take(28)}-video") }
                    .getOrElse { throw IOException(tr("Impossible de préparer le fichier d'export.")) }
                wav = w
                val out = File(w.parentFile, w.nameWithoutExtension + ".mp4").also { mp4 = it }
                // 1. The sound: the song from its start to the end of the window (about 40 % of the time).
                val engine = finishOrRelease(Dispatchers.Default, OfflineExport::close) { OfflineExport() }!!
                render = engine
                // Lot 1: a render that stops moving is stopped, and the video says so (never a bar
                // stuck for good). Armed once the render really starts (review M2): before it, the
                // project's sounds are waited for at a progress of 0.
                var stalled = false
                val watch = com.pocketstudio.app.export.StallWatch(armed = false)
                val poll = launch {
                    while (isActive) {
                        val done = engine.progress
                        _state.value = _state.value.copy(progress = SOUND_SHARE * done)
                        if (!stalled && watch.stalled(done)) {
                            stalled = true
                            AppLog.add("Vidéo : rendu bloqué")
                            engine.cancel()
                        }
                        kotlinx.coroutines.delay(100)
                    }
                }
                val result = try {
                    rhythmVm.renderForVideo(engine, w, plan.renderFrames, onRenderStart = watch::arm)
                } finally {
                    poll.cancel()
                    render = null
                    engine.close()
                }
                when (result) {
                    ExportResult.Cancelled -> {
                        if (stalled && !cancelled) throw IOException(tr("la fabrication du son s'est bloquée, réessaie dans un instant"))
                        throw kotlinx.coroutines.CancellationException("annulé")
                    }
                    ExportResult.IoError -> throw IOException(tr("L'écriture du fichier a échoué (stockage plein ?)."))
                    ExportResult.Ok -> Unit
                }
                if (cancelled) throw kotlinx.coroutines.CancellationException("annulé")
                // 2. The pictures, drawn and encoded with the sound.
                _state.value = _state.value.copy(stage = VideoExportUi.Stage.Pictures, progress = SOUND_SHARE)
                withContext(Dispatchers.Default) {
                    val window = WavPcm.open(w).use { windowOf(it, plan) }
                    w.delete()
                    val levels = VideoLevels.of(plan, window)
                    val (bold, regular) = VideoFrameRenderer.fonts(app)
                    val renderer = VideoFrameRenderer(plan, levels, bold = bold, regular = regular)
                    val sink = MuxerSink(out)
                    try {
                        VideoEncoder.encode(
                            sink, plan, window, renderer::draw,
                            progress = { f -> _state.value = _state.value.copy(progress = SOUND_SHARE + (1f - SOUND_SHARE) * f) },
                            isCancelled = { cancelled || !isActive },
                        )
                    } finally {
                        sink.release()
                    }
                }
                val seconds = (System.nanoTime() - started) / 1e9
                AppLog.add("Vidéo : ${plan.videoFrames} images, ${"%.1f".format(plan.seconds)} s de son, faite en ${"%.1f".format(seconds)} s")
                withContext(Dispatchers.IO) { copyToMovies(app, out) }
                _state.value = VideoExportUi(stage = VideoExportUi.Stage.Done, progress = 1f, file = out, seconds = seconds)
                ending = tr("Vidéo prête : touche pour la partager.")
            } catch (e: kotlinx.coroutines.CancellationException) {
                mp4?.delete()
                _state.value = VideoExportUi()
            } catch (e: Exception) {
                mp4?.delete()
                AppLog.add("Vidéo : échec (${e.message})")
                _state.value = VideoExportUi(error = tr("La vidéo n'a pas pu être faite : %s", e.message ?: e.javaClass.simpleName))
                ending = _state.value.error
            } finally {
                shown.cancel()
                withContext(NonCancellable) { wav?.delete() }
                rhythmVm.videoBusy = false
                com.pocketstudio.app.export.ExportRun.end(ending)
            }
        }
    }

    /** « Annuler »: stops the render or the pictures; nothing is kept. */
    fun cancel() {
        cancelled = true
        render?.cancel()
        job?.cancel()
    }

    /** The share sheet opened by itself for the video made ([VideoExportUi.autoShared]). */
    fun markShared() {
        _state.value = _state.value.copy(autoShared = true)
    }

    /** Back to the first page of the sheet (after an error, or to make another one). */
    fun reset() {
        if (job?.isActive != true) _state.value = VideoExportUi()
    }

    override fun onCleared() = cancel()

    companion object {
        /** The part of the progress bar the sound's render takes. */
        const val SOUND_SHARE = 0.4f
        const val MOVIES_FOLDER = "Movies/Pocket Studio"

        /** A copy in Movies › Pocket Studio, as the songs go to Music (no permission needed). Blocking. */
        fun copyToMovies(context: Context, file: File): Boolean = runCatching {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, MOVIES_FOLDER)
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return false
            try {
                resolver.openOutputStream(uri)!!.use { o -> file.inputStream().use { it.copyTo(o) } }
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                true
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
        }.getOrDefault(false)
    }
}
