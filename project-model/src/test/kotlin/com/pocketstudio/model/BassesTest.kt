package com.pocketstudio.model

import com.pocketstudio.model.StarterWizard.Part
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** « Des basses qui tapent » (docs/hits/basses-qui-tapent.md): what the assistant does with its basses. */
class BassesTest {
    private fun song(style: StarterWizard.Style): Project {
        val parts = StarterWizard.defaultParts(style).associateWith { StarterWizard.soundsFor(it, style).first() }
        return StarterWizard.build(style, style.lines.associate { it.role to null }, style.melody.mood, parts)
    }

    private val songs by lazy { StarterWizard.STYLES.associateWith(::song) }

    @Test
    fun everyBassIsHeardOnAPhoneAndNothingPlaysUnder28Hz() {
        for ((style, p) in songs) {
            // The master keeps no low cut of its own (MIX_V3 cost the Drop loudness on the bench): S7's MIX_V4 (MIX_V5 since the Acidcore kick fix).
            assertEquals(style.id, Project.MIX_V5, p.mixVersion)
            val amount = WizardMix.exciterAmount(style)
            val subs = p.instrumentTracks.filter { it.name in setOf(StarterWizard.SUB_TRACK, StarterWizard.SUB_DROP_TRACK) }.map { InstrumentTrack.mixerChannel(it.id) }
            val pads = p.padVoices.filterValues { it.kind == PadVoice.BASS_808 }.keys.map { Mixer.padChannel(it) }
            // Every strip that plays a bass has the exciter at the style's amount, before its clipper and OTT.
            for (ch in p.bassChannels()) {
                val inserts = p.mixer.channels[ch].inserts
                val at = inserts.indexOfFirst { it.type == EffectType.BASS_EXCITER.ordinal }
                assertTrue("${style.id} $ch", at >= 0)
                assertEquals("${style.id} $ch", amount, inserts[at].params[0], 1e-6f)
                for (later in listOf(EffectType.CLIPPER, EffectType.OTT)) {
                    val k = inserts.indexOfFirst { it.type == later.ordinal }
                    assertTrue("${style.id} $ch ${later.name}", k < 0 || k > at)
                }
                // One owner per low end (RoleMix): the Sub and the sub drop keep theirs whole (the
                // sub drop its saturation too), a bass gets RoleMix's adaptive low cut (or the
                // high-pass over its Sub), the 808's pad its own under its lowest note; the EQ that
                // cuts sits before the exciter (no infra feeds its harmonics).
                val cut = RoleMix.lowCutHz(p.mixer.channels[ch])
                when (ch) {
                    in subs -> {
                        assertNull("${style.id} $ch", cut)
                        if (p.instrumentTracks.any { it.name == StarterWizard.SUB_DROP_TRACK && InstrumentTrack.mixerChannel(it.id) == ch }) {
                            assertTrue("${style.id} $ch", inserts.any { it.type == EffectType.SATURATION.ordinal })
                        }
                    }
                    in pads -> assertTrue("${style.id} $ch $cut", cut != null && cut <= 30.5f)
                    else -> assertTrue("${style.id} $ch $cut", cut != null && cut <= 30.5f || inserts.any { it.type == EffectType.FILTER.ordinal })
                }
                val eqCut = inserts.indexOfFirst { it.type == EffectType.PARAM_EQ.ordinal && !it.bypass && it.params.getOrElse(3) { 0f } >= 0.01f }
                assertTrue("${style.id} $ch", eqCut < 0 || eqCut < at)
            }
        }
        // The song's curves on the basses' effects still move the effect they were drawn for: the
        // exciter and the low cut were moved into place with them, never under them (a curve on the
        // bass's filter that landed on the high-pass over its Sub silenced the bass).
        for (style in StarterWizard.STYLES) {
            val parts = StarterWizard.defaultParts(style).associateWith { StarterWizard.soundsFor(it, style).first() }
            val before = StarterWizard.build(style, style.lines.associate { it.role to null }, style.melody.mood, parts, true, null, audibleBass = false)
            val after = songs.getValue(style)
            assertEquals(style.id, before.automation.size, after.automation.size)
            for ((a, b) in before.automation.zip(after.automation)) {
                if (b.target.kind != AutoTarget.INSERT_PARAM) continue
                val was = before.mixer.channels[a.target.ch].inserts[a.target.slot]
                val now = after.mixer.channels[b.target.ch].inserts[b.target.slot]
                assertEquals("${style.id} ${b.target}", was.type, now.type)
                assertEquals("${style.id} ${b.target}", was.params, now.params)
                assertTrue("${style.id} ${b.target}", now.type != EffectType.BASS_EXCITER.ordinal)
                if (b.target.ch in after.bassChannels()) assertTrue("${style.id} ${b.target}", now.type != 0)
            }
        }
        // Per family: hip-hop the most, bass music the least; S6 (M11): +0.1 where the Drop lost over 4 LU on a phone.
        assertEquals(0.5f + WizardMix.PHONE_BUMP, WizardMix.exciterAmount(StarterWizard.style("trap")!!), 1e-6f)
        assertEquals(0.5f, WizardMix.exciterAmount(StarterWizard.style("grime")!!.copy(id = "grime_like")), 1e-6f)
        assertEquals(0.35f + WizardMix.PHONE_BUMP, WizardMix.exciterAmount(StarterWizard.style("techno")!!), 1e-6f)
        assertEquals(0.35f, WizardMix.exciterAmount(StarterWizard.style("tech_house")!!), 1e-6f)
        assertEquals(0.25f, WizardMix.exciterAmount(StarterWizard.style("dnb")!!), 1e-6f)
        // A project saved before keeps its mix (no exciter, mix version 1 unless it says so).
        assertEquals(Project.MIX_V1, ProjectJson.decode(ProjectJson.encode(Project())).mixVersion)
        assertEquals(Project.MIX_V5, Project(mixVersion = 9).sanitized().mixVersion)
    }

