package com.pocketstudio.app.export

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import com.pocketstudio.model.i18n.tr

/**
 * Turns one of our 24-bit stereo WAV exports into an M4A (AAC-LC) with Android's own encoder:
 * about ten times smaller, read by every phone and computer. Blocking: call off the main thread.
 */
object AacEncoder {
    const val BIT_RATE = 256_000
    /**
     * The true peak an M4A's source is kept under (dBTP): AAC rebuilds the wave up to about 1 dB
     * over its source (+0.4 to +1.1 dB measured at 256 kbps on loud songs, phase 33), so −2 keeps
     * the decoded file under −1 dBTP.
     */
    const val CEILING_DBTP = -2f

    /** The gain that brings a source with this true peak (dBTP) under [CEILING_DBTP]; 1 when it already is. */
    fun headroomGain(truePeakDb: Double?, ceiling: Float = CEILING_DBTP): Float =
        if (truePeakDb == null || truePeakDb <= ceiling) 1f else Math.pow(10.0, (ceiling - truePeakDb) / 20.0).toFloat()

    /**
     * [gain]: applied to every sample on the way in (see [headroomGain]). [progress] goes from 0
     * to 1 over the file. Never stuck ([CodecPump]): a codec that stops answering fails the step.
     */
    fun encode(wav: File, out: File, gain: Float = 1f, isCancelled: () -> Boolean = { false }, progress: (Float) -> Unit = {}) {
        val pcm = WavPcm.open(wav)
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, pcm.sampleRate, pcm.channels).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16 * 1024)
        }
        // Each one let go when the next fails to start (review m8: nothing left open before the try).
        val codec = try {
            MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        } catch (e: Exception) {
            pcm.close()
            throw e
        }
        val muxer = try {
            MediaMuxer(out.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        } catch (e: Exception) {
            pcm.close()
            codec.release()
            throw e
        }
        // Started or not, whatever the loop threw: the finally stops the writer only when it started.
        val mux = object : MuxSink {
            var track = -1
            var started = false
            override fun addTrack(format: MediaFormat) = muxer.addTrack(format).also { track = it }
            override fun start() {
                muxer.start()
                started = true
            }
            override fun write(data: ByteBuffer, info: MediaCodec.BufferInfo) = muxer.writeSampleData(track, data, info)
        }
        try {
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            pump(CodecPort.of(codec), pcm, gain, isCancelled, progress, { i -> codec.getInputBuffer(i)!! }, mux)
        } catch (e: Exception) {
            out.delete()
            throw e
        } finally {
            pcm.close()
            runCatching { codec.stop() }
            codec.release()
            // A failed stop leaves an unreadable file: that is an error, not a success.
            val stopped = runCatching { if (mux.started) muxer.stop() }
            runCatching { muxer.release() }
            if (stopped.isFailure && out.exists()) {
                out.delete()
                throw IOException(tr("fichier M4A incomplet"))
            }
        }
    }

    /** Where the encoded frames go: the MP4 writer, or a recorder in the tests. */
    internal interface MuxSink {
        fun addTrack(format: MediaFormat): Int
        fun start()
        fun write(data: ByteBuffer, info: MediaCodec.BufferInfo)
    }

    /**
     * The encoding loop on any codec ([CodecPump]): [pcm] in (times [gain], dithered to 16 bits),
     * the AAC frames out to [sink]. [input] gives input buffer `i`. True once the sink started.
     */
    internal fun pump(
        codec: CodecPort, pcm: WavPcm, gain: Float, isCancelled: () -> Boolean, progress: (Float) -> Unit,
        input: (Int) -> ByteBuffer, sink: MuxSink, now: () -> Long = CodecPump::clockMs,
    ): Boolean {
        val frameBytes = 2 * pcm.channels  // 16-bit PCM in
        val total = pcm.framesLeft.coerceAtLeast(1)
        var framesIn = 0L
        var muxing = false
        var shown = -1
        CodecPump.run(
            codec,
            feed = { i ->
                val buf = input(i).order(ByteOrder.LITTLE_ENDIAN)
                buf.clear()
                val frames = pcm.read16(buf, buf.capacity() / frameBytes, gain, dithered = true)
                val timeUs = framesIn * 1_000_000L / pcm.sampleRate
                framesIn += frames
                val p = (framesIn * 100 / total).toInt().coerceIn(0, 100)
                if (p != shown) { shown = p; progress(p / 100f) }
                CodecPump.Fed(frames * frameBytes, timeUs, end = frames == 0)
            },
            format = { f ->
                if (!muxing) {
                    sink.addTrack(f)
                    sink.start()
                    muxing = true
                }
            },
            output = { data, info ->
                if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0  // already in the format
                if (info.size > 0 && muxing) sink.write(data, info)
                false
            },
            isCancelled = isCancelled,
            now = now,
        )
        return muxing
    }
}

