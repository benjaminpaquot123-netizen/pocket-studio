package com.pocketstudio.model.tuto

import com.pocketstudio.model.allScenes
import com.pocketstudio.model.switchScene
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 6, N6: house's « variation » (and the 9 other steps that check « changed:drums ») had no target: the pad
 * stayed the step before's (the cymbal), a hit there ticked nothing and nothing said so. Each such step names
 * its pad (a drum role), and a hit on that very pad, in that part, ticks it.
 */
class Tour6TutoVariationTargetTest {
    @Test
    fun aStepOnAnotherPadNamesItsPad() {
        val missing = mutableListOf<String>()
        var checked = 0
        for (style in TutoCourse.STYLES) {
            val target = TutoTarget.of(style)!!
            val flat = TutoCourse.of(style)!!.chapters.flatMap { it.steps }
            for (s in flat.filter { it.check.startsWith("changed:drums:") }) {
                if (s.target.isEmpty() || s.target !in TutoChecks.DRUM_ROLES) { missing += "$style/${s.id}"; continue }
                // Played up to this step by « Montre-moi », then one new hit on the target's pad in the part: ticked.
                var p = target.blank()
                for (x in flat.takeWhile { it.id != s.id }) TutoChecks.show(x, p, target)?.let { p = it }
                val part = s.check.removePrefix("changed:drums:")
                val idx = p.allScenes.indexOfFirst { it.label == part && !it.isEmpty }
                assertTrue("$style/${s.id}: no part « $part » made before it", idx >= 0)
                val q = p.switchScene(idx)
                val base = TutoChecks.signature("drums:$part", q)
                val lane = TutoChecks.laneOf(s.target)
                val free = (0 until q.drums.steps).first { it !in q.drums.lanes[lane].hits }
                val after = q.copy(drums = q.drums.setHit(lane, free, true, 0.8f))
                assertTrue("$style/${s.id}: a hit on « ${s.target} » in « $part » does not tick it", TutoChecks.done(s, after, target, base))
                checked++
            }
        }
        assertTrue("steps on another pad without their pad as target: $missing", missing.isEmpty())
        assertTrue(checked >= 10)
    }
}
