package com.pocketstudio.app.remix

import com.pocketstudio.app.rhythm.RhythmViewModel.RemixCopy
import org.junit.Assert.assertEquals
import org.junit.Test

class RemixTempoCopyTest {
    @Test
    fun duringATempoDragTheCopyForThePreviousTempoGoesOn() {
        // Copy made for x1.2; the tempo moved to x1.25, its copy not ready yet: x1.2 keeps playing.
        assertEquals(1.2, RemixCopy.playRatio(true, "song", 1.25, "song" to 1.2), 0.0)
        // Once the x1.25 copy is in, it plays.
        assertEquals(1.25, RemixCopy.playRatio(true, "song", 1.25, "song" to 1.25), 0.0)
    }

    @Test
    fun theSongAsIsWhenNoCopyFitsOrNoneIsWanted() {
        assertEquals(1.0, RemixCopy.playRatio(true, "song", 1.25, null), 0.0)
        assertEquals(1.0, RemixCopy.playRatio(true, "song", 1.25, "other" to 1.2), 0.0)  // another song's copy
        assertEquals(1.0, RemixCopy.playRatio(true, "song", 1.0, "song" to 1.2), 0.0)    // back to its own tempo
        assertEquals(1.0, RemixCopy.playRatio(false, "song", 2.5, "song" to 1.2), 0.0)   // too far to fit, or failed
    }
}
