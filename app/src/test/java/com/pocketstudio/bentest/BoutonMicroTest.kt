package com.pocketstudio.bentest

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.pocketstudio.app.journey.ComposeRuleDriver
import com.pocketstudio.app.journey.Journey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** « Écoute au micro » on BenStudio Test's screen (T1b): its button, the sentence before the microphone is asked for, the extracts said before sending. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class BoutonMicroTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun theButtonSaysWhatWillHappenBeforeTheMicrophoneIsAskedFor() {
        var started = 0
        var etat by mutableStateOf(Robot.Etat())
        compose.setContent {
            BenTestScreen(etat, listOf(Triple("android.permission.RECORD_AUDIO", "Autoriser le micro", false)), BenTestActions(startMicro = { started++ }))
        }
        val j = Journey(ComposeRuleDriver(compose))
        assertTrue("« Écoute au micro » on the screen", j.exists("bentest_micro"))
        j.tap("bentest_micro")
        j.await("bentest_micro_phrase")
        assertEquals("nothing started before « Commencer »", 0, started)
        compose.onNodeWithTag("bentest_micro_phrase").assertTextContains("micro", substring = true)
        compose.onNodeWithTag("bentest_micro_phrase").assertTextContains("Rien n'est enregistré", substring = true)
        compose.onNodeWithTag("bentest_micro_phrase").assertTextContains("au plus 4", substring = true)
        j.tap("bentest_micro_go")
        assertEquals(1, started)

        etat = Robot.Etat(verdict = "5 verts, 0 orange, 0 rouge", passage = Robot.MICRO, extraits = 1)
        j.await("bentest_extraits")
        compose.onNodeWithTag("bentest_extraits").assertTextContains("2 s", substring = true)
        assertTrue(j.exists("bentest_envoyer"))
    }
}
