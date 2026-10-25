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
            // The leads chantier (lot 1): version 3, Plaits' FM voices following the velocity.
            assertEquals(style.id, Project.SOUND_V3, p.soundVersion)
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
                // The leads chantier: the 8-bar filter curve is a song lane open on each phrase's
                // first bar (the engine's cycle, closed there, off), and the octave-down double only
                // in club music without a sub track, never under a pluck or a key, at 0.15 (0.3 before).
                val curve = p.automation.firstOrNull { it.target == AutoTarget.synth(lead.id, SynthPatch.CUTOFF) }
                if (curve != null) assertEquals(style.id, 0f, lead.synth.get(SynthPatch.CYCLE_DEPTH), 0f)
                else assertTrue(style.id, lead.synth.get(SynthPatch.CYCLE_DEPTH) in 0.01f..0.25f)
                val sub = p.instrumentTracks.any { it.name == StarterWizard.SUB_TRACK }
                val category = SoundLibrary.synthCategoryOf(lead.synth)
                val doubled = style.family in setOf("Techno", "House", "Bass music", "Autres") && !sub && category !in setOf("Pluck", "Claviers")
                assertEquals(style.id, if (doubled) WideMix.OCTAVE_DOWN_V3 else 0f, lead.synth.get(SynthPatch.OCTAVE_DOWN), 0f)
                // No square sub under a lead any more.
                assertEquals(style.id, 0f, lead.synth.get(SynthPatch.SUB), 0f)
            }
            val types = ch.inserts.map { it.effect }
            // A unison lead is wide by itself: no chorus folding it to mono first.
            val unison = lead.engine == 0 && lead.instrument == null && lead.synth.get(SynthPatch.UNISON) > 0f
            assertTrue(style.id, unison || types.any { it in setOf(EffectType.CHORUS, EffectType.FLANGER, EffectType.WIDENER) })
            assertTrue(style.id, types.any { it == EffectType.ECHO || it == EffectType.PING_PONG } || p.mixer.longFxCount(EffectType.PING_PONG) >= EffectType.longLimit(EffectType.PING_PONG))
            assertTrue(style.id, ch.sends[0] >= WideMix.LEAD_SEND_V3)
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
            // It breathes over 8 bars: on a song lane of its cut-off open on each phrase's first bar,
            // or (no lane left) on the engine's cycle.
            val curve = p.automation.firstOrNull { it.target == AutoTarget.synth(pad.id, SynthPatch.CUTOFF) }
            assertTrue(style.id, curve != null || pad.synth.get(SynthPatch.CYCLE_BARS) == SynthPatch.cycleValue(8))
            assertEquals(style.id, 0f, pad.synth.get(SynthPatch.SUB), 0f)
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
        // The library's other sounds keep their S4 settings at rest (a user's pick sounds as before); the
        // basses that hit (« des basses qui tapent », new) are driven by their own design.
        val hitting = setOf("Basse roulante", "Basse pluck", "808 synthé", "Donk", "Sub chaud")
        assertNull(SoundLibrary.SYNTH.filter { it.name !in setOf("Reese DnB", "Basse riddim", "Basse neuro") + hitting }
            .firstOrNull { s -> SynthPatch.S4_PARAMS.any { s.sound.get(it) != SynthPatch.DEFAULTS[it] } })
    }

    /** The leads chantier, lot 1: the filter's 8-bar curve opens each phrase (the hook comes in bright). */
    @Test
    fun theFilterCurveIsOpenOnEachPhrasesFirstBar() {
        var checked = 0
        for (style in StarterWizard.STYLES) {
            val p = song(style)
            val sections = StarterWizard.songSections(p)
            for (lane in p.automation.filter { it.target.kind == AutoTarget.SYNTH && it.target.param == SynthPatch.CUTOFF }) {
                val values = lane.songValues.map { it!! }
                for (sec in sections.filter { it.bars >= 8 }) {
                    val phrase = values.subList(sec.start, sec.start + 8)
                    // Its most open bar is the first, its darkest the fifth, at most about an octave apart.
                    assertEquals("${style.id} ${sec.start}", phrase.max(), phrase[0], 1e-6f)
                    assertEquals("${style.id} ${sec.start}", phrase.min(), phrase[4], 1e-6f)
                    assertTrue(style.id, ProVoices.cutoffUnits(1.05f) >= phrase[0] - phrase[4])
                }
                checked++
            }
        }
        assertTrue(checked > 30)
    }

    /** The leads chantier: no sub under the leads, pads, plucks and keys; the SVF under every unison. */
    @Test
    fun theLibraryVoicesAreSetAsProducersSetThem() {
        for (s in SoundLibrary.SYNTH.filter { it.category in setOf("Leads", "Nappes", "Pluck", "Claviers") }) {
            assertEquals(s.name, 0f, s.sound.get(SynthPatch.SUB), 0f)
            if (s.sound.get(SynthPatch.UNISON) > 0f) assertEquals(s.name, 1, (s.sound.get(SynthPatch.FILTER_TYPE) * 3.999f).toInt())
            // Released over 100 ms at least (envelope time 0.001 x 5000^v s): no note cut dead.
            assertTrue(s.name, s.sound.get(SynthPatch.A_RELEASE) >= 0.54f)
            // A vibrato is at 4 Hz or more (0.05 x 400^v Hz), within +-15 cents.
            if (s.sound.get(SynthPatch.VIBRATO) > 0f) {
                assertTrue(s.name, 0.05 * Math.pow(400.0, s.sound.get(SynthPatch.LFO1_RATE).toDouble()) >= 4.0)
                assertTrue(s.name, s.sound.get(SynthPatch.VIBRATO) * 200f <= 15f)
            }
        }
        // The basses keep their sub; « Piano FM » is the DX7 « E.PIANO 1 » (it played « BASS 1 »).
        assertTrue(SoundLibrary.SYNTH.first { it.name == "Basse ronde" }.sound.get(SynthPatch.SUB) > 0f)
        val piano = SoundLibrary.PLAITS.first { it.name == "Piano FM" }.sound
        assertEquals(3, piano.model)
        assertEquals(0, (piano.harmonics * 32f * 1.02f).toInt())
        // Liquid: its chords on the DX « WINTRHODES » over a pad, its counter-melody on « BELL C ».
        val liquid = song(style("liquid_dnb"))
        val chords = trackOf(liquid, Part.Pad)!!
        assertEquals(1, chords.engine)
        assertEquals(SoundLibrary.PLAITS.first { it.name == "Rhodes FM" }.sound.harmonics, chords.plaits.harmonics, 0f)
        assertNotNull(liquid.instrumentTracks.firstOrNull { it.name == StarterWizard.PAD_LAYER_TRACK && it.id != chords.id })
        assertEquals("Cloche FM", style("liquid_dnb").melody.counter)
        // A project saved before the chantier still names its sounds (its own patches, the old library's).
        for (old in SoundLibrary.SYNTH_V2) assertEquals(old.name, SoundLibrary.synthNameOf(old.sound))
    }

    /** The leads chantier: a sound's cut-off follows its part's octave (key tracking, done once by the recipe). */
    @Test
    fun theCutOffFollowsThePartsNotes() {
        val pluck = SoundLibrary.SYNTH.first { it.name == "Pluck" }.sound
        val c4 = ProVoices.keyTracked(pluck, 60, 0.8f)
        val c6 = ProVoices.keyTracked(pluck, 84, 0.8f)
        assertEquals(pluck.get(SynthPatch.CUTOFF), c4.get(SynthPatch.CUTOFF), 0f)
        // Two octaves up: 1.6 octave more open.
        assertEquals(ProVoices.cutoffUnits(1.6f), c6.get(SynthPatch.CUTOFF) - pluck.get(SynthPatch.CUTOFF), 1e-4f)
        assertEquals("Pluck", SoundLibrary.synthNameOf(c6))
    }

    /**
     * The leads chantier: a song the assistant made before it (sound version 2, the old library's
     * Supersaw with its square sub, its octave-down double and its 8-bar cycle, « Piano FM » on the
     * DX « BASS 1 ») opens as it was saved, every setting the engine gets unchanged; the engine
     * renders versions 1 and 2 sample for sample as before (host test leads_sound_version_fm_velocity).
     */
    @Test
    fun aSongSavedBeforeTheLeadsChantierKeepsItsSound() {
        val supersaw = WideMix.lead(SoundLibrary.SYNTH_V2.first { it.name == "Supersaw" }.sound, style("dnb"))
        val piano = PlaitsPatch(model = 2, harmonics = 0.2f, timbre = 0.5f, morph = 0.5f, decay = 0.5f, gain = SynthPatch.gainValue(10.5f))
        val saved = Project(soundVersion = Project.SOUND_V2).let { p ->
            val added = p.addTrack("Lead")!!
            added.withTrack(added.track(0).copy(engine = 1, plaits = piano)).withTrack(added.tracks.last().copy(synth = supersaw))
        }
        val opened = ProjectJson.decode(ProjectJson.encode(saved))
        assertEquals(Project.SOUND_V2, opened.soundVersion)
        assertEquals(piano, opened.track(0).plaits)
        assertEquals(supersaw.sanitized().values, opened.tracks.last().synth.sanitized().values)
        assertEquals(0.1f, opened.tracks.last().synth.get(SynthPatch.SUB), 0f)
        assertEquals(0.3f, opened.tracks.last().synth.get(SynthPatch.OCTAVE_DOWN), 0f)
        // Still named as chosen, though the library's « Supersaw » is set otherwise now.
        assertEquals("Supersaw", SoundLibrary.synthNameOf(opened.tracks.last().synth))
        assertTrue(SoundLibrary.SYNTH.first { it.name == "Supersaw" }.sound != SoundLibrary.SYNTH_V2.first { it.name == "Supersaw" }.sound)
        // A version this code does not know is read as the newest it knows, never beyond.
        assertEquals(Project.SOUND_V3, Project(soundVersion = 9).sanitized().soundVersion)
    }
}
