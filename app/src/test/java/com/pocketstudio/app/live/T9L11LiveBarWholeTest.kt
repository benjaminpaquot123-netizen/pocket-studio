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
 * Tour 9, L10's rests (N3, Arabic, 360 dp, 200 %): in the Live's bar the « BPM » under the tempo (88 px of lines in 35),
 * the help's « ? » (88 in 54), « مازورة واحدة » in « Lancer à » and « الخانات » in « Cases » (132 in 126) stood taller
 * than their boxes, cut at the top and bottom. And the bar counter, fitted into its fixed 56 x 48 dp, could shrink its
 * figures to nothing. Acceptance: every word of the Live's bar and of the row over the grid is whole, in every language
 * and text size, and the counter and the tempo keep figures one can read (never under [READABLE] dp).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xxhdpi")
class T9L11LiveBarWholeTest {
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

    /** The nearest test tag over [n] (its own first). */
    private fun tagOf(n: SemanticsNode): String? {
        var up: SemanticsNode? = n
        while (up != null) { up.config.getOrNull(SemanticsProperties.TestTag)?.let { return it }; up = up.parent }
        return null
    }

    private fun live(lang: Lang, rtl: Boolean, scale: Float) {
        I18n.lang = lang
        val pads = LivePads(8).with(0, LiveClip("k.wav", "Kick 909", 1)).with(1, LiveClip("c.wav", "Charley", 2))
        compose.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, scale), LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
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
        val all = texts().map { (n, t) -> Triple(n, t, tagOf(n)) }
        val bar = all.filter { (_, _, tag) -> tag != null && (tag in BAR || tag.startsWith("help_")) }
        assertTrue("$lang ${scale * 100} %: no text read in the Live's bar (${all.map { it.third }.distinct()})", bar.any { it.third == "live_counter" } && bar.any { it.third == "live_tempo" })
        val cut = bar.mapNotNull { (n, t, tag) ->
            // A layout's own width is its constraint's (not the words'): the words' widest line is read instead.
            val wide = (0 until t.lineCount).maxOfOrNull { t.getLineRight(it) - t.getLineLeft(it) } ?: 0f
            val cutShort = t.multiParagraph.didExceedMaxLines || (0 until t.lineCount).any { t.isLineEllipsized(it) }
            val outside = t.multiParagraph.height > t.size.height + 0.5f || wide > t.size.width + 0.5f || cutShort ||
                n.boundsInRoot.height < 0.9f * n.size.height || n.boundsInRoot.width < 0.9f * n.size.width
            if (!outside) null
            else "« ${n.config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text }} » (in $tag): ${t.multiParagraph.height} px of lines in a box of ${t.size.height} px, " +
                "$wide px wide in ${t.size.width}${if (cutShort) " (cut short: « … » or lines left out)" else ""}, drawn ${n.boundsInRoot.size} of ${n.size}"
        }
        assertTrue("$lang, 360 dp, ${scale * 100} %: words of the Live's bar cut\n" + cut.joinToString("\n"), cut.isEmpty())
        val small = bar.filter { it.third == "live_counter" || it.third == "live_tempo" }.mapNotNull { (n, t, tag) ->
            val dp = t.layoutInput.style.fontSize.value * scale
            if (dp >= READABLE) null else "« ${n.config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text }} » (in $tag) set at $dp dp"
        }
        assertTrue("$lang, 360 dp, ${scale * 100} %: figures too small to read\n" + small.joinToString("\n"), small.isEmpty())
    }

    @Test fun arabicAt200() = live(Lang.AR, rtl = true, scale = 2f)
    @Test fun frenchAt200() = live(Lang.FR, rtl = false, scale = 2f)
    @Test fun frenchAt130() = live(Lang.FR, rtl = false, scale = 1.3f)
    @Test fun germanAtTheSmallestSize() = live(Lang.DE, rtl = false, scale = 0.85f)

    private companion object {
        /** The words looked at: the Live's bar (tempo, counter) and the row over the grid (« Lancer à », « Cases »). */
        val BAR = setOf("live_top_bar", "live_tempo", "live_tempo_box", "live_counter", "live_quantum", "live_cases")

        /** The smallest figure one reads at a glance on a phone, in dp. */
        const val READABLE = 9f
    }
}
