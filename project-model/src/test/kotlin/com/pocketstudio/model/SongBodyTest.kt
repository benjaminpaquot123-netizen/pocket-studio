package com.pocketstudio.model

import com.pocketstudio.model.StarterWizard.Part
import com.pocketstudio.model.StarterWizard.Role
import com.pocketstudio.model.StarterWizard.SongForm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.pocketstudio.model.SongBars.isDrumBar

/**
 * The body of the assistant's songs (audit « corps », docs/CORPS.md): what can be read in the
 * project it makes with each style's default choices (the built-in kit, the parts a beginner gets):
 * a mix to start from, something that glues it, a beat that moves every 4 bars, a Drop (or a
 * Refrain) fuller than what comes before it, a Break that still plays.
 */
class SongBodyTest {
    private fun song(style: StarterWizard.Style): Project {
        val lines = style.lines.filter { !it.off }.associate { it.role to null as PadSample? }
        val parts = StarterWizard.defaultParts(style).associateWith { StarterWizard.soundsFor(it, style).first() }
        return StarterWizard.build(style, lines, style.melody.mood, parts)
    }

    /** What scene [index] plays: its drum lines, the tracks with notes, the 303. */
    private fun Project.layers(index: Int): Int {
        val sc = allScenes[index]
        val tracks = instrumentTracks.count { t -> t.name != StarterWizard.RISER_TRACK && (if (t.id == 0) sc.synthNotes else sc.trackNotes[t.id] ?: NoteClip()).notes.isNotEmpty() }
        // A pad heard only in the phrase's fill (the kit's toms) is not a layer of the section.
        val end = sc.drums.steps - 4
        return sc.drums.lanes.count { l -> l.hits.keys.any { it < end } } + tracks + (if (sc.acid.steps.any { it.gate }) 1 else 0)
    }

    @Test
    fun everyLibrarySoundIsCalibrated() {
        // No typo: each calibrated name is a sound of the library.
        assertTrue(SoundLibrary.SYNTH_GAIN_DB.keys.all { n -> SoundLibrary.SYNTH.any { it.name == n } })
        assertTrue(SoundLibrary.PLAITS_GAIN_DB.keys.all { n -> SoundLibrary.PLAITS.any { it.name == n } })
        // Every playable sound has its calibration (the subs, the Plaits FM bass and the effects stay at 0 dB).
        val plain = setOf("Basse sub", "Sub pur", "Basse FM")
        for (s in SoundLibrary.SYNTH) if (s.category != "Effets" && s.name !in plain) {
            // The leads chantier's sounds keep their loudness through their own change ([ProVoices.GAIN_DB],
            // and [ProVoices.TOUCH_GAIN_DB] for the JP-8000 unison of its lots B to D).
            val db = SoundLibrary.SYNTH_GAIN_DB.getValue(s.name) + (ProVoices.GAIN_DB[s.name] ?: 0f) + (ProVoices.TOUCH_GAIN_DB[s.name] ?: 0f)
            assertEquals(s.name, SynthPatch.gainValue(db), s.sound.get(SynthPatch.GAIN), 1e-5f)
        }
        for (s in SoundLibrary.PLAITS) if (s.category !in setOf("Effets", "Batterie") && s.name !in plain) {
            assertEquals(s.name, SynthPatch.gainValue(SoundLibrary.PLAITS_GAIN_DB.getValue(s.name)), s.sound.gain, 1e-6f)
        }
        // The pluck (20 dB under a pad before) is brought up, the pads down; nothing beyond the knob's range.
        assertTrue(SoundLibrary.SYNTH_GAIN_DB.getValue("Pluck") > 0f && SoundLibrary.SYNTH_GAIN_DB.getValue("Nappe") < 0f)
        assertTrue((SoundLibrary.SYNTH_GAIN_DB.values + SoundLibrary.PLAITS_GAIN_DB.values).all { it in -SynthPatch.GAIN_RANGE_DB..SynthPatch.GAIN_RANGE_DB })
    }

    @Test
    fun oldSoundsAndProjectsKeepTheirLevel() {
        // A patch saved before has no calibration: it reads 0 dB.
        assertEquals(0.5f, SynthPatch(SynthPatch.DEFAULTS.take(28)).sanitized().get(SynthPatch.GAIN))
        assertEquals(0.5f, ProjectJson.decode("""{"plaits":{"model":3}}""").plaits.gain)
        assertEquals(0f, SynthPatch.gainDb(0.5f))
        assertEquals(6f, SynthPatch.gainDb(SynthPatch.gainValue(6f)), 1e-4f)
        // The calibration is not a knob: not on a MIDI controller, kept when a sound is edited.
        assertTrue(MidiMapping.sanitize(listOf(MidiMapping(20, AutoTarget(MidiMapping.SYNTH, param = SynthPatch.GAIN)))).isEmpty())
        val pluck = SoundLibrary.SYNTH.first { it.name == "Pluck" }.sound
        assertEquals(pluck.get(SynthPatch.GAIN), pluck.with(SynthPatch.CUTOFF, 0.9f).get(SynthPatch.GAIN))
    }

