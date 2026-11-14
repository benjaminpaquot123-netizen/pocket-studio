package com.pocketstudio.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.PocketStudioTheme
import com.pocketstudio.ui.WholeWordsAutoSize
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private fun simpleEntries(): List<PlusEntry> = plusEntries(
    home = {}, projects = {}, glossary = {}, missions = {}, simple = true, setSimple = {}, bugReport = {},
    simpleWays = SimplePlusWays({}, {}, {}, {}, {}),
)

private fun tagOf(e: PlusEntry) = if (e.page != null) "tool_${e.page.name}" else "plus_${e.key}"

/**
 * Tour 5, lot « moyens » (review of the Plus lot, Moyen): on a 360 dp phone, at Réglages › Taille 130 % or with the
 * phone's font at 200 %, the Mode simple's Plus tiles cut their titles after two lines with « … » (« Bibliothèque de
 * sons », in German « Sound-Bibliothek »). Every tile's title is read whole: no line lost, no « … », no word split.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-xxhdpi")
class PlusTilesBigTextTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun tearDown() {
        I18n.lang = Lang.FR
        RuntimeEnvironment.setFontScale(1f)
    }

    private fun layout(e: PlusEntry): TextLayoutResult {
        val node = compose.onNode(hasText(e.title) and hasAnyAncestor(hasTestTag(tagOf(e))), useUnmergedTree = true).fetchSemanticsNode()
        val results = ArrayList<TextLayoutResult>()
        node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
        check(results.isNotEmpty()) { "${e.key}: no text laid out" }
        return results.first()
    }

    private fun cut(uiScale: Float, fontScale: Float): List<String> {
        if (fontScale != 1f) RuntimeEnvironment.setFontScale(fontScale)
        var lang by mutableStateOf(Lang.FR)
        compose.setContent {
            I18n.lang = lang
            androidx.compose.runtime.key(lang) {
                UiScaled(uiScale) {
                    PocketStudioTheme {
                        PlusSpace(simpleEntries(), page = null, open = {}, back = {}, widePage = ToolPage.Aide, leave = {}, tiles = true) {}
                    }
                }
            }
        }
        val problems = ArrayList<String>()
        for (l in Lang.entries) {
            lang = l
            I18n.lang = l
            compose.waitForIdle()
            for (e in simpleEntries()) {
                val r = layout(e)
                if (!WholeWordsAutoSize.fits(r, e.title)) problems += "${l.code}: « ${e.title} » cut (${r.lineCount} lines)"
            }
        }
        return problems
    }

    @Test
    fun everyTileTitleIsWholeAtTaille130() {
        val problems = cut(uiScale = 1.3f, fontScale = 1f)
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun everyTileTitleIsWholeWithThePhonesFontAt200() {
        val problems = cut(uiScale = 1f, fontScale = 2f)
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }
}
