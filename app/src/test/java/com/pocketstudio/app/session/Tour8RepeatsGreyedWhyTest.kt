package com.pocketstudio.app.session

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import com.pocketstudio.ui.PocketStudioTheme
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongPart
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tour 8, lot T0c (seat 3a #14, beginner's control t0c-a, screen 62): after « Donner les durées conseillées » every
 * part has a fixed length, so every − / + is greyed; the help line only spoke of Live. It now says why they are grey.
 */
@RunWith(RobolectricTestRunner::class)
class Tour8RepeatsGreyedWhyTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun partsWithTheSuggestedLengthsSayWhyMinusPlusAreGrey() {
        val drums = DrumPattern()
        compose.setContent {
            PocketStudioTheme {
                SessionScreen(
                    SessionScreenUi(
                        scenes = listOf(Scene("A", drums, label = "Intro"), Scene("B", drums, label = "Drop")) + List(22) { null },
                        current = 0, pending = null, songIndex = -1, playing = false, tempoBpm = 132.0,
                        // What « Donner les durées conseillées » leaves: one repeat, a length in bars.
                        song = listOf(SongPart(0, 1, bars = 8), SongPart(1, 1, bars = 16)),
                    ),
                    SessionActions(),
                )
            }
        }
        val text = compose.onNodeWithTag("song_repeats_help", useUnmergedTree = true).fetchSemanticsNode()
            .config[SemanticsProperties.Text].joinToString("") { it.text }
        assertTrue("the help under greyed − / + must say why (a length given, « conseillée »): $text", "Gris" in text && "conseillée" in text)
        // Under the parts' − / + (« juste sous les −/+ »), never above them: above, it pushed the first part off a
        // phone's screen (SpacesTest, CI 380).
        val help = compose.onNodeWithTag("song_repeats_help", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val last = compose.onNodeWithTag("part_1").getUnclippedBoundsInRoot()
        assertTrue("the help must sit under the last part: help top ${help.top}, part bottom ${last.bottom}", help.top >= last.bottom)
    }
}
