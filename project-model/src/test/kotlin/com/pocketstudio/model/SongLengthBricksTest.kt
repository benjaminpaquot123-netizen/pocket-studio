package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tour 6, lot « padlive2 », P3 (control seats Ab-2 écran 54, Ab-3 écrans 30 et 39): takes of 6, 2 then 5 bars put in Live
 * bricks (« Enregistrer ce que je joue » › « Dans une brique du Live ») and the home card and « Mes projets » said
 * « Boucle de 1 mes. »: the loop's length counted the drums, the 303 and the notes, never the audio bricks of the scene.
 * It is the longest of them now; a song keeps its own length, and another scene's brick does not count.
 */
class SongLengthBricksTest {
    private fun brick(id: Int, scene: Int?, bars: Int, song: Boolean = false) =
        AudioTrack(id = id, name = "Audio ${id + 1}", clip = AudioClip("t$id.wav", "Prise $id", seconds = bars * 2.0, loopSteps = bars * 16, scene = scene, song = song))

    @Test
    fun aLoopLastsAsLongAsItsLongestBrick() {
        val p = Project(name = "Live", tempoBpm = 120.0).copy(audioTracks = listOf(brick(0, 0, 6), brick(1, 0, 2), brick(2, 0, 5)))
        assertEquals(SongLength(6, 12, 0), SongLength.of(p))
    }

    @Test
    fun anotherScenesBrickOrASongClipDoesNotCount() {
        val p = Project(name = "Live", tempoBpm = 120.0).copy(audioTracks = listOf(brick(0, 3, 6), brick(1, null, 8, song = true)))
        assertEquals(SongLength(1, 2, 0), SongLength.of(p))
    }
}
