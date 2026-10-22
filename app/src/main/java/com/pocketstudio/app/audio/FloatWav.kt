package com.pocketstudio.app.audio

import java.io.DataInputStream
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import com.pocketstudio.model.i18n.tr

/** Decoded audio: interleaved float samples. */
class Pcm(val data: FloatArray, val channels: Int, val sampleRate: Int) {
    val frames: Int get() = data.size / channels
    val seconds: Double get() = frames.toDouble() / sampleRate

    /**
     * Without the silence after the sound (under -60 dB of its peak, 10 ms kept), for one-shots
     * of at most [maxSeconds]: the end of a pad then cuts what is heard from its first move.
     */
    fun withoutTail(maxSeconds: Double = 3.0): Pcm {
        if (seconds > maxSeconds || frames == 0) return this
        var peak = 0f
        for (x in data) peak = maxOf(peak, kotlin.math.abs(x))
        if (peak <= 0f) return this
        val floor = peak * 0.001f
        var last = frames - 1
        while (last > 0 && (0 until channels).all { c -> kotlin.math.abs(data[last * channels + c]) < floor }) last--
        val keep = (last + 1 + sampleRate / 100).coerceAtMost(frames)
        return if (keep >= frames) this else Pcm(data.copyOf(keep * channels), channels, sampleRate)
    }
}

/**
 * How a pad's sound is kept in the project folder: 32-bit float WAV, lossless and quick
 * to read back. Written to a temporary file first, then renamed.
 */
object FloatWav {
    fun write(file: File, pcm: Pcm) = write(file, pcm.data, 0, pcm.data.size / pcm.channels, pcm.channels, pcm.sampleRate)

    /**
     * Frames [from] until [to] of [data] ([channels] interleaved) as a WAV, written in small pieces:
     * minutes of sound never need a second copy in memory.
     */
    fun write(file: File, data: FloatArray, from: Int, to: Int, channels: Int, sampleRate: Int) {
        val samples = (to - from) * channels
        val dataBytes = samples * 4
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + dataBytes); put("WAVE".toByteArray())
            put("fmt ".toByteArray()); putInt(16); putShort(3)  // IEEE float
            putShort(channels.toShort()); putInt(sampleRate)
            putInt(sampleRate * channels * 4); putShort((channels * 4).toShort()); putShort(32)
            put("data".toByteArray()); putInt(dataBytes)
        }
        val chunk = ByteBuffer.allocate(CHUNK * 4).order(ByteOrder.LITTLE_ENDIAN)
        val floats = chunk.asFloatBuffer()
        val tmp = File(file.parentFile, file.name + ".tmp")
        try {
            java.io.FileOutputStream(tmp).use { out ->
                out.write(header.array())
                var at = from * channels
                val end = at + samples
                while (at < end) {
                    val n = minOf(CHUNK, end - at)
                    floats.clear()
                    floats.put(data, at, n)
                    out.write(chunk.array(), 0, n * 4)
                    at += n
                }
                out.fd.sync()
            }
            if (!tmp.renameTo(file)) throw IOException(tr("écriture impossible : %s", file.name))
        } catch (e: Throwable) {
            // A take that failed (a full phone) leaves no half file behind: it would take the room again.
            tmp.delete()
            throw e
        }
    }

    /** Reads files written by [write] (and other plain 32-bit float WAVs), a piece at a time. */
    fun read(file: File): Pcm {
        val length = file.length()
        DataInputStream(file.inputStream().buffered(1 shl 16)).use { input ->
            val head = ByteArray(12)
            if (length < 44 || input.read(head) != 12 || String(head, 0, 4) != "RIFF" || String(head, 8, 4) != "WAVE") throw IOException("not a WAV")
            var pos = 12L
            var channels = 0
            var rate = 0
            var format = 0
            var bits = 0
            val chunkHead = ByteArray(8)
            while (pos + 8 <= length) {
                input.readFully(chunkHead)
                val id = String(chunkHead, 0, 4)
                val size = ByteBuffer.wrap(chunkHead, 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
                if (size < 0 || pos + 8 + size > length) throw IOException("bad chunk $id")
                when (id) {
                    "fmt " -> {
                        if (size < 16) throw IOException("bad chunk $id")
                        val fmt = ByteArray(size).also { input.readFully(it) }
                        val b = ByteBuffer.wrap(fmt).order(ByteOrder.LITTLE_ENDIAN)
                        format = b.getShort(0).toInt()
                        channels = b.getShort(2).toInt()
                        rate = b.getInt(4)
                        bits = b.getShort(14).toInt()
                        if (size and 1 == 1) input.skipBytes(1)
                    }
                    "data" -> {
                        if (format != 3 || bits != 32 || channels !in 1..2 || rate <= 0) throw IOException("unsupported WAV")
                        val out = FloatArray(size / 4)
                        val bytes = ByteArray(CHUNK * 4)
                        val view = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
                        var at = 0
                        while (at < out.size) {
                            val n = minOf(CHUNK, out.size - at)
                            input.readFully(bytes, 0, n * 4)
                            view.clear()
                            view.get(out, at, n)
                            at += n
                        }
                        return Pcm(out, channels, rate)
                    }
                    else -> {
                        var left = size + (size and 1)
                        while (left > 0) {
                            val skipped = input.skipBytes(left)
                            if (skipped <= 0) throw IOException("bad chunk $id")
                            left -= skipped
                        }
                    }
                }
                pos += 8 + size + (size and 1)
            }
            throw IOException("no data chunk")
        }
    }

    /** Floats written or read at a time (256 kB). */
    private const val CHUNK = 1 shl 16
}
