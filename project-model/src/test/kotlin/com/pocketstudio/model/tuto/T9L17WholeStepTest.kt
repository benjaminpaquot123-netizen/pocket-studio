package com.pocketstudio.model.tuto

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 9, F032 (seats t9-34, t9-64, t9-73): a step passed on part of what it says. The rule: a step is done once
 * all its words ask for is there, on the pad they name.
 */
class T9L17WholeStepTest {
    private fun step(style: String, id: String): TutoStep =
        TutoCourse.of(style)!!.chaptersFor(TutoLevel.Pro).flatMap { it.steps }.firstOrNull { it.id == id }
            ?: TutoCourse.of(style)!!.chaptersFor(TutoLevel.Facile).flatMap { it.steps }.first { it.id == id }

    @Test
    fun trapsTripletWantsCells13And15() {
        // t9-73: « appui long sur les cases 13 et 15 … « 3 sur 2 cases » »: « Passed » on cell 13 alone.
        val target = TutoTarget.of("trap")!!
        val s = step("trap", "hat_triplet")
        val both = TutoChecks.withTriplet(target.blank(), "hat", target)!!
        assertTrue("both triplets: done", TutoChecks.done(s, both, target, null))
        val lane = TutoChecks.laneOf("hat")
        val triplets = both.drums.lanes[lane].hits.filter { (st, h) -> h.isTriplet && st < both.drums.steps }.keys.sorted()
        assertTrue("the line has two: $triplets", triplets.size >= 2)
        val one = both.copy(drums = both.drums.setHit(lane, triplets.last(), false))
        assertFalse("cell 13 alone: not done", TutoChecks.done(s, one, target, null))
    }
}