    @Test
    fun theFive808StylesPlayTheEnginesPunchy808() {
        for (id in listOf("trap", "drill", "phonk", "jersey_club", "reggaeton")) {
            val style = StarterWizard.style(id)!!
            assertTrue(id, id in StudioDrums.BASS_808)
            val voice = songs.getValue(style).padVoices.getValue(StudioDrums.LANE_808)
            assertEquals(id, 1f, voice.param(PadVoice.VERSION))
            assertTrue(id, voice.param(PadVoice.PITCH_DROP)!! in 18f..24f)
            assertTrue(id, voice.param(PadVoice.DROP_TIME)!! in 0.0199f..0.0351f)
            val drive = voice.param(PadVoice.DRIVE)!!
            when (id) {
                "phonk" -> assertEquals(0.9f, drive)
                "reggaeton" -> assertEquals(0.5f, drive)
                "jersey_club" -> assertTrue(drive >= 0.5f)
                else -> assertEquals(id, 0.7f, drive)
            }
            // The pack's 808 (a pure sine) second, the engine's first.
            val packId = style.melody.packSounds.getValue(Part.Bass).substringBefore(':')
            val pack = PackManifest(packId, packId, sounds = listOf(PackSound("808_01.flac", "808", "808 1")))
            val offered = StarterWizard.soundsFor(Part.Bass, style, listOf(pack))
            assertTrue(id, StudioDrums.bassOn808(style, offered[0]))
            assertNotNull(id, offered[1].sampled)
        }
        // An 808 track sends its version and drop too (a track without them plays version 0, as before).
        for (param in listOf(PadVoice.VERSION, PadVoice.PITCH_DROP, PadVoice.DROP_TIME)) assertTrue(param in PadVoice.TRACK_808_PARAMS)
        assertEquals(0f, PadVoice.track808Default(PadVoice.VERSION))
        assertEquals(24f, PadVoice.track808Default(PadVoice.PITCH_DROP))
        assertEquals(0.025f, PadVoice.track808Default(PadVoice.DROP_TIME))
        // The 808 on « Triolets »: its hits keep their place off the sixteenths (micro-timing); the
        // recipe's line plays on the grid.
        val trap = StarterWizard.style("trap")!!
        fun hits808(p: Project) = (listOf(p.drums) + p.allScenes.map { it.drums }).flatMap { it.lanes[StudioDrums.LANE_808].hits.values }
        assertTrue(hits808(songs.getValue(trap)).isNotEmpty())
        assertTrue(hits808(songs.getValue(trap)).all { it.offset == 0f })
        val triplets = hits808(song(BassPatterns.styled(trap, BassPatterns.byId("triplets")!!)))
        assertTrue(triplets.any { it.offset != 0f })
        // 1/12 of a beat late (the 2nd triplet on the 2nd sixteenth) at 140 BPM: 35.7 ms.
        assertTrue(triplets.any { kotlin.math.abs(kotlin.math.abs(it.offset) - 60000f / trap.bpm / 12f) < 1f })
        // A saved 808 without a version stays the 808 it was (the engine's version 0); the new settings are kept sane.
        val old = PadVoice(PadVoice.BASS_808, mapOf(PadVoice.DRIVE to 0.3f))
        assertNull(old.param(PadVoice.VERSION))
        val wild = PadVoice(PadVoice.BASS_808, mapOf(PadVoice.PITCH_DROP to 99f, PadVoice.DROP_TIME to 0f, PadVoice.VERSION to Float.NaN, 42 to 1f)).sanitized()
        assertEquals(36f, wild.param(PadVoice.PITCH_DROP))
        assertEquals(0.005f, wild.param(PadVoice.DROP_TIME))
        assertNull(wild.param(PadVoice.VERSION))
        assertNull(wild.param(42))
    }

