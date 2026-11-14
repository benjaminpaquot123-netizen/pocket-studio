package com.pocketstudio.model

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tour 5, lot « langues » (promise t5-Bc-3, seat B c, a German producer: « Grundton … B♭ … B »): German musicians
 * write the note between A and C « H », and its flat « B ». With the letters (C D E) in German, every note name says so;
 * English keeps B and B♭.
 */
class GermanNoteNamesTest {
    @After
    fun back() {
        I18n.lang = Lang.FR
    }

    @Test
    fun germanSaysHAndB() {
        I18n.lang = Lang.DE
        assertEquals("H", NoteNames.key(11, english = true))
        assertEquals("B", NoteNames.key(10, english = true))
        assertEquals("H", NoteNames.pitchClass(71, english = true))
        assertEquals("H4", NoteNames.withOctave(71, english = true))
        assertEquals("B", NoteNames.withAccidentals(10, english = true, sharps = false))
        assertEquals("A♯", NoteNames.withAccidentals(10, english = true, sharps = true))
        assertEquals("H", NoteNames.spelt(11, 6, english = true))
        assertEquals("C", NoteNames.key(0, english = true))
    }

    @Test
    fun englishKeepsBAndBFlat() {
        I18n.lang = Lang.EN
        assertEquals("B", NoteNames.key(11, english = true))
        assertEquals("B♭", NoteNames.key(10, english = true))
        assertEquals("B4", NoteNames.withOctave(71, english = true))
    }
}
