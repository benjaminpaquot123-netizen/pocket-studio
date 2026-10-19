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

/**
 * Decodes MP3, WAV, FLAC, M4A/AAC, OGG… with Android's own codecs into float PCM
 * (more than two channels are folded to stereo). Blocking: call off the main thread.
 */
object AudioDecoder {
    /** Pads hold one-shots and short loops: longer files are cut (memory: ~23 MB per stereo minute). */
    const val MAX_SECONDS = 60

    class TooLongException(val seconds: Double) : IOException("audio too long ($seconds s)")
    class CancelledException : IOException("annulé")

    fun decode(context: Context, uri: Uri): Pcm {
        val out = FloatBuilder()
        val (channels, rate) = decodeInto(context, uri, out, MAX_SECONDS.toDouble()) { false }
        if (out.size < channels) throw IOException("fichier vide")
        return Pcm(out.toArray(), channels, rate)
    }

    /** Where a long decode went: raw interleaved little-endian floats. */
    data class RawAudio(val file: File, val channels: Int, val sampleRate: Int, val frames: Long) {
        val seconds get() = frames.toDouble() / sampleRate
    }

    /**
     * Whole songs: decodes straight into [target] (raw interleaved floats, 1 or 2 channels at the
     * file's rate) so minutes of audio never sit on the Java heap.
     */
    fun decodeToFile(context: Context, uri: Uri, target: File, maxSeconds: Double, isCancelled: () -> Boolean): RawAudio {
        target.parentFile?.mkdirs()
        val sink = FileSink(target)
        val (channels, rate) = try {
            sink.use { decodeInto(context, uri, it, maxSeconds, isCancelled) }
        } catch (e: Throwable) {
            target.delete()
            throw e
        }
        if (sink.size < channels) {
            target.delete()
            throw IOException("fichier vide")
        }
        return RawAudio(target, channels, rate, sink.size / channels)
    }

    private fun decodeInto(context: Context, uri: Uri, out: FoldingSink, maxSeconds: Double, isCancelled: () -> Boolean): Pair<Int, Int> {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: throw IOException("pas de piste audio")
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
            try {
                codec.configure(format, null, null, 0)
                codec.start()
                return drain(extractor, codec, format, out, maxSeconds, isCancelled)
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
        out: FoldingSink, maxSeconds: Double, isCancelled: () -> Boolean,
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
                    if (seconds > maxSeconds) throw TooLongException(seconds)
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

        fun toArray() = data.copyOf(size.toInt())
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
