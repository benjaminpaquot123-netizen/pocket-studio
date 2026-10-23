package com.pocketstudio.app

import com.pocketstudio.app.export.AacEncoder
import org.junit.Assert.assertEquals
import org.junit.Test

/** An M4A's source over −2 dBTP is turned down first: decoded, the AAC stays under −1 dBTP. */
class AacHeadroomTest {
    @Test
    fun onlyASourceOverTheCeilingIsTurnedDown() {
        assertEquals(1f, AacEncoder.headroomGain(null))
        assertEquals(1f, AacEncoder.headroomGain(-2.2))  // the master chain's own M4A export
        assertEquals(1f, AacEncoder.headroomGain(-2.0))
        // A mix without the limiter peaking at +1.5 dBTP: 3.5 dB down.
        assertEquals(Math.pow(10.0, -3.5 / 20.0).toFloat(), AacEncoder.headroomGain(1.5), 1e-6f)
    }
}
