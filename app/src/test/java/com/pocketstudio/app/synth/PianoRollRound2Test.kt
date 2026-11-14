package com.pocketstudio.app.synth

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * Audit ultra round 2, n° 27 (seat B c, screen 71): a long press on a note deleted it without a word
 * (4 notes, then 3; the producer's reflex was a menu to copy it). It chooses the note and opens its
 * menu: « Copier la note », « Dupliquer », « Supprimer la note »; nothing goes without being asked.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w392dp-h852dp")
class PianoRollRound2Test {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val t = NoteClip.TICKS_PER_STEP

    private fun idle() = repeat(6) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50)); compose.waitForIdle() }
    private fun has(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun aLongPressOnANoteOpensItsMenuAndDeletesNothing() {
        var clip by mutableStateOf(NoteClip(steps = 16, notes = listOf(Note(0, 4 * t, 72), Note(8 * t, 4 * t, 72))))
        val removed = ArrayList<Note>()
        val actions = PianoRollActions(
            remove = { n -> removed += n; clip = clip.copy(notes = clip.notes - n) },
            replaceClip = { c, _ -> clip = c },
        )
        compose.setContent {
            PocketStudioTheme { androidx.compose.material3.Surface(color = com.pocketstudio.ui.Neon.Background) { PianoRoll(clip, emptyList(), -1, actions) } }
        }
        idle()
        val key = compose.onNodeWithTag("roll_key_72", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val grid = compose.onNodeWithTag("piano_roll", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val stepPx = grid.width / 16f
        // On the first note (its first step).
        val at = Offset(grid.left + stepPx * 0.8f, key.center.y)
        compose.onRoot().performTouchInput { longClick(at, durationMillis = 900) }
        idle()
        assertEquals("a note deleted by a long press, without a word", 2, clip.notes.size)
        assertTrue("removed: $removed", removed.isEmpty())
        assertTrue("the note's menu", has("roll_note_menu") || has("roll_note_delete"))
        assertTrue("« Copier »", has("roll_note_copy"))
        // « Supprimer la note », asked in words: now it goes (one note only).
        compose.onNodeWithTag("roll_note_delete").performClick()
        idle()
        assertEquals(listOf(Note(8 * t, 4 * t, 72)), clip.notes)
    }
}
