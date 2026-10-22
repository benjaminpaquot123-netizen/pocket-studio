package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** « Enregistrer la performance »: launches cut the take into song parts, gestures become song curves. */
class PerformanceTakeTest {
    private val acidB = AcidPattern(steps = List(AcidPattern.MAX_STEPS) { i -> AcidStep(gate = i % 4 == 0) })

    /** A (16 steps), B (16, with an acid line of 32), C (drums of 32 steps). */
    private fun project(): Project = Project(
        scenes = listOf(
            Scene("A", drums = DrumPattern().toggle(0, 0)),
            Scene("B", drums = DrumPattern().toggle(1, 0), acid = acidB.copy(length = 32)),
            Scene("C", drums = DrumPattern().withSteps(32).toggle(2, 0)),
        ),
        scene = 0, drums = DrumPattern().toggle(0, 0),
    ).sanitized()

    @Test
    fun launchesCutTheTakeIntoParts() {
        val p = project()
        val take = PerformanceTake(16, LiveState.on(0, 0))
        take.launched(LiveSlots.ACID, 1, 32)           // the acid alone on B
        for (k in 0 until LiveSlots.COUNT) take.launched(k, 1, 48)  // the whole row B
        take.launched(LiveSlots.DRUMS, -1, 80)         // the drums stop
        take.launched(LiveSlots.DRUMS, -1, 88)         // stopped again: changes nothing
        val w = p.writeTake(take, 96, append = false)
        val song = w.project.song
        assertEquals(4, w.parts)
        // A once; A with the acid on B (from its start: no « from »); B (two bars: its acid line);
        // one bar of B without the drums.
        assertEquals(SongPart(0, 1), song[0])
        assertEquals(SongPart(0, 1, play = mapOf(SongPart.ACID to 1), bars = 1), song[1])
        assertEquals(SongPart(1, 1), song[2])
        assertEquals(SongPart(1, 1, setOf(SongPart.DRUMS), bars = 1), song[3])
        assertEquals(5, w.project.songBars)
        assertTrue(song[1].mixed)
        assertFalse(song[2].mixed)
        assertEquals(0, w.settings)
    }

    @Test
    fun tracksThatCarryOnStartWhereTheyWere() {
        val p = project()
        val take = PerformanceTake(0, LiveState.on(0, 0))
        take.launched(LiveSlots.ACID, 1, 32)    // the acid's 32-step line from step 32
        take.launched(LiveSlots.DRUMS, 2, 48)   // the drums on C from 48: the acid is half-way through
        val w = p.writeTake(take, 80, append = false)
        val song = w.project.song
        assertEquals(3, song.size)
        assertEquals(SongPart(0, 2), song[0])
        assertEquals(SongPart(0, 1, play = mapOf(SongPart.ACID to 1), bars = 1), song[1])
        // Row A (no whole row was launched), the drums on C, the acid one bar into its line.
        val third = song[2]
        assertEquals(0, third.scene)
        assertEquals(mapOf(SongPart.ACID to 1, SongPart.DRUMS to 2), third.play)
        assertEquals(mapOf(SongPart.ACID to 1), third.from)
        assertEquals(2, third.bars)
        assertEquals(2 * 16, p.partSteps(third))
        // The part as played: each track its own scene.
        val view = w.project.partView(third)
        assertEquals(32, view.drums.steps)
        assertTrue(view.drums.lanes[2].hits.containsKey(0))
        assertEquals(32, view.acid.length)
    }

    @Test
    fun partsThatOnlyCarryOnAreMerged() {
        val p = project()
        val take = PerformanceTake(0, LiveState.on(0, 0))
        // A slot launched on the scene it already plays, on its loop's line: nothing changes.
        take.launched(LiveSlots.ACID, 1, 16)
        take.launched(LiveSlots.ACID, 1, 48)   // its 32-step line again, from its start: carries on
        val w = p.writeTake(take, 64, append = false)
        assertEquals(listOf(SongPart(0, 1), SongPart(0, 1, play = mapOf(SongPart.ACID to 1), bars = 3)), w.project.song)
    }

