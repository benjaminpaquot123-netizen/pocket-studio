package com.pocketstudio.model.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/** The languages are data: each entry has its catalog, and the phone's language picks one. */
class LangTest {
    @Test
    fun aPhoneInALanguageOfTheAppGetsIt() {
        assertEquals(Lang.FR, Lang.forLocale(Locale.FRANCE))
        assertEquals(Lang.FR, Lang.forLocale(Locale.forLanguageTag("fr-BE")))
        assertEquals(Lang.FR, Lang.forLocale(Locale.forLanguageTag("fr-CA")))
        assertEquals(Lang.NL, Lang.forLocale(Locale.forLanguageTag("nl-BE")))
        assertEquals(Lang.EN, Lang.forLocale(Locale.UK))
        assertEquals(Lang.EN, Lang.forLocale(Locale.US))
    }

    @Test
    fun anotherLanguageGetsEnglishNotFrench() {
        assertEquals(Lang.EN, Lang.forLocale(Locale.forLanguageTag("sw-KE")))
        assertEquals(Lang.EN, Lang.forLocale(Locale.ROOT))
    }

    @Test
    fun everyLanguageHasItsCatalogAndANativeName() {
        Lang.entries.forEach { lang ->
            assertTrue("${lang.name}: a name", lang.nativeName.isNotBlank())
            assertEquals("${lang.name}: its code is a real language tag", lang.code, java.util.Locale.forLanguageTag(lang.code).toLanguageTag())
            assertEquals("${lang.name}: its locale is its language", Locale.forLanguageTag(lang.code).language, lang.locale.language)
            if (lang != Lang.FR) {
                // The folder the catalogs are read from: every area there.
                I18n.AREAS.forEach { area ->
                    assertTrue("${lang.code}/$area.json missing", javaClass.classLoader.getResource("i18n/${lang.code}/$area.json") != null)
                }
                assertTrue("${lang.code}: translations read", I18n.table(lang).isNotEmpty())
            }
        }
        assertEquals("codes are unique", Lang.entries.size, Lang.entries.map { it.code }.toSet().size)
    }

    @Test
    fun aRegionalLanguageIsPickedBeforeItsPlainOne() {
        // A Brazilian phone: pt-BR when it is listed, then any Portuguese, else English.
        val brazil = Lang.forLocale(Locale.forLanguageTag("pt-BR"))
        val expected = Lang.entries.firstOrNull { it.code == "pt-BR" } ?: Lang.entries.firstOrNull { it.locale.language == "pt" } ?: Lang.EN
        assertEquals(expected, brazil)
        // A right-to-left language is said so (Arabic, Hebrew, Persian, Urdu).
        Lang.entries.forEach { assertEquals(it.name, it.locale.language in setOf("ar", "he", "fa", "ur"), it.rtl) }
    }

    @Test
    fun theTwelveNewLanguagesArePickedByThePhone() {
        val expected = mapOf(
            "es-ES" to Lang.ES, "es-MX" to Lang.ES, "pt-BR" to Lang.PT_BR, "pt-PT" to Lang.PT_BR, "de-DE" to Lang.DE, "de-AT" to Lang.DE,
            "it-IT" to Lang.IT, "pl-PL" to Lang.PL, "tr-TR" to Lang.TR, "id-ID" to Lang.ID, "hi-IN" to Lang.HI, "ar-EG" to Lang.AR,
            "ar-MA" to Lang.AR, "ja-JP" to Lang.JA, "ko-KR" to Lang.KO, "zh-CN" to Lang.ZH_CN, "zh-TW" to Lang.ZH_CN, "zh-HK" to Lang.ZH_CN,
            "zh-Hans-CN" to Lang.ZH_CN, "nl-NL" to Lang.NL, "en-IN" to Lang.EN, "ru-RU" to Lang.EN,
        )
        expected.forEach { (tag, lang) -> assertEquals(tag, lang, Lang.forLocale(Locale.forLanguageTag(tag))) }
        // Android still names Indonesian "in".
        assertEquals(Lang.ID, Lang.forLocale(Locale("in", "ID")))
        assertEquals(Lang.AR, Lang.of("ar"))
        assertTrue(Lang.AR.rtl)
        assertEquals("العربية", Lang.AR.nativeName)
    }

    @Test
    fun thePickerListsEveryLanguageOnce() {
        assertEquals(Lang.entries.toSet(), Lang.PICKER.toSet())
        assertEquals(Lang.entries.size, Lang.PICKER.size)
        assertEquals(15, Lang.entries.size)
        // The first three keep their order (a word given by two languages goes back to the first one's).
        assertEquals(listOf(Lang.FR, Lang.EN, Lang.NL), Lang.entries.take(3))
    }

    @Test
    fun numbersAreWrittenWithWesternDigitsInEveryLanguage() {
        Lang.entries.forEach { lang ->
            assertEquals(lang.code, "120", String.format(lang.locale, "%d", 120))
            assertTrue(lang.code, String.format(lang.locale, "%.1f", 1.5).matches(Regex("""1[.,]5""")))
        }
        val before = I18n.lang
        try {
            I18n.lang = Lang.AR
            assertEquals("120", tr("%d", 120))
        } finally {
            I18n.lang = before
        }
    }
}
