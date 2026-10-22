package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** « Joue dans »: an effect that plays in some scenes (song sections) only. */
class InsertScenesTest {
    /** Scenes A, B, C (one hit each), a distortion on the drums' slot 1 and a reverb on the master's slot 0; A shown. */
    private fun project(): Project {
        var p = Project().copy(drums = DrumPattern().toggle(0, 0))
        p = p.switchScene(1).let { it.copy(drums = it.drums.toggle(0, 4)) }.renameScene(1, "Groove")
        p = p.switchScene(2).let { it.copy(drums = it.drums.toggle(0, 8)) }
        p = p.switchScene(0)
        val drums = p.mixer.channels[0].let { c -> c.copy(inserts = c.inserts.mapIndexed { i, ins -> if (i == 1) Insert.of(EffectType.DISTORTION) else ins }) }
        return p.copy(
            mixer = p.mixer.copy(
                channels = p.mixer.channels.mapIndexed { i, c -> if (i == 0) drums else c },
                master = p.mixer.master.mapIndexed { i, ins -> if (i == 0) Insert.of(EffectType.REVERB) else ins },
            ),
        ).sanitized()
    }

    private fun Project.disto() = mixer.channels[0].inserts[1]

    @Test
    fun anEffectPlaysEverywhereUntilItIsKeptToSomeScenes() {
        val p = project()
        assertNull(p.disto().scenes)
        assertTrue((0..2).all { p.disto().playsIn(it) })
        val b = p.withInsertScenes(0, 1, setOf(1))
        assertEquals(setOf(1), b.disto().scenes)
        assertFalse(b.disto().playsIn(0))
        assertTrue(b.disto().playsIn(1))
        assertEquals(setOf(0), p.withInsertScenes(Mixer.MASTER, 0, setOf(0)).mixer.master[0].scenes)
        // An empty slot has no scenes to play in.
        assertEquals(p, p.withInsertScenes(0, 5, setOf(1)))
        assertEquals(b, b.sanitized())
    }

    @Test
    fun touchingTheLettersGoesFromAllToSomeAndBack() {
        val all = Insert.of(EffectType.CHORUS)
        assertEquals(setOf(2), all.scenesToggled(2, 4))  // from « Tout »: that scene alone
        val two = all.copy(scenes = setOf(2)).scenesToggled(0, 4)
        assertEquals(setOf(0, 2), two)
        assertEquals(listOf(0, 2), two!!.toList())  // in scene order
        assertNull(all.copy(scenes = setOf(2)).scenesToggled(2, 4))  // the last one taken out: « Tout » again
        assertNull(all.copy(scenes = setOf(0, 1, 2)).scenesToggled(3, 4))  // every scene: « Tout »
    }

    @Test
    fun oldProjectsPlayTheirEffectsEverywhereAndTheChoiceIsSaved() {
        val p = project()
        // A project saved before « Joue dans »: no "scenes" anywhere, and none written for « Tout ».
        val text = ProjectJson.encode(p)
        val insertScenes = Regex("\"scenes\"\\s*:\\s*\\[\\s*\\d")  // a list of scene numbers (the project's own scenes are objects)
        assertFalse(Regex("\"scenes\"\\s*:\\s*null").containsMatchIn(text))
        assertFalse(insertScenes.containsMatchIn(text))
        assertEquals(p, ProjectJson.decode(text))
        val limited = p.withInsertScenes(0, 1, setOf(2, 1))
        assertTrue(insertScenes.containsMatchIn(ProjectJson.encode(limited)))
        val back = ProjectJson.decode(ProjectJson.encode(limited))
        assertEquals(setOf(1, 2), back.disto().scenes)
        assertEquals(limited, back)
        // Out of range scenes are dropped; an emptied slot forgets its scenes.
        assertEquals(setOf(3), Insert(EffectType.CHORUS.ordinal, scenes = setOf(-1, 3, Scene.MAX)).sanitized().scenes)
        assertNull(Insert(0, scenes = setOf(1)).sanitized().scenes)
    }

    @Test
    fun movingScenesTakesTheirEffectsAlong() {
        val p = project().withInsertScenes(0, 1, setOf(1))  // in B « Groove » only
        // B and C swap: the distortion still plays in « Groove », now at C.
        val swapped = p.moveScene(1, 2)
        assertEquals("Groove", swapped.sceneLabel(2))
        assertEquals(setOf(2), swapped.disto().scenes)
        // B onto an empty slot (E): it follows.
        assertEquals(setOf(4), p.moveScene(1, 4).disto().scenes)
        // A dropped onto B (replace): B is gone, so is the distortion's place there.
        // B is now A's content, where it did not play: its place follows A's, nowhere (the box says so).
        val replaced = p.moveScene(0, 1, replace = true)
        assertEquals(emptySet<Int>(), replaced.disto().scenes)
        assertTrue((0..4).none { replaced.disto().playsIn(it) })
        // Saved and loaded as it is (an empty list in the file).
        assertEquals(replaced, ProjectJson.decode(ProjectJson.encode(replaced)))
        // B dropped onto A: A is now B, so the effect plays there.
        assertEquals(setOf(0), p.moveScene(1, 0, replace = true).disto().scenes)
        // Playing in A and C, then C dropped onto A: the effect plays in A (C's content), not where A was.
        val ac = project().withInsertScenes(0, 1, setOf(0, 2))
        assertEquals(setOf(0), ac.moveScene(2, 0, replace = true).disto().scenes)
        assertEquals(setOf(0, 2), ac.moveScene(1, 4).disto().scenes)  // another scene moved: unchanged
    }

    @Test
    fun aCopyOfASceneKeepsItsEffects() {
        val p = project().withInsertScenes(0, 1, setOf(0))  // in A only; A shown
        assertEquals(setOf(0, 3), p.copySceneTo(3).disto().scenes)
        // Copied over a scene the effect played in, from one where it does not: no longer there.
        val inBC = project().withInsertScenes(0, 1, setOf(1, 2))
        assertEquals(setOf(1), inBC.copySceneTo(2).disto().scenes)
        // « Tout » stays « Tout ».
        assertNull(project().copySceneTo(3).disto().scenes)
    }

    @Test
    fun anEmptySceneAnEffectPlaysInIsKept() {
        // D (empty) shown, the distortion kept to D, then back to A: D stays, as its effect names it.
        val p = project().switchScene(3).withInsertScenes(0, 1, setOf(3)).switchScene(0)
        assertEquals(4, p.allScenes.size)
        // Without it, the empty D at the end goes.
        assertEquals(3, project().switchScene(3).switchScene(0).allScenes.size)
    }

    @Test
    fun aReadyMadeAmbienceKeptToSomeScenesIsStillThatAmbience() {
        val preset = FxPresets.ALL.first { it.inserts.isNotEmpty() }
        val c = Channel().withFx(preset)
        val kept = c.copy(inserts = c.inserts.mapIndexed { i, ins -> if (i == 0) ins.copy(scenes = setOf(1)) else ins })
        assertEquals(preset.id, kept.matchingFx()?.id)
    }
}
