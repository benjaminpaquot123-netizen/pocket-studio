package com.pocketstudio.app.synth

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 6, lot 1 (T16, its piano roll part): the roll's tool chips are seen whole, French at 100 %, on a phone: « Basse en
 * fantôme » was the end of a row scrolled sideways, « Bas… » under its fading edge.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RollToolsWordsTest {
    @get:Rule
    val compose = createComposeRule()

    private fun chipIsWhole() {
        val t = NoteClip.TICKS_PER_STEP
        compose.setContent {
            PocketStudioTheme { PianoRoll(NoteClip(steps = 64), listOf(Note(0, 4 * t, 36)), -1, PianoRollActions(), trackName = "Lead") }
        }
        compose.waitForIdle()
        val chip = compose.onNodeWithText("Basse en fantôme").fetchSemanticsNode()
        val shown = chip.boundsInRoot
        val width = compose.onRoot().fetchSemanticsNode().size.width
        assertTrue("« Basse en fantôme » shown $shown of ${chip.size} (screen $width px)", shown.width >= chip.size.width - 1f && shown.right <= width + 1f)
    }

    @Test
    @Config(qualifiers = "w360dp-h780dp-port-hdpi")
    fun theBassAsAGhostChipIsWholeOnASmallPhone() = chipIsWhole()

    @Test
    @Config(qualifiers = "w392dp-h852dp")
    fun theBassAsAGhostChipIsWholeOnAPhone() = chipIsWhole()
}
