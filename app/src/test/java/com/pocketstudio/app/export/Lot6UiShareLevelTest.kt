package com.pocketstudio.app.export

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Lot 6, U2 (seat A c, screens 7 and 14): in the Mode simple, « Niveau : comme la musique en streaming (−14) » was the
 * meter's reading while the song played, not what the file aims at (« Auto (Trap) · −8,5 LUFS » in Pro): « mon m4a
 * n'était pas streaming ». The level said in Simple is the export's when a target is set; without one, what is heard.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-port-hdpi", fontScale = 1.3f)
class Lot6UiShareLevelTest {
    @get:Rule
    val compose = createComposeRule()

    private fun said(ui: ShareSheetUi): String {
        I18n.lang = Lang.FR
        compose.setContent { PocketStudioTheme { ShareSheetContent(ui, ShareSheetActions(), page = true, simple = true) } }
        compose.waitForIdle()
        return compose.onNodeWithTag("share_loudness").fetchSemanticsNode().config[SemanticsProperties.Text].joinToString("") { it.text }
    }

    private val base = ShareSheetUi(name = "Nuit trap", seconds = 150, parts = 4, checks = emptyList(), projectName = "Nuit trap")

    @Test
    fun theSimpleLevelIsTheExportsTarget() {
        // Heard at −14 LUFS while the file aims at −8,5 (the Trap style's « Auto »).
        assertEquals(tr("Niveau : %s", Loudness.describe(-8.5)), said(base.copy(lufs = -14.0, exportLufs = -8.5)))
    }

    @Test
    fun theSimpleLevelIsSaidBeforeAnyListen() {
        // Not played yet (no reading): the file's level is known all the same.
        assertEquals(tr("Niveau : %s", Loudness.describe(-8.5)), said(base.copy(lufs = null, exportLufs = -8.5)))
    }

    @Test
    fun withoutATargetTheSimpleLevelIsWhatIsHeard() {
        assertEquals(tr("Niveau : %s", Loudness.describe(-14.0)), said(base.copy(lufs = -14.0, exportLufs = null)))
    }

    @Test
    fun theRouteGivesTheProjectsTarget() {
        // The Route's value: the project's one target (Trap style's « Auto »); none when the target is off.
        val trap = com.pocketstudio.model.Project(style = "trap").let { it.copy(mixer = it.mixer.copy(target = com.pocketstudio.model.LoudnessTarget.AUTO)) }
        assertEquals(com.pocketstudio.model.LoudnessTargets.of(trap)?.toDouble(), shareExportLufs(trap))
        val off = trap.copy(mixer = trap.mixer.copy(target = com.pocketstudio.model.LoudnessTarget.OFF, pro = null))
        assertEquals(null, shareExportLufs(off))
    }
}
