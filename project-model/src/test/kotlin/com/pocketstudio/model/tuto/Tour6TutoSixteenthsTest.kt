package com.pocketstudio.model.tuto

import com.pocketstudio.model.Hit
import com.pocketstudio.model.StarterWizard
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lot « tuto-flèches », control seat B b (techno Pro, chapter 4): « pose les charleys fermés en doubles croches » and
 * the four hits of the offbeats were refused (« 4 coups en trop ici »): the line leaves them to the open hat. A pad's
 * step accepts what its words ask for: when they say « en doubles croches » with nothing more, every sixteenth of
 * the grid, written as the line has them where it has some, ticks the step. In every tutorial, Facile and Pro.
 */
class Tour6TutoSixteenthsTest {
    @Test
    fun aPadSaidInSixteenthsAcceptsEverySixteenth() {
        val bad = mutableListOf<String>()
        var seen = 0
        for (style in TutoCourse.STYLES) for (level in TutoLevel.values()) {
            val target = TutoTarget.of(style)!!
            var p = target.blank()
            for (s in TutoCourse.of(style)!!.chaptersFor(level).flatMap { it.steps }) {
                val text = s.textFor(level)
                if (s.check.startsWith("lane:") && "en doubles croches" in text) {
                    seen++
                    var q = p
                    var g = 0
                    while (g++ < 5) {
                        val need = TutoChecks.need(s, q, target) ?: break
                        q = TutoChecks.fulfil(need, s, q, target)?.first ?: break
                    }
                    val line = target.line(TutoChecks.argOf(s.check), s.part.split(',').map { it.trim() }.firstOrNull { it.isNotEmpty() })!!
                    val (lane, cells) = TutoChecks.hint(s, target, q.drums.steps)!!
                    val theirs = StarterWizard.hits(line, q.drums.steps)
                    val all = (0 until q.drums.steps).associateWith { theirs[it] ?: Hit() }
                    val full = q.copy(drums = q.drums.copy(lanes = q.drums.lanes.mapIndexed { k, l -> if (k == lane) l.copy(hits = all) else l }))
                    if (!TutoChecks.done(s, full, target)) bad += "$style/${level.name}/${s.id}: « $text » — the dotted cells are ${cells.size} of ${q.drums.steps}, every sixteenth is refused"
                }
                if (!TutoChecks.done(s, p, target)) p = TutoChecks.show(s, p, target) ?: p
            }
        }
        assertTrue("pads said « en doubles croches »: $seen", seen >= 3)
        assertTrue("${bad.size} of $seen:\n" + bad.joinToString("\n"), bad.isEmpty())
    }
}
