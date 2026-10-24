package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Phase 37, « Accords du morceau »: the song's chords, and the notes that follow them. */
class ChordTrackTest {
    private val bar = 16 * NoteClip.TICKS_PER_STEP
    private val cMajor = Harmony(0, Scale.Major)

    /** A triad held a whole bar on [root] (MIDI) in each bar of [roots]. */
    private fun chords(vararg roots: List<Int>): NoteClip =
        NoteClip(steps = 16 * roots.size, notes = roots.flatMapIndexed { i, ps -> ps.map { Note(i * bar, bar, it) } }).sanitized()

    private fun handMade(clip: NoteClip, follow: Boolean? = null): Project =
        Project(harmony = cMajor, synthNotes = clip, synthFollowChords = follow).sanitized()

    @Test
    fun diatonicStepsKeepTheShape() {
        // Do majeur: Mi two steps up is Sol, Si one step up is Do of the next octave, Do down two is La.
        assertEquals(67, cMajor.shiftDiatonic(64, 2))
        assertEquals(72, cMajor.shiftDiatonic(71, 1))
        assertEquals(57, cMajor.shiftDiatonic(60, -2))
        // A note outside the scale moves as the chord's root does (Do♯ over I, I → IV: + 5 semitones).
        assertEquals(66, cMajor.shiftDiatonic(61, 3, fromDegree = 0))
        // The nearest way round: I → vi is two steps down, I → IV three up.
        assertEquals(-2, ChordTrack.stepsBetween(0, 5))
        assertEquals(3, ChordTrack.stepsBetween(0, 3))
        assertEquals(1, ChordTrack.stepsBetween(6, 0))
    }

    @Test
    fun theChordsAreGuessedFromTheNotes() {
        // C E G, then A C E, then F A C (with F in the bass), then G B D.
        val p = handMade(chords(listOf(60, 64, 67), listOf(57, 60, 64), listOf(53, 57, 60), listOf(55, 59, 62)))
        assertTrue(ChordTrack.isGuessed(p, 0))
        assertEquals(listOf(0, 5, 3, 4), ChordTrack.chordsOf(p, 0))
        assertEquals(listOf("C", "Am", "F", "G"), ChordTrack.chordsOf(p, 0).map { ChordTrack.name(p, it, english = true) })
        // A silent bar keeps the chord before it; an empty scene is the key's I.
        assertEquals(listOf(0), ChordTrack.chordsOf(Project(harmony = cMajor), 0))
    }

    @Test
    fun theAssistantsSongIsReadOnItsProgression() {
        // The assistant writes one chord a bar on its progression: guessed back from its notes.
        val style = StarterWizard.style("house")!!
        val parts = StarterWizard.defaultParts(style).associateWith { StarterWizard.soundsFor(it, style).first() }
        val p = StarterWizard.build(style, style.lines.associate { it.role to null }, style.melody.mood, parts)
        val (_, degrees) = StarterWizard.progressionOf(style, style.melody.mood)
        val expected = degrees.map { Math.floorMod(p.harmony.progressionDegree(it), 7) }
        // Its stabs leave the root to the bass (3-5-7 voicings): read with the bass, every bar.
        for (label in listOf("Groove", "Drop")) {
            val scene = p.allScenes.indexOfFirst { it.label == label }
            assertTrue(label, scene >= 0)
            val read = ChordTrack.chordsOf(p, scene)
            assertEquals("$label: ${read.joinToString()}", List(read.size) { expected[it % expected.size] }, read)
        }
        // Its tracks follow the chords by default (not the riser).
        val followers = ChordTrack.followers(p)
        assertTrue(followers.isNotEmpty())
        p.riserTrackId()?.let { assertFalse(it in followers) }
    }

