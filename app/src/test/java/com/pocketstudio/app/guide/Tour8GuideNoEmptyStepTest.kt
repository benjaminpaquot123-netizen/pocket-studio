package com.pocketstudio.app.guide

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import com.pocketstudio.model.guide.GuideStatus
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 8, lot R (review of guide2, mineur): « en fr, deux mains, phone à 130 %, Mode Simple : depuis le Guide sans étape
 * en cours, je ne vois pas de blanc entre le titre et la suite. » The block of the step (« À toi : … », « Écouter seul »,
 * the button it names) is not laid out when it has nothing to show: an empty one took the card's 6 dp gap once more.
 *
 * Tour 9, L9: the gap itself is measured, from the card's why to what follows it: one gap of the card (NeonCard's 8 dp; the
 * 6 dp are the spacing inside the block), plus the own top padding of what follows, never a second gap. The card whose only
 * option opens a screen, settled (a button it would name, no step to name it), is covered too.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-xhdpi")
class Tour8GuideNoEmptyStepTest {
    @get:Rule
    val compose = createComposeRule()

    private val why = "Pour de l'air."

    private fun card(status: GuideStatus, options: List<GuideOptionUi>) = GuideCardUi(
        id = "mix.width", stage = 3, target = "song", kind = "choice", title = "Élargis le son", why = why,
        options = options, elsewhere = emptyList(), lexicon = "", status = status, alert = false, undoable = false,
    )

    private val choice = GuideOptionUi("wide", "Plus large", "", null, null, false, edits = true)
    private val opener = GuideOptionUi("width", "La largeur", "", null, "mix", false)

    private fun show(c: GuideCardUi) {
        compose.setContent { PocketStudioTheme { GuideCard(c, null, GuidePanelActions()) } }
        compose.waitForIdle()
    }

    private fun blocks(): Int =
        compose.onAllNodesWithTag("guide_step_block", useUnmergedTree = true).fetchSemanticsNodes(atLeastOneRootRequired = false).size

    /** In dp: from the bottom of the card's why to the top of the first thing laid out under it. */
    private fun gapUnderWhy(): Float {
        val density = compose.onNode(isRoot()).fetchSemanticsNode().layoutInfo.density.density
        val w = compose.onNodeWithText(why, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        fun all(n: SemanticsNode): List<SemanticsNode> = listOf(n) + n.children.flatMap { all(it) }
        val below = all(compose.onNode(isRoot(), useUnmergedTree = true).fetchSemanticsNode())
            .map { it.boundsInRoot }.filter { it.height > 0f && it.top >= w.bottom - 0.5f }
        assertTrue("nothing under the card's why", below.isNotEmpty())
        return (below.minOf { it.top } - w.bottom) / density
    }

    /** [own]: the top padding of what follows the why (« Voir le réglage »'s 4 dp, the step's row 2 dp). */
    private fun oneGap(what: String, own: Float) {
        val gap = gapUnderWhy()
        assertTrue("$what: $gap dp between its why and what follows, more than the card's one gap ($CARD_GAP dp) and its own $own dp", gap <= CARD_GAP + own + 0.5f)
    }

    private fun noEmptyBlock(what: String, c: GuideCardUi, own: Float) {
        show(c)
        oneGap(what, own)
        assertEquals("$what lays out an empty step block", 0, blocks())
    }

    @Test
    fun settledCardHasNoEmptyStepBlock() = noEmptyBlock("a settled card (no step, no « Écouter seul »)", card(GuideStatus.DONE, listOf(choice)), 0f)

    @Test
    fun settledCardNamingAScreenHasNoEmptyStepBlock() =
        noEmptyBlock("a settled card whose option opens a screen (no step to name it)", card(GuideStatus.DONE, listOf(opener)), 4f)

    @Test
    fun cardToDoKeepsItsStep() {
        show(card(GuideStatus.TODO, listOf(choice)))
        assertEquals(1, blocks())
        oneGap("a card to do, its step", 2f)
    }

    @Test
    fun cardToDoNamingAScreenKeepsItsStepAndButton() {
        show(card(GuideStatus.TODO, listOf(opener)))
        assertEquals(1, blocks())
        assertEquals("the button the step names stands in its block", 1,
            compose.onAllNodesWithTag("guide_open_width", useUnmergedTree = true).fetchSemanticsNodes().size)
    }

    private companion object {
        /** NeonCard's spacing between its children. */
        const val CARD_GAP = 8f
    }
}
