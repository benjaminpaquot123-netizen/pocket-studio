package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Phase 37: a length per pad (a polyrhythm) and a chance per hit, drawn the same way every time. */
class PolyrhythmChanceTest {
    @Test
    fun aPadLoopsOnItsOwnLength() {
        val drums = DrumPattern().toggle(0, 0).withLaneLength(0, 3)
        assertEquals(3, drums.lanes[0].length)
        assertEquals(listOf(0, 1, 2, 0, 1, 2, 0), (0..6).map { drums.laneStep(0, it) })
        // The other pads keep the pattern's loop.
        assertEquals(15, drums.laneStep(1, 15))
        assertEquals(0, drums.laneStep(1, 16))
        // The pattern's own length (or 0) means no length of its own.
        assertEquals(0, drums.withLaneLength(0, 16).lanes[0].length)
        assertEquals(0, drums.withLaneLength(0, 0).lanes[0].length)
        assertTrue(drums.isPolyOrChance)
        assertFalse(DrumPattern().toggle(0, 0).isPolyOrChance)
        // A wild value from a file is brought back in range.
        val wild = DrumPattern(lanes = List(DrumPattern.PAD_COUNT) { DrumLane(length = 999) }).sanitized()
        assertTrue(wild.lanes.all { it.length == DrumPattern.MAX_STEPS })
    }

    @Test
    fun aHitsChanceIsKeptAndChecked() {
        val drums = DrumPattern().toggle(2, 4).withHitChance(2, 4, 50)
        assertEquals(50, drums.lanes[2].hits.getValue(4).chance)
        // No hit there: nothing changes; too low or too high: brought back in range.
        assertEquals(drums, drums.withHitChance(2, 5, 30))
        assertEquals(Hit.MIN_CHANCE, drums.withHitChance(2, 4, 0).lanes[2].hits.getValue(4).chance)
        assertEquals(100, drums.withHitChance(2, 4, 400).lanes[2].hits.getValue(4).chance)
        assertEquals(Hit.MIN_CHANCE, Hit(chance = -3).sanitized().chance)
    }

    @Test
    fun oldProjectsReadAndWriteTheSame() {
        // Defaults are left out of the file: a project without lengths, chances or seed is saved as before.
        val plain = Project(drums = DrumPattern().toggle(0, 0).toggle(1, 4))
        val text = ProjectJson.encode(plain)
        assertFalse(text, text.contains("\"pan\":0.0,\"length\""))
        assertFalse(text, text.contains("\"chance\""))
        assertFalse(text, text.contains("drumSeed"))
        // Written with them, read back with them.
        val poly = plain.copy(drums = plain.drums.withLaneLength(0, 5).withHitChance(1, 4, 25), drumSeed = 77)
        val back = ProjectJson.decode(ProjectJson.encode(poly))
        assertEquals(5, back.drums.lanes[0].length)
        assertEquals(25, back.drums.lanes[1].hits.getValue(4).chance)
        assertEquals(77, back.drumSeed)
        // A file from before phase 37 has none of the keys: everything plays as before.
        val old = ProjectJson.decode("""{"drums":{"steps":16,"lanes":[{"hits":{"0":{"velocity":1.0}}}]}}""")
        assertEquals(0, old.drums.lanes[0].length)
        assertEquals(100, old.drums.lanes[0].hits.getValue(0).chance)
        assertEquals(0, old.drumSeed)
    }

    @Test
    fun theDrawIsTheEnginesAndTheSameEveryTime() {
        // The same masks as the engine's host test (phase37_tests.inc): one draw, app and engine.
        var m = 0L
        var m2 = 0L
        for (k in 0 until 32) {
            if (DrumChance.plays(7, 3, k, 50)) m = m or (1L shl k)
            if (DrumChance.plays(0, 0, k, 75)) m2 = m2 or (1L shl k)
        }
        assertEquals(0x40b5c267L, m)
        assertEquals(0x5b7df3f6L, m2)
        assertTrue(DrumChance.plays(1, 2, 3, 100))
        // About the chance asked over many passes, and another seed draws otherwise.
        val played = (0 until 4000).count { DrumChance.plays(3, 5, it, 30) }
        assertTrue("$played", played in 1000..1400)
        val a = (0 until 256).map { DrumChance.plays(3, 5, it, 50) }
        val b = (0 until 256).map { DrumChance.plays(DrumChance.next(3), 5, it, 50) }
        assertNotEquals(a, b)
        assertEquals(a, (0 until 256).map { DrumChance.plays(3, 5, it, 50) })
        assertNotEquals(3, DrumChance.next(3))
        assertTrue(DrumChance.next(DrumChance.MAX_SEED - 1) in 0 until DrumChance.MAX_SEED)
    }

    @Test
    fun theMidiFileHasThePolyrhythmAndTheChancesHeard() {
        // A kick on a lane of 3 steps over one bar: hits on 0, 3, 6, 9, 12, 15.
        val p = Project(drums = DrumPattern().toggle(0, 0).withLaneLength(0, 3))
        val kicks = midiNoteStarts(MidiFile.export(p, song = false), 36)
        assertEquals((0 until 16 step 3).map { it * NoteClip.TICKS_PER_STEP }, kicks)
        // A hat at 50 % on every step: the steps drawn by the engine's draw, no others.
        var q = Project(drums = DrumPattern(), drumSeed = 11)
        for (s in 0 until 16) q = q.copy(drums = q.drums.toggle(2, s).withHitChance(2, s, 50))
        val hats = midiNoteStarts(MidiFile.export(q, song = false), MidiFile.PAD_NOTES[2])
        val expected = (0 until 16).filter { DrumChance.plays(11, 2, it, 50) }.map { it * NoteClip.TICKS_PER_STEP }
        assertEquals(expected, hats)
        assertTrue(hats.size in 3..13)
    }

    /** The starts (in MIDI ticks) of the note-ons of [note] on the drums' channel. */
    private fun midiNoteStarts(bytes: ByteArray, note: Int): List<Int> =
        MidiFile.read(bytes).notes.filter { it.pitch == note && it.channel == MidiFile.DRUM_CHANNEL }.map { it.start }.sorted()
}
