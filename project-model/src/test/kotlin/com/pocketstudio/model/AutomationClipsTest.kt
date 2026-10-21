package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomationClipsTest {
    private val volume = AutoTarget(AutoTarget.VOLUME, 2)

    /** The curve the engine builds from [commands] (as Engine.cpp applies them). */
    private fun replay(commands: List<CurveCommand>, size: Int): List<Float> {
        val out = MutableList(size) { -1f }
        for (c in commands) when (c) {
            is CurveCommand.Run -> for (i in c.first until c.first + c.count) out[i] = c.value
            is CurveCommand.Ramp -> {
                val to = Math.round(c.to * 65535f) / 65535f  // sixteen bits, as sent
                for (i in c.first until c.first + c.count) out[i] = c.from + (to - c.from) * (i - c.first) / c.count
            }
        }
        return out
    }

    @Test
    fun songCurvesGoToTheEngineInFewCommands() {
        // A wizard's sweep (a point a bar over 256 bars, 40 points drawn) spread over beats: 1024 points.
        var sweep = AutomationLane(volume, mode = AutomationLane.SONG)
        for (b in 0 until 256 step 6) sweep = sweep.withSongPoint(b, (b % 17) / 17f)
        val curve = sweep.perBeat(4).songCurve(1024)
        val commands = songCurveCommands(curve)
        val replayed = replay(commands, curve.size)
        for (i in curve.indices) assertEquals("point $i", curve[i], replayed[i], 1e-4f)
        assertTrue("${commands.size} commands", commands.size <= 2 * 43 + 2)
        // Sixteen such lanes fit in the engine's queue (4096) with room to spare.
        assertTrue(16 * commands.size < 2048)
        // A dense take (a new value every beat, held flat): never more commands than points.
        var take = AutomationLane(volume, mode = AutomationLane.SONG, songPerBeat = true, hold = true)
        for (b in 0 until 640) take = take.withSongPoint(b, ((b * 37) % 100) / 100f)
        val dense = take.songCurve(640)
        val denseCommands = songCurveCommands(dense)
        assertTrue(denseCommands.size <= dense.size)
        val back = replay(denseCommands, dense.size)
        for (i in dense.indices) assertEquals(dense[i], back[i], 1e-4f)
        assertTrue(songCurveCommands(emptyList()).isEmpty())
        assertEquals(listOf(CurveCommand.Run(0, 3, 0.5f)), songCurveCommands(listOf(0.5f, 0.5f, 0.5f)))
    }

    @Test
    fun theSongCurveIsTheSameInOneWalk() {
        // The curve as it was computed (for each point, the points drawn around it).
        fun naive(lane: AutomationLane, points: Int): List<Float> {
            val drawn = lane.songValues.withIndex().filter { it.value != null }.map { it.index to it.value!! }
            if (drawn.isEmpty()) return emptyList()
            return List(points.coerceIn(1, lane.maxSongPoints)) { b ->
                val after = drawn.firstOrNull { it.first >= b } ?: return@List drawn.last().second
                val before = drawn.lastOrNull { it.first <= b } ?: return@List after.second
                if (after.first == before.first || lane.hold) before.second
                else before.second + (after.second - before.second) * (b - before.first) / (after.first - before.first)
            }
        }
        val random = java.util.Random(7)
        repeat(50) { n ->
            var lane = AutomationLane(volume, mode = AutomationLane.SONG, songPerBeat = n % 2 == 0, hold = n % 3 == 0)
            repeat(random.nextInt(12)) { lane = lane.withSongPoint(random.nextInt(300), random.nextFloat()) }
            val points = 1 + random.nextInt(400)
            assertEquals(naive(lane, points), lane.songCurve(points))
        }
    }

    @Test
    fun aPadsOwnStripIsNeverAutomated() {
        // The engine reads 4 bits of channel: pad 5's send (strip 20) would land on the master's first effect.
        val padSend = AutoTarget(AutoTarget.SEND, Mixer.padChannel(4), 0)
        assertFalse(padSend.isValid)
        assertTrue(AutoTarget(AutoTarget.INSERT_PARAM, 15, 3, 2).isValid)
        val lanes = AutomationLane.sanitize(listOf(AutomationLane(padSend), AutomationLane(volume)))
        assertEquals(listOf(volume), lanes.map { it.target })
    }

    @Test
    fun perSceneLanesKeepEachScenesSteps() {
        val lane = AutomationLane(volume, mode = AutomationLane.PER_SCENE)
            .withPoint(0, 0.2f, scene = 0).withPoint(0, 0.9f, scene = 1)
        assertEquals(0.2f, lane.valuesFor(0)[0])
        assertEquals(0.9f, lane.valuesFor(1)[0])
        assertNull(lane.valuesFor(2)[0])
        assertTrue(lane.values.all { it == null })  // the loop's own steps are untouched
    }

    @Test
    fun theSongCurveSlidesBetweenPoints() {
        val lane = AutomationLane(volume, mode = AutomationLane.SONG).withSongPoint(2, 0.0f).withSongPoint(6, 1.0f)
        val curve = lane.songCurve(10)
        assertEquals(listOf(0f, 0f, 0f, 0.25f, 0.5f, 0.75f, 1f, 1f, 1f, 1f), curve)
        assertTrue(AutomationLane(volume).songCurve(8).isEmpty())
        // Erasing the last point trims the list.
        assertEquals(3, lane.withSongPoint(6, null).songValues.size)
    }

    @Test
    fun savedAndCleaned() {
        val lane = AutomationLane(volume, mode = 7, sceneValues = mapOf(99 to listOf(2f), 1 to listOf(Float.NaN)), songValues = List(300) { 0.5f })
        val clean = lane.sanitized()
        assertEquals(AutomationLane.SONG, clean.mode)
        assertEquals(setOf(1), clean.sceneValues.keys)
        assertNull(clean.sceneValues[1]!![0])
        assertEquals(AutomationLane.MAX_SONG_BARS, clean.songValues.size)
        val p = Project(automation = listOf(AutomationLane(volume, mode = AutomationLane.SONG).withSongPoint(3, 0.4f))).sanitized()
        assertEquals(p, ProjectJson.decode(ProjectJson.encode(p)))
    }

    @Test
    fun songBarsAddUpParts() {
        val p = Project(scenes = listOf(Scene("A"), Scene("B", drums = DrumPattern().withSteps(32))), song = listOf(SongPart(0, 2), SongPart(1, 1))).sanitized()
        assertEquals(4, p.songBars)
    }

    @Test
    fun anInsertLaneGoesWhenItsSlotChangesEffect() {
        val filterCut = AutoTarget(AutoTarget.INSERT_PARAM, 2, 1, 0)
        val masterEq = AutoTarget(AutoTarget.INSERT_PARAM, Mixer.MASTER, 0, 1)
        val withFilter = Project(automation = listOf(AutomationLane(filterCut), AutomationLane(masterEq), AutomationLane(volume)))
            .let { p ->
                val ch = p.mixer.channels.toMutableList()
                ch[2] = ch[2].copy(inserts = ch[2].inserts.mapIndexed { i, ins -> if (i == 1) Insert.of(EffectType.FILTER) else ins })
                p.copy(mixer = p.mixer.copy(channels = ch, master = listOf(Insert.of(EffectType.EQ)) + p.mixer.master.drop(1)))
            }
        // Only a parameter moved: every lane stays.
        val tweaked = withFilter.copy(mixer = withFilter.mixer.copy(master = listOf(Insert(EffectType.EQ.ordinal, listOf(0.9f, 0.5f, 0.5f, 0f))) + withFilter.mixer.master.drop(1)))
        assertEquals(withFilter.automation, tweaked.withoutStaleInsertLanes(withFilter).automation)
        // The filter becomes a chorus: its lane goes, the master's and the volume stay.
        val chorus = withFilter.mixer.channels.toMutableList().also { ch ->
            ch[2] = ch[2].copy(inserts = ch[2].inserts.mapIndexed { i, ins -> if (i == 1) Insert.of(EffectType.CHORUS) else ins })
        }
        val changed = withFilter.copy(mixer = withFilter.mixer.copy(channels = chorus)).withoutStaleInsertLanes(withFilter)
        assertEquals(listOf(masterEq, volume), changed.automation.map { it.target })
    }
}
