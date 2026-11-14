package com.pocketstudio.model

import com.pocketstudio.model.StarterWizard.Part
import com.pocketstudio.model.StarterWizard.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Phase 34, S5: an honest mix and master in the assistant's songs; the projects saved before unchanged. */
class HonestMixTest {
    private fun song(style: StarterWizard.Style): Project {
        val lines = style.lines.filter { !it.off }.associate { it.role to null as PadSample? }
        val chosen = Part.entries.associateWith { StarterWizard.soundsFor(it, style).first() }
        return StarterWizard.build(style, lines, style.melody.mood, chosen)
    }

    @Test
    fun projectsSavedBeforeKeepTheirMix() {
        val old = ProjectJson.decode("""{"name":"Ancien","style":"techno","mixer":{"target":"AUTO"}}""")
        assertEquals(Project.MIX_V1, old.mixVersion)
        assertEquals(-1, old.mixer.sidechainKey)
        assertNull(old.mixer.musicBus)
        // Their target: the whole song at the phase 33 loudness of their style.
        assertEquals(LoudnessTargets.BY_STYLE["techno"], LoudnessTargets.lufs(old.mixer.target, old.style, old.mixVersion))
        assertEquals(-8.5f, LoudnessTargets.forStyle("phonk", old.mixVersion))
        // Saved again, nothing of S5 is written.
        val json = ProjectJson.encode(old)
        for (key in listOf("mixVersion", "sidechainKey", "musicBus")) assertTrue(key, key !in json)
    }

    @Test
    fun theDropTargetsOfS5() {
        val v2 = Project.MIX_V2
        for (id in listOf("deep_house", "afro_house", "amapiano", "disco")) assertEquals(id, -9f, LoudnessTargets.forStyle(id, v2))
        assertEquals(-6f, LoudnessTargets.forStyle("hard_techno", v2))
        assertEquals(-7f, LoudnessTargets.forStyle("phonk", v2))
        assertEquals(-8.5f, LoudnessTargets.forStyle("trap", v2))
        assertEquals(-8.5f, LoudnessTargets.forStyle("drill", v2))
        StarterWizard.STYLES.forEach { s -> assertTrue(s.id, LoudnessTargets.forStyle(s.id, v2) in -12f..-6f) }
    }

    @Test
    fun everySongGetsTheHonestMix() {
        for (style in StarterWizard.STYLES) {
            val p = song(style)
            assertEquals(style.id, Project.MIX_V5, p.mixVersion)
            val channels = p.mixer.channels
            // One stage on the kick's transient: no fast compressor on the drum bus nor the master (the Punch at 10 ms).
            for (c in listOf(channels[0].inserts, p.mixer.master)) {
                assertTrue(style.id, c.none { it.type == EffectType.COMPRESSOR.ordinal && it.params == WizardMix.BUS_GLUE[0].sanitized().params })
                assertTrue(style.id, c.none { it.type == EffectType.COMPRESSOR.ordinal && it.params == WizardMix.MASTER_GLUE[0].sanitized().params })
            }
            val punch = FxPresets.byId("punch")!!.inserts[0]
            for (c in channels) assertTrue(style.id, c.inserts.none { it.type == punch.type && it.params[2] == punch.params[2] && it.params[1] == punch.params[1] && it.params[0] == punch.params[0] })
            val kicks = p.allScenes.any { it.drums.lanes[Role.Kick.lane].hits.isNotEmpty() }
            if (!kicks) continue
            // The real sidechain: the kick's pad is the key; every melodic track (the sub drop apart) in the Musique bus.
            assertEquals(style.id, Mixer.padChannel(Role.Kick.lane), p.mixer.sidechainKey)
            val bus = p.mixer.musicBus!!
            assertTrue(style.id, bus.returns && bus.lowDepth >= bus.depth)
            for (t in p.instrumentTracks) {
                val ch = InstrumentTrack.mixerChannel(t.id)
                assertEquals("${style.id} ${t.name}", t.name != StarterWizard.SUB_DROP_TRACK, ch in bus.members)
                // Ducked once: no pump of its own on the kick's steps.
                if (ch in bus.members) assertTrue("${style.id} ${t.name}", channels[ch].inserts.none { it.type == EffectType.PUMP.ordinal && it.params[2] < 0.5f })
            }
            // A pad's pump (the rumble) listens to the kick's sound.
            for (ch in Mixer.FIRST_PAD until Mixer.CHANNELS) {
                for (ins in channels[ch].inserts.filter { it.type == EffectType.PUMP.ordinal && it.params[2] < 0.5f }) {
                    assertEquals(style.id, 1f, ins.params[EffectType.PUMP_AUDIO_KEY], 0f)
                }
            }
        }
    }

