package com.pocketstudio.app.export

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
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

/**
 * Tour 6, seat A c (E1): in Mode simple the seat could not pick « streaming » or « club » without slipping into Pro
 * (« Autres fichiers … passe en mode Pro »), and the level shown did not carry the file's number. The Mode simple's
 * « Envoyer » page now offers the same target, in plain words (« Streaming », « Club »), so a file of either kind is
 * two touches away; and the « Niveau » line carries the file's number, as Pro's does.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-port-hdpi", fontScale = 1.0f)
class Tour6ExportsSimpleTargetTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun french() { I18n.lang = Lang.FR }

    private val base = ShareSheetUi(name = "Trap", seconds = 158, parts = 4, checks = emptyList(), projectName = "Trap")

    private fun show(ui: ShareSheetUi, chosen: MutableList<TargetChoice>) {
        I18n.lang = Lang.FR
        compose.setContent {
            PocketStudioTheme {
                ShareSheetContent(ui, ShareSheetActions(chooseTarget = { chosen += it }), page = true, simple = true)
            }
        }
        compose.waitForIdle()
    }

    // Merged tree: a FilterChip (clickable) carries its label text on its tagged node; a plain tagged Text does too.
    private fun tagText(tag: String): String? =
        compose.onAllNodesWithTag(tag).fetchSemanticsNodes().firstOrNull()
            ?.config?.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text }

    @Test
    fun theSimplePageOffersStreamingAndClubWithoutGoingPro() {
        val chosen = mutableListOf<TargetChoice>()
        val ui = base.copy(target = TargetPick.of(com.pocketstudio.model.Project(style = "trap")), exportLufs = -8.5)
        show(ui, chosen)
        val streaming = tagText("share_target_STREAMING")
        val club = tagText("share_target_CLUB")
        assertTrue("the Simple page shows a « Streaming » target, got « $streaming »", streaming?.contains("Streaming", ignoreCase = true) == true)
        assertTrue("the Simple page shows a « Club » target, got « $club »", club?.contains("Club", ignoreCase = true) == true)
        // No LUFS jargon on the plain chips.
        assertTrue("the Simple chips stay plain (no LUFS), got « $streaming » / « $club »",
            streaming?.contains("LUFS", ignoreCase = true) != true && club?.contains("LUFS", ignoreCase = true) != true)
    }

    @Test
    fun theSimpleLevelLineCarriesTheFilesNumber() {
        val ui = base.copy(target = TargetPick.of(com.pocketstudio.model.Project(style = "trap")), exportLufs = -8.5, lufs = -14.0)
        show(ui, mutableListOf())
        val level = tagText("share_loudness").orEmpty()
        assertTrue("the Simple level line must carry the file's number (−8,5), got « $level »", level.contains("8,5") || level.contains("8.5"))
    }
}
