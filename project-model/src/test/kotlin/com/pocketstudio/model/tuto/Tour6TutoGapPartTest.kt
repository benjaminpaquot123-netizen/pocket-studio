package com.pocketstudio.model.tuto

import com.pocketstudio.model.allScenes
import com.pocketstudio.model.switchScene
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 6, seat D a-8 (ja, techno, the course chained chapter after chapter): at the step « gap » the screen said
 * « もうできています » and, together, « 赤い×をタップ »: the hits to cross out were worked out on the part shown
 * (A · Groove, the kick and the hats of its last beat), not on the step's part (the Montée). Every style's gap step,
 * reached by the course itself: crosses only in the step's own part, never on another part shown.
 */
class Tour6TutoGapPartTest {
    @Test
    fun theCrossesAreTheStepsOwnPart() {
        val bad = ArrayList<String>()
        var checked = 0
        for (style in STYLES) {
            val course = TutoCourse.of(style) ?: continue
            val target = TutoTarget.of(style) ?: continue
            for (level in TutoLevel.entries) {
                var p = target.blank()
                for (s in course.chaptersFor(level).flatMap { it.steps }) {
                    if (s.check == "gap") {
                        val part = TutoChecks.partsOf(s).first()
                        val at = p.allScenes.indexOfFirst { it.label == part && !it.isEmpty }
                        if (at < 0) { bad += "$style ${level.name}: no « $part » at the gap step"; break }
                        // The part shown is another one (the Groove, as the seat's « パート：A »).
                        val other = p.allScenes.indices.firstOrNull { it != at && !p.allScenes[it].isEmpty } ?: break
                        val elsewhere = TutoChecks.toErase(s, p.switchScene(other), target).orEmpty().filterValues { it.isNotEmpty() }
                        if (elsewhere.isNotEmpty()) bad += "$style ${level.name}: crosses on « ${p.allScenes[other].label} » for a gap in « $part »: $elsewhere"
                        // In its own part: its crosses are the hits of its last beat (the check's own reading).
                        val own = p.switchScene(at)
                        val left = TutoChecks.toErase(s, own, target).orEmpty().filterValues { it.isNotEmpty() }
                        if (left.isEmpty() != TutoChecks.done(s, own, target)) bad += "$style ${level.name}: crosses $left in « $part », step done: ${TutoChecks.done(s, own, target)}"
                        checked++
                        break
                    }
                    if (!TutoChecks.done(s, p, target)) p = TutoChecks.show(s, p, target) ?: p
                }
            }
        }
        assertTrue("no gap step met", checked > 0)
        assertTrue("${bad.size} gap steps crossing another part:\n" + bad.take(40).joinToString("\n"), bad.isEmpty())
    }

    companion object {
        val STYLES = listOf("techno", "house", "trance", "dnb", "jungle", "dubstep", "hard_techno", "tech_house")
    }
}
