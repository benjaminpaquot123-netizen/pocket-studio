package com.pocketstudio.app.ui

import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.Project
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.switchScene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** What the play strip says (the position) and the tempo keypad's typing, without a screen. */
class PlayStripLogicTest {
    /** A one bar, B two bars (both named); the song A, B ×2, A: bars 1 | 2-5 | 6, 6 bars. */
    private val song = Project(
        scenes = listOf(Scene("A", DrumPattern(), label = "Intro"), Scene("B", DrumPattern().copy(steps = 32), label = "Refrain")),
        song = listOf(SongPart(0), SongPart(1, 2), SongPart(0)),
    ).sanitized().switchScene(1)  // B shown

    @Test
    fun stoppedTheStripSaysWhereTheSongStarts() {
        assertEquals(PlayPosition("Intro", 1, 6), playPosition(song, playing = false, songIndex = -1, bar = -1, playFrom = 0, loopPart = false))
        assertEquals(PlayPosition("Refrain", 2, 6), playPosition(song, false, -1, -1, playFrom = 1, loopPart = false))
        assertEquals(PlayPosition("Intro", 6, 6), playPosition(song, false, -1, -1, playFrom = 2, loopPart = false))
        // A start point gone with its part: the song's start.
        assertEquals(PlayPosition("Intro", 1, 6), playPosition(song, false, -1, -1, playFrom = 7, loopPart = false))
        // « ⟳ Partie »: the scene shown, over its own bars.
        assertEquals(PlayPosition("Refrain", 1, 2, loop = true), playPosition(song, false, -1, -1, playFrom = 0, loopPart = true))
    }

    @Test
    fun playingItFollowsTheBars() {
        assertEquals(PlayPosition("Refrain", 4, 6), playPosition(song, playing = true, songIndex = 1, bar = 3, playFrom = 0, loopPart = false))
        // The bar not read yet: the part's first.
        assertEquals(PlayPosition("Refrain", 2, 6), playPosition(song, true, songIndex = 1, bar = -1, playFrom = 0, loopPart = false))
        // A scene in a loop: its own bars.
        assertEquals(PlayPosition("Refrain", 2, 2, loop = true), playPosition(song, true, songIndex = -1, bar = 1, playFrom = 0, loopPart = true))
    }

    @Test
    fun withoutASongNothingIsSaidUntilItPlays() {
        val p = Project()
        assertNull(playPosition(p, false, -1, -1, 0, false))
        assertEquals(PlayPosition("A", 1, 1, loop = true), playPosition(p, true, -1, 0, 0, false))
    }

    /** Above the grids: the part edited, and what the music does with it. */
    @Test
    fun theEditorsSayWhichPartIsEditedAndWhenItLoops() {
        // Stopped, nothing to add.
        assertEquals(PartHeaderUi("Partie : B · Refrain"), partHeader(song, playing = false, songPlaying = false, loopPart = false, armedBySelf = false))
        // The song plays and the screens follow it: a touch keeps the part.
        val following = partHeader(song, playing = true, songPlaying = true, loopPart = false, armedBySelf = false)
        assertEquals("Partie : B · Refrain", following.title)
        assertTrue(following.note!!.contains("touche la grille"))
        assertFalse(following.loop)
        // « ⟳ Partie » went on by itself (a hit touched): said, with the way back to the song.
        val armed = partHeader(song, playing = true, songPlaying = false, loopPart = true, armedBySelf = true)
        assertTrue(armed.loop)
        assertTrue(armed.note!!.contains("reprendre le morceau"))
        // « ⟳ Partie » on by hand, stopped: the part loops, ▶ plays it alone.
        val loop = partHeader(song, playing = false, songPlaying = false, loopPart = true, armedBySelf = false)
        assertTrue(loop.loop)
        assertTrue(loop.note!!.contains("⟳ Boucler"))
        // Without a song, « ⟳ Partie » means nothing: nothing said.
        assertEquals(PartHeaderUi("Partie : A"), partHeader(Project(), playing = false, songPlaying = false, loopPart = true, armedBySelf = false))
    }

    @Test
    fun theKeypadTypesATempo() {
        var d = TempoDraft()
        d = d.type(0)  // a leading zero is nothing
        assertEquals("", d.digits)
        d = d.type(1).type(2)
        assertNull(d.bpm)  // 12: too slow to play
        d = d.type(8).type(9)  // a fourth digit is ignored
        assertEquals("128", d.digits)
        assertEquals(128, d.bpm)
        assertEquals("12", d.erase().digits)
        assertNull(TempoDraft("400").bpm)
        assertEquals(300, TempoDraft("300").bpm)
    }

    @Test
    fun halfAndDoubleStayInTheTemposPlayed() {
        assertEquals(70, scaledTempo(140.0, 0.5))
        assertEquals(240, scaledTempo(120.0, 2.0))
        assertNull(scaledTempo(70.0, 0.5))  // 35 BPM
        assertNull(scaledTempo(174.0, 2.0))  // 348 BPM
    }
}
