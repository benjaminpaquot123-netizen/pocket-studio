package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneLabelTest {
    @Test
    fun aSceneKeepsItsSectionNameThroughSwitchesEditsAndSaving() {
        var p = Project().sanitized().renameScene(0, "  Intro  ")
        assertEquals("A · Intro", p.sceneTitle(0))
        p = p.copySceneTo(1).switchScene(1).renameScene(1, "Refrain").switchScene(0)
        assertEquals("Intro", p.sceneLabel(0))
        assertEquals("Refrain", p.sceneLabel(1))
        // Editing the scene shown and clearing it keep its name.
        p = p.copy(drums = p.drums.toggle(0, 3)).sanitized().clearCurrentScene()
        assertEquals("A · Intro", p.sceneTitle(0))
        val back = ProjectJson.decode(ProjectJson.encode(p))
        assertEquals(listOf("Intro", "Refrain"), back.allScenes.map { it.label })
        // A blank name goes back to the letter alone; names are capped.
        assertEquals("B", p.renameScene(1, " ").sceneTitle(1))
        assertEquals(Scene.MAX_LABEL, p.renameScene(1, "x".repeat(50)).sceneLabel(1).length)
    }

    @Test
    fun aNamedEmptySceneStaysAndACopyIsNotTakenForAnotherSection() {
        val p = Project().sanitized().copy(drums = DrumPattern().toggle(0, 0)).sanitized()
            .switchScene(2).renameScene(2, "Fin").switchScene(0)
        assertEquals(3, p.allScenes.size)
        assertEquals("C · Fin", p.sceneTitle(2))
        val copy = p.copySceneTo(1).renameScene(1, "Couplet 2")
        assertEquals(0, copy.copyOf(1))
        assertTrue(copy.sceneTitle(1).endsWith("Couplet 2"))
    }

    @Test
    fun aTakeOnANewEmptySceneKeepsItsScene() {
        var p = Project().sanitized().copy(drums = DrumPattern().toggle(0, 0)).sanitized()
        val fresh = p.firstEmptyScene()!!
        assertEquals(1, fresh)
        p = p.switchScene(fresh).addAudioTrack("Voix")!!
        p = p.withAudioTrack(p.audioTracks[0].copy(clip = AudioClip("take.wav", "Prise 1", 4.0, scene = fresh))).sanitized()
        // Back to A: scene B (no notes, no name) stays, its voice with it, and is not handed out as new.
        p = p.switchScene(0)
        assertEquals(2, p.allScenes.size)
        assertEquals(fresh, ProjectJson.decode(ProjectJson.encode(p)).sanitized().audioTracks[0].clip?.scene)
        assertEquals(2, ProjectJson.decode(ProjectJson.encode(p)).sanitized().allScenes.size)
        assertEquals(2, p.firstEmptyScene())
    }
}
