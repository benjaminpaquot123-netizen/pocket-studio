package com.pocketstudio.model.tuto

import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

/**
 * Tour 7, second control of « fable-repar » (seat a, V1 still there; fr, phone, house, « Basse · mesure 1/8 » and
 * « mesure 4/8 »): the low « La » in sight, the high ones an octave over them, « ↑ … Touche ici ». The first touch
 * only slid the low notes down to the edge of the tutorial's panel, the high ones still out of the screen; it took
 * a second touch to see them. Played as the roll plays it (`PianoRoll`), on the real lines of every style: while the
 * line points away, ONE touch brings into the rows in sight, whole, a dotted note that was not in sight before it.
 */
class Tour7TutoTouchShowsNewNoteTest {
    private val step = 40f
    private val row = 30f
    private val low = 24
    private val high = 108
    private val tick = NoteClip.TICKS_PER_STEP
    private val bar = 16

    private class Roll(var h: Int, var v: Int, val width: Float, val sight: Float, val steps: Int)

    private fun Roll.view() = TutoRollView(h.toFloat(), h + width, v.toFloat(), v + sight, step, row, high)
    private fun Roll.maxH() = (steps * step - width).roundToInt().coerceAtLeast(0)
    private fun Roll.maxV() = ((high - low + 1) * row - sight).roundToInt().coerceAtLeast(0)
    private fun Roll.inSight(left: List<Note>) = left.filter { TutoRollHint.inTime(it, view()) && TutoRollHint.inRows(it, view()) }
    private fun Roll.points(left: List<Note>) = TutoRollHint.way(left, view(), bar) != null || TutoRollHint.also(left, view(), bar) != null

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

    private fun Roll.touch(left: List<Note>) {
        val (dx, dy) = TutoRollHint.brought(left, view(), bar)
        h = (h + dx).roundToInt().coerceIn(0, maxH())
        v = (v + dy).roundToInt().coerceIn(0, maxV())
    }

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
    fun oneTouchBringsADottedNoteThatWasOutOfSight() {
        val faults = ArrayList<String>()
        var touches = 0
        var houseBass = 0
        for ((name, notes) in lines()) {
            // The seat's own line: the house bass, its low and high « La » an octave apart.
            val seat = name.startsWith("house") && "bass" in name && notes.maxOf { it.pitch } - notes.minOf { it.pitch } >= 12
            for (rows in listOf(3f, 4f, 4.5f, 5f, 6f, 7.3f, 8f, 10f)) for (width in listOf(8, 11, 16, 20)) {
                val where = "$name, $rows rows, $width sixteenths wide"
                // The user lays what is in sight, then touches the line; and the user who only touches it.
                for (lays in listOf(true, false)) {
                    val r = opened(notes, width * step, rows * row)
                    var left = notes.sortedWith(compareBy({ it.start }, { -it.pitch }))
                    var n = 0
                    while (left.isNotEmpty() && n < notes.size + 8) {
                        if (lays) left = left - r.inSight(left).toSet()
                        if (left.isEmpty() || !r.points(left)) break
                        n++
                        touches++
                        if (seat) houseBass++
                        val before = r.inSight(left).toSet()
                        r.touch(left)
                        val brought = r.inSight(left).filter { it !in before }
                        if (brought.isEmpty()) {
                            faults += "$where, touch $n (${if (lays) "laying" else "touching only"}): ${left.size} left, no new dotted note in sight (still ${r.inSight(left).map { it.pitch }.distinct()})"
                            break
                        }
                    }
                }
            }
        }
        assertTrue("${faults.size} touches of $touches bring no new dotted note into sight:\n" + faults.sortedByDescending { it.startsWith("house") && "bass" in it }.take(25).joinToString("\n"), faults.isEmpty())
        assertTrue("the house bass with its two « La » was touched $houseBass times of $touches: the test checks too little", houseBass >= 20 && touches >= 500)
    }
}
