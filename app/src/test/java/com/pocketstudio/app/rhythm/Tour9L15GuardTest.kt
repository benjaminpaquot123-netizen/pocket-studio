package com.pocketstudio.app.rhythm

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextLayoutResult
import com.pocketstudio.app.ui.UiScaled
import com.pocketstudio.model.Project
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 9, lot L15, guards for what was not reproduced on the Rythme screen of a 360 dp phone:
 * - the Kit chip L4 saw pale at 200 %: it is enabled and its « Kit : Classique » reads whole, on one line;
 * - F049: « À 130 et 200 %, l'en-tête de la grille ne cache ni la première rangée, ni le sélecteur de mesure, ni le nom
 *   du pad. » The head (« Pas : Kick », the pad's name) ends above the first cell, and the « Tous les pads » chip does
 *   not lie over it.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h1800dp")
class Tour9L15GuardTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun tidy() = RuntimeEnvironment.setFontScale(1f)

    private fun show(uiScale: Float = 1f, fontScale: Float = 1f) {
        if (fontScale != 1f) RuntimeEnvironment.setFontScale(fontScale)
        val start = Project()
        compose.setContent {
            UiScaled(uiScale) {
                PocketStudioTheme {
                    RhythmScreen(project = start.copy(drums = start.drums.withSteps(32)), transport = TransportUi(selectedPad = 0), live = { LiveUi() }, canUndo = false, canRedo = false, actions = RhythmActions())
                }
            }
        }
        compose.waitForIdle()
    }

    /** The text laid out for [words] (case aside), with its node's bounds on the screen. */
    private fun laidOut(words: String): Pair<TextLayoutResult, Rect> {
        val found = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true)
            .fetchSemanticsNodes().firstNotNullOfOrNull { n ->
                val layouts = mutableListOf<TextLayoutResult>()
                n.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
                layouts.firstOrNull()?.takeIf { it.layoutInput.text.text.trim().equals(words, ignoreCase = true) }?.let { it to n.boundsInRoot }
            }
        assertNotNull("« $words » is shown", found)
        return found!!
    }

    private fun kitWhole(uiScale: Float = 1f, fontScale: Float = 1f) {
        show(uiScale, fontScale)
        compose.onNodeWithTag("kit_chip").assertIsEnabled()
        val (kit, _) = laidOut("Kit : Classique")
        assertEquals("« Kit : Classique » on one line", 1, kit.lineCount)
        assertFalse("« Kit : Classique » not cut with « … »", kit.isLineEllipsized(0))
    }

    @Test fun kitChipWholeAt100() = kitWhole()
    @Test fun kitChipWholeAt200() = kitWhole(fontScale = 2f)

    private fun headClear(uiScale: Float = 1f, fontScale: Float = 1f) {
        show(uiScale, fontScale)
        val (head, at) = laidOut("Pas : Kick")
        assertFalse("the pad's name in the head is not cut", head.isLineEllipsized(head.lineCount - 1))
        val cell = compose.onNodeWithTag("step_0", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue("the head ($at) ends above the first row of cells ($cell)", at.bottom <= cell.top)
        val chip = compose.onNodeWithTag("all_pads").fetchSemanticsNode().boundsInRoot
        assertFalse("« Tous les pads » ($chip) does not lie over the head ($at)", chip.overlaps(at))
        assertTrue("« Tous les pads » ($chip) ends above the first row of cells ($cell)", chip.bottom <= cell.top)
    }

    @Test fun f049HeadClearsTheGridAt130() = headClear(uiScale = 1.3f)
    @Test fun f049HeadClearsTheGridAt200() = headClear(fontScale = 2f)
}
