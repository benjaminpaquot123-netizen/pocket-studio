package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pocketstudio.app.record.TakeSource
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
 * Audit ultra round 2, n° 21 (seat B b, a guitarist: « rien sur l'accueil pour enregistrer ma voix ou
 * ma guitare » ; the Rythme tab's red ● recorded the pads only, round 1's n° 30 back): the microphone
 * is one touch away from the first launch, from « Que veux-tu faire ? », and from the ● of every tab.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class RecordVoiceWaysTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)

    @After
    fun end() = h.finish()

    private fun tap(tag: String) {
        // Scrolled to on a screen that scrolls (the welcome), as it is in a window that does not.
        runCatching { compose.onNodeWithTag(tag).performScrollTo() }
        compose.onNodeWithTag(tag).performClick()
        compose.waitForIdle()
    }

    private fun exists(tag: String) = compose.onAllNodes(androidx.compose.ui.test.hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun theRythmeTabsRecordAsksAndOffersTheMicrophone() {
        h.start(simple = true)
        compose.onNodeWithTag("tab_simple_Rythme").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("top_record").performClick()
        compose.waitForIdle()
        assertTrue("the ● asks what to record on Rythme too", exists("record_choice"))
        // The pads are the first choice here, one touch more than before.
        tap("record_choice_mic")
        assertTrue(exists("audio_tracks"))
        assertEquals(1, h.vm.project.value.audioTracks.size)
    }

    @Test
    fun thePadsStillRecordFromRythmeThroughTheChoice() {
        h.start(simple = true)
        compose.onNodeWithTag("tab_simple_Rythme").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("top_record").performClick()
        compose.waitForIdle()
        tap("record_choice_pads")
        assertEquals(TakeSource.PADS, h.vm.noteTake.value?.source)
    }

    @Test
    fun theFirstLaunchHasTheMicrophone() {
        h.start(simple = true, firstLaunch = true)
        tap("welcome_voice")
        assertFalse("the welcome is gone", exists("welcome"))
        assertTrue(exists("audio_tracks"))
        assertEquals(1, h.vm.project.value.audioTracks.size)
    }

    @Test
    fun theHomeScreenHasTheMicrophone() {
        h.start(simple = true, launchChoice = true)
        h.awaitTag("launch_voice")
        tap("launch_voice")
        assertFalse(exists("launch"))
        assertTrue(exists("audio_tracks"))
    }
}
