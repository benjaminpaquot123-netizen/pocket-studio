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

    /**
     * The chord built on [pitch] by stacking every other note of the scale ([size] notes:
     * 3 = triad, 4 = seventh). Without a scale: a major triad, or a dominant seventh.
     */
    fun chord(pitch: Int, size: Int = 3): List<Int> {
        if (scale == Scale.Off || scale.intervals.size < 7) {
            val shape = if (size >= 4) listOf(0, 4, 7, 10) else listOf(0, 4, 7)
            return shape.map { pitch + it }
        }
        val base = snap(pitch)
        val degree = scale.intervals.indexOf(Math.floorMod(base - root, 12))
        return (0 until size).map { i ->
            val d = degree + 2 * i
            val octave = d / scale.intervals.size
            base - scale.intervals[degree] + scale.intervals[d % scale.intervals.size] + 12 * octave
        }
    }

    /** Chord roots of a progression given as scale degrees (0 = I), from [octaveRoot]. */
    fun degreeRoot(degree: Int, octaveRoot: Int): Int {
        val ints = if (scale.intervals.size == 7) scale.intervals else Scale.Major.intervals
        return octaveRoot + root + ints[Math.floorMod(degree, 7)] + 12 * Math.floorDiv(degree, 7)
    }
}

/** One pad of the chord pads: a degree of the scale and its full chord. */
data class ChordPad(val degree: Int, val roman: String, val name: String, val notes: List<Int>)

/**
 * The scale's chords for the chord pads: degrees I to VII and I an octave up, built from
 * [octaveRoot] (C of an octave). Scales of fewer than seven notes (and "Libre") use the major
 * scale of the same root, so every pad is a chord that fits.
 */
fun Harmony.chordPads(octaveRoot: Int): List<ChordPad> {
    val h = if (scale.intervals.size == 7) this else copy(scale = Scale.Major)
    val numerals = listOf("I", "II", "III", "IV", "V", "VI", "VII")
    return (0..7).map { d ->
        val notes = h.chord(h.degreeRoot(d, octaveRoot))
        val third = notes[1] - notes[0]
        val fifth = notes[2] - notes[0]
        val numeral = numerals[d % 7]
        val (roman, suffix) = when {
            third == 3 && fifth == 6 -> numeral.lowercase() + "°" to " dim"
            third == 3 -> numeral.lowercase() to " m"
            else -> numeral to ""
        }
        ChordPad(d, roman, NOTE_NAMES[Math.floorMod(notes[0], 12)] + suffix, notes)
    }
}

val NOTE_NAMES = listOf("Do", "Do♯", "Ré", "Mi♭", "Mi", "Fa", "Fa♯", "Sol", "La♭", "La", "Si♭", "Si")

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
