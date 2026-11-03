package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Benjamin's n° 23 (« la mélodie de la voix en notes sonne comme un bruit de test »): the voice in
 * notes follows the sung pitch within one semitone on most notes, over the whole song (not only its
 * start), and the notes start where the voice sings. A voice-like signal (harmonics, vibrato, glides,
 * breath, silences) stands for the separated voice; the bass is checked the same way.
 */
class VoiceNotesSongTest {
    private val rate = 48_000
    private val bpm = 120.0
    private val stepFrames = rate * 15.0 / bpm  // a sixteenth

    /** A sung line: each note [steps] long at [midi] (-1: silence), harmonics, 5.5 Hz vibrato, a glide in, breath. */
    private fun sing(notes: List<Pair<Int, Int>>, seed: Int, harmonics: Int = 6, lowRoll: Double = 0.7): FloatArray {
        val total = (notes.sumOf { it.second } * stepFrames).toInt()
        val out = FloatArray(total)
        val rnd = Random(seed)
        var at = 0
        var phase = 0.0
        var prevHz = 0.0
        for ((midi, steps) in notes) {
            val n = (steps * stepFrames).toInt()
            if (midi < 0) { at += n; prevHz = 0.0; continue }
            val hz = 440.0 * 2.0.pow((midi - 69) / 12.0)
            for (i in 0 until n) {
                if (at + i >= total) break
                val t = i.toDouble() / rate
                val glide = if (prevHz > 0 && t < 0.05) prevHz + (hz - prevHz) * t / 0.05 else hz
                val f = glide * (1 + 0.012 * sin(2 * PI * 5.5 * t))
                phase += 2 * PI * f / rate
                var v = 0.0
                for (h in 1..harmonics) v += sin(phase * h) * lowRoll.pow(h - 1)
                val env = minOf(1.0, t / 0.03) * minOf(1.0, (n - i).toDouble() / (0.04 * rate))
                out[at + i] = (0.3 * env * v / harmonics + 0.01 * (rnd.nextDouble() - 0.5)).toFloat()
            }
            at += n
            prevHz = hz
        }
        return out
    }

    /** A melody of [bars] bars: notes of 1 to 4 sixteenths between [low] and [high], a rest now and then; an intro of silence first. */
    private fun melody(bars: Int, low: Int, high: Int, seed: Int, introBars: Int): List<Pair<Int, Int>> {
        val rnd = Random(seed)
        val out = mutableListOf(-1 to introBars * 16)
        var left = bars * 16
        while (left > 0) {
            val len = minOf(left, 1 + rnd.nextInt(4))
            out += (if (rnd.nextInt(8) == 0) -1 else low + rnd.nextInt(high - low + 1)) to len
            left -= len
        }
        return out
    }

    /** The share of the sung sixteenths whose note is within a semitone of the one sung, window after window over the whole line. */
    private fun accuracy(notes: List<Pair<Int, Int>>, audio: FloatArray, voice: Boolean): Double {
        val sung = IntArray((audio.size / stepFrames).toInt()) { -1 }
        var at = 0
        notes.forEach { (m, n) -> for (k in at until minOf(sung.size, at + n)) sung[k] = m; at += n }
        val heard = IntArray(sung.size) { -1 }
        var w = 0
        while (w < sung.size) {
            val start = (w * stepFrames).toInt()
            val chunk = audio.copyOfRange(start, minOf(audio.size, start + (stepFrames * (NoteClip.MAX_STEPS + 2)).toInt()))
            val found = if (voice) SongMatter.voiceNotes(chunk, rate, stepFrames, NoteClip.MAX_STEPS) else SongMatter.bassNotes(chunk, rate, stepFrames, NoteClip.MAX_STEPS)
            found.forEach { n ->
                for (k in n.start / NoteClip.TICKS_PER_STEP until (n.start + n.length) / NoteClip.TICKS_PER_STEP) if (w + k < heard.size) heard[w + k] = n.pitch
            }
            w += NoteClip.MAX_STEPS
        }
        // A note's first sixteenth may still be gliding in: the others are counted.
        val counted = sung.indices.filter { k -> sung[k] >= 0 && k > 0 && sung[k - 1] == sung[k] }
        return counted.count { k -> heard[k] >= 0 && abs(heard[k] - sung[k]) <= 1 }.toDouble() / counted.size
    }

