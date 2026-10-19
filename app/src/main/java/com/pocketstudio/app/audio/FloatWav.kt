package com.pocketstudio.app.audio

import java.io.DataInputStream
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Decoded audio: interleaved float samples. */
class Pcm(val data: FloatArray, val channels: Int, val sampleRate: Int) {
    val frames: Int get() = data.size / channels
    val seconds: Double get() = frames.toDouble() / sampleRate
}

/**
 * How a pad's sound is kept in the project folder: 32-bit float WAV, lossless and quick
 * to read back. Written to a temporary file first, then renamed.
 */
object FloatWav {
    fun write(file: File, pcm: Pcm) {
        val dataBytes = pcm.data.size * 4
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + dataBytes); put("WAVE".toByteArray())
            put("fmt ".toByteArray()); putInt(16); putShort(3)  // IEEE float
            putShort(pcm.channels.toShort()); putInt(pcm.sampleRate)
            putInt(pcm.sampleRate * pcm.channels * 4); putShort((pcm.channels * 4).toShort()); putShort(32)
            put("data".toByteArray()); putInt(dataBytes)
        }
        val body = ByteBuffer.allocate(dataBytes).order(ByteOrder.LITTLE_ENDIAN)
        body.asFloatBuffer().put(pcm.data)
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.outputStream().use { out ->
            out.write(header.array())
            out.write(body.array())
            out.fd.sync()
        }
        if (!tmp.renameTo(file)) throw IOException("rename failed: $file")
    }

    /** Reads files written by [write] (and other plain 32-bit float WAVs). */
    fun read(file: File): Pcm {
        DataInputStream(file.inputStream().buffered()).use { input ->
            val bytes = input.readBytes()
            val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            if (bytes.size < 44 || String(bytes, 0, 4) != "RIFF" || String(bytes, 8, 4) != "WAVE") throw IOException("not a WAV")
            var pos = 12
            var channels = 0
            var rate = 0
            var format = 0
            var bits = 0
            while (pos + 8 <= bytes.size) {
                val id = String(bytes, pos, 4)
                val size = b.getInt(pos + 4)
                if (size < 0 || pos + 8 + size > bytes.size) throw IOException("bad chunk $id")
                when (id) {
                    "fmt " -> {
                        format = b.getShort(pos + 8).toInt()
                        channels = b.getShort(pos + 10).toInt()
                        rate = b.getInt(pos + 12)
                        bits = b.getShort(pos + 22).toInt()
                    }
                    "data" -> {
                        if (format != 3 || bits != 32 || channels !in 1..2 || rate <= 0) throw IOException("unsupported WAV")
                        val out = FloatArray(size / 4)
                        b.position(pos + 8)
                        b.slice().order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(out)
                        return Pcm(out, channels, rate)
                    }
                }
                pos += 8 + size + (size and 1)
            }
            throw IOException("no data chunk")
        }
    }
}
