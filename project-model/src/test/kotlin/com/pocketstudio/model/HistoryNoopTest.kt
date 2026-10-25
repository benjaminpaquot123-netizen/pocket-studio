package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lot 2 review: a slider's gesture the system takes back (the pan slider puts its value back)
 * merged into one step equal to where it started: an undo that did nothing. Now no step is left.
 */
class HistoryNoopTest {
    @Test
    fun aBurstBackToItsStartLeavesNoStep() {
        val h = History(0, coalesceWindowMs = 600)
        h.push(1, "pan", nowMs = 0)
        h.push(2, "pan", nowMs = 100)
        h.push(0, "pan", nowMs = 200)
        assertEquals(0, h.current)
        assertFalse("no step to undo", h.canUndo)
    }

    @Test
    fun anEarlierStepStays() {
        val h = History(0, coalesceWindowMs = 600)
        h.push(5, "vol", nowMs = 0)
        h.push(6, "pan", nowMs = 1000)
        h.push(5, "pan", nowMs = 1100)
        assertTrue(h.canUndo)
        assertEquals(1, h.undoDepth)
        h.undo()
        assertEquals(0, h.current)
    }
}
