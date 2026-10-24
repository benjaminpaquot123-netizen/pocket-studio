package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.Lifecycle
import com.pocketstudio.app.learn.MissionPrefs
import com.pocketstudio.app.onboarding.LaunchPrefs
import com.pocketstudio.app.onboarding.LaunchRule
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.app.ui.withinGestures
import com.pocketstudio.model.DefaultNames
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * « Que veux-tu faire ? » in the whole app: at each start (not the first, not after a rotation or
 * a short trip away), nothing changed nor playing until a choice, and each choice where it says.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class LaunchFlowTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)

    @After
    fun back() = h.finish()

    private fun exists(tag: String) = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    private fun tap(tag: String) {
        val node = compose.onNodeWithTag(tag)
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
        compose.waitForIdle()
    }

    @Test
    fun theAppOpensOnTheChoiceAndContinueIsOneTouchThatChangesNothing() {
        h.start(launchChoice = true)
        val before = h.vm.project.value
        compose.onNodeWithTag("launch").assertIsDisplayed()
        compose.onNodeWithTag("launch_name", useUnmergedTree = true).assertTextEquals(DefaultNames.shown(before.name))
        // When it was last changed, from its last save.
        h.awaitTag("launch_edited")
        compose.onNodeWithTag("launch_edited", useUnmergedTree = true).assertTextEquals("modifié à l'instant")
        // Nothing plays, nothing changed, the project not shown yet.
        assertFalse(h.vm.transport.value.playing)
        compose.withinGestures("Continuer après l'ouverture", 1) { tap("launch_continue") }
        assertFalse(exists("launch"))
        compose.onNodeWithTag("timeline_screen").assertIsDisplayed()
        assertSame(before, h.vm.project.value)
        assertFalse(h.vm.transport.value.playing)
    }

    @Test
    fun theFirstLaunchKeepsItsWelcomeAndDoesNotAskTwice() {
        h.start(firstLaunch = true)
        compose.onNodeWithTag("welcome").assertExists()
        assertFalse(exists("launch"))
        compose.onNodeWithTag("welcome_later").performScrollTo().performClick()
        compose.waitForIdle()
        assertFalse(exists("launch"))
        assertTrue(SettingsStore.state.value.guideSeen)
    }

    @Test
    fun aRotationOrAProcessRestoredSoonAfterKeepsTheChoice() {
        val restore = StateRestorationTester(compose)
        h.start(launchChoice = true, restore = restore)
        // Not chosen yet: still there after the rebuild, and only once.
        restore.emulateSavedInstanceStateRestore()
        compose.waitForIdle()
        assertEquals(1, compose.onAllNodesWithTag("launch").fetchSemanticsNodes().size)
        tap("launch_continue")
        // Chosen: a rotation (or the process restored a few minutes later) goes back to the project.
        LaunchPrefs(h.app).leftAt = System.currentTimeMillis() - 5 * 60_000L
        restore.emulateSavedInstanceStateRestore()
        compose.waitForIdle()
        assertFalse(exists("launch"))
        compose.onNodeWithTag("timeline_screen").assertIsDisplayed()
    }

    @Test
    fun aProcessRestoredAfterALongTimeAsksAgain() {
        val restore = StateRestorationTester(compose)
        h.start(launchChoice = true, restore = restore)
        tap("launch_continue")
        LaunchPrefs(h.app).leftAt = System.currentTimeMillis() - LaunchRule.AWAY_MS - 60_000L
        restore.emulateSavedInstanceStateRestore()
        compose.waitForIdle()
        assertEquals(1, compose.onAllNodesWithTag("launch").fetchSemanticsNodes().size)
    }

    @Test
    fun backFromTheBackgroundAsksAgainOnlyAfterHalfAnHour() {
        h.start()
        assertFalse(exists("launch"))
        val scenario = compose.activityRule.scenario
        // A short trip to another app.
        scenario.moveToState(Lifecycle.State.CREATED)
        assertTrue(System.currentTimeMillis() - LaunchPrefs(h.app).leftAt < 10_000)
        scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitForIdle()
        assertFalse(exists("launch"))
        // Away for 40 minutes.
        scenario.moveToState(Lifecycle.State.CREATED)
        LaunchPrefs(h.app).leftAt = System.currentTimeMillis() - 40 * 60_000L
        scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitForIdle()
        compose.onNodeWithTag("launch").assertIsDisplayed()
    }

    @Test
    fun newSongOpensTheNewProjectPageAndItsBackComesBackHere() {
        h.start(launchChoice = true)
        val before = h.vm.project.value
        tap("launch_new")
        assertFalse(exists("launch"))
        compose.onNodeWithTag("new_project").assertIsDisplayed()
        compose.onNodeWithTag("starter_wizard").assertExists()
        tap("new_project_back")
        compose.onNodeWithTag("launch").assertIsDisplayed()
        assertFalse(exists("new_project"))
        assertSame(before, h.vm.project.value)
    }

    @Test
    fun myProjectsListsThemAndItsBackComesBackHere() {
        h.start(launchChoice = true)
        tap("launch_projects")
        assertFalse(exists("launch"))
        compose.onNodeWithTag("projects").assertIsDisplayed()
        assertFalse(exists("new_project"))
        tap("projects_back")
        compose.onNodeWithTag("launch").assertIsDisplayed()
        assertFalse(exists("projects"))
        // The open project touched in the list: on it, as « Continuer ».
        tap("launch_projects")
        val card = "project_${h.vm.projectId}"
        h.awaitTag(card)
        tap(card)
        assertFalse(exists("projects"))
        assertFalse(exists("launch"))
        compose.onNodeWithTag("timeline_screen").assertIsDisplayed()
    }

    @Test
    fun missionsStartTheGuide() {
        h.start(launchChoice = true)
        tap("launch_missions")
        assertFalse(exists("launch"))
        assertTrue(MissionPrefs(h.app).active)
        compose.onNodeWithTag("mission_overlay").assertExists()
    }

    @Test
    fun theModeChangesThereAndTheChoiceStays() {
        h.start(simple = true, launchChoice = true)
        val before = h.vm.project.value
        compose.onNodeWithTag("launch_mode_simple").assertIsSelected()
        tap("launch_mode_full")
        assertFalse(SettingsStore.state.value.simpleMode)
        compose.onNodeWithTag("launch_mode_full").assertIsSelected()
        compose.onNodeWithTag("launch").assertIsDisplayed()
        tap("launch_mode_simple")
        assertTrue(SettingsStore.state.value.simpleMode)
        // The Mode simple's project behind « Continuer ».
        tap("launch_continue")
        compose.onNodeWithTag("tab_simple_Morceau").assertIsDisplayed()
        assertSame(before, h.vm.project.value)
    }

    /**
     * Lot 1: no project at all (the last one deleted). « Que veux-tu faire ? » has no « Continuer »,
     * « Mes projets » shows its empty state, and its Retour comes back here, never to a studio
     * on nothing.
     */
    @Test
    fun withNoProjectThereIsNothingToContinueAndTheListOffersANewSong() {
        h.start(launchChoice = true, noProject = true, build = {})
        compose.onNodeWithTag("launch").assertIsDisplayed()
        assertFalse(exists("launch_continue"))
        compose.onNodeWithTag("launch_no_project").assertExists()
        tap("launch_projects")
        h.awaitTag("projects_empty")
        compose.onNodeWithTag("projects_empty_new").assertExists()
        tap("projects_back")
        compose.onNodeWithTag("launch").assertIsDisplayed()
        // « Nouveau morceau »: the page of styles; its Retour back here too.
        tap("launch_new")
        compose.onNodeWithTag("new_project").assertIsDisplayed()
        tap("new_project_back")
        compose.onNodeWithTag("launch").assertIsDisplayed()
    }

    @Test
    fun withNoProjectTheAppOpensOnTheEmptyList() {
        h.start(noProject = true, build = {})
        assertFalse(exists("launch"))
        h.awaitTag("projects_empty")
        compose.onNodeWithTag("projects").assertIsDisplayed()
    }
}
