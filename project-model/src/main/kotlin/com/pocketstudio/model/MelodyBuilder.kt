package com.pocketstudio.model

import com.pocketstudio.model.StarterWizard.LeadKind
import com.pocketstudio.model.StarterWizard.Mood
import com.pocketstudio.model.StarterWizard.Style
import com.pocketstudio.model.StarterWizard.Version
import kotlin.math.abs

/**
 * Writes the assistant's melodies the way the hits do (docs/hits/autres-et-melodie.md § B14):
 * from the recipe's cell (one, two or four bars) an eight-bar phrase in the form A A′ B A″ (call,
 * call with its end changed, answer, cadence; then the same developed), with rests at the phrase
 * ends, a single peak in the second half, chord tones on the strong beats, a line that keeps a
 * common tone or steps into the next chord instead of being transposed with it, velocities that
 * follow the position in the bar and rise towards the end, and a deterministic humanisation (the
 * same few ticks for the same position, never on a bar's first beat). The same clip comes in
 * [Version]s: the Intro hears the first phrase, the Couplet a thinned-out line, the Refrain all of
 * it, the second Refrain doubled an octave down (or an octave up for a gliding lead). The bass
 * gets eight bars too: its line, then its line varied, a beat of rest before each fill and two
 * beats before the turn. The counter-melody answers the lead's motif in the lead's own rests.
 * Everything is seeded by the style and the mood: the same song every time, on every phone.
 */
object MelodyBuilder {
    private const val T = NoteClip.TICKS_PER_STEP
    private const val BAR = 16
    private const val PHRASE_BARS = 8
    private const val STEPS = BAR * PHRASE_BARS

    /** Velocity of a lead's and a bass's strongest note. */
    private const val LEAD_VELOCITY = 0.8f
    private const val BASS_VELOCITY = 0.9f
    private const val COUNTER_VELOCITY = 0.62f

    /** Lower case in a pattern: that much softer. */
    private const val SOFT = 0.7f

    /** The lead's rise towards the Refrain: louder over the last bars of the phrase, bar by bar. */
    private val RISE = listOf(1f, 1f, 1.02f, 1.02f, 1.05f, 1.06f, 1.12f, 1.14f)

    /** The most a humanised note moves, in ticks (about 10 ms at 120 BPM). */
    const val HUMANIZE_TICKS = 2

    /**
     * A note of a line being written: its step in the clip, its length in steps, its pitch as a
     * degree of the key counted from the tonic (7 = the tonic an octave up: a scale index, so that
     * a step up or down stays in the key), [soft] for a lower-case token, [slide] for a note that
     * glides into the next.
     */
    data class Ev(val step: Int, val len: Int, val index: Int, val soft: Boolean = false, val slide: Boolean = false) {
        val end: Int get() = step + len
        val bar: Int get() = step / BAR
        val pos: Int get() = step % BAR
        val strong: Boolean get() = pos % 8 == 0
    }

    /** The key as scale indices: index ↔ pitch, chord tones per bar. */
    class Key(val harmony: Harmony, val degrees: List<Int>, val seventh: Boolean) {
        private val ints = harmony.scale.parent.intervals
        private val root = Math.floorMod(harmony.root, 12)

        fun pitch(index: Int): Int = root + ints[Math.floorMod(index, 7)] + 12 * Math.floorDiv(index, 7)

        /** The scale index of [pitch] (snapped into the key when it is not in it). */
        fun index(pitch: Int): Int {
            val p = harmony.chordKey.snap(pitch)
            val octave = Math.floorDiv(p - root, 12)
            val degree = ints.indexOf(Math.floorMod(p - root, 12)).coerceAtLeast(0)
            return octave * 7 + degree
        }

        fun degree(bar: Int): Int = degrees[Math.floorMod(bar, degrees.size)]

        /** The chord's pitch classes in [bar]: its triad, and its seventh when the style voices sevenths. */
        fun chordTones(bar: Int): Set<Int> =
            harmony.voiced(degree(bar), 0, if (seventh) Voicing.Seventh else Voicing.Triad).map { Math.floorMod(it, 12) }.toSet()

        /** The triad's pitch classes in [bar]: where a phrase comes to rest. */
        fun triad(bar: Int): Set<Int> = harmony.voiced(degree(bar), 0, Voicing.Triad).map { Math.floorMod(it, 12) }.toSet()

        fun inChord(index: Int, bar: Int): Boolean = Math.floorMod(pitch(index), 12) in chordTones(bar)

        /** The closest scale index to [index] whose pitch is in [tones] (the lower one on a tie, or [up] to prefer the higher). */
        fun nearest(index: Int, tones: Set<Int>, up: Boolean = false): Int {
            for (d in 0..7) {
                val a = if (up) index + d else index - d
                val b = if (up) index - d else index + d
                if (Math.floorMod(pitch(a), 12) in tones) return a
                if (Math.floorMod(pitch(b), 12) in tones) return b
            }
            return index
        }

        /** The first scale index strictly above [index] whose pitch is in [tones]. */
        fun above(index: Int, tones: Set<Int>): Int {
            var i = index + 1
            while (Math.floorMod(pitch(i), 12) !in tones) i++
            return i
        }

        /** The first scale index strictly below [index] whose pitch is in [tones]. */
        fun below(index: Int, tones: Set<Int>): Int {
            var i = index - 1
            while (Math.floorMod(pitch(i), 12) !in tones) i--
            return i
        }
    }

    /** The key of [style] in [mood]: its harmony, its four chords, whether sevenths are chord tones. */
    fun keyOf(style: Style, mood: Mood, harmony: Harmony): Key {
        val degrees = StarterWizard.progressionOf(style, mood).second
        val seventh = style.melody.voicing in setOf(Voicing.Seventh, Voicing.Ninth, Voicing.Rootless7, Voicing.Rootless9)
        return Key(harmony, degrees, seventh)
    }

    /** The seed of [style]'s [part] in [mood]: the same line on every phone. */
    fun seed(style: Style, mood: Mood, part: String): Int = Seeded.of(style.id, mood.id, part)

    // ---- The lead ---------------------------------------------------------------------------

