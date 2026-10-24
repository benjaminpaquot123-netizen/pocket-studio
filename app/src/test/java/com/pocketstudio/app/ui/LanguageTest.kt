package com.pocketstudio.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.onboarding.WelcomeActions
import com.pocketstudio.app.onboarding.WelcomeScreen
import com.pocketstudio.app.session.SessionActions
import com.pocketstudio.app.session.SessionScreen
import com.pocketstudio.app.session.SessionScreenUi
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.Project
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.renameScene
import com.pocketstudio.model.sceneTitle
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The screens follow the language chosen: English or Dutch texts, and none of the French ones left. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h1700dp")
class LanguageTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun french() {
        I18n.lang = Lang.FR
    }

    private fun session() = compose.setContent {
        PocketStudioTheme {
            val drums = DrumPattern().toggle(0, 0)
            SessionScreen(
                SessionScreenUi(
                    scenes = listOf(Scene("A", drums, label = "Refrain"), Scene("B", drums, label = "Pré-refrain 2"), Scene("C", label = "Mon pont")),
                    current = 0, pending = null,
                    song = listOf(SongPart(0, 2), SongPart(1)),
                    songIndex = -1, playing = false, tempoBpm = 120.0,
                ),
                SessionActions(),
            )
        }
    }

    private fun topBar() = compose.setContent {
        PocketStudioTheme {
            Box(Modifier.width(400.dp)) {
                TopBar(TopBarUi("Mon morceau", playing = false, canUndo = true, canRedo = true), TopBarActions())
            }
        }
    }

    @Test
    fun welcomeInEnglish() {
        I18n.lang = Lang.EN
        compose.setContent { PocketStudioTheme { WelcomeScreen(WelcomeActions()) } }
        compose.onNodeWithText("Make my first song (guided)").assertExists()
        compose.onAllNodesWithText("Créer mon premier morceau (guidé)").assertCountEquals(0)
    }

    @Test
    fun welcomeInDutch() {
        I18n.lang = Lang.NL
        compose.setContent { PocketStudioTheme { WelcomeScreen(WelcomeActions()) } }
        compose.onNodeWithText("Mijn eerste nummer maken (begeleid)").assertExists()
        compose.onAllNodesWithText("Écouter une démo").assertCountEquals(0)
    }

    @Test
    fun songScreenInEnglish() {
        I18n.lang = Lang.EN
        session()
        // Section titles are shown in capitals.
        compose.onNodeWithText("Song settings", ignoreCase = true).assertExists()
        // ▶ is the top bar's alone: no « Play the song » button on the page.
        compose.onAllNodesWithText("Play the song", substring = true).assertCountEquals(0)
        // Section names follow the language (a copy's number too); a name typed by the user stays.
        compose.onAllNodesWithText("A · Chorus").assertCountEquals(1)
        compose.onAllNodesWithText("Chorus").assertCountEquals(1)
        compose.onAllNodesWithText("Pre-chorus 2").assertCountEquals(1)
        compose.onAllNodesWithText("Mon pont").assertCountEquals(1)
        compose.onAllNodesWithText("Le morceau", ignoreCase = true).assertCountEquals(0)
        compose.onAllNodesWithText("A · Refrain").assertCountEquals(0)
        compose.onAllNodesWithText("Réglages du morceau", ignoreCase = true).assertCountEquals(0)
        compose.onAllNodesWithText("1 bars").assertCountEquals(0)
    }

    @Test
    fun songScreenInDutch() {
        I18n.lang = Lang.NL
        session()
        compose.onNodeWithText("Instellingen van het nummer", ignoreCase = true).assertExists()
        compose.onNodeWithText("Tik op een deel om het nummer vanaf daar te horen.").assertExists()
        compose.onAllNodesWithText("A · Refrein").assertCountEquals(1)
        compose.onAllNodesWithText("  Jouer le morceau").assertCountEquals(0)
    }

    @Test
    fun undoIsNotCancel() {
        I18n.lang = Lang.EN
        topBar()
        compose.onNodeWithContentDescription("Undo").assertExists()
        compose.onNodeWithContentDescription("Redo").assertExists()
        compose.onAllNodesWithContentDescription("Cancel").assertCountEquals(0)
        compose.onAllNodesWithContentDescription("Annuler la dernière action").assertCountEquals(0)
    }

    @Test
    fun tabsAndSceneTitlesFollowTheLanguage() {
        I18n.lang = Lang.NL
        // Phase 36: the bottom bar shows the five spaces (or the Mode simple's four tabs), no longer the seven tabs.
        assertEquals(listOf("Structuur", "Klanken", "Mix", "Live", "Meer"), Space.entries.map { it.label })
        assertEquals(listOf("Mijn nummer", "Ritme", "Klavier", "Versturen"), SimpleTab.entries.map { it.label })
        assertEquals(listOf("Ritme", "Acid", "Synth", "Mixer", "Audio", "Nummer", "Tools", "Live", "Versturen"), AppTab.entries.map { it.label })
        val p = Project().renameScene(0, "Pont")
        assertEquals("A · Brug", p.sceneTitle(0))
        I18n.lang = Lang.EN
        assertEquals("Song", AppTab.Morceau.label)
        assertEquals(listOf("Structure", "Sounds", "Mix", "Live", "More"), Space.entries.map { it.label })
        assertEquals("A · Bridge", p.sceneTitle(0))
        assertEquals("Verse 3", Scene.shownLabel("Couplet 3"))
        assertEquals("Ma fin", Scene.shownLabel("Ma fin"))
        I18n.lang = Lang.FR
        assertEquals("Morceau", AppTab.Morceau.label)
        assertEquals("A · Pont", p.sceneTitle(0))
    }
}
