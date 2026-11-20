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
    fun readAllShowsTheWholeInstruction() {
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
                        TutoPanel(u, canShow = true, actions = TutoActions(), reducedMotion = true)
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.onNode(hasTestTag("tuto_text_more"), useUnmergedTree = true).performClick()
        compose.waitForIdle()
        val text = compose.onAllNodes(hasTestTag("tuto_text"), useUnmergedTree = true).fetchSemanticsNodes().first()
        assertTrue(
            "after « Verder lezen », ${text.boundsInRoot.height.toInt()} of ${text.size.height} px of the instruction in sight",
            text.boundsInRoot.height >= text.size.height - 1f,
        )
    }
}
