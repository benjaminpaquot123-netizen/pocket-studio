package com.pocketstudio.app.synth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Tour 9, lot L19: what L7's fresh review left of F112 and F118 on the piano roll. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-port-hdpi")
class Tour9L19RollTest {
    @get:Rule val compose = createComposeRule()

    /**
     * F118 (L7's review): bar 1 copied, « Coller » touched again and again fills bars 2, 3, 4… even once the next bar
     * is out of sight (it fell back on the bar in the middle of the roll and pasted bar 1 on itself).
     */
    @Test
    fun repeatedPastesFillTheNextBarsEvenOutOfSight() {
        val bar = 16 * NoteClip.TICKS_PER_STEP
        var clip by mutableStateOf(NoteClip(steps = 16, notes = listOf(Note(0, bar / 4, 60))))
        compose.setContent {
            PocketStudioTheme {
                PianoRoll(clip, emptyList(), -1, PianoRollActions(replaceClip = { c, _ -> clip = c }, setSteps = { clip = clip.copy(steps = it) }),
                    gridHeight = 300.dp)
            }
        }
        compose.onNodeWithTag("roll_bar_0").performClick()
        compose.onNodeWithTag("sel_copy").performScrollTo().performClick()
        repeat(6) {
            compose.onNodeWithTag("sel_paste").performScrollTo().performClick()
            compose.waitForIdle()
        }
        val bars = clip.notes.map { it.start / bar }.sorted()
        assertEquals("bar 1 and the six bars after it, one note each", (0..6).toList(), bars)
    }

    /**
     * F112 (L7's review): a drag up that runs the roll to its end does not carry the page (the header stays), but a new
     * drag up on a roll already at its end still reaches what is under it (« Gamme et accords », the sound); a drag
     * down brings the header back.
     */
    @Test
    fun aFreshDragUpOnARollAtItsEndStillReachesWhatIsUnder() {
        val page = androidx.compose.foundation.ScrollState(0)
        compose.setContent {
            PocketStudioTheme {
                Column(Modifier.height(700.dp).verticalScroll(page)) {
                    PianoRoll(NoteClip(steps = 64), emptyList(), -1, PianoRollActions(), gridHeight = 300.dp, trackName = "Mélodie")
                    Box(Modifier.height(600.dp))
                }
            }
        }
        compose.waitForIdle()
        val top = compose.onNodeWithTag("roll_bar_0").fetchSemanticsNode().boundsInRoot.bottom
        val dp = compose.onRoot().fetchSemanticsNode().layoutInfo.density.density
        fun drag(from: Float, to: Float) = compose.onRoot().performTouchInput {
            swipe(Offset(18 * dp, top + from * dp), Offset(18 * dp, top + to * dp), 200)
        }
        drag(250f, 30f)
        compose.waitForIdle()
        assertEquals("a drag on a roll far from its end leaves the page", 0, page.value)
        repeat(30) { if (page.value == 0) drag(250f, 30f) }
        compose.waitForIdle()
        assertTrue("a fresh drag on a roll at its end scrolls the page to what is under the roll", page.value > 0)
    }
}