    @Test
    fun theSynthsBassSettingsAreAppendedAndOffByDefault() {
        assertEquals(SynthPatch.PARAM_COUNT, SynthPatch.DEFAULTS.size)
        assertEquals(59, SynthPatch.PARAM_COUNT)  // 48 after them, then the leads chantier's lots B to D
        // A patch saved before them (40 values) plays them at their defaults: as before.
        val old = SynthPatch(SynthPatch.DEFAULTS.take(40))
        assertEquals(SynthPatch.DEFAULTS, old.sanitized().values)
        for (i in SynthPatch.BASS_PARAMS) assertEquals(SynthPatch.DEFAULTS[i], old.get(i))
        // A frozen track's signature reads the patch as it was written before them.
        assertEquals(old.toString(), SynthPatch.beforeBassParams(old.sanitized()).toString())
        val pitched = SynthPatch().with(SynthPatch.PITCH_ENV, 0.5f)
        // (Its leads settings, at their defaults, are left out as well: a patch written before them.)
        assertEquals(SynthPatch(pitched.values.take(SynthPatch.KEY_TRACK)), SynthPatch.beforeBassParams(pitched))
        assertEquals(0.5f, SynthPatch.pitchEnvValue(24f))
        assertEquals(0.5f, SynthPatch.pitchDecayValue(22.36f), 1e-3f)
    }

    @Test
    fun theNewBassSoundsAndWhereTheyPlay() {
        for (name in listOf("Basse roulante", "Basse pluck", "808 synthé", "Donk", "Sub chaud")) {
            val s = SoundLibrary.SYNTH.single { it.name == name }
            assertEquals(name, "Basses", s.category)
            assertEquals(name, 1f, s.sound.get(SynthPatch.PHASE_RESET))
            assertEquals(name, name, SoundLibrary.synthNameOf(s.sound))
        }
        assertTrue(SoundLibrary.SYNTH.single { it.name == "808 synthé" }.sound.get(SynthPatch.PITCH_ENV) > 0f)
        assertTrue(SoundLibrary.SYNTH.single { it.name == "Basse roulante" }.sound.get(SynthPatch.VELOCITY_CUTOFF) > 0f)
        // Every library bass with a sub never plays it under 30 Hz; a sound chosen before the floor is still recognised.
        for (s in SoundLibrary.SYNTH.filter { it.category == "Basses" && it.sound.get(SynthPatch.SUB) > 0f }) {
            assertEquals(s.name, 1f, s.sound.get(SynthPatch.SUB_FLOOR))
            assertEquals(s.name, s.name, SoundLibrary.synthNameOf(s.sound.with(SynthPatch.SUB_FLOOR, 0f)))
        }
        // The per-style mapping (docs/hits/basses-qui-tapent.md § 9).
        fun bass(id: String) = StarterWizard.style(id)!!.melody
        for (id in listOf("tech_house", "hard_techno", "industrial_techno", "trance")) assertEquals(id, "Basse roulante", bass(id).bass)
        // Techno and rétro techno keep « Basse analogique » (Plaits): on the bench it speaks better through a phone.
        for (id in listOf("techno", "retro_techno")) assertEquals(id, "Basse analogique", bass(id).bass)
        for (id in listOf("minimal", "afro_house")) assertEquals(id, "Basse pluck", bass(id).bass)
        for (id in listOf("acid_techno", "acid_house")) assertEquals(id, "Sub chaud", bass(id).bass)
        // Detroit keeps its octave 1 (its octave jumps): « Basse ronde »'s sub floor takes it out of the infra.
        assertEquals(1, bass("detroit").bassOctave)
        assertEquals(1f, SoundLibrary.SYNTH.single { it.name == "Basse ronde" }.sound.get(SynthPatch.SUB_FLOOR))
        assertTrue(bass("grime").subBass)
    }

