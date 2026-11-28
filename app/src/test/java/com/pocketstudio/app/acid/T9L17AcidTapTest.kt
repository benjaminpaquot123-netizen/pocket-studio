package com.pocketstudio.app.acid

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pocketstudio.app.tuto.TutoHints
import com.pocketstudio.model.AcidPattern
import com.pocketstudio.model.AcidStep
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tour 9, F040 (t9-01): « Touche une case pour la changer »: a touch on a cell of the 303 grid puts or changes its note. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h2400dp")
class T9L17AcidTapTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun clear() { TutoHints.acid = null }

    private var acid by mutableStateOf(AcidPattern())
    private var selected by mutableIntStateOf(0)

    private fun show() {
        compose.setContent {
            PocketStudioTheme {
                AcidScreen(
                    acid = acid, playing = false, currentStep = -1, selected = selected,
                    actions = AcidActions(
                        selectStep = { selected = it },
                        editStep = { t -> acid = acid.withStep(selected, t(acid.steps[selected])) },
                    ),
                )
            }
        }
    }

    private fun tap(i: Int) {
        compose.onNodeWithTag("acid_step_$i").performScrollTo().performClick()
        compose.waitForIdle()
    }

    @Test
    fun touchingASilentCellPutsANote() {
        show()
        assertFalse(acid.steps[3].gate)
        tap(3)
        assertEquals(3, selected)
        assertTrue("a touch on a silent cell puts a note", acid.steps[3].gate)
        // Touched again: the note goes back to silence (the cell is changed both ways).
        tap(3)
        assertFalse("a second touch takes it away", acid.steps[3].gate)
    }

    @Test
    fun touchingACellTheTutorialPointsAtWritesItsNote() {
        val want = AcidStep(note = 7, gate = true, accent = true)
        TutoHints.acid = mapOf(5 to want)
        show()
        tap(5)
        assertEquals("the dotted cell gets the tutorial's note", want, acid.steps[5])
    }

    @Test
    fun touchingANoteFirstSelectsItForTheKeys() {
        acid = acid.withStep(2, AcidStep(note = 3, gate = true))
        show()
        tap(2)
        assertEquals(2, selected)
        assertEquals("a note touched once is kept, shown in the editor below", AcidStep(note = 3, gate = true), acid.steps[2])
    }
}
