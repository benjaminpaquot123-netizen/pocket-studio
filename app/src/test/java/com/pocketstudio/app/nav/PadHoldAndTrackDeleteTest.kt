package com.pocketstudio.app.nav

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.graphics.toPixelMap
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.pocketstudio.model.SongPart
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration

/**
 * Owner (2026-11-04): « quand on fait un appui prolongé sur un pad, on arrive directement sur la
 * fenêtre Changer le son » (the pad's sheet is its card's « Réglages du pad »), and « J'aimerais
 * supprimer le synthé, je sais pas comment » (« Supprimer la piste » in « Mes pistes »).
 * Images: app/screenshots/nav/pad_settings_card.png, app/screenshots/nav/simple_track_delete.png.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class PadHoldAndTrackDeleteTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)

    @After
    fun done() = h.finish()

    private fun idle(ms: Long = 100) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
        compose.mainClock.advanceTimeBy(ms)
        compose.waitForIdle()
    }

    private fun tap(tag: String) {
        val node = compose.onNodeWithTag(tag)
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
        compose.waitForIdle()
    }

    private fun shown(tag: String) = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    private fun holdPadOpensTheLibraryForIt(simple: Boolean) {
        h.start(simple = simple)
        tap(if (simple) "tab_simple_Rythme" else "tab_Sons")
        compose.onNodeWithTag("pad_3").performTouchInput { longClick() }
        compose.waitForIdle()
        h.awaitTag("library")
        assertEquals("the pad held is the one the library is for", 3, h.vm.transport.value.selectedPad)
        assertTrue("« Pour le pad 4 (…) »", compose.onAllNodesWithText("le pad 4", substring = true).fetchSemanticsNodes().isNotEmpty())
        assertFalse("not its sheet", shown("pad_sheet"))
    }

    @Test
    fun aLongPressOnAPadOpensChangerLeSonForThatPad() = holdPadOpensTheLibraryForIt(simple = false)

    @Test
    fun theSameInModeSimple() = holdPadOpensTheLibraryForIt(simple = true)

    @Test
    fun thePadsSheetIsItsCardsReglagesDuPad() {
        h.start()
        tap("tab_Sons")
        h.vm.selectPad(2)
        compose.waitForIdle()
        compose.onNodeWithTag("pad_settings").performScrollTo()
        idle(300)
        captureScreenRoboImage("screenshots/nav/pad_settings_card.png")
        tap("pad_settings")
        h.awaitTag("pad_sheet")
        assertFalse(shown("library"))
    }

    @Test
    fun mesPistesDeletesTheSynthAndAnnulerBringsItBack() {
        h.start(simple = true)
        val synth = SongPart.inst(0)
        compose.onNodeWithTag("simple_tracks").performScrollTo()
        tap("simple_volume_$synth")
        compose.onNodeWithTag("simple_track_delete").performScrollTo()
        idle(300)
        captureScreenRoboImage("screenshots/nav/simple_track_delete.png")
        tap("simple_track_delete")
        idle()
        assertFalse("gone from « Mes pistes »", shown("simple_track_$synth"))
        assertTrue("the other tracks stay", shown("simple_track_${h.inst("Basse")}"))
        h.vm.undo()
        idle()
        assertTrue(shown("simple_track_$synth"))
    }

    @Test
    fun theBatteriesSheetOffersNoDelete() {
        h.start(simple = true)
        compose.onNodeWithTag("simple_tracks").performScrollTo()
        tap("simple_volume_${SongPart.DRUMS}")
        assertFalse(shown("simple_track_delete"))
    }

    /** Benjamin's capture: « Tom aigu (sy… » at the top right of « Son du pad »; now its own line, whole, at 130 % too. */
    @Test
    fun thePadsSoundNameIsShownWhole() {
        h.start(simple = true, scale = 1.3f)
        tap("tab_simple_Rythme")
        h.vm.selectPad(9)
        compose.waitForIdle()
        val node = compose.onNodeWithTag("pad_sound_name").performScrollTo()
        idle(300)
        captureScreenRoboImage("screenshots/nav/pad_sound_name_whole.png")
        val layouts = mutableListOf<TextLayoutResult>()
        node.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action!!.invoke(layouts)
        val text = layouts.single()
        assertTrue("« ${text.layoutInput.text} »", text.layoutInput.text.text.endsWith("(synthétisé)"))
        assertFalse("not cut", text.hasVisualOverflow || text.isLineEllipsized(text.lineCount - 1))
    }

    /**
     * Benjamin's capture: scrolled, the first row of pads showed cut in half right under « Partie : A ».
     * Now the page's top fades: just under the header the pads are (almost) the background.
     */
    @Test
    fun scrolledThePadsFadeUnderThePartLine() {
        h.start(simple = true)
        tap("tab_simple_Rythme")
        idle(300)
        val header = compose.onNodeWithTag("part_header").fetchSemanticsNode().boundsInRoot
        val pad = compose.onNodeWithTag("pad_0").fetchSemanticsNode().boundsInRoot
        // A fifth of the first row scrolled under the header (the rest below the fade).
        val by = (pad.top - header.bottom) + pad.height / 5
        compose.onNode(hasScrollAction() and hasAnyAncestor(hasTestTag("rhythm"))).performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, by) }
        idle(300)
        captureScreenRoboImage("screenshots/nav/rhythm_scrolled_under_part.png")
        val cut = compose.onNodeWithTag("pad_0").fetchSemanticsNode().boundsInRoot
        assertTrue("the first row partly under the header: $header $pad by $by -> $cut", cut.top <= header.bottom + 16f && cut.height < pad.height * 0.9f)
        val image = compose.onRoot().captureToImage().toPixelMap()
        val x = cut.center.x.toInt()
        fun lum(y: Float) = image[x, y.toInt()].let { 0.2126f * it.red + 0.7152f * it.green + 0.0722f * it.blue }
        // The page's top (where the pad is cut): there the fade starts.
        val edge = lum(cut.top + 2f)
        val body = lum(cut.bottom - 6f)
        val ground = lum(header.bottom - 2f)
        assertTrue("faded at the edge ($edge) like the ground ($ground), the pad below shows ($body)", kotlin.math.abs(edge - ground) < kotlin.math.abs(body - ground) / 3f)
    }
}
