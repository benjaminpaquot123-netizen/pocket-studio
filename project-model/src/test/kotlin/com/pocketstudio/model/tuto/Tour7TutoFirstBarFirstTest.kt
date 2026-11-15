package com.pocketstudio.model.tuto

import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 7, seat 1c (ja, techno, chapter 3): 「ここをタップ」 on the line took the roll to bar 2 while a note of bar 1 was
 * still to write (the nearest note won). The touch brings the first note left in the order of the bars.
 */
class Tour7TutoFirstBarFirstTest {
    private val tick = NoteClip.TICKS_PER_STEP
    private val step = 40f
    private val row = 30f
    private val high = 96

    @Test
    fun theTouchGoesToTheFirstBarWithANoteLeft() {
        val bar1 = Note(2 * tick, tick, 81)      // bar 1, an octave and more over the rows in sight
        val bar2 = Note(18 * tick, tick, 60)     // bar 2, just right of the time in sight, in the rows in sight
        val y2 = (high - 60) * row
        val v = TutoRollView(4 * step, 17 * step, y2 - 4 * row, y2 + 6 * row, step, row, high)
        val (dx, dy) = TutoRollHint.brought(listOf(bar1, bar2), v, 16)
        assertTrue("the touch goes back to bar 1 (dx $dx, dy $dy)", dx < 0f && dy < 0f)
    }
}
