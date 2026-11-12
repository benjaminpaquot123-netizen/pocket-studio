package com.pocketstudio.app.human

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * « Ma chanson préférée », the song the « testeur humain » bench's file picker offers for the Remix
 * (owner, 2026-10-28: two testers take the voice of a song into their jump up). Made here, nothing
 * protected: 40 s at 120 BPM in A minor (Am F C G), a synthetic kit (kick, snare, hats), a bass, a
 * sung voice (phrases of 2 to 4 s with silences between them, a small melody on vowels made of
 * three formants, a light vibrato) and a pad. The bench's separator ([BenchSeparation]) gives back
 * each [Part] alone, as a perfect separation would: the « Voix » stem is the voice, clean.
 */
internal object BenchSong {
    const val FILE_NAME = "Ma chanson préférée.wav"
    const val SECONDS = 40
    const val BPM = 120.0
    /** A minor (La mineur), as the separator's analysis says: root 9, minor. */
    const val ROOT = 9

    /** The four stems, in [com.pocketstudio.separation.Demucs.STEMS] order. */
    enum class Part { Drums, Bass, Other, Vocals }

    private const val BEAT = 60.0 / BPM
    private const val BAR = 4 * BEAT

    /** Chords, one a bar: Am, F, C, G (bass roots and the pad's triads, as MIDI notes). */
    private val roots = intArrayOf(33, 29, 36, 31)
    private val triads = arrayOf(intArrayOf(57, 60, 64), intArrayOf(53, 57, 60), intArrayOf(55, 60, 64), intArrayOf(55, 59, 62))

    /** A sung note: from [beat], [beats] long, [midi], on [vowel]. */
    private class Note(val beat: Double, val beats: Double, val midi: Int, val vowel: Char)

    /** Six phrases (2 to 4 s), 2 s of silence or more between them; the voice comes in on bar 3. */
    private val melody: List<Note> = run {
        fun phrase(at: Double, vararg notes: Pair<Double, Pair<Int, Char>>): List<Note> {
            var t = at
            return notes.map { (len, nv) -> Note(t, len, nv.first, nv.second).also { t += len } }
        }
        val a = arrayOf(1.0 to (69 to 'a'), 1.0 to (72 to 'o'), 0.5 to (71 to 'e'), 1.5 to (69 to 'a'), 2.0 to (64 to 'u'))
        val b = arrayOf(1.0 to (64 to 'i'), 1.0 to (67 to 'a'), 1.0 to (69 to 'a'), 1.0 to (72 to 'o'))
        val c = arrayOf(1.0 to (74 to 'e'), 1.0 to (72 to 'a'), 1.0 to (71 to 'o'), 1.0 to (69 to 'a'), 1.0 to (67 to 'u'), 3.0 to (69 to 'a'))
        phrase(8.0, *a) + phrase(18.0, *b) + phrase(26.0, *c) + phrase(40.0, *a) + phrase(50.0, *b) + phrase(60.0, *c)
    }

    /** Where the voice sings, in seconds (start, end): for the tests. */
    val phrases: List<Pair<Double, Double>> = listOf(8.0 to 14.0, 18.0 to 22.0, 26.0 to 34.0, 40.0 to 46.0, 50.0 to 54.0, 60.0 to 68.0)
        .map { (a, b) -> a * BEAT to b * BEAT }

    fun frames(rate: Int): Int = SECONDS * rate

    /** One part alone at [rate], stereo interleaved, at the song's level (the mix peaks at −1 dBFS). */
    fun part(part: Part, rate: Int): FloatArray {
        val out = FloatArray(2 * frames(rate))
        when (part) {
            Part.Drums -> drums(out, rate)
            Part.Bass -> bass(out, rate)
            Part.Other -> pad(out, rate)
            Part.Vocals -> voice(out, rate)
        }
        val g = gain
        val fade = 2 * rate
        val n = frames(rate)
        for (i in 0 until n) {
            val f = if (i > n - fade) (n - i).toFloat() / fade else 1f
            out[2 * i] *= g * f
            out[2 * i + 1] *= g * f
        }
        return out
    }

    /** The whole song at [rate]: the four parts added. */
    fun mix(rate: Int): FloatArray {
        val out = part(Part.Drums, rate)
        for (p in listOf(Part.Bass, Part.Other, Part.Vocals)) {
            val x = part(p, rate)
            for (i in out.indices) out[i] += x[i]
        }
        return out
    }

