package com.pocketstudio.app.tuto

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
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
 * Control seats Ha-3/Da-3 (« Encore 9 notes » over three chords): with « Accords » on, one touch writes a whole dotted
 * chord and the line counts touches; it said « notes » all the same. It says « accords » then.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w392dp-h852dp")
class Tour6TutoChordWordsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val t = NoteClip.TICKS_PER_STEP

    @After
    fun end() = TutoHints.reset()

    private fun idle() = repeat(6) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50)); compose.waitForIdle() }

    @Test
    fun withAccordsOnTheLineSaysChordsNotNotes() {
        TutoHints.active = true
        val chords = listOf(0, 4, 8).flatMap { s -> listOf(60, 64, 67).map { Note(s * t, 2 * t, it) } }
        compose.setContent {
            PocketStudioTheme { Surface(color = Neon.Background) { PianoRoll(NoteClip(steps = 16), emptyList(), -1, PianoRollActions(), hints = chords) } }
        }
        idle()
        compose.onNodeWithTag("chord_mode", useUnmergedTree = true).performClick()
        idle()
        val said = compose.onNodeWithTag("tuto_notes_left_text", useUnmergedTree = true).fetchSemanticsNode().config[SemanticsProperties.Text].joinToString()
        assertTrue("three chords, said as chords: « $said »", said.contains("3") && said.contains("ccord") && !said.contains("note"))
    }
}
