package com.pocketstudio.model

import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Lot C (n° 23): the app's « La mélodie de la voix en notes » ([SongMatter.voiceNotes]) run on a
 * real separated voice over the whole song, for scripts/voice-notes-check.py, which compares the
 * notes with Basic Pitch's. Skipped unless VOICE_NOTES_RAW names the voice (mono 32-bit float at
 * 48 kHz, as the app reads its stems) and VOICE_NOTES_OUT where to write the notes (JSON: one
 * [start s, end s, MIDI] per note). The song and its notes stay outside the repository.
 */
class VoiceNotesFileCheck {
    @Test
    fun voiceNotesOfAGivenVoice() {
        val raw = System.getenv("VOICE_NOTES_RAW")?.let(::File)
        val out = System.getenv("VOICE_NOTES_OUT")?.let(::File)
        assumeTrue("no voice given (VOICE_NOTES_RAW)", raw != null && raw.isFile && out != null)
        val bpm = System.getenv("VOICE_NOTES_BPM")?.toDoubleOrNull() ?: 120.0
        val firstBeat = System.getenv("VOICE_NOTES_FIRST_BEAT")?.toDoubleOrNull() ?: 0.0
        val rate = 48_000
        val bytes = raw!!.readBytes()
        val all = FloatArray(bytes.size / 4)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(all)
        val from = (firstBeat * rate).toInt().coerceIn(0, all.size)
        val mono = all.copyOfRange(from, all.size)
        // A sixteenth of the song, as the app's grid (RhythmViewModel.stemToNotes, the song not stretched).
        val stepFrames = 60.0 / bpm / 4.0 * rate
        val steps = (mono.size / stepFrames).toInt() - 2
        val notes = SongMatter.voiceNotes(mono, rate, stepFrames, steps)
        val perTick = stepFrames / NoteClip.TICKS_PER_STEP / rate
        out!!.writeText(notes.joinToString(",\n", "[\n", "\n]\n") { n ->
            "[%.4f, %.4f, %d]".format(java.util.Locale.ROOT, firstBeat + n.start * perTick, firstBeat + (n.start + n.length) * perTick, n.pitch)
        })
        println("VOICE NOTES: ${notes.size} notes over ${"%.1f".format(mono.size / rate.toDouble())} s -> $out")
    }
}