    /** The level that puts the mix's peak at −1 dBFS (worked out once, at 48 kHz). */
    private val gain: Float by lazy {
        val rate = 48_000
        val sum = FloatArray(2 * frames(rate))
        val tmp = FloatArray(sum.size)
        for (p in Part.entries) {
            tmp.fill(0f)
            when (p) {
                Part.Drums -> drums(tmp, rate)
                Part.Bass -> bass(tmp, rate)
                Part.Other -> pad(tmp, rate)
                Part.Vocals -> voice(tmp, rate)
            }
            for (i in sum.indices) sum[i] += tmp[i]
        }
        val peak = sum.maxOf { abs(it) }.coerceAtLeast(1e-6f)
        0.89f / peak
    }

    /** The song as a phone keeps it: a 16-bit WAV, 48 kHz, stereo. */
    fun writeWav(file: File) {
        val rate = 48_000
        val data = mix(rate)
        val bytes = ByteBuffer.allocate(44 + data.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        bytes.put("RIFF".toByteArray()).putInt(36 + data.size * 2).put("WAVE".toByteArray())
        bytes.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(2).putInt(rate).putInt(rate * 4).putShort(4).putShort(16)
        bytes.put("data".toByteArray()).putInt(data.size * 2)
        for (v in data) bytes.putShort((v.coerceIn(-1f, 1f) * 32767f).roundToInt().toShort())
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeBytes(bytes.array())
        check(tmp.renameTo(file)) { "cannot write $file" }
    }

    // --- The parts --------------------------------------------------------------------------

    private fun hz(midi: Double) = 440.0 * 2.0.pow((midi - 69) / 12)

    /** A small deterministic noise (the same hit sounds the same at any rate). */
    private class Noise(seed: Int) {
        private var s = seed * 1103515245 + 12345
        fun next(): Float {
            s = s * 1664525 + 1013904223
            return (s ushr 8) / 8388608f - 1f
        }
    }

    private fun add(out: FloatArray, frame: Int, l: Float, r: Float) {
        if (frame < 0 || 2 * frame + 1 >= out.size) return
        out[2 * frame] += l
        out[2 * frame + 1] += r
    }

    private fun drums(out: FloatArray, rate: Int) {
        val bars = (SECONDS / BAR).toInt()
        var hit = 0
        for (bar in 0 until bars) {
            val t0 = bar * BAR
            // Kick on 1 and 3 (and the « and » of 3 every other bar), snare on 2 and 4, hats on the eighths.
            val kicks = if (bar % 2 == 1) listOf(0.0, 2.0, 2.5) else listOf(0.0, 2.0)
            for (k in kicks) kick(out, rate, t0 + k * BEAT)
            for (s in listOf(1.0, 3.0)) snare(out, rate, t0 + s * BEAT, hit++)
            for (h in 0 until 8) hat(out, rate, t0 + h * BEAT / 2, open = h % 2 == 1, seed = hit++)
        }
    }

    private fun kick(out: FloatArray, rate: Int, at: Double) {
        val start = (at * rate).toInt()
        val n = (0.35 * rate).toInt()
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / rate
            val f = 50 + 110 * exp(-t * 30)
            phase += 2 * PI * f / rate
            val v = (0.85 * sin(phase) * exp(-t * 9)).toFloat()
            add(out, start + i, v, v)
        }
    }

    private fun snare(out: FloatArray, rate: Int, at: Double, seed: Int) {
        val start = (at * rate).toInt()
        val n = (0.22 * rate).toInt()
        val noise = Noise(seed)
        for (i in 0 until n) {
            val t = i.toDouble() / rate
            val body = 0.25 * sin(2 * PI * 185 * t) * exp(-t * 25)
            val v = (body + 0.35 * noise.next() * exp(-t * 18)).toFloat()
            add(out, start + i, v * 0.95f, v)
        }
    }

    private fun hat(out: FloatArray, rate: Int, at: Double, open: Boolean, seed: Int) {
        val start = (at * rate).toInt()
        val n = ((if (open) 0.12 else 0.05) * rate).toInt()
        val noise = Noise(seed + 7919)
        var last = 0f
        for (i in 0 until n) {
            val t = i.toDouble() / rate
            val x = noise.next()
            val bright = x - last  // a first difference: only the highs
            last = x
            val v = (0.18 * bright * exp(-t * (if (open) 30 else 70))).toFloat()
            add(out, start + i, v * 0.8f, v)  // a little to the right
        }
    }

