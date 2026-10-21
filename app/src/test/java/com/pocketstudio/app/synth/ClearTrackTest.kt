package com.pocketstudio.app.synth

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.Project
import com.pocketstudio.model.instrumentTracks
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ClearTrackTest {
    @get:Rule val compose = createComposeRule()

    private val clip = NoteClip(16, listOf(Note(start = 0, length = 48, pitch = 60, velocity = 1f)))

    private fun show(tracks: TracksUi) = compose.setContent {
        PocketStudioTheme { SynthScreen(Project().synth, 4, SynthActions(), notes = NotesUi(clip, emptyList(), -1, PianoRollActions()), tracks = tracks) }
    }

    @Test
    fun clearEmptiesTheSelectedTrackInTheSceneShown() {
        val cleared = mutableListOf<Int>()
        val tracks = Project().instrumentTracks.map { it.copy(notes = clip) }
        show(TracksUi(tracks, selected = tracks.first().id, clear = { cleared += it }, scene = "B"))
        compose.onNodeWithTag("track_clear_button").performClick()
        assertEquals(listOf(tracks.first().id), cleared)
    }

    @Test
    fun nothingToClearMeansNoClick() {
        val tracks = Project().instrumentTracks
        show(TracksUi(tracks, selected = tracks.first().id, clear = {}, scene = "B"))
        compose.onNodeWithTag("track_clear_button").assertIsNotEnabled()
    }
}
