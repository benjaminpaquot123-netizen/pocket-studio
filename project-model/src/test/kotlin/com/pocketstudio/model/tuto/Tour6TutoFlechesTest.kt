package com.pocketstudio.model.tuto

import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.Project
import com.pocketstudio.model.track
import com.pocketstudio.model.withTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 6, lot « tuto-flèches » (control seats D a, B b, H a): the line over the piano roll (« plus haut ↑ »,
 * « plus bas ↓ », « plus loin → », « ← plus tôt ») must lead a beginner to the next dotted note, whatever the
 * roll shows. Seat D a, chapter 5: « plus haut ↑ » for a note that was further right too, never found; chapter 7:
 * ↑, then →, then ↓ for the same notes. Seat H a, chapter 5: « plus bas ↓ » then « plus loin → » after « Copier ».
 *
 * A property over every step that shows dotted notes, in every tutorial (each style, Facile and Pro), at each note
 * written, for many places of the roll (a bar or less in sight, a phone's rows at 100 % and at 130 %):
 * - no hint exactly when a dotted note is whole in sight;
 * - the bar first: « plus haut / plus bas » only for a note of the time in sight, never with the panel over the rows;
 * - following the hint (the touch on the line, or the scroll it asks for, as far as it asks) reaches a dotted note
 *   in at most two moves, never by two opposite ways.
 */
class Tour6TutoFlechesTest {
    private val step = 30f
    private val row = 30f
    private val high = 108
    private val low = 24
    private val tick = NoteClip.TICKS_PER_STEP

    /** A state of a tutorial: the dotted notes left on a clip of [steps] sixteenths, [bar] of them a bar. */
    private class Case(val name: String, val left: List<Note>, val steps: Int, val bar: Int)

    private fun written(p: Project, track: Int) = p.track(track).notes.notes.map { it.pitch to it.start }.toSet()

    /** Every state a user goes through writing the dotted notes of every step, a note at a time, with the step's own buttons. */
    private fun cases(): List<Case> {
        val out = mutableListOf<Case>()
        for (style in TutoCourse.STYLES) for (level in TutoLevel.values()) {
            val target = TutoTarget.of(style)!!
            var p = target.blank()
            for (s in TutoCourse.of(style)!!.chaptersFor(level).flatMap { it.steps }) {
                var guard = 0
                while (!TutoChecks.done(s, p, target) && guard++ < 400) {
                    val need = TutoChecks.need(s, p, target)
                    if (need != null) {
                        p = TutoChecks.fulfil(need, s, p, target)?.first ?: break
                        continue
                    }
                    val (track, dotted) = TutoChecks.ghostNotes(s, p, target) ?: break
                    val have = written(p, track)
                    val left = dotted.filter { (it.pitch to it.start) !in have && it.pitch in low..high }
                    if (left.isEmpty()) break
                    val clip = p.track(track).notes
                    out += Case("$style/${level.name}/${s.id} (${left.size} left)", left, clip.steps, p.stepsPerBar)
                    // A beginner writes the one that comes first.
                    val next = left.minWith(compareBy({ it.start }, { -it.pitch }))
                    p = p.withTrack(p.track(track).copy(notes = clip.copy(notes = clip.notes + next)))
                }
                if (!TutoChecks.done(s, p, target)) p = TutoChecks.show(s, p, target) ?: p
            }
        }
        return out
    }

    private fun view(left: Float, top: Float, width: Float, height: Float) = TutoRollView(left, left + width, top, top + height, step, row, high)
    private fun x(n: Note) = n.start / tick.toFloat() * step
    private fun y(n: Note) = (high - n.pitch) * row
    private fun inTime(n: Note, v: TutoRollView) = x(n) >= v.left - 1f && x(n) + step <= v.right + 1f
    private fun inRows(n: Note, v: TutoRollView) = y(n) >= v.top - 1f && y(n) + row <= v.bottom + 1f
    private fun seen(left: List<Note>, v: TutoRollView) = left.any { inTime(it, v) && inRows(it, v) }
    private fun across(w: TutoWay) = w == TutoWay.Later || w == TutoWay.Earlier

    /** The places of the roll tried for [c]: every half bar, every fifth row, a bar or two thirds of one wide, 10 or 4 rows high. */
    private fun views(c: Case, heights: List<Float>): List<TutoRollView> {
        val whole = c.steps * step
        val out = mutableListOf<TutoRollView>()
        for (width in listOf(c.bar * step, c.bar * step * 2 / 3).map { it.coerceAtMost(whole) }) for (height in heights) {
            var l = 0f
            while (l <= whole - width + 0.5f) {
                var t = 0f
                while (t <= (high - low + 1) * row - height + 0.5f) {
                    out += view(l, t, width, height)
                    t += 5 * row
                }
                l += c.bar * step / 2
            }
        }
        return out
    }

    /** The view after the scroll [w] asks for, as short as it can be: the note of the line just whole along that axis. */
    private fun scrolled(c: Case, v: TutoRollView, w: TutoWay): TutoRollView {
        val n = TutoRollHint.next(c.left, v)!!
        val width = v.right - v.left
        val height = v.bottom - v.top
        return when (w) {
            TutoWay.Later -> view(x(n) + step - width, v.top, width, height)
            TutoWay.Earlier -> view(x(n), v.top, width, height)
            TutoWay.Higher -> view(v.left, y(n), width, height)
            TutoWay.Lower -> view(v.left, y(n) + row - height, width, height)
        }
    }

    /** The view after a touch on the line. */
    private fun touched(c: Case, v: TutoRollView): TutoRollView {
        val (dx, dy) = TutoRollHint.brought(c.left, v, c.bar)
        val width = v.right - v.left
        val l = (v.left + dx).coerceIn(0f, (c.steps * step - width).coerceAtLeast(0f))
        return view(l, v.top + dy, width, v.bottom - v.top)
    }

    private fun failures(check: (Case, TutoRollView) -> String?): List<String> {
        val all = cases()
        assertTrue("the tutorials show dotted notes in ${all.size} states only", all.size > 1000)
        // Every style that writes on a piano roll (the three acid ones write their 303's steps, which have no roll).
        val rolls = TutoCourse.STYLES.filter { st -> TutoCourse.of(st)!!.chapters.any { ch -> ch.steps.any { it.check.startsWith("notes:") && it.check != "notes:acid" } } }
        assertTrue("styles with a piano roll: ${rolls.size}", rolls.size >= 30)
        assertEquals(rolls.toSet(), all.map { it.name.substringBefore('/') }.toSet())
        val bad = LinkedHashMap<String, String>()
        for (c in all) for (v in views(c, listOf(10 * row, 4 * row))) check(c, v)?.let { bad.putIfAbsent(c.name.substringBeforeLast(" ("), "${c.name}, time ${v.left}..${v.right}, rows ${v.top}..${v.bottom}: $it") }
        return bad.values.toList()
    }

    private fun report(bad: List<String>) = assertTrue("${bad.size} steps:\n" + bad.take(12).joinToString("\n"), bad.isEmpty())

    @Test
    fun noHintExactlyWhenADottedNoteIsInSight() = report(failures { c, v ->
        val w = TutoRollHint.way(c.left, v)
        when {
            seen(c.left, v) && w != null -> "a dotted note is whole in sight, the line says $w"
            !seen(c.left, v) && w == null -> "no dotted note in sight, the line says « à toucher »"
            else -> null
        }
    })

    @Test
    fun theBarIsSaidBeforeTheRows() = report(failures { c, v ->
        val w = TutoRollHint.way(c.left, v) ?: return@failures null
        val n = TutoRollHint.next(c.left, v)!!
        when {
            !across(w) && !inTime(n, v) -> "$w for the note ${n.pitch}@${n.start / tick}, which is not in the time in sight"
            across(w) && c.left.any { inTime(it, v) } -> "$w while a dotted note is in the time in sight"
            w == TutoWay.Later && x(n) < v.left || w == TutoWay.Earlier && x(n) > v.left -> "$w, the note ${n.pitch}@${n.start / tick} is the other way"
            w == TutoWay.Higher && y(n) > v.top || w == TutoWay.Lower && y(n) < v.top -> "$w, the note ${n.pitch}@${n.start / tick} is the other way"
            else -> null
        }
    })

    @Test
    fun followingTheHintReachesTheNoteInTwoMoves() = report(failures { c, v ->
        for (byTouch in listOf(true, false)) {
            var now = v
            val ways = mutableListOf<TutoWay>()
            while (true) {
                val w = TutoRollHint.way(c.left, now) ?: break
                ways += w
                if (ways.size > 2) return@failures "more than two moves (${if (byTouch) "touches" else "scrolls"}): $ways…"
                now = if (byTouch) touched(c, now) else scrolled(c, now, w)
            }
            if (!seen(c.left, now)) return@failures "after $ways (${if (byTouch) "touches" else "scrolls"}) no dotted note is in sight"
            if (ways.size == 2 && across(ways[0]) == across(ways[1])) return@failures "two moves along the same axis: $ways"
            if (ways.size == 2 && !across(ways[0])) return@failures "the rows before the bar: $ways"
        }
        null
    })

    /** The tutorial's panel open over the roll (seat D a, screen 125: not a row in sight, the line said « plus bas ↓ »). */
    @Test
    fun noRowInSightNeverSendsUpOrDown() {
        val all = cases()
        val bad = LinkedHashMap<String, String>()
        for (c in all) for (v in views(c, listOf(0f, row / 2))) {
            val w = TutoRollHint.way(c.left, v)
            if (w != null && !across(w)) bad.putIfAbsent(c.name.substringBeforeLast(" ("), "${c.name}, time ${v.left}..${v.right}, rows ${v.top}..${v.bottom}: $w with no row in sight")
        }
        report(bad.values.toList())
    }

    /** Seat D a, chapter 5 (techno, « La basse », step 3): bar 4's last note, higher and further right than what is in sight. */
    @Test
    fun technoBassLastNoteOfBarFourIsSaidFurtherFirst() {
        val c = cases().last { it.name.startsWith("techno/Facile/bass_end") }
        assertEquals("the last dotted note of the bass", 1, c.left.size)
        val n = c.left.single()
        // The first ten sixteenths of its bar in sight, ten rows whose top one is an octave under the note.
        val bar = n.start / tick / c.bar
        assertTrue("the note is in the second half of its bar", n.start / tick - bar * c.bar >= 10)
        val v = view((bar * c.bar - 6) * step, y(n) + 12 * row, c.bar * step, 10 * row)
        assertEquals(TutoWay.Later, TutoRollHint.way(c.left, v))
        val there = scrolled(c, v, TutoWay.Later)
        assertEquals(TutoWay.Higher, TutoRollHint.way(c.left, there))
        assertEquals(null, TutoRollHint.way(c.left, scrolled(c, there, TutoWay.Higher)))
    }
}
