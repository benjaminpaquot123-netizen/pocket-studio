package com.pocketstudio.model

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
     * Up or down by [semitones], the selection as one block: when a note would leave the keyboard
     * (0..127) nothing moves (a chord never folds onto itself). The whole clip chosen: the notes
     * hidden past its end (a shortened loop) move too, so they come back in key.
     */
    fun transpose(clip: NoteClip, selection: Set<Note>, semitones: Int): Result {
        if (semitones == 0 || selection.isEmpty() || semitones !in transposeRange(selection)) return Result(clip, selection)
        val move = { n: Note -> if (n.pitch + semitones in 0..127) n.copy(pitch = n.pitch + semitones) else n }
        val all = clip.hidden.isNotEmpty() && selection.containsAll(clip.notes)
        return replace(if (all) clip.copy(hidden = clip.hidden.map(move)) else clip, selection, move)
    }

    /** The semitones [selection] can move by as one block and stay on the keyboard (0..127). */
    fun transposeRange(selection: Set<Note>): IntRange =
        if (selection.isEmpty()) 0..0 else -selection.minOf { it.pitch }..(127 - selection.maxOf { it.pitch })

    /**
     * Earlier or later by [ticks], inside the loop, the selection as one block: against the loop's
     * start or end it stops there ([shiftRange]), its notes keeping their gaps (never piled on one tick).
     */
    fun shift(clip: NoteClip, selection: Set<Note>, ticks: Int): Result {
        val by = ticks.coerceIn(shiftRange(clip, selection))
        if (by == 0 || selection.isEmpty()) return Result(clip, selection)
        return replace(clip, selection) { n -> n.copy(start = n.start + by, played = null) }
    }

    /** The ticks [selection] can move by as one block and stay in [clip]'s loop. */
    fun shiftRange(clip: NoteClip, selection: Set<Note>): IntRange =
        if (selection.isEmpty()) 0..0 else -selection.minOf { it.start }..(clip.ticks - 1 - selection.maxOf { it.start }).coerceAtLeast(-selection.minOf { it.start })

    fun setVelocity(clip: NoteClip, selection: Set<Note>, velocity: Float): Result =
        replace(clip, selection) { it.copy(velocity = velocity.coerceIn(0.05f, 1f)) }

    fun setLength(clip: NoteClip, selection: Set<Note>, ticks: Int): Result =
        replace(clip, selection) { it.copy(length = ticks.coerceIn(1, NoteClip.MAX_TICKS)) }

    /**
     * « Remettre en rythme »: starts onto the [grid] (ticks) by [strength] 0..1, from where they
     * were played ([Note.played] keeps it: [asPlayed] undoes it, another strength does not pile up).
     */
    fun quantize(clip: NoteClip, selection: Set<Note>, grid: Int, strength: Float = 1f): Result =
        replace(clip, selection) { n -> n.inTime(grid.coerceAtLeast(1), strength.coerceIn(0f, 1f), clip.ticks) }

    /** « Comme joué »: the selection back where it was played. */
    fun asPlayed(clip: NoteClip, selection: Set<Note>): Result =
        replace(clip, selection) { n -> if (n.played != null) n.copy(start = n.played, played = null) else n }

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
                played = null,
                velocity = (n.velocity + (random.nextFloat() * 2 - 1) * velocity).coerceIn(0.05f, 1f),
            )
        }

    /** A copy of the selection right after it (aligned on the bar when it spans whole bars). */
    fun duplicate(clip: NoteClip, selection: Set<Note>): Result {
        if (selection.isEmpty()) return Result(clip, selection)
        val first = selection.minOf { it.start }
        val end = selection.maxOf { it.end }
        val span = roundUp(end - first, NoteClip.TICKS_PER_STEP)
        // Never more than the clip can take: past it the clean-up would drop other notes, not these.
        val copies = selection.map { it.copy(start = it.start + span, played = null) }.filter { it.start < clip.ticks }
            .sortedWith(compareBy({ it.start }, { it.pitch })).take(clip.room)
        val next = clip.copy(notes = clip.notes + copies).sanitized()
        val kept = copies.filter { it in next.notes }.toSet()
        return Result(next, kept, selection.size - kept.size)
    }

    /** The selection as a clipboard: notes relative to the first start. */
    fun copy(selection: Set<Note>): List<Note> {
        val first = selection.minOfOrNull { it.start } ?: return emptyList()
        return selection.map { it.copy(start = it.start - first, played = null) }.sortedWith(compareBy({ it.start }, { it.pitch }))
    }

    /**
     * Pastes [clipboard] at [at] ticks; the pasted notes become the selection. What starts past the
     * loop's end is not pasted: [Result.dropped] counts it (the screen asks for a longer loop).
     */
    fun paste(clip: NoteClip, clipboard: List<Note>, at: Int): Result {
        // Never more than the clip can take: past it the clean-up would drop other notes, not these.
        val pasted = clipboard.map { it.copy(start = at.coerceAtLeast(0) + it.start) }.filter { it.start < clip.ticks }
            .sortedWith(compareBy({ it.start }, { it.pitch })).take(clip.room)
        if (pasted.isEmpty()) return Result(clip, emptySet(), clipboard.size)
        val next = clip.copy(notes = clip.notes + pasted).sanitized()
        val kept = pasted.filter { it in next.notes }.toSet()
        return Result(next, kept, clipboard.size - kept.size)
    }

    fun delete(clip: NoteClip, selection: Set<Note>): Result = Result(clip.copy(notes = clip.notes - selection), emptySet())

    /**
     * Phase 37: [selection] as a [NoteClipboard] shared by every track and part: each note's place
     * from the start of the first bar it touches ([barTicks] long), so a paste on a bar keeps where
     * the notes fall in it.
     */
    fun toClipboard(selection: Set<Note>, barTicks: Int, from: String = ""): NoteClipboard? {
        if (selection.isEmpty() || barTicks <= 0) return null
        val firstBar = selection.minOf { it.start } / barTicks
        val origin = firstBar * barTicks
        val end = selection.maxOf { it.end }
        val notes = selection.map { it.copy(start = it.start - origin, played = null) }.sortedWith(compareBy({ it.start }, { it.pitch }))
        return NoteClipboard(notes, ((end - origin + barTicks - 1) / barTicks).coerceAtLeast(1), firstBar, from)
    }

    /** The notes starting in bar [bar] ([barTicks] long) of [clip]. */
    fun inBar(clip: NoteClip, bar: Int, barTicks: Int): Set<Note> =
        clip.notes.filter { it.start / barTicks.coerceAtLeast(1) == bar }.toSet()

    /**
     * Phase 37: [board] pasted on bar [bar] of [clip] ([barTicks] long), the notes where they were
     * in their bars; the loop grows (whole bars, up to its longest) when they reach past its end.
     * The pasted notes become the selection; [Result.dropped] counts those that still did not fit.
     */
    fun pasteOnBar(clip: NoteClip, board: NoteClipboard, bar: Int, barTicks: Int): Result {
        val at = bar.coerceAtLeast(0) * barTicks
        val needed = at + board.bars * barTicks
        val grown = if (needed > clip.ticks) {
            // Whole bars of the time signature ([barTicks]): 12 steps a bar in 3/4, never rounded to 4/4's 16.
            val bar = (barTicks / NoteClip.TICKS_PER_STEP).coerceAtLeast(1)
            val steps = ((needed / NoteClip.TICKS_PER_STEP + bar - 1) / bar * bar).coerceAtMost(NoteClip.MAX_STEPS / bar * bar)
            clip.withSteps(maxOf(clip.steps, steps))
        } else clip
        return paste(grown, board.notes, at)
    }

    /** Notes whose start is in [fromTick, toTick) and pitch in [low, high]: a rectangle drawn on the grid. */
    fun inBox(clip: NoteClip, fromTick: Int, toTick: Int, low: Int, high: Int): Set<Note> =
        clip.notes.filter { it.end > fromTick && it.start < toTick && it.pitch in low..high }.toSet()

    /**
     * True when some notes start together (a chord): only then do the arpeggio and the strum
     * change anything ([Performance.expand] plays single notes as they are).
     */
    fun hasChords(notes: List<Note>): Boolean = notes.groupingBy { it.start }.eachCount().values.any { it > 1 }

    /** How many notes are out of [harmony]'s scale (0 without a scale). */
    fun outOfScale(notes: List<Note>, harmony: Harmony): Int =
        if (harmony.scale == Scale.Off) 0 else notes.count { !harmony.inScale(it.pitch) }

    /**
     * « Mettre les notes dans la gamme »: every note out of the scale moves to the nearest note of
     * it ([Harmony.snap]), the hidden ones past the loop too; a note that lands on another one
     * (same start, same pitch) merges into it, the longer and louder kept.
     */
    fun fitToScale(clip: NoteClip, harmony: Harmony): NoteClip {
        if (harmony.scale == Scale.Off) return clip
        fun fit(list: List<Note>) = list.map { it.copy(pitch = harmony.snap(it.pitch)) }
            .groupBy { it.start to it.pitch }.values
            .map { same -> same.reduce { a, b -> a.copy(length = maxOf(a.length, b.length), velocity = maxOf(a.velocity, b.velocity)) } }
        return clip.copy(notes = fit(clip.notes), hidden = fit(clip.hidden)).sanitized()
    }

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

/**
 * Phase 37: notes copied in a piano roll, kept by the app (not the screen) so they can be pasted in
 * another track or part: their places from the start of the bar they came from ([fromBar]), the
 * [bars] they span, and where they came from ([from], said on the paste button).
 */
data class NoteClipboard(val notes: List<Note>, val bars: Int = 1, val fromBar: Int = 0, val from: String = "")
