package com.pocketstudio.app.export

import com.pocketstudio.model.LoudnessTargets
import com.pocketstudio.model.i18n.tr

/**
 * Decision 1A (Benjamin, 2026-11-03): a master that stops under its target (« Club » −9 on the
 * reference `enorme`: −10.6) because going louder would crush the song says so, with the
 * loudness it reached and why. The level is never forced: the master chain's cap on what it takes
 * off (6 LU on the whole song, 3 on the Drop: MasterChain::kMaxLossDb, kMaxLossDropDb) holds.
 */
object TargetReach {
    /** Under the target by more than this (LU), it is said (the export's own tolerance is ±0.5). */
    const val SHORT_LU = 0.5

    /** The chain's cap on what it takes off (LU): the whole song's, or the Drop's. */
    const val CAP_SONG = 6.0
    const val CAP_DROP = 3.0

    /** How close to the cap the loss must be for the cap to be why the target was missed (LU). */
    private const val AT_CAP = 0.5

    /**
     * The loudness reached when it stays short of [targetLufs] because of the cap, else null.
     * [lufs]: the whole song's integrated loudness; [loudLufs]: its Drop's, when the target aims at
     * the Drop (a style's « Auto » or « Club » in the mix versions 2 and up); [lossDb]: what the chain took off.
     */
    fun short(targetLufs: Double?, lufs: Double?, loudLufs: Double?, lossDb: Double?): Double? {
        val target = targetLufs ?: return null
        val loss = lossDb ?: return null
        val drop = loudLufs != null && loudLufs > -100.0
        val reached = (if (drop) loudLufs else lufs)?.takeIf { it > -100.0 } ?: return null
        val cap = if (drop) CAP_DROP else CAP_SONG
        return reached.takeIf { it < target - SHORT_LU && loss >= cap - AT_CAP }
    }

    /** « −10,6 LUFS au lieu de −9 : plus fort, le son s'écraserait. » */
    fun words(reached: Double, targetLufs: Double): String =
        tr("%1\$s LUFS au lieu de %2\$s : plus fort, le son s'écraserait.", LoudnessTargets.format(reached), LoudnessTargets.formatTarget(targetLufs.toFloat()))

    /** What it means, under the line. */
    fun why(): String = tr("Le master s'arrête avant d'écraser les coups : le morceau garde son punch, un peu moins fort que la cible.")
}
