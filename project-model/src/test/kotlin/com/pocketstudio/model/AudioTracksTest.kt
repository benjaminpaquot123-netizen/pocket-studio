package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioTracksTest {
    @Test
    fun upToFourAudioTracksWithTheirOwnChannel() {
        var p = Project()
        repeat(4) { p = p.addAudioTrack()!! }
        assertEquals(listOf(0, 1, 2, 3), p.audioTracks.map { it.id })
        assertNull(p.addAudioTrack())
        assertEquals(15, AudioTrack.mixerChannel(3))
        assertEquals(3, p.removeAudioTrack(1).audioTracks.size)
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
