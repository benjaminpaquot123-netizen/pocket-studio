package com.pocketstudio.app.tuto

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
 * Tour 9, F003 against replay t9-d14 (seat t9-44): the 60 % cap must not cut « Lire la suite ». In Dutch at 200 %, the
 * liquid_dnb Pro step pad_do: after « Verder lezen » the instruction's end (« C op de bas A (= Am7). ») is in sight.
 * Upright the app gives the panel a ceiling (it reads up to the status bar); on its side none. Either way the step's
 * buttons stay in sight (fresh review: unbounded words left the action bar 0 px).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Tour9L13TutoReadAllTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun end() {
        I18n.lang = Lang.FR
    }

    @Test
    @Config(qualifiers = "w360dp-h780dp")
    fun readAllShowsTheWholeInstruction() = check(400.dp, wholeText = true)

    @Test
    @Config(qualifiers = "w780dp-h360dp")
    fun onItsSideReadAllKeepsTheStepsButtons() = check(Dp.Infinity, wholeText = false)

    private fun check(ceiling: Dp, wholeText: Boolean) {
        I18n.lang = Lang.NL
        val style = "liquid_dnb"
        val chapters = TutoCourse.of(style)!!.chaptersFor(TutoLevel.Pro)
        val c = chapters.indexOfFirst { ch -> ch.steps.any { it.id == "pad_do" } }
        val i = chapters[c].steps.indexOfFirst { it.id == "pad_do" }
        val step = chapters[c].steps[i]
        compose.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, 2f)) {
                val u = TutoUi(TutoProgress(style, TutoLevel.Pro, current = step.id), chapters, c, i)
                PocketStudioTheme {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                        TutoPanel(u, canShow = true, actions = TutoActions(), reducedMotion = true, ceiling = ceiling)
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.onNode(hasTestTag("tuto_text_more"), useUnmergedTree = true).performClick()
        compose.waitForIdle()
        val next = compose.onAllNodes(hasTestTag("tuto_next_big"), useUnmergedTree = true).fetchSemanticsNodes().firstOrNull()
            ?: compose.onAllNodes(hasTestTag("tuto_next"), useUnmergedTree = true).fetchSemanticsNodes().first()
        assertTrue(
            "after « Verder lezen », the next button ${next.boundsInRoot.height.toInt()} of ${next.size.height} px in sight",
            next.size.height > 0 && next.boundsInRoot.height >= next.size.height - 1f,
        )
        if (!wholeText) return
        val text = compose.onAllNodes(hasTestTag("tuto_text"), useUnmergedTree = true).fetchSemanticsNodes().first()
        assertTrue(
            "after « Verder lezen », ${text.boundsInRoot.height.toInt()} of ${text.size.height} px of the instruction in sight",
            text.boundsInRoot.height >= text.size.height - 1f,
        )
    }
}
