package com.pocketstudio.app.onboarding

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.pocketstudio.app.library.IndexState
import com.pocketstudio.app.library.PacksUi
import com.pocketstudio.app.library.PartRow
import com.pocketstudio.app.library.WizardActions
import com.pocketstudio.app.library.WizardRow
import com.pocketstudio.app.library.WizardScreen
import com.pocketstudio.app.library.WizardUi
import com.pocketstudio.app.library.soundCategories
import com.pocketstudio.app.projects.ProjectStarter
import com.pocketstudio.app.projects.ProjectsActions
import com.pocketstudio.app.projects.ProjectsScreen
import com.pocketstudio.app.projects.ProjectsUi
import com.pocketstudio.app.projects.StarterGroup
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.ui.AppTab
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class OnboardingUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theWelcomeOffersTheThreeWaysToStart() {
        val chosen = mutableListOf<String>()
        compose.setContent {
            PocketStudioTheme {
                WelcomeScreen(WelcomeActions(guided = { chosen += "guided" }, demo = { chosen += "demo" }, empty = { chosen += "empty" }, later = { chosen += "later" }))
            }
        }
        compose.onNodeWithText("Créer mon premier morceau (guidé)").assertExists()
        compose.onNodeWithTag("welcome_guided").performClick()
        compose.onNodeWithTag("welcome_demo").performClick()
        compose.onNodeWithTag("welcome_empty").performClick()
        compose.onNodeWithTag("welcome_later").performScrollTo().performClick()
        assertEquals(listOf("guided", "demo", "empty", "later"), chosen)
    }

    @Test
    fun theFirstLaunchShowsOnlyWhenAsked() {
        compose.setContent { PocketStudioTheme { FirstLaunch(show = false, actions = WelcomeActions(), playDemo = {}) } }
        compose.onNodeWithTag("welcome").assertDoesNotExist()
        compose.onNodeWithTag("demo_ready").assertDoesNotExist()
    }

    @Test
    fun theNextStepCardGoesToTheRightTabAndCloses() {
        val tabs = mutableListOf<AppTab>()
        var closed = false
        // The starter beat: the melody is next, in Synthé.
        val steps = nextSteps(RhythmViewModel.starterProject())
        compose.setContent { PocketStudioTheme { NextStepCard(steps, go = { tabs += it }, close = { closed = true }) } }
        compose.onNodeWithTag("next_step_title").assertExists()
        compose.onNodeWithText("3. Mélodie").assertExists()
        compose.onNodeWithTag("next_step_go").performClick()
        assertEquals(listOf(AppTab.Synthe), tabs)
        // Another step chosen: its hint and its button.
        compose.onNodeWithTag("next_step_Song").performClick()
        compose.onNodeWithText("5. Morceau").assertExists()
        compose.onNodeWithTag("next_step_go").performClick()
        assertEquals(AppTab.Morceau, tabs.last())
        compose.onNodeWithTag("next_steps_close").performClick()
        assertEquals(true, closed)
    }

    @Test
    fun aStepOnTheRhythmTabHasNoButtonAndAManualOneCanBeTicked() {
        val marked = mutableListOf<StepGoal>()
        compose.setContent {
            PocketStudioTheme {
                NextStepCard(nextSteps(com.pocketstudio.model.Project()), mark = { marked += it })
            }
        }
        compose.onNodeWithText("1. Rythme").assertExists()
        compose.onNodeWithTag("next_step_go").assertDoesNotExist()
        compose.onNodeWithTag("next_step_Share").performClick()
        compose.onNodeWithTag("next_step_done").performClick()
        assertEquals(listOf(StepGoal.Share), marked)
    }

    @Test
    fun newProjectChoicesAreGroupedUnderHeadings() {
        var wizard = 0
        val created = mutableListOf<Int>()
        val starters = listOf(
            ProjectStarter("Techno", "4/4", "genre_0", StarterGroup.Style),
            ProjectStarter("Acid house", "Démo", "demo_0", StarterGroup.Demo),
            ProjectStarter("Projet vide", "Vierge", "empty", StarterGroup.Empty),
        )
        compose.setContent {
            PocketStudioTheme {
                ProjectsScreen(ProjectsUi(loading = false), "p-0", starters, ProjectsActions(wizard = { wizard++ }, create = { created += it }))
            }
        }
        // The guided song is the main button of the list.
        compose.onNodeWithTag("project_wizard").performClick()
        assertEquals(1, wizard)
        compose.onNodeWithTag("project_new").performClick()
        for (heading in listOf("GUIDÉ", "PARTIR D'UN STYLE", "ÉCOUTER UNE DÉMO", "VIDE")) compose.onNodeWithText(heading).assertExists()
        compose.onNodeWithTag("starter_demo_0").performScrollTo().performClick()
        assertEquals(listOf(1), created)
    }

    @Test
    fun theSummaryShowsTheThreeSectionsAndTheSong() {
        val style = StarterWizard.style("house")!!
        val rows = style.lines.associate { l ->
            val cats = soundCategories(l.role, style.id, emptyList())
            l.role to WizardRow(true, cats.first().id, cats.first().options.first())
        }
        val parts = StarterWizard.Part.entries.associateWith { PartRow(true, StarterWizard.soundsFor(it, style).first()) }
        val ui = WizardUi(true, 0, style.id, rows, StarterWizard.Mood.Happy, parts)
        compose.setContent {
            PocketStudioTheme { WizardScreen(ui.copy(step = ui.lastStep), PacksUi(IndexState.Unavailable), null, WizardActions()) }
        }
        compose.onNodeWithText("Intro · le début, en douceur").assertExists()
        compose.onNodeWithText("Couplet · le groove, avec la basse").assertExists()
        compose.onNodeWithText("Refrain · tout le monde joue").assertExists()
        compose.onNodeWithText("Intro → Couplet → Refrain → Couplet → Refrain → Intro pour finir", substring = true).assertExists()
    }

    @Test
    fun eachLineSaysWhichSectionsItPlaysIn() {
        val style = StarterWizard.style("house")!!
        val rows = style.lines.associate { l ->
            val cats = soundCategories(l.role, style.id, emptyList())
            l.role to WizardRow(true, cats.first().id, cats.first().options.first())
        }
        val ui = WizardUi(true, 2, style.id, rows, null, emptyMap())  // step 2: the kick
        compose.setContent { PocketStudioTheme { WizardScreen(ui, PacksUi(IndexState.Unavailable), null, WizardActions()) } }
        compose.onNodeWithText("Joue dans les trois sections").assertExists()
    }
    @Test
    fun theShareStepIsTickedOnlyOnceTheSongIsShared() {
        val marked = mutableListOf<StepGoal>()
        var shared = 0
        // Everything done but the sharing: the card shows it, with its one-touch button.
        val steps = nextSteps(com.pocketstudio.model.Project()).map { it.copy(done = it.goal != StepGoal.Share) }
        compose.setContent { PocketStudioTheme { NextStepCard(steps, mark = { marked += it }, share = { shared++ }) } }
        compose.onNodeWithText("7. Partager").assertExists()
        compose.onNodeWithTag("next_step_go").performClick()
        // The export and the share sheet tick it (rememberShareSong), not the touch: cancelling leaves it to do.
        assertEquals(1, shared)
        assertEquals(emptyList<StepGoal>(), marked)
    }

    @Test
    fun theRhythmStepBringsTheGridIntoView() {
        var shown = 0
        compose.setContent { PocketStudioTheme { NextStepCard(nextSteps(com.pocketstudio.model.Project()), showHere = { shown++ }) } }
        compose.onNodeWithText("1. Rythme").assertExists()
        compose.onNodeWithTag("next_step_here").assertTextEquals("Voir la grille").performClick()
        assertEquals(1, shown)
    }

    @Test
    fun aClosedCardCanBeShownAgain() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val prefs = NextStepPrefs(context, "p-reopen")
        val before = NextStepPrefs.changes
        prefs.close()
        assertEquals(true, NextStepPrefs(context, "p-reopen").closed)
        prefs.reopen()
        assertEquals(false, NextStepPrefs(context, "p-reopen").closed)
        // A card already on screen reads its prefs again.
        assertEquals(true, NextStepPrefs.changes > before)
        prefs.mark(StepGoal.Share)
        assertEquals(setOf(StepGoal.Share), NextStepPrefs(context, "p-reopen").marks)
    }

    private fun houseUi(step: Int): WizardUi {
        val style = StarterWizard.style("house")!!
        val rows = style.lines.associate { l ->
            val cats = soundCategories(l.role, style.id, emptyList())
            l.role to WizardRow(true, cats.first().id, cats.first().options.first())
        }
        val parts = StarterWizard.Part.entries.associateWith { PartRow(true, StarterWizard.soundsFor(it, style).first()) }
        return WizardUi(true, step, style.id, rows, StarterWizard.Mood.Happy, parts)
    }

    @Test
    fun theAssistantCanCreateTheSongFromTheSecondStep() {
        var created = 0
        var ui by androidx.compose.runtime.mutableStateOf(houseUi(1))
        compose.setContent { PocketStudioTheme { WizardScreen(ui, PacksUi(IndexState.Unavailable), null, WizardActions(create = { created++ })) } }
        compose.onNodeWithTag("wizard_create_now").assertTextEquals("Créer tout de suite (choix conseillés)").performClick()
        assertEquals(1, created)
        compose.onNodeWithText("Étape 2 sur ${ui.lastStep + 1} · House").assertExists()
        // On the summary, « Créer le morceau » is the button: no shortcut twice.
        ui = ui.copy(step = ui.lastStep)
        compose.onNodeWithTag("wizard_create_now").assertDoesNotExist()
        compose.onNodeWithTag("wizard_create").assertExists()
        // No style yet: nothing to create.
        ui = WizardUi(open = true)
        compose.onNodeWithTag("wizard_create_now").assertDoesNotExist()
        compose.onNodeWithText("Étape 1 sur", substring = true).assertExists()
    }

    @Test
    fun closingTheAssistantAsksFromTheThirdStep() {
        var closed = 0
        var ui by androidx.compose.runtime.mutableStateOf(houseUi(1))
        compose.setContent { PocketStudioTheme { WizardScreen(ui, PacksUi(IndexState.Unavailable), null, WizardActions(close = { closed++ })) } }
        // Step 2: little to lose, it closes at once.
        compose.onNodeWithTag("wizard_close").performClick()
        assertEquals(1, closed)
        ui = houseUi(2)
        compose.onNodeWithTag("wizard_close").performClick()
        assertEquals(1, closed)
        compose.onNodeWithText("Quitter l'assistant ?").assertExists()
        compose.onNodeWithText("Continuer").performClick()
        assertEquals(1, closed)
        compose.onNodeWithTag("wizard_close").performClick()
        compose.onNodeWithTag("wizard_close_confirm").performClick()
        assertEquals(2, closed)
    }

    @Test
    fun eachStyleCanBeHeardBeforeChoosingIt() {
        val heard = mutableListOf<com.pocketstudio.app.library.SoundRef>()
        val chosen = mutableListOf<String>()
        compose.setContent {
            PocketStudioTheme {
                WizardScreen(WizardUi(open = true), PacksUi(IndexState.Unavailable), null, WizardActions(preview = { heard += it }, chooseStyle = { chosen += it }))
            }
        }
        compose.onNodeWithTag("wizard_style_listen_house").performScrollTo().performClick()
        assertEquals(listOf<com.pocketstudio.app.library.SoundRef>(com.pocketstudio.app.library.SoundRef.StyleDemo("house")), heard)
        // Listening does not choose: the card itself does.
        assertEquals(emptyList<String>(), chosen)
        compose.onNodeWithTag("wizard_style_house").performClick()
        assertEquals(listOf("house"), chosen)
    }
}
