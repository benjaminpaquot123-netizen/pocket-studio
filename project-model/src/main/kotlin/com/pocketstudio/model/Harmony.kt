package com.pocketstudio.model

import kotlinx.serialization.Serializable
import kotlin.random.Random

/** Scales the piano roll and keyboard can lock to. Labels are what the user sees. */
enum class Scale(val label: String, val intervals: List<Int>) {
    Off("Libre", (0..11).toList()),
    Major("Majeure", listOf(0, 2, 4, 5, 7, 9, 11)),
    Minor("Mineure", listOf(0, 2, 3, 5, 7, 8, 10)),
    Dorian("Dorienne", listOf(0, 2, 3, 5, 7, 9, 10)),
    MinorPentatonic("Penta mineure", listOf(0, 3, 5, 7, 10)),
    MajorPentatonic("Penta majeure", listOf(0, 2, 4, 7, 9)),
    Blues("Blues", listOf(0, 3, 5, 6, 7, 10)),
    /** Minor with a flat second: dark and tense (techno, drill, metal). */
    Phrygian("Phrygienne", listOf(0, 1, 3, 5, 7, 8, 10)),
    /** Minor with a raised seventh: oriental, dramatic (drill, trance). */
    HarmonicMinor("Mineure harmonique", listOf(0, 2, 3, 5, 7, 8, 11)),
    ;

    /**
     * The seven-note scale this one's chords come from: itself, the natural minor for the minor
     * pentatonic and the blues (both minor), the major for the major pentatonic and for « Libre »
     * (no scale chosen: the major scale of the root, as on the chord pads).
     */
    val parent: Scale get() = when (this) {
        Off, MajorPentatonic -> Major
        MinorPentatonic, Blues -> Minor
        else -> this
    }
}

/**
 * The project's key: [root] 0..11 (0 = Do) and [scale]. [lock]: the keyboard, the chord pads and
 * MIDI only play notes of the scale (no wrong note possible).
 */
@Serializable
data class Harmony(val root: Int = 0, val scale: Scale = Scale.Off, val lock: Boolean = false) {
    fun sanitized() = copy(root = Math.floorMod(root, 12))

    fun inScale(pitch: Int): Boolean = Math.floorMod(pitch - root, 12) in scale.intervals

    /** The nearest pitch of the scale (ties go down). */
    fun snap(pitch: Int): Int {
        if (scale == Scale.Off || inScale(pitch)) return pitch
        for (d in 1..6) {
            if (inScale(pitch - d)) return pitch - d
            if (inScale(pitch + d)) return pitch + d
        }
        return pitch
    }

    /** This key with the seven-note scale its chords come from ([Scale.parent]). */
    val chordKey: Harmony get() = if (scale.parent == scale) this else copy(scale = scale.parent)

    /**
     * The chord built on [pitch] by stacking every other note of the scale ([size] notes:
     * 3 = triad, 4 = seventh), the scale's own chords: a pentatonic or the blues take those of
     * their [Scale.parent], « Libre » those of the root's major scale. A note outside that scale
     * with no scale chosen (« Libre ») gets a major triad, or a dominant seventh: a borrowed chord.
     */
    fun chord(pitch: Int, size: Int = 3): List<Int> {
        val key = chordKey
        if (scale == Scale.Off && !key.inScale(pitch)) {
            val shape = if (size >= 4) listOf(0, 4, 7, 10) else listOf(0, 4, 7)
            return shape.map { pitch + it }
        }
        val ints = key.scale.intervals
        val base = key.snap(pitch)
        val degree = ints.indexOf(Math.floorMod(base - root, 12))
        return (0 until size).map { i ->
            val d = degree + 2 * i
            val octave = d / ints.size
            base - ints[degree] + ints[d % ints.size] + 12 * octave
        }
    }

    /** Chord roots of a progression given as scale degrees (0 = I), from [octaveRoot], in the [Scale.parent] scale. */
    fun degreeRoot(degree: Int, octaveRoot: Int): Int {
        val ints = scale.parent.intervals
        return octaveRoot + root + ints[Math.floorMod(degree, 7)] + 12 * Math.floorDiv(degree, 7)
    }

