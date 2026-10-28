package com.pocketstudio.app.human

import android.os.SystemClock
import com.pocketstudio.app.audio.FloatWav
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.OfflineExport
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sin

/**
 * The phone's engine as far as the « testeur humain » can tell, for « Partager » (lot « tuto 4 »,
 * É0): Robolectric has no native engine, so the share said « le moteur audio n'est pas prêt »
 * where a phone exports. While the bench sets it as [com.pocketstudio.engine.NativeEngine.sink],
 * each render writes [SECONDS] s of a soft tone (a WAV the app then compresses, checks and shares
 * as it would a song), and the live engine's commands are let go. Never in the app.
 *
 * The Remix (2026-10-28): a separated song's stems are read from their WAVs when asked (a piece
 * for « → Pad », the waveforms), and where the song is while it plays along the project or alone
 * (« Écouter ») follows the bench's clock, as the engine's would.
 */
internal class BenchEngine : CommandSink {
    private var next = 1L

    /** Renders asked for since this engine was made (tests). */
    @Volatile var renders = 0
        private set

    override fun post(target: Long, type: Int, value: Float, a: Int, b: Int) {
        if (target != 0L) return
        when (type) {
            CMD_SET_TRANSPORT -> transportSince = if (value > 0f) SystemClock.uptimeMillis() else -1L
            CMD_STEM_START -> stemStart = a.toLong()
            CMD_PLAY_PREVIEW -> previewSince = if (value > 0f) SystemClock.uptimeMillis() else -1L
        }
    }

    override fun scheduleFrom(target: Long, frame: Long) = Unit

    override fun install(target: Long, kind: CommandSink.Install, index: Int, sample: Long) {
        if (target != 0L) return
        when (kind) {
            CommandSink.Install.Stems -> playedStems = sample
            CommandSink.Install.PreviewStems -> previewStems = sample
            else -> Unit
        }
    }
    @Synchronized override fun sampleCreate(data: FloatArray, channels: Int, sampleRate: Int): Long = next++
    override fun sampleFrames(handle: Long): Long = OfflineExport.SAMPLE_RATE.toLong()
    override fun sampleRelease(handle: Long) = Unit
    @Synchronized override fun exportCreate(): Long = next++
    override fun exportDestroy(target: Long) = Unit

    /** Each render's progress (lot « tuto 5 »: the bar moves as on a phone) and its « Annuler ». */
    private val progress = java.util.concurrent.ConcurrentHashMap<Long, Float>()
    private val cancelled: MutableSet<Long> = java.util.concurrent.ConcurrentHashMap.newKeySet()

    override fun exportProgress(target: Long): Float = progress[target] ?: 0f
    override fun exportCancel(target: Long) { cancelled += target }

    override fun exportRender(
        target: Long, path: String, frames: Long, tailFrames: Long, fadeOutFrames: Long, autoTail: Boolean, floatWav: Boolean,
        sampleRate: Int, bits: Int,
    ): ExportResult {
        renders++
        // A phone renders a 3-minute song in a few seconds: the bench takes [RENDER_MS], its progress moving.
        for (k in 1..STEPS) {
            if (target in cancelled) return ExportResult.Cancelled
            Thread.sleep(renderMs / STEPS)
            progress[target] = k / STEPS.toFloat()
        }
        val n = SECONDS * sampleRate
        FloatWav.write(File(path), tone(n, sampleRate), 0, n, 2, sampleRate)
        return ExportResult.Ok
    }

    /** How long a render takes here (real time), [RENDER_MS] unless a test wants it longer. */
    @Volatile var renderMs = RENDER_MS

    override fun exportRenderFloat(target: Long, frames: Long, tailFrames: Long): FloatArray =
        tone(minOf(frames + tailFrames, (SECONDS * OfflineExport.SAMPLE_RATE).toLong()).toInt().coerceAtLeast(1), OfflineExport.SAMPLE_RATE)

    // --- The Remix's stems ---------------------------------------------------------------------

    /** A song's four stems: their WAVs (read a piece at a time), or one sound in memory ([pcm]). */
    private class Stems(val files: List<WavFile>, val pcm: FloatArray?, val ratio: Double) {
        val frames: Long = if (pcm != null) pcm.size / 2L else Math.round(files.maxOf { it.frames.toDouble() * RATE / it.rate } * ratio)
    }

    private val stems = ConcurrentHashMap<Long, Stems>()
    @Volatile private var transportSince = -1L
    @Volatile private var stemStart = 0L
    @Volatile private var playedStems = 0L
    @Volatile private var previewSince = -1L
    @Volatile private var previewStems = 0L

    /** Bundles held now (tests: each one let go when the app is done with it). */
    val stemsHeld: Int get() = stems.size

    @Synchronized private fun keep(s: Stems): Long = (next++).also { stems[it] = s }

    override fun stemsLoad(paths: Array<String>): Long {
        val files = paths.map { File(it) }
        if (files.any { !it.isFile }) return 0L
        return runCatching { keep(Stems(files.map(::WavFile), null, 1.0)) }.getOrDefault(0L)
    }

    override fun stemsFromPcm(data: FloatArray): Long = keep(Stems(emptyList(), data.copyOf(), 1.0))
    override fun stemsFrames(handle: Long): Long = stems[handle]?.frames ?: 0L
    override fun stemsRelease(handle: Long): Boolean {
        stems.remove(handle)
        return true
    }

    override fun stemsStretched(handle: Long, ratio: Double): Long {
        val s = stems[handle] ?: return 0L
        return keep(Stems(s.files, s.pcm, s.ratio * ratio))
    }

