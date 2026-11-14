package com.pocketstudio.app.rhythm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pocketstudio.model.Project
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 6, lot 1 « tuto-grille »: the step grid and the pads on the seats' small phone (360 dp, text at 130 %).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-port-hdpi", fontScale = 1.3f)
class RhythmGrilleTest {
    @get:Rule
    val compose = createComposeRule()

    private fun has(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    /** Two bars of drums (32 steps), a starter's sounds. */
    private fun twoBars(): Project = RhythmViewModel.starterProject().let { p -> p.copy(drums = p.drums.copy(steps = 32)) }.sanitized()

    /**
     * U6 (seat C a #12): playing with « Suivre » on, a cell touched on the bar in sight: the page stays on that bar
     * when the playhead goes on to the next one (the hit just put was taken out of sight).
     */
    @Test
    fun aCellTouchedWhilePlayingKeepsItsBar() {
        var step by mutableStateOf(2)
        val touched = mutableListOf<Int>()
        compose.setContent {
            PocketStudioTheme {
                RhythmScreen(twoBars(), TransportUi(playing = true), { LiveUi(currentStep = step) }, canUndo = false, canRedo = false,
                    actions = RhythmActions(toggleStep = { touched += it }))
            }
        }
        compose.waitForIdle()
        assertTrue("bar 1 in sight", has("step_5"))
        compose.onNodeWithTag("step_5").performClick()
        assertTrue(touched == listOf(5))
        compose.runOnIdle { step = 20 }
        compose.waitForIdle()
        assertTrue("the page went on to bar 2 under the hit just put", has("step_5") && !has("step_21"))
    }

    /** R21 (seats C a #11, H a): a pad's corner badge « G 30 » says what it is (TalkBack, and the words of the pad's sheet). */
    @Test
    fun aPadsPanBadgeSaysWhatItIs() {
        val p = twoBars().let { q -> q.copy(drums = q.drums.copy(lanes = q.drums.lanes.mapIndexed { i, l -> if (i == 1) l.copy(pan = -0.3f) else l })) }
        compose.setContent {
            PocketStudioTheme { RhythmScreen(p, TransportUi(), { LiveUi() }, canUndo = false, canRedo = false, actions = RhythmActions()) }
        }
        compose.waitForIdle()
        val badge = compose.onNodeWithTag("pad_pan_badge", useUnmergedTree = true).fetchSemanticsNode()
        val said = badge.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().joinToString()
        assertTrue("the badge « ${badge.config.getOrNull(SemanticsProperties.Text)} » says « $said »", said.contains("Gauche 30"))
    }

    /**
     * T11 (seat B b screen 35, « Takt 2/2 » at the clap): a tutorial step that begins shows the bar of its first dotted
     * cell, even when the step before left the page on a bar that has some of the new step's cells.
     */
    @Test
    fun aStepThatBeginsShowsTheBarOfItsFirstCell() {
        val tuto = com.pocketstudio.app.tuto.TutoHints
        try {
            tuto.active = true
            tuto.step = "kick_do"
            tuto.cells = 0 to setOf(21, 29)
            compose.setContent {
                PocketStudioTheme { RhythmScreen(twoBars(), TransportUi(), { LiveUi() }, canUndo = false, canRedo = false, actions = RhythmActions()) }
            }
            compose.waitForIdle()
            assertTrue("the kick's cells: bar 2", has("step_21"))
            compose.runOnIdle {
                tuto.step = "clap_do"
                tuto.cells = 0 to setOf(5, 21)
            }
            compose.waitForIdle()
            assertTrue("the next step's first cell (bar 1) in sight", has("step_5"))
        } finally {
            tuto.active = false
            tuto.step = null
            tuto.cells = null
        }
    }
}
