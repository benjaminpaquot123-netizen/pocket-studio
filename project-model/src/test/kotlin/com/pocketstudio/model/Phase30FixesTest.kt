package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.log10

/** Benjamin's phone feedback of 2026-10-22 (phase 30): levels of what is added later, the sound shown as chosen. */
class Phase30FixesTest {
    private fun db(track: InstrumentTrack) = SynthPatch.gainDb(track.synth.get(SynthPatch.GAIN))

    @Test
    fun aTrackAddedLaterSitsAtTheAssistantsLevelForItsPart() {
        val p = Project()
        fun added(kind: TrackKind): InstrumentTrack {
            val next = NewTracks.add(p, kind, null)!!
            return next.tracks.first { t -> p.tracks.none { it.id == t.id } }
        }
        val pad = added(TrackKind.Chords)
        val bass = added(TrackKind.Bass)
        val padPreset = SoundLibrary.SYNTH.first { it.name == SoundLibrary.synthNameOf(pad.synth) }.sound
        val bassPreset = SoundLibrary.SYNTH.first { it.name == SoundLibrary.synthNameOf(bass.synth) }.sound
        // The bass up front, a pad behind (WizardMix.partDb), on top of their sound's calibration.
        assertEquals(SynthPatch.gainDb(bassPreset.get(SynthPatch.GAIN)) + 2.5f, db(bass), 0.01f)
        assertTrue(db(pad) < SynthPatch.gainDb(padPreset.get(SynthPatch.GAIN)) - 0.5f)
    }

    @Test
    fun the303SoundsAreCalibratedAndATakenSoundTakesItsLevel() {
        // Measured alone (phase 30), each brought to about -16 LUFS: none at the old 0.7 that put it 4 to 9 dB over a pad.
        val base = SoundLibrary.ACID.first { it.name == "Acide de base" }.sound
        assertEquals(AcidSound.DEFAULT_LEVEL, base.level, 0f)
        assertEquals(AcidSound.DEFAULT_LEVEL, AcidSound().level, 0f)
        assertTrue(SoundLibrary.ACID.first { it.name == "Hurleur" }.sound.level < 0.35f)
        // Measured 3.2 dB over -16 LUFS at 0.7: the new level takes them off.
        assertEquals(-3.2f, (20 * log10(AcidSound.DEFAULT_LEVEL / AcidSound.OLD_LEVEL)), 0.15f)
        // A 303 at the old level takes the preset's calibrated level; a volume the user set stays.
        val hurleur = SoundLibrary.ACID.first { it.name == "Hurleur" }.sound
        assertEquals(hurleur.level, SoundLibrary.withAcidPreset(hurleur, AcidSound(level = AcidSound.OLD_LEVEL, tuning = 0.6f)).level, 0f)
        assertEquals(0.6f, SoundLibrary.withAcidPreset(hurleur, AcidSound(level = AcidSound.OLD_LEVEL, tuning = 0.6f)).tuning, 0f)
        assertEquals(0.55f, SoundLibrary.withAcidPreset(hurleur, AcidSound(level = 0.55f)).level, 0f)
        // The assistant's quick 303 sounds too.
        assertEquals(SoundLibrary.ACID.first { it.name == "Carré saturé" }.sound.level, WizardAcid.sound("Saturée", AcidSound(level = AcidSound.OLD_LEVEL))!!.level, 0f)
    }

    @Test
    fun aSavedSilent303ComesBackAtTheCalibratedLevel() {
        val silent = Project(acid = AcidPattern(sound = AcidSound(level = AcidSound.OLD_LEVEL)))
        assertEquals(AcidSound.DEFAULT_LEVEL, ProjectJson.decode(ProjectJson.encode(silent)).acid.sound.level, 0f)
        // A 303 that plays keeps its volume: what is heard never changes.
        val playing = silent.copy(acid = silent.acid.copy(steps = silent.acid.steps.mapIndexed { i, s -> if (i == 0) s.copy(gate = true) else s }))
        assertEquals(AcidSound.OLD_LEVEL, ProjectJson.decode(ProjectJson.encode(playing)).acid.sound.level, 0f)
    }

    @Test
    fun theAssistantsSoundIsFoundAmongTheReadyMadeOnes() {
        // Jump up DnB: its pad (calibrated, lowered), its bass (calibrated, gliding).
        val style = StarterWizard.style("jump_up")!!
        val parts = StarterWizard.Part.entries.filter { it in StarterWizard.defaultParts(style) }.associateWith { StarterWizard.soundsFor(it, style).first() }
        val p = StarterWizard.build(style, emptyMap(), style.melody.mood, parts)
        val pad = p.instrumentTracks.first { t -> t.name.contains("Nappe") || parts[StarterWizard.Part.Pad]?.name?.let { t.name.contains(it) } == true }
        assertEquals("Nappe", SoundLibrary.synthNameOf(pad.synth))
        val bass = p.instrumentTracks.first { SoundLibrary.synthNameOf(it.synth) == "Basse growl" }
        assertTrue(abs(bass.synth.get(SynthPatch.GLIDE) - SoundLibrary.SYNTH.first { it.name == "Basse growl" }.sound.get(SynthPatch.GLIDE)) > 0f)
    }
}
