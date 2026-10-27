package com.pocketstudio.app

import com.pocketstudio.app.guide.ago
import com.pocketstudio.app.guide.formatDb
import com.pocketstudio.app.guide.playsIn
import com.pocketstudio.app.library.packsInstalled
import com.pocketstudio.app.record.NoteTakeUi
import com.pocketstudio.app.record.TakePhase
import com.pocketstudio.app.record.TakeSource
import com.pocketstudio.app.settings.GUIDE_PAGES
import com.pocketstudio.app.settings.GUIDE_PAGES_SIMPLE
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/** Final audit, texts: one of a thing said in the singular, the interface's decimal separator, the Mode simple's own first steps. */
class TextsAuditTest {
    private val phoneLocale = Locale.getDefault()

    @After
    fun back() {
        I18n.lang = Lang.FR
        Locale.setDefault(phoneLocale)
    }

    @Test
    fun oneIsSaidInTheSingular() {
        assertEquals("Joue dans 1 partie sur 4.", playsIn(1, 4))
        assertEquals("Joue dans 3 parties sur 4.", playsIn(3, 4))
        assertEquals("il y a 1 jour", ago(30 * 3_600_000L))
        assertEquals("il y a 3 jours", ago(3 * 24 * 3_600_000L))
        assertEquals("Ta prise : 1 note et 3 coups", NoteTakeUi(TakeSource.KEYS, TakePhase.DONE, notes = 1, hits = 3).summary())
        assertEquals("Ta prise : 4 notes et 1 coup", NoteTakeUi(TakeSource.KEYS, TakePhase.DONE, notes = 4, hits = 1).summary())
        assertEquals("1 pack installé", packsInstalled(1))
        I18n.lang = Lang.EN
        assertEquals("1 day ago", ago(30 * 3_600_000L))
        assertEquals("Plays in 1 part out of 4.", playsIn(1, 4))
        assertEquals("Your take: 1 note and 3 hits", NoteTakeUi(TakeSource.KEYS, TakePhase.DONE, notes = 1, hits = 3).summary())
        I18n.lang = Lang.NL
        assertEquals("1 pack geïnstalleerd", packsInstalled(1))
    }

    /** « −6,0 dB » in French even on a phone set to English, as the mixer says it. */
    @Test
    fun decibelsUseTheInterfacesSeparator() {
        Locale.setDefault(Locale.US)
        assertEquals("−6,0 dB", formatDb(-6f))
        assertEquals("+1,5 dB", formatDb(1.5f))
        I18n.lang = Lang.EN
        assertEquals("−6.0 dB", formatDb(-6f))
    }

    /** The Mode simple has neither Structure, Sons, Mix nor Plus: its first steps name only its own tabs. */
    @Test
    fun theModeSimplesFirstStepsNameItsOwnTabs() {
        val simple = GUIDE_PAGES_SIMPLE.flatMap { listOf(it.title) + it.lines }
        val full = listOf("Structure", "Sons ›", "Plus ›", "Mix :", "⤴")
        full.forEach { word -> assertTrue("$word in the Mode simple's guide", simple.none { word in it }) }
        assertTrue(simple.any { "Mon morceau" in it } && simple.any { "Envoyer" in it })
        assertEquals(GUIDE_PAGES.size, GUIDE_PAGES_SIMPLE.size)
    }
}