    @Test
    fun theRollingBasslinesNeverTouchNorCoverTheKick() {
        assertEquals(11, BassPatterns.ALL.size)
        assertEquals(BassPatterns.ALL.size, BassPatterns.ALL.map { it.id }.toSet().size)
        val techno = StarterWizard.style("techno")!!
        val t = NoteClip.TICKS_PER_STEP
        for (grid in BassPatterns.ALL) {
            val style = BassPatterns.styled(techno, grid)
            assertEquals(grid, BassPatterns.of(style))
            val clip = StarterWizard.startingNotes(Part.Bass, style, style.melody.mood, StarterWizard.harmonyOf(style, style.melody.mood))
            assertTrue(grid.id, clip.notes.isNotEmpty())
            // B10: a note ends before the next one starts (a power chord's notes start together, alike).
            val lines = clip.notes.groupBy { it.pitch - clip.notes.filter { o -> o.start == it.start }.minOf { o -> o.pitch } }
            for (line in lines.values) {
                val sorted = line.sortedBy { it.start }
                for ((a, b) in sorted.zipWithNext()) assertTrue("${grid.id} ${a.start}", a.start + a.length <= b.start)
            }
            // Grids off the downbeat (1, 4, 5, 8): no note sounds on a kick's step (4/4).
            if (grid.id in setOf("rolling_kbbb", "trance_rolling", "arp_rolling", "velocity_roll")) {
                for (n in clip.notes) {
                    for (kick in (0 until clip.steps step 4).map { it * t }) assertFalse("${grid.id} $n", n.start <= kick && n.start + n.length > kick)
                }
            }
            // The velocities breathe: at least two per bar where the grid gives them.
            if (grid.velocities != null && grid.velocities.toSet().size > 2) {
                val bar = clip.notes.filter { it.start < 16 * t }.map { it.velocity }.toSet()
                assertTrue(grid.id, bar.size >= 2)
            }
        }
        // The styles' own grids first, then every other one; the choice reaches the song.
        assertEquals("rolling_kbbb", BassPatterns.forStyle("techno").first().id)
        assertEquals(BassPatterns.ALL.toSet(), BassPatterns.forStyle("trap").toSet())
        val grid = BassPatterns.byId("velocity_roll")!!
        val p = song(BassPatterns.styled(techno, grid))
        val bassTrack = p.instrumentTracks.first { it.name == "Basse" }
        // Its gallop: the softer sixteenths at 70 % of the loud ones (the grid's 40 falls on its rest, the kick's step).
        val played = StarterWizard.startingNotes(Part.Bass, BassPatterns.styled(techno, grid), techno.melody.mood, StarterWizard.harmonyOf(techno, techno.melody.mood))
        val velocities = played.notes.map { it.velocity }
        assertTrue(velocities.minOf { it } <= 0.75f * velocities.maxOf { it })
        // In the song's Drop: off the kick's steps, as the grid says.
        fun clips(project: Project, id: Int) = listOf(project.track(id).notes) + project.allScenes.mapNotNull { if (id == 0) it.synthNotes else it.trackNotes[id] }
        val drop = clips(p, bassTrack.id).first { it.notes.isNotEmpty() }
        assertTrue(drop.notes.none { (it.start / t) % 4 == 0 && it.start % t == 0 })
        // A power chord's fifth and octave stay over the sub: the Sub plays one note at a time.
        val power = song(BassPatterns.styled(StarterWizard.style("tech_house")!!, BassPatterns.byId("power")!!))
        val sub = power.instrumentTracks.first { it.name == StarterWizard.SUB_TRACK }
        val subClips = listOf(sub.notes) + power.allScenes.mapNotNull { it.trackNotes[sub.id] }
        assertTrue(subClips.any { it.notes.isNotEmpty() })
        for (c in subClips) assertTrue(c.notes.groupBy { it.start }.values.all { it.size == 1 })
        val bassClips = clips(power, power.instrumentTracks.first { it.name == "Basse" }.id)
        assertTrue(bassClips.any { c -> c.notes.groupBy { it.start }.values.any { it.size == 3 } })
        // Tech house's bass glides: a power chord there would slide root -> octave in 80 ms (the synth's
        // portamento) instead of sounding three notes. The assistant does not offer it, and a grid
        // chosen anyway plays without the slide (the bass and its Sub).
        val techHouse = StarterWizard.style("tech_house")!!
        assertTrue(techHouse.melody.bassGlide)
        assertTrue(BassPatterns.offered(techHouse).none { it.power })
        assertTrue(BassPatterns.offered(techno).any { it.power })
        for (name in listOf("Basse", StarterWizard.SUB_TRACK)) {
            assertEquals(name, 0f, power.instrumentTracks.first { it.name == name }.synth.get(SynthPatch.GLIDE))
        }
        assertEquals(StarterWizard.BASS_GLIDE, song(techHouse).instrumentTracks.first { it.name == "Basse" }.synth.get(SynthPatch.GLIDE))
    }
}
