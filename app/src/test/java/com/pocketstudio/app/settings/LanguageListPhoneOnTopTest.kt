package com.pocketstudio.app.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.pocketstudio.app.ui.UiScaled
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.PocketStudioTheme
import com.pocketstudio.ui.layoutDirectionOf
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 6, R18 (seats B c #29, Arabic, and F b screen 23, Polish): once another language was chosen, the list opened on
 * it and the section « Langue du téléphone » was scrolled out of sight (back to the phone's language: scroll up first).
 * At the opening, the phone's language is in sight under its section's name, at the top, and the language in use too,
 * on a small phone (360 dp), at 100 %, at 130 % (the interface's size) and at 200 % (the phone's font).
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-xxhdpi")
class LanguageListPhoneOnTopTest(private val code: String, private val scale: String, private val font: String) {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun back() {
        I18n.lang = Lang.FR
        RuntimeEnvironment.setFontScale(1f)
    }

    @Test
    fun thePhonesLanguageIsOnTopAtTheOpening() {
        val lang = Lang.entries.first { it.code == code }
        I18n.lang = lang
        if (font != "1.0") RuntimeEnvironment.setFontScale(font.toFloat())
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirectionOf(lang.rtl)) {
                UiScaled(scale.toFloat()) {
                    PocketStudioTheme { Box(Modifier.fillMaxSize().background(Neon.Background)) { LanguageList(lang, {}, {}, phone = Lang.FR) } }
                }
            }
        }
        compose.waitForIdle()
        // Images to look at (never committed): under -Proborazzi.test.record=true, build/lot5-captures/.
        if (System.getProperty("roborazzi.test.record") == "true") captureScreenRoboImage("build/lot5-captures/language_list_${code}_${scale}_$font.png")
        val where = "$code, interface $scale, font $font"
        val dialog = compose.onNodeWithTag("lang_dialog").fetchSemanticsNode().boundsInRoot
        val phone = compose.onAllNodesWithTag("lang_suggested").fetchSemanticsNodes().firstOrNull()?.boundsInRoot
        assertTrue("$where: the phone's language is not in sight at the opening", phone != null && inside(phone, dialog))
        val title = tr("Langue du téléphone").uppercase(lang.locale)
        val head = compose.onAllNodes(hasText(title), useUnmergedTree = true).fetchSemanticsNodes().firstOrNull()?.boundsInRoot
        assertTrue("$where: « $title » is not in sight above the phone's language", head != null && inside(head, dialog) && head.bottom <= phone!!.top + 1f)
        // The list still shows the language in use (half of its row at least).
        val list = compose.onNodeWithTag("lang_list").fetchSemanticsNode().boundsInRoot
        val chosen = compose.onAllNodesWithTag("lang_$code").fetchSemanticsNodes().firstOrNull()?.boundsInRoot
        val seen = chosen?.intersect(list)
        assertTrue("$where: the language in use is not in sight", chosen != null && seen != null && !seen.isEmpty && seen.height >= chosen.height / 2)
        // The phone's language above the whole list.
        assertTrue("$where: the phone's language is not above the list", phone!!.bottom <= list.top + 1f)
    }

    private fun inside(r: Rect, outer: Rect) = r.top >= outer.top - 1f && r.bottom <= outer.bottom + 1f && r.height > 0f

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}_{2}")
        fun cases(): List<Array<Any>> = listOf("ar", "pl", "de", "hi").flatMap { l ->
            listOf("1.0" to "1.0", "1.3" to "1.0", "1.0" to "2.0").map { (s, f) -> arrayOf<Any>(l, s, f) }
        }
    }
}
