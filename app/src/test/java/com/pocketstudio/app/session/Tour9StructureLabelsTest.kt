package com.pocketstudio.app.session

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.LayoutDirection
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 9, lot L6, Structure's words:
 * - F100 « Le titre de la liste d'ajout ne change pas après un ajout. »
 * - F108 « Le bouton d'ajout se lit en entier, même en arabe. »
 * - F109 « Chaque bloc du morceau montre au moins la lettre ou le nom de sa partie. »
 * - F110 « Les rangées du morceau gardent « N mesures » sur une seule ligne. »
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Tour9StructureLabelsTest {
    @get:Rule val compose = createComposeRule()

    @After fun french() { I18n.lang = Lang.FR }

    private var labels = listOf("Intro", "Couplet", "Refrain", "Pont")

    /** The language shown: a test going through every language sets the screen once and changes only this. */
    private var shown: androidx.compose.runtime.MutableState<Lang>? = null

    private fun show(lang: Lang = Lang.FR, parts: Int = 0, playingAt: Int = -1, mixed: Boolean = false) {
        I18n.lang = lang
        shown?.let { it.value = lang; compose.waitForIdle(); return }
        val state = mutableStateOf(lang).also { shown = it }
        compose.setContent {
            var song by remember { mutableStateOf(List(parts) { SongPart(scene = it % labels.size, repeats = 2, from = if (mixed) mapOf("kick" to 1) else emptyMap()) }) }
            val l = state.value
            CompositionLocalProvider(LocalLayoutDirection provides if (l.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                androidx.compose.runtime.key(l) {
                    PocketStudioTheme {
                        SessionScreen(
                            SessionScreenUi(
                                scenes = List(Scene.MAX) { if (it < labels.size) Scene(name = Scene.NAMES[it], label = labels[it], drums = com.pocketstudio.model.DrumPattern(steps = 128), chords = if (it < 3) listOf(0, 5, 3, 4) else emptyList()) else null }, current = 2, pending = null,
                                song = song, songIndex = playingAt, playing = playingAt >= 0, tempoBpm = 120.0,
                            ),
                            SessionActions(addPart = { s -> song = song + SongPart(scene = s, repeats = 2) }),
                            arrangeOnly = true,
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun layout(node: SemanticsNode): TextLayoutResult {
        val out = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(out)
        return out.single()
    }

    /**
     * A text drawn cut: taller than its box, ended by « … », or a single line wider than its box. (Compose sizes a short
     * line to its words while its paragraph keeps the whole room, so `didOverflowWidth` alone says « cut » wrongly.)
     */
    private fun cut(l: TextLayoutResult) = l.didOverflowHeight || (0 until l.lineCount).any { l.isLineEllipsized(it) } ||
        (l.lineCount == 1 && l.multiParagraph.intrinsics.maxIntrinsicWidth > l.size.width + 1f)

    private fun text(node: SemanticsNode) = node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }.orEmpty()

    @Test @Config(qualifiers = "w360dp-h640dp")
    fun theAddListKeepsItsTitleAfterAnAdd() {
        show()
        assertEquals(listOf("Ajouter :"), addTitles())
        compose.onNodeWithTag("add_part_0").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(listOf("Ajouter :"), addTitles())
    }

    /** The words starting with « Ajouter » on the screen (French spaces made plain). */
    private fun addTitles() = compose.onAllNodes(androidx.compose.ui.test.hasText("Ajouter", substring = true), useUnmergedTree = true)
        .fetchSemanticsNodes().map { text(it).replace('\u00A0', ' ').replace('\u202F', ' ') }.filter { it.startsWith("Ajouter") }

    /** Every add button's label reads whole and the button stays on the screen. */
    private fun addButtonsReadWhole() {
        val screen = compose.onRoot().fetchSemanticsNode().boundsInRoot
        for (tag in listOf("add_part_new_copy", "add_part_new_empty", "add_part_0", "add_part_2")) {
            val button = compose.onNodeWithTag(tag).performScrollTo().fetchSemanticsNode()
            assertTrue("« $tag » goes out of the screen: ${button.boundsInRoot} on $screen", button.boundsInRoot.left >= screen.left - 1f && button.boundsInRoot.right <= screen.right + 1f)
            val words = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().children.flatMap { listOf(it) + it.children }
                .filter { it.config.contains(SemanticsActions.GetTextLayoutResult) }
            assertTrue("« $tag » has no words", words.isNotEmpty())
            for (w in words) assertFalse("« $tag » is cut: « ${text(w)} »", cut(layout(w)))
        }
    }

    @Test @Config(qualifiers = "w360dp-h640dp")
    fun theNewPartButtonsReadWholeOnASmallPhone() { show(); addButtonsReadWhole() }

    @Test @Config(qualifiers = "w360dp-h640dp")
    fun theNewPartButtonsReadWholeInArabic() { show(Lang.AR); addButtonsReadWhole() }

    @Test @Config(qualifiers = "w360dp-h640dp", fontScale = 2f)
    fun theNewPartButtonsReadWholeWithBigText() { show(); addButtonsReadWhole() }

    /** Each song row: its title shows its letter at least, « N mes. » sits on one line, no word cut in two, no lone dot. */
    private fun rowsReadWell(parts: Int, lang: Lang = Lang.FR, playingAt: Int = -1, mixed: Boolean = false) {
        show(lang, parts, playingAt, mixed)
        for (i in 0 until parts) {
            val row = compose.onNodeWithTag("part_$i", useUnmergedTree = true).performScrollTo().fetchSemanticsNode()
            val words = generateSequence(listOf(row)) { level -> level.flatMap { it.children }.takeIf { it.isNotEmpty() } }.flatten()
                .filter { it.config.contains(SemanticsActions.GetTextLayoutResult) }.toList()
            val letter = Scene.NAMES[i % labels.size]
            val title = words.firstOrNull { text(it).startsWith(letter) }
            assertTrue("row $i shows no title among ${words.map(::text)}", title != null)
            val t = layout(title!!)
            assertTrue("row $i: its title « ${text(title)} » loses its letter (${t.getLineEnd(0, visibleEnd = true)})", t.getLineEnd(0, visibleEnd = true) >= 1 && !text(title).substring(0, t.getLineEnd(0, visibleEnd = true)).startsWith("…"))
            val bars = words.firstOrNull { "mes." in text(it) }
            assertTrue("row $i shows no length among ${words.map(::text)}", bars != null)
            val s = text(bars!!)
            val l = layout(bars)
            assertFalse("row $i: « $s » is cut", cut(l))
            val at = Regex("""\d+[\s\u00A0\u202F]?mes\.""").find(s)!!.range
            assertEquals("row $i: « $s » breaks inside « ${s.substring(at)} »", l.getLineForOffset(at.first), l.getLineForOffset(at.last))
            for (line in 0 until l.lineCount) {
                val piece = s.substring(l.getLineStart(line), l.getLineEnd(line)).trim()
                assertTrue("row $i: line ${line + 1} of « $s » is only « $piece »", piece.isNotEmpty() && piece != "." && piece != "·")
            }
        }
    }

    @Test @Config(qualifiers = "w360dp-h640dp", fontScale = 2f)
    fun songRowsKeepTheirNameAndLengthWithBigText() = rowsReadWell(parts = 4)

    @Test @Config(qualifiers = "w320dp-h640dp", fontScale = 1.3f)
    fun songRowsKeepTheirNameAndLengthOnANarrowPhone() = rowsReadWell(parts = 4)

    @Test @Config(qualifiers = "w360dp-h640dp", fontScale = 2f)
    fun songRowsPlayingAndMixedKeepTheirNameAndLength() {
        labels = listOf("Intro", "Couplet 2", "Refrain +", "Pont")
        rowsReadWell(parts = 4, playingAt = 1, mixed = true)
    }

    @Test @Config(qualifiers = "w360dp-h640dp", fontScale = 1.5f)
    fun songRowsReadWellInEveryLanguage() {
        labels = listOf("Intro", "Couplet 2", "Refrain +", "Pont")
        for (lang in Lang.entries) {
            I18n.lang = lang
            if ("mes." !in com.pocketstudio.app.ui.barsShort(8)) continue
            rowsReadWell(parts = 4, lang = lang, playingAt = 1, mixed = true)
        }
    }

    /**
     * The bricks (« blocs ») of the song: each shows its letter whole, its name not reduced to « .. », and « N mes. »
     * on one line, never « 8 » alone nor a lone dot.
     */
    private fun bricksReadWell(lang: Lang) {
        show(lang)
        for (i in labels.indices) {
            val tile = compose.onNodeWithTag("scene_$i", useUnmergedTree = true).performScrollTo().fetchSemanticsNode()
            val words = generateSequence(listOf(tile)) { level -> level.flatMap { it.children }.takeIf { it.isNotEmpty() } }.flatten()
                .filter { it.config.contains(SemanticsActions.GetTextLayoutResult) }.toList()
            val where = "brick ${Scene.NAMES[i]} in $lang (${words.map(::text)})"
            assertTrue("$where: no words", words.size >= 3)
            for (w in words) {
                val l = layout(w)
                val s = text(w)
                assertFalse("$where: « $s » is cut", cut(l))
                assertEquals("$where: « $s » goes on ${l.lineCount} lines", 1, l.lineCount)
                assertEquals("$where: « $s » is shortened", s.length, l.getLineEnd(0, visibleEnd = true))
            }
        }
    }

    @Test @Config(qualifiers = "w360dp-h640dp", fontScale = 2f)
    fun bricksReadWellWithBigText() = bricksReadWell(Lang.FR)

    @Test @Config(qualifiers = "w320dp-h640dp", fontScale = 1.3f)
    fun bricksReadWellInEveryLanguage() {
        labels = listOf("Intro", "Couplet 2", "Refrain +", "Pont")
        for (lang in Lang.entries) bricksReadWell(lang)
    }
}
