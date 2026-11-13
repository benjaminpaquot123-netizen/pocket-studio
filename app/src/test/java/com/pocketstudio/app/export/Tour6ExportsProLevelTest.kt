package com.pocketstudio.app.export

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 6, control seat Ac-2 (new, screen 40): in Pro, « Club · −8 LUFS » chosen, the line above read « Volume : −13,0,
 * comme la musique en streaming (−14) » (what played, not what the file will be). The level said is the file's, as in
 * Mode simple: the same word and the same number in both modes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-port-hdpi")
class Tour6ExportsProLevelTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theProLevelIsTheFilesTarget() {
        I18n.lang = Lang.FR
        val ui = ShareSheetUi(name = "Trap", seconds = 178, parts = 26, checks = emptyList(), projectName = "Trap", lufs = -13.0, exportLufs = -8.0)
        compose.setContent { PocketStudioTheme { ShareSheetContent(ui, ShareSheetActions(), simple = false) } }
        compose.waitForIdle()
        val said = compose.onNodeWithTag("share_loudness").fetchSemanticsNode().config[SemanticsProperties.Text].joinToString("") { it.text }
        assertTrue("the file's number (−8,0): « $said »", "−8,0" in said)
        assertFalse("not the streaming words under Club: « $said »", "streaming" in said)
    }
}
