package com.pocketstudio.app.tuto

import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 7, lot 1 D3 (seat lotA/a, house Facile, ohat_do; captures 26, 30, 33, 53): with the grid's cells in sight
 * (the words' lower room), the last line of « À toi : … » showed cut in half above « ▸ Pourquoi ? », and the kit
 * offer at the end of the words showed « Télécharger le kit House (4 Mo) » and « Plus tard » cut. Every step of the
 * course, in French, Japanese and Korean: no line of the step's words is cut by the bottom of its room, and the kit
 * offer's two buttons are never cut (whole, or wholly below the room).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h852dp")
class Tour7TutoPanelTopWholeTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun end() {
        I18n.lang = Lang.FR
        TutoHints.reset()
    }

    @Test
    fun wordsAndKitAreNeverCutAboveTheGrid() {
        val style = "house"
        val chapters = TutoCourse.of(style)!!.chaptersFor(TutoLevel.Facile)
        val places = chapters.flatMapIndexed { c, ch -> ch.steps.indices.map { c to it } }
        TutoHints.gridInSight = true
        TutoHints.cells = 0 to setOf(0, 4)
        var lang by mutableStateOf(Lang.FR)
        var at by mutableStateOf(places.first())
        compose.setContent {
            key(lang, at) {
                val (c, i) = at
                val shown = TutoUi(TutoProgress(style, TutoLevel.Facile, current = chapters[c].steps[i].id), chapters, c, i)
                PocketStudioTheme {
                    TutoPanel(shown, canShow = true, actions = TutoActions(), reducedMotion = true,
                        kit = { TutoKitCard("House", "4 Mo", progress = null, error = null, download = {}, later = {}) })
                }
            }
        }
        val cut = ArrayList<String>()
        var kits = 0
        for (l in listOf(Lang.FR, Lang.JA, Lang.KO)) for (p in places) {
            I18n.lang = l
            lang = l
            at = p
            compose.waitForIdle()
            val where = "${l.code} ${chapters[p.first].steps[p.second].id}"
            compose.onAllNodes(hasTestTag("tuto_text"), useUnmergedTree = true).fetchSemanticsNodes().firstOrNull()?.let { text ->
                val seen = text.boundsInRoot
                val top = text.positionInRoot.y
                val layouts = ArrayList<TextLayoutResult>()
                text.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
                val layout = layouts.firstOrNull()
                // Its bounds as seen are clipped by the words' room: a bottom above its own is where the room ends.
                if (layout != null && seen.height > 0.5f && seen.bottom < top + text.size.height - 0.5f) {
                    for (n in 0 until layout.lineCount) {
                        val a = top + layout.getLineTop(n)
                        val b = top + layout.getLineBottom(n)
                        if (a < seen.bottom - 0.5f && b > seen.bottom + 0.5f) {
                            cut += "$where: line ${n + 1} cut (${(seen.bottom - a).toInt()} of ${(b - a).toInt()} px seen)"
                            break
                        }
                    }
                }
            }
            for (tag in listOf("tuto_kit_download", "tuto_kit_later")) {
                val b = compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().firstOrNull() ?: continue
                kits++
                // Whole, or wholly below the words' room (« Lire la suite »): never cut. It stays in the words (round 3, n° 28).
                val seen = b.boundsInRoot.height
                if (seen > 0.5f && seen < b.size.height - 1f) cut += "$where: $tag ${b.boundsInRoot.height.toInt()} of ${b.size.height} px seen"
            }
        }
        assertTrue("no kit offer met", kits > 0)
        assertTrue("${cut.size} cut:\n" + cut.take(40).joinToString("\n"), cut.isEmpty())
    }
}
