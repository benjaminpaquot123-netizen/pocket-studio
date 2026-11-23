package com.pocketstudio.app.playback

import com.pocketstudio.engine.OutputBuffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * C4 (tour 9): cuts when the sound moves to Android Auto / Bluetooth. After the reroute the engine
 * reopens the output on a slow shared path, but kept the tiny "Rapide" buffer (2 bursts), too thin
 * for a device that pulls big chunks. Simulates each output change and checks the buffer it gets.
 */
class OutputChangeBufferTest {
    private val rate = 48_000
    private fun ms(frames: Int) = frames * 1000.0 / rate

    @Test
    fun fastSpeakerPathKeepsTheChosenLowLatency() {
        // Phone speaker, fast path: 2 bursts of 96 frames stays 4 ms, the user's "Rapide".
        assertEquals(192, OutputBuffer.frames(96, 2, lowLatency = true, capacity = 3072, sampleRate = rate))
    }

    @Test
    fun rerouteToCarOrBluetoothGetsASafeBuffer() {
        // Reroute: the stream reopens shared, not low latency (Android Auto, Bluetooth A2DP).
        val car = OutputBuffer.frames(96, 2, lowLatency = false, capacity = 8192, sampleRate = rate)
        assertTrue("car path buffer ${ms(car)} ms", ms(car) >= 40.0)
        assertEquals("whole bursts only", 0, car % 96)
        // Low latency granted but on a big mixer burst (10 ms): not a fast path either.
        val bt = OutputBuffer.frames(480, 2, lowLatency = true, capacity = 8192, sampleRate = rate)
        assertTrue("bluetooth buffer ${ms(bt)} ms", ms(bt) >= 40.0)
    }

    @Test
    fun safeBufferNeverExceedsCapacityNorShrinksABiggerChoice() {
        assertEquals(1024, OutputBuffer.frames(256, 2, lowLatency = false, capacity = 1024, sampleRate = rate))
        assertEquals(960 * 6, OutputBuffer.frames(960, 6, lowLatency = false, capacity = 16384, sampleRate = rate))
    }

    @Test
    fun everyOutputChangeOfTheLadderIsCovered() {
        // Every user choice x every path: a slow path never gets less than 40 ms (capacity allowing).
        for (bursts in listOf(2, 4, 6)) for (burst in listOf(96, 192, 240, 480, 960, 1920)) for (fast in listOf(true, false)) {
            val f = OutputBuffer.frames(burst, bursts, fast, capacity = 32768, sampleRate = rate)
            val slow = !fast || burst > rate * 4 / 1000
            if (slow) assertTrue("burst $burst x$bursts fast=$fast: ${ms(f)} ms", ms(f) >= 40.0)
            else assertEquals(burst * bursts, f)
        }
    }
}
