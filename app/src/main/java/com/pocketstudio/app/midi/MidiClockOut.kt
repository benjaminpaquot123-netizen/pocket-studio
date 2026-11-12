package com.pocketstudio.app.midi

import com.pocketstudio.app.Metrics
import com.pocketstudio.engine.NativeEngine

/**
 * MIDI clock out (n° 25, lot B2): the engine counts the clock on the audio thread, on the very
 * frames of the transport's beats (Start on the first step, 24 ticks a beat, never swung, Stop),
 * and gives each message the time its frame is heard ([NativeEngine.drainMidiClock]). Here they go
 * to the device with that time, the output's own latency added: Android delivers them then, so the
 * other machine plays with what we hear, whatever the tempo changes, without drifting.
 * [pump] is called every few milliseconds while the clock is sent.
 */
class MidiClockOut(
    /** The device: a message ([status], a single byte) to send at [System.nanoTime] [atNanos]. */
    private val send: (status: Int, atNanos: Long) -> Unit,
    private val drain: (LongArray) -> Int = NativeEngine::drainMidiClock,
    /** How long the output takes to be heard, in nanoseconds. */
    private val latencyNanos: () -> Long = { ((Metrics.streamInfo.value.outputLatencyMs.takeIf { it > 0 } ?: 0.0) * 1_000_000).toLong() },
) {
    private val pairs = LongArray(2 * BATCH)

    /** Sends every message the engine has ready; returns how many. */
    fun pump(): Int {
        val latency = latencyNanos()
        var total = 0
        while (true) {
            val n = drain(pairs)
            for (i in 0 until n) send(pairs[i * 2].toInt(), pairs[i * 2 + 1] + latency)
            total += n
            if (n < BATCH) return total
        }
    }

    companion object {
        /** Messages read at once (the JNI's own batch). */
        const val BATCH = 256
        /** How often [pump] runs while the clock is sent: a tick is 20 ms at 125 BPM, sent ahead by the output's latency. */
        const val PUMP_MS = 4L
    }
}