    /** Stereo interleaved at 48 kHz, from frame [start] of the (stretched) song. */
    override fun stemsExtract(handle: Long, stem: Int, start: Long, count: Long): FloatArray {
        val s = stems[handle] ?: return FloatArray(0)
        val n = minOf(count, s.frames - start).coerceAtLeast(0L).toInt()
        if (n == 0) return FloatArray(0)
        s.pcm?.let { pcm -> return if (stem == 0) pcm.copyOfRange(2 * start.toInt(), 2 * (start.toInt() + n)) else FloatArray(2 * n) }
        val wav = s.files.getOrNull(stem) ?: return FloatArray(2 * n)
        val step = wav.rate.toDouble() / RATE / s.ratio  // source frames per output frame
        val first = floor(start * step).toLong()
        val src = wav.read(first, (n * step).toInt() + 3)
        return FloatArray(2 * n) { k ->
            val pos = (start + k / 2) * step - first
            val i = pos.toInt()
            val f = (pos - i).toFloat()
            val a = src.getOrElse(2 * i + k % 2) { 0f }
            val b = src.getOrElse(2 * (i + 1) + k % 2) { a }
            a + (b - a) * f
        }
    }

    /** The peak (0..1) of each of [buckets] slices of one stem. */
    override fun stemsPeaks(handle: Long, stem: Int, buckets: Int): FloatArray {
        val s = stems[handle] ?: return FloatArray(buckets)
        val out = FloatArray(buckets)
        if (s.pcm != null) {
            if (stem == 0) for (k in s.pcm.indices) {
                val b = (k / 2L * buckets / (s.pcm.size / 2)).toInt().coerceAtMost(buckets - 1)
                out[b] = maxOf(out[b], abs(s.pcm[k]))
            }
            return out
        }
        val wav = s.files.getOrNull(stem) ?: return out
        var at = 0L
        while (at < wav.frames) {
            val piece = wav.read(at, 1 shl 15)
            for (k in piece.indices) {
                val b = ((at + k / 2) * buckets / wav.frames).toInt().coerceAtMost(buckets - 1)
                out[b] = maxOf(out[b], abs(piece[k]).coerceAtMost(1f))
            }
            at += 1 shl 15
        }
        return out
    }

    /** The song playing along the project: from its start frame, as long as the transport runs. */
    override fun stemPosition(): Long {
        val since = transportSince
        val s = stems[playedStems] ?: return -1L
        if (since < 0) return -1L
        val pos = stemStart + (SystemClock.uptimeMillis() - since) * RATE / 1000
        return if (pos < s.frames) pos else -1L
    }

    /** « Écouter » on the Remix page: from the top, until its end. */
    override fun previewPosition(): Long {
        val since = previewSince
        val s = stems[previewStems] ?: return -1L
        if (since < 0) return -1L
        val pos = (SystemClock.uptimeMillis() - since) * RATE / 1000
        if (pos >= s.frames) {
            previewSince = -1L
            return -1L
        }
        return pos
    }

    /** A float or 16-bit stereo WAV, read a piece at a time (stems are a few dozen MB). */
    private class WavFile(val file: File) {
        val rate: Int
        val channels: Int
        val bits: Int
        val dataAt: Long
        val frames: Long

        init {
            RandomAccessFile(file, "r").use { f ->
                var pos = 12L
                var r = 0
                var c = 0
                var b = 0
                var data = -1L
                var size = 0L
                val head = ByteArray(8)
                while (pos + 8 <= f.length()) {
                    f.seek(pos)
                    f.readFully(head)
                    val id = String(head, 0, 4)
                    val n = ByteBuffer.wrap(head, 4, 4).order(ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xFFFFFFFFL
                    if (id == "fmt ") {
                        val fmt = ByteArray(16).also { f.readFully(it) }
                        val bb = ByteBuffer.wrap(fmt).order(ByteOrder.LITTLE_ENDIAN)
                        c = bb.getShort(2).toInt()
                        r = bb.getInt(4)
                        b = bb.getShort(14).toInt()
                    } else if (id == "data") {
                        data = pos + 8
                        size = n
                        break
                    }
                    pos += 8 + n + (n and 1)
                }
                require(data > 0 && r > 0 && c in 1..2 && (b == 32 || b == 16)) { "not a WAV: $file" }
                rate = r
                channels = c
                bits = b
                dataAt = data
                frames = size / (c * b / 8)
            }
        }

        /** [count] frames from [from], stereo interleaved (zeros past the end). */
        fun read(from: Long, count: Int): FloatArray {
            val out = FloatArray(2 * count)
            val have = (frames - from).coerceIn(0L, count.toLong()).toInt()
            if (have == 0 || from < 0) return out
            val width = bits / 8
            val bytes = ByteArray(have * channels * width)
            RandomAccessFile(file, "r").use { f ->
                f.seek(dataAt + from * channels * width)
                f.readFully(bytes)
            }
            val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until have) for (ch in 0..1) {
                val k = i * channels + if (channels == 2) ch else 0
                out[2 * i + ch] = if (width == 4) bb.getFloat(4 * k) else bb.getShort(2 * k) / 32768f
            }
            return out
        }
    }

    companion object {
        const val SECONDS = 3
        const val RENDER_MS = 1_200L
        private const val STEPS = 24
        private const val RATE = OfflineExport.SAMPLE_RATE
        // NativeEngine's own command numbers (its private CMD_… constants).
        private const val CMD_SET_TRANSPORT = 5
        private const val CMD_STEM_START = 32
        private const val CMD_PLAY_PREVIEW = 47

        /** [frames] of a quiet 220 Hz tone, stereo interleaved (−12 dBFS: no limiter needed). */
        fun tone(frames: Int, rate: Int): FloatArray = FloatArray(2 * frames) { i -> (0.25 * sin(2 * PI * 220.0 * (i / 2) / rate)).toFloat() }
    }
}
