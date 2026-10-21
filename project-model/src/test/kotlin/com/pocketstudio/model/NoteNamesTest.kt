package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteNamesTest {
    @Test
    fun frenchNamesUseTheFrenchOctaveNumbers() {
        assertEquals("Do3", NoteNames.withOctave(60, english = false))
        assertEquals("La3", NoteNames.withOctave(69, english = false))
        assertEquals("Si2", NoteNames.withOctave(59, english = false))
        assertEquals("Do#-2", NoteNames.withOctave(1, english = false))
    }

    @Test
    fun englishNamesPutMiddleCInOctaveFour() {
        assertEquals("C4", NoteNames.withOctave(60, english = true))
        assertEquals("A4", NoteNames.withOctave(69, english = true))
        assertEquals("B3", NoteNames.withOctave(59, english = true))
        assertEquals("F♯2", NoteNames.withOctave(42, english = true, sharp = "♯"))
    }

    @Test
    fun theKeyboardOctaveFollowsTheNotation() {
        // The keyboard starting on middle C (selector value 4, MIDI 60) is in octave 3 in Do Ré Mi.
        assertEquals(4, NoteNames.keyboardOctave(4, english = true))
        assertEquals(3, NoteNames.keyboardOctave(4, english = false))
        assertEquals(NoteNames.octave(48, english = false), NoteNames.keyboardOctave(3, english = false))
    }

    @Test
    fun keysUseFlatsWhereMusiciansDo() {
        assertEquals(listOf("C", "C♯", "D", "E♭", "E", "F", "F♯", "G", "A♭", "A", "B♭", "B"), (0..11).map { NoteNames.key(it, true) })
        assertEquals("Si♭", NoteNames.key(-2, false))
        assertEquals("Ré#", NoteNames.pitchClass(63 - 12 * 4 - 12, false))
    }
}
