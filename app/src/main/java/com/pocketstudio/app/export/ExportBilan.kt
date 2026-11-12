package com.pocketstudio.app.export

import com.pocketstudio.model.LoudnessTargets
import com.pocketstudio.model.TargetChoice
import com.pocketstudio.model.i18n.tr
import kotlin.math.abs

/**
 * Audit ultra round 1 (n° 15, seats A c, E d, F c): after every export of the mix, a short report in
 * numbers: the integrated loudness (LUFS), the true peak (dBTP), the format, the length and the
 * target, with why the loudness differs from the target when it does (more than [TOLERANCE_LU]).
 * The Mixeur's export card shows it under the file; « Partager » shows it once the share menu is open.
 */
data class ExportBilan(
    val lufs: Double?,
    val truePeakDb: Double?,
    val format: String,
    val seconds: Double?,
    val choice: TargetChoice,
    val targetLufs: Double?,
    /** The target aims at the whole song's integrated loudness (Streaming, pro values; mix version 1), else at the Drop. */
    val wholeSong: Boolean = true,
    /** The Drop's loudness when the target aims at it (the engine's report). */
    val loudLufs: Double? = null,
    /** What the master chain took off (dB). */
    val lossDb: Double? = null,
) {
    /** « −8,1 LUFS intégrés · crête −1,0 dBTP ». */
    fun levelLine(): String = tr(
        "%1\$s LUFS intégrés · crête %2\$s dBTP",
        lufs?.let { LoudnessTargets.format(it) } ?: "—",
        truePeakDb?.takeIf { it > -100.0 }?.let { LoudnessTargets.format(it) } ?: "—",
    )

    /** « M4A, 48 kHz · 2 min 38 s ». */
    fun fileLine(): String = seconds?.let { tr("%1\$s · %2\$s", format, ExportSummary.durationLabel(it)) } ?: format

    /** « Cible : Club · −8 LUFS », « Cible : aucune ». */
    fun targetLine(): String =
        if (targetLufs == null || choice == TargetChoice.OFF) tr("Cible : aucune (Volume visé désactivé)")
        else tr("Cible : %s", LoudnessTargets.label(choice, targetLufs.toFloat()))

    /** The loudness that is compared with the target: the whole song's, or the Drop's. */
    private val reached: Double? get() = if (wholeSong) lufs else loudLufs ?: lufs

    /** Why the loudness is not the target's (null when it is, within [TOLERANCE_LU], or without a target). */
    fun whyLine(): String? {
        val target = targetLufs?.takeIf { choice != TargetChoice.OFF } ?: return null
        val song = lufs ?: return null
        // Under the target because louder would crush the song (decision 1A).
        TargetReach.short(target, song, if (wholeSong) null else loudLufs, lossDb)?.let { r ->
            return TargetReach.words(r, target) + " " + TargetReach.why()
        }
        val drop = loudLufs
        if (!wholeSong && drop != null && abs(drop - target) <= TOLERANCE_LU && abs(song - target) > TOLERANCE_LU) {
            return tr(
                "La cible se mesure sur le passage le plus fort (%1\$s LUFS) ; le morceau entier, avec ses passages calmes, mesure %2\$s LUFS.",
                LoudnessTargets.format(drop), LoudnessTargets.format(song),
            )
        }
        val gap = (reached ?: song) - target
        return if (abs(gap) <= TOLERANCE_LU) null
        else tr("Écart avec la cible : %s LU.", (if (gap > 0) "+" else "") + LoudnessTargets.format(gap))
    }

    /**
     * Audit ultra round 2 (n° 9): how far the file is from its target (LU, reached − target) when that is
     * worth a word (over [TOLERANCE_LU]); null when it is reached, without a target, or when only the
     * quiet passages of a Drop-aimed song make the whole song softer (the Drop itself is there).
     */
    fun gapLu(): Double? {
        val target = targetLufs?.takeIf { choice != TargetChoice.OFF } ?: return null
        val song = lufs ?: return null
        TargetReach.short(target, song, if (wholeSong) null else loudLufs, lossDb)?.let { return it - target }
        val drop = loudLufs
        if (!wholeSong && drop != null && abs(drop - target) <= TOLERANCE_LU) return null
        val gap = (reached ?: song) - target
        return gap.takeIf { abs(it) > TOLERANCE_LU }
    }

    /**
     * « Niveau : à la cible (Club) »: the file's level said against the target it aimed at (audit ultra
     * round 2, n° 9: a club export's report said « comme la musique en streaming »); null without a target.
     */
    fun levelWords(): String? {
        if (targetLufs == null || choice == TargetChoice.OFF) return null
        val name = LoudnessTargets.label(choice, null)
        val gap = gapLu()
        return when {
            gap == null -> tr("Niveau : à la cible (%s)", name)
            gap < 0 -> tr("Niveau : moins fort que la cible (%s)", name)
            else -> tr("Niveau : plus fort que la cible (%s)", name)
        }
    }

    /** Every line, in the order shown. */
    fun lines(): List<String> = listOfNotNull(levelLine(), fileLine(), targetLine(), whyLine())

    companion object {
        /** Within this (LU) the target is reached (the export's own tolerance, host tests). */
        const val TOLERANCE_LU = 0.5
    }
}

/**
 * The export's verdict over its report (audit ultra round 2, n° 9): « Prêt à partager » in green only
 * when nothing is to fix and the target is reached; a file far from its target says so in amber
 * (« Prêt, mais moins fort que la cible (−5,4 LU) »). Second: green or not.
 */
fun exportVerdict(summary: ExportSummary, bilan: ExportBilan?): Pair<String, Boolean> {
    val gap = bilan?.gapLu()
    if (!summary.ready || gap == null) return summary.verdict() to summary.ready
    val lu = (if (gap > 0) "+" else "") + LoudnessTargets.format(gap)
    return (if (gap < 0) tr("Prêt, mais moins fort que la cible (%s LU)", lu) else tr("Prêt, mais plus fort que la cible (%s LU)", lu)) to false
}
