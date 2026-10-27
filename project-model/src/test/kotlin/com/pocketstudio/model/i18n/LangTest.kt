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
            assertEquals("${lang.name}: its code is a real language tag", lang.code, lang.locale.toLanguageTag())
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
}