/** Reads the PCM samples of a WAV file (16/24/32-bit integer or 32-bit float), as 16-bit. */
class WavPcm private constructor(private val file: RandomAccessFile, val channels: Int, val sampleRate: Int, val bits: Int, private val float: Boolean, dataBytes: Long) : AutoCloseable {
    private var remaining = dataBytes
    private val bytesPerSample = bits / 8
    private val chunk = ByteArray(16 * 1024 * 4)

    /** Frames left to read. */
    val framesLeft: Long get() = remaining / (bytesPerSample * channels)

    /** Up to [maxFrames] frames into [out] as interleaved floats (-1..1); returns the frames read (0 at the end). */
    fun readFloat(out: FloatArray, maxFrames: Int): Int {
        val frameBytes = bytesPerSample * channels
        val frames = minOf(maxFrames.toLong(), remaining / frameBytes, (chunk.size / frameBytes).toLong(), (out.size / channels).toLong()).toInt()
        if (frames <= 0) return 0
        val n = frames * frameBytes
        file.readFully(chunk, 0, n)
        remaining -= n
        val src = ByteBuffer.wrap(chunk, 0, n).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until frames * channels) out[i] = sample(src)
        return frames
    }

    private fun sample(src: ByteBuffer): Float = when {
        float -> src.float
        bits == 16 -> src.short / 32768f
        bits == 24 -> ((src.get().toInt() and 0xFF) or ((src.get().toInt() and 0xFF) shl 8) or (src.get().toInt() shl 16)) / 8388608f
        else -> src.int / 2147483648f
    }

    /**
     * Writes up to [maxFrames] frames into [out] as 16-bit little-endian, times [gain]; [dithered]:
     * with a triangular dither of one step (no truncation grain in fades). Returns the frames
     * written (0 at the end).
     */
    fun read16(out: ByteBuffer, maxFrames: Int, gain: Float = 1f, dithered: Boolean = false): Int {
        val frameBytes = bytesPerSample * channels
        val frames = minOf(maxFrames.toLong(), remaining / frameBytes, (chunk.size / frameBytes).toLong()).toInt()
        if (frames <= 0) return 0
        val n = frames * frameBytes
        file.readFully(chunk, 0, n)
        remaining -= n
        val src = ByteBuffer.wrap(chunk, 0, n).order(ByteOrder.LITTLE_ENDIAN)
        repeat(frames * channels) {
            if (dithered) {
                out.putShort(Math.round(sample(src) * gain * 32767f + dither()).coerceIn(-32768, 32767).toShort())
            } else {
                out.putShort(((sample(src) * gain).coerceIn(-1f, 1f) * 32767f).toInt().toShort())
            }
        }
        return frames
    }

    private var seed = 0x9E3779B9.toInt()
    private fun uniform(): Float {
        seed = seed xor (seed shl 13)
        seed = seed xor (seed ushr 17)
        seed = seed xor (seed shl 5)
        return (seed.toLong() and 0xFFFFFFFFL).toFloat() / 4294967296f
    }
    private fun dither(): Float = uniform() - uniform()

    override fun close() = file.close()

    companion object {
        fun open(wav: File): WavPcm {
            val f = RandomAccessFile(wav, "r")
            try {
                val header = ByteArray(12)
                f.readFully(header)
                if (String(header, 0, 4) != "RIFF" || String(header, 8, 4) != "WAVE") throw IOException(tr("pas un WAV"))
                var channels = 0
                var rate = 0
                var bits = 0
                var float = false
                val tag = ByteArray(8)
                while (true) {
                    f.readFully(tag)
                    val id = String(tag, 0, 4)
                    val size = ByteBuffer.wrap(tag, 4, 4).order(ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xFFFFFFFFL
                    if (id == "fmt ") {
                        val fmt = ByteArray(size.toInt())
                        f.readFully(fmt)
                        val b = ByteBuffer.wrap(fmt).order(ByteOrder.LITTLE_ENDIAN)
                        val code = b.short.toInt()
                        channels = b.short.toInt()
                        rate = b.int
                        b.int; b.short
                        bits = b.short.toInt()
                        float = code == 3
                        if (size % 2 == 1L) f.skipBytes(1)
                    } else if (id == "data") {
                        if (channels !in 1..2 || rate <= 0 || bits !in listOf(16, 24, 32)) throw IOException(tr("WAV non pris en charge"))
                        return WavPcm(f, channels, rate, bits, float, size)
                    } else {
                        f.skipBytes((size + (size and 1)).toInt())
                    }
                }
            } catch (e: Exception) {
                f.close()
                throw if (e is IOException) e else IOException(e.message)
            }
        }
    }
}