    /** The lead of [style] in [mood], in its [version]. */
    fun lead(style: Style, mood: Mood, harmony: Harmony, version: Version = Version.Chorus, mono: Boolean = style.melody.leadGlide): NoteClip {
        // In range once, so that every version is cut from the same notes.
        val chorus = StarterWizard.inLeadRange(chorus(style, mood, harmony))
        return when (version) {
            Version.Chorus -> chorus
            Version.Intro -> intro(chorus)
            Version.Verse -> verse(chorus)
            Version.Chorus2 -> secondChorus(chorus, mono = mono || style.melody.leadGlide)
        }
    }

    /** The whole hook: eight bars. */
    fun chorus(style: Style, mood: Mood, harmony: Harmony): NoteClip {
        val m = style.melody
        val key = keyOf(style, mood, harmony)
        val seed = seed(style, mood, "lead")
        val octaveRoot = 12 * (m.leadOctave + 1)
        val cell = cell(key, m.leadPattern, octaveRoot, m.leadOnKey)
        val answer = if (m.leadAnswer.isBlank()) null else cell(key, m.leadAnswer, octaveRoot, m.leadOnKey)
        val kind = m.leadKind
        var line = phrase(cell, m.leadPattern.length / BAR, answer, key, seed, kind)
        line = targetChords(line, key)
        if (kind == LeadKind.Hook) line = voiceLead(line, key)
        line = restAtPhraseEnds(line, key, kind)
        if (kind == LeadKind.Hook) {
            line = smoothLeaps(line, key, peak = null)
            line = singlePeak(line, key)
            line = inAmbitus(line, key)
            line = targetChords(line, key)
        }
        line = ensureMotif(line, key, kind)
        line = targetChords(restAtPhraseEnds(line, key, kind), key)
        if (kind == LeadKind.Hook) {
            line = singlePeak(line, key)
            // Leaps, common tones and passing tones settle each other: a few rounds until nothing moves.
            repeat(4) {
                line = ensureCommonTone(line, key, peak = line.maxByOrNull { key.pitch(it.index) })
                line = targetChords(line, key)
                line = smoothLeaps(line, key, peak = line.maxByOrNull { key.pitch(it.index) })
            }
        }
        val notes = humanize(withVelocities(line, key, LEAD_VELOCITY, lead = true), seed)
        return NoteClip(steps = STEPS, notes = notes).sanitized()
    }

    /**
     * The recipe's cell as events, from a 16, 32 or 64-step pattern on the chords of the first
     * bars ([NoteClip.withLine] semantics: tokens on the bar's chord, or on the key with [onKey]).
     */
    fun cell(key: Key, pattern: String, octaveRoot: Int, onKey: Boolean): List<Ev> {
        val bars = (pattern.length / BAR).coerceIn(1, 4)
        val clip = NoteClip().withLine(List(bars) { key.degree(it) }, key.harmony, pattern, octaveRoot, velocity = 1f, onKey = onKey)
        return clip.notes.map { n ->
            val slide = n.length % T != 0
            Ev(n.start / T, ((n.length - if (slide) StarterWizard.SLIDE_OVERLAP else 0) / T).coerceAtLeast(1), key.index(n.pitch), soft = n.velocity < 0.99f, slide = slide)
        }
    }

    /**
     * Eight bars from a cell of [cellBars] bars: A, A with its last note changed, the answer, a
     * cadence; then the answer a step away, the call again, A with its end lifted, a cadence. A
     * four-bar cell is a whole phrase already: its second half is itself developed.
     */
    private fun phrase(cell: List<Ev>, cellBars: Int, answer: List<Ev>?, key: Key, seed: Int, kind: LeadKind): List<Ev> {
        fun bar(src: List<Ev>, b: Int): List<Ev> = src.filter { it.bar == b % maxOf(1, src.maxOfOrNull { it.bar + 1 } ?: 1) }.map { it.copy(step = it.pos) }
        val a0 = bar(cell, 0)
        val plan: List<Pair<List<Ev>, String>> = when (cellBars) {
            1 -> listOf(
                a0 to "", a0 to "eola", (answer?.let { bar(it, 0) } ?: a0) to (if (answer == null) "answer" else ""), a0 to "cadence",
                (answer?.let { bar(it, 0) } ?: a0) to (if (answer == null) "answer+" else "sequence"), a0 to "sequence", a0 to "lift", a0 to "cadence",
            )
            2 -> {
                val a1 = answer?.let { bar(it, 0) } ?: bar(cell, 1)
                listOf(a0 to "", a1 to "", a0 to "eola", a1 to "cadence", a1 to "sequence", a0 to "sequence", a0 to "lift", a1 to "cadence")
            }
            // A four-bar phrase: its second half developed; a riff keeps its rhythm (written in the bass's rests).
            else -> if (kind == LeadKind.Hook) listOf(bar(cell, 0) to "", bar(cell, 1) to "", bar(cell, 2) to "", bar(cell, 3) to "cadence", a0 to "eola", bar(cell, 1) to "sequence", bar(cell, 2) to "lift", bar(cell, 3) to "cadence")
            else listOf(bar(cell, 0) to "", bar(cell, 1) to "", bar(cell, 2) to "", bar(cell, 3) to "", a0 to "eola", bar(cell, 1) to "eola", bar(cell, 2) to "lift", bar(cell, 3) to "")
        }
        val out = ArrayList<Ev>()
        plan.forEachIndexed { b, (src, op) ->
            val next = key.chordTones(b + 1)
            val evs = when (op) {
                "eola" -> eola(src, key, b, next)
                "answer" -> answer(src, a0, key, b)
                "answer+" -> answer(src, a0, key, b).map { it.copy(index = it.index + 1) }
                "sequence" -> sequence(src, a0, key, b, seed)
                "lift" -> lift(src, key, b)
                "cadence" -> cadence(src, key, b, consequent = b == PHRASE_BARS - 1)
                else -> src
            }
            out += evs.map { it.copy(step = it.step + b * BAR) }
        }
        return out.sortedBy { it.step }.distinctBy { it.step }
    }

    /** End-of-line alteration: the last note moves to the nearest tone of the next chord, a step away at least. */
    private fun eola(bar: List<Ev>, key: Key, b: Int, next: Set<Int>): List<Ev> {
        if (bar.isEmpty()) return bar
        val last = bar.last()
        var target = key.nearest(last.index + 1, next, up = true)
        if (target == last.index) target = key.above(last.index, next)
        if (target - last.index > 3) target = key.below(last.index, next)
        return bar.dropLast(1) + last.copy(index = target, slide = false)
    }

