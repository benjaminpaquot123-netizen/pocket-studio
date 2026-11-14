package com.pocketstudio.app.tuto

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import com.pocketstudio.app.synth.PianoRoll
import com.pocketstudio.app.synth.PianoRollActions
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * Tour 6, control seat (tuto Facile, chapter 7, the melody): the last dotted note appeared half off the right edge, a
 * sliver; touching it placed a parasite note beside it (six gestures to undo). A tap a hair off a dotted note the
 * tutorial still waits for must write that note, not a new one in the empty cell next to it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w392dp-h852dp")
class Tour6TutoGhostTapTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val t = NoteClip.TICKS_PER_STEP

    @After
    fun end() = TutoHints.reset()

    private fun idle() = repeat(6) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50)); compose.waitForIdle() }

    @Test
    fun aTapAHairPastADottedNoteWritesThatNoteNotAParasite() {
        TutoHints.active = true
        val ghost = Note(10 * t, t, 72)
        val added = ArrayList<Note>()
        val actions = PianoRollActions(add = { added += it }, replaceClip = { _, _ -> })
        compose.setContent {
            PocketStudioTheme {
                Surface(color = Neon.Background) {
                    PianoRoll(NoteClip(steps = 16, notes = emptyList()), emptyList(), -1, actions, hints = listOf(ghost))
                }
            }
        }
        idle()
        val key = compose.onNodeWithTag("roll_key_72", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val grid = compose.onNodeWithTag("piano_roll", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val stepPx = grid.width / 16f
        // A hair past the dotted note (its step is 10; this lands in the empty cell at step 11).
        val at = Offset(grid.left + stepPx * 11.25f, key.center.y)
        compose.onRoot().performTouchInput { click(at) }
        idle()
        assertEquals("a tap a hair past the ghost writes the ghost, not a parasite", listOf(ghost), added)
    }
}
