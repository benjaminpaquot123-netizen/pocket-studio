package com.pocketstudio.model

import kotlin.math.abs
import kotlin.math.ceil

/**
 * Pad Live's challenges (n° 25, lot B3, « refais ce rythme »): a short rhythm on the « coup » pads
 * of the page shown is played once, then the player plays it back on the pads and gets 0 to 3
 * stars. Pure: the app plays the example and times the touches.
 *
 * A level's rhythm is written with « voices » (0, 1, 2): the first, second and third « coup » pad
 * of the page; a page with fewer of them uses them again in turn.
 */
object PadChallenge {
    /** A hit of the rhythm: [voice] (0..2) on beat [beat] of the bar (quarter notes from 0). */
    data class Note(val voice: Int, val beat: Double)

    /** A level: its [name] (French, translated by the app) and its bar of [notes]. */
    data class Level(val name: String, val notes: List<Note>)

    /** A rhythm to play back: [pad] (0..63) hit on [beat] (from the first hit, quarter notes). */
    data class Hit(val pad: Int, val beat: Double)

    data class Challenge(val level: Int, val name: String, val bpm: Double, val hits: List<Hit>) {
        /** Milliseconds of a beat. */
        val beatMs: Double get() = 60_000.0 / bpm
    }

    /** A touch while playing back: [pad] at [ms] (any clock, only the gaps count). */
    data class Tap(val pad: Int, val ms: Long)

    /**
     * How it went: [right] hits played on the right pad close enough to their place, of [total];
     * [extra] touches that were none of them; [stars] 0..3; [matched] per hit of the rhythm.
     * [tendency]: -1 rushed, 1 dragged, 0 neither (the gaps between the hits, compared).
     */
    data class Result(val right: Int, val total: Int, val extra: Int, val stars: Int, val matched: List<Boolean>, val tendency: Int) {
        /** The next level opens with two stars. */
        val won: Boolean get() = stars >= 2
    }

    val LEVELS: List<Level> = listOf(
        Level("Boum boum", listOf(Note(0, 0.0), Note(0, 1.0), Note(0, 2.0), Note(0, 3.0))),
        Level("Ping-pong", listOf(Note(0, 0.0), Note(1, 1.0), Note(0, 2.0), Note(1, 3.0))),
        Level("Le galop", listOf(Note(0, 0.0), Note(0, 0.5), Note(1, 1.0), Note(0, 2.0), Note(0, 2.5), Note(1, 3.0))),
        Level("Le contretemps", listOf(Note(0, 0.0), Note(1, 1.0), Note(0, 1.5), Note(2, 2.5), Note(1, 3.0))),
        Level("Le roulement", listOf(Note(0, 0.0), Note(1, 1.0), Note(0, 2.0), Note(2, 3.0), Note(2, 3.25), Note(2, 3.5), Note(2, 3.75))),
        Level("Le boss", listOf(Note(0, 0.0), Note(2, 0.75), Note(1, 1.0), Note(0, 1.5), Note(0, 2.25), Note(1, 3.0), Note(2, 3.5), Note(2, 3.75))),
    )

    /** The tempo a challenge is played at: the project's, kept between these (a child keeps up). */
    const val MIN_BPM = 80.0
    const val MAX_BPM = 120.0

    /** How far from its place a hit still counts, in beats (a sixteenth). */
    const val TOLERANCE_BEATS = 0.25

    /** Three stars: on average this close (after the whole rhythm is lined up), in beats. */
    const val TIGHT_BEATS = 0.12

    /**
     * Level [level] (1-based, clamped to the levels there are) on [shots], the « coup » pads of the
     * page in order; null when the page has none.
     */
    fun make(level: Int, shots: List<Int>, tempoBpm: Double): Challenge? {
        if (shots.isEmpty()) return null
        val n = level.coerceIn(1, LEVELS.size)
        val l = LEVELS[n - 1]
        val bpm = tempoBpm.takeIf { it.isFinite() && it > 0 }?.coerceIn(MIN_BPM, MAX_BPM) ?: MAX_BPM
        return Challenge(n, l.name, bpm, l.notes.map { Hit(shots[it.voice % shots.size], it.beat) })
    }

