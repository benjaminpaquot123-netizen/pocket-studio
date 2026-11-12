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
        val nonFinite: Boolean,
        val secondes: List<Seconde>,
        val alertes: List<Alerte>,
    ) {
        fun text() = "${reads / READS_PER_SECOND} s (en lecture ${playing / READS_PER_SECOND} s), $xruns décrochage(s), charge moyenne ${pct(cpuMean)} (pire ${pct(cpuMax)}), " +
            "crête ${fmt(peakMax)}, alertes : ${if (alertes.isEmpty()) "aucune" else alertes.joinToString(" ; ") { it.text() }}"

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

    fun secondes(reads: List<Lu>): List<Seconde> = emptyList()

    fun alertes(reads: List<Lu>): List<Alerte> = emptyList()

    /** [xrunsBefore]: the engine's count when the song started (the dropouts are the ones heard since). */
    fun bilan(reads: List<Lu>, xrunsBefore: Int): Bilan = Bilan(reads.size, 0, 0, 0f, 0f, 0f, false, emptyList(), emptyList())

    fun fmt(v: Float) = String.format(Locale.ROOT, "%.2f", v)
    fun pct(v: Float) = String.format(Locale.ROOT, "%.0f %%", v * 100)
}
