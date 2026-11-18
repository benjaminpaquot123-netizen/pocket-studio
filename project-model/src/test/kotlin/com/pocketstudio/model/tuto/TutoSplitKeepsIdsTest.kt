package com.pocketstudio.model.tuto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Tour 7, P7 (docs/audits/ultra/tour-7-plan-p6-p7.md § 3.4): a Facile consigne cut in pieces never moves the Pro
 * course. Pro's step ids, course by course in order, are the frozen fixture `tuto/pro-step-ids.txt` (blocs 1 to 4 cut
 * steps with `levels: "f"` pieces, so the list stays the same); a piece's first part keeps the old id, so an old
 * `done` still ticks the same step.
 */
class TutoSplitKeepsIdsTest {
    private fun proIds(style: String) = TutoCourse.of(style)!!.chaptersFor(TutoLevel.Pro).flatMap { c -> c.steps.map { it.id } }

    private fun fixture(): Map<String, List<String>> =
        javaClass.classLoader!!.getResourceAsStream(FIXTURE)!!.bufferedReader().readLines()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .associate { line -> line.substringBefore(':').trim() to line.substringAfter(':').trim().split(' ').filter { it.isNotEmpty() } }

    @Test fun proStepIdsAreTheFrozenOnes() {
        val now = TutoCourse.STYLES.associateWith(::proIds)
        File("build/tuto-pro-step-ids.txt").writeText(now.entries.joinToString("\n", postfix = "\n") { (s, ids) -> "$s: ${ids.joinToString(" ")}" })
        val want = fixture()
        assertEquals("the courses", want.keys.sorted(), now.keys.sorted())
        val bad = now.filter { (s, ids) -> ids != want[s] }.map { (s, ids) -> "$s: now ${ids - want[s].orEmpty().toSet()} new, ${want[s].orEmpty() - ids.toSet()} gone, or reordered" }
        assertTrue("${bad.size} Pro courses changed their steps (build/tuto-pro-step-ids.txt has today's):\n" + bad.joinToString("\n"), bad.isEmpty())
    }

    @Test fun anOldDoneStillTicksItsStep() {
        // Every Facile step that is not a Pro step is a piece: it follows a step of the same check that Pro keeps.
        val bad = ArrayList<String>()
        for (style in TutoCourse.STYLES) {
            val pro = proIds(style).toSet()
            for (c in TutoCourse.of(style)!!.chaptersFor(TutoLevel.Facile)) for ((i, s) in c.steps.withIndex()) {
                // A listening or manual step ticks nothing (no done to keep): only a cut piece of a gesture step is checked.
                if (s.id in pro || TutoLevel.Pro.code in s.levels || s.listening || s.manual) continue
                val before = c.steps.getOrNull(i - 1)
                if ("$style/${s.id}" in OLD_FACILE_ONLY) continue
                if (before == null || before.check != s.check) bad += "$style/${s.id}: a Facile-only step that continues no step"
            }
            for (id in fixture()[style].orEmpty()) assertTrue("$style/$id: an old id is gone, an old done ticks nothing", id in pro)
        }
        assertTrue(bad.joinToString("\n"), bad.isEmpty())
    }

    companion object {
        const val FIXTURE = "tuto/pro-step-ids.txt"

        /** The Facile-only gesture steps written before tour 7 (Pro asks them inside another step): not cut pieces. */
        val OLD_FACILE_ONLY = setOf(
            "acid_techno/acid_show", "acidcore/acid_show", "trap/hat_do", "trap/hat_triplet",
            "drill/hat_do", "drill/hat_triplet", "phonk/hat_do",
        )
    }
}
