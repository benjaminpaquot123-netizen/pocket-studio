package com.pocketstudio.app.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Audit ultra round 1, n° 26: a name is typed over, its limit said before it cuts.
 * - Seat E b renamed « Audio 1 » and got « Audio 1Chitarra »: the field is touched, then typed in.
 * - Seat F d typed past 40 letters and « rythme » became « rythm » without a word: « 40 caractères au plus » shows.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w392dp-h852dp")
class NameFieldTest {
    @get:Rule
    val compose = createComposeRule()

    /** Every rename field of the app is this one (a project, a track, an audio track, a block). */
    @Test
    fun aNameIsReplacedByWhatIsTyped() {
        compose.setContent {
            PocketStudioTheme {
                var name by remember { mutableStateOf("Audio 1") }
                NameField(name, { name = it }, max = 24, label = "Nom", modifier = Modifier.testTag("field"))
                Text("[$name]", modifier = Modifier.testTag("shown"))
            }
        }
        compose.onNodeWithTag("field").performClick()
        compose.onNodeWithTag("field").performTextInput("Chitarra")
        compose.onNodeWithText("[Chitarra]").assertExists()
    }

    @Test
    fun theLimitIsSaidBeforeTheNameIsCut() {
        compose.setContent {
            PocketStudioTheme {
                var name by remember { mutableStateOf("Mon premier rythme") }
                NameField(name, { name = it }, max = 20, label = "Nom", modifier = Modifier.testTag("field"))
                Text(name, modifier = Modifier.testTag("shown"))
            }
        }
        compose.onNodeWithText("18/20").assertExists()
        compose.onNodeWithTag("field").performClick()
        compose.onNodeWithTag("field").performTextInput("Un nom bien trop long pour la limite")
        compose.onNodeWithText("20 caractères au plus").assertExists()
    }

    /** Relecture tour 1 (mineur): the limit never cuts an emoji in half. */
    @Test
    fun theLimitKeepsLettersWhole() {
        compose.setContent {
            PocketStudioTheme {
                var name by remember { mutableStateOf("") }
                NameField(name, { name = it }, max = 3, label = "Nom", modifier = Modifier.testTag("field"))
                Text("[$name]", modifier = Modifier.testTag("shown"))
            }
        }
        compose.onNodeWithTag("field").performClick()
        compose.onNodeWithTag("field").performTextInput("ab\uD83D\uDE00")
        compose.onNodeWithText("[ab]").assertExists()
    }
}
