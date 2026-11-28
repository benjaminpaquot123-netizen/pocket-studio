package com.pocketstudio.app.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 9, F119: « La feuille Tempo reste en place pendant la saisie, et la valeur choisie est marquée (pas la puce
 * « 120 ») ». Typing 1, 2, 8 at 120 BPM: no key moves, and « 120 » is not marked while 128 is typed (it is when 120 is).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class TempoTypingStaysTest {
    @get:Rule
    val compose = createComposeRule()

    private fun top(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInWindow.top

    private fun marked(tag: String) = compose.onNodeWithTag(tag).fetchSemanticsNode().config.getOrNull(SemanticsProperties.Selected) == true

    @Test
    fun theKeypadStaysAndOnlyTheTypedTempoIsMarked() {
        compose.setContent { PocketStudioTheme { TempoPad(120.0, TempoActions(), close = {}) } }
        compose.waitForIdle()
        assertTrue("« 120 » marks the tempo before typing", marked("tempo_preset_120"))
        val keys = listOf("tempo_key_1", "tempo_key_0", "tempo_key_ok", "tempo_tap")
        val before = keys.map(::top)
        listOf("1", "2", "8").forEachIndexed { i, d ->
            compose.onNodeWithTag("tempo_key_$d").performClick()
            compose.waitForIdle()
            assertEquals("a key moved after ${i + 1} figure(s)", before, keys.map(::top))
            assertFalse("« 120 » still marked while « ${listOf("1", "12", "128")[i]} » is typed", marked("tempo_preset_120"))
        }
        // Typing a ready-made tempo marks its chip.
        repeat(3) { compose.onNodeWithTag("tempo_key_erase").performClick() }
        listOf("1", "4", "0").forEach { compose.onNodeWithTag("tempo_key_$it").performClick() }
        compose.waitForIdle()
        assertTrue("« 140 » typed marks its chip", marked("tempo_preset_140"))
        assertEquals(before, keys.map(::top))
    }
}
