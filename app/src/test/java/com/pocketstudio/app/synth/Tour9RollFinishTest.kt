package com.pocketstudio.app.synth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.Project
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Tour 9, lot L7, second pass: the piano roll's and the Clavier's finishes (F112 to F118, F142). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-port-hdpi")
class Tour9RollFinishTest {
    @get:Rule val compose = createComposeRule()

    private fun bigLetters(content: @Composable () -> Unit) = compose.setContent {
        val d = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(d.density, 2f)) { PocketStudioTheme { content() } }
    }

    /** F142: « Après un enregistrement, l'espacement du synthé revient à la normale. » */
    @Test
    fun theKeysGetTheirHeightBackOnceTheTakesCardIsGone() {
        val take = mutableStateOf<(@Composable (Boolean) -> Unit)?>(null)
        bigLetters { SynthScreen(Project().synth, 4, SynthActions(), simple = true, record = take.value) }
        compose.waitForIdle()
        val before = compose.onNodeWithTag("synth_keys_first").fetchSemanticsNode().size.height
        take.value = { Box(Modifier.fillMaxWidth().height(720.dp)) }
        compose.waitForIdle()
        val during = compose.onNodeWithTag("synth_keys_first").fetchSemanticsNode().size.height
        take.value = null
        compose.waitForIdle()
        val after = compose.onNodeWithTag("synth_keys_first").fetchSemanticsNode().size.height
        assertEquals("the keys, before a take ($before px) and during it ($during px)", true, during < before)
        assertEquals("the keys once the take's card is gone", before, after)
    }

    /** F114: « Une piste mélodique s'ouvre sans basse fantôme sélectionnée. » */
    @Test
    fun aMelodyTrackOpensWithoutTheGhostBassTicked() {
        val t = NoteClip.TICKS_PER_STEP
        compose.setContent {
            PocketStudioTheme { PianoRoll(NoteClip(steps = 64), listOf(Note(0, 4 * t, 36)), -1, PianoRollActions(), trackName = "Mélodie") }
        }
        compose.onNodeWithText("Basse en fantôme").assertIsNotSelected()
    }

    /**
     * F113: « La note mise en évidence est celle que la consigne demande de toucher. » A chords step asks for « the
     * bottom dotted note of each chord » (a touch there writes the whole chord): the note lit is the chord's bottom one.
     */
    @Test
    fun theNoteLitInAChordIsItsBottomOne() {
        val t = NoteClip.TICKS_PER_STEP
        val chords = listOf(0, 16).flatMap { s -> listOf(56, 60, 63).map { p -> Note(s * t, 8 * t, p) } }
            .sortedWith(compareBy({ it.start }, { -it.pitch }))
        val view = com.pocketstudio.model.tuto.TutoRollView(0f, 1000f, 0f, 2000f, 20f, 20f, 100)
        val asked = aimedNotes(chords, chordMode = true)
        val lit = com.pocketstudio.model.tuto.TutoRollHint.lit(asked, view, 16)
        assertEquals("the chord's bottom note", Note(0, 8 * t, 56), lit)
        assertEquals("the next one, the next chord's bottom", listOf(56, 56), asked.map { it.pitch })
        // Without the chords' mode each note is touched: they all stay asked for.
        assertEquals(chords, aimedNotes(chords, chordMode = false))
    }
    /**
     * F118 (seat t9-27, id): the tutorial's motif copied over bars 1 to 5, the roll on bar 2, the button read « Tempel
     * birama 6 » (bar 6, out of sight). A paste goes on the bar chosen or after a copy while it is in sight, else on
     * the bar the eye is on (the middle of the roll), and on the bar playing while it plays.
     */
    @Test
    fun thePasteGoesOnABarInSight() {
        val bar = 300f
        // Bar 2 in sight (from 300 to 600 px), the target after the copy is bar 6 (index 5): the paste goes on bar 2.
        assertEquals(1, pasteBarFor(playBar = -1, targetBar = 5, left = 300f, right = 600f, barPx = bar, lastBar = 7))
        // The target in sight is kept (copy bar 1, paste on bar 2 next to it).
        assertEquals(1, pasteBarFor(playBar = -1, targetBar = 1, left = 0f, right = 700f, barPx = bar, lastBar = 7))
        // Playing: the bar playing, whatever is in sight.
        assertEquals(4, pasteBarFor(playBar = 4, targetBar = 1, left = 0f, right = 700f, barPx = bar, lastBar = 7))
        // Nothing laid out yet (no width): the target as before.
        assertEquals(5, pasteBarFor(playBar = -1, targetBar = 5, left = 0f, right = 0f, barPx = bar, lastBar = 7))
        // Never past the last bar a clip can hold.
        assertEquals(7, pasteBarFor(playBar = -1, targetBar = 9, left = 2100f, right = 2400f, barPx = bar, lastBar = 7))
    }
}
