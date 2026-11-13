package com.pocketstudio.app.session

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.Scene
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 6, N14 (seats C b #19 and E c #27): « Ton morceau est vide » was said while the loop played and right after the
 * voice was put on a new audio track: the order of parts was empty, the music was not. The card says the song is empty
 * only when no part holds any music.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h914dp-xxhdpi")
class SongCardEmptyWordsTest {
    @get:Rule
    val compose = createComposeRule()

    private val empty = "Ton morceau est vide : ajoute tes parties dans l'ordre où elles jouent (Intro, Couplet, Refrain…)."

    private fun show(scenes: List<Scene?>, playing: Boolean) {
        compose.setContent {
            PocketStudioTheme {
                SessionScreen(
                    SessionScreenUi(scenes = scenes, current = 0, pending = null, song = emptyList(), songIndex = -1, playing = playing, tempoBpm = 120.0),
                    SessionActions(),
                )
            }
        }
        compose.waitForIdle()
    }

    private fun saysEmpty() = compose.onAllNodesWithText(empty).fetchSemanticsNodes().size

    @Test
    fun aPartWithABeatPlayingInALoopIsNotAnEmptySong() {
        show(listOf(Scene("A", DrumPattern().toggle(0, 0))), playing = true)
        assertEquals("a beat plays in part A: the song is not « vide »", 0, saysEmpty())
    }

    @Test
    fun noMusicAnywhereIsAnEmptySong() {
        show(listOf(Scene("A")), playing = false)
        assertEquals(1, saysEmpty())
    }
}
