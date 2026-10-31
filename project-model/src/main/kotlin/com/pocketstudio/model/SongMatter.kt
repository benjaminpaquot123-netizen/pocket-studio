package com.pocketstudio.model

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Step 5d (« Faire de la matière »): the song's stems turned into material of the project. Pure
 * signal work on mono floats, off the main thread: the drums' hits found and told apart (kick,
 * snare, hat), the bass line followed note by note.
 */
object SongMatter {
    enum class Drum { Kick, Snare, Hat }

    /** A hit of the drums stem: where (frames), what, and how cleanly it stands alone (higher: better as a one-shot). */
    data class Hit(val at: Int, val kind: Drum, val score: Double, val low: Double, val high: Double)

    private const val HOP = 256

    /**
     * The hits of [mono] at [rate]: an onset where the loudness (every 256 frames) jumps well
     * above the 40 ms before it; each told apart by where its first 60 ms hold their energy (lows
     * under 150 Hz: kick; highs over 5 kHz: hat; else snare).
     */
    fun drumHits(mono: FloatArray, rate: Int): List<Hit> {
        val n = mono.size / HOP
        if (n < 16 || rate <= 0) return emptyList()
        val env = DoubleArray(n) { k -> var s = 0.0; for (i in k * HOP until (k + 1) * HOP) s += mono[i].toDouble() * mono[i]; sqrt(s / HOP) }
        val top = env.maxOrNull() ?: 0.0
        if (top < 1e-4) return emptyList()
        val gap = (0.05 * rate / HOP).roundToInt().coerceAtLeast(1)
        val out = mutableListOf<Hit>()
        var last = -gap
        for (k in 8 until n) {
            var before = 0.0
            for (j in k - 8 until k) before += env[j]
            before /= 8
            if (env[k] > 0.08 * top && env[k] > 2.5 * before + 0.01 * top && env[k] >= env[k - 1] && k - last >= gap) {
                // The attack's frame within the hop: back to the first frame of the jump.
                val at = (k - 1) * HOP
                val (low, high) = bands(mono, at, (0.06 * rate).toInt(), rate)
                val kind = when {
                    low > 0.5 -> Drum.Kick
                    // Hats: more highs than lows (a kick still ringing under them keeps some lows).
                    high > 0.25 && high > low -> Drum.Hat
                    else -> Drum.Snare
                }
                out.add(Hit(at, kind, env[k] / (before + 0.02 * top), low, high))
                last = k
            }
        }
        return out
    }

    /** Shares of energy under 150 Hz and over 5 kHz in [count] frames from [from]. */
    private fun bands(x: FloatArray, from: Int, count: Int, rate: Int): Pair<Double, Double> {
        val aLow = exp(-2 * Math.PI * 150.0 / rate)
        val aHigh = exp(-2 * Math.PI * 5000.0 / rate)
        var lp = 0.0
        var lp5 = 0.0
        var eLow = 0.0
        var eHigh = 0.0
        var eAll = 0.0
        for (i in from until minOf(x.size, from + count)) {
            val v = x[i].toDouble()
            lp = (1 - aLow) * v + aLow * lp
            lp5 = (1 - aHigh) * v + aHigh * lp5
            val hp = v - lp5
            eLow += lp * lp
            eHigh += hp * hp
            eAll += v * v
        }
        if (eAll <= 0) return 0.0 to 0.0
        return eLow / eAll to eHigh / eAll
    }

    /** The cleanest [perKind] hits of each kind (loud against what comes before), in time order within each kind. */
    fun bestHits(hits: List<Hit>, perKind: Int = 2): Map<Drum, List<Hit>> =
        hits.groupBy { it.kind }.mapValues { (_, h) -> h.sortedByDescending { it.score }.take(perKind) }

    /** Frames a one-shot of [kind] lasts at most, at [rate]. */
    fun oneShotFrames(kind: Drum, rate: Int): Int = when (kind) {
        Drum.Kick -> rate * 4 / 10
        Drum.Snare -> rate * 3 / 10
        Drum.Hat -> rate * 15 / 100
    }

