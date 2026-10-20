package com.pocketstudio.app

import org.junit.Assert.assertEquals
import org.junit.Test

class RecordLatencyTest {
    @Test
    fun aTakeOverBluetoothIsMovedByTheSlowerOutput() {
        // Measured on the speaker (45 ms round trip, 10 ms out), recorded over Bluetooth (200 ms out).
        assertEquals(235.0, Metrics.recordLatencyMs(45.0, 10.0, 200.0), 1e-9)
        // Same output as the measure: the measure itself.
        assertEquals(45.0, Metrics.recordLatencyMs(45.0, 10.0, 10.0), 1e-9)
    }

    @Test
    fun withoutAMeasureOrAnOutputTheBestGuess() {
        assertEquals(20.0, Metrics.recordLatencyMs(null, null, 10.0), 1e-9)
        assertEquals(45.0, Metrics.recordLatencyMs(45.0, null, -1.0), 1e-9)
        // An older measure (output unknown): kept, unless the output now is clearly slower.
        assertEquals(45.0, Metrics.recordLatencyMs(45.0, null, 12.0), 1e-9)
        assertEquals(400.0, Metrics.recordLatencyMs(45.0, null, 200.0), 1e-9)
    }
}
