package com.pocketstudio.model

import com.pocketstudio.model.video.VideoPlanner

/**
 * S6 son: where the first listen of a song starts, and which part tells the live loudness gain
 * what the export will use.
 *
 * - [startPart] (decision 2A, Benjamin 2026-10-25): « Écouter » on « Ton morceau est prêt ! »
 *   starts about [BARS_BEFORE_DROP] bars before the first Drop (or Refrain), on a part's start,
 *   so the Drop comes after about 15 s even in a techno whose Drop is two minutes in; « Depuis le
 *   début » plays the whole song.
 * - [loudPart] / [loudSteps] (G5, « live = export »): the fullest of the song's Drops (or Refrains),
 *   rendered offline before ▶ on its own (as ▶ plays a part) for 8 to 16 bars: the drive the
 *   export finds on the whole song's loud part, found on it, is where the live gain starts.
 */
object FirstListen {
    const val BARS_BEFORE_DROP = 8
    const val MIN_LOUD_BARS = 8
    const val MAX_LOUD_BARS = 16

    /** The first Drop (else Refrain, Post-refrain) of [p]'s song; -1 without a song or such a part. */
    fun firstMain(p: Project): Int {
        for (wanted in VideoPlanner.WANTED) {
            val i = p.song.indexOfFirst { VideoPlanner.baseSection(p.sceneLabel(it.scene)) == wanted }
            if (i >= 0) return i
        }
        return -1
    }

    /**
     * The part « Écouter » starts on: of the parts before the first Drop (that one included), the
     * one whose start is nearest [BARS_BEFORE_DROP] bars before it (the earlier one on a tie); 0
     * when the song has no Drop or Refrain, or it is the first part.
     */
    fun startPart(p: Project): Int {
        val main = firstMain(p)
        if (main <= 0) return 0
        val want = BARS_BEFORE_DROP * p.stepsPerBar
        val dropStep = p.songStepOf(main)
        // Never the Drop itself (a long part before it would be nearer): some lead-in always.
        var best = main - 1
        var bestGap = Int.MAX_VALUE
        for (i in main - 1 downTo 0) {
            val gap = kotlin.math.abs(dropStep - p.songStepOf(i) - want)
            if (gap <= bestGap) {
                best = i
                bestGap = gap
            }
        }
        return best
    }

    /** Whether « Écouter » starts later than the song's start (then « Depuis le début » is offered). */
    fun startsLater(p: Project): Boolean = startPart(p) > 0

    /**
     * The part the live gain is estimated on: among the Drops (else Refrains, Post-refrains, else
     * every part), the fullest ([VideoPlanner.energy]), the first of the fullest. -1 without a song.
     */
    fun loudPart(p: Project): Int {
        if (p.song.isEmpty()) return -1
        val candidates = VideoPlanner.WANTED.asSequence()
            .map { wanted -> p.song.indices.filter { VideoPlanner.baseSection(p.sceneLabel(p.song[it].scene)) == wanted } }
            .firstOrNull { it.isNotEmpty() } ?: p.song.indices.toList()
        return candidates.maxByOrNull { VideoPlanner.energy(p, p.song[it]) * 10_000 - it } ?: -1
    }

    /** How long [loudPart] is rendered: its own length, within [MIN_LOUD_BARS]..[MAX_LOUD_BARS] bars (steps). */
    fun loudSteps(p: Project, index: Int): Int {
        val part = p.song.getOrNull(index) ?: return 0
        return p.partSteps(part).coerceIn(MIN_LOUD_BARS * p.stepsPerBar, MAX_LOUD_BARS * p.stepsPerBar)
    }
}
