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
 * Tour 7, control of « fable-repar » (seat a, V2; fr, 100 %, chapters 3 to 5): under the grid's dotted cells the panel
 * showed « Télécharger le kit House », « Plus tard », « ▸ Pourquoi ? » and « Lire la suite », and not one line of the
 * step's instruction (it came only after « Plus tard »). On a 360 dp phone at 100 % and at 130 %, at every step of the
 * course, the grid's cells in sight or not, the offer there from the start or come late: a whole line of the
 * instruction at least is in sight over the offer, and both of the offer's buttons are whole.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp")
class Tour7TutoKitWordsInSightTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun end() = TutoHints.reset()

    @Test
    fun theInstructionShowsOverTheKitOffer() = played()

    @Test
    @Config(qualifiers = "w360dp-h780dp", fontScale = 1.3f)
    fun theInstructionShowsOverTheKitOfferAt130() = played()

    private fun played() {
        val style = "house"
        val chapters = TutoCourse.of(style)!!.chaptersFor(TutoLevel.Facile)
        val starts = chapters.indices.flatMap { c -> chapters[c].steps.indices.map { c to it } }.filter { (c, i) -> chapters[c].steps[i].id != "ready" }
        var at by mutableStateOf(starts.first())
        var grid by mutableStateOf(false)
        var late by mutableStateOf(false)
        var offer by mutableStateOf(true)
        compose.setContent {
            key(at, grid, late) {
                TutoHints.gridInSight = grid
                TutoHints.cells = if (grid) 0 to setOf(0, 4) else null
                val (c, i) = at
                val shown = TutoUi(TutoProgress(style, TutoLevel.Facile, current = chapters[c].steps[i].id), chapters, c, i, open = true)
                PocketStudioTheme {
                    TutoPanel(shown, canShow = true, actions = TutoActions(), reducedMotion = true,
                        kit = if (offer) ({ TutoKitCard("House", "4 Mo", progress = null, error = null, download = {}, later = {}) }) else null)
                }
            }
        }
        val hidden = ArrayList<String>()
        var met = 0
        for (l in listOf(false, true)) for (g in listOf(false, true)) for (p in starts) {
            offer = !l
            late = l
            grid = g
            at = p
            compose.waitForIdle()
            if (l) { offer = true; compose.waitForIdle() }
            val where = "${if (l) "late" else "there"} ${if (g) "grid" else "words"} ${chapters[p.first].steps[p.second].id}"
            val text = compose.onAllNodes(hasTestTag("tuto_text"), useUnmergedTree = true).fetchSemanticsNodes().firstOrNull()
            if (text == null) { hidden += "$where: no instruction"; continue }
            val lines = ArrayList<TextLayoutResult>()
            text.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(lines)
            val line = lines.firstOrNull()?.let { it.getLineBottom(0) - it.getLineTop(0) } ?: text.size.height.toFloat()
            val seen = text.boundsInRoot.height
            if (seen < line - 1f) hidden += "$where: ${seen.toInt()} px of the instruction seen, a line is ${line.toInt()}"
            for (tag in listOf("tuto_kit_download", "tuto_kit_later")) {
                val b = compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().firstOrNull()
                if (b == null) { hidden += "$where: no $tag"; continue }
                met++
                if (b.boundsInRoot.height < b.size.height - 1f) hidden += "$where: $tag ${b.boundsInRoot.height.toInt()} of ${b.size.height} px seen"
            }
        }
        assertTrue("too few kit offers met: $met", met >= 200)
        assertTrue("${hidden.size} hidden:\n" + hidden.take(40).joinToString("\n"), hidden.isEmpty())
    }
}