    @Test
    fun theDefaultMixPutsEachLineAtItsPlace() {
        for (style in StarterWizard.STYLES) {
            val p = song(style)
            val roles = style.lines.filter { !it.off }.map { it.role }.toSet()
            // Kit v2's voices are near their place already: the kick and the hats need hardly any
            // fader (kit v1's kick came down 6 dB, its hats went to the top); the hats go to the side.
            if (Role.Kick in roles && style.lines.first { it.role == Role.Kick }.voice == null) {
                assertEquals(style.id, 0.8f, p.mixer.channels[Mixer.padChannel(Role.Kick.lane)].volume, 0.03f)
            }
            if (Role.Hat in roles) assertTrue(style.id, p.drums.lanes[Role.Hat.lane].pan < 0f)
            if (Role.Hat in roles && style.lines.first { it.role == Role.Hat }.voice == null) assertEquals(style.id, 0.8f, p.mixer.channels[Mixer.padChannel(Role.Hat.lane)].volume, 0.03f)
            // Something glues the master (a compressor or a saturation, the style's or the assistant's), under the limiter.
            assertTrue(style.id, p.mixer.master.any { it.type == EffectType.COMPRESSOR.ordinal || it.type == EffectType.SATURATION.ordinal })
            assertEquals(style.id, WizardMix.MASTER_DB, p.mixer.masterDb)
            // The tracks' faders stay at 0 dB for the user: the bass sits up front through its sound's level, held chords behind it.
            assertTrue(style.id, p.instrumentTracks.all { p.mixer.channels[InstrumentTrack.mixerChannel(it.id)].volume == 0.8f })
            val bass = p.instrumentTracks.firstOrNull { it.name == "Basse" }
            val bassSound = StarterWizard.soundsFor(Part.Bass, style).first()
            if (bass != null && bass.engine == 0 && bassSound.sampled == null && bassSound.plaits == null) {
                assertTrue(style.id, SynthPatch.gainDb(bass.synth.get(SynthPatch.GAIN)) > SynthPatch.gainDb(bassSound.synth.get(SynthPatch.GAIN)))
            }
            val pad = p.instrumentTracks.firstOrNull { it.name == "Nappe" }
            val padSound = StarterWizard.soundsFor(Part.Pad, style).first()
            if (pad != null && pad.engine == 0 && padSound.sampled == null && padSound.plaits == null) {
                assertTrue(style.id, SynthPatch.gainDb(pad.synth.get(SynthPatch.GAIN)) < SynthPatch.gainDb(padSound.synth.get(SynthPatch.GAIN)))
            }
            // House and techno pump their bass on the kick: since S5 once, in the Musique bus keyed by the kick's sound.
            if (bass != null && style.family in setOf("House", "Techno") && style.id !in setOf("gabber", "acidcore", "industrial_techno", "hard_techno")) {
                val bus = p.mixer.musicBus
                assertTrue(style.id, bus != null && InstrumentTrack.mixerChannel(bass.id) in bus.members && bus.depth >= 0.2f && bus.lowDepth >= bus.depth)
                assertTrue(style.id, p.mixer.channels[InstrumentTrack.mixerChannel(bass.id)].inserts.none { it.type == EffectType.PUMP.ordinal })
                assertEquals(style.id, Mixer.padChannel(Role.Kick.lane), p.mixer.sidechainKey)
            }
        }
    }

