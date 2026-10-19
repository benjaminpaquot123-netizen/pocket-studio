package com.pocketstudio.model

import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/** A piano-roll note. Positions in ticks: [NoteClip.TICKS_PER_STEP] per sixteenth. */
@Serializable
data class Note(val start: Int, val length: Int, val pitch: Int, val velocity: Float = 0.85f) {
    val end: Int get() = start + length
}

/** The synth's notes: a loop of [steps] sixteenths (1 to 8 bars). */
@Serializable
data class NoteClip(val steps: Int = 16, val notes: List<Note> = emptyList()) {
    val ticks: Int get() = steps * TICKS_PER_STEP

    fun sanitized(): NoteClip {
        val s = steps.coerceIn(MIN_STEPS, MAX_STEPS).let { (it / 16).coerceAtLeast(1) * 16 }
        val total = s * TICKS_PER_STEP
        val clean = notes.asSequence()
            .filter { it.start in 0 until total && it.velocity.isFinite() }
            .map {
                it.copy(
                    length = it.length.coerceIn(1, MAX_TICKS),
                    pitch = it.pitch.coerceIn(0, 127),
                    velocity = it.velocity.coerceIn(0.05f, 1f),
                )
            }
            .sortedWith(compareBy({ it.start }, { it.pitch }))
            .take(MAX_NOTES)
            .toList()
        return copy(steps = s, notes = clean)
    }

    fun add(note: Note) = copy(notes = notes + note).sanitized()

    fun remove(note: Note) = copy(notes = notes - note)

    fun replace(old: Note, new: Note) = copy(notes = notes.map { if (it == old) new else it }).sanitized()

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
                n.copy(start = (n.start + (target - n.start) * k).roundToInt().coerceIn(0, ticks - 1))
            },
        ).sanitized()
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
