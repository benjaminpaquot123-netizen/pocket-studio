package com.pocketstudio.app.onboarding

import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.ui.AppTab
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.PadSample
import com.pocketstudio.model.Project
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.addTrack
import com.pocketstudio.model.copySceneTo
import com.pocketstudio.model.renameScene
import com.pocketstudio.model.switchScene
import com.pocketstudio.model.track
import com.pocketstudio.model.withTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NextStepsTest {
    private fun List<NextStep>.done() = filter { it.done }.map { it.goal }.toSet()

    private fun Project.withNotes(id: Int, vararg pitches: Int): Project =
        withTrack(track(id).copy(notes = NoteClip(notes = pitches.mapIndexed { i, p -> Note(i * 24, 12, p) })))

    @Test
    fun anEmptyProjectStartsWithTheRhythm() {
        val steps = nextSteps(Project(name = ""))
        assertEquals(StepGoal.entries, steps.map { it.goal })
        assertEquals(emptySet<StepGoal>(), steps.done())
        assertEquals(StepGoal.Rhythm, steps.next()!!.goal)
        // Each step says what to do in one line and where.
        assertTrue(steps.all { it.hint.isNotBlank() && it.button.isNotBlank() && '\n' !in it.hint })
        assertEquals(
            listOf(AppTab.Rythme, AppTab.Acide, AppTab.Synthe, AppTab.Morceau, AppTab.Morceau, AppTab.Mixeur, AppTab.Mixeur),
            steps.map { it.tab },
        )
        assertEquals((1..7).toList(), steps.map { it.number })
    }

    @Test
    fun theStarterBeatHasItsRhythmAndItsBass() {
        // Drum hits and an acid line: steps 1 and 2 are done, the melody comes next.
        val steps = nextSteps(RhythmViewModel.starterProject())
        assertEquals(setOf(StepGoal.Rhythm, StepGoal.Bass), steps.done())
        assertEquals(StepGoal.Melody, steps.next()!!.goal)
    }

    @Test
    fun aLowTrackIsABassAndAHighOneAMelody() {
        val low = Project().withNotes(0, 36, 38, 41)
        assertEquals(setOf(StepGoal.Bass), nextSteps(low).done())
        val high = Project().withNotes(0, 60, 64, 67)
        assertEquals(setOf(StepGoal.Melody), nextSteps(high).done())
        // A track named "Basse" is the bass whatever its notes; another one plays the melody.
        val both = Project().withTrack(Project().track(0).copy(name = "Basse")).withNotes(0, 60, 62).addTrack("Lead")!!
        val withLead = both.withNotes(both.tracks.last().id, 72, 76)
        assertEquals(setOf(StepGoal.Bass, StepGoal.Melody), nextSteps(withLead).done())
    }

    @Test
    fun sectionsNeedTwoFilledScenesWithNames() {
        val beat = RhythmViewModel.starterProject()
        val two = beat.copySceneTo(1)
        val unnamed = nextSteps(two).first { it.goal == StepGoal.Sections }
        assertFalse(unnamed.done)
        assertTrue(unnamed.hint, unnamed.hint.contains("nom"))
        // One scene only: the hint asks for a second one first.
        assertTrue(nextSteps(beat).first { it.goal == StepGoal.Sections }.hint.contains("deuxième section"))
        val named = two.renameScene(0, "Couplet").renameScene(1, "Refrain")
        assertTrue(nextSteps(named).first { it.goal == StepGoal.Sections }.done)
        // The song: three parts or more.
        assertFalse(nextSteps(named.copy(song = listOf(SongPart(0), SongPart(1)))).first { it.goal == StepGoal.Song }.done)
        assertTrue(nextSteps(named.copy(song = listOf(SongPart(0), SongPart(1), SongPart(0)))).first { it.goal == StepGoal.Song }.done)
    }

    @Test
    fun theVolumesAreDoneOnceTheMixerIsTouchedOrTicked() {
        val p = Project()
        assertFalse(nextSteps(p).first { it.goal == StepGoal.Volumes }.done)
        val mixed = p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.mapIndexed { i, c -> if (i == 0) c.copy(volume = 0.6f) else c }))
        assertTrue(nextSteps(mixed).first { it.goal == StepGoal.Volumes }.done)
        assertTrue(nextSteps(p, setOf(StepGoal.Volumes)).first { it.goal == StepGoal.Volumes }.done)
        // Sharing cannot be seen in the project: only the user's tick says it.
        val share = nextSteps(p, setOf(StepGoal.Share)).first { it.goal == StepGoal.Share }
        assertTrue(share.done && share.manual)
    }

    @Test
    fun theGuidedSongLeavesOnlyTheMixAndTheSharing() {
        val style = StarterWizard.style("house")!!
        val parts = StarterWizard.Part.entries.associateWith { StarterWizard.soundsFor(it, style).first() }
        val song = StarterWizard.build(style, style.lines.associate { it.role to null as PadSample? }, StarterWizard.Mood.Happy, parts)
        val steps = nextSteps(song)
        assertEquals(setOf(StepGoal.Rhythm, StepGoal.Bass, StepGoal.Melody, StepGoal.Sections, StepGoal.Song), steps.done())
        assertEquals(StepGoal.Volumes, steps.next()!!.goal)
        // Everything done: no next step (the card hides).
        assertNull(nextSteps(song, setOf(StepGoal.Volumes, StepGoal.Share)).next())
        // Still true on another scene shown.
        assertEquals(steps.done(), nextSteps(song.switchScene(2)).done())
    }
}
