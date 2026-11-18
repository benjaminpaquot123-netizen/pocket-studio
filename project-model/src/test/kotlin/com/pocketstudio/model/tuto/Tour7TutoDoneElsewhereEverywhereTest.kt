package com.pocketstudio.model.tuto

import com.pocketstudio.model.allScenes
import com.pocketstudio.model.switchScene
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 7, control of « fable-repar » (seat a, V4), everywhere it can happen: every style, both levels, every step of the
 * whole loop that draws a line (pads, a track's notes, the 303). The course played, a new empty part shown: nothing of
 * such a step is dotted there; in the part that holds its line, what it showed is still shown; a step not done yet
 * still dots the part shown, empty or not.
 */
class Tour7TutoDoneElsewhereEverywhereTest {
    @Test
    fun aLineDoneInAPartIsNotDottedInANewOne() {
        var met = 0
        var cells = 0
        var notes = 0
        val faults = ArrayList<String>()
        for (style in TutoCourse.STYLES) for (level in TutoLevel.entries) {
            val target = TutoTarget.of(style) ?: continue
            val steps = TutoCourse.of(style)!!.chaptersFor(level).flatMap { it.steps }
            var p = target.blank()
            for (s in steps) {
                if (s.manual || s.listening || s.check == "shared") continue
                val whole = s.part.isBlank() && (s.check.startsWith("lane:") || s.check.startsWith("notes:"))
                if (whole) {
                    // Not done yet: a new empty part shown still has the step's line dotted.
                    val fresh = p.switchScene(p.allScenes.size)
                    if (!TutoPlay.done(s, fresh, target, null) && s.check.startsWith("lane:") && TutoChecks.hint(s, target, fresh.drums.steps) != null) {
                        if (TutoPlay.hint(s, fresh, target, fresh.drums.steps, null, false) == null) faults += "$style $level ${s.id}: not done, and no cell dotted in a new part"
                    }
                }
                p = TutoPlay.play(s, p, target, level).project
                if (!whole || !TutoPlay.done(s, p, target, null)) continue
                met++
                val a = p
                val b = p.switchScene(p.allScenes.size)
                val n = b.drums.steps
                val inA = TutoPlay.hint(s, a, target, a.drums.steps, null, true)
                if (TutoChecks.hint(s, target, a.drums.steps) != null && !TutoChecks.heldElsewhere(s, a, target) && inA == null) faults += "$style $level ${s.id}: no cell left in its own part"
                TutoPlay.hint(s, b, target, n, null, true)?.let { cells++; faults += "$style $level ${s.id}: cells $it dotted in a new empty part" }
                TutoPlay.notes(s, b, target)?.second?.takeIf { it.isNotEmpty() }?.let { notes++; faults += "$style $level ${s.id}: ${it.size} notes dotted in a new empty part" }
                TutoPlay.acid(s, b, target)?.takeIf { it.isNotEmpty() }?.let { faults += "$style $level ${s.id}: ${it.size} steps of the 303 dotted in a new empty part" }
                TutoPlay.toErase(s, b, target, null)?.takeIf { it.isNotEmpty() }?.let { faults += "$style $level ${s.id}: hits crossed out in a new empty part: $it" }
            }
        }
        assertTrue("${faults.size} of $met ($cells cells, $notes notes):\n" + faults.joinToString("\n"), faults.isEmpty())
        assertTrue("only $met steps met: the test checks too little", met >= 60)
    }
}
