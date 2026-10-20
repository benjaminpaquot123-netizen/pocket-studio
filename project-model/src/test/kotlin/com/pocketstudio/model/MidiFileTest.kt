package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class MidiFileTest {
    private fun project(): Project {
        val notes = NoteClip(steps = 16, notes = listOf(Note(0, 48, 60, 0.8f), Note(96, 24, 67, 0.5f)))
        val acid = AcidPattern(length = 16, root = 36, steps = List(AcidPattern.MAX_STEPS) { i -> if (i == 4) AcidStep(note = 3, octave = 1, accent = true, gate = true) else AcidStep() })
        val drums = DrumPattern().toggle(0, 0).toggle(2, 4, 0.5f)
        return Project(name = "Test", tempoBpm = 111.0, synthNotes = notes, acid = acid, drums = drums)
    }

    @Test
    fun exportThenImportKeepsNotesTimingAndTempo() {
        val bytes = MidiFile.export(project(), song = false)
        assertEquals("MThd", String(bytes, 0, 4))
        val content = MidiFile.read(bytes)
        assertEquals(111.0, content.tempoBpm!!, 0.01)
        val synth = content.notes.filter { it.channel == 0 }
        assertEquals(listOf(60 to 0, 67 to 96), synth.map { it.pitch to it.start })
        assertEquals(48, synth[0].length)
        val acid = content.notes.single { it.channel == 1 }
        assertEquals(36 + 3 + 12, acid.pitch)
        assertEquals(4 * NoteClip.TICKS_PER_STEP, acid.start)
        assertEquals(120, acid.velocity)
        val drums = content.notes.filter { it.channel == MidiFile.DRUM_CHANNEL }
        assertEquals(listOf(36 to 0, 38 to 96), drums.map { it.pitch to it.start })
        // Back into an empty project: same notes, same drum steps, same tempo.
        val back = MidiFile.importInto(Project(), content.copy(notes = content.notes.filter { it.channel != 1 }))
        assertEquals(listOf(60, 67), back.synthNotes.notes.map { it.pitch })
        assertEquals(0.5f, back.drums.lanes[2].hits[4]!!.velocity, 0.01f)
        assertTrue(back.drums.lanes[0].hits.containsKey(0))
        assertEquals(111.0, back.tempoBpm, 0.01)
    }

    @Test
    fun addedTracksGetTheirOwnChannelAndComeBackAsTracks() {
        var p = project().addTrack("Cordes")!!
        val id = p.tracks.single().id
        p = p.withTrack(p.track(id).copy(notes = NoteClip(notes = listOf(Note(24, 24, 72))))).sanitized()
        val content = MidiFile.read(MidiFile.export(p, song = false))
        assertEquals(listOf(72 to 24), content.notes.filter { it.channel == 1 + id }.map { it.pitch to it.start })
        // Channels 1 and 3 (acid left out): the first track, then a new track named after its channel.
        val back = MidiFile.importInto(Project(), content.copy(notes = content.notes.filter { it.channel != 1 }))
        assertEquals(listOf(60, 67), back.synthNotes.notes.map { it.pitch })
        assertEquals(1, back.tracks.size)
        assertEquals("Canal ${2 + id}", back.tracks.single().name)
        assertEquals(listOf(72), back.tracks.single().notes.notes.map { it.pitch })
    }

    @Test
    fun theSongIsChainedScenes() {
        val a = project()
        // Scene B: an empty copy; song A ×2 then B.
        val p = a.copySceneTo(1).copy(song = listOf(SongPart(0, 2), SongPart(1, 1))).sanitized()
        val synth = MidiFile.read(MidiFile.export(p)).notes.filter { it.channel == 0 }
        val bar = 16 * NoteClip.TICKS_PER_STEP
        assertEquals(listOf(0, 96, bar, bar + 96, 2 * bar, 2 * bar + 96), synth.map { it.start })  // B is a copy of A
    }

    @Test
    fun foreignFilesAndBadData() {
        // Type 0, 480 PPQ, running status, a note-on at velocity 0 as note-off.
        val track = byteArrayOf(
            0x00, 0x90.toByte(), 0x40, 0x64,
            0x83.toByte(), 0x60, 0x40, 0x00,          // 480 ticks later, running status, vel 0
            0x00, 0x99.toByte(), 0x2A, 0x50,          // closed hi-hat on channel 10
            0x10, 0x89.toByte(), 0x2A, 0x00,
            0x00, 0xFF.toByte(), 0x2F, 0x00,
        )
        val header = byteArrayOf(0x4D, 0x54, 0x68, 0x64, 0, 0, 0, 6, 0, 0, 0, 1, 0x01, 0xE0.toByte())
        val mtrk = byteArrayOf(0x4D, 0x54, 0x72, 0x6B, 0, 0, 0, track.size.toByte())
        val content = MidiFile.read(header + mtrk + track)
        assertEquals(MidiNote(0, 64, 0, MidiFile.PPQ, 100), content.notes.first { it.channel == 0 })
        assertEquals(5, MidiFile.padFor(content.notes.first { it.channel == 9 }.pitch))
        assertEquals(null, content.tempoBpm)
        val failed = runCatching { MidiFile.read("pas du midi".toByteArray()) }.exceptionOrNull()
        assertTrue(failed is IOException)
        assertTrue(runCatching { MidiFile.read(header + mtrk + track.copyOf(6)) }.exceptionOrNull() is IOException)
    }
}