    @Test
    fun appendingKeepsTheSongAndPlacesTheCurvesAfterIt() {
        val p = project().copy(song = listOf(SongPart(0, 2))).sanitized()
        val take = PerformanceTake(16, LiveState.on(1, 16))
        take.gesture(AutoTarget.acid(0), 0.9f, 24.0, 0.4f)
        val w = p.writeTake(take, 48, append = true)
        assertEquals(listOf(SongPart(0, 2), SongPart(1, 1)), w.project.song)
        assertEquals(1, w.parts)
        val lane = w.project.automation.single()
        // The song lasts 32 steps (8 beats) before the take: the gesture on its beat 2 is beat 10.
        assertEquals(0.4f, lane.songValues[9])
        assertEquals(0.9f, lane.songValues[10])
        // « Remplacer » (the default) drops the old song.
        assertEquals(listOf(SongPart(1, 1)), p.writeTake(take, 48, append = false).project.song)
    }

    @Test
    fun gesturesBecomeASongCurveWithAPointPerBeat() {
        val p = project()
        val take = PerformanceTake(16, LiveState.on(0, 0))
        val cutoff = AutoTarget.acid(0)
        take.gesture(cutoff, 0.8f, 24.0, 0.5f)   // beat 2 of the take
        take.gesture(cutoff, 0.9f, 25.0, 0.8f)   // the same beat: the last value wins
        take.gesture(cutoff, 0.2f, 40.0, 0.9f)   // beat 6
        take.gesture(cutoff, 0.7f, 70.0, 0.2f)   // after the end: left out
        val w = p.writeTake(take, 64, append = false)
        assertEquals(1, w.settings)
        val lane = w.project.automation.single()
        assertEquals(AutomationLane.SONG, lane.mode)
        assertTrue(lane.songPerBeat)
        assertFalse(lane.hold)
        // The setting as it was just before, the moves, and flat until the next one (no slow ramp).
        assertEquals(listOf(null, 0.5f, 0.9f, null, null, 0.9f, 0.2f), lane.songValues)
        val curve = lane.songCurve(8)
        assertEquals(0.5f, curve[0])
        assertEquals(0.9f, curve[4])
        assertEquals(0.2f, curve[7])
    }

    @Test
    fun onOffControlsStepAndAQuickPressLastsABeat() {
        val take = PerformanceTake(0, LiveState.on(0, 0))
        val repeat = AutoTarget.live(AutoTarget.LIVE_REPEAT)
        take.gesture(repeat, 1f, 4.0, 0f)    // beat 1
        take.gesture(repeat, 0f, 5.0, 1f)    // let go in the same beat
        take.gesture(repeat, 1f, 16.0, 0f)   // beat 4
        take.gesture(repeat, 0f, 28.0, 1f)   // beat 7
        val lane = project().writeTake(take, 32, append = false).project.automation.single()
        assertTrue(lane.hold)
        assertEquals(listOf(0f, 1f, 0f, 0f, 1f, 1f, 1f, 0f), lane.songCurve(8))
    }

    @Test
    fun theTakeKeepsTheCurveItHeardBeforeItsGesture() {
        val target = AutoTarget(AutoTarget.VOLUME, 2)
        // A song curve with a point per bar: 0.1 on bar 0, 0.9 on bar 3.
        val drawn = AutomationLane(target, mode = AutomationLane.SONG).withSongPoint(0, 0.1f).withSongPoint(3, 0.9f)
        val p = project().copy(automation = listOf(drawn)).sanitized()
        val take = PerformanceTake(0, LiveState.on(0, 0))
        take.gesture(target, 0.5f, 8.0, 0.3f)   // beat 2 (the knob's own setting 0.3 was not what played)
        val lane = p.writeTake(take, 16, append = false).project.automation.single()
        assertTrue(lane.songPerBeat)
        val heard = drawn.perBeat(4).songCurve(16)
        // Beats 0 and 1 as the curve played them, then the gesture held to the take's end (the
        // take is the song now: nothing of the curve after it).
        assertEquals(0.1f, lane.songValues[0])
        assertEquals(heard[1], lane.songValues[1]!!, 1e-6f)
        assertEquals(0.5f, lane.songValues[2])
        assertEquals(3, lane.songValues.size)
        assertEquals(listOf(0.5f, 0.5f), lane.songCurve(4).drop(2))
    }

    @Test
    fun appendingKeepsTheSongCurveOfTheSongBefore() {
        val target = AutoTarget(AutoTarget.VOLUME, 2)
        val drawn = AutomationLane(target, mode = AutomationLane.SONG).withSongPoint(0, 0.1f).withSongPoint(1, 0.5f).withSongPoint(5, 0.2f)
        val p = project().copy(song = listOf(SongPart(0, 2)), automation = listOf(drawn)).sanitized()
        val take = PerformanceTake(16, LiveState.on(0, 16))
        take.gesture(target, 0.9f, 24.0, 0.3f)   // beat 2 of the take: song beat 10
        val lane = p.writeTake(take, 48, append = true).project.automation.single()
        val before = drawn.perBeat(4).songCurve(10)
        val after = lane.songCurve(16)
        for (b in 0..9) assertEquals("beat $b", before[b], after[b], 1e-6f)
        assertEquals(0.9f, after[10])
        assertEquals(0.9f, after[15])
    }

