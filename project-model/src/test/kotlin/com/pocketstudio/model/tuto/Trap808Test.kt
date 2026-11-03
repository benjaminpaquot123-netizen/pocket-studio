package com.pocketstudio.model.tuto

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Review « Tuto tous styles », MINEUR 8: the trap course's 808 must « taper ». A note that ends exactly
 * where the next starts plays legato (no new attack), so each 808 note stops a step before the next one;
 * only the two meant glides overlap the note after them (the synth slides between overlapping notes).
 */
class Trap808Test {
    @Test
    fun eachNoteOfTheTrap808IsHitAgainUnlessItGlides() {
        val notes = TutoTarget.parseLine(TutoCourse.of("trap")!!.lines.getValue("bass"), 1f).sortedBy { it.start }
        var glides = 0
        for ((a, b) in notes.zipWithNext()) {
            val end = a.start + a.length
            assertTrue("note at ${a.start} runs into the next one without gliding", end != b.start)
            assertTrue("note at ${a.start}: a gap or a glide", end < b.start || b.pitch != a.pitch)
            if (end > b.start) glides++
        }
        assertTrue("the two octave glides kept ($glides)", glides == 2)
    }
}
