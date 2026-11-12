package com.pocketstudio.model.tuto

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

/**
 * Tour 6, T3: a lane step compared the cells only, never the strength: techno's clap step asks for « un petit coup
 * doux » and a hit at 100 % there ticked it. A step that names the « Doux » button (in every level's words) wants its
 * soft cells soft; and a step that speaks of « 2 mesures » has a loop of 2 bars by then (techno's clap had 16 cells).
 */
class Tour6TutoSoftHitTest {
    private fun asksSoft(s: TutoStep) = listOfNotNull(s.text, s.textPro).all { "« Doux »" in it }

    @Ignore("T3: on with TutoChecks.SOFT_HITS_CHECKED, once TutoJourneyTest plays soft cells with « Doux » (not this team's file)")
    @Test
    fun aSoftCellPlayedLoudDoesNotTick() {
        val checked = mutableListOf<String>()
        for (style in TutoCourse.STYLES) {
            val target = TutoTarget.of(style)!!
            for (s in TutoCourse.of(style)!!.chapters.flatMap { it.steps }.filter { it.check.startsWith("lane:") && asksSoft(it) }) {
                val shown = TutoChecks.show(s, target.blank(), target)!!
                assertTrue("$style/${s.id}: « Montre-moi » does not tick it", TutoChecks.done(s, shown, target))
                val lane = TutoChecks.laneOf(TutoChecks.argOf(s.check))
                val soft = shown.drums.lanes[lane].hits.filterValues { it.velocity < 0.5f }.keys
                assertTrue("$style/${s.id}: no soft cell", soft.isNotEmpty())
                val loud = shown.copy(drums = shown.drums.copy(lanes = shown.drums.lanes.mapIndexed { i, l ->
                    if (i != lane) l else l.copy(hits = l.hits.mapValues { (k, h) -> if (k in soft) h.copy(velocity = 1f) else h })
                }))
                assertFalse("$style/${s.id}: its soft hits played at 100 % tick it", TutoChecks.done(s, loud, target))
                checked += "$style/${s.id}"
            }
        }
        assertTrue("techno's clap is not checked: $checked", "techno/clap_do" in checked)
    }

    @Test
    fun aStepThatSaysTwoBarsHasTwoBars() {
        for (style in TutoCourse.STYLES) {
            val target = TutoTarget.of(style)!!
            var longest = 0
            for (s in TutoCourse.of(style)!!.chapters.flatMap { it.steps }) {
                if (!s.check.startsWith("lane:") || s.part.isNotEmpty()) continue
                val line = target.line(TutoChecks.argOf(s.check)) ?: continue
                longest = maxOf(longest, line.pattern.length)
                for (text in listOfNotNull(s.text, s.textPro)) {
                    if ("des 2 mesures" in text && ("Passer à 2 mesures" !in text || "si on te le propose" in text)) {
                        assertTrue("$style/${s.id}: the loop has ${longest / 16} bar(s) here, the step says: $text", longest >= 32)
                    }
                }
            }
        }
    }
}
