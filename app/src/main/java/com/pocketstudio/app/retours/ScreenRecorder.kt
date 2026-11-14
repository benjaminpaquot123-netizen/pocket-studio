package com.pocketstudio.app.retours

import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import com.pocketstudio.app.export.video.MuxerSink
import com.pocketstudio.app.report.AppLog
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.i18n.tr
import java.io.File
import java.nio.ByteOrder
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

/** The video's size: the screen scaled to fit 720 × 1280 (either way up), multiples of 16. */
object RecordSize {
    fun of(width: Int, height: Int): Pair<Int, Int> {
        val short = minOf(width, height).coerceAtLeast(16)
        val long = maxOf(width, height).coerceAtLeast(16)
        val scale = minOf(1f, 720f / short, 1280f / long)
        fun even16(v: Float) = ((v / 16f).toInt() * 16).coerceAtLeast(16)
        return even16(width * scale) to even16(height * scale)
    }
}

/**
 * « Mode retours »: the screen ([projection]) in H.264 (~2.5 Mbit/s, 30 images/s) with the app's own
 * sound (the engine's output, copied by NativeEngine.tapStart: nothing in the audio callback but a
 * lock-free ring write) in AAC, into files of at most 20 MB ([SegmentMuxer]) in [dir].
 * [ended]: called once, with a message when it stopped by itself (no room, an error).
 */