    @Test
    fun theBassesAreClippedBeforeTheirOtt() {
        for (id in listOf("dubstep", "jump_up", "neurofunk", "techno", "house")) {
            val p = song(StarterWizard.style(id)!!)
            val bass = p.instrumentTracks.first { it.name == "Basse" }
            val types = p.mixer.channels[InstrumentTrack.mixerChannel(bass.id)].inserts.map { it.effect }
            assertTrue(id, EffectType.CLIPPER in types)
            if (EffectType.OTT in types) assertTrue(id, types.indexOf(EffectType.CLIPPER) < types.indexOf(EffectType.OTT))
        }
        // The dynamic hip-hop keeps its bass whole.
        val lofi = song(StarterWizard.style("lofi")!!)
        assertTrue(lofi.mixer.channels.none { c -> c.inserts.any { it.effect == EffectType.CLIPPER } })
    }

    @Test
    fun eachMonteeRisesOverItsLastFourBars() {
        for (id in listOf("techno", "house", "dnb")) {
            val p = song(StarterWizard.style(id)!!)
            val runs = WizardMix.monteeRuns(p)
            assertTrue(id, runs.isNotEmpty())
            val b = p.beatsPerBar
            val drums = p.automation.single { it.target == AutoTarget(AutoTarget.VOLUME, 0) }
            val riserCh = InstrumentTrack.mixerChannel(p.instrumentTracks.first { it.name == StarterWizard.RISER_TRACK }.id)
            val riser = p.automation.single { it.target == AutoTarget(AutoTarget.VOLUME, riserCh) }
            for ((_, end) in runs) {
                val from = (end - WizardMix.RISE_BARS) * b
                // Up from its rest to its top over the last 4 bars, back at rest on the Drop's first beat.
                assertEquals(id, WizardMix.RISER_REST, riser.songValues[from]!!, 1e-6f)
                assertEquals(id, WizardMix.RISER_PEAK, riser.songValues[end * b - 2]!!, 1e-6f)
                assertEquals(id, WizardMix.RISER_REST, riser.songValues[end * b]!!, 1e-6f)
                // Final audit (report 10): a Build's roll is not lifted (on a phone the Drop has to land over it).
                val peak = if (StarterWizard.style(id)!!.form == StarterWizard.SongForm.Build) 0.8f else WizardMix.DRUMS_PEAK
                assertEquals(id, peak, drums.songValues[end * b - 2]!!, 1e-6f)
                assertEquals(id, 0.8f, drums.songValues[end * b]!!, 1e-6f)
            }
            assertTrue(id, p.automation.size <= AutomationLane.MAX_LANES)
        }
    }

    @Test
    fun theSectionsSitUnderTheDrop() {
        val p = song(StarterWizard.style("house")!!)
        val lane = p.automation.single { it.target == AutoTarget.masterVolume() }
        assertTrue(lane.songPerBeat && lane.songOnly)
        val b = p.beatsPerBar
        // The first beat of each part: the Groove and the Break under the Drop, the silent beat before each Drop kept.
        var beat = 0
        val levels = HashMap<String, MutableSet<Float>>()
        for (part in p.song) {
            val label = p.allScenes[part.scene].label.substringBefore(' ')
            levels.getOrPut(label) { HashSet() } += lane.songValues[beat]!!
            beat += p.partSteps(part) / 4
        }
        val drop = levels.getValue("Drop").max()
        assertEquals(1f, drop, 0f)
        assertTrue(levels.getValue("Groove").all { it < drop })
        assertTrue(levels.getValue("Break").all { it < levels.getValue("Groove").min() })
        assertTrue(lane.songValues.count { it == 0f } >= WizardMix.monteeRuns(p).size)
        assertTrue(b > 0)
    }