    @Test
    fun newChordsMoveTheNotesThatFollow() {
        val p = handMade(chords(listOf(60, 64, 67), listOf(60, 64, 67)), follow = true)
        val q = ChordTrack.withChords(p, 0, listOf(5, 3))  // Am, F
        assertEquals(listOf(5, 3), q.allScenes[0].chords)
        assertFalse(ChordTrack.isGuessed(q, 0))
        val notes = q.synthNotes.notes
        assertEquals(listOf(57, 60, 64), notes.filter { it.start == 0 }.map { it.pitch }.sorted())      // A C E
        assertEquals(listOf(65, 69, 72), notes.filter { it.start == bar }.map { it.pitch }.sorted())    // F A C
        // Lengths, strengths and places stay.
        assertTrue(notes.all { it.length == bar })
        assertEquals(p.synthNotes.notes.map { it.start }, notes.map { it.start })
        // And back: the chords written are the old ones now, the notes come back where they were.
        val r = ChordTrack.withChords(q, 0, listOf(0, 0))
        assertEquals(p.synthNotes.notes.map { it.pitch }.sorted(), r.synthNotes.notes.map { it.pitch }.sorted())
    }

    @Test
    fun theUsersOwnNotesStayUnlessAsked() {
        val clip = chords(listOf(60, 64, 67))
        // A project made by hand: nothing follows until the track is told to.
        val mine = handMade(clip)
        assertTrue(ChordTrack.followers(mine).isEmpty())
        val kept = ChordTrack.withChords(mine, 0, listOf(4))
        assertEquals(clip.notes, kept.synthNotes.notes)
        assertEquals(listOf(4), kept.allScenes[0].chords)  // the chord is written all the same
        val asked = ChordTrack.withChords(ChordTrack.withFollow(mine, 0, true), 0, listOf(4))
        assertEquals(listOf(55, 59, 62), asked.synthNotes.notes.map { it.pitch }.sorted())  // G B D, the nearest way (down)
        // An assistant's song: its tracks follow (its 303 too), one the user said no to stays.
        val song = mine.copy(style = "house")
        assertEquals(setOf(0, ChordTrack.ACID), ChordTrack.followers(song))
        assertEquals(setOf(ChordTrack.ACID), ChordTrack.followers(ChordTrack.withFollow(song, 0, false)))
    }

    @Test
    fun aShortLoopIsWrittenOutUnderChangingChords() {
        // A one-bar bass loop (Do on each beat) in a four-bar scene: under I V vi IV it becomes 4 bars.
        val bass = NoteClip(steps = 16, notes = (0 until 4).map { Note(it * 4 * NoteClip.TICKS_PER_STEP, 2 * NoteClip.TICKS_PER_STEP, 36) })
        val drums = DrumPattern().withSteps(64)
        val p = Project(harmony = cMajor, synthNotes = bass, drums = drums, synthFollowChords = true).sanitized()
        assertEquals(4, ChordTrack.bars(p, 0))
        val q = ChordTrack.withProgression(p, Progression.Pop)
        assertEquals(64, q.synthNotes.steps)
        val roots = (0 until 4).map { b -> q.synthNotes.notes.filter { it.start / bar == b }.map { it.pitch }.distinct() }
        assertEquals(listOf(listOf(36), listOf(31), listOf(33), listOf(41)), roots)  // Do, Sol below, La below, Fa
        // The song's chords as written: the progression once (it repeats over the scene).
        assertEquals(listOf(0, 4, 5, 3), q.allScenes[0].chords)
    }

    @Test
    fun theWholeSongTakesTheProgressionInOneEdit() {
        // Two scenes in the song, each with its chords; the progression goes into both.
        var p = handMade(chords(listOf(60, 64, 67)), follow = true)
        p = p.copySceneTo(1)
        p = p.copy(song = listOf(SongPart(0), SongPart(1), SongPart(0))).sanitized()
        val q = ChordTrack.withProgression(p, Progression.Sad)
        val sad = ChordTrack.degreesOf(q, Progression.Sad)
        assertEquals(listOf(5, 3, 0, 4), sad)
        for (s in 0..1) assertEquals(sad.take(1), q.allScenes[s].chords)  // a 1-bar scene: its first chord
        assertEquals(listOf(57, 60, 64), q.allScenes[1].synthNotes.notes.map { it.pitch }.sorted())
        assertEquals(listOf(0, 1), ChordTrack.songScenes(q))
    }

