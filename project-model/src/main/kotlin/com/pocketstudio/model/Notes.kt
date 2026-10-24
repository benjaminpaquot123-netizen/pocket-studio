package com.pocketstudio.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/**
 * A piano-roll note. Positions in ticks: [NoteClip.TICKS_PER_STEP] per sixteenth. [played]: the
 * start as it was played, once « Remettre en rythme » moved it (null: it starts where it was
 * played, or it was drawn); « Comme joué » puts it back there ([NoteClip.asPlayed]). Left out of
 * the file when null.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Note(
    val start: Int,
    val length: Int,
    val pitch: Int,
    val velocity: Float = 0.85f,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val played: Int? = null,
) {
    val end: Int get() = start + length

    /** Where it was played (its start when it was never put in time). */
    val playedStart: Int get() = played ?: start

    /**
     * How far from its start it was played, in a loop of [loopTicks]: across the loop's end when
     * that is nearer (a note played just before the loop came round, put on its first beat).
     */
    fun playedOffset(loopTicks: Int): Int? = played?.let { p ->
        val t = loopTicks.coerceAtLeast(1)
        val d = p - start
        when {
            d > t / 2 -> d - t
            d < -t / 2 -> d + t
            else -> d
        }
    }

    /**
     * Moved to [newStart] from a loop of [oldTicks] into one of [newTicks]: where it was played
     * follows it (across the new loop's end for a note that was put on the first beat).
     */
    fun movedTo(newStart: Int, oldTicks: Int, newTicks: Int): Note {
        val offset = playedOffset(oldTicks) ?: return copy(start = newStart)
        val at = newStart + offset
        val replayed = if (newStart < newTicks) Math.floorMod(at, newTicks.coerceAtLeast(1)) else at.takeIf { it >= 0 }
        return copy(start = newStart, played = replayed?.takeIf { it != newStart })
    }
}

