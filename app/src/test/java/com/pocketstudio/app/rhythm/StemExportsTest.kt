package com.pocketstudio.app.rhythm

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
}