    /** The « coup » pads of [page] (the ones a touch plays once). */
    fun shotsOf(pads: LivePads, page: Int): List<Int> =
        (0 until PadLive.PAGE_SIZE).map { PadLive.pad(page, it) }.filter { pads.at(it)?.let { c -> PadLive.modeOf(c) == PadMode.SHOT } == true }

    /**
     * [taps] played back against [c]. Each hit, in order, takes the first touch left on its pad
     * close enough to its place, the places counted from the first touch; then the whole rhythm is
     * lined up on its touches (a first touch a little late is forgiven) and the gaps are measured.
     */
    fun score(c: Challenge, taps: List<Tap>): Result {
        val total = c.hits.size
        if (taps.isEmpty() || total == 0) return Result(0, total, taps.size, 0, List(total) { false }, 0)
        val t0 = taps.first().ms
        val at = taps.map { (it.ms - t0) / c.beatMs }
        val first = c.hits.first().beat
        val used = BooleanArray(taps.size)
        val pairs = arrayOfNulls<Int>(total)
        var from = 0
        for ((i, h) in c.hits.withIndex()) {
            val want = h.beat - first
            var pick = -1
            for (k in from until taps.size) {
                if (used[k] || taps[k].pad != h.pad) continue
                if (abs(at[k] - want) <= TOLERANCE_BEATS) { pick = k; break }
                if (at[k] > want + TOLERANCE_BEATS) break
            }
            if (pick >= 0) {
                used[pick] = true
                pairs[i] = pick
                from = pick + 1
            }
        }
        val matched = pairs.map { it != null }
        val right = matched.count { it }
        val extra = taps.size - right
        // Lined up on its touches: the mean gap removed, what is left is how steady it was.
        val errors = pairs.withIndex().mapNotNull { (i, k) -> k?.let { at[it] - (c.hits[i].beat - first) } }
        val shift = if (errors.isEmpty()) 0.0 else errors.average()
        val steady = if (errors.isEmpty()) 1.0 else errors.map { abs(it - shift) }.average()
        val stars = when {
            right == total && extra == 0 && steady <= TIGHT_BEATS -> 3
            right * 4 >= total * 3 && extra <= 1 -> 2
            right * 2 >= total -> 1
            else -> 0
        }
        // Rushing or dragging: the touches' span against the rhythm's (over its matched ends).
        val firstK = pairs.firstOrNull { it != null }
        val lastI = pairs.indexOfLast { it != null }
        val tendency = if (firstK == null || lastI <= 0) 0 else {
            val firstI = pairs.indexOfFirst { it != null }
            val want = c.hits[lastI].beat - c.hits[firstI].beat
            val got = at[pairs[lastI]!!] - at[firstK]
            when {
                want <= 0 -> 0
                got < want - TOLERANCE_BEATS / 2 -> -1
                got > want + TOLERANCE_BEATS / 2 -> 1
                else -> 0
            }
        }
        return Result(right, total, extra, stars, matched, tendency)
    }

    /** Touches still awaited before [c] is scored on its own (each of its hits once). */
    fun left(c: Challenge, taps: Int): Int = (c.hits.size - taps).coerceAtLeast(0)

    /** Frames of the example of [c] at [rate] (its last hit plus a beat to ring). */
    fun exampleFrames(c: Challenge, rate: Int): Int = ceil((c.hits.maxOf { it.beat } - c.hits.first().beat + 1.0) * c.beatMs / 1000.0 * rate).toInt()

    /** Where hit [i] of [c] starts in its example, in frames at [rate]. */
    fun frameOf(c: Challenge, i: Int, rate: Int): Int = Math.round((c.hits[i].beat - c.hits.first().beat) * c.beatMs / 1000.0 * rate).toInt()
}
