package com.pocketstudio.engine

/**
 * Raises the output buffer one notch (2 → 4 → 6 bursts) when underruns pile up: [THRESHOLD] new
 * xruns within [WINDOW_MS], at most one change per [COOLDOWN_MS]. It never goes back down on its
 * own during the session (a new user choice resets it), and a reopened stream (its counter restarts,
 * e.g. after an output change) only rebaselines: the jump is not counted as dropouts. The change is
 * applied through setBufferBursts, which resizes the buffer without restarting the stream.
 * Not on the audio thread: fed from the UI's stream polling.
 */
class AutoBuffer(
    private val window: Long = WINDOW_MS,
    private val cooldown: Long = COOLDOWN_MS,
    private val threshold: Int = THRESHOLD,
) {
    /** Bursts asked by the boost; 0 = none (the user's choice applies). */
    var bursts: Int = 0
        private set

    private val events = LongArray(threshold)
    private var eventCount = 0
    private var eventHead = 0
    private var lastXruns = -1
    private var lastRestarts = -1
    private var lastChange = Long.MIN_VALUE

    /** Forgets the boost (the user picked a new buffer size). */
    fun reset() {
        bursts = 0
        clearEvents()
    }

    /** Feeds one stream reading; returns the bursts to apply, or null when nothing changes. */
    fun update(userBursts: Int, xruns: Int, restarts: Int, running: Boolean, nowMs: Long): Int? {
        if (!running || lastXruns < 0 || restarts != lastRestarts || xruns < lastXruns) {
            lastXruns = xruns
            lastRestarts = restarts
            clearEvents()
            return null
        }
        var delta = xruns - lastXruns
        lastXruns = xruns
        while (delta-- > 0) push(nowMs)
        if (countSince(nowMs - window) < threshold) return null
        if (lastChange != Long.MIN_VALUE && nowMs - lastChange < cooldown) return null
        clearEvents()
        val current = maxOf(userBursts, bursts)
        val next = LADDER.firstOrNull { it > current } ?: return null
        bursts = next
        lastChange = nowMs
        return next
    }

    private fun push(t: Long) {
        events[eventHead] = t
        eventHead = (eventHead + 1) % threshold
        if (eventCount < threshold) eventCount++
    }

    private fun countSince(from: Long): Int {
        var n = 0
        for (i in 0 until eventCount) if (events[i] > from) n++
        return n
    }

    private fun clearEvents() {
        eventCount = 0
        eventHead = 0
    }

    companion object {
        const val WINDOW_MS = 10_000L
        const val COOLDOWN_MS = 10_000L
        const val THRESHOLD = 3
        val LADDER = intArrayOf(2, 4, 6)
    }
}
