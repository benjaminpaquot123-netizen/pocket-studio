package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrumDetailTest {
    @Test
    fun hitDetailIsKeptAndChecked() {
        val drums = DrumPattern().toggle(2, 4).withHitDetail(2, 4, -0.5f, 7).cycleRepeats(2, 4)
        val hit = drums.lanes[2].hits[4]!!
        assertEquals(-0.5f, hit.pan)
        assertEquals(7, hit.pitch)
        assertEquals(2, hit.repeats)
        assertFalse(hit.isPlain)
        // No hit there: nothing changes.
        assertEquals(drums, drums.withHitDetail(2, 5, 1f, 3))
        // Out of range values from a file are brought back.
        val wild = Hit(pan = Float.NaN, pitch = 99).sanitized()
        assertEquals(0f, wild.pan)
        assertEquals(Hit.MAX_PITCH, wild.pitch)
        assertTrue(Hit().isPlain)
    }

    @Test
    fun padPanSurvivesSavingAndSceneChanges() {
        val p = Project(drums = DrumPattern().withPadPan(0, -0.7f).toggle(0, 0).withHitDetail(0, 0, 0.3f, -2))
        val back = ProjectJson.decode(ProjectJson.encode(p))
        assertEquals(-0.7f, back.drums.lanes[0].pan)
        assertEquals(Hit(pan = 0.3f, pitch = -2), back.drums.lanes[0].hits[0])
        // Like the pad's sound, its pan is the same in every scene.
        val other = back.switchScene(1)
        assertEquals(-0.7f, other.drums.lanes[0].pan)
        assertTrue(other.drums.lanes[0].hits.isEmpty())
        assertEquals(-0.7f, other.switchScene(0).drums.lanes[0].pan)
        // Old files without the fields read as centred, plain hits.
        assertEquals(0f, ProjectJson.decode(ProjectJson.encode(Project())).drums.lanes[5].pan)
    }
}
