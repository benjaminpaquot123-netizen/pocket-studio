package com.pocketstudio.app.ui

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.app.synth.ChordPads
import com.pocketstudio.app.synth.HarmonyPanel
import com.pocketstudio.app.synth.PianoRollActions
import com.pocketstudio.model.Harmony
import com.pocketstudio.model.Performance
import com.pocketstudio.model.Scale
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The "C D E" setting reaches every screen that names notes: chord pads, their title, the key chips. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h1700dp")
class NotationTest {
    @get:Rule
    val compose = createComposeRule()
    private val app: Application = ApplicationProvider.getApplicationContext()

    @After
    fun french() {
        SettingsStore.update(app) { it.copy(language = Lang.FR, englishNotes = false) }
        I18n.lang = Lang.FR
    }

    /** A French note name as a word ("Ré m", "La mineure"; not the "Do" of "Dorian"). */
    private val frenchNote = Regex("""(^|[\s·])(Do|Ré|Mi|Fa|Sol|La|Si)([♯♭#]|\s|$)""")

    private fun assertNoFrenchNoteNames() {
        val texts = compose.onAllNodes(SemanticsMatcher("any text") { SemanticsProperties.Text in it.config })
            .fetchSemanticsNodes().flatMap { n -> n.config[SemanticsProperties.Text].map { it.text } }
        assertTrue("no text found", texts.isNotEmpty())
        assertEquals(emptyList<String>(), texts.filter { frenchNote.containsMatchIn(it) })
    }

    @Test
    fun chordPadsInEnglishNameNotesCDE() {
        SettingsStore.update(app) { it.copy(language = Lang.EN, englishNotes = true) }
        compose.setContent { PocketStudioTheme { ChordPads(Harmony(9, Scale.Minor, lock = true), 48, { _, _ -> }, {}) } }
        compose.onNodeWithText("Chords in A minor").assertExists()
        listOf("Am", "Bdim", "C", "Dm", "Em", "F", "G").forEach { compose.onAllNodesWithText(it)[0].assertExists() }
        assertNoFrenchNoteNames()
    }

    @Test
    fun theKeyChipsAndGuessesInEnglishNameNotesCDE() {
        SettingsStore.update(app) { it.copy(language = Lang.EN, englishNotes = true) }
        compose.setContent { PocketStudioTheme { Column { HarmonyPanel(Harmony(9, Scale.Minor), Performance(), PianoRollActions()) } } }
        compose.onNodeWithText("B♭").assertExists()
        compose.onNodeWithText("F♯").assertExists()
        assertNoFrenchNoteNames()
    }

    @Test
    fun frenchNotationStaysDoReMi() {
        SettingsStore.update(app) { it.copy(language = Lang.FR, englishNotes = false) }
        compose.setContent { PocketStudioTheme { ChordPads(Harmony(9, Scale.Minor, lock = true), 48, { _, _ -> }, {}) } }
        compose.onNodeWithText("Accords de La mineure").assertExists()
        compose.onNodeWithText("Si dim").assertExists()
    }

    @Test
    fun theKeyboardOctaveFollowsTheNotation() {
        // The keyboard starting on middle C (MIDI 60): Do3 in Do Ré Mi, C4 in C D E.
        SettingsStore.update(app) { it.copy(englishNotes = false) }
        compose.setContent { PocketStudioTheme { com.pocketstudio.app.synth.OctaveControl(4, com.pocketstudio.app.synth.SynthActions()) } }
        compose.onNodeWithText("Octave 3").assertExists()
        SettingsStore.update(app) { it.copy(englishNotes = true) }
        compose.waitForIdle()
        compose.onNodeWithText("Octave 4").assertExists()
    }
}
