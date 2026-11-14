package com.pocketstudio.bentest

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import kotlin.math.abs

/**
 * What the robot watches for while the microphone listens to a song: a hole (the sound gone for 160 ms: a trap or techno
 * cuts its sounds for 125 ms on the beat, that is the music) or the microphone saturated. It keeps only the last 2 s in memory; on the first anomaly it
 * writes the 2 s around it (1 s before, 1 s after) in [dossier], at most [maxExtraits]: nothing else is kept.
 */
class Guet(val rate: Int = 48_000, private val dossier: File, private val nom: String, private val maxExtraits: Int = 1) {
    data class Anomalie(val kind: String, val seconde: Double, val detail: String)

    private val found = ArrayList<Anomalie>()
    private val written = ArrayList<File>()
    val anomalies: List<Anomalie> get() = found
    val extraits: List<File> get() = written

    private val bloc = rate / 50
    private val cur = FloatArray(bloc)
    private var fill = 0
    /** Samples heard so far (the start of the block being filled is [total] - [fill]). */
    private var total = 0L
    private val ring = FloatArray(2 * rate)
    private val levels = DoubleArray(HISTOIRE)
    private var nLevels = 0
    private val sorted = DoubleArray(HISTOIRE)
    private var quiet = 0
    private var quietFrom = 0L
    private var inHole = false
    private var inClip = false
    /** The extract waiting for its second after: the anomaly's sample, -1 when none. */
    private var pendingAt = -1L

    fun ajouter(x: FloatArray, n: Int = x.size) {
        for (i in 0 until n) {
            val v = x[i]
            ring[(total % ring.size).toInt()] = v
            total++
            cur[fill++] = v
            if (fill == bloc) { blocFini(total - bloc); fill = 0 }
            if (pendingAt >= 0 && total >= pendingAt + rate) ecrire()
        }
    }

    private fun blocFini(start: Long) {
        var s = 0.0
        var clipped = 0
        for (v in cur) { s += v.toDouble() * v; if (abs(v) >= CLIP) clipped++ }
        val db = Tiers.db(s / bloc)
        // The microphone saturated: once per run of clipped blocks.
        if (clipped >= CLIP_MIN) {
            if (!inClip) noter(Anomalie("saturation", start.toDouble() / rate, "$clipped échantillons au maximum en 20 ms"), start)
            inClip = true
        } else inClip = false
        // A hole: 8 blocks (160 ms) in a row 30 dB under the median of the last 2 s, sound having been there.
        if (nLevels >= HISTOIRE / 4) {
            val m = mediane()
            if (m > SON_DB && db < m - TROU_DB) {
                if (quiet == 0) quietFrom = start
                quiet++
                if (quiet == TROU_BLOCS && !inHole) {
                    inHole = true
                    noter(Anomalie("trou", quietFrom.toDouble() / rate, String.format(Locale.ROOT, "%.0f dB sous le niveau des 2 s d'avant", m - db)), quietFrom)
                }
            } else {
                quiet = 0
                inHole = false
            }
        }
        levels[nLevels % HISTOIRE] = db
        nLevels++
    }

    private fun mediane(): Double {
        val k = minOf(nLevels, HISTOIRE)
        System.arraycopy(levels, 0, sorted, 0, k)
        java.util.Arrays.sort(sorted, 0, k)
        return sorted[k / 2]
    }

    private fun noter(a: Anomalie, at: Long) {
        found += a
        if (pendingAt < 0 && written.size < maxExtraits) pendingAt = at
    }

    /** The listening is over: an extract still waiting for its second after is written with what there is. */
    fun finir() {
        if (pendingAt >= 0) ecrire()
    }

    /** The 2 s around [pendingAt] (what the ring still holds of them) as a 16-bit mono WAV. */
    private fun ecrire() {
        val at = pendingAt
        pendingAt = -1
        val from = maxOf(at - rate, total - ring.size, 0L)
        val to = minOf(at + rate, total)
        val n = (to - from).toInt()
        if (n <= 0) return
        dossier.mkdirs()
        val f = File(dossier, String.format(Locale.ROOT, "%s-%.1fs.wav", nom, at.toDouble() / rate))
        val data = ByteBuffer.allocate(44 + 2 * n).order(ByteOrder.LITTLE_ENDIAN)
        data.put("RIFF".toByteArray()).putInt(36 + 2 * n).put("WAVE".toByteArray())
        data.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(rate).putInt(rate * 2).putShort(2).putShort(16)
        data.put("data".toByteArray()).putInt(2 * n)
        for (s in from until to) {
            val v = ring[(s % ring.size).toInt()].coerceIn(-1f, 1f)
            data.putShort((v * 32767).toInt().toShort())
        }
        RandomAccessFile(f, "rw").use { it.setLength(0); it.write(data.array()) }
        written += f
    }


    companion object {
        /** 2 s of 20 ms blocks. */
        const val HISTOIRE = 100
        const val TROU_DB = 30.0
        const val TROU_BLOCS = 8
        /** The median level under which there is no song to lose (dBFS of the microphone). */
        const val SON_DB = -50.0
        const val CLIP = 0.99f
        const val CLIP_MIN = 8
    }
}
