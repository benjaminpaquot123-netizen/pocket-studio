package com.pocketstudio.app.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 7, D1 (grave, 2d #1, #25, #34; owner, 2026-11-14): « Simple ou Pro ? » showed its title at the top, then a
 * big dark hole down to mid-screen; a beginner thought it was loading. The help text follows the title (no hole over
 * 48 dp), and no empty band over 160 dp is left between the title, the text and the cards, at 100 % and 130 % text,
 * in the « J'apprends » wording and the new project's.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h914dp-port-hdpi")
class ModeStepNoGapTest {
    @get:Rule
    val compose = createComposeRule()

    private fun noHole(fontScale: Float, tuto: Boolean) {
        compose.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale)) {
                PocketStudioTheme { ModeStep(choose = {}, back = {}, tuto = tuto) }
            }
        }
        compose.waitForIdle()
        val where = "(${fontScale * 100} %, tuto=$tuto)"
        val title = compose.onNodeWithText(tr("Simple ou Pro ?")).getBoundsInRoot()
        val help = compose.onNodeWithText(tr("Tu pourras"), substring = true).getBoundsInRoot()
        val titleToHelp = help.top.value - title.bottom.value
        assertTrue("$where help text ${titleToHelp.toInt()} dp below the title: a hole", titleToHelp in 0f..48f)
        // What is drawn between the help text and the first card (a picture of the two modes) counts as filled.
        val card = compose.onNodeWithTag("mode_card_simple").getBoundsInRoot()
        val filled = compose.onAllNodesWithTagOrNull("mode_step_picture")
        var bottom = help.bottom.value
        var hole = 0f
        if (filled != null) {
            hole = maxOf(hole, filled.first - bottom)
            bottom = filled.second
        }
        hole = maxOf(hole, card.top.value - bottom)
        assertTrue("$where an empty band of ${hole.toInt()} dp above the cards", hole <= 160f)
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithTagOrNull(tag: String): Pair<Float, Float>? {
        val nodes = onAllNodes(androidx.compose.ui.test.hasTestTag(tag)).fetchSemanticsNodes()
        val n = nodes.firstOrNull() ?: return null
        return n.boundsInRoot.top / density.density to n.boundsInRoot.bottom / density.density
    }

    @Test
    fun noHoleBelowTheTitle() = noHole(1f, tuto = false)

    @Test
    fun noHoleBelowTheTitleForTheTutorial() = noHole(1f, tuto = true)

    @Test
    fun noHoleBelowTheTitleAtBigText() = noHole(1.3f, tuto = true)
}
