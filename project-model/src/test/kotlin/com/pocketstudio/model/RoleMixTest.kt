package com.pocketstudio.model

import com.pocketstudio.model.StarterWizard.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * G0: no track of the assistant's songs left untreated (Benjamin's Sub, Voix and Sub drop had no
 * effect at all), each at its place in the stereo (his faders all at 0 dB, nothing panned).
 */
class RoleMixTest {
    private fun song(id: String) = DropVariationTest.song(StarterWizard.style(id)!!)

    private fun Project.strip(track: Int) = mixer.channels[InstrumentTrack.mixerChannel(track)]

    @Test
    fun benjaminsSubVoiceAndSubDropHadNothing() {
        val p = DropVariationTest.benj()
        val untreated = p.instrumentTracks.filter { t -> p.strip(t.id).inserts.all { it.type == 0 } }.map { it.name }
        assertEquals(listOf("Sub", "Voix", "Sub drop"), untreated)
        // Nothing panned but the pads of the drums.
        assertTrue(p.instrumentTracks.all { p.strip(it.id).pan == 0f })
    }

    @Test
    fun everyAssistantTrackHasItsChainButTheSub() {
        val bad = ArrayList<String>()
        for (style in StarterWizard.STYLES) {
            val p = DropVariationTest.song(style)
            for (t in p.instrumentTracks) {
                val c = p.strip(t.id)
                val name = DefaultNames.french(t.name) ?: t.name
                // Played anywhere in the song (an unused track keeps an empty strip).
                val plays = p.allScenes.any { s -> (if (t.id == 0) s.synthNotes else s.trackNotes[t.id])?.notes?.isNotEmpty() == true }
                if (!plays) continue
                when (name) {
                    // The exceptions: the sub (its drive is its synth's) has nothing on its strip; the sub drop is saturated.
                    StarterWizard.SUB_TRACK -> if (RoleMix.lowCutHz(c) != null) bad += "${style.id}/$name cut"
                    StarterWizard.SUB_DROP_TRACK -> if (c.inserts.none { it.effect == EffectType.SATURATION }) bad += "${style.id}/$name"
                    else -> {
                        if (RoleMix.lowCutHz(c) == null) bad += "${style.id}/$name no low cut"
                        // The one G0 sets (a flat EQ's « Coupe-bas »; a style's own filter is its choice).
                        val cut = c.inserts.filter { it.effect == EffectType.PARAM_EQ }.mapNotNull { RoleMix.lowCutHz(Channel(inserts = listOf(it))) }.maxOrNull()
                        val bass = name in setOf(StarterWizard.BASS_SWITCH_TRACK) || t.id in p.bassTrackIds()
                        if (!bass && c.sends.all { it <= 0f }) bad += "${style.id}/$name no send"
                        // Never over the track's lowest note (the riser is noise: its note is no pitch).
                        if (TrackRoles.isRiserName(name)) continue
                        val low = p.allScenes.flatMap { s -> ((if (t.id == 0) s.synthNotes else s.trackNotes[t.id])?.notes).orEmpty() }.minOf { it.pitch }
                        val hz = 440f * Math.pow(2.0, (low - 69) / 12.0).toFloat()
                        if (cut != null && cut > hz * 1.01f) bad += "${style.id}/$name cut ${cut.toInt()} Hz over its lowest note ${hz.toInt()} Hz"
                    }
                }
            }
            // The 303 when it plays.
            if (p.allScenes.any { s -> s.acid.steps.take(s.acid.length).any { it.gate } } && RoleMix.lowCutHz(p.mixer.channels[AutoTarget.ACID_CHANNEL]) == null) bad += "${style.id}/303"
        }
        assertTrue(bad.joinToString("\n"), bad.isEmpty())
    }

