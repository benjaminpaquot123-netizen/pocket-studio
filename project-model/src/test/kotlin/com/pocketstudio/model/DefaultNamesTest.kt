package com.pocketstudio.model

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DefaultNamesTest {
    @After
    fun french() {
        I18n.lang = Lang.FR
    }

    @Test
    fun defaultNamesAreSavedInFrenchWhateverTheLanguage() {
        I18n.lang = Lang.EN
        assertEquals("Nouveau projet", Project().name)
        val p = Project().addTrack()!!
        assertEquals("Piste 2", p.tracks.last().name)
        assertEquals("Synthé", p.track(0).name)
        assertEquals("Piste 4", InstrumentTrack(3, "").sanitized().name)
        val style = StarterWizard.STYLES.first()
        assertEquals(style.name, StarterWizard.build(style, emptyMap()).name)
    }

    @Test
    fun defaultNamesShowInTheInterfaceLanguage() {
        I18n.lang = Lang.EN
        assertEquals("Track 3", DefaultNames.shown("Piste 3"))
        assertEquals("My first beat", DefaultNames.shown("Mon premier rythme"))
        assertEquals("New project", DefaultNames.shown("Nouveau projet"))
        assertEquals("Channel 10", DefaultNames.shown("Canal 10"))
        assertEquals("Song 4 bars", DefaultNames.shown("Morceau 4 mesures"))
        assertEquals("Bass 2", DefaultNames.shown("Basse 2"))
        assertEquals("My project 3", DefaultNames.shown("Mon projet 3"))
        assertEquals("Track 2 (copy)", DefaultNames.shown("Piste 2 (copie)"))
        assertEquals("Funk (copy)", DefaultNames.shown("Funk (copie)"))
        assertEquals("Drums Da Funk", DefaultNames.shownSample("Batterie Da Funk"))
        I18n.lang = Lang.NL
        assertEquals("Spoor 3", DefaultNames.shown("Piste 3"))
        assertEquals("Mijn eerste beat", DefaultNames.shown("Mon premier rythme"))
        I18n.lang = Lang.FR
        assertEquals("Piste 3", DefaultNames.shown("Piste 3"))
    }

    @Test
    fun namesSavedInEnglishOrDutchByOlderVersionsAreRecognised() {
        I18n.lang = Lang.FR
        assertEquals("Piste 2", DefaultNames.shown("Track 2"))
        assertEquals("Nouveau projet", DefaultNames.shown("New project"))
        assertEquals("Basse", DefaultNames.shown("Bas"))
        I18n.lang = Lang.NL
        assertEquals("Nieuw project", DefaultNames.shown("New project"))
        assertEquals("Spoor 7", DefaultNames.shown("Track 7"))
        assertEquals("Basse", DefaultNames.french("Bass"))
    }

    @Test
    fun namesTheUserTypedAreNeverTranslated() {
        I18n.lang = Lang.EN
        // "Son", "Couplet" and "Refrain" are words the app translates, but not names it gives.
        listOf("Son", "Couplet", "Refrain", "Ma basse", "Piste funky", "Funk 2").forEach { name -> assertEquals(name, DefaultNames.shown(name)) }
        assertNull(DefaultNames.french("Ma basse"))
    }

    @Test
    fun takesAreNamedInFrenchAndShownTranslated() {
        I18n.lang = Lang.EN
        assertEquals("Take 21:14", DefaultNames.shownSample("Prise 21:14"))
        I18n.lang = Lang.NL
        assertEquals("Opname 09:05", DefaultNames.shown("Prise 09:05"))
        I18n.lang = Lang.FR
        assertEquals("Prise 21:14", DefaultNames.shown("Prise 21:14"))
        assertEquals("Prise de son", DefaultNames.shown("Prise de son"))
    }

    @Test
    fun aHugeNumberIsJustAName() {
        I18n.lang = Lang.EN
        // More digits than an Int holds: never a crash (the top bar shows it at every launch).
        assertEquals("Track 99999999999", DefaultNames.shown("Piste 99999999999"))  // "Piste" and a number, like "Basse 2"
        assertEquals("Canal 123456789012345", DefaultNames.shown("Canal 123456789012345"))
        assertEquals("Morceau 99999999999 mesures", DefaultNames.shown("Morceau 99999999999 mesures"))
        assertEquals("Track 12", DefaultNames.shown("Piste 12"))
        I18n.lang = Lang.FR
    }
}
