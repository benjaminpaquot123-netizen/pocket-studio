package com.pocketstudio.app.sounds

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Audit ultra round 2, n° 26 (seat B c, screen 34, back from round 1): « Garder ce son » proposed « Mon son 1 »
 * and what was typed went after it (« Mon son 1Pluie »). The name is selected at the touch: typed over.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w392dp-h852dp")
class KeptNameFieldTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun aKeptSoundsNameIsReplacedByWhatIsTyped() {
        // The dialog's field (the dialog itself never gets idle under Robolectric).
        var kept by androidx.compose.runtime.mutableStateOf("Mon son 1")
        compose.setContent { PocketStudioTheme { KeptNameField(kept, { kept = it }, "synth") } }
        compose.onNodeWithTag("synth_name_field").performClick()
        compose.onNodeWithTag("synth_name_field").performTextInput("Pluie")
        compose.waitForIdle()
        assertEquals("Pluie", kept)
    }
}
