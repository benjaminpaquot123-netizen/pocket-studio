package com.pocketstudio.model.tuto

import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 7, audit « Fable », fresh review (minor): a dotted note too far over the others to fit the rows with them, the
 * note the line speaks of already at the edge: the line kept its arrow and « Touche ici », and the touch moved nothing.
 * While the line says a touch takes you there, a touch moves the roll: it goes to the hidden note.
 */
class Tour7TutoTouchMovesTest {
    private val step = 40f
    private val row = 30f
    private val high = 96
    private val tick = NoteClip.TICKS_PER_STEP

    private fun view(low: Int, rows: Int) = TutoRollView(0f, 16 * step, (high - (low + rows - 1)) * row, (high - low + 1) * row, step, row, high)

    private fun TutoRollView.moved(d: Pair<Float, Float>) = copy(left = left + d.first, right = right + d.first, top = top + d.second, bottom = bottom + d.second)

    @Test
    fun aTouchOnALineThatPointsAwayAlwaysMovesTheRoll() {
        val still = ArrayList<String>()
        var asked = 0
        // One note in sight, another [far] rows over or under it, too far to fit the rows with it; every place of the first.
        for (rows in 4..10) for (far in listOf(rows + 2, 18, -(rows + 2), -18)) for (at in 0 until rows) {
            val left = listOf(Note(0, tick, 60), Note(4 * tick, tick, 60 + far))
            var v = view(60 - at, rows)
            // Three touches in a row: each one moves the roll while the line points away.
            repeat(3) { n ->
                if (TutoRollHint.also(left, v, 16) == null) return@repeat
                asked++
                val hidden = TutoRollHint.aside(left, v, 16)!!
                val d = TutoRollHint.brought(left, v, 16)
                val now = v.moved(d)
                if (d == 0f to 0f) still += "$rows rows, the other $far rows away, the first on row $at, touch ${n + 1}: nothing moves"
                else if (!TutoRollHint.inRows(left[0], now) && !TutoRollHint.inRows(hidden, now)) still += "$rows rows, $far, row $at, touch ${n + 1}: neither note in sight"
                v = now
            }
        }
        assertTrue("the line never pointed away: the test checks nothing", asked > 100)
        assertTrue("${still.size} touches of $asked on « Touche ici » do nothing:\n" + still.take(20).joinToString("\n"), still.isEmpty())
    }

    /** The touch that could go no further towards the hidden note shows it, whole, and the line then tells of the one left behind. */
    @Test
    fun theTouchThatCouldNotGoFurtherShowsTheHiddenNote() {
        val left = listOf(Note(0, tick, 60), Note(4 * tick, tick, 78))
        // The first note on the lowest row in sight: no going up keeps it in sight.
        val v = view(60, 8)
        assertEquals(TutoWay.Higher, TutoRollHint.also(left, v, 16))
        val now = v.moved(TutoRollHint.brought(left, v, 16))
        assertTrue("the hidden note is not in sight after the touch", TutoRollHint.inRows(left[1], now))
        assertEquals("the line tells of the note left behind", TutoWay.Lower, TutoRollHint.also(left, now, 16))
    }
}
