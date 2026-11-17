package com.pocketstudio.model.tuto

import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 7, audit « Fable », fresh review (minor): the note lit after a touch on the line was chosen from the move
 * asked for, not the move the roll can make: at the keyboard's edge (the lowest rows are under the tutorial's panel)
 * the ring went to a note that stayed out of sight. The note lit is one that is in sight after the real move.
 */
class Tour7TutoLitInSightTest {
    private val step = 40f
    private val row = 30f
    private val high = 96
    private val tick = NoteClip.TICKS_PER_STEP

    private fun view(low: Int, rows: Int) = TutoRollView(0f, 16 * step, (high - (low + rows - 1)) * row, (high - low + 1) * row, step, row, high)

    @Test
    fun theNoteLitIsInSightAfterTheMoveTheRollCanMake() {
        val bad = ArrayList<String>()
        var held = 0
        // Five notes in sight, a sixth a few rows under them; the rows can still go down by [down] pixels only.
        for (under in 1..3) for (down in listOf(0f, row / 2, row, 2 * row, Float.MAX_VALUE)) {
            val left = (0 until 5).map { Note(it * 2 * tick, tick, 60 + it % 3) } + Note(12 * tick, tick, 58 - under)
            val v = view(58, 8)
            val hidden = TutoRollHint.aside(left, v, 16)
            assertEquals("the test checks nothing: the sixth is not the hidden one", left.last(), hidden)
            val asked = TutoRollHint.brought(left, v, 16).second
            assertTrue("the test checks nothing: the touch does not go down", asked > 0f)
            val real = asked.coerceIn(-Float.MAX_VALUE, down)
            if (real < asked) held++
            val now = v.copy(top = v.top + real, bottom = v.bottom + real)
            val lit = TutoRollHint.lit(left, v, 16, true, Float.MAX_VALUE, down)!!
            if (!TutoRollHint.inRows(lit, now)) bad += "the sixth $under rows under, $down px of scroll left: the note lit (${lit.pitch}) is out of the rows in sight"
            if (real == asked && lit != hidden) bad += "the sixth $under rows under, room to go: the note lit is ${lit.pitch}, not the one brought (${hidden!!.pitch})"
        }
        assertTrue("the roll was never held at its edge: the test checks nothing", held > 3)
        assertTrue("${bad.size} notes lit out of sight:\n" + bad.joinToString("\n"), bad.isEmpty())
    }
}
