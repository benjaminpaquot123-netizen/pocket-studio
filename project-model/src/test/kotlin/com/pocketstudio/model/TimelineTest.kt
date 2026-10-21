package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineTest {
    private fun project(): Project {
        var p = Project(
            drums = DrumPattern().toggle(0, 0),
            synthNotes = NoteClip(notes = listOf(Note(0, 24, 60))),
            song = listOf(SongPart(0, 2)),
        ).addTrack("Cordes")!!.addAudioTrack("Voix")!!
        p = p.withTrack(p.track(1).copy(notes = NoteClip(notes = listOf(Note(0, 24, 72)))))
        return p.sanitized()
    }

    @Test
    fun aPartPlaysOnlyItsTracks() {
        var p = project()
        p = p.togglePartTrack(0, SongPart.DRUMS).togglePartTrack(0, SongPart.inst(1)).togglePartTrack(0, SongPart.audio(0)).sanitized()
        val part = p.song[0]
        assertEquals(setOf("drums", "inst:1", "audio:0"), part.muted)
        val v = p.partView(part)
        assertTrue(v.drums.lanes.all { it.hits.isEmpty() })
        assertTrue(v.track(1).notes.notes.isEmpty())
        assertEquals(1, v.synthNotes.notes.size)  // the first track still plays
        assertFalse(part.plays(SongPart.audio(0)))
        // Toggling again brings the track back; the project itself is untouched.
        assertTrue(p.togglePartTrack(0, SongPart.DRUMS).song[0].plays(SongPart.DRUMS))
        assertEquals(1, p.drums.lanes[0].hits.size)
    }

    @Test
    fun mutesOfDeletedTracksAreForgotten() {
        var p = project().togglePartTrack(0, SongPart.inst(1)).sanitized()
        p = p.removeTrack(1).sanitized()
        assertTrue(p.song[0].muted.isEmpty())
        assertEquals(p, ProjectJson.decode(ProjectJson.encode(p)))
    }

    @Test
    fun threeFourBarsAndSceneLengths() {
        val p = Project(beatsPerBar = 3, drums = DrumPattern().withSteps(24))
        assertEquals(12, p.stepsPerBar)
        assertEquals(24, p.sanitized().drums.steps)
        assertEquals(24, p.sceneSteps(0))
        // A 16-step piano roll in 3/4 makes a scene of two 3/4 bars.
        assertEquals(24, p.copy(drums = DrumPattern().withSteps(12)).sceneSteps(0))
        assertEquals(7, Project(beatsPerBar = 11).sanitized().beatsPerBar)
    }

    @Test
    fun aNewTimeSignatureConvertsEveryScene() {
        var p = Project(drums = DrumPattern().withSteps(32)).copySceneTo(1)
        p = p.switchScene(1).copy(drums = DrumPattern().withSteps(16)).sanitized()
        val waltz = p.withBeatsPerBar(3)
        assertEquals(3, waltz.beatsPerBar)
        assertEquals(listOf(24, 12), waltz.allScenes.map { it.drums.steps })  // 2 bars, then 1 bar of 3/4
        // 7/4: 28 steps a bar, 64 at most → two bars become 56.
        assertEquals(56, p.switchScene(0).withBeatsPerBar(7).drums.steps)
    }

    @Test
    fun midiExportLeavesOutMutedTracksAndKeepsTheMeter() {
        val p = project().copy(beatsPerBar = 3).togglePartTrack(0, SongPart.DRUMS).sanitized()
        val content = MidiFile.read(MidiFile.export(p, song = true))
        assertTrue(content.notes.none { it.channel == MidiFile.DRUM_CHANNEL })
        // Two repeats of a two-bar 3/4 scene (48 steps in all): the 16-step clip runs on across the
        // repeats as the engine plays it, starting at 0, 16 and 32.
        assertEquals(listOf(0, 16, 32), content.notes.filter { it.channel == 0 }.map { it.start / NoteClip.TICKS_PER_STEP })
    }

    @Test
    fun tryingThreeFourThenBackKeepsTheBeat() {
        var drums = DrumPattern()
        for (s in 0 until 16 step 2) drums = drums.toggle(5, s)
        drums = drums.toggle(0, 0).toggle(0, 12).toggle(2, 4).toggle(2, 12)
        val p = Project(drums = drums).sanitized()
        val waltz = p.withBeatsPerBar(3)
        assertEquals(12, waltz.drums.steps)
        // The fourth beat waits past the loop's end (unheard), and comes back with 4/4.
        assertEquals(p.drums, waltz.withBeatsPerBar(4).drums)
        assertEquals(p.drums, ProjectJson.decode(ProjectJson.encode(waltz)).withBeatsPerBar(4).drums)
        // Two bars of 7/4 through 4/4 come back whole.
        val seven = Project(beatsPerBar = 7, drums = DrumPattern(steps = 56).toggle(0, 0).toggle(0, 28).toggle(3, 55)).sanitized()
        assertEquals(32, seven.withBeatsPerBar(4).drums.steps)
        assertEquals(seven.drums, seven.withBeatsPerBar(4).withBeatsPerBar(7).drums)
        // A rotation only moves what the loop plays.
        val turned = waltz.drums.rotate(0, 1)
        assertEquals(setOf(1, 12), turned.lanes[0].hits.keys)
    }

    @Test
    fun aLongerBarRepeatsTheGrooveInsteadOfSilence() {
        val drums = DrumPattern().toggle(0, 0).toggle(0, 4).toggle(0, 8).toggle(0, 12).toggle(2, 6)
        val five = Project(drums = drums).sanitized().withBeatsPerBar(5)
        assertEquals(20, five.drums.steps)
        assertEquals(setOf(0, 4, 8, 12, 16), five.drums.lanes[0].hits.keys)
        assertEquals(setOf(6), five.drums.lanes[2].hits.keys)
    }

    @Test
    fun anAudioClipKeepsItsBarWithANewTimeSignature() {
        var p = Project().addAudioTrack("Voix")!!
        p = p.copy(audioTracks = p.audioTracks.map { it.copy(clip = AudioClip("voix.wav", "Voix", seconds = 4.0, start = 16, loopSteps = 32)) }).sanitized()
        val clip = p.withBeatsPerBar(3).audioTracks[0].clip!!
        assertEquals(12, clip.start)  // bar 2 of 3/4
        assertEquals(24, clip.loopSteps)  // still two bars
        assertEquals(p.audioTracks, p.withBeatsPerBar(3).withBeatsPerBar(4).audioTracks)
    }

    @Test
    fun aTapOnAFarSlotLeavesNoTrailOfEmptyScenes() {
        val p = Project(drums = DrumPattern().toggle(0, 0)).sanitized()
        val far = p.switchScene(23)
        assertEquals(24, far.allScenes.size)
        assertEquals(1, far.switchScene(0).allScenes.size)
        // A scene with something in it stays, and so does the empty one the song plays.
        val filled = far.copy(drums = far.drums.toggle(1, 0)).switchScene(0)
        assertEquals(24, filled.allScenes.size)
        assertEquals(6, far.copy(song = listOf(SongPart(0), SongPart(5))).switchScene(0).allScenes.size)
    }

    @Test
    fun aCopiedSceneTakesItsAutomationAlong() {
        val lane = AutomationLane(AutoTarget(AutoTarget.VOLUME), mode = AutomationLane.PER_SCENE).withPoint(3, 0.5f, scene = 0)
        val p = Project(drums = DrumPattern().toggle(0, 0), automation = listOf(lane)).sanitized()
        val copy = p.copySceneTo(1)
        assertEquals(2, copy.allScenes.size)
        assertEquals(0.5f, copy.automation[0].valuesFor(1)[3])
        // A scene without its own steps gives none to the copy.
        val bare = copy.switchScene(1).copy(automation = listOf(lane)).copySceneTo(2)
        assertEquals(null, bare.automation[0].sceneValues[2])
    }

    @Test
    fun theSongGoesOnInOrderWhenAPartIsRemovedWhilePlaying() {
        // A B C D, C playing (2).
        assertEquals(1, songIndexAfterRemoval(playing = 2, removed = 0, sizeAfter = 3))  // B C D: C is now 1
        assertEquals(2, songIndexAfterRemoval(playing = 2, removed = 3, sizeAfter = 3))  // A B C: C still 2
        // C itself goes: B is "before" what comes next, so D still follows.
        assertEquals(1, songIndexAfterRemoval(playing = 2, removed = 2, sizeAfter = 3))
        // A goes while it plays: the song wraps round, B (now 0) comes next.
        assertEquals(2, songIndexAfterRemoval(playing = 0, removed = 0, sizeAfter = 3))
        assertEquals(-1, songIndexAfterRemoval(playing = 0, removed = 0, sizeAfter = 0))
    }

    @Test
    fun afterAnUndoTheSongFollowsTheSceneHeard() {
        val song = listOf(SongPart(0), SongPart(1), SongPart(2), SongPart(1))
        assertEquals(3, followSongIndex(song, 3, 1))
        assertEquals(1, followSongIndex(song, 0, 1))  // the first part from there that plays it
        assertEquals(3, followSongIndex(song, 7, 1))  // past the end (a part was undone away)
        assertEquals(-1, followSongIndex(song, 0, 5))
        assertEquals(-1, followSongIndex(emptyList(), 0, 0))
    }
}
