package com.pocketstudio.model

/**
 * Note names as the screens show them, following Réglages › Notation: French (Do Ré Mi, the French
 * octave numbers: middle C = Do3) or English (C D E, middle C = C4). Every screen that names a
 * note goes through here, so the setting is never half applied.
 */
object NoteNames {
    /**
     * The solfège as the interface's language writes it: « Ré » in French and Portuguese, « Re » in Italian,
     * Spanish and Turkish (audit ultra round 3, n° 38: « Ré♭ » in the Italian chords).
     */
    private fun solfege(names: List<String>): List<String> = when (com.pocketstudio.model.i18n.I18n.lang) {
        in PLAIN_RE -> names.map { it.replace("Ré", "Re") }
        // Arabic names notes دو ري مي (round 3, n° 38: « A الصغير » for « لا الصغير »).
        com.pocketstudio.model.i18n.Lang.AR -> names.map { n -> ARABIC.entries.fold(n) { acc, (fr, ar) -> acc.replace(fr, ar) } }
        else -> names
    }

    /**
     * Tour 5, lot « langues » (promise t5-Bc-3): German writes the note between A and C « H », its flat « B »
     * (« B♭ » would read « B-flat-flat »); the other letters as in English.
     */
    private fun letters(names: List<String>): List<String> =
        if (com.pocketstudio.model.i18n.I18n.lang == com.pocketstudio.model.i18n.Lang.DE) names.map { GERMAN[it] ?: it } else names

    private val GERMAN = mapOf("B" to "H", "B♭" to "B", "B♯" to "H♯")

    private val ARABIC = linkedMapOf("Sol" to "صول", "Do" to "دو", "Ré" to "ري", "Mi" to "مي", "Fa" to "فا", "La" to "لا", "Si" to "سي")

    private val PLAIN_RE = setOf(com.pocketstudio.model.i18n.Lang.IT, com.pocketstudio.model.i18n.Lang.ES, com.pocketstudio.model.i18n.Lang.TR)

    private val FR = listOf("Do", "Do#", "Ré", "Ré#", "Mi", "Fa", "Fa#", "Sol", "Sol#", "La", "La#", "Si")
    private val EN = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    private val FR_KEY = listOf("Do", "Do♯", "Ré", "Mi♭", "Mi", "Fa", "Fa♯", "Sol", "La♭", "La", "Si♭", "Si")
    private val EN_KEY = listOf("C", "C♯", "D", "E♭", "E", "F", "F♯", "G", "A♭", "A", "B♭", "B")

    /** The pitch class of [note] (a MIDI note or 0..11) with sharps: "Do#" or "C#" ([sharp] replaces the "#"). */
    fun pitchClass(note: Int, english: Boolean, sharp: String = "#"): String {
        val name = (if (english) letters(EN) else solfege(FR))[Math.floorMod(note, 12)]
        return if (sharp == "#") name else name.replace("#", sharp)
    }

    /** A key or a chord root, spelt the usual way (E♭, A♭, B♭): "Mi♭" or "E♭". */
    fun key(note: Int, english: Boolean): String = (if (english) letters(EN_KEY) else solfege(FR_KEY))[Math.floorMod(note, 12)]

    private val FR_LETTERS = listOf("Do", "Ré", "Mi", "Fa", "Sol", "La", "Si")
    private val EN_LETTERS = listOf("C", "D", "E", "F", "G", "A", "B")
    private val NATURALS = listOf(0, 2, 4, 5, 7, 9, 11)

    /** The letter [key] writes pitch class [pc] on (0 = Do … 6 = Si): Mi for Mi♭, Fa for Fa♯. */
    fun usualLetter(pc: Int): Int = listOf(0, 0, 1, 2, 2, 3, 3, 4, 5, 5, 6, 6)[Math.floorMod(pc, 12)]

    /** The accidental pitch class [pc] needs on [letter] (0 = Do … 6 = Si): 0, 1 (♯) or -1 (♭); null when it takes more. */
    fun accidental(pc: Int, letter: Int): Int? = (Math.floorMod(pc - NATURALS[letter] + 6, 12) - 6).takeIf { it in -1..1 }

    /** Pitch class [pc] written on [letter] (0 = Do … 6 = Si): "Sol♯", "Mi♯", "Do♭", "G♯"; null when it would need a double sharp or flat. */
    fun spelt(pc: Int, letter: Int, english: Boolean): String? {
        val a = accidental(pc, letter) ?: return null
        val name = (if (english) EN_LETTERS else solfege(FR_LETTERS))[letter] + when (a) {
            1 -> "♯"
            -1 -> "♭"
            else -> ""
        }
        return if (english) letters(listOf(name)).first() else name
    }

    /** Pitch class [pc] with sharps ([sharps]: "Ré♯") or flats ("Mi♭"), a natural note plain. */
    fun withAccidentals(pc: Int, english: Boolean, sharps: Boolean): String {
        val p = Math.floorMod(pc, 12)
        val letter = NATURALS.indexOf(p).takeIf { it >= 0 } ?: if (sharps) NATURALS.indexOf(p - 1) else NATURALS.indexOf(p + 1)
        return spelt(p, letter, english)!!
    }

    /** The octave number of MIDI [note]: middle C (60) is in octave 4 in English, 3 in French. */
    fun octave(note: Int, english: Boolean): Int = Math.floorDiv(note, 12) - if (english) 1 else 2

    /** "Do3" or "C4" for MIDI 60: never a space between the name and the octave, on every screen. */
    fun withOctave(note: Int, english: Boolean, sharp: String = "#"): String =
        pitchClass(note, english, sharp) + octave(note, english)

    /** The number shown by the keyboard's octave selector whose lowest key is MIDI 12 × ([keyboardOctave] + 1): 4 or 3 for middle C. */
    fun keyboardOctave(keyboardOctave: Int, english: Boolean): Int = octave(12 * (keyboardOctave + 1), english)
}
