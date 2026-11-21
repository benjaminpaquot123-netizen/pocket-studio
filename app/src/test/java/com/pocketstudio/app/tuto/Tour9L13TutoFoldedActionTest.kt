package com.pocketstudio.app.tuto

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoNeed
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 9, F002 (seats t9-47, t9-60, t9-11): folded, the panel hid the step's action. After « Montre-moi », « Voilà ! À toi
 * maintenant. » and « Arrêter » were all the line said, « Créer la partie … » unseen; a chapter's end folded said « Bravo,
 * chapitre fini ! » with no « Chapitre suivant ». Folded, the step's action (or its button) is still read and touched.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Tour9L13TutoFoldedActionTest {
    @get:Rule
    val compose = createComposeRule()

    private val style = "jump_up"
    private val chapters = TutoCourse.of(style)!!.chaptersFor(TutoLevel.Facile)

    private fun node(tag: String) = compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().firstOrNull()

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun foldedTheStepsActionIsStillThere() {
        val c = chapters.indexOfFirst { ch -> ch.steps.any { it.id == "make_intro" } }
        val s = chapters[c].steps.indexOfFirst { it.id == "make_intro" }
        var needs = 0
        var nexts = 0
        // The demonstration's end, folded: « À toi maintenant » and the step's button.
        var ui by mutableStateOf(
            TutoUi(TutoProgress(style, TutoLevel.Facile, current = "make_intro"), chapters, c, s, open = false,
                need = TutoNeed.Part("Intro"), demo = TutoDemoPhase.FADING),
        )
        compose.setContent {
            PocketStudioTheme {
                TutoPanel(ui, canShow = true, actions = TutoActions(need = { needs++ }, next = { nexts++ }), reducedMotion = true)
            }
        }
        compose.waitForIdle()
        val wrong = ArrayList<String>()
        // Still the demonstration's line (it fades): what waits for its end (the journey, the user) sees it under way.
        if (node("tuto_demo_caption") == null) wrong += "demo's end folded: the line no longer says the demonstration is under way"
        if (node("tuto_need_folded") == null) wrong += "demo's end folded: no « Créer la partie » on the line"
        else {
            compose.onAllNodes(hasTestTag("tuto_need_folded"), useUnmergedTree = true)[0].performClick()
            compose.waitForIdle()
            if (needs != 1) wrong += "demo's end folded: « Créer la partie » touched, $needs need"
        }
        // A chapter's end, folded: « Bravo, chapitre fini ! » and « Chapitre suivant ».
        val first = chapters[0]
        ui = TutoUi(TutoProgress(style, TutoLevel.Facile, done = first.steps.map { it.id }.toSet(), current = first.steps.last().id),
            chapters, 0, first.steps.lastIndex, open = false, bravo = true)
        compose.waitForIdle()
        if (node("tuto_chapter_end_folded") == null) wrong += "chapter's end folded: no « Bravo, chapitre fini ! »"
        if (node("tuto_chapter_next_folded") == null) wrong += "chapter's end folded: no « Chapitre suivant »"
        else {
            compose.onAllNodes(hasTestTag("tuto_chapter_next_folded"), useUnmergedTree = true)[0].performClick()
            compose.waitForIdle()
            if (nexts != 1) wrong += "chapter's end folded: « Chapitre suivant » touched, $nexts next"
        }
        assertTrue("Folded line:\n" + wrong.joinToString("\n"), wrong.isEmpty())
    }
}
