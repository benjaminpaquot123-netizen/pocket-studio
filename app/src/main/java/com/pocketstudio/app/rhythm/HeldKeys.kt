package com.pocketstudio.app.rhythm

/**
 * Keys held on the on-screen keyboard. In chord mode two chords held together often share notes
 * (C and A minor share C and E): each pitch counts the keys holding it, so letting go of one chord
 * does not cut the notes the other still holds, and the take records them as long as they sound.
 */
class HeldKeys {
    private class Held(val track: Int, var count: Int)

    private val pitches = HashMap<Int, Held>()
    private val chords = HashMap<Int, List<Int>>()

    /** What a press does: the track the pitch sounds on, and whether another key already held it. */
    data class Press(val track: Int, val alreadyHeld: Boolean)

    /** One more key holds [pitch]; a pitch already held stays on the track it started on. */
    fun press(pitch: Int, track: Int): Press {
        val held = pitches[pitch]
        if (held != null) {
            held.count++
            return Press(held.track, alreadyHeld = true)
        }
        pitches[pitch] = Held(track, 1)
        return Press(track, alreadyHeld = false)
    }

    /**
     * One key less on [pitch]: the track to release it on when no key holds it any more (or
     * [fallbackTrack] for a pitch that was not held), null while another key still holds it.
     */
    fun release(pitch: Int, fallbackTrack: Int): Int? {
        val held = pitches[pitch] ?: return fallbackTrack
        if (--held.count > 0) return null
        pitches.remove(pitch)
        return held.track
    }

    /** The chord key [root] now holds [notes] (released as they were, even if the scale changes meanwhile). */
    fun pressChord(root: Int, notes: List<Int>) {
        chords[root] = notes
    }

    /** The notes the chord key [root] held, or null when it held none. */
    fun releaseChord(root: Int): List<Int>? = chords.remove(root)

    /** Everything let go at once (leaving the keyboard, all notes off). */
    fun clear() {
        pitches.clear()
        chords.clear()
    }
}