    /**
     * The letter (0 = Do … 6 = Si) each degree of the [Scale.parent] scale is written on: one letter
     * per degree, from the spelling of the root that needs the fewest sharps and flats (Mi majeur:
     * Fa♯ Sol♯ Do♯ Ré♯, never La♭; Fa majeur: Si♭), the usual one on a tie (Mi♭ mineur).
     */
    fun letters(): List<Int> {
        val ints = scale.parent.intervals
        val pc = Math.floorMod(root, 12)
        val candidates = (0..6).filter { l -> NoteNames.accidental(pc, l) != null }
        val best = candidates.minBy { l ->
            val cost = ints.indices.sumOf { i ->
                val a = NoteNames.accidental(Math.floorMod(pc + ints[i], 12), (l + i) % 7)
                a?.let { Math.abs(it) } ?: 10
            }
            2 * cost + if (l == NoteNames.usualLetter(pc)) 0 else 1
        }
        return ints.indices.map { (best + it) % 7 }
    }

    /**
     * [pitch]'s name in this key, as a score writes it: a note of the key on its degree's letter
     * (Sol♯ in Mi majeur, Si♭ in Fa majeur), another one with the key's accidentals (♭ when the key
     * has flats or none, ♯ when it has sharps).
     */
    fun noteName(pitch: Int, english: Boolean): String {
        val key = chordKey
        val pc = Math.floorMod(pitch, 12)
        val letters = letters()
        val degree = key.scale.intervals.indexOf(Math.floorMod(pc - root, 12))
        if (degree >= 0) NoteNames.spelt(pc, letters[degree], english)?.let { return it }
        val sharps = key.scale.intervals.indices.any { i ->
            (NoteNames.accidental(Math.floorMod(root + key.scale.intervals[i], 12), letters[i]) ?: 0) > 0
        }
        return NoteNames.withAccidentals(pc, english, sharps)
    }

    /** The root's name, spelt as the key is ([noteName]): "Mi♭", "Fa♯", "Ré♭" (Ré♭ majeur). */
    fun keyName(english: Boolean): String = noteName(root, english)
}

/**
 * One pad of the chord pads: a degree of the scale and its full chord. [root] is the chord's pitch
 * class, [letter] the letter it is written on in the key (0 = Do … 6 = Si; -1: the usual spelling)
 * and [quality] its kind ("m", "dim", "+" augmented or "" for major): the name is made at display, in the
 * notation the user picked.
 */
data class ChordPad(val degree: Int, val roman: String, val root: Int, val quality: String, val notes: List<Int>, val letter: Int = -1) {
    /** "Ré m" in French notation, "Dm" in English (the way chord charts write them); "Sol♯ m" in Mi majeur, "Do+" augmented. */
    fun name(english: Boolean): String =
        ((if (letter >= 0) NoteNames.spelt(root, letter, english) else null) ?: NoteNames.key(root, english)) +
            if (quality.isEmpty()) "" else if (english || quality == "+") quality else " $quality"
}

/**
 * The scale's chords for the chord pads: degrees I to VII and I an octave up, built from
 * [octaveRoot] (C of an octave), the chords of its [Scale.parent] scale: the minor pentatonic and
 * the blues take the natural minor's, the major pentatonic and « Libre » the major's, so every pad
 * is a chord that fits.
 */
fun Harmony.chordPads(octaveRoot: Int): List<ChordPad> {
    val h = chordKey
    val letters = h.letters()
    val numerals = listOf("I", "II", "III", "IV", "V", "VI", "VII")
    return (0..7).map { d ->
        val notes = h.chord(h.degreeRoot(d, octaveRoot))
        val third = notes[1] - notes[0]
        val fifth = notes[2] - notes[0]
        val numeral = numerals[d % 7]
        val (roman, suffix) = when {
            third == 3 && fifth == 6 -> numeral.lowercase() + "°" to "dim"
            third == 4 && fifth == 8 -> "$numeral+" to "+"
            third == 3 -> numeral.lowercase() to "m"
            else -> numeral to ""
        }
        ChordPad(d, roman, Math.floorMod(notes[0], 12), suffix, notes, letters[d % 7])
    }
}

