package com.pocketstudio.app.rhythm

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import com.pocketstudio.app.ui.UiScaled
import com.pocketstudio.model.Project
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 9, lot L15 (seen by L4 at 200 %): the head of the step grid, « Pas : Kick », reads on one line on a 360 dp phone
 * at 100 %, Réglages › Taille 130 % and the phone's font at 200 %. Before, the « Tous les pads » chip kept its width and
 * squeezed the head into a column of one word per line (« PAS / : / KICK »).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h1800dp")
class Tour9L15StepHeadTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun tidy() = RuntimeEnvironment.setFontScale(1f)

    private fun headLines(uiScale: Float = 1f, fontScale: Float = 1f): Int {
        if (fontScale != 1f) RuntimeEnvironment.setFontScale(fontScale)
        compose.setContent {
            UiScaled(uiScale) {
                PocketStudioTheme {
                    RhythmScreen(project = Project(), transport = TransportUi(selectedPad = 0), live = { LiveUi() }, canUndo = false, canRedo = false, actions = RhythmActions())
                }
            }
        }
        compose.waitForIdle()
        val head = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true)
            .fetchSemanticsNodes().firstNotNullOfOrNull { n ->
                val layouts = mutableListOf<TextLayoutResult>()
                n.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
                layouts.firstOrNull()?.takeIf { it.layoutInput.text.text.trim().equals("Pas : Kick", ignoreCase = true) }
            }
        assertNotNull("the grid's head « Pas : Kick » is shown", head)
        return head!!.lineCount
    }

    @Test fun at100() = assertEquals(1, headLines())

    @Test fun at130() = assertEquals(1, headLines(uiScale = 1.3f))

    @Test fun at200() = assertEquals(1, headLines(fontScale = 2f))
}
