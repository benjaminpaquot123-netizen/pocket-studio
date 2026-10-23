package com.pocketstudio.app.onboarding

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertIsSelected
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
        compose.setContent { PocketStudioTheme { NextStepCard(steps, go = { tabs += it }, close = { closed = true }, compact = false) } }
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
                NextStepCard(nextSteps(com.pocketstudio.model.Project()), mark = { marked += it }, compact = false)
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
        val quick = mutableListOf<String>()
        val custom = mutableListOf<String>()
        val previewed = mutableListOf<Int>()
        val starters = listOf(
            ProjectStarter("Rythme de départ", "Un rythme", "starter", StarterGroup.Style),
            ProjectStarter("Acid house", "Démo", "demo_0", StarterGroup.Demo),
            ProjectStarter("Projet vide", "Vierge", "empty", StarterGroup.Empty),
        )
        compose.setContent {
            PocketStudioTheme {
                ProjectsScreen(
                    ProjectsUi(loading = false), "p-0", starters,
                    ProjectsActions(wizard = { wizard++ }, create = { created += it }, quickStyle = { quick += it }, customStyle = { custom += it }, previewDemo = { previewed += it }),
                )
            }
        }
        // The guided song is the main button of the list.
        compose.onNodeWithTag("project_wizard").performClick()
        assertEquals(1, wizard)
        compose.onNodeWithTag("project_new").performClick()
        for (heading in listOf("PARTIR D'UN STYLE", "ÉCOUTER UNE DÉMO", "VIDE")) compose.onNodeWithText(heading).assertExists()
        // Every family is folded at first (no style on screen); one touch opens one.
        StarterWizard.STYLES.forEach { compose.onNodeWithTag("quick_style_${it.id}").assertDoesNotExist() }
        StarterWizard.FAMILIES.forEach { compose.onNodeWithTag("quick_family_$it").assertExists() }
        compose.onNodeWithTag("quick_family_Techno").performScrollTo().performClick()
        compose.onNodeWithTag("quick_family_House").performScrollTo().performClick()
        compose.waitForIdle()
        StarterWizard.STYLES.filter { it.family == "Techno" || it.family == "House" }.forEach { compose.onNodeWithTag("quick_style_${it.id}").assertExists() }
        compose.onNodeWithTag("quick_style_trap").assertDoesNotExist()  // Hip-hop still folded
        compose.onNodeWithTag("quick_style_techno").performScrollTo().performClick()
        compose.onNodeWithTag("quick_style_house").performScrollTo().performClick()
        assertEquals(listOf("techno", "house"), quick)
        // A folded family opens with one touch; « Personnaliser pas à pas » opens the assistant on that style.
        compose.onNodeWithTag("quick_family_Hip-hop").performScrollTo().performClick()
        compose.onNodeWithTag("quick_custom_trap").performScrollTo().performClick()
        assertEquals(listOf("trap"), custom)
        // An open family closes the same way.
        compose.onNodeWithTag("quick_family_Techno").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("quick_style_techno").assertDoesNotExist()
        compose.onNodeWithTag("quick_style_house").assertExists()
        // The other starters still there; the old one-scene genres are gone.
        compose.onNodeWithTag("starter_starter").assertExists()
        compose.onNodeWithTag("starter_genre_0").assertDoesNotExist()
        // A demo only plays: its play button previews it, no project is created, the card itself does nothing.
        compose.onNodeWithTag("starter_demo_0").performScrollTo().performClick()
        assertTrue(created.isEmpty())
        compose.onNodeWithTag("demo_play_0").performScrollTo().performClick()
        assertEquals(listOf(0), previewed)
        assertTrue(created.isEmpty())
        // Empty project and starting beat still create.
        compose.onNodeWithTag("starter_empty").performScrollTo().performClick()
        assertEquals(listOf(2), created)
    }

    @Test
    fun aStyleSongBeingPreparedIsShownAndTheOtherChoicesWait() {
        val quick = mutableListOf<String>()
        var dismissed = 0
        var state by androidx.compose.runtime.mutableStateOf(com.pocketstudio.app.library.QuickUi(creating = "house"))
        compose.setContent {
            PocketStudioTheme {
                ProjectsScreen(
                    ProjectsUi(loading = false), "p-0", emptyList(),
                    ProjectsActions(quickStyle = { quick += it }, dismissQuickError = { dismissed++ }), quick = state,
                )
            }
        }
        compose.onNodeWithTag("project_new").performClick()
        compose.onNodeWithTag("quick_preparing", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Préparation du morceau…", substring = true).assertExists()
        compose.onNodeWithTag("quick_family_Techno").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("quick_style_techno").performScrollTo().performClick()
        assertTrue(quick.isEmpty())
        // A failure is said in plain words, and the styles can be touched again.
        state = com.pocketstudio.app.library.QuickUi(error = "Le morceau n'a pas pu être préparé (son illisible : Kick).")
        compose.onNodeWithTag("quick_preparing").assertDoesNotExist()
        compose.onNodeWithTag("quick_error").assertTextEquals("Le morceau n'a pas pu être préparé (son illisible : Kick).").performClick()
        assertEquals(1, dismissed)
        compose.onNodeWithTag("quick_style_techno").performScrollTo().performClick()
        assertEquals(listOf("techno"), quick)
    }

    @Test
    fun theSummaryShowsTheThreeSectionsAndTheSong() {
        val style = StarterWizard.style("pop")!!
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
    fun clubMusicShowsItsFourSectionsWithTheBreak() {
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
        compose.onNodeWithText("Quatre sections", substring = true, ignoreCase = true).assertExists()
        compose.onNodeWithText("Intro · la batterie pour démarrer").assertExists()
        compose.onNodeWithText("Break · une pause sans kick ni basse").assertExists()
        compose.onNodeWithText("Intro → Groove → Drop → Break → Drop → Intro pour finir", substring = true).assertExists()
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
        // House is club music: the kick plays everywhere but in the Break.
        compose.onNodeWithText("Joue dans Intro, Groove et Drop").assertExists()
    }
    @Test
    fun theShareStepIsTickedOnlyOnceTheSongIsShared() {
        val marked = mutableListOf<StepGoal>()
        var shared = 0
        // Everything done but the sharing: the card shows it, with its one-touch button.
        val steps = nextSteps(com.pocketstudio.model.Project()).map { it.copy(done = it.goal != StepGoal.Share) }
        compose.setContent { PocketStudioTheme { NextStepCard(steps, mark = { marked += it }, share = { shared++ }, compact = false) } }
        compose.onNodeWithText("8. Partager").assertExists()
        compose.onNodeWithTag("next_step_go").performClick()
        // The export and the share sheet tick it (rememberShareSong), not the touch: cancelling leaves it to do.
        assertEquals(1, shared)
        assertEquals(emptyList<StepGoal>(), marked)
    }

    @Test
    fun onAPhoneTheCardIsOneLineThatOpens() {
        val tabs = mutableListOf<AppTab>()
        // Right after the assistant: rhythm, bass, melody, sections and song done, the volumes next.
        val steps = nextSteps(com.pocketstudio.model.Project()).map { it.copy(done = it.goal < StepGoal.Volumes) }
        compose.setContent { PocketStudioTheme { NextStepCard(steps, go = { tabs += it }) } }
        // A phone's width: one line, the step's name, no hint or button yet.
        compose.onNodeWithTag("next_steps_compact").assertExists()
        compose.onNodeWithText("Volumes").assertExists()
        compose.onNodeWithTag("next_step_go").assertDoesNotExist()
        compose.onNodeWithTag("next_steps_compact").performClick()
        compose.onNodeWithText("7. Volumes").assertExists()
        compose.onNodeWithTag("next_step_go").performClick()
        assertEquals(listOf(AppTab.Mixeur), tabs)
        // And it folds back to its line.
        compose.onNodeWithTag("next_steps_shrink").performClick()
        compose.onNodeWithTag("next_steps_compact").assertExists()
    }

    @Test
    fun theRhythmStepBringsTheGridIntoView() {
        var shown = 0
        compose.setContent { PocketStudioTheme { NextStepCard(nextSteps(com.pocketstudio.model.Project()), showHere = { shown++ }, compact = false) } }
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
        compose.onNodeWithText("Quitter sans créer le morceau ?").assertExists()
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
        compose.onNodeWithTag("wizard_family_House").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("wizard_style_listen_house").performScrollTo().performClick()
        assertEquals(listOf<com.pocketstudio.app.library.SoundRef>(com.pocketstudio.app.library.SoundRef.StyleDemo("house")), heard)
        // Listening does not choose, nor does a touch on the card: it opens it to be read.
        assertEquals(emptyList<String>(), chosen)
        compose.onNodeWithTag("wizard_style_choose_house").assertDoesNotExist()
        compose.onNodeWithTag("wizard_style_house").performClick()
        assertEquals(emptyList<String>(), chosen)
        // Opened: every reference, and the button that chooses.
        StarterWizard.style("house")!!.references.forEach { compose.onNodeWithText("• $it").assertExists() }
        compose.onNodeWithTag("wizard_style_choose_house").performScrollTo().performClick()
        assertEquals(listOf("house"), chosen)
    }

    @Test
    fun theStylesComeByFamilyInBenjaminsOrder() {
        compose.setContent {
            PocketStudioTheme { WizardScreen(WizardUi(open = true), PacksUi(IndexState.Unavailable), null, WizardActions()) }
        }
        // Every family is folded until touched.
        StarterWizard.STYLES.forEach { s -> compose.onNodeWithTag("wizard_style_${s.id}").assertDoesNotExist() }
        compose.onNodeWithTag("wizard_family_Techno").performScrollTo().performClick()
        compose.onNodeWithTag("wizard_family_House").performScrollTo().performClick()
        compose.waitForIdle()
        StarterWizard.STYLES.filter { it.family == "Techno" || it.family == "House" }.forEach { compose.onNodeWithTag("wizard_style_${it.id}").assertExists() }
        compose.onNodeWithTag("wizard_family_Hip-hop").performScrollTo().performClick()
        compose.waitForIdle()
        StarterWizard.stylesOf("Hip-hop").forEach { compose.onNodeWithTag("wizard_style_${it.id}").assertExists() }
        compose.onNodeWithTag("wizard_family_Techno").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("wizard_style_techno").assertDoesNotExist()
        compose.onNodeWithTag("wizard_style_house").assertExists()
    }

    @Test
    fun aPartTheRecipeLeavesOutSaysSoAndTheLeadIsNoArpeggioWhenItIsARiff() {
        val trap = StarterWizard.style("trap")!!
        assertTrue(trap.melody.leadPattern.isNotBlank())
        val riff = com.pocketstudio.app.library.partNotes(trap, StarterWizard.Part.Lead).joinToString()
        assertFalse(riff, riff.contains("arpégiateur"))
        val riddim = StarterWizard.style("riddim")!!
        assertTrue(riddim.melody.leadPattern.isBlank())
        assertTrue(com.pocketstudio.app.library.partNotes(riddim, StarterWizard.Part.Lead).joinToString().contains("arpégiateur"))
        // A recipe's own word and its signature are said.
        val signed = trap.copy(melody = trap.melody.copy(signature = StarterWizard.Part.Lead, tips = mapOf(StarterWizard.Part.Lead to "La cloche qui tourne.")))
        assertEquals(listOf("En Trap : la cloche qui tourne.", "C'est le son qui fait le style : il joue dans toutes les sections."),
            com.pocketstudio.app.library.partNotes(signed, StarterWizard.Part.Lead))
    }
}
