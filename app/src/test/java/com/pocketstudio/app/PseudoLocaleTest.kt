package com.pocketstudio.app

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.i18n.trCount
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Audit §3: the pseudo-language keeps every text formattable, and is longer and accented. */
class PseudoLocaleTest {
    private val spec = Regex("%(\\d+\\$)?[-#+ 0,(]*\\d*(\\.\\d+)?[a-zA-Z%]")

    @After
    fun french() = PseudoLocale.off()

    @Test
    fun everyFrenchTextKeepsItsFormatsAndGrowsByFortyPercent() {
        val keys = Lang.entries.filter { it != Lang.FR }.flatMap { I18n.table(it).keys }.map { it.substringBefore('|') }.toSet()
        assertTrue("the catalogs are read (${keys.size} texts)", keys.size > 1000)
        for (fr in keys) {
            val p = PseudoLocale.of(fr)
            assertEquals(fr, spec.findAll(fr).map { it.value }.toList(), spec.findAll(p).map { it.value }.toList())
            assertTrue("$fr → $p", p.length >= fr.length * 1.4)
            assertTrue(p, p.startsWith("[") && p.endsWith("]"))
            if (spec.replace(fr, "").any { it in 'a'..'z' }) assertTrue("accented: $p", spec.replace(p, "").none { it in 'a'..'z' })
            val r = PseudoLocale.of(fr, rtl = true)
            assertEquals(PseudoLocale.RLE, r.first())
            assertEquals(PseudoLocale.PDF, r.last())
        }
    }

    @Test
    fun theAppTextsGoThroughItWhenItIsOnAndOnlyThen() {
        assertEquals("Lecture", tr("Lecture"))
        PseudoLocale.on()
        assertEquals(PseudoLocale.of("Lecture"), tr("Lecture"))
        // Formatted texts and plurals still format, in the pseudo-language.
        assertTrue(tr("%d pistes, toutes au centre : rien n'est placé dans la stéréo.", 7).contains("7 þîšţéš"))
        assertTrue(trCount(3, "%d note", "%d notes").startsWith("[3 ñöţéš"))
        PseudoLocale.on(rtl = true)
        assertTrue(I18n.lang.rtl)
        assertEquals(PseudoLocale.RLE, tr("Lecture").first())
        PseudoLocale.off()
        assertEquals("Lecture", tr("Lecture"))
    }
}

/**
 * The newcomer's screens in the pseudo-language (app/screenshots/i18n/pseudo_*.png): a text cut
 * shows its missing bracket, a text too long for its button overflows 40 % before any real language.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
open class I18nPseudoScreenshotTest : I18nGermanScreenshotTest() {
    override val lang = Lang.FR
    protected open val pseudoRtl = false
    override val prefix get() = if (pseudoRtl) "i18n/pseudo-rtl_" else "i18n/pseudo_"
    override val rtl get() = pseudoRtl

    @org.junit.Before
    fun pseudo() = PseudoLocale.on(pseudoRtl)

    @After
    fun noPseudo() = PseudoLocale.off()
}

/** The same, right to left. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class I18nPseudoRtlScreenshotTest : I18nPseudoScreenshotTest() {
    override val pseudoRtl = true
}
