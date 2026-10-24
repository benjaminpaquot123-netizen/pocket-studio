package com.pocketstudio.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The riser is heard only when the whole song plays: a section played in a loop holds it shut
 * (no noise through a Montée, no burst at every Drop loop). The ride, the rumble preset.
 */
class RiserLoopTest {
    private fun build(s: StarterWizard.Style, parts: Boolean): Project {
        val lines = s.lines.associate { it.role to null as PadSample? }
        val chosen = if (parts) StarterWizard.Part.entries.associateWith { StarterWizard.soundsFor(it, s).first() } else emptyMap()
        return StarterWizard.build(s, lines, s.melody.mood, chosen)
    }

    @Test
    fun aSectionInALoopKeepsTheRiserShutInEveryStyle() {
        val risers = StarterWizard.STYLES.filter { it.riser }
        assertTrue(risers.size >= 10)
        for (s in risers) for (parts in listOf(true, false)) {
            val p = build(s, parts)
            val track = p.instrumentTracks.single { it.name == StarterWizard.RISER_TRACK }
            val ch = InstrumentTrack.mixerChannel(track.id)
            val index = p.automation.indexOfFirst { it.target == AutoTarget(AutoTarget.INSERT_PARAM, ch, 0, 0) }
            val lane = p.automation[index]
            assertTrue(s.id, lane.songOnly)
            // Each scene of the song played on its own: the riser's filter at 0 (its insert's own value too).
            for (scene in p.song.map { it.scene }.distinct()) {
                assertEquals("${s.id} $scene", 0f, p.songCurvesAt(scene)[index] ?: 0f, 0f)
            }
            assertEquals(s.id, 0f, p.mixer.channels[ch].inserts[0].params[0], 0f)
            // The song itself still rises and hits: open on each Drop's first bar.
            // A point per beat (the beat of silence before each Drop).
            val unit = if (lane.songPerBeat) p.beatsPerBar else 1
            val curve = lane.songCurve(p.songBars * unit)
            val drops = StarterWizard.songSections(p).filter { it.scene == 2 }
            assertTrue(s.id, drops.isNotEmpty())
            drops.forEach { assertTrue("${s.id} ${it.start}", curve[it.start * unit] > 0.5f) }
        }
    }

    @Test
    fun otherSongCurvesStillHoldWhereTheSceneFirstPlays() {
        val s = StarterWizard.style("techno")!!
        val p = build(s, parts = true)
        val sweeps = p.automation.withIndex().filter { (_, l) -> l.mode == AutomationLane.SONG && !l.songOnly && l.hasPoints }
        assertTrue(sweeps.isNotEmpty())
        val drop = p.song.indexOfFirst { it.scene == 2 }
        val bar = p.songStepOf(drop) / p.stepsPerBar
        sweeps.forEach { (i, lane) ->
            val at = if (lane.songPerBeat) p.songStepOf(drop) / 4 else bar
            assertEquals(lane.songCurve(at + 1)[at], p.songCurvesAt(2)[i])
        }
    }

    @Test
    fun aSongOnlyLaneIsSavedOnlyWhenSetAndOldFilesLoad() {
        val plain = AutomationLane(AutoTarget(AutoTarget.INSERT_PARAM, 4, 0, 0), mode = AutomationLane.SONG)
        val json = Json { encodeDefaults = true }
        assertFalse(json.encodeToString(AutomationLane.serializer(), plain).contains("songOnly"))
        val only = plain.copy(songOnly = true)
        assertEquals(only, json.decodeFromString(AutomationLane.serializer(), json.encodeToString(AutomationLane.serializer(), only)))
        assertEquals(plain, Json.decodeFromString(AutomationLane.serializer(), """{"target":{"kind":${AutoTarget.INSERT_PARAM},"ch":4},"mode":${AutomationLane.SONG}}"""))
        assertEquals(only, only.sanitized())
    }

    @Test
    fun theRideIsTheCymbalMadeShortHighAndSoft() {
        val kit = PadSample("r.wav", "Crash")
        val ride = StarterWizard.kitShaped(StarterWizard.Role.Ride, kit)
        assertTrue(StarterWizard.shapesKit(StarterWizard.Role.Ride))
        assertEquals(StarterWizard.RIDE_NAME, ride.name)
        assertTrue(ride.pitch > 0f)
        assertEquals(0f, ride.sustain, 0f)
        assertTrue(ride.decay in 0.1f..0.5f)
        assertTrue(ride.gain < 0.8f)
        // The other lines keep the kit voice as it is.
        assertFalse(StarterWizard.shapesKit(StarterWizard.Role.OpenHat))
        assertEquals(kit, StarterWizard.kitShaped(StarterWizard.Role.Hat, kit))
        // Softer lines where the research says: hard techno under its open hat, Detroit on beats 1 and 3.
        val hard = StarterWizard.style("hard_techno")!!.lines.single { it.role == StarterWizard.Role.Ride }
        assertTrue(hard.pattern.none { it == 'x' || it == 'X' })
        val detroit = StarterWizard.style("detroit")!!.lines.single { it.role == StarterWizard.Role.Ride }
        assertEquals(2, detroit.pattern.count { it != '.' })
    }

    @Test
    fun theRumbleIsOfferedOnAPadsStripNotOnTheWholeKit() {
        assertTrue(FxPresets.suggestedFor(SoundKind.DRUMS).any { it.id == FxPresets.RUMBLE })
        assertFalse(FxPresets.suggestedFor(SoundKind.DRUMS, 0).any { it.id == FxPresets.RUMBLE })
        assertTrue(FxPresets.suggestedFor(SoundKind.DRUMS, Mixer.padChannel(3)).any { it.id == FxPresets.RUMBLE })
        assertEquals(FxPresets.suggestedFor(SoundKind.BASS), FxPresets.suggestedFor(SoundKind.BASS, 2))
    }
}
