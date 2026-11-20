package com.pocketstudio.app.tuto

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.Palettes
import com.pocketstudio.ui.PocketStudioTheme
import com.pocketstudio.ui.layoutDirectionOf
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 9, lot L5b: the tuto's panel on a 360 dp phone, at 100 and 130 % font, on a step (or a chapter title)
 * whose words the lot changed, in the language of the finding. The step is found by the new words, so the test
 * also proves the panel shows them. Images: app/build/l5b-captures/<case>_<font>.png.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-xxhdpi")
class Tour9L5bTutoCapturesTest(private val name: String, private val lang: String, private val needle: String, private val font: String) {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun panelShowsTheNewWords() {
        val l = Lang.entries.first { it.code == lang }
        SettingsStore.update(ApplicationProvider.getApplicationContext()) { it.copy(language = l, englishNotes = true) }
        I18n.lang = l
        val table = I18n.table(l)
        fun shown(fr: String?) = fr?.let { table[it]?.takeIf { t -> t.isNotEmpty() } ?: it }.orEmpty()
        var found: TutoUi? = null
        var style = ""
        search@ for (st in TutoCourse.STYLES) {
            val course = TutoCourse.of(st) ?: continue
            for (level in TutoLevel.entries) {
                val chapters = course.chaptersFor(level)
                chapters.forEachIndexed { c, ch ->
                    ch.steps.forEachIndexed { s, step ->
                        val title = s == 0 && needle in shown(ch.titleFor(level))
                        if (found == null && (title || needle in shown(step.textFor(level)))) {
                            found = TutoUi(TutoProgress(st, level, emptySet(), step.id), chapters, c, s)
                            style = st
                        }
                    }
                }
                if (found != null) break@search
            }
        }
        assertTrue("$name: no step of the tuto shows « $needle » in $lang", found != null)
        val ui = found!!
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirectionOf(l.rtl)) {
                val local = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(local.density, local.fontScale * font.toFloat())) {
                    PocketStudioTheme(Palettes.Studio) {
                        Box(Modifier.fillMaxSize().background(Neon.Background), contentAlignment = Alignment.BottomCenter) {
                            TutoPanel(ui, canShow = true, actions = TutoActions(), map = tutoMap(style))
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/l5b-captures/${name}_$font.png")
    }

    @After
    fun french() {
        SettingsStore.update(ApplicationProvider.getApplicationContext()) { it.copy(language = Lang.FR, englishNotes = false) }
        I18n.lang = Lang.FR
    }

    companion object {
        private val CASES = listOf(
            listOf("f068_en", "en", "then “Roll”"),
            listOf("f075_ar", "ar", "المكس»"),
            listOf("f076_de", "de", "der Drop, dein Refrain"),
            listOf("f077_it", "it", "Tonalità del brano"),
            listOf("f085_de", "de", "der letzten Zählzeit"),
            listOf("f088_ar", "ar", "تصفيق"),
            listOf("f090_ja", "ja", "取り消し線"),
            listOf("f090_nl", "nl", "doorgestreept"),
            listOf("f094_zh", "zh-CN", "那几下错位的击打"),
            listOf("f096_nl", "nl", "overstuurt"),
            listOf("f068_tr", "tr", "altındaki “×4”"),
        )

        @JvmStatic @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{3}")
        fun cases(): List<Array<Any>> = CASES.flatMap { c -> listOf("1.0", "1.3").map { arrayOf<Any>(c[0], c[1], c[2], it) } }
    }
}
