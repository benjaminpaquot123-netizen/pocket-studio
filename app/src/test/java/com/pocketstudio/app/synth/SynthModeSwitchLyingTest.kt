package com.pocketstudio.app.synth

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pocketstudio.ui.PocketStudioTheme
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.Project
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A small phone lying down (the CI emulator, PR #50): the « Clavier / Notes » switch shared its row
 * with the chords' chips and the octave, weighted to what they left; that was nothing, and « Notes »
 * could not be touched.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w640dp-h320dp")
class SynthModeSwitchLyingTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun notesCanBeTouchedOnASmallPhoneLyingDown() {
        compose.setContent {
            PocketStudioTheme {
                SynthScreen(Project().synth, 4, SynthActions(), notes = NotesUi(NoteClip(16, listOf(Note(0, 24, 60))), emptyList(), -1, PianoRollActions()))
            }
        }
        val notes = compose.onNodeWithTag("mode_notes").fetchSemanticsNode()
        assertTrue("« Notes » is ${notes.size.width} px wide", notes.size.width > 0)
        compose.onNodeWithTag("mode_notes").performClick()
        compose.waitForIdle()
        assertTrue("the piano roll opens", compose.onAllNodes(androidx.compose.ui.test.hasTestTag("piano_roll")).fetchSemanticsNodes().isNotEmpty())
    }
}
