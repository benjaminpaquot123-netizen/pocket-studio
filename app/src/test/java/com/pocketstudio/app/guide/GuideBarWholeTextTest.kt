package com.pocketstudio.app.guide

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import com.pocketstudio.model.guide.GuideStages
import com.pocketstudio.model.guide.GuideStatus
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
 * Tour 7, lot 3, D4: the Guide's bar cut its words at big text (pt-BR at 200 %: « Guia · Crie uma batida… »; hi: « गाइड »
 * cut). Every text of the bar is whole, on the smallest phone, folded and open.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-port-hdpi")
class GuideBarWholeTextTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun tearDown() {
        I18n.lang = Lang.FR
    }

    private fun whole(lang: Lang, open: Boolean) {
        I18n.lang = lang
        val card = GuideCardUi(
            "start.beat", GuideStages.START, "song", "decide", tr("Pose un rythme"), "", options = emptyList(),
            elsewhere = emptyList(), lexicon = "", status = GuideStatus.TODO, alert = false, undoable = false,
        )
        val ui = GuideUi(loading = false, percent = 0, done = 1, total = 30, next = card, step = 1)
        compose.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, 2f)) {
                PocketStudioTheme { GuideBar(ui, open = open, toggle = {}) }
            }
        }
        compose.waitForIdle()
        val cut = compose.onAllNodesWithTag("guide_bar", useUnmergedTree = true).fetchSemanticsNodes().flatMap { bar ->
            fun all(n: androidx.compose.ui.semantics.SemanticsNode): List<androidx.compose.ui.semantics.SemanticsNode> = listOf(n) + n.children.flatMap { all(it) }
            all(bar).mapNotNull { n ->
                val text = n.config.getOrNull(SemanticsProperties.Text)?.joinToString("") ?: return@mapNotNull null
                val layouts = mutableListOf<TextLayoutResult>()
                n.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
                val r = layouts.firstOrNull() ?: return@mapNotNull null
                // (didOverflowWidth is no help: a text 88.5 px wide in its 89 px box says it overflows.)
                val clipped = r.didOverflowHeight || (0 until r.lineCount).any { r.isLineEllipsized(it) } ||
                    r.lineCount == 1 && r.multiParagraph.maxIntrinsicWidth > r.size.width + 0.5f
                if (clipped && text.isNotBlank()) "« $text » [coupé]" else null
            }
        }
        assertTrue("${lang.code}, open=$open: ${cut.joinToString()}", cut.isEmpty())
    }

    @Test
    fun portugueseFolded() = whole(Lang.PT_BR, open = false)

    @Test
    fun portugueseOpen() = whole(Lang.PT_BR, open = true)

    @Test
    fun hindiFolded() = whole(Lang.HI, open = false)
}
