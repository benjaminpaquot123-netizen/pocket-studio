package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.random.Random

private typealias AR = StarterWizard.Role

/**
 * The 303's knobs as automation targets (kind 5), the richer acid generator and the Acidcore
 * style (Confusion A – Out of World): the 303 alone in the intro, its filter opening and sweeping.
 */
class AcidcoreTest {
    private fun style(id: String) = StarterWizard.style(id)!!

    private fun defaultSong(style: StarterWizard.Style): Project {
        val lines = style.lines.associate { it.role to null as PadSample? }
        val parts = StarterWizard.defaultParts(style).associateWith { StarterWizard.soundsFor(it, style).first() }
        return StarterWizard.build(style, lines, style.melody.mood, parts)
    }

    @Test
    fun the303KnobsAreAutomationTargets() {
        assertEquals(5, AutoTarget.ACID)
        val cutoff = AutoTarget.acid(0)
        assertTrue(cutoff.isValid)
        assertEquals((5 shl 16) or (1 shl 12), cutoff.pack())
        assertEquals(listOf(0, 1, 2, 3, 4, 7), AutoTarget.ACID_PARAMS.filter { AutoTarget.acid(it).isValid })
        // Tuning, waveform and volume are not knobs a lane turns; nor another channel.
        for (p in listOf(5, 6, 8, 9)) assertFalse("$p", AutoTarget.acid(p).isValid)
        assertFalse(cutoff.copy(ch = 2).isValid)
        assertEquals(0, cutoff.acidParam)
        assertEquals(null, AutoTarget(AutoTarget.VOLUME, 1).acidParam)
        val lanes = AutomationLane.sanitize(listOf(AutomationLane(cutoff), AutomationLane(AutoTarget.acid(5)), AutomationLane(AutoTarget.acid(7))))
        assertEquals(listOf(cutoff, AutoTarget.acid(7)), lanes.map { it.target })
        // Saved and read back; a MIDI knob can move it too.
        val p = Project(automation = lanes, midiMap = listOf(MidiMapping(20, cutoff)))
        val back = ProjectJson.decode(ProjectJson.encode(p))
        assertEquals(lanes, back.automation)
        assertEquals(listOf(MidiMapping(20, cutoff)), MidiMapping.sanitize(back.midiMap))
        assertEquals(emptyList<MidiMapping>(), MidiMapping.sanitize(listOf(MidiMapping(21, AutoTarget.acid(6)))))
    }

    /** The generator as it was before its feels (phase 25), to prove the default did not change. */
    private fun legacy(random: Random, pattern: AcidPattern, noteSet: List<Int>): AcidPattern {
        val steps = pattern.steps.toMutableList()
        for (i in 0 until pattern.length) {
            if (i in pattern.locked) continue
            val base = when {
                i % 4 == 0 -> 0.6
                i % 3 == 0 -> 0.5
                i % 2 == 0 -> 0.3
                else -> 0.1
            }
            val d = pattern.density.toDouble()
            val chance = if (d <= 0.8) base * d / 0.8 else base + (1.0 - base) * (d - 0.8) / 0.2
            steps[i] = if (random.nextDouble() < chance) {
                val offset = noteSet.random(random)
                AcidStep(note = offset % 12, octave = (offset / 12).coerceAtMost(2), accent = random.nextDouble() < 0.3, slide = random.nextDouble() < 0.1, gate = true)
            } else {
                AcidStep()
            }
        }
        if (0 !in pattern.locked && !steps[0].gate) steps[0] = AcidStep(gate = true, accent = true)
        return pattern.copy(steps = steps)
    }

    @Test
    fun theDefaultGeneratorIsUnchanged() {
        for (seed in 0 until 40) for (length in listOf(16, 32, 64)) for (density in listOf(0.3f, 0.8f, 1f)) {
            val pattern = AcidPattern(length = length, density = density, locked = setOf(3, 17))
            val set = AcidGenerator.NOTE_SETS[seed % AcidGenerator.NOTE_SETS.size]
            assertEquals("$seed $length $density", legacy(Random(seed), pattern, set), AcidGenerator(Random(seed)).generate(pattern, set))
        }
    }