    /**
     * The answer to a call: its contour turned upside down around its first note, its rhythm
     * pushed back by an eighth and its last note lengthened; made different enough from the motif
     * (three edits at least) so that it is heard as an answer, not a repeat.
     */
    private fun answer(bar: List<Ev>, motif: List<Ev>, key: Key, b: Int): List<Ev> {
        if (bar.isEmpty()) return bar
        val first = bar.first().index
        var out = bar.map { it.copy(index = first - (it.index - first), slide = false) }
        val fits = out.last().end + 2 <= BAR
        out = if (fits) out.map { it.copy(step = it.step + 2) } else out.map { it.copy(len = maxOf(1, it.len / 2)) }
        out = out.dropLast(1) + out.last().let { it.copy(len = (BAR - 2 - it.step).coerceIn(1, 8)) }
        var guard = 0
        while (distance(out, motif) < 3 && guard++ < 4) {
            out = when (guard) {
                1 -> out.dropLast(1) + out.last().let { it.copy(index = it.index + 1) }
                2 -> out.take(1).map { it.copy(len = it.len + 1) } + out.drop(1)
                3 -> if (out.size > 1) out.drop(1) else if (out.last().end + 1 < BAR) out + out.last().copy(step = out.last().end + 1, len = 1, index = out.last().index - 1) else out.map { it.copy(len = maxOf(1, it.len - 1)) }
                else -> out.map { it.copy(index = it.index - 1) }
            }.filter { it.step < BAR }.map { it.copy(len = minOf(it.len, BAR - it.step)) }
        }
        return out
    }

    /** The bar a step higher or lower (a sequence), its rhythm augmented so that it develops rather than repeats. */
    private fun sequence(bar: List<Ev>, motif: List<Ev>, key: Key, b: Int, seed: Int): List<Ev> {
        if (bar.isEmpty()) return bar
        val shift = if (Seeded.unit(seed, b) >= 0f) 1 else -1
        var out = bar.map { it.copy(index = it.index + shift, slide = false) }
        // Half the notes, twice as long: the sequence breathes.
        if (out.size >= 3) {
            val kept = out.filterIndexed { i, _ -> i % 2 == 0 }
            out = kept.mapIndexed { i, e -> e.copy(len = ((kept.getOrNull(i + 1)?.step ?: BAR) - e.step).coerceIn(1, 8)) }
        } else out = out.map { it.copy(len = minOf(it.len * 2, BAR - it.step)) }
        var guard = 0
        while (distance(out, motif) < 3 && guard++ < 3) {
            out = when (guard) {
                1 -> out.dropLast(1) + out.last().let { it.copy(index = it.index + shift) }
                2 -> out.map { it.copy(step = (it.step + 2).coerceAtMost(BAR - 1)) }
                else -> out.take(1).map { it.copy(len = it.len + 1) } + out.drop(1)
            }.map { it.copy(len = minOf(it.len, BAR - it.step)) }
        }
        return out
    }

    /** The motif again, its last note lifted to the chord tone above (the rise before the cadence). */
    private fun lift(bar: List<Ev>, key: Key, b: Int): List<Ev> {
        if (bar.isEmpty()) return bar
        val last = bar.last()
        return bar.dropLast(1) + last.copy(index = key.above(last.index, key.chordTones(b)), slide = false)
    }

    /**
     * A cadence: the bar's first two beats, its last note held to the third beat on a tone of the
     * chord (the consequent on its root or third; the antecedent on any chord tone), then rest.
     */
    private fun cadence(bar: List<Ev>, key: Key, b: Int, consequent: Boolean): List<Ev> {
        val kept = bar.filter { it.step < 8 }.ifEmpty { listOf(Ev(0, 8, bar.firstOrNull()?.index ?: key.nearest(7 * 5, key.triad(b)))) }
        val last = kept.last()
        val tones = if (consequent) {
            val triad = key.harmony.voiced(key.degree(b), 0, Voicing.Triad).map { Math.floorMod(it, 12) }
            setOf(triad[0], triad[1])
        } else key.chordTones(b)
        val held = last.copy(len = (8 - last.step).coerceAtLeast(2), index = key.nearest(last.index, tones), slide = false)
        return kept.dropLast(1).map { it.copy(slide = false, len = minOf(it.len, held.step - it.step).coerceAtLeast(1)) } + held
    }

    /** Edits between a bar and the motif: on their intervals and on their durations. */
    private fun distance(bar: List<Ev>, motif: List<Ev>): Int {
        val a = bar.sortedBy { it.step }
        val m = motif.sortedBy { it.step }
        return MelodyMetrics.editDistance(a.zipWithNext { x, y -> y.index - x.index }, m.zipWithNext { x, y -> y.index - x.index }) +
            MelodyMetrics.editDistance(a.map { it.len }, m.map { it.len })
    }

    /**
     * Chord tones where they must be: on the strong beats; a note off the chord lasts an eighth at
     * most and steps into a chord tone of the same bar, else it is moved onto the nearest one; a
     * long note never sits a semitone over a chord tone. A riff keeps its notes off the beat.
     */
    private fun targetChords(line: List<Ev>, key: Key): List<Ev> {
        val out = line.toMutableList()
        for (i in out.indices) {
            val e = out[i]
            if (key.inChord(e.index, e.bar)) continue
            val next = out.getOrNull(i + 1)
            val passing = e.len <= 2 && next != null && next.bar == e.bar && key.inChord(next.index, next.bar) && abs(key.pitch(next.index) - key.pitch(e.index)) <= 2
            if (e.strong || !passing) {
                val previous = out.getOrNull(i - 1)?.index ?: e.index
                out[i] = e.copy(index = key.nearest(e.index, key.chordTones(e.bar), up = previous > e.index))
            }
        }
        return out
    }

