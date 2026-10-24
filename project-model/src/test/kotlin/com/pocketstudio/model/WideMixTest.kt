package com.pocketstudio.model

import com.pocketstudio.model.StarterWizard.Part
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Phase 34, S4: wide and moving synths in the assistant's songs; the projects saved before unchanged. */
class WideMixTest {
    private fun song(style: StarterWizard.Style, parts: Set<Part> = Part.entries.toSet()): Project {
        val lines = style.lines.filter { !it.off }.associate { it.role to null as PadSample? }
        val chosen = parts.associateWith { StarterWizard.soundsFor(it, style).first() }
        return StarterWizard.build(style, lines, style.melody.mood, chosen)
    }

    private fun style(id: String) = StarterWizard.style(id)!!

    private fun trackOf(p: Project, part: Part): InstrumentTrack? {
        val name = { t: InstrumentTrack -> DefaultNames.french(t.name) ?: t.name }
        return when (part) {
            Part.Bass -> p.bassTrackIds().map { p.track(it) }.firstOrNull { name(it) != StarterWizard.SUB_TRACK && name(it) != StarterWizard.BASS_SWITCH_TRACK }
            Part.Pad -> p.padTrackIds().firstOrNull()?.let { p.track(it) }
            Part.Lead -> p.instrumentTracks.firstOrNull { it.id !in p.bassTrackIds() && it.id !in p.padTrackIds() && p.riserTrackId() != it.id &&
                name(it) != StarterWizard.COUNTER_TRACK }
        }
    }

    @Test
    fun projectsSavedBeforeKeepTheirSound() {
        val old = ProjectJson.decode("""{"name":"Ancien","synth":{"values":[0,0,0.3,0.5,0.5,0.2,0,0,0.55,0.3,0.4,0,0.3,0.3,0.3,0.05,0.3,0.8,0.35,0.5,0,0.3,0,0.6,0.5,0.3,0,0,0.5]}}""")
        assertEquals(Project.SOUND_V1, old.soundVersion)
        assertEquals(0f, old.mixer.monoBassHz, 0f)
        // Its synth's S4 settings at rest: the engine renders it as before, sample for sample.
        for (i in SynthPatch.S4_PARAMS) assertEquals(SynthPatch.DEFAULTS[i], old.synth.get(i), 0f)
        assertEquals(0f, old.synth.get(SynthPatch.WIDTH), 0f)
        assertEquals(0f, old.synth.get(SynthPatch.OVERSAMPLE), 0f)
        // The reverb a hall, as always, its return plain.
        assertEquals(Buses.HALL, old.mixer.buses[0][Buses.REVERB_TYPE], 0f)
        assertEquals(0f, old.mixer.buses[0][Buses.LOW_CUT], 0f)
        assertEquals(1f, old.mixer.buses[0][Buses.HIGH_CUT], 0f)
        // Nothing new written in a plain project's file.
        val json = ProjectJson.encode(Project())
        assertFalse(json.contains("soundVersion"))
        assertFalse(json.contains("monoBassHz"))
    }

    @Test
    fun everySongIsWideWithAPlateAndMonoLows() {
        for (style in StarterWizard.STYLES) {
            val p = song(style)
            assertEquals(style.id, Project.SOUND_V2, p.soundVersion)
            assertEquals(style.id, WideMix.MONO_BASS_HZ, p.mixer.monoBassHz, 0f)
            val reverb = p.mixer.buses[0]
            assertEquals(style.id, if (style.family == "Hip-hop") Buses.ROOM else Buses.PLATE, reverb[Buses.REVERB_TYPE], 0f)
            assertEquals(style.id, Buses.lowCutValue(200f), reverb[Buses.LOW_CUT], 1e-4f)
            assertTrue(style.id, reverb[Buses.HIGH_CUT] < 1f && p.mixer.buses[1][Buses.LOW_CUT] > 0f)
            // Saved and loaded: the same song.
            val back = ProjectJson.decode(ProjectJson.encode(p))
            assertEquals(style.id, p.soundVersion, back.soundVersion)
            assertEquals(style.id, p.mixer.monoBassHz, back.mixer.monoBassHz, 0f)
            assertEquals(style.id, p.mixer.buses, back.mixer.buses)
            assertEquals(style.id, p.instrumentTracks.map { it.synth.sanitized() }, back.instrumentTracks.map { it.synth.sanitized() })
            // Every curve valid and within the engine's lanes.
            assertTrue(style.id, p.automation.size <= AutomationLane.MAX_LANES)
            assertTrue(style.id, p.automation.all { it.target.isValid })
            assertEquals(style.id, p.automation.size, AutomationLane.sanitize(p.automation).size)
        }
    }

