package com.pocketstudio.app.rhythm

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.getOrNull
import com.pocketstudio.app.audit.ScreenScanTest
import com.pocketstudio.model.Project
import com.pocketstudio.ui.HelpAbout
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 9, lot L4: the bar row's new buttons (◀ ▶ of the pages, « Vider · Mesure N ») have a « ? » that explains
 * them, as the census of « sans ? » asks (its ceiling never rises).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class Tour9L4BarHelpTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theBarButtonsHaveAHelp() {
        val start = Project()
        val project = start.copy(drums = start.drums.withSteps(32))
        compose.setContent {
            PocketStudioTheme {
                RhythmScreen(project = project, transport = TransportUi(selectedPad = 0), live = { LiveUi() }, canUndo = false, canRedo = false, actions = RhythmActions())
            }
        }
        compose.waitForIdle()
        val about = compose.onAllNodes(SemanticsMatcher("any") { true }, useUnmergedTree = true).fetchSemanticsNodes()
            .flatMap { it.config.getOrNull(HelpAbout).orEmpty() }.map(ScreenScanTest::regex)
        val bare = listOf("drum_page_prev", "drum_page_next", "clear_bar", "drum_page_0", "drum_page_1").filter { tag -> about.none { it.matches(tag) } }
        assertTrue("no « ? » explains $bare", bare.isEmpty())
    }
}
