package com.pocketstudio.app.remix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Benjamin's n° 18 (« pas d'avancement visible sur les pistes pendant la lecture »): the stems board
 * shows where the project's ▶ is in the song; a stretch listened to alone keeps its own head.
 */
class BoardSongHeadTest {
    @Test
    fun theHeadFollowsTheSongUnlessAStretchIsHeard() {
        assertEquals(96_000L, boardHead(-1, 96_000))
        assertEquals(1_000L, boardHead(1_000, 96_000))
        assertEquals(-1L, boardHead(-1, -1))
    }

    @Test
    fun theAppGivesTheBoardTheSongPosition() {
        val root = File("src/main/java/com/pocketstudio/app/ui/AppRoot.kt").readText()
        assertTrue("StemBoardUi without the song's position", "songFrame = rhythmVm.stemPosition" in root)
        val board = File("src/main/java/com/pocketstudio/app/remix/StemBoardScreen.kt").readText()
        // Every play head of the board (ruler and the four tracks) reads the song too.
        assertEquals(0, Regex("""drawPlayHead\(work\.value\.playFrame""").findAll(board).count())
        assertTrue(Regex("""drawPlayHead\(boardHead\(""").findAll(board).count() >= 2)
    }
}
