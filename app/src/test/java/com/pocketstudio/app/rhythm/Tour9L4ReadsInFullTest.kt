package com.pocketstudio.app.rhythm

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import com.pocketstudio.model.Project
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 9, lot L4, F049 / F050: on a 360 dp phone at 100, 130 and 200 %, every word of the Rythme screen reads in full:
 * no text cut by « … », clipped in height or squeezed narrower than itself (the pads' help sentence lost its end, the
 * « Effets » button and a « ? » were squeezed to nothing beside the chips).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Tour9L4ReadsInFullTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun tidy() = RuntimeEnvironment.setFontScale(1f)

    private fun check(scale: Float) {
        if (scale != 1f) RuntimeEnvironment.setFontScale(scale)
        compose.setContent {
            PocketStudioTheme {
                RhythmScreen(project = Project(), transport = TransportUi(selectedPad = 5), live = { LiveUi() }, canUndo = false, canRedo = false, actions = RhythmActions())
            }
        }
        compose.waitForIdle()
        val nodes = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true).fetchSemanticsNodes()
        val cut = nodes.mapNotNull { n ->
            val layouts = mutableListOf<TextLayoutResult>()
            n.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
            val r = layouts.firstOrNull() ?: return@mapNotNull null
            val squeezed = r.size.width < r.multiParagraph.maxIntrinsicWidth.coerceAtMost(r.layoutInput.constraints.maxWidth.toFloat()) - 2
            if ((0 until r.lineCount).any { r.isLineEllipsized(it) } || r.didOverflowHeight || squeezed) "«${r.layoutInput.text}» ${r.size}" else null
        }.distinct()
        assertTrue("at ${(scale * 100).toInt()} %: $cut", cut.isEmpty())
    }

    @Test fun at100() = check(1f)

    @Test fun at130() = check(1.3f)

    @Test fun at200() = check(2f)
}
