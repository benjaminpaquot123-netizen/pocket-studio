package com.pocketstudio.model.tuto

import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 7, seat 1a (fr, phone, 100 %, Simple, house chapter 5 « Basse · mesure 4/8 », screen 45): after a touch on
 * « Encore 5 notes en pointillé », three dotted notes sat half under the tutorial's panel and the line showed no
 * arrow. The touch centred the next note in the rows in sight; a note of the same time, a few rows lower, was left
 * cut by the panel's edge. A touch on the line never leaves a dotted note of the time in sight cut by an edge of the rows in sight.
 */
class Tour7TutoNoteNotCutTest {
    private val step = 40f
    private val row = 30f
    private val high = 96
    private val tick = NoteClip.TICKS_PER_STEP

    private fun cut(n: Note, v: TutoRollView): Boolean {
        val y = TutoRollHint.y(n, v)
        return TutoRollHint.inTime(n, v) && !TutoRollHint.inRows(n, v) && y + row > v.top + 1f && y < v.bottom - 1f
    }

    @Test
    fun aTouchLeavesNoDottedNoteHalfUnderThePanel() {
        val bad = ArrayList<String>()
        // Rows in sight of every height between 7 and 12 rows (the panel folded or not, 100 % or 130 %).
        for (h in 210..360 step 5) for (lowNote in 60..72) for (gap in 2..9) {
            val a = Note(0, tick, lowNote + gap)
            val b = Note(4 * tick, tick, lowNote)
            val v = TutoRollView(0f, 16 * step, 0f, h.toFloat(), step, row, high)
            val (dx, dy) = TutoRollHint.brought(listOf(a, b), v, 16)
            val now = v.copy(left = v.left + dx, right = v.right + dx, top = v.top + dy, bottom = v.bottom + dy)
            val halves = listOf(a, b).filter { cut(it, now) }
            if (halves.isNotEmpty()) bad += "rows $h px, notes ${a.pitch}/${b.pitch}: ${halves.map { it.pitch }} cut (rows ${now.top}..${now.bottom})"
        }
        assertTrue("${bad.size} touches leave a dotted note cut:\n" + bad.take(10).joinToString("\n"), bad.isEmpty())
    }
}
