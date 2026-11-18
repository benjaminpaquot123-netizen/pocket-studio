package com.pocketstudio.model.tuto

import com.pocketstudio.model.Project
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 7, review of the lot « fable-repar » (M1): what the step's main button costs to compute. The panel asks for it
 * on the screen's thread each time a step is reached ([TutoPlay.button], `TutoController.save`), so it is measured:
 * every step of every course at both levels, on the states the course played in order goes through and on each
 * chapter opened on the tutorial's empty project (where « Poser ce qui manque » is worked out).
 */
class Tour7TutoNeedCostTest {
    private class Case(val name: String, val step: TutoStep, val p: Project, val base: Any?, val target: TutoTarget, val level: TutoLevel)

    @Test
    fun theMainButtonIsCheapToWorkOut() {
        val cases = ArrayList<Case>()
        for (style in TutoCourse.STYLES) for (level in TutoLevel.entries) {
            val target = TutoTarget.of(style)!!
            val chapters = TutoCourse.of(style)!!.chaptersFor(level)
            fun plays(from: Project, steps: List<TutoStep>, how: String): Project {
                var p = from
                for (s in steps) {
                    var held = TutoPlay.arrive(s, p)
                    p = TutoPlay.play(s, p, target, level) { q, _ ->
                        held = TutoPlay.base(s, held, q)
                        cases += Case("$style $level ${s.id} [${s.check}] $how", s, q, held, target, level)
                    }.project
                }
                return p
            }
            plays(target.blank(), chapters.flatMap { it.steps }.filter(::inReach), "in order")
            for (c in chapters) plays(target.blank(), c.steps.filter(::inReach), "chapter on the empty project")
        }
        assertTrue("nothing to measure: ${cases.size} states", cases.size > 2000)
        // Twice round before the clock is read (the first calls pay for the classes and the compiler, not for the work).
        repeat(2) { for (c in cases) TutoPlay.button(c.step, c.p, c.target, c.base, c.level) }
        // Round after round, the best of each state kept: no two calls in a row on the same state (one remembered
        // would be read back, not worked out).
        val ms = DoubleArray(cases.size) { Double.MAX_VALUE }
        repeat(3) {
            for ((i, c) in cases.withIndex()) {
                val t = System.nanoTime()
                TutoPlay.button(c.step, c.p, c.target, c.base, c.level)
                ms[i] = minOf(ms[i], (System.nanoTime() - t) / 1e6)
            }
        }
        val sorted = ms.sorted()
        val worst = ms.indices.maxByOrNull { ms[it] }!!
        val median = sorted[sorted.size / 2]
        val said = "the main button over ${cases.size} states: median %.2f ms, 99 %% under %.2f ms, worst %.2f ms (%s)"
            .format(median, sorted[(sorted.size * 0.99).toInt().coerceAtMost(sorted.lastIndex)], ms[worst], cases[worst].name)
        println("TUTO-NEED-COST $said")
        // Generous ceilings (a phone is several times slower than the machine the tests run on, a loaded one slower still).
        assertTrue(said, median < MEDIAN_MS && ms[worst] < WORST_MS)
    }

    private companion object {
        const val MEDIAN_MS = 5.0
        const val WORST_MS = 60.0

        fun inReach(s: TutoStep): Boolean = !(s.manual || s.listening || s.check == "shared")
    }
}
