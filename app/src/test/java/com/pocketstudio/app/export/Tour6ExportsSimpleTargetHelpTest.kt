package com.pocketstudio.app.export

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import com.pocketstudio.app.mixer.TargetPick
import com.pocketstudio.model.Project
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
 * Tour 6, E1 (CI of 35930493, ScreenScanTest census of NavScreenshotTest.simpleSend): the Mode simple's « Volume visé »
 * chips came without a « ? » (rule: every control has its help). Its « ? » is there, in plain words (no LUFS).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-port-hdpi")
class Tour6ExportsSimpleTargetHelpTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theSimpleTargetHasItsHelp() {
        I18n.lang = Lang.FR
        val ui = ShareSheetUi(name = "Trap", seconds = 178, parts = 26, checks = emptyList(), projectName = "Trap", exportLufs = -8.5,
            target = TargetPick.of(Project(style = "trap")))
        compose.setContent { PocketStudioTheme { ShareSheetContent(ui, ShareSheetActions(), page = true, simple = true) } }
        compose.waitForIdle()
        assertEquals("one « ? » for « Volume visé » on the Simple page", 1,
            compose.onAllNodesWithTag("help_" + tr("Volume visé"), useUnmergedTree = true).fetchSemanticsNodes().size)
    }
}
