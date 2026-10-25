package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Lot 2 (audit 35, amateurs 1 and 5): the « ? » labels sat on other controls (« Mes projets » on
 * the tempo), one was cut by the top of the screen, « Masquer les étiquettes » and « Signaler un
 * bug » covered the tracks in the middle, and the Live page had no « ? » at all.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp")
class HelpLabelsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)

    @After
    fun end() = h.finish()

    private fun tap(tag: String) {
        compose.onNodeWithTag(tag).performClick()
        compose.waitForIdle()
    }

    private val labelTag = SemanticsMatcher("a « ? » label") { n -> n.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("help_label_") == true }

    /** The labels shown: the id of the control each one names, and where it is. */
    private fun labels(): List<Pair<String, Rect>> = compose.onAllNodes(labelTag, useUnmergedTree = true).fetchSemanticsNodes()
        .filter { it.layoutInfo.isPlaced && it.boundsInRoot.width > 0f }
        .map { it.config[SemanticsProperties.TestTag].removePrefix("help_label_") to it.boundsInRoot }

    private fun boundsOf(tag: String): Rect? =
        compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().firstOrNull()?.let(SemanticsNode::boundsInRoot)

    private fun Rect.overlapsBy(o: Rect, slack: Float = 1f) = left < o.right - slack && o.left < right - slack && top < o.bottom - slack && o.top < bottom - slack

    private fun assertLabelsClear(what: String, minLabels: Int = 5) {
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        val shown = labels()
        assertTrue("$what: labels shown ${shown.map { it.first }}", shown.size >= minLabels)
        val screen = compose.onRoot().fetchSemanticsNode().boundsInRoot
        shown.forEach { (id, r) ->
            assertTrue("$what: « $id »'s label $r is on the screen $screen", r.top >= screen.top - 0.5f && r.bottom <= screen.bottom + 0.5f && r.left >= -0.5f && r.right <= screen.right + 0.5f)
        }
        for ((i, a) in shown.withIndex()) for ((j, b) in shown.withIndex()) {
            if (i < j) assertTrue("$what: the labels of ${a.first} and ${b.first} overlap", !a.second.overlapsBy(b.second))
            // A label never sits on the middle of another labelled control (where its icon or word is).
            if (i != j) boundsOf(b.first)?.let { other ->
                val middle = other.deflate(minOf(other.width, other.height) * 0.3f)
                assertTrue("$what: « ${a.first} »'s label sits on « ${b.first} »", !a.second.overlapsBy(middle, 0f))
            }
        }
        // Nor on the « ? » itself (« Partager » sat half over it lying down).
        boundsOf("help_labels_button")?.let { q -> shown.forEach { (id, r) -> assertTrue("$what: « $id »'s label sits on « ? »", !r.overlapsBy(q)) } }
        val actions = boundsOf("help_labels_actions")!!
        shown.forEach { (id, r) -> assertTrue("$what: « Masquer » covers « $id »'s label", !actions.overlapsBy(r)) }
        // At the bottom, not over the middle of the screen.
        assertTrue("$what: « Masquer » at ${actions.top} of ${screen.bottom}", actions.top > screen.height * 0.6f)
    }

    @Test
    fun theModeSimplesLabelsDoNotOverlap() {
        h.start(simple = true)
        tap("help_labels_button")
        assertLabelsClear("Mode simple", minLabels = 12)
    }

    @Test
    fun theStructuresLabelsDoNotOverlap() {
        h.start()
        tap("help_labels_button")
        assertLabelsClear("Structure")
    }

    @Test
    fun theMixLabelsDoNotOverlap() {
        h.start()
        tap("tab_Mix")
        tap("help_labels_button")
        assertLabelsClear("Mix")
    }

    /** Review: lying down the rail's spaces were taken for a bottom bar, « Masquer » landed mid-screen. */
    @Test
    @Config(qualifiers = "w860dp-h400dp-land")
    fun lyingDownTheButtonsGoToTheBottom() {
        h.start()
        tap("help_labels_button")
        assertLabelsClear("Structure couchée", minLabels = 8)
    }

    @Test
    fun theLivePageHasItsQuestionMark() {
        h.start()
        tap("tab_Live")
        tap("help_labels_button")
        compose.onNodeWithTag("help_labels").assertExists()
        listOf("live_record", "live_quantum", "live_back").forEach { id ->
            assertTrue("a label for $id: ${labels().map { it.first }}", labels().any { it.first == id })
        }
        assertLabelsClear("Live")
    }
}
