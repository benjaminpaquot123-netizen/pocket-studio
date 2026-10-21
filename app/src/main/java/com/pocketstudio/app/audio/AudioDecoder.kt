package com.pocketstudio.app.audio

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.io.BufferedOutputStream
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import com.pocketstudio.model.i18n.tr

/**
 * Decodes MP3, WAV, FLAC, M4A/AAC, OGG… with Android's own codecs into float PCM
 * (more than two channels are folded to stereo). Blocking: call off the main thread.
 */
object AudioDecoder {
    /** Pads hold one-shots and loops: longer files are cut there (memory: ~23 MB per stereo minute). */
    const val MAX_SECONDS = 120

    class TooLongException(val seconds: Double) : IOException("audio too long ($seconds s)")
    class CancelledException : IOException("annulé")

    /** The file's first [MAX_SECONDS] seconds at most (a longer file is cut, never refused). */
    fun decode(context: Context, uri: Uri): Pcm {
        val out = FloatBuilder()
        val (channels, rate) = decodeInto(context, uri, out, MAX_SECONDS.toDouble(), cut = true) { false }
        if (out.size < channels) throw IOException(tr("fichier vide"))
        val keep = minOf(out.size, MAX_SECONDS.toLong() * rate * channels).toInt() / channels * channels
        return Pcm(out.toArray(keep), channels, rate)
    }

    /** An audio track holds a whole song's voice: up to 5 minutes (a longer file is cut). */
    const val TRACK_MAX_SECONDS = 300

    /** Past this, a file for an audio track is kept in mono: 5 minutes then weigh 58 MB, not 115. */
    const val TRACK_STEREO_SECONDS = 120

    /**
     * A file for an audio track: its first [TRACK_MAX_SECONDS] seconds, in mono past
     * [TRACK_STEREO_SECONDS]. Decoded through a file in [cacheDir], so only the result sits in memory.
     */
    fun decodeForTrack(context: Context, uri: Uri, cacheDir: File): Pcm {
        val raw = File(cacheDir, "track-import-${System.nanoTime()}.raw")
        try {
            val audio = decodeToFile(context, uri, raw, TRACK_MAX_SECONDS.toDouble(), { false }, cut = true)
            val frames = minOf(audio.frames, TRACK_MAX_SECONDS.toLong() * audio.sampleRate).toInt()
            val mono = audio.channels == 2 && frames > TRACK_STEREO_SECONDS.toLong() * audio.sampleRate
            return Pcm(readRaw(raw, frames, audio.channels, mono), if (mono) 1 else audio.channels, audio.sampleRate)
        } finally {
            raw.delete()
        }
    }

    /** [frames] of raw interleaved floats ([channels]), folded to mono when [mono]. */
    internal fun readRaw(file: File, frames: Int, channels: Int, mono: Boolean): FloatArray {
        val out = FloatArray(frames * if (mono) 1 else channels)
        val bytes = ByteArray(channels * 4 * 4096)
        val floats = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
        val frame = FloatArray(channels * 4096)
        java.io.DataInputStream(file.inputStream().buffered(1 shl 16)).use { input ->
            var at = 0
            while (at < frames) {
                val n = minOf(4096, frames - at)
                input.readFully(bytes, 0, n * channels * 4)
                floats.clear()
                floats.get(frame, 0, n * channels)
                if (mono) {
                    for (i in 0 until n) out[at + i] = (frame[i * 2] + frame[i * 2 + 1]) * 0.5f
                } else {
                    System.arraycopy(frame, 0, out, at * channels, n * channels)
                }
                at += n
            }
        }
        return out
    }

    /** Where a long decode went: raw interleaved little-endian floats. */
    data class RawAudio(val file: File, val channels: Int, val sampleRate: Int, val frames: Long) {
        val seconds get() = frames.toDouble() / sampleRate
    }

    /**
     * Whole songs: decodes straight into [target] (raw interleaved floats, 1 or 2 channels at the
     * file's rate) so minutes of audio never sit on the Java heap.
     */
    fun decodeToFile(context: Context, uri: Uri, target: File, maxSeconds: Double, isCancelled: () -> Boolean, cut: Boolean = false): RawAudio {
        target.parentFile?.mkdirs()
        val sink = FileSink(target)
        val (channels, rate) = try {
            sink.use { decodeInto(context, uri, it, maxSeconds, cut = cut, isCancelled = isCancelled) }
        } catch (e: Throwable) {
            target.delete()
            throw e
        }
        if (sink.size < channels) {
            target.delete()
            throw IOException(tr("fichier vide"))
        }
        return RawAudio(target, channels, rate, sink.size / channels)
    }

