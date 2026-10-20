package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class NoteEditsTest {
    private val c4 = Note(0, 24, 60)
    private val e4 = Note(24, 24, 64)
    private val g4 = Note(96, 48, 67)
    private val clip = NoteClip(steps = 16, notes = listOf(c4, e4, g4))

    @Test
    fun transposeShiftAndVelocityKeepTheSelection() {
        val up = NoteEdits.transpose(clip, setOf(c4, e4), 12)
        assertEquals(setOf(72, 76, 67), up.clip.notes.map { it.pitch }.toSet())
        assertEquals(setOf(72, 76), up.selection.map { it.pitch }.toSet())
        val later = NoteEdits.shift(up.clip, up.selection, 48)
        assertEquals(listOf(48, 72), later.selection.map { it.start }.sorted())
        val soft = NoteEdits.setVelocity(clip, setOf(g4), 0.3f)
        assertEquals(0.3f, soft.clip.notes.single { it.pitch == 67 }.velocity, 0f)
    }

    @Test
    fun legatoDuplicatePasteDelete() {
        val legato = NoteEdits.legato(clip, clip.notes.toSet())
        assertEquals(listOf(24, 72, 384 - 96), legato.clip.notes.sortedBy { it.start }.map { it.length })
        val dup = NoteEdits.duplicate(clip, setOf(c4, e4))  // spans 2 steps: copied 2 steps later
        assertEquals(listOf(0, 24, 48, 72, 96), dup.clip.notes.map { it.start }.sorted())
        assertEquals(setOf(48, 72), dup.selection.map { it.start }.toSet())
        val board = NoteEdits.copy(setOf(e4, g4))
        assertEquals(listOf(0, 72), board.map { it.start })
        val pasted = NoteEdits.paste(clip, board, 300)
        assertEquals(setOf(300, 372), pasted.selection.map { it.start }.toSet())
        assertEquals(setOf(360), NoteEdits.paste(clip, board, 360).selection.map { it.start }.toSet())  // 432 is past the loop
        assertEquals(listOf(g4), NoteEdits.delete(clip, setOf(c4, e4)).clip.notes)
    }

    @Test
    fun quantizeHumanizeAndBox() {
        val loose = NoteClip(notes = listOf(Note(5, 20, 60), Note(29, 20, 62)))
        assertEquals(listOf(0, 24), NoteEdits.quantize(loose, loose.notes.toSet(), 24).clip.notes.map { it.start })
        val human = NoteEdits.humanize(clip, clip.notes.toSet(), Random(1))
        human.clip.notes.zip(clip.notes.sortedBy { it.start }).forEach { (h, n) -> assertTrue(kotlin.math.abs(h.start - n.start) <= 3) }
        assertEquals(setOf(c4, e4), NoteEdits.inBox(clip, 0, 48, 55, 65))
    }

    @Test
    fun guessesTheScale() {
        // A G minor riff: G Bb C D F.
        val riff = listOf(55, 58, 60, 62, 65, 55, 62).mapIndexed { i, p -> Note(i * 24, 24, p) } + Note(168, 96, 55)  // ends on a long G
        val best = NoteEdits.guessScales(riff).first()
        assertEquals(7, best.root)
        assertEquals(Scale.Minor, best.scale)
        assertEquals(emptyList<Harmony>(), NoteEdits.guessScales(emptyList()))
    }
}
