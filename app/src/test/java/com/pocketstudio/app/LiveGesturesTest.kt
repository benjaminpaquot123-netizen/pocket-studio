package com.pocketstudio.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import com.pocketstudio.app.live.LiveActions
import com.pocketstudio.app.live.LivePanelTab
import com.pocketstudio.app.live.LiveScreen
import com.pocketstudio.app.live.LiveScreenUi
import com.pocketstudio.app.live.TakeUi
import com.pocketstudio.app.live.isDoubleTouch
import com.pocketstudio.app.live.livePadCompositions
import com.pocketstudio.app.session.Perf
import com.pocketstudio.model.AcidPattern
import com.pocketstudio.model.AcidStep
import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.LiveSlots
import com.pocketstudio.model.LiveState
import com.pocketstudio.model.Project
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongPart
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The Live page's gestures after the phase 27 review: targets, take banner, leaving, redraws, the 303 page. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class LiveGesturesTest {
    @get:Rule
    val compose = createComposeRule()

    private val acid = AcidPattern(steps = List(AcidPattern.MAX_STEPS) { AcidStep(gate = it % 4 == 0) })
    private val project = Project(
        scenes = listOf(Scene("A", DrumPattern().toggle(0, 0), acid = acid, label = "Intro"), Scene("B", DrumPattern().toggle(1, 4))),
        scene = 0, acid = acid,
    ).sanitized()

    private fun show(ui: LiveScreenUi, actions: LiveActions, tab: LivePanelTab = LivePanelTab.Effets) =
        compose.setContent { PocketStudioTheme { LiveScreen(ui, actions, initialTab = tab) } }

    @Test
    fun theTrackChopperFollowsTheTargetChosenAfterIt() {
        val calls = mutableListOf<String>()
        show(LiveScreenUi(project, LiveState.on(0), clock = 5, playing = true), LiveActions(trackChop = { c, on -> calls += "$c $on" }))
        compose.onNodeWithTag("live_fx_acid").performClick()
        compose.onNodeWithTag("live_track_chop_acid").performScrollTo().performTouchInput { down(center); up() }
        // Another track: its own chopper, not the first one's (the button stays in place).
        // (Its chip is scrolled out of sight above the chopper: chosen by its click action.)
        compose.onNodeWithTag("live_fx_drums").performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithTag("live_track_chop_drums").performScrollTo().performTouchInput { down(center); up() }
        assertEquals(listOf("1 true", "1 false", "0 true", "0 false"), calls)
    }

    @Test
    fun aChopHeldElsewhereLightsItsButton() {
        var chops by mutableStateOf(emptySet<Int>())
        compose.setContent {
            PocketStudioTheme { LiveScreen(LiveScreenUi(project, LiveState.on(0), clock = 5, playing = true, chops = chops), LiveActions(), initialTab = com.pocketstudio.app.live.LivePanelTab.Effets) }
        }
        compose.onNodeWithTag("live_fx_acid").performClick()
        compose.onNodeWithTag("live_track_chop_acid").performScrollTo().assertIsNotSelected()
        chops = setOf(1)
        compose.waitForIdle()
        compose.onNodeWithTag("live_track_chop_acid").assertIsSelected()
    }

    @Test
    fun aRollPadRepeatsWhileHeldThenLetsGo() {
        val calls = mutableListOf<com.pocketstudio.model.LiveDivision?>()
        show(LiveScreenUi(project, LiveState.on(0), clock = 5, playing = true), LiveActions(roll = { calls += it }))
        compose.onNodeWithTag("live_page_ROLLS").performClick()
        compose.onNodeWithTag("live_roll_EIGHTH").performTouchInput { down(center); up() }
        assertEquals(listOf(com.pocketstudio.model.LiveDivision.EIGHTH, null), calls)
    }

    @Test
    fun theWrittenTakeLineGoesAwayAndOffersAnnulerOnlyWhileItIsTheLastChange() {
        val calls = mutableListOf<String>()
        var undoable by mutableStateOf(true)
        val withSong = project.copy(song = listOf(SongPart(0))).sanitized()
        compose.setContent {
            PocketStudioTheme {
                LiveScreen(
                    LiveScreenUi(withSong, LiveState.on(0), clock = 5, playing = true, take = TakeUi.Done(1, 0, 0), takeUndoable = undoable),
                    LiveActions(undoTake = { calls += "undo" }, dismissTake = { calls += "dismiss" }),
                )
            }
        }
        compose.onNodeWithTag("live_take_undo").assertIsDisplayed()
        undoable = false
        compose.waitForIdle()
        assertTrue(compose.onAllNodes(androidx.compose.ui.test.hasTestTag("live_take_undo")).fetchSemanticsNodes().isEmpty())
        compose.onNodeWithTag("live_take_dismiss").performClick()
        assertEquals(listOf("dismiss"), calls)
    }

    @Test
    fun leavingDuringATakeAsksFirst() {
        val calls = mutableListOf<String>()
        var take by mutableStateOf<TakeUi>(TakeUi.Recording(16, 10, 2))
        compose.setContent {
            PocketStudioTheme {
                LiveScreen(LiveScreenUi(project, LiveState.on(0), clock = 5, playing = true, take = take), LiveActions(back = { calls += "back" }, record = { calls += "record" }))
            }
        }
        compose.onNodeWithTag("live_back").performClick()
        compose.onNodeWithTag("live_leave_dialog").assertIsDisplayed()
        compose.onNodeWithTag("live_leave_stay").performClick()
        assertTrue(calls.isEmpty())
        // « Quitter »: the take ends (■ Fin), then the page closes.
        compose.onNodeWithTag("live_back").performClick()
        compose.onNodeWithTag("live_leave_confirm").performClick()
        assertEquals(listOf("record", "back"), calls)
        // No take: straight back.
        take = TakeUi.Ready
        calls.clear()
        compose.onNodeWithTag("live_back").performClick()
        assertEquals(listOf("back"), calls)
    }

    @Test
    fun neitherTheClockNorABlinkRecomposesThePads() {
        val clock = mutableIntStateOf(0)
        // Scene B waits for its line on the drums: the grid blinks.
        val grid = LiveState.on(0).armed(LiveSlots.mask(LiveSlots.DRUMS), 1)
        val ui = LiveScreenUi(project, grid, playing = true, clockOf = { clock.intValue })
        compose.mainClock.autoAdvance = false
        show(ui, LiveActions())
        compose.mainClock.advanceTimeBy(100)
        compose.waitForIdle()
        val before = livePadCompositions
        assertTrue(before > 0)
        repeat(20) {
            clock.intValue += 1
            compose.mainClock.advanceTimeBy(50)
        }
        compose.waitForIdle()
        assertEquals(before, livePadCompositions)
        // The counter still follows the clock: step 20 is bar 2, beat 2.
        compose.onNodeWithText("2.2").assertExists()
    }

    @Test
    fun aPadSaysItsTrackItsRowAndWhatItDoes() {
        show(LiveScreenUi(project, LiveState.on(0), clock = 5, playing = true), LiveActions())
        val said = compose.onNodeWithTag("live_pad_drums_0").fetchSemanticsNode().config[SemanticsProperties.ContentDescription].joinToString()
        assertTrue(said, said.startsWith("Batterie · A · Intro · "))
        val other = compose.onNodeWithTag("live_pad_drums_1").fetchSemanticsNode().config[SemanticsProperties.ContentDescription].joinToString()
        assertEquals("Batterie · B · prêt", other)
    }

    @Test
    fun withoutA303LineIts303PageSaysWhereToWriteOne() {
        val none = project.copy(acid = AcidPattern(), scenes = project.scenes.map { it.copy(acid = AcidPattern()) }).sanitized()
        show(LiveScreenUi(none, LiveState.on(0), clock = 5, playing = true), LiveActions(), LivePanelTab.Acide)
        compose.onNodeWithTag("live_acid_none").assertIsDisplayed()
        assertTrue(compose.onAllNodes(androidx.compose.ui.test.hasTestTag("live_acid_xy")).fetchSemanticsNodes().isEmpty())
        assertFalse(LiveScreenUi(none).hasAcid)
        assertTrue(LiveScreenUi(project).hasAcid)
    }

    @Test
    fun onlyTwoTouchesCloseTogetherBringBackTheSavedSound() {
        val slop = 100f
        assertTrue(isDoubleTouch(1200, 1000, Offset(10f, 10f), Offset(40f, 50f), slop))
        // Fast drumming across the pad: far apart, not a double touch.
        assertFalse(isDoubleTouch(1200, 1000, Offset(10f, 10f), Offset(300f, 10f), slop))
        // Too late, or nothing let go before.
        assertFalse(isDoubleTouch(1400, 1000, Offset(10f, 10f), Offset(10f, 10f), slop))
        assertFalse(isDoubleTouch(100, 0, Offset(10f, 10f), Offset(10f, 10f), slop))
    }
}
