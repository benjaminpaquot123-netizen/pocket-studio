package com.pocketstudio.app

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.live.LiveActions
import com.pocketstudio.app.live.LiveColumn
import com.pocketstudio.app.live.LiveScreen
import com.pocketstudio.app.live.LiveScreenUi
import com.pocketstudio.app.live.StartWait
import com.pocketstudio.app.live.XyMode
import com.pocketstudio.app.live.beatsLabel
import com.pocketstudio.app.live.latchButtonCompositions
import com.pocketstudio.app.live.launchWaitLeft
import com.pocketstudio.app.live.liveXyPadCompositions
import com.pocketstudio.app.live.livePadCompositions
import com.pocketstudio.app.live.xyModesFor
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.session.Perf
import com.pocketstudio.app.ui.liveActionsOf
import com.pocketstudio.model.AcidPattern
import com.pocketstudio.model.AcidStep
import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.Insert
import com.pocketstudio.model.LiveDivision
import com.pocketstudio.model.LiveFxSettings
import com.pocketstudio.model.LiveState
import com.pocketstudio.model.Macros
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.Project
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongPart
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * The Live effects panel, mostly with the ViewModel behind it (no audio engine): its pages, the
 * lock, « Tout remettre », the pad's modes, the roll pads, Montée and Drop, the settings, the beat
 * ticks, and what the clock and the sound may only redraw.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class LiveFxLockTest {
    @get:Rule
    val compose = createComposeRule()
    private val app: Application = ApplicationProvider.getApplicationContext()

    private val acid = AcidPattern(steps = List(AcidPattern.MAX_STEPS) { AcidStep(gate = it % 4 == 0) })
    private val project = Project(
        scenes = listOf(Scene("A", DrumPattern().toggle(0, 0), acid = acid, label = "Intro"), Scene("B", DrumPattern().toggle(1, 4))),
        scene = 0, acid = acid,
    ).sanitized()
    private val noAcid = project.copy(acid = AcidPattern(), scenes = project.scenes.map { it.copy(acid = AcidPattern()) }).sanitized()

    private fun freshVm(): RhythmViewModel {
        File(app.filesDir, "projects").deleteRecursively()
        return RhythmViewModel(app).also { it.toggleHit(0, 0) }
    }

    /** The Live page as the app shows it, on [vm]'s state and gestures. */
    @Composable
    private fun Page(vm: RhythmViewModel) {
        val p by vm.project.collectAsState()
        val fx by vm.liveFx.collectAsState()
        val returns by vm.liveReturns.collectAsState()
        val chops by vm.liveChops.collectAsState()
        val acid by vm.liveAcid.collectAsState()
        PocketStudioTheme {
            LiveScreen(LiveScreenUi(p, LiveState.on(0), clock = 5, playing = true, acid = acid, returns = returns, chops = chops, fx = fx), liveActionsOf(vm))
        }
    }

    private fun showVm(vm: RhythmViewModel) = compose.setContent { Page(vm) }

    private fun show(ui: LiveScreenUi, actions: LiveActions) =
        compose.setContent { PocketStudioTheme { LiveScreen(ui, actions) } }

    private fun tap(tag: String) {
        val node = compose.onNodeWithTag(tag)
        // Only a track's page scrolls; the whole mix's pages fit.
        runCatching { node.performScrollTo() }
        node.performTouchInput { down(center); up() }
    }

    private fun exists(tag: String) = compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    private fun page(name: String) = compose.onNodeWithTag("live_page_$name").performClick()

    @Test
    fun withTheLockAHeldEffectStaysUntilTouchedAgain() {
        val vm = freshVm()
        showVm(vm)
        page("HOLDS")
        compose.onNodeWithTag("live_lock").performClick()
        compose.onNodeWithTag("live_lock").assertIsSelected()
        tap("live_hold_brake")
        // Lifted: still on, locked in the ViewModel, and lit.
        assertTrue(vm.liveFx.value.on(AutoTarget.LIVE_TAPE_STOP))
        assertEquals(setOf(AutoTarget.LIVE_TAPE_STOP), vm.liveFx.value.locks)
        compose.onNodeWithTag("live_hold_brake").assertIsSelected()
        // Touched again: let go.
        tap("live_hold_brake")
        assertFalse(vm.liveFx.value.on(AutoTarget.LIVE_TAPE_STOP))
        assertTrue(vm.liveFx.value.locks.isEmpty())
        compose.onNodeWithTag("live_hold_brake").assertIsNotSelected()
        // Unlocked: a hold lasts as long as the finger.
        compose.onNodeWithTag("live_lock").performClick()
        compose.onNodeWithTag("live_kill_0").performTouchInput { down(center) }
        assertTrue(vm.liveFx.value.on(AutoTarget.LIVE_KILL_LOW))
        compose.onNodeWithTag("live_kill_0").performTouchInput { up() }
        assertFalse(vm.liveFx.value.on(AutoTarget.LIVE_KILL_LOW))
    }

    @Test
    fun unlockingLetsGoOfWhatTheLockKept() {
        val vm = freshVm()
        showVm(vm)
        page("HOLDS")
        compose.onNodeWithTag("live_lock").performClick()
        tap("live_hold_chop")
        tap("live_kill_2")
        // A track's kill is kept by the page itself.
        compose.onNodeWithTag("live_fx_drums").performScrollTo().performClick()
        tap("live_track_kill_drums_0")
        assertTrue(vm.liveFx.value.on(AutoTarget.LIVE_CHOP) && vm.liveFx.value.on(AutoTarget.LIVE_KILL_HIGH))
        assertEquals(setOf(0), vm.liveFx.value.trackKills[0])
        compose.onNodeWithTag("live_lock").performClick()
        assertTrue(vm.liveFx.value.values.toString(), vm.liveFx.value.values.isEmpty())
        assertTrue(vm.liveFx.value.locks.isEmpty())
        assertTrue(vm.liveFx.value.trackKills.isEmpty())
    }

    @Test
    fun theDjFilterSpringsBackUnlessLocked() {
        val vm = freshVm()
        showVm(vm)
        val left = Offset(20f, 0f)
        compose.onNodeWithTag("xy_pad").performScrollTo().performTouchInput { down(centerLeft + left); up() }
        assertFalse(AutoTarget.LIVE_FILTER in vm.liveFx.value.values)
        compose.onNodeWithTag("live_lock").performClick()
        compose.onNodeWithTag("xy_pad").performScrollTo().performTouchInput { down(centerLeft + left); up() }
        // Locked: it stays dark (the filter far left).
        assertTrue(vm.liveFx.value.value(AutoTarget.LIVE_FILTER, 0.5f) < 0.2f)
        assertTrue(AutoTarget.LIVE_FILTER in vm.liveFx.value.locks)
        // « Tout remettre »: back to rest, nothing kept.
        compose.onNodeWithTag("live_reset").performClick()
        assertTrue(vm.liveFx.value.values.isEmpty() && vm.liveFx.value.locks.isEmpty())
    }

    @Test
    fun toutRemettreGoesToTheEnginesReset() {
        val vm = freshVm()
        showVm(vm)
        vm.setLiveReturn(0, 0.9f)
        page("HOLDS")
        compose.onNodeWithTag("live_lock").performClick()
        tap("live_hold_backspin")
        page("BUILD")
        compose.onNodeWithTag("live_build_up").performTouchInput { down(centerRight - Offset(30f, 0f)); up() }
        assertTrue(vm.liveFx.value.value(AutoTarget.LIVE_BUILD_UP) > 0.8f)
        compose.onNodeWithTag("live_reset").performClick()
        assertTrue(vm.liveFx.value.values.isEmpty())
        assertEquals(listOf(0.5f, 0.5f), vm.liveReturns.value)
        page("HOLDS")
        compose.onNodeWithTag("live_hold_backspin").assertIsNotSelected()
    }

    @Test
    fun withoutTheEnginesResetThePageResetsWhatItHas() {
        val calls = mutableListOf<String>()
        val ui = LiveScreenUi(project, LiveState.on(0), clock = 5, playing = true, acid = mapOf(0 to 0.9f, 7 to 0.2f), returns = listOf(0.8f, 0.1f), chops = setOf(1))
        show(
            ui,
            LiveActions(
                performance = { p, v -> calls += "perf $p $v" },
                setReturn = { b, v -> calls += "return $b $v" },
                holdAcid = { p, h -> if (!h) calls += "let go $p" },
                resetAcid = { p -> calls += "acid $p" },
                trackChop = { ch, on -> calls += "chop $ch $on" },
            ),
        )
        compose.onNodeWithTag("live_reset").performClick()
        listOf(
            "perf ${Perf.REPEAT} 0.0", "perf ${Perf.TAPE_STOP} 0.0", "perf ${Perf.CHOP} 0.0", "perf ${Perf.FILTER} 0.5", "perf ${Perf.RESONANCE} 0.3",
            "return 0 0.5", "return 1 0.5", "acid 0", "acid 7", "chop 1 false", "let go 0", "let go 1",
        ).forEach { assertTrue("$it in $calls", it in calls) }
    }

    @Test
    fun theRollPadsRepeatWhileHeldAndFollowTheFinger() {
        val vm = freshVm()
        showVm(vm)
        page("ROLLS")
        compose.onNodeWithTag("live_roll_SIXTEENTH").performTouchInput { down(center) }
        assertEquals(LiveDivision.SIXTEENTH, vm.liveRollDivision())
        compose.onNodeWithTag("live_roll_SIXTEENTH").assertIsSelected()
        // Slid onto 1/8 (the pad to its left).
        compose.onNodeWithTag("live_roll_SIXTEENTH").performTouchInput { moveBy(Offset(-width.toFloat(), 0f)) }
        assertEquals(LiveDivision.EIGHTH, vm.liveRollDivision())
        compose.onNodeWithTag("live_roll_SIXTEENTH").performTouchInput { up() }
        assertNull(vm.liveRollDivision())
        assertFalse(AutoTarget.LIVE_DIVISION in vm.liveFx.value.values)
        // Locked: it keeps rolling, the next touch stops it.
        compose.onNodeWithTag("live_lock").performClick()
        tap("live_roll_EIGHTH_TRIPLET")
        assertEquals(LiveDivision.EIGHTH_TRIPLET, vm.liveRollDivision())
        tap("live_roll_QUARTER")
        assertNull(vm.liveRollDivision())
    }

    @Test
    fun theDropWaitsForItsBarAndPutsEverythingBack() {
        val vm = freshVm()
        vm.togglePlay()
        vm.takeClockForTest = 5  // the bar line comes at step 16
        showVm(vm)
        page("BUILD")
        compose.onNodeWithTag("live_build_up").performTouchInput { down(center); up() }
        assertEquals(0.5f, vm.liveFx.value.value(AutoTarget.LIVE_BUILD_UP), 0.05f)
        compose.onNodeWithTag("live_drop").performClick()
        assertTrue(vm.liveFx.value.dropArmed)
        compose.onNodeWithTag("live_drop").assertIsSelected()
        assertFalse(AutoTarget.LIVE_BUILD_UP in vm.liveFx.value.values)
        vm.togglePlay()
    }

    @Test
    fun theDubAndMonteePagesThrowFreezeAndRise() {
        val calls = mutableListOf<String>()
        show(
            LiveScreenUi(project, LiveState.on(0), clock = 5, playing = true),
            LiveActions(throwFx = { b, ch -> calls += "throw $b $ch" }, hold = { p, on -> calls += "hold $p $on" }, riser = { calls += "riser $it" }),
        )
        page("DUB")
        compose.onNodeWithTag("live_throw_1").performClick()
        tap("live_hold_freeze")
        page("BUILD")
        compose.onNodeWithTag("live_downlifter").performClick()
        assertEquals(listOf("throw 1 ${Mixer.MASTER}", "hold ${AutoTarget.LIVE_FREEZE} true", "hold ${AutoTarget.LIVE_FREEZE} false", "riser true"), calls)
    }

    @Test
    fun theSettingsAreChosenOnTheirPage() {
        val vm = freshVm()
        showVm(vm)
        compose.onNodeWithTag("live_fx_settings").performClick()
        compose.onNodeWithTag("live_fx_settings_page").assertExists()
        compose.onNodeWithTag("live_set_grid_2").performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithTag("live_set_release_3").performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithTag("live_set_riser_3").performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithTag("live_set_half_0").performSemanticsAction(SemanticsActions.OnClick)
        val s = vm.project.value.liveFx
        assertEquals(LiveFxSettings.GRID_BEAT, s.grid)
        assertEquals(LiveFxSettings.RELEASE_BACKSPIN, s.release)
        assertEquals(8, s.riserBars)
        assertFalse(s.halfTimeBar)
        // (Chosen by their click action: the rows are taller than the page here.)
        // The gear again: back to the pad.
        compose.onNodeWithTag("live_fx_settings").performClick()
        compose.onNodeWithTag("xy_pad").assertIsDisplayed()
        assertEquals("1 temps", beatsLabel(1.0))
        assertEquals("1/4 temps", beatsLabel(0.25))
        assertEquals("8 temps", beatsLabel(8.0))
    }

    @Test
    fun theModesChangeWhatThePadPlays() {
        val calls = mutableListOf<String>()
        val wobbly = project.copy(
            mixer = project.mixer.copy(
                channels = project.mixer.channels.mapIndexed { i, c ->
                    if (i == 0) c.copy(inserts = listOf(Insert(EffectType.WOBBLE.ordinal, EffectType.WOBBLE.defaults)) + c.inserts.drop(1)) else c
                },
            ),
        ).sanitized()
        show(
            LiveScreenUi(wobbly, LiveState.on(0), clock = 5, playing = true),
            LiveActions(
                setReturn = { b, v -> calls += "return $b ${"%.1f".format(v)}" },
                setAcid = { p, _ -> calls += "acid $p" },
                fx = com.pocketstudio.app.mixer.QuickFxActions(setMacroPair = { ch, x, _, y, _ -> calls += "pair $ch $x $y" }),
                setInsertParam = { ch, slot, k, _ -> calls += "insert $ch $slot $k" },
            ),
        )
        compose.onNodeWithTag("live_xy_mode_DJ_FILTER").assertIsSelected()
        compose.onNodeWithTag("live_xy_mode_SPACE").performClick()
        compose.onNodeWithTag("live_xy_space").performScrollTo().performTouchInput { down(percentOffset(0.97f, 0.08f)); up() }
        assertTrue(calls.toString(), "return 0 1.0" in calls && "return 1 1.0" in calls)
        compose.onNodeWithTag("live_xy_mode_ACID").performClick()
        compose.onNodeWithTag("live_acid_xy").performScrollTo().performTouchInput { down(center); up() }
        assertTrue(calls.toString(), "acid 0" in calls && "acid 1" in calls)
        // A track: its own modes; « Son cassé » plays its dirt × colour, Wobble its wobble's rate × depth.
        compose.onNodeWithTag("live_fx_drums").performScrollTo().performClick()
        compose.onNodeWithTag("live_xy_mode_COLOUR").assertIsSelected()
        assertFalse(exists("live_xy_mode_ACID"))
        compose.onNodeWithTag("live_xy_mode_CRUSH").performClick()
        compose.onNodeWithTag("live_xy_crush_drums").performScrollTo().performTouchInput { down(center); up() }
        assertTrue(calls.toString(), "pair 0 ${Macros.DIRT} ${Macros.COLOR}" in calls)
        compose.onNodeWithTag("live_xy_mode_WOBBLE").performScrollTo().performClick()
        compose.onNodeWithTag("live_xy_wobble_drums").performScrollTo().performTouchInput { down(center); up() }
        assertTrue(calls.toString(), "insert 0 0 0" in calls && "insert 0 0 1" in calls)
        // Another track has no wobble: no Wobble mode.
        compose.onNodeWithTag("live_fx_acid").performScrollTo().performClick()
        assertFalse(exists("live_xy_mode_WOBBLE"))
        // Back to the whole mix: its mode was kept.
        page("PAD")
        compose.onNodeWithTag("live_xy_mode_ACID").assertIsSelected()
    }

    @Test
    fun theModesOfferedFollowTheTargetAndTheSong() {
        val acidCol = LiveColumn(SongPart.ACID, "Acide", androidx.compose.ui.graphics.Color.Green, 1)
        val drums = LiveColumn(SongPart.DRUMS, "Batterie", androidx.compose.ui.graphics.Color.Blue, 0)
        assertEquals(listOf(XyMode.DJ_FILTER, XyMode.ACID, XyMode.SPACE), xyModesFor(null, hasAcid = true))
        assertEquals(listOf(XyMode.DJ_FILTER, XyMode.SPACE), xyModesFor(null, hasAcid = false))
        assertEquals(listOf(XyMode.COLOUR, XyMode.CRUSH, XyMode.SPACE, XyMode.ACID), xyModesFor(acidCol, hasAcid = true))
        assertEquals(listOf(XyMode.COLOUR, XyMode.CRUSH, XyMode.SPACE), xyModesFor(drums, hasAcid = true))
        assertEquals(listOf(XyMode.COLOUR, XyMode.CRUSH, XyMode.SPACE, XyMode.WOBBLE), xyModesFor(drums, true, setOf(XyMode.WOBBLE)))
        // Without a 303 line, no Acide mode: the DJ filter shows.
        show(LiveScreenUi(noAcid, LiveState.on(0), clock = 5, playing = true), LiveActions())
        assertFalse(exists("live_xy_mode_ACID"))
        compose.onNodeWithTag("xy_pad").assertIsDisplayed()
    }

    @Test
    fun aStartCountsDownToItsLine() {
        assertEquals(1f, launchWaitLeft(-1, 16))
        assertEquals(1f, launchWaitLeft(16, 16))
        assertEquals(0.25f, launchWaitLeft(12, 16))
        assertEquals(0.5f, launchWaitLeft(40, 16))
        val clock = intArrayOf(5)
        val wait = StartWait { clock[0] }
        wait.arm(4)
        assertEquals(0.75f, wait.left()!!, 1e-6f)  // step 5: three steps to the beat line at 8
        clock[0] = 7
        assertEquals(0.25f, wait.left()!!, 1e-6f)
        clock[0] = 8
        assertNull(wait.left())
        wait.arm(1)  // a sixteenth: too short to show
        assertNull(wait.left())
    }

    private class CountingHaptics : HapticFeedback {
        var count = 0
        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            count++
        }
    }

    /** Vibrations felt while the clock goes through four beats, with the option [on] or off. */
    private fun ticksOverFourBeats(on: Boolean): Int {
        val haptics = CountingHaptics()
        val clock = mutableIntStateOf(0)
        val ui = LiveScreenUi(project, LiveState.on(0), playing = true, beatHaptics = on, clockOf = { clock.intValue })
        compose.setContent {
            CompositionLocalProvider(LocalHapticFeedback provides haptics) { PocketStudioTheme { LiveScreen(ui, LiveActions()) } }
        }
        compose.waitForIdle()
        val start = haptics.count
        repeat(16) {
            clock.intValue += 1
            compose.waitForIdle()
        }
        return haptics.count - start
    }

    @Test
    fun theBeatIsFeltWhenAskedFor() = assertEquals(4, ticksOverFourBeats(true))

    @Test
    fun theBeatIsNotFeltByDefault() {
        assertEquals(false, LiveScreenUi(project).beatHaptics)
        assertEquals(0, ticksOverFourBeats(false))
    }

    @Test
    fun neitherTheClockNorTheSoundRecomposeThePad() {
        val clock = mutableIntStateOf(0)
        val bands = mutableStateOf<FloatArray?>(null)
        val ui = LiveScreenUi(project, LiveState.on(0), playing = true, clockOf = { clock.intValue }, spectrum = { bands.value })
        compose.mainClock.autoAdvance = false
        show(ui, LiveActions())
        compose.mainClock.advanceTimeBy(100)
        compose.waitForIdle()
        val pads = liveXyPadCompositions
        assertTrue(pads > 0)
        repeat(20) { i ->
            clock.intValue += 1
            bands.value = FloatArray(40) { -60f + i }
            compose.mainClock.advanceTimeBy(66)
        }
        compose.waitForIdle()
        assertEquals(pads, liveXyPadCompositions)
    }

    @Test
    fun theClockOnlyRedrawsAHoldWaitingForItsBeat() {
        val clock = mutableIntStateOf(5)
        val beatGrid = project.copy(liveFx = LiveFxSettings(grid = LiveFxSettings.GRID_BEAT))
        val ui = LiveScreenUi(beatGrid, LiveState.on(0), playing = true, clockOf = { clock.intValue })
        show(ui, LiveActions())
        page("HOLDS")
        compose.onNodeWithTag("live_hold_brake").performTouchInput { down(center) }
        compose.waitForIdle()
        val buttons = latchButtonCompositions
        compose.mainClock.autoAdvance = false
        repeat(12) {
            clock.intValue += 1
            compose.mainClock.advanceTimeBy(66)
        }
        compose.waitForIdle()
        assertEquals(buttons, latchButtonCompositions)
        compose.mainClock.autoAdvance = true
        compose.onNodeWithTag("live_hold_brake").performTouchInput { up() }
    }

    @Test
    fun theLockThePageAndWhatItKeptSurviveARotation() {
        val vm = freshVm()
        val restore = StateRestorationTester(compose)
        restore.setContent { Page(vm) }
        page("HOLDS")
        compose.onNodeWithTag("live_lock").performClick()
        tap("live_hold_brake")
        compose.onNodeWithTag("live_fx_drums").performScrollTo().performClick()
        tap("live_track_chop_drums")
        assertEquals(setOf(0), vm.liveChops.value)
        // The activity is recreated (no configChanges): the page comes back as it was.
        restore.emulateSavedInstanceStateRestore()
        assertEquals(setOf(AutoTarget.LIVE_TAPE_STOP), vm.liveFx.value.locks)
        assertTrue(vm.liveFx.value.on(AutoTarget.LIVE_TAPE_STOP))
        assertEquals(setOf(0), vm.liveChops.value)
        compose.onNodeWithTag("live_lock").assertIsSelected()
        compose.onNodeWithTag("live_fx_drums").assertIsSelected()
        // The chopper still kept by the page: one touch lets it go.
        tap("live_track_chop_drums")
        assertTrue(vm.liveChops.value.isEmpty())
    }

    @Test
    fun aTrackButtonKeptByTheLockShowsTheDropLetItGo() {
        val vm = freshVm()
        showVm(vm)
        compose.onNodeWithTag("live_lock").performClick()
        compose.onNodeWithTag("live_fx_drums").performScrollTo().performClick()
        tap("live_track_kill_drums_0")
        assertEquals(setOf(0), vm.liveFx.value.trackKills[0])
        compose.onNodeWithTag("live_track_kill_drums_0").assertIsSelected()
        vm.liveDrop()  // stopped: everything at rest at once
        compose.waitForIdle()
        // Neither lit nor padlocked, and one touch plays it again (kept: the lock is on).
        compose.onNodeWithTag("live_track_kill_drums_0").assertIsNotSelected()
        tap("live_track_kill_drums_0")
        assertEquals(setOf(0), vm.liveFx.value.trackKills[0])
        compose.onNodeWithTag("live_track_kill_drums_0").assertIsSelected()
    }

    @Test
    fun theMonteeAndTheFilterMovedDoNotRecomposeTheGrid() {
        val vm = freshVm()
        compose.setContent {
            PocketStudioTheme {
                com.pocketstudio.app.ui.LiveRoute(
                    vm, androidx.lifecycle.compose.LocalLifecycleOwner.current, close = {}, openEffects = {},
                    padsActions = com.pocketstudio.app.session.LivePadsActions(), liveEditing = null,
                )
            }
        }
        page("BUILD")
        compose.waitForIdle()
        val pads = livePadCompositions
        assertTrue(pads > 0)
        // A finger on the Montée and on the DJ filter: values at finger speed.
        repeat(20) { i ->
            vm.setLiveBuildUp(0.04f * (i + 1))
            vm.livePerformance(AutoTarget.LIVE_FILTER, 0.3f - 0.01f * i)
            compose.waitForIdle()
        }
        vm.setLiveBuildUp(0.8f)
        compose.waitForIdle()
        assertEquals(pads, livePadCompositions)
        // The slider itself follows.
        compose.onNodeWithTag("live_build_up").assert(
            androidx.compose.ui.test.SemanticsMatcher.expectValue(
                androidx.compose.ui.semantics.SemanticsProperties.ProgressBarRangeInfo,
                androidx.compose.ui.semantics.ProgressBarRangeInfo(0.8f, 0f..1f),
            ),
        )
    }

    @Test
    fun theHoldsRollsAndPadsCanBePlayedWithoutAFinger() {
        val vm = freshVm()
        showVm(vm)
        page("HOLDS")
        // TalkBack's double tap: on and kept, then off.
        compose.onNodeWithTag("live_hold_reverse").performSemanticsAction(SemanticsActions.OnClick)
        assertTrue(vm.liveFx.value.on(AutoTarget.LIVE_REVERSE))
        compose.onNodeWithTag("live_hold_reverse").performSemanticsAction(SemanticsActions.OnClick)
        assertFalse(vm.liveFx.value.on(AutoTarget.LIVE_REVERSE))
        page("ROLLS")
        compose.onNodeWithTag("live_roll_${LiveDivision.EIGHTH.name}").performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(LiveDivision.EIGHTH, vm.liveRollDivision())
        compose.onNodeWithTag("live_roll_${LiveDivision.EIGHTH.name}").performSemanticsAction(SemanticsActions.OnClick)
        assertNull(vm.liveRollDivision())
        page("PAD")
        val actions = compose.onNodeWithTag("xy_pad").fetchSemanticsNode().config[SemanticsActions.CustomActions]
        actions.first { it.label == com.pocketstudio.model.i18n.tr("Passe-haut →") }.action()
        assertTrue(vm.liveFx.value.value(AutoTarget.LIVE_FILTER, 0.5f) > 0.55f)
        actions.first { it.label == com.pocketstudio.model.i18n.tr("Remettre") }.action()
        assertEquals(0.5f, vm.liveFx.value.value(AutoTarget.LIVE_FILTER, 0.5f))
    }

    @Test
    fun theSpectrumIsMeasuredOnlyWhileAPadShowsIt() {
        val users = androidx.compose.runtime.mutableIntStateOf(0)
        compose.setContent {
            CompositionLocalProvider(com.pocketstudio.app.live.LocalSpectrumUsers provides users) {
                PocketStudioTheme { LiveScreen(LiveScreenUi(project, LiveState.on(0), playing = true), LiveActions()) }
            }
        }
        compose.waitForIdle()
        assertEquals(1, users.intValue)  // the Pad page: the DJ filter's pad
        page("HOLDS")
        compose.waitForIdle()
        assertEquals(0, users.intValue)
        page("BUILD")
        compose.waitForIdle()
        assertEquals(0, users.intValue)
        compose.onNodeWithTag("live_fx_drums").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(1, users.intValue)  // a track's pad
    }
}
