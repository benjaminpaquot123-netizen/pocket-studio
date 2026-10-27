package com.pocketstudio.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.live.TakeUi
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.LiveDivision
import com.pocketstudio.model.LiveFxSettings
import com.pocketstudio.model.Mixer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * The Live page's effects through the ViewModel (no audio engine here): what is held, locked,
 * dropped and reset, and where a take writes each gesture (on the grid's line the engine plays it
 * on).
 */
@RunWith(RobolectricTestRunner::class)
class LiveFxVmTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    private fun fresh(): RhythmViewModel {
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        vm.toggleHit(0, 0)
        return vm
    }

    /** Playing, a take recording from step 16. */
    private fun recording(vm: RhythmViewModel) {
        vm.togglePlay()
        vm.takeClockForTest = 5
        vm.togglePerformance()
        vm.takeClockForTest = 16
        vm.tickPerformance()
        assertTrue(vm.take.value is TakeUi.Recording)
    }

    private fun laneOf(vm: RhythmViewModel, t: AutoTarget) = vm.project.value.automation.first { it.target == t }

    @Test
    fun rollsHoldTheirDivisionAndLetGoBackToRest() {
        val vm = fresh()
        vm.liveRoll(LiveDivision.EIGHTH_TRIPLET)
        assertEquals(LiveDivision.EIGHTH_TRIPLET, vm.liveRollDivision())
        assertTrue(vm.liveFx.value.on(AutoTarget.LIVE_REPEAT))
        vm.liveRoll(LiveDivision.THIRTY_SECOND)  // slid to another pad
        assertEquals(LiveDivision.THIRTY_SECOND, vm.liveRollDivision())
        vm.liveRoll(null)
        assertNull(vm.liveRollDivision())
        assertTrue(vm.liveFx.value.values.isEmpty())  // the division at rest too
        // The single « Répéter » button plays the project's division.
        vm.setLiveFxSettings(vm.project.value.liveFx.copy(repeat = LiveDivision.QUARTER))
        vm.liveHold(AutoTarget.LIVE_REPEAT, true)
        assertEquals(LiveDivision.QUARTER, vm.liveRollDivision())
        vm.liveHold(AutoTarget.LIVE_REPEAT, false)
        assertNull(vm.liveRollDivision())
    }

    @Test
    fun aLockedEffectStaysOnWhenLetGoUntilUnlocked() {
        val vm = fresh()
        vm.liveHold(AutoTarget.LIVE_HALF_TIME, true)
        vm.setLiveLock(AutoTarget.LIVE_HALF_TIME, true)
        vm.liveHold(AutoTarget.LIVE_HALF_TIME, false)  // the finger leaves
        assertTrue(vm.liveFx.value.on(AutoTarget.LIVE_HALF_TIME))
        assertEquals(setOf(AutoTarget.LIVE_HALF_TIME), vm.liveFx.value.locks)
        vm.setLiveLock(AutoTarget.LIVE_HALF_TIME, false)
        assertFalse(vm.liveFx.value.on(AutoTarget.LIVE_HALF_TIME))
        // The DJ filter: where the finger left it.
        vm.livePerformance(AutoTarget.LIVE_FILTER, 0.2f)
        vm.setLiveLock(AutoTarget.LIVE_FILTER, true)
        vm.livePerformance(AutoTarget.LIVE_FILTER, 0.5f)
        assertEquals(0.2f, vm.liveFx.value.value(AutoTarget.LIVE_FILTER, 0.5f))
        // Triggers cannot be locked.
        vm.setLiveLock(AutoTarget.LIVE_RISER, true)
        assertFalse(AutoTarget.LIVE_RISER in vm.liveFx.value.locks)
    }

    @Test
    fun resetAllPutsEverythingBackAtOnce() {
        val vm = fresh()
        vm.liveHold(AutoTarget.LIVE_BACKSPIN, true)
        vm.setLiveLock(AutoTarget.LIVE_BACKSPIN, true)
        vm.liveKill(0, true)
        vm.liveKill(2, true, ch = 1)
        vm.setLiveBuildUp(0.6f)
        vm.setLiveReturn(0, 0.9f)
        vm.liveTrackChop(1, true)
        vm.liveResetAll()
        val fx = vm.liveFx.value
        assertTrue(fx.values.isEmpty() && fx.locks.isEmpty() && fx.trackKills.isEmpty())
        assertEquals(listOf(0.5f, 0.5f), vm.liveReturns.value)
        assertTrue(vm.liveChops.value.isEmpty())
    }

    @Test
    fun theSettingsAreThePrjectsAndStayWithIt() {
        val vm = fresh()
        val s = LiveFxSettings(grid = LiveFxSettings.GRID_BEAT, release = LiveFxSettings.RELEASE_ECHO, restart = 1, riserBars = 8)
        vm.setLiveFxSettings(s)
        assertEquals(s, vm.project.value.liveFx)
        vm.setLiveFxSettings(s.copy(riserBars = 3))  // mended
        assertEquals(4, vm.project.value.liveFx.riserBars)
    }

    @Test
    fun aTakeWritesHeldEffectsOnTheLineTheEnginePlaysThem() {
        val vm = fresh()
        vm.setLiveFxSettings(LiveFxSettings(grid = LiveFxSettings.GRID_BEAT))
        recording(vm)
        // Backspin pressed at step 37 (heard there): the engine waits for the beat at step 40.
        vm.takeClockForTest = 37
        vm.liveHold(AutoTarget.LIVE_BACKSPIN, true)
        vm.takeClockForTest = 48
        vm.liveHold(AutoTarget.LIVE_BACKSPIN, false)  // let go: where it was heard
        // A riser on bar 4 (pressed in bar 3: the engine starts it on the next bar line, step 48).
        vm.takeClockForTest = 41
        vm.liveRiser()
        // The bass of track 1 cut (steps 49 -> 52, both on the beat line after).
        vm.takeClockForTest = 49
        vm.liveKill(0, true, ch = 1)
        vm.takeClockForTest = 54
        vm.liveKill(0, false, ch = 1)
        // A throw of the whole mix into the echo: one beat from step 60.
        vm.takeClockForTest = 57
        vm.liveThrow(1)
        vm.takeClockForTest = 66
        vm.tickPerformance()
        vm.takeClockForTest = 70  // past the bar's first beat: the bar is kept
        vm.togglePerformance()  // ends on step 80
        assertTrue(vm.take.value is TakeUi.Done)
        val spin = laneOf(vm, AutoTarget.live(AutoTarget.LIVE_BACKSPIN)).songCurve(16)
        assertEquals(listOf(0f, 1f, 1f, 0f), spin.subList(5, 9))  // song beats (40 - 16) / 4 = 6 and 7, off on 8
        val riser = laneOf(vm, AutoTarget.live(AutoTarget.LIVE_RISER)).songCurve(16)
        assertEquals(1f, riser[8])  // step 48 = song beat 8
        assertEquals(0f, riser[9])
        val kill = laneOf(vm, AutoTarget.macro(1, AutoTarget.MACRO_KILL_LOW)).songCurve(16)
        assertEquals(listOf(0f, 1f, 0f), kill.subList(8, 11))  // steps 52 and 56: beats 9 and 10
        val thrown = laneOf(vm, AutoTarget.live(AutoTarget.LIVE_THROW_ECHO)).songCurve(16)
        assertEquals(listOf(0f, 1f, 0f), thrown.subList(10, 13))  // step 60: beat 11
    }

    @Test
    fun theDropIsWrittenOnTheNextBarLineWithEverythingAtRest() {
        val vm = fresh()
        recording(vm)
        vm.takeClockForTest = 20
        vm.setLiveBuildUp(0.8f)
        vm.liveKill(1, true)
        vm.takeClockForTest = 37
        vm.liveDrop()  // the bar line after step 37: 48
        assertTrue(vm.liveFx.value.dropArmed)
        assertTrue(vm.liveFx.value.values.isEmpty())
        vm.takeClockForTest = 50
        vm.tickPerformance()
        vm.takeClockForTest = 53  // past the bar's first beat: the bar of the Drop is kept
        vm.togglePerformance()
        val build = laneOf(vm, AutoTarget.live(AutoTarget.LIVE_BUILD_UP)).songCurve(16)
        assertEquals(0.8f, build[7], 1e-6f)   // up to the bar line
        assertEquals(0f, build[8], 1e-6f)     // song beat (48 - 16) / 4
        val kill = laneOf(vm, AutoTarget.live(AutoTarget.LIVE_KILL_MID)).songCurve(16)
        assertEquals(1f, kill[7])
        assertEquals(0f, kill[8])
        val drop = laneOf(vm, AutoTarget.live(AutoTarget.LIVE_DROP)).songCurve(16)
        assertEquals(listOf(0f, 1f, 0f), drop.subList(7, 10))
        // As the engine plays the song, step by step: built up through the last beat (no slide
        // down over it: the rolls and the high-pass last to the bar line), nothing from it on.
        val lane = laneOf(vm, AutoTarget.live(AutoTarget.LIVE_BUILD_UP))
        assertEquals(listOf(0.8f, 0.8f, 0.8f, 0.8f, 0f), (28..32).map { lane.songValueAtStep(it, 16, 16) })
    }

    @Test
    fun leavingTheLivePageLeavesNothingOnUnseen() {
        val vm = fresh()
        vm.togglePlay()
        vm.setLiveBuildUp(0.7f)
        vm.livePerformance(AutoTarget.LIVE_FILTER, 0.2f)
        vm.setLiveLock(AutoTarget.LIVE_FILTER, true)
        vm.liveHold(AutoTarget.LIVE_CHOP, true)
        vm.setLiveLock(AutoTarget.LIVE_CHOP, true)
        vm.setLiveReturn(0, 0.9f)
        vm.liveKill(1, true, ch = 2)
        vm.liveTrackChop(1, true)
        vm.liveCommandsForTest = mutableListOf()
        vm.leaveLive()
        val fx = vm.liveFx.value
        assertTrue(fx.values.toString(), fx.values.isEmpty())
        assertTrue(fx.locks.isEmpty() && fx.trackKills.isEmpty())
        assertEquals(listOf(0.5f, 0.5f), vm.liveReturns.value)
        assertTrue(vm.liveChops.value.isEmpty())
        // The engine heard it (the Montée at rest, the filter open, the return as mixed).
        val sent = vm.liveCommandsForTest!!
        assertTrue(sent.toString(), "perf ${AutoTarget.LIVE_BUILD_UP} 0.0" in sent && "perf ${AutoTarget.LIVE_FILTER} 0.5" in sent && "return 0 1.0" in sent)
    }

    @Test
    fun leavingDuringATakeWritesTheMonteeBackAtRestAtOnce() {
        val vm = fresh()
        recording(vm)
        vm.takeClockForTest = 20
        vm.setLiveBuildUp(0.6f)
        vm.takeClockForTest = 42
        vm.leaveLive()
        vm.takeClockForTest = 50
        vm.tickPerformance()
        vm.togglePerformance()
        val lane = laneOf(vm, AutoTarget.live(AutoTarget.LIVE_BUILD_UP))
        // Song steps: up from 4 (step 20), back at rest from beat 7 (step 42 -> 44 nearest), at once.
        assertEquals(listOf(0.6f, 0.6f, 0.6f, 0.6f, 0f), (24..28).map { lane.songValueAtStep(it, 16, 16) })
    }

    @Test
    fun stopAlwaysPutsTheEnginesLiveEffectsAtRest() {
        val vm = fresh()
        vm.togglePlay()
        vm.liveCommandsForTest = mutableListOf()
        // A kill locked, then Drop: the ViewModel is at rest at once, the engine waits for the bar.
        vm.liveHold(AutoTarget.LIVE_KILL_LOW, true)
        vm.setLiveLock(AutoTarget.LIVE_KILL_LOW, true)
        vm.takeClockForTest = 5
        vm.liveDrop()  // on step 16
        assertTrue(vm.liveFx.value.values.isEmpty() && vm.liveFx.value.dropArmed)
        val sent = vm.liveCommandsForTest!!
        sent.clear()
        vm.togglePlay()  // Stop before the bar line
        // One reset to the engine whatever the ViewModel thinks (nothing stays on while stopped).
        assertTrue(sent.toString(), "perf ${AutoTarget.LIVE_RESET_ALL} 1.0" in sent && "perf ${AutoTarget.LIVE_RESET_ALL} 0.0" in sent)
        assertTrue(sent.toString(), "return 0 1.0" in sent && "return 1 1.0" in sent)
        // The kill's hold kept through the Drop's wait is let go too.
        assertTrue(sent.toString(), sent.any { it.startsWith("hold ") && it.endsWith(" false") })
    }

    @Test
    fun aPressBetweenTheDropAndItsBarLineIsCutThereAsInTheSong() {
        val vm = fresh()
        recording(vm)
        vm.liveCommandsForTest = mutableListOf()
        vm.takeClockForTest = 20
        vm.setLiveBuildUp(0.8f)
        vm.takeClockForTest = 37
        vm.liveDrop()  // the bar line after step 37: 48
        // Pressed after the Drop: it plays up to the bar line, then the Drop cuts it.
        vm.takeClockForTest = 41
        vm.liveHold(AutoTarget.LIVE_REVERSE, true)
        assertTrue(vm.liveFx.value.on(AutoTarget.LIVE_REVERSE))
        // Pressed in the last half beat: written on the Drop's beat, where all is at rest: not played.
        vm.takeClockForTest = 47
        vm.liveHold(AutoTarget.LIVE_KILL_MID, true)
        assertFalse(vm.liveFx.value.on(AutoTarget.LIVE_KILL_MID))
        assertFalse(vm.liveCommandsForTest!!.toString(), "perf ${AutoTarget.LIVE_KILL_MID} 1.0" in vm.liveCommandsForTest!!)
        // The bar line passes: the reverse shows cut, though the finger is still down.
        vm.takeClockForTest = 49
        vm.tickPerformance()
        assertFalse(vm.liveFx.value.on(AutoTarget.LIVE_REVERSE))
        assertFalse(vm.liveFx.value.dropArmed)
        vm.liveHold(AutoTarget.LIVE_REVERSE, false)  // let go: nothing more
        vm.liveHold(AutoTarget.LIVE_KILL_MID, false)
        vm.takeClockForTest = 60
        vm.tickPerformance()
        vm.togglePerformance()
        // The song: the reverse from beat 6 (step 41 -> 40) to the Drop's beat 8, then off.
        val reverse = laneOf(vm, AutoTarget.live(AutoTarget.LIVE_REVERSE)).songCurve(16)
        assertEquals(listOf(0f, 1f, 1f, 0f, 0f), reverse.subList(5, 10))
        assertTrue(vm.project.value.automation.none { it.target == AutoTarget.live(AutoTarget.LIVE_KILL_MID) })
    }

    /** Audit 28 live M2: « Tout remettre » cancels a Drop waiting (as the engine does). */
    @Test
    fun resetAllCancelsAWaitingDropSoWhatIsHeldAfterItIsLetGo() {
        val vm = fresh()
        recording(vm)
        vm.takeClockForTest = 20
        vm.setLiveBuildUp(0.8f)
        vm.takeClockForTest = 37
        vm.liveDrop()  // waits for the bar line at 48
        vm.takeClockForTest = 38
        vm.liveResetAll()
        assertFalse(vm.liveFx.value.dropArmed)
        vm.takeClockForTest = 40
        vm.liveHold(AutoTarget.LIVE_CHOP, true)
        vm.liveCommandsForTest = mutableListOf()
        // The bar line passes, the finger still down: the chopper goes on (no Drop came).
        vm.takeClockForTest = 50
        vm.tickPerformance()
        assertTrue(vm.liveFx.value.on(AutoTarget.LIVE_CHOP))
        vm.takeClockForTest = 53
        vm.liveHold(AutoTarget.LIVE_CHOP, false)
        val sent = vm.liveCommandsForTest!!
        assertTrue(sent.toString(), "perf ${AutoTarget.LIVE_CHOP} 0.0" in sent)
        vm.takeClockForTest = 66
        vm.tickPerformance()
        vm.togglePerformance()
        // The song: the Montée at rest from the reset (step 38: song beat 6), not from the Drop's
        // bar (beat 8); no Drop; the chopper from step 40 (beat 6) to its let-go (beat 9).
        val build = laneOf(vm, AutoTarget.live(AutoTarget.LIVE_BUILD_UP)).songCurve(16)
        assertEquals(0.8f, build[5], 1e-6f)
        assertEquals(0f, build[6], 1e-6f)
        val drop = vm.project.value.automation.firstOrNull { it.target == AutoTarget.live(AutoTarget.LIVE_DROP) }
        assertTrue(drop == null || drop.songCurve(16).all { it == 0f })
        val chop = laneOf(vm, AutoTarget.live(AutoTarget.LIVE_CHOP)).songCurve(16)
        assertEquals(listOf(1f, 1f, 1f, 0f), chop.subList(6, 10))
    }

    /** Audit 28 live M3: a tap let go before the grid line its press waits for: not played, not written. */
    @Test
    fun aTapShorterThanTheGridWaitIsNeitherPlayedNorWritten() {
        val vm = fresh()
        vm.setLiveFxSettings(LiveFxSettings(grid = LiveFxSettings.GRID_BEAT))
        recording(vm)
        vm.takeClockForTest = 37
        vm.liveHold(AutoTarget.LIVE_CHOP, true)  // waits for step 40
        vm.takeClockForTest = 38
        vm.liveHold(AutoTarget.LIVE_CHOP, false)
        vm.takeClockForTest = 41
        vm.liveKill(0, true, ch = 1)  // a track's bass, waits for 44
        vm.takeClockForTest = 42
        vm.liveKill(0, false, ch = 1)
        vm.takeClockForTest = 45
        vm.liveKill(0, true)  // the whole mix's bass, waits for 48
        vm.takeClockForTest = 46
        vm.liveKill(0, false)
        // Let go after its line: played, and written (a beat at least).
        vm.takeClockForTest = 49
        vm.liveHold(AutoTarget.LIVE_REVERSE, true)  // 52
        vm.takeClockForTest = 53
        vm.liveHold(AutoTarget.LIVE_REVERSE, false)
        vm.takeClockForTest = 66
        vm.tickPerformance()
        vm.togglePerformance()
        val lanes = vm.project.value.automation
        for (t in listOf(AutoTarget.live(AutoTarget.LIVE_CHOP), AutoTarget.macro(1, AutoTarget.MACRO_KILL_LOW), AutoTarget.live(AutoTarget.LIVE_KILL_LOW))) {
            val lane = lanes.firstOrNull { it.target == t }
            assertTrue("$t: ${lane?.songValues}", lane == null || lane.songCurve(16).all { it == 0f })
        }
        val reverse = laneOf(vm, AutoTarget.live(AutoTarget.LIVE_REVERSE)).songCurve(16)
        assertEquals(listOf(0f, 1f, 0f), reverse.subList(8, 11))
    }

    /**
     * Audit 28 live M4: an effect's setting (the Wobble pad, « Effets… »), a fader, a MIDI knob on
     * the live filter or the 303, played during a take, are written into the song where they were.
     */
    @Test
    fun aTakeWritesEffectsFadersAndMidiKnobsPlayedDuringIt() {
        val vm = fresh()
        vm.setInsertType(2, 0, com.pocketstudio.model.EffectType.EQ)
        vm.learn(AutoTarget(com.pocketstudio.model.MidiMapping.PERFORMANCE, Mixer.MASTER, 0, AutoTarget.LIVE_FILTER))
        vm.midiControl(0, 20, 0)
        vm.learn(AutoTarget.acid(0))
        vm.midiControl(0, 21, 0)
        val savedCutoff = vm.project.value.acid.sound.values()[0]
        recording(vm)
        vm.takeClockForTest = 24
        vm.setInsertParam(2, 0, 0, 0.9f)
        vm.setChannelVolume(2, 0.3f)
        vm.midiControl(0, 20, 25)   // the live filter down
        vm.midiControl(0, 21, 127)  // the 303's cutoff open
        assertEquals(25 / 127f, vm.liveFx.value.value(AutoTarget.LIVE_FILTER, 0.5f), 1e-6f)  // followed by the page
        vm.takeClockForTest = 50
        vm.tickPerformance()
        vm.togglePerformance()
        // Song beat (24 - 16) / 4 = 2: each at its new value from there, as before it up to there.
        val eq = laneOf(vm, AutoTarget(AutoTarget.INSERT_PARAM, 2, 0, 0)).songCurve(8)
        assertEquals(0.5f, eq[1], 1e-6f)
        assertEquals(0.9f, eq[2], 1e-6f)
        assertEquals(0.3f, laneOf(vm, AutoTarget(AutoTarget.VOLUME, 2)).songCurve(8)[2], 1e-6f)
        assertEquals(25 / 127f, laneOf(vm, AutoTarget.live(AutoTarget.LIVE_FILTER)).songCurve(8)[2], 1e-6f)
        assertEquals(1f, laneOf(vm, AutoTarget.acid(0)).songCurve(8)[2], 1e-6f)
        // The 303's saved setting stays (the take plays it as the Live page does).
        assertEquals(savedCutoff, vm.project.value.acid.sound.values()[0], 1e-6f)
    }

    @Test
    fun stopEndsEveryLiveEffect() {
        val vm = fresh()
        vm.togglePlay()
        vm.liveHold(AutoTarget.LIVE_ECHO_OUT, true)
        vm.liveKill(2, true, ch = Mixer.MASTER)
        vm.setLiveLock(AutoTarget.LIVE_ECHO_OUT, true)
        vm.liveKill(0, true, ch = 2)
        vm.togglePlay()
        assertEquals(com.pocketstudio.app.live.LiveFxUi(), vm.liveFx.value)
    }
}