    /**
     * The bass line of [mono] at [rate] as notes on the project's sixteenths: [steps] steps of
     * [stepFrames] frames from frame 0 (the song's bar to start on). Monophonic (one pitch at a
     * time, the YIN method on a copy at 4 kHz), each step's pitch the most frequent of its frames,
     * a step too quiet a rest; equal steps in a row one note (a new attack starts another). MIDI 24..64 (the bass's range).
     */
    fun bassNotes(mono: FloatArray, rate: Int, stepFrames: Double, steps: Int): List<Note> {
        if (rate <= 0 || stepFrames <= 0 || steps <= 0) return emptyList()
        val factor = (rate / 4000).coerceAtLeast(1)
        val low = FloatArray(mono.size / factor) { k -> var s = 0f; for (i in k * factor until (k + 1) * factor) s += mono[i]; s / factor }
        val r = rate.toDouble() / factor
        val window = (0.06 * r).toInt()          // 60 ms: two periods of 33 Hz
        val maxLag = (r / 30.0).toInt()
        val minLag = (r / 400.0).toInt().coerceAtLeast(2)
        val top = low.maxOfOrNull { abs(it) } ?: 0f
        if (top < 1e-4f) return emptyList()
        val pitches = IntArray(steps) { -1 }
        // Each step's loudness at its start: a new attack on the same pitch is a new note.
        val attack = DoubleArray(steps)
        val diff = DoubleArray(maxLag + 1)
        for (s in 0 until steps) {
            val votes = HashMap<Int, Int>()
            for (q in 0 until 3) {
                val start = ((s + q / 3.0) * stepFrames / factor).toInt()
                if (start + window + maxLag >= low.size) break
                var energy = 0.0
                for (i in start until start + window) energy += low[i].toDouble() * low[i]
                if (q == 0) attack[s] = sqrt(energy / window)
                if (sqrt(energy / window) < 0.04 * top) continue
                // YIN: the difference function, normalised, its first dip under the threshold.
                for (lag in 1..maxLag) {
                    var d = 0.0
                    for (i in start until start + window) { val e = low[i] - low[i + lag]; d += e * e }
                    diff[lag] = d
                }
                var sum = 0.0
                var found = -1
                for (lag in 1..maxLag) {
                    sum += diff[lag]
                    val norm = if (sum > 0) diff[lag] * lag / sum else 1.0
                    if (lag >= minLag && norm < 0.2) {
                        var l = lag
                        while (l + 1 <= maxLag && diff[l + 1] < diff[l]) l++
                        found = l
                        break
                    }
                }
                if (found <= 0) continue
                val hz = r / found
                val midi = (69 + 12 * ln(hz / 440.0) / ln(2.0)).roundToInt()
                if (midi in 24..64) votes.merge(midi, 1, Int::plus)
            }
            pitches[s] = votes.maxByOrNull { it.value }?.key ?: -1
        }
        val notes = mutableListOf<Note>()
        var s = 0
        while (s < steps) {
            val p = pitches[s]
            if (p < 0) { s++; continue }
            var e = s + 1
            while (e < steps && pitches[e] == p && attack[e] < 1.4 * attack[e - 1]) e++
            notes.add(Note(start = s * NoteClip.TICKS_PER_STEP, length = (e - s) * NoteClip.TICKS_PER_STEP, pitch = p))
            s = e
        }
        return notes
    }

    /**
     * [notes] (one at a time, on sixteenths) as the Acid 303's steps over its [root]: a note's
     * first step played, the next ones tied (slide); a pitch out of the 303's four octaves folded in.
     * [length] steps (at most [AcidPattern.MAX_STEPS]).
     */
    fun acidSteps(notes: List<Note>, length: Int, root: Int): List<AcidStep> {
        val steps = MutableList(AcidPattern.MAX_STEPS) { AcidStep() }
        val t = NoteClip.TICKS_PER_STEP
        for (n in notes) {
            var semis = n.pitch - root
            while (semis < -12) semis += 12
            while (semis > 35) semis -= 12
            val octave = Math.floorDiv(semis, 12)
            val note = Math.floorMod(semis, 12)
            val first = n.start / t
            val last = (n.end - 1) / t
            for (s in first..last) {
                if (s !in 0 until minOf(length, AcidPattern.MAX_STEPS)) continue
                steps[s] = AcidStep(note = note, octave = octave, gate = true, slide = s < last)
            }
        }
        return steps
    }
}
