package com.pocketstudio.model

import kotlin.math.roundToInt

/** A note heard in a recording (seconds from its start), as audio-to-notes gives it. */
data class HeardNote(val start: Double, val end: Double, val pitch: Int, val amplitude: Float)

object Transcription {
    /**
     * The heard notes as a piano-roll clip at [bpm]: starts on the sixteenth grid when [snap],
     * lengths rounded to the grid (never under half a sixteenth), the clip as long as needed
     * (1 to 8 bars; later notes are left out). Silence before the first note is dropped.
     */
    fun toClip(heard: List<HeardNote>, bpm: Double, snap: Boolean = true, base: NoteClip = NoteClip()): NoteClip {
        if (heard.isEmpty()) return base.copy(notes = emptyList(), hidden = emptyList())
        val ticksPerSecond = bpm / 60.0 * 4 * NoteClip.TICKS_PER_STEP
        val step = NoteClip.TICKS_PER_STEP
        // The first note lands on the first beat of the clip.
        val offset = heard.minOf { it.start }
        val notes = heard.map { h ->
            val rawStart = (h.start - offset) * ticksPerSecond
            val rawLength = (h.end - h.start) * ticksPerSecond
            val start = if (snap) (rawStart / step).roundToInt() * step else rawStart.roundToInt()
            val length = if (snap) maxOf(step / 2, (rawLength / step).roundToInt() * step) else maxOf(1, rawLength.roundToInt())
            Note(start, length, h.pitch.coerceIn(0, 127), (0.4f + 0.6f * h.amplitude).coerceIn(0.05f, 1f))
        }.distinctBy { it.start to it.pitch }
        val lastEnd = notes.maxOf { it.end }
        val steps = NoteClip.LENGTHS_IN_STEPS.firstOrNull { it * step >= lastEnd } ?: NoteClip.MAX_STEPS
        return base.copy(steps = steps, hidden = emptyList(), notes = notes.filter { it.start < steps * step }.map { it.copy(length = minOf(it.length, steps * step - it.start)) }).sanitized()
    }
}
