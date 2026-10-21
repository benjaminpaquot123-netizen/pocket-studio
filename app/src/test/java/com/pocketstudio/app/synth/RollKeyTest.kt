package com.pocketstudio.app.synth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.NoteClip
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The piano roll's keys never leave a note sounding (or held while ● records). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w480dp-h900dp")
class RollKeyTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun aHeldKeyStopsItsNoteWhenTheRollLeaves() {
        val on = mutableListOf<Int>()
        val off = mutableListOf<Int>()
        var shown by mutableStateOf(true)
        compose.setContent {
            PocketStudioTheme {
                if (shown) {
                    PianoRoll(
                        NoteClip(16, emptyList()), emptyList(), playStep = -1, actions = PianoRollActions(), gridHeight = 300.dp,
                        keyOn = { p, _ -> on += p }, keyOff = { off += it },
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("roll_key_74", useUnmergedTree = true).performTouchInput { down(center) }
        compose.waitForIdle()
        assertEquals(listOf(74), on)
        assertEquals(emptyList<Int>(), off)
        // Another tab (or a rotation) takes the roll away while the finger is still down.
        shown = false
        compose.waitForIdle()
        assertEquals("the held note is released", listOf(74), off)
    }

    @Test
    fun theKeysCallTheLatestActions() {
        val first = mutableListOf<Int>()
        val second = mutableListOf<Int>()
        var useSecond by mutableStateOf(false)
        compose.setContent {
            PocketStudioTheme {
                val sink = if (useSecond) second else first
                PianoRoll(
                    NoteClip(16, emptyList()), emptyList(), playStep = -1, actions = PianoRollActions(), gridHeight = 300.dp,
                    keyOn = { p, _ -> sink += p }, keyOff = { sink += -it },
                )
            }
        }
        compose.waitForIdle()
        val key = compose.onNodeWithTag("roll_key_74", useUnmergedTree = true)
        key.performTouchInput { down(center); up() }
        compose.waitForIdle()
        // Chord keys toggled (new actions): the next touch plays with them, not with the first pair.
        useSecond = true
        compose.waitForIdle()
        key.performTouchInput { down(center); up() }
        compose.waitForIdle()
        assertEquals(listOf(74, -74), first)
        assertEquals(listOf(74, -74), second)
    }

    @Test
    fun aHeldKeyStopsItsNoteWhenTheScreenIsRebuilt() {
        val on = mutableListOf<Int>()
        val off = mutableListOf<Int>()
        compose.setContent {
            PocketStudioTheme {
                PianoRoll(
                    NoteClip(16, emptyList()), emptyList(), playStep = -1, actions = PianoRollActions(), gridHeight = 300.dp,
                    keyOn = { p, _ -> on += p }, keyOff = { off += it },
                )
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("roll_key_74", useUnmergedTree = true).performTouchInput { down(center) }
        compose.waitForIdle()
        assertEquals(listOf(74), on)
        // A rotation rebuilds the activity while the finger is still down.
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        assertEquals("the held note is released", listOf(74), off)
    }
}
