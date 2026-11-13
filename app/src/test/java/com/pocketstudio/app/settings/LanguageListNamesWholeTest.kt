package com.pocketstudio.app.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.text.TextLayoutResult
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
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 6, R1 (its part in the language list, seen while checking R18): at 200 % on a small phone the list wrote
 * « Bahasa Indone… », a language's own name cut by « … ». Each language's own name is whole (on two lines if it
 * must), on a 360 dp phone, at 100 %, at 130 % (the interface's size) and at 200 % (the phone's font).
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-xxhdpi")
class LanguageListNamesWholeTest(private val code: String, private val scale: String, private val font: String) {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun back() {
        I18n.lang = Lang.FR
        RuntimeEnvironment.setFontScale(1f)
    }

    @Test
    fun eachLanguagesOwnNameIsWhole() {
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
        val cut = Lang.PICKER.mapNotNull { l ->
            compose.onNodeWithTag("lang_list").performScrollToNode(hasTestTag("lang_${l.code}"))
            compose.waitForIdle()
            val node = compose.onAllNodes(hasAnyAncestor(hasTestTag("lang_${l.code}")), useUnmergedTree = true).fetchSemanticsNodes()
                .firstOrNull { it.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { t -> t.text } == l.nativeName }
                ?: return@mapNotNull "« ${l.nativeName} » not shown"
            val layout = mutableListOf<TextLayoutResult>().also { node.config[SemanticsActions.GetTextLayoutResult].action!!(it) }.first()
            val last = layout.lineCount - 1
            val whole = (0..last).none { layout.isLineEllipsized(it) } && layout.getLineEnd(last, visibleEnd = true) >= l.nativeName.length
            if (whole) null else "« ${l.nativeName} » cut"
        }
        assertTrue("$code, interface $scale, font $font: " + cut.joinToString(", "), cut.isEmpty())
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}_{2}")
        fun cases(): List<Array<Any>> = listOf("ar", "pl", "de", "hi").flatMap { l ->
            listOf("1.0" to "1.0", "1.3" to "1.0", "1.0" to "2.0").map { (s, f) -> arrayOf<Any>(l, s, f) }
        }
    }
}
