package com.pocketstudio.model.tuto

import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

/**
 * Tour 8, lot T0a (control p8b, seat a V1 still there; seat b in ja; review of « fable-repar » p8, « À ranger »):
 * the house bass, its low and high « La » 13 rows apart, more than the rows above the tutorial's panel. « Touche ici »
 * showed the high ones, the line then said « ↓ Touche ici » for the low ones, and touching again without laying
 * anything flipped the view back: low, high, low… The roll is played here as the phone draws it: the rows in sight end
 * at the tutorial's panel, and under the lowest row the roll has as much room as the panel covers (`covered`), so the
 * lowest notes can come above it. Every line of every course:
 * - a touch never flips the view back to where it was two touches before (no alternation);
 * - the note a touch lights is whole in the rows in sight, above the panel, once the roll has moved.
 */
class Tour8TutoTouchInSightTest {
    private val step = 40f
    private val row = 30f
    private val low = 24
    private val high = 108
    private val tick = NoteClip.TICKS_PER_STEP
    private val bar = 16

    /**
     * The roll: [height] pixels tall, the tutorial's panel over its lowest [panel] pixels (the rows in sight end at its
     * top), and the same [panel] pixels of room under the lowest row (`covered` in `PianoRoll`).
     */
    private class Roll(var h: Int, var v: Int, val width: Float, val height: Float, val panel: Float, val steps: Int) {
        val sight get() = height - panel
    }

    private fun Roll.view() = TutoRollView(h.toFloat(), h + width, v.toFloat(), v + sight, step, row, high)
    private fun Roll.maxH() = (steps * step - width).roundToInt().coerceAtLeast(0)
    private fun Roll.maxV() = ((high - low + 1) * row + panel - height).roundToInt().coerceAtLeast(0)
    private fun Roll.inSight(n: Note) = TutoRollHint.inTime(n, view()) && TutoRollHint.inRows(n, view())
    private fun Roll.points(left: List<Note>) = TutoRollHint.way(left, view(), bar) != null || TutoRollHint.also(left, view(), bar) != null

    /** The roll as it opens on a step: the middle pitch in the middle of the rows in sight, the bar of the first note. */
    private fun opened(notes: List<Note>, width: Float, height: Float, panel: Float): Roll {
        val steps = ((notes.maxOf { it.start + it.length } + bar * tick - 1) / (bar * tick)) * bar
        val r = Roll(0, 0, width, height, panel, steps)
        val pitches = notes.map { it.pitch }.sorted()
        val middle = pitches[pitches.size / 2].coerceIn(low, high)
        val first = notes.sortedWith(compareBy({ it.start }, { -it.pitch })).first()
        val x = (first.start / tick / bar) * bar * step
        val top = ((high - middle) * row + row / 2 - r.sight / 2).coerceAtLeast(0f)
        r.v = (top + TutoRollHint.whole(notes, TutoRollView(x, x + bar * step, top, top + r.sight, step, row, high))).roundToInt().coerceIn(0, r.maxV())
        r.h = x.roundToInt().coerceIn(0, r.maxH())
        return r
    }

    /** One touch on the line, as `PianoRoll` makes it: the note lit, then the roll moved (as far as it can go). */
    private fun Roll.touch(left: List<Note>): Note? {
        val v0 = view()
        val lit = TutoRollHint.lit(left, v0, bar, true, v.toFloat(), (maxV() - v).toFloat())
        val (dx, dy) = TutoRollHint.brought(left, v0, bar)
        h = (h + dx).roundToInt().coerceIn(0, maxH())
        v = (v + dy).roundToInt().coerceIn(0, maxV())
        return lit
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

    /** Rolls of a phone (pixels): its height, and how much of it the tutorial's panel covers (360 dp and wider; 100 % and 130 %). */
    private val rolls = listOf(330f to 200f, 330f to 160f, 420f to 230f, 420f to 180f, 480f to 220f, 540f to 240f, 600f to 260f)

    private fun play(check: (where: String, seat: Boolean, r: Roll, notes: List<Note>) -> Unit) {
        for ((name, notes) in lines()) {
            val seat = name.startsWith("house") && "bass" in name && notes.maxOf { it.pitch } - notes.minOf { it.pitch } >= 12
            for ((height, panel) in rolls) for (width in listOf(11, 16, 20)) {
                check("$name, roll $height px, panel $panel px, $width sixteenths wide", seat, opened(notes, width * step, height, panel), notes)
            }
        }
    }

    @Test
    fun touchingWithoutLayingNeverFlipsTheViewBack() {
        val faults = ArrayList<String>()
        var touches = 0
        var houseBass = 0
        play { where, seat, r, notes ->
            val left = notes.sortedWith(compareBy({ it.start }, { -it.pitch }))
            val views = ArrayList<Pair<Int, Int>>()
            views += r.h to r.v
            var n = 0
            while (n < 6 && r.points(left)) {
                n++
                touches++
                if (seat) houseBass++
                r.touch(left)
                views += r.h to r.v
                val k = views.size - 1
                if (k >= 2 && views[k] == views[k - 2] && views[k] != views[k - 1]) {
                    faults += "$where, touch $n: the view flips back (rows from ${views[k - 1].second} to ${views[k].second})"
                    break
                }
            }
        }
        assertTrue("${faults.size} lines flip the view back and forth on « Touche ici »:\n" + faults.sortedByDescending { it.startsWith("house") && "bass" in it }.take(25).joinToString("\n"), faults.isEmpty())
        assertTrue("the house bass with its two « La » was touched $houseBass times of $touches: the test checks too little", houseBass >= 10 && touches >= 200)
    }

    @Test
    fun theNoteATouchLightsIsWholeAboveThePanel() {
        val faults = ArrayList<String>()
        var touches = 0
        var houseBass = 0
        play { where, seat, r, notes ->
            var left = notes.sortedWith(compareBy({ it.start }, { -it.pitch }))
            var n = 0
            // The user lays the note lit, then touches the line again, to the line's end.
            while (left.isNotEmpty() && n < notes.size + 8) {
                if (!r.points(left)) {
                    left = left - left.filter { r.inSight(it) }.toSet()
                    if (left.isEmpty() || !r.points(left)) break
                }
                n++
                touches++
                if (seat) houseBass++
                val lit = r.touch(left)
                if (lit == null || !r.inSight(lit)) {
                    faults += "$where, touch $n: the note lit (${lit?.pitch}) is not whole above the panel (rows ${r.v}..${r.v + r.sight.toInt()})"
                    break
                }
                left = left - lit
            }
        }
        assertTrue("${faults.size} touches light a note out of sight:\n" + faults.sortedByDescending { it.startsWith("house") && "bass" in it }.take(25).joinToString("\n"), faults.isEmpty())
        assertTrue("the house bass with its two « La » was touched $houseBass times of $touches: the test checks too little", houseBass >= 10 && touches >= 200)
    }
}
