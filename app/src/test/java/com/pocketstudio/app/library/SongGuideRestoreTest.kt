package com.pocketstudio.app.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.pocketstudio.app.settings.GuideDialog
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SongGuideRestoreTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theGuideOutlivesTheRebuildThatOpensTheProject() {
        val plan = StarterWizard.scenePlan(setOf(StarterWizard.Role.Kick), setOf(StarterWizard.Part.Pad), acid = false)
        val restore = StateRestorationTester(compose)
        var set = false
        restore.setContent {
            PocketStudioTheme {
                var guide by rememberSongGuide()
                // Set once, as "Créer le morceau" does just before the activity is recreated.
                if (!set) {
                    set = true
                    guide = songGuide("Techno", plan, inKey = true)
                }
                guide?.let { GuideDialog(onClose = { guide = null }, pages = it, tag = "song_guide") }
            }
        }
        compose.onNodeWithText("Ton morceau Techno est prêt").assertExists()
        restore.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Ton morceau Techno est prêt").assertExists()
    }
}
