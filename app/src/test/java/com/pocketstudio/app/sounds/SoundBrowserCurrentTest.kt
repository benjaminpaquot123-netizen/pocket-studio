package com.pocketstudio.app.sounds

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pocketstudio.model.SoundCategories
import com.pocketstudio.model.SoundLibrary
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** « La nappe choisie n'est pas sélectionnée » (2026-10-22): the browser opens on the sound playing now. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class SoundBrowserCurrentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun anotherTracksSoundOpensItsFamilyAndIsSelected() {
        val nappe = SoundLibrary.SYNTH.indexOfFirst { it.name == "Nappe" }
        val bass = SoundLibrary.SYNTH.indexOfFirst { it.category == "Basses" }
        var current by mutableStateOf<String?>(SoundLibrary.SYNTH[bass].name)
        compose.setContent {
            PocketStudioTheme { SoundBrowser(SoundBrowserUi(SoundCategories.SYNTH, SoundLibrary.SYNTH, emptyList(), current, tag = "synth_sounds")) }
        }
        // One browser everywhere (7d): opened in the folder of the sound playing now, that sound selected.
        compose.onNodeWithTag("synth_sounds_$bass").assertIsSelected()
        // The pad track shown next (the browser stays on screen): its folder, its sound selected.
        current = "Nappe"
        compose.waitForIdle()
        compose.onNodeWithTag("synth_sounds_$nappe").assertIsSelected()
        // Browsing another family stays there while the sound does not change.
        compose.onNodeWithTag("synth_sounds_back").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("synth_sounds_folder_Leads").performClick()
        compose.waitForIdle()
        val lead = SoundLibrary.SYNTH.indexOfFirst { it.category == "Leads" }
        compose.onNodeWithTag("synth_sounds_$lead").assertExists()
        compose.onNodeWithTag("synth_sounds_$nappe").assertDoesNotExist()
    }

    @Test
    fun keptSoundsAreTheFavouritesFolderAndCanBeRemoved() {
        val kept = listOf(SoundLibrary.SYNTH[0].copy(name = "Ma basse"))
        var removed = -1
        var picked = -1
        compose.setContent {
            PocketStudioTheme {
                SoundBrowser(SoundBrowserUi(SoundCategories.SYNTH, SoundLibrary.SYNTH, kept, current = null, tag = "synth_sounds",
                    pickFavorite = { picked = it }, deleteFavorite = { removed = it }))
            }
        }
        // Opened on the first family (no sound of the list plays): one step up, the kept ones.
        compose.onNodeWithTag("synth_sounds_back").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("synth_sounds_shelf_mine").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("synth_sounds_fav_0").performClick()
        org.junit.Assert.assertEquals(0, picked)
        compose.onNodeWithTag("synth_sounds_sound_remove_0").performClick()
        org.junit.Assert.assertEquals(0, removed)
    }

    @Test
    fun theAssistantsCalibratedPadIsFound() {
        // The assistant lowers its pad (GAIN): still « Nappe ».
        val pad = SoundLibrary.SYNTH.first { it.name == "Nappe" }.sound.let { it.with(com.pocketstudio.model.SynthPatch.GAIN, it.get(com.pocketstudio.model.SynthPatch.GAIN) - 0.07f) }
        org.junit.Assert.assertEquals("Nappe", SoundLibrary.synthNameOf(pad))
    }
}
