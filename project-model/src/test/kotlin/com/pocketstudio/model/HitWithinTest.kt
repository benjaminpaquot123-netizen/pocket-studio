package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/** Review « Tuto tous styles », MINEUR 5: a triplet never spills past the end of its pad's loop. */
class HitWithinTest {
    private val triplet = Hit(1f, repeats = 3, span = 2)

    @Test
    fun aTripletInsideTheLoopPlaysWhole() {
        assertSame(triplet, triplet.within(step = 10, loop = 16))
        assertSame(triplet, triplet.within(step = 14, loop = 16))
        // A hit past the end is not heard at all: left as it is.
        assertSame(triplet, triplet.within(step = 20, loop = 16))
    }

    @Test
    fun aTripletOverTheLoopsEndKeepsOnlyItsHitsBeforeTheEnd() {
        // 3 hits over 2 steps at 0, 2/3, 4/3 of a step: on the last step, the first two.
        assertEquals(triplet.copy(repeats = 2, span = 1), triplet.within(step = 15, loop = 16))
        // 6 over a beat from step 13 of a 15-step pad loop: at 0, 2/3, 4/3 (2 steps left) → 3 hits.
        val six = Hit(1f, repeats = 6, span = 4)
        assertEquals(six.copy(repeats = 3, span = 2), six.within(step = 13, loop = 15))
        assertEquals(six.copy(repeats = 2, span = 1), six.within(step = 14, loop = 15))
    }

    @Test
    fun aRollInOneStepIsUnchanged() {
        val roll = Hit(1f, repeats = 3)
        assertSame(roll, roll.within(step = 15, loop = 16))
    }

    @Test
    fun theMidiFileWritesTheTripletAsItIsHeard() {
        // A triplet on steps 13-14, then the pad's loop shortened to 14 steps: two hits inside the loop, none on its next turn.
        val drums = DrumPattern().toggle(2, 13).withSpread(2, 13, 3, 2).withLaneLength(2, 14)
        assertEquals(2, drums.lanes[2].hits[13]!!.span)
        val notes = MidiFile.read(MidiFile.export(Project(drums = drums), song = false)).notes.filter { it.channel == MidiFile.DRUM_CHANNEL }
        val step = NoteClip.TICKS_PER_STEP
        assertEquals(listOf(13 * step, 13 * step + step / 2), notes.map { it.start })
    }
}
