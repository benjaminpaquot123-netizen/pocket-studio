package com.pocketstudio.app.tuto

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
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
 * Tour 6, N31 fallout: the room kept for « Lire la suite » stayed under « ▸ Pourquoi ? » on a step whose words fit
 * whole (no « Lire la suite »): the panel higher for nothing, more of the grid hidden. With no « Lire la suite » shown,
 * the step's buttons come right under its last words, in several languages, at 100 and 130 %, on 360 and 400 dp.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Tour6TutoPanelFitRoomTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun end() {
        I18n.lang = Lang.FR
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun noEmptyRoomUnderTheWordsOnA360dpPhone() = check()

    @Test
    @Config(qualifiers = "w400dp-h800dp")
    fun noEmptyRoomUnderTheWordsOnA400dpPhone() = check()

    private fun check() {
        val chapters = TutoCourse.of("techno")!!.chaptersFor(TutoLevel.Facile)
        // The first chapters' steps, with and without a « Pourquoi ? ».
        val places = (0..3).flatMap { c -> chapters[c].steps.indices.map { c to it } }
        var lang by mutableStateOf(Lang.FR)
        var scale by mutableStateOf(1f)
        var at by mutableStateOf(places.first())
        compose.setContent {
            val d = LocalDensity.current
            key(lang, scale, at) {
                CompositionLocalProvider(LocalDensity provides Density(d.density, scale)) {
                    val (c, i) = at
                    val ui = TutoUi(TutoProgress("techno", TutoLevel.Facile, current = chapters[c].steps[i].id), chapters, c, i)
                    PocketStudioTheme { TutoPanel(ui, canShow = true, actions = TutoActions(), reducedMotion = true) }
                }
            }
        }
        val density = compose.density.density
        val buttons = setOf("tuto_next_big", "tuto_need", "tuto_do", "tuto_mark", "tuto_play_mine", "tuto_listen", "tuto_show", "tuto_prev", "tuto_next")
        val empty = ArrayList<String>()
        var seen = 0
        for (l in listOf(Lang.FR, Lang.DE, Lang.NL, Lang.AR, Lang.HI, Lang.JA, Lang.KO)) for (s in listOf(1f, 1.3f)) for (p in places) {
            I18n.lang = l
            lang = l
            scale = s
            at = p
            compose.waitForIdle()
            if (compose.onAllNodes(hasTestTag("tuto_text_more"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()) continue
            val inPanel = compose.onAllNodes(hasAnyAncestor(hasTestTag("tuto_panel")), useUnmergedTree = true).fetchSemanticsNodes()
            val top = inPanel.filter { it.config.getOrNull(SemanticsProperties.TestTag) in buttons }.minOfOrNull { it.boundsInRoot.top } ?: continue
            // The step's words and all under them, down to its buttons (the header is above them).
            val words = compose.onAllNodes(hasTestTag("tuto_text"), useUnmergedTree = true).fetchSemanticsNodes().firstOrNull()?.boundsInRoot ?: continue
            val bottom = inPanel.map { it.boundsInRoot }.filter { it.height > 0f && it.top >= words.top && it.bottom <= top }.maxOf { it.bottom }
            seen++
            // The panel's 6 dp between blocks, and a few dp for a button's own touch margin.
            val gap = (top - bottom) / density
            if (gap > 14f) empty += "${l.code} ${(s * 100).toInt()} % ${chapters[p.first].steps[p.second].id}: ${gap.toInt()} dp empty"
        }
        assertTrue(seen > 0)
        assertTrue("room left empty under the words:\n" + empty.joinToString("\n"), empty.isEmpty())
    }
}
