package com.pocketstudio.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.pocketstudio.ui.FitText
import com.pocketstudio.ui.PocketStudioTheme
import com.pocketstudio.ui.shrinkToFit
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 9, L10 N2: « Un texte ajusté ne déborde jamais de sa case, ni en haut ni sur les côtés. » [FitText] in a box too
 * low or too narrow for it, at 200 %, in French and in Arabic; [shrinkToFit] around content wider than its box.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xxhdpi")
class FitTextBoxTest {
    @get:Rule val compose = createComposeRule()

    private fun fitted(text: String, w: Dp, h: Dp, lines: Int, rtl: Boolean): TextLayoutResult {
        compose.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, 2f), LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                PocketStudioTheme { Box(Modifier.size(w, h)) { FitText(text, Modifier.testTag("t"), MaterialTheme.typography.titleMedium, maxLines = lines) } }
            }
        }
        compose.waitForIdle()
        val n = compose.onNodeWithTag("t", useUnmergedTree = true).fetchSemanticsNode()
        val rs = ArrayList<TextLayoutResult>()
        n.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(rs)
        return rs.first()
    }

    private fun inside(what: String, r: TextLayoutResult) {
        val lines = 0 until r.lineCount
        val left = lines.minOf { r.getLineLeft(it) }
        val right = lines.maxOf { r.getLineRight(it) }
        assertTrue("$what: the text is ${r.multiParagraph.height} high in a box of ${r.size.height}", r.multiParagraph.height <= r.size.height + 0.5f)
        assertTrue("$what: the text goes from $left to $right in a box of ${r.size.width}", left >= -0.5f && right <= r.size.width + 0.5f)
    }

    @Test fun aFittedWordInALowBoxStaysInside() = inside("low, fr", fitted("Synthé", 120.dp, 12.dp, 1, false))
    @Test fun aFittedArabicWordInALowBoxStaysInside() = inside("low, ar", fitted("المُركِّب", 120.dp, 12.dp, 1, true))
    @Test fun aFittedWordInANarrowBoxStaysInside() = inside("narrow, fr", fitted("Synthétiseur", 50.dp, 40.dp, 1, false))
    @Test fun aFittedArabicWordInANarrowBoxStaysInside() = inside("narrow, ar", fitted("المُركِّب الصوتي", 50.dp, 40.dp, 1, true))
    @Test fun twoFittedLinesInASmallBoxStayInside() = inside("two lines, fr", fitted("Synthétiseur modulaire analogique", 60.dp, 30.dp, 2, false))
    @Test fun twoFittedArabicLinesInASmallBoxStayInside() = inside("two lines, ar", fitted("المُركِّب الصوتي الكبير جداً", 60.dp, 30.dp, 2, true))

    /** Guard (review L10): a word longer than its box is clipped at the box's sides; never drawn past them. */
    @Test fun aShrunkContentWiderThanItsBoxStaysInside() {
        compose.setContent {
            PocketStudioTheme {
                Box(Modifier.size(40.dp).testTag("box")) {
                    Box(Modifier.shrinkToFit()) { androidx.compose.material3.Text("Schlagzeugmaschine", Modifier.testTag("wide"), maxLines = 1) }
                }
            }
        }
        compose.waitForIdle()
        val box = compose.onNodeWithTag("box").fetchSemanticsNode().boundsInRoot
        val wide = compose.onNodeWithTag("wide", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue("a word longer than a 40 dp box: drawn at $wide, the box at $box", wide.left >= box.left - 0.5f && wide.right <= box.right + 0.5f)
    }
}
