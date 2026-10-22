package com.pocketstudio.app.rhythm

import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.AutomationLane
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.LivePads
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.Project
import com.pocketstudio.model.Remix
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.addTrack
import com.pocketstudio.model.switchScene
import org.junit.Assert.assertEquals
import org.junit.Test

class StemExportsTest {
    private val beat = Project(drums = DrumPattern().toggle(0, 0))

    @Test
    fun silentOrMutedStripsGiveNoFile() {
        // Drums only: no empty acid, synth or remix file; live pads never play in an export.
        val live = beat.copy(live = LivePads(clips = listOf(LiveClip("a.wav", "Boucle")) + List(LivePads.MAX - 1) { null }))
        assertEquals(listOf("batterie"), stemExports(live, song = false).map { it.first })
        val full = beat.copy(synthNotes = NoteClip(notes = listOf(Note(0, 4, 60))), remix = Remix(setId = "s"))
        assertEquals(listOf("batterie" to 0, "synthe" to 2, "remix" to 3), stemExports(full, song = false))
        // Muted in the mixer: left out.
        val muted = full.copy(mixer = full.mixer.copy(channels = full.mixer.channels.mapIndexed { i, c -> if (i == 2) c.copy(mute = true) else c }))
        assertEquals(listOf("batterie", "remix"), stemExports(muted, song = false).map { it.first })
        // An added track with no note in the scene: no file.
        val track = beat.addTrack("Basse")!!
        assertEquals(listOf("batterie"), stemExports(track, song = false).map { it.first })
    }

    @Test
    fun aSongCountsEveryPartPlayed() {
        // Scene B has synth notes, scene A only drums; the song plays both.
        val b = beat.switchScene(1).copy(synthNotes = NoteClip(notes = listOf(Note(0, 4, 60))), drums = DrumPattern())
        val song = b.copy(song = listOf(SongPart(0), SongPart(1)))
        assertEquals(listOf("batterie", "synthe"), stemExports(song, song = true).map { it.first })
        // The only part with drums has them muted: no drums file.
        val noDrums = b.copy(song = listOf(SongPart(0, muted = setOf(SongPart.DRUMS)), SongPart(1)))
        assertEquals(listOf("synthe"), stemExports(noDrums, song = true).map { it.first })
    }

    @Test
    fun liveNoisesGetAFileOfTheirOwn() {
        // A take with the riser (a song curve): one more file, « Effets live », after the tracks.
        val riser = AutomationLane(AutoTarget.live(AutoTarget.LIVE_RISER), mode = AutomationLane.SONG, songValues = listOf(0f, 1f, 0f), songOnly = true)
        val song = beat.copy(song = listOf(SongPart(0)), automation = listOf(riser))
        assertEquals(listOf("batterie" to 0, LIVE_FX_STEM_LABEL to LIVE_FX_STEM), stemExports(song, song = true))
        // The scene alone does not play a song-only curve: no such file.
        assertEquals(listOf("batterie" to 0), stemExports(song, song = false))
        // The build-up on a loop lane plays in the scene too; a lane never raised makes no sound.
        val build = AutomationLane(AutoTarget.live(AutoTarget.LIVE_BUILD_UP)).withPoint(4, 0.8f)
        assertEquals(listOf("batterie" to 0, LIVE_FX_STEM_LABEL to LIVE_FX_STEM), stemExports(beat.copy(automation = listOf(build)), song = false))
        val flat = AutomationLane(AutoTarget.live(AutoTarget.LIVE_BUILD_UP)).withPoint(4, 0f)
        assertEquals(listOf("batterie" to 0), stemExports(beat.copy(automation = listOf(flat)), song = false))
        // The DJ filter only changes the mix: nothing of its own.
        val filter = AutomationLane(AutoTarget.live(AutoTarget.LIVE_FILTER)).withPoint(4, 0.9f)
        assertEquals(listOf("batterie" to 0), stemExports(beat.copy(automation = listOf(filter)), song = false))
        // Nothing to export: no « Effets live » alone either.
        assertEquals(emptyList<Pair<String, Int>>(), stemExports(Project(automation = listOf(build)), song = false))
    }
}
