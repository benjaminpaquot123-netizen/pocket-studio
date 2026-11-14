package com.pocketstudio.model

import com.pocketstudio.model.StarterWizard.Part
import com.pocketstudio.model.StarterWizard.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The final audit's sound lot (reports 10, 11 and 12): what the assistant's new songs do since. */
class AuditSonTest {
    private fun style(id: String) = StarterWizard.style(id)!!

    /** « Partir d'un style » with the advised choices (the song bench's). */
    private fun song(id: String): Project {
        val s = style(id)
        val lines = s.lines.filter { !it.off }.associate { it.role to null as PadSample? }
        val parts = StarterWizard.defaultParts(s).associateWith { StarterWizard.soundsFor(it, s).first() }
        return StarterWizard.build(s, lines, s.melody.mood, parts)
    }

    private fun Project.montees() = StarterWizard.songSections(this).withIndex().filter { (_, sec) -> allScenes[sec.scene].label == SongShape.MONTEE }

    // --- Report 11: per-style transitions -------------------------------------------------------

    @Test
    fun houseAndTechnoThatDoNotBuildLikeEdmTakeTheMonteeInAFilterSweep() {
        for (id in listOf("deep_house", "minimal", "detroit", "disco")) {
            val s = style(id)
            assertTrue(id, s.lightBuild)
            val p = song(id)
            // No riser, no sub drop, no silent beat.
            assertTrue(id, p.instrumentTracks.none { it.name == StarterWizard.RISER_TRACK || it.name == StarterWizard.SUB_DROP_TRACK })
            val plan = StarterWizard.planFor(s, s.lines.map { it.role }.toSet(), StarterWizard.defaultParts(s), s.melody.acid != null)
            val shape = SongShape.shape(s, plan)
            assertTrue(id, shape.scenes.none { it.silence || it.subDrop })
            // The kick out of the Montée's last bar only.
            assertTrue(id, shape.scenes.filter { it.cutBars > 0 }.all { it.cutBars == SongShape.LIGHT_CUT_BARS })
            // The master's low-pass: open, closing over each Montée, open again on the Drop's first bar.
            val slot = p.mixer.master.indexOfFirst { it.effect == EffectType.FILTER }
            assertTrue(id, slot >= 0)
            val lane = p.automation.single { it.target == AutoTarget(AutoTarget.INSERT_PARAM, Mixer.MASTER, slot, 0) }
            val curve = lane.songCurve(p.songBars)
            val runs = WizardMix.monteeRuns(p)
            assertTrue(id, runs.size >= 2)
            for ((first, end) in runs) {
                assertEquals(id, 1f, curve[first], 1e-6f)
                assertEquals(id, StarterWizard.LIGHT_SWEEP_LOW, curve[end - 1], 1e-6f)
                assertEquals(id, 1f, curve[end], 1e-6f)
            }
            assertEquals(id, 1f, curve[0], 1e-6f)
        }
        // The EDM styles keep their riser and their sub drop.
        for (id in listOf("techno", "house", "dubstep")) {
            assertFalse(id, style(id).lightBuild)
            assertTrue(id, song(id).instrumentTracks.any { it.name == StarterWizard.RISER_TRACK })
        }
    }

    @Test
    fun aTechnoIntroSitsAtItsGroovesLevelNotOverIt() {
        for (s in StarterWizard.STYLES.filter { it.family == "Techno" }) {
            assertEquals(s.id, WizardMix.grooveDb(s), WizardMix.introDb(s), 0f)
        }
        // House keeps its Intro over its Groove (its Grooves read louder than its Intro).
        val house = style("house")
        assertTrue(WizardMix.introDb(house) > WizardMix.grooveDb(house))
    }

    // --- Report 11: the 303 plays -----------------------------------------------------------------

