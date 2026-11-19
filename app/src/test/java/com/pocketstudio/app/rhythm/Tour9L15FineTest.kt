package com.pocketstudio.app.rhythm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pocketstudio.app.tuto.TutoHints
import com.pocketstudio.model.Project
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 9, lot L15, on the Rythme screen:
 * - F052 (seat t9-41): « Le repère vers une autre mesure dit s'il faut y ajouter ou y enlever une case. »
 * - F057 (seats t9-23, t9-25, t9-41), its half on this screen: the Swing reaches any value to 1 % (« − » and « + »).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h1800dp")
class Tour9L15FineTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun tearDown() = TutoHints.reset()

    private var project by mutableStateOf(Project())

    private fun show(actions: RhythmActions = RhythmActions()) {
        compose.setContent {
            PocketStudioTheme {
                RhythmScreen(project = project, transport = TransportUi(selectedPad = 0), live = { LiveUi() }, canUndo = false, canRedo = false, actions = actions)
            }
        }
        compose.waitForIdle()
    }

    /** Two bars, the kick on the first beat of each: the step takes both away. */
    private fun twoBarsToErase() {
        val start = Project()
        project = start.copy(drums = start.drums.withSteps(32).setHit(0, 0, true).setHit(0, 16, true))
        TutoHints.active = true
        TutoHints.erase = mapOf(0 to setOf(0, 16))
    }

    @Test
    fun f052FarHitToEraseSaysToTakeItAway() {
        twoBarsToErase()
        show()
        // The first one taken away: the line points to the second bar.
        project = project.copy(drums = project.drums.setHit(0, 0, false))
        compose.waitForIdle()
        compose.onNodeWithTag("tuto_bar_go").performScrollTo().assertTextContains(tr("barré : à enlever"), substring = true)
    }

    @Test
    fun f052FarCellToAddSaysNothingOfTakingAway() {
        val start = Project()
        project = start.copy(drums = start.drums.withSteps(32).setHit(0, 0, true))
        TutoHints.active = true
        TutoHints.cells = 0 to setOf(0, 16)
        show()
        val words = compose.onNodeWithTag("tuto_bar_go").performScrollTo().fetchSemanticsNode().config
            .getOrElse(androidx.compose.ui.semantics.SemanticsProperties.Text) { emptyList() }.joinToString { it.text }
        assertFalse(words, words.contains(tr("barré : à enlever")))
    }

    @Test
    fun f057SwingOnePercentUpAndDown() {
        var set = -1.0
        project = Project().copy(swing = 0.25 * Project.MAX_SWING)
        show(RhythmActions(setSwing = { set = it }))
        compose.onNodeWithTag("swing_plus").performScrollTo().performClick()
        assertEquals("« + » gives 26 %", 0.26, set / Project.MAX_SWING, 1e-9)
        compose.onNodeWithTag("swing_minus").performClick()
        assertEquals("« − » gives 24 %", 0.24, set / Project.MAX_SWING, 1e-9)
    }

    @Test
    fun f057SwingStopsAtItsEnds() {
        project = Project().copy(swing = 0.0)
        show()
        compose.onNodeWithTag("swing_minus").performScrollTo().assertIsNotEnabled()
        project = Project().copy(swing = Project.MAX_SWING)
        compose.waitForIdle()
        compose.onNodeWithTag("swing_plus").performScrollTo().assertIsNotEnabled()
    }
}
