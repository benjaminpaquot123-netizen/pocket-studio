package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** A clip "replaced whole" never brings back notes hidden by an earlier shortening. */
class ReplaceClipTest {
    private val cMajor = Harmony(0, Scale.Major)

    /** A 4-bar melody shortened to 1 bar: 3 bars of notes are hidden. */
    private fun shortened(): NoteClip {
        val melody = (0 until 16).map { Note(it * 4 * NoteClip.TICKS_PER_STEP, NoteClip.TICKS_PER_STEP, 84 + it % 5, 0.9f) }
        val clip = NoteClip(steps = 64, notes = melody).sanitized().copy(steps = 16).sanitized()
        assertTrue(clip.hidden.isNotEmpty())
        return clip
    }

    @Test
    fun chordsBassAndTranscriptionReplaceTheWholeClip() {
        val clip = shortened()
        assertEquals(NoteClip().withProgression(Progression.Pop, cMajor).notes, clip.withProgression(Progression.Pop, cMajor).notes)
        assertTrue(clip.withProgression(Progression.Pop, cMajor).hidden.isEmpty())
        assertEquals(NoteClip().withBassLine(listOf(0, 4), cMajor).notes, clip.withBassLine(listOf(0, 4), cMajor).notes)
        val heard = listOf(HeardNote(0.0, 0.5, 60, 0.8f), HeardNote(3.0, 3.5, 62, 0.8f))  // 2 bars at 120 BPM
        val sung = Transcription.toClip(heard, 120.0, base = clip)
        assertEquals(Transcription.toClip(heard, 120.0).notes, sung.notes)
        assertTrue(sung.hidden.isEmpty())
    }

    @Test
    fun aMidiImportReplacesTheSynthClip() {
        val p = Project(synthNotes = shortened())
        val content = MidiFile.read(MidiFile.export(Project(synthNotes = NoteClip(notes = listOf(Note(0, 24, 60, 0.8f))))))
        val imported = MidiFile.importInto(p, content)
        assertEquals(listOf(60), imported.synthNotes.notes.map { it.pitch })
        assertTrue(imported.synthNotes.hidden.isEmpty())
    }
}
