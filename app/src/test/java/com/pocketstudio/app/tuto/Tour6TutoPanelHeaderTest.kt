package com.pocketstudio.app.tuto

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 6, N23: « Bravo » on the riff's chapter (its other step done before) while the header still said « リフ
 * チャプター 7/15 · ステップ 1/2 »: a step to do in a chapter said over. After its « Bravo », the header no longer
 * shows the finished chapter's step 1/2, and the « Bravo » is the chapter's.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w392dp-h852dp")
class Tour6TutoPanelHeaderTest {
    @get:Rule
    val compose = createComposeRule()

    private fun has(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun afterTheChaptersBravoTheHeaderSaysNoStepLeft() {
        val chapters = TutoCourse.of("techno")!!.chaptersFor(TutoLevel.Facile)
        // Chapter 7 of 15, two steps (the example, then the riff).
        val ch = chapters.indexOfFirst { c -> c.steps.any { it.id == "lead_do" } }
        assertTrue(ch == 6 && chapters.size == 15 && chapters[ch].steps.size == 2)
        val done = chapters.take(ch + 1).flatMap { it.steps }.map { it.id }.toSet()
        val ui = TutoUi(TutoProgress("techno", TutoLevel.Facile, done = done, current = chapters[ch].steps[0].id), chapters, ch, 0, bravo = true)
        compose.setContent { PocketStudioTheme { TutoPanel(ui, canShow = false, actions = TutoActions(), reducedMotion = true) } }
        compose.waitForIdle()
        val stepShown = compose.onAllNodes(hasText("Étape 1/2", substring = true), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        assertFalse("the header says « Étape 1/2 » of a chapter its « Bravo » says over", stepShown)
        assertTrue("« Bravo, chapitre fini ! »", has("tuto_chapter_done"))
    }
}