    @Test
    fun aTwoBarPhraseAnswersItsFirstBarAndJumpsOctaves() {
        var jumps = 0
        var changed = 0
        for (seed in 0 until 30) {
            val p = AcidGenerator(Random(seed)).generate(AcidPattern(density = 0.95f), AcidGenerator.NOTE_SETS[4], AcidGenerator.Feel.ACIDCORE)
            assertEquals(32, p.length)
            // The answer starts like the call; only its second half may differ (and its last step turns).
            assertEquals(p.steps.subList(0, 8), p.steps.subList(16, 24))
            if (p.steps.subList(8, 15) != p.steps.subList(24, 31)) changed++
            assertTrue(p.steps[31].gate && p.steps[31].slide && p.steps[31].accent)
            jumps += p.steps.take(16).count { it.gate && it.octave >= 1 }
            // Almost every sixteenth plays.
            assertTrue(p.steps.take(32).count { it.gate } >= 20)
        }
        assertTrue("the answers vary: $changed", changed >= 15)
        assertTrue("octave jumps: $jumps", jumps > 30 * 3)
        // A longer line repeats the phrase; locked steps stay.
        val locked = AcidPattern(length = 64, locked = setOf(40)).withStep(40, AcidStep(note = 7, gate = true))
        val long = AcidGenerator(Random(5)).generate(locked, AcidGenerator.NOTE_SETS[1], AcidGenerator.Feel.ACID_TECHNO)
        assertEquals(64, long.length)
        assertEquals(long.steps.subList(0, 32).filterIndexed { i, _ -> i != 8 }, long.steps.subList(32, 64).filterIndexed { i, _ -> i != 8 })
        assertEquals(AcidStep(note = 7, gate = true), long.steps[40])
        assertEquals(AcidGenerator.Feel.ACIDCORE, AcidGenerator.Feel.forStyle("acidcore"))
        assertEquals(AcidGenerator.Feel.CLASSIC, AcidGenerator.Feel.forStyle("house"))
    }

    @Test
    fun aTwoBarRiffFillsTwoBarsOfThe303() {
        val h = Harmony(6, Scale.Phrygian)
        val line = acidLine(h, "R.O.".repeat(4) + "R2O.".repeat(4), "x".repeat(32), ".".repeat(30) + "x.")
        assertEquals(32, line.length)
        assertEquals(42, line.root)  // F#2
        assertTrue(line.steps[17].gate && line.steps[17].note == 1)  // the flat second of F# phrygian: G
        assertTrue(line.steps[18].octave == 1 && line.steps[30].slide)
        assertFalse(line.steps[32].gate)
    }

    @Test
    fun acidcoreIsThe303AloneThenTheGabberKick() {
        val s = style("acidcore")
        assertEquals("Techno", s.family)
        assertEquals(185.0, s.bpm, 0.0)
        assertEquals(StarterWizard.SongForm.Rave, s.form)
        // The built-in kick rings at 47 Hz: the key's root (F#1), as in gabber.
        val rootHz = 440.0 * 2.0.pow((24 + s.melody.root - 69) / 12.0)
        assertTrue(abs(12 * ln(47.0 / rootHz) / ln(2.0)) < 0.5)
        assertEquals("X...X...X...X...", s.lines.first { it.role == AR.Kick }.pattern)
        assertTrue(s.references.first().startsWith("Confusion A – Out of World (1997)"))
        val acid = s.melody.acid!!
        assertEquals(32, acid.pattern.length)
        // A busy riff: (almost) every sixteenth, octave jumps, accents on the high notes.
        assertTrue(acid.pattern.count { it != '.' && it != '-' } >= 28)
        assertTrue(acid.pattern.count { it == 'O' } >= 8)
        assertTrue(acid.pattern.indices.filter { acid.pattern[it] == 'O' }.count { acid.accents[it] == 'x' } >= 6)

        val p = defaultSong(s)
        assertEquals(32, p.acid.length)
        assertEquals(listOf("Intro", "Groove", "Drop", "Break"), p.allScenes.take(4).map { it.label })
        // Intro: the 303 alone; Groove: kick and offbeat hats; Drop: everything; Break: no kick.
        val (intro, groove, drop, brk) = p.allScenes
        assertTrue(intro.drums.lanes.all { it.hits.isEmpty() } && intro.acid.steps.any { it.gate })
        assertEquals(setOf(0, 4, 8, 12), groove.drums.lanes[AR.Kick.lane].hits.keys)
        assertEquals(setOf(2, 6, 10, 14), groove.drums.lanes[AR.Hat.lane].hits.keys)
        assertTrue(groove.drums.lanes[AR.OpenHat.lane].hits.isEmpty())
        assertEquals(12, drop.drums.lanes[AR.Hat.lane].hits.size)
        assertTrue(drop.drums.lanes[AR.OpenHat.lane].hits.isNotEmpty())
        assertTrue(brk.drums.lanes.all { it.hits.isEmpty() } && brk.acid.steps.any { it.gate })
        // Intro, Groove, Montée, Drop, Break, Montée, Drop, Break, Montée, Drop, outro: 4 min 40 (a techno style: 5 to 6 minutes
        // at a slower tempo).
        val order = StarterWizard.songSections(p).map { p.allScenes[it.scene].label }
        assertEquals(listOf("Intro", "Groove", "Montée", "Drop", "Break", "Montée", "Drop", "Break", "Montée", "Drop", "Intro"), order)
        val seconds = p.songBars * 4 * 60.0 / p.tempoBpm
        assertTrue("$seconds s", seconds in 270.0..SongShape.LONG_SECONDS)
        // The 303's sound: Hurleur, saturated; the kick distorted; the master compressed and saturated.
        assertTrue(p.acid.sound.drive >= 0.6f && p.acid.sound.resonance >= 0.7f)
        assertTrue(p.mixer.channels[1].inserts.any { it.effect == EffectType.SATURATION })
        assertEquals(FxPresets.byId("distortion")!!.macros, p.mixer.channels[Mixer.padChannel(AR.Kick.lane)].macros)
        assertEquals(listOf(EffectType.OTT, EffectType.SATURATION), p.mixer.master.filter { it.effect != EffectType.NONE }.map { it.effect })
        assertEquals(0.3f, p.mixer.master.first().params[0], 1e-6f)
    }

