package com.pocketstudio.app.rhythm

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 9, F120: « Fermer » in the kit and pad sheets stands apart from the text beside it and is easy to touch
 * (seats read « Fermer » as the end of the hint, and missed it). Each sheet: the button is at least 48 × 48 dp,
 * at least 8 dp from the text on its side, and the text never runs under it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class SheetCloseSeparateTest {
    @get:Rule
    val compose = createComposeRule()

    private fun bounds(m: SemanticsMatcher) = compose.onNode(m, useUnmergedTree = true).fetchSemanticsNode().boundsInWindow

    private fun check(textOf: SemanticsMatcher, closeTag: String) {
        val density = compose.density.density
        val text = bounds(textOf)
        val close = bounds(hasTestTag(closeTag))
        val gap = (close.left - text.right) / density
        val w = close.width / density
        val h = close.height / density
        assertTrue("« Fermer » only ${gap}dp from the text", gap >= 8f)
        assertTrue("« Fermer » ${w}×${h}dp, under 48", w >= 48f && h >= 48f)
    }

    @Test
    fun kitSheetCloseStandsApart() {
        val kits = listOf(KitChoice("k1", "Studio", KitChoice.BUILT_IN))
        compose.setContent { PocketStudioTheme { KitSheetContent("k1", kits, {}, {}) } }
        check(hasText("Touche un kit", substring = true), "kit_sheet_close")
    }

    @Test
    fun padSheetCloseStandsApart() {
        val ui = PadSheetUi(lane = 0, name = "Studio · Grosse caisse 1 très longue", volume = 1f, pan = 0f, muted = false, hasSample = true)
        compose.setContent { PocketStudioTheme { PadSheetContent(ui, PadSheetActions(), {}) } }
        check(hasTestTag("pad_sheet_title"), "pad_sheet_close")
    }
}
