package com.pocketstudio.model.tuto

import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tour 8, lot T0b (control p8b, seat b in ja: « あと 8 個 » over 7 dotted notes in sight, the 8th below the rows): the
 * line over the piano roll counts the dotted notes to touch that are in sight while some are, whether or not one more
 * is out of the rows (its arrow says so); all of them when none is in sight (the arrow says where they are).
 */
class Tour8TutoGesturesCounterTest {
    private val step = 40f
    private val row = 30f
    private val high = 108
    private val tick = NoteClip.TICKS_PER_STEP

    private fun note(at: Int, pitch: Int) = Note(pitch = pitch, start = at * tick, length = tick)

    /** 16 steps wide, rows from [high] down 10 rows. */
    private val view = TutoRollView(0f, 16 * step, 0f, 10 * row, step, row, high)

    @Test
    fun theCountIsTheNotesInSightWhenOneMoreIsOutOfTheRows() {
        val inSight = (0 until 7).map { note(it * 2, high - 2) }
        val below = note(14, high - 20)
        val left = inSight + below
        // The case of the seat: an arrow for the one below, the others in sight.
        assertEquals(TutoWay.Lower, TutoRollHint.also(left, view, 16))
        assertEquals(7, TutoRollHint.counted(left, view, 16))
    }

    @Test
    fun theCountIsAllOfThemWhenNoneIsInSightOrAllAre() {
        val inSight = (0 until 7).map { note(it * 2, high - 2) }
        assertEquals(7, TutoRollHint.counted(inSight, view, 16))
        val later = inSight.map { it.copy(start = it.start + 32 * tick) }
        assertEquals(7, TutoRollHint.counted(later, view, 16))
        assertEquals(0, TutoRollHint.counted(emptyList(), view, 16))
        // Rows not measured yet (rowPx 0): the time alone says what is in sight.
        assertEquals(8, TutoRollHint.counted(inSight + note(14, high - 20), view.copy(rowPx = 0f), 16))
    }
}
