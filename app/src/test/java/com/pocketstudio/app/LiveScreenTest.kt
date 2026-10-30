package com.pocketstudio.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.addTrack
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.pocketstudio.app.live.TakeUi
import com.pocketstudio.app.live.LiveActions
import com.pocketstudio.app.live.LivePanelTab
import com.pocketstudio.app.live.LiveScreen
import com.pocketstudio.app.live.LiveScreenUi
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.LiveSlots
import com.pocketstudio.model.LiveState
import com.pocketstudio.model.Project
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongPart
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The Live page's buttons reach the right ViewModel calls. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class LiveScreenTest {
    @get:Rule
    val compose = createComposeRule()

    /** A 303 line of one note (the 303 page has something to play). */
    private val oneNote = com.pocketstudio.model.AcidPattern(steps = List(com.pocketstudio.model.AcidPattern.MAX_STEPS) { com.pocketstudio.model.AcidStep(gate = it == 0) })

    private val project = Project(
        scenes = listOf(
            Scene("A", DrumPattern().toggle(0, 0), label = "Intro", acid = oneNote),
            Scene("B", DrumPattern().toggle(1, 4)),
        ),
        scene = 0, acid = oneNote,
    ).sanitized()

    @Test
    fun aPadARowAStopAMuteAndTheQuantumGoToTheViewModel() {
        val calls = mutableListOf<String>()
        val actions = LiveActions(
            launchPad = { key, scene -> calls += "pad $key $scene" },
            launchRow = { calls += "row $it" },
            stopTrack = { calls += "stop $it" },
            stopAll = { calls += "stop all" },
            toggleMute = { calls += "mute $it" },
            setQuantum = { calls += "quantum $it" },
        )
        compose.setContent { PocketStudioTheme { LiveScreen(LiveScreenUi(project, LiveState.on(0), clock = 5, playing = true), actions) } }
        compose.onNodeWithTag("live_pad_drums_1").performClick()
        compose.onNodeWithTag("live_row_1").performClick()
        compose.onNodeWithTag("live_stop_acid").performClick()
        compose.onNodeWithTag("live_stop_all").performClick()
        compose.onNodeWithTag("live_mute_drums").performClick()
        compose.onNodeWithTag("live_quantum").performClick()
        assertEquals(listOf("pad drums 1", "row 1", "stop acid", "stop all", "mute drums", "quantum 2"), calls)
    }

    /** Audit 28 live M5: a pad launches on the finger's down; a finger sliding on (the grid scrolling) takes it back. */
    @Test
    fun aPadLaunchesOnTheFingersDownAndASlideTakesItBack() {
        val calls = mutableListOf<String>()
        val actions = LiveActions(
            launchPad = { key, scene -> calls += "tap $key $scene" },
            stopTrack = { calls += "tap stop $it" },
            press = { key, scene -> calls += "press $key $scene"; true },
            retract = { calls += "retract $it" },
        )
        compose.setContent { PocketStudioTheme { LiveScreen(LiveScreenUi(project, LiveState.on(0), clock = 5, playing = true), actions) } }
        val pad = compose.onNodeWithTag("live_pad_drums_1")
        pad.performTouchInput { down(center) }
        compose.waitForIdle()
        assertEquals(listOf("press drums 1"), calls)  // before the let-go
        pad.performTouchInput { up() }
        compose.waitForIdle()
        assertEquals(listOf("press drums 1"), calls)  // the let-go adds nothing
        calls.clear()
        pad.performTouchInput {
            down(center)
            moveBy(androidx.compose.ui.geometry.Offset(0f, 40f))
            moveBy(androidx.compose.ui.geometry.Offset(0f, 120f))
            up()
        }
        compose.waitForIdle()
        assertEquals(listOf("press drums 1", "retract drums"), calls)
        calls.clear()
        compose.onNodeWithTag("live_stop_acid").performClick()
        assertEquals(listOf("press acid -1"), calls)
    }

    @Test
    fun aPadXyKeepsItsValuesClearOfItsTopAndBottomLabels() {
        val h = 300f
        val inset = 60f
        assertEquals(1f, com.pocketstudio.app.mixer.xyValue(10f, h, inset))  // on the top label: the top value
        assertEquals(0f, com.pocketstudio.app.mixer.xyValue(290f, h, inset))
        assertEquals(inset, com.pocketstudio.app.mixer.xyPosition(1f, h, inset))
        assertEquals(h - inset, com.pocketstudio.app.mixer.xyPosition(0f, h, inset))
        for (v in listOf(0f, 0.25f, 0.5f, 0.9f)) assertEquals(v, com.pocketstudio.app.mixer.xyValue(com.pocketstudio.app.mixer.xyPosition(v, h, inset), h, inset), 1e-5f)
    }

    @Test
    fun theStoppedBassIsSaidOnThe303Page() {
        val grid = LiveState.on(0).launched(LiveSlots.ACID, -1, 16)
        compose.setContent {
            PocketStudioTheme { LiveScreen(LiveScreenUi(project, grid, clock = 20, playing = true), LiveActions(), initialTab = LivePanelTab.Acide) }
        }
        compose.onNodeWithTag("live_acid_stopped").assertIsDisplayed()
        compose.onNodeWithTag("live_acid_xy").assertIsDisplayed()
        assertEquals(SongPart.ACID, LiveSlots.key(LiveSlots.ACID))
    }

    @Test
    fun theRecordButtonGoesFromReadyToArmedToRecordingToDone() {
        val calls = mutableListOf<String>()
        val actions = LiveActions(
            record = { calls += "record" },
            setTakeAppend = { calls += "append $it" },
            undoTake = { calls += "undo" },
            listenTake = { calls += "listen" },
        )
        val withSong = project.copy(song = listOf(SongPart(0), SongPart(1))).sanitized()
        var take by androidx.compose.runtime.mutableStateOf<TakeUi>(TakeUi.Ready)
        compose.setContent {
            PocketStudioTheme { LiveScreen(LiveScreenUi(withSong, LiveState.on(0), clock = 5, playing = true, take = take), actions) }
        }
        // Ready, the song has parts: where the take goes, « Remplacer » chosen.
        compose.onNodeWithTag("live_take_replace").assertIsDisplayed()
        compose.onNodeWithTag("live_take_append").performClick()
        compose.onNodeWithTag("live_record").assertTextContains("REC").performClick()
        // Armed: the button says so; the choice can still change.
        take = TakeUi.Armed(16, 11)
        compose.onNodeWithTag("live_record").assertTextContains("Armé")
        compose.onNodeWithTag("live_take_replace").performClick()
        // Recording: the clock and the song's bar, ■ Fin in the line and on the button.
        take = TakeUi.Recording(16, 42, 21)
        compose.onNodeWithTag("live_take_clock").assertTextContains("REC 0:42 · mesure 21")
        compose.onNodeWithTag("live_record").assertTextContains("Fin")
        compose.onNodeWithTag("live_take_end").performClick()
        // Done: what was written, Annuler and Écouter.
        take = TakeUi.Done(parts = 3, settings = 2, firstPart = 0)
        compose.onNodeWithTag("live_take_done").assertTextContains("Morceau écrit : 3 parties, 2 réglages")
        compose.onNodeWithTag("live_take_undo").performClick()
        compose.onNodeWithTag("live_take_listen").performClick()
        assertEquals(listOf("append true", "record", "append false", "record", "undo", "listen"), calls)
    }

    @Test
    fun aTrackHasItsChopperOnThePanel() {
        val ch = mutableListOf<String>()
        compose.setContent {
            PocketStudioTheme {
                LiveScreen(LiveScreenUi(project, LiveState.on(0), clock = 5, playing = true), LiveActions(trackChop = { c, on -> ch += "$c $on" }))
            }
        }
        compose.onNodeWithTag("live_fx_acid").performClick()
        compose.onNodeWithTag("live_track_chop_acid").performScrollTo().performTouchInput { down(center); up() }
        assertEquals(listOf("1 true", "1 false"), ch)
    }

    @Test
    fun manyTracksUprightKeepTheGridAsItIsAndItsColumnsScroll() {
        // Seven columns (drums, 303, five instrument tracks) do not fit across 400 dp: the grid keeps
        // its scenes in rows (it never turns as tracks are added) and its columns scroll.
        var many: Project = project
        repeat(4) { many = many.addTrack("Piste ${it + 2}")!! }
        val columns = com.pocketstudio.app.live.liveColumnsOf(many)
        assertEquals(7, columns.size)
        val calls = mutableListOf<String>()
        compose.setContent {
            PocketStudioTheme {
                LiveScreen(
                    LiveScreenUi(many, LiveState.on(0), clock = 5, playing = true),
                    LiveActions(toggleMute = { calls += "mute $it" }, stopTrack = { calls += "stop $it" }, launchPad = { k, s -> calls += "pad $k $s" }),
                )
            }
        }
        val screen = compose.onNodeWithTag("live_screen").getBoundsInRoot()
        // Scenes down the left, « ■ Tout » under them (turned, it was at the top).
        val row = compose.onNodeWithTag("live_row_0").getBoundsInRoot()
        val stopAll = compose.onNodeWithTag("live_stop_all").getBoundsInRoot()
        org.junit.Assert.assertTrue("■ Tout under the scenes", stopAll.top > row.bottom)
        org.junit.Assert.assertEquals(row.left.value, stopAll.left.value, 0.5f)
        columns.forEach { c ->
            val m = compose.onNodeWithTag("live_mute_${c.key}").performScrollTo().getBoundsInRoot()
            org.junit.Assert.assertTrue("${c.key} M within the width", m.right <= screen.right + 0.5.dp)
        }
        val last = columns.last().key
        compose.onNodeWithTag("live_mute_$last").performScrollTo().performClick()
        compose.onNodeWithTag("live_stop_$last").performScrollTo().performClick()
        // Its cell in B is empty (lot 4b): a touch asks what goes in it, it launches nothing.
        compose.onNodeWithTag("live_pad_${last}_1").performScrollTo().performClick()
        assertEquals(listOf("mute $last", "stop $last"), calls)
        compose.onNodeWithTag("live_cell_menu").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w600dp-h340dp-land-xxhdpi")
    fun lyingDownWithABigTextEveryTabStaysClearOfTheFullScreenButton() {
        // 09/20: at 130 % the side panel's « Notes » went under ⛶; the tabs now share the row.
        compose.setContent {
            val d = androidx.compose.ui.platform.LocalDensity.current
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(d.density, 1.3f)) {
                PocketStudioTheme { LiveScreen(LiveScreenUi(project, LiveState.on(0), clock = 5, playing = true), LiveActions()) }
            }
        }
        val full = compose.onNodeWithTag("live_fullscreen").getBoundsInRoot()
        listOf("Effets", "Acide", "Pads", "Notes").forEach { t ->
            val tab = compose.onNodeWithTag("live_tab_$t").assertIsDisplayed().getBoundsInRoot()
            org.junit.Assert.assertTrue("$t clear of ⛶", tab.right <= full.left + 0.5.dp)
        }
    }

    @Test
    @Config(qualifiers = "w316dp-h640dp-xxhdpi")
    fun uprightOnAShortScreenThePadOfTheEffectsIsInView() {
        // 09/20: at 130 % upright the pad XY was all under the fold; the panel keeps room for it.
        compose.setContent { PocketStudioTheme { LiveScreen(LiveScreenUi(project, LiveState.on(0), clock = 5, playing = true), LiveActions()) } }
        val screen = compose.onNodeWithTag("live_screen").getBoundsInRoot()
        val xy = compose.onNodeWithTag("xy_pad").getBoundsInRoot()
        org.junit.Assert.assertTrue("at least 90 dp of the pad in view (${screen.bottom - xy.top})", screen.bottom - xy.top >= 90.dp)
    }

    @Test
    fun fullScreenFoldsThePanelAndATabOpensItAgain() {
        compose.setContent { PocketStudioTheme { LiveScreen(LiveScreenUi(project, LiveState.on(0), clock = 5, playing = true), LiveActions()) } }
        compose.onNodeWithTag("live_fx_acid").assertIsDisplayed()
        compose.onNodeWithTag("live_fullscreen").performClick()
        compose.onNodeWithTag("live_fx_acid").assertDoesNotExist()
        compose.onNodeWithTag("live_tab_Effets").performClick()
        compose.onNodeWithTag("live_fx_acid").assertIsDisplayed()
        // Back out of full screen: nothing else changes.
        compose.onNodeWithTag("live_fullscreen").performClick()
        compose.onNodeWithTag("live_fx_acid").assertIsDisplayed()
    }

    /** Lot 4a: « Pads » in the Live is the pads view on the whole screen; while a pad is being set up, its card. */
    @Test
    fun thePadsTabOpensThePadsOnTheWholeScreen() {
        var full = 0
        val pads = com.pocketstudio.app.session.LivePadsUi(com.pocketstudio.model.LivePads(8), List(8) { 0 }, editing = null)
        val actions = com.pocketstudio.app.session.LivePadsActions(fullScreen = { full++ })
        var editing by androidx.compose.runtime.mutableStateOf<Int?>(null)
        compose.setContent {
            PocketStudioTheme {
                LiveScreen(
                    LiveScreenUi(project, LiveState.on(0), clock = 5, playing = true), LiveActions(),
                    pads = com.pocketstudio.app.session.LivePadsUi(pads.pads, pads.states, editing = editing), padsActions = actions,
                )
            }
        }
        compose.onNodeWithTag("live_tab_Pads").performClick()
        assertEquals(1, full)
        // « Régler » from the pads view: back here, the pad's settings in the card, no second jump.
        editing = 3
        compose.onNodeWithTag("live_tab_Pads").performClick()
        assertEquals(1, full)
        compose.onNodeWithTag("live_editor").assertIsDisplayed()
    }
}
