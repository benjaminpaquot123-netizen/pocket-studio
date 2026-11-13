package com.pocketstudio.app.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import com.pocketstudio.app.ui.UiScaled
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.PocketStudioTheme
import com.pocketstudio.ui.layoutDirectionOf
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Fresh review of lot 5 (R18): the phone's language put out of the scroll left the list of all the languages only the
 * room under it; on a phone lying down (360 dp high) that list was 58 dp high at 100 %, 32 dp with the phone's font at
 * 130 %, 0 dp at 200 % and at Taille 130 %: no other language to pick. Lying down, at each size: the phone's language
 * is in sight at the opening (R18) and every language of the list can be scrolled into sight.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w800dp-h360dp-xxhdpi")
class LanguageListLyingTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun back() {
        I18n.lang = Lang.FR
        RuntimeEnvironment.setFontScale(1f)
    }

    private fun Rect.dp() = "%.0f..%.0f dp".format(top / compose.density.density, bottom / compose.density.density)

    private fun everyLanguageCanBePicked(scale: Float, font: Float) {
        if (font != 1f) RuntimeEnvironment.setFontScale(font)
        val lang = Lang.DE
        I18n.lang = lang
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirectionOf(lang.rtl)) {
                UiScaled(scale) {
                    PocketStudioTheme { Box(Modifier.fillMaxSize().background(Neon.Background)) { LanguageList(lang, {}, {}, phone = Lang.FR) } }
                }
            }
        }
        compose.waitForIdle()
        val where = "lying down, interface $scale, font $font"
        val dialog = compose.onNodeWithTag("lang_dialog").fetchSemanticsNode().boundsInRoot
        val phone = compose.onAllNodesWithTag("lang_suggested").fetchSemanticsNodes().firstOrNull()?.boundsInRoot
        assertTrue("$where: the phone's language is not in sight at the opening", phone != null && phone.top >= dialog.top - 1f && phone.bottom <= dialog.bottom + 1f)
        val list = compose.onNodeWithTag("lang_list").fetchSemanticsNode().boundsInRoot
        // Two rows of the list at least (a row is some 60 dp): checked before any scroll (a list 0 dp high never shows one).
        assertTrue("$where: the list of the languages is ${list.dp()} high", list.height / compose.density.density >= 96f)
        val missing = Lang.PICKER.filter { l ->
            compose.onNodeWithTag("lang_list").performScrollToNode(hasTestTag("lang_${l.code}"))
            compose.waitForIdle()
            val row = compose.onNodeWithTag("lang_${l.code}").fetchSemanticsNode().boundsInRoot
            val seen = minOf(row.bottom, list.bottom) - maxOf(row.top, list.top)
            seen < row.height / 2
        }
        assertTrue("$where: out of sight after scrolling to them: ${missing.map { it.code }}", missing.isEmpty())
    }

    @Test
    fun everyLanguageCanBePickedAt100() = everyLanguageCanBePicked(1f, 1f)

    @Test
    fun everyLanguageCanBePickedWithThePhonesFontAt130() = everyLanguageCanBePicked(1f, 1.3f)

    @Test
    fun everyLanguageCanBePickedWithThePhonesFontAt200() = everyLanguageCanBePicked(1f, 2f)

    @Test
    fun everyLanguageCanBePickedAtTaille130() = everyLanguageCanBePicked(1.3f, 1f)
}
