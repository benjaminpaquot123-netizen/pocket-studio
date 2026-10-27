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

    @Test
    fun aNoteQuantizedOntoTheLoopsEndStartsTheLoop() {
        // Played a little before the next « 1 » (the loop's end, 384 ticks): it belongs on step 0,
        // not a tick before the end where it would flam with the kick.
        val late = Note(380, 12, 60)
        val q = NoteEdits.quantize(NoteClip(steps = 16, notes = listOf(late)), setOf(late), 24)
        assertEquals(listOf(0), q.clip.notes.map { it.start })
        assertEquals(listOf(0), q.selection.map { it.start })
        assertEquals(listOf(0), NoteClip(steps = 16, notes = listOf(late)).quantized(24, 1f).notes.map { it.start })
        // Half way: still inside the loop, not wrapped.
        assertEquals(listOf(382), NoteClip(steps = 16, notes = listOf(late)).quantized(24, 0.5f).notes.map { it.start })
        // Earlier notes still go to their nearest line.
        assertEquals(listOf(360), NoteEdits.quantize(clip.copy(notes = listOf(Note(365, 12, 60))), setOf(Note(365, 12, 60)), 24).clip.notes.map { it.start })
    }

    @Test
    fun aPasteThatDoesNotFitSaysHowManyNotesWereLeftOut() {
        val board = NoteEdits.copy(setOf(e4, g4))  // starts 0 and 72
        val fits = NoteEdits.paste(clip, board, 300)
        assertEquals(0, fits.dropped)
        val half = NoteEdits.paste(clip, board, 360)  // 432 is past the loop
        assertEquals(1, half.dropped)
        assertEquals(setOf(360), half.selection.map { it.start }.toSet())
        // After the last note of a full loop: nothing fits, nothing changes (no stray note a tick before the end).
        val none = NoteEdits.paste(clip, board, 384)
        assertEquals(2, none.dropped)
        assertEquals(clip.notes, none.clip.notes)
        assertTrue(none.selection.isEmpty())
    }

    @Test
    fun shorteningTheLoopHidesTheNotesPastItsEndAndLengtheningBringsThemBack() {
        val long = NoteClip(steps = 32, notes = listOf(c4, Note(400, 24, 62), Note(700, 24, 64))).sanitized()
        val short = long.copy(steps = 16).sanitized()
        assertEquals(listOf(c4), short.notes)  // played, shown and exported: the loop's notes only
        assertEquals(listOf(400, 700), short.hidden.map { it.start })
        // Edits of the short loop keep them.
        val edited = NoteEdits.transpose(short, setOf(c4), 2).clip
        assertEquals(2, edited.hidden.size)
        val back = edited.copy(steps = 32).sanitized()
        assertEquals(listOf(0, 400, 700), back.notes.map { it.start })
        assertTrue(back.hidden.isEmpty())
        // Saved with the project; an older file without the field loads with none.
        assertEquals(short, ProjectJson.decode(ProjectJson.encode(Project(synthNotes = short))).synthNotes)
        assertTrue(ProjectJson.decode("""{"synthNotes":{"steps":16,"notes":[{"start":0,"length":24,"pitch":60}]}}""").synthNotes.hidden.isEmpty())
        // « Effacer » removes them too.
        assertTrue(short.cleared().copy(steps = 32).sanitized().notes.isEmpty())
        // The MIDI file has only what is heard.
        val midi = MidiFile.read(MidiFile.export(Project(synthNotes = short).sanitized(), song = false)).notes.filter { it.channel == 0 }
        assertEquals(listOf(60), midi.map { it.pitch })
    }

    @Test
    fun aDuplicateThatDoesNotFitSaysHowManyNotesWereLeftOut() {
        // The three notes span six steps: their copies fit.
        val all = NoteEdits.duplicate(clip, setOf(c4, e4, g4))
        assertEquals(0, all.dropped)
        // A note near the end: its copy would start past the loop.
        val late = Note(clip.ticks - 48, 24, 72)
        val full = clip.copy(notes = clip.notes + late)
        val dup = NoteEdits.duplicate(full, setOf(c4, late))
        assertEquals(1, dup.dropped)
        assertEquals(1, dup.selection.size)
    }

    @Test
    fun transposingTheWholeClipMovesTheHiddenNotesToo() {
        val long = NoteClip(steps = 32, notes = listOf(c4, Note(400, 24, 62))).sanitized()
        val short = long.copy(steps = 16).sanitized()
        // Everything chosen: the hidden note moves with the rest and comes back in key.
        val all = NoteEdits.transpose(short, short.notes.toSet(), 2).clip
        assertEquals(listOf(64), all.hidden.map { it.pitch })
        assertEquals(listOf(62, 64), all.copy(steps = 32).sanitized().notes.map { it.pitch })
        // Only some notes chosen: the hidden ones stay.
        val some = NoteEdits.transpose(clip.copy(hidden = listOf(Note(400, 24, 62))), setOf(c4), 2).clip
        assertEquals(listOf(62), some.hidden.map { it.pitch })
    }

    @Test
    fun chordsAreNotesStartingTogether() {
        assertEquals(false, NoteEdits.hasChords(clip.notes))
        assertEquals(true, NoteEdits.hasChords(clip.notes + Note(0, 24, 64)))
        assertEquals(false, NoteEdits.hasChords(emptyList()))
    }

    @Test
    fun notesAreFittedIntoTheScaleWithoutDoubles() {
        val cMajor = Harmony(0, Scale.Major)
        // C# (61) goes down to C (60), where a C already starts: they merge, the longer kept.
        val sharp = Note(0, 48, 61, 0.5f)
        val past = Note(400, 24, 66)  // F#, hidden past the loop
        val c = NoteClip(steps = 16, notes = listOf(c4, sharp, e4, g4), hidden = listOf(past))
        assertEquals(2, NoteEdits.outOfScale(c.notes + past, cMajor))
        val fitted = NoteEdits.fitToScale(c, cMajor)
        assertEquals(listOf(Note(0, 48, 60, 0.85f), e4, g4), fitted.notes)
        assertEquals(listOf(65), fitted.hidden.map { it.pitch })
        assertEquals(0, NoteEdits.outOfScale(fitted.notes, cMajor))
        // Without a scale nothing moves.
        assertEquals(c, NoteEdits.fitToScale(c, Harmony()))
        assertEquals(0, NoteEdits.outOfScale(c.notes, Harmony()))
    }

    @Test
    fun aSelectionMovedAgainstAnEdgeKeepsItsShape() {
        // Audit final (lot notes): 4 notes pushed past the loop's start or end stop there together.
        val four = (0 until 4).map { Note(48 + it * 24, 12, 60 + it) }
        val c = NoteClip(steps = 16, notes = four)
        val early = NoteEdits.shift(c, four.toSet(), -200)
        assertEquals(listOf(0, 24, 48, 72), early.clip.notes.map { it.start })
        assertEquals(4, early.selection.size)
        val late = NoteEdits.shift(c, four.toSet(), 1000)
        assertEquals(listOf(311, 335, 359, 383), late.clip.notes.map { it.start })
        // Transposed past the keyboard: nothing moves (a chord never folds onto one note).
        val chord = NoteClip(notes = listOf(Note(0, 24, 120), Note(0, 24, 124), Note(0, 24, 127)))
        val up = NoteEdits.transpose(chord, chord.notes.toSet(), 12)
        assertEquals(listOf(120, 124, 127), up.clip.notes.map { it.pitch })
        assertEquals(3, up.selection.size)
        assertEquals(-120..0, NoteEdits.transposeRange(chord.notes.toSet()))
        assertEquals(listOf(121, 125, 128 - 1), NoteEdits.transpose(chord, setOf(chord.notes[0], chord.notes[1]), 1).clip.notes.map { it.pitch })
    }

    @Test
    fun aLoopOfNotesIsWholeBarsOfTheTimeSignature() {
        // Audit final (lot notes): a one-bar waltz stays 12 steps (it was rounded to 16 and drifted against the drums).
        assertEquals(12, NoteClip(steps = 12).sanitized().steps)
        assertEquals(36, NoteClip(steps = 64).withSteps(36).steps)
        // Files written before (whole 4/4 bars) read the same.
        for (s in NoteClip.LENGTHS_IN_STEPS) assertEquals(s, NoteClip(steps = s).sanitized().steps)
        // A paste past the end in 3/4 grows the loop by 3/4 bars.
        val waltz = NoteClip(steps = 12, notes = listOf(Note(0, 24, 60)))
        val board = NoteEdits.toClipboard(waltz.notes.toSet(), 12 * NoteClip.TICKS_PER_STEP)!!
        assertEquals(24, NoteEdits.pasteOnBar(waltz, board, 1, 12 * NoteClip.TICKS_PER_STEP).clip.steps)
    }

    @Test
    fun pastedTwiceOnTheSamePlaceTheNotesAreThereOnce() {
        val board = NoteEdits.copy(setOf(c4, e4))
        val once = NoteEdits.paste(clip, board, 192)
        val twice = NoteEdits.paste(once.clip, board, 192)
        assertEquals(once.clip.notes.size, twice.clip.notes.size)
        assertEquals(2, twice.selection.size)
        // The longer and louder one is kept.
        val two = NoteClip(notes = listOf(Note(0, 24, 60, 0.5f), Note(0, 48, 60, 0.9f))).sanitized()
        assertEquals(listOf(Note(0, 48, 60, 0.9f)), two.notes)
    }
}
