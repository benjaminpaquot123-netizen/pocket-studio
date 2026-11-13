package com.pocketstudio.app.ui

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.modifiers.TextAutoSizeLayoutScope
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.pocketstudio.ui.WholeWords
import com.pocketstudio.ui.WholeWordsAutoSize
import com.pocketstudio.ui.splitsAWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 5, moyens2 (fumée finale, seat A d: German, the phone's font at 200 %, Mode simple): opening the app froze the
 * main thread 789 ms in `WholeWords.getFontSize` (the bench's watchdog limit: 700 ms). Under Robolectric with that seat's
 * settings (HumanTester.begin: `lang=de device=phone font=200 mode=simple`), the first screen made 18 auto-sized labels
 * cost 168 text layouts: each label that does not fit tried every half point from its size down (up to 15 layouts at
 * 200 %, where the floor goes lowest). Here each label is laid out as the app does it, with real text, at 200 %: the
 * same size is found (the largest that keeps every word whole) in a handful of layouts.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "de-w392dp-h852dp-port-hdpi")
class WholeWordsCostTest {
    @get:Rule
    val compose = createComposeRule()

    /**
     * Lays the text out as Text does with an auto size, counting the layouts. Compose's scope is a sealed interface:
     * stood in for by a proxy (its layout and its density answered here; the auto sizes ask nothing else).
     */
    private class Counting(private val measurer: TextMeasurer, private val density: Density, private val style: TextStyle, private val maxLines: Int) {
        var layouts = 0

        fun layout(constraints: Constraints, text: AnnotatedString, fontSize: TextUnit): TextLayoutResult {
            layouts++
            return measurer.measure(
                text, style.copy(fontSize = fontSize), overflow = TextOverflow.Ellipsis, softWrap = maxLines > 1, maxLines = maxLines,
                constraints = constraints, density = density,
            )
        }

        val scope: TextAutoSizeLayoutScope = java.lang.reflect.Proxy.newProxyInstance(
            TextAutoSizeLayoutScope::class.java.classLoader, arrayOf(TextAutoSizeLayoutScope::class.java),
        ) { _, method, args ->
            when {
                method.name.startsWith("performLayout") ->
                    layout(constraintsOf(args[0] as Long), args[1] as AnnotatedString, textUnitOf(args[2] as Long))
                method.name == "getDensity" -> density.density
                method.name == "getFontScale" -> density.fontScale
                else -> error("not stood in for: ${method.name}")
            }
        } as TextAutoSizeLayoutScope
    }

    /** German labels of the app's buttons and pads, in boxes as narrow as a phone gives them. */
    private val cases = listOf(
        "Schlagzeug" to 52, "Aufnehmen" to 48, "Bibliothek" to 56, "Importieren" to 60, "Mikrofon" to 44,
        "Effekte des Pads" to 64, "Generieren" to 58, "Vom Gerät" to 50, "Lautstärke" to 54, "Hinzufügen" to 56,
    )

    private fun measure(autoSize: TextAutoSize, maxLines: Int, oldFits: (TextLayoutResult, String) -> Boolean, top: Float, bottom: Float) {
        lateinit var measurer: TextMeasurer
        lateinit var density: Density
        compose.setContent {
            val d = LocalDensity.current
            // The phone's font at 200 % (seat A d).
            CompositionLocalProvider(LocalDensity provides Density(d.density, 2f)) {
                measurer = rememberTextMeasurer()
                density = LocalDensity.current
            }
        }
        compose.waitForIdle()
        val style = TextStyle(fontSize = top.sp, lineHeight = 1.2.em)
        var worst = 0
        for ((word, widthDp) in cases) {
            val constraints = Constraints(maxWidth = with(density) { widthDp.dp.roundToPx() }, maxHeight = Constraints.Infinity)
            val text = AnnotatedString(word)
            val scope = Counting(measurer, density, style, maxLines)
            val size = with(autoSize) { scope.scope.getFontSize(constraints, text) }
            // The size as found before, one half point after the other: the same.
            val ref = Counting(measurer, density, style, maxLines)
            var expected = top
            while (expected > bottom) {
                if (oldFits(ref.layout(constraints, text, expected.sp), word)) break
                expected -= 0.5f
            }
            if (expected <= bottom) expected = bottom
            assertEquals("« $word » in $widthDp dp at 200 %", expected, size.value, 0.001f)
            worst = maxOf(worst, scope.layouts)
        }
        assertTrue("a label that must shrink at 200 % costs at most 6 text layouts on the main thread, not $worst", worst <= 6)
    }

    @Test
    fun aWholeWordsLabelAt200FindsItsSizeInAFewLayouts() {
        // WholeWordsText's default floor: 72 % of the size at 130 %, lower at 200 % (14 × 0,72 / (2 / 1,3)).
        measure(WholeWords(14.sp, 0.72f), 2, { r, t -> !r.hasVisualOverflow && !splitsAWord(r, t) }, 14f, 14f * 0.72f / (2f / 1.3f))
    }

    @Test
    fun aFitTextLabelAt200FindsItsSizeInAFewLayouts() {
        // FitText's floor: 9 sp, as at 130 % past it (9 / (2 / 1,3)).
        measure(WholeWordsAutoSize(9.sp, 14.sp), 1, { r, t -> WholeWordsAutoSize.fits(r, t) }, 14f, 9f / (2f / 1.3f))
    }
}

/** Compose's value classes as the JVM passes them (their packed Long). */
private fun constraintsOf(packed: Long): Constraints = Constraints::class.java.getDeclaredMethod("box-impl", Long::class.javaPrimitiveType).invoke(null, packed) as Constraints

private fun textUnitOf(packed: Long): TextUnit = TextUnit::class.java.getDeclaredMethod("box-impl", Long::class.javaPrimitiveType).invoke(null, packed) as TextUnit
