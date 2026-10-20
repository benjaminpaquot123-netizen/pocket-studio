package com.pocketstudio.app.export

import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import kotlin.math.abs

/**
 * WAV to FLAC, lossless (same bit depth: 16 or 24). A small encoder: blocks of 4096 frames, the
 * best of left/right, left/side, right/side and mid/side for stereo, the best fixed predictor
 * (order 0 to 4) per channel and partitioned Rice-coded residuals. Every player and DAW reads it;
 * it stays within FLAC's streamable subset.
 */
object FlacEncoder {
    private const val BLOCK = 4096
    private const val MAX_PARTITION_ORDER = 8

    /**
     * Background thread. Throws on a WAV it cannot read, on a write error or when cancelled.
     * [onProgress] gets 0..1 now and then.
     */
    fun encode(wav: File, flac: File, isCancelled: () -> Boolean = { false }, onProgress: (Float) -> Unit = {}) {
        WavPcm.open(wav).use { pcm ->
            val bps = if (pcm.bits == 16) 16 else 24  // 32-bit and float sources come out as 24-bit
            val total = pcm.framesLeft
            val channels = pcm.channels
            val tmp = File(flac.parentFile, flac.name + ".tmp")
            try {
                BufferedOutputStream(tmp.outputStream(), 1 shl 16).use { out ->
                    val encoder = Encoder(pcm.sampleRate, channels, bps)
                    out.write(streamInfo(pcm.sampleRate, channels, bps, total))
                    val buf = FloatArray(BLOCK * channels)
                    val samples = Array(channels) { LongArray(BLOCK) }
                    val scale = (1L shl (bps - 1)).toDouble()
                    val max = (1L shl (bps - 1)) - 1
                    var frameNumber = 0L
                    var done = 0L
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
                        encoder.frame(samples, n, frameNumber++, out)
                        done += n
                        if (frameNumber % 32 == 0L && total > 0) onProgress((done.toDouble() / total).toFloat().coerceIn(0f, 1f))
                    }
                }
                if (!tmp.renameTo(flac)) throw IOException("enregistrement impossible")
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

    /** Frame header codes of the streamable subset: the rate and sample size are in every frame. */
    private fun rateCode(rate: Int) = when (rate) {
        88200 -> 1; 176400 -> 2; 192000 -> 3; 8000 -> 4; 16000 -> 5; 22050 -> 6; 24000 -> 7
        32000 -> 8; 44100 -> 9; 48000 -> 10; 96000 -> 11
        else -> 0  // from STREAMINFO
    }

    private fun sizeCode(bps: Int) = if (bps == 16) 4 else 6

    /** One encode: buffers reused from frame to frame. */
    private class Encoder(rate: Int, private val channels: Int, private val bps: Int) {
        private val rate = rateCode(rate)
        private val w = BitWriter()
        private val mid = LongArray(BLOCK)
        private val side = LongArray(BLOCK)
        private val residuals = LongArray(BLOCK)
        private val partitionSums = LongArray(1 shl MAX_PARTITION_ORDER)
        private val partitionCounts = IntArray(1 shl MAX_PARTITION_ORDER)
        private val parameters = IntArray(1 shl MAX_PARTITION_ORDER)
        private val crcTail = ByteArray(2)

        fun frame(samples: Array<LongArray>, n: Int, number: Long, out: java.io.OutputStream) {
            // Stereo: code the pair that predicts best (sum of the best residuals as the guide).
            var assignment = channels - 1
            if (channels == 2) {
                val l = samples[0]
                val r = samples[1]
                for (i in 0 until n) {
                    side[i] = l[i] - r[i]
                    mid[i] = (l[i] + r[i]) shr 1
                }
                val cl = cost(l, n)
                val cr = cost(r, n)
                val cs = cost(side, n)
                val cm = cost(mid, n)
                val options = longArrayOf(cl + cr, cl + cs, cs + cr, cm + cs)
                val best = options.indices.minBy { options[it] }
                assignment = if (best == 0) 1 else 7 + best  // 1 independent, 8 left/side, 9 right/side, 10 mid/side
            }
            w.reset()
            w.bits(0x3FFE, 14)  // sync
            w.bits(0, 1)
            w.bits(0, 1)        // fixed block size
            w.bits(if (n == BLOCK) 12L else 7L, 4)  // 4096, or the size in 16 bits at the end of the header
            w.bits(rate.toLong(), 4)
            w.bits(assignment.toLong(), 4)
            w.bits(sizeCode(bps).toLong(), 3)
            w.bits(0, 1)
            w.utf8(number)
            if (n != BLOCK) w.bits((n - 1).toLong(), 16)
            w.bits(crc8(w.buffer, w.size).toLong(), 8)
            when (assignment) {
                8 -> { subframe(samples[0], n, bps); subframe(side, n, bps + 1) }
                9 -> { subframe(side, n, bps + 1); subframe(samples[1], n, bps) }
                10 -> { subframe(mid, n, bps); subframe(side, n, bps + 1) }
                else -> for (c in 0 until channels) subframe(samples[c], n, bps)
            }
            w.alignToByte()
            val crc = crc16(w.buffer, w.size)
            crcTail[0] = (crc shr 8).toByte()
            crcTail[1] = crc.toByte()
            out.write(w.buffer, 0, w.size)
            out.write(crcTail)
        }

        /** The smallest sum of |residuals| over the fixed predictors. */
        private fun cost(x: LongArray, n: Int): Long {
            var best = Long.MAX_VALUE
            for (order in 0..minOf(4, n - 1)) {
                var sum = 0L
                for (i in order until n) sum += abs(residual(x, i, order))
                if (sum < best) best = sum
            }
            return best
        }

        private fun subframe(x: LongArray, n: Int, bits: Int) {
            var constant = true
            for (i in 1 until n) if (x[i] != x[0]) { constant = false; break }
            if (constant) {
                // Digital silence (the export's tail): one value for the whole block.
                w.bits(0, 8)  // padding bit, CONSTANT, no wasted bits
                w.signed(x[0], bits)
                return
            }
            // The fixed predictor whose residuals are smallest.
            var best = 0
            var bestSum = Long.MAX_VALUE
            for (order in 0..minOf(4, n - 1)) {
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
            for (i in 0 until best) w.signed(x[i], bits)
            for (i in best until n) residuals[i] = residual(x, i, best)
            rice(n, best)
        }

        /**
         * Partitioned Rice coding with 5-bit parameters (24-bit residuals can need more than 14):
         * the partition order and each partition's parameter that give the fewest bits.
         */
        private fun rice(n: Int, order: Int) {
            // Sums of the zigzagged residuals per partition at the finest order, merged for the coarser ones.
            var maxOrder = 0
            while (maxOrder < MAX_PARTITION_ORDER && n % (1 shl (maxOrder + 1)) == 0 && (n shr (maxOrder + 1)) > order) maxOrder++
            val finest = 1 shl maxOrder
            val size = n shr maxOrder
            for (p in 0 until finest) {
                var sum = 0L
                val from = if (p == 0) order else p * size
                for (i in from until (p + 1) * size) sum += zigzag(residuals[i])
                partitionSums[p] = sum
                partitionCounts[p] = (p + 1) * size - from
            }
            var bestOrder = maxOrder
            var bestBits = Long.MAX_VALUE
            var sums = partitionSums.copyOf(finest)
            var counts = partitionCounts.copyOf(finest)
            for (po in maxOrder downTo 0) {
                var bits = 0L
                for (p in 0 until (1 shl po)) bits += 5 + bestBits(sums[p], counts[p]).second
                if (bits < bestBits) {
                    bestBits = bits
                    bestOrder = po
                }
                if (po > 0) {
                    sums = LongArray(1 shl (po - 1)) { sums[2 * it] + sums[2 * it + 1] }
                    counts = IntArray(1 shl (po - 1)) { counts[2 * it] + counts[2 * it + 1] }
                }
            }
            // Parameters of the chosen order (from the finest sums again).
            val partitions = 1 shl bestOrder
            val merge = 1 shl (maxOrder - bestOrder)
            for (p in 0 until partitions) {
                var sum = 0L
                var count = 0
                for (q in p * merge until (p + 1) * merge) {
                    sum += partitionSums[q]
                    count += partitionCounts[q]
                }
                parameters[p] = bestBits(sum, count).first
            }
            w.bits(1, 2)  // RICE2: 5-bit parameters
            w.bits(bestOrder.toLong(), 4)
            val length = n shr bestOrder
            for (p in 0 until partitions) {
                val k = parameters[p]
                w.bits(k.toLong(), 5)
                val from = if (p == 0) order else p * length
                for (i in from until (p + 1) * length) {
                    val u = zigzag(residuals[i])
                    w.unary(u ushr k)
                    if (k > 0) w.bits(u and ((1L shl k) - 1), k)
                }
            }
        }

        /** The Rice parameter for [count] values summing to [sum], and about how many bits it takes. */
        private fun bestBits(sum: Long, count: Int): Pair<Int, Long> {
            if (count == 0) return 0 to 0L
            val mean = sum / count
            val guess = if (mean <= 0) 0 else 63 - java.lang.Long.numberOfLeadingZeros(mean)
            var best = guess.coerceIn(0, 30)
            var bestBits = Long.MAX_VALUE
            for (k in (guess - 1).coerceAtLeast(0)..(guess + 1).coerceAtMost(30)) {
                val bits = count.toLong() * (k + 1) + (sum ushr k)
                if (bits < bestBits) {
                    bestBits = bits
                    best = k
                }
            }
            return best to bestBits
        }
    }

    private fun zigzag(r: Long) = (r shl 1) xor (r shr 63)

    private fun residual(x: LongArray, i: Int, order: Int): Long = when (order) {
        0 -> x[i]
        1 -> x[i] - x[i - 1]
        2 -> x[i] - 2 * x[i - 1] + x[i - 2]
        3 -> x[i] - 3 * x[i - 1] + 3 * x[i - 2] - x[i - 3]
        else -> x[i] - 4 * x[i - 1] + 6 * x[i - 2] - 4 * x[i - 3] + x[i - 4]
    }

    internal fun crc8(data: ByteArray, length: Int = data.size): Int {
        var crc = 0
        for (j in 0 until length) {
            crc = crc xor (data[j].toInt() and 0xFF)
            repeat(8) { crc = if (crc and 0x80 != 0) ((crc shl 1) xor 0x07) and 0xFF else (crc shl 1) and 0xFF }
        }
        return crc
    }

    internal fun crc16(data: ByteArray, length: Int = data.size): Int {
        var crc = 0
        for (j in 0 until length) {
            crc = crc xor ((data[j].toInt() and 0xFF) shl 8)
            repeat(8) { crc = if (crc and 0x8000 != 0) ((crc shl 1) xor 0x8005) and 0xFFFF else (crc shl 1) and 0xFFFF }
        }
        return crc
    }

    /** Writes bits most significant first. */
    internal class BitWriter {
        var buffer = ByteArray(1 shl 14)
            private set
        var size = 0
            private set
        private var acc = 0L
        private var count = 0  // bits waiting in acc, always under 8 between calls

        fun reset() {
            size = 0
            acc = 0L
            count = 0
        }

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
            if (size == buffer.size) buffer = buffer.copyOf(size * 2)
            buffer[size++] = b.toByte()
        }

        /** The whole bytes written so far (a partial last byte is not included). */
        fun bytes(): ByteArray = buffer.copyOf(size)
    }
}