    /**
     * At each chord change: the bar's first note keeps the previous pitch when the new chord has
     * it, else steps into the new chord when it would have leapt; the hook never merely
     * transposes its first bar (two bars of four at most).
     */
    private fun voiceLead(line: List<Ev>, key: Key): List<Ev> {
        val out = line.toMutableList()
        for (b in 1 until PHRASE_BARS) {
            if (key.chordTones(b) == key.chordTones(b - 1)) continue
            val i = out.indexOfFirst { it.bar == b }.takeIf { it >= 0 } ?: continue
            val before = out.lastOrNull { it.bar < b } ?: continue
            val first = out[i]
            val leap = abs(key.pitch(first.index) - key.pitch(before.index))
            if (leap <= 2 || before.slide) continue
            out[i] = when {
                key.inChord(before.index, b) -> first.copy(index = before.index)
                else -> first.copy(index = key.nearest(before.index, key.chordTones(b)))
            }
        }
        // Bars that only transpose the phrase's first bar: their last note changed.
        for (p in 0 until 2) {
            fun shape(b: Int) = out.filter { it.bar == b }.map { key.pitch(it.index) - Math.floorMod(key.harmony.degreeRoot(key.degree(b), 0), 12) }.sorted()
            val ref = shape(4 * p)
            if (ref.isEmpty()) continue
            var alike = (4 * p + 1 until 4 * p + 4).filter { shape(it) == ref }
            while (alike.size > 1) {
                val b = alike.last()
                val i = out.indexOfLast { it.bar == b }
                out[i] = out[i].copy(index = key.above(out[i].index, key.chordTones(b)))
                alike = (4 * p + 1 until 4 * p + 4).filter { shape(it) == ref }
            }
        }
        return out
    }

    /** At least one chord change crossed on a common tone: the first one whose new chord holds the pitch before it (never the [peak]). */
    private fun ensureCommonTone(line: List<Ev>, key: Key, peak: Ev?): List<Ev> {
        val out = line.sortedBy { it.step }.toMutableList()
        val changes = (1 until PHRASE_BARS).filter { key.chordTones(it) != key.chordTones(it - 1) }
        val common = changes.any { b ->
            val before = out.lastOrNull { it.bar < b } ?: return@any false
            val first = out.firstOrNull { it.bar == b } ?: return@any false
            first.index == before.index || before.end > b * BAR && key.inChord(before.index, b)
        }
        if (common) return out
        for (b in changes) {
            val i = out.indexOfFirst { it.bar == b }.takeIf { it >= 0 } ?: continue
            val before = out.lastOrNull { it.bar < b } ?: continue
            if (!key.inChord(before.index, b) || before.slide || before === peak || out[i] === peak) continue
            // Never at the price of a new leap to the note after.
            val after = out.getOrNull(i + 1)
            if (after != null && abs(key.pitch(after.index) - key.pitch(before.index)) > LEAP_SEMITONES) continue
            out[i] = out[i].copy(index = before.index)
            return out
        }
        // No such boundary: both notes of one move onto a tone the two chords share, close to where they were.
        for (b in changes) {
            val i = out.indexOfFirst { it.bar == b }.takeIf { it >= 0 } ?: continue
            val j = out.indexOfLast { it.bar < b }.takeIf { it >= 0 } ?: continue
            if (out[i] === peak || out[j] === peak || out[j].slide) continue
            val shared = key.chordTones(b) intersect key.chordTones(b - 1)
            if (shared.isEmpty()) continue
            val tone = key.nearest(out[j].index, shared)
            if (abs(key.pitch(tone) - key.pitch(out[j].index)) > 4) continue
            val after = out.getOrNull(i + 1)
            if (after != null && abs(key.pitch(after.index) - key.pitch(tone)) > LEAP_SEMITONES) continue
            out[j] = out[j].copy(index = tone)
            out[i] = out[i].copy(index = tone)
            return out
        }
        return out
    }

    /**
     * One peak: the highest note sounds twice at most, first in the second half on a strong beat.
     * A strong note of bars 5–7 is raised to the chord tone above the old top; the other notes at
     * the top come down a step onto a chord tone.
     */
    private fun singlePeak(line: List<Ev>, key: Key): List<Ev> {
        if (line.size < 2) return line
        val out = line.toMutableList()
        // Bars 6 and 7 first (bar 5 may restate the motif), then bar 5.
        var candidates = out.withIndex().filter { (_, e) -> e.bar in 5..6 && e.strong }.ifEmpty { out.withIndex().filter { (_, e) -> e.bar == 4 && e.strong } }
        if (candidates.isEmpty()) {
            // A line all off the beat: the note nearest a strong beat of bars 5–7 is pulled onto it (its rhythm develops there).
            val moved = out.withIndex().filter { (_, e) -> e.bar in 4..6 }.minByOrNull { (_, e) -> minOf(e.pos % 8, 8 - e.pos % 8) } ?: return out
            val e = moved.value
            val strongStep = e.bar * BAR + if (e.pos % 8 < 4) e.pos / 8 * 8 else (e.pos / 8 + 1) * 8
            val stepIn = strongStep.coerceIn(e.bar * BAR, e.bar * BAR + 8)
            val previous = out.filter { it.step < stepIn && it !== e }.maxByOrNull { it.step }
            val next = out.filter { it.step > stepIn && it !== e }.minByOrNull { it.step }
            if (previous != null && previous.end > stepIn || next != null && next.step < stepIn + 1) return out
            val len = minOf(e.len, (next?.step ?: (e.bar + 1) * BAR) - stepIn).coerceAtLeast(1)
            out[moved.index] = e.copy(step = stepIn, len = len)
            out.sortBy { it.step }
            candidates = out.withIndex().filter { (_, ev) -> ev.bar in 4..6 && ev.strong }
        }
        // The candidate whose neighbours sit closest to the line's top: the peak needs no wide leap to reach it.
        val lineTop = out.maxOf { key.pitch(it.index) }
        fun reach(i: Int): Int = listOfNotNull(out.getOrNull(i - 1), out.getOrNull(i + 1)).sumOf { lineTop - key.pitch(it.index) }
        val target = candidates.minWithOrNull(compareBy<IndexedValue<Ev>>({ reach(it.index) }, { -key.pitch(it.value.index) }, { -it.value.step })) ?: return out
        fun others() = out.withIndex().filter { it.index != target.index }
        fun top() = others().maxOf { key.pitch(it.value.index) }
        val low = out.minOf { key.pitch(it.index) }
        // The peak: a chord tone just above the chosen note (a fifth over the lowest at least), never a wide leap away;
        // every other note at or over it comes down a chord tone at a time.
        val tones = key.chordTones(target.value.bar)
        var peak = key.above(target.value.index, tones)
        while (key.pitch(peak) - low < 7) peak = key.above(peak, tones)
        out[target.index] = target.value.copy(index = peak)
        var guard = 0
        while (guard++ < 6 && top() >= key.pitch(peak)) {
            for ((i, e) in others()) if (key.pitch(e.index) >= key.pitch(peak)) out[i] = e.copy(index = key.below(e.index, key.chordTones(e.bar)))
        }
        return out
    }

