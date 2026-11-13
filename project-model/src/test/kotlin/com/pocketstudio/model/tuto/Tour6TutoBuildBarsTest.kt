package com.pocketstudio.model.tuto

import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.allScenes
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lot « tuto-flèches », control seats D a and B b (techno): the Montée's step says its pad goes faster « mesure après
 * mesure » (Pro: « 4 mesures sans kick »), the song's step says each part comes with 8 bars, and the Montée came with
 * 1 bar (« F · Build-up × 1 · 1 Takt »): its pad went faster beat after beat, and « dernière mesure » of chapter 12
 * meant nothing. In every tutorial whose part builds up: that part, made by its step, is 4 bars long at least, a bar
 * for each speed of its pad.
 */
class Tour6TutoBuildBarsTest {
    @Test
    fun aPartThatBuildsBarAfterBarIsFourBarsLong() {
        val bad = mutableListOf<String>()
        var seen = 0
        var byHands = 0
        for (style in TutoCourse.STYLES) for (level in TutoLevel.values()) {
            val target = TutoTarget.of(style)!!
            var p = target.blank()
            for (s in TutoCourse.of(style)!!.chaptersFor(level).flatMap { it.steps }) {
                if (s.build.isNotEmpty()) {
                    val label = s.check.removePrefix("part:").substringBeforeLast(':')
                    val grooveSteps = (p.allScenes.firstOrNull { it.label.equals("Groove", ignoreCase = true) }?.drums ?: p.drums).steps
                    var guard = 0
                    while (guard++ < 5) {
                        val need = TutoChecks.need(s, p, target) ?: break
                        p = TutoChecks.fulfil(need, s, p, target)?.first ?: break
                    }
                    val made = TutoChecks.partContent(p, label)
                    assertTrue("$style/${level.name}/${s.id}: no part « $label » made", made != null)
                    seen++
                    // What « + F · Montée » brings into the song: the part once, as long as its longest loop.
                    val bars = made!!.stepsFor(p.stepsPerBar) / p.stepsPerBar
                    if (bars < 4) bad += "$style/${level.name}/${s.id}: « $label » is $bars bar(s) long (the Groove's pads: ${grooveSteps / p.stepsPerBar} bar(s))"
                }
                // The pads by hand, as a beginner does: the dotted cells touched on the grid as long as it is (« Montre-moi »
                // writes the example's whole line, up to 4 bars, which no seat did).
                if (s.check.startsWith("lane:") && !TutoChecks.done(s, p, target)) {
                    var g = 0
                    while (g++ < 5) {
                        val need = TutoChecks.need(s, p, target) ?: break
                        p = TutoChecks.fulfil(need, s, p, target)?.first ?: break
                    }
                    val line = target.line(TutoChecks.argOf(s.check), s.part.split(',').map { it.trim() }.firstOrNull { it.isNotEmpty() })
                    TutoChecks.hint(s, target, p.drums.steps)?.let { (lane, _) ->
                        // Each cell as the line has it (a soft hit by its long press).
                        val theirs = StarterWizard.hits(line!!, p.drums.steps)
                        val byHand = p.copy(drums = p.drums.copy(lanes = p.drums.lanes.mapIndexed { k, l -> if (k == lane) l.copy(hits = l.hits + theirs) else l }))
                        if (TutoChecks.done(s, byHand, target)) { p = byHand; byHands++ }
                    }
                }
                if (!TutoChecks.done(s, p, target)) p = TutoChecks.show(s, p, target) ?: p
            }
        }
        assertTrue("parts that build up: $seen, pads written by hand: $byHands", seen >= 30 && byHands >= 100)
        assertTrue("${bad.size} of $seen:\n" + bad.joinToString("\n"), bad.isEmpty())
    }
}
