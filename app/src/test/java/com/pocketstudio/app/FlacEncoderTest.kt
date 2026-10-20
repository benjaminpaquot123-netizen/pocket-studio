package com.pocketstudio.app

import com.pocketstudio.app.export.FlacEncoder
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

class FlacEncoderTest {
    /** A stereo WAV: a sine on the left, noise on the right, a length that is not a whole block. */
    private fun wav(frames: Int, file: File, bits: Int = 24): File {
        val bytes = bits / 8
        val data = ByteBuffer.allocate(frames * 2 * bytes).order(ByteOrder.LITTLE_ENDIAN)
        val rng = java.util.Random(3)
        for (i in 0 until frames) {
            // The second block is digital silence, as in the tail of an export.
            val silent = i in 4096 until 8192
            val left = if (silent) 0 else (sin(i * 0.05) * 4_000_000).toInt()
            val right = if (silent) 0 else rng.nextInt(1 shl 20) - (1 shl 19)
            for (v in listOf(left, right)) {
                val sample = v shr (24 - bits)
                for (b in 0 until bytes) data.put((sample shr (8 * b) and 0xFF).toByte())
            }
        }
        val b = ByteBuffer.allocate(44 + data.capacity()).order(ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray()).putInt(b.capacity() - 8).put("WAVE".toByteArray())
        b.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(2).putInt(48000).putInt(48000 * 2 * bytes)
            .putShort((2 * bytes).toShort()).putShort(bits.toShort())
        b.put("data".toByteArray()).putInt(data.capacity()).put(data.array())
        return file.apply { writeBytes(b.array()) }
    }

    @Test
    fun writesAValidStream() {
        // Kept in build/ so the decode can also be checked by hand with any FLAC reader.
        val dir = File("build/flac-test").apply { mkdirs() }
        val frames = 4096 * 3 + 1234
        val wav = wav(frames, File(dir, "in.wav"))
        val flac = File(dir, "out.flac")
        FlacEncoder.encode(wav, flac)
        val bytes = flac.readBytes()
        assertArrayEquals("fLaC".toByteArray(), bytes.copyOfRange(0, 4))
        // STREAMINFO: last block, 34 bytes, 48 kHz, 2 channels, 24 bits, the frame count.
        assertEquals(0x80, bytes[4].toInt() and 0xFF)
        val info = ByteBuffer.wrap(bytes, 8, 34)
        info.position(8 + 10)
        val packed = info.long
        assertEquals(48000L, packed ushr 44)
        assertEquals(1L, (packed ushr 41) and 7)
        assertEquals(23L, (packed ushr 36) and 31)
        assertEquals(frames.toLong(), packed and 0xFFFFFFFFFL)
        // First frame: sync code, 4 header bytes and frame number 0, then the CRC-8 of those 5 bytes.
        assertEquals(0xFF, bytes[42].toInt() and 0xFF)
        assertEquals(0xF8, bytes[43].toInt() and 0xFF)
        assertEquals(0, bytes[46].toInt())
        assertEquals(FlacEncoder.crc8(bytes.copyOfRange(42, 47)), bytes[47].toInt() and 0xFF)
        // Lossless and compressed: smaller than the WAV (the sine side predicts well).
        assertTrue(flac.length() < wav.length())
    }

    @Test
    fun sixteenBitStaysSixteenBit() {
        val dir = File("build/flac-test").apply { mkdirs() }
        val wav = wav(5000, File(dir, "in16.wav"), bits = 16)
        val flac = File(dir, "out16.flac")
        FlacEncoder.encode(wav, flac)
        val packed = ByteBuffer.wrap(flac.readBytes(), 18, 8).long
        assertEquals(15L, (packed ushr 36) and 31)
        assertEquals(5000L, packed and 0xFFFFFFFFFL)
        assertTrue(flac.length() < wav.length())
    }

    @Test
    fun crcsMatchTheReferenceValues() {
        // Check values of the CRC-8 (poly 0x07) and CRC-16 (poly 0x8005) used by FLAC.
        assertEquals(0xF4, FlacEncoder.crc8("123456789".toByteArray()))
        assertEquals(0xFEE8, FlacEncoder.crc16("123456789".toByteArray()))
    }
}
