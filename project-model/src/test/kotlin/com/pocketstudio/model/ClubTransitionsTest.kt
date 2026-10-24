package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.pocketstudio.model.SongBars.isDrumBar


/** The club transitions of the styles: the riser and its impact, the rumble, the ride, the toms, the filter sweeps. */
class ClubTransitionsTest {
    private fun style(id: String) = StarterWizard.style(id)!!

    private fun full(s: StarterWizard.Style, parts: Set<StarterWizard.Part> = StarterWizard.Part.entries.toSet(), offToo: Boolean = true): Project {
        val lines = s.lines.filter { offToo || !it.off }.associate { it.role to null as PadSample? }
        return StarterWizard.build(s, lines, s.melody.mood, parts.associateWith { StarterWizard.soundsFor(it, s).first() })
    }

    private fun Project.notes(scene: Int, track: Int) = allScenes[scene].let { if (track == 0) it.synthNotes else it.trackNotes.getValue(track) }.notes

    /** The song's sections: (scene, first bar, bars). */
    private fun Project.sections() = StarterWizard.songSections(this).map { Triple(it.scene, it.start, it.bars) }

    @Test
    fun theRiserRisesBeforeEachDropAndHitsOnItsFirstBar() {
        for (id in listOf("techno", "tech_house", "dnb", "dubstep", "acidcore")) {
            val p = full(style(id))
            val riser = p.instrumentTracks.single { it.name == StarterWizard.RISER_TRACK }
            val ch = InstrumentTrack.mixerChannel(riser.id)
            // White noise, through a low-pass shut while the song does not play (a section in a loop stays quiet).
            assertEquals(id, StarterWizard.RISER_SOUND, riser.synth)
            val filter = p.mixer.channels[ch].inserts[0]
            assertEquals(id, EffectType.FILTER, filter.effect)
            assertEquals(id, 0f, filter.params[0], 0f)
            // Its notes: none in the Intro, held all along before a Drop, a short hit on the Drop's first step.
            assertTrue(id, p.notes(0, riser.id).isEmpty())
            val held = p.notes(1, riser.id).single()
            assertEquals(id, p.allScenes[1].stepsFor(16) * NoteClip.TICKS_PER_STEP, held.length)
            val hit = p.notes(2, riser.id).single()
            assertEquals(id, 0, hit.start)
            assertTrue(id, hit.length <= 2 * NoteClip.TICKS_PER_STEP)
            // Its filter curve, a point per beat: shut, opening all through the Montée before each Drop (held under the
            // Drop), shut on the silent beat before it, open on the Drop's first beat, shut again by the next bar.
            val lane = p.automation.single { it.target == AutoTarget(AutoTarget.INSERT_PARAM, ch, 0, 0) }
            assertEquals(AutomationLane.SONG, lane.mode)
            assertTrue(id, lane.songPerBeat)
            val curve = lane.songCurve(p.songBars * 4)
            val sections = p.sections()
            var drops = 0
            sections.zipWithNext().forEach { (sec, next) ->
                if (next.first != 2 || sec.first == 2) return@forEach
                drops++
                val end = (sec.second + sec.third) * 4
                assertEquals("$id $sec", StarterWizard.RISER_TOP, curve[end - 2], 1e-6f)
                assertEquals("$id $sec", 0f, curve[end - 1], 0f)
                assertEquals("$id $next", 1f, curve[next.second * 4], 0f)
                assertEquals("$id $next", 0f, curve[next.second * 4 + 4], 0f)
                // Before the Montée (8 bars) nothing; it rises all along it.
                assertEquals("$id $sec", 8, sec.third)
                assertEquals("$id $sec", StarterWizard.RISER_LOW, curve[sec.second * 4], 1e-6f)
                assertEquals("$id $sec", 0f, curve[sec.second * 4 - 1], 0f)
                assertEquals("$id", listOf("Montée"), listOf(p.allScenes[sec.first].label))
            }
            assertEquals(id, sections.count { it.first == 2 && sections.getOrNull(sections.indexOf(it) - 1)?.first != 2 }, drops)
            assertTrue(id, drops >= 2)
            // In the middle of a Drop the riser is silent; it falls at the Break's start (a downlifter).
            val drop = sections.first { it.first == 2 }
            assertEquals(id, 0f, curve[(drop.second + drop.third / 2) * 4], 0f)
            sections.firstOrNull { it.first == 3 }?.let { brk ->
                assertEquals(id, StarterWizard.DOWNLIFTER, curve[brk.second * 4], 1e-6f)
                assertEquals(id, 0f, curve[brk.second * 4 + 8], 0f)
            }
            // The plan says so on the sections the riser plays in.
            val plan = StarterWizard.planFor(style(id), style(id).lines.map { it.role }.toSet(), StarterWizard.Part.entries.toSet(), style(id).melody.acid != null)
            assertTrue(id, StarterWizard.ScenePlan.RISER_TITLE in plan.contents(1))
            assertTrue(id, StarterWizard.ScenePlan.IMPACT_TITLE in plan.contents(2))
            assertFalse(id, StarterWizard.ScenePlan.RISER_TITLE in plan.contents(0))
        }
        // Without a part the riser takes the first track; styles without a riser (amapiano, a song without an impact) get none.
        val drums = StarterWizard.build(style("techno"), style("techno").lines.associate { it.role to null as PadSample? })
        assertEquals(listOf(StarterWizard.RISER_TRACK), drums.instrumentTracks.map { it.name })
        for (id in listOf("amapiano", "boom_bap", "lofi")) {
            assertTrue(id, full(style(id)).instrumentTracks.none { it.name == StarterWizard.RISER_TRACK })
        }
    }

