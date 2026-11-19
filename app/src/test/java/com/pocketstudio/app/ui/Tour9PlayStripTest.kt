package com.pocketstudio.app.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.LayoutDirection
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.Project
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.switchScene
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
 * Tour 9, lot L6, F105: the top indicator said « Intro mesure 1/84 » while Refrain was edited, or cut its figures.
 * Acceptance: « L'indicateur du haut nomme la partie que j'édite, avec ses mesures lisibles en entier. »
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Tour9PlayStripTest {
    @get:Rule val compose = createComposeRule()

    @After fun french() { I18n.lang = Lang.FR; I18n.everyday = false }

    /** A one bar, B two bars; the song A, B ×2, A (bars 1 | 2-5 | 6). */
    private val song = Project(
        scenes = listOf(Scene("A", DrumPattern(), label = "Intro"), Scene("B", DrumPattern().copy(steps = 32), label = "Refrain")),
        song = listOf(SongPart(0), SongPart(1, 2), SongPart(0)),
    ).sanitized()

    @Test
    fun stoppedItNamesThePartBeingEdited() {
        // Refrain edited, the start point left on Intro (the part was switched to otherwise than by picking it).
        val pos = playPosition(song.switchScene(1), playing = false, songIndex = -1, bar = -1, playFrom = 0, loopPart = false)
        assertEquals(PlayPosition("Refrain", 2, 6), pos)
        // The start point already one of its parts: that one.
        assertEquals(PlayPosition("Intro", 6, 6), playPosition(song.switchScene(0), false, -1, -1, playFrom = 2, loopPart = false))
    }

    private fun layout(tag: String): Pair<String, TextLayoutResult> {
        val node = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()
        val out = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(out)
        return node.config.getOrNull(SemanticsProperties.Text)!!.joinToString("") { it.text } to out.single()
    }

    private fun cut(l: TextLayoutResult) = l.didOverflowHeight || (0 until l.lineCount).any { l.isLineEllipsized(it) } ||
        (l.lineCount == 1 && l.multiParagraph.intrinsics.maxIntrinsicWidth > l.size.width + 1f)

    /** The strip of every tab (the tempo, the loop chip, the « ? »): the bar reads its two figures whole. */
    private fun barsReadWhole(lang: Lang, everyday: Boolean) {
        I18n.lang = lang
        I18n.everyday = everyday
        val pos = PlayPosition("Refrain", 1, 84)
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides if (lang.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                PocketStudioTheme {
                    PlayStrip(
                        PlayStripUi(120.0, hasSong = true, loopPart = false, playing = false, position = { pos }, parts = listOf("A", "B"), helpOn = false),
                        PlayStripActions(),
                    )
                }
            }
        }
        compose.waitForIdle()
        val (s, l) = layout("top_position_bar")
        assertTrue("« $s » lost a figure", "1/84" in s.replace(" ", "") || "١/٨٤" in s)
        assertFalse("« $s » is cut ($lang)", cut(l))
    }

    @Test @Config(qualifiers = "w360dp-h640dp", fontScale = 2f)
    fun theBarsReadWholeWithBigText() = barsReadWhole(Lang.FR, everyday = true)

    @Test @Config(qualifiers = "w360dp-h640dp", fontScale = 2f)
    fun theShortBarsReadWholeWithBigText() = barsReadWhole(Lang.FR, everyday = false)

    @Test @Config(qualifiers = "w360dp-h640dp", fontScale = 2f)
    fun theBarsReadWholeInArabic() = barsReadWhole(Lang.AR, everyday = true)

    @Test @Config(qualifiers = "w360dp-h640dp")
    fun theWholeWordStaysWhereItFits() {
        barsReadWhole(Lang.FR, everyday = true)
        assertEquals("mesure 1/84", layout("top_position_bar").first)
    }
}
