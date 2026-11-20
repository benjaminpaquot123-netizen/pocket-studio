package com.pocketstudio.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 9, lot L6, F107: « Le nom du projet et « Enregistré » tiennent dans la barre. » Seen on Structure: « Sauvegardé »
 * gone (its word hidden when cut), « Mon projet 1 » alone. On 360 and 411 dp phones, at the usual size, 130 % and 200 %, in
 * Simple and Pro, with and without the part chip: « Sauvegardé » is never shown cut; it reads whole (tried a size smaller,
 * then tighter) or its tick alone says it. The name keeps its place in the row: a line of its own took the screen's
 * height (the keys, the labels lying down), so it may still read « Mon prem… » on a full row (F107 half, a reste).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w480dp-h900dp")
class Tour9TopBarSavedTest {
    @get:Rule val compose = createComposeRule()

    private data class Case(val width: Int, val font: Float, val simple: Boolean?, val scene: Boolean, val undo: Boolean = true)

    private fun layout(node: SemanticsNode): TextLayoutResult {
        val out = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(out)
        return out.single()
    }

    private fun cut(l: TextLayoutResult) = l.hasVisualOverflow || (0 until l.lineCount).any { l.isLineEllipsized(it) } ||
        (l.lineCount == 1 && l.multiParagraph.intrinsics.maxIntrinsicWidth > l.size.width + 1f)

    private fun text(n: SemanticsNode) = n.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }.orEmpty()

    @Test
    fun theNameAndSavedFitTheBar() {
        var case by mutableStateOf(Case(360, 1f, null, false))
        compose.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, case.font)) {
                PocketStudioTheme {
                    Box(Modifier.width(case.width.dp)) {
                        TopBar(
                            TopBarUi(
                                "Mon projet 1", playing = false, canUndo = true, canRedo = true, saved = true, mode = case.simple,
                                scene = if (case.scene) SceneChipUi(current = 0, titles = listOf("A · Intro", "B · Refrain")) else null,
                                redo = case.simple != true, undo = case.undo,
                            ),
                            TopBarActions(),
                        )
                    }
                }
            }
        }
        val bad = mutableListOf<String>()
        for (w in listOf(360, 411)) for (f in listOf(1f, 1.3f, 2f)) for (simple in listOf(null, true, false)) for (scene in listOf(false, true)) for (undo in listOf(true, false)) {
            if (simple != true && !undo) continue
            case = Case(w, f, simple, scene, undo)
            compose.waitForIdle()
            val names = compose.onAllNodesWithText("Mon projet 1", useUnmergedTree = true).fetchSemanticsNodes()
            if (names.isEmpty()) continue // the folder alone, its tick on it (no name in the bar)
            val saved = compose.onAllNodesWithText("Sauvegardé", useUnmergedTree = true).fetchSemanticsNodes()
            val whole = saved.any { !cut(layout(it)) }
            // A row too full for the word: the tick alone says it (never the word cut).
            val tick = saved.isEmpty() && compose.onAllNodesWithContentDescription("Sauvegardé", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
            if (!whole && !tick) bad += "$case: « Sauvegardé » ${if (saved.isEmpty()) "missing" else "cut"} next to the name"
        }
        assertTrue(bad.joinToString("\n"), bad.isEmpty())
    }
}
