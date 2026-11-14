package com.pocketstudio.app.human

import com.pocketstudio.app.tuto.TutoHints
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tour 6, lot « stable »: [TutoHints.ticked] is a change counter (its readers key effects on it, never read its value).
 * Put back to 0 between tests, after NavTabletScreenshotTest (2) the next screen's part menu lost its « Insérer après »
 * window (HumanLucasBugsTest, red three times in a row). The bench puts every other hint back, and keeps the counter.
 */
@RunWith(RobolectricTestRunner::class)
class BenchTutoTickedKeptTest {
    @Test
    fun theBenchPutsTheHintsBackButKeepsTheChangeCounter() {
        try {
            TutoHints.focus = 5
            TutoHints.ticked = 2
            BenchPristine.restore()
            assertEquals("the hints are put back", 0, TutoHints.focus)
            assertEquals("the change counter is kept as it was", 2, TutoHints.ticked)
        } finally {
            TutoHints.reset()
        }
    }
}
