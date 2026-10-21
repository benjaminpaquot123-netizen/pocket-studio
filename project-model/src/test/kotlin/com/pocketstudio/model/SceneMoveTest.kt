package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneMoveTest {
    /** Scenes A (one hit on step 0), B (step 4), C (step 8), named; song A B A C; A shown. */
    private fun project(): Project {
        var p = Project().copy(drums = DrumPattern().toggle(0, 0)).renameScene(0, "Intro")
        p = p.switchScene(1).let { it.copy(drums = it.drums.toggle(0, 4)) }.renameScene(1, "Couplet")
        p = p.switchScene(2).let { it.copy(drums = it.drums.toggle(0, 8)) }.renameScene(2, "Refrain")
        return p.switchScene(0).copy(song = listOf(SongPart(0), SongPart(1, 2), SongPart(0), SongPart(2))).sanitized()
    }

    /** The step of the one hit in scene [i] (null: empty). */
    private fun Project.hitOf(i: Int): Int? = allScenes.getOrNull(i)?.drums?.lanes?.get(0)?.hits?.keys?.singleOrNull()

    /** What the song plays, part by part: the hit and the repeats. */
    private fun Project.heard() = song.map { hitOf(it.scene) to it.repeats }

    @Test
    fun aSceneDroppedOnAnEmptySlotMovesAndTheSongFollows() {
        val p = project()
        val m = p.moveScene(1, 5)
        assertEquals(4, m.hitOf(5))
        assertEquals("Couplet", m.sceneLabel(5))
        assertTrue(m.allScenes[1].isEmpty)
        assertEquals("", m.sceneLabel(1))
        // The song plays exactly the same music, from the scene's new place.
        assertEquals(listOf(0, 5, 0, 2), m.song.map { it.scene })
        assertEquals(p.heard(), m.heard())
        // The scene shown (A) did not move and still mirrors its content.
        assertEquals(0, m.scene)
        assertEquals(p.drums, m.drums)
        assertEquals(m, m.sanitized())
    }

    @Test
    fun twoScenesSwapAndTheSceneShownFollowsItsContent() {
        val p = project()
        val m = p.moveScene(0, 2)
        assertEquals(8, m.hitOf(0))
        assertEquals(0, m.hitOf(2))
        assertEquals("Refrain", m.sceneLabel(0))
        assertEquals("Intro", m.sceneLabel(2))
        assertEquals(listOf(2, 1, 2, 0), m.song.map { it.scene })
        assertEquals(p.heard(), m.heard())
        // A was shown: the screens stay on it, now in slot C, nothing else changes in what is edited.
        assertEquals(2, m.scene)
        assertEquals(p.drums, m.drums)
    }

    @Test
    fun theSceneShownIsSwappedAwayItsMirrorFollows() {
        val p = project().switchScene(2)
        val m = p.moveScene(0, 2)
        assertEquals(0, m.scene)
        assertEquals(8, m.drums.lanes[0].hits.keys.single())
        assertEquals(8, m.hitOf(0))
        assertEquals(0, m.hitOf(2))
    }

    @Test
    fun replacingOverwritesTheTargetAndItsPartsPlayTheSceneMoved() {
        val p = project()
        val m = p.moveScene(2, 1, replace = true)
        assertEquals(8, m.hitOf(1))
        assertEquals("Refrain", m.sceneLabel(1))
        // The old Couplet is gone; C's slot is empty (and dropped from the end of the list).
        assertTrue(m.allScenes.none { it.drums.lanes[0].hits.keys == setOf(4) })
        assertTrue(m.allScenes.getOrNull(2)?.isEmpty ?: true)
        // Every part of B or C now plays the scene moved, in the same order and repeats.
        assertEquals(listOf(0, 1, 0, 1), m.song.map { it.scene })
        assertEquals(listOf(0 to 1, 8 to 2, 0 to 1, 8 to 1), m.heard())
        assertEquals(m, m.sanitized())
    }

    @Test
    fun replacingTheSceneShownShowsTheSceneMoved() {
        val p = project()  // A shown
        val m = p.moveScene(1, 0, replace = true)
        assertEquals(0, m.scene)
        assertEquals(4, m.drums.lanes[0].hits.keys.single())
        assertEquals("Couplet", m.sceneLabel(0))
    }

    @Test
    fun perSceneAutomationAndSceneClipsMoveWithTheirScene() {
        val lane = AutomationLane(
            AutoTarget(AutoTarget.VOLUME), mode = AutomationLane.PER_SCENE,
            sceneValues = mapOf(0 to List(AutomationLane.STEPS) { 0.1f }, 1 to List(AutomationLane.STEPS) { 0.2f }),
        )
        val clip = AudioClip("voice.wav", "Voix", scene = 1)
        val p = project().copy(automation = listOf(lane), audioTracks = listOf(AudioTrack(0, "Voix", clip = clip, previous = clip.copy(file = "old.wav"))))
        val swapped = p.moveScene(1, 3)
        assertEquals(0.2f, swapped.automation[0].sceneValues[3]!![0])
        assertEquals(null, swapped.automation[0].sceneValues[1])
        assertEquals(0.1f, swapped.automation[0].sceneValues[0]!![0])
        assertEquals(3, swapped.audioTracks[0].clip!!.scene)
        assertEquals(3, swapped.audioTracks[0].previous!!.scene)
        // Replacing A with B: A's own curve goes with A, B's comes to A's slot.
        val replaced = p.moveScene(1, 0, replace = true)
        assertEquals(mapOf(0 to List(AutomationLane.STEPS) { 0.2f }), replaced.automation[0].sceneValues)
        assertEquals(0, replaced.audioTracks[0].clip!!.scene)
    }

    @Test
    fun replacingASceneTakesItsAudioTakesAwayWithIt() {
        // B has a voice take; D is dropped on B with « Remplacer »: B's voice must not play over D.
        val voice = AudioClip("voice.wav", "Voix", scene = 1)
        val drums = AudioClip("loop.wav", "Boucle", scene = 3)
        var p = project().switchScene(3).let { it.copy(drums = it.drums.toggle(0, 12)) }.switchScene(0)
        p = p.copy(audioTracks = listOf(AudioTrack(0, "Voix", clip = voice, previous = voice.copy(file = "old.wav")), AudioTrack(1, "Boucle", clip = drums))).sanitized()
        val m = p.moveScene(3, 1, replace = true)
        assertEquals(12, m.hitOf(1))
        assertEquals(null, m.audioTracks[0].clip)
        assertEquals(null, m.audioTracks[0].previous)
        // D's own take moves with D.
        assertEquals(1, m.audioTracks[1].clip!!.scene)
        // A swap keeps both scenes' takes, each with its scene.
        val s = p.moveScene(3, 1)
        assertEquals(3, s.audioTracks[0].clip!!.scene)
        assertEquals(1, s.audioTracks[1].clip!!.scene)
    }

    @Test
    fun aMoveUndoneMapsEveryPlaceBack() {
        for (replace in listOf(false, true)) {
            val move = SceneMove(0, 2, replace)
            // What was played before the move, then after it, then back: the same scene's content.
            for (i in 0 until Scene.MAX) {
                if (replace && i == 2) continue  // the replaced scene is gone: nothing played it after
                assertEquals("replace=$replace i=$i", i, move.back(move.forward(i)))
            }
        }
        // After a swap, the scene playing in A's new slot (2) was in slot 0 before.
        assertEquals(0, SceneMove(0, 2, false).back(2))
        assertEquals(2, SceneMove(0, 2, false).back(0))
        assertEquals(0, SceneMove(0, 2, true).back(2))
        assertEquals(5, SceneMove(0, 2, true).back(5))
    }

    @Test
    fun undoAndRedoHandBackTheStepTag() {
        val h = History(0)
        h.push(1)
        h.push(2, tag = SceneMove(0, 2, false))
        h.push(3)
        h.undo()
        assertEquals(null, h.lastTag)
        h.undo()
        assertEquals(SceneMove(0, 2, false), h.lastTag)
        h.redo()
        assertEquals(SceneMove(0, 2, false), h.lastTag)
        h.redo()
        assertEquals(null, h.lastTag)
        // A merged burst keeps the first step's tag.
        h.push(4, coalesceKey = "k", nowMs = 0, tag = "first")
        h.push(5, coalesceKey = "k", nowMs = 1, tag = "second")
        h.undo()
        assertEquals("first", h.lastTag)
        assertEquals(3, h.current)
    }

    @Test
    fun nothingHappensForAMoveOntoItselfOrOutOfRange() {
        val p = project()
        assertEquals(p, p.moveScene(1, 1))
        assertEquals(p, p.moveScene(1, Scene.MAX))
        assertEquals(p, p.moveScene(-1, 2))
        // Moving an empty slot that does not exist yet changes nothing.
        assertEquals(p.allScenes, p.moveScene(10, 1).allScenes)
    }

    @Test
    fun indicesFollowTheMove() {
        assertEquals(5, sceneIndexAfterMove(1, 1, 5, replace = false))
        assertEquals(1, sceneIndexAfterMove(5, 1, 5, replace = false))
        assertEquals(5, sceneIndexAfterMove(5, 1, 5, replace = true))
        assertEquals(3, sceneIndexAfterMove(3, 1, 5, replace = true))
    }
}
