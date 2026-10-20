package com.pocketstudio.app

import com.pocketstudio.app.rhythm.HeldKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HeldKeysTest {
    /** Plays chord keys like the view model: the pitches that really start and stop sounding. */
    private class Keyboard {
        val keys = HeldKeys()
        val sounding = mutableSetOf<Int>()
        val retriggered = mutableListOf<Int>()

        fun chordOn(root: Int, notes: List<Int>) {
            keys.pressChord(root, notes)
            notes.forEach { p -> if (keys.press(p, track = 0).alreadyHeld) retriggered += p else sounding += p }
        }

        fun chordOff(root: Int) {
            keys.releaseChord(root)!!.forEach { p -> if (keys.release(p, fallbackTrack = 0) != null) sounding -= p }
        }
    }

    @Test
    fun overlappingChordsKeepTheSharedNotesUntilTheLastKeyLetsGo() {
        val k = Keyboard()
        k.chordOn(60, listOf(60, 64, 67))  // C major
        k.chordOn(57, listOf(57, 60, 64))  // A minor: shares C and E
        assertEquals(listOf(60, 64), k.retriggered)
        k.chordOff(60)
        // C and E are still held by the A minor key; only G stops.
        assertEquals(setOf(57, 60, 64), k.sounding)
        k.chordOff(57)
        assertTrue(k.sounding.isEmpty())
    }

    @Test
    fun aChordReleasesTheNotesItPressedEvenIfTheScaleChanged() {
        val keys = HeldKeys()
        keys.pressChord(62, listOf(62, 65, 69))
        assertEquals(listOf(62, 65, 69), keys.releaseChord(62))
        assertNull(keys.releaseChord(62))
    }

    @Test
    fun aPitchIsReleasedOnTheTrackItStartedOn() {
        val keys = HeldKeys()
        assertFalse(keys.press(60, track = 2).alreadyHeld)
        assertEquals(2, keys.press(60, track = 5).track)
        assertNull(keys.release(60, fallbackTrack = 5))
        assertEquals(2, keys.release(60, fallbackTrack = 5))
        // Not held (any more): released on the fallback track, as before.
        assertEquals(5, keys.release(60, fallbackTrack = 5))
    }

    @Test
    fun clearForgetsEveryHold() {
        val keys = HeldKeys()
        keys.press(60, 0)
        keys.press(60, 0)
        keys.clear()
        // A key pressed again after "all notes off" is released by a single key-up.
        keys.press(60, 1)
        assertEquals(1, keys.release(60, 0))
    }
}
