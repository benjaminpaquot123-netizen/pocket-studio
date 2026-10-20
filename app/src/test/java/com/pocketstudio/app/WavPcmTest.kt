package com.pocketstudio.app

import com.pocketstudio.app.export.WavPcm
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files

class WavPcmTest {
    /** A 24-bit stereo WAV with an extra chunk before the data, like many tools write. */
    private fun wav24(samples: IntArray): File {
        val data = ByteBuffer.allocate(samples.size * 3).order(ByteOrder.LITTLE_ENDIAN)
        samples.forEach { v -> data.put((v and 0xFF).toByte()).put((v shr 8 and 0xFF).toByte()).put((v shr 16 and 0xFF).toByte()) }
        val b = ByteBuffer.allocate(12 + 24 + 12 + 8 + data.capacity()).order(ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray()).putInt(b.capacity() - 8).put("WAVE".toByteArray())
        b.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(2).putInt(48000).putInt(48000 * 6).putShort(6).putShort(24)
        b.put("LIST".toByteArray()).putInt(4).put("INFO".toByteArray())
        b.put("data".toByteArray()).putInt(data.capacity()).put(data.array())
        return Files.createTempFile("pcm", ".wav").toFile().apply { writeBytes(b.array()) }
    }

    @Test
    fun readsTwentyFourBitsAsSixteen() {
        val f = wav24(intArrayOf(8388607, -8388608, 4194304, 0, -1, 256))
        WavPcm.open(f).use { pcm ->
            assertEquals(2, pcm.channels)
            assertEquals(48000, pcm.sampleRate)
            val out = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN)
            assertEquals(3, pcm.read16(out, 10))
            out.flip()
            assertEquals(listOf(32766, -32767, 16383, 0, 0, 0), List(6) { out.short.toInt() })
            out.clear()
            assertEquals(0, pcm.read16(out, 10))  // the end
        }
        f.delete()
    }
}
