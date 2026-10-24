package com.pocketstudio.app.onboarding

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.ui.AppTab
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.Project
import com.pocketstudio.model.addTrack
import com.pocketstudio.model.track
import com.pocketstudio.model.withTrack
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The way to a finished song (phase 33): the « Prochaine étape » strip on every tab, « Ton morceau est prêt » once. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp")
class FinishFlowTest {
    @get:Rule
    val compose = createComposeRule()

    private val app = ApplicationProvider.getApplicationContext<Application>()

    @Before
    fun freshPrefs() {
        app.getSharedPreferences(NextStepPrefs.PREFS, 0).edit().clear().commit()
        app.getSharedPreferences(SongReadyPrefs.PREFS, 0).edit().clear().commit()
    }

    /** A beat, nothing else: the next step is the bass (step 2, on Acide). */
    private val beat = Project(drums = DrumPattern().toggle(0, 0))

    @Test
    fun theStripShowsOnEveryCreationTabButNotOnOutils() {
        var here by mutableStateOf(AppTab.Rythme)
        compose.setContent { PocketStudioTheme { ProjectNextStepStrip(beat, "p1", here, share = {}) {} } }
        for (tab in listOf(AppTab.Rythme, AppTab.Acide, AppTab.Synthe, AppTab.Mixeur, AppTab.Audio, AppTab.Morceau)) {
            here = tab
            compose.waitForIdle()
            compose.onNodeWithTag("next_step_strip").assertExists()
            compose.onNodeWithTag("next_step_strip_text", useUnmergedTree = true).assertTextEquals("2/8 · Ajoute une basse")
        }
        here = AppTab.Outils
        compose.waitForIdle()
        compose.onNodeWithTag("next_step_strip").assertDoesNotExist()
    }

    @Test
    fun aTouchGoesToTheStepsTabAndOnThatTabOpensTheCard() {
        val went = mutableListOf<AppTab>()
        var here by mutableStateOf(AppTab.Rythme)
        compose.setContent { PocketStudioTheme { ProjectNextStepStrip(beat, "p1", here, share = {}) { went += it; here = it } } }
        compose.onNodeWithTag("next_step_strip").performClick()
        assertEquals(listOf(AppTab.Acide), went)
        // Now on Acide: the same touch shows the whole card (its hint), « Replier » folds it back to one line.
        compose.onNodeWithTag("next_step_strip").performClick()
        compose.onNodeWithTag("next_steps").assertExists()
        compose.onNodeWithTag("next_step_strip").assertDoesNotExist()
        compose.onNodeWithTag("next_steps_shrink").performClick()
        compose.onNodeWithTag("next_step_strip").assertExists()
        assertEquals(listOf(AppTab.Acide), went)
    }

    @Test
    fun theLastStepOpensTheShareSheetAndAllDoneSaysSo() {
        var shared = 0
        val steps = nextSteps(Project()).map { it.copy(done = it.goal != StepGoal.Share) }
        var shown by mutableStateOf(steps)
        compose.setContent { PocketStudioTheme { NextStepStrip(shown, AppTab.Morceau, share = { shared++ }) } }
        compose.onNodeWithTag("next_step_strip_text", useUnmergedTree = true).assertTextEquals("8/8 · Partage ton morceau")
        compose.onNodeWithTag("next_step_strip").performClick()
        assertEquals(1, shared)
        shown = steps.map { it.copy(done = true) }
        compose.waitForIdle()
        compose.onNodeWithTag("next_step_strip_text", useUnmergedTree = true).assertTextEquals("8/8 · Ton morceau est fini, bravo !")
    }

    @Test
    fun theCrossClosesItAndOutilsAideBringsItBack() {
        compose.setContent { PocketStudioTheme { ProjectNextStepStrip(beat, "p1", AppTab.Rythme, share = {}) {} } }
        compose.onNodeWithTag("next_step_strip_close").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("next_step_strip").assertDoesNotExist()
        assertTrue(NextStepPrefs(app, "p1").closed)
        // Outils › Aide › « Revoir la carte »: the same call the help page makes.
        NextStepPrefs(app, "p1").reopen()
        compose.waitForIdle()
        compose.onNodeWithTag("next_step_strip").assertExists()
    }