    @Test
    fun theRumbleIsTheKicksTailThroughItsPreset() {
        val preset = FxPresets.byId("rumble")!!
        assertEquals(
            listOf(EffectType.REVERB, EffectType.DISTORTION, EffectType.FILTER, EffectType.PUMP, EffectType.WIDENER),
            preset.inserts.map { it.effect },
        )
        // Only the reverb's tail, kept under 150 Hz, ducked by the kick, in mono.
        assertEquals(1f, preset.inserts[0].params[3], 0f)
        assertTrue(preset.inserts[2].params[0] < 0.25f && preset.inserts[2].params[2] < 1f / 3)
        assertTrue(preset.inserts[3].params[2] < 0.5f)
        assertEquals(0f, preset.inserts[4].params[0], 0f)
        for (id in listOf("techno", "industrial_techno", "hard_techno")) {
            val s = style(id)
            val line = s.lines.single { it.role == StarterWizard.Role.Rumble }
            val kick = s.lines.single { it.role == StarterWizard.Role.Kick }
            assertEquals(id, kick.pattern, line.pattern)
            val p = full(s)
            // S3: the rumble pad plays the engine's rumble voice (the kick's tail already, tuned with the kick):
            // its strip keeps only the duck under the kick and the mono, no reverb. S5: that duck listens to
            // the kick's sound (the mixer's sidechain key), not to the kick lane's steps.
            val voice = p.padVoices.getValue(StarterWizard.Role.Rumble.lane)
            assertEquals(id, PadVoice.RUMBLE, voice.kind)
            assertEquals(id, p.padVoices[StarterWizard.Role.Kick.lane]?.param(PadVoice.TUNE), voice.param(PadVoice.TUNE))
            val strip = p.mixer.channels[Mixer.padChannel(StarterWizard.Role.Rumble.lane)].inserts
            val keyed = StudioDrums.RUMBLE_STRIP.map {
                if (it.effect == EffectType.PUMP && it.params[2] < 0.5f) it.copy(params = it.params.toMutableList().also { v -> v[EffectType.PUMP_AUDIO_KEY] = 1f }) else it
            }
            assertEquals(id, keyed, strip.take(StudioDrums.RUMBLE_STRIP.size))
            assertTrue(id, strip.none { it.type == EffectType.REVERB.ordinal })
            assertEquals(id, Mixer.padChannel(StarterWizard.Role.Kick.lane), p.mixer.sidechainKey)
            // It plays wherever the kick plays; in the Break only in industrial techno (its breaks drop the kick, keep the rumble).
            // (The song's one-bar fills and crashes aside: a fill stops the kick for its last beats.)
            val scenes = p.allScenes.filter { !it.isDrumBar() }
            val kickIn = scenes.mapIndexed { i, sc -> sc.drums.lanes[StarterWizard.Role.Kick.lane].hits.isNotEmpty() || (id == "industrial_techno" && i == 3) }
            assertEquals(id, kickIn, scenes.map { it.drums.lanes[StarterWizard.Role.Rumble.lane].hits.isNotEmpty() })
        }
        // Hard and industrial techno: the rumble is their low end, the bass waits to be ticked; techno keeps its
        // rolling bass with the rumble under it (docs/hits/techno.md: « rumble coché par défaut en plus de la basse »).
        for (id in listOf("industrial_techno", "hard_techno")) assertFalse(id, StarterWizard.Part.Bass in StarterWizard.defaultParts(style(id)))
        assertTrue(StarterWizard.Part.Bass in StarterWizard.defaultParts(style("techno")))
        assertFalse(style("techno").lines.single { it.role == StarterWizard.Role.Rumble }.off)
        assertTrue(style("hard_techno").lines.none { it.off })
    }

