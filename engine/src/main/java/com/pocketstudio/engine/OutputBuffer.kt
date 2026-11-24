package com.pocketstudio.engine

/**
 * How many frames the output buffer holds for a given stream. Mirror of AudioHost::bufferFramesLocked
 * (engine/src/main/cpp/android/AudioHost.cpp): both must change together.
 *
 * A fast path (low latency granted, bursts of 4 ms or less) keeps the user's choice. Any other path
 * (Android Auto, Bluetooth, a shared mixer after a reroute) pulls big chunks: a 2-burst buffer
 * underruns all the time there, so it gets at least [SLOW_PATH_MIN_MS], in whole bursts.
 */
object OutputBuffer {
    const val SLOW_PATH_MIN_MS = 40
    const val FAST_BURST_MAX_MS = 4

    fun isFastPath(framesPerBurst: Int, lowLatency: Boolean, sampleRate: Int): Boolean =
        lowLatency && framesPerBurst <= sampleRate * FAST_BURST_MAX_MS / 1000

    /** Frames the output buffer asks for: [bursts] bursts of [framesPerBurst], floored on a slow path, at most [capacity]. */
    fun frames(framesPerBurst: Int, bursts: Int, lowLatency: Boolean, capacity: Int, sampleRate: Int): Int {
        if (framesPerBurst <= 0) return 0
        var frames = framesPerBurst * bursts.coerceIn(1, 8)
        if (!isFastPath(framesPerBurst, lowLatency, sampleRate)) {
            val floor = sampleRate * SLOW_PATH_MIN_MS / 1000
            val floorBursts = (floor + framesPerBurst - 1) / framesPerBurst
            frames = maxOf(frames, floorBursts * framesPerBurst)
        }
        return if (capacity > 0) minOf(frames, capacity) else frames
    }
}
