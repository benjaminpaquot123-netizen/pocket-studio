package com.pocketstudio.model

import kotlin.math.roundToInt

/**
 * Audit ultra round 1, n° 23: a project's length, counted one way for every screen that shows it (the home's
 * « Continuer », « Mes projets », the studio's « Le morceau »): the song's bars (each part's own, as the studio
 * counts them), its seconds (rounded, never cut: 4.6 s is « 0:05 » everywhere) and its parts; without a song, the
 * loop shown ([parts] 0).
 */
data class SongLength(val bars: Int, val seconds: Int, val parts: Int) {
    /** No song yet: the scene shown loops. */
    val loop: Boolean get() = parts == 0

    companion object {
        /** [steps] sixteenths at [bpm], in whole seconds (a sixteenth lasts 15/bpm seconds). */
        fun seconds(steps: Long, bpm: Double): Int = if (bpm <= 0.0) 0 else (steps * 15.0 / bpm).roundToInt()

        fun of(p: Project): SongLength {
            val spb = p.stepsPerBar.coerceAtLeast(1)
            if (p.song.isNotEmpty()) {
                val steps = p.song.map { p.partSteps(it).toLong() }
                return SongLength(steps.sumOf { (it / spb).toInt() }, seconds(steps.sum(), p.tempoBpm), p.song.size)
            }
            // Tour 6, padlive2 (P3): the scene's audio bricks too (takes put in the Live), the longest of them.
            val bricks = p.audioTracks.mapNotNull { it.clip }.filter { !it.song && it.scene == p.scene }.maxOfOrNull { it.loopSteps } ?: 0
            val bars = maxOf(p.currentSceneContent().stepsFor(spb), (bricks + spb - 1) / spb * spb) / spb
            return SongLength(bars, seconds(bars.toLong() * spb, p.tempoBpm), 0)
        }
    }
}
