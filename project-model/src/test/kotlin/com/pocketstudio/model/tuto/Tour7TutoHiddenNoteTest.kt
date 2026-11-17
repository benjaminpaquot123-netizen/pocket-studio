package com.pocketstudio.model.tuto

import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 7, control « tuto2 », seat a (fr, phone, house « Basse · mesure 4/8 »): « Encore 6 notes en pointillé à
 * toucher », five of them in sight, the sixth higher than the rows in sight, with no arrow; a touch on the line
 * changed nothing (the note it spoke of was in sight already). A touch on the line brings the dotted notes of the
 * time in sight that are out of the rows in sight into view: all of them when they fit the rows, else the nearest one.
 */
class Tour7TutoHiddenNoteTest {
    private val step = 40f
    private val row = 30f
    private val high = 96
    private val tick = NoteClip.TICKS_PER_STEP

    /** Five notes in the rows in sight (Do3 to Ré3), the sixth [above] rows over the highest of them. */
    private fun notes(above: Int) = (0 until 5).map { Note(it * 2 * tick, tick, 48 + it % 3) } + Note(12 * tick, tick, 50 + above)

    /** The rows from [low] up, [rows] of them, the first bar in sight. */
    private fun view(low: Int, rows: Int) = TutoRollView(0f, 16 * step, (high - (low + rows - 1)) * row, (high - low + 1) * row, step, row, high)

    private fun TutoRollView.moved(d: Pair<Float, Float>) = copy(left = left + d.first, right = right + d.first, top = top + d.second, bottom = bottom + d.second)

    @Test
    fun aTouchBringsTheNoteOutOfTheRowsIntoView() {
        val bad = ArrayList<String>()
        // The rows in sight begin two rows under the lowest note; the sixth is one or two rows over them: the six fit.
        for (rows in 8..10) for (above in rows - 4..rows - 3) {
            val left = notes(above)
            val v = view(46, rows)
            val hidden = left.filter { !TutoRollHint.inRows(it, v) }
            assertTrue("the test checks nothing: no dotted note out of the rows in sight", hidden.size == 1 && left.size - hidden.size == 5)
            val now = v.moved(TutoRollHint.brought(left, v, 16))
            val out = left.filter { !TutoRollHint.inRows(it, now) }
            if (out.isNotEmpty()) bad += "$rows rows, note ${hidden[0].pitch}: after the touch ${out.map { it.pitch }} out of the rows in sight"
        }
        assertTrue("${bad.size} touches leave a dotted note hidden:\n" + bad.joinToString("\n"), bad.isEmpty())
    }

    @Test
    fun aNoteTooFarToFitComesIntoViewAlone() {
        // An octave and a half over: the six do not fit 8 rows; the touch shows the hidden one, the next touch the others.
        val left = notes(18)
        val v = view(48, 8)
        val hidden = left.last()
        val now = v.moved(TutoRollHint.brought(left, v, 16))
        assertTrue("the note ${hidden.pitch} is still out of the rows in sight after the touch", TutoRollHint.inRows(hidden, now))
    }
}
