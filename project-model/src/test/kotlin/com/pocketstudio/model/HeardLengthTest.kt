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
}