    /**
     * A hook moves mostly by step: a leap over a fourth is answered by a step back within two
     * notes, and a phrase has one leap at most. An unanswered leap is closed by bringing its
     * second note onto the chord tone nearest the first (the note before the [peak] rises to it
     * instead, so the peak stays); a phrase's extra leaps are closed the same way.
     */
    private fun smoothLeaps(line: List<Ev>, key: Key, peak: Ev?): List<Ev> {
        val out = line.sortedBy { it.step }.toMutableList()
        var peakAt = out.indexOfFirst { it === peak }
        val peakPitch = if (peakAt >= 0) key.pitch(out[peakAt].index) else Int.MAX_VALUE
        fun pitch(i: Int) = key.pitch(out[i].index)
        fun resolved(i: Int): Boolean {
            val leap = pitch(i + 1) - pitch(i)
            return (i + 2..i + 3).any { j -> j < out.size && (pitch(j) - pitch(j - 1)).let { d -> d != 0 && abs(d) <= STEP_SEMITONES && (d > 0) != (leap > 0) } }
        }
        /** The chord tone of [bar] nearest [index], under the peak. */
        fun near(index: Int, bar: Int): Int {
            val tones = key.chordTones(bar)
            val n = key.nearest(index, tones)
            return if (peakAt >= 0 && key.pitch(n) >= peakPitch) key.below(out[peakAt].index, tones) else n
        }
        var guard = 0
        while (guard++ < 64) {
            val leaps = (0 until out.size - 1).filter { abs(pitch(it + 1) - pitch(it)) > LEAP_SEMITONES }
            val phrases = leaps.groupBy { out[it].step / (4 * BAR) }
            val bad = leaps.firstOrNull { i -> !resolved(i) && !(abs(pitch(i + 1) - pitch(i)) == 12 && i == out.size - 2) }
                ?: phrases.values.firstOrNull { it.size > 1 }?.let { it.firstOrNull { i -> peakAt < 0 || i + 1 != peakAt } ?: it.last() }
                ?: break
            // First choice: answer the leap with a step back, on a chord tone, within the next two notes.
            if (!resolved(bad)) {
                // Kept only when it leaves fewer unanswered leaps (answering one must not undo another's answer).
                fun unanswered() = (0 until out.size - 1).count { abs(pitch(it + 1) - pitch(it)) > LEAP_SEMITONES && !resolved(it) }
                val before = out.toList()
                val count = unanswered()
                if (answerLeap(out, bad, key, peakAt, peakPitch) && unanswered() < count) continue
                for (i in out.indices) out[i] = before[i]
            }
            when {
                // Into the peak: the peak stays, the note after it steps down (the answer to the leap).
                bad + 1 == peakAt -> {
                    val j = bad + 2
                    if (j >= out.size || resolved(bad)) {
                        // Nothing after the peak (or already answered): the leap is closed from before instead.
                        out[bad] = out[bad].copy(index = key.below(out[peakAt].index, key.chordTones(out[bad].bar)))
                    } else {
                        val tones = key.chordTones(out[j].bar)
                        val top = out[peakAt].index
                        val within = listOf(top - 1, top - 2).filter { peakPitch - key.pitch(it) in 1..STEP_SEMITONES }
                        val chordStep = within.firstOrNull { Math.floorMod(key.pitch(it), 12) in tones }
                        // A passing tone needs a chord tone a step away right after it, in the same bar.
                        val passing = within.firstOrNull()?.takeIf { p ->
                            val after = out.getOrNull(j + 1)
                            after != null && after.bar == out[j].bar && key.chordTones(after.bar).any { c ->
                                (key.index(key.pitch(p) - 2)..key.index(key.pitch(p) + 2)).any { Math.floorMod(key.pitch(it), 12) == c && abs(key.pitch(it) - key.pitch(p)) in 1..STEP_SEMITONES }
                            }
                        }
                        when {
                            chordStep != null -> out[j] = out[j].copy(index = chordStep)
                            passing != null -> {
                                out[j] = out[j].copy(index = passing, len = minOf(out[j].len, 2))
                                val after = out[j + 1]
                                val land = (passing - 2..passing + 2).filter { Math.floorMod(key.pitch(it), 12) in key.chordTones(after.bar) && abs(key.pitch(it) - key.pitch(passing)) in 1..STEP_SEMITONES }
                                    .minBy { abs(key.pitch(it) - key.pitch(passing)) }
                                out[j + 1] = after.copy(index = land)
                            }
                            // No step down from the peak: the leap is closed from before.
                            else -> out[bad] = out[bad].copy(index = key.below(top, key.chordTones(out[bad].bar)))
                        }
                    }
                }
                // Out of the peak: the next note lands on the chord tone just under it.
                bad == peakAt -> out[bad + 1] = out[bad + 1].copy(index = key.below(out[peakAt].index, key.chordTones(out[bad + 1].bar)))
                else -> out[bad + 1] = out[bad + 1].copy(index = near(out[bad].index, out[bad + 1].bar))
            }
        }
        // Still a leap left unanswered or doubled (the fixes above went round in circles): the notes after the peak
        // follow their predecessor onto its nearest chord tone, the notes before it climb to theirs.
        fun bad(i: Int) = abs(pitch(i + 1) - pitch(i)) > LEAP_SEMITONES
        fun anyBad() = (0 until out.size - 1).any { i -> bad(i) && !resolved(i) } ||
            (0 until out.size - 1).filter { bad(it) }.groupBy { out[it].step / (4 * BAR) }.values.any { it.size > 1 }
        if (anyBad()) {
            val from = if (peakAt >= 0) peakAt else 0
            for (i in from until out.size - 1) if (bad(i)) out[i + 1] = out[i + 1].copy(index = near(out[i].index, out[i + 1].bar))
            if (peakAt > 0) for (i in peakAt - 1 downTo 0) if (bad(i)) out[i] = out[i].copy(index = near(out[i + 1].index, out[i].bar))
        }
        return out
    }

