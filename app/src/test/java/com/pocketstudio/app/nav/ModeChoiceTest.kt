package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.projects.ProjectStarter
import com.pocketstudio.app.projects.ProjectsActions
import com.pocketstudio.app.projects.ProjectsScreen
import com.pocketstudio.app.projects.ProjectsUi
import com.pocketstudio.app.projects.StarterGroup
import com.pocketstudio.app.settings.ProjectModes
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.app.ui.TopBar
import com.pocketstudio.app.ui.TopBarActions
import com.pocketstudio.app.ui.TopBarUi
import com.pocketstudio.app.ui.topBarLayout
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Owner (2026-10-29): « Simple ou Pro ? » is asked before every new project (none chosen for the
 * user, the last choice marked), kept with each project, shown in the top bar; « J'apprends »
 * takes the tutorial's level from it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w392dp-h852dp")
class ModeChoiceTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)

    @After
    fun back() = h.finish()

    private fun exists(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun tap(tag: String) {
        val node = compose.onNodeWithTag(tag)
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
        compose.waitForIdle()
    }

    private fun clearModes() = h.app.getSharedPreferences("project_modes", 0).edit().clear().commit()

    /** Review (MINEUR 2): the record of a project gone for good goes with it; the others and the last choice stay. */
    @Test
    fun theModeOfAProjectGoneForGoodIsForgotten() {
        clearModes()
        ProjectModes.set(h.app, "kept", true)
        ProjectModes.set(h.app, "gone", false)
        ProjectModes.choose(h.app, false)
        ProjectModes.dropMissing(h.app) { setOf("kept") }
        assertEquals(true, ProjectModes.of(h.app, "kept"))
        assertNull(ProjectModes.of(h.app, "gone"))
        assertEquals(false, ProjectModes.last(h.app))
        SettingsStore.forgetForTests()
    }

    @Test
    fun eachProjectKeepsItsOwnModeAndTheLastChoiceIsRemembered() {
        clearModes()
        SettingsStore.forgetForTests()
        assertNull(ProjectModes.of(h.app, "a"))
        assertNull(ProjectModes.last(h.app))
        ProjectModes.set(h.app, "a", true)
        ProjectModes.set(h.app, "b", false)
        assertEquals(true, ProjectModes.of(h.app, "a"))
        assertEquals(false, ProjectModes.of(h.app, "b"))
        // A copy keeps its mode; a project with none takes the one it is opened in.
        ProjectModes.copy(h.app, "a", "a2")
        assertEquals(true, ProjectModes.of(h.app, "a2"))
        var applied: Boolean? = null
        ProjectModes.follow(h.app, "c", shown = false) { applied = it }
        assertNull(applied)
        assertEquals(false, ProjectModes.of(h.app, "c"))
        // A project recorded otherwise is shown in its own mode.
        ProjectModes.follow(h.app, "a", shown = false) { applied = it }
        assertEquals(true, applied)
        // A choice: shown at once and suggested next time.
        ProjectModes.choose(h.app, true)
        assertTrue(SettingsStore.state.value.simpleMode)
        assertEquals(true, ProjectModes.last(h.app))
        ProjectModes.forget(h.app, "a")
        assertNull(ProjectModes.of(h.app, "a"))
    }

    /** Every way to start of « Nouveau projet » waits for the mode; Retour comes back with nothing made. */
    @Test
    fun noWayToStartSkipsTheModeAndItsBackMakesNothing() {
        clearModes()
        SettingsStore.forgetForTests()
        val done = mutableListOf<String>()
        val starters = listOf(
            ProjectStarter("Rythme de départ", "Un rythme", "starter", StarterGroup.Style),
            ProjectStarter("Projet vide", "Vierge", "empty", StarterGroup.Empty),
        )
        compose.setContent {
            PocketStudioTheme {
                ProjectsScreen(
                    ProjectsUi(loading = false), "p-0", starters,
                    ProjectsActions(
                        learn = { s, l -> done += "learn:$s:$l" }, wizard = { done += "wizard" }, create = { done += "create:$it" },
                        quickStyle = { done += "quick:$it" }, customStyle = { done += "custom:$it" },
                    ),
                    newOnly = true,
                )
            }
        }
        // No mark before any choice on this phone: nothing is put forward.
        tap("start_empty")
        assertFalse(exists("mode_mark"))
        fun techno() { if (!exists("quick_style_techno")) tap("quick_family_Techno") }
        val ways: List<Pair<String, () -> Unit>> = listOf(
            "wizard" to { tap("starter_wizard") },
            "create:1" to { tap("start_empty") },
            "create:0" to { tap("starter_starter") },
            "quick:techno" to { techno(); tap("quick_style_techno") },
            "custom:techno" to { techno(); tap("quick_custom_techno") },
        )
        tap("mode_step_back")
        // « Autres façons de commencer » open, and still open on the way back from the step.
        tap("start_others")
        ways.forEachIndexed { i, (what, open) ->
            open()
            compose.onNodeWithTag("mode_step").assertIsDisplayed()
            assertEquals("$what made before the mode", i, done.size)
            // Retour: back on the page, nothing made.
            tap("mode_step_back")
            assertFalse(exists("mode_step"))
            compose.onNodeWithTag("new_project").assertExists()
            assertEquals(i, done.size)
            open()
            tap(if (i % 2 == 0) "mode_card_simple" else "mode_card_pro")
            assertEquals(what, done.last())
            assertEquals(i % 2 == 0, SettingsStore.state.value.simpleMode)
        }
        // The last choice (Simple) is marked next time, and nothing is chosen for the user.
        tap("start_empty")
        compose.onNodeWithText("Ton dernier choix").assertExists()
        assertEquals(5, done.size)
    }

    /** « J'apprends »: Simple is the Facile tutorial, Pro the Pro one; the level is not asked again. */
    @Test
    fun learningTakesTheTutorialLevelFromTheMode() {
        clearModes()
        SettingsStore.forgetForTests()
        val learned = mutableListOf<Pair<String, TutoLevel>>()
        compose.setContent {
            PocketStudioTheme {
                ProjectsScreen(ProjectsUi(loading = false), "p-0", emptyList(), ProjectsActions(learn = { s, l -> learned += s to l }), newOnly = true)
            }
        }
        for ((card, level) in listOf("mode_card_simple" to TutoLevel.Facile, "mode_card_pro" to TutoLevel.Pro)) {
            tap("start_learn")
            compose.onNodeWithText(if (level == TutoLevel.Facile) "Avec le tuto Facile" else "Avec le tuto Pro").assertExists()
            tap(card)
            compose.onNodeWithTag("tuto_chooser").assertExists()
            assertFalse(exists("tuto_level_easy"))
            assertFalse(exists("tuto_level_pro"))
            // Retour from the styles: back on « Simple ou Pro ? », then on the page.
            tap("tuto_chooser_back")
            compose.onNodeWithTag("mode_step").assertExists()
            tap(card)
            tap("tuto_style_jump_up")
            assertEquals("jump_up" to level, learned.last())
        }
    }

    @Test
    fun theTopBarSaysTheModeAndATouchOpensItsChange() {
        var asked = 0
        var mode by androidx.compose.runtime.mutableStateOf(true)
        compose.setContent {
            PocketStudioTheme {
                TopBar(TopBarUi("Mon morceau", false, true, false, mode = mode), TopBarActions(mode = { asked++ }))
            }
        }
        compose.onNodeWithTag("top_mode_text", useUnmergedTree = true).assertTextEquals("Simple")
        tap("top_mode")
        assertEquals(1, asked)
        mode = false
        compose.waitForIdle()
        compose.onNodeWithTag("top_mode_text", useUnmergedTree = true).assertTextEquals("Pro")
        val chip = compose.onNodeWithTag("top_mode").fetchSemanticsNode().boundsInRoot
        val d = compose.density.density
        assertTrue("the chip is touchable", (chip.bottom - chip.top) / d >= 47.5f && (chip.right - chip.left) / d >= 47.5f)
    }

    /** The chip never pushes an essential out: in the row when it fits, else on the name's line. */
    @Test
    fun theTopBarMakesRoomForTheModeChip() {
        for (w in 280..900 step 4) for (scene in listOf(false, true)) for (simple in listOf(true, false)) {
            val l = topBarLayout(w.dp, scene, modeChip = com.pocketstudio.app.ui.modeChipRoom(simple))
            if (l.modeInRow) {
                assertTrue("$w dp: the name keeps its line in the row", l.projectName)
                if (scene) assertTrue("$w dp: the scene keeps its letter", l.scene != com.pocketstudio.app.ui.SceneChipMode.Hidden)
            } else {
                assertFalse("$w dp: chip on the name's line", l.projectName)
            }
        }
        // A usual phone (360 dp and more, the Mode simple's bar): in the row.
        assertTrue(topBarLayout(360.dp, false, modeChip = com.pocketstudio.app.ui.modeChipRoom(true)).modeInRow)
        // Pro on a part's screen (the chip « Pro », the part's letter): one row from 412 dp (most phones).
        assertTrue(topBarLayout(412.dp, true, modeChip = com.pocketstudio.app.ui.modeChipRoom(false)).modeInRow)
        // Without the chip, nothing changes.
        assertEquals(topBarLayout(412.dp, true), topBarLayout(412.dp, true, modeChip = null))
    }

    /**
     * In the app: a mode chosen for a new song that is not made after all (the assistant closed)
     * never sticks to the open project; the chip changes it, kept with the project.
     */
    @Test
    fun aModeChosenForASongNotMadeLeavesTheOpenProjectAsItWas() {
        h.start(simple = false)
        compose.onNodeWithTag("top_mode_text", useUnmergedTree = true).assertTextEquals("Pro")
        assertEquals(false, ProjectModes.of(h.app, h.vm.projectId))
        tap("top_projects")
        tap("project_new")
        tap("starter_wizard")
        tap("mode_card_simple")
        // The assistant speaks the Mode simple's words for the song to come.
        h.awaitTag("wizard")
        assertTrue(SettingsStore.state.value.simpleMode)
        tap("wizard_close")
        compose.waitForIdle()
        assertFalse(exists("wizard"))
        // « Mes projets » under it, then back on the open project: its own mode again.
        if (exists("projects")) tap("projects_back")
        assertFalse(exists("projects"))
        assertFalse(SettingsStore.state.value.simpleMode)
        assertEquals(false, ProjectModes.of(h.app, h.vm.projectId))
        compose.onNodeWithTag("tab_Structure").assertIsDisplayed()
        // The chip: Simple for this project, kept with it.
        tap("top_mode")
        tap("mode_card_simple")
        assertTrue(SettingsStore.state.value.simpleMode)
        assertEquals(true, ProjectModes.of(h.app, h.vm.projectId))
        compose.onNodeWithTag("top_mode_text", useUnmergedTree = true).assertTextEquals("Simple")
    }
}
