package com.pocketstudio.model.tuto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 7, D4: the Facile consignes were cut in pieces of two gestures (Facile-only steps after the first, same check);
 * the first piece got a length that stops at its own bars. Pro does not see the pieces: its step must still ask for
 * the whole line (and the clap's « Doux »), as before the cut.
 */
class Tour7TutoProKeepsItsStepsTest {
    private fun step(style: String, level: TutoLevel, id: String) =
        TutoCourse.of(style)!!.chaptersFor(level).flatMap { it.steps }.single { it.id == id }

    @Test
    fun proBassStepsReachTheWholeLine() {
        // Before the cut: house bass_do had no length (the whole line), drill's had 32 cells.
        assertEquals(0, step("house", TutoLevel.Pro, "bass_do").length)
        assertEquals(32, step("drill", TutoLevel.Pro, "bass_do").length)
        assertEquals(0, step("jump_up", TutoLevel.Pro, "make_drop2").length)
        // Facile keeps its first piece short; the next pieces carry the rest.
        assertEquals(16, step("house", TutoLevel.Facile, "bass_do").length)
        assertEquals(16, step("drill", TutoLevel.Facile, "bass_do").length)
    }

    @Test
    fun everyProStepIsAsBeforeTheCut() {
        // No Pro step keeps a length its Facile pieces would complete.
        TutoCourse.STYLES.forEach { style ->
            val course = TutoCourse.of(style)!!
            val pro = course.chaptersFor(TutoLevel.Pro).flatMap { it.steps }.associateBy { it.id }
            course.chapters.forEach { c ->
                c.steps.zipWithNext().forEach { (a, b) ->
                    if ("p" !in b.levels && "p" in a.levels && a.check == b.check && !a.manual && a.length > 0) {
                        assertTrue("$style ${a.id}: Pro reaches past its first Facile piece", pro.getValue(a.id).length == 0 || pro.getValue(a.id).length > a.length)
                    }
                }
            }
        }
    }

    @Test
    fun proClapStillAsksForSoft() {
        assertTrue(TutoChecks.asksSoft(step("techno", TutoLevel.Pro, "clap_do")))
        assertFalse(TutoChecks.asksSoft(step("techno", TutoLevel.Facile, "clap_do")))
        assertTrue(TutoChecks.asksSoft(step("techno", TutoLevel.Facile, "clap_2")))
    }
}
