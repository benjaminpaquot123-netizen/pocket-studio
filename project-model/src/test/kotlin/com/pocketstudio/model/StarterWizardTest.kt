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
                assertTrue("${s.name} ${l.role}", l.pattern.length == 16 || l.pattern.length == 32)
                assertTrue("${s.name} ${l.role}", l.pattern.all { it in "Xxo." } && StarterWizard.hits(l.pattern).isNotEmpty())
                assertTrue(l.tip.isNotBlank())
            }
            // Its sounds exist (a typo would silently fall back to another one).
            val names = SoundLibrary.SYNTH.map { it.name } + SoundLibrary.PLAITS.map { it.name }
            for (part in StarterWizard.Part.entries) assertTrue("${s.name} $part", s.melody.sound(part) in names)
            for ((_, progression) in s.melody.progressions) assertEquals(s.name, 4, progression.second.size)
            for (line in listOf(s.melody.bassPattern, s.melody.leadPattern, s.melody.chordRhythm).filter { it.isNotEmpty() }) {
                assertTrue("${s.name} $line", line.length == 16 || line.length == 32)
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
        assertEquals(house.bpm, p.tempoBpm, 0.0)
        assertEquals(house.swing, p.swing, 0.0)
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
    fun aOneBarLineRepeatsUnderATwoBarOne() {
        // UK garage: the kick spans two bars, the snare one.
        val garage = StarterWizard.style("uk_garage")!!
        assertEquals(32, StarterWizard.drumSteps(garage))
        val p = StarterWizard.build(garage, mapOf(StarterWizard.Role.Kick to null, StarterWizard.Role.Snare to null))
        assertEquals(32, p.drums.steps)
        assertEquals(setOf(4, 12, 20, 28), p.drums.lanes[StarterWizard.Role.Snare.lane].hits.keys)
        assertEquals(StarterWizard.hits(garage.lines.first { it.role == StarterWizard.Role.Kick }.pattern).keys, p.drums.lanes[0].hits.keys)
    }

    @Test
    fun melodicPartsAreInTheKeyAndLocked() {
        for (style in StarterWizard.STYLES) {
            // The proposed sounds exist in the library, and the bass rhythm is a bar.
            StarterWizard.Part.entries.forEach { part ->
                assertEquals("${style.name} $part", style.melody.sound(part), StarterWizard.soundsFor(part, style).first().name)
            }
            assertTrue(style.melody.bassPattern.length in listOf(16, 32))
            assertTrue(StepLine.parse(style.melody.bassPattern).any { StepLine.stepOf(it.token) != null })
            for (mood in StarterWizard.Mood.entries) {
                val parts = StarterWizard.Part.entries.associateWith { StarterWizard.soundsFor(it, style).first() }
                val p = StarterWizard.build(style, mapOf(StarterWizard.Role.Kick to null), mood, parts)
                assertTrue(p.harmony.lock)
                assertEquals(StarterWizard.progressionOf(style, mood).first, p.harmony.scale)
                assertEquals(3, p.instrumentTracks.size)
                assertEquals(listOf("Basse", "Nappe", "Lead"), p.instrumentTracks.map { it.name })
                for (t in p.instrumentTracks) {
                    assertTrue("${style.name} $mood ${t.name}", t.notes.notes.isNotEmpty())
                    assertTrue("${style.name} $mood ${t.name}", t.notes.notes.all { p.harmony.inScale(it.pitch) })
                    assertEquals(64, t.notes.steps)
                }
                // The bass plays each chord's root, bar after bar.
                val bass = p.instrumentTracks[0].notes.notes
                StarterWizard.progressionOf(style, mood).second.forEachIndexed { bar, d ->
                    val inBar = bass.filter { it.start / (16 * NoteClip.TICKS_PER_STEP) == bar }
                    val root = Math.floorMod(p.harmony.degreeRoot(d, 36), 12)
                    assertTrue("${style.name} $mood bar $bar", inBar.any { Math.floorMod(it.pitch, 12) == root })
                }
                if (style.melody.leadPattern.isBlank()) assertEquals(ArpMode.UpDown, p.instrumentTracks[2].notes.performance.arp)
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

    @Test
    fun packSoundsCanPlayTheBassAndTheLead() {
        val trap = StarterWizard.style("trap")!!
        val pack = PackManifest(
            "trap", "Trap", sounds = listOf(
                PackSound("808_01.flac", "808", "808 1"), PackSound("bass_01.flac", "bass", "Basse 1"),
                PackSound("kick_01.flac", "kick", "Kick 1"), PackSound("stab_01.flac", "stab", "Stab 1"),
            ),
        )
        val bass = StarterWizard.soundsFor(StarterWizard.Part.Bass, trap, listOf(pack))
        val first808 = bass.first { it.sampled != null }
        assertEquals("trap" to "808_01.flac", first808.packFile)
        assertTrue(first808.sampled!!.glide > 0f)  // 808s slide
        assertTrue(bass.none { it.packFile?.second == "kick_01.flac" })
        assertEquals(listOf("stab_01.flac"), StarterWizard.soundsFor(StarterWizard.Part.Lead, trap, listOf(pack)).mapNotNull { it.packFile?.second })
        assertTrue(StarterWizard.soundsFor(StarterWizard.Part.Pad, trap, listOf(pack)).none { it.sampled != null })
        // Once copied into the project, the bass track plays the sound across the keys.
        val placed = first808.copy(sampled = first808.sampled!!.copy(file = "abc.wav", root = 24.3f))
        val p = StarterWizard.build(trap, emptyMap(), StarterWizard.Mood.Dark, mapOf(StarterWizard.Part.Bass to placed))
        val t = p.track(0)
        assertEquals(2, t.engine)
        assertEquals("abc.wav", t.sampled.file)
        assertEquals(24.3f, t.sampled.root)
        assertTrue("abc.wav" in p.sampleFiles)
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
