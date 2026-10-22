package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** « Ce qui joue »: nothing that acts on the sound is left out, per scene and for the whole song. */
class WhatActsTest {
    private fun wizard(id: String): Project {
        val s = StarterWizard.STYLES.single { it.id == id }
        val lines = s.lines.associate { it.role to null as PadSample? }
        val chosen = StarterWizard.Part.entries.associateWith { StarterWizard.soundsFor(it, s).first() }
        return StarterWizard.build(s, lines, s.melody.mood, chosen)
    }

    @Test
    fun theWholeSongListsEveryLaneEveryEffectAndEverySend() {
        for (id in listOf("jump_up", "retro_techno", "acid_techno")) {
            val p = wizard(id)
            val acts = p.whatActs()
            val lanes = acts.filterIsInstance<Acting.Moves>().map { it.lane }.toSet()
            assertEquals(id, p.automation.indices.toSet(), lanes)
            p.mixer.channels.forEachIndexed { ch, c ->
                if (ch == Mixer.MASTER) return@forEachIndexed
                c.inserts.forEachIndexed { slot, ins ->
                    if (ins.effect != EffectType.NONE) assertTrue("$id $ch/$slot", acts.any { it is Acting.Effect && it.ch == ch && it.slot == slot })
                }
                c.sends.forEachIndexed { bus, v -> if (v > 0.01f) assertTrue("$id send $ch/$bus", acts.any { it is Acting.Send && it.ch == ch && it.bus == bus }) }
            }
            p.mixer.master.forEachIndexed { slot, ins ->
                if (ins.effect != EffectType.NONE) assertTrue("$id master $slot", acts.any { it is Acting.Effect && it.ch == Mixer.MASTER && it.slot == slot })
            }
        }
    }

    @Test
    fun aSceneShowsOnlyWhatPlaysInIt() {
        val p = Project().let { base ->
            val ch = base.mixer.channels.toMutableList()
            ch[0] = ch[0].copy(inserts = listOf(Insert.of(EffectType.FILTER).copy(scenes = setOf(1))) + ch[0].inserts.drop(1))
            base.copy(
                mixer = base.mixer.copy(channels = ch, masterDb = -3f),
                automation = listOf(
                    AutomationLane(AutoTarget(AutoTarget.VOLUME, 0), mode = AutomationLane.PER_SCENE, sceneValues = mapOf(1 to List(AutomationLane.STEPS) { 0.5f })),
                    AutomationLane(AutoTarget(AutoTarget.PAN, 0)),
                ),
            )
        }
        val a = p.whatActs(0)
        assertFalse(a.any { it is Acting.Effect })
        assertFalse(a.any { it is Acting.Moves && it.lane == 0 })
        // An empty loop lane still shows (nothing hidden), as empty.
        assertEquals(CurveShape.Kind.EMPTY, a.filterIsInstance<Acting.Moves>().single { it.lane == 1 }.shape.kind)
        assertTrue(a.any { it is Acting.MasterTrim })
        val b = p.whatActs(1)
        assertTrue(b.any { it is Acting.Effect && it.ch == 0 })
        assertEquals(CurveShape.Kind.STEADY, b.filterIsInstance<Acting.Moves>().single { it.lane == 0 }.shape.kind)
        assertEquals(listOf(0), p.automatedLanes(0).take(1))
    }

    @Test
    fun aSongCurveShowsUnderTheScenesItCovers() {
        val scenes = listOf(Scene("A"), Scene("B"))
        val lane = AutomationLane(AutoTarget(AutoTarget.VOLUME, 0), mode = AutomationLane.SONG)
            .withSongPoint(0, 0f).withSongPoint(3, 1f)
        val p = Project(scenes = scenes, song = listOf(SongPart(0, 4), SongPart(1, 4)), automation = listOf(lane))
        // Rises over A (bars 0..3), stays up over B.
        assertEquals(CurveShape.Kind.RISES, p.whatActs(0).filterIsInstance<Acting.Moves>().single().shape.kind)
        assertEquals(CurveShape.Kind.STEADY, p.whatActs(1).filterIsInstance<Acting.Moves>().single().shape.kind)
        assertNull(lane.shapeIn(p, 5))
    }

    @Test
    fun shapesSayWhatACurveDoes() {
        assertEquals(CurveShape.Kind.RISES, CurveShape.of(listOf(0f, 0.3f, 0.8f)).kind)
        assertEquals(CurveShape.Kind.FALLS, CurveShape.of(listOf(1f, 0.4f, 0.1f)).kind)
        assertEquals(CurveShape.Kind.UP_DOWN, CurveShape.of(listOf(0.2f, 1f, 0.2f)).kind)
        assertEquals(CurveShape.Kind.DOWN_UP, CurveShape.of(listOf(0.9f, 0f, 0.9f)).kind)
        assertEquals(CurveShape.Kind.EMPTY, CurveShape.of(emptyList()).kind)
    }

    @Test
    fun aLineAndSmoothingAreOneEditAndKeepTheData() {
        val lane = AutomationLane(AutoTarget(AutoTarget.VOLUME, 0)).withStepLine(12, 1f, 4, 0f)
        assertEquals(0f, lane.values[4]!!, 1e-6f)
        assertEquals(0.5f, lane.values[8]!!, 1e-6f)
        assertEquals(1f, lane.values[12]!!, 1e-6f)
        assertNull(lane.values[3])
        assertEquals(AutomationLane.STEPS, lane.values.size)
        val shaky = AutomationLane(AutoTarget(AutoTarget.VOLUME, 0)).withPoint(0, 0f).withPoint(1, 1f).withPoint(2, 0f).withStepsSmoothed()
        assertEquals(0.5f, shaky.values[1]!!, 1e-6f)
        assertNull(shaky.values[5])

        val song = AutomationLane(AutoTarget(AutoTarget.VOLUME, 0), mode = AutomationLane.SONG).withSongPoint(5, 1f).withSongJump(5, true)
            .withSongLine(0, 0f, 8, 1f)
        assertEquals(9, song.songValues.size)
        assertEquals(0.5f, song.songValues[4]!!, 1e-6f)
        assertTrue(song.songJumps.isEmpty())
        // Saved and read back: the same fields as ever.
        val json = ProjectJson.encode(Project(automation = listOf(song)))
        assertEquals(song, ProjectJson.decode(json).automation.single())
        assertEquals(song.songValues.size, song.withSongSmoothed().songValues.size)
    }
}
