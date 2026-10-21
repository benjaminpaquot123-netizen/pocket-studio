package com.pocketstudio.model

/**
 * Note names as the screens show them, following Réglages › Notation: French (Do Ré Mi, the French
 * octave numbers: middle C = Do3) or English (C D E, middle C = C4). Every screen that names a
 * note goes through here, so the setting is never half applied.
 */
object NoteNames {
    private val FR = listOf("Do", "Do#", "Ré", "Ré#", "Mi", "Fa", "Fa#", "Sol", "Sol#", "La", "La#", "Si")
    private val EN = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    private val FR_KEY = listOf("Do", "Do♯", "Ré", "Mi♭", "Mi", "Fa", "Fa♯", "Sol", "La♭", "La", "Si♭", "Si")
    private val EN_KEY = listOf("C", "C♯", "D", "E♭", "E", "F", "F♯", "G", "A♭", "A", "B♭", "B")

    /** The pitch class of [note] (a MIDI note or 0..11) with sharps: "Do#" or "C#" ([sharp] replaces the "#"). */
    fun pitchClass(note: Int, english: Boolean, sharp: String = "#"): String {
        val name = (if (english) EN else FR)[Math.floorMod(note, 12)]
        return if (sharp == "#") name else name.replace("#", sharp)
    }

    /** A key or a chord root, spelt the usual way (E♭, A♭, B♭): "Mi♭" or "E♭". */
    fun key(note: Int, english: Boolean): String = (if (english) EN_KEY else FR_KEY)[Math.floorMod(note, 12)]

    /** The octave number of MIDI [note]: middle C (60) is in octave 4 in English, 3 in French. */
    fun octave(note: Int, english: Boolean): Int = Math.floorDiv(note, 12) - if (english) 1 else 2

    /** "Do3" or "C4" for MIDI 60: never a space between the name and the octave, on every screen. */
    fun withOctave(note: Int, english: Boolean, sharp: String = "#"): String =
        pitchClass(note, english, sharp) + octave(note, english)

    /** The number shown by the keyboard's octave selector whose lowest key is MIDI 12 × ([keyboardOctave] + 1): 4 or 3 for middle C. */
    fun keyboardOctave(keyboardOctave: Int, english: Boolean): Int = octave(12 * (keyboardOctave + 1), english)
}
