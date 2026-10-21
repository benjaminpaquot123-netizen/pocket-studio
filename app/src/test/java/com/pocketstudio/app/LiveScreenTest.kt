package com.pocketstudio.app

import androidx.compose.ui.test.assertIsDisplayed
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
}
