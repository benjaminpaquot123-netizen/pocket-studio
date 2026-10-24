package com.pocketstudio.model

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.i18n.tr
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The review of phases 35 to 39 (model): each finding, the case it was about. */
class Review3539ModelTest {
    private val bar = 16 * NoteClip.TICKS_PER_STEP
    private val cMajor = Harmony(0, Scale.Major)

    /** Scenes A and B (a kick each), the song A B B. */
    private fun song(): Project {
        fun kick(step: Int) = DrumPattern().let { d -> d.copy(lanes = d.lanes.toMutableList().also { it[0] = it[0].copy(hits = mapOf(step to Hit())) }) }
        return Project(scenes = listOf(Scene("A", drums = kick(0)), Scene("B", drums = kick(4))), drums = kick(0))
            .copy(song = listOf(SongPart(0), SongPart(1, repeats = 2))).sanitized()
    }

    // --- Freezing -------------------------------------------------------------------------------

    @Test
    fun aRemovedTrackTakesItsFrozenSoundAlong() {
        val p = song().addTrack()!!
        val frozen = Freeze.frozen(p, 1, "frozen1.wav", 12.0, "Gelée")!!
        assertTrue(Freeze.isFrozen(frozen, 1))
        val removed = frozen.removeTrack(1)
        assertTrue(removed.audioTracks.none { it.frozenFrom == 1 })
        // A new track given the same id is not frozen.
        val again = removed.addTrack()!!
        assertTrue(again.tracks.any { it.id == 1 })
        assertFalse(Freeze.isFrozen(again, 1))
        assertTrue(Freeze.frozenIds(again).isEmpty())
        // A file whose frozen sound names a track that is not there: an ordinary audio track.
        val orphan = frozen.copy(tracks = emptyList()).sanitized()
        assertNull(orphan.audioTracks.single().frozenFrom)
    }

    @Test
    fun theFreezeSignatureDoesNotChangeFromOneLaunchToTheNext() {
        // An arpeggio's mode is an enum: its hashCode() differs at every launch of the app. The
        // signature of this project is the same number on every run (and on every machine).
        val clip = NoteClip(16, listOf(Note(0, 24, 60)), performance = Performance(arp = ArpMode.Up, arpRate = 12))
        val p = Project(synthNotes = clip, song = listOf(SongPart(0))).sanitized()
        assertEquals(EXPECTED_SIGNATURE, Freeze.signature(p, 0))
    }

    @Test
    fun aTakeOnAFrozenSoundIsNoLongerFrozen() {
        val p = song().addTrack()!!
        val frozen = Freeze.frozen(p, 1, "frozen1.wav", 12.0, "Gelée")!!
        val audio = frozen.audioTracks.single { it.frozenFrom == 1 }
        val take = PendingTake(audio.id, "take1.wav", "Prise", 4.0, 120.0, replaces = "frozen1.wav")
        val taken = frozen.withTake(take)
        assertEquals("take1.wav", taken.audioTracks.single { it.id == audio.id }.clip!!.file)
        assertFalse(Freeze.isFrozen(taken, 1))
        // Back to the frozen render by the take picker: still not the frozen track (its signature is gone).
        val chosen = taken.chooseTake(audio.id, 0)
        assertEquals("frozen1.wav", chosen.audioTracks.single { it.id == audio.id }.clip!!.file)
        assertFalse(Freeze.isFrozen(chosen, 1))
    }

    // --- Chords and notes -----------------------------------------------------------------------

    @Test
    fun notesHiddenPastAWrittenOutLoopDoNotComeBackInTheOldKey() {
        // A 1-bar loop shortened from 2 bars: a note (Fa) hidden in what was its second bar.
        val clip = NoteClip(16, listOf(Note(0, 96, 60), Note(192, 96, 64)), hidden = listOf(Note(bar + 96, 96, 65))).sanitized()
        assertEquals(1, clip.hidden.size)
        val p = Project(harmony = cMajor, synthNotes = clip).sanitized()
        // Under I IV V I the loop is written out over the 4 bars of the scene.
        val out = ChordTrack.reharmonized(p, clip, 64, listOf(0, 0, 0, 0), listOf(0, 3, 4, 0))
        assertEquals(64, out.steps)
        assertEquals(8, out.notes.size)  // the two notes, four times: the hidden Fa is not among them
        assertTrue(out.notes.none { it.start == bar + 96 && it.pitch == 65 })
        assertTrue(out.hidden.isEmpty())
    }

    @Test
    fun aReharmonizationNeverLosesNotesPastTheLimit() {
        // 200 notes in a 1-bar loop: written out over 4 bars it would be 800 (more than MAX_NOTES).
        val many = (0 until 200).map { Note(it, 1, 48 + it % 24) }
        val clip = NoteClip(16, many).sanitized()
        assertEquals(200, clip.notes.size)
        val p = Project(harmony = cMajor, synthNotes = clip).sanitized()
        val out = ChordTrack.reharmonized(p, clip, 64, listOf(0, 0, 0, 0), listOf(0, 3, 4, 0))
        assertEquals(200, out.notes.size)
        assertEquals(16, out.steps)
    }

