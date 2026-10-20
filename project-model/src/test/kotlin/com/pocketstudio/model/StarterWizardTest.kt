package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StarterWizardTest {
    @Test
    fun everyStyleIsWellFormed() {
        assertEquals(StarterWizard.STYLES.size, StarterWizard.STYLES.map { it.id }.toSet().size)
        for (s in StarterWizard.STYLES) {
            assertTrue(s.name, s.family in StarterWizard.FAMILIES)
            assertTrue(s.name, s.bpm in Project.MIN_TEMPO..Project.MAX_TEMPO && s.swing in 0.0..Project.MAX_SWING)
            assertTrue(s.name, s.lines.any { it.role == StarterWizard.Role.Kick })
            // One line per pad, every pattern a whole bar of known marks with at least one hit.
            assertEquals(s.name, s.lines.size, s.lines.map { it.role.lane }.toSet().size)
            for (l in s.lines) {
                assertEquals("${s.name} ${l.role}", 16, l.pattern.length)
                assertTrue("${s.name} ${l.role}", l.pattern.all { it in "Xxo." } && StarterWizard.hits(l.pattern).isNotEmpty())
                assertTrue(l.tip.isNotBlank())
            }
        }
        // Every built-in voice exists in the kit.
        assertTrue(StarterWizard.Role.entries.all { r -> r.builtIn.all { it in 0 until DrumPattern.PAD_COUNT } })
    }

    @Test
    fun theProjectHasTheChosenSoundsAndTheGroove() {
        val house = StarterWizard.style("house")!!
        val clap = PadSample("abc123.wav", "Clap house 2")
        val p = StarterWizard.build(house, mapOf(StarterWizard.Role.Kick to null, StarterWizard.Role.Clap to clap))
        assertEquals(124.0, p.tempoBpm, 0.0)
        assertEquals("House", p.name)
        assertEquals(setOf(0, 4, 8, 12), p.drums.lanes[0].hits.keys)
        assertNull(p.drums.lanes[0].sample)  // the kit's own kick
        assertEquals(clap, p.drums.lanes[4].sample)
        assertEquals(setOf(4, 12), p.drums.lanes[4].hits.keys)
        // Lines that were skipped stay empty.
        assertTrue(p.drums.lanes[6].hits.isEmpty() && p.drums.lanes[5].hits.isEmpty())
        // Strong, medium and soft marks.
        assertEquals(listOf(1f, 0.6f, 0.35f), listOf('X', 'x', 'o').map { StarterWizard.hits("$it")[0]!!.velocity })
    }
}