/**
 * The degree a ready-made progression plays for [degree] in this key: the scale's own chord, or,
 * when that one is diminished or augmented (the vii° of a major key, the ii° of a minor one, the
 * III+ of the harmonic minor), the major or minor chord closest to it in the same role: a third
 * below for the seventh degree (vii° → V), a third above for the others (ii° → iv, vi° → i).
 */
fun Harmony.progressionDegree(degree: Int): Int {
    val key = chordKey
    fun plain(d: Int): Boolean = key.chord(key.degreeRoot(d, 48)).let { it[2] - it[0] == 7 }
    if (plain(degree)) return degree
    val other = degree + if (Math.floorMod(degree, 7) == 6) -2 else 2
    return if (plain(other)) other else degree
}

/** The roman numerals [harmony]'s scale gives this progression (minor in lower case): "I–V–vi–IV", or "i–v–VI–iv" in a minor key. */
fun Progression.labelIn(harmony: Harmony): String {
    val pads = harmony.chordPads(48)
    return degrees.joinToString("–") { pads[Math.floorMod(harmony.progressionDegree(it), 7)].roman }
}

/** Ready-made chord progressions, as scale degrees (one chord per bar). */
enum class Progression(val label: String, val degrees: List<Int>) {
    Pop("I–V–vi–IV", listOf(0, 4, 5, 3)),
    Sad("vi–IV–I–V", listOf(5, 3, 0, 4)),
    Jazz("ii–V–I–I", listOf(1, 4, 0, 0)),
    Epic("i–VI–III–VII", listOf(0, 5, 2, 6)),
    House("i–iv–v–i", listOf(0, 3, 4, 0)),
}

enum class ArpMode(val label: String) { Off("Non"), Up("↑"), Down("↓"), UpDown("↑↓"), Random("Hasard") }

/** How chords are played: broken into an arpeggio and/or strummed. */
@Serializable
data class Performance(
    val arp: ArpMode = ArpMode.Off,
    /** Arpeggio step in ticks (12 = 1/32, 24 = 1/16, 48 = 1/8). */
    val arpRate: Int = 24,
    /** Delay between the notes of a strummed chord, in ticks (0 = together). */
    val strum: Int = 0,
) {
    fun sanitized() = copy(arpRate = arpRate.coerceIn(6, 96), strum = strum.coerceIn(0, 24))

    /**
     * The notes the engine actually plays: notes starting together form a chord; with an
     * arpeggio they are cycled at [arpRate] for the chord's length, with strum they start one
     * after the other. Never more than [NoteClip.MAX_NOTES].
     */
    fun expand(clip: NoteClip): List<Note> {
        if (arp == ArpMode.Off && strum == 0) return clip.notes
        val out = ArrayList<Note>()
        val random = Random(clip.notes.size * 31 + clip.steps)
        for ((start, chord) in clip.notes.groupBy { it.start }.toSortedMap()) {
            val sorted = chord.sortedBy { it.pitch }
            if (sorted.size == 1 || arp == ArpMode.Off) {
                sorted.forEachIndexed { i, n ->
                    val delay = i * strum
                    if (delay < n.length) out += n.copy(start = n.start + delay, length = n.length - delay)
                }
                continue
            }
            val length = sorted.maxOf { it.length }
            val order = when (arp) {
                ArpMode.Up -> sorted
                ArpMode.Down -> sorted.reversed()
                ArpMode.UpDown -> sorted + sorted.drop(1).dropLast(1).reversed()
                else -> sorted
            }
            var t = 0
            var i = 0
            while (t < length) {
                val n = if (arp == ArpMode.Random) sorted[random.nextInt(sorted.size)] else order[i % order.size]
                out += Note(start + t, minOf(arpRate, length - t), n.pitch, n.velocity)
                t += arpRate
                i++
            }
        }
        return out.filter { it.start < clip.ticks }.sortedBy { it.start }.take(NoteClip.MAX_NOTES)
    }
}

/** A General MIDI preset from the instrument library, found by bank and program. */
@Serializable
data class InstrumentChoice(val bank: Int, val program: Int, val name: String, val bankId: String = "generaluser")
