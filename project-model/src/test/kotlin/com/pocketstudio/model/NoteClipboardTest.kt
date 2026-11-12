package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Phase 37: notes copied in one piano roll and pasted in another track or part, on a bar. */
class NoteClipboardTest {
    private val t = NoteClip.TICKS_PER_STEP
    private val bar = 16 * t

    @Test
    fun aBarIsCopiedWithWhereItsNotesFall() {
        // Bar 2 of a 2-bar clip: a note on beat 2 and one on beat 4.
        val clip = NoteClip(steps = 32, notes = listOf(Note(0, t, 60), Note(bar + 4 * t, 2 * t, 64), Note(bar + 12 * t, t, 67, played = bar + 11 * t))).sanitized()
        val picked = NoteEdits.inBar(clip, 1, bar)
        assertEquals(setOf(64, 67), picked.map { it.pitch }.toSet())
        val board = NoteEdits.toClipboard(picked, bar, "Basse")!!
        assertEquals(1, board.fromBar)
        assertEquals(1, board.bars)
        assertEquals("Basse", board.from)
        // Relative to its bar's start, and forgetting where it was played (a paste is a new place).
        assertEquals(listOf(4 * t, 12 * t), board.notes.map { it.start })
        assertTrue(board.notes.all { it.played == null })
        assertNull(NoteEdits.toClipboard(emptySet(), bar))
    }

    @Test
    fun aPasteOnABarKeepsTheTimingAndGrowsTheLoop() {
        val source = NoteClip(steps = 16, notes = listOf(Note(2 * t, t, 60), Note(10 * t, 3 * t, 62))).sanitized()
        val board = NoteEdits.toClipboard(source.notes.toSet(), bar)!!
        // Into another track's one-bar loop, on its bar 2: the loop grows to 2 bars, nothing is lost.
        val other = NoteClip(steps = 16, notes = listOf(Note(0, t, 40))).sanitized()
        val r = NoteEdits.pasteOnBar(other, board, 1, bar)
        assertEquals(32, r.clip.steps)
        assertEquals(0, r.dropped)
        assertEquals(listOf(0, bar + 2 * t, bar + 10 * t), r.clip.notes.map { it.start })
        assertEquals(2, r.selection.size)
        // On bar 1 of the same clip: on top of what is there, same timing. Audit final (lot notes):
        // the same notes on the same places make one each (no invisible double), still chosen.
        val same = NoteEdits.pasteOnBar(source, board, 0, bar)
        assertEquals(2, same.clip.notes.size)
        assertEquals(2, same.selection.size)
        // Past the longest loop: what does not fit is counted, not lost silently.
        val full = NoteClip(steps = NoteClip.MAX_STEPS).sanitized()
        val past = NoteEdits.pasteOnBar(full, board, NoteClip.MAX_STEPS / 16, bar)
        assertEquals(2, past.dropped)
        assertEquals(NoteClip.MAX_STEPS, past.clip.steps)
    }

    @Test
    fun aLoopTakesAnyNumberOfBars() {
        // Free length to the bar: 3 bars, 5 bars… up to the longest (16 bars).
        assertEquals(256, NoteClip.MAX_STEPS)
        assertEquals(176, NoteClip(steps = 176).sanitized().steps)
        assertEquals(48, NoteClip(steps = 48).sanitized().steps)
        assertEquals(80, NoteClip(steps = 80).sanitized().steps)
        assertEquals(NoteClip.MAX_STEPS, NoteClip(steps = 999).sanitized().steps)
        // Audit final (lot notes): whole beats, so whole bars of any time signature (20 = a bar of 5/4).
        assertEquals(20, NoteClip(steps = 20).sanitized().steps)
        assertEquals(16, NoteClip(steps = 18).sanitized().steps)
    }
}