    @Test
    fun theSongReadyCardShowsOnceForItsProject() {
        val done = mutableListOf<String>()
        var project by mutableStateOf("p1")
        var pending by mutableStateOf(true)
        compose.setContent {
            PocketStudioTheme {
                SongReady(
                    pending, project, SongReadyUi("Mon tube", seconds = 160, parts = 6),
                    SongReadyActions(listen = { done += "listen" }, close = { done += "close"; pending = false }),
                )
            }
        }
        compose.onNodeWithTag("song_ready").assertExists()
        compose.onNodeWithTag("song_ready_summary").assertTextEquals("Mon tube · 2:40 · 6 parties")
        // ▶ keeps the card: listening first, choosing after.
        compose.onNodeWithTag("song_ready_listen").performClick()
        compose.onNodeWithTag("song_ready").assertExists()
        compose.onNodeWithTag("song_ready_later").performClick()
        compose.onNodeWithTag("song_ready").assertDoesNotExist()
        assertEquals(listOf("listen", "close"), done)
        assertTrue(SongReadyPrefs(app).seen("p1"))
        // The same project asked again (the screen rebuilt with the request still there): not shown.
        pending = true
        compose.waitForIdle()
        compose.onNodeWithTag("song_ready").assertDoesNotExist()
        // Another new song: shown for it.
        project = "p2"
        compose.waitForIdle()
        compose.onNodeWithTag("song_ready").assertExists()
    }

    @Test
    fun eachBigChoiceDoesItsJobAndClosesTheCard() {
        val chosen = mutableListOf<String>()
        var id by mutableStateOf("first")
        compose.setContent {
            PocketStudioTheme {
                SongReady(
                    true, id, SongReadyUi("Mon tube", 160, 6),
                    SongReadyActions(
                        changeSound = { chosen += "sound" }, structure = { chosen += "structure" }, share = { chosen += "share" },
                        guide = { chosen += "guide" },
                    ),
                )
            }
        }
        listOf("song_ready_sound" to "sound", "song_ready_structure" to "structure", "song_ready_share" to "share", "song_ready_guide" to "guide")
            .forEachIndexed { i, (tag, what) ->
                id = "song_$i"
                compose.waitForIdle()
                compose.onNodeWithTag(tag).performClick()
                compose.onNodeWithTag("song_ready").assertDoesNotExist()
                assertEquals(what, chosen.last())
                assertTrue(SongReadyPrefs(app).seen(id))
            }
        assertEquals(4, chosen.size)
    }

    /** The assistant's song: « Changer un son » opens the Lead (named for it), never the chords' track with its many notes. */
    @Test
    fun changerUnSonOpensTheAssistantsLead() {
        val style = com.pocketstudio.model.StarterWizard.style("house")!!
        val parts = com.pocketstudio.model.StarterWizard.Part.entries.associateWith { com.pocketstudio.model.StarterWizard.soundsFor(it, style).first() }
        val song = com.pocketstudio.model.StarterWizard.build(style, style.lines.associate { it.role to null as com.pocketstudio.model.PadSample? }, com.pocketstudio.model.StarterWizard.Mood.Happy, parts)
        val lead = song.tracks.first { it.name == "Lead" }
        assertEquals(lead.id, mainTrackOf(song))
    }

    @Test
    fun changerUnSonPicksTheTrackThatCarriesTheTune() {
        assertNull(mainTrackOf(Project()))
        val bass = Project(synthNotes = NoteClip(notes = listOf(Note(0, 4, 36), Note(4, 4, 38), Note(8, 4, 41))))
        assertEquals(0, mainTrackOf(bass))
        val withLead = bass.addTrack("Mélodie")!!.let { p ->
            p.withTrack(p.track(1).copy(notes = NoteClip(notes = listOf(Note(0, 2, 72), Note(2, 2, 74)))))
        }
        assertEquals(1, mainTrackOf(withLead))
        assertFalse(mainTrackOf(withLead) == 0)
    }
}