    /**
     * Answers the leap from note [i] to note i+1 with a step (1 or 2 semitones) the other way: note
     * i+2, or else note i+3, moves onto the chord tone of its bar a step from the note before it.
     * Never the peak, never above it. False when no such note exists.
     */
    private fun answerLeap(out: MutableList<Ev>, i: Int, key: Key, peakAt: Int, peakPitch: Int): Boolean {
        val up = key.pitch(out[i + 1].index) > key.pitch(out[i].index)
        for (k in i + 2..i + 3) {
            if (k >= out.size || k == peakAt) return false
            val from = key.pitch(out[k - 1].index)
            val tones = key.chordTones(out[k].bar)
            val candidates = (out[k - 1].index - 2..out[k - 1].index + 2).filter { idx ->
                val d = key.pitch(idx) - from
                d != 0 && abs(d) <= STEP_SEMITONES && (d < 0) == up && Math.floorMod(key.pitch(idx), 12) in tones && key.pitch(idx) < peakPitch
            }
            val pick = candidates.minByOrNull { abs(key.pitch(it) - from) } ?: continue
            // Never at the price of a leap into the note after it.
            val next = out.getOrNull(k + 1)
            if (next != null && abs(key.pitch(next.index) - key.pitch(pick)) > LEAP_SEMITONES && k + 1 != peakAt) {
                if (k == i + 3) return false else continue
            }
            out[k] = out[k].copy(index = pick)
            return true
        }
        return false
    }

    private const val LEAP_SEMITONES = 5
    private const val STEP_SEMITONES = 2

    /** The hook within an octave and a fifth, and at least a fifth wide: the lowest notes rise an octave, a flat line opens. */
    private fun inAmbitus(line: List<Ev>, key: Key): List<Ev> {
        var out = line
        var guard = 0
        while (guard++ < 4) {
            val pitches = out.map { key.pitch(it.index) }
            val top = pitches.max()
            val low = pitches.min()
            if (top - low <= 17) break
            out = out.map { if (key.pitch(it.index) < top - 17 && key.pitch(it.index) + 12 < top) it.copy(index = it.index + 7) else it }
        }
        return out
    }

    /**
     * Each four bars end in rest: no note starts in the last two beats of bars 4 and 8, notes
     * crossing into them stop there (a riff keeps a beat of rest at the end of bar 4: the fill).
     */
    private fun restAtPhraseEnds(line: List<Ev>, key: Key, kind: LeadKind): List<Ev> {
        val out = ArrayList<Ev>()
        for (e in line) {
            val endBar = e.bar == 3 || e.bar == 7
            val cut = if (endBar) 8 else if (e.bar % 2 == 1 && kind != LeadKind.Hook) 15 else BAR
            if (e.pos >= cut) continue
            out += if (e.end > e.bar * BAR + cut) e.copy(len = e.bar * BAR + cut - e.step, slide = false) else e
        }
        // A breath of a beat in every four bars, whatever the cell: bar 2 and bar 6 lose their last beat when nothing else rests.
        for (p in 0 until 2) {
            val phrase = out.filter { it.bar / 4 == p }.sortedBy { it.step }
            val after = out.filter { it.bar / 4 > p }.minOfOrNull { it.step } ?: (STEPS + (out.minOfOrNull { it.step } ?: 0))
            val breathes = phrase.isEmpty() || phrase.zipWithNext().any { (a, b) -> b.step - a.end >= 4 } || phrase.first().pos >= 4 || after - phrase.last().end >= 4
            if (breathes) continue
            val b = 4 * p + 1
            out.removeAll { it.bar == b && it.pos >= 12 }
            for (i in out.indices) if (out[i].bar == b && out[i].end > b * BAR + 12) out[i] = out[i].copy(len = b * BAR + 12 - out[i].step)
        }
        return out.sortedBy { it.step }
    }

    /**
     * The motif of bar 1 comes back three times in eight bars at most (six in sixteen) and twice
     * at least, and no two consecutive bars are the same: extra repeats are answered, a missing
     * one restated.
     */
    private fun ensureMotif(line: List<Ev>, key: Key, kind: LeadKind): List<Ev> {
        var out = line
        fun bars() = (0 until PHRASE_BARS).map { b -> out.filter { it.bar == b } }
        val motif = bars()[0].map { it.copy(step = it.pos) }
        if (motif.isEmpty()) return out
        fun matches() = bars().withIndex().filter { (_, bar) -> bar.isNotEmpty() && distance(bar.map { it.copy(step = it.pos) }, motif) <= 2 }.map { it.index }
        fun replace(b: Int, evs: List<Ev>) {
            out = (out.filter { it.bar != b } + targetChords(evs.map { it.copy(step = it.step + b * BAR) }, key)).sortedBy { it.step }
        }
        val most = if (kind == LeadKind.Hook) 3 else 5
        var guard = 0
        while (matches().size > most && guard++ < 6) {
            val b = matches().filter { it != 0 && it != 1 && it != 6 }.lastOrNull() ?: matches().last()
            replace(b, answer(bars()[b].map { it.copy(step = it.pos) }, motif, key, b))
        }
        // Too few: bar 5 (the same chord as bar 1) restates the motif as it is.
        if (matches().size < 2) replace(4, motif)
        // No two consecutive bars alike.
        for (b in 1 until PHRASE_BARS) {
            val sigs = bars().map { bar -> bar.map { Triple(it.pos, it.len, it.index) } }
            if (sigs[b].isEmpty() || sigs[b] != sigs[b - 1]) continue
            replace(b, eola(bars()[b].map { it.copy(step = it.pos) }, key, b, key.chordTones(b + 1)))
        }
        return out.sortedBy { it.step }
    }

