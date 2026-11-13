package com.pocketstudio.app.export

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.mixer.TargetPick
import com.pocketstudio.model.Project
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 6, control seat Ac-2 (E3, round 1, screen 40): in Pro, the sheet opened with « Autres fichiers » and its « ? »
 * cut by the bottom edge of what scrolls, right above the pinned M4A / WAV row, before any scroll. Whatever the room,
 * the lines of the sheet are whole or out of sight when it opens: never cut in half by the edge.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-port-hdpi")
class Tour6ExportsWholeLinesTest {
    @get:Rule
    val compose = createComposeRule()

    private val scrolls = SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)

    private fun sweep(simple: Boolean) {
        I18n.lang = Lang.FR
        var height by mutableStateOf(300.dp)
        val ui = ShareSheetUi(
            name = "Trap", seconds = 178, parts = 26, checks = emptyList(), projectName = "Trap", lufs = -13.0, exportLufs = -8.0,
            target = TargetPick.of(Project(style = "trap")),
        )
        compose.setContent {
            PocketStudioTheme {
                Box(Modifier.fillMaxWidth().height(height)) {
                    ShareSheetContent(ui, ShareSheetActions(sendProject = {}), page = simple, simple = simple)
                }
            }
        }
        val cut = mutableListOf<String>()
        for (h in 300..760 step 10) {
            height = h.dp
            compose.waitForIdle()
            val frame = compose.onAllNodes(scrolls, useUnmergedTree = true).fetchSemanticsNodes().firstOrNull()?.boundsInRoot ?: continue
            listOf("share_other", "share_project", "share_video", "share_check_ok").forEach { tag ->
                val n = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().firstOrNull() ?: return@forEach
                // Unclipped place and size (boundsInRoot is clipped by the scroll).
                val top = n.positionInRoot.y
                val full = n.size.height.toFloat()
                val seen = minOf(top + full, frame.bottom) - maxOf(top, frame.top)
                if (seen > 1f && seen < full - 1f) cut += "$tag at ${h}dp: ${seen.toInt()} of ${full.toInt()} px"
            }
        }
        assertTrue("lines cut by the scroll's edge when the sheet opens:\n" + cut.joinToString("\n"), cut.isEmpty())
    }

    @Test
    fun theProSheetOpensWithWholeLines() = sweep(simple = false)

    @Test
    fun theSimplePageOpensWithWholeLines() = sweep(simple = true)
}
