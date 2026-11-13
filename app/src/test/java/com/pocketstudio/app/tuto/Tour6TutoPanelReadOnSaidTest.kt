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
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp

/**
 * Tour 6, lot 2, fresh review (moyen): at the end of a step's words « Lire la suite » is hidden by its transparency only, its
 * room kept, and TalkBack still reads « Lire la suite » with nothing more to read. Once the words are read to their end, no
 * « Lire la suite » is left for TalkBack, in several languages, at 130 %, on a 360 dp phone.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Tour6TutoPanelReadOnSaidTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun end() {
        I18n.lang = Lang.FR
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun readOnIsNotSaidOnceTheWordsAreRead() {
        val chapters = TutoCourse.of("techno")!!.chaptersFor(TutoLevel.Facile)
        val places = (0..5).flatMap { c -> chapters[c].steps.indices.map { c to it } }
        var lang by mutableStateOf(Lang.FR)
        var at by mutableStateOf(places.first())
        compose.setContent {
            val d = LocalDensity.current
            key(lang, at) {
                CompositionLocalProvider(LocalDensity provides Density(d.density, 1.3f)) {
                    val (c, i) = at
                    val ui = TutoUi(TutoProgress("techno", TutoLevel.Facile, current = chapters[c].steps[i].id), chapters, c, i)
                    PocketStudioTheme { TutoPanel(ui, canShow = true, actions = TutoActions(), reducedMotion = true) }
                }
            }
        }
        val said = ArrayList<String>()
        var scrolled = 0
        for (l in listOf(Lang.FR, Lang.DE, Lang.HI)) for (p in places) {
            I18n.lang = l
            lang = l
            at = p
            compose.waitForIdle()
            if (compose.onAllNodes(hasTestTag("tuto_text_more"), useUnmergedTree = true).fetchSemanticsNodes().isEmpty()) continue
            repeat(6) { compose.onNodeWithTag("tuto_text").performTouchInput { swipeUp() }; compose.waitForIdle() }
            if (compose.onAllNodes(hasTestTag("tuto_text_more"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()) continue
            scrolled++
            val left = compose.onAllNodes(hasText(com.pocketstudio.model.i18n.tr("Lire la suite")), useUnmergedTree = false).fetchSemanticsNodes()
            if (left.isNotEmpty()) said += "${l.code} ${chapters[p.first].steps[p.second].id}"
        }
        assertTrue("no step was read to its end", scrolled > 0)
        assertTrue("« Lire la suite » still said once read:\n" + said.joinToString("\n"), said.isEmpty())
    }
}
