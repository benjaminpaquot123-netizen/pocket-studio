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

    @Test
    fun melodicPartsAreInTheKeyAndLocked() {
        for (style in StarterWizard.STYLES) {
            // The proposed sounds exist in the library, and the bass rhythm is a bar.
            StarterWizard.Part.entries.forEach { part ->
                assertEquals("${style.name} $part", style.melody.sound(part), StarterWizard.soundsFor(part, style).first().name)
            }
            assertEquals(16, style.melody.bassPattern.length)
            assertTrue(StarterWizard.hits(style.melody.bassPattern).isNotEmpty())
            for (mood in StarterWizard.Mood.entries) {
                val parts = StarterWizard.Part.entries.associateWith { StarterWizard.soundsFor(it, style).first() }
                val p = StarterWizard.build(style, mapOf(StarterWizard.Role.Kick to null), mood, parts)
                assertTrue(p.harmony.lock)
                assertEquals(mood.scale, p.harmony.scale)
                assertEquals(3, p.instrumentTracks.size)
                assertEquals(listOf("Basse", "Nappe", "Lead"), p.instrumentTracks.map { it.name })
                for (t in p.instrumentTracks) {
                    assertTrue("${style.name} $mood ${t.name}", t.notes.notes.isNotEmpty())
                    assertTrue("${style.name} $mood ${t.name}", t.notes.notes.all { p.harmony.inScale(it.pitch) })
                    assertEquals(64, t.notes.steps)
                }
                // The bass plays each chord's root, bar after bar.
                val bass = p.instrumentTracks[0].notes.notes
                mood.degrees.forEachIndexed { bar, d ->
                    val inBar = bass.filter { it.start / (16 * NoteClip.TICKS_PER_STEP) == bar }
                    assertTrue(inBar.isNotEmpty() && inBar.all { it.pitch == p.harmony.degreeRoot(d, 36) })
                }
                assertEquals(ArpMode.UpDown, p.instrumentTracks[2].notes.performance.arp)
                // Plaits sounds land on a Plaits track.
                StarterWizard.Part.entries.forEachIndexed { i, part ->
                    assertEquals(if (parts.getValue(part).plaits != null) 1 else 0, p.instrumentTracks[i].engine)
                }
            }
        }
        // Without a mood: only the beat, the key left free.
        val beat = StarterWizard.build(StarterWizard.STYLES[0], mapOf(StarterWizard.Role.Kick to null))
        assertEquals(Scale.Off, beat.harmony.scale)
        assertEquals(1, beat.instrumentTracks.size)
        assertTrue(beat.synthNotes.notes.isEmpty())
        // Steps: style, mood, the drum lines, the three parts, the summary.
        val house = StarterWizard.style("house")!!
        assertEquals(2 + house.lines.size + 3 + 1, StarterWizard.steps(house).size)
    }
}

class ChordPadsTest {
    @Test
    fun padsAreTheScalesChords() {
        val aMinor = Harmony(9, Scale.Minor).chordPads(48)
        assertEquals(listOf("i", "ii°", "III", "iv", "v", "VI", "VII", "i"), aMinor.map { it.roman })
        assertEquals("La m", aMinor[0].name)
        assertEquals(listOf(57, 60, 64), aMinor[0].notes)
        assertEquals(aMinor[0].notes.map { it + 12 }, aMinor[7].notes)
        val cMajor = Harmony(0, Scale.Major).chordPads(48)
        assertEquals(listOf("Do", "Ré m", "Mi m", "Fa", "Sol", "La m", "Si dim", "Do"), cMajor.map { it.name })
        // No scale, or a pentatonic one: the major scale of the root, every note in it.
        val free = Harmony(2, Scale.Off).chordPads(48)
        assertTrue(free.all { pad -> pad.notes.all { Harmony(2, Scale.Major).inScale(it) } })
        assertTrue(Harmony(4, Scale.MinorPentatonic).chordPads(60).all { it.notes.size == 3 })
    }
}
