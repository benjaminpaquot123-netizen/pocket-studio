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
 * Control seat Da-5 (ja, Facile, chapter 7, on 307759b3): the dotted notes left out of sight, the line said « この先に → »
 * and « もっと上 ↑ »; the seat swiped ten times by hand, never knowing a touch on the line brings the note into view
 * (Da-4, who touched it, needed three gestures). While the line points away, it says that a touch takes you there.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-port-hdpi")
class Tour6TutoLineGoTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val t = NoteClip.TICKS_PER_STEP

    @After
    fun end() = TutoHints.reset()

    private fun idle() = repeat(6) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50)); compose.waitForIdle() }

    @Test
    fun aLinePointingAwaySaysATouchTakesYouThere() {
        TutoHints.active = true
        // One dotted note far to the right, in the last bar of four.
        val far = Note(60 * t, t, 72)
        compose.setContent {
            PocketStudioTheme { Surface(color = Neon.Background) { PianoRoll(NoteClip(steps = 64), emptyList(), -1, PianoRollActions(), hints = listOf(far)) } }
        }
        idle()
        val said = compose.onNodeWithTag("tuto_notes_left_text", useUnmergedTree = true).fetchSemanticsNode().config[SemanticsProperties.Text].joinToString()
        assertTrue("setup: the line points away: « $said »", listOf("↑", "↓", "→", "←").any { it in said })
        val go = compose.onAllNodesWithTag("tuto_notes_left_go", useUnmergedTree = true).fetchSemanticsNodes()
        assertTrue("the line says a touch takes you there (« Touche ici »)", go.isNotEmpty() && go.first().config[SemanticsProperties.Text].joinToString() == "Touche ici")
    }
}
