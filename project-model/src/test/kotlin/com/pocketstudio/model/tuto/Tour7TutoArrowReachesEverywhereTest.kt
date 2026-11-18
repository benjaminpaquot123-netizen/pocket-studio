package com.pocketstudio.model.tuto

import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

/**
 * Tour 7, control of « fable-repar » (seat a, V1, the bass arrow): dotted notes on two pitches far apart, the roll
 * stopped before the note aimed at, the arrow stayed and a new touch did nothing. Played here as the roll plays it
 * (`PianoRoll`: placed on the line's middle pitch, each touch moved by [TutoRollHint.brought], rounded and held within
 * the keyboard's ends), on the real lines of every style at both levels, with 3 to 10 rows in sight above the panel:
 * while the line points away a touch moves the roll and leaves a dotted note whole in sight, and the user who lays
 * what is in sight then touches the line ends with the whole line laid.
 */
class Tour7TutoArrowReachesEverywhereTest {
    private val step = 40f
    private val row = 30f
    private val low = 24
    private val high = 108
    private val tick = NoteClip.TICKS_PER_STEP
    private val bar = 16

    /** The roll: its scrolls, the width and the rows in sight (pixels), the line's length (sixteenths). */
    private class Roll(var h: Int, var v: Int, val width: Float, val sight: Float, val steps: Int)

    private fun Roll.view() = TutoRollView(h.toFloat(), h + width, v.toFloat(), v + sight, step, row, high)
    private fun Roll.maxH() = (steps * step - width).roundToInt().coerceAtLeast(0)
    // The roll leaves under its rows the room the panel covers: its last row comes up into the rows in sight.
    private fun Roll.maxV() = ((high - low + 1) * row - sight).roundToInt().coerceAtLeast(0)

    /** The roll as it opens on a step: the middle pitch in the middle of the rows in sight, the bar of the first note. */
    private fun opened(notes: List<Note>, width: Float, sight: Float): Roll {
        val steps = ((notes.maxOf { it.start + it.length } + bar * tick - 1) / (bar * tick)) * bar
        val r = Roll(0, 0, width, sight, steps)
        val pitches = notes.map { it.pitch }.sorted()
        val middle = pitches[pitches.size / 2].coerceIn(low, high)
        val first = notes.sortedWith(compareBy({ it.start }, { -it.pitch })).first()
        val x = (first.start / tick / bar) * bar * step
        val top = ((high - middle) * row + row / 2 - sight / 2).coerceAtLeast(0f)
        r.v = (top + TutoRollHint.whole(notes, TutoRollView(x, x + bar * step, top, top + sight, step, row, high))).roundToInt().coerceIn(0, r.maxV())
        r.h = x.roundToInt().coerceIn(0, r.maxH())
        return r
    }

    /** A touch on the line: the roll moved as `PianoRoll` moves it; false when nothing moved. */
    private fun Roll.touch(left: List<Note>): Boolean {
        val (dx, dy) = TutoRollHint.brought(left, view(), bar)
        val h0 = h
        val v0 = v
        h = (h + dx).roundToInt().coerceIn(0, maxH())
        v = (v + dy).roundToInt().coerceIn(0, maxV())
        return h != h0 || v != v0
    }

    private fun Roll.points(left: List<Note>) = TutoRollHint.way(left, view(), bar) != null || TutoRollHint.also(left, view(), bar) != null
    private fun Roll.inSight(left: List<Note>) = left.filter { TutoRollHint.inTime(it, view()) && TutoRollHint.inRows(it, view()) }

    /** Every line of dotted notes of the courses: (its name, its notes). */
    private fun lines(): List<Pair<String, List<Note>>> {
        val out = ArrayList<Pair<String, List<Note>>>()
        for (style in TutoCourse.STYLES) for (level in TutoLevel.entries) {
            val target = TutoTarget.of(style) ?: continue
            var p = target.blank()
            for (s in TutoCourse.of(style)!!.chaptersFor(level).flatMap { it.steps }) {
                if (s.manual || s.listening || s.check == "shared") continue
                if (s.check.startsWith("notes:")) TutoPlay.notes(s, p, target)?.second?.takeIf { it.isNotEmpty() }?.let { out += "$style $level ${s.id}" to it }
                p = TutoPlay.play(s, p, target, level).project
            }
        }
        return out
    }

    @Test
    fun aTouchOnTheLineReachesTheNoteAimedAt() {
        val faults = ArrayList<String>()
        var touches = 0
        var played = 0
        var apart = 0
        val lines = lines()
        for ((name, notes) in lines) {
            if (notes.maxOf { it.pitch } - notes.minOf { it.pitch } >= 7) apart++
            for (rows in listOf(3f, 4f, 4.5f, 5f, 6f, 7.3f, 8f, 10f)) for (width in listOf(8, 11, 16, 20)) {
                val where = "$name, $rows rows, $width sixteenths wide"
                // The user who only touches the line: each touch moves the roll while the line points away, and shows a note.
                var r = opened(notes, width * step, rows * row)
                repeat(4) { n ->
                    if (!r.points(notes)) return@repeat
                    touches++
                    if (!r.touch(notes)) faults += "$where, touch ${n + 1}: the line points away and the touch moves nothing"
                    else if (r.inSight(notes).isEmpty()) faults += "$where, touch ${n + 1}: no dotted note whole in sight after it"
                }
                // The user who lays what is in sight, then touches the line: the whole line is laid.
                r = opened(notes, width * step, rows * row)
                var left = notes.sortedWith(compareBy({ it.start }, { -it.pitch }))
                var n = 0
                played++
                while (left.isNotEmpty() && n < notes.size + 8) {
                    val seen = r.inSight(left)
                    if (seen.isNotEmpty()) { left = left - seen.toSet(); continue }
                    n++
                    touches++
                    if (!r.points(left)) { faults += "$where: ${left.size} notes left, none in sight, and the line does not point"; break }
                    if (!r.touch(left)) { faults += "$where: ${left.size} notes left (next ${TutoRollHint.next(left, r.view(), bar)}), the touch moves nothing"; break }
                    if (r.inSight(left).isEmpty() && TutoRollHint.inTime(TutoRollHint.next(left, r.view(), bar)!!, r.view())) {
                        // One touch along the time, one along the rows at most: the note aimed at is then whole in sight.
                        r.touch(left)
                        if (r.inSight(left).isEmpty()) { faults += "$where: ${left.size} notes left, the roll stops before the note aimed at"; break }
                    }
                }
                if (left.isNotEmpty() && n >= notes.size + 8) faults += "$where: ${left.size} notes never reached after $n touches"
            }
        }
        assertTrue("${faults.size} faults over $played lines played, $touches touches:\n" + faults.take(25).joinToString("\n"), faults.isEmpty())
        assertTrue("only ${lines.size} lines ($apart with pitches far apart), $touches touches: the test checks too little", lines.size >= 20 && apart >= 8 && touches >= 500)
    }
}
