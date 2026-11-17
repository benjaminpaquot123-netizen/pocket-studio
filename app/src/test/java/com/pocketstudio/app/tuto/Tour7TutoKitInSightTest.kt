package com.pocketstudio.app.tuto

import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
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
 * Tour 7, end of D3 (seats 1a #1 and #26, 1c #1, 2a #2 and #62, 2d #3 and #5): at a chapter's start, the kit offer
 * at the end of the step's words sat below their room: « Télécharger le kit House (4 Mo) » and « Plus tard » showed
 * only after « Lire la suite ». On a 360 dp phone at 100 %, in French, Japanese and Korean, with the grid's cells in
 * sight or not: at each chapter's first step, both buttons are whole in sight, no « Lire la suite » touched.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp")
class Tour7TutoKitInSightTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun end() {
        I18n.lang = Lang.FR
        TutoHints.reset()
    }

    @Test
    fun theKitOfferIsWholeWithoutReadingOn() {
        val style = "house"
        val chapters = TutoCourse.of(style)!!.chaptersFor(TutoLevel.Facile)
        val starts = chapters.indices.map { it to 0 }.filter { (c, i) -> chapters[c].steps[i].id != "ready" }
        var lang by mutableStateOf(Lang.FR)
        var at by mutableStateOf(starts.first())
        var grid by mutableStateOf(false)
        compose.setContent {
            key(lang, at, grid) {
                TutoHints.gridInSight = grid
                TutoHints.cells = if (grid) 0 to setOf(0, 4) else null
                val (c, i) = at
                val shown = TutoUi(TutoProgress(style, TutoLevel.Facile, current = chapters[c].steps[i].id), chapters, c, i, open = true)
                PocketStudioTheme {
                    TutoPanel(shown, canShow = true, actions = TutoActions(), reducedMotion = true,
                        kit = { TutoKitCard("House", "4 Mo", progress = null, error = null, download = {}, later = {}) })
                }
            }
        }
        val hidden = ArrayList<String>()
        var met = 0
        for (l in listOf(Lang.FR, Lang.JA, Lang.KO)) for (g in listOf(false, true)) for (p in starts) {
            I18n.lang = l
            lang = l
            grid = g
            at = p
            compose.waitForIdle()
            val where = "${l.code} ${if (g) "grid" else "words"} ${chapters[p.first].steps[p.second].id}"
            for (tag in listOf("tuto_kit_download", "tuto_kit_later")) {
                val b = compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().firstOrNull()
                if (b == null) { hidden += "$where: no $tag"; continue }
                met++
                val seen = b.boundsInRoot.height
                if (seen < b.size.height - 1f) hidden += "$where: $tag ${seen.toInt()} of ${b.size.height} px seen"
            }
        }
        assertTrue("no kit offer met", met > 0)
        assertTrue("${hidden.size} hidden:\n" + hidden.take(40).joinToString("\n"), hidden.isEmpty())
    }
}
