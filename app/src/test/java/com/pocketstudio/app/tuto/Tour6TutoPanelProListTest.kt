package com.pocketstudio.app.tuto

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 6, T9: in Pro an example not listened to does not hold the course back (« Bravo, chapitre fini ! »), but the
 * chapters' list counted it and said « 3/4 » with no tick. A chapter finished by « Bravo » is ticked in the list.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w392dp-h852dp")
class Tour6TutoPanelProListTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun aProChapterFinishedWithItsExampleUnheardIsTicked() {
        val chapters = TutoCourse.of("techno")!!.chaptersFor(TutoLevel.Pro)
        val ch = chapters.indexOfFirst { c -> c.steps.any { it.id == "kick_hear" } }
        val steps = chapters[ch].steps
        assertTrue("the chapter has an example", steps.any { it.listening })
        // Every step of the chapter done but its example.
        val done = chapters.take(ch + 1).flatMap { it.steps }.filterNot { it.listening }.map { it.id }.toSet()
        val ui = TutoUi(TutoProgress("techno", TutoLevel.Pro, done = done, current = steps.last().id), chapters, ch, steps.lastIndex)
        assertTrue("the panel says the chapter is over (nothing left of it)", ui.left.none { it in steps })
        compose.setContent { PocketStudioTheme { TutoChapterList(ui, goTo = {}, setLevel = {}, quit = {}, close = {}) } }
        compose.waitForIdle()
        val ticked = compose.onAllNodes(hasContentDescription("Fait") and hasAnyAncestor(hasTestTag("tuto_chapter_$ch")), useUnmergedTree = true)
            .fetchSemanticsNodes().isNotEmpty()
        val count = "${steps.size - 1}/${steps.size}"
        val counted = compose.onAllNodes(hasText(count) and hasAnyAncestor(hasTestTag("tuto_chapter_$ch")), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        assertTrue("the list says « $count » for a chapter the panel says finished", ticked && !counted)
        assertTrue(compose.onAllNodesWithTag("tuto_chapter_list").fetchSemanticsNodes().isNotEmpty())
    }
}
