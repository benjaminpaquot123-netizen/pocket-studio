package com.pocketstudio.engine

/**
 * How many frames the output buffer holds for a given stream. Mirror of AudioHost::bufferFramesLocked
 * (engine/src/main/cpp/android/AudioHost.cpp): both must change together.
 */
object OutputBuffer {
    /** Frames the output buffer asks for: [bursts] bursts of [framesPerBurst], at most [capacity]. */
    fun frames(framesPerBurst: Int, bursts: Int, lowLatency: Boolean, capacity: Int, sampleRate: Int): Int {
        if (framesPerBurst <= 0) return 0
        val frames = framesPerBurst * bursts.coerceIn(1, 8)
        return if (capacity > 0) minOf(frames, capacity) else frames
    }
}
