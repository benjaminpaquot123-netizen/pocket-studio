package com.pocketstudio.app.synth

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.NoteClipboard
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The piano roll counts in the time signature's bars: a copy in 3/4 takes a bar of 3/4. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w480dp-h900dp")
class RollMeterTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun aBarOfThreeFourIsCopiedAsOne() {
        val t = NoteClip.TICKS_PER_STEP
        var copied: NoteClipboard? = null
        // 32 sixteenths in 3/4 (12 a bar): bars 1, 2 and the start of 3; a note on each of the first two.
        val clip = NoteClip(32, listOf(Note(0, 2 * t, 60), Note(12 * t, 2 * t, 64)))
        compose.setContent {
            PocketStudioTheme {
                PianoRoll(clip, emptyList(), playStep = -1, actions = PianoRollActions(copyNotes = { copied = it }), gridHeight = 300.dp, stepsPerBar = 12)
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("roll_bar_2").assertExists()
        compose.onNodeWithTag("roll_bar_1").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("sel_copy").performClick()
        compose.waitForIdle()
        val board = copied!!
        assertEquals(listOf(64), board.notes.map { it.pitch })
        assertEquals(1, board.fromBar)
        assertEquals(0, board.notes.single().start)
    }
}