    @Test
    fun aLoopLaneTouchedDuringTheTakeIsBakedAsItPlayed() {
        val cutoff = AutoTarget.acid(0)
        // A loop lane: 0.2 on the drum loop's step 0, 0.8 on step 8 (scene A's loop: 16 steps).
        val loop = AutomationLane(cutoff).withPoint(0, 0.2f).withPoint(8, 0.8f)
        val p = project().copy(automation = listOf(loop)).sanitized()
        val take = PerformanceTake(0, LiveState.on(0, 0))
        take.gesture(cutoff, 0.5f, 20.0, 0.4f)   // beat 5
        val lane = p.writeTake(take, 32, append = false).project.automation.single()
        assertEquals(AutomationLane.SONG, lane.mode)
        assertEquals(listOf(0.2f, 0.2f, 0.8f, 0.8f, 0.2f, 0.5f, 0.5f, 0.5f), lane.songCurve(8))
        // Its own steps stay for a switch back.
        assertEquals(0.2f, lane.values[0])
        assertEquals(0.8f, lane.values[8])
    }

    @Test
    fun aPerSceneLaneKeepsEachSceneOfTheSongBeforeATakeAppended() {
        val cutoff = AutoTarget.acid(0)
        val perScene = AutomationLane(cutoff, mode = AutomationLane.PER_SCENE)
            .withPoint(0, 0.3f, scene = 0).withPoint(0, 0.7f, scene = 1)
        val p = project().copy(song = listOf(SongPart(0), SongPart(1)), automation = listOf(perScene)).sanitized()
        val take = PerformanceTake(0, LiveState.on(0, 0))
        take.gesture(cutoff, 0.9f, 4.0, 0.4f)   // the take's beat 1: song beat 13 (A 4 beats, B 8)
        val lane = p.writeTake(take, 16, append = true).project.automation.single()
        val curve = lane.songCurve(16)
        assertEquals(List(4) { 0.3f } + List(8) { 0.7f } + 0.3f, curve.take(13))
        assertEquals(0.9f, curve[13])
        assertEquals(mapOf(0 to 0.3f, 1 to 0.7f), lane.sceneValues.mapValues { it.value[0] })
    }

    @Test
    fun replacingTheSongCutsTheCurvesOfTheSongBefore() {
        // A wizard-like sweep over 8 bars: the take (2 bars) heard its first two bars only.
        val sweep = (0..7).fold(AutomationLane(AutoTarget(AutoTarget.INSERT_PARAM, 2, 0, 0), mode = AutomationLane.SONG)) { l, b ->
            l.withSongPoint(b, b / 8f)
        }
        val p = project().copy(song = List(8) { SongPart(0) }, automation = listOf(sweep)).sanitized()
        val take = PerformanceTake(0, LiveState.on(0, 0))
        val replaced = p.writeTake(take, 32, append = false).project.automation.single()
        assertEquals(listOf(0f, 1 / 8f, 2 / 8f), replaced.songValues)
        assertEquals(sweep.songCurve(2), replaced.songCurve(2))
        // Appended, the song before stays: so does its curve.
        assertEquals(sweep.songValues, p.writeTake(take, 32, append = true).project.automation.single().songValues)
    }

    @Test
    fun aTakeOfMoreThan128PartsEndsWhereTheSongIsFull() {
        val p = project()
        val take = PerformanceTake(0, LiveState.on(0, 0))
        // The acid changes scene on every bar: 200 parts.
        for (b in 1 until 200) take.launched(LiveSlots.ACID, b % 2, b * 16)
        take.gesture(AutoTarget.acid(0), 0.9f, 130 * 16.0, 0.4f)   // after the 128th part: left out
        val w = p.writeTake(take, 200 * 16, append = false)
        assertTrue(w.cut)
        assertEquals(SongPart.MAX_PARTS, w.parts)
        assertEquals(SongPart.MAX_PARTS, w.project.song.size)
        assertEquals(128, w.project.songBars)
        assertEquals(0, w.settings)
        // Appended after 100 parts: 28 more.
        val long = p.copy(song = List(100) { SongPart(0) }).sanitized()
        val a = long.writeTake(take, 200 * 16, append = true)
        assertTrue(a.cut)
        assertEquals(28, a.parts)
        assertEquals(SongPart.MAX_PARTS, a.project.song.size)
        assertEquals(p.takeParts(take, 200 * 16).size, 200)
        assertFalse(p.writeTake(take, 100 * 16, append = false).cut)
    }