    private fun decodeInto(
        context: Context, uri: Uri, out: FoldingSink, maxSeconds: Double, cut: Boolean = false, isCancelled: () -> Boolean,
    ): Pair<Int, Int> {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: throw IOException(tr("pas de piste audio"))
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
            try {
                codec.configure(format, null, null, 0)
                codec.start()
                return drain(extractor, codec, format, out, maxSeconds, cut, isCancelled)
            } finally {
                runCatching { codec.stop() }
                codec.release()
            }
        } finally {
            extractor.release()
        }
    }

    private fun drain(
        extractor: MediaExtractor, codec: MediaCodec, input: MediaFormat,
        out: FoldingSink, maxSeconds: Double, cut: Boolean, isCancelled: () -> Boolean,
    ): Pair<Int, Int> {
        var rate = input.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var srcChannels = input.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        // Raw WAVs can pass straight through at 8, 24 or 32 bits: never assume 16.
        var encoding = if (input.containsKey(MediaFormat.KEY_PCM_ENCODING)) input.getInteger(MediaFormat.KEY_PCM_ENCODING)
        else AudioFormat.ENCODING_PCM_16BIT
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        while (true) {
            if (isCancelled()) throw CancelledException()
            if (!inputDone) {
                val i = codec.dequeueInputBuffer(10_000)
                if (i >= 0) {
                    val buf = codec.getInputBuffer(i)!!
                    val n = extractor.readSampleData(buf, 0)
                    if (n < 0) {
                        codec.queueInputBuffer(i, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(i, 0, n, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
            val o = codec.dequeueOutputBuffer(info, 10_000)
            when {
                o == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val f = codec.outputFormat
                    rate = f.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    srcChannels = f.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    if (f.containsKey(MediaFormat.KEY_PCM_ENCODING)) encoding = f.getInteger(MediaFormat.KEY_PCM_ENCODING)
                }
                o >= 0 -> {
                    val buf = codec.getOutputBuffer(o)!!.order(ByteOrder.LITTLE_ENDIAN)
                    buf.position(info.offset)
                    buf.limit(info.offset + info.size)
                    out.appendFolded(buf, encoding, srcChannels)
                    codec.releaseOutputBuffer(o, false)
                    val seconds = out.size.toDouble() / minOf(srcChannels, 2) / rate
                    if (seconds > maxSeconds) {
                        if (cut) break
                        throw TooLongException(seconds)
                    }
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                }
            }
        }
        return minOf(srcChannels, 2) to rate
    }

    /** Receives decoded samples; folds more than two channels into stereo (odd ones left, even ones right). */
    private abstract class FoldingSink {
        /** Samples received (all channels). */
        var size = 0L
            protected set

        protected abstract fun add(v: Float)

        fun appendFolded(buf: java.nio.ByteBuffer, encoding: Int, channels: Int) {
            val frame = FloatArray(channels)
            while (true) {
                for (c in 0 until channels) {
                    frame[c] = when (encoding) {
                        AudioFormat.ENCODING_PCM_FLOAT -> if (buf.remaining() >= 4) buf.float else return
                        AudioFormat.ENCODING_PCM_8BIT -> if (buf.remaining() >= 1) ((buf.get().toInt() and 0xFF) - 128) / 128f else return
                        AudioFormat.ENCODING_PCM_24BIT_PACKED -> if (buf.remaining() >= 3) {
                            val v = (buf.get().toInt() and 0xFF) or ((buf.get().toInt() and 0xFF) shl 8) or (buf.get().toInt() shl 16)
                            v / 8388608f
                        } else return
                        AudioFormat.ENCODING_PCM_32BIT -> if (buf.remaining() >= 4) buf.int / 2147483648f else return
                        else -> if (buf.remaining() >= 2) buf.short / 32768f else return
                    }
                }
                when {
                    channels <= 2 -> frame.forEach(::add)
                    else -> {
                        var l = 0f; var r = 0f
                        for (c in 0 until channels) if (c % 2 == 0) l += frame[c] else r += frame[c]
                        val k = 2f / channels
                        add(l * k); add(r * k)
                    }
                }
            }
        }

    }

    private class FloatBuilder : FoldingSink() {
        var data = FloatArray(1 shl 16)

        override fun add(v: Float) {
            if (size == data.size.toLong()) data = data.copyOf(data.size * 2)
            data[size.toInt()] = v
            size++
        }

        fun toArray(limit: Int = size.toInt()) = data.copyOf(minOf(limit, size.toInt()))
    }

    private class FileSink(file: File) : FoldingSink(), Closeable {
        private val out = BufferedOutputStream(FileOutputStream(file), 1 shl 16)
        private val bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)

        override fun add(v: Float) {
            bytes.clear()
            bytes.putFloat(v)
            out.write(bytes.array(), 0, 4)
            size++
        }

        override fun close() = out.close()
    }
}
