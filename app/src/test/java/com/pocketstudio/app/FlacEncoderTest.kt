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
    private val dir = File("build/flac-test").apply { mkdirs() }

    /** Interleaved integer samples as a PCM WAV of [bits] (16, 24 or 32), or float when [float]. */
    private fun wav(name: String, samples: Array<LongArray>, bits: Int, float: Boolean = false): File {
        val channels = samples.size
        val frames = samples[0].size
        val bytes = bits / 8
        val data = ByteBuffer.allocate(frames * channels * bytes).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until frames) for (c in 0 until channels) {
            val v = samples[c][i]
            if (float) data.putFloat(v / 8388608f) else for (b in 0 until bytes) data.put((v shr (8 * b) and 0xFF).toByte())
        }
        val b = ByteBuffer.allocate(44 + data.capacity()).order(ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray()).putInt(b.capacity() - 8).put("WAVE".toByteArray())
        b.put("fmt ".toByteArray()).putInt(16).putShort(if (float) 3 else 1).putShort(channels.toShort()).putInt(48000)
            .putInt(48000 * channels * bytes).putShort((channels * bytes).toShort()).putShort(bits.toShort())
        b.put("data".toByteArray()).putInt(data.capacity()).put(data.array())
        return File(dir, "$name.wav").apply { writeBytes(b.array()) }
    }

    /** A sine on the left, noise on the right, silence in the second block, at [peak]. */
    private fun music(frames: Int, peak: Long, channels: Int = 2): Array<LongArray> {
        val rng = java.util.Random(3)
        return Array(channels) { c ->
            LongArray(frames) { i ->
                when {
                    i in 4096 until 8192 -> 0L
                    c == 0 -> (sin(i * 0.05) * peak * 0.5).toLong()
                    else -> (rng.nextDouble() * 2 - 1).times(peak / 4).toLong() + (sin(i * 0.01) * peak * 0.4).toLong()
                }
            }
        }
    }

    private fun roundTrip(name: String, samples: Array<LongArray>, bits: Int, float: Boolean = false): Decoded {
        val source = wav(name, samples, bits, float)
        val flac = File(dir, "$name.flac")
        var lastProgress = 0f
        FlacEncoder.encode(source, flac, onProgress = { lastProgress = it })
        val decoded = FlacDecoder(flac.readBytes()).decode()
        assertEquals(samples.size, decoded.channels)
        assertEquals(samples[0].size, decoded.samples[0].size)
        if (samples[0].size > 32 * 4096) assertTrue(lastProgress > 0.5f)
        return decoded
    }

    @Test
    fun sixteenAndTwentyFourBitsComeBackBitForBit() {
        for ((bits, peak) in listOf(16 to 32767L, 24 to 8388607L)) {
            for (frames in listOf(1, 5, 4095, 4096, 4097, 4096 * 3 + 1234)) {
                val x = music(frames, peak)
                val d = roundTrip("s$bits-$frames", x, bits)
                assertEquals(bits, d.bits)
                for (c in 0 until 2) assertArrayEquals("$bits bits, $frames frames, channel $c", x[c], d.samples[c])
            }
        }
    }

    @Test
    fun monoLoudAndCorrelatedStereoComeBackBitForBit() {
        val mono = music(9000, 32767L, channels = 1)
        assertArrayEquals(mono[0], roundTrip("mono", mono, 16).samples[0])
        // Full-scale square and both channels alike: side and mid channels at their widest.
        val square = LongArray(10000) { if ((it / 50) % 2 == 0) 8388607L else -8388608L }
        val stereo = arrayOf(square, LongArray(square.size) { -square[it] - 1 })
        val d = roundTrip("square", stereo, 24)
        assertArrayEquals(stereo[0], d.samples[0])
        assertArrayEquals(stereo[1], d.samples[1])
        val same = arrayOf(music(9000, 8388607L)[0], music(9000, 8388607L)[0])
        val ds = roundTrip("same", same, 24)
        assertArrayEquals(same[1], ds.samples[1])
    }

    @Test
    fun floatAndThirtyTwoBitSourcesBecomeTwentyFourBits() {
        val x = music(6000, 8388607L)
        val d = roundTrip("float", x, 32, float = true)
        assertEquals(24, d.bits)
        for (c in 0 until 2) for (i in x[c].indices) assertTrue(kotlin.math.abs(x[c][i] - d.samples[c][i]) <= 1)
        val i32 = Array(2) { c -> LongArray(6000) { x[c][it] shl 8 } }
        val d32 = roundTrip("int32", i32, 32)
        for (c in 0 until 2) for (i in x[c].indices) assertTrue(kotlin.math.abs(x[c][i] - d32.samples[c][i]) <= 1)
    }

    @Test
    fun itIsSmallerThanTheWav() {
        val x = music(4096 * 20, 32767L)
        val source = wav("size", x, 16)
        val flac = File(dir, "size.flac")
        FlacEncoder.encode(source, flac)
        assertTrue(flac.length() < source.length() * 0.75)
    }

    @Test
    fun crcsMatchTheReferenceValues() {
        // Check values of the CRC-8 (poly 0x07) and CRC-16 (poly 0x8005) used by FLAC.
        assertEquals(0xF4, FlacEncoder.crc8("123456789".toByteArray()))
        assertEquals(0xFEE8, FlacEncoder.crc16("123456789".toByteArray()))
    }

    class Decoded(val channels: Int, val bits: Int, val samples: Array<LongArray>)

    /** Enough of a FLAC decoder to check our files: CONSTANT, VERBATIM, FIXED, Rice and both CRCs. */
    private class FlacDecoder(private val data: ByteArray) {
        private var bit = 0L

        private fun read(n: Int): Long {
            var v = 0L
            repeat(n) {
                val byte = data[(bit ushr 3).toInt()].toInt()
                v = (v shl 1) or ((byte shr (7 - (bit and 7).toInt())) and 1).toLong()
                bit++
            }
            return v
        }

        private fun signed(n: Int): Long {
            val v = read(n)
            return if (n > 0 && v and (1L shl (n - 1)) != 0L) v - (1L shl n) else v
        }

        private fun unary(): Long {
            var q = 0L
            while (read(1) == 0L) q++
            return q
        }

        fun decode(): Decoded {
            assertEquals("fLaC", String(data, 0, 4))
            bit = 32
            val last = read(1)
            assertEquals(1L, last)
            assertEquals(0L, read(7))
            assertEquals(34L, read(24))
            read(16); read(16); read(24); read(24)
            val rate = read(20)
            val channels = read(3).toInt() + 1
            val bits = read(5).toInt() + 1
            val total = read(36).toInt()
            read(128)
            assertEquals(48000L, rate)
            val out = Array(channels) { LongArray(total) }
            var at = 0
            var expectedNumber = 0L
            while (at < total) {
                val frameStart = (bit ushr 3).toInt()
                assertEquals(0x3FFEL, read(14))
                assertEquals(0L, read(1))
                assertEquals(0L, read(1))  // fixed block size
                val sizeCode = read(4).toInt()
                assertEquals(10L, read(4))  // 48 kHz in the header: the streamable subset
                val assignment = read(4).toInt()
                assertEquals(if (bits == 16) 4L else 6L, read(3))
                assertEquals(0L, read(1))
                assertEquals(expectedNumber++, utf8())
                val n = when (sizeCode) {
                    12 -> 4096
                    7 -> read(16).toInt() + 1
                    else -> error("block size code $sizeCode")
                }
                val headerEnd = (bit ushr 3).toInt()
                assertEquals(FlacEncoder.crc8(data.copyOfRange(frameStart, headerEnd)).toLong(), read(8))
                val sub = Array(channels) { c ->
                    val extra = when (assignment) {
                        8, 10 -> if (c == 1) 1 else 0
                        9 -> if (c == 0) 1 else 0
                        else -> 0
                    }
                    subframe(n, bits + extra)
                }
                when (assignment) {
                    8 -> for (i in 0 until n) sub[1][i] = sub[0][i] - sub[1][i]
                    9 -> for (i in 0 until n) sub[0][i] = sub[0][i] + sub[1][i]
                    10 -> for (i in 0 until n) {
                        val mid = (sub[0][i] shl 1) or (sub[1][i] and 1)
                        val s = sub[1][i]
                        sub[0][i] = (mid + s) shr 1
                        sub[1][i] = (mid - s) shr 1
                    }
                    else -> assertEquals(channels - 1, assignment)
                }
                while (bit and 7 != 0L) assertEquals(0L, read(1))
                val bodyEnd = (bit ushr 3).toInt()
                assertEquals(FlacEncoder.crc16(data.copyOfRange(frameStart, bodyEnd)).toLong(), read(16))
                for (c in 0 until channels) System.arraycopy(sub[c], 0, out[c], at, n)
                at += n
            }
            assertEquals(data.size.toLong(), bit ushr 3)
            return Decoded(channels, bits, out)
        }

        private fun utf8(): Long {
            val first = read(8)
            if (first < 0x80) return first
            var extra = 0
            var mask = 0x40L
            while (first and mask != 0L) {
                extra++
                mask = mask shr 1
            }
            var v = first and (mask - 1)
            repeat(extra) { v = (v shl 6) or (read(8) and 0x3F) }
            return v
        }

        private fun subframe(n: Int, bits: Int): LongArray {
            assertEquals(0L, read(1))
            val type = read(6).toInt()
            assertEquals(0L, read(1))  // no wasted bits
            val x = LongArray(n)
            when {
                type == 0 -> x.fill(signed(bits))
                type == 1 -> for (i in 0 until n) x[i] = signed(bits)
                type in 8..12 -> {
                    val order = type - 8
                    for (i in 0 until order) x[i] = signed(bits)
                    val method = read(2).toInt()
                    val partitionOrder = read(4).toInt()
                    val partitions = 1 shl partitionOrder
                    var i = order
                    for (p in 0 until partitions) {
                        val k = read(if (method == 1) 5 else 4).toInt()
                        assertTrue(k < (if (method == 1) 31 else 15))  // no escape codes written
                        val count = (n shr partitionOrder) - if (p == 0) order else 0
                        repeat(count) {
                            val u = (unary() shl k) or read(k)
                            val r = (u ushr 1) xor -(u and 1)
                            x[i] = r + when (order) {
                                0 -> 0
                                1 -> x[i - 1]
                                2 -> 2 * x[i - 1] - x[i - 2]
                                3 -> 3 * x[i - 1] - 3 * x[i - 2] + x[i - 3]
                                else -> 4 * x[i - 1] - 6 * x[i - 2] + 4 * x[i - 3] - x[i - 4]
                            }
                            i++
                        }
                    }
                }
                else -> error("subframe type $type")
            }
            return x
        }
    }
}
