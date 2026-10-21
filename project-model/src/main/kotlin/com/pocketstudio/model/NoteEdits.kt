package com.pocketstudio.model

import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Piano-roll editing tools on a selection of notes (FL Studio Mobile's "More…" menu). Every
 * function returns the new clip and the new selection, so the edited notes stay selected.
 */
object NoteEdits {
    /** [dropped]: notes of a paste or a copy that did not fit in the loop (none were added for them). */
    data class Result(val clip: NoteClip, val selection: Set<Note>, val dropped: Int = 0)

    private fun replace(clip: NoteClip, selection: Set<Note>, transform: (Note) -> Note): Result {
        val changed = selection.associateWith(transform)
        val notes = clip.notes.map { changed[it] ?: it }
        val next = clip.copy(notes = notes).sanitized()
        // Notes the clean-up dropped (out of range) leave the selection too.
        return Result(next, changed.values.filter { it in next.notes }.toSet())
    }

    /**
     * Up or down by [semitones]; notes past the keyboard stay where they are. The whole clip
     * chosen: the notes hidden past its end (a shortened loop) move too, so they come back in key.
     */
    fun transpose(clip: NoteClip, selection: Set<Note>, semitones: Int): Result {
        val move = { n: Note -> if (n.pitch + semitones in 0..127) n.copy(pitch = n.pitch + semitones) else n }
        val all = clip.hidden.isNotEmpty() && selection.isNotEmpty() && selection.containsAll(clip.notes)
        return replace(if (all) clip.copy(hidden = clip.hidden.map(move)) else clip, selection, move)
    }

    /** Earlier or later by [ticks], inside the loop. */
    fun shift(clip: NoteClip, selection: Set<Note>, ticks: Int): Result =
        replace(clip, selection) { n -> n.copy(start = (n.start + ticks).coerceIn(0, clip.ticks - 1)) }

    fun setVelocity(clip: NoteClip, selection: Set<Note>, velocity: Float): Result =
        replace(clip, selection) { it.copy(velocity = velocity.coerceIn(0.05f, 1f)) }

    fun setLength(clip: NoteClip, selection: Set<Note>, ticks: Int): Result =
        replace(clip, selection) { it.copy(length = ticks.coerceIn(1, NoteClip.MAX_TICKS)) }

    /** Starts onto the [grid] (ticks) by [strength] 0..1. */
    fun quantize(clip: NoteClip, selection: Set<Note>, grid: Int, strength: Float = 1f): Result =
        replace(clip, selection) { n ->
            val target = (n.start.toFloat() / grid).roundToInt() * grid
            n.copy(start = clip.wrap((n.start + (target - n.start) * strength.coerceIn(0f, 1f)).roundToInt()))
        }

    /** Each note lasts until the next note starts (chords: notes starting together end together). */
    fun legato(clip: NoteClip, selection: Set<Note>): Result {
        val starts = selection.map { it.start }.distinct().sorted()
        return replace(clip, selection) { n ->
            val next = starts.firstOrNull { it > n.start } ?: clip.ticks
            n.copy(length = (next - n.start).coerceAtLeast(1))
        }
    }

    /** Small random nudges of timing (±[ticks]) and strength (±[velocity]): a human feel. */
    fun humanize(clip: NoteClip, selection: Set<Note>, random: Random = Random.Default, ticks: Int = 3, velocity: Float = 0.08f): Result =
        replace(clip, selection) { n ->
            n.copy(
                start = (n.start + random.nextInt(-ticks, ticks + 1)).coerceIn(0, clip.ticks - 1),
                velocity = (n.velocity + (random.nextFloat() * 2 - 1) * velocity).coerceIn(0.05f, 1f),
            )
        }

    /** A copy of the selection right after it (aligned on the bar when it spans whole bars). */
    fun duplicate(clip: NoteClip, selection: Set<Note>): Result {
        if (selection.isEmpty()) return Result(clip, selection)
        val first = selection.minOf { it.start }
        val end = selection.maxOf { it.end }
        val span = roundUp(end - first, NoteClip.TICKS_PER_STEP)
        val copies = selection.map { it.copy(start = it.start + span) }.filter { it.start < clip.ticks }
        val next = clip.copy(notes = clip.notes + copies).sanitized()
        val kept = copies.filter { it in next.notes }.toSet()
        return Result(next, kept, selection.size - kept.size)
    }

    /** The selection as a clipboard: notes relative to the first start. */
    fun copy(selection: Set<Note>): List<Note> {
        val first = selection.minOfOrNull { it.start } ?: return emptyList()
        return selection.map { it.copy(start = it.start - first) }.sortedWith(compareBy({ it.start }, { it.pitch }))
    }

    /**
     * Pastes [clipboard] at [at] ticks; the pasted notes become the selection. What starts past the
     * loop's end is not pasted: [Result.dropped] counts it (the screen asks for a longer loop).
     */
    fun paste(clip: NoteClip, clipboard: List<Note>, at: Int): Result {
        val pasted = clipboard.map { it.copy(start = at.coerceAtLeast(0) + it.start) }.filter { it.start < clip.ticks }
        if (pasted.isEmpty()) return Result(clip, emptySet(), clipboard.size)
        val next = clip.copy(notes = clip.notes + pasted).sanitized()
        val kept = pasted.filter { it in next.notes }.toSet()
        return Result(next, kept, clipboard.size - kept.size)
    }

    fun delete(clip: NoteClip, selection: Set<Note>): Result = Result(clip.copy(notes = clip.notes - selection), emptySet())

    /** Notes whose start is in [fromTick, toTick) and pitch in [low, high]: a rectangle drawn on the grid. */
    fun inBox(clip: NoteClip, fromTick: Int, toTick: Int, low: Int, high: Int): Set<Note> =
        clip.notes.filter { it.end > fromTick && it.start < toTick && it.pitch in low..high }.toSet()

    /**
     * The scales that fit the notes best (most notes inside, the root used most breaking ties), best
     * first. Empty when there are no notes.
     */
    fun guessScales(notes: List<Note>, count: Int = 3): List<Harmony> {
        if (notes.isEmpty()) return emptyList()
        val weight = IntArray(12)
        notes.forEach { weight[Math.floorMod(it.pitch, 12)] += it.length.coerceAtLeast(1) }
        val candidates = (0 until 12).flatMap { root -> listOf(Scale.Major, Scale.Minor).map { Harmony(root, it) } }
        return candidates.sortedWith(
            compareByDescending<Harmony> { h -> (0 until 12).filter { h.inScale(it) }.sumOf { weight[it] } }
                .thenByDescending { h -> weight[h.root] }
                .thenBy { h -> if (h.scale == Scale.Major) 0 else 1 },
        ).take(count)
    }

    private fun roundUp(v: Int, step: Int) = (v + step - 1) / step * step
}
