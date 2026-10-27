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
    fun anAcidLineShorterThanTheSceneRunsOnAcrossThePartsRepeats() {
        // 12 steps in a 16-step scene played twice: like the engine, the line keeps cycling
        // (steps 0, 12, 24), it does not start over at the second repeat (0, 12, 16, 28).
        val acid = AcidPattern(length = 12, steps = List(AcidPattern.MAX_STEPS) { i -> AcidStep(gate = i == 0) })
        val p = Project(acid = acid, drums = DrumPattern().toggle(0, 0)).copy(song = listOf(SongPart(0, 2))).sanitized()
        val starts = MidiFile.read(MidiFile.export(p)).notes.filter { it.channel == 1 }.map { it.start / NoteClip.TICKS_PER_STEP }
        assertEquals(listOf(0, 12, 24), starts)
    }

    @Test
    fun theSwingIsWrittenIntoTheTiming() {
        val drums = DrumPattern().toggle(0, 0).toggle(2, 5).toggle(2, 6)
        val p = Project(swing = 0.5, drums = drums, synthNotes = NoteClip(notes = listOf(Note(3 * 24 + 6, 12, 60))))
        val notes = MidiFile.read(MidiFile.export(p, song = false)).notes
        // Odd sixteenths start half a sixteenth late (12 ticks); even ones stay on the grid.
        assertEquals(listOf(0, 6 * 24, 5 * 24 + 12), notes.filter { it.channel == MidiFile.DRUM_CHANNEL }.map { it.start }.sorted().let { listOf(it[0], it[2], it[1]) })
        // A note inside an odd sixteenth: late by the swing, then its place in the squeezed step
        // (6 ticks of 24 → 3 of the 12 left), as the engine plays it (audit 06).
        assertEquals(listOf(3 * 24 + 12 + 3), notes.filter { it.channel == 0 }.map { it.start })
        // No swing: the grid as it is.
        assertEquals(5 * 24, MidiFile.read(MidiFile.export(p.copy(swing = 0.0), song = false)).notes.filter { it.channel == MidiFile.DRUM_CHANNEL }.map { it.start }.sorted()[1])
    }

    /** Audit 06: 1/32 and triplet notes under the swing keep their order, never a flam with the next sixteenth. */
    @Test
    fun swungNotesBetweenSixteenthsKeepTheirOrder() {
        for (swing in listOf(0.375, 0.5, 0.75)) {
            // 1/32 (12 ticks) and 1/16 triplets (16 ticks) over two beats.
            for (grid in listOf(12, 16)) {
                val clip = NoteClip(notes = (0 until 8 * 24 / grid).map { Note(it * grid, grid, 60 + it % 12) })
                val got = MidiFile.read(MidiFile.export(Project(swing = swing, synthNotes = clip), song = false)).notes
                    .filter { it.channel == 0 }.sortedBy { it.start }
                assertEquals(clip.notes.size, got.size)
                // Same order as written, at least a tick apart, never overlapping the next one.
                assertEquals(clip.notes.map { it.pitch }, got.map { it.pitch })
                got.zipWithNext().forEach { (a, b) ->
                    assertTrue("swing $swing grid $grid: ${a.start} then ${b.start}", b.start >= a.start + 1)
                    assertTrue("swing $swing grid $grid: ${a.start}+${a.length} past ${b.start}", a.start + a.length <= b.start)
                }
            }
        }
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

    /** Phase 29 C6: the file has what is heard, an arpeggio or a strum played out, not the chord held. */
    @Test
    fun arpeggiosAndStrumsAreWrittenAsTheyPlay() {
        // A C major chord over a quarter note, arpeggiated upwards every sixteenth.
        val chord = listOf(Note(0, 96, 60), Note(0, 96, 64), Note(0, 96, 67))
        val arp = NoteClip(steps = 16, notes = chord, performance = Performance(arp = ArpMode.Up, arpRate = 24))
        val notes = MidiFile.read(MidiFile.export(Project(synthNotes = arp), song = false)).notes.filter { it.channel == 0 }
        assertEquals(listOf(60 to 0, 64 to 24, 67 to 48, 60 to 72), notes.map { it.pitch to it.start })
        assertTrue(notes.all { it.length == 24 })
        // Strummed: each note a little later, all ending together.
        val strum = arp.copy(performance = Performance(strum = 6))
        val strummed = MidiFile.read(MidiFile.export(Project(synthNotes = strum), song = false)).notes.filter { it.channel == 0 }
        assertEquals(listOf(60 to 0, 64 to 6, 67 to 12), strummed.map { it.pitch to it.start })
        assertEquals(listOf(96, 90, 84), strummed.map { it.length })
        // An added track arpeggiates too.
        var p = Project().addTrack("Cordes")!!
        val id = p.tracks.single().id
        p = p.withTrack(p.track(id).copy(notes = arp)).sanitized()
        assertEquals(4, MidiFile.read(MidiFile.export(p, song = false)).notes.count { it.channel == 1 + id })
    }

    /** Phase 29 C6: a roll is its hits, the step split evenly, each a little softer (as the engine plays it). */
    @Test
    fun rollsAreWrittenHitByHit() {
        val drums = DrumPattern().toggle(2, 4).cycleRepeats(2, 4).cycleRepeats(2, 4).cycleRepeats(2, 4)  // 4 hits
        assertEquals(4, drums.lanes[2].hits[4]!!.repeats)
        val notes = MidiFile.read(MidiFile.export(Project(drums = drums), song = false)).notes.filter { it.channel == MidiFile.DRUM_CHANNEL }
        val step = NoteClip.TICKS_PER_STEP
        assertEquals(listOf(4 * step, 4 * step + 6, 4 * step + 12, 4 * step + 18), notes.map { it.start })
        assertTrue(notes.all { it.pitch == 38 })
        // Softer each time (0.85 per hit), never overlapping the next one.
        assertEquals(listOf(127, 108, 92, 78), notes.map { it.velocity })
        notes.zipWithNext().forEach { (a, b) -> assertTrue(a.start + a.length <= b.start) }
        // Swung: the odd step is shorter, its roll stays inside it (before the next step).
        val swung = DrumPattern().toggle(2, 5).cycleRepeats(2, 5)
        val late = MidiFile.read(MidiFile.export(Project(drums = swung, swing = 0.5), song = false)).notes.filter { it.channel == MidiFile.DRUM_CHANNEL }
        assertEquals(listOf(5 * step + 12, 5 * step + 12 + 6), late.map { it.start })
    }

    /** Phase 29 C6: two notes of one pitch overlapping (a long note, an arpeggio over it) never hide each other. */
    @Test
    fun overlappingNotesOfOnePitchEndBeforeTheNextStarts() {
        val clip = NoteClip(steps = 16, notes = listOf(Note(0, 96, 60), Note(48, 24, 60), Note(48, 12, 64)))
        val notes = MidiFile.read(MidiFile.export(Project(synthNotes = clip), song = false)).notes.filter { it.channel == 0 }
        assertEquals(listOf(Triple(60, 0, 48), Triple(60, 48, 24), Triple(64, 48, 12)), notes.map { Triple(it.pitch, it.start, it.length) })
        // A note running past the loop's end does not swallow the loop's next start either.
        val looping = NoteClip(steps = 16, notes = listOf(Note(0, 24, 62), Note(360, 48, 62)))
        val again = MidiFile.read(MidiFile.export(Project(synthNotes = looping).copy(song = listOf(SongPart(0, 2))).sanitized())).notes.filter { it.channel == 0 }
        assertEquals(listOf(0 to 24, 360 to 24, 384 to 24, 744 to 24), again.map { it.start to it.length })
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
