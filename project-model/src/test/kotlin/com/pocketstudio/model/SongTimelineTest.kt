package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SongTimelineTest {
    /** Scene A: a kick on 0 and 8, a snare on 4; the piano roll C4 then G4; the 303 on 0 and 2. */
    private fun project(): Project {
        val acid = AcidPattern().let { a -> a.copy(steps = a.steps.mapIndexed { i, s -> if (i == 0 || i == 2) s.copy(gate = true, note = i) else s }) }
        var p = Project(
            drums = DrumPattern().toggle(0, 0).toggle(0, 8).toggle(2, 4),
            synthNotes = NoteClip(steps = 16, notes = listOf(Note(0, 4, 60), Note(8, 4, 67))),
            acid = acid,
        ).sanitized()
        p = p.copySceneTo(1).switchScene(1).copy(drums = DrumPattern().withSteps(32)).sanitized().switchScene(0)
        return p.copy(song = listOf(SongPart(0, 4), SongPart(1, 1))).sanitized()
    }

    @Test
    fun aClipPreviewShowsWhatItsTrackPlays() {
        val p = project()
        val drums = p.clipPreview(0, SongPart.DRUMS)
        assertEquals(16, drums.steps)
        assertEquals(listOf(0, 4, 8), drums.marks.map { it.start }.sorted())
        // Two pads used: the kick's row under the snare's.
        val kickY = drums.marks.first { it.start == 0 }.y
        val snareY = drums.marks.first { it.start == 4 }.y
        assertTrue(kickY > snareY)
        val notes = p.clipPreview(0, SongPart.inst(0))
        assertEquals(listOf(0, 8), notes.marks.map { it.start })
        assertEquals(4, notes.marks[0].length)
        // G4 drawn above C4.
        assertTrue(notes.marks[1].y < notes.marks[0].y)
        val acid = p.clipPreview(0, SongPart.ACID)
        assertEquals(listOf(0, 2), acid.marks.map { it.start })
        // Scene B: an empty 2-bar beat.
        assertTrue(p.clipPreview(1, SongPart.DRUMS).isEmpty)
        assertEquals(32, p.clipPreview(1, SongPart.DRUMS).steps)
        // An audio track, a track that is not there, a scene that is not there: nothing.
        assertTrue(p.clipPreview(0, SongPart.audio(0)).isEmpty)
        assertTrue(p.clipPreview(0, SongPart.inst(5)).isEmpty)
        assertTrue(p.clipPreview(9, SongPart.DRUMS).isEmpty)
    }

    @Test
    fun theShownSceneIsPreviewedAsItIsNow() {
        // An edit not yet mirrored into the scene list is still what the clip shows.
        val p = project()
        val edited = p.copy(drums = p.drums.toggle(5, 2))
        assertTrue(edited.clipPreview(0, SongPart.DRUMS).marks.any { it.start == 2 })
    }

    @Test
    fun partsHaveTheirPlaceOnTheTimeline() {
        val p = project()
        assertEquals(listOf(PartSpan(0, 0, 64), PartSpan(1, 64, 32)), p.partSpans)
        assertEquals(0, p.partAtStep(0))
        assertEquals(0, p.partAtStep(63))
        assertEquals(1, p.partAtStep(64))
        assertEquals(-1, p.partAtStep(96))
        assertEquals(-1, p.partAtStep(-1))
    }

    @Test
    fun aRepeatedPartIsCutBetweenTwoRepeats() {
        val p = project()
        val cut = p.splitSongPart(0, 1)
        assertEquals(listOf(SongPart(0, 1), SongPart(0, 3), SongPart(1, 1)), cut.song)
        // The song plays the same: as long, every part where it was.
        assertEquals(p.songBars, cut.songBars)
        assertEquals(p.automation, cut.automation)
        assertEquals(1, p.splitBar(0, 1))
        // A scene of two bars repeated twice: cut on bar 2, never inside a pass.
        val twoBars = p.copy(song = listOf(SongPart(1, 2))).sanitized()
        assertEquals(2, twoBars.splitBar(0, 1))
        assertEquals(listOf(SongPart(1, 1), SongPart(1, 1)), twoBars.splitSongPart(0, 1).song)
        // One pass only: nothing to cut.
        assertSame(p, p.splitSongPart(1, 1))
        assertNull(p.splitBar(1, 1))
    }

    @Test
    fun aPartWithItsOwnLengthIsCutAtAnyBarAndCarriesOn() {
        val p = project().copy(song = listOf(SongPart(0, bars = 6, from = mapOf(SongPart.DRUMS to 1)))).sanitized()
        val cut = p.splitSongPart(0, 2)
        assertEquals(2, cut.song[0].bars)
        assertEquals(4, cut.song[1].bars)
        // The second half picks each track up two bars further on.
        assertEquals(3, cut.song[1].fromBars(SongPart.DRUMS))
        assertEquals(2, cut.song[1].fromBars(SongPart.ACID))
        assertEquals(p.songBars, cut.songBars)
        assertSame(p, p.splitSongPart(0, 6))
        assertSame(p, p.splitSongPart(0, 0))
    }

    @Test
    fun aLoopLeadsBackToItsFirstPart() {
        assertEquals(1, songNextIndexLooped(playing = 2, queued = -1, size = 4, loops = false, loop = 1..2))
        // Inside the loop, the song goes on in order; a part chosen to come next still wins.
        assertEquals(2, songNextIndexLooped(1, -1, 4, false, 1..2))
        assertEquals(3, songNextIndexLooped(2, 3, 4, false, 1..2))
        // No loop, or one out of the song: as before.
        assertEquals(-1, songNextIndexLooped(3, -1, 4, false, null))
        assertEquals(-1, songNextIndexLooped(3, -1, 4, false, 2..5))
        // A loop of one part plays it again and again.
        assertEquals(0, songNextIndexLooped(0, -1, 4, false, 0..0))
        assertEquals(1..3, songLoopOf(3, 1, 4))
        assertNull(songLoopOf(0, 4, 4))
    }

    @Test
    fun aClipSaysWhenItsTrackPlaysAnotherSceneOrCarriesOn() {
        val part = SongPart(0, play = mapOf(SongPart.ACID to 1), from = mapOf(SongPart.DRUMS to 2), bars = 4)
        assertEquals("B", part.trackBadge(SongPart.ACID))
        assertEquals("↪", part.trackBadge(SongPart.DRUMS))
        assertEquals("", part.trackBadge(SongPart.inst(0)))
    }

    @Test
    fun tracksAndMixerChannelsGoBothWays() {
        val keys = listOf(SongPart.DRUMS, SongPart.ACID, SongPart.inst(0), SongPart.inst(3), SongPart.audio(2))
        keys.forEach { k -> assertEquals(k, keyOfChannel(channelOfKey(k)!!)) }
        assertNull(channelOfKey(SongPart.REMIX))
        assertNull(keyOfChannel(Mixer.MASTER))
        assertNull(keyOfChannel(3))
    }

    @Test
    fun songCurvesAreDrawnUnderTheirTrack() {
        val cutoff = AutomationLane(AutoTarget(AutoTarget.ACID, ch = AutoTarget.ACID_CHANNEL), mode = AutomationLane.SONG, songValues = listOf(0f, null, 1f))
        val volume = AutomationLane(AutoTarget(AutoTarget.VOLUME, ch = 0), mode = AutomationLane.SONG, songValues = listOf(0.5f))
        val loop = AutomationLane(AutoTarget(AutoTarget.VOLUME, ch = 2), mode = AutomationLane.LOOP)
        val master = AutomationLane(AutoTarget(AutoTarget.VOLUME, ch = Mixer.MASTER), mode = AutomationLane.SONG, songValues = listOf(0.2f))
        // A track removed since: its curve goes with the whole mix.
        val gone = AutomationLane(AutoTarget(AutoTarget.VOLUME, ch = 7), mode = AutomationLane.SONG, songValues = listOf(0.2f))
        val p = project().copy(automation = listOf(cutoff, volume, loop, master, gone))
        assertEquals(listOf(0), p.songLanesOf(SongPart.ACID))
        assertEquals(listOf(1), p.songLanesOf(SongPart.DRUMS))
        assertEquals(emptyList<Int>(), p.songLanesOf(SongPart.inst(0)))
        assertEquals(listOf(3, 4), p.songLanesOf(null))
    }
}
