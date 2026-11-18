package com.pocketstudio.app.guide

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import com.pocketstudio.model.guide.GuideStatus
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 8, lot R (review of guide2, mineur): « en fr, deux mains, phone à 130 %, Mode Simple : depuis le Guide sans étape
 * en cours, je ne vois pas de blanc entre le titre et la suite. » The block of the step (« À toi : … », « Écouter seul »,
 * the button it names) is not laid out when it has nothing to show: an empty one took the card's 6 dp gap once more.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-xhdpi")
class Tour8GuideNoEmptyStepTest {
    @get:Rule
    val compose = createComposeRule()

    private fun card(status: GuideStatus, options: List<GuideOptionUi>) = GuideCardUi(
        id = "mix.width", stage = 3, target = "song", kind = "choice", title = "Élargis le son", why = "Pour de l'air.",
        options = options, elsewhere = emptyList(), lexicon = "", status = status, alert = false, undoable = false,
    )

    private fun blocks(c: GuideCardUi): Int {
        compose.setContent { PocketStudioTheme { GuideCard(c, null, GuidePanelActions()) } }
        compose.waitForIdle()
        return compose.onAllNodesWithTag("guide_step_block", useUnmergedTree = true).fetchSemanticsNodes(atLeastOneRootRequired = false).size
    }

    @Test
    fun settledCardHasNoEmptyStepBlock() =
        assertEquals("a settled card (no step, no « Écouter seul ») lays out an empty step block", 0, blocks(card(GuideStatus.DONE, emptyList())))

    @Test
    fun cardToDoKeepsItsStep() =
        assertEquals(1, blocks(card(GuideStatus.TODO, listOf(GuideOptionUi("wide", "Plus large", "", null, null, false, edits = true)))))
}
