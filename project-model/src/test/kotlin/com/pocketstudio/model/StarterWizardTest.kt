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
                assertTrue("${s.name} ${l.role}", l.pattern.all { it in "Xxo.234" } && StarterWizard.hits(l.pattern).isNotEmpty())
                assertTrue(l.tip.isNotBlank())
                assertTrue("${s.name} ${l.role}", l.voice == null || l.voice in 0 until DrumPattern.PAD_COUNT)
            }
            // The mood proposed first has the style's own chords; tips and effects point at what exists.
            assertTrue(s.name, s.melody.mood in s.melody.progressions)
            assertTrue(s.name, s.melody.tips.values.all { it.isNotBlank() })
            val targets = StarterWizard.Part.entries.map { it.id } + s.lines.map { it.role.id } + listOf("acid", "drums", "master")
            for (fx in s.effects) {
                assertTrue("${s.name} ${fx.target}", fx.target in targets)
                assertTrue("${s.name} ${fx.preset}", fx.preset.isEmpty() || FxPresets.byId(fx.preset) != null)
                assertTrue("${s.name} ${fx.insert}", fx.insert.isEmpty() || EffectType.entries.any { it.name == fx.insert })
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
        // Four on the floor over two bars, and the soft push at the end of the second ("jack").
        assertEquals(setOf(0, 4, 8, 12, 16, 20, 24, 28, 30), p.drums.lanes[0].hits.keys)
        assertNull(p.drums.lanes[0].sample)  // the kit's own kick
        assertEquals(clap, p.drums.lanes[4].sample)
        assertEquals(setOf(4, 12, 20, 28), p.drums.lanes[4].hits.keys)
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
        // UK garage: the kick spans two bars, the clap one.
        val garage = StarterWizard.style("uk_garage")!!
        assertEquals(32, StarterWizard.drumSteps(garage))
        val p = StarterWizard.build(garage, mapOf(StarterWizard.Role.Kick to null, StarterWizard.Role.Clap to null)).switchScene(2)
        assertEquals(32, p.drums.steps)
        assertEquals(setOf(4, 12, 20, 28), p.drums.lanes[StarterWizard.Role.Clap.lane].hits.keys)
        assertEquals(StarterWizard.hits(garage.lines.first { it.role == StarterWizard.Role.Kick }.pattern).keys, p.drums.lanes[0].hits.keys)
    }

    @Test
    fun melodicPartsAreInTheKeyAndLocked() {
        for (style in StarterWizard.STYLES) {
            // The proposed sounds exist in the library, and the bass rhythm is a bar.
            StarterWizard.Part.entries.forEach { part ->
                val first = StarterWizard.soundsFor(part, style).first()
                if (first.kitVoice != null) assertTrue(StarterWizard.soundsFor(part, style).any { it.name == style.melody.sound(part) })
                else assertEquals("${style.name} $part", style.melody.sound(part), first.name)
            }
            assertTrue(style.melody.bassPattern.length in listOf(16, 32))
            assertTrue(StepLine.parse(style.melody.bassPattern).any { StepLine.stepOf(it.token) != null })
            for (mood in StarterWizard.Mood.entries) {
                val parts = StarterWizard.Part.entries.associateWith { StarterWizard.soundsFor(it, style).first() }
                val p = StarterWizard.build(style, mapOf(StarterWizard.Role.Kick to null), mood, parts)
                assertTrue(p.harmony.lock)
                assertEquals(StarterWizard.progressionOf(style, mood).first, p.harmony.scale)
                // Club music with a riser gets its « Montée » track after the parts.
                val riser = style.riser && style.form != StarterWizard.SongForm.Song
                assertEquals(listOf("Basse", "Nappe", "Lead") + listOfNotNull(StarterWizard.RISER_TRACK.takeIf { riser }), p.instrumentTracks.map { it.name })
                for (t in p.instrumentTracks.take(3)) {
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
                // Plaits sounds land on a Plaits track, kit voices played across the keys (phonk's cowbell) on a sampler.
                StarterWizard.Part.entries.forEachIndexed { i, part ->
                    val sound = parts.getValue(part)
                    assertEquals(if (sound.sampled != null) 2 else if (sound.plaits != null) 1 else 0, p.instrumentTracks[i].engine)
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
        val style = StarterWizard.style("pop")!!
        assertEquals(StarterWizard.SongForm.Song, style.form)
        val lines = style.lines.associate { it.role to null as PadSample? }
        val parts = P.entries.associateWith { StarterWizard.soundsFor(it, style).first() }
        val p = StarterWizard.build(style, lines, StarterWizard.Mood.Dark, parts)
        assertEquals(listOf("A", "B", "C"), p.allScenes.map { it.name })
        assertEquals(listOf("A · Intro", "B · Couplet", "C · Refrain"), (0..2).map { p.sceneTitle(it) })
        assertEquals(0, p.scene)
        // Intro, Couplet, Refrain, Couplet, Refrain, and the Intro again to end.
        assertEquals(listOf(0, 1, 2, 1, 2, 0), p.song.map { it.scene })
        assertEquals(p.song.first().repeats, p.song.last().repeats)
        // Intro: the kick and the hats, the chords; Couplet: the drums but the open hat and the
        // percussion, the bass and the chords; Refrain: everything, the lead too.
        for (line in style.lines) {
            val expected = when (line.role) {
                R.Kick, R.Hat -> listOf(true, true, true)
                R.OpenHat, R.Perc -> listOf(false, false, true)
                else -> listOf(false, true, true)
            }
            assertEquals(line.role.title, expected, p.drumsIn(line.role))
        }
        val (bass, pad, lead) = p.instrumentTracks
        assertEquals(listOf("Basse", "Nappe", "Lead"), listOf(bass.name, pad.name, lead.name))
        assertEquals(listOf(false, true, true), p.notesIn(bass.id))
        assertEquals(listOf(true, true, true), p.notesIn(pad.id))
        assertEquals(listOf(false, false, true), p.notesIn(lead.id))
        val (a, b, c) = p.allScenes
        assertFalse(a.isEmpty || b.isEmpty || c.isEmpty)
        // The sounds are the same in every scene (only the notes are shared out).
        assertEquals(p.drums.lanes.map { it.sample }, p.switchScene(2).drums.lanes.map { it.sample })
        assertEquals(p, p.sanitized())
    }

    @Test
    fun clubMusicHasAGrooveADropAndABreakWithoutKickNorBass() {
        val style = StarterWizard.style("techno")!!
        assertEquals(StarterWizard.SongForm.Club, style.form)
        val lines = style.lines.associate { it.role to null as PadSample? }
        val parts = P.entries.associateWith { StarterWizard.soundsFor(it, style).first() }
        val p = StarterWizard.build(style, lines, StarterWizard.Mood.Dark, parts)
        assertEquals(listOf("A · Intro", "B · Groove", "C · Drop", "D · Break"), (0..3).map { p.sceneTitle(it) })
        // Intro, Groove, Drop, Break, Drop, the Intro again: the Break 8 bars, the others 16 (scenes of 4 bars).
        assertEquals(listOf(0, 1, 2, 3, 2, 0), p.song.map { it.scene })
        assertEquals(listOf(4, 4, 4, 2, 4, 4), p.song.map { it.repeats })
        // The DJ's intro is the beat (kick, hats, percussion), no note; the 909 open hat waits for the Drop;
        // the Break keeps the hats and the percussion under the chords, without kick nor bass.
        val (bass, pad, lead) = p.instrumentTracks
        for (line in style.lines) {
            val expected = when (line.role) {
                // The rumble is the kick's tail: it plays wherever the kick plays.
                R.Kick, R.Rumble -> listOf(true, true, true, false)
                R.Hat, R.Perc -> listOf(true, true, true, true)
                // The open hat and the ride wait for the Drop.
                R.OpenHat, R.Ride -> listOf(false, false, true, false)
                else -> listOf(false, true, true, false)
            }
            assertEquals(line.role.title, expected, p.drumsIn(line.role))
        }
        assertEquals(listOf(false, true, true, false), p.notesIn(bass.id))
        assertEquals(listOf(false, true, true, true), p.notesIn(pad.id))
        assertEquals(listOf(false, false, true, true), p.notesIn(lead.id))
        // A fast style gets 32-bar drops (Intro 16, Groove 16, Drop 32, Break 8, Drop 32, Intro 16).
        val dnb = StarterWizard.sectionsOf(174.0, StarterWizard.SongForm.Club, hasBreak = true)
        assertEquals(listOf(16, 16, 32, 8, 32, 16), dnb.map { it.first })
        // Nothing to play without the kick (a kick alone): no Break, three sections.
        val kickOnly = StarterWizard.scenePlan(setOf(R.Kick), emptySet(), acid = false, form = StarterWizard.SongForm.Club)
        assertFalse(kickOnly.hasBreak)
        assertEquals(3, kickOnly.count)
        // The 303 of an acid style carries the Break when there is no chord nor lead.
        val acid = StarterWizard.scenePlan(setOf(R.Kick, R.Hat), emptySet(), acid = true, form = StarterWizard.SongForm.Club)
        assertEquals(setOf(1, 2, 3), acid.acid)
        assertEquals(listOf("Intro", "Groove", "Drop", "Break"), acid.labels)
    }

    @Test
    fun theSignaturePlaysInEverySection() {
        // The boom bap's piano loop, the phonk's cowbell melody: they never leave the song.
        val pad = StarterWizard.scenePlan(setOf(R.Kick, R.Snare, R.Hat), setOf(P.Bass, P.Pad), acid = false, signature = P.Pad)
        assertEquals(setOf(0, 1, 2), pad.parts[P.Pad])
        assertEquals(setOf(1, 2), pad.parts[P.Bass])
        // A song's intro with a signature: the signature and the hats, the kick waits for the couplet.
        assertEquals(setOf(0, 1, 2), pad.lines[R.Hat])
        assertEquals(setOf(1, 2), pad.lines[R.Kick])
        val lead = StarterWizard.scenePlan(setOf(R.Kick, R.Hat), setOf(P.Bass, P.Pad, P.Lead), acid = false, signature = P.Lead)
        assertEquals(setOf(0, 1, 2), lead.parts[P.Lead])
        assertEquals(setOf(2), lead.parts[P.Pad])  // the chords make the Refrain instead
        // In club music too (the Break keeps it).
        val club = StarterWizard.scenePlan(setOf(R.Kick, R.Hat), setOf(P.Bass, P.Pad), acid = false, signature = P.Pad, form = StarterWizard.SongForm.Club)
        assertEquals(setOf(0, 1, 2, 3), club.parts[P.Pad])
        // Built: a style whose chords are its signature has them in every scene.
        val boomBap = StarterWizard.style("boom_bap")!!.let { it.copy(melody = it.melody.copy(signature = P.Pad)) }
        val parts = listOf(P.Bass, P.Pad).associateWith { StarterWizard.soundsFor(it, boomBap).first() }
        val p = StarterWizard.build(boomBap, boomBap.lines.associate { it.role to null }, StarterWizard.Mood.Dark, parts)
        assertEquals(listOf(true, true, true), p.notesIn(p.instrumentTracks[1].id))
    }

    @Test
    fun withoutALeadTheChordsMakeTheRefrain() {
        val plan = StarterWizard.scenePlan(setOf(R.Kick, R.Snare, R.Hat), setOf(P.Bass, P.Pad), acid = true)
        assertEquals(mapOf(R.Kick to setOf(0, 1, 2), R.Snare to setOf(1, 2), R.Hat to setOf(0, 1, 2)), plan.lines)
        // The chords are the Refrain's own layer: not in the Intro either (they used to play there,
        // vanish in the Couplet and come back).
        assertEquals(mapOf(P.Bass to setOf(1, 2), P.Pad to setOf(2)), plan.parts)
        assertEquals(setOf(1, 2), plan.acid)
        assertEquals(listOf("Kick (grosse caisse)", "Charley fermé (hi-hat)"), plan.contents(0))
        assertEquals(listOf("Kick (grosse caisse)", "Caisse claire", "Charley fermé (hi-hat)", "Basse", "Ligne acide"), plan.contents(1))
        assertEquals("Couplet et Refrain", plan.sections(plan.parts.getValue(P.Bass)))
        assertEquals("Intro, Couplet et Refrain", plan.sections(setOf(2, 0, 1)))
        assertEquals("Refrain", plan.sections(setOf(2)))
        // With a lead, the chords play from the Intro on and never stop.
        val withLead = StarterWizard.scenePlan(setOf(R.Kick, R.Hat), setOf(P.Bass, P.Pad, P.Lead), acid = false)
        assertEquals(setOf(0, 1, 2), withLead.parts[P.Pad])
    }

    @Test
    fun aBeatAloneKeepsItsOpenHatAndPercussionForTheDrop() {
        val style = StarterWizard.style("house")!!
        val lines = style.lines.associate { it.role to null as PadSample? }
        val p = StarterWizard.build(style, lines)
        for (line in style.lines) {
            val expected = when (line.role) {
                R.Kick -> listOf(true, true, true, false)
                R.Hat -> listOf(true, true, true, true)
                R.OpenHat, R.Perc -> listOf(false, false, true, true)
                else -> listOf(false, true, true, false)
            }
            assertEquals(line.role.title, expected, p.drumsIn(line.role))
        }
        assertEquals(listOf("Intro", "Groove", "Drop", "Break"), (0..3).map { p.sceneLabel(it) })
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
    fun theSongHasSixteenBarSectionsAndLastsAboutTwoToThreeAndAHalfMinutes() {
        for (style in StarterWizard.STYLES) for (melody in listOf(false, true)) {
            val lines = style.lines.associate { it.role to null as PadSample? }
            val parts = if (melody) P.entries.associateWith { StarterWizard.soundsFor(it, style).first() } else emptyMap()
            val p = StarterWizard.build(style, lines, StarterWizard.Mood.Dark.takeIf { melody }, parts)
            val seconds = p.songBars * p.beatsPerBar * 60.0 / p.tempoBpm
            assertTrue("${style.name} $melody: $seconds s", seconds in 120.0..220.0)
            // What the assistant's summary announces is what it builds.
            val plan = StarterWizard.planFor(style, lines.keys, parts.keys, acid = melody && style.melody.acid != null)
            assertEquals(style.name, p.song, StarterWizard.songOf(style, plan))
            assertEquals(style.name, seconds, StarterWizard.songSeconds(style, plan), 0.01)
            // Couplets and Refrains (Grooves and Drops) of 16 bars at least, whatever the tempo; a Montée of 8.
            val sections = StarterWizard.sectionsOf(style.bpm, plan.form, plan.hasBreak)
            val montee = plan.form == StarterWizard.SongForm.Build
            assertTrue(style.name, sections.filter { it.second in 1..2 }.all { it.first >= if (montee && it.second == 1) StarterWizard.BUILD_BARS else 16 })
        }
        // A song: 16-bar Couplets and Refrains, an 8-bar Intro, 4 bars when that would last over 3 min 30.
        assertEquals(listOf(2, 4, 4, 4, 4, 2), StarterWizard.songOf(4, 128.0).map { it.repeats })
        assertEquals(listOf(2, 4, 4, 4, 4, 2), StarterWizard.songOf(4, 95.0).map { it.repeats })  // 3 min 22 (was 1 min 41)
        assertEquals(listOf(1, 4, 4, 4, 4, 1), StarterWizard.songOf(4, 85.0).map { it.repeats })
        assertEquals(listOf(8, 16, 16, 16, 16, 8), StarterWizard.songOf(1, 128.0).map { it.repeats })
        // A section longer than a song part can repeat is cut in two parts of the same scene.
        val fast = StarterWizard.songOf(1, 174.0, form = StarterWizard.SongForm.Club, hasBreak = true)
        assertEquals(listOf(0, 1, 2, 2, 3, 2, 2, 0), fast.map { it.scene })
        assertEquals(120, fast.sumOf { it.repeats })
    }

    @Test
    fun sectionNamesAreSavedInFrenchWhateverTheLanguage() {
        com.pocketstudio.model.i18n.I18n.lang = com.pocketstudio.model.i18n.Lang.EN
        try {
            val house = StarterWizard.build(StarterWizard.style("house")!!, StarterWizard.style("house")!!.lines.associate { it.role to null })
            assertEquals(listOf("Intro", "Groove", "Drop", "Break"), house.allScenes.map { it.label })
            assertEquals("D · Break", house.sceneTitle(3))
            val pop = StarterWizard.build(StarterWizard.style("pop")!!, emptyMap())
            assertEquals(listOf("Intro", "Couplet", "Refrain"), pop.allScenes.map { it.label })
            assertEquals("B · Verse", pop.sceneTitle(1))
        } finally {
            com.pocketstudio.model.i18n.I18n.lang = com.pocketstudio.model.i18n.Lang.FR
        }
    }

    @Test
    fun everyChoiceFollowsThePlanAndSurvivesSavingAndSwitching() {
        for (style in StarterWizard.STYLES) for (mood in listOf(null) + StarterWizard.Mood.entries.take(2)) {
            val lines = style.lines.associate { it.role to null as PadSample? }
            val parts = if (mood == null) emptyMap() else P.entries.associateWith { StarterWizard.soundsFor(it, style).first() }
            val p = StarterWizard.build(style, lines, mood, parts)
            val plan = StarterWizard.planFor(style, lines.keys, parts.keys, acid = mood != null && style.melody.acid != null)
            assertEquals(plan.count, p.allScenes.size)
            // Cumulative over the first three (a line in a section is in every section after it); with a
            // Montée, what the Intro and the Montée play is in the Drop, and the Montée has no kick.
            val montee = plan.form == StarterWizard.SongForm.Build
            fun builds(list: List<Boolean>) = if (montee) list[2] || !(list[0] || list[1]) else list.take(3).dropWhile { !it }.all { it }
            for (line in style.lines) {
                val expected = List(plan.count) { it in plan.lines.getValue(line.role) }
                assertEquals("${style.name} $mood ${line.role}", expected, p.drumsIn(line.role))
                assertTrue("${style.name} $mood ${line.role}", builds(expected))
            }
            if (montee) assertFalse(style.name, p.drumsIn(R.Kick)[1])
            if (parts.isNotEmpty()) P.entries.forEachIndexed { i, part ->
                val id = p.instrumentTracks[i].id
                assertEquals("${style.name} $mood $part", List(plan.count) { it in plan.parts.getValue(part) }, p.notesIn(id))
                assertTrue(p.notesIn(id)[2])  // the Refrain (Drop) plays everything
                assertTrue(builds(p.notesIn(id)))
            }
            // The intro never has the bass (nor the Montée); the Break never the kick, the bass only where the style keeps it.
            if (parts.isNotEmpty()) assertFalse(p.notesIn(p.instrumentTracks[0].id)[0])
            if (parts.isNotEmpty() && montee) assertFalse(p.notesIn(p.instrumentTracks[0].id)[1])
            if (plan.hasBreak) {
                assertFalse(style.name, p.drumsIn(R.Kick)[3])
                val keepsBass = style.breakWith?.parts.orEmpty().contains(P.Bass)
                if (parts.isNotEmpty()) assertEquals(style.name, keepsBass, p.notesIn(p.instrumentTracks[0].id)[3])
            }
            assertTrue("${style.name} $mood", p.allScenes.none { it.isEmpty })
            assertEquals(p, ProjectJson.decode(ProjectJson.encode(p)))
            assertEquals(p, p.switchScene(2).switchScene(0))
        }
    }

    @Test
    fun rollsAndKitVoicesAreWrittenInTheRecipe() {
        // '2' to '4': a medium hit rolled that many times in its step.
        val hits = StarterWizard.hits("X.2.3.4.")
        assertEquals(listOf(1, 2, 3, 4), listOf(0, 2, 4, 6).map { hits.getValue(it).repeats })
        assertEquals(0.6f, hits.getValue(2).velocity)
        val style = StarterWizard.style("trap")!!
        val rolled = style.copy(lines = style.lines.map { if (it.role == R.Hat) it.copy(pattern = "x.x.x.x.x.x.x.4.") else it })
        val p = StarterWizard.build(rolled, mapOf(R.Hat to null)).switchScene(2)
        assertEquals(4, p.drums.lanes[R.Hat.lane].hits.getValue(14).repeats)
    }

    @Test
    fun aBassThatGlidesOverlapsWhereItsLineSlides() {
        val h = Harmony(5, Scale.Minor)
        // '/' holds the note and slides into the next: the root lasts into the octave by a few ticks.
        val clip = NoteClip().withLine(listOf(0), h, "R--------/O-----", 36)
        val (root, octave) = clip.notes
        assertEquals(10 * NoteClip.TICKS_PER_STEP, octave.start)
        assertEquals(octave.start + StarterWizard.SLIDE_OVERLAP, root.end)
        assertEquals(12, octave.pitch - root.pitch)
        assertTrue(StarterWizard.isLine(clip.notes))
        assertFalse(SampledPatch.isMonophonic(clip.notes))
        assertFalse(StarterWizard.isLine(NoteClip().withChordRhythm(listOf(0), h, "X---------------", Voicing.Triad, 48).notes))
        // Without '/' nothing overlaps.
        assertTrue(SampledPatch.isMonophonic(NoteClip().withLine(listOf(0), h, "R---------O-----", 36).notes))
        // A gliding style: the synth bass gets its portamento, a pack sound keeps its slide on the line.
        val trap = StarterWizard.style("trap")!!.let { it.copy(melody = it.melody.copy(bassPattern = "R--------/O-----", bassGlide = true)) }
        val synth = StarterWizard.soundsFor(P.Bass, trap).first { it.sampled == null && it.plaits == null }
        val p = StarterWizard.build(trap, emptyMap(), StarterWizard.Mood.Dark, mapOf(P.Bass to synth))
        assertEquals(StarterWizard.BASS_GLIDE, p.track(0).synth.get(SynthPatch.GLIDE))
        val pack = PackManifest("trap", "Trap", sounds = listOf(PackSound("808_01.flac", "808", "808 1")))
        val sampled = StarterWizard.soundsFor(P.Bass, trap, listOf(pack)).first { it.sampled != null }
        val placed = sampled.copy(sampled = sampled.sampled!!.copy(file = "abc.wav"))
        val q = StarterWizard.build(trap, emptyMap(), StarterWizard.Mood.Dark, mapOf(P.Bass to placed))
        assertEquals(SampledPatch.SLIDE, q.track(0).sampled.glide)
        // The 303 reads '/' as a tie that slides.
        val acid = acidLine(h, "R/O.............", "", "")
        assertTrue(acid.steps[0].slide && !acid.steps[1].gate && acid.steps[2].gate)
        // A style without glide: the bass synth stays as it was.
        val plain = StarterWizard.style("house")!!
        val house = StarterWizard.build(plain, emptyMap(), StarterWizard.Mood.Dark, mapOf(P.Bass to StarterWizard.soundsFor(P.Bass, plain).first { it.plaits == null && it.sampled == null }))
        assertEquals(0f, house.track(0).synth.get(SynthPatch.GLIDE))
    }

    @Test
    fun theStylesEffectsGoOnWhatIsUsed() {
        val base = StarterWizard.style("lofi")!!
        val style = base.copy(
            effects = listOf(
                StarterWizard.StyleFx("pad", insert = "VINYL"),
                StarterWizard.StyleFx("bass", preset = "pump"),
                StarterWizard.StyleFx("snare", reverb = 0.4f),
                StarterWizard.StyleFx("lead", preset = "dub_echo"),
                StarterWizard.StyleFx("acid", preset = "distortion"),
            ),
        )
        val parts = listOf(P.Bass, P.Pad).associateWith { StarterWizard.soundsFor(it, style).first() }
        val p = StarterWizard.build(style, mapOf(R.Kick to null, R.Snare to null), StarterWizard.Mood.Dark, parts)
        val (bass, pad) = p.instrumentTracks
        val channels = p.mixer.channels
        assertEquals(EffectType.VINYL.ordinal, channels[InstrumentTrack.mixerChannel(pad.id)].inserts[0].type)
        assertEquals(FxPresets.byId("pump")!!.inserts, channels[InstrumentTrack.mixerChannel(bass.id)].inserts.filter { it.type != 0 })
        assertEquals(0.4f, channels[Mixer.padChannel(R.Snare.lane)].sends[0])
        // No lead, no acid line: their effects go nowhere; the rest of the mixer is untouched.
        val touched = setOf(InstrumentTrack.mixerChannel(pad.id), InstrumentTrack.mixerChannel(bass.id), Mixer.padChannel(R.Snare.lane))
        channels.forEachIndexed { i, c -> if (i !in touched) assertEquals("channel $i", Channel().sanitized(), c) }
    }

    @Test
    fun aKitVoiceCanPlayTheMelody() {
        // The phonk's 808 cowbell: the kit's own cowbell, played across the keys.
        val base = StarterWizard.style("phonk")!!
        val style = base.copy(melody = base.melody.copy(packSounds = base.melody.packSounds + (P.Lead to StarterWizard.KIT_SOUND + "11")))
        val first = StarterWizard.soundsFor(P.Lead, style).first()
        assertEquals(11, first.kitVoice)
        assertEquals("Cowbell 808", first.name)
        assertNull(first.packFile)
        // Its synth stand-in is still offered, and "kit" is no pack to download.
        assertTrue(StarterWizard.soundsFor(P.Lead, style).any { it.name == style.melody.lead })
        assertFalse("kit" in style.packs)
    }

    @Test
    fun theAcidLineHasItsOwnStepAndCanBeLeftOut() {
        val style = StarterWizard.style("acid_techno")!!
        assertTrue(StarterWizard.Step.Acid in StarterWizard.steps(style))
        assertFalse(StarterWizard.Step.Acid in StarterWizard.steps(style, melody = false))
        assertFalse(StarterWizard.Step.Acid in StarterWizard.steps(StarterWizard.style("techno")!!))
        val without = StarterWizard.build(style, mapOf(R.Kick to null), StarterWizard.Mood.Dark, acid = false)
        assertTrue(without.allScenes.all { s -> s.acid.steps.none { it.gate } })
    }

    @Test
    fun theDefaultPartsAreTheRecipes() {
        // Riddim: the bass alone; gabber: no bass (the kick is the bass); acid: the 303 alone.
        assertEquals(setOf(P.Bass), StarterWizard.defaultParts(StarterWizard.style("riddim")!!))
        assertFalse(P.Bass in StarterWizard.defaultParts(StarterWizard.style("gabber")!!))
        assertEquals(emptySet<P>(), StarterWizard.defaultParts(StarterWizard.style("acid_techno")!!))
        assertEquals(emptySet<P>(), StarterWizard.defaultParts(StarterWizard.style("acid_house")!!))
        for (s in StarterWizard.STYLES) assertEquals(s.name, s.melody.parts, StarterWizard.defaultParts(s))
    }
}

class ChordPadsTest {
    @Test
    fun padsAreTheScalesChords() {
        val aMinor = Harmony(9, Scale.Minor).chordPads(48)
        assertEquals(listOf("i", "ii°", "III", "iv", "v", "VI", "VII", "i"), aMinor.map { it.roman })
        assertEquals("La m", aMinor[0].name(english = false))
        assertEquals("Am", aMinor[0].name(english = true))
        assertEquals(listOf(57, 60, 64), aMinor[0].notes)
        assertEquals(aMinor[0].notes.map { it + 12 }, aMinor[7].notes)
        val cMajor = Harmony(0, Scale.Major).chordPads(48)
        assertEquals(listOf("Do", "Ré m", "Mi m", "Fa", "Sol", "La m", "Si dim", "Do"), cMajor.map { it.name(false) })
        // English notation: no French note name anywhere (C D E, as chord charts write them).
        assertEquals(listOf("C", "Dm", "Em", "F", "G", "Am", "Bdim", "C"), cMajor.map { it.name(true) })
        val french = Regex("Do|Ré|Mi|Fa|Sol|La|Si")
        Scale.entries.forEach { sc -> (0..11).forEach { root ->
            Harmony(root, sc).chordPads(48).forEach { pad -> assertFalse(pad.name(true), french.containsMatchIn(pad.name(true))) }
        } }
        // No scale, or a pentatonic one: the major scale of the root, every note in it.
        val free = Harmony(2, Scale.Off).chordPads(48)
        assertTrue(free.all { pad -> pad.notes.all { Harmony(2, Scale.Major).inScale(it) } })
        assertTrue(Harmony(4, Scale.MinorPentatonic).chordPads(60).all { it.notes.size == 3 })
    }

}

/** Track [id]'s notes in the first scene that plays them. */
private fun Project.notesOf(id: Int): NoteClip =
    allScenes.map { if (id == 0) it.synthNotes else it.trackNotes[id] ?: NoteClip() }.firstOrNull { it.notes.isNotEmpty() } ?: track(id).notes
