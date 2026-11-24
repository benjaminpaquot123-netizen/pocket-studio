package com.pocketstudio.engine

/** Raises the output buffer one notch when underruns pile up (stub). */
class AutoBuffer(
    private val window: Long = WINDOW_MS,
    private val cooldown: Long = COOLDOWN_MS,
    private val threshold: Int = THRESHOLD,
) {
    /** Extra bursts asked on top of the user's choice; 0 = none. */
    var bursts: Int = 0
        private set

    fun reset() { bursts = 0 }

    /** Feeds one stream reading; returns the bursts to apply, or null when nothing changes. */
    fun update(userBursts: Int, xruns: Int, restarts: Int, running: Boolean, nowMs: Long): Int? = null

    companion object {
        const val WINDOW_MS = 10_000L
        const val COOLDOWN_MS = 10_000L
        const val THRESHOLD = 3
        val LADDER = intArrayOf(2, 4, 6)
    }
}