    @Test
    fun aStretchLongerThanAPartGoesOnInTheNext() {
        val p = project()
        val take = PerformanceTake(0, LiveState.on(0, 0))
        val w = p.writeTake(take, 300 * 16, append = false)
        assertEquals(300, w.project.songBars)
        assertEquals(listOf(256, 44), w.project.song.map { it.bars })
    }

    @Test
    fun theRemixStoppedDuringTheTakeIsMutedInTheSong() {
        val p = project().copy(remix = Remix(setId = "set")).sanitized()
        val take = PerformanceTake(0, LiveState.on(0, 0))
        take.launched(LiveSlots.REMIX, -1, 16)
        take.launched(LiveSlots.REMIX, 1, 48)   // back (a remix has no scenes: it just plays)
        val song = p.writeTake(take, 64, append = false).project.song
        assertEquals(listOf(SongPart(0), SongPart(0, 2, setOf(SongPart.REMIX)), SongPart(0)), song)
        // Without a remix loaded, no such column.
        assertEquals(listOf(SongPart(0, 4)), project().writeTake(take, 64, append = false).project.song)
    }

    @Test
    fun aTakeMarkedLaterStartsThere() {
        val take = PerformanceTake(16, LiveState.on(0, 0))
        take.launched(LiveSlots.ACID, 1, 32)
        take.launched(LiveSlots.DRUMS, 2, 48)
        take.startsAt(32)   // the engine marked the song a quantum later
        assertEquals(32, take.start)
        assertEquals(1, take.initial.slot(LiveSlots.ACID).playing)
        assertEquals(1, take.launchCount)
        val song = project().writeTake(take, 64, append = false).project.song
        assertEquals(SongPart(0, 1, play = mapOf(SongPart.ACID to 1), bars = 1), song.first())
        take.startsAt(16)   // never back
        assertEquals(32, take.start)
    }

    @Test
    fun noMoreThanSixteenLanes() {
        val full = (0..15).filter { it != Mixer.MASTER }.map { AutomationLane(AutoTarget(AutoTarget.VOLUME, it)) } +
            AutomationLane(AutoTarget(AutoTarget.PAN, 0))
        val p = project().copy(automation = full).sanitized()
        assertEquals(AutomationLane.MAX_LANES, p.automation.size)
        val take = PerformanceTake(0, LiveState.on(0, 0))
        take.gesture(AutoTarget.live(AutoTarget.LIVE_FILTER), 0.2f, 4.0, 0.5f)
        take.gesture(AutoTarget(AutoTarget.VOLUME, 2), 0.2f, 4.0, 0.8f)   // a lane already there: written
        val w = p.writeTake(take, 16, append = false)
        assertEquals(1, w.settings)
        assertEquals(1, w.skipped)  // said, not silently dropped
        assertNull(w.project.automation.firstOrNull { it.target.kind == AutoTarget.LIVE })
    }

    @Test
    fun liveAndMacroTargets() {
        assertTrue(AutoTarget.live(AutoTarget.LIVE_ECHO_RETURN).isValid)
        assertFalse(AutoTarget(AutoTarget.LIVE, 0, 0, 0).isValid)
        // The phase 27 effects are targets; the settings in between (18..26) never are.
        assertTrue(AutoTarget.live(AutoTarget.LIVE_BACKSPIN).isValid && AutoTarget.live(AutoTarget.LIVE_RESET_ALL).isValid)
        assertFalse(AutoTarget.live(18).isValid)
        assertFalse(AutoTarget.live(26).isValid)
        assertFalse(AutoTarget.live(32).isValid)
        assertTrue(AutoTarget.macro(5, AutoTarget.MACRO_THROW_ECHO).isValid)
        assertFalse(AutoTarget.macro(5, AutoTarget.MACRO_THROW_ECHO + 1).isValid)
        assertTrue(AutoTarget.macro(5, AutoTarget.MACRO_KILL_LOW).isOnOff)
        assertTrue(AutoTarget.live(AutoTarget.LIVE_RISER).isOnOff && AutoTarget.live(AutoTarget.LIVE_KILL_HIGH).isOnOff)
        assertFalse(AutoTarget.live(AutoTarget.LIVE_BUILD_UP).isOnOff)  // a knob: its curve slides
        assertTrue(AutoTarget.macro(5, AutoTarget.MACRO_CHOP).isValid)
        assertFalse(AutoTarget.macro(Mixer.MASTER, 0).isValid)
        assertTrue(AutoTarget.macro(5, AutoTarget.MACRO_CHOP).isOnOff)
        assertFalse(AutoTarget.macro(5, 0).isOnOff)
        assertTrue(AutoTarget.live(AutoTarget.LIVE_TAPE_STOP).isOnOff)
        assertFalse(AutoTarget.live(AutoTarget.LIVE_FILTER).isOnOff)
    }

