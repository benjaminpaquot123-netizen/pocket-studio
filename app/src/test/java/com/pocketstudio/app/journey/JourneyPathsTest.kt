package com.pocketstudio.app.journey

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import com.pocketstudio.app.nav.AppHarness
import com.pocketstudio.app.session.partSheetUi
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.sceneLabel
import com.pocketstudio.model.viewOfTrack
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The users' paths of the emulator's JourneyTest and EnduranceTest (docs/TESTS-REELS.md), touch
 * for touch ([Journey], the same code), on the app without its engine: each screen and button
 * they go through is there, where they look for it. What needs the engine (a song made, heard,
 * recorded in time, exported) is checked on the emulator only.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class JourneyPathsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val h = AppHarness(compose)
    private val j = Journey(compose)

    @After
    fun back() = h.finish()

    /** First launch, every tool, « Plus tard »: the starter beat, the mode kept. */
    @Test
    fun firstLaunchInFullModeThenLater() {
        h.start(firstLaunch = true)
        j.firstLaunch(simple = false, guided = false)
        assertTrue(SettingsStore.state.value.guideSeen)
        assertFalse(SettingsStore.state.value.simpleMode)
        j.await("tab_Structure")
    }

    /** First launch in the Mode simple, the guided song: the assistant opens on the calm styles, lo-fi chosen. */
    @Test
    fun firstLaunchInSimpleModeGuidedToTheAssistant() {
        h.start(firstLaunch = true)
        j.firstLaunch(simple = true, guided = true)
        assertTrue(SettingsStore.state.value.simpleMode)
        j.chooseStyle("lofi", "calm")
    }

    /** A later start: « Que veux-tu faire ? » › Continuer opens the last project. */
    @Test
    fun aLaterStartContinues() {
        h.start(launchChoice = true)
        j.continueLastProject()
        j.await("tab_Structure")
    }

    /** A later start: « Que veux-tu faire ? » › Nouveau morceau › the assistant › jump up. */
    @Test
    fun aLaterStartMakesANewSongInTheAssistant() {
        h.start(launchChoice = true)
        j.newSongInAssistant()
        j.chooseStyle("jump_up", "Bass music")
    }

    /** Structure › Parties › ⋮ › « Fill à la fin », then « Montée avant · 4 mes. » on another part. */
    @Test
    fun aFillAndABuildUpFromThePartSheet() {
        h.start()
        j.tap("tab_Structure")
        val p0 = h.vm.project.value
        val fillPart = (1 until p0.song.size).first { partSheetUi(p0, it)?.endsOnFill == false }
        j.partIntent(fillPart, "part_fill")
        val p1 = h.vm.project.value
        assertEquals("Fill", p1.sceneLabel(p1.song[fillPart].play.getValue(SongPart.DRUMS)))
        val buildUpPart = (1 until p1.song.size).first { it != fillPart }
        j.partIntent(buildUpPart, "part_buildup_4")
        assertTrue(h.vm.project.value.automation.any { it.target == AutoTarget.live(AutoTarget.LIVE_BUILD_UP) })
    }

    /** Sons › a melody track › ● Enregistrer ma mélodie, its keys, ■, « Remettre en rythme »: the notes on the sixteenths. */
    @Test
    fun aMelodyTakeFromTheKeyboardPutInTime() {
        h.start()
        j.tap("tab_Sons")
        val lead = h.trackId("Lead")
        j.tap("track_$lead")
        j.await("record_start")
        assertTrue("the keyboard's keys are on screen", j.tagsMatching(Journey.KEY).isNotEmpty())
        h.vm.playingForTest = true
        j.tap("record_start")
        val key = j.tagsMatching(Journey.KEY).first()
        // Played off the grid (no engine: the position is given), as a hand plays.
        h.vm.recordPositionForTest = 1.3
        h.vm.playedClockForTest = 1.3
        compose.onNodeWithTag(key).performTouchInput { down(center) }
        h.vm.recordPositionForTest = 2.6
        h.vm.playedClockForTest = 2.6
        compose.onNodeWithTag(key).performTouchInput { up() }
        compose.waitForIdle()
        j.tap("record_stop")
        j.await("take_summary")
        j.tap("take_in_time")
        val notes = h.vm.project.value.viewOfTrack(lead).synthNotes.notes
        assertEquals(1, notes.size)
        assertEquals(0, notes[0].start % NoteClip.TICKS_PER_STEP)
        j.tap("take_keep")
    }

    /** Mix › Exporter: the whole song, M4A then WAV, then « Partager » from the top bar's ⤴. */
    @Test
    fun exportingTheSongAndSharingIt() {
        h.start()
        j.tap("tab_Mix")
        j.tap("export_what_Song")
        j.tap("export_format_M4a")
        j.await("export")
        j.tap("export_format_Wav")
        j.tap("top_share")
        j.await("share_sheet")
        j.await("share_go")
        j.await("share_format_M4a")
    }

    /** The Mode simple's tabs: Clavier records a melody, Envoyer shares (WAV or M4A). */
    @Test
    fun theSimpleModesKeyboardAndSendTabs() {
        h.start(simple = true)
        j.tap("tab_simple_Clavier")
        j.await("record_start")
        j.tap("tab_simple_Envoyer")
        j.await("share_go")
        j.await("share_format_Wav")
    }

    /** The endurance's moves: Mes projets › a project; the Live page and back; loop the part. */
    @Test
    fun theEndurancesMoves() {
        h.start()
        j.tap("top_projects")
        j.await("projects")
        // « ← » of « Mes projets »: the home screen (owner, 2026-10-29), « Continuer » back on the project.
        j.tap("projects_back")
        j.await("launch_continue")
        j.tap("launch_continue")
        j.tap("tab_Live")
        j.await("live_screen")
        j.await("live_play")
        assertTrue("the Live page has its pads", j.tagsMatching(Regex("live_pad_.+")).isNotEmpty())
        j.tap("tab_Structure")
        j.await("top_loop_part")
    }
}
