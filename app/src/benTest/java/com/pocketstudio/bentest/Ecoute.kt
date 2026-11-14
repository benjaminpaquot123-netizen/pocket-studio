package com.pocketstudio.bentest

import java.util.Locale
import kotlin.math.log10

/**
 * What the robot hears of a song played live (docs/PLAN-TEST-APK.md, niveau 1): the engine read 4
 * times a second ([Lu]), folded into seconds, and the alerts of the live bench (docs/son/BANC.md,
 * « Écoute en direct ») as far as a phone's readings show them:
 * - SILENCE: two seconds in a row under −50 dBFS while the transport runs, the song's curve (the
 *   master's lane) not planning it;
 * - FONDU: the short-term level 6 dB under the best of the 10 s before while the live gain fell by
 *   more than 3 dB, the song's curve not planning it (a quieter part at a steady gain is the music);
 * - SAUT: a quarter second 12 dB over everything within a second around it, louder than −12 dBFS
 *   (the bench's click reads the samples; a phone only has each quarter second's peak: an isolated
 *   pop, not a small click inside the music).
 */
object Ecoute {
    /** One reading. [lufs]: short-term (3 s) loudness; [drive]: the live loudness gain (dB); [lane]: the master's song curve (0..1), null without one. */
    data class Lu(
        val ms: Long,
        val xruns: Int,
        val cpu: Float,
        val peak: Float,
        val nonFinite: Boolean = false,
        val playing: Boolean = true,
        val lufs: Float = Float.NaN,
        val drive: Float = Float.NaN,
        val lane: Float? = null,
        val cpuWorst: Float = 0f,
        val gapWorst: Float = 0f,
    )

    /** One second: its peak (dBFS), its last short-term level and live gain, its curve (the lowest), whether it all played. */
    data class Seconde(val peakDb: Float, val lufs: Float, val drive: Float, val lane: Float?, val playing: Boolean)

    data class Alerte(val kind: String, val second: Int, val detail: String) {
        fun text() = "$kind à ${second} s ($detail)"
    }

    data class Bilan(
        val reads: Int,
        val playing: Int,
        val xruns: Int,
        val cpuMean: Float,
        val cpuMax: Float,
        val peakMax: Float,
        /** The true worst block (over 1: a callback alone missed its turn) and the longest gap between two callbacks (in callbacks); 0: unknown. */
        val cpuWorst: Float = 0f,
        val gapWorst: Float = 0f,
        val nonFinite: Boolean,
        val secondes: List<Seconde>,
        val alertes: List<Alerte>,
    ) {
        /** The loudest short-term level heard (LUFS), null when the engine gave none. */
        fun lufsMax(): Float? = secondes.map { it.lufs }.filter { it.isFinite() }.maxOrNull()

        /** « pire bloc vrai 80 %, plus long écart 1,2 blocs »: empty when the engine does not say. */
        fun blocs(): String = if (cpuWorst <= 0f && gapWorst <= 0f) "" else
            "pire bloc vrai ${pct(cpuWorst)}, plus long écart ${String.format(Locale.ROOT, "%.1f", gapWorst).replace('.', ',')} blocs"

        fun text() = "${reads / READS_PER_SECOND} s (en lecture ${playing / READS_PER_SECOND} s), $xruns décrochage(s), charge moyenne ${pct(cpuMean)} (lissée, pour info ; pire ${pct(cpuMax)}), " +
            (blocs().takeIf { it.isNotEmpty() }?.let { "$it, " } ?: "") +
            (lufsMax()?.let { "niveau ${String.format(Locale.ROOT, "%.1f", it)} LUFS, " } ?: "") +
            "alertes : ${if (alertes.isEmpty()) "aucune" else alertes.joinToString(" ; ") { it.text() }}"

        /** The level of each second (dBFS, one decimal), for the report's numbers. */
        fun niveaux(): String = secondes.joinToString(",") { String.format(Locale.ROOT, "%.1f", it.peakDb) }
    }

    const val READS_PER_SECOND = 4
    const val SILENCE_DB = -50f
    const val FONDU_DB = 6f
    const val FONDU_GAIN_DB = 3f
    const val FONDU_WINDOW_S = 10
    const val SAUT_DB = 12f
    const val SAUT_MIN = 0.25f
    /** A curve under this (−20 dB) plans a quiet second. */
    const val LANE_QUIET = 0.1f

    fun db(v: Float): Float = if (v <= 1e-6f) -120f else 20f * log10(v)

