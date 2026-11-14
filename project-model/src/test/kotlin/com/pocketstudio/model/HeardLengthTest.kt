package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tour 6, lot « coquille2 » (P6): a take of 6, 2 then 5 bars put in a brick of the Live (its loop as `placeInCell` sets
 * it: [LiveCells.loopBars] whole bars) is counted by [heardSceneSteps] at that loop, never at the 1-bar scene; and the
 * same number as a loop's `SongLength.of` once lot padlive2 counts the bricks (aligned at its entry, chef's note).
 */
class HeardLengthTest {
    private fun withTake(bars: Int): Project {
        val p = Project()
        val seconds = bars * p.beatsPerBar * 60.0 / p.tempoBpm
        val loop = LiveCells.loopBars(seconds, p.tempoBpm, p.beatsPerBar)!!
        val clip = AudioClip("take.wav", "Ma prise", seconds = seconds, loopSteps = loop * p.stepsPerBar, bpm = p.tempoBpm, fit = true, scene = p.scene)
        return p.copy(audioTracks = listOf(AudioTrack(id = 0, name = "Audio 1", clip = clip)))
    }

    @Test
    fun aTakeInABrickIsCountedAtItsLoop() {
        for (bars in listOf(6, 2, 5)) {
            val p = withTake(bars)
            val loop = LiveCells.loopBars(bars * p.beatsPerBar * 60.0 / p.tempoBpm, p.tempoBpm, p.beatsPerBar)!!
            assertEquals("a take of $bars bars", loop * p.stepsPerBar, p.heardSceneSteps(p.scene))
        }
        // Another scene's brick is not this one's length; a scene with no brick keeps its notes' length.
        val other = withTake(6).let { it.copy(audioTracks = it.audioTracks.map { t -> t.copy(clip = t.clip!!.copy(scene = 3)) }) }
        assertEquals(other.sceneSteps(other.scene), other.heardSceneSteps(other.scene))
    }

    /**
     * Fresh review (minor 1): counted as the engine loops them ([AudioClip.engineLoopSteps], [AudioClip.belongsTo]): a
     * clip of every part counts, a « once » clip counts its whole passes, a muted track is not heard.
     */
    @Test
    fun bricksAreCountedAsTheEngineLoopsThem() {
        val p = Project()
        val spb = p.stepsPerBar
        fun with(clip: AudioClip, mute: Boolean = false): Project {
            val q = p.copy(audioTracks = listOf(AudioTrack(id = 0, name = "Audio 1", clip = clip)))
            return if (!mute) q else q.copy(mixer = q.mixer.copy(channels = q.mixer.channels.mapIndexed { i, c -> if (i == AudioTrack.mixerChannel(0)) c.copy(mute = true) else c }))
        }
        val bar = p.beatsPerBar * 60.0 / p.tempoBpm
        assertEquals("a clip of every part", 4 * spb, with(AudioClip("a.wav", "a", seconds = 4 * bar, loopSteps = 4 * spb, scene = null)).heardSceneSteps(p.scene))
        assertEquals("a « once » clip, its passes", 3 * spb, with(AudioClip("a.wav", "a", seconds = 3 * bar, loopSteps = spb, once = true, scene = p.scene)).heardSceneSteps(p.scene))
        assertEquals("a muted track", p.sceneSteps(p.scene), with(AudioClip("a.wav", "a", seconds = 4 * bar, loopSteps = 4 * spb, scene = p.scene), mute = true).heardSceneSteps(p.scene))
    }

    /** Chef's note (padlive2 entered): the home's « Boucle de N mes. » and the transport say the same number (6, 2, 5). */
    @Test
    fun theHomeAndTheTransportSayTheSameLength() {
        for (bars in listOf(6, 2, 5)) {
            val p = withTake(bars)
            assertEquals("a take of $bars bars", p.heardSceneSteps(p.scene) / p.stepsPerBar, SongLength.of(p).bars)
        }
        // A brick muted, or one of every part: the same number too.
        val every = withTake(6).let { it.copy(audioTracks = it.audioTracks.map { t -> t.copy(clip = t.clip!!.copy(scene = null)) }) }
        assertEquals(every.heardSceneSteps(every.scene) / every.stepsPerBar, SongLength.of(every).bars)
    }
}
