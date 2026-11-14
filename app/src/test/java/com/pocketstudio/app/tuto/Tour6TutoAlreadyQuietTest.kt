package com.pocketstudio.app.tuto

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
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

/**
 * Control seat Da-5 (ja, Facile, chapter 12, the step that takes hits away): « déjà fait » and « À toi : … efface … »
 * on the same screen. A step the song held already asks for nothing: « Déjà fait » is said, its « À toi » is not.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w392dp-h852dp")
class Tour6TutoAlreadyQuietTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun end() = TutoHints.reset()

    @Test
    fun aStepDoneAlreadyDoesNotAskToDoIt() {
        val style = "techno"
        val chapters = TutoCourse.of(style)!!.chaptersFor(TutoLevel.Facile)
        val ch = chapters.indexOfFirst { c -> c.steps.any { it.id == "gap" } }
        val at = chapters[ch].steps.indexOfFirst { it.id == "gap" }
        compose.setContent {
            val ui = TutoUi(TutoProgress(style, TutoLevel.Facile, done = setOf("gap"), current = "gap"), chapters, ch, at, already = true)
            PocketStudioTheme { TutoPanel(ui, canShow = false, actions = TutoActions(), reducedMotion = true) }
        }
        compose.waitForIdle()
        assertTrue("« Déjà fait » is said", compose.onAllNodesWithTag("tuto_already", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
        assertTrue("its « À toi : … efface » is not asked any more", compose.onAllNodesWithTag("tuto_text", useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
    }
}
