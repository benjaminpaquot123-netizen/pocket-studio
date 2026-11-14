package com.pocketstudio.app.ui

import com.pocketstudio.app.report.reportPosition
import com.pocketstudio.app.rhythm.SessionUi
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.Project
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongPart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 6 lot 8 (U1, its wiring; seat A a #7): lot 7 taught the position the part queued by « Jouer depuis… » while the
 * song plays, but its two callers never passed it: the strip under the top bar and the bug report kept saying only the
 * part playing. Both pass [SessionUi.queued] now.
 */
class Lot8QueuedWiringTest {
    private val song = Project(
        scenes = listOf(Scene("A", DrumPattern().toggle(0, 0), label = "Intro"), Scene("B", DrumPattern().toggle(1, 4), label = "Refrain")),
        song = listOf(SongPart(0), SongPart(1)),
    ).sanitized()

    private val queuedB = SessionUi(songIndex = 0, queued = 1)

    @Test
    fun theStripUnderTheTopBarSaysThePartQueued() {
        val pos = stripPosition(song, playing = true, session = queuedB, bar = 0, playFrom = 0, loopPart = false)
        assertEquals(partName(song, song.song[1].scene), pos?.next)
    }

    @Test
    fun theBugReportSaysThePartQueued() {
        val said = reportPosition(song, playing = true, session = queuedB, bar = 0, playFrom = 0, loopPart = false)
        assertTrue("the report's position: $said", said != null && said.endsWith("ensuite ${partName(song, song.song[1].scene)}"))
    }
}
