package com.pocketstudio.model.tuto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tour 8, lot T0b (seat a, ch.3: the Clap's dotted cells, no « Touche ici » ring on any): the ring goes round the
 * first dotted cell of the bar shown that is still empty; the next one once it is filled; none when all are.
 */
class Tour8TutoGesturesRingTest {
    private val cells = setOf(4, 12, 20, 28)

    @Test
    fun theRingIsOnTheFirstEmptyDottedCellOfTheBar() {
        assertEquals(4, TutoPlay.ringed(cells, emptySet(), 0, 16))
    }

    @Test
    fun theRingMovesOnOnceTheCellIsFilled() {
        assertEquals(12, TutoPlay.ringed(cells, setOf(4), 0, 16))
    }

    @Test
    fun theRingIsInTheBarShown() {
        assertEquals(20, TutoPlay.ringed(cells, emptySet(), 16, 32))
    }

    @Test
    fun noRingWhenTheBarIsDone() {
        assertNull(TutoPlay.ringed(cells, setOf(4, 12), 0, 16))
    }
}
