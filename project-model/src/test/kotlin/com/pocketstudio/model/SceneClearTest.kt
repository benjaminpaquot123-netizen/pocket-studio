package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneClearTest {
    private fun filled(): Project {
        val style = StarterWizard.style("techno")!!
        val parts = StarterWizard.Part.entries.associateWith { StarterWizard.soundsFor(it, style).first() }
        return StarterWizard.build(style, style.lines.associate { it.role to null as PadSample? }, StarterWizard.Mood.Dark, parts)
    }

    @Test
    fun aNewSceneIsTheFirstFreeSlotAndStartsEmpty() {
        val p = filled()  // A, B, C filled by the assistant
        assertEquals(3, p.firstEmptyScene())
        val d = p.switchScene(p.firstEmptyScene()!!)
        assertTrue(d.allScenes[3].isEmpty)
        // The sounds stay (pads, tracks), only the notes are new.
        assertEquals(p.drums.lanes.map { it.sample }, d.drums.lanes.map { it.sample })
        assertEquals(p.tracks.map { it.sampled }, d.tracks.map { it.sampled })
    }

    @Test
    fun clearingEmptiesOnlyTheSceneShown() {
        val p = filled()
        val cleared = p.clearCurrentScene()
        assertTrue(cleared.allScenes[0].isEmpty)
        assertFalse(cleared.allScenes[1].isEmpty)
        assertFalse(cleared.allScenes[2].isEmpty)
        assertEquals(p.drums.steps, cleared.drums.steps)
        assertEquals(cleared, cleared.sanitized())
    }

    @Test
    fun aFreshCopySaysWhoseCopyItIs() {
        val p = filled()
        val copy = p.copySceneTo(3).switchScene(3)
        assertEquals(0, copy.copyOf(3))
        assertNull(p.copyOf(1))  // B is its own
        assertNull(copy.switchScene(3).clearCurrentScene().copyOf(3))  // an empty scene is nobody's copy
        val full = (3 until Scene.MAX).fold(p) { acc, i -> acc.copySceneTo(i) }
        assertNull(full.firstEmptyScene())
    }
}
