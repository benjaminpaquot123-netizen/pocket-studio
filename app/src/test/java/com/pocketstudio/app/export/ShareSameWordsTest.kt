package com.pocketstudio.app.export

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import com.pocketstudio.app.mixer.TargetPick
import com.pocketstudio.model.TargetChoice
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 7, lot 5 D3 (seat 3b, #32): the export sheet said other words in the Mode Pro than on the Mode simple's page
 * for the same choice (« Prêt à partager ? » / « Fini ? Partage ton morceau », « Auto (House) · −8 LUFS » / « Auto
 * (style) », « Désactivée » only in Pro). One sheet, one set of words: the same title and the same target chips.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-port-hdpi", fontScale = 1.0f)
class ShareSameWordsTest {
    @get:Rule
    val compose = createComposeRule()

    private fun chips(): List<String> = TargetChoice.entries.mapNotNull { c ->
        compose.onAllNodesWithTag("share_target_${c.name}").fetchSemanticsNodes().firstOrNull()
            ?.config?.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text }?.let { "${c.name}=$it" }
    }

    private fun titled(): Boolean = compose.onAllNodesWithText(tr("Prêt à partager ?")).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun simpleAndProSayTheSameWords() {
        I18n.lang = Lang.FR
        val ui = ShareSheetUi(name = "House", seconds = 158, parts = 4, checks = emptyList(), projectName = "House",
            target = TargetPick.of(com.pocketstudio.model.Project(style = "house")))
        var simple by mutableStateOf(true)
        compose.setContent { PocketStudioTheme { ShareSheetContent(ui, ShareSheetActions(), page = simple, simple = simple) } }
        compose.waitForIdle()
        val simpleChips = chips()
        val simpleTitle = titled()
        simple = false
        compose.waitForIdle()
        val proChips = chips()
        val proTitle = titled()
        assertTrue("the target chips are shown: $simpleChips", simpleChips.isNotEmpty())
        assertEquals("the same target chips in Simple and Pro", simpleChips, proChips)
        assertTrue("the same title « Prêt à partager ? » in Simple ($simpleTitle) and Pro ($proTitle)", simpleTitle && proTitle)
    }
}