    @Test
    fun theBeatMovesEveryFourBars() {
        for (style in StarterWizard.STYLES) {
            val p = song(style)
            if (StarterWizard.defaultParts(style).isEmpty()) continue
            val plan = StarterWizard.planFor(style, style.lines.filter { !it.off }.map { it.role }.toSet(), StarterWizard.defaultParts(style), style.melody.acid != null)
            for ((i, sc) in p.allScenes.withIndex()) {
                if (sc.isDrumBar()) continue  // the song's one-bar fills and crashes (S3)
                if (plan.form == SongForm.Build && i == 1) continue  // the Montée rolls on its own
                if (sc.label.startsWith("Montée")) continue  // so does a Montée before a Drop
                // The bars before a Drop or a Refrain end on a beat of silence instead of a fill.
                if (sc.drums.lanes.all { l -> l.hits.keys.none { it >= sc.drums.steps - 4 } }) continue
                // A beat without snare, clap nor hats (the industrial Break's rumble alone) has nothing to roll.
                if (listOf(Role.Snare, Role.Clap, Role.Hat, Role.OpenHat).all { sc.drums.lanes[it.lane].hits.isEmpty() }) continue
                assertEquals("${style.id} $i", WizardMix.PHRASE_STEPS, sc.drums.steps)
                val lanes = sc.drums.lanes
                val last = WizardMix.PHRASE_STEPS - 4
                // The last beat is a fill (S3: one of the style's 3 or 4 endings): the snare, the clap, the hats, the open
                // hat or the toms play something else there than on the first bar's last beat; and the kick stops there.
                val fillLanes = listOf(Role.Snare, Role.Clap, Role.Hat, Role.OpenHat).map { it.lane } + DrumHumanizer.TOM_LANES
                fun beat(lane: Int, from: Int) = lanes[lane].hits.filterKeys { it in from until from + 4 }.map { (st, h) -> Triple(st - from, h.repeats, Math.round(h.velocity * 10)) }.toSet()
                val fill = fillLanes.any { l -> beat(l, last) != beat(l, 12) }
                assertTrue("${style.id} $i", fill)
                val kick = lanes[Role.Kick.lane].hits
                if (kick.keys.any { it < last }) assertTrue("${style.id} $i", kick.keys.none { it >= last })
            }
        }
    }

    @Test
    fun theDropIsFullerAndTheBreakStillPlays() {
        for (style in StarterWizard.STYLES) {
            val p = song(style)
            val plan = StarterWizard.planFor(style, style.lines.filter { !it.off }.map { it.role }.toSet(), StarterWizard.defaultParts(style), style.melody.acid != null)
            // The Drop (the Refrain) adds to the Groove (the Couplet); a Montée has no kick to compare.
            if (plan.form != SongForm.Build) assertTrue("${style.id} ${p.layers(1)} -> ${p.layers(2)}", p.layers(2) >= p.layers(1) + 1)
            // A Break keeps at least two layers (never a hole of one hat); the rave's is its 303 alone, its filter sweeping.
            if (plan.hasBreak && plan.form != SongForm.Rave) assertTrue("${style.id} break ${p.layers(3)}", p.layers(3) >= 2)
            // The second Drop is fuller: the first holds its counter-melody (or its lead) back for its second half.
            val held = p.instrumentTracks.firstOrNull { it.name == StarterWizard.COUNTER_TRACK }
                ?: p.instrumentTracks.firstOrNull { it.name == "Lead" }?.takeIf {
                    val scenes = plan.parts[Part.Lead].orEmpty()
                    plan.form != SongForm.Song && 2 in scenes && 1 !in scenes && 0 !in scenes
                }
            if (held != null) {
                val ch = InstrumentTrack.mixerChannel(held.id)
                val lane = p.automation.firstOrNull { it.target == AutoTarget(AutoTarget.VOLUME, ch) }
                assertNotNull(style.id, lane)
                assertTrue(style.id, lane!!.songOnly)
                val curve = lane.songCurve(p.songBars)
                val drops = StarterWizard.songSections(p).filter { it.scene == 2 }
                assertEquals(style.id, 0f, curve[drops.first().start])
                assertTrue(style.id, drops.drop(1).all { d -> curve[d.start] > 0.5f })
            }
        }
    }

    @Test
    fun midBassesGetASubAndSongsAnImpact() {
        for (id in listOf("dnb", "dubstep", "neurofunk", "tech_house", "speed_garage")) {
            val style = StarterWizard.style(id)!!
            assertTrue(id, style.melody.subBass)
            val p = song(style)
            val bass = p.instrumentTracks.first { it.name == "Basse" }
            assertTrue(id, p.instrumentTracks.any { it.name == StarterWizard.SUB_TRACK })
            // The mid bass leaves the low end to the sub: a high-pass on its strip.
            assertTrue(id, p.mixer.channels[InstrumentTrack.mixerChannel(bass.id)].inserts.any { it.type == EffectType.FILTER.ordinal && it.params[2] > 0.66f })
        }
        // A song with a riser: the noise held through its Pré-refrain (until its silent last beat), one short hit on the Refrain's first beat.
        val pop = song(StarterWizard.style("pop")!!)
        val riser = pop.instrumentTracks.single { it.name == StarterWizard.RISER_TRACK }
        fun notes(scene: Int) = pop.allScenes[scene].let { if (riser.id == 0) it.synthNotes else it.trackNotes[riser.id] ?: NoteClip() }.notes
        val pre = pop.allScenes.indexOfFirst { it.label == "${SongShape.PRE_CHORUS} 2" }
        assertEquals((64 - 4) * NoteClip.TICKS_PER_STEP, notes(pre).single().end)
        assertEquals(1, notes(2).size)
    }
}
