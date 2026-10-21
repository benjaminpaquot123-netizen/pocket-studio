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
                    TopBarUi(projectName = "Mon morceau de dubstep", tempoBpm = 140.0, playing = false, canUndo = true, canRedo = true, scene = scene),
                    actions,
                )
            }
        }
    }

    /** The essentials are there and fit: projects (touchable), undo, record, a round play inside the bar. */
    private fun assertEssentials(width: Int) {
        val projects = compose.onNodeWithTag("top_projects").getBoundsInRoot()
        assertTrue("projects ${projects.right - projects.left}", projects.right - projects.left >= 48.dp)
        compose.onNodeWithTag("top_undo").assertIsDisplayed()
        compose.onNodeWithTag("top_record").assertIsDisplayed()
        val play = compose.onNodeWithTag("top_play").getBoundsInRoot()
        assertEquals((play.right - play.left).value, (play.bottom - play.top).value, 0.5f)
        assertTrue("play ends at ${play.right}", play.right <= width.dp + 0.5.dp)
        val scene = compose.onNodeWithTag("top_scene").getBoundsInRoot()
        assertTrue("the chip stays left of undo", scene.right <= compose.onNodeWithTag("top_undo").getBoundsInRoot().left + 0.5.dp)
        assertTrue("the chip is touchable", scene.bottom - scene.top >= 36.dp && scene.right - scene.left >= 48.dp)
    }

    @Test
    fun aVeryNarrowPhoneShowsTheSceneLetterOnly() {
        show(300, scenes)
        assertEssentials(300)
        compose.onNodeWithTag("top_scene_text", useUnmergedTree = true).assertTextEquals("B")
        compose.onNodeWithTag("top_redo").assertDoesNotExist()
    }

    @Test
    fun aUsualPhoneShowsTheSceneNameAndKeepsTheProjectNameReadable() {
        show(412, scenes)
        assertEssentials(412)
        compose.onNodeWithTag("top_scene_text", useUnmergedTree = true).assertTextEquals("B · Refrain")
        val projects = compose.onNodeWithTag("top_projects").getBoundsInRoot()
        assertTrue("projects ${projects.right - projects.left}", projects.right - projects.left >= 120.dp)
    }

    @Test
    fun aBigPhoneAlsoGetsRedo() {
        show(480, scenes)
        assertEssentials(480)
        compose.onNodeWithTag("top_scene_text", useUnmergedTree = true).assertTextEquals("B · Refrain")
        compose.onNodeWithTag("top_redo").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w700dp-h900dp")
    fun aWideScreenShowsEverythingWithTheScene() {
        show(700, scenes)
        assertEssentials(700)
        compose.onNodeWithTag("top_scene_text", useUnmergedTree = true).assertTextEquals("B · Refrain")
        compose.onNodeWithText("140").assertIsDisplayed()
    }

    @Test
    fun theLayoutGivesUpExtrasBeforeTheEssentials() {
        // Every width from a tiny phone to a tablet: what is shown never overflows the bar.
        for (w in 280..900 step 4) {
            val l = topBarLayout(w.dp, hasScene = true)
            var used = 48.dp * 3 + 12.dp + 92.dp
            if (l.showRedo || l.showTempo || l.scene == SceneChipMode.Full) used += 36.dp  // the name's reserve
            // Without the scene, the old widths: 340 dp for redo, 400 dp for the tempo.
            val plain = topBarLayout(w.dp, hasScene = false)
            assertEquals(w >= 340, plain.showRedo)
            assertEquals(w >= 400, plain.showTempo)
            if (l.showRedo) used += 48.dp
            if (l.showTempo) used += 84.dp
            if (l.scene != SceneChipMode.Hidden) used += l.sceneMaxWidth
            assertTrue("$w dp: $l", used <= w.dp)
            if (w >= 300) assertTrue("$w dp: the scene letter at least", l.scene != SceneChipMode.Hidden)
        }
        assertEquals(SceneChipMode.Letter, topBarLayout(300.dp, true).scene)
        assertEquals(SceneChipMode.Full, topBarLayout(412.dp, true).scene)
        assertTrue(topBarLayout(700.dp, true).showTempo)
        // Without a scene (Morceau, Outils), the bar is as before: redo and the tempo come back sooner.
        assertEquals(SceneChipMode.Hidden, topBarLayout(412.dp, false).scene)
        assertTrue(topBarLayout(412.dp, false).showTempo)
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
