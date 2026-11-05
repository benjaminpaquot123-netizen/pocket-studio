package com.pocketstudio.app.ui

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Benjamin's n° 21 (« M et S pas intuitifs »), règle « partout »: no screen draws the letters M or S
 * for mute / solo any more; every mute and solo button is the shared [MuteToggle] / [SoloToggle]
 * (speaker, crossed speaker, headphones), with its words for TalkBack and in Mode simple.
 */
@RunWith(RobolectricTestRunner::class)
class MuteSoloEverywhereTest {
    @get:Rule
    val compose = createComposeRule()

    private val sources = File("src/main/java").walkTopDown().filter { it.extension == "kt" }.toList()

    @Test
    fun noScreenDrawsTheLettersMOrS() {
        val letter = Regex("""Text\(\s*"[MS]"|\(\s*"[MS]"\s*,\s*[\w.]*(mute|solo|Mute|Solo)|label\s*=\s*\{\s*Text\("[MS]"\)""")
        val found = sources.flatMap { f -> f.readLines().withIndex().filter { letter.containsMatchIn(it.value) }.map { "${f.name}:${it.index + 1}" } }
        assertTrue("M / S letters instead of the shared icons: $found", found.isEmpty())
    }

    @Test
    fun everyScreenWithMuteOrSoloUsesTheSharedButtons() {
        val screens = listOf("RemixScreen.kt", "SimpleMix.kt", "Console.kt", "TimelineScreen.kt", "LiveScreen.kt", "AppSpaces.kt")
        screens.forEach { name ->
            val f = sources.first { it.name == name }
            assertTrue("$name: mute without the shared button", "MuteToggle(" in f.readText())
        }
        // The only place that draws a mute / solo icon is the shared component.
        val own = sources.filter { f -> f.name != "MuteSolo.kt" && Regex("""Icons\.(AutoMirrored\.)?Filled\.VolumeOff\b""").containsMatchIn(f.readText()) && "toggleMute" in f.readText() }
        assertTrue("A screen draws its own mute / solo icon: ${own.map { it.name }}", own.isEmpty())
    }

    @Test
    fun theButtonsSayWhatTheyDoAndShowAWordInModeSimple() {
        var muted by mutableStateOf(false)
        var solo by mutableStateOf(false)
        compose.setContent {
            PocketStudioTheme {
                androidx.compose.foundation.layout.Row {
                    MuteToggle(muted, "Voix", "m", caption = true) { muted = !muted }
                    SoloToggle(solo, "Voix", "s", caption = true) { solo = !solo }
                }
            }
        }
        // Audit ultra round 2, n° 19 (seat B b: « Son », « désactivé » while the track played): the switch
        // « Son de Voix » is on while the track sounds; what a touch does is its action's words.
        compose.onNodeWithTag("m").assertIsOn().assertContentDescriptionEquals("Son de Voix")
        org.junit.Assert.assertEquals("Couper le son de Voix", compose.onNodeWithTag("m").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsActions.OnClick].label)
        compose.onNodeWithText("Son").assertExists()
        compose.onNodeWithTag("m").performClick()
        compose.onNodeWithTag("m").assertIsOff().assertContentDescriptionEquals("Son de Voix")
        org.junit.Assert.assertEquals("Remettre le son de Voix", compose.onNodeWithTag("m").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsActions.OnClick].label)
        compose.onNodeWithText("Muet").assertExists()
        compose.onNodeWithTag("s").assertContentDescriptionEquals("Écouter la piste Voix seule").performClick()
        compose.onNodeWithTag("s").assertIsOn()
        compose.onNodeWithText("Seule").assertExists()
        assertEquals(MuteSolo.muteIcon(true), MuteSolo.muteIcon(true)); org.junit.Assert.assertNotEquals(MuteSolo.muteIcon(true), MuteSolo.muteIcon(false))
    }
}
