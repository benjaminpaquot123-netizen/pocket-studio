package com.pocketstudio.app.library

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.pocketstudio.app.settings.GuideDialog
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectJson
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.guide.GuideStart
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** G5: the song's guide is kept with the song (not only through the rebuild that opens it): read from the Guide's first card, even days later. */
@RunWith(RobolectricTestRunner::class)
class SongGuideRestoreTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theGuideIsKeptWithTheSong() {
        val plan = StarterWizard.scenePlan(setOf(StarterWizard.Role.Kick), setOf(StarterWizard.Part.Pad), acid = false)
        val made = GuideStart.begin(Project(), GuideStart.SONG, songGuide("Techno", plan, inKey = true).toSongGuide())
        // Saved and read again, as when the project is opened another day.
        val saved = ProjectJson.decode(ProjectJson.encode(made))
        compose.setContent {
            PocketStudioTheme { GuideDialog(onClose = {}, pages = saved.guide.songGuide.toGuidePages(), tag = "song_guide") }
        }
        compose.onNodeWithText("Ton morceau Techno est prêt").assertExists()
    }
}
