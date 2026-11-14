package com.pocketstudio.app.synth

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Lot 6, R19 (its TrackBar part: « + 트랙 » D b q9, « 303 basu » F b q1, « Geschlossene Hi-Hats » B b-suite q9): the
 * row of tracks went on past the screen's edge with a chip cut there, its soft fade read as a plain cut. While the row
 * goes on off screen, its edge carries a sign (a chevron over the fade) on that side; it takes no touch (the chip under
 * it still answers), and at the row's end it is gone.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-port-hdpi", fontScale = 1.3f)
class Lot6UiTrackBarMoreTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun french() { I18n.lang = Lang.FR }

    private fun node(tag: String): SemanticsNode? =
        compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes(atLeastOneRootRequired = false).firstOrNull()

    private fun show(lang: Lang, simple: Boolean, n: Int = 6, selected: Int = 0, name: String = "Geschlossene Hi-Hats", sound: Boolean = true) {
        I18n.lang = lang
        val tracks = List(n) { InstrumentTrack(it, "$name $it") }
        compose.setContent { PocketStudioTheme { TrackBar(TracksUi(tracks, selected, changeSound = if (sound) ({ _: Int -> }) else null, effects = if (sound) ({ _: Int -> }) else null, simple = simple)) } }
        compose.waitForIdle()
    }

    private fun scrollBy(dx: Float) {
        compose.onNodeWithTag("track_bar").performSemanticsAction(SemanticsActions.ScrollBy) { it(dx, 0f) }
        compose.waitForIdle()
    }

    private fun goesOn(lang: Lang, simple: Boolean) {
        show(lang, simple)
        val row = node("track_bar")!!.boundsInRoot
        val more = node("track_bar_more")
        assertNotNull("the row goes on past the right edge: a sign there", more)
        assertEquals("on the row's right edge", row.right, more!!.boundsInRoot.right, 2f)
        assertFalse("the sign takes no touch", more.config.contains(SemanticsActions.OnClick))
        assertNull("at the start, nothing before", node("track_bar_less"))
        // Half way: both sides go on.
        scrollBy(200f)
        assertNotNull(node("track_bar_more"))
        assertNotNull("scrolled: the left edge says it too", node("track_bar_less"))
        // At the end: nothing more after.
        scrollBy(100_000f)
        assertNull("at the end of the row, no sign after", node("track_bar_more"))
        assertNotNull(node("track_bar_less"))
    }

    @Test
    fun simpleGerman() = goesOn(Lang.DE, simple = true)

    @Test
    fun proKorean() = goesOn(Lang.KO, simple = false)

    @Test
    fun theChipUnderTheSignStillAnswers() {
        var picked = -1
        I18n.lang = Lang.FR
        val tracks = List(6) { InstrumentTrack(it, "Geschlossene Hi-Hats $it") }
        compose.setContent { PocketStudioTheme { TrackBar(TracksUi(tracks, 0, select = { picked = it })) } }
        compose.waitForIdle()
        val more = node("track_bar_more")!!.boundsInRoot
        // The chip cut at the edge, under the sign.
        val cut = (1 until 6).first { node("track_$it")!!.boundsInRoot.let { b -> b.left < more.left && b.right >= more.left } }
        compose.onNodeWithTag("track_$cut").performClick()
        compose.waitForIdle()
        assertEquals(cut, picked)
    }

    @Test
    fun aRowThatFitsSaysNothing() {
        show(Lang.FR, simple = true, n = 1, name = "Lead", sound = false)
        val range = node("track_bar")!!.config[androidx.compose.ui.semantics.SemanticsProperties.HorizontalScrollAxisRange]
        assertNull("the row fits (${range.value()} of ${range.maxValue()}, ${node("track_bar")!!.boundsInRoot})", node("track_bar_more"))
        assertNull(node("track_bar_less"))
    }
}