    /**
     * Benjamin (APK 233): « quand j'écoute toutes les pistes ensemble le gros kick disparaît ». The 303's saturation
     * (drive 0.45) did not hold its level: rendered (ps_render, his song's Drop, before the master) it made the 303
     * 10.7 LU louder (-18.3 → -7.6 LUFS) and 14 dB louder under 120 Hz, 10 LU over the drums (-17.4) and 4 dB over
     * the kick in its own band; the master then held the 303 and the kick sank (a hit lifted the mix by 0.1 dB, 4.7
     * with the drums alone). At a constant level (« Volume constant ») the 303 sits 2 LU under the kick.
     */
    @Test
    fun acidcores303SaturationKeepsItsLevelSoTheKickStaysOverIt() {
        val p = defaultSong(style("acidcore"))
        val sats = p.mixer.channels[AutoTarget.ACID_CHANNEL].inserts.filter { it.effect == EffectType.SATURATION }
        assertEquals(1, sats.size)
        assertEquals(0.45f, sats.single().params[0], 1e-6f)  // its grit as before
        assertEquals(1f, sats.single().params[EffectType.SAT_COMPENSATION])
        assertEquals(0.7f, sats.single().params[3], 1e-6f)  // at 0 dB
        assertEquals(Project.MIX_V5, p.mixVersion)
    }

