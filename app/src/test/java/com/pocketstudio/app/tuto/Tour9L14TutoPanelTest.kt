package com.pocketstudio.app.tuto

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tour 9, lot L14: the tutorial's panel, its kit banner and its end (finitions F012 to F022). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w392dp-h852dp")
class Tour9L14TutoPanelTest {
    @get:Rule
    val compose = createComposeRule()

    private fun nodes(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true)
    private fun has(tag: String) = nodes(tag).fetchSemanticsNodes().isNotEmpty()
    private val chapters = TutoCourse.of("techno")!!.chaptersFor(TutoLevel.Facile)
    private fun ui(ch: Int = 0, at: Int = 0) = TutoUi(TutoProgress("techno", TutoLevel.Facile, current = chapters[ch].steps[at].id), chapters, ch, at)

    /** F013 (t9-02, t9-48): a finger aiming at « Arrêter » during « Montre-moi » hit « Suivant » and got « pas encore faite ». */
    @Test
    fun demoMakesNextAndPreviousInert() {
        var nexts = 0
        var prevs = 0
        compose.setContent {
            PocketStudioTheme {
                TutoPanel(ui(0, 1).copy(demo = TutoDemoPhase.SHOWING), canShow = true, actions = TutoActions(next = { nexts++ }, previous = { prevs++ }), reducedMotion = true)
            }
        }
        compose.waitForIdle()
        assertTrue("« Suivant » is there, in its place", has("tuto_next"))
        nodes("tuto_next")[0].assertIsNotEnabled()
        nodes("tuto_next")[0].performClick()
        nodes("tuto_prev")[0].assertIsNotEnabled()
        nodes("tuto_prev")[0].performClick()
        compose.waitForIdle()
        assertEquals("« Suivant » moved on during the demo", 0, nexts)
        assertEquals("« Précédent » moved back during the demo", 0, prevs)
    }

    /** F017 (t9-05, t9-07, t9-19): while the kit downloads, « Plus tard » went on showing beside the progress. */
    @Test
    fun kitDownloadingShowsOnlyItsProgress() {
        compose.setContent {
            PocketStudioTheme { TutoKitCard("Techno", "3 Mo", progress = 0.4f, error = null, download = {}, later = {}) }
        }
        compose.waitForIdle()
        assertTrue(has("tuto_kit_progress"))
        assertFalse("« Télécharger » during its download", has("tuto_kit_download"))
        assertFalse("« Plus tard » during the download", has("tuto_kit_later"))
    }

    /** F017, the card pinned under the step's words (the panel's own place for it). */
    @Test
    fun kitDownloadingPinnedShowsOnlyItsProgress() {
        compose.setContent {
            PocketStudioTheme {
                TutoPanel(ui(0, 1), canShow = false, actions = TutoActions(), reducedMotion = true, kit = {
                    TutoKitCard("Techno", "3 Mo", progress = 0.4f, error = null, download = {}, later = {})
                })
            }
        }
        compose.waitForIdle()
        assertTrue(has("tuto_kit_progress"))
        assertFalse("« Télécharger » during its download", has("tuto_kit_download"))
        assertFalse("« Plus tard » during the download", has("tuto_kit_later"))
    }
}
