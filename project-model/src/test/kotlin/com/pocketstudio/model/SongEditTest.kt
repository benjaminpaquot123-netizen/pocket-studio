package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test

class SongEditTest {
    /** Scenes A (Intro), B (Couplet) and C (Refrain), each with a kick on a different step. */
    private fun threeScenes(): Project {
        var p = Project().sanitized().copy(drums = DrumPattern().toggle(0, 0)).sanitized().renameScene(0, "Intro")
        p = p.switchScene(1).copy(drums = DrumPattern().toggle(0, 4)).sanitized().renameScene(1, "Couplet")
        p = p.switchScene(2).copy(drums = DrumPattern().toggle(0, 8)).sanitized().renameScene(2, "Refrain")
        return p.switchScene(0)
    }

    private fun Project.scenesOfSong() = song.map { it.scene }

    @Test
    fun partsMoveInsertDuplicateAndGo() {
        var p = threeScenes().copy(song = listOf(SongPart(0), SongPart(1, 2), SongPart(2, 4, setOf(SongPart.ACID)))).sanitized()
        // Move the refrain up, then back down.
        p = p.moveSongPart(2, 1)
        assertEquals(listOf(0, 2, 1), p.scenesOfSong())
        assertEquals(setOf(SongPart.ACID), p.song[1].muted)
        p = p.moveSongPart(1, 2)
        assertEquals(listOf(0, 1, 2), p.scenesOfSong())
        // Out of range or onto itself: nothing changes.
        assertSame(p, p.moveSongPart(0, 3))
        assertSame(p, p.moveSongPart(1, 1))
        // Insert a refrain after the intro (at 1), and an intro at the very end.
        p = p.insertSongPart(1, 2)
        assertEquals(listOf(0, 2, 1, 2), p.scenesOfSong())
        assertEquals(1, p.song[1].repeats)
        p = p.insertSongPart(99, 0)
        assertEquals(listOf(0, 2, 1, 2, 0), p.scenesOfSong())
        // A scene that does not exist is not added.
        assertSame(p, p.insertSongPart(0, 9))
        // Duplicate the couplet: the copy follows it, with its repeats.
        p = p.duplicateSongPart(2)
        assertEquals(listOf(0, 2, 1, 1, 2, 0), p.scenesOfSong())
        assertEquals(2, p.song[3].repeats)
        p = p.removeSongPart(0)
        assertEquals(listOf(2, 1, 1, 2, 0), p.scenesOfSong())
        // Survives saving.
        assertEquals(p.song, ProjectJson.decode(ProjectJson.encode(p)).song)
    }

    @Test
    fun theSongNeverGrowsPastItsLimit() {
        var p = threeScenes()
        repeat(SongPart.MAX_PARTS) { p = p.insertSongPart(p.song.size, it % 3) }
        assertEquals(SongPart.MAX_PARTS, p.song.size)
        assertSame(p, p.insertSongPart(0, 0))
        assertSame(p, p.duplicateSongPart(0))
    }

    @Test
    fun playbackFollowsThePartPlayingThroughEdits() {
        // A B C D, C (2) plays.
        assertEquals(1, songIndexAfterMove(2, from = 2, to = 1))  // C moved up: still C
        assertEquals(1, songIndexAfterMove(2, from = 0, to = 3))  // A to the end: B C D A
        assertEquals(1, songIndexAfterMove(2, from = 0, to = 2))  // B C A D
        assertEquals(3, songIndexAfterMove(2, from = 3, to = 0))
        assertEquals(2, songIndexAfterMove(2, from = 3, to = 3))
        assertEquals(3, songIndexAfterInsert(2, at = 2))  // inserted at its place: it moves on
        assertEquals(2, songIndexAfterInsert(2, at = 3))  // inserted right after it: comes next
    }

    @Test
    fun theSongEndsOrStartsOver() {
        assertEquals(1, songNextIndex(playing = 0, queued = -1, size = 3, loops = false))
        assertEquals(-1, songNextIndex(playing = 2, queued = -1, size = 3, loops = false))
        assertEquals(0, songNextIndex(playing = 2, queued = -1, size = 3, loops = true))
        // A part chosen to come next wins, even an earlier one.
        assertEquals(0, songNextIndex(playing = 2, queued = 0, size = 3, loops = false))
        // The playing part was the last and was taken out: the song ends (or starts over).
        assertEquals(-1, songNextIndex(playing = 1, queued = 2, size = 2, loops = false))
        assertEquals(0, songNextIndex(playing = 1, queued = 2, size = 2, loops = true))
        assertEquals(-1, songNextIndex(playing = 0, queued = -1, size = 0, loops = true))
    }

    @Test
    fun aNewProjectStopsAtTheEndAndAnOldOneToo() {
        assertFalse(Project().songLoops)
        // A file written before the setting existed has no such key: it stops at the end too.
        val old = ProjectJson.encode(Project()).replace(Regex(""",\s*"songLoops":\s*false"""), "")
        assertFalse(old.contains("songLoops"))
        assertFalse(ProjectJson.decode(old).songLoops)
        assertEquals(true, ProjectJson.decode(ProjectJson.encode(Project(songLoops = true))).songLoops)
    }
}