    @Test
    fun leadsAreWideWithChorusEchoAndAReverbAtEachPhrasesEnd() {
        var checked = 0
        for (style in StarterWizard.STYLES) {
            val p = song(style)
            val lead = trackOf(p, Part.Lead) ?: continue
            val ch = p.mixer.channels[InstrumentTrack.mixerChannel(lead.id)]
            if (lead.engine == 0 && lead.instrument == null) {
                assertTrue(style.id, lead.synth.get(SynthPatch.WIDTH) > 0f)
                assertEquals(style.id, 1f, lead.synth.get(SynthPatch.OVERSAMPLE), 0f)
                assertEquals(style.id, SynthPatch.cycleValue(8), lead.synth.get(SynthPatch.CYCLE_BARS), 0f)
                assertTrue(style.id, lead.synth.get(SynthPatch.CYCLE_DEPTH) > 0f)
                assertEquals(style.id, style.family in setOf("Techno", "House", "Bass music", "Autres"), lead.synth.get(SynthPatch.OCTAVE_DOWN) > 0f)
            }
            val types = ch.inserts.map { it.effect }
            assertTrue(style.id, types.any { it in setOf(EffectType.CHORUS, EffectType.FLANGER, EffectType.WIDENER) })
            assertTrue(style.id, types.any { it == EffectType.ECHO || it == EffectType.PING_PONG } || p.mixer.longFxCount(EffectType.PING_PONG) >= EffectType.longLimit(EffectType.PING_PONG))
            assertTrue(style.id, ch.sends[0] >= WideMix.LEAD_SEND)
            // The reverb send's curve: raised on the last bar of each 8-bar phrase, jumping on the bar lines.
            val lane = p.automation.firstOrNull { it.target == AutoTarget(AutoTarget.SEND, InstrumentTrack.mixerChannel(lead.id), 0) }
            assertNotNull(style.id, lane)
            lane!!
            val values = lane.songValues.map { it!! }
            val high = values.max()
            assertTrue(style.id, high >= WideMix.PHRASE_END_SEND)
            val sections = StarterWizard.songSections(p)
            for (sec in sections) {
                val end = minOf(sec.start + 8, sec.start + sec.bars) - 1
                assertEquals("${style.id} bar $end", high, values[end], 0f)
                if (sec.bars > 1) assertTrue(style.id, values[sec.start] < high || sec.bars == 1)
            }
            assertTrue(style.id, lane.songJumps.isNotEmpty())
            checked++
        }
        assertTrue(checked > 20)
    }

    @Test
    fun padsAreWideAndBreathe() {
        var checked = 0
        for (style in StarterWizard.STYLES) {
            val p = song(style)
            val pad = trackOf(p, Part.Pad) ?: continue
            if (pad.engine != 0 || pad.instrument != null) continue
            assertEquals(style.id, WideMix.PAD_WIDTH, pad.synth.get(SynthPatch.WIDTH), 0f)
            assertEquals(style.id, SynthPatch.cycleValue(8), pad.synth.get(SynthPatch.CYCLE_BARS), 0f)
            checked++
        }
        assertTrue(checked > 10)
    }

    @Test
    fun bassMusicBassesHaveAWideMidOverAMonoSub() {
        var checked = 0
        val wobbles = HashSet<String>()
        for (style in StarterWizard.STYLES.filter { it.family == "Bass music" }) {
            val p = song(style)
            val bass = trackOf(p, Part.Bass) ?: continue
            if (bass.engine != 0) continue
            checked++
            val s = bass.synth
            assertEquals(style.id, SynthPatch.monoBelowValue(250f), s.get(SynthPatch.MONO_BELOW), 1e-4f)
            assertTrue(style.id, s.get(SynthPatch.MID_LAYER) > 0f && s.get(SynthPatch.WIDTH) > 0f && s.get(SynthPatch.DRIVE) > 0f && s.get(SynthPatch.OCTAVE_DOWN) > 0f)
            // The sub stays a mono sine, a little saturated; the bass over it is cut at 80 Hz, not 120.
            p.instrumentTracks.firstOrNull { it.name == StarterWizard.SUB_TRACK }?.let { sub ->
                for (i in SynthPatch.S4_PARAMS - SynthPatch.DRIVE) assertEquals(style.id, SynthPatch.DEFAULTS[i], sub.synth.get(i), 0f)
                assertEquals(style.id, WideMix.SUB_DRIVE, sub.synth.get(SynthPatch.DRIVE), 0f)
                val filter = p.mixer.channels[InstrumentTrack.mixerChannel(bass.id)].inserts.firstOrNull { it.effect == EffectType.FILTER }
                assertEquals(style.id, WideMix.BASS_MUSIC_SUB_HIGH_PASS, filter?.params?.first() ?: WideMix.BASS_MUSIC_SUB_HIGH_PASS, 0f)
            }
            when (style.id) {
                "neurofunk" -> {
                    assertTrue(s.get(SynthPatch.FORMANT) > 0f)
                    val vowel = p.automation.firstOrNull { it.target == AutoTarget.synth(bass.id, SynthPatch.VOWEL) }
                    assertNotNull(vowel)
                    assertTrue(vowel!!.songValues.filterNotNull().toSet().size >= 2)
                }
                "riddim" -> assertEquals(SynthPatch.SHAPE_SQUARE, s.get(SynthPatch.LFO2_SHAPE), 0f)
            }
            if (style.id in setOf("dubstep", "riddim") && s.get(SynthPatch.LFO2_SYNC) > 0f) {
                // The wobble's speed: 1/8, 1/4, 1/16 for each 8-bar block of the Drop (held, not sliding).
                val lane = p.automation.first { it.target == AutoTarget.synth(bass.id, SynthPatch.LFO2_SYNC) }
                wobbles += style.id
                assertTrue(lane.hold)
                val drop = StarterWizard.songSections(p).first { it.bars >= 24 }
                val v = lane.songValues.map { it!! }
                assertEquals(SynthPatch.syncValue(SynthPatch.SYNC_EIGHTH), v[drop.start], 0f)
                assertEquals(SynthPatch.syncValue(SynthPatch.SYNC_QUARTER), v[drop.start + 8], 0f)
                assertEquals(SynthPatch.syncValue(SynthPatch.SYNC_SIXTEENTH), v[drop.start + 16], 0f)
            }
        }
        assertTrue("$checked bass music basses", checked >= 5)
        assertEquals(setOf("dubstep", "riddim"), wobbles)
    }

