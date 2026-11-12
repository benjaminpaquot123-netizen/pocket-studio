package com.pocketstudio.app.remix

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Audit ultra round 2, n° 31 (seat A b, Remix in Spanish): « Elegir una canción » with no model
 * downloaded its 174 MB at once, unasked. The model is asked first (its size, Wi-Fi or not); the
 * phone's picker opens once the person said yes, never before.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class RemixModelAskTest {
    @get:Rule
    val compose = createComposeRule()

    private var picks = 0
    private var downloads = 0

    private fun show(installed: Boolean) = compose.setContent {
        PocketStudioTheme {
            RemixScreen(RemixUi(installed, SeparationState.Idle, emptyList(), home = true), RemixActions(pickSong = { picks++ }, downloadModel = { downloads++ }))
        }
    }

    @Test
    fun theSongsChoiceAsksForTheModelFirst() {
        show(installed = false)
        compose.onNodeWithTag("remix_pick").performClick()
        assertEquals("the picker (and the download) waited for an answer", 0, picks)
        compose.onNodeWithTag("remix_model_ask").assertIsDisplayed()
        // Its size, and the phone's network (Robolectric's is mobile data: the forfait is said).
        val inAsk = androidx.compose.ui.test.hasAnyAncestor(androidx.compose.ui.test.hasTestTag("remix_model_ask"))
        compose.onNode(inAsk and androidx.compose.ui.test.hasText("modèle : " + SeparationModel.SPEC.sizeLabel, substring = true)).assertIsDisplayed()
        compose.onNode(inAsk and androidx.compose.ui.test.hasText("forfait mobile", substring = true)).assertIsDisplayed()
        compose.onNodeWithTag("remix_model_ask_later").performClick()
        compose.onNodeWithTag("remix_model_ask").assertDoesNotExist()
        assertEquals(0, picks)
        compose.onNodeWithTag("remix_pick").performClick()
        compose.onNodeWithTag("remix_model_ask_ok").performClick()
        assertEquals("yes: the picker, then the download with the song", 1, picks)
        assertEquals(0, downloads)
    }

    @Test
    fun onMobileDataTheDownloadButtonAsksToo() {
        show(installed = false)
        compose.onNodeWithTag("remix_download").performClick()
        assertEquals(0, downloads)
        compose.onNodeWithTag("remix_model_ask_ok").performClick()
        assertEquals(1, downloads)
        assertEquals(0, picks)
    }

    @Test
    fun withTheModelTheSongIsChosenAtOnce() {
        show(installed = true)
        compose.onNodeWithTag("remix_pick").performClick()
        compose.onNodeWithTag("remix_model_ask").assertDoesNotExist()
        assertEquals(1, picks)
    }
}
