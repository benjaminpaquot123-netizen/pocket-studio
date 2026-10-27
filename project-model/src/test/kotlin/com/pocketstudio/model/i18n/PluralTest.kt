package com.pocketstudio.model.i18n

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

/** Counts in words in every language (CLDR plural rules), and capitals the language's way. */
class PluralTest {
    @After
    fun french() {
        I18n.lang = Lang.FR
        I18n.everyday = false
    }

    private fun categories(code: String, vararg n: Long) = n.map { pluralOf(code, it) }

    @Test
    fun theCldrRules() {
        assertEquals(listOf(Plural.ONE, Plural.OTHER, Plural.OTHER), categories("en", 1, 0, 2))
        assertEquals(listOf(Plural.ONE, Plural.ONE, Plural.OTHER), categories("fr", 0, 1, 2))
        assertEquals(listOf(Plural.OTHER, Plural.OTHER), categories("ja", 1, 5))
        assertEquals(listOf(Plural.OTHER), categories("zh-CN", 1))
        assertEquals(listOf(Plural.ONE, Plural.OTHER), categories("pt-BR", 1, 2))
        assertEquals(
            listOf(Plural.ONE, Plural.FEW, Plural.FEW, Plural.MANY, Plural.MANY, Plural.MANY, Plural.FEW, Plural.MANY, Plural.MANY),
            categories("pl", 1, 2, 4, 5, 12, 21, 22, 25, 0),
        )
        assertEquals(listOf(Plural.ONE, Plural.FEW, Plural.MANY, Plural.MANY, Plural.ONE, Plural.FEW), categories("ru", 1, 2, 5, 11, 21, 22))
        assertEquals(
            listOf(Plural.ZERO, Plural.ONE, Plural.TWO, Plural.FEW, Plural.FEW, Plural.MANY, Plural.MANY, Plural.OTHER, Plural.OTHER, Plural.FEW),
            categories("ar", 0, 1, 2, 3, 10, 11, 99, 100, 102, 103),
        )
    }

    @Test
    fun frenchEnglishAndDutchShowWhatTheirTwoKeysSay() {
        val cases = listOf(0, 1, 2, 5, 21, 22)
        I18n.lang = Lang.FR
        assertEquals(listOf("0 mesures", "1 mesure", "2 mesures", "5 mesures", "21 mesures", "22 mesures"), cases.map { trCount(it, "1 mesure", "%d mesures") })
        for (lang in listOf(Lang.EN, Lang.NL)) {
            I18n.lang = lang
            assertEquals(lang.code, cases.map { if (it == 1) tr("1 mesure") else tr("%d mesures", it) }, cases.map { trCount(it, "1 mesure", "%d mesures") })
        }
        I18n.lang = Lang.EN
        assertEquals("1 bar", trCount(1, "1 mesure", "%d mesures"))
        assertEquals("4 bars", trCount(4, "1 mesure", "%d mesures"))
    }

    @Test
    fun polishAndArabicSayEachForm() {
        I18n.lang = Lang.PL
        assertEquals("1 takt", trCount(1, "1 mesure", "%d mesures"))
        assertEquals("2 takty", trCount(2, "1 mesure", "%d mesures"))
        assertEquals("5 taktów", trCount(5, "1 mesure", "%d mesures"))
        assertEquals("22 takty", trCount(22, "1 mesure", "%d mesures"))
        assertEquals("12 taktów", trCount(12, "1 mesure", "%d mesures"))
        I18n.lang = Lang.AR
        assertEquals("مازورتان", trCount(2, "1 mesure", "%d mesures"))
        assertEquals("3 مازورات", trCount(3, "1 mesure", "%d mesures"))
        assertEquals("11 مازورة", trCount(11, "1 mesure", "%d mesures"))
        // A form a language does not give: its count's key.
        assertEquals(tr("%d mesures", 100), trCount(100, "1 mesure", "%d mesures"))
    }

    @Test
    fun aCountWithOtherArguments() {
        I18n.lang = Lang.FR
        assertEquals("Joue dans 1 partie sur 4.", trCount(1, "Joue dans 1 partie sur %d.", "Joue dans %1\$d parties sur %2\$d.", 4))
        assertEquals("Joue dans 3 parties sur 4.", trCount(3, "Joue dans 1 partie sur %d.", "Joue dans %1\$d parties sur %2\$d.", 3, 4))
    }

    @Test
    fun turkishCapitalsKeepTheDot() {
        I18n.lang = Lang.TR
        assertEquals("BİR STİL SEÇ", "bir stil seç".upperShown())
        assertEquals("ılık", "ILIK".inSentence())
        I18n.lang = Lang.EN
        assertEquals("BIG", "big".upperShown())
        assertEquals("reverb", "Reverb".inSentence())
        // German nouns keep their capital inside a sentence.
        I18n.lang = Lang.DE
        assertEquals("Hall", "Hall".inSentence())
    }

    @Test
    fun percentTheLanguagesWay() {
        val shown = listOf(Lang.FR, Lang.EN, Lang.NL, Lang.DE, Lang.TR, Lang.AR).associateWith { I18n.lang = it; percent(50) }
        assertEquals(mapOf(Lang.FR to "50 %", Lang.EN to "50%", Lang.NL to "50%", Lang.DE to "50 %", Lang.TR to "%50", Lang.AR to "50%"), shown)
    }

    @Test
    fun aSignedNumberKeepsItsOrderRightToLeft() {
        I18n.lang = Lang.FR
        assertEquals("+0,0 dB", ltrNumber("+0,0 dB"))
        I18n.lang = Lang.AR
        assertEquals("\u2066−8.2 dB\u2069", ltrNumber("−8.2 dB"))
    }
}