/**
 * The synth's notes: a loop of [steps] sixteenths (1 to 16 bars). [notes] are the ones in the loop
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
        // Already clean (nearly always: every edit cleans the whole project): the clip itself, not
        // a rebuilt copy, so a gesture on a big project neither sorts nor compares every note again.
        if (s == steps && isClean(total)) {
            val perf = performance.sanitized()
            if (perf == performance) return this
        }
        val clean = (notes + hidden).asSequence()
            // Past the longest loop a note goes for good.
            .filter { it.start in 0 until MAX_STEPS * TICKS_PER_STEP && it.velocity.isFinite() }
            .map {
                it.copy(
                    length = it.length.coerceIn(1, MAX_TICKS),
                    pitch = it.pitch.coerceIn(0, 127),
                    velocity = it.velocity.coerceIn(0.05f, 1f),
                    played = it.played?.takeIf { p -> p in 0 until MAX_STEPS * TICKS_PER_STEP && p != it.start },
                )
            }
            .sortedWith(compareBy({ it.start }, { it.pitch }))
            .toList()
        val (inLoop, past) = clean.partition { it.start < total }
        val shown = inLoop.take(MAX_NOTES)
        return copy(steps = s, notes = shown, hidden = past.take(MAX_NOTES - shown.size), performance = performance.sanitized())
    }

    /** What [sanitized] would leave as it is: notes in the loop and [hidden] past it, both in order, all in range. */
    private fun isClean(total: Int): Boolean {
        if (notes.size + hidden.size > MAX_NOTES) return false
        fun ordered(list: List<Note>, from: Int, until: Int): Boolean {
            var previous: Note? = null
            for (n in list) {
                if (n.start !in from until until || n.length !in 1..MAX_TICKS || n.pitch !in 0..127) return false
                if (!n.velocity.isFinite() || n.velocity < 0.05f || n.velocity > 1f) return false
                if (n.played != null && (n.played !in 0 until MAX_STEPS * TICKS_PER_STEP || n.played == n.start)) return false
                if (previous != null && (n.start < previous.start || (n.start == previous.start && n.pitch < previous.pitch))) return false
                previous = n
            }
            return true
        }
        return ordered(notes, 0, total) && ordered(hidden, total, MAX_STEPS * TICKS_PER_STEP)
    }

    /** No note at all, not even past the loop's end (« Effacer »). */
    fun cleared(): NoteClip = copy(notes = emptyList(), hidden = emptyList())

    /**
     * The loop [newSteps] long: the notes past its end are hidden (and come back when it grows
     * again), and where each note was played follows the new loop ([Note.movedTo]: « Comme joué »
     * still puts a note played just before the loop came round at the new loop's end).
     */
    fun withSteps(newSteps: Int): NoteClip {
        val s = newSteps.coerceIn(MIN_STEPS, MAX_STEPS).let { (it / 16).coerceAtLeast(1) * 16 }
        if (s == steps) return sanitized()
        val newTicks = s * TICKS_PER_STEP
        fun moved(n: Note) = n.movedTo(n.start, ticks, newTicks)
        return copy(steps = s, notes = notes.map(::moved), hidden = hidden.map(::moved)).sanitized()
    }

    /** Notes the clip can still take before [MAX_NOTES] (the ones hidden past its end count). */
    val room: Int get() = (MAX_NOTES - notes.size - hidden.size).coerceAtLeast(0)

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
    fun quantized(gridTicks: Int, strength: Float): NoteClip = inTime({ true }, gridTicks, strength)

    /**
     * « Remettre en rythme », without losing anything: the starts of the notes [chosen] picks move
     * toward the [gridTicks] grid by [strength] (0 = as played, 1 = exactly on it), always from where
     * they were played ([Note.playedStart], kept in [Note.played]): doing it again with another
     * strength never piles up, and [asPlayed] brings them back. Lengths are kept.
     */
    fun inTime(chosen: (Note) -> Boolean, gridTicks: Int, strength: Float): NoteClip {
        val k = strength.coerceIn(0f, 1f)
        val grid = gridTicks.coerceAtLeast(1)
        return copy(notes = notes.map { n -> if (chosen(n)) n.inTime(grid, k, ticks) else n }).sanitized()
    }

    /** The notes [chosen] picks back where they were played (« Comme joué »). */
    fun asPlayed(chosen: (Note) -> Boolean): NoteClip =
        copy(notes = notes.map { n -> if (chosen(n) && n.played != null) n.copy(start = n.played, played = null) else n }).sanitized()

    /** Some notes are not where they were played (« Comme joué » would move them). */
    val anyMovedInTime: Boolean get() = notes.any { it.played != null }

    /**
     * Replaces the whole clip with [progression] in [harmony]: one chord per bar, 4 bars, the
     * scale's own chords ([Harmony.progressionDegree]).
     */
    fun withProgression(progression: Progression, harmony: Harmony, octaveRoot: Int = 48): NoteClip =
        withChords(progression.degrees, harmony, octaveRoot)

    /**
     * A bass line: each chord's root (one chord per bar on the scale [degrees]) in the bass, on
     * [pattern] (one character per step: 'X' strong, 'x' medium, 'o' soft, '.' nothing); each note
     * lasts until the next one (two beats at most). A diminished or augmented degree plays its
     * closest major or minor chord's root ([Harmony.progressionDegree]), as [withChords] does.
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
            val pitch = harmony.degreeRoot(harmony.progressionDegree(degree), octaveRoot)
            hits.mapIndexed { i, (step, velocity) ->
                val next = hits.getOrNull(i + 1)?.first ?: 16
                Note((bar * 16 + step) * TICKS_PER_STEP, (next - step).coerceIn(1, 8) * TICKS_PER_STEP, pitch, velocity)
            }
        }
        // A new clip: notes hidden by an earlier shortening do not come back inside it.
        return copy(steps = 16 * degrees.size.coerceAtLeast(1), notes = notes, hidden = emptyList()).sanitized()
    }

    /**
     * One chord per bar on the scale [degrees] (0 = I), held for the bar: the triads of the key's
     * seven-note scale ([Scale.parent]: the minor pentatonic and the blues take the minor's,
     * « Libre » the major's), a diminished or augmented one replaced ([Harmony.progressionDegree]).
     */
    fun withChords(degrees: List<Int>, harmony: Harmony, octaveRoot: Int = 48, velocity: Float = 0.8f): NoteClip {
        val bar = 16 * TICKS_PER_STEP
        val chords = degrees.flatMapIndexed { i, degree ->
            harmony.chord(harmony.degreeRoot(harmony.progressionDegree(degree), octaveRoot)).map { Note(i * bar, bar, it, velocity) }
        }
        return copy(steps = 16 * degrees.size.coerceAtLeast(1), notes = chords, hidden = emptyList()).sanitized()
    }

    companion object {
        const val TICKS_PER_STEP = 24
        const val MIN_STEPS = 16
        /** 16 bars of 4/4 (the engine's NoteTrack::kMaxSteps). */
        const val MAX_STEPS = 256
        const val MAX_NOTES = 512
        /** A note lasts at most the longest loop (the engine takes up to 15 bits: NoteTrack::packIndex). */
        const val MAX_TICKS = MAX_STEPS * TICKS_PER_STEP
        val LENGTHS_IN_STEPS = listOf(16, 32, 64, 128, 256)

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

/**
 * This note put in time on a [grid] of ticks by [strength] 0..1 from where it was played, in a loop
 * of [loopTicks] (a note pulled onto the loop's end starts the loop, as it is heard).
 */
fun Note.inTime(grid: Int, strength: Float, loopTicks: Int): Note {
    val from = playedStart
    val target = (from.toFloat() / grid).roundToInt() * grid
    val start = Math.floorMod((from + (target - from) * strength).roundToInt(), loopTicks.coerceAtLeast(1))
    return copy(start = start, played = from.takeIf { it != start })
}
