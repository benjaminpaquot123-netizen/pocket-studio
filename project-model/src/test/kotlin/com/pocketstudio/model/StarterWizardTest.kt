package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private typealias R = StarterWizard.Role
private typealias P = StarterWizard.Part

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
        // The Refrain (scene C) plays every line.
        val p = StarterWizard.build(house, mapOf(StarterWizard.Role.Kick to null, StarterWizard.Role.Clap to clap)).switchScene(2)
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
        // Without chords nor lead, the acid line plays in the Couplet and the Refrain (B and C).
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
        val p = StarterWizard.build(garage, mapOf(StarterWizard.Role.Kick to null, StarterWizard.Role.Snare to null)).switchScene(2)
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
    fun aPlanWithNothingUsedIsEmptyButDoesNotThrow() {
        // Every drum line off and no part: the assistant still draws its plan (it used to throw).
        val plan = StarterWizard.scenePlan(emptySet(), emptySet(), acid = false)
        assertTrue(plan.isEmpty)
        assertEquals(emptyList<String>(), plan.contents(1))
        assertEquals("Couplet", plan.title(1))
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

    /** Whether each scene plays drum line [role]. */
    private fun Project.drumsIn(role: StarterWizard.Role) = allScenes.map { it.drums.lanes[role.lane].hits.isNotEmpty() }

    /** Whether each scene plays track [id]'s notes. */
    private fun Project.notesIn(id: Int) = allScenes.map { (if (id == 0) it.synthNotes else it.trackNotes.getValue(id)).notes.isNotEmpty() }

    @Test
    fun theSongIsAnIntroACoupletAndARefrainThatPileUp() {
        val style = StarterWizard.style("techno")!!
        val lines = style.lines.associate { it.role to null as PadSample? }
        val parts = P.entries.associateWith { StarterWizard.soundsFor(it, style).first() }
        val p = StarterWizard.build(style, lines, StarterWizard.Mood.Dark, parts)
        assertEquals(listOf("A", "B", "C"), p.allScenes.map { it.name })
        assertEquals(listOf("A · Intro", "B · Couplet", "C · Refrain"), (0..2).map { p.sceneTitle(it) })
        assertEquals(0, p.scene)
        // Intro, Couplet, Refrain, Couplet, Refrain, and the Intro again to end.
        assertEquals(listOf(0, 1, 2, 1, 2, 0), p.song.map { it.scene })
        assertEquals(p.song.first().repeats, p.song.last().repeats)
        // Intro: the kick and the hats, the chords; Couplet: all the drums, the bass and the chords; Refrain: the lead too.
        for (line in style.lines) {
            if (StarterWizard.hits(line.pattern).isEmpty()) continue
            val expected = when (line.role) {
                R.Kick, R.Hat -> listOf(true, true, true)
                R.Bass808 -> listOf(false, true, true)
                else -> listOf(false, true, true)
            }
            assertEquals(line.role.title, expected, p.drumsIn(line.role))
        }
        val (bass, pad, lead) = p.instrumentTracks
        assertEquals(listOf("Basse", "Nappe", "Lead"), listOf(bass.name, pad.name, lead.name))
        assertEquals(listOf(false, true, true), p.notesIn(bass.id))
        assertEquals(listOf(true, true, true), p.notesIn(pad.id))
        assertEquals(listOf(false, false, true), p.notesIn(lead.id))
        // Each section has more going on than the one before: the Refrain holds everything.
        val (a, b, c) = p.allScenes
        assertFalse(a.isEmpty || b.isEmpty || c.isEmpty)
        // The sounds are the same in every scene (only the notes are shared out).
        assertEquals(p.drums.lanes.map { it.sample }, p.switchScene(2).drums.lanes.map { it.sample })
        assertEquals(p, p.sanitized())
    }

    @Test
    fun withoutALeadTheChordsMakeTheRefrain() {
        val plan = StarterWizard.scenePlan(setOf(R.Kick, R.Snare, R.Hat), setOf(P.Bass, P.Pad), acid = true)
        assertEquals(mapOf(R.Kick to setOf(0, 1, 2), R.Snare to setOf(1, 2), R.Hat to setOf(0, 1, 2)), plan.lines)
        assertEquals(mapOf(P.Bass to setOf(1, 2), P.Pad to setOf(0, 2)), plan.parts)
        assertEquals(setOf(1, 2), plan.acid)
        assertEquals(listOf("Kick (grosse caisse)", "Charley fermé (hi-hat)", "Nappe / accords"), plan.contents(0))
        assertEquals(listOf("Kick (grosse caisse)", "Caisse claire", "Charley fermé (hi-hat)", "Basse", "Ligne acide"), plan.contents(1))
        assertEquals("Couplet et Refrain", StarterWizard.ScenePlan.sections(plan.parts.getValue(P.Bass)))
        assertEquals("Intro, Couplet et Refrain", StarterWizard.ScenePlan.sections(setOf(2, 0, 1)))
        assertEquals("Refrain", StarterWizard.ScenePlan.sections(setOf(2)))
    }

    @Test
    fun aBeatAloneKeepsItsOpenHatAndPercussionForTheRefrain() {
        val style = StarterWizard.style("house")!!
        val lines = style.lines.associate { it.role to null as PadSample? }
        val p = StarterWizard.build(style, lines)
        for (line in style.lines) {
            val expected = when (line.role) {
                R.Kick, R.Hat -> listOf(true, true, true)
                R.OpenHat, R.Perc -> listOf(false, false, true)
                else -> listOf(false, true, true)
            }
            assertEquals(line.role.title, expected, p.drumsIn(line.role))
        }
        assertEquals(listOf("Intro", "Couplet", "Refrain"), (0..2).map { p.sceneLabel(it) })
    }

    @Test
    fun aSilentSectionPlaysTheNextOne() {
        // Only an 808: no light drums for the intro, so it plays the couplet.
        val bassOnly = StarterWizard.scenePlan(setOf(R.Bass808), emptySet(), acid = false)
        assertEquals(setOf(0, 1, 2), bassOnly.lines[R.Bass808])
        // Only an acid line: it plays everywhere.
        assertEquals(setOf(0, 1, 2), StarterWizard.scenePlan(emptySet(), emptySet(), acid = true).acid)
        // Only a kick: the three sections play it, none is silent.
        val style = StarterWizard.style("techno")!!
        val p = StarterWizard.build(style, mapOf(R.Kick to null))
        assertEquals(listOf(true, true, true), p.drumsIn(R.Kick))
        assertTrue(p.allScenes.none { it.isEmpty })
    }

    @Test
    fun theSongLastsBetweenOneAndAHalfAndThreeMinutes() {
        for (style in StarterWizard.STYLES) for (melody in listOf(false, true)) {
            val lines = style.lines.associate { it.role to null as PadSample? }
            val parts = if (melody) P.entries.associateWith { StarterWizard.soundsFor(it, style).first() } else emptyMap()
            val p = StarterWizard.build(style, lines, StarterWizard.Mood.Dark.takeIf { melody }, parts)
            val seconds = p.songBars * p.beatsPerBar * 60.0 / p.tempoBpm
            assertTrue("${style.name} $melody: $seconds s", seconds in 90.0..180.0)
            // What the assistant's summary announces is what it builds.
            val plan = StarterWizard.scenePlan(lines.keys, parts.keys, acid = melody && style.melody.acid != null)
            assertEquals(style.name, p.song, StarterWizard.songOf(style, plan))
            assertEquals(style.name, p.songBars * 16, p.song.sumOf { it.repeats } * StarterWizard.loopBars(style, plan) * 16)
        }
        // Phrases of 8 bars when they fit, of 4 for a slow style.
        assertEquals(listOf(2, 4, 4, 4, 4, 2), StarterWizard.songOf(4, 128.0).map { it.repeats })
        assertEquals(listOf(1, 2, 2, 2, 2, 1), StarterWizard.songOf(4, 85.0).map { it.repeats })
        assertEquals(listOf(8, 16, 16, 16, 16, 8), StarterWizard.songOf(1, 128.0).map { it.repeats })
    }

    @Test
    fun everyChoiceFollowsThePlanAndSurvivesSavingAndSwitching() {
        for (style in StarterWizard.STYLES) for (mood in listOf(null) + StarterWizard.Mood.entries.take(2)) {
            val lines = style.lines.associate { it.role to null as PadSample? }
            val parts = if (mood == null) emptyMap() else P.entries.associateWith { StarterWizard.soundsFor(it, style).first() }
            val p = StarterWizard.build(style, lines, mood, parts)
            val plan = StarterWizard.scenePlan(lines.keys, parts.keys, acid = mood != null && style.melody.acid != null)
            assertEquals(3, p.allScenes.size)
            for (line in style.lines) {
                if (StarterWizard.hits(line.pattern).isEmpty()) continue
                val expected = List(3) { it in plan.lines.getValue(line.role) }
                assertEquals("${style.name} $mood ${line.role}", expected, p.drumsIn(line.role))
                // Cumulative: a line in a section is in every section after it.
                assertTrue("${style.name} $mood ${line.role}", expected.dropWhile { !it }.all { it })
            }
            if (parts.isNotEmpty()) P.entries.forEachIndexed { i, part ->
                val id = p.instrumentTracks[i].id
                assertEquals("${style.name} $mood $part", List(3) { it in plan.parts.getValue(part) }, p.notesIn(id))
                assertTrue(p.notesIn(id)[2])  // the Refrain plays everything
            }
            // The intro never has the bass.
            if (parts.isNotEmpty()) assertFalse(p.notesIn(p.instrumentTracks[0].id)[0])
            assertTrue("${style.name} $mood", p.allScenes.none { it.isEmpty })
            assertEquals(p, ProjectJson.decode(ProjectJson.encode(p)))
            assertEquals(p, p.switchScene(2).switchScene(0))
        }
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

/** Track [id]'s notes in the first scene that plays them. */
private fun Project.notesOf(id: Int): NoteClip =
    allScenes.map { if (id == 0) it.synthNotes else it.trackNotes[id] ?: NoteClip() }.firstOrNull { it.notes.isNotEmpty() } ?: track(id).notes
