package com.pocketstudio.app.tuto

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextLayoutResult
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
import org.robolectric.annotation.GraphicsMode
import java.time.Duration

/**
 * Tour 7, lot « fable-repar », end checks (capture `tuto_app_bass`, phone 360 dp): the line read « Encore 4 notes en
 * pointillé à to… », the arrow that tells of the hidden note cut off by « Touche ici ». The arrow is read on a phone.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-port-hdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Tour7TutoArrowInSightTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val t = NoteClip.TICKS_PER_STEP

    @After
    fun end() = TutoHints.reset()

    private fun idle() = repeat(6) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50)); compose.waitForIdle() }

    @Test
    fun theArrowOfAHiddenNoteIsNotCutOffOnAPhone() {
        TutoHints.active = true
        // Five dotted notes close together and a sixth three octaves over them, all in the first bar.
        val notes = (0 until 5).map { Note(it * 2 * t, t, 48 + it % 3) } + Note(12 * t, t, 86)
        compose.setContent {
            PocketStudioTheme { Surface(color = Neon.Background) { PianoRoll(NoteClip(steps = 16), emptyList(), -1, PianoRollActions(), hints = notes) } }
        }
        idle()
        val node = compose.onNodeWithTag("tuto_notes_left_text", useUnmergedTree = true).fetchSemanticsNode()
        val said = node.config[SemanticsProperties.Text].joinToString()
        val arrow = listOf("↑", "↓").firstOrNull { it in said }
        assertTrue("setup: the line tells of a note over or under the rows in sight: « $said »", arrow != null && "plus" !in said)
        val layouts = ArrayList<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action!!(layouts)
        val layout = layouts.first()
        val read = if (layout.isLineEllipsized(0)) layout.getLineEnd(0, visibleEnd = true) else said.length
        assertTrue("the arrow is cut off: « ${said.take(read)}… »", said.indexOf(arrow!!) < read)
    }
}
