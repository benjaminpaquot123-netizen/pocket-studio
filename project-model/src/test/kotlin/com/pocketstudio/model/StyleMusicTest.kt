package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StyleMusicTest {
    private val aMinor = Harmony(9, Scale.Minor)
    private val tps = NoteClip.TICKS_PER_STEP

    @Test
    fun stepLinesStartHoldAndRest() {
        val events = StepLine.parse("X---..x.R-5.....")
        assertEquals(listOf(StepLine.Event(0, 4, 'X'), StepLine.Event(6, 1, 'x'), StepLine.Event(8, 2, 'R'), StepLine.Event(10, 1, '5')), events)
        // A two-bar line alternates; a one-bar line repeats.
        assertEquals("1".repeat(16), StepLine.barOf("1".repeat(16) + "2".repeat(16), 2))
        assertEquals("2".repeat(16), StepLine.barOf("1".repeat(16) + "2".repeat(16), 3))
    }

    @Test
    fun voicingsStackNotesOfTheScale() {
        // A minor from A3 (57): A C E, then G for the seventh, B for the ninth.
        assertEquals(listOf(57, 60, 64), aMinor.voiced(0, 48, Voicing.Triad))
        assertEquals(listOf(57, 60, 64, 67), aMinor.voiced(0, 48, Voicing.Seventh))
        assertEquals(listOf(57, 60, 64, 67, 71), aMinor.voiced(0, 48, Voicing.Ninth))
        assertEquals(listOf(57, 64, 69), aMinor.voiced(0, 48, Voicing.Power))
        // VI of A minor is F major: F A C.
        assertEquals(listOf(65, 69, 72), aMinor.voiced(5, 48, Voicing.Triad))
        // Every note of every voicing is in the scale (the octave of Power too).
        Voicing.entries.forEach { v -> (0..6).forEach { d -> assertTrue("$v on $d", aMinor.voiced(d, 48, v).all(aMinor::inScale)) } }
    }

    @Test
    fun offbeatStabsChangeChordEachBar() {
        val clip = NoteClip().withChordRhythm(listOf(0, 5), aMinor, "..X...X...X...X.", Voicing.Triad, 48)
        assertEquals(32, clip.steps)
        assertEquals(8 * 3, clip.notes.size)
        val starts = clip.notes.map { it.start / tps }.distinct().sorted()
        assertEquals(listOf(2, 6, 10, 14, 18, 22, 26, 30), starts)
        assertTrue(clip.notes.all { it.length == tps })
        // Bar 2 is the VI chord (F A C), just under the A chord rather than above it.
        assertEquals(setOf(53, 57, 60), clip.notes.filter { it.start / tps >= 16 }.map { it.pitch }.toSet())
    }

    @Test
    fun linesFollowTheChords() {
        val clip = NoteClip().withLine(listOf(0, 5), aMinor, "R-..5...O...r...", 36)
        val bar1 = clip.notes.filter { it.start < 16 * tps }
        assertEquals(listOf(45, 52, 57, 45), bar1.map { it.pitch })
        assertEquals(2 * tps, bar1.first().length)
        assertTrue(bar1.last().velocity < bar1.first().velocity)
        // Bar 2 on F, under the A: F, C, F an octave up.
        assertEquals(listOf(41, 48, 53, 41), clip.notes.filter { it.start >= 16 * tps }.map { it.pitch })
    }

    @Test
    fun acidLinesFillThe303() {
        val acid = acidLine(aMinor, "R.R.O.3-5.R.7.R.", "x.......x.......", "....x...........")
        assertEquals(16, acid.length)
        assertEquals(45, acid.root)
        val s = acid.steps
        assertTrue(s[0].gate && s[0].accent && s[0].note == 0)
        assertTrue(s[4].gate && s[4].slide && s[4].note == 0 && s[4].octave == 1)
        assertTrue(s[6].gate && s[6].slide && s[6].note == 3)  // the third of A minor, tied into the next step
        assertTrue(!s[1].gate)
    }
}
