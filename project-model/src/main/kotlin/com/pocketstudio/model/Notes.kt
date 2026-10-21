package com.pocketstudio.model

import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/** A piano-roll note. Positions in ticks: [NoteClip.TICKS_PER_STEP] per sixteenth. */
@Serializable
data class Note(val start: Int, val length: Int, val pitch: Int, val velocity: Float = 0.85f) {
    val end: Int get() = start + length
}

/**
 * The synth's notes: a loop of [steps] sixteenths (1 to 8 bars). [notes] are the ones in the loop
 * (played, shown, exported); [hidden] the ones past its end after it was shortened: kept unheard
 * and back in [notes] when the loop is long enough again, as the drums keep their hits.
 */
@Serializable
data class NoteClip(
    val steps: Int = 16,
    val notes: List<Note> = emptyList(),
    val performance: Performance = Performance(),
    val hidden: List<Note> = emptyList(),
) {
    val ticks: Int get() = steps * TICKS_PER_STEP

    fun sanitized(): NoteClip {
        val s = steps.coerceIn(MIN_STEPS, MAX_STEPS).let { (it / 16).coerceAtLeast(1) * 16 }
        val total = s * TICKS_PER_STEP
        val clean = (notes + hidden).asSequence()
            // Past the longest loop a note goes for good.
            .filter { it.start in 0 until MAX_STEPS * TICKS_PER_STEP && it.velocity.isFinite() }
            .map {
                it.copy(
                    length = it.length.coerceIn(1, MAX_TICKS),
                    pitch = it.pitch.coerceIn(0, 127),
                    velocity = it.velocity.coerceIn(0.05f, 1f),
                )
            }
            .sortedWith(compareBy({ it.start }, { it.pitch }))
            .toList()
        val (inLoop, past) = clean.partition { it.start < total }
        val shown = inLoop.take(MAX_NOTES)
        return copy(steps = s, notes = shown, hidden = past.take(MAX_NOTES - shown.size), performance = performance.sanitized())
    }

    /** No note at all, not even past the loop's end (« Effacer »). */
    fun cleared(): NoteClip = copy(notes = emptyList(), hidden = emptyList())

    fun add(note: Note) = copy(notes = notes + note).sanitized()

    fun remove(note: Note) = copy(notes = notes - note)

    fun replace(old: Note, new: Note) = copy(notes = notes.map { if (it == old) new else it }).sanitized()

    /**
     * [tick] folded into the loop: a note quantized onto the loop's end (the next « 1 ») starts the
     * loop, as it is heard, instead of sitting a tick before it.
     */
    fun wrap(tick: Int): Int = Math.floorMod(tick, ticks.coerceAtLeast(1))

    /** The note under ([tick], [pitch]), the most recent first. */
    fun noteAt(tick: Int, pitch: Int): Note? = notes.lastOrNull { it.pitch == pitch && tick >= it.start && tick < it.end }

    /**
     * Moves note starts toward the [gridTicks] grid by [strength] (0 = untouched, 1 = exactly on
     * the grid). Lengths are kept.
     */
    fun quantized(gridTicks: Int, strength: Float): NoteClip {
        val k = strength.coerceIn(0f, 1f)
        return copy(
            notes = notes.map { n ->
                val target = (n.start.toFloat() / gridTicks).roundToInt() * gridTicks
                n.copy(start = wrap((n.start + (target - n.start) * k).roundToInt()))
            },
        ).sanitized()
    }

    /** Replaces the whole clip with [progression] in [harmony]: one chord per bar, 4 bars. */
    fun withProgression(progression: Progression, harmony: Harmony, octaveRoot: Int = 48): NoteClip =
        withChords(progression.degrees, harmony, octaveRoot)

    /**
     * A bass line: each chord's root (one chord per bar on the scale [degrees]) in the bass, on
     * [pattern] (one character per step: 'X' strong, 'x' medium, 'o' soft, '.' nothing); each note
     * lasts until the next one (two beats at most).
     */
    fun withBassLine(degrees: List<Int>, harmony: Harmony, pattern: String = "X.x.X.x.X.x.X.x.", octaveRoot: Int = 36): NoteClip {
        val hits = pattern.take(16).mapIndexedNotNull { i, c ->
            when (c) {
                'X' -> i to 0.9f
                'x' -> i to 0.75f
                'o' -> i to 0.6f
                else -> null
            }
        }
        val notes = degrees.flatMapIndexed { bar, degree ->
            val pitch = harmony.degreeRoot(degree, octaveRoot)
            hits.mapIndexed { i, (step, velocity) ->
                val next = hits.getOrNull(i + 1)?.first ?: 16
                Note((bar * 16 + step) * TICKS_PER_STEP, (next - step).coerceIn(1, 8) * TICKS_PER_STEP, pitch, velocity)
            }
        }
        // A new clip: notes hidden by an earlier shortening do not come back inside it.
        return copy(steps = 16 * degrees.size.coerceAtLeast(1), notes = notes, hidden = emptyList()).sanitized()
    }

    /** One chord per bar on the scale [degrees] (0 = I), held for the bar. */
    fun withChords(degrees: List<Int>, harmony: Harmony, octaveRoot: Int = 48, velocity: Float = 0.8f): NoteClip {
        val bar = 16 * TICKS_PER_STEP
        val chords = degrees.flatMapIndexed { i, degree ->
            harmony.chord(harmony.degreeRoot(degree, octaveRoot)).map { Note(i * bar, bar, it, velocity) }
        }
        return copy(steps = 16 * degrees.size.coerceAtLeast(1), notes = chords, hidden = emptyList()).sanitized()
    }

    companion object {
        const val TICKS_PER_STEP = 24
        const val MIN_STEPS = 16
        const val MAX_STEPS = 128
        const val MAX_NOTES = 512
        const val MAX_TICKS = 4095  // engine packing limit (12 bits)
        val LENGTHS_IN_STEPS = listOf(16, 32, 64, 128)

        /** The acid bass line as faint "ghost" notes behind the piano roll. */
        fun ghostsOf(acid: AcidPattern, clipSteps: Int): List<Note> {
            if (acid.length <= 0) return emptyList()
            return (0 until clipSteps).mapNotNull { step ->
                val s = acid.steps.getOrNull(step % acid.length) ?: return@mapNotNull null
                if (!s.gate) return@mapNotNull null
                Note(
                    start = step * TICKS_PER_STEP,
                    length = if (s.slide) TICKS_PER_STEP else TICKS_PER_STEP / 2,
                    pitch = acid.root + s.note + 12 * s.octave,
                )
            }
        }
    }
}