    /**
     * Velocities as a player's: the position in the beat (on it, the « and », the sixteenths), the
     * beat in the bar (1 > 3 > 2 and 4), the bar in the phrase (a rise towards the end), lower
     * case softer, the first note of the clip at full strength. Then notes.
     */
    private fun withVelocities(line: List<Ev>, key: Key, base: Float, lead: Boolean): List<Note> {
        val total = STEPS * T
        val sorted = line.sortedBy { it.step }
        val notes = sorted.mapIndexed { i, e ->
            val pos = when (e.pos % 4) { 0 -> 1f; 2 -> 0.82f; else -> 0.68f }
            val beat = when ((e.pos / 4) % 4) { 0 -> 1f; 2 -> 0.9f; else -> 0.82f }
            val rise = if (lead) RISE[e.bar % RISE.size] else listOf(1f, 0.94f, 0.97f, 0.9f)[e.bar % 4]
            val v = if (i == 0) 1f else (base * pos * beat * rise * (if (e.soft) SOFT else 1f)).coerceIn(0.05f, 1f)
            val length = e.len * T + if (e.slide) StarterWizard.SLIDE_OVERLAP else 0
            Note(e.step * T, length.coerceAtMost(total - e.step * T), key.pitch(e.index), v)
        }
        return if (lead) shaped(notes) else notes
    }

    /**
     * The lead's dynamics made sure of, whatever its rhythm: the strong beats louder than the rest
     * by [STRONG_OVER_WEAK] on average (the weak notes come down), and the last two bars louder
     * than the first two by [CRESCENDO] (they go up, or the first bars come down when they cannot).
     */
    private fun shaped(notes: List<Note>): List<Note> {
        if (notes.size < 4) return notes
        var out = notes.toMutableList()
        fun strong(n: Note) = (n.start / T) % 8 == 0
        fun mean(list: List<Note>) = if (list.isEmpty()) 0f else list.map { it.velocity }.average().toFloat()
        val rest = out.drop(1)
        val strongNotes = rest.filter { strong(it) }
        val weakNotes = rest.filter { !strong(it) }
        if (strongNotes.size >= 3 && weakNotes.size >= 3) {
            val gap = mean(strongNotes) - mean(weakNotes)
            if (gap < STRONG_OVER_WEAK) {
                val factor = ((mean(strongNotes) - STRONG_OVER_WEAK) / mean(weakNotes)).coerceIn(0.5f, 1f)
                out = out.mapIndexed { i, n -> if (i > 0 && !strong(n)) n.copy(velocity = (n.velocity * factor).coerceAtLeast(0.05f)) else n }.toMutableList()
            }
        }
        val bars = STEPS / BAR
        val head = out.drop(1).filter { n -> n.start / (BAR * T) < 2 }
        val tail = out.filter { n -> n.start / (BAR * T) >= bars - 2 }
        if (head.isNotEmpty() && tail.isNotEmpty() && mean(tail) < mean(head) + CRESCENDO) {
            val up = ((mean(head) + CRESCENDO) / mean(tail)).coerceAtLeast(1f)
            val canRise = tail.all { it.velocity * up <= 1f }
            out = out.mapIndexed { i, n ->
                when {
                    canRise && n.start / (BAR * T) >= bars - 2 -> n.copy(velocity = (n.velocity * up).coerceAtMost(1f))
                    !canRise && i > 0 && n.start / (BAR * T) < 2 -> n.copy(velocity = (n.velocity * (mean(tail) - CRESCENDO) / mean(head)).coerceAtLeast(0.05f))
                    else -> n
                }
            }.toMutableList()
        }
        return out
    }

    /** How much louder the strong beats are, on average, and how much louder the last two bars than the first two. */
    private const val STRONG_OVER_WEAK = 0.1f
    private const val CRESCENDO = 0.07f

    /**
     * A few ticks off the grid, the same for the same position in the bar (a profile, not noise),
     * never on a bar's first beat, never on a slide; a note pulled earlier shortens the one before
     * it so that the line stays a line.
     */
    fun humanize(notes: List<Note>, seed: Int): List<Note> {
        // Always a touch late (a player leans back; a note never starts inside the one before it).
        val profile = IntArray(BAR) { pos -> if (pos == 0) 0 else if (abs(Seeded.unit(seed, pos)) < 0.5f) 1 else HUMANIZE_TICKS }
        val sorted = notes.sortedBy { it.start }
        val out = sorted.toMutableList()
        // A position where any note slides (or follows a slide) stays on the grid for all its notes: one shift per position.
        val frozen = sorted.withIndex().filter { (i, n) -> n.length % T != 0 || i > 0 && sorted[i - 1].length % T != 0 }.map { (it.value.start / T) % BAR }.toSet()
        for (i in out.indices) {
            val n = out[i]
            if (n.start % T != 0) continue
            val pos = (n.start / T) % BAR
            if (pos in frozen) continue
            val offset = profile[pos]
            if (offset == 0) continue
            val start = (n.start + offset).coerceAtLeast(0)
            out[i] = n.copy(start = start, length = (n.end - start).coerceAtLeast(T / 2))
            if (i > 0 && out[i - 1].end > start && out[i - 1].pitch != n.pitch || i > 0 && out[i - 1].end > start) {
                val p = out[i - 1]
                out[i - 1] = p.copy(length = (start - p.start).coerceAtLeast(T / 2))
            }
        }
        return out
    }

    // ---- Versions ---------------------------------------------------------------------------

    /** The Intro's preview: the first phrase of the hook, then four bars of rest. */
    fun intro(chorus: NoteClip): NoteClip = chorus.copy(notes = chorus.notes.filter { MelodyMetrics.grid(it.start) < 4 * BAR * T }).sanitized()

    /**
     * The Couplet's line: at most 60 % of the hook's notes, the ones that carry it (a bar's first
     * beat, the strong beats, the long notes) kept, the rest left for the Refrain.
     */
    fun verse(chorus: NoteClip): NoteClip {
        val n = chorus.notes.size
        if (n == 0) return chorus
        val keep = (n * 3 / 5).coerceAtLeast(1)
        val ranked = chorus.notes.sortedWith(
            compareByDescending<Note> { MelodyMetrics.grid(it.start) % (BAR * T) == 0 }
                .thenByDescending { MelodyMetrics.grid(it.start) % (8 * T) == 0 }
                .thenByDescending { it.length }
                .thenBy { it.start },
        ).take(keep).toSet()
        return chorus.copy(notes = chorus.notes.filter { it in ranked }).sanitized()
    }

