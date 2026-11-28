package com.pocketstudio.app.rhythm

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import com.pocketstudio.ui.Help
import com.pocketstudio.ui.HelpDot
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 9, lot L4 (given by the chef): a help dot's « ? » holds whole in its circle at 100, 130 and 200 %, on every
 * screen (the dot is ui-components' one [HelpDot]): not clipped in height, not squeezed, not cut.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Tour9L4HelpDotTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun tidy() = RuntimeEnvironment.setFontScale(1f)

    private fun check(scale: Float) {
        if (scale != 1f) RuntimeEnvironment.setFontScale(scale)
        compose.setContent { PocketStudioTheme { HelpDot(Help("Pas", "Une case de la grille.")) } }
        compose.waitForIdle()
        val nodes = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true).fetchSemanticsNodes()
        val layouts = nodes.mapNotNull { n ->
            val out = mutableListOf<TextLayoutResult>()
            n.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(out)
            out.firstOrNull()?.takeIf { it.layoutInput.text.text == "?" }
        }
        assertEquals(1, layouts.size)
        val r = layouts.single()
        val squeezed = r.size.width < r.multiParagraph.maxIntrinsicWidth - 0.5f
        assertTrue(
            "at ${(scale * 100).toInt()} %: overflow ${r.didOverflowHeight} squeezed $squeezed size ${r.size} lines ${r.lineCount}",
            !r.didOverflowHeight && !squeezed && r.lineCount == 1 && !r.isLineEllipsized(0),
        )
    }

    @Test fun at100() = check(1f)

    @Test fun at130() = check(1.3f)

    @Test fun at200() = check(2f)
}
