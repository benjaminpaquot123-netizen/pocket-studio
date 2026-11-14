package com.pocketstudio.model

import com.pocketstudio.model.StarterWizard.Part
import com.pocketstudio.model.tuto.TutoTarget
import org.junit.Assert.assertTrue
import org.junit.Test

/** Benjamin's list n° 5 (choice 1A): an empty project opens on the pads, a song on « Mon morceau ». */
class OpenOnTest {
    @Test
    fun anEmptyProjectOpensOnThePads() {
        assertTrue("« Projet vide »", OpenOn.pads(Project(name = "")))
        assertTrue("a new project", OpenOn.pads(Project()))
        // A tutorial's project: the style's drum sounds on the pads, not a hit.
        assertTrue("a tutorial's blank", OpenOn.pads(TutoTarget.of("jump_up")!!.blank()))
    }

    @Test
    fun songPartsOverEmptyScenesAreNothingToHear() {
        val p = Project(name = "").let { it.copy(scenes = listOf(it.currentSceneContent()), song = listOf(SongPart(0), SongPart(0, repeats = 2))) }
        assertTrue(OpenOn.pads(p))
    }

    @Test
    fun aBeatNotYetInTheSongOpensOnThePadsWhereItIs() {
        val p = Project(name = "").let { it.copy(drums = it.drums.fill(0, 4)) }
        assertTrue(OpenOn.pads(p))
    }

    @Test
    fun aSongWithAHitANoteOrA303StepOpensOnIt() {
        val blank = Project(name = "")
        val beat = blank.copy(drums = blank.drums.fill(0, 4))
        assertTrue("drums", OpenOn.song(beat.copy(scenes = listOf(beat.currentSceneContent()), song = listOf(SongPart(0)))))
        val notes = blank.copy(synthNotes = NoteClip(notes = listOf(Note(0, 4, 60))))
        assertTrue("notes", OpenOn.song(notes.copy(scenes = listOf(notes.currentSceneContent()), song = listOf(SongPart(0)))))
        val acid = blank.copy(acid = blank.acid.copy(steps = blank.acid.steps.mapIndexed { i, s -> if (i == 0) s.copy(gate = true) else s }))
        assertTrue("303", OpenOn.song(acid.copy(scenes = listOf(acid.currentSceneContent()), song = listOf(SongPart(0)))))
    }

    @Test
    fun anAudioClipOrASeparatedSongIsSomethingToHear() {
        val withTrack = Project(name = "").addAudioTrack()!!
        assertTrue("an audio track without a clip", OpenOn.pads(withTrack))
        val clip = withTrack.copy(audioTracks = withTrack.audioTracks.map { it.copy(clip = AudioClip("take.wav", "Prise", seconds = 2.0)) })
        assertTrue("a clip", OpenOn.song(clip))
        assertTrue("a remix", OpenOn.song(Project(name = "").copy(remix = Remix(setId = "set1"))))
    }

    @Test
    fun theAssistantsSongsAndTheDemosOpenOnTheSong() {
        StarterWizard.STYLES.forEach { s ->
            val lines = s.lines.filter { !it.off }.associate { it.role to null as PadSample? }
            val parts: Set<Part> = StarterWizard.defaultParts(s)
            val p = StarterWizard.build(s, lines, s.melody.mood, parts.associateWith { StarterWizard.soundsFor(it, s).first() })
            assertTrue(s.id, OpenOn.song(p))
        }
        Demos.ALL.forEach { (title, _, make) -> val p = make(); if (p.song.isNotEmpty()) assertTrue(title, OpenOn.song(p)) }
    }
}
