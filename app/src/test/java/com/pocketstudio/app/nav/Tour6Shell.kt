package com.pocketstudio.app.nav

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/**
 * Audit ultra round 6, lot 3 « guide-et-coquille »: what its tests share to read the app's shell (the Guide, the bars
 * under the page) as a person sees it: what is on screen, where, and what a touch can reach.
 */
internal class Tour6Shell(private val compose: AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity>) {
    /** The main looper and the animations run for [ms]. */
    fun idle(ms: Long = 100) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
        compose.mainClock.advanceTimeBy(ms)
        compose.waitForIdle()
    }

    /** True once [ok] holds; false when it still does not after [timeoutMs] (the test then says what is missing). */
    fun reached(timeoutMs: Long = 20_000, ok: () -> Boolean): Boolean {
        val end = System.currentTimeMillis() + timeoutMs
        while (!ok()) {
            if (System.currentTimeMillis() >= end) return false
            idle(50)
            Thread.sleep(5)
        }
        compose.waitForIdle()
        return true
    }

    fun has(tag: String): Boolean = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    fun bounds(tag: String): Rect? = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().firstOrNull()?.boundsInRoot

    fun textOf(tag: String): String = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().flatMap(::textsOf).joinToString(" ")

    fun tap(tag: String) {
        val node = compose.onNodeWithTag(tag)
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
        compose.waitForIdle()
    }

    /** Every node on screen, as TalkBack and a finger find them (each one apart). */
    fun nodes(): List<SemanticsNode> {
        val out = ArrayList<SemanticsNode>()
        fun walk(n: SemanticsNode) { out += n; n.children.forEach(::walk) }
        compose.onAllNodes(androidx.compose.ui.test.isRoot(), useUnmergedTree = true).fetchSemanticsNodes().forEach(::walk)
        return out
    }

    /** The nodes a touch does something on. */
    fun touchables(): List<SemanticsNode> = nodes().filter { it.config.getOrNull(SemanticsActions.OnClick) != null }

    companion object {
        fun tagOf(n: SemanticsNode): String? = n.config.getOrNull(SemanticsProperties.TestTag)

        fun textsOf(n: SemanticsNode): List<String> {
            val own = n.config.getOrNull(SemanticsProperties.Text)?.map { it.text }.orEmpty() + n.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
            return own + n.children.flatMap(::textsOf)
        }

        /** [n] or one of the nodes around it is tagged with one of [tags]. */
        fun within(n: SemanticsNode, tags: Set<String>): Boolean {
            var p: SemanticsNode? = n
            while (p != null) {
                if (tagOf(p) in tags) return true
                p = p.parent
            }
            return false
        }

        /** [n] is in something that scrolls up and down: what is cut at its edge comes back with a finger. */
        fun scrolls(n: SemanticsNode): Boolean {
            var p: SemanticsNode? = n.parent
            while (p != null) {
                if (p.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange) != null) return true
                p = p.parent
            }
            return false
        }

        /** Where [n] is laid out, whether or not what is around it cuts it. */
        fun whole(n: SemanticsNode): Rect = Rect(n.positionInRoot, Size(n.size.width.toFloat(), n.size.height.toFloat()))

        /** A word for [n] in a failure's message. */
        fun name(n: SemanticsNode): String = (tagOf(n) ?: "") + " « " + textsOf(n).joinToString(" ").take(40) + " » " + n.boundsInRoot
    }
}
