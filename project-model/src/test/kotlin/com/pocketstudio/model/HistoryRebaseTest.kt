package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Relecture tour 1, M3: Pad Live's page filled on its own was a quiet change of the current step
 * only, so « Annuler » of an older step took the page away. [History.rebase] puts it in every step.
 */
class HistoryRebaseTest {
    @Test
    fun aChangeMadeEverywhereSurvivesUndoAndRedo() {
        // Values are sets of pads holding a sound; the app fills pad 9 on its own.
        val h = History(setOf(1), coalesceWindowMs = 0)
        h.push(setOf(1, 2), nowMs = 0)
        h.push(setOf(1, 2, 3), nowMs = 1_000)
        h.undo()
        h.rebase { it + 9 }
        assertEquals(setOf(1, 2, 9), h.current)
        h.undo()
        assertEquals(setOf(1, 9), h.current)
        h.redo()
        h.redo()
        assertEquals(setOf(1, 2, 3, 9), h.current)
        assertTrue(h.canUndo)
        assertEquals(2, h.undoDepth)
    }
}
