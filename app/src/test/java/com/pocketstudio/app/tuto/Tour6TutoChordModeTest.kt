package com.pocketstudio.app.tuto

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pocketstudio.app.synth.PianoRoll
import com.pocketstudio.app.synth.PianoRollActions
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * Tour 6, control seats: « Encore 9 notes » over a step of chords (three chords to touch, « Accords » left off: the
 * line counted the notes, a touch wrote one note of a chord); and the review of lot « tuto-flèches » (PianoRoll
 * 220-221): « Accords » turned on by the user went off again when the roll came back on the same step.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w392dp-h852dp")
class Tour6TutoChordModeTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val t = NoteClip.TICKS_PER_STEP

    @After
    fun end() = TutoHints.reset()

    private fun idle() = repeat(6) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50)); compose.waitForIdle() }

    private fun text(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().config[SemanticsProperties.Text].joinToString()

    @Test
    fun aStepOfChordsTurnsAccordsOnAndCountsTheChords() {
        TutoHints.active = true
        TutoHints.step = "pad_do"
        val chords = listOf(0, 8).flatMap { s -> listOf(60, 64, 67).map { Note(s * t, 2 * t, it) } }
        compose.setContent {
            PocketStudioTheme { Surface(color = Neon.Background) { PianoRoll(NoteClip(steps = 16), emptyList(), -1, PianoRollActions(), hints = chords) } }
        }
        idle()
        compose.onNodeWithTag("chord_mode", useUnmergedTree = true).assertIsSelected()
        val said = text("tuto_notes_left_text")
        assertTrue("two chords to touch, said as two touches: « $said »", said.contains("2 ") && !said.contains("6 "))
    }

    @Test
    fun accordsTurnedOnByTheUserStaysWhenTheRollComesBack() {
        TutoHints.active = true
        TutoHints.step = "lead_do"
        val line = listOf(0, 4, 8).map { Note(it * t, t, 72) }
        var shown by mutableStateOf(true)
        compose.setContent {
            val holder = rememberSaveableStateHolder()
            PocketStudioTheme {
                Surface(color = Neon.Background) {
                    if (shown) holder.SaveableStateProvider("roll") { PianoRoll(NoteClip(steps = 16), emptyList(), -1, PianoRollActions(), hints = line) }
                }
            }
        }
        idle()
        // A step of single notes begins with « Accords » off; the user turns it on.
        compose.onNodeWithTag("chord_mode", useUnmergedTree = true).assertIsNotSelected()
        compose.onNodeWithTag("chord_mode", useUnmergedTree = true).performClick()
        idle()
        compose.onNodeWithTag("chord_mode", useUnmergedTree = true).assertIsSelected()
        // Away to another screen and back to the track, the same step.
        shown = false
        idle()
        shown = true
        idle()
        compose.onNodeWithTag("chord_mode", useUnmergedTree = true).assertIsSelected()
    }
}