    private fun bass(out: FloatArray, rate: Int) {
        val bars = (SECONDS / BAR).toInt()
        for (bar in 0 until bars) {
            val f = hz(roots[bar % 4].toDouble())
            for (e in 0 until 8) {
                val start = ((bar * BAR + e * BEAT / 2) * rate).toInt()
                val n = (0.22 * rate).toInt()
                val accent = if (e % 4 == 0) 1.0 else 0.7
                for (i in 0 until n) {
                    val t = i.toDouble() / rate
                    var s = 0.0
                    for (k in 1..6) s += sin(2 * PI * k * f * t) / (k * k * 0.6 + 0.4)
                    val env = min(1.0, t / 0.005) * exp(-t * 6) * (if (i > n - rate / 200) (n - i).toDouble() / (rate / 200) else 1.0)
                    val v = (0.22 * accent * s * env).toFloat()
                    add(out, start + i, v, v)
                }
            }
        }
    }

    private fun pad(out: FloatArray, rate: Int) {
        val n = frames(rate)
        val barFrames = BAR * rate
        for (i in 0 until n) {
            val t = i.toDouble() / rate
            val bar = (i / barFrames).toInt()
            val inBar = t - bar * BAR
            // Each chord swells in, and the next one takes over over 0.3 s.
            val env = min(1.0, inBar / 0.4) * (if (inBar > BAR - 0.3) (BAR - inBar) / 0.3 else 1.0)
            var l = 0.0
            var r = 0.0
            for (m in triads[bar % 4]) {
                val f = hz(m.toDouble())
                l += sin(2 * PI * f * 0.9985 * t) + 0.3 * sin(2 * PI * 2 * f * t)
                r += sin(2 * PI * f * 1.0015 * t) + 0.3 * sin(2 * PI * 2 * f * t)
            }
            add(out, i, (0.045 * env * l).toFloat(), (0.045 * env * r).toFloat())
        }
    }

    /** Formants (Hz) and their weights for the five vowels. */
    private val formants = mapOf(
        'a' to doubleArrayOf(800.0, 1150.0, 2900.0),
        'e' to doubleArrayOf(400.0, 1900.0, 2700.0),
        'i' to doubleArrayOf(280.0, 2200.0, 2950.0),
        'o' to doubleArrayOf(450.0, 800.0, 2830.0),
        'u' to doubleArrayOf(325.0, 700.0, 2600.0),
    )
    private val formantGain = doubleArrayOf(1.0, 0.55, 0.25)
    private val formantWidth = doubleArrayOf(90.0, 120.0, 160.0)

    /** A voice that sings: harmonics of the note shaped by the vowel's formants, a light vibrato. */
    private fun voice(out: FloatArray, rate: Int) {
        for (note in melody) {
            val start = (note.beat * BEAT * rate).toInt()
            val n = (note.beats * BEAT * rate).toInt()
            val f0 = hz(note.midi.toDouble())
            val fm = formants.getValue(note.vowel)
            val count = ((minOf(4500.0, rate / 2.0 - 500) / f0).toInt()).coerceAtLeast(1)
            val amps = DoubleArray(count) { k ->
                val h = (k + 1) * f0
                var g = 0.12
                for (j in 0..2) g += formantGain[j] / (1 + ((h - fm[j]) / formantWidth[j]).pow(2))
                g / (k + 1).toDouble().pow(0.6)
            }
            val norm = 1.0 / amps.sum()
            var phase = 0.0
            for (i in 0 until n) {
                val t = i.toDouble() / rate
                // Vibrato (5.5 Hz, about 20 cents) comes in after a quarter of a second.
                val depth = 0.012 * ((t - 0.25) / 0.3).coerceIn(0.0, 1.0)
                phase += 2 * PI * f0 * (1 + depth * sin(2 * PI * 5.5 * t)) / rate
                var s = 0.0
                for (k in 0 until count) s += amps[k] * sin((k + 1) * phase)
                val env = min(1.0, t / 0.03) * min(1.0, (n - i).toDouble() / (0.06 * rate))
                val v = (0.38 * norm * s * env).toFloat()
                add(out, start + i, v, v)
            }
        }
    }
}