    /**
     * Take vs song, step by step, as the engine plays the written curves (a slide within a beat
     * toward the next point, held before a jump): the Montée dragged up then cut by the Drop on its
     * bar line, a locked DJ filter cleared by « Tout remettre » off the beat, a return tapped up
     * off the beat and let go. Wherever the take was still, the song sets exactly what was heard;
     * only around a gesture off the beat (moved to the nearest beat) or a drag may they differ.
     */
    @Test
    fun theSongSetsWhatTheTakeHeardUpToTheDropAndAfterOffBeatReleases() {
        val take = PerformanceTake(16, LiveState.on(0, 0))
        val build = AutoTarget.live(AutoTarget.LIVE_BUILD_UP)
        val filter = AutoTarget.live(AutoTarget.LIVE_FILTER)
        val reverb = AutoTarget.live(AutoTarget.LIVE_REVERB_RETURN)
        class G(val target: AutoTarget, val value: Float, val at: Double, val before: Float, val jump: Boolean = false)
        val moves = ArrayList<G>()
        // The Montée dragged from 0 to 0.8 (a move every 0.3 step), then left there.
        var b = 0f
        var at = 20.3
        while (b < 0.8f) {
            val v = minOf(0.8f, b + 0.03f)
            moves += G(build, v, at, b)
            b = v
            at += 0.3
        }
        // The DJ filter dragged to 0.2 and locked there.
        moves += G(filter, 0.3f, 33.3, 0.5f)
        moves += G(filter, 0.25f, 33.9, 0.3f)
        moves += G(filter, 0.2f, 34.5, 0.25f)
        // Drop pressed in bar 2: everything at rest on the bar line (step 48), at once.
        moves += G(build, 0f, 48.0, 0.8f, jump = true)
        // The reverb return tapped up off the beat.
        moves += G(reverb, 0.8f, 52.7, 0.5f)
        // « Tout remettre » off the beat: the filter back open at once.
        moves += G(filter, 0.5f, 61.4, 0.2f, jump = true)
        // The return let go back to as mixed, off the beat.
        moves += G(reverb, 0.5f, 77.0, 0.8f, jump = true)
        moves.sortedBy { it.at }.forEach { take.gesture(it.target, it.value, it.at, it.before, it.jump) }
        val w = project().writeTake(take, 96, append = false)
        val points = (96 - 16) / 4

        fun heard(t: AutoTarget, step: Int): Float {
            val list = moves.filter { it.target == t }
            return list.lastOrNull { it.at <= step }?.value ?: list.first().before
        }
        fun song(t: AutoTarget, step: Int): Float =
            w.project.automation.first { it.target == t }.songValueAtStep(step - 16, points, 16)!!

        // Where they may differ: a drag (sliding from beat to beat, to the nearest beat), and
        // between a gesture off the beat and the beat it is written on.
        val loose = mapOf(
            build to (16 until 36),
            filter to (29 until 39) + (60 until 62),
            reverb to (52 until 53) + (76 until 77),
        )
        for ((t, skip) in loose) {
            for (step in 16 until 96) {
                if (step in skip) continue
                assertEquals("${t.param} at step $step", heard(t, step), song(t, step), 1e-6f)
            }
        }
        // The build-up's last beat is all built up, then nothing from the bar line on.
        assertEquals(listOf(0.8f, 0.8f, 0.8f, 0.8f, 0f), (44..48).map { song(build, it) })
        // The effects did move: the song is not flat.
        assertTrue(song(build, 40) > 0.7f && song(filter, 50) < 0.25f && song(reverb, 60) > 0.75f)
        // The jumps survive saving.
        val lane = w.project.automation.first { it.target == build }
        assertTrue(lane.songJumps.isNotEmpty())
        assertEquals(lane, ProjectJson.decode(ProjectJson.encode(w.project)).automation.first { it.target == build })
    }
}