    @Test
    fun theVoiceInNotesFollowsTheSungPitchOverTheWholeSong() {
        // About three minutes (88 bars at 120) of a voice between G3 and C6: windows of 16 bars, end to end.
        val line = melody(bars = 88, low = 55, high = 84, seed = 7, introBars = 4)
        val acc = accuracy(line, sing(line, 1), voice = true)
        assertTrue("voice: only ${"%.0f".format(acc * 100)} % of the sung sixteenths within a semitone", acc >= 0.85)
    }

    @Test
    fun theBassInNotesToo() {
        val line = melody(bars = 48, low = 28, high = 52, seed = 3, introBars = 2)
        val acc = accuracy(line, sing(line, 2, harmonics = 8, lowRoll = 0.8), voice = false)
        assertTrue("bass: only ${"%.0f".format(acc * 100)} % within a semitone", acc >= 0.85)
    }

    @Test
    fun theNotesStartWhereTheVoiceSings() {
        // Eight silent bars (an intro), then the voice: its notes taken from bar 8, not from the silence.
        val levels = DoubleArray(40) { b -> if (b < 8 || b in 20..23) 0.002 else 0.2 }
        assertEquals(8, SongMatter.singingStart(levels, 16))
        assertEquals(0, SongMatter.singingStart(DoubleArray(10), 16))
        // A verse, a long instrumental, a chorus sung all through: the most sung window wins.
        val chorus = DoubleArray(60) { b -> if (b in 2..5 || b in 30..45) 0.3 else 0.0 }
        assertEquals(30, SongMatter.singingStart(chorus, 16))
    }

    @Test
    fun theVoiceSoundIsSoftNotATestTone() {
        val v = SongMatter.VOICE_SOUND
        assertTrue("an attack, not a click", v.get(SynthPatch.A_ATTACK) > 0.1f)
        assertTrue("a vibrato", v.get(SynthPatch.VIBRATO) > 0f)
        assertTrue("held, as a sung note", v.get(SynthPatch.A_SUSTAIN) >= 0.8f)
        assertTrue("rounded", v.get(SynthPatch.CUTOFF) < 0.6f)
    }

    /**
     * Benjamin's song (scratchpad copy, never committed; DECISIONS 2026-11-01), when it is there:
     * decoded with ffmpeg, its singing found after the intro and notes found all through it. The mix
     * is not the separated voice (no separation on the computer), so the pitch is the synthetic test's.
     */
    @Test
    fun benjaminsSongWhenItIsHere() {
        val mp3 = File(System.getenv("POCKET_TEST_SONG") ?: "/tmp/claude-0/-home-user-pocket-studio/2ed29bda-5e9a-5850-a6a7-bbf3293c7a09/scratchpad/humain/chansons-test/adela-boys.mp3")
        if (!mp3.isFile) return
        val ffmpeg = listOf("/usr/bin/ffmpeg", "/usr/local/bin/ffmpeg").firstOrNull { File(it).canExecute() } ?: return
        val proc = ProcessBuilder(ffmpeg, "-v", "quiet", "-i", mp3.path, "-ac", "1", "-ar", "$rate", "-f", "f32le", "-").start()
        val bytes = proc.inputStream.readBytes()
        proc.waitFor()
        val buf = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
        val audio = FloatArray(buf.remaining()).also { buf.get(it) }
        assertTrue("decoded", audio.size > rate * 60)
        val bar = (stepFrames * 16).toInt()
        val levels = DoubleArray(audio.size / bar) { b -> var e = 0.0; for (i in b * bar until (b + 1) * bar) e += audio[i] * audio[i].toDouble(); kotlin.math.sqrt(e / bar) }
        val start = SongMatter.singingStart(levels, 16)
        assertTrue("never the silent start", levels[start] >= 0.25 * levels.max())
        // Notes found all through the song, window after window (not only at its start).
        var windows = 0
        var withNotes = 0
        var w = 0
        while ((w + 16) * bar <= audio.size) {
            val chunk = audio.copyOfRange(w * bar, minOf(audio.size, (w + 17) * bar))
            windows++
            if (SongMatter.voiceNotes(chunk, rate, stepFrames, NoteClip.MAX_STEPS).size >= 8) withNotes++
            w += 16
        }
        assertTrue("$withNotes of $windows windows with notes", withNotes * 2 >= windows)
    }
}
