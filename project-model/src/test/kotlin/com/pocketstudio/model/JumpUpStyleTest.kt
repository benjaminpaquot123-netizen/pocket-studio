package com.pocketstudio.model

import com.pocketstudio.model.StarterWizard.Mood
import com.pocketstudio.model.StarterWizard.Part
import com.pocketstudio.model.StarterWizard.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The jump up / dancefloor drum & bass (phase 29): a growl over a clean sub, a second bass answering it, a real Drop. */
class JumpUpStyleTest {
    private val style = StarterWizard.style("jump_up")!!

    private fun build(parts: Set<Part> = Part.entries.toSet(), mood: Mood = style.melody.mood): Project =
        StarterWizard.build(style, style.lines.associate { it.role to null as PadSample? }, mood,
            parts.associateWith { StarterWizard.soundsFor(it, style).first() })

    private fun Project.notes(scene: Int, track: Int) = allScenes[scene].let { if (track == 0) it.synthNotes else it.trackNotes[track] ?: NoteClip() }.notes

    @Test
    fun itIsADrumAndBassOfTheBassMusicFamilyInTheWizard() {
        assertEquals("Bass music", style.family)
        assertTrue(style.bpm in 172.0..176.0)
        assertTrue(style in StarterWizard.STYLES && "jump_up" in StarterWizard.ORDER)
        assertEquals(StarterWizard.SongForm.Build, style.form)
        assertTrue(style.riser)
        assertEquals(Mood.Dark, style.melody.mood)
        assertEquals(setOf(Part.Bass, Part.Pad, Part.Lead), StarterWizard.defaultParts(style))
        // Two-step: kick on 1 and just after 3; the snare (and its clap) on 2 and 4.
        assertEquals(listOf(0, 10), StarterWizard.hits(style.lines.first { it.role == Role.Kick }.pattern).keys.sorted())
        for (role in listOf(Role.Snare, Role.Clap)) {
            val hits = StarterWizard.hits(style.lines.first { it.role == role }.pattern)
            assertTrue(role.id, 4 in hits && 12 in hits)
        }
        assertTrue(style.references.size >= 4)
    }

    @Test
    fun theGrowlPlaysOverACleanSubAnOctaveDown() {
        val growl = StarterWizard.soundsFor(Part.Bass, style).first()
        assertEquals("Basse growl", growl.name)
        // Its filter moves every sixteenth, deep and resonant: the growl.
        assertEquals(SynthPatch.syncValue(SynthPatch.SYNC_SIXTEENTH), growl.synth.get(SynthPatch.LFO2_SYNC), 0f)
        assertTrue(growl.synth.get(SynthPatch.SWEEP) >= 0.5f && growl.synth.get(SynthPatch.RESONANCE) >= 0.5f)
        val p = build()
        val bass = p.instrumentTracks.first { it.name == "Basse" }
        val sub = p.instrumentTracks.first { it.name == StarterWizard.SUB_TRACK }
        // S4 (WideMix): the bass music sub a little saturated, still mono.
        // S5 (WizardMix.honest): 2 dB lower, the bass's body lifted instead.
        val subSound = StarterWizard.SUB_SOUND.with(SynthPatch.GLIDE, StarterWizard.BASS_GLIDE).with(SynthPatch.DRIVE, WideMix.SUB_DRIVE)
        assertEquals(subSound.with(SynthPatch.GAIN, SynthPatch.gainValue(SynthPatch.gainDb(subSound.get(SynthPatch.GAIN)) + WizardMix.BASS_MUSIC_SUB_DB)), sub.synth)
        // In every section, the sub plays the bass's notes an octave down (nothing where the bass rests).
        for (scene in p.allScenes.indices) {
            assertEquals("scene $scene", p.notes(scene, bass.id).map { it.copy(pitch = it.pitch - 12) }, p.notes(scene, sub.id))
        }
        val drop = p.notes(2, sub.id)
        assertTrue(drop.isNotEmpty() && p.notes(1, sub.id).isEmpty())
        // F1 (44 Hz) at the lowest: felt on a club system, the growl above carries it on a phone.
        assertTrue(drop.minOf { it.pitch } >= 28 && p.notes(2, bass.id).minOf { it.pitch } >= 40)
        // The growl slides up an octave somewhere (notes that overlap), the sub with it.
        assertFalse(drop.sortedBy { it.start }.zipWithNext().all { (a, b) -> b.start >= a.end })
        // Effects: the growl preset with a soft clipper before its OTT. S5: no pump of their own on the
        // growl nor the sub: both go through the Musique bus, which ducks once (their lows deeper) under the kick.
        val types = { id: Int -> p.mixer.channels[InstrumentTrack.mixerChannel(id)].inserts.map { it.effect } }
        assertTrue(types(bass.id).containsAll(listOf(EffectType.DISTORTION, EffectType.CLIPPER, EffectType.OTT)))
        assertTrue(types(bass.id).indexOf(EffectType.CLIPPER) < types(bass.id).indexOf(EffectType.OTT))
        assertTrue(EffectType.PUMP !in types(bass.id) && EffectType.PUMP !in types(sub.id))
        val bus = p.mixer.musicBus!!
        assertTrue(InstrumentTrack.mixerChannel(bass.id) in bus.members && InstrumentTrack.mixerChannel(sub.id) in bus.members)
        assertTrue(bus.lowDepth > bus.depth)
        assertEquals(Mixer.padChannel(StarterWizard.Role.Kick.lane), p.mixer.sidechainKey)
    }

