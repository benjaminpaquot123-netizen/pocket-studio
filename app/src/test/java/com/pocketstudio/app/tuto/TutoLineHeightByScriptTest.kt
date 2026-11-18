package com.pocketstudio.app.tuto

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.runtime.CompositionLocalProvider
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 7, P6 (docs/audits/ultra/tour-7-plan-p6-p7.md § 1.3, § 3.3): the line height the panel counts the words in, and
 * the folded panel's line, are measured on a character of the language's own script (« Ag » understated a CJK,
 * Devanagari or Arabic line), at 100 and 200 %, so the words are never counted shorter than they lay out.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class TutoLineHeightByScriptTest {
    @get:Rule
    val compose = createComposeRule()

    @After fun french() { I18n.lang = Lang.FR }

    @Test
    fun theLineHeightsAreAtLeastAWitnessLine() {
        val bad = ArrayList<String>()
        var seen = 0
        val langs = listOf(Lang.JA, Lang.ZH_CN, Lang.KO, Lang.HI, Lang.AR, Lang.FR)
        compose.setContent {
            PocketStudioTheme {
                val base = LocalDensity.current
                for (scale in listOf(1f, 2f)) CompositionLocalProvider(LocalDensity provides Density(base.density, scale)) {
                    for (lang in langs) {
                        I18n.lang = lang
                        val w = TutoReadability.witness(lang)
                        val measurer = rememberTextMeasurer()
                        val style = MaterialTheme.typography.bodyLarge
                        val pitch = measurer.measure("$w\n$w", style).size.height - measurer.measure(w, style).size.height
                        val words = wordsLineHeight()
                        val folded = with(LocalDensity.current) { foldedLineHeight().toPx() }
                        val two = measurer.measure("$w\n$w", MaterialTheme.typography.bodyMedium).size.height
                        seen++
                        if (words < pitch) bad += "${lang.code} ${scale * 100}%: words counted in $words px, a « $w » line is $pitch px"
                        if (words < with(LocalDensity.current) { style.lineHeight.toPx() }) bad += "${lang.code} ${scale * 100}%: under the type's line height"
                        if (folded < two) bad += "${lang.code} ${scale * 100}%: the folded line $folded px holds no two « $w » lines ($two px)"
                    }
                }
            }
        }
        compose.waitForIdle()
        I18n.lang = Lang.FR
        assertTrue("measured only $seen cases: the test checks nothing", seen >= 12)
        assertTrue(bad.joinToString("\n"), bad.isEmpty())
    }
}