    @Test
    fun newDrumLinesHaveTheirOwnPadsAndPlaces() {
        // Every role has its own pad.
        assertEquals(StarterWizard.Role.entries.size, StarterWizard.Role.entries.map { it.lane }.toSet().size)
        // The ride comes with the Drop (techno, tech house); the second percussion and the toms start in the DJ's intro.
        for (id in listOf("techno", "tech_house", "hard_techno", "detroit")) {
            val p = full(style(id))
            assertEquals(id, listOf(false, false, true), p.allScenes.take(3).map { it.drums.lanes[StarterWizard.Role.Ride.lane].hits.isNotEmpty() })
        }
        val th = full(style("tech_house"))
        assertTrue(th.allScenes.take(4).all { it.drums.lanes[StarterWizard.Role.Perc2.lane].hits.isNotEmpty() })
        val deep = full(style("deep_house"))
        assertTrue(deep.allScenes[0].drums.lanes[StarterWizard.Role.Tom.lane].hits.isNotEmpty())
        // Afro house: three percussion lines, kept in its Break.
        val afro = style("afro_house")
        assertTrue(listOf(StarterWizard.Role.Perc, StarterWizard.Role.Perc2, StarterWizard.Role.Tom).all { r -> afro.lines.any { it.role == r } })
        assertTrue(full(afro).allScenes[3].drums.lanes[StarterWizard.Role.Perc2.lane].hits.isNotEmpty())
        // A pack sound for the rumble is a kick, for the second percussion a percussion; no pack has a ride.
        assertEquals("kick", StarterWizard.Role.Rumble.packKind)
        assertEquals("perc", StarterWizard.Role.Perc2.packKind)
        assertEquals("tom", StarterWizard.Role.Tom.packKind)
    }

