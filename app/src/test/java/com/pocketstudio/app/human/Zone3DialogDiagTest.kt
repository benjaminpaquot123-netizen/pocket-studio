package com.pocketstudio.app.human

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Audit ultra round 3, n° 16 (seats B a and E c, « gel en tapant un nom »): the bench. Not one line of the app here:
 * Material's own AlertDialog holding a plain text field (or even Compose's Dialog with a BasicTextField, in either
 * graphics mode, with or without the app's theme) never lets Compose rest under Robolectric (AppNotIdleException after
 * 60 s, before any touch), while the same field alone or in a Popup rests. Every rename window of the app is such a
 * dialog, hence the bench « frozen » there; a phone runs this everyday pairing. Kept as the proof (ignored: 60 s each).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class Zone3DialogDiagTest {
    @get:Rule
    val compose = androidx.compose.ui.test.junit4.createComposeRule()

    @Test
    @Ignore("Round 3 proof, the bench (Robolectric): a text field in any Dialog never idles, with no app code at all.")
    fun aPlainMaterialDialogWithATextFieldNeverRestsUnderRobolectric() {
        compose.setContent {
            var v by remember { mutableStateOf(TextFieldValue("abc")) }
            AlertDialog({}, { Text("OK") }, text = { OutlinedTextField(v, { v = it }) })
        }
        compose.waitForIdle()
    }

    /** The control: the same field in a Popup (a window of its own too) rests. */
    @Test
    fun theSameFieldInAPopupRests() {
        compose.setContent {
            var v by remember { mutableStateOf(TextFieldValue("abc")) }
            androidx.compose.ui.window.Popup { OutlinedTextField(v, { v = it }) }
        }
        compose.waitForIdle()
    }
}
