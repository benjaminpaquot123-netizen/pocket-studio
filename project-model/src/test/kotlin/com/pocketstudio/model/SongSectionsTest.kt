package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Step 5c: a song's parts found from its stems' loudness bar by bar, made bricks of the project. */
class SongSectionsTest {
    /** Bars of drums, bass, other, voice at these levels. */
    private fun bars(n: Int, drums: Float, bass: Float, other: Float, voice: Float) = List(n) { floatArrayOf(drums, bass, other, voice) }

    /** Intro (pad alone), verse, chorus, verse, chorus, end (pad alone): 40 bars. */
    private val song = bars(4, 0f, 0f, 0.3f, 0f) + bars(8, 0.6f, 0.6f, 0.4f, 0.8f) + bars(8, 1f, 1f, 1f, 1f) +
        bars(8, 0.6f, 0.6f, 0.4f, 0.8f) + bars(8, 1f, 1f, 1f, 1f) + bars(4, 0f, 0f, 0.3f, 0f)

    private fun energy(rows: List<FloatArray>) = List(Remix.STEMS) { k -> FloatArray(rows.size) { rows[it][k] * (0.95f + 0.1f * (it % 3) / 2f) } }

    @Test
    fun introVersesChorusesAndEndAreFoundOnTheBars() {
        val parts = SongSections.detect(energy(song))
        assertEquals(listOf("Intro", "Couplet", "Refrain", "Couplet", "Refrain", "Fin"), parts.map { it.label })
        assertEquals(listOf(0, 4, 12, 20, 28, 36), parts.map { it.fromBar })
        assertEquals(listOf(4, 8, 8, 8, 8, 4), parts.map { it.bars })
        // A chorus coming back is the same group.
        assertEquals(parts[2].group, parts[4].group)
    }

    @Test
    fun withoutAVoiceTheLoudestPartIsADropAndBreaksHaveNoDrums() {
        val club = bars(8, 0.8f, 0.6f, 0.4f, 0f) + bars(8, 0f, 0.2f, 0.6f, 0f) + bars(8, 1f, 1f, 1f, 0f)
        assertEquals(listOf("Groove", "Break", "Drop"), SongSections.detect(energy(club)).map { it.label })
    }

    @Test
    fun neverMoreThanEightBricks() {
        val busy = (0 until 12).flatMap { bars(4, (it % 4) / 3f, (it % 3) / 2f, (it % 5) / 4f, (it % 2).toFloat()) }
        assert(SongSections.detect(energy(busy)).size <= SongSections.MAX)
    }

    @Test
    fun thePartsBecomeBricksAndTheSongsStructure() {
        val p = Project(tempoBpm = 120.0, remix = Remix("boys", "Boys", songBpm = 120.0, seconds = 80.0)).withRemixPlaced()
        val made = p.withSongSections(SongSections.detect(energy(song)))!!
        assertEquals(listOf("Intro", "Couplet", "Refrain", "Fin"), made.allScenes.take(4).map { it.label })
        assertEquals(listOf(0, 1, 2, 1, 2, 3), made.song.map { it.scene })
        assertEquals(listOf(4, 8, 8, 8, 8, 4), made.song.map { it.bars })
        // The song comes in on brique A, from its first part.
        assertEquals(0, made.remix.atStep)
        assertEquals(40 * 16, made.song.sumOf { made.partSteps(it) })
        // A song of the user's own is never rewritten.
        val mine = p.copy(song = listOf(SongPart(0), SongPart(0)))
        assertNull(mine.withSongSections(SongSections.detect(energy(song))))
    }
}
