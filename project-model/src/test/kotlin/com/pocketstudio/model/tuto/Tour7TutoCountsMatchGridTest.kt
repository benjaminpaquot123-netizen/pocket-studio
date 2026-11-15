package com.pocketstudio.model.tuto

import com.pocketstudio.model.StarterWizard
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 7, lot 1 D5 (seat 1a #21, screen 101: Rythme): « touche les 8 cases en pointillé (4 par mesure) » over a grid of
 * one bar, 4 dotted cells. Every style and level, the course replayed: a drum step that counts N dotted cells shows a
 * grid holding N of them, or first offers « Passer à N mesures » that makes them.
 */
class Tour7TutoCountsMatchGridTest {
    private val count = Regex("""(\d+) cases en pointillé""")

    @Test
    fun theDottedCellsCountedAreTheCellsShown() {
        val bad = ArrayList<String>()
        var checked = 0
        for (style in TutoCourse.STYLES) {
            val course = TutoCourse.of(style) ?: continue
            val target = TutoTarget.of(style) ?: continue
            for (level in TutoLevel.entries) {
                var p = target.blank()
                for (s in course.chaptersFor(level).flatMap { it.steps }) {
                    if (s.check.startsWith("lane:") && !TutoChecks.done(s, p, target)) {
                        val text = s.textFor(level)
                        val said = count.find(text)?.groupValues?.get(1)?.toInt()
                        val part = s.part.split(',').map { it.trim() }.firstOrNull { it.isNotEmpty() }
                        val line = target.line(TutoChecks.argOf(s.check), part)
                        if (said != null && line != null) {
                            // As played, and as met by « Aller au chapitre » on a loop of one bar.
                            for ((how, q) in listOf("played" to p, "goTo" to p.copy(drums = p.drums.withSteps(p.stepsPerBar)))) {
                                val need = TutoChecks.need(s, q, target)
                                val steps = (need as? TutoNeed.Bars)?.let { it.bars * q.stepsPerBar } ?: q.drums.steps
                                val shown = StarterWizard.hits(line, steps).size
                                if (shown != said) bad += "$style ${level.name} ${s.id} $how: says $said dotted cells, the grid shows $shown (need $need)"
                            }
                            checked++
                        }
                    }
                    if (!TutoChecks.done(s, p, target)) p = TutoChecks.show(s, p, target) ?: p
                }
            }
        }
        assertTrue("no counted step met", checked > 0)
        assertTrue("${bad.size} counts off:\n" + bad.take(60).joinToString("\n"), bad.isEmpty())
    }
}
