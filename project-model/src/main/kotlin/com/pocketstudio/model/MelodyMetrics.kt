package com.pocketstudio.model

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The twenty measures of a melody of docs/hits/autres-et-melodie.md (§ B13), computed from a
 * [NoteClip] and the harmony it plays on: phrase architecture, motif recurrence, rests, peak,
 * contour, chord tones, voice leading, register, velocities, humanisation, rhythm. Pure Kotlin,
 * no audio: [MelodyRules] turns a [Report] into pass or fail for a lead, a bass or a counter.
 * Positions are read on the sixteenth grid (a humanised note counts where it was meant), bars
 * are 16 steps, beats 4, phrases 4 bars.
 */
object MelodyMetrics {
    const val T = NoteClip.TICKS_PER_STEP
    const val BEAT = 4 * T
    const val BAR = 16 * T
    const val PHRASE = 4 * BAR

    /** Ticks a rest must last to count as a breath (one beat) and as a phrase end (two beats). */
    const val REST = BEAT
    const val PHRASE_REST = 2 * BEAT

    /** Ticks a note is "long" (a beat) and a passing note "short" (an eighth). */
    const val LONG = BEAT
    const val SHORT = 2 * T

    /** An interval over a fourth is a leap; up to a tone, a step. */
    const val LEAP = 5
    const val STEP = 2

    /**
     * The harmony a clip is judged against: the key, one chord degree per bar (cycling over the
     * clip), and whether the chord's seventh counts as a chord tone (styles voiced in sevenths and
     * ninths) or only its triad.
     */
    data class Context(val harmony: Harmony, val degrees: List<Int>, val seventh: Boolean = true) {
        fun degree(bar: Int): Int = degrees[Math.floorMod(bar, degrees.size.coerceAtLeast(1))]

        /** The chord's pitch classes in [bar]. */
        fun chordTones(bar: Int): Set<Int> =
            harmony.voiced(degree(bar), 0, if (seventh) Voicing.Seventh else Voicing.Triad).map { Math.floorMod(it, 12) }.toSet()

        /** The chord's root in [bar], as a pitch class. */
        fun root(bar: Int): Int = Math.floorMod(harmony.degreeRoot(degree(bar), 0), 12)
    }

    /** A note on the grid: where it was meant ([start], [end]) and its bar. */
    private data class G(val note: Note, val start: Int, val end: Int) {
        val bar: Int get() = start / BAR
        val pos: Int get() = start % BAR
        val strong: Boolean get() = start % (2 * BEAT) == 0
        val pitch: Int get() = note.pitch
        /** The length as meant, on the grid (a humanised note is a tick or two longer or shorter). */
        val length: Int get() = end - start
        val velocity: Float get() = note.velocity
        val lengthSteps: Int get() = (length.toFloat() / T).roundToInt().coerceAtLeast(1)
    }

    /** The nearest sixteenth to [tick]. */
    fun grid(tick: Int): Int = ((tick + T / 2) / T) * T

    data class Report(
        val notes: Int,
        /** M1: the clip's length in steps. */
        val steps: Int,
        /** M2: bars (over 16 bars, the clip repeated) whose motif is bar 1's within two edits. */
        val motifOccurrences: Int,
        /** M3: consecutive bars exactly alike (per clip), bars alike another bar (share, empty bars left out). */
        val identicalPairs: Int,
        val identicalShare: Float,
        /** M4: rests of a beat or more in each 4-bar phrase; a rest of two beats at each phrase's end. */
        val restsPerPhrase: List<Int>,
        val phraseEndRest: List<Boolean>,
        /** M5: each phrase ends on a chord tone held two beats or followed by two beats of rest. */
        val phraseEndOk: List<Boolean>,
        /** M6: beats per note. */
        val density: Float,
        /** M7: how often the top note sounds, where it first sounds, whether on a strong beat. */
        val peakCount: Int,
        val peakStart: Int,
        val peakStrong: Boolean,
        /** M8: share of steps among the intervals, most leaps in a phrase, share of leaps answered by a step back. */
        val conjunct: Float,
        val leapsPerPhrase: Int,
        val leapsResolved: Float,
        /** M9: chord tones by duration, overall and on beats 1 and 3. */
        val chordTones: Float,
        val strongChordTones: Float,
        /** M10: non-chord notes short and resolved by step; long notes a semitone over a chord tone. */
        val tensionsResolved: Float,
        val longClashes: Int,
        /** M11: range and middle (MIDI) of the line. */
        val ambitus: Int,
        val median: Int,
        /** M12: velocity spread, share of the most common value (to 0.05), strong − weak, bars 7–8 − bars 1–2 (the first hit apart). */
        val velocityRange: Float,
        val modalShare: Float,
        val strongWeak: Float,
        val crescendo: Float,
        /** M13: notes off the grid (among those not on a bar's first beat), the largest shift, one shift per position. */
        val shiftedShare: Float,
        val maxShift: Int,
        val profileStable: Boolean,
        /** M14: distinct lengths (in steps), share of the most common one. */
        val distinctLengths: Int,
        val commonLengthShare: Float,
        /** M15: notes off the beat (per clip), notes in the last eighth of a bar (pickups). */
        val syncopated: Int,
        val pickups: Int,
        /** M18: at chord changes, common tones kept and entries by step; most bars of a phrase that merely transpose its first; chord changes. */
        val commonTones: Int,
        val stepEntries: Int,
        val transposedBars: Int,
        val chordChanges: Int,
        val distinctPitches: Int,
        /** The bars of the clip. */
        val bars: Int,
    )

