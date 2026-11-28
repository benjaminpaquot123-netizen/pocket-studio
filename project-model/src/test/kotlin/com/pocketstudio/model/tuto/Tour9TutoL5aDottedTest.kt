package com.pocketstudio.model.tuto

import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.Project
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 9 lot L5a, F059 (t9-39, jump_up): « les 2 notes en pointillé de la mesure 1 », then a 3rd dotted note waited in bar 3
 * that no text spoke of. The notes dotted when a step opens are the ones its text counts, in the bars it names.
 */
class Tour9TutoL5aDottedTest {
    private val said = Regex("(?:les )?(\\d+) notes en pointillé (?:de la mesure (\\d+)(?: et les (\\d+) de la mesure (\\d+))?|des mesures (\\d+) et (\\d+))")

    @Test
    fun theNotesDottedAreTheOnesTheTextCounts() {
        val bad = ArrayList<String>()
        var seen = 0
        for (style in TutoCourse.STYLES) for (level in TutoLevel.entries) {
            val target = TutoTarget.of(style) ?: continue
            var p: Project = target.blank()
            for (s in TutoCourse.of(style)!!.chaptersFor(level).flatMap { it.steps }) {
                var guard = 0
                while (guard++ < 10) p = TutoChecks.need(s, p, target)?.let { TutoChecks.fulfil(it, s, p, target)?.first } ?: break
                val m = said.find(s.textFor(level))
                val ghosts = TutoChecks.ghostNotes(s, p, target)
                if (m != null && ghosts != null && !TutoChecks.done(s, p, target)) {
                    seen++
                    val (track, dotted) = ghosts
                    val have = TutoChecks.notesOf(p, track).map { it.pitch to it.start }.toSet()
                    val left = dotted.filter { (it.pitch to it.start) !in have }
                    val g = m.groupValues
                    val bars = listOf(g[2], g[4], g[5], g[6]).filter { it.isNotEmpty() }.map { it.toInt() }.toSet()
                    val bar = p.stepsPerBar * NoteClip.TICKS_PER_STEP
                    val leftBars = left.map { it.start / bar + 1 }.toSet()
                    val n = g[1].toInt() + (g[3].toIntOrNull() ?: 0)
                    if (left.size != n || !bars.containsAll(leftBars)) {
                        bad += "$style/${level.name}/${s.id}: « ${m.value} », dotted ${left.size} in bars $leftBars"
                    }
                }
                if (!TutoChecks.done(s, p, target)) p = TutoChecks.show(s, p, target) ?: p
            }
        }
        assertTrue("only $seen steps counted: the test checks nothing", seen >= 5)
        assertTrue("dotted notes the text does not count:\n" + bad.joinToString("\n"), bad.isEmpty())
    }
}