    @Test
    fun aMonteeEndsUnderTheDropItLeadsInto() {
        // S5b: Benjamin's A/B test heard Montées that ended louder than their Drop. On the master fader
        // every beat of a Montée stays at its top at most (MONTEE_TOP_DB; lower in bass music, whose
        // Montée has no kick nor bass at all: BUILD_TOP_DB; each style's trim on it, from the bench),
        // the Drop's first beat back at full level, and it climbs over its last 4 bars; the riser and
        // the drums climb by 1 dB only.
        var checked = 0
        for (style in StarterWizard.STYLES) {
            val p = song(style)
            val runs = WizardMix.monteeRuns(p)
            if (runs.isEmpty()) continue
            val topDb = WizardMix.monteeTopDb(style)
            assertTrue(style.id, topDb <= 0f && topDb >= -9f)
            val top = WizardMix.fader(topDb).let { if (topDb >= 0f) 1f else it }
            val lane = p.automation.single { it.target == AutoTarget.masterVolume() }
            val b = p.beatsPerBar
            for ((first, end) in runs) {
                for (k in first * b until end * b) assertTrue("${style.id} beat $k", lane.songValues[k]!! <= top + 1e-6f)
                // A Montée climbs to its top, reached before its silent beat.
                assertEquals(style.id, top, lane.songValues[end * b - 2]!!, 1e-6f)
                // It climbs at least 2 dB over those 4 bars (S5b: the roll, the riser and the filter do the rest).
                val floor = lane.songValues[(end - WizardMix.RISE_BARS) * b]!!
                assertTrue(style.id, floor < top)
                assertTrue(style.id, floor <= WizardMix.fader(WizardMix.monteeFloorDb(style)) + 1e-6f)  // lower after a Break
                assertTrue(style.id, WizardMix.monteeTopDb(style) - WizardMix.monteeFloorDb(style) >= 2f)
                val next = p.allScenes[p.song[partAtBar(p, end)].scene].label
                if (next.startsWith("Drop")) assertEquals("${style.id} $next", 1f, lane.songValues[end * b]!!, 0f)
            }
            checked++
        }
        assertTrue(checked >= 20)
        // About +1 dB (0.85 on the strips' law, 0.8 = 0 dB).
        assertTrue(WizardMix.RISER_PEAK <= 0.85f && WizardMix.DRUMS_PEAK <= 0.85f)
    }

    private fun partAtBar(p: Project, bar: Int): Int {
        var at = 0
        p.song.forEachIndexed { i, part ->
            val bars = p.partSteps(part) / p.stepsPerBar
            if (bar < at + bars) return i
            at += bars
        }
        return p.song.lastIndex
    }

    @Test
    fun aRapSongOpensOnItsLoop() {
        // S5b: a rap song's Intro (trap) plays its loop, the whole hook and its chords over the hats, not the
        // hook's first phrase then 4 bars of hats alone; the kick, the clap and the 808 wait for the Refrain.
        // A song's Intro keeps its preview (pop).
        val trap = song(StarterWizard.style("trap")!!)
        val lead = trap.instrumentTracks.first { it.name == "Lead" }
        val intro = trap.allScenes[0].trackNotes.getValue(lead.id).notes
        val refrain = trap.allScenes[2].trackNotes.getValue(lead.id).notes
        assertTrue(intro.isNotEmpty() && intro.size <= refrain.size)
        assertTrue("the Intro's second half plays", intro.any { it.start >= 4 * 16 * NoteClip.TICKS_PER_STEP })
        val chords = trap.instrumentTracks.first { it.name in StarterWizard.CHORD_TRACK_NAMES }
        fun chordsIn(scene: Int) = trap.allScenes[scene].let { if (chords.id == 0) it.synthNotes.notes else it.trackNotes[chords.id]?.notes.orEmpty() }
        assertTrue(chordsIn(0).isNotEmpty())
        assertTrue(chordsIn(1).isEmpty())  // the Couplet leaves room for the voice
        assertTrue(listOf(Role.Kick, Role.Clap).all { trap.allScenes[0].drums.lanes[it.lane].hits.isEmpty() })
        val pop = song(StarterWizard.style("pop")!!)
        val popLead = pop.instrumentTracks.first { it.name == "Lead" }
        val popIntro = pop.allScenes[0].trackNotes.getValue(popLead.id).notes
        assertTrue(popIntro.isNotEmpty() && popIntro.all { it.start < 4 * 16 * NoteClip.TICKS_PER_STEP })
    }

    @Test
    fun theMusicBusSurvivesASave() {
        val p = song(StarterWizard.style("house")!!)
        val back = ProjectJson.decode(ProjectJson.encode(p))
        assertEquals(p.mixer.musicBus, back.mixer.musicBus)
        assertEquals(p.mixer.sidechainKey, back.mixer.sidechainKey)
        assertEquals(Project.MIX_V5, back.mixVersion)
        // Out of range: kept sane.
        val wild = Mixer(sidechainKey = Mixer.MASTER, musicBus = MusicBus(members = setOf(-1, 2, Mixer.MASTER, 20), depth = 3f, lowDepth = Float.NaN)).sanitized()
        assertEquals(-1, wild.sidechainKey)
        assertEquals(setOf(2), wild.musicBus!!.members)
        assertEquals(1f, wild.musicBus!!.depth, 0f)
        assertEquals(0f, wild.musicBus!!.lowDepth, 0f)
    }
}