    /** An empty clip's report: everything at zero. */
    private val EMPTY = Report(
        0, 0, 0, 0, 0f, emptyList(), emptyList(), emptyList(), 0f, 0, 0, false, 0f, 0, 1f, 0f, 0f, 1f, 0,
        0, 0, 0f, 1f, 0f, 0f, 0f, 0, true, 0, 1f, 0, 0, 0, 0, 0, 0, 0, 0,
    )

    fun analyse(clip: NoteClip, ctx: Context): Report {
        val all = clip.notes.sortedWith(compareBy({ it.start }, { it.pitch })).map { n ->
            val s = grid(n.start)
            G(n, s, s + grid(n.length).coerceAtLeast(T))
        }
        if (all.isEmpty()) return EMPTY.copy(steps = clip.steps, bars = clip.steps / 16)
        val bars = (clip.steps / 16).coerceAtLeast(1)
        val ticks = clip.steps * T
        // The line: one note per attack, the top one of a doubled octave or a chord.
        val line = all.groupBy { it.start }.toSortedMap().values.map { g -> g.maxBy { it.pitch } }
        val byBar = (0 until bars).map { b -> line.filter { it.bar == b } }

        // M2: the motif of bar 1 (intervals, durations) found again within two edits over 16 bars.
        val motif = signature(byBar[0])
        val occurrences = if (byBar[0].isEmpty()) 0 else (0 until 16).count { b ->
            val s = signature(byBar[b % bars])
            byBar[b % bars].isNotEmpty() && editDistance(motif.first, s.first) + editDistance(motif.second, s.second) <= 2
        }

        // M3: bars exactly alike.
        val sigs = byBar.map { bar -> bar.map { listOf(it.pos, it.lengthSteps, it.pitch, (it.velocity * 100).roundToInt()) } }
        val identicalPairs = (0 until bars - 1).count { sigs[it].isNotEmpty() && sigs[it] == sigs[it + 1] }
        val filled = sigs.filter { it.isNotEmpty() }
        val identicalShare = if (filled.isEmpty()) 0f else filled.count { s -> filled.count { it == s } > 1 }.toFloat() / filled.size

        // M4 and M5: rests between notes (the last one wraps to the first), phrase ends.
        val phrases = (bars + 3) / 4
        val gaps = line.zipWithNext { a, b -> Triple(a.end, b.start - a.end, a) } +
            Triple(line.last().end, ticks - line.last().end + line.first().start, line.last())
        val restsPerPhrase = (0 until phrases).map { p -> gaps.count { (at, gap, _) -> gap >= REST && (at / PHRASE).coerceAtMost(phrases - 1) == p } }
        val phraseEndRest = ArrayList<Boolean>()
        val phraseEndOk = ArrayList<Boolean>()
        for (p in 0 until phrases) {
            val inPhrase = line.filter { it.start / PHRASE == p }
            if (inPhrase.isEmpty()) { phraseEndRest += true; phraseEndOk += true; continue }
            val last = inPhrase.last()
            val gap = gaps.first { it.third === last }.second
            phraseEndRest += gap >= PHRASE_REST
            phraseEndOk += (last.length >= PHRASE_REST || gap >= PHRASE_REST) && Math.floorMod(last.pitch, 12) in ctx.chordTones(last.bar)
        }

        // M6.
        val density = all.sumOf { it.length }.toFloat() / BEAT / all.size

        // M7.
        val top = line.maxOf { it.pitch }
        val peaks = line.filter { it.pitch == top }
        val peakStart = peaks.first().start

        // M8.
        val intervals = line.zipWithNext { a, b -> b.pitch - a.pitch }
        val conjunct = if (intervals.isEmpty()) 1f else intervals.count { abs(it) <= STEP }.toFloat() / intervals.size
        val leapIndices = intervals.indices.filter { abs(intervals[it]) > LEAP }
        val leapsPerPhrase = leapIndices.groupBy { line[it].start / PHRASE }.values.maxOfOrNull { it.size } ?: 0
        val resolved = leapIndices.count { i ->
            val leap = intervals[i]
            // An octave at the very end of the clip (the lift before the chorus) needs no answer.
            if (abs(leap) == 12 && i == intervals.lastIndex) return@count true
            (i + 1..i + 2).any { j -> j < intervals.size && intervals[j] != 0 && abs(intervals[j]) <= STEP && (intervals[j] > 0) != (leap > 0) }
        }
        val leapsResolved = if (leapIndices.isEmpty()) 1f else resolved.toFloat() / leapIndices.size

        // M9.
        fun inChord(g: G) = Math.floorMod(g.pitch, 12) in ctx.chordTones(g.bar)
        val total = all.sumOf { it.length }.toFloat()
        val chordTones = all.filter { inChord(it) }.sumOf { it.length } / total
        val strong = all.filter { it.strong }
        val strongChordTones = if (strong.isEmpty()) 1f else strong.filter { inChord(it) }.sumOf { it.length }.toFloat() / strong.sumOf { it.length }

        // M10.
        val tensions = line.withIndex().filter { !inChord(it.value) }
        val tensionsResolved = if (tensions.isEmpty()) 1f else tensions.count { (i, g) ->
            val next = line.getOrNull(i + 1)
            g.length <= SHORT && next != null && next.bar == g.bar && inChord(next) && abs(next.pitch - g.pitch) <= STEP
        }.toFloat() / tensions.size
        val longClashes = all.count { g -> g.length >= LONG && !inChord(g) && ctx.chordTones(g.bar).any { c -> Math.floorMod(g.pitch - c, 12) == 1 } }

        // M11.
        val pitches = line.map { it.pitch }.sorted()
        val ambitus = pitches.last() - pitches.first()
        val median = pitches[pitches.size / 2]

        // M12.
        val velocities = all.map { it.velocity }
        val velocityRange = velocities.max() - velocities.min()
        val modalShare = velocities.groupBy { (it / 0.05f).roundToInt() }.values.maxOf { it.size }.toFloat() / velocities.size
        val weak = all.filter { !it.strong }
        // The section's first hit (at full strength by rule) left out; a line all on the beat, or all off it (or nearly), has nothing to compare.
        val strongRest = all.drop(1).filter { it.strong }
        val weakRest = all.drop(1).filter { !it.strong }
        val strongWeak = if (strongRest.size < 3 || weakRest.size < 3) 1f else strongRest.map { it.velocity }.average().toFloat() - weakRest.map { it.velocity }.average().toFloat()
        // The section's first hit (at full strength by rule) is left out of the rise.
        val head = all.drop(1).filter { it.bar < 2 }.map { it.velocity }
        val tail = all.filter { it.bar in bars - 2 until bars }.map { it.velocity }
        // Too sparse to measure (a note every two bars): taken as rising.
        val crescendo = if (head.isEmpty() || tail.isEmpty()) 1f else tail.average().toFloat() - head.average().toFloat()

        // M13.
        val offBeatOne = all.filter { it.pos != 0 }
        val shifted = offBeatOne.filter { it.note.start != it.start }
        val shiftedShare = if (offBeatOne.isEmpty()) 1f else shifted.size.toFloat() / offBeatOne.size
        val maxShift = all.maxOf { abs(it.note.start - it.start) }
        val profileStable = all.groupBy { it.pos }.values.all { group ->
            group.groupBy { it.note.start - it.start }.values.maxOf { it.size } * 4 >= group.size * 3
        }

        // M14.
        val lengths = all.map { it.lengthSteps }
        val distinctLengths = lengths.toSet().size
        val commonLengthShare = lengths.groupBy { it }.values.maxOf { it.size }.toFloat() / lengths.size

        // M15.
        val syncopated = line.count { it.start % BEAT != 0 }
        val pickups = line.count { it.pos >= 14 * T }

        // M18.
        var commonTones = 0
        var stepEntries = 0
        var chordChanges = 0
        for (b in 1 until bars) {
            val changed = ctx.chordTones(b) != ctx.chordTones(b - 1)
            if (!changed) continue
            chordChanges++
            val before = line.lastOrNull { it.start < b * BAR } ?: continue
            val first = byBar[b].firstOrNull() ?: continue
            val held = before.end > b * BAR && Math.floorMod(before.pitch, 12) in ctx.chordTones(b)
            if (first.pitch == before.pitch || held) commonTones++
            else if (abs(first.pitch - before.pitch) <= STEP) stepEntries++
        }
        val transposedBars = (0 until phrases).maxOf { p ->
            val ref = byBar.getOrNull(4 * p)?.map { it.pitch - ctx.root(it.bar) }?.sorted() ?: emptyList()
            if (ref.isEmpty()) 0 else (4 * p until minOf(4 * p + 4, bars)).count { b -> byBar[b].map { it.pitch - ctx.root(b) }.sorted() == ref }
        }

        return Report(
            notes = all.size, steps = clip.steps, motifOccurrences = occurrences,
            identicalPairs = identicalPairs, identicalShare = identicalShare,
            restsPerPhrase = restsPerPhrase, phraseEndRest = phraseEndRest, phraseEndOk = phraseEndOk,
            density = density, peakCount = peaks.size, peakStart = peakStart, peakStrong = peakStart % (2 * BEAT) == 0,
            conjunct = conjunct, leapsPerPhrase = leapsPerPhrase, leapsResolved = leapsResolved,
            chordTones = chordTones, strongChordTones = strongChordTones,
            tensionsResolved = tensionsResolved, longClashes = longClashes,
            ambitus = ambitus, median = median,
            velocityRange = velocityRange, modalShare = modalShare, strongWeak = strongWeak, crescendo = crescendo,
            shiftedShare = shiftedShare, maxShift = maxShift, profileStable = profileStable,
            distinctLengths = distinctLengths, commonLengthShare = commonLengthShare,
            syncopated = syncopated, pickups = pickups,
            commonTones = commonTones, stepEntries = stepEntries, transposedBars = transposedBars, chordChanges = chordChanges,
            distinctPitches = pitches.toSet().size, bars = bars,
        )
    }