    @Test
    fun theChordsAreSavedOnlyWhenWritten() {
        val p = handMade(chords(listOf(60, 64, 67)))
        val text = ProjectJson.encode(p)
        assertFalse(text, text.contains("\"chords\""))
        assertFalse(text, text.contains("followChords") || text.contains("synthFollowChords"))
        val q = ChordTrack.withChords(ChordTrack.withFollow(p.copy(tracks = listOf(InstrumentTrack(1, "Basse"))), 1, false), 0, listOf(3))
        val back = ProjectJson.decode(ProjectJson.encode(q))
        assertEquals(listOf(3), back.allScenes[0].chords)
        assertEquals(false, back.tracks.first { it.id == 1 }.followChords)
        // A damaged file: degrees brought back on the scale.
        assertEquals(listOf(2, 6), ChordTrack.clean(listOf(9, -1)))
        assertNotEquals(p, q)
    }

    /** A 303 line: Do on each beat (the first accented, the last sliding), Sol an octave up on the offbeats. */
    private fun acidLine(): AcidPattern {
        val steps = List(AcidPattern.MAX_STEPS) { i ->
            when {
                i >= 16 -> AcidStep()
                i % 4 == 0 -> AcidStep(note = 0, accent = i == 0, slide = i == 12, gate = true)
                i % 4 == 2 -> AcidStep(note = 7, octave = 1, gate = true)
                else -> AcidStep()
            }
        }
        return AcidPattern(length = 16, root = 36, steps = steps)
    }

    @Test
    fun theAcidLineFollowsTheChordsByScaleSteps() {
        val drums = DrumPattern().withSteps(64)
        // By hand: the 303 keeps its line until asked; the assistant's follows.
        val mine = Project(harmony = cMajor, acid = acidLine(), drums = drums).sanitized()
        assertFalse(ChordTrack.acidFollows(mine))
        assertTrue(ChordTrack.acidFollows(mine.copy(style = "acid")))
        assertTrue(ChordTrack.acidPlays(mine))
        assertEquals(acidLine().steps, ChordTrack.withProgression(mine, Progression.Pop).acid.steps)
        val p = ChordTrack.withFollow(mine, ChordTrack.ACID, true)
        assertEquals(true, p.acidFollowChords)
        // I V vi IV over the 4 bars: the one-bar line is written out, each bar moved by scale steps.
        val q = ChordTrack.withProgression(p, Progression.Pop)
        assertEquals(64, q.acid.length)
        fun pitch(i: Int) = q.acid.root + q.acid.steps[i].note + 12 * q.acid.steps[i].octave
        // Bar 1 (I) as it was; bar 2 (V): Do 36 → Sol 31 (the nearest way, down), Sol 55 → Ré 50.
        assertEquals(listOf(36, 55), listOf(pitch(0), pitch(2)))
        assertEquals(listOf(31, 50), listOf(pitch(16), pitch(18)))
        // Bar 3 (vi): La 33, Mi 52; bar 4 (IV): Fa 41, Do 60.
        assertEquals(listOf(33, 52), listOf(pitch(32), pitch(34)))
        assertEquals(listOf(41, 60), listOf(pitch(48), pitch(50)))
        // The acid character stays: accents, slides and gates where they were, on every pass.
        for (bar in 0 until 4) {
            val b = bar * 16
            assertTrue(q.acid.steps[b].accent)
            assertTrue(q.acid.steps[b + 12].slide)
            assertEquals((0 until 16).map { acidLine().steps[it].gate }, (0 until 16).map { q.acid.steps[b + it].gate })
        }
        // Inside the 303's range (an octave under its root, three above), and saved only when asked.
        assertTrue(q.acid.steps.all { it.note in 0..12 && it.octave in -1..2 })
        assertFalse(ProjectJson.encode(mine).contains("acidFollowChords"))
        assertEquals(true, ProjectJson.decode(ProjectJson.encode(q)).acidFollowChords)
    }
}