    @Test
    fun the303OpensThroughEachMonteeAndMovesByPhraseInTheDrop() {
        for (id in listOf("acid_techno", "acid_house")) {
            val p = song(id)
            val lane = p.automation.first { it.target == AutoTarget.acid(0) }
            val curve = lane.songCurve(p.songBars)
            val sections = StarterWizard.songSections(p)
            val montees = p.montees()
            assertTrue(id, montees.size >= 2)
            for ((i, m) in montees) {
                val start = curve[m.start]
                val end = curve[m.start + m.bars - 1]
                val drop = sections[i + 1]
                assertEquals(id, 2, drop.scene)
                assertTrue("$id ${m.start}: $start -> $end", end > start)
                assertTrue("$id ${m.start}: $end over the Drop's ${curve[drop.start]}", end > curve[drop.start])
            }
            // In each Drop the filter turns back at least twice (an 8-bar gesture, not one 32-bar ramp).
            for (drop in sections.filter { it.scene == 2 && it.bars >= 16 }) {
                val d = (drop.start until drop.start + drop.bars).map { curve[it] }
                val turns = (1 until d.size - 1).count { k -> (d[k] - d[k - 1]) * (d[k + 1] - d[k]) < 0f || (d[k] < d[k - 1] && d[k + 1] > d[k]) }
                assertTrue("$id drop at ${drop.start}: $turns", turns >= 2)
            }
            assertTrue(id, curve.all { it in 0.1f..0.9f })
        }
    }

    // --- Report 11: the low end of techno, hard techno, amapiano --------------------------------

    @Test
    fun technoChordsAreCutUnder250HzAndHardTechnoDipsAt300Hz() {
        val techno = song("techno")
        val chords = InstrumentTrack.mixerChannel(techno.instrumentTracks.first { it.name == StarterWizard.trackName(Part.Pad, StarterWizard.soundsFor(Part.Pad, style("techno")).first(), style("techno")) }.id)
        assertTrue((RoleMix.lowCutHz(techno.mixer.channels[chords]) ?: 0f) >= 240f)
        val hard = style("hard_techno")
        // Its Hoover and its Supersaw dip 3 dB around 300 Hz.
        val p = song("hard_techno")
        for (part in listOf(Part.Pad, Part.Lead)) {
            val name = StarterWizard.trackName(part, StarterWizard.soundsFor(part, hard).first(), hard)
            val ch = InstrumentTrack.mixerChannel(p.instrumentTracks.first { it.name == name }.id)
            assertTrue(part.name, p.mixer.channels[ch].inserts.any { it.effect == EffectType.PARAM_EQ && it.params[1] < 0.45f && it.params[0] in 0.35f..0.45f })
        }
    }

    @Test
    fun amapianoAndAfroHouseHaveASubBetween40And60Hz() {
        for (id in listOf("amapiano", "afro_house")) {
            val p = song(id)
            val sub = p.instrumentTracks.firstOrNull { it.name == StarterWizard.SUB_TRACK }
            assertNotNull(id, sub)
            val pitches = (p.allScenes.mapNotNull { it.trackNotes[sub!!.id] } + sub!!.notes).flatMap { it.notes }.map { it.pitch }
            assertTrue(id, pitches.isNotEmpty())
            // Its root note in the 40-60 Hz band (amapiano in G: G1, 49 Hz; afro house in E: E1, 41 Hz).
            val lowest = pitches.min()
            assertTrue("$id $lowest", StudioDrums.hz(lowest) in 40.0..62.0)
        }
    }

    // --- Report 10: bass music ----------------------------------------------------------------

    @Test
    fun bassMusicDucksItsLowsUnderTheKickDeeperAndShorter() {
        for (id in listOf("dubstep", "jump_up", "neurofunk")) {
            val bus = song(id).mixer.musicBus!!
            assertTrue(id, bus.lowDepth >= WizardMix.BASS_MUSIC_LOW_DUCK)
            assertEquals(id, WizardMix.BASS_MUSIC_DUCK_RELEASE, bus.release, 0f)
        }
        // The neurofunk's Sub 3 dB under the dubstep's (its bass talks in the mids).
        fun subDb(id: String) = song(id).instrumentTracks.first { it.name == StarterWizard.SUB_TRACK }.synth.get(SynthPatch.GAIN).let { SynthPatch.gainDb(it) }
        assertEquals(-3f, subDb("neurofunk") - subDb("dubstep"), 0.01f)
    }

    @Test
    fun aBassMusicDropSpeaksOverWhatAPhonePlays() {
        // The wobble's mid layer fuller and its filter never shut as far.
        val p = song("dubstep")
        val bass = p.instrumentTracks.first { it.name == StarterWizard.trackName(Part.Bass, StarterWizard.soundsFor(Part.Bass, style("dubstep")).first(), style("dubstep")) }
        assertTrue(bass.synth.get(SynthPatch.MID_LAYER) >= 0.9f)
        val library = SoundLibrary.SYNTH.first { it.name == "Basse wobble" }.sound
        assertTrue(bass.synth.get(SynthPatch.CUTOFF) > library.get(SynthPatch.CUTOFF))
        // A Build's Montée lower (its roll and noise all over 300 Hz), and its roll not lifted on the drums' fader.
        assertEquals(WizardMix.BUILD_TOP_DB + WizardMix.BUILD_PHONE_TRIM_DB - 2f, WizardMix.monteeTopDb(style("dubstep")), 1e-4f)
        val drums = p.automation.first { it.target == AutoTarget(AutoTarget.VOLUME, 0) }
        assertTrue(drums.songValues.filterNotNull().all { it <= 0.8f + 1e-6f })
    }

