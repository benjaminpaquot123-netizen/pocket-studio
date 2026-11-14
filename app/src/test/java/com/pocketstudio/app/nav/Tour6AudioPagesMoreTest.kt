package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.pocketstudio.model.i18n.Lang
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Audit ultra round 6, R19 (seats B a 21 and G a 12, Turkish): Sons › Voix has four pages, « Pistes audio / Remix /
 * Accordeur / Générer »; on a phone the row is wider than the screen and « Générer » (« Üret ») was cut at the edge or
 * out of sight, with no sign that the row went on. Where pages wait past an edge, the row now fades there and shows
 * « › » (or « ‹ »); the sign goes once the row's end is in view.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-xhdpi")
class Tour6AudioPagesMoreTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val h = AppHarness(compose)
    private val shell = Tour6Shell(compose)

    @After
    fun back() = h.finish()

    private fun openVoicePages(lang: Lang, scale: Float) {
        h.start(simple = false, lang = lang, scale = scale)
        shell.tap("tab_Sons")
        shell.tap("sons_Audio")
        shell.idle(600)
        assertTrue("the pages of Sons › Voix", shell.has("audio_page_0") && shell.has("audio_page_3"))
    }

    /** The last page's tab is cut or out of sight. */
    private fun lastPageCut(): Boolean {
        val last = shell.nodes().first { Tour6Shell.tagOf(it) == "audio_page_3" }
        return last.boundsInRoot.width < Tour6Shell.whole(last).width - 1f
    }

    private fun aCutRowSaysItGoesOn(lang: Lang, scale: Float) {
        openVoicePages(lang, scale)
        assertTrue("the row of pages is wider than the screen here", lastPageCut())
        assertTrue("« Générer » is cut at the edge and nothing says the row goes on", shell.has("audio_pages_more"))
        assertFalse("a sign of pages before the first one", shell.has("audio_pages_before"))
        // The end of the row in view: the sign goes, the other edge now says pages wait before.
        compose.onNodeWithTag("audio_page_1").performTouchInput { swipeLeft() }
        shell.idle(600)
        compose.onNodeWithTag("audio_page_3").performScrollTo()
        shell.idle(300)
        assertFalse("the last page is still cut", lastPageCut())
        assertFalse("« › » still shows with the row's end in view", shell.has("audio_pages_more"))
        assertTrue("nothing says pages wait before", shell.has("audio_pages_before"))
    }

    @Test fun theCutRowOfPagesSaysItGoesOnInTurkishWithBigText() = aCutRowSaysItGoesOn(Lang.TR, 1.3f)

    @Test fun theCutRowOfPagesSaysItGoesOnInFrenchWithBigText() = aCutRowSaysItGoesOn(Lang.FR, 1.3f)

    /** A row that fits says nothing. */
    @Test
    @Config(qualifiers = "sw600dp-w800dp-h1280dp-port-mdpi")
    fun aRowThatFitsShowsNoSign() {
        openVoicePages(Lang.FR, 1f)
        assertFalse("the row fits on a tablet", lastPageCut())
        assertFalse(shell.has("audio_pages_more") || shell.has("audio_pages_before"))
    }
}
