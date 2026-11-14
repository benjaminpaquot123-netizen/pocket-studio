package com.pocketstudio.app.tuto

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.unit.dp
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
 * Tour 6, T7: the techno's step « ready » says « ton kit est déjà celui de la techno… Tu n'as rien à régler » while
 * the kit's card under it offers « Télécharger le kit Techno » (below « Lire la suite »). Not on the step that says
 * the kit is ready; on the steps after it, still offered.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w392dp-h852dp")
class Tour6TutoPanelKitReadyTest {
    @get:Rule
    val compose = createComposeRule()

    private fun has(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun theStepThatSaysTheKitIsReadyDoesNotOfferIt() {
        val chapters = TutoCourse.of("techno")!!.chaptersFor(TutoLevel.Facile)
        val ch = chapters.indexOfFirst { c -> c.steps.any { it.id == "ready" } }
        val at = chapters[ch].steps.indexOfFirst { it.id == "ready" }
        assertTrue("« ready » says the kit is set", "kit" in chapters[ch].steps[at].text && "rien à régler" in chapters[ch].steps[at].text)
        fun ui(i: Int) = TutoUi(TutoProgress("techno", TutoLevel.Facile, current = chapters[ch].steps[i].id), chapters, ch, i)
        var shown by mutableStateOf(ui(at))
        compose.setContent {
            PocketStudioTheme {
                TutoPanel(shown, canShow = false, actions = TutoActions(), reducedMotion = true, kit = { Box(Modifier.fillMaxWidth().height(160.dp).testTag("kit_probe")) })
            }
        }
        compose.waitForIdle()
        assertFalse("the kit's card offered on the step that says the kit is ready", has("kit_probe"))
        shown = ui(at + 1)
        compose.waitForIdle()
        assertTrue("the kit's card on the next step", has("kit_probe"))
    }
}
