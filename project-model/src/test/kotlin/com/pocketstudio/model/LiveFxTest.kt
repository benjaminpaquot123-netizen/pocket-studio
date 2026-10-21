package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** The Live page's effects (phase 27 research, the sound side): targets, settings, wobble, takes. */
class LiveFxTest {
    // The engine's PerformanceFx declaration (its enum of parameters).
    private val header = File("../engine/src/main/cpp/core/Effects.h").readText().substringAfter("class PerformanceFx {")

    private fun engineParam(name: String): Int = Regex("\\b$name = (\\d+)").find(header)!!.groupValues[1].toInt()

    @Test
    fun theAppAndTheEngineNumberTheLiveEffectsAlike() {
        val live = mapOf(
            AutoTarget.LIVE_FILTER to "Filter", AutoTarget.LIVE_REPEAT to "Repeat", AutoTarget.LIVE_DIVISION to "RepeatDivision",
            AutoTarget.LIVE_TAPE_STOP to "TapeStop", AutoTarget.LIVE_CHOP to "Chop", AutoTarget.LIVE_BACKSPIN to "Backspin",
            AutoTarget.LIVE_REVERSE to "Reverse", AutoTarget.LIVE_HALF_TIME to "HalfTime", AutoTarget.LIVE_ECHO_OUT to "EchoOut",
            AutoTarget.LIVE_KILL_LOW to "KillLow", AutoTarget.LIVE_KILL_MID to "KillMid", AutoTarget.LIVE_KILL_HIGH to "KillHigh",
            AutoTarget.LIVE_FREEZE to "ReverbFreeze", AutoTarget.LIVE_THROW_REVERB to "ThrowReverb", AutoTarget.LIVE_THROW_ECHO to "ThrowEcho",
            AutoTarget.LIVE_RISER to "Riser", AutoTarget.LIVE_DOWNLIFTER to "Downlifter", AutoTarget.LIVE_BUILD_UP to "BuildUp",
            AutoTarget.LIVE_DROP to "Drop", AutoTarget.LIVE_RESET_ALL to "ResetAll",
            LiveFxSettings.ENGINE_BRAKE_LENGTH to "BrakeLength", LiveFxSettings.ENGINE_BRAKE_RESTART to "BrakeRestart",
            LiveFxSettings.ENGINE_BACKSPIN_LENGTH to "BackspinLength", LiveFxSettings.ENGINE_CHOP_DIVISION to "ChopDivision",
            LiveFxSettings.ENGINE_HALF_TIME_LENGTH to "HalfTimeLength", LiveFxSettings.ENGINE_RELEASE to "ReleaseMode",
            LiveFxSettings.ENGINE_GRID to "Grid", LiveFxSettings.ENGINE_RISER_LENGTH to "RiserLength",
            LiveFxSettings.ENGINE_REVERSE_LENGTH to "ReverseLength",
        )
        live.forEach { (app, name) -> assertEquals(name, engineParam(name), app) }
        assertEquals(AutoTarget.LIVE_REVERB_RETURN, engineParam("kReverbReturn"))
        assertEquals(AutoTarget.LIVE_ECHO_RETURN, engineParam("kEchoReturn"))
        // The strips' kills and throws: the mixer's macro numbers.
        val mixer = File("../engine/src/main/cpp/core/Mixer.h").readText()
        assertEquals(AutoTarget.MACRO_KILL_LOW, Regex("kKillMacro = (\\d+)").find(mixer)!!.groupValues[1].toInt())
        assertEquals(AutoTarget.MACRO_THROW_REVERB, Regex("kThrowMacro = (\\d+)").find(mixer)!!.groupValues[1].toInt())
        // The engine's tables the settings index (same lengths).
        val cpp = File("../engine/src/main/cpp/core/LiveEffects.cpp").readText()
        fun table(name: String) = Regex("$name\\[(\\d+)\\]").find(cpp)!!.groupValues[1].toInt()
        assertEquals(LiveFxSettings.BRAKE_BEATS.size, table("kBrakeBeats"))
        assertEquals(LiveFxSettings.BACKSPIN_BEATS.size, table("kSpinBeats"))
        assertEquals(LiveFxSettings.RESTART_BEATS.size, table("kRestartBeats"))
        assertEquals(LiveFxSettings.REVERSE_STEPS.size, table("kReverseSteps"))
        assertEquals(LiveFxSettings.RISER_BARS.size, table("kRiserBars"))
    }