    @Test
    fun theSecondBassAnswersInTheGrowlsSilences() {
        for (mood in Mood.entries) {
            val p = build(mood = mood)
            val bass = p.instrumentTracks.first { it.name == "Basse" }.id
            val lead = p.instrumentTracks.first { it.name == "Lead" }
            // The leads chantier: the answer is a hoover an octave up (it was a second wobble bass at
            // 177 Hz, -17.9 dB of presence on a phone), still in the growl's silences.
            assertEquals("Hoover", StarterWizard.soundsFor(Part.Lead, style).first().name)
            val calls = p.notes(2, bass)
            val answers = p.notes(2, lead.id)
            assertTrue(answers.isNotEmpty())
            for (a in answers) assertTrue("$mood ${a.start}", calls.none { it.start <= a.start && a.start < it.end })
            // In the middle, where a phone hears it (Fa4 to Fa5), not a squealing lead.
            assertTrue(answers.map { it.pitch }.toString(), answers.all { it.pitch in 60..StarterWizard.LEAD_TOP })
            // Only in the Drop: the Montée and the Break leave it out.
            assertTrue(p.notes(1, lead.id).isEmpty() && p.notes(3, lead.id).isEmpty())
        }
    }

    @Test
    fun noSubWithoutTheBassNorInTheOtherStyles() {
        assertNull(build(setOf(Part.Pad, Part.Lead)).instrumentTracks.firstOrNull { it.name == StarterWizard.SUB_TRACK })
        val dnb = StarterWizard.style("house")!!
        val p = StarterWizard.build(dnb, dnb.lines.associate { it.role to null as PadSample? }, dnb.melody.mood,
            mapOf(Part.Bass to StarterWizard.soundsFor(Part.Bass, dnb).first()))
        assertNull(p.instrumentTracks.firstOrNull { it.name == StarterWizard.SUB_TRACK })
        assertNotNull(build(setOf(Part.Bass)).instrumentTracks.firstOrNull { it.name == StarterWizard.SUB_TRACK })
    }

    @Test
    fun theSongBuildsUpDropsBreaksAndDropsAgainInAboutThreeMinutes() {
        val p = build()
        assertEquals(listOf("Intro", "Montée", "Drop", "Break"), p.allScenes.take(4).map { it.label })
        // Bass music lasts 5 minutes (Benjamin, 2026-10-23): three Drops at 174 BPM.
        val seconds = p.songBars * 4 * 60.0 / p.tempoBpm
        assertTrue("$seconds", seconds in 270.0..SongShape.LONG_SECONDS)
        assertEquals(3, StarterWizard.songSections(p).count { it.scene == 2 })
        // The Break: the pad and the hats, no kick, no bass, no sub.
        val sub = p.instrumentTracks.first { it.name == StarterWizard.SUB_TRACK }.id
        assertTrue(p.allScenes[3].drums.lanes[Role.Kick.lane].hits.isEmpty() && p.notes(3, sub).isEmpty())
        assertTrue(p.allScenes[3].drums.lanes[Role.Hat.lane].hits.isNotEmpty())
        // A white noise riser before each Drop.
        assertTrue(p.instrumentTracks.any { it.name == StarterWizard.RISER_TRACK })
    }
}
