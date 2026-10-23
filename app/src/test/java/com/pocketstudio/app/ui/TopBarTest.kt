package com.pocketstudio.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import com.pocketstudio.model.renameScene
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.report.forUser
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w480dp-h900dp")
class TopBarTest {
    @get:Rule
    val compose = createComposeRule()

    private val scenes = SceneChipUi(current = 1, titles = listOf("A · Intro", "B · Refrain", "C"), empty = setOf(2))

    private fun show(width: Int, scene: SceneChipUi? = null, actions: TopBarActions = TopBarActions()) = compose.setContent {
        PocketStudioTheme {
            Box(Modifier.width(width.dp)) {
                TopBar(
                    TopBarUi(projectName = "Mon morceau de dubstep", playing = false, canUndo = true, canRedo = true, scene = scene),
                    actions,
                )
            }
        }
    }

    /** The essentials are there and fit: projects (touchable), undo, record, a round play and share inside the bar. */
    private fun assertEssentials(width: Int) {
        val projects = compose.onNodeWithTag("top_projects").getBoundsInRoot()
        assertTrue("projects ${projects.right - projects.left}", projects.right - projects.left >= 48.dp)
        compose.onNodeWithTag("top_undo").assertIsDisplayed()
        compose.onNodeWithTag("top_record").assertIsDisplayed()
        val play = compose.onNodeWithTag("top_play").getBoundsInRoot()
        assertEquals((play.right - play.left).value, (play.bottom - play.top).value, 0.5f)
        assertTrue("play ends at ${play.right}", play.right <= width.dp + 0.5.dp)
        compose.onNodeWithTag("top_share").assertIsDisplayed()
        val share = compose.onNodeWithTag("top_share").getBoundsInRoot()
        assertTrue("share ends at ${share.right}", share.right <= width.dp + 0.5.dp && share.left >= play.right - 0.5.dp)
        val scene = compose.onNodeWithTag("top_scene").getBoundsInRoot()
        assertTrue("the chip stays left of undo", scene.right <= compose.onNodeWithTag("top_undo").getBoundsInRoot().left + 0.5.dp)
        assertTrue("the chip is touchable", scene.bottom - scene.top >= 36.dp && scene.right - scene.left >= 48.dp)
    }

    @Test
    fun aVeryNarrowPhoneShowsTheSceneLetterOnly() {
        show(304, scenes)
        assertEssentials(304)
        compose.onNodeWithTag("top_scene_text", useUnmergedTree = true).assertTextEquals("B")
        compose.onNodeWithTag("top_redo").assertDoesNotExist()
    }

    @Test
    fun aLargePhoneShowsTheSceneNameAndKeepsTheProjectNameReadable() {
        // ⤴ Partager took 48 dp of the bar: the section's name shows from about 444 dp.
        show(448, scenes)
        assertEssentials(448)
        compose.onNodeWithTag("top_scene_text", useUnmergedTree = true).assertTextEquals("B · Refrain")
        val projects = compose.onNodeWithTag("top_projects").getBoundsInRoot()
        assertTrue("projects ${projects.right - projects.left}", projects.right - projects.left >= 120.dp)
    }

