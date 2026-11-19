package com.pocketstudio.app.live

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.pocketstudio.app.session.LivePadsActions
import com.pocketstudio.app.session.LivePadsUi
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.LivePads
import com.pocketstudio.model.LiveState
import com.pocketstudio.model.Project
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
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
 * Tour 9, L10 N3: « En arabe, à 360 dp et 200 %, chaque titre de brique se lit en entier. » The Live's titles (the rows'
 * letters and names) are fitted texts; in Arabic at twice the text a title stood 16 to 21 dp taller than its box, its
 * top and bottom cut (the bar counter « مازورة »: 46 px of lines in 25). Every title of the grid, its rows, its stops and
 * the counter stands whole inside its box.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xxhdpi")
class LiveArabicTitlesTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun backToFrench() {
        I18n.lang = Lang.FR
    }

    private fun texts(): List<Pair<SemanticsNode, TextLayoutResult>> =
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true)
            .fetchSemanticsNodes(atLeastOneRootRequired = false).mapNotNull { n ->
                val results = ArrayList<TextLayoutResult>()
                n.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
                results.firstOrNull()?.let { n to it }
            }

    private fun live(lang: Lang, rtl: Boolean) {
        I18n.lang = lang
        val pads = LivePads(8).with(0, LiveClip("k.wav", "Kick 909", 1)).with(1, LiveClip("c.wav", "Charley", 2))
        compose.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, 2f), LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                PocketStudioTheme {
                    LiveScreen(
                        LiveScreenUi(Project(drums = DrumPattern().toggle(0, 0)).sanitized(), LiveState.on(0, 0), clock = 0, playing = false),
                        LiveActions(),
                        pads = LivePadsUi(pads, List(16) { 0 }, editing = null),
                        padsActions = LivePadsActions(),
                    )
                }
            }
        }
        compose.waitForIdle()
        val cut = texts().mapNotNull { (n, t) ->
            var up: SemanticsNode? = n
            var tag: String? = null
            while (up != null && tag == null) { tag = up.config.getOrNull(SemanticsProperties.TestTag); up = up.parent }
            val brick = tag != null && (tag == "live_grid" || tag == "live_counter" || tag.startsWith("live_row_") || tag.startsWith("live_stop_"))
            val outside = t.multiParagraph.height > t.size.height + 0.5f || n.boundsInRoot.height < 0.9f * n.size.height || n.boundsInRoot.width < 0.9f * n.size.width
            if (!brick || !outside) null
            else "« ${n.config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text }} » (in $tag): ${t.multiParagraph.height} px of lines in a box of ${t.size.height} px, drawn ${n.boundsInRoot.size} of ${n.size}"
        }
        assertTrue("$lang, 360 dp, 200 %: Live titles cut\n" + cut.joinToString("\n"), cut.isEmpty())
    }

    @Test fun arabic() = live(Lang.AR, rtl = true)
    @Test fun french() = live(Lang.FR, rtl = false)
}