class ScreenRecorder(
    private val context: Context,
    private val projection: MediaProjection,
    private val dir: File,
    private val ended: (String?) -> Unit,
) {
    private val thread = HandlerThread("retours-video").apply { start() }
    private val handler = Handler(thread.looper)
    private var video: MediaCodec? = null
    private var audio: MediaCodec? = null
    private var surface: Surface? = null
    private var display: VirtualDisplay? = null
    private var audioThread: Thread? = null
    private val running = AtomicBoolean(false)
    private val finished = AtomicBoolean(false)
    private val videoDone = CountDownLatch(1)
    private val stamp = SegmentNames.stamp(LocalDateTime.now())
    private lateinit var muxer: SegmentMuxer

    /** Starts; false (everything released) when it cannot. */
    fun start(): Boolean = runCatching {
        dir.mkdirs()
        if (dir.usableSpace < MIN_FREE_START) {
            release()
            ended(tr("Plus assez de place : l'enregistrement s'est arrêté."))
            return false
        }
        val metrics = context.resources.displayMetrics
        val (w, h) = RecordSize.of(metrics.widthPixels, metrics.heightPixels)
        muxer = SegmentMuxer(
            open = { n ->
                val f = File(dir, SegmentNames.name(stamp, n))
                Retours.recordingFile = f
                MuxerSink(f)
            },
            requestKeyFrame = { runCatching { video?.setParameters(Bundle().apply { putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0) }) } },
            onSegment = { n, ptsUs -> Retours.segmentStarted(n, ptsUs * 1000L) },
        )
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, w, h).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, 2_500_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            // A still screen still gives pictures (the video keeps its length).
            setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 100_000L)
        }
        val v = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        video = v
        v.setCallback(object : MediaCodec.Callback() {
            override fun onInputBufferAvailable(codec: MediaCodec, index: Int) = Unit
            override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
                runCatching {
                    val buf = codec.getOutputBuffer(index)
                    if (buf != null) synchronized(muxer) { muxer.writeVideo(buf, info) }
                    codec.releaseOutputBuffer(index, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0 && dir.usableSpace < MIN_FREE) {
                        stopAsync(tr("Plus assez de place : l'enregistrement s'est arrêté."))
                    }
                }.onFailure { stopAsync(tr("L'enregistrement s'est arrêté.")) }
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) videoDone.countDown()
            }
            override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                AppLog.add("Retours : vidéo en erreur (${e.diagnosticInfo})")
                videoDone.countDown()
                stopAsync(tr("L'enregistrement s'est arrêté."))
            }
            override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                synchronized(muxer) { muxer.videoFormat(format) }
            }
        }, handler)
        v.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        surface = v.createInputSurface()
        startAudio()
        v.start()
        running.set(true)
        display = projection.createVirtualDisplay(
            "BenStudio-retours", w, h, context.resources.displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, surface, null, handler,
        )
        Retours.setRec(RecState.Recording(0, System.nanoTime()))
        true
    }.getOrElse { e ->
        AppLog.add("Retours : enregistrement impossible (${e.javaClass.simpleName}: ${e.message})")
        release()
        false
    }

    /** The app's sound: the engine's output copy, as 16-bit AAC; silence when the engine is quiet. */
    private fun startAudio() {
        val a = runCatching {
            val f = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, RATE, 2).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, 128_000)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            }
            MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC).apply {
                configure(f, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                start()
            }
        }.getOrNull()
        if (a == null) {
            AppLog.add("Retours : pas de son (encodeur AAC indisponible)")
            muxer.withoutAudio()
            return
        }
        audio = a
        runCatching { NativeEngine.tapStart() }
        audioThread = Thread({ audioLoop(a) }, "retours-audio").apply { start() }
    }

    private fun audioLoop(codec: MediaCodec) {
        val floats = FloatArray(4096)
        val info = MediaCodec.BufferInfo()
        val startNs = System.nanoTime()
        var frames = 0L
        fun ptsUs() = startNs / 1000L + frames * 1_000_000L / RATE
        fun drain(untilEnd: Boolean) {
            val deadline = System.nanoTime() + 3_000_000_000L
            while (true) {
                val i = codec.dequeueOutputBuffer(info, if (untilEnd) 10_000 else 0)
                if (i == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    synchronized(muxer) { muxer.audioFormat(codec.outputFormat) }
                } else if (i >= 0) {
                    codec.getOutputBuffer(i)?.let { b -> synchronized(muxer) { muxer.writeAudio(b, info) } }
                    codec.releaseOutputBuffer(i, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                } else if (!untilEnd || System.nanoTime() > deadline) {
                    return
                }
            }
        }
        fun queue(count: Int, silent: Boolean) {
            var done = 0
            while (done < count) {
                val index = codec.dequeueInputBuffer(10_000)
                if (index < 0) { drain(false); if (!running.get()) return; continue }
                val buf = codec.getInputBuffer(index) ?: return
                buf.clear()
                val n = minOf(count - done, buf.capacity() / 2) and 1.inv()
                val shorts = buf.order(ByteOrder.nativeOrder()).asShortBuffer()
                for (k in 0 until n) {
                    val s = if (silent) 0f else floats[done + k]
                    shorts.put((s.coerceIn(-1f, 1f) * 32767f).roundToInt().toShort())
                }
                codec.queueInputBuffer(index, 0, n * 2, ptsUs(), 0)
                frames += n / 2
                done += n
            }
        }
        runCatching {
            while (running.get()) {
                val n = runCatching { NativeEngine.tapRead(floats) }.getOrDefault(0)
                if (n > 0) {
                    queue(n, silent = false)
                } else {
                    // The engine quiet (stream closed, in the background): silence keeps the sound in time.
                    val due = (System.nanoTime() - startNs) * RATE / 1_000_000_000L
                    if (due - frames > RATE / 10) queue((minOf(due - frames, 2048L) * 2).toInt(), silent = true)
                    else Thread.sleep(5)
                }
                drain(false)
            }
            val index = codec.dequeueInputBuffer(50_000)
            if (index >= 0) codec.queueInputBuffer(index, 0, 0, ptsUs(), MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            drain(true)
        }.onFailure { AppLog.add("Retours : son en erreur (${it.javaClass.simpleName})") }
        runCatching { NativeEngine.tapStop() }
    }

    /** Stops from any thread (the bubble's ■, the system's « Arrêter le partage », no room). */
    fun stopAsync(why: String?) {
        if (finished.get()) return
        Thread({ stop(why) }, "retours-stop").start()
    }

    /** Finishes the last file and releases everything. Blocking (up to a few seconds). */
    fun stop(why: String? = null) {
        if (!finished.compareAndSet(false, true)) return
        Retours.setRec(RecState.Stopping)
        running.set(false)
        runCatching { video?.signalEndOfInputStream() }
        videoDone.await(3, TimeUnit.SECONDS)
        runCatching { audioThread?.join(3000) }
        release()
        ended(why)
    }

    private fun release() {
        running.set(false)
        runCatching { display?.release() }
        if (::muxer.isInitialized) synchronized(muxer) { runCatching { muxer.finish() } }
        runCatching { video?.stop() }
        runCatching { video?.release() }
        runCatching { audio?.stop() }
        runCatching { audio?.release() }
        runCatching { surface?.release() }
        runCatching { NativeEngine.tapStop() }
        runCatching { projection.stop() }
        thread.quitSafely()
        video = null; audio = null; display = null; surface = null
    }

    companion object {
        private const val RATE = 48_000
        /** Below this much free space nothing starts (100 MB), and a recording stops (50 MB). */
        const val MIN_FREE_START = 100_000_000L
        const val MIN_FREE = 50_000_000L
    }
}
