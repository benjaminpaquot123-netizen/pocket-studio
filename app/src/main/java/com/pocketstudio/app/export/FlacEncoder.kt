package com.pocketstudio.app.export

import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import kotlin.math.abs

/**
 * WAV to FLAC, lossless (same bit depth: 16 or 24). A small encoder: blocks of 4096 frames,
 * channels coded separately with the best fixed predictor (order 0 to 4) and Rice-coded
 * residuals. About half the size of the WAV; every player and DAW reads it.
 */
object FlacEncoder {
    private const val BLOCK = 4096

    /** Background thread. Throws on a WAV it cannot read, on a write error or when cancelled. */
    fun encode(wav: File, flac: File, isCancelled: () -> Boolean = { false }) {
        WavPcm.open(wav).use { pcm ->
            val bps = if (pcm.bits == 16) 16 else 24  // 32-bit and float sources come out as 24-bit
            val total = pcm.framesLeft
            val channels = pcm.channels
            val tmp = File(flac.parentFile, flac.name + ".tmp")
            try {
                BufferedOutputStream(tmp.outputStream(), 1 shl 16).use { out ->
                    out.write(streamInfo(pcm.sampleRate, channels, bps, total))
                    val buf = FloatArray(BLOCK * channels)
                    val samples = Array(channels) { LongArray(BLOCK) }
                    val scale = (1L shl (bps - 1)).toDouble()
                    val max = (1L shl (bps - 1)) - 1
                    var frameNumber = 0L
                    while (true) {
                        if (isCancelled()) throw kotlinx.coroutines.CancellationException("annulé")
                        var n = 0
                        while (n < BLOCK) {
                            val got = pcm.readFloat(buf, BLOCK - n)
                            if (got == 0) break
                            for (f in 0 until got) for (c in 0 until channels) {
                                samples[c][n + f] = Math.round(buf[f * channels + c] * scale).coerceIn(-max - 1, max)
                            }
                            n += got
                        }
                        if (n == 0) break
                        out.write(frame(samples, n, channels, bps, frameNumber++))
                    }
                }
                if (!tmp.renameTo(flac)) throw IOException("rename failed")
            } finally {
                tmp.delete()
            }
        }
    }

    private fun streamInfo(rate: Int, channels: Int, bps: Int, total: Long): ByteArray {
        val w = BitWriter()
        "fLaC".forEach { w.bits(it.code.toLong(), 8) }
        w.bits(1, 1)    // last metadata block
        w.bits(0, 7)    // STREAMINFO
        w.bits(34, 24)
        w.bits(BLOCK.toLong(), 16)
        w.bits(BLOCK.toLong(), 16)
        w.bits(0, 24)   // frame sizes unknown
        w.bits(0, 24)
        w.bits(rate.toLong(), 20)
        w.bits((channels - 1).toLong(), 3)
        w.bits((bps - 1).toLong(), 5)
        w.bits(total, 36)
        repeat(16) { w.bits(0, 8) }  // MD5 not computed (allowed: all zeros)
        return w.bytes()
    }

    internal fun frame(samples: Array<LongArray>, n: Int, channels: Int, bps: Int, number: Long): ByteArray {
        val w = BitWriter()
        w.bits(0x3FFE, 14)  // sync
        w.bits(0, 1)
        w.bits(0, 1)        // fixed block size
        w.bits(if (n == BLOCK) 12L else 7L, 4)  // 4096, or the size in 16 bits at the end of the header
        w.bits(0, 4)        // sample rate: from STREAMINFO
        w.bits((channels - 1).toLong(), 4)  // independent channels
        w.bits(0, 3)        // sample size: from STREAMINFO
        w.bits(0, 1)
        w.utf8(number)
        if (n != BLOCK) w.bits((n - 1).toLong(), 16)
        w.bits(crc8(w.bytes()).toLong(), 8)
        for (c in 0 until channels) subframe(w, samples[c], n, bps)
        w.alignToByte()
        val body = w.bytes()
        val crc = crc16(body)
        return body + byteArrayOf((crc shr 8).toByte(), crc.toByte())
    }

