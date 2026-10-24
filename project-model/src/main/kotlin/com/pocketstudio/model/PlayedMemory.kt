package com.pocketstudio.model

import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * « Garder ce que je viens de jouer »: the keys and pads played lately, kept even when nothing is
 * recorded, so that the last bars played can still be kept afterwards (retroactive recording).
 *
 * A ring of [capacity] events in arrays made once: playing a note writes a few numbers and never
 * allocates (the keyboard calls it on every key). The oldest events go first. Positions are in
 * sixteenths on one continuous clock per [epoch] (the transport's while it plays, the wall clock at
 * the project's tempo while stopped); a new epoch starts each time that clock changes, and a
 * capture only takes the latest one (two clocks do not line up).
 *
 * Not thread safe: the screen's thread only.
 */
class PlayedMemory(val capacity: Int = 2048) {
    private val kind = ByteArray(capacity)  // NOTE or PAD
    private val track = IntArray(capacity)  // instrument track, or the pad's lane
    private val pitch = IntArray(capacity)
    private val velocity = FloatArray(capacity)
    private val start = DoubleArray(capacity)
    private val end = DoubleArray(capacity)  // NaN while the key is held
    private val epochOf = IntArray(capacity)
    private var head = 0  // the next slot written
    private var count = 0

    /** The clock the next events count on; see [newEpoch]. */
    var epoch = 0
        private set

    /** True when the clock of [epoch] is the transport's (the bars line up with the music). */
    var epochPlaying = false
        private set

    /** The clock changes (play, stop): what comes next counts on another one. */
    fun newEpoch(playing: Boolean) {
        epoch++
        epochPlaying = playing
        // Keys held across the change keep the length they had (their end is unknown on the new clock).
        for (i in 0 until count) {
            val k = slot(i)
            if (end[k].isNaN() && epochOf[k] == epoch - 1) end[k] = start[k] + MIN_NOTE_STEPS
        }
    }

    val isEmpty: Boolean get() = count == 0

    /** Nothing played on the current clock. */
    val isEmptyNow: Boolean get() = count == 0 || epochOf[slot(count - 1)] != epoch

    /** The last event's start on the current clock (NaN when none). */
    val lastAt: Double get() = if (isEmptyNow) Double.NaN else start[slot(count - 1)]

    fun clear() {
        head = 0
        count = 0
    }

    private fun slot(i: Int): Int = Math.floorMod(head - count + i, capacity)

    private fun push(k: Byte, t: Int, p: Int, v: Float, at: Double, until: Double) {
        kind[head] = k
        track[head] = t
        pitch[head] = p
        velocity[head] = v
        start[head] = at
        end[head] = until
        epochOf[head] = epoch
        head = (head + 1) % capacity
        if (count < capacity) count++
    }

    /** Key [p] down on instrument track [t] at [at] sixteenths. */
    fun noteOn(t: Int, p: Int, v: Float, at: Double) = push(NOTE, t, p, v, at, Double.NaN)

    /** Key [p] up on track [t] at [at]: the last one held gets its end. */
    fun noteOff(t: Int, p: Int, at: Double) {
        for (i in count - 1 downTo 0) {
            val k = slot(i)
            if (kind[k] == NOTE && track[k] == t && pitch[k] == p && end[k].isNaN() && epochOf[k] == epoch) {
                end[k] = maxOf(at, start[k] + MIN_NOTE_STEPS)
                return
            }
        }
    }

    /** Pad [lane] hit at [at]. */
    fun pad(lane: Int, v: Float, at: Double) = push(PAD, lane, 0, v, at, at)

    /** A note kept: [start] and [length] in sixteenths from the capture's first bar line. */
    data class KeptNote(val track: Int, val pitch: Int, val velocity: Float, val start: Double, val length: Double)

    /** A pad hit kept, [at] sixteenths from the capture's first bar line. */
    data class KeptHit(val lane: Int, val velocity: Float, val at: Double)

    /**
     * What was played over the last [bars] bars of [stepsPerBar] before [now] on the current clock:
     * [firstBar] is the bar (of that clock) the capture starts on, [bars] how many it covers, from
     * the bar of its first event to the bar of its last. On the wall clock (stopped) the bars are
     * the player's own: the first note starts the first bar. Keys still held end at [now].
     */
    data class Capture(val notes: List<KeptNote>, val hits: List<KeptHit>, val firstBar: Int, val bars: Int, val playing: Boolean) {
        val isEmpty: Boolean get() = notes.isEmpty() && hits.isEmpty()
    }

    fun capture(now: Double, bars: Int, stepsPerBar: Int): Capture {
        @Suppress("NAME_SHADOWING") val bars = bars.coerceAtLeast(1)
        val bar = stepsPerBar.coerceAtLeast(1).toDouble()
        val from = now - bars * bar
        var first = Double.POSITIVE_INFINITY
        var last = Double.NEGATIVE_INFINITY
        for (i in 0 until count) {
            val k = slot(i)
            if (epochOf[k] != epoch || start[k] < from || start[k] > now) continue
            first = minOf(first, start[k])
            last = maxOf(last, start[k])
        }
        if (first == Double.POSITIVE_INFINITY) return Capture(emptyList(), emptyList(), 0, 0, epochPlaying)
        // On the transport, the bar lines are the music's; on the wall clock, the first note's.
        val origin = if (epochPlaying) floor((first + EARLY_STEPS) / bar) * bar else first
        val span = ((floor((last - origin) / bar) + 1).toInt()).coerceIn(1, bars)
        val notes = ArrayList<KeptNote>()
        val hits = ArrayList<KeptHit>()
        for (i in 0 until count) {
            val k = slot(i)
            if (epochOf[k] != epoch || start[k] < from || start[k] > now) continue
            val at = (start[k] - origin).coerceAtLeast(0.0)
            if (at >= span * bar) continue
            if (kind[k] == NOTE) {
                val until = if (end[k].isNaN()) now else end[k]
                notes += KeptNote(track[k], pitch[k], velocity[k], at, (until - start[k]).coerceAtLeast(MIN_NOTE_STEPS))
            } else {
                hits += KeptHit(track[k], velocity[k], at)
            }
        }
        return Capture(notes, hits, (origin / bar).roundToInt(), span, epochPlaying)
    }

    companion object {
        const val NOTE: Byte = 0
        const val PAD: Byte = 1
        /** The shortest note kept (half a sixteenth). */
        const val MIN_NOTE_STEPS = 0.5
        /** A note up to this much before a bar line (played a hair early) starts that bar. */
        const val EARLY_STEPS = 0.5
        /** Bars kept by « Garder ce que je viens de jouer ». */
        const val BARS = 8
    }
}

/**
 * [capture] (see [PlayedMemory.capture]) written into instrument track [id]'s notes, unchanged where
 * it was played: the loop grows to hold it (whole bars, up to [NoteClip.MAX_STEPS]); played on the
 * music's clock, each note keeps its place in the loop heard ([PlayedMemory.Capture.firstBar]),
 * else the first note starts the loop. The notes already there stay. Null when nothing of that
 * track was played.
 */
fun NoteClip.withCaptured(capture: PlayedMemory.Capture, id: Int, stepsPerBar: Int): NoteClip? {
    val bar = stepsPerBar.coerceAtLeast(1)
    val longest = NoteClip.MAX_STEPS / bar * bar
    // More bars than the longest loop holds: the last ones that fit (folded, they would land on each other).
    val over = (capture.bars - longest / bar).coerceAtLeast(0)
    val mine = capture.notes.filter { it.track == id && it.start >= over * bar }.map { it.copy(start = it.start - over * bar) }
    if (mine.isEmpty()) return null
    val firstBar = capture.firstBar + over
    val needed = minOf(longest, (capture.bars - over) * bar)
    // Whole bars, and only longer (the notes already there keep their loop).
    val steps = maxOf(this.steps, NoteClip.LENGTHS_IN_STEPS.firstOrNull { it >= needed } ?: NoteClip.MAX_STEPS)
    val loopBars = (steps / bar).coerceAtLeast(1)
    val shift = if (capture.playing) Math.floorMod(firstBar, loopBars) * bar else 0
    val ticks = steps * NoteClip.TICKS_PER_STEP
    // Never more than the clip can take: past it the clean-up would drop notes already there.
    val room = (NoteClip.MAX_NOTES - notes.size - hidden.size).coerceAtLeast(0)
    val added = mine.take(room).map { n ->
        val start = Math.floorMod(((n.start + shift) * NoteClip.TICKS_PER_STEP).roundToInt(), ticks)
        Note(start, (n.length * NoteClip.TICKS_PER_STEP).roundToInt().coerceIn(1, NoteClip.MAX_TICKS), n.pitch, n.velocity)
    }
    return withSteps(steps).let { it.copy(notes = it.notes + added) }.sanitized()
}

/** The pads of [capture] written into [drums] on the nearest sixteenth (a hit already there stays). Null when none. */
fun DrumPattern.withCaptured(capture: PlayedMemory.Capture, stepsPerBar: Int): DrumPattern? {
    if (capture.hits.isEmpty()) return null
    val bar = stepsPerBar.coerceAtLeast(1)
    val loopBars = (steps / bar).coerceAtLeast(1)
    val shift = if (capture.playing) Math.floorMod(capture.firstBar, loopBars) * bar else 0
    var out = this
    capture.hits.forEach { h ->
        val lane = out.lanes.getOrNull(h.lane) ?: return@forEach
        val step = Math.floorMod((h.at + shift).roundToInt(), steps.coerceAtLeast(1))
        if (lane.hits[step] == null) {
            out = out.copy(lanes = out.lanes.toMutableList().also { it[h.lane] = lane.copy(hits = lane.hits + (step to Hit(h.velocity))) })
        }
    }
    return out
}
