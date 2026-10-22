package com.pocketstudio.app.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Taille de l'interface 130 % on a 411 dp phone: the screens see (and get) 316 dp, not 411. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h914dp")
class UiScaleTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theWidthTheScreensSeeIsTheRoomTheyReallyHave() {
        var configured = 0
        var smallest = 0
        var measured = 0f
        compose.setContent {
            UiScaled(1.3f) {
                configured = LocalConfiguration.current.screenWidthDp
                smallest = LocalConfiguration.current.smallestScreenWidthDp
                BoxWithConstraints(Modifier.fillMaxSize()) { measured = maxWidth.value }
            }
        }
        compose.waitForIdle()
        assertEquals(316, configured)
        assertEquals(316, smallest)
        assertEquals(316f, measured, 1.5f)
    }

    @Test
    fun atHundredPercentNothingChanges() {
        var configured = 0
        compose.setContent { UiScaled(1f) { configured = LocalConfiguration.current.screenWidthDp } }
        compose.waitForIdle()
        assertEquals(411, configured)
    }

    /**
     * Audit 28 M3: Réglages › Taille de l'interface from 100 % to 115 % and back: the tab chosen
     * (saveable) and a dialog open (remembered) stay; the app is not composed again from scratch.
     */
    @Test
    fun changingTheSizeKeepsTheTabAndWhatIsOpen() {
        var scale by mutableStateOf(1f)
        compose.setContent {
            UiScaled(scale) {
                var tab by rememberSaveable { mutableStateOf("Rythme") }
                var dialog by remember { mutableStateOf(false) }
                Column {
                    Text(tab, Modifier.testTag("tab"))
                    Button(onClick = { tab = "Outils › Réglages"; dialog = true }, modifier = Modifier.testTag("open")) { Text("Réglages") }
                    Text(if (dialog) "ouvert" else "fermé", Modifier.testTag("dialog"))
                }
            }
        }
        compose.onNodeWithTag("open").performClick()
        compose.onNodeWithTag("tab").assertTextEquals("Outils › Réglages")
        for (s in listOf(1.15f, 1f, 1.3f)) {
            scale = s
            compose.waitForIdle()
            compose.onNodeWithTag("tab").assertTextEquals("Outils › Réglages")
            compose.onNodeWithTag("dialog").assertTextEquals("ouvert")
        }
    }
}
