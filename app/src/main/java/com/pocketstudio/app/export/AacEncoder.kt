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

/**
 * Turns one of our 24-bit stereo WAV exports into an M4A (AAC-LC) with Android's own encoder:
 * about ten times smaller, read by every phone and computer. Blocking: call off the main thread.
 */
object AacEncoder {
    const val BIT_RATE = 256_000

    fun encode(wav: File, out: File, isCancelled: () -> Boolean = { false }) {
        val pcm = WavPcm.open(wav)
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, pcm.sampleRate, pcm.channels).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16 * 1024)
        }
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        val muxer = MediaMuxer(out.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var track = -1
        var muxing = false
        try {
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            val info = MediaCodec.BufferInfo()
            val frameBytes = 2 * pcm.channels  // 16-bit PCM in
            var framesIn = 0L
            var inputDone = false
            while (true) {
                if (isCancelled()) throw IOException("annulé")
                if (!inputDone) {
                    val i = codec.dequeueInputBuffer(10_000)
                    if (i >= 0) {
                        val buf = codec.getInputBuffer(i)!!.order(ByteOrder.LITTLE_ENDIAN)
                        buf.clear()
                        val frames = pcm.read16(buf, buf.capacity() / frameBytes)
                        val timeUs = framesIn * 1_000_000L / pcm.sampleRate
                        if (frames == 0) {
                            codec.queueInputBuffer(i, 0, 0, timeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(i, 0, frames * frameBytes, timeUs, 0)
                            framesIn += frames
                        }
                    }
                }
                val o = codec.dequeueOutputBuffer(info, 10_000)
                when {
                    o == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        track = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                        muxing = true
                    }
                    o >= 0 -> {
                        val data = codec.getOutputBuffer(o)!!
                        if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0  // already in the format
                        if (info.size > 0 && muxing) {
                            data.position(info.offset)
                            data.limit(info.offset + info.size)
                            muxer.writeSampleData(track, data, info)
                        }
                        codec.releaseOutputBuffer(o, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                    }
                }
            }
        } catch (e: Exception) {
            out.delete()
            throw e
        } finally {
            pcm.close()
            runCatching { codec.stop() }
            codec.release()
            runCatching { if (muxing) muxer.stop() }
            muxer.release()
        }
    }
}

/** Reads the PCM samples of a WAV file (16/24/32-bit integer or 32-bit float), as 16-bit. */
class WavPcm private constructor(private val file: RandomAccessFile, val channels: Int, val sampleRate: Int, private val bits: Int, private val float: Boolean, dataBytes: Long) : AutoCloseable {
    private var remaining = dataBytes
    private val bytesPerSample = bits / 8
    private val chunk = ByteArray(16 * 1024 * 4)

    /** Writes up to [maxFrames] frames into [out] as 16-bit little-endian; returns the frames written (0 at the end). */
    fun read16(out: ByteBuffer, maxFrames: Int): Int {
        val frameBytes = bytesPerSample * channels
        val frames = minOf(maxFrames.toLong(), remaining / frameBytes, (chunk.size / frameBytes).toLong()).toInt()
        if (frames <= 0) return 0
        val n = frames * frameBytes
        file.readFully(chunk, 0, n)
        remaining -= n
        val src = ByteBuffer.wrap(chunk, 0, n).order(ByteOrder.LITTLE_ENDIAN)
        repeat(frames * channels) {
            val v: Float = when {
                float -> src.float
                bits == 16 -> src.short / 32768f
                bits == 24 -> ((src.get().toInt() and 0xFF) or ((src.get().toInt() and 0xFF) shl 8) or (src.get().toInt() shl 16)) / 8388608f
                else -> src.int / 2147483648f
            }
            out.putShort((v.coerceIn(-1f, 1f) * 32767f).toInt().toShort())
        }
        return frames
    }

    override fun close() = file.close()

    companion object {
        fun open(wav: File): WavPcm {
            val f = RandomAccessFile(wav, "r")
            try {
                val header = ByteArray(12)
                f.readFully(header)
                if (String(header, 0, 4) != "RIFF" || String(header, 8, 4) != "WAVE") throw IOException("pas un WAV")
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
                        if (channels !in 1..2 || rate <= 0 || bits !in listOf(16, 24, 32)) throw IOException("WAV non pris en charge")
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