    @Test
    fun filtersSweepAlongTheSong() {
        // Techno: the stab's low-pass opens through each Drop and from dark to open in the Break.
        val techno = full(style("techno"))
        val pad = techno.instrumentTracks.first { it.name in StarterWizard.CHORD_TRACK_NAMES }
        val ch = InstrumentTrack.mixerChannel(pad.id)
        val inserts = techno.mixer.channels[ch].inserts
        val slot = inserts.indexOfLast { it.effect == EffectType.FILTER }
        val filter = inserts[slot]
        // Heard as in the Drop when a section plays in a loop.
        assertEquals(0.55f, filter.params[0], 1e-6f)
        val lane = techno.automation.single { it.target == AutoTarget(AutoTarget.INSERT_PARAM, ch, slot, 0) }
        val curve = lane.songCurve(techno.songBars)
        val sections = techno.sections()
        val drop = sections.first { it.first == 2 }
        assertEquals(0.55f, curve[drop.second], 1e-6f)
        assertEquals(0.9f, curve[drop.second + drop.third - 1], 1e-6f)
        val brk = sections.first { it.first == 3 }
        assertEquals(0.3f, curve[brk.second], 1e-6f)
        assertEquals(0.75f, curve[brk.second + brk.third - 1], 1e-6f)
        assertTrue(StarterWizard.hasSweeps(techno))
        // Bass music: the pad opens all along the Montée (it is shut at its start, open at the Drop).
        val dnb = full(style("dnb"))
        val dnbPad = InstrumentTrack.mixerChannel(dnb.instrumentTracks.first { it.name in StarterWizard.CHORD_TRACK_NAMES }.id)
        val dnbLane = dnb.automation.single { it.target.kind == AutoTarget.INSERT_PARAM && it.target.ch == dnbPad }
        val montee = dnb.sections().first { it.first == 1 }
        val dnbCurve = dnbLane.songCurve(dnb.songBars)
        assertEquals(0.25f, dnbCurve[montee.second], 1e-6f)
        assertEquals(1f, dnbCurve[montee.second + montee.third - 1], 1e-6f)
        // A style without a sweep of its own sweeps its chords in the Break only (shut at its start, open at its end);
        // without the part it sweeps, no filter either.
        val house = full(style("house"))
        val housePad = InstrumentTrack.mixerChannel(house.instrumentTracks.first { it.name in StarterWizard.CHORD_TRACK_NAMES }.id)
        val houseCurve = house.automation.single { it.target.kind == AutoTarget.INSERT_PARAM && it.target.ch == housePad }.songCurve(house.songBars)
        val houseBreak = house.sections().first { it.first == 3 }
        assertEquals(0.35f, houseCurve[houseBreak.second], 1e-6f)
        assertEquals(0.85f, houseCurve[houseBreak.second + houseBreak.third - 1], 1e-6f)
        // The bass of club music opens through the Groove (dark at its start) and is open in the Drop; without the chords,
        // that is the only filter that moves.
        for ((id, parts) in listOf("house" to setOf(StarterWizard.Part.Bass, StarterWizard.Part.Lead), "techno" to setOf(StarterWizard.Part.Bass))) {
            val p = full(style(id), parts = parts)
            val bassCh = InstrumentTrack.mixerChannel(p.instrumentTracks.first { it.name == "Basse" }.id)
            val lanes = p.automation.filter { it.mode == AutomationLane.SONG && it.target.kind == AutoTarget.INSERT_PARAM && !it.songOnly }
            assertEquals(id, listOf(bassCh), lanes.map { it.target.ch })
            val curve = lanes.single().songCurve(p.songBars)
            val groove = p.sections().first { it.first == 1 }
            val drop = p.sections().first { it.first == 2 }
            assertEquals(id, WizardMix.GROOVE_BASS_FROM, curve[groove.second], 1e-6f)
            assertEquals(id, WizardMix.GROOVE_BASS_TO, curve[groove.second + groove.third - 1], 1e-6f)
            assertEquals(id, 1f, curve[drop.second], 1e-6f)
        }
        // The lanes never go past the engine's limit, the 303's knobs first (acid techno: moves, riser).
        val acid = full(style("acid_techno"))
        assertTrue(acid.automation.size <= AutomationLane.MAX_LANES)
        assertNotNull(acid.automation.firstOrNull { it.target.kind == AutoTarget.ACID })
        assertNull(full(style("amapiano"), parts = setOf(StarterWizard.Part.Bass)).automation.firstOrNull { it.mode == AutomationLane.SONG && it.target.kind == AutoTarget.INSERT_PARAM })
    }
}