    /**
     * The second Refrain: the hook doubled an octave down, softer (a lead that glides must stay a
     * single line: it goes up an octave instead, or down when that would squeal).
     */
    fun secondChorus(chorus: NoteClip, mono: Boolean): NoteClip {
        if (chorus.notes.isEmpty()) return chorus
        val top = chorus.notes.maxOf { it.pitch }
        val low = chorus.notes.minOf { it.pitch }
        val notes = if (mono) {
            val shift = if (top + 12 <= StarterWizard.LEAD_TOP) 12 else if (low - 12 >= 36) -12 else 0
            chorus.notes.map { it.copy(pitch = it.pitch + shift) }
        } else {
            val shift = if (low - 12 >= 36) -12 else 12
            chorus.notes + chorus.notes.map { it.copy(pitch = it.pitch + shift, velocity = (it.velocity * 0.75f).coerceAtLeast(0.05f)) }
        }
        return chorus.copy(notes = notes).sanitized()
    }

    // ---- The bass ---------------------------------------------------------------------------

    /**
     * The bass over eight bars: the recipe's line on bars 1–4, then [StarterWizard.Melody.bassVariation]
     * or the line again with an approach note at the end of bar 7; a beat of rest at the end of
     * bar 4 (the fill) and two beats at the end of bar 8 (the turn); velocities that breathe.
     */
    fun bass(style: Style, mood: Mood, harmony: Harmony): NoteClip {
        val m = style.melody
        val key = keyOf(style, mood, harmony)
        val octaveRoot = 12 * (m.bassOctave + 1)
        val degrees8 = List(PHRASE_BARS) { key.degree(it) }
        val first = NoteClip().withLine(degrees8.take(4), harmony, m.bassPattern, octaveRoot, velocity = 1f, bass = true)
        val second = NoteClip().withLine(degrees8.take(4), harmony, m.bassVariation.ifBlank { m.bassPattern }, octaveRoot, velocity = 1f, bass = true)
        fun events(clip: NoteClip, offset: Int): List<Ev> = clip.notes.map { n ->
            val slide = n.length % T != 0
            Ev(n.start / T + offset, ((n.length - if (slide) StarterWizard.SLIDE_OVERLAP else 0) / T).coerceAtLeast(1), key.index(n.pitch), soft = n.velocity < 0.99f, slide = slide)
        }
        var line = events(first, 0) + events(second, 4 * BAR)
        if (m.bassVariation.isBlank()) {
            // Bars 4 and 7 end on an approach note: the fifth below the root (or above when that would be inaudible).
            for (b in listOf(3, 6)) {
                val bar = line.filter { it.bar == b && it.pos < (if (b == 3) 12 else BAR) }
                if (bar.size < 2) continue
                val last = bar.last()
                // The bar keeps its chord's root somewhere else, or it keeps this note.
                val chordRoot = Math.floorMod(key.harmony.degreeRoot(key.degree(b), 0), 12)
                if (bar.dropLast(1).none { Math.floorMod(key.pitch(it.index), 12) == chordRoot }) continue
                val root = key.pitch(last.index)
                // The fifth below from a root high enough to keep it over F1, else the fifth above.
                val approach = if (root - 5 >= LOWEST_BASS + 6) key.index(root - 5) else key.index(root + 7)
                line = line.map { if (it === last) it.copy(index = approach, slide = false) else it }
            }
        }
        // The rests: the last beat of bar 4, the last two beats of bar 8; a slide into a note cut away slides no more.
        val out = ArrayList<Ev>()
        for (e in line) {
            val cut = when (e.bar) { 3 -> 12; 7 -> 8; else -> BAR }
            if (e.pos >= cut) continue
            out += if (e.end > e.bar * BAR + cut) e.copy(len = e.bar * BAR + cut - e.step, slide = false) else e
        }
        for (i in out.indices) if (out[i].slide && out.none { it.step == out[i].end }) out[i] = out[i].copy(slide = false)
        val notes = withVelocities(out, key, BASS_VELOCITY, lead = false)
        return NoteClip(steps = STEPS, notes = notes).sanitized()
    }

    // ---- The counter-melody -----------------------------------------------------------------

    /**
     * The counter-melody: the lead's motif (its first bar's rhythm, up to three notes) answered
     * in each rest of two beats or more of [lead], on the chord tones an octave below the lead's
     * middle, never at the same time as the lead.
     */
    fun counter(lead: NoteClip, key: Key, seed: Int): NoteClip {
        if (lead.notes.isEmpty()) return NoteClip(steps = lead.steps)
        val starts = lead.notes.map { MelodyMetrics.grid(it.start) }.distinct().sorted()
        val ends = starts.map { s -> lead.notes.filter { MelodyMetrics.grid(it.start) == s }.maxOf { MelodyMetrics.grid(it.start) + MelodyMetrics.grid(it.length).coerceAtLeast(T) } }
        val ticks = lead.steps * T
        val median = lead.notes.map { it.pitch }.sorted().let { it[it.size / 2] }
        val centre = key.index(median - 12)
        val motif = lead.notes.filter { MelodyMetrics.grid(it.start) < BAR * T }.sortedBy { it.start }.take(3)
        val rhythm = motif.map { MelodyMetrics.grid(it.length) / T }.ifEmpty { listOf(2) }
        val out = ArrayList<Ev>()
        for (i in starts.indices) {
            val gapStart = ends[i]
            val gapEnd = starts.getOrNull(i + 1) ?: ticks
            if (gapEnd - gapStart < 2 * 4 * T) continue
            var step = (gapStart / T + 1) / 2 * 2 + 2
            val endStep = gapEnd / T - 1
            var index = key.nearest(centre, key.chordTones(step / BAR))
            var k = 0
            while (k < 3 && step + 1 <= endStep) {
                val len = minOf(rhythm[k % rhythm.size].coerceIn(1, 4), endStep - step)
                out += Ev(step, len, index, soft = k > 0)
                index = if (k % 2 == 0) key.above(index, key.chordTones(step / BAR)) else key.below(index, key.chordTones(step / BAR))
                step += len + 1
                k++
            }
        }
        val notes = withVelocities(out, key, COUNTER_VELOCITY, lead = true)
        return NoteClip(steps = lead.steps, notes = humanize(notes, seed)).sanitized()
    }
}