    @Test
    fun aPasteOrAKeptTakeNeverPushesOutNotesAlreadyThere() {
        // 500 notes late in a 4-bar loop; 30 pasted at its start would come first in the clean-up.
        val late = (0 until 500).map { Note(2 * bar + it, 1, 60 + it % 12) }
        val clip = NoteClip(64, late).sanitized()
        assertEquals(500, clip.notes.size)
        val board = (0 until 30).map { Note(it * 4, 2, 72) }
        val pasted = NoteEdits.paste(clip, board, 0)
        assertTrue(pasted.clip.notes.containsAll(late))
        assertEquals(12, pasted.selection.size)
        assertEquals(18, pasted.dropped)
        // « Garder ce que je viens de jouer » on the same clip.
        val capture = PlayedMemory.Capture((0 until 30).map { PlayedMemory.KeptNote(0, 72, 0.8f, it * 0.5, 0.5) }, emptyList(), 0, 1, playing = true)
        val kept = clip.withCaptured(capture, 0, 16)!!
        assertTrue(kept.notes.containsAll(late))
        assertEquals(NoteClip.MAX_NOTES, kept.notes.size)
    }

    @Test
    fun whereANoteWasPlayedFollowsTheLoopLength() {
        // Played a hair before the loop came round, put on its first beat (« Remettre en rythme »).
        val played = NoteClip(16, listOf(Note(bar - 4, 12, 60))).inTime({ true }, NoteClip.TICKS_PER_STEP * 4, 1f)
        val n = played.notes.single()
        assertEquals(0, n.start)
        assertEquals(bar - 4, n.played)
        // The loop doubled: « Comme joué » puts it just before the new loop's end, not in its middle.
        val longer = played.withSteps(32)
        assertEquals(2 * bar - 4, longer.notes.single().played)
        assertEquals(2 * bar - 4, longer.asPlayed { true }.notes.single().start)
        // Written out by the chords (4 passes): each copy keeps where it was played, the last at the end.
        val p = Project(harmony = cMajor, synthNotes = played).sanitized()
        val out = ChordTrack.reharmonized(p, played, 64, listOf(0, 0, 0, 0), listOf(0, 3, 4, 0))
        assertEquals(listOf(4 * bar - 4, bar - 4, 2 * bar - 4, 3 * bar - 4), out.notes.sortedBy { it.start }.map { it.played })
    }

    @Test
    fun aCaptureOfNoBarOrOfMoreBarsThanALoopHolds() {
        val memory = PlayedMemory()
        memory.noteOn(0, 60, 0.8f, 1.0)
        memory.noteOff(0, 60, 2.0)
        // No bar asked: one, no crash.
        assertEquals(1, memory.capture(4.0, 0, 16).bars)
        // 12 bars of 7/4 (28 sixteenths): the longest loop holds 9; the last 9 are kept, unfolded.
        val notes = (0 until 12).map { b -> PlayedMemory.KeptNote(0, 60 + b, 0.8f, b * 28.0, 4.0) }
        val clip = NoteClip(16).withCaptured(PlayedMemory.Capture(notes, emptyList(), 0, 12, playing = false), 0, 28)!!
        assertEquals((63..71).toList(), clip.notes.sortedBy { it.start }.map { it.pitch })
        assertEquals(clip.notes.size, clip.notes.map { it.start }.distinct().size)
    }

    // --- Takes ----------------------------------------------------------------------------------

    @Test
    fun theNinthTakeSaysWhichOneGoes() {
        var p = Project()
        var last: String? = null
        repeat(AudioTrack.MAX_TAKES) { i ->
            val take = PendingTake(0, "t$i.wav", "Prise ${i + 1}", 4.0, 120.0, replaces = last)
            assertNull(p.takeForgets(take))
            p = p.withTake(take)
            last = "t$i.wav"
        }
        assertEquals(AudioTrack.MAX_TAKES - 1, p.audioTracks.single().otherTakes.size)
        val ninth = PendingTake(0, "t8.wav", "Prise 9", 4.0, 120.0, replaces = last)
        assertEquals("t0.wav", p.takeForgets(ninth)?.file)
        val after = p.withTake(ninth)
        assertTrue(after.audioTracks.single().otherTakes.none { it.file == "t0.wav" })
    }

    // --- Versions of the song -------------------------------------------------------------------

