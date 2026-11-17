package com.pocketstudio.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 7, replay t4-26-prise-clavier red again (gesture 28: a finger on « Enregistrer ma mélodie », no take): at 200 %
 * the Guide's bar folds on two lines (tour 7, D4), the bottom bar is taller, and « Nouveau son : Vibraphone FM …
 * Annuler », floating just above it, came 40 dp over the record button. Its card ate the finger, though the button
 * showed under it. The line only says what was done: a finger on its card reaches the page under it; only its
 * « Annuler » is a button. The touches here are a finger's (a point on the screen), not a click on a node.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w392dp-h852dp-xhdpi")
class DoneBarTouchThroughTest {
    @get:Rule
    val compose = createComposeRule()

    private var presses by mutableIntStateOf(0)
    private var undos by mutableIntStateOf(0)
    private var text by mutableStateOf<String?>("Nouveau son : Vibraphone FM")

    /** A wide button, and the done line over its lower half (as at 200 % on the replay's phone). */
    private fun page() = compose.setContent {
        PocketStudioTheme {
            Box(Modifier.fillMaxSize()) {
                Button(
                    onClick = { presses++ },
                    modifier = Modifier.fillMaxWidth().height(120.dp).offset(y = 200.dp).testTag("under"),
                ) { Text("Enregistrer ma mélodie") }
                Box(Modifier.fillMaxWidth().offset(y = 260.dp).align(Alignment.TopStart)) {
                    DoneBar(text, undo = { undos++; text = null }, dismiss = { text = null })
                }
            }
        }
    }

    @Test
    fun aFingerOnTheLinesCardReachesTheButtonUnderIt() {
        page()
        val under = compose.onNodeWithTag("under").fetchSemanticsNode().boundsInRoot
        val line = compose.onNodeWithTag("done_text", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val over = under.intersect(line)
        assertTrue("precondition: the line's text $line is not over the button $under", over.width > 1f && over.height > 1f)
        compose.onRoot().performTouchInput { click(Offset(over.center.x, over.center.y)) }
        compose.waitForIdle()
        assertEquals("a finger on « Fait : … » over the button did not reach it", 1, presses)
        assertEquals(0, undos)
    }

    @Test
    fun theLinesUndoStillTakesTheFinger() {
        page()
        val undo = compose.onNodeWithTag("done_undo", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        compose.onRoot().performTouchInput { click(undo.center) }
        compose.waitForIdle()
        assertEquals("« Annuler » of « Fait : … » took no finger", 1, undos)
        assertEquals("« Annuler » let the finger through to the button", 0, presses)
    }
}
