package com.pocketstudio.model

/**
 * Tap tempo: averages the intervals of the last taps. A pause longer than [resetAfterMs]
 * starts a new measurement; a single tap gives nothing.
 */
class TapTempo(private val maxTaps: Int = 6, private val resetAfterMs: Long = 2000) {
    private val taps = ArrayDeque<Long>()

    /** Registers a tap at [nowMs]; returns the new tempo once at least two taps are known. */
    fun tap(nowMs: Long): Double? {
        if (taps.isNotEmpty() && nowMs - taps.last() > resetAfterMs) taps.clear()
        taps.addLast(nowMs)
        while (taps.size > maxTaps) taps.removeFirst()
        if (taps.size < 2) return null
        val averageMs = (taps.last() - taps.first()).toDouble() / (taps.size - 1)
        return (60_000.0 / averageMs).coerceIn(Project.MIN_TEMPO, Project.MAX_TEMPO)
    }
}