    @Test
    @Config(qualifiers = "w600dp-h900dp")
    fun aSmallTabletAlsoGetsRedo() {
        show(528, scenes)
        assertEssentials(528)
        compose.onNodeWithTag("top_scene_text", useUnmergedTree = true).assertTextEquals("B · Refrain")
        compose.onNodeWithTag("top_redo").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w700dp-h900dp")
    fun aWideScreenShowsEverythingWithTheScene() {
        show(700, scenes)
        assertEssentials(700)
        compose.onNodeWithTag("top_scene_text", useUnmergedTree = true).assertTextEquals("B · Refrain")
        compose.onNodeWithTag("top_redo").assertIsDisplayed()
        // The tempo is in the play strip under the bar, on every width.
        compose.onNodeWithText("140").assertDoesNotExist()
    }

    @Test
    fun aTouchOnTheNameOpensTheProjectsAndAHoldRenamesIt() {
        var opened = 0
        var renamed = 0
        show(412, scenes, TopBarActions(projects = { opened++ }, rename = { renamed++ }))
        compose.onNodeWithTag("top_projects").performClick()
        compose.onNodeWithTag("top_projects").performTouchInput { longClick() }
        assertEquals(1, opened)
        assertEquals(1, renamed)
    }

    @Test
    fun theLayoutGivesUpExtrasBeforeTheEssentials() {
        // Every width from a tiny phone to a tablet: what is shown never overflows the bar.
        for (w in 280..900 step 4) {
            val l = topBarLayout(w.dp, hasScene = true)
            // Undo, record, play, share; the projects button keeps a bit of the name (only its folder below 348 dp).
            var used = 48.dp * 4 + 12.dp + if (w >= 348) 92.dp else 48.dp
            if (l.showRedo || l.scene == SceneChipMode.Full) used += 36.dp  // the name's reserve
            // Without the scene: 388 dp for redo (the tempo is in the play strip below).
            val plain = topBarLayout(w.dp, hasScene = false)
            assertEquals(w >= 388, plain.showRedo)
            if (l.showRedo) used += 48.dp
            if (l.scene != SceneChipMode.Hidden) used += l.sceneMaxWidth
            assertTrue("$w dp: $l", used <= w.dp)
            // The name goes only where the folder alone makes room for the scene's letter (never a clipped "…").
            assertEquals("$w dp: $l", w !in 304 until 348, l.projectName)
            assertTrue("$w dp: $l", plain.projectName)
            if (w >= 304) assertTrue("$w dp: the scene letter at least", l.scene != SceneChipMode.Hidden)
        }
        assertEquals(SceneChipMode.Letter, topBarLayout(304.dp, true).scene)
        assertEquals(SceneChipMode.Letter, topBarLayout(412.dp, true).scene)
        assertEquals(SceneChipMode.Full, topBarLayout(448.dp, true).scene)
        assertTrue(topBarLayout(700.dp, true).showRedo)
        // Without a scene (Morceau, Outils): redo comes back sooner.
        assertEquals(SceneChipMode.Hidden, topBarLayout(412.dp, false).scene)
        assertTrue(topBarLayout(412.dp, false).showRedo)
        assertTrue(!topBarLayout(300.dp, false).showRedo)
    }

    @Test
    fun theSceneChipListsTheScenesAndPicksOne() {
        var picked = -1
        var fresh = 0
        show(412, scenes, TopBarActions(selectScene = { picked = it }, newScene = { fresh++ }))
        compose.onNodeWithTag("top_scene").performClick()
        compose.onNodeWithTag("scene_menu").assertIsDisplayed()
        compose.onNodeWithText("A · Intro").assertIsDisplayed()
        compose.onNodeWithText("C (vide)").assertIsDisplayed()
        compose.onNodeWithTag("scene_pick_0").performClick()
        assertEquals(0, picked)
        compose.onNodeWithTag("scene_menu").assertDoesNotExist()
        // The scene already edited: nothing to do. « Nouvelle scène vide »: a fresh one.
        picked = -1
        compose.onNodeWithTag("top_scene").performClick()
        compose.onNodeWithTag("scene_pick_1").performClick()
        assertEquals(-1, picked)
        compose.onNodeWithTag("top_scene").performClick()
        compose.onNodeWithTag("scene_new").performClick()
        assertEquals(1, fresh)
    }

    @Test
    fun theChipFollowsTheProject() {
        // Titles from the model: section names when given, the letter otherwise; current one marked.
        var p = com.pocketstudio.model.Project()
        p = p.renameScene(0, "Intro")
        val chip = sceneChip(p)
        assertEquals(0, chip.current)
        assertEquals("A · Intro", chip.titles[0])
        assertTrue(0 in chip.empty)
    }

    @Test
    fun aNarrowPhoneKeepsTheProjectsButtonAndARoundPlayButton() {
        show(300)
        // The projects button is still there and wide enough to touch; play keeps its round shape.
        compose.onNodeWithTag("top_projects").assertIsDisplayed()
        val projects = compose.onNodeWithTag("top_projects").getBoundsInRoot()
        assertTrue("projects ${projects.right - projects.left}", projects.right - projects.left >= 48.dp)
        val play = compose.onNodeWithTag("top_play").getBoundsInRoot()
        assertEquals((play.right - play.left).value, (play.bottom - play.top).value, 0.5f)
        assertTrue(play.right <= 300.dp)
        compose.onNodeWithTag("top_redo").assertDoesNotExist()
    }

    @Test
    fun aWidePhoneShowsEverything() {
        show(420)
        compose.onNodeWithTag("top_redo").assertIsDisplayed()
        compose.onNodeWithTag("top_play").assertIsDisplayed()
    }

    @Test
    fun networkErrorsReadInFrench() {
        assertEquals("pas de connexion internet", java.net.UnknownHostException("Unable to resolve host").forUser())
        assertEquals("la connexion est trop lente ou a coupé", java.net.SocketTimeoutException("timeout").forUser())
        assertEquals("autre chose", IllegalStateException("autre chose").forUser())
    }
}
