package com.pocketstudio.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 9, F111 (Structure and Rythme): « Fait : nouvelle longueur pour X · Annuler » still showed chapters later.
 * Acceptance: « Le message « nouvelle longueur » disparaît après quelques secondes ou au geste suivant. »
 * The line sits in the app's Scaffold (its snackbar place) over a page: the next gesture anywhere on the page lets it
 * go, the gesture still doing its work; its own « Annuler » still undoes; and it goes by itself a few seconds later.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h640dp")
class Tour9DoneBarGoesTest {
    @get:Rule val compose = createComposeRule()

    private var presses by mutableIntStateOf(0)
    private var undos by mutableIntStateOf(0)
    private var dismissed by mutableIntStateOf(0)
    private var text by mutableStateOf<String?>("Fait : nouvelle longueur pour B · Couplet")

    /** As in AppRoot: the page in a Scaffold, the line in its snackbar place. */
    private fun app() = compose.setContent {
        PocketStudioTheme {
            Scaffold(snackbarHost = { DoneBar(text, undo = { undos++; text = null }, dismiss = { dismissed++; text = null }) }) { pad ->
                Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).testTag("page")) {
                    Button(onClick = { presses++ }, modifier = Modifier.fillMaxWidth().height(80.dp).testTag("part_length")) { Text("8 mesures") }
                    Box(Modifier.height(1200.dp))
                }
            }
        }
    }

    @Test
    fun theNextTouchOnThePageLetsTheLineGoAndStillWorks() {
        app()
        compose.onNodeWithTag("done_bar").assertExists()
        compose.onNodeWithTag("part_length").performClick()
        compose.waitForIdle()
        assertEquals("the touch did not reach the page", 1, presses)
        assertNull("« ${text} » still shows after the next gesture", text)
        compose.onNodeWithTag("done_bar").assertDoesNotExist()
    }

    @Test
    fun aScrollOfThePageLetsTheLineGo() {
        app()
        compose.onNodeWithTag("page").performTouchInput { swipeUp() }
        compose.waitForIdle()
        assertNull("« ${text} » still shows after a scroll", text)
    }

    @Test
    fun itsUndoStillUndoes() {
        app()
        compose.onNodeWithTag("done_undo").performClick()
        compose.waitForIdle()
        assertEquals(1, undos)
        assertEquals("« Annuler » counted as a gesture elsewhere", 0, dismissed)
    }

    @Test
    fun itGoesByItselfAFewSecondsLater() {
        compose.mainClock.autoAdvance = false
        app()
        compose.mainClock.advanceTimeBy(3_000)
        assertEquals("it went too soon", "Fait : nouvelle longueur pour B · Couplet", text)
        compose.mainClock.advanceTimeBy(4_000)
        assertNull("« ${text} » still shows 7 s later", text)
    }
}
