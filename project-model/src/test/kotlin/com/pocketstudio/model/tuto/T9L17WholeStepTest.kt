package com.pocketstudio.model.tuto

import com.pocketstudio.model.Project
import com.pocketstudio.model.allScenes
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

    /** The course of [style] at Pro walked until step [id] (not done yet), each step before made as « Montre-moi » would. */
    private fun upTo(style: String, id: String): Project {
        val target = TutoTarget.of(style)!!
        var p = target.blank()
        for (s in TutoCourse.of(style)!!.chaptersFor(TutoLevel.Pro).flatMap { it.steps }) {
            if (s.id == id) return p
            if (s.listening) continue
            var tries = 0
            while (!TutoChecks.done(s, p, target, null) && tries++ < 8) {
                val need = TutoChecks.need(s, p, target, null)
                val q = need?.let { TutoChecks.fulfil(it, s, p, target)?.first } ?: TutoChecks.show(s, p, target) ?: break
                if (q == p) break
                p = q
            }
        }
        error("no step $id")
    }

    @Test
    fun aCymbalOnTheDropAndTheDrop2WantsBoth() {
        // t9-34: « pose une cymbale sur le 1er temps du Drop et du Drop 2 »: « Passed » on the Drop alone.
        for (style in listOf("house", "speed_garage")) {
            val target = TutoTarget.of(style)!!
            val s = TutoCourse.of(style)!!.chaptersFor(TutoLevel.Pro).flatMap { it.steps }.first { it.check == "crash" && "Drop 2" in it.part }
            val p = upTo(style, s.id)
            val drop = p.allScenes.indexOfFirst { TutoChecks.labelIs(it.label, "Drop") }
            assertTrue("$style: a Drop and a Drop 2 by then", drop >= 0 && p.allScenes.any { TutoChecks.labelIs(it.label, "Drop 2") })
            val one = TutoChecks.inScene(p, "Drop") { q -> q.copy(drums = q.drums.setHit(TutoChecks.CRASH_LANE, 0, true)) }!!
            assertFalse("$style: the Drop's cymbal alone: not done", TutoChecks.done(s, one, target, null))
            val both = TutoChecks.show(s, p, target)!!
            assertTrue("$style: shown: done", TutoChecks.done(s, both, target, null))
        }
    }

    @Test
    fun aStepOnThePadOfTheTomsChecksATomPad() {
        // t9-64 (synthwave, « Charley et tom »): « sur le pad des toms » ticked on the « Rim » pad, the pad it checks.
        val toms = setOf(7, 8, 9)
        val wrong = TutoCourse.STYLES.flatMap { style ->
            val target = TutoTarget.of(style) ?: return@flatMap emptyList()
            TutoLevel.entries.flatMap { l -> TutoCourse.of(style)!!.chaptersFor(l).flatMap { it.steps } }.distinctBy { it.id }
                .filter { s -> listOfNotNull(s.text, s.textPro).any { "pad des toms" in it } }
                .filter { s -> s.target.isEmpty() || !toms.containsAll(TutoChecks.lanesOf(s.target, target)) }
                .map { "$style/${it.id}" }
        }
        assertTrue("« pad des toms » on another pad: $wrong", wrong.isEmpty())
    }
}