    /** A bar's motif: its intervals and its durations in steps. */
    private fun signature(bar: List<G>): Pair<List<Int>, List<Int>> =
        bar.zipWithNext { a, b -> b.pitch - a.pitch } to bar.map { it.lengthSteps }

    /** Levenshtein distance between two sequences. */
    fun editDistance(a: List<Int>, b: List<Int>): Int {
        if (a.isEmpty()) return b.size
        if (b.isEmpty()) return a.size
        var previous = IntArray(b.size + 1) { it }
        for (i in 1..a.size) {
            val current = IntArray(b.size + 1)
            current[0] = i
            for (j in 1..b.size) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(previous[j] + 1, current[j - 1] + 1, previous[j - 1] + cost)
            }
            previous = current
        }
        return previous[b.size]
    }

    /** M16: how a second clip ([other]) answers [lead]: its attacks in each rest of two beats, and how many land with the lead. */
    data class Response(val gaps: Int, val gapsAnswered: Int, val simultaneous: Float)

    fun response(lead: NoteClip, other: NoteClip): Response {
        val l = lead.notes.sortedBy { it.start }.groupBy { grid(it.start) }.toSortedMap()
        val ticks = lead.steps * T
        val starts = l.keys.toList()
        val ends = l.values.map { g -> g.maxOf { grid(it.start) + grid(it.length) } }
        val gaps = ArrayList<IntRange>()
        for (i in starts.indices) {
            val end = ends[i]
            val next = starts.getOrNull(i + 1) ?: (ticks + starts.first())
            if (next - end >= PHRASE_REST) gaps += end until next
        }
        val attacks = other.notes.map { grid(it.start) }.distinct()
        val answered = gaps.count { gap -> attacks.any { a -> a in gap || (a + ticks) in gap } }
        val simultaneous = if (attacks.isEmpty()) 0f else attacks.count { it in l.keys }.toFloat() / attacks.size
        return Response(gaps.size, answered, simultaneous)
    }

    /** M17: an arpeggio as it sounds (its [Performance] expanded): distinct pitches, range, a fixed pattern, step, accent. */
    data class Arp(val distinctPitches: Int, val range: Int, val fixedPattern: Boolean, val rate: Int, val accent: Float, val mode: ArpMode)

    fun arp(clip: NoteClip): Arp {
        val played = clip.performance.expand(clip)
        val pitches = played.map { it.pitch }
        val perf = clip.performance
        return Arp(
            distinctPitches = pitches.toSet().size,
            range = if (pitches.isEmpty()) 0 else pitches.max() - pitches.min(),
            fixedPattern = perf.arp == ArpMode.Pattern && perf.pattern.isNotEmpty(),
            rate = perf.arpRate,
            accent = if (played.isEmpty()) 0f else played.maxOf { it.velocity } - played.minOf { it.velocity },
            mode = perf.arp,
        )
    }

    /** M19: [verse]'s notes are among [chorus]'s (same start and pitch); how many of them it keeps. */
    data class Variant(val subset: Boolean, val share: Float)

    fun variant(verse: NoteClip, chorus: NoteClip): Variant {
        val full = chorus.notes.map { grid(it.start) to it.pitch }.toSet()
        val part = verse.notes.map { grid(it.start) to it.pitch }
        return Variant(part.all { it in full }, if (chorus.notes.isEmpty()) 0f else verse.notes.size.toFloat() / chorus.notes.size)
    }
}
