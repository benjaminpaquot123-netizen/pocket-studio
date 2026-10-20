package com.pocketstudio.model

/** How a chord is stacked on its root, with notes of the scale. */
enum class Voicing(val scaleSteps: List<Int>, val octaveUp: Boolean = false) {
    Triad(listOf(0, 2, 4)),
    Seventh(listOf(0, 2, 4, 6)),
    Ninth(listOf(0, 2, 4, 6, 8)),
    Sus2(listOf(0, 1, 4)),
    Sus4(listOf(0, 3, 4)),
    /** Root, fifth and the root an octave up: the hard, open sound of rave and rock. */
    Power(listOf(0, 4), octaveUp = true),
    Octave(listOf(0), octaveUp = true),
    Single(listOf(0)),
}

/** The notes of the chord on scale [degree] (0 = I) from [octaveRoot], stacked as [voicing]. */
fun Harmony.voiced(degree: Int, octaveRoot: Int, voicing: Voicing): List<Int> {
    val notes = voicing.scaleSteps.map { degreeRoot(degree + it, octaveRoot) }
    return if (voicing.octaveUp) notes + (notes.first() + 12) else notes
}

/**
 * Octaves to move the chord on [degree] by so that its root stays close to the key's root (no more
 * than a fifth above, a fourth below): the bass and the chords move by small steps, as played.
 */
fun Harmony.nearShift(degree: Int, octaveRoot: Int): Int =
    if (degreeRoot(degree, octaveRoot) - degreeRoot(0, octaveRoot) > 7) -12 else 0

/**
 * Music written as one character per sixteenth step (16 per bar; a 32-character line covers two
 * bars and repeats). A note or chord starts on any character other than '-' and '.', lasts while
 * '-' follows, and '.' is silence.
 */
object StepLine {
    data class Event(val step: Int, val length: Int, val token: Char)

    fun parse(bar: String): List<Event> {
        val events = ArrayList<Event>()
        var i = 0
        while (i < bar.length) {
            val c = bar[i]
            if (c == '-' || c == '.') { i++; continue }
            var len = 1
            while (i + len < bar.length && bar[i + len] == '-') len++
            events += Event(i, len, c)
            i += len
        }
        return events
    }

    /** The part of [pattern] heard in [bar] (a two-bar line alternates). */
    fun barOf(pattern: String, bar: Int): String {
        val bars = (pattern.length / 16).coerceAtLeast(1)
        val b = Math.floorMod(bar, bars)
        return pattern.padEnd(16 * bars, '.').substring(16 * b, 16 * b + 16)
    }

    /** Scale steps above the chord's root for a melodic token: R root, 2…7, O the root an octave up. */
    fun stepOf(token: Char): Int? = when (token.uppercaseChar()) {
        'R' -> 0
        '2' -> 1
        '3' -> 2
        '4' -> 3
        '5' -> 4
        '6' -> 5
        '7' -> 6
        'O' -> 7
        else -> null
    }
}

/**
 * Chords on a rhythm: one chord per bar on the scale [degrees], hit where [rhythm] says ('X'
 * strong, 'x' softer), stacked as [voicing]. A pad held all bar is "X---------------"; offbeat
 * stabs are "..X...X...X...X.".
 */
fun NoteClip.withChordRhythm(degrees: List<Int>, harmony: Harmony, rhythm: String, voicing: Voicing, octaveRoot: Int, velocity: Float = 0.8f): NoteClip {
    val notes = degrees.flatMapIndexed { bar, degree ->
        val shift = harmony.nearShift(degree, octaveRoot)
        val chord = harmony.voiced(degree, octaveRoot, voicing).map { it + shift }
        StepLine.parse(StepLine.barOf(rhythm, bar)).flatMap { e ->
            val v = if (e.token.isLowerCase()) velocity * 0.75f else velocity
            chord.map { Note((bar * 16 + e.step) * NoteClip.TICKS_PER_STEP, e.length * NoteClip.TICKS_PER_STEP, it, v) }
        }
    }
    return copy(steps = 16 * degrees.size.coerceAtLeast(1), notes = notes).sanitized()
}

/**
 * A line on the chords: each token of [pattern] is a note of the current bar's chord ('R' root,
 * '3' third, '5' fifth, '7' seventh, '2' '4' '6' the scale's other notes, 'O' the root an octave
 * up; lower case is softer), from [octaveRoot]. Bass lines, riffs, hooks.
 */
fun NoteClip.withLine(degrees: List<Int>, harmony: Harmony, pattern: String, octaveRoot: Int, velocity: Float = 0.9f): NoteClip {
    val notes = degrees.flatMapIndexed { bar, degree ->
        val shift = harmony.nearShift(degree, octaveRoot)
        StepLine.parse(StepLine.barOf(pattern, bar)).mapNotNull { e ->
            val step = StepLine.stepOf(e.token) ?: return@mapNotNull null
            val v = if (e.token.isLowerCase()) velocity * 0.7f else velocity
            Note((bar * 16 + e.step) * NoteClip.TICKS_PER_STEP, e.length * NoteClip.TICKS_PER_STEP, harmony.degreeRoot(degree + step, octaveRoot) + shift, v)
        }
    }
    return copy(steps = 16 * degrees.size.coerceAtLeast(1), notes = notes).sanitized()
}

/**
 * An acid line for the 303 track: [pattern] tokens as in [withLine] on the key's root chord (a 303
 * riff stays on it while the chords move; every recipe's progression starts there), [accents] and
 * [slides] one 'x' per step that has one.
 */
fun acidLine(harmony: Harmony, pattern: String, accents: String, slides: String, base: AcidPattern = AcidPattern()): AcidPattern {
    val bar = pattern.padEnd(16, '.').take(16)
    val tonic = harmony.degreeRoot(0, 0)
    val steps = (0 until 16).map { i ->
        val token = bar[i]
        val step = StepLine.stepOf(token)
        if (step == null) {
            // '-' keeps the previous note sounding: a tie on the 303.
            AcidStep(gate = false)
        } else {
            val offset = harmony.degreeRoot(step, 0) - tonic
            AcidStep(
                note = Math.floorMod(offset, 12), octave = Math.floorDiv(offset, 12),
                accent = accents.getOrNull(i) == 'x', slide = slides.getOrNull(i) == 'x' || bar.getOrNull(i + 1) == '-', gate = true,
            )
        }
    }
    return base.copy(length = 16, root = (36 + Math.floorMod(harmony.root, 12)).coerceIn(24, 60), steps = steps + List(AcidPattern.MAX_STEPS - 16) { AcidStep() }).sanitized()
}
