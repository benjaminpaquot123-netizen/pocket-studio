package com.pocketstudio.app.acid

import com.pocketstudio.ui.TrackColors
import org.junit.Assert.assertTrue
import org.junit.Test

/** Benjamin (2026-10-30): « Retire le smiley et rends le jaune comme les autres couleurs. C'est trop là. » */
class AcidLookTest {
    @Test
    fun theAcid303sYellowIsOneOfTheTracksColours() {
        assertTrue("the 303's yellow at the level of the other tracks'", AcidLook.Yellow in TrackColors)
        assertTrue(AcidLook.Track == AcidLook.Yellow)
    }
}
