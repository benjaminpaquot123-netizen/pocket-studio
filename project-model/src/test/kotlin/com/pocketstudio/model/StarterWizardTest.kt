package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun anAcidStyleStartsWithItsThreeOThreeLine() {
        val style = StarterWizard.style("acid_techno")!!
        val recipe = style.melody.acid!!
        // Without chords nor lead, the acid line goes to scene C (the kick stays in A).
        val p = StarterWizard.build(style, mapOf(StarterWizard.Role.Kick to null), StarterWizard.Mood.Dark).switchScene(2)
        assertEquals(16, p.acid.length)
        assertEquals(36 + style.melody.root, p.acid.root)
        recipe.pattern.forEachIndexed { i, c -> assertEquals("step $i", c != '.' && c != '-', p.acid.steps[i].gate) }
        recipe.accents.forEachIndexed { i, c -> assertEquals("accent $i", c == 'x', p.acid.steps[i].accent) }
        // Its notes are the key's: 'R' the root, 'O' the octave above.
        assertEquals(listOf(0, 0), listOf(p.acid.steps[0].note, p.acid.steps[0].octave))
        assertEquals(listOf(0, 1), listOf(p.acid.steps[2].note, p.acid.steps[2].octave))
        // Without a mood (the beat only), the acid track is left as it was.
        assertEquals(Project().acid, StarterWizard.build(style, mapOf(StarterWizard.Role.Kick to null)).acid)
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
                    val notes = p.notesOf(t.id)
                    assertTrue("${style.name} $mood ${t.name}", notes.notes.isNotEmpty())
                    assertTrue("${style.name} $mood ${t.name}", notes.notes.all { p.harmony.inScale(it.pitch) })
                    assertEquals(64, notes.steps)
                }
                // The bass plays each chord's root, bar after bar.
                val bass = p.notesOf(0).notes
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
    fun withoutAMelodyThePartStepsAreLeftOut() {
        val house = StarterWizard.style("house")!!
        val steps = StarterWizard.steps(house, melody = false)
        assertEquals(2 + house.lines.size + 1, steps.size)
        assertTrue(steps.none { it is StarterWizard.Step.Melodic })
        // The mood step and the drum lines keep their place, whatever the mood.
        assertEquals(StarterWizard.steps(house).take(2 + house.lines.size), steps.take(2 + house.lines.size))
    }

    @Test
    fun aPlanWithNothingUsedHasNoCopies() {
        // Every drum line off and no part: the assistant still draws its plan (it used to throw).
        val plan = StarterWizard.scenePlan(emptySet(), emptySet(), acid = false)
        assertEquals(emptyMap<Int, Int>(), plan.copies)
        assertEquals(emptyList<String>(), plan.contents(1))
        assertEquals("le groove", plan.title(1))
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

    @Test
    fun theSongIsCutInARhythmABassAndAMelodyScene() {
        val style = StarterWizard.style("techno")!!
        val lines = style.lines.associate { it.role to null as PadSample? }
        val parts = StarterWizard.Part.entries.associateWith { StarterWizard.soundsFor(it, style).first() }
        val p = StarterWizard.build(style, lines, StarterWizard.Mood.Dark, parts)
        assertEquals(listOf("A", "B", "C"), p.allScenes.map { it.name })
        assertEquals(0, p.scene)
        assertEquals(listOf(SongPart(0, 2), SongPart(1, 2), SongPart(2, 2)), p.song)
        val (a, b, c) = p.allScenes
        // A: the drums; B: the bass (and the 808); C: the chords and the lead.
        for (line in style.lines) {
            val inB = line.role == StarterWizard.Role.Bass808
            assertEquals(line.role.title, !inB && StarterWizard.hits(line.pattern).isNotEmpty(), a.drums.lanes[line.role.lane].hits.isNotEmpty())
        }
        assertTrue(c.drums.lanes.all { it.hits.isEmpty() })
        val (bass, pad, lead) = p.instrumentTracks
        assertEquals("Basse", bass.name)
        assertTrue(a.synthNotes.notes.isEmpty() && b.synthNotes.notes.isNotEmpty() && c.synthNotes.notes.isEmpty())
        assertTrue(c.trackNotes.getValue(pad.id).notes.isNotEmpty() && b.trackNotes.getValue(pad.id).notes.isEmpty())
        assertTrue(c.trackNotes.getValue(lead.id).notes.isNotEmpty() && a.trackNotes.getValue(lead.id).notes.isEmpty())
        assertFalse(a.isEmpty || b.isEmpty || c.isEmpty)
        // The sounds are the same in every scene (only the notes are shared out).
        assertEquals(p.drums.lanes.map { it.sample }, p.switchScene(2).drums.lanes.map { it.sample })
        assertEquals(p, p.sanitized())
    }

    @Test
    fun aBeatAloneIsCutInItsBaseItsGrooveAndItsRelief() {
        val style = StarterWizard.style("house")!!
        val lines = style.lines.associate { it.role to null as PadSample? }
        val p = StarterWizard.build(style, lines)
        val scenes = p.allScenes
        val sceneOf = mapOf(
            StarterWizard.Role.Kick to 0, StarterWizard.Role.Snare to 0, StarterWizard.Role.Clap to 0,
            StarterWizard.Role.Hat to 1, StarterWizard.Role.OpenHat to 1, StarterWizard.Role.Perc to 2, StarterWizard.Role.Bass808 to 2,
        )
        for (line in style.lines) {
            val plays = scenes.map { it.drums.lanes[line.role.lane].hits.isNotEmpty() }
            assertEquals(line.role.title, List(3) { it == sceneOf.getValue(line.role) }, plays)
        }
        val plan = StarterWizard.scenePlan(lines.keys, emptySet(), acid = false)
        assertEquals(listOf("la base", "le groove", "le relief"), List(3) { plan.title(it) })
    }

    @Test
    fun everyChoicePlaysInOneSceneAndSurvivesSavingAndSwitching() {
        for (style in StarterWizard.STYLES) for (mood in listOf(null) + StarterWizard.Mood.entries.take(2)) {
            val lines = style.lines.associate { it.role to null as PadSample? }
            val parts = if (mood == null) emptyMap() else StarterWizard.Part.entries.associateWith { StarterWizard.soundsFor(it, style).first() }
            val p = StarterWizard.build(style, lines, mood, parts)
            val plan = StarterWizard.scenePlan(lines.keys, parts.keys, acid = mood != null && style.melody.acid != null)
            val scenes = p.allScenes
            assertEquals(3, scenes.size)
            for (line in style.lines) {
                if (StarterWizard.hits(line.pattern).isEmpty()) continue
                // In the scene the plan says, and in the scenes that copy it.
                val expected = List(3) { plan.source(it) == plan.lines[line.role] }
                assertEquals("${style.name} $mood ${line.role}", expected, scenes.map { it.drums.lanes[line.role.lane].hits.isNotEmpty() })
            }
            // Without a mood no part is made (the first track stays empty).
            if (parts.isNotEmpty()) p.instrumentTracks.forEach { t ->
                val plays = scenes.map { (if (t.id == 0) it.synthNotes else it.trackNotes.getValue(t.id)).notes.isNotEmpty() }
                assertTrue("${style.name} $mood ${t.name}", plays.count { it } >= 1)
            }
            assertTrue("${style.name} $mood", scenes.none { it.isEmpty })
            assertEquals(p, ProjectJson.decode(ProjectJson.encode(p)))
            assertEquals(p, p.switchScene(2).switchScene(0))
        }
    }

    @Test
    fun anEmptySceneStartsAsACopy() {
        val style = StarterWizard.style("techno")!!
        val p = StarterWizard.build(style, mapOf(StarterWizard.Role.Kick to null))
        val (a, b, c) = p.allScenes
        assertEquals(a.drums, b.drums)
        assertEquals(a.drums, c.drums)
        assertTrue(a.drums.lanes[0].hits.isNotEmpty())
        val plan = StarterWizard.scenePlan(setOf(StarterWizard.Role.Kick), emptySet(), acid = false)
        assertEquals(mapOf(1 to 0, 2 to 0), plan.copies)
        assertEquals(listOf("Kick (grosse caisse)"), plan.contents(2))
        assertEquals("une copie de la scène A, à varier", plan.title(1))
        // With chords, the bass goes to B and the chords to C.
        val melodic = StarterWizard.scenePlan(setOf(StarterWizard.Role.Kick), setOf(StarterWizard.Part.Bass, StarterWizard.Part.Pad), acid = true)
        assertEquals(mapOf(StarterWizard.Part.Bass to 1, StarterWizard.Part.Pad to 2), melodic.parts)
        assertEquals(1, melodic.acid)
        assertEquals(listOf("le rythme", "la basse", "la mélodie"), List(3) { melodic.title(it) })
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

/** Track [id]'s notes in the scene that plays them (each part lives in one scene). */
private fun Project.notesOf(id: Int): NoteClip =
    allScenes.map { if (id == 0) it.synthNotes else it.trackNotes[id] ?: NoteClip() }.firstOrNull { it.notes.isNotEmpty() } ?: track(id).notes
