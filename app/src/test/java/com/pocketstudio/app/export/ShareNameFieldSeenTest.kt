package com.pocketstudio.app.export

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.PocketStudioTheme
import com.pocketstudio.ui.UiScaled
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 9, F123: « M4A / WAV ne cache pas le champ du nom ». Seat t9-74 (en, 200 %, screen 67): only the field's label
 * was readable, the name in it lay under the pinned M4A / WAV chips. At 200 % on a 360 dp phone, the sheet opened as it
 * opens, the typed name is drawn whole above the chips, clear of them.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp")
class ShareNameFieldSeenTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun french() { I18n.lang = Lang.FR }

    private fun bounds(m: SemanticsMatcher): Rect = compose.onNode(m, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    /** The field's own height (what is shown of it, [bounds], is cut by the scroll's frame). */
    private fun whole(m: SemanticsMatcher): Int = compose.onNode(m, useUnmergedTree = true).fetchSemanticsNode().size.height

    private var case by androidx.compose.runtime.mutableStateOf(Triple(Lang.EN, true, false))

    private fun show(lang: Lang, simple: Boolean, warned: Boolean) {
        I18n.lang = lang
        case = Triple(lang, simple, warned)
        compose.waitForIdle()
    }

    private fun start() = compose.setContent {
        val (lang, simple, warned) = case
        androidx.compose.runtime.key(lang, simple, warned) {
            val ui = ShareSheetUi(
                name = "My song", seconds = 158, parts = 4, checks = if (warned) listOf(ShareCheck.Short(25)) else emptyList(),
                projectName = "My song", lufs = -12.0,
            )
            PocketStudioTheme {
                UiScaled(2f) {
                    // The sheet under the status bar, as tall as a phone gives it at 200 %.
                    Box(Modifier.fillMaxWidth().height(340.dp)) { ShareSheetContent(ui, ShareSheetActions(sendProject = {}), simple = simple) }
                }
            }
        }
    }

    @Test
    fun theTypedNameIsSeenAboveTheFormatChipsAt200Percent() {
        val faults = mutableListOf<String>()
        start()
        for (lang in listOf(Lang.EN, Lang.FR, Lang.AR)) for (simple in listOf(true, false)) for (warned in listOf(false, true)) {
            show(lang, simple, warned)
            val typed = bounds(hasText("My song") and SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText))
            val chip = bounds(hasTestTag("share_format_M4a"))
            val field = bounds(hasTestTag("share_name"))
            val shown = field.height
            val full = whole(hasTestTag("share_name"))
            val overChips = chip.height > 0f && chip.top < field.bottom && chip.bottom > field.top
            if (typed.height < 1f || shown < full - 1f || overChips) {
                faults += "${lang.code} simple=$simple warned=$warned: field shown $shown of $full px, chips ${chip.top}..${chip.bottom}"
            }
        }
        assertTrue(faults.joinToString("\n"), faults.isEmpty())
    }
}
