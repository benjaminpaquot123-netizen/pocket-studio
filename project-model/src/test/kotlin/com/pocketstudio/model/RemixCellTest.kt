package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Lot D2 (« case Remix du Live »): a passage of the song per brick of the remix column. */
class RemixCellTest {
    private fun project(): Project = Project(
        scenes = listOf(Scene("A", drums = DrumPattern().toggle(0, 0)), Scene("B", drums = DrumPattern().toggle(1, 0)), Scene("C")),
        scene = 0, drums = DrumPattern().toggle(0, 0),
        remix = Remix(setId = "boys", name = "Boys", songBpm = 113.0, seconds = 180.0),
    ).sanitized()

    private val voice = RemixCell(1, "remix_voix.wav", "Voix", bars = 2, bpm = 113.0, stem = 3, from = 42.5)

    @Test
    fun aBrickHoldsOnePassageAndTheOthersPlayTheSong() {
        val p = project().copy(remix = project().remix.withCell(1, voice)).sanitized()
        assertEquals(voice, p.remix.cell(1))
        assertNull(p.remix.cell(0))
        assertEquals(1, p.remixCellOf(1))
        assertEquals(-1, p.remixCellOf(0))  // the whole song
        assertEquals(0, p.firstFreeRemixCell())
        // Another passage on the same brick replaces it; null gives the song back.
        val drums = voice.copy(name = "Batterie", stem = 0, file = "remix_bat.wav")
        val q = p.copy(remix = p.remix.withCell(1, drums))
        assertEquals(listOf(drums), q.remix.cells)
        assertTrue(q.copy(remix = q.remix.withCell(1, null)).remix.cells.isEmpty())
        // Its loop: whole bars of the project.
        assertEquals(32, p.slotLoopSteps(SongPart.REMIX, 1))
        // Without a song, nothing plays it.
        assertEquals(-1, p.copy(remix = p.remix.copy(setId = null)).remixCellOf(1))
    }

    @Test
    fun barsAreWholeAndBounded() {
        assertEquals(4, RemixCell.barsFor(4 * 4 * 60 / 113.0, 113.0, 4))
        assertEquals(2, RemixCell.barsFor(2.3 * 4 * 60 / 113.0, 113.0, 4))
        assertEquals(1, RemixCell.barsFor(0.5, 113.0, 4))
        assertEquals(1, RemixCell.barsFor(10.0, 0.0, 4))
        assertEquals(RemixCell.MAX_BARS, RemixCell.barsFor(600.0, 120.0, 4))
        assertEquals(96_000L, RemixCell.framesFor(1, 120.0, 4, 48_000))
    }

    @Test
    fun badCellsAreDroppedAndTheFileStaysSmall() {
        val bad = listOf(voice, voice.copy(scene = 99), voice.copy(scene = 2, file = "../x.wav"), voice.copy(name = "doublon"), voice.copy(scene = 0, bars = 500, bpm = Double.NaN))
        val r = Remix(setId = "boys", cells = bad).sanitized()
        assertEquals(listOf(0, 1), r.cells.map { it.scene })
        assertEquals(RemixCell.MAX_BARS, r.cells[0].bars)
        assertEquals(0.0, r.cells[0].bpm, 0.0)
        // Saved and read back; left out of the file while empty.
        val p = project().copy(remix = project().remix.withCell(1, voice))
        val back = ProjectJson.decode(ProjectJson.encode(p))
        assertEquals(p.remix.cells, back.remix.cells)
        assertFalse("\"cells\"" in ProjectJson.encode(project()))
    }

    @Test
    fun itsSoundTravelsWithTheProject() {
        val p = project().copy(remix = project().remix.withCell(1, voice))
        assertTrue("remix_voix.wav" in p.sampleFiles)
        assertTrue(p.withoutSounds(setOf("remix_voix.wav")).remix.cells.isEmpty())
    }

    @Test
    fun aTakeWritesThePassageTheRemixPlayed() {
        val p = project().copy(remix = project().remix.withCell(1, voice)).sanitized()
        val take = PerformanceTake(0, LiveState.on(0, 0))
        take.launched(LiveSlots.REMIX, 1, 16)  // the remix on brick B's passage at bar 2, the row stays A
        take.launched(LiveSlots.REMIX, -1, 48) // stopped at bar 4
        val w = p.writeTake(take, 64, append = false)
        val song = w.project.song
        assertEquals(3, song.size)
        assertEquals(SongPart(0, 1), song[0])
        // Bars 2 and 3: the passage (two bars, from its start), then the remix silent.
        assertEquals(1, song[1].sceneFor(SongPart.REMIX))
        assertEquals(2, song[1].bars ?: song[1].repeats)
        assertEquals(0, song[1].fromBars(SongPart.REMIX))
        assertFalse(song[2].plays(SongPart.REMIX))
        // Kept when the project is saved and read again.
        val back = ProjectJson.decode(ProjectJson.encode(w.project)).sanitized()
        assertEquals(1, back.song[1].sceneFor(SongPart.REMIX))
        // A passage still playing on into a next part carries on where it was (« from »).
        val t2 = PerformanceTake(0, LiveState.on(0, 0))
        t2.launched(LiveSlots.REMIX, 1, 0)
        t2.launched(LiveSlots.DRUMS, 1, 48)  // the drums change at bar 4: the passage is in its 2nd pass, bar 2
        val s2 = p.writeTake(t2, 64, append = false).project.song
        assertEquals(2, s2.size)
        assertEquals(1, s2[1].sceneFor(SongPart.REMIX))
        assertEquals(1, s2[1].fromBars(SongPart.REMIX))
    }
}
