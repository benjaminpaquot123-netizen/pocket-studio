package com.pocketstudio.app.export

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.PocketStudioTheme
import com.pocketstudio.ui.SCROLL_FADE
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 6, seat A c (E3): on the share page, « Autres fichiers » is the last line of what scrolls, right above the pinned
 * M4A / WAV row. At the scroll's edge its second line and its « ? » sat under the fade band, so they looked covered by
 * the chips. The content can now scroll far enough that the whole line clears the fade band (a bottom pad the height of
 * the fade): its bottom ends at least one fade band above the scroll viewport's bottom edge.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-port-hdpi", fontScale = 1.3f)
class Tour6ExportsOtherFilesClearTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun french() { I18n.lang = Lang.FR }

    private val scrolls = SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)

    private fun scrollNode(): SemanticsNode = compose.onAllNodes(scrolls, useUnmergedTree = true).fetchSemanticsNodes().first()
    private fun node(tag: String): SemanticsNode = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().first()

    @Test
    fun theOtherFilesLineCanClearTheFadeBand() {
        I18n.lang = Lang.FR
        // Simple « Envoyer » page, no « .zip » line, so « Autres fichiers » is the last scrolling line, as the seat saw it.
        val ui = ShareSheetUi(name = "Trap", seconds = 158, parts = 4, checks = emptyList(), projectName = "Trap", lufs = -12.0)
        compose.setContent {
            PocketStudioTheme {
                // A short viewport, so the content scrolls and the bottom edge falls on « Autres fichiers ».
                Box(Modifier.fillMaxWidth().height(360.dp)) {
                    ShareSheetContent(ui, ShareSheetActions(sendProject = null), page = true, simple = true)
                }
            }
        }
        compose.waitForIdle()
        // Scroll to the very bottom.
        repeat(8) {
            compose.onAllNodes(scrolls, useUnmergedTree = true)[0].performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 4000f) }
            compose.waitForIdle()
        }
        val frame = scrollNode().boundsInRoot
        val row = node("share_other")
        val rowBottom = row.positionInRoot.y + row.size.height
        val fadePx = with(compose.density) { SCROLL_FADE.toPx() }
        val clearance = frame.bottom - rowBottom
        assertTrue(
            "« Autres fichiers » must clear the fade band: its bottom should sit ≥ ${fadePx}px above the viewport bottom, got ${clearance}px",
            clearance >= fadePx - 1f,
        )
    }
}