    fun secondes(reads: List<Lu>): List<Seconde> = reads.chunked(READS_PER_SECOND).filter { it.size == READS_PER_SECOND }.map { sec ->
        Seconde(
            peakDb = db(sec.maxOf { if (it.peak.isFinite()) it.peak else 0f }),
            lufs = sec.last().lufs,
            drive = sec.last().drive,
            lane = sec.mapNotNull { it.lane }.minOrNull(),
            playing = sec.all { it.playing },
        )
    }

    fun alertes(reads: List<Lu>): List<Alerte> {
        val out = ArrayList<Alerte>()
        val s = secondes(reads)
        fun planned(sec: Seconde) = sec.lane != null && sec.lane < LANE_QUIET
        // SILENCE: two seconds in a row, once per hole.
        var hole = false
        for (i in 1 until s.size) {
            val quiet = { k: Int -> s[k].playing && s[k].peakDb < SILENCE_DB && !planned(s[k]) }
            if (quiet(i) && quiet(i - 1)) {
                if (!hole) out += Alerte("SILENCE", i - 1, "${String.format(Locale.ROOT, "%.0f", s[i].peakDb)} dBFS")
                hole = true
            } else if (!quiet(i)) {
                hole = false
            }
        }
        // FONDU: the short-term level 6 dB under the best of the 10 s before, the live gain 3 dB lower, the curve steady.
        var fading = false
        for (i in 1 until s.size) {
            val now = s[i]
            val before = s.subList(maxOf(0, i - FONDU_WINDOW_S), i)
            val best = before.map { it.lufs }.filter { it.isFinite() }.maxOrNull()
            val bestDrive = before.map { it.drive }.filter { it.isFinite() }.maxOrNull()
            val laneBefore = before.mapNotNull { it.lane }.maxOrNull()
            val low = now.playing && best != null && now.lufs.isFinite() && now.lufs < best - FONDU_DB
            val gainFell = bestDrive != null && now.drive.isFinite() && now.drive < bestDrive - FONDU_GAIN_DB
            val curveSteady = now.lane == null || laneBefore == null || laneBefore <= 0f || db(now.lane / laneBefore) > -FONDU_GAIN_DB
            if (low && gainFell && curveSteady) {
                if (!fading) out += Alerte("FONDU", i, String.format(Locale.ROOT, "%.1f LUFS sous %.1f, gain %+.1f dB", now.lufs, best, now.drive - bestDrive!!))
                fading = true
            } else if (!low) {
                fading = false
            }
        }
        // SAUT: a quarter second 12 dB over everything within a second around it.
        for (k in reads.indices) {
            val p = reads[k].peak
            if (!reads[k].playing || !p.isFinite() || p < SAUT_MIN) continue
            val around = (maxOf(0, k - READS_PER_SECOND)..minOf(reads.size - 1, k + READS_PER_SECOND)).filter { it != k }.map { reads[it].peak }.filter { it.isFinite() }
            if (around.size < READS_PER_SECOND) continue
            val loudest = around.max()
            if (db(p) - db(loudest) >= SAUT_DB) out += Alerte("SAUT", k / READS_PER_SECOND, String.format(Locale.ROOT, "%.1f dBFS, %.0f dB au-dessus", db(p), db(p) - db(loudest)))
        }
        return out.sortedBy { it.second }
    }

    /** [xrunsBefore]: the engine's count when the song started (the dropouts are the ones heard since). */
    fun bilan(reads: List<Lu>, xrunsBefore: Int): Bilan = Bilan(
        reads = reads.size,
        playing = reads.count { it.playing },
        xruns = ((reads.lastOrNull()?.xruns ?: xrunsBefore) - xrunsBefore).coerceAtLeast(0),
        cpuMean = if (reads.isEmpty()) 0f else reads.map { it.cpu }.average().toFloat(),
        cpuMax = reads.maxOfOrNull { it.cpu } ?: 0f,
        peakMax = reads.maxOfOrNull { if (it.peak.isFinite()) it.peak else 0f } ?: 0f,
        cpuWorst = reads.maxOfOrNull { it.cpuWorst } ?: 0f,
        gapWorst = reads.maxOfOrNull { it.gapWorst } ?: 0f,
        nonFinite = reads.any { it.nonFinite },
        secondes = secondes(reads),
        alertes = alertes(reads),
    )

    fun fmt(v: Float) = String.format(Locale.ROOT, "%.2f", v)
    fun pct(v: Float) = String.format(Locale.ROOT, "%.0f %%", v * 100)
}