    @Test
    fun otherBassesStayMonoAndTight() {
        for (style in StarterWizard.STYLES.filter { it.family != "Bass music" }) {
            val p = song(style)
            val bass = trackOf(p, Part.Bass) ?: continue
            assertEquals(style.id, 0f, bass.synth.get(SynthPatch.WIDTH), 0f)
            assertEquals(style.id, 0f, bass.synth.get(SynthPatch.MID_LAYER), 0f)
        }
    }

    @Test
    fun synthAndPlaitsKnobsAreAutomationTargets() {
        val t = AutoTarget.synth(2, SynthPatch.CUTOFF)
        assertTrue(t.isValid)
        assertEquals(2, t.track)
        assertEquals(InstrumentTrack.mixerChannel(2), t.ch)
        assertFalse(AutoTarget.synth(0, SynthPatch.GAIN).isValid)  // the calibration is not a knob
        assertFalse(AutoTarget(AutoTarget.SYNTH, 3, 0, SynthPatch.CUTOFF).isValid)  // channel 3: no instrument track
        assertTrue(AutoTarget.plaits(0, 2).isValid)
        assertFalse(AutoTarget.plaits(0, 0).isValid)  // the model
        // The project's setting, read and written by a lane.
        var p = Project().withTrack(Project().track(0).copy(synth = SynthPatch().with(SynthPatch.CUTOFF, 0.3f)))
        assertEquals(0.3f, p.settingOf(AutoTarget.synth(0, SynthPatch.CUTOFF)), 1e-6f)
        p = p.withSetting(AutoTarget.synth(0, SynthPatch.WIDTH), 0.8f)
        assertEquals(0.8f, p.track(0).synth.get(SynthPatch.WIDTH), 1e-6f)
        p = p.withSetting(AutoTarget.plaits(0, 3), 0.25f)
        assertEquals(0.25f, p.track(0).plaits.morph, 1e-6f)
        // A song lane on a synth knob is saved and loaded.
        val lane = AutomationLane(AutoTarget.synth(0, SynthPatch.LFO2_SYNC), mode = AutomationLane.SONG, songValues = listOf(0.5f, 0.2f), hold = true)
        val back = ProjectJson.decode(ProjectJson.encode(p.copy(automation = listOf(lane)))).automation
        assertEquals(listOf(lane.sanitized()), back)
    }

    @Test
    fun theBassMusicVoicesAreInTheLibrary() {
        for (name in listOf("Reese DnB", "Basse riddim", "Basse neuro")) {
            val sound = SoundLibrary.SYNTH.first { it.name == name }.sound
            assertEquals(name, SoundLibrary.synthNameOf(sound))
            // The assistant's width on top of it: still the same sound.
            assertEquals(name, SoundLibrary.synthNameOf(sound.with(SynthPatch.WIDTH, 0.9f).with(SynthPatch.CYCLE_DEPTH, 0.4f)))
            assertTrue(name, sound.get(SynthPatch.MID_LAYER) > 0f)
        }
        assertEquals(SynthPatch.SHAPE_SQUARE, SoundLibrary.SYNTH.first { it.name == "Basse riddim" }.sound.get(SynthPatch.LFO2_SHAPE), 0f)
        assertTrue(SoundLibrary.SYNTH.first { it.name == "Basse neuro" }.sound.get(SynthPatch.FORMANT) > 0f)
        // The library's other sounds keep their S4 settings at rest (a user's pick sounds as before).
        assertNull(SoundLibrary.SYNTH.filter { it.name !in setOf("Reese DnB", "Basse riddim", "Basse neuro") }
            .firstOrNull { s -> SynthPatch.S4_PARAMS.any { s.sound.get(it) != SynthPatch.DEFAULTS[it] } })
    }
}
