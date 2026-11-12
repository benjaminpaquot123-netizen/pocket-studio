package com.pocketstudio.app.mixer

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import com.pocketstudio.app.audit.CutWords
import com.pocketstudio.app.nav.acidTechnoSong
import com.pocketstudio.app.ui.UiScaled
import com.pocketstudio.app.ui.simpleTracks
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.DefaultNames
import com.pocketstudio.model.guide.MixTips
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
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
 * Tour 6, lot 5, R1 (« Batteri / e ▸ » C a, « Batteri / a » F a, « Snar / e » B b): the Mode simple's Mix on a
 * 360 dp phone, the phone's font at 130 % and 200 % or Réglages › Taille 130 %, in each of the 15 languages, drums
 * folded then open on their sounds: every text is whole (no word over two lines, no « … », no line lost). The
 * project's name after « Mix · » is left out: it may be long, its « … » is meant; so is the « ? » of the help dots
 * (ui-components' Help.kt, another lot: a glyph, not a word).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-xxhdpi")
class SimpleMixWholeWordsTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun tearDown() {
        I18n.lang = Lang.FR
        RuntimeEnvironment.setFontScale(1f)
    }

    private val song = acidTechnoSong()

    private fun cuts(label: String, uiScale: Float, fontScale: Float): List<String> {
        if (fontScale != 1f) RuntimeEnvironment.setFontScale(fontScale)
        var lang by mutableStateOf(Lang.FR)
        var words by mutableStateOf(OutputWords.LOUD)
        val meters = FloatArray(NativeEngine.METERS)
        val loudness = FloatArray(NativeEngine.LOUDNESS) { NativeEngine.LOUDNESS_NONE }
        compose.setContent {
            I18n.lang = lang
            key(lang) {
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirectionOf(lang.rtl)) {
                    UiScaled(uiScale) {
                        PocketStudioTheme {
                            val tracks = simpleTracks(song)
                            val ui = SimpleMixUi(DefaultNames.shown(song.name), tracks, emptyMap(), 0f, false, null, MixTips.of(song).firstOrNull())
                            SimpleMixScreen(ui, { meters }, { loudness }, SimpleMixActions(), words = { words })
                        }
                    }
                }
            }
        }
        val problems = ArrayList<String>()
        fun read(state: String) {
            CutWords.under(compose, "simple_mix").filterNot { it.text.startsWith(" · ") || it.tags.any { t -> t.startsWith("help_") } }
                .forEach { problems += "$label ${I18n.lang.code} $state: $it" }
        }
        for (l in Lang.entries) {
            lang = l
            I18n.lang = l
            compose.waitForIdle()
            // Each of the output's words, the longest first in some language.
            for (w in OutputWords.entries) {
                words = w
                compose.waitForIdle()
                CutWords.under(compose, "mix_out").forEach { problems += "$label ${l.code} $w: $it" }
            }
            words = OutputWords.LOUD
            read("folded")
            compose.onNodeWithTag("mix_drums_expand").performSemanticsAction(SemanticsActions.OnClick)
            compose.waitForIdle()
            read("open")
        }
        return problems.distinct()
    }

    @Test
    fun everyTextIsWholeWithThePhonesFontAt130() {
        val problems = cuts("font 130 %", uiScale = 1f, fontScale = 1.3f)
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun everyTextIsWholeWithThePhonesFontAt200() {
        val problems = cuts("font 200 %", uiScale = 1f, fontScale = 2f)
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun everyTextIsWholeAtTaille130() {
        val problems = cuts("Taille 130 %", uiScale = 1.3f, fontScale = 1f)
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }
}
