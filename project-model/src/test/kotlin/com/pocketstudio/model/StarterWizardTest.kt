package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.pocketstudio.model.SongBars.isDrumBar
import com.pocketstudio.model.SongBars.sceneOfBars

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
                assertTrue("${s.name} ${l.role}", l.pattern.all { it in "Xxo.234Tt" } && StarterWizard.hits(l.pattern).isNotEmpty())
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
            for (line in listOf(s.melody.bassPattern, s.melody.leadPattern, s.melody.chordRhythm, s.melody.leadAnswer, s.melody.bassVariation).filter { it.isNotEmpty() }) {
                assertTrue("${s.name} $line", line.length in listOf(16, 32, 64))
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
        // The claps of the line (S3: a ghost note may join them, played very softly).
        assertEquals(setOf(4, 12, 20, 28), p.drums.lanes[4].hits.filterValues { it.velocity > DrumHumanizer.GHOST_HIGH }.keys)
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
        assertEquals(setOf(4, 12, 20, 28), p.drums.lanes[StarterWizard.Role.Clap.lane].hits.filterValues { it.velocity > DrumHumanizer.GHOST_HIGH }.keys)
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
            assertTrue(style.melody.bassPattern.length in listOf(16, 32, 64))
            assertTrue(StepLine.parse(style.melody.bassPattern).any { StepLine.stepOf(it.token) != null })
            for (mood in StarterWizard.Mood.entries) {
                val parts = StarterWizard.Part.entries.associateWith { StarterWizard.soundsFor(it, style).first() }
                val p = StarterWizard.build(style, mapOf(StarterWizard.Role.Kick to null), mood, parts)
                assertTrue(p.harmony.lock)
                assertEquals(StarterWizard.progressionOf(style, mood).first, p.harmony.scale)
                // A style with a riser gets its « Montée » track after the parts (the sub layer and the counter-melody before it); a song, its impact alone.
                val riser = style.riser
                // The 808 styles' bass plays on its pad (S3): no « Basse » track there.
                val on808 = StudioDrums.bassOn808(style, parts[P.Bass])
                val partTracks = if (on808) 2 else 3
                assertEquals(
                    listOfNotNull("Basse".takeIf { !on808 }, StarterWizard.trackName(P.Pad, parts[P.Pad], style), "Lead") +
                        listOfNotNull(
                            StarterWizard.SUB_TRACK.takeIf { style.melody.subBass }, StarterWizard.BASS_SWITCH_TRACK.takeIf { style.melody.bassSwitch.isNotBlank() },
                            // The leads chantier: liquid's pad behind its Rhodes.
                            StarterWizard.PAD_LAYER_TRACK.takeIf { style.melody.padLayer != null },
                            StarterWizard.COUNTER_TRACK.takeIf { style.melody.counter != null },
                            StarterWizard.RISER_TRACK.takeIf { riser },
                            // The vocal (all parts on: the chops have a hook to answer), then the sub drop under club music's Drops.
                            StarterWizard.VOCAL_TRACK.takeIf { style.vocal != null },
                            StarterWizard.SUB_DROP_TRACK.takeIf { !style.form.sung && !style.lightBuild },
                        ).let { extra -> extra.take(InstrumentTrack.MAX - partTracks) },
                    p.instrumentTracks.map { it.name },
                )
                for (t in p.instrumentTracks.take(partTracks)) {
                    val notes = p.notesOf(t.id)
                    assertTrue("${style.name} $mood ${t.name}", notes.notes.isNotEmpty())
                    assertTrue("${style.name} $mood ${t.name}", notes.notes.all { p.harmony.inScale(it.pitch) })
                    // Eight-bar phrases (a Montée or a short Intro cuts them to its own length).
                    assertEquals(StarterWizard.PHRASE_STEPS, p.allScenes.maxOf { (if (t.id == 0) it.synthNotes else it.trackNotes[t.id] ?: NoteClip()).steps })
                    assertTrue("${style.name} $mood ${t.name} ${notes.steps}", notes.steps in setOf(64, StarterWizard.PHRASE_STEPS))
                }
                // The bass plays each chord's root, bar after bar (the 808: its hits' notes from its tuning, in the Refrain).
                val tune = p.padVoices[StudioDrums.LANE_808]?.param(PadVoice.TUNE)?.toInt() ?: 0
                val bass = if (on808) p.allScenes[2].drums.lanes[StudioDrums.LANE_808].hits.map { (st, h) -> Note(st * NoteClip.TICKS_PER_STEP, 1, tune + h.pitch) }
                else p.notesOf(0).notes
                StarterWizard.progressionOf(style, mood).second.forEachIndexed { bar, d ->
                    val inBar = bass.filter { it.start / (16 * NoteClip.TICKS_PER_STEP) == bar }
                    val root = Math.floorMod(p.harmony.degreeRoot(d, 36), 12)
                    assertTrue("${style.name} $mood bar $bar", inBar.any { Math.floorMod(it.pitch, 12) == root })
                }
                // A style without a written lead arpeggiates on a fixed pattern (never a triad up and down).
                if (style.melody.leadPattern.isBlank()) assertEquals(ArpMode.Pattern, p.instrumentTracks[partTracks - 1].notes.performance.arp)
                // Plaits sounds land on a Plaits track, kit voices played across the keys (phonk's cowbell) on a sampler.
                (if (on808) StarterWizard.Part.entries - P.Bass else StarterWizard.Part.entries).forEachIndexed { i, part ->
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

    /** The plan's own scenes (Intro, Couplet or Groove, Refrain or Drop, and the Break or the Pont): the variants come after them. */
    private fun Project.sections() = allScenes.take(if (allScenes.getOrNull(3)?.label in setOf("Break", "Pont")) 4 else 3)

    /** Whether each of the plan's scenes plays drum line [role]. */
    private fun Project.drumsIn(role: StarterWizard.Role) = sections().map { it.drums.lanes[role.lane].hits.isNotEmpty() }

    /** Whether each of the plan's scenes plays track [id]'s notes. */
    private fun Project.notesIn(id: Int) = sections().map { (if (id == 0) it.synthNotes else it.trackNotes.getValue(id)).notes.isNotEmpty() }

    /** The song's sections by name: a variant counted with its section ("Drop 2" in the Drop). */
    private fun Project.sectionNames() = StarterWizard.songSections(this).map { allScenes[it.scene].label }

    @Test
    fun theSongIsAnIntroACoupletAndARefrainThatPileUp() {
        val style = StarterWizard.style("pop")!!
        assertEquals(StarterWizard.SongForm.Song, style.form)
        val lines = style.lines.associate { it.role to null as PadSample? }
        val parts = P.entries.associateWith { StarterWizard.soundsFor(it, style).first() }
        val p = StarterWizard.build(style, lines, StarterWizard.Mood.Dark, parts)
        assertEquals(listOf("A", "B", "C", "D"), p.allScenes.take(4).map { it.name })
        assertEquals(listOf("A · Intro", "B · Couplet", "C · Refrain", "D · Pont"), (0..3).map { p.sceneTitle(it) })
        assertEquals(0, p.scene)
        // Intro, Couplet, Pré-refrain, Refrain, Post-refrain, Couplet, Pré-refrain, Refrain, Pont, Refrain, and the Intro again to end.
        assertEquals(
            listOf("Intro", "Couplet", "Pré-refrain", "Refrain", "Post-refrain", "Couplet", "Pré-refrain", "Refrain", "Pont", "Refrain", "Intro"),
            p.sectionNames(),
        )
        assertEquals(p.song.first().repeats, p.song.last().repeats)
        // The first Refrain comes within 45 seconds.
        val firstRefrain = StarterWizard.songSections(p).first { it.scene == 2 }
        assertTrue(firstRefrain.start * 4 * 60.0 / p.tempoBpm <= SongShape.FIRST_CHORUS_SECONDS)
        // Intro: the kick and the hats, the hook previewed; Couplet: the drums but the open hat and the
        // percussion, the bass and the hook thinned out; Refrain: everything, the piano too; Pont: the
        // hats, the piano and the hook, without kick nor bass.
        for (line in style.lines) {
            val expected = when (line.role) {
                R.Kick -> listOf(true, true, true, false)
                R.Hat -> listOf(true, true, true, true)
                // The tambourine (the second percussion) waits for the Refrain too, and stays in the Pont.
                R.Perc, R.Perc2 -> listOf(false, false, true, true)
                R.OpenHat -> listOf(false, false, true, false)
                else -> listOf(false, true, true, false)
            }
            assertEquals(line.role.title, expected, p.drumsIn(line.role))
        }
        val (bass, pad, lead) = p.instrumentTracks
        // The pop's chords are a piano: its track is named so (a beginner looks for « le piano »).
        assertEquals(listOf("Basse", "Piano", "Lead"), listOf(bass.name, pad.name, lead.name))
        assertEquals(listOf(false, true, true, false), p.notesIn(bass.id))
        assertEquals(listOf(false, false, true, true), p.notesIn(pad.id))
        assertEquals(listOf(true, true, true, true), p.notesIn(lead.id))
        // The second Refrain scene plays the hook doubled (its fourth version), the Post-refrain the hook without the piano.
        val refrain2 = p.allScenes.indexOfFirst { it.label == "Refrain 2" }
        assertEquals(StarterWizard.startingNotes(P.Lead, style, StarterWizard.Mood.Dark, p.harmony, parts[P.Lead], StarterWizard.Version.Chorus2).notes,
            p.allScenes[refrain2].trackNotes.getValue(lead.id).notes)
        val post = p.allScenes.first { it.label == SongShape.POST_CHORUS }
        assertTrue(post.trackNotes.getValue(pad.id).notes.isEmpty() && post.trackNotes.getValue(lead.id).notes.isNotEmpty())
        // Three versions of the hook: the Intro's preview and the Couplet's line are parts of the Refrain's.
        val refrain = p.allScenes[2].trackNotes.getValue(lead.id)
        val couplet = p.allScenes[1].trackNotes.getValue(lead.id)
        assertTrue(MelodyMetrics.variant(couplet, refrain).subset && couplet.notes.size * 5 <= refrain.notes.size * 3)
        assertTrue(MelodyMetrics.variant(p.allScenes[0].trackNotes.getValue(lead.id), refrain).subset)
        assertTrue(p.allScenes.none { it.isEmpty })
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
        // Techno lasts 5 to 6 minutes: an intro of 32 bars, a Groove of 24 and a Montée of 8, Drops of 32, a Break of 16.
        assertEquals(listOf("Intro", "Groove", "Montée", "Drop", "Break", "Montée", "Drop", "Intro"), p.sectionNames())
        assertEquals(listOf(32, 24, 8, 32, 16, 8, 32, 24), StarterWizard.songSections(p).map { it.bars })
        assertTrue(p.songBars * 4 * 60.0 / p.tempoBpm in 300.0..SongShape.LONG_SECONDS)
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
        // Club music of about 3 minutes (house): Intro 16, Groove 8 and Montée 8, Drop 16, Break 8, Montée 8, Drop 16, Intro 16.
        val house = StarterWizard.sectionsOf(124.0, StarterWizard.SongForm.Club, hasBreak = true)
        assertEquals(listOf(16 to 0, 16 to 1, 16 to 2, 8 to 3, 8 to 1, 16 to 2, 16 to 0), house)
        // A long one at a fast tempo (jungle) has room for a third Drop under 6 minutes.
        val fast = StarterWizard.sectionsOf(165.0, StarterWizard.SongForm.Club, hasBreak = true, long = true)
        assertEquals(listOf(32, 32, 32, 16, 8, 32, 8, 8, 32, 24), fast.map { it.first })
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
        // Its Pont too (the chords without kick nor bass), and every variant of the song.
        assertEquals(listOf(true, true, true, true), p.notesIn(p.instrumentTracks[1].id))
        assertTrue(p.allScenes.filter { !it.isDrumBar() }.all { it.trackNotes.getValue(p.instrumentTracks[1].id).notes.isNotEmpty() })
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
        // With a lead, the hook plays from the Intro on and never stops; the chords still lift the Refrain.
        val withLead = StarterWizard.scenePlan(setOf(R.Kick, R.Hat), setOf(P.Bass, P.Pad, P.Lead), acid = false)
        assertEquals(setOf(0, 1, 2), withLead.parts[P.Lead])
        assertEquals(setOf(2), withLead.parts[P.Pad])
    }

    @Test
    fun aBeatAloneKeepsItsOpenHatAndPercussionForTheDrop() {
        val style = StarterWizard.style("house")!!
        val lines = style.lines.associate { it.role to null as PadSample? }
        val p = StarterWizard.build(style, lines)
        for (line in style.lines) {
            val expected = when (line.role) {
                R.Kick -> listOf(true, true, true, false)
                // House has its own Break: the hats and the shaker under the chords (here alone).
                R.Hat -> listOf(true, true, true, true)
                R.Perc -> listOf(false, false, true, true)
                // The open hat, the ride and the conga wait for the Drop.
                R.OpenHat, R.Ride, R.Perc2 -> listOf(false, false, true, false)
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
    fun theSongLastsFiveToSixMinutesInTechnoAndBassMusicAndAboutThreeElsewhere() {
        for (style in StarterWizard.STYLES) for (melody in listOf(false, true)) {
            val lines = style.lines.associate { it.role to null as PadSample? }
            val parts = if (melody) P.entries.associateWith { StarterWizard.soundsFor(it, style).first() } else emptyMap()
            val p = StarterWizard.build(style, lines, StarterWizard.Mood.Dark.takeIf { melody }, parts)
            val seconds = p.songBars * p.beatsPerBar * 60.0 / p.tempoBpm
            // Benjamin's choice (2026-10-23): techno and bass music 5 to 6 minutes (4 min 30 at the fastest tempos), the rest about 3.
            val long = style.family in setOf("Techno", "Bass music")
            assertTrue("${style.name} $melody: $seconds s", seconds in (if (long) 270.0..SongShape.LONG_SECONDS else 120.0..StarterWizard.MAX_SONG_SECONDS))
            assertEquals(style.name, if (long) SongShape.LONG_SECONDS else StarterWizard.MAX_SONG_SECONDS, StarterWizard.maxSongSeconds(style), 0.0)
            // What the assistant's summary announces is what it builds.
            val plan = StarterWizard.planFor(style, lines.keys, parts.keys, acid = melody && style.melody.acid != null)
            // Bar by bar (S3 cuts a part around a one-bar fill or crash, which plays its drums only).
            val announced = p.copy(song = StarterWizard.songOf(style, plan))
            assertEquals(style.name, announced.sceneOfBars(), p.sceneOfBars())
            assertEquals(style.name, seconds, StarterWizard.songSeconds(style, plan), 0.01)
            assertEquals(style.name, plan.sceneCount, p.allScenes.count { !it.isDrumBar() })
            // Grooves and Drops of 16 bars at least; Couplets of 8 (a rap Couplet 16), Refrains of 8; a Montée of 8.
            val sections = StarterWizard.songSections(p)
            for (sec in sections) {
                val label = p.allScenes[sec.scene].label
                val least = when {
                    label == "Montée" || label == SongShape.PRE_CHORUS -> 4
                    label == SongShape.POST_CHORUS || label == SongShape.FAKE_DROP -> 4
                    sec.scene == 0 || sec.scene == 3 -> 4
                    plan.form.sung -> 8
                    else -> 8
                }
                assertTrue("${style.name} $label ${sec.bars}", sec.bars >= least)
            }
            // A song's first Refrain within 45 seconds.
            if (plan.form.sung && parts.isNotEmpty()) {
                val first = sections.first { it.scene == 2 }
                assertTrue("${style.name} ${first.start}", first.start * p.beatsPerBar * 60.0 / p.tempoBpm <= SongShape.FIRST_CHORUS_SECONDS)
            }
        }
        // The forms (sections merged by scene): a song, a rap song, club music, bass music that builds up.
        val song = StarterWizard.sectionsOf(116.0, StarterWizard.SongForm.Song, hasBreak = true)
        assertEquals(listOf(4 to 0, 16 to 1, 20 to 2, 16 to 1, 16 to 2, 8 to 3, 16 to 2, 4 to 0), song)
        val rap = StarterWizard.sectionsOf(140.0, StarterWizard.SongForm.Rap, hasBreak = true)
        assertEquals(listOf(8 to 0, 16 to 2, 16 to 1, 16 to 2, 16 to 1, 8 to 3, 16 to 2, 8 to 0), rap)
        // A slow rap song (boom bap, lo-fi): shorter Refrains, no Pont, under 3 min 30.
        assertEquals(listOf(4 to 0, 8 to 2, 16 to 1, 8 to 2, 16 to 1, 8 to 2, 4 to 0), StarterWizard.sectionsOf(92.0, StarterWizard.SongForm.Rap, hasBreak = true))
        val build = StarterWizard.sectionsOf(174.0, StarterWizard.SongForm.Build, hasBreak = true, long = true)
        assertEquals(listOf(32 to 0, 8 to 1, 32 to 2, 16 to 3, 8 to 1, 32 to 2, 16 to 3, 8 to 1, 32 to 2, 32 to 0), build)
        // The plan's scenes only (no variant): each section its scene repeated; a section longer than a part can repeat cut in two.
        assertEquals(listOf(2, 4, 5, 4, 8, 2), StarterWizard.songOf(4, 128.0).map { it.repeats })
        val fast = StarterWizard.songOf(1, 165.0, form = StarterWizard.SongForm.Club, hasBreak = true, long = true)
        assertEquals(listOf(0, 0, 1, 1, 2, 2, 3, 1, 2, 2, 3, 1, 2, 2, 0, 0), fast.map { it.scene })
        assertEquals(224, fast.sumOf { it.repeats })
    }

    @Test
    fun sectionNamesAreSavedInFrenchWhateverTheLanguage() {
        com.pocketstudio.model.i18n.I18n.lang = com.pocketstudio.model.i18n.Lang.EN
        try {
            val house = StarterWizard.build(StarterWizard.style("house")!!, StarterWizard.style("house")!!.lines.associate { it.role to null })
            // G0: the last Drop's own scene (« Drop 3 ») after the others.
            assertEquals(listOf("Intro", "Groove", "Drop", "Break", "Intro 2", "Montée", "Montée 2", "Drop 2", "Drop 3"), house.allScenes.filter { !it.isDrumBar() }.map { it.label })
            // Then the one-bar drum scenes (fills, crashes), named too.
            assertTrue(house.allScenes.drop(9).all { it.isDrumBar() })
            assertEquals("D · Break", house.sceneTitle(3))
            assertEquals(listOf("F · Build-up", "G · Build-up 2", "H · Drop 2"), (5..7).map { house.sceneTitle(it) })
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
            assertEquals(plan.sceneCount, p.allScenes.count { !it.isDrumBar() })
            assertTrue(p.allScenes.dropWhile { !it.isDrumBar() }.all { it.isDrumBar() })
            val on808 = StudioDrums.bassOn808(style, parts[P.Bass])
            // Each variant plays the style's own lines and parts (a roll may join a Montée), never the kick in a Break.
            for ((i, sc) in p.allScenes.withIndex().drop(plan.count).filter { !it.value.isDrumBar() }) {
                val spec = plan.variants[i - plan.count]
                assertEquals("${style.name} $mood ${sc.label}", sc.label, spec.label)
                val roles = sc.drums.lanes.indices.filter { sc.drums.lanes[it].hits.isNotEmpty() }.mapNotNull { lane -> R.entries.firstOrNull { it.lane == lane } }
                // G0: the last Drop's scene adds a layer of its own (an open hat, a percussion, claps).
                val layer = if (spec.extra) setOf(R.OpenHat, R.Perc2, R.Hat, R.Clap) else emptySet()
                assertTrue("${style.name} $mood ${sc.label} $roles", roles.all { it in lines.keys || it == R.Ride || it in layer || (on808 && it == R.Bass808) })
            }
            // Cumulative over the first three (a line in a section is in every section after it); with a
            // Montée, what the Intro and the Montée play is in the Drop, and the Montée has no kick; a rap
            // song's Intro leads straight into its Refrain (S5b: it opens on the hook and its chords), so
            // what the Intro and the Couplet play is in the Refrain.
            val montee = plan.form == StarterWizard.SongForm.Build
            val rap = plan.form == StarterWizard.SongForm.Rap
            fun builds(list: List<Boolean>) = if (montee || rap) list[2] || !(list[0] || list[1]) else list.take(3).dropWhile { !it }.all { it }
            for (line in style.lines) {
                val expected = List(plan.count) { it in plan.lines.getValue(line.role) }
                assertEquals("${style.name} $mood ${line.role}", expected, p.drumsIn(line.role))
                assertTrue("${style.name} $mood ${line.role}", builds(expected))
            }
            if (montee) assertFalse(style.name, p.drumsIn(R.Kick)[1])
            // Where each part plays: its track's notes, or the 808's pad for the bass of the 808 styles (S3).
            val tracksOf by lazy { (if (on808) P.entries - P.Bass else P.entries).withIndex().associate { (i, part) -> part to p.instrumentTracks[i].id } }
            fun playsIn(part: P) = if (on808 && part == P.Bass) p.drumsIn(R.Bass808) else p.notesIn(tracksOf.getValue(part))
            if (parts.isNotEmpty()) P.entries.forEach { part ->
                assertEquals("${style.name} $mood $part", List(plan.count) { it in plan.parts.getValue(part) }, playsIn(part))
                assertTrue(playsIn(part)[2])  // the Refrain (Drop) plays everything
                assertTrue(builds(playsIn(part)))
            }
            // The intro never has the bass (nor the Montée); the Break never the kick, the bass only where the style keeps it.
            if (parts.isNotEmpty()) assertFalse(playsIn(P.Bass)[0])
            if (parts.isNotEmpty() && montee) assertFalse(playsIn(P.Bass)[1])
            if (plan.hasBreak) {
                assertFalse(style.name, p.drumsIn(R.Kick)[3])
                val keepsBass = style.breakWith?.parts.orEmpty().contains(P.Bass)
                if (parts.isNotEmpty()) assertEquals(style.name, keepsBass, playsIn(P.Bass)[3])
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
        // (Another synth than the style's own « Basse sub », which the 808 plays on its pad since S3: it slides there.)
        val synth = StarterWizard.soundsFor(P.Bass, trap).first { it.sampled == null && it.plaits == null && it.name != trap.melody.bass }
        val p = StarterWizard.build(trap, emptyMap(), StarterWizard.Mood.Dark, mapOf(P.Bass to synth))
        assertEquals(StarterWizard.BASS_GLIDE, p.track(0).synth.get(SynthPatch.GLIDE))
        val own = StarterWizard.build(trap, emptyMap(), StarterWizard.Mood.Dark, mapOf(P.Bass to StarterWizard.soundsFor(P.Bass, trap).first()))
        val slide = own.allScenes[2].drums.lanes[StudioDrums.LANE_808].hits.getValue(10)
        assertEquals(12, slide.pitch - own.allScenes[2].drums.lanes[StudioDrums.LANE_808].hits.getValue(0).pitch)
        assertEquals(StudioDrums.glideMs(trap), slide.glide)
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
        // S5: the style's pump on the bass is the Musique bus's one duck under the kick's sound (its lows), not a pump of its own;
        // G0: its low cut (a flat EQ); « des basses qui tapent »: the bass exciter (heard on a phone).
        val bassFx = channels[InstrumentTrack.mixerChannel(bass.id)].inserts.filter { it.type != 0 }
        assertEquals(listOf(EffectType.PARAM_EQ.ordinal, EffectType.BASS_EXCITER.ordinal), bassFx.map { it.type })
        assertEquals(30f, RoleMix.lowCutHz(channels[InstrumentTrack.mixerChannel(bass.id)])!!, 1f)
        val bus = p.mixer.musicBus!!
        assertTrue(InstrumentTrack.mixerChannel(bass.id) in bus.members)
        assertEquals(FxPresets.byId("pump")!!.inserts[0].params[0] * WizardMix.BASS_PUMP_TO_LOW, bus.lowDepth, 1e-6f)
        assertEquals(0.4f, channels[Mixer.padChannel(R.Snare.lane)].sends[0])
        // The default mix: the beat glued (its strip), the kit's loud kick brought down (its pad's strip).
        assertEquals(EffectType.COMPRESSOR.ordinal, channels[0].inserts[0].type)
        assertTrue(channels[Mixer.padChannel(R.Kick.lane)].volume < 0.8f)
        // No lead, no acid line: their effects go nowhere; the rest of the mixer is untouched.
        // The kit's cymbal pad plays the crash on each Refrain's first beat: its level and its echo.
        assertEquals(listOf(0.2f, 0.25f), channels[Mixer.padChannel(R.Ride.lane)].sends)
        val touched = setOf(0, Mixer.padChannel(R.Kick.lane), InstrumentTrack.mixerChannel(pad.id), InstrumentTrack.mixerChannel(bass.id), Mixer.padChannel(R.Snare.lane), Mixer.padChannel(R.Ride.lane))
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
