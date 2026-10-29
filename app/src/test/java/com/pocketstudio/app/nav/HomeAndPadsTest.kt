package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.ui.studioCover
import com.pocketstudio.model.Project
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
import org.robolectric.annotation.GraphicsMode

/**
 * Benjamin's list (2026-10-29, APK 232): the pads of an empty project reopened, « Que veux-tu
 * faire ? » straight after the opening (no screen in between), and the way home from a project
 * (« Accueil », « ← » of « Mes projets », the system's Retour: the project, home, out).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp")
class HomeAndPadsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)

    @After
    fun end() = h.finish()

    private fun exists(tag: String) = compose.onAllNodes(androidx.compose.ui.test.hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    private fun tap(tag: String) {
        val node = compose.onNodeWithTag(tag)
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
        compose.waitForIdle()
    }

    private fun back() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    private fun emptyProject(): com.pocketstudio.app.rhythm.RhythmViewModel.() -> Unit = { session.edit { Project(name = "") } }

    // --- 1. The pads of an empty project -------------------------------------------------------

    /**
     * The cause: under « Mes projets » (and now « Que veux-tu faire ? ») the studio was not drawn
     * at all; the pads' own layers could then come back empty on the phone. It is drawn, covered.
     */
    @Test
    fun theStudioIsStillDrawnUnderItsCover() {
        var drawn = 0
        compose.setContent {
            Box(Modifier.size(40.dp).studioCover(true, Color.Black).testTag("covered")) {
                Box(Modifier.size(40.dp).drawBehind { drawn++; drawRect(Color.Red) })
            }
        }
        compose.waitForIdle()
        val image = compose.onNodeWithTag("covered").captureToImage().asAndroidBitmap()
        assertTrue("the content under the cover is drawn (its layers kept)", drawn > 0)
        assertEquals("but not seen", android.graphics.Color.BLACK, image.getPixel(image.width / 2, image.height / 2))
    }

    /** An empty project reopened (« Que veux-tu faire ? › Continuer »), Rythme: its 16 pads; again after « Mes projets ». */
    @Test
    fun anEmptyProjectReopenedShowsItsPads() {
        h.start(simple = true, launchChoice = true, build = emptyProject())
        tap("launch_continue")
        tap("tab_simple_Rythme")
        (0 until 16).forEach { compose.onNodeWithTag("pad_$it").assertExists() }
        compose.onNodeWithTag("pad_0").assertIsDisplayed()
        com.github.takahirom.roborazzi.captureScreenRoboImage("screenshots/nav/rythme_empty_project.png")
        // A kit chosen, « Mes projets », the open project touched: still there.
        h.vm.chooseKit("kitdnb:jump_up")
        tap("top_projects")
        h.awaitTag("project_${h.vm.projectId}")
        compose.onNodeWithTag("project_${h.vm.projectId}").performClick()
        compose.waitForIdle()
        assertFalse(exists("projects"))
        compose.onNodeWithTag("pad_0").assertIsDisplayed()
        compose.onNodeWithTag("pad_15").assertExists()
    }

    // --- 2. From the opening to the home screen ----------------------------------------------------

    /**
     * « Que veux-tu faire ? » is drawn in the app's own window, already there under the opening's
     * logo: as a window of its own it came a moment later, the project's last screen and a flash
     * showing in between.
     */
    @Test
    fun theHomeScreenIsNoWindowOfItsOwn() {
        h.start(simple = true, launchChoice = true)
        compose.onNodeWithTag("launch").assertIsDisplayed()
        assertTrue("no dialog window for it", compose.onAllNodes(isDialog()).fetchSemanticsNodes().isEmpty())
        // The studio under it is not read out (nor found).
        assertFalse(exists("tab_simple_Rythme"))
        tap("launch_continue")
        assertFalse(exists("launch"))
        compose.onNodeWithTag("tab_simple_Morceau").assertIsDisplayed()
    }

    /** At a start the home screen is shown under the opening (it no longer waits for its end). */
    @Test
    fun theHomeScreenIsUnderTheOpening() {
        com.pocketstudio.app.onboarding.Intro.playing = true
        try {
            h.start(simple = true, launchChoice = true)
            assertTrue(exists("launch"))
        } finally {
            com.pocketstudio.app.onboarding.Intro.playing = false
        }
    }

    // --- 4. The way home ----------------------------------------------------------------------------

    /** The system's Retour: whatever is open closes first, then the project, home, then out of the app. */
    @Test
    fun backGoesFromTheProjectHomeThenOut() {
        h.start(simple = true)
        // A page of the project first (« Ma voix »): Retour closes it.
        tap("top_more")
        tap("more_voice")
        back()
        compose.onNodeWithTag("tab_simple_Morceau").assertIsDisplayed()
        assertFalse(exists("launch"))
        // Then the project: home.
        back()
        compose.onNodeWithTag("launch").assertIsDisplayed()
        compose.onNodeWithTag("launch_remix").assertExists()
        assertFalse(h.vm.transport.value.playing)
        // Then out of the app (the home screen stays for when it comes back).
        back()
        assertTrue(exists("launch"))
        tap("launch_continue")
        assertFalse(exists("launch"))
        compose.onNodeWithTag("tab_simple_Morceau").assertIsDisplayed()
    }

    /** Mode simple: « ⋯ › Accueil », and the Remix under its own name there. */
    @Test
    fun theSimpleMenuLeadsHome() {
        h.start(simple = true)
        tap("top_more")
        compose.onNodeWithTag("more_remix").assertTextContains("Remix")
        tap("more_home")
        compose.onNodeWithTag("launch").assertIsDisplayed()
        tap("launch_continue")
        compose.onNodeWithTag("tab_simple_Morceau").assertIsDisplayed()
    }

    /** Mode Pro: « Plus › Accueil ». */
    @Test
    fun theProPlusPageLeadsHome() {
        h.start()
        tap("tab_Plus")
        tap("plus_home")
        compose.onNodeWithTag("launch").assertIsDisplayed()
    }

    /** « ← » of « Mes projets » opened from a project: home, not back on the project. */
    @Test
    fun myProjectsBackLeadsHome() {
        h.start(simple = true)
        val before = h.vm.project.value
        tap("top_projects")
        compose.onNodeWithTag("projects").assertIsDisplayed()
        tap("projects_back")
        assertFalse(exists("projects"))
        compose.onNodeWithTag("launch").assertIsDisplayed()
        tap("launch_continue")
        assertSame(before, h.vm.project.value)
    }
}
