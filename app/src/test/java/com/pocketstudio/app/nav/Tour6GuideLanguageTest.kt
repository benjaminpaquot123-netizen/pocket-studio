package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.model.i18n.Lang
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Audit ultra round 6, G2 (seats B c #28 and F b #22): the language changed in Réglages, the Guide's bar still said its
 * step in the old one (« Guide · ضع إيقاعًا 9 % » in a French app, « Guide · Zrób drugą część 30 % ») until the song
 * changed. The Guide's texts were worked out once, with the song; the language now brings them round again.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-xhdpi")
class Tour6GuideLanguageTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val h = AppHarness(compose)
    private val shell = Tour6Shell(compose)

    @After
    fun back() = h.finish()

    private fun stepSaysItsLanguage(simple: Boolean, from: Lang, to: Lang) {
        h.start(simple = simple, lang = from)
        h.awaitGuide()
        val before = h.vm.guide.ui.value.next!!
        val barBefore = shell.textOf("guide_bar_step")
        assertTrue("the bar says the step « ${before.title} »: $barBefore", barBefore.contains(before.title))
        // Réglages › Langue, as the picker does it.
        SettingsStore.update(h.app) { it.copy(language = to) }
        val rewritten = shell.reached(8_000) { h.vm.guide.ui.value.next?.title != before.title }
        val after = h.vm.guide.ui.value.next!!
        assertEquals("the same step", before.id, after.id)
        assertTrue("the Guide's step is still « ${after.title} », in ${from.code}, after the language went to ${to.code}", rewritten)
        assertNotEquals(before.why, after.why)
        val bar = shell.textOf("guide_bar_step")
        assertTrue("the bar says the step in ${to.code} (« ${after.title} »): $bar", bar.contains(after.title))
    }

    @Test fun theStepIsRewrittenFromFrenchToPolishInPro() = stepSaysItsLanguage(simple = false, from = Lang.FR, to = Lang.PL)

    @Test fun theStepIsRewrittenFromArabicToFrenchInSimple() = stepSaysItsLanguage(simple = true, from = Lang.AR, to = Lang.FR)
}