    // --- Report 12: rap ---------------------------------------------------------------------------

    @Test
    fun theRapKickIsInThe808sKeyAndThe808DucksUnderIt() {
        for (id in listOf("trap", "drill", "phonk", "jersey_club", "reggaeton")) {
            val s = style(id)
            val kick = StudioDrums.kickVoice(s, Harmony(s.melody.root))
            assertNotNull(id, kick)
            assertEquals(id, StudioDrums.kickHz(s.melody.root), kick!!.param(PadVoice.TUNE)!!, 0.01f)
            val p = song(id)
            if (p.padVoices[StudioDrums.LANE_808]?.kind != PadVoice.BASS_808) continue
            val strip = p.mixer.channels[Mixer.padChannel(StudioDrums.LANE_808)]
            val pump = strip.inserts.firstOrNull { it.effect == EffectType.PUMP }
            assertNotNull(id, pump)
            // Keyed by the kick's sound (WizardMix.honest).
            assertTrue(id, pump!!.params.getOrElse(EffectType.PUMP_AUDIO_KEY) { 0f } >= 0.5f)
        }
    }

    @Test
    fun theTrapKickAlwaysHitsWithThe808() {
        val p = song("trap")
        assertEquals(PadVoice.BASS_808, p.padVoices[StudioDrums.LANE_808]?.kind)
        var checked = 0
        for (sc in p.allScenes) {
            val kicks = sc.drums.lanes[Role.Kick.lane].hits.keys
            val hits808 = sc.drums.lanes[StudioDrums.LANE_808].hits.keys
            if (kicks.isEmpty() || hits808.isEmpty()) continue
            // The drummer's hand (fills, a ghost kick) aside: every kick of the written pattern lands on an 808 attack.
            val written = kicks.filter { it % 16 in setOf(0, 7, 10) }
            assertTrue(sc.label, written.all { it in hits808 })
            checked++
        }
        assertTrue(checked > 0)
    }

    @Test
    fun theChopsLeadsWithoutTheirPackPlayAnOctaveHigherAndBoomBapsKeysAreLighter() {
        for (id in listOf("jersey_club", "tech_house", "uk_garage", "speed_garage", "deep_house")) {
            assertEquals(id, 5, style(id).melody.leadOctave)
        }
        val bb = style("boom_bap")
        assertEquals(Voicing.Rootless7, bb.melody.voicing)
        assertTrue(bb.effects.any { it.target == Part.Pad.id && it.insert == "PARAM_EQ" })
    }

    // --- Report 04: texts and the « Club » target ------------------------------------------------

    @Test
    fun clubIsNeverQuieterThanAuto() {
        for (s in StarterWizard.STYLES) for (v in listOf(Project.MIX_V1, Project.MIX_V4)) {
            assertTrue(s.id, LoudnessTargets.lufs(LoudnessTarget.CLUB, s.id, v)!! >= LoudnessTargets.lufs(LoudnessTarget.AUTO, s.id, v)!! ||
                LoudnessTargets.lufs(LoudnessTarget.CLUB, s.id, v) == LoudnessTargets.CLUB_LUFS)
        }
        assertEquals(-7.5f, LoudnessTargets.lufs(LoudnessTarget.CLUB, "jump_up", Project.MIX_V4)!!, 0f)
        assertEquals(LoudnessTargets.CLUB_LUFS, LoudnessTargets.lufs(LoudnessTarget.CLUB, null)!!, 0f)
    }

    @Test
    fun theJumpUpLeadTipNamesTheHoover() {
        val tip = style("jump_up").melody.tips.getValue(Part.Lead)
        assertTrue(tip, "hoover" in tip)
        assertFalse(tip, "wob" in tip || "deuxième basse" in tip)
        assertFalse("deuxième basse" in style("jump_up").melody.tips.getValue(Part.Bass))
    }
}
