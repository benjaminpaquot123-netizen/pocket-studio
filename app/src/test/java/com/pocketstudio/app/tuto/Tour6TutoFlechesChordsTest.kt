package com.pocketstudio.app.tuto

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pocketstudio.app.synth.PianoRoll
import com.pocketstudio.app.synth.PianoRollActions
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Lot « tuto-flèches », control seats H a (House, chapter 7: « Accords » reste allumé sur la piste Mélodie, rien ne le
 * dit ; je l'ai éteint à l'instinct) and D a (techno, « コード » still on all through the riff): « Accords », turned on
 * for the chords' step as its words ask, stays on when the next step's dotted notes are a single line: a touch beside
 * a dotted note then writes a chord. A step whose dotted notes come one at a time begins with « Accords » off.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-port-hdpi")
class Tour6TutoFlechesChordsTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun end() {
        TutoHints.step = null
        TutoHints.active = false
    }

    private fun chordsOn(): Boolean {
        val c = compose.onNodeWithTag("chord_mode").fetchSemanticsNode().config
        return c.getOrNull(SemanticsProperties.Selected) ?: (c.getOrNull(SemanticsProperties.ToggleableState) == ToggleableState.On)
    }

    @Test
    fun aStepOfSingleNotesBeginsWithChordsOff() {
        val t = NoteClip.TICKS_PER_STEP
        // The chords' step: two chords of three dotted notes; then the riff's: one dotted note at a time.
        val chords = listOf(60, 63, 67).flatMap { p -> listOf(Note(4 * t, 2 * t, p), Note(11 * t, 2 * t, p)) }
        val riff = listOf(Note(0, 2 * t, 53), Note(4 * t, 2 * t, 65), Note(8 * t, 2 * t, 53))
        var hints by mutableStateOf(chords)
        TutoHints.active = true
        TutoHints.step = "pad_do"
        compose.setContent {
            PocketStudioTheme { PianoRoll(NoteClip(steps = 16), emptyList(), -1, PianoRollActions(), hints = hints, trackName = "Accords") }
        }
        compose.waitForIdle()
        assertEquals("« Accords » is off when the roll opens", false, chordsOn())
        // « allume « Accords », juste au-dessus de la grille » (the chords' step).
        compose.onNodeWithTag("chord_mode").performClick()
        compose.waitForIdle()
        assertEquals("the user's touch turns « Accords » on", true, chordsOn())
        // The next step, on the melody's track: its dotted notes come one at a time.
        TutoHints.step = "lead_do"
        hints = riff
        compose.waitForIdle()
        assertEquals("the riff's step begins with « Accords » off", false, chordsOn())
        // The user's own choice during that step is kept.
        compose.onNodeWithTag("chord_mode").performClick()
        compose.waitForIdle()
        assertEquals("turned on again by the user, it stays", true, chordsOn())
    }

    /** The chords' own step never turns it off: its dotted notes come three at a time. */
    @Test
    fun theChordsStepKeepsChordsOn() {
        val t = NoteClip.TICKS_PER_STEP
        val chords = listOf(60, 63, 67).flatMap { p -> listOf(Note(4 * t, 2 * t, p), Note(11 * t, 2 * t, p)) }
        val next = listOf(61, 65, 68).flatMap { p -> listOf(Note(52 * t, 2 * t, p), Note(59 * t, 2 * t, p)) }
        var hints by mutableStateOf(chords)
        TutoHints.active = true
        TutoHints.step = "pad_do"
        compose.setContent {
            PocketStudioTheme { PianoRoll(NoteClip(steps = 64), emptyList(), -1, PianoRollActions(), hints = hints, trackName = "Accords") }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("chord_mode").performClick()
        compose.waitForIdle()
        TutoHints.step = "pad_end"
        hints = chords + next
        compose.waitForIdle()
        assertEquals("the chords' second step keeps « Accords » on", true, chordsOn())
    }
}
