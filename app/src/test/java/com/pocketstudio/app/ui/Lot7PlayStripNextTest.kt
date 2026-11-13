package com.pocketstudio.app.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.Project
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongPart
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 6 lot 7 (U1, its PlayStrip part; seat A a #7): « Jouer depuis… » while the song plays only queues the part (it
 * comes at the end of the part playing), and the strip kept saying the part playing: only the list's « next » line said
 * it, out of sight. The position carries the part queued and the strip says « B : ensuite ».
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-port-hdpi")
class Lot7PlayStripNextTest {
    @get:Rule
    val compose = createComposeRule()

    private val song = Project(
        scenes = listOf(Scene("A", DrumPattern().toggle(0, 0), label = "Intro"), Scene("B", DrumPattern().toggle(1, 4), label = "Refrain")),
        song = listOf(SongPart(0), SongPart(1)),
    ).sanitized()

    @Test
    fun theQueuedPartIsInThePosition() {
        val pos = playPosition(song, playing = true, songIndex = 0, bar = 0, playFrom = 0, loopPart = false, queued = 1)
        assertEquals(partName(song, song.song[1].scene), pos?.next)
        // Nothing queued, or the part playing chosen again: nothing to say.
        assertEquals(null, playPosition(song, playing = true, songIndex = 0, bar = 0, playFrom = 0, loopPart = false)?.next)
        assertEquals(null, playPosition(song, playing = true, songIndex = 0, bar = 0, playFrom = 0, loopPart = false, queued = 0)?.next)
    }

    @Test
    fun theStripSaysThePartComingNext() {
        val pos = playPosition(song, playing = true, songIndex = 0, bar = 0, playFrom = 0, loopPart = false, queued = 1)
        compose.setContent {
            PocketStudioTheme { PlayStrip(PlayStripUi(120.0, hasSong = true, loopPart = false, playing = true, position = { pos }, parts = listOf("A", "B")), PlayStripActions()) }
        }
        compose.waitForIdle()
        val said = "${partName(song, song.song[1].scene)} : ensuite"
        assertTrue("« $said » in the strip", compose.onAllNodesWithText(said, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
    }
}
