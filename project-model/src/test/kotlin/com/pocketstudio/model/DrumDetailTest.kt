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
    fun aHitsForceIsContinuousAndKeepsTheRest() {
        val drums = DrumPattern().toggle(2, 4).withHitDetail(2, 4, 0.5f, 3).cycleRepeats(2, 4).withHitVelocity(2, 4, 0.37f)
        val hit = drums.lanes[2].hits[4]!!
        assertEquals(0.37f, hit.velocity)
        assertEquals(0.5f, hit.pan)
        assertEquals(3, hit.pitch)
        assertEquals(2, hit.repeats)
        // Never silent (0 removes a hit in the engine), never above full; no hit there: unchanged.
        assertEquals(DrumPattern.MIN_VELOCITY, drums.withHitVelocity(2, 4, 0f).lanes[2].hits[4]!!.velocity)
        assertEquals(1f, drums.withHitVelocity(2, 4, 3f).lanes[2].hits[4]!!.velocity)
        assertEquals(0.37f, drums.withHitVelocity(2, 4, Float.NaN).lanes[2].hits[4]!!.velocity)
        assertEquals(drums, drums.withHitVelocity(2, 5, 0.5f))
        assertEquals(0.37f, ProjectJson.decode(ProjectJson.encode(Project(drums = drums))).drums.lanes[2].hits[4]!!.velocity)
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

    @Test
    fun filesWrittenBeforeTheseFieldsStillOpen() {
        // A project saved by the previous version: no pan, pitch, macros or scale lock anywhere.
        val p = Project(drums = DrumPattern().toggle(0, 0).withHitDetail(0, 0, 0.5f, 3).withPadPan(2, 1f), harmony = Harmony(4, Scale.Minor, lock = true))
        val newer = ProjectJson.encode(p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.map { it.copy(macros = listOf(0.1f, 0.2f, 0.3f, 0.4f)) })))
        fun strip(e: kotlinx.serialization.json.JsonElement): kotlinx.serialization.json.JsonElement = when (e) {
            is kotlinx.serialization.json.JsonObject -> kotlinx.serialization.json.JsonObject(
                e.filterKeys { it !in setOf("pan", "pitch", "macros", "lock") || it == "pan" && e.containsKey("volume") }.mapValues { strip(it.value) },
            )
            is kotlinx.serialization.json.JsonArray -> kotlinx.serialization.json.JsonArray(e.map(::strip))
            else -> e
        }
        val older = strip(kotlinx.serialization.json.Json.parseToJsonElement(newer)).toString()
        assertTrue(!older.contains("\"macros\"") && !older.contains("\"lock\"") && !older.contains("\"pitch\""))
        val back = ProjectJson.decode(older)
        assertEquals(Hit(), back.drums.lanes[0].hits[0])
        assertEquals(0f, back.drums.lanes[2].pan)
        assertEquals(Macros.DEFAULTS, back.mixer.channels[2].macros)
        assertEquals(Harmony(4, Scale.Minor, lock = false), back.harmony)
    }
}
