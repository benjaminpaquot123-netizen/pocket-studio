package com.pocketstudio.app.tuto

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 6, N22: the panel folded on a step, the chapters were a bare list icon nobody read as such (seat ja: « no
 * チャプター button, I took the ← arrow »). The way to the chapters says its name (« Chapitres »), whole, in every
 * language, at 100 and 130 %, folded or not, on a 360 dp phone.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp")
class Tour6TutoPanelChaptersTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun end() {
        I18n.lang = Lang.FR
    }

    @Test
    fun theChaptersAreNamedOnTheFoldedPanel() {
        val chapters = TutoCourse.of("techno")!!.chaptersFor(TutoLevel.Facile)
        var lang by mutableStateOf(Lang.FR)
        var scale by mutableStateOf(1f)
        var open by mutableStateOf(false)
        compose.setContent {
            val d = LocalDensity.current
            key(lang, scale, open) {
                CompositionLocalProvider(LocalDensity provides Density(d.density, scale)) {
                    val ui = TutoUi(TutoProgress("techno", TutoLevel.Facile, current = chapters[3].steps[0].id), chapters, 3, 0, open = open)
                    PocketStudioTheme { TutoPanel(ui, canShow = true, actions = TutoActions(), reducedMotion = true) }
                }
            }
        }
        val wrong = ArrayList<String>()
        for (l in Lang.entries) for (s in listOf(1f, 1.3f)) for (o in listOf(false, true)) {
            I18n.lang = l
            lang = l
            scale = s
            open = o
            compose.waitForIdle()
            val name = tr("Chapitres")
            val nodes = compose.onAllNodes(hasText(name) and hasAnyAncestor(hasTestTag("tuto_chapters")), useUnmergedTree = true).fetchSemanticsNodes()
            val where = "${l.code} ${(s * 100).toInt()} % ${if (o) "open" else "folded"}"
            if (nodes.isEmpty()) { wrong += "$where: no « $name »"; continue }
            val layouts = ArrayList<TextLayoutResult>()
            nodes.first().config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
            // The word on one line as it wants to be, against the room it was given (cut: smaller).
            val wide = layouts.firstOrNull()?.multiParagraph?.maxIntrinsicWidth ?: 0f
            if (wide > nodes.first().size.width + 1) wrong += "$where: « ${nodes.first().config.getOrNull(SemanticsProperties.Text)?.joinToString()} » cut"
        }
        assertTrue("the chapters not named: $wrong", wrong.isEmpty())
    }
}
