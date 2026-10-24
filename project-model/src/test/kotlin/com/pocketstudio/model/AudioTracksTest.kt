package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioTracksTest {
    @Test
    fun upToEightAudioTracksWithTheirOwnChannel() {
        var p = Project()
        repeat(8) { p = p.addAudioTrack()!! }
        assertEquals((0..7).toList(), p.audioTracks.map { it.id })
        assertNull(p.addAudioTrack())
        // 1..4 on 12..15 as always; 5..8 after the pads' strips, on 32..35.
        assertEquals(listOf(12, 13, 14, 15, 32, 33, 34, 35), (0..7).map(AudioTrack::mixerChannel))
        assertEquals((0..7).toList(), (0..7).map { AudioTrack.ofChannel(AudioTrack.mixerChannel(it)) })
        assertNull(AudioTrack.ofChannel(16))
        assertEquals(7, p.removeAudioTrack(1).audioTracks.size)
        // A strip of its own, set back to neutral when the track goes.
        val solo = p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.mapIndexed { i, c -> if (i == 34) c.copy(solo = true) else c }))
        assertEquals(Channel(), solo.removeAudioTrack(6).mixer.channels[34])
        assertEquals("audio:6", keyOfChannel(34))
        assertEquals(34, channelOfKey("audio:6"))
    }

    @Test
    fun aProjectSavedWithFourAudioTracksLoadsUnchanged() {
        // A file from before: 32 mixer strips, audio tracks 0..3.
        var p = Project()
        repeat(4) { p = p.addAudioTrack()!! }
        p = p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.take(32).mapIndexed { i, c -> if (i == 13) c.copy(volume = 0.5f, mute = true) else c }))
        val text = ProjectJson.encode(p)
        val back = ProjectJson.decode(text)
        assertEquals(Mixer.CHANNELS, back.mixer.channels.size)
        assertEquals(p.mixer.channels.take(32).map { it.sanitized() }, back.mixer.channels.take(32))
        assertTrue(back.mixer.channels.drop(32).all { it == Channel() })
        assertEquals(p.audioTracks.map { it.sanitized() }, back.audioTracks)
        assertEquals(0.5f, back.mixer.channels[AudioTrack.mixerChannel(1)].volume)
    }

    @Test
    fun theNewAudioStripsCanBeAutomatedButNotThePads() {
        // The engine reads the channel's bits 4..5 at 24 (Automation::pack): 34 = 0x22.
        val t = AutoTarget(AutoTarget.VOLUME, 34)
        assertTrue(t.isValid)
        assertEquals((2 shl 24) or (2 shl 12), t.pack())
        assertEquals(AutoTarget(AutoTarget.VOLUME, 13).pack(), 13 shl 12)  // as before for 0..15
        assertTrue(!AutoTarget(AutoTarget.VOLUME, Mixer.padChannel(3)).isValid)
        assertTrue(AutoTarget(AutoTarget.MACRO, 33, 0, AutoTarget.MACRO_CHOP).isValid)
    }

    @Test
    fun clipFilesAreKeptWithTheProject() {
        var p = Project().addAudioTrack("Voix")!!
        p = p.withAudioTrack(p.audioTracks[0].copy(clip = AudioClip("abc.wav", "Prise 1", 3.0)))
        assertTrue("abc.wav" in p.sampleFiles)
        assertEquals(p.sanitized(), ProjectJson.decode(ProjectJson.encode(p)))
    }

    @Test
    fun badClipsAreCleaned() {
        val bad = AudioTrack(0, "", clip = AudioClip("../x.wav", "x"))
        assertNull(bad.sanitized().clip)
        assertEquals("Audio 1", bad.sanitized().name)
        val clip = AudioClip("a.wav", "a", loopSteps = 20, gain = Float.NaN, start = -3).sanitized()
        assertEquals(20, clip.loopSteps)
        assertEquals(1f, clip.gain)
        assertEquals(0, clip.start)
    }

    @Test
    fun barsForALength() {
        assertEquals(1, AudioClip.barsFor(1.5, 120.0))
        assertEquals(2, AudioClip.barsFor(4.0, 120.0))   // exactly two bars
        assertEquals(3, AudioClip.barsFor(4.5, 120.0))
        assertEquals(3, AudioClip.barsFor(4.0, 120.0, beatsPerBar = 3))  // 8 beats = 2 bars of 3/4 and a bit
    }
}