    @Test
    fun aRestoreBringsBackTheSongCurvesOnly() {
        val target = AutoTarget(AutoTarget.VOLUME, 0)
        val trackTarget = AutoTarget(AutoTarget.VOLUME, InstrumentTrack.mixerChannel(1))
        val withTrack = song().addTrack()!!
        val p = withTrack.copy(
            automation = listOf(
                AutomationLane(target, songValues = listOf(0.5f)),
                AutomationLane(trackTarget, songValues = listOf(0.2f)),
            ),
        ).sanitized()
        val kept = p.withVersionKept(SongVersion.BEFORE_TAKE, 1L)
        // Since: the drums' loop curve drawn, their song curve changed, track 1 deleted.
        val loop = List(AutomationLane.STEPS) { if (it == 0) 0.9f else null }
        val edited = kept.copy(automation = kept.automation.map { if (it.target == target) it.copy(values = loop, songValues = listOf(0.1f)) else it })
            .removeTrack(1).copy(song = listOf(SongPart(1))).sanitized()
        val back = edited.restoreVersion(0, 2L)
        val lane = back.automation.single { it.target == target }
        assertEquals(listOf(0.5f), lane.songValues)
        assertEquals(loop, lane.values)
        assertTrue(back.automation.none { it.target == trackTarget })
    }

    @Test
    fun aVersionIsKeptInRangeAndNeverTwiceTheSame() {
        val v = SongVersion(scenes = List(30) { Scene() }, scene = 28).sanitized()
        assertEquals(Scene.MAX, v.scenes.size)
        assertEquals(Scene.MAX - 1, v.scene)
        val p = song()
        val twice = p.withVersionKept(SongVersion.BEFORE_TAKE, 1L).withVersionKept(SongVersion.BEFORE_TAKE, 2L)
        assertEquals(1, twice.songVersions.size)
        val w = TakeWritten(twice, parts = 1, settings = 0)
        assertEquals(1, twice.keepingVersion(w, 3L).project.songVersions.size)
        // A part played a version stored with parts on scenes it does not have: cleaned when read.
        val damaged = p.copy(songVersions = listOf(SongVersion(song = listOf(SongPart(0), SongPart(7)), scenes = listOf(Scene())))).sanitized()
        assertEquals(listOf(0), damaged.songVersions.single().song.map { it.scene })
    }

    @Test
    fun aTakeBeforeThereIsASongStillKeepsTheScenes() {
        // No song yet, notes in scene A: a take writes a song (and its notes): the scenes come back.
        val p = Project(synthNotes = NoteClip(16, listOf(Note(0, 24, 72)))).sanitized()
        assertTrue(p.song.isEmpty())
        val written = p.copy(song = listOf(SongPart(0)), synthNotes = NoteClip(16, listOf(Note(96, 24, 74)))).sanitized()
        val w = p.keepingVersion(TakeWritten(written, parts = 1, settings = 0), 1L)
        assertEquals(1, w.project.songVersions.size)
        assertEquals(listOf(72), w.project.songVersions.single().scenes[0].synthNotes.notes.map { it.pitch })
        // An empty project: nothing to keep.
        assertTrue(Project().keepingVersion(TakeWritten(written, parts = 1, settings = 0), 1L).project.songVersions.isEmpty())
    }

    // --- Scenes ---------------------------------------------------------------------------------

    @Test
    fun aSceneWithOnlyChordsIsNotEmpty() {
        assertTrue(Scene().isEmpty)
        assertFalse(Scene(chords = listOf(0, 3)).isEmpty)
    }

    @Test
    fun aClearedSceneAndAFillPlayStraight() {
        val lanes = DrumPattern().lanes.toMutableList().also { it[2] = it[2].copy(hits = mapOf(0 to Hit(), 3 to Hit()), length = 3) }
        val p = Project(drums = DrumPattern(lanes = lanes), song = listOf(SongPart(0, repeats = 2))).sanitized()
        assertEquals(3, p.drums.lanes[2].length)
        assertEquals(0, p.clearCurrentScene().drums.lanes[2].length)
        val fill = p.fillAtEnd(0)
        val fillScene = fill.allScenes.first { it.label == PartIntents.FILL_LABEL }
        assertTrue(fillScene.drums.lanes.all { it.length == 0 })
    }

    // --- Mode simple ----------------------------------------------------------------------------

    @Test
    fun theHelpFindsItsWordWithTheEverydayWordsOn() {
        val lang = I18n.lang
        val everyday = I18n.everyday
        try {
            for (l in listOf(Lang.FR, Lang.EN, Lang.NL)) {
                I18n.lang = l
                I18n.everyday = true
                val shown = tr("Lead")
                assertEquals(l.name, "Lead", I18n.frenchOf(shown))
                assertNotNull(l.name, com.pocketstudio.model.learn.SoundGlossary.forHelp(I18n.frenchOf(tr("Quantifier"))))
            }
        } finally {
            I18n.lang = lang
            I18n.everyday = everyday
        }
    }

    companion object {
        /** [Freeze.signature] of [theFreezeSignatureDoesNotChangeFromOneLaunchToTheNext]'s project. */
        const val EXPECTED_SIGNATURE = -787936358
    }
}