    @Test
    fun aSavedAcidcoreSongGetsIts303BackUnderTheKickOnce() {
        val fresh = defaultSong(style("acidcore"))
        fun withSat(p: Project, f: (List<Float>) -> List<Float>) = p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.mapIndexed { i, c ->
            if (i != AutoTarget.ACID_CHANNEL) c else c.copy(inserts = c.inserts.map { if (it.effect == EffectType.SATURATION) it.copy(params = f(it.params)) else it })
        }))
        fun satOf(p: Project) = p.mixer.channels[AutoTarget.ACID_CHANNEL].inserts.single { it.effect == EffectType.SATURATION }.params
        val off: (List<Float>) -> List<Float> = { it.toMutableList().also { v -> v[EffectType.SAT_COMPENSATION] = 0f } }
        // As APK 233 saved it (Benjamin's song): read back at a constant level, MIX_V5, nothing else changed.
        val saved = withSat(fresh, off).copy(mixVersion = Project.MIX_V4)
        val back = ProjectJson.decode(ProjectJson.encode(saved))
        assertEquals(1f, satOf(back)[EffectType.SAT_COMPENSATION])
        assertEquals(Project.MIX_V5, back.mixVersion)
        assertEquals(ProjectJson.decode(ProjectJson.encode(fresh)), back)
        // Once: set back by hand since, it stays so.
        val byHand = withSat(back, off)
        assertEquals(0f, satOf(ProjectJson.decode(ProjectJson.encode(byHand)))[EffectType.SAT_COMPENSATION])
        // A saturation the user turned (its drive), another style, a song of another mix: left as they are.
        val turned = withSat(saved) { it.toMutableList().also { v -> v[0] = 0.6f } }
        assertEquals(0f, satOf(ProjectJson.decode(ProjectJson.encode(turned)))[EffectType.SAT_COMPENSATION])
        assertEquals(Project.MIX_V4, ProjectJson.decode(ProjectJson.encode(saved.copy(style = "acid_techno"))).mixVersion)
        assertEquals(0f, satOf(ProjectJson.decode(ProjectJson.encode(saved.copy(style = "acid_techno"))))[EffectType.SAT_COMPENSATION])
        assertEquals(0f, satOf(ProjectJson.decode(ProjectJson.encode(saved.copy(mixVersion = Project.MIX_V2))))[EffectType.SAT_COMPENSATION])
    }

    @Test
    fun acidcoreFilterOpensOverTheIntroAndSweepsInTheBreaks() {
        val p = defaultSong(style("acidcore"))
        val cutoff = p.automation.single { it.target == AutoTarget.acid(0) }
        assertEquals(AutomationLane.SONG, cutoff.mode)
        assertTrue(p.automation.any { it.target == AutoTarget.acid(2) })
        val curve = cutoff.songCurve(p.songBars)
        val sections = StarterWizard.songSections(p)
        // Intro (32 bars): closed, opening.
        val intro = sections.first()
        assertEquals(0.08f, curve[0], 1e-4f)
        assertEquals(0.45f, curve[intro.bars - 1], 1e-4f)
        assertTrue((1 until intro.bars).all { curve[it] > curve[it - 1] })
        // First Break: from dark to wide open.
        val brk = sections.first { it.scene == 3 }
        assertEquals(0.2f, curve[brk.start], 1e-4f)
        assertEquals(0.85f, curve[brk.start + brk.bars - 1], 1e-4f)
        // The outro closes it again, the last bar the darkest of the song's end.
        val outro = sections.last()
        assertEquals(0.08f, curve.last(), 1e-4f)
        assertEquals(0.45f, curve[outro.start], 1e-4f)
        // Played alone, a scene holds the curve where it first plays in the song.
        assertEquals(0.2f, p.songCurvesAt(3)[p.automation.indexOf(cutoff)]!!, 1e-4f)
    }

    @Test
    fun theAcidStylesFilterMovesGentlyToo() {
        for (id in listOf("acid_techno", "acid_house")) {
            val p = defaultSong(style(id))
            val lane = p.automation.firstOrNull { it.target == AutoTarget.acid(0) }
            assertNotNull(id, lane)
            val curve = lane!!.songCurve(p.songBars)
            // Final audit (report 11): wider since the 303 plays its phrases and opens through each Montée.
            assertTrue(id, curve.max() - curve.min() in 0.2f..0.6f)
            assertTrue(id, curve.all { it in 0.15f..0.85f })
        }
        // Without the 303, no curve for it.
        val s = style("acid_techno")
        val beat = StarterWizard.build(s, s.lines.associate { it.role to null as PadSample? }, s.melody.mood, acid = false)
        assertTrue(beat.automation.none { it.target.kind == AutoTarget.ACID })
    }

    @Test
    fun theMasterTakesAStylesInsertsInItsFreeSlots() {
        val base = style("house")
        val style = base.copy(effects = listOf(StarterWizard.StyleFx("master", insert = "OTT", params = listOf(0.2f)), StarterWizard.StyleFx("master", preset = "punch")))
        val p = StarterWizard.withEffects(Project(), style, emptySet(), emptyMap(), acid = false)
        assertEquals(listOf(EffectType.OTT, EffectType.COMPRESSOR, EffectType.SATURATION), p.mixer.master.filter { it.effect != EffectType.NONE }.map { it.effect })
        assertEquals(listOf(0.2f) + EffectType.OTT.defaults.drop(1), p.mixer.master[0].params)
    }
}
