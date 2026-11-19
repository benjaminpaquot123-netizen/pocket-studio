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
}
