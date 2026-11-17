package com.pocketstudio.app.tuto

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * Tour 7, D4 (seat 1a, the chords' step): « Allume « Accords » » was one more gesture in a Facile consigne already
 * long. A step whose dotted notes are chords begins with « Accords » on; turned off by the user, it stays off.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class Tour7TutoChordOnTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val t = NoteClip.TICKS_PER_STEP

    @After
    fun end() = TutoHints.reset()

    private fun idle() = repeat(6) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50)); compose.waitForIdle() }

    @Test
    fun aStepOfDottedChordsBeginsWithAccordsOn() {
        TutoHints.active = true
        TutoHints.step = "pad_do"
        val chords = listOf(0, 6, 12).flatMap { s -> listOf(57, 60, 64).map { Note(s * t, 2 * t, it) } }
        var shown by mutableStateOf(true)
        compose.setContent {
            val holder = rememberSaveableStateHolder()
            PocketStudioTheme {
                Surface(color = Neon.Background) {
                    if (shown) holder.SaveableStateProvider("roll") { PianoRoll(NoteClip(steps = 16), emptyList(), -1, PianoRollActions(), hints = chords) }
                }
            }
        }
        idle()
        compose.onNodeWithTag("chord_mode", useUnmergedTree = true).assertIsSelected()
        // Turned off by the user: away and back on the same step, it stays off.
        compose.onNodeWithTag("chord_mode", useUnmergedTree = true).performClick()
        idle()
        shown = false
        idle()
        shown = true
        idle()
        compose.onNodeWithTag("chord_mode", useUnmergedTree = true).assertIsNotSelected()
    }
}