    @Test
    fun divisionsKeepTheOlderValuesAndRoundTrip() {
        // What the Live page sent before (1/8 = 1.5 / 4, the rest 0.5 = 1/16) and the old four chips.
        assertEquals(LiveDivision.EIGHTH, LiveDivision.ofValue(1.5f / 4f))
        assertEquals(LiveDivision.SIXTEENTH, LiveDivision.ofValue(AutoTarget.liveRest(AutoTarget.LIVE_DIVISION)))
        assertEquals(
            listOf(LiveDivision.QUARTER, LiveDivision.EIGHTH, LiveDivision.SIXTEENTH, LiveDivision.THIRTY_SECOND),
            (0..3).map { LiveDivision.ofValue((it + 0.5f) / 4f) },
        )
        LiveDivision.entries.forEach { assertEquals(it, LiveDivision.ofValue(it.engineValue)) }
        assertEquals(6, LiveDivision.ROLLS.size)
    }

    @Test
    fun settingsAreSavedOlderProjectsLoadAndBadValuesAreMended() {
        // An older project: no « liveFx » at all.
        val old = ProjectJson.decode("""{"name":"Ancien","tempoBpm":128.0}""")
        assertEquals(LiveFxSettings(), old.liveFx)
        val s = LiveFxSettings(grid = LiveFxSettings.GRID_BEAT, release = LiveFxSettings.RELEASE_BACKSPIN, brake = 4, restart = 2,
            backspin = 0, repeat = LiveDivision.SIXTEENTH_TRIPLET, chop = LiveDivision.THIRTY_SECOND, halfTimeBar = false, reverse = 0, riserBars = 8)
        val back = ProjectJson.decode(ProjectJson.encode(Project(liveFx = s)))
        assertEquals(s, back.liveFx)
        // Out of range (an edited file, a newer division name): mended, never refused.
        val bad = ProjectJson.decode("""{"liveFx":{"grid":9,"release":-3,"brake":40,"riserBars":3,"repeat":"QUINTUPLET"}}""").sanitized()
        assertEquals(LiveFxSettings.GRID_BEAT, bad.liveFx.grid)
        assertEquals(LiveFxSettings.RELEASE_CUT, bad.liveFx.release)
        assertEquals(LiveFxSettings.BRAKE_BEATS.lastIndex, bad.liveFx.brake)
        assertEquals(4, bad.liveFx.riserBars)
        assertEquals(LiveDivision.EIGHTH, bad.liveFx.repeat)
    }

    @Test
    fun settingsBecomeTheEnginesValues() {
        val params = LiveFxSettings(grid = LiveFxSettings.GRID_OFF, brake = 0, riserBars = 8).engineParams().toMap()
        // The engine reads each in bins of its count: the middle of the right one.
        assertTrue(params.getValue(LiveFxSettings.ENGINE_GRID) < 1f / 3f)
        assertTrue(params.getValue(LiveFxSettings.ENGINE_BRAKE_LENGTH) < 1f / 6f)
        assertTrue(params.getValue(LiveFxSettings.ENGINE_RISER_LENGTH) > 0.75f)
        assertEquals(LiveDivision.SIXTEENTH.engineValue, params.getValue(LiveFxSettings.ENGINE_CHOP_DIVISION))
        assertEquals(1f, params.getValue(LiveFxSettings.ENGINE_HALF_TIME_LENGTH))
        val beat = LiveFxSettings(grid = LiveFxSettings.GRID_BEAT).engineParams().toMap()
        assertTrue(beat.getValue(LiveFxSettings.ENGINE_GRID) > 2f / 3f)
        assertEquals(4, LiveFxSettings(grid = LiveFxSettings.GRID_BEAT).gridSteps)
        assertEquals(1, LiveFxSettings().gridSteps)  // a sixteenth by default
        assertEquals(0, LiveFxSettings(grid = LiveFxSettings.GRID_OFF).gridSteps)
    }

    @Test
    fun aGestureLandsOnTheNextLineOfTheGrid() {
        assertEquals(40.0, nextGridLine(37.2, 4), 1e-9)
        assertEquals(40.0, nextGridLine(40.0, 4), 1e-9)  // on a line: that one
        assertEquals(38.0, nextGridLine(37.2, 1), 1e-9)
        assertEquals(37.2, nextGridLine(37.2, 0), 1e-9)  // no grid: where it was
        assertEquals(48.0, nextGridLine(33.0, 16), 1e-9)
    }

