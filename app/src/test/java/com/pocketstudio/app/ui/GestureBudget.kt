package com.pocketstudio.app.ui

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import org.junit.Assert.assertTrue

/**
 * Counts the gestures one action takes, as a user would make them, by the testTag of what is
 * touched; [assertWithin] fails when the action costs more than its budget (docs/plan/clics.md,
 * column « Cible »). A touch, a long press, and a scroll to reach something not on screen count 1
 * each; typing text is counted apart ([typed]), like clics.md's « plus une saisie ».
 *
 * Each phase adds its actions to GestureBudgetTest with it, e.g.:
 * `compose.withinGestures("Fill en fin de partie", 2) { longPress("part_play_2"); tap("part_fill") }`.
 */
class GestureBudget(private val compose: ComposeTestRule, val action: String) {
    private val path = mutableListOf<String>()

    /** The gestures made so far. */
    val gestures: Int get() = path.size

    /** The texts typed so far (not gestures of the budget). */
    var typed = 0
        private set

    /** A touch on the node tagged [tag] (scrolled to first, one more gesture, when off screen). */
    fun tap(tag: String) {
        reach(tag).performClick()
        path += tag
        compose.waitForIdle()
    }

    /** A long press on the node tagged [tag]. */
    fun longPress(tag: String) {
        reach(tag).performTouchInput { longClick() }
        path += "hold $tag"
        compose.waitForIdle()
    }

    /** [text] typed into the field tagged [tag], replacing what it holds. */
    fun type(tag: String, text: String) {
        compose.onNodeWithTag(tag).assertExists("« $action »: no field tagged $tag (after ${describe()})").performTextReplacement(text)
        typed++
        compose.waitForIdle()
    }

    private fun reach(tag: String): SemanticsNodeInteraction {
        val node = compose.onNodeWithTag(tag)
        node.assertExists("« $action »: nothing tagged $tag to touch (after ${describe()})")
        if (!node.isDisplayed()) {
            node.performScrollTo()
            path += "scroll to $tag"
        }
        return node
    }

    /** The path so far: "top_tempo › tempo_preset_140". */
    fun describe(): String = if (path.isEmpty()) "nothing" else path.joinToString(" › ")

    fun assertWithin(budget: Int) =
        assertTrue("« $action » takes $gestures gestures (${describe()}), budget $budget", gestures <= budget)
}

/** Makes « [action] » the way [path] says, then fails if it took more than [budget] gestures. */
fun ComposeTestRule.withinGestures(action: String, budget: Int, path: GestureBudget.() -> Unit): GestureBudget =
    GestureBudget(this, action).apply {
        path()
        assertWithin(budget)
    }