    private fun subframe(w: BitWriter, x: LongArray, n: Int, bps: Int) {
        if ((1 until n).all { x[it] == x[0] }) {
            // Digital silence (the export's tail): one value for the whole block.
            w.bits(0, 8)  // padding bit, CONSTANT, no wasted bits
            w.signed(x[0], bps)
            return
        }
        // The fixed predictor whose residuals are smallest.
        val maxOrder = minOf(4, n - 1).coerceAtLeast(0)
        var best = 0
        var bestSum = Long.MAX_VALUE
        for (order in 0..maxOrder) {
            var sum = 0L
            for (i in order until n) sum += abs(residual(x, i, order))
            if (sum < bestSum) {
                bestSum = sum
                best = order
            }
        }
        w.bits(0, 1)
        w.bits((8 + best).toLong(), 6)  // FIXED, order
        w.bits(0, 1)                    // no wasted bits
        for (i in 0 until best) w.signed(x[i], bps)
        val residuals = LongArray(n - best) { residual(x, it + best, best) }
        // Rice coding, 5-bit parameters (24-bit residuals can need more than 14), one partition.
        w.bits(1, 2)
        w.bits(0, 4)
        val k = riceParameter(residuals)
        w.bits(k.toLong(), 5)
        for (r in residuals) {
            val u = (r shl 1) xor (r shr 63)
            w.unary(u ushr k)
            if (k > 0) w.bits(u and ((1L shl k) - 1), k)
        }
    }

    private fun residual(x: LongArray, i: Int, order: Int): Long = when (order) {
        0 -> x[i]
        1 -> x[i] - x[i - 1]
        2 -> x[i] - 2 * x[i - 1] + x[i - 2]
        3 -> x[i] - 3 * x[i - 1] + 3 * x[i - 2] - x[i - 3]
        else -> x[i] - 4 * x[i - 1] + 6 * x[i - 2] - 4 * x[i - 3] + x[i - 4]
    }

    /** The Rice parameter that codes [r] in the fewest bits (tried around the mean's magnitude). */
    private fun riceParameter(r: LongArray): Int {
        if (r.isEmpty()) return 0
        var sum = 0L
        for (v in r) sum += (v shl 1) xor (v shr 63)
        val mean = sum / r.size
        val guess = if (mean <= 0) 0 else 63 - java.lang.Long.numberOfLeadingZeros(mean)
        var best = guess.coerceIn(0, 30)
        var bestBits = Long.MAX_VALUE
        for (k in (guess - 1).coerceAtLeast(0)..(guess + 1).coerceAtMost(30)) {
            var bits = 0L
            for (v in r) bits += (((v shl 1) xor (v shr 63)) ushr k) + 1 + k
            if (bits < bestBits) {
                bestBits = bits
                best = k
            }
        }
        return best
    }

    internal fun crc8(data: ByteArray): Int {
        var crc = 0
        for (b in data) {
            crc = crc xor (b.toInt() and 0xFF)
            repeat(8) { crc = if (crc and 0x80 != 0) ((crc shl 1) xor 0x07) and 0xFF else (crc shl 1) and 0xFF }
        }
        return crc
    }

    internal fun crc16(data: ByteArray): Int {
        var crc = 0
        for (b in data) {
            crc = crc xor ((b.toInt() and 0xFF) shl 8)
            repeat(8) { crc = if (crc and 0x8000 != 0) ((crc shl 1) xor 0x8005) and 0xFFFF else (crc shl 1) and 0xFFFF }
        }
        return crc
    }

    /** Writes bits most significant first. */
    internal class BitWriter {
        private var buf = ByteArray(1 shl 14)
        private var size = 0
        private var acc = 0L
        private var count = 0  // bits waiting in acc, always under 8 between calls

        fun bits(value: Long, n: Int) {
            if (n > 32) {
                bits(value ushr 32, n - 32)
                bits(value and 0xFFFFFFFFL, 32)
                return
            }
            if (n == 0) return
            acc = (acc shl n) or (value and ((1L shl n) - 1))
            count += n
            while (count >= 8) {
                count -= 8
                put((acc ushr count).toInt())
            }
            acc = acc and ((1L shl count) - 1)
        }

        fun signed(value: Long, n: Int) = bits(value and ((1L shl n) - 1), n)

        /** [q] zeros then a one. */
        fun unary(q: Long) {
            var z = q
            while (z >= 32) {
                bits(0, 32)
                z -= 32
            }
            bits(1, z.toInt() + 1)
        }

        /** FLAC's "UTF-8" coding of the frame number. */
        fun utf8(v: Long) {
            if (v < 0x80) return bits(v, 8)
            val bytes = when {
                v < 0x800 -> 2
                v < 0x10000 -> 3
                v < 0x200000 -> 4
                v < 0x4000000 -> 5
                v < 0x80000000L -> 6
                else -> 7
            }
            val first = ((0xFF00 shr bytes) and 0xFF).toLong() or (v ushr (6 * (bytes - 1)))
            bits(first, 8)
            for (i in bytes - 2 downTo 0) bits(0x80L or ((v ushr (6 * i)) and 0x3F), 8)
        }

        fun alignToByte() {
            if (count > 0) bits(0, 8 - count)
        }

        private fun put(b: Int) {
            if (size == buf.size) buf = buf.copyOf(size * 2)
            buf[size++] = b.toByte()
        }

        /** The whole bytes written so far (a partial last byte is not included). */
        fun bytes(): ByteArray = buf.copyOf(size)
    }
}