    @Test
    fun aPadsPanSnapsToTheCentreForTheWholeSong() {
        // Benjamin's kick: hard left in every scene but the two intro ones, copies of the song's one pan (Scenes.kt keeps them in step).
        val p = DropVariationTest.benj()
        assertEquals(-1f, p.drums.lanes[Role.Kick.lane].pan, 0f)
        // The sliders snap near the middle, as the console does.
        val d = p.drums.withPadPan(Role.Kick.lane, 0.03f)
        assertEquals(0f, d.lanes[Role.Kick.lane].pan, 0f)
        assertEquals(0f, p.drums.withPadPan(Role.Kick.lane, -0.039f).lanes[Role.Kick.lane].pan, 0f)
        assertEquals(-0.3f, p.drums.withPadPan(Role.Kick.lane, -0.3f).lanes[Role.Kick.lane].pan, 0f)
        assertEquals(1f, p.drums.withPadPan(Role.Kick.lane, 3f).lanes[Role.Kick.lane].pan, 0f)
        // A pad's pan is the song's (the pattern shown; the scenes' own copies are not what plays): every part plays it.
        val all = p.copy(drums = d)
        assertTrue(all.song.all { all.partView(it).drums.lanes[Role.Kick.lane].pan == 0f })
    }

    @Test
    fun aTrackThatPlaysNothingKeepsItsStrip() {
        // A track the assistant left empty (a drums-only song's, one added before RoleMix): a bass played there later is not thinned.
        for (id in listOf("techno", "dnb", "trap")) {
            val style = StarterWizard.style(id)!!
            val made = StarterWizard.build(style, style.lines.associate { it.role to null as PadSample? })
            val p = made.addTrack("Basse")!!
            val empty = p.tracks.last().id
            val after = RoleMix.apply(p, style, mapOf(empty to StarterWizard.Part.Bass))
            assertEquals(id, p.strip(empty), after.strip(empty))
            // With a note, the same track gets its chain.
            val played = p.withTrack(p.track(empty).copy(notes = NoteClip(notes = listOf(Note(0, 24, 40)))))
            assertNotNull(id, RoleMix.lowCutHz(RoleMix.apply(played, style, mapOf(empty to StarterWizard.Part.Bass)).strip(empty)))
        }
    }

    @Test
    fun jumpUpsSubVoiceAndSubDropAreTreated() {
        val p = song("jump_up")
        val byName = p.instrumentTracks.associateBy { DefaultNames.french(it.name) ?: it.name }
        val voice = p.strip(byName.getValue(StarterWizard.VOCAL_TRACK).id)
        assertNotNull(RoleMix.lowCutHz(voice))
        assertTrue(voice.sends[0] > 0f)
        assertTrue(p.strip(byName.getValue(StarterWizard.SUB_DROP_TRACK).id).inserts.any { it.effect == EffectType.SATURATION })
        assertNull(RoleMix.lowCutHz(p.strip(byName.getValue(StarterWizard.SUB_TRACK).id)))
    }

    @Test
    fun eachRoleAtItsPlace() {
        for (style in StarterWizard.STYLES) {
            val p = DropVariationTest.song(style)
            val id = style.id
            // The kick, the basses, the subs centred.
            assertEquals(id, 0f, p.drums.lanes[Role.Kick.lane].pan, 0f)
            for (t in p.instrumentTracks) {
                val name = DefaultNames.french(t.name) ?: t.name
                val pan = p.strip(t.id).pan
                when {
                    t.id in p.bassTrackIds() || name in setOf(StarterWizard.SUB_TRACK, StarterWizard.SUB_DROP_TRACK, StarterWizard.BASS_SWITCH_TRACK) -> assertEquals("$id/$name", 0f, pan, 0f)
                    t.id == p.leadTrackId() -> assertTrue("$id/$name $pan", pan in 0.05f..0.25f)
                    name == StarterWizard.COUNTER_TRACK -> assertTrue("$id/$name $pan", pan in -0.25f..-0.05f)
                    // The chords centred and wide: a widener, or a synth spread already.
                    t.id in p.padTrackIds() -> {
                        assertEquals("$id/$name", 0f, pan, 0f)
                        val spread = t.engine == 0 && t.instrument == null && t.synth.get(SynthPatch.WIDTH) > 0f
                        assertTrue("$id/$name", spread || p.strip(t.id).inserts.any { it.effect == EffectType.WIDENER })
                    }
                }
            }
            // The hats and the percussions out to the sides (at least ±15 %).
            for (role in listOf(Role.Hat, Role.OpenHat, Role.Perc, Role.Perc2)) {
                val lane = p.drums.lanes[role.lane]
                val used = p.allScenes.any { it.drums.lanes[role.lane].hits.isNotEmpty() }
                if (used) assertTrue("$id ${role.id} ${lane.pan}", kotlin.math.abs(lane.pan) >= 0.15f)
            }
        }
    }
}