    @Test
    fun theTargetsOfTheNewEffects() {
        AutoTarget.LIVE_GESTURES.forEach { assertTrue("$it", AutoTarget.live(it).isValid) }
        assertFalse(AutoTarget.LIVE_REVERB_RETURN in AutoTarget.LIVE_GESTURES)
        assertTrue(AutoTarget.isTrigger(AutoTarget.LIVE_RISER) && AutoTarget.isTrigger(AutoTarget.LIVE_DROP) && AutoTarget.isTrigger(AutoTarget.LIVE_THROW_ECHO))
        assertFalse(AutoTarget.isTrigger(AutoTarget.LIVE_BUILD_UP) || AutoTarget.isTrigger(AutoTarget.LIVE_KILL_LOW))
        assertEquals(AutoTarget.LIVE_KILL_HIGH, AutoTarget.liveKill(2))
        // A kill or a throw on a track strip is a macro of that strip (not the master's).
        assertTrue(AutoTarget.macro(2, AutoTarget.MACRO_KILL_MID).isValid)
        assertFalse(AutoTarget.macro(Mixer.MASTER, AutoTarget.MACRO_KILL_MID).isValid)
    }

    @Test
    fun aTakeWritesTheNewGesturesAsSongCurves() {
        val p = Project().sanitized()
        val take = PerformanceTake(0, LiveState.on(0, 0))
        // A riser pressed (and let go at once) on bar 2, a bass kill held over beats 5-7, the
        // Montée knob rising over beats 8-11.
        take.gesture(AutoTarget.live(AutoTarget.LIVE_RISER), 1f, 16.0, 0f)
        take.gesture(AutoTarget.live(AutoTarget.LIVE_RISER), 0f, 16.0, 1f)
        take.gesture(AutoTarget.live(AutoTarget.LIVE_KILL_LOW), 1f, 20.0, 0f)
        take.gesture(AutoTarget.live(AutoTarget.LIVE_KILL_LOW), 0f, 28.0, 1f)
        take.gesture(AutoTarget.live(AutoTarget.LIVE_BUILD_UP), 0.3f, 32.0, 0f)
        take.gesture(AutoTarget.live(AutoTarget.LIVE_BUILD_UP), 0.9f, 44.0, 0.3f)
        take.gesture(AutoTarget.macro(1, AutoTarget.MACRO_THROW_ECHO), 1f, 36.0, 0f)
        take.gesture(AutoTarget.macro(1, AutoTarget.MACRO_THROW_ECHO), 0f, 36.0, 1f)
        val w = p.writeTake(take, 64, append = false)
        assertEquals(4, w.settings)
        fun lane(t: AutoTarget) = w.project.automation.first { it.target == t }
        val riser = lane(AutoTarget.live(AutoTarget.LIVE_RISER)).songCurve(16)
        assertEquals(listOf(0f, 0f, 0f, 0f, 1f, 0f), riser.take(6))  // one beat on: its bar line
        assertTrue(lane(AutoTarget.live(AutoTarget.LIVE_RISER)).hold)
        val kill = lane(AutoTarget.live(AutoTarget.LIVE_KILL_LOW)).songCurve(16)
        assertEquals(listOf(0f, 1f, 1f, 0f), kill.subList(4, 8))
        val build = lane(AutoTarget.live(AutoTarget.LIVE_BUILD_UP))
        assertFalse(build.hold)  // a knob: it slides
        assertEquals(0.9f, build.songCurve(16)[11], 1e-6f)
        val thrown = lane(AutoTarget.macro(1, AutoTarget.MACRO_THROW_ECHO)).songCurve(16)
        assertEquals(listOf(0f, 1f, 0f), thrown.subList(8, 11))
    }

    @Test
    fun theWobbleAndItsPresets() {
        val w = EffectType.WOBBLE
        assertEquals(EffectGroup.MOTION, w.group)
        assertEquals("1/8", w.params[0].format(w.defaults[0]))
        assertEquals("1/2", w.params[0].format(0.12f))
        assertTrue(w.params[0].format(0.4f).contains("1/4"))  // the triplet quarter
        assertFalse(w.long)
        val wob = FxPresets.byId("wob_dubstep")!!
        val reese = FxPresets.byId("reese_dnb")!!
        assertTrue(wob.inserts.any { it.effect == EffectType.WOBBLE } && reese.inserts.any { it.effect == EffectType.WOBBLE })
        assertTrue(wob.inserts.size <= Mixer.INSERTS && reese.inserts.size <= Mixer.INSERTS)
        assertTrue(FxPresets.suggestedFor(SoundKind.BASS).containsAll(listOf(wob, reese)))
        // A track given the preset is recognised as having it.
        assertEquals(wob, Channel().withFx(wob).matchingFx())
    }
}
