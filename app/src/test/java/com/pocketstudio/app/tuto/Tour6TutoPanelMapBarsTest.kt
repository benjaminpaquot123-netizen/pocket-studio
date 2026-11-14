package com.pocketstudio.app.tuto

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.i18n.tr
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
 * Tour 6, N30: on the song's map the bars' number went out of its block (« 16 » in Arabic, « 끝 » in Korean) at 130 %,
 * on a 360 dp phone. Every number written in a block fits it whole, in every language, at 100 and 130 %
 * ([Tour6TutoPanelMapNamesTest]: the names).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp")
class Tour6TutoPanelMapBarsTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun end() {
        I18n.lang = Lang.FR
    }

    /** The words written in the map's blocks that do not fit them, « lang scale: text », [numbers]: the bars' numbers, else the names. */
    private fun cut(numbers: Boolean): List<String> {
        var lang by mutableStateOf(Lang.FR)
        var scale by mutableStateOf(1f)
        // Every style's map: their short parts make the narrowest blocks.
        var style by mutableStateOf("techno")
        compose.setContent {
            val d = LocalDensity.current
            val blocks = tutoMap(style)
            key(lang, scale, style) {
                CompositionLocalProvider(LocalDensity provides Density(d.density, scale)) {
                    // The panel's words on a 360 dp phone: its 14 dp margins on each side.
                    PocketStudioTheme { Box(Modifier.width(332.dp)) { SongMap(blocks) } }
                }
            }
        }
        val out = ArrayList<String>()
        for (st in com.pocketstudio.model.tuto.TutoCourse.STYLES) for (l in Lang.entries) for (s in listOf(1f, 1.3f)) {
            I18n.lang = l
            lang = l
            scale = s
            style = st
            compose.waitForIdle()
            val nodes = compose.onAllNodes(hasAnyAncestor(hasTestTag("tuto_map")) and !hasAnyAncestor(hasTestTag("tuto_map_names")), useUnmergedTree = true).fetchSemanticsNodes()
            for (n in nodes) {
                val text = n.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text } ?: continue
                // The map's own lines (its title, its caption) wrap: only the words in its blocks.
                if (text.isBlank() || text == tr("Énergie") || text == tr("Chaque nombre : la durée de la partie en mesures.")) continue
                if (text.all { it.isDigit() } != numbers) continue
                val layouts = ArrayList<TextLayoutResult>()
                n.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
                val layout = layouts.firstOrNull() ?: continue
                // The words on one line as they want to be, against the room their text was given (clipped: smaller).
                val wide = layout.multiParagraph.maxIntrinsicWidth
                val high = layout.multiParagraph.height
                // Or drawn past its block (clipped by it: its bounds on the screen smaller than itself).
                val seen = n.boundsInRoot
                val clipped = seen.width < n.size.width - 1 || seen.height < n.size.height - 1
                if (wide > n.size.width + 1 || high > n.size.height + 1 || clipped) out += "$st ${l.code} ${(s * 100).toInt()} %: « $text » ${wide.toInt()}x${high.toInt()} in ${n.size.width}x${n.size.height}"
            }
        }
        return out
    }

    @Test
    fun everyBarsNumberFitsItsBlock() {
        val cut = cut(numbers = true)
        assertTrue("numbers out of their block: $cut", cut.isEmpty())
    }
}
