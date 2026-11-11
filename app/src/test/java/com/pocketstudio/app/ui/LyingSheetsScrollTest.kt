package com.pocketstudio.app.ui

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.padlive.PadLiveActions
import com.pocketstudio.app.padlive.PlayedTakeSheet
import com.pocketstudio.app.tuto.TutoKeySheet
import com.pocketstudio.model.Harmony
import com.pocketstudio.model.Scale
import com.pocketstudio.ui.PocketStudioTheme
import com.pocketstudio.ui.UiScaled
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Hands-on audit, chantier 9 (G10, rule 22 of docs/DESIGN.md « En paysage et à 130 %, chaque feuille défile ou se
 * réorganise »; round 5, seat E d #45: « seules les touches 1 à 6 sont visibles… contourné avec Fermer + 3 × Plus vite »).
 * On a phone lying down (800 × 360 dp): the tempo sheet's keypad, OK included, is whole on screen at 100 % and with big
 * text; at 130 % it scrolls under a header that stays (title, the tempo, « Fermer »). The sheets that did not scroll
 * (the take of Pad Live, the tutorial's key) scroll lying down at 130 %.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w800dp-h360dp-land-xxhdpi")
class LyingSheetsScrollTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun end() = RuntimeEnvironment.setFontScale(1f)

    private val screen: Rect
        get() = ApplicationProvider.getApplicationContext<android.content.Context>().resources.displayMetrics.let {
            Rect(0f, 0f, it.widthPixels.toFloat(), it.heightPixels.toFloat())
        }

    private fun node(tag: String): SemanticsNode = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()

    /** [tag] whole on the screen: nothing of it clipped by its sheet or its scroll, nor past the screen's edges. */
    private fun assertWhole(tag: String, where: String) {
        val n = node(tag)
        val seen = n.layoutInfo.coordinates.boundsInWindow().intersect(screen)
        val whole = seen.width >= n.size.width - 1 && seen.height >= n.size.height - 1
        assertTrue("$where: « $tag » not whole on screen (seen ${seen.width} × ${seen.height} of ${n.size.width} × ${n.size.height} px)", whole)
    }

    /** The scrolling node holding [tag] (itself or an ancestor), null when nothing around it scrolls. */
    private fun scrollerOf(tag: String): SemanticsNode? {
        var n: SemanticsNode? = node(tag)
        while (n != null) {
            if (n.config.contains(SemanticsActions.ScrollBy)) return n
            n = n.parent
        }
        return null
    }

    private fun tempo(scale: Float = 1f) {
        compose.setContent { PocketStudioTheme { UiScaled(scale) { TempoSheet(124.0, TempoActions(), onClose = {}) } } }
        compose.waitForIdle()
    }

    @Test
    fun theTempoKeypadFitsLyingDown() {
        tempo()
        listOf("tempo_close", "tempo_display", "tempo_key_1", "tempo_key_ok", "tempo_key_0", "tempo_tap").forEach { assertWhole(it, "Tempo, phone lying down, 100 %") }
    }

    @Test
    fun theTempoKeypadFitsLyingDownWithBigText() {
        RuntimeEnvironment.setFontScale(1.3f)
        tempo()
        listOf("tempo_close", "tempo_display", "tempo_key_ok", "tempo_tap").forEach { assertWhole(it, "Tempo, phone lying down, text 130 %") }
    }

    @Test
    fun theTempoSheetScrollsUnderItsHeaderAt130() {
        tempo(scale = 1.3f)
        val body = scrollerOf("tempo_key_ok")
        assertTrue("Tempo at 130 % lying down: the keypad is in nothing that scrolls", body != null)
        assertTrue("Tempo at 130 %: « Fermer » scrolls away with the keypad (the header must stay)", scrollerOf("tempo_close") == null)
        compose.onNodeWithTag("tempo_key_ok").performScrollTo()
        compose.waitForIdle()
        assertWhole("tempo_key_ok", "Tempo at 130 % lying down, scrolled")
        assertWhole("tempo_close", "Tempo at 130 % lying down, scrolled")
        assertWhole("tempo_display", "Tempo at 130 % lying down, scrolled")
        compose.onNodeWithTag("tempo_tap").performScrollTo()
        compose.waitForIdle()
        assertWhole("tempo_tap", "Tempo at 130 % lying down, scrolled")
    }

    @Test
    fun theTakeSheetScrollsLyingDown() {
        compose.setContent { PocketStudioTheme { UiScaled(1.3f) { PlayedTakeSheet(4, "A12", PadLiveActions()) } } }
        compose.waitForIdle()
        assertTrue("The take's sheet at 130 % lying down: « Jeter la prise » is in nothing that scrolls", scrollerOf("padlive_take_drop") != null)
    }

    @Test
    fun theKeySheetScrollsLyingDown() {
        compose.setContent {
            PocketStudioTheme { UiScaled(1.3f) { TutoKeySheet(Harmony(), Harmony(2, Scale.entries.last()), set = {}, close = {}) } }
        }
        compose.waitForIdle()
        assertTrue("The tutorial's key sheet at 130 % lying down: its choices are in nothing that scrolls", scrollerOf("tuto_key_now") != null)
    }
}
