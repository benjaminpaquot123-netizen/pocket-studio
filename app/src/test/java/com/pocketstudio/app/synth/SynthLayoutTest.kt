package com.pocketstudio.app.synth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pocketstudio.model.Harmony
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.Project
import com.pocketstudio.model.Scale
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp")
class SynthLayoutTest {
    @get:Rule val compose = createComposeRule()

    private val melody = NoteClip(16, listOf(Note(0, 24, 60), Note(24, 24, 61), Note(48, 24, 64)))

    private fun showSynth(clip: NoteClip = melody, notesFirst: Boolean = false) = compose.setContent {
        PocketStudioTheme {
            SynthScreen(Project().synth, 4, SynthActions(), notes = NotesUi(clip, emptyList(), -1, PianoRollActions()), startWithNotes = notesFirst)
        }
    }

    /** Where the node starts, scrolled out of sight or not. */
    private fun SemanticsNodeInteraction.top() = fetchSemanticsNode().positionInRoot.y

    /** The sound sections, top to bottom: the same in both modes. */
    private fun soundOrder(): List<Float> = listOf(
        compose.onNodeWithTag("sound_area_title").top(),
        compose.onNodeWithText("SONS PRÊTS À JOUER").top(),
        compose.onNodeWithText("RÉGLAGES DU SON").top(),
    )

    @Test
    fun bothModesShowTheSameSoundSectionsInTheSameOrderUnderTheirOwnTools() {
        showSynth()
        // Clavier: the keyboard first, then the track's sound.
        val keys = soundOrder()
        assertEquals(keys.sorted(), keys)
        assertTrue(compose.onNodeWithText("CLAVIER").top() < keys.first())
        compose.onNodeWithTag("mode_notes").performClick()
        // Notes: the piano roll, its import and the scale first, then the same sound sections.
        val notes = soundOrder()
        assertEquals(notes.sorted(), notes)
        assertTrue(compose.onNodeWithText("GAMME ET ACCORDS").top() < notes.first())
        assertTrue(compose.onNodeWithText("Dessiner", substring = true).top() < notes.first())
    }

    @Test
    @Config(qualifiers = "w820dp-h360dp")
    fun aPhoneLyingDownKeepsTheReadyMadeSoundsToo() {
        showSynth()
        // Before, a phone lying down had only four chips at the top and no « Sons prêts à jouer » card.
        val order = soundOrder()
        assertEquals(order.sorted(), order)
    }

    @Test
    fun withoutAChordTheChordPlayIsOffAndPointsToTheSound() {
        var toSound = 0
        compose.setContent { PocketStudioTheme { Column(Modifier.verticalScroll(rememberScrollState())) { HarmonyPanel(Harmony(), melody, PianoRollActions(), toSound = { toSound++ }) } } }
        compose.onNodeWithText("Jeu des accords").assertExists()
        compose.onNodeWithTag("arp_Up").assertIsNotEnabled()
        compose.onNodeWithTag("strum_3").assertIsNotEnabled()
        // Turning things off is always possible.
        compose.onNodeWithTag("arp_Off").assertIsEnabled()
        compose.onNodeWithTag("strum_0").assertIsEnabled()
        compose.onNodeWithTag("chord_play_none").assertExists()
        compose.onNodeWithTag("go_to_sound").performScrollTo().performClick()
        assertEquals(1, toSound)
    }

    @Test
    fun anArpeggioLeftOnCanBeTurnedOffWithoutAChord() {
        val arpeggiated = melody.copy(performance = melody.performance.copy(arp = com.pocketstudio.model.ArpMode.Up, strum = 8))
        var picked: com.pocketstudio.model.Performance? = null
        compose.setContent { PocketStudioTheme { Column(Modifier.verticalScroll(rememberScrollState())) { HarmonyPanel(Harmony(), arpeggiated, PianoRollActions(setPerformance = { picked = it })) } } }
        compose.onNodeWithTag("arp_Off").performScrollTo().assertIsEnabled().performClick()
        assertEquals(com.pocketstudio.model.ArpMode.Off, picked?.arp)
        compose.onNodeWithTag("strum_0").performScrollTo().assertIsEnabled().performClick()
        assertEquals(0, picked?.strum)
    }

    @Test
    fun withAChordArpeggioAndStrumCanBeChosen() {
        val chord = melody.copy(notes = melody.notes + Note(0, 24, 64))
        var picked: com.pocketstudio.model.Performance? = null
        compose.setContent { PocketStudioTheme { Column(Modifier.verticalScroll(rememberScrollState())) { HarmonyPanel(Harmony(), chord, PianoRollActions(setPerformance = { picked = it })) } } }
        compose.onNodeWithTag("arp_Up").assertIsEnabled().performClick()
        assertEquals(com.pocketstudio.model.ArpMode.Up, picked?.arp)
        compose.onNodeWithTag("chord_play_none").assertDoesNotExist()
    }

    @Test
    fun theScaleSaysItKeepsTheNotesAndOffersToFitThem() {
        var replaced: NoteClip? = null
        compose.setContent {
            PocketStudioTheme { Column(Modifier.verticalScroll(rememberScrollState())) { HarmonyPanel(Harmony(0, Scale.Major), melody, PianoRollActions(replaceClip = { c, _ -> replaced = c })) } }
        }
        compose.onNodeWithTag("scale_keeps_notes").assertExists()
        // One note (C♯) is out of C major: one touch puts it in.
        compose.onNodeWithTag("fit_to_scale").performScrollTo().performClick()
        assertEquals(listOf(60, 64), replaced?.notes?.map { it.pitch }?.distinct())
    }
}
