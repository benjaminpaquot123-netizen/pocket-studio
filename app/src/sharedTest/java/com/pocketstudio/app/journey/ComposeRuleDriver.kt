package com.pocketstudio.app.journey

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo

/** The tests' [JourneyDriver]: the compose test rule's finders and touches (emulator and Robolectric). */
class ComposeRuleDriver(private val compose: ComposeTestRule) : JourneyDriver {
    override fun count(tag: String, merged: Boolean): Int = compose.onAllNodesWithTag(tag, useUnmergedTree = !merged).fetchSemanticsNodes().size

    override fun bounds(tag: String): Rect = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInWindow

    override fun scrollTo(tag: String) {
        compose.onNodeWithTag(tag).performScrollTo()
    }

    override fun click(tag: String) {
        compose.onNodeWithTag(tag).performClick()
        runCatching { compose.waitForIdle() }
    }

    override fun idle(ms: Long) {
        val end = System.currentTimeMillis() + ms
        do {
            Thread.sleep(minOf(50L, ms))
            // Under createEmptyComposeRule the activity's frames come from the test clock: moved by one frame
            // (as JourneyTest.frame does), so frame-driven waits (the opening's still hold) go on (CI 242).
            runCatching { if (compose.mainClock.autoAdvance) compose.mainClock.advanceTimeByFrame() }
            runCatching { compose.waitForIdle() }
        } while (System.currentTimeMillis() < end)
    }

    override fun tags(pattern: Regex): List<String> =
        compose.onAllNodes(SemanticsMatcher("tag matches $pattern") { n -> n.config.getOrNull(SemanticsProperties.TestTag)?.matches(pattern) == true }, useUnmergedTree = true)
            .fetchSemanticsNodes().mapNotNull { it.config.getOrNull(SemanticsProperties.TestTag) }
}

/** The tests' [Journey]: through the compose test rule, as before the in-app driver. */
fun Journey(compose: ComposeTestRule, log: (String) -> Unit = {}): Journey = Journey(ComposeRuleDriver(compose), log)
