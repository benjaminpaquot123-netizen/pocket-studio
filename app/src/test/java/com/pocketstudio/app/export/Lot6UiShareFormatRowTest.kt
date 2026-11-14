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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.audit.LayoutScan
import com.pocketstudio.app.mixer.TargetPick
import com.pocketstudio.model.TargetChoice
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Lot 6, U4 and its duplicates U15, N7: the M4A / WAV row stays pinned with « Partager » under what scrolls, and the
 * line cut at the scroll's bottom edge, just above it, looked covered by the chips: « Autres fichiers » (A c #7),
 * « Salva il brano in .zip » (F a #19), the « Nom du morceau » field (H a-suite #246, D a-suite #223), cut net with no
 * sign that the list goes on. Now no line is cut net against the row: the cut edge fades (rule 17, « Tout défilement
 * se voit », read by [LayoutScan.unhintedScrolls]), one test per screen cited.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-port-hdpi", fontScale = 1.3f)
class Lot6UiShareFormatRowTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun french() { I18n.lang = Lang.FR }

    private val ui = ShareSheetUi(
        name = "Mon morceau", seconds = 158, parts = 4, checks = listOf(ShareCheck.Short(25)), projectName = "Mon morceau", lufs = -12.0,
    )

    private fun show(lang: Lang, height: Dp, simple: Boolean, page: Boolean, ui: ShareSheetUi = this.ui, sendProject: (() -> Unit)? = {}) {
        I18n.lang = lang
        compose.setContent {
            PocketStudioTheme {
                Box(Modifier.fillMaxWidth().height(height)) {
                    ShareSheetContent(ui, ShareSheetActions(sendProject = sendProject), page = page, simple = simple)
                }
            }
        }
        compose.waitForIdle()
    }

    private val scrolls = SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)

    private fun scrollNode(): SemanticsNode = compose.onAllNodes(scrolls, useUnmergedTree = true).fetchSemanticsNodes().first()

    private fun node(tag: String): SemanticsNode = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().first()

    /** Scrolls so that the bottom edge of what scrolls cuts the line [tag] in its middle, as on the testers' screens. */
    private fun cutAtTheEdge(tag: String) {
        repeat(4) {
            val frame = scrollNode().boundsInRoot
            val row = node(tag)
            val delta = row.positionInRoot.y + row.size.height / 2f - frame.bottom
            if (kotlin.math.abs(delta) < 2f) return
            compose.onAllNodes(scrolls, useUnmergedTree = true)[0].performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, delta) }
            compose.waitForIdle()
        }
    }

    /** The line [tag] is cut by the bottom edge, right above the M4A / WAV row, and that edge says the list goes on. */
    private fun theCutLineCarriesASign(tag: String) {
        cutAtTheEdge(tag)
        val frame = scrollNode().boundsInRoot
        val row = node(tag)
        val top = row.positionInRoot.y
        assertTrue("$tag is cut by the edge (the testers' screen): $top..${top + row.size.height} vs ${frame.bottom}",
            top < frame.bottom - 1f && top + row.size.height > frame.bottom + 1f)
        val chips = node("share_format_M4a").boundsInRoot
        assertTrue("the M4A / WAV row is right under that edge", chips.top >= frame.bottom - 1f)
        assertEquals("$tag: cut net against the M4A / WAV row, with no sign the list goes on", emptyList<String>(), LayoutScan.unhintedScrolls(compose))
        val fade = compose.onAllNodesWithTag("scroll_hint_fade", useUnmergedTree = true).fetchSemanticsNodes().single().boundsInRoot
        assertTrue("$tag: the fade lies on the cut edge ($fade vs ${frame.bottom})", kotlin.math.abs(fade.bottom - frame.bottom) < 2f)
    }

    /** U4 (A c #7, the Mode simple's « Envoyer » page, French): « Autres fichiers » under the chips. */
    @Test
    fun otherFilesLineSimplePage() {
        show(Lang.FR, 560.dp, simple = true, page = true)
        theCutLineCarriesASign("share_other")
    }

    /** U15 (F a #19, the sheet in Pro, Italian): « Salva il brano in .zip » under the chips. */
    @Test
    fun zipLineProSheetItalian() {
        show(Lang.IT, 600.dp, simple = false, page = false, ui = ui.copy(target = TargetPick(TargetChoice.AUTO, -8.5f, null, "Trap")))
        theCutLineCarriesASign("share_project")
    }

    /** N7 (H a-suite #246, a short page, the tutorial's panel under it): the « Nom du morceau » field cut in half. */
    @Test
    fun nameFieldShortSimplePage() {
        show(Lang.FR, 340.dp, simple = true, page = true)
        theCutLineCarriesASign("share_name")
    }

    /** N7 in Pro (the sheet over the tutorial's panel): the same field. */
    @Test
    fun nameFieldShortProSheet() {
        show(Lang.FR, 340.dp, simple = false, page = false)
        theCutLineCarriesASign("share_name")
    }

    /** Scrolled to the end, nothing is cut any more: no sign is drawn. */
    @Test
    fun atTheEndNoSign() {
        show(Lang.FR, 560.dp, simple = true, page = true)
        compose.onAllNodes(scrolls, useUnmergedTree = true)[0].performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 100_000f) }
        compose.waitForIdle()
        assertEquals(0, compose.onAllNodesWithTag("scroll_hint_fade", useUnmergedTree = true).fetchSemanticsNodes().size)
    }
}
