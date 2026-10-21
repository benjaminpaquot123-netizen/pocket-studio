package com.pocketstudio.app

import com.pocketstudio.app.analysis.GUITAR_STRINGS
import com.pocketstudio.app.analysis.nearestString
import org.junit.Assert.assertEquals
import org.junit.Test

class TunerStringsTest {
    @Test
    fun theStringsAreStandardTuning() {
        // E2 A2 D3 G3 B3 E4.
        assertEquals(listOf(40, 45, 50, 55, 59, 64), GUITAR_STRINGS)
    }

    @Test
    fun theNearestStringIsLit() {
        assertEquals(1, nearestString(45, -9.0))  // A2, a little flat
        assertEquals(0, nearestString(41, -40.0))  // a low E tuned up too far: still the low E
        assertEquals(4, nearestString(58, 30.0))  // between G3 and B3, closer to B3
        assertEquals(5, nearestString(76, 0.0))  // above the guitar: its highest string
        assertEquals(0, nearestString(30, 0.0))  // below: its lowest
    }
}
