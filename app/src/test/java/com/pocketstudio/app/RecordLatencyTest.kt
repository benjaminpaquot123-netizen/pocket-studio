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

    @Test
    fun overBluetoothWithoutAMeasureTheVoiceIsNotMovedTwiceTheOutput() {
        // 180 ms out to the headset, the voice from the phone's microphone: about 200 ms late, not 360.
        assertEquals(200.0, Metrics.recordLatencyMs(null, null, 180.0, bluetooth = true), 1e-9)
        assertEquals(360.0, Metrics.recordLatencyMs(null, null, 180.0, bluetooth = false), 1e-9)
        assertEquals(200.0, Metrics.recordLatencyMs(45.0, null, 180.0, bluetooth = true), 1e-9)
        // A measure that knows its output stays the reference.
        assertEquals(215.0, Metrics.recordLatencyMs(45.0, 10.0, 180.0, bluetooth = true), 1e-9)
    }

    @Test
    fun headphonesAreRecognised() {
        val h = com.pocketstudio.app.audio.Headphones
        assertEquals(com.pocketstudio.app.audio.Listening.Speaker, h.classify(listOf(android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)))
        assertEquals(com.pocketstudio.app.audio.Listening.Wired, h.classify(listOf(android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, android.media.AudioDeviceInfo.TYPE_USB_HEADSET)))
        assertEquals(com.pocketstudio.app.audio.Listening.Bluetooth, h.classify(listOf(android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES, android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)))
    }
}
