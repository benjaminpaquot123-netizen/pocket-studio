package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pocketstudio.app.ui.AppTab
import com.pocketstudio.app.ui.OpeningTab
import com.pocketstudio.model.Project
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Benjamin's list n° 5 (APK 233, his video: « Continuer › Mon projet 3 », empty, Mode simple, opened
 * on « Mon morceau »). Choice 1A: an empty project opens on the pads, a song on « Mon morceau »,
 * in both modes, from « Continuer », « Mes projets », « Nouveau projet » and an import.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp")
class OpenOnPadsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)

    @After
    fun end() = h.finish()

    private val empty: com.pocketstudio.app.rhythm.RhythmViewModel.() -> Unit = { session.edit { Project(name = "") } }

    private fun tap(tag: String) {
        compose.onNodeWithTag(tag).performClick()
        compose.waitForIdle()
    }

    @Test
    fun anEmptyProjectContinuedInTheModeSimpleOpensOnItsPads() {
        h.start(simple = true, launchChoice = true, build = empty)
        tap("launch_continue")
        compose.onNodeWithTag("tab_simple_Rythme").assertIsSelected()
        compose.onNodeWithTag("pad_0").assertIsDisplayed()
        com.github.takahirom.roborazzi.captureScreenRoboImage("screenshots/nav/open_empty_simple.png")
    }

    @Test
    fun anEmptyProjectContinuedInProOpensOnItsPads() {
        h.start(simple = false, launchChoice = true, build = empty)
        tap("launch_continue")
        compose.onNodeWithTag("pad_0").assertIsDisplayed()
    }

    @Test
    fun anEmptyProjectOpenedStraightOpensOnItsPads() {
        h.start(simple = true, build = empty)
        compose.onNodeWithTag("tab_simple_Rythme").assertIsSelected()
        compose.onNodeWithTag("pad_0").assertIsDisplayed()
    }

    @Test
    fun aSongContinuedInTheModeSimpleOpensOnMonMorceau() {
        h.start(simple = true, launchChoice = true)
        tap("launch_continue")
        compose.onNodeWithTag("tab_simple_Morceau").assertIsSelected()
    }

    @Test
    fun aSongContinuedInProOpensOnItsTimeline() {
        h.start(simple = false, launchChoice = true)
        tap("launch_continue")
        compose.onNodeWithTag("timeline_screen").assertIsDisplayed()
    }

    /** Back home from another screen, « Continuer » opens it again as a start does. */
    @Test
    fun continuerFromHomeOpensAnEmptyProjectOnItsPadsAgain() {
        h.start(simple = true, launchChoice = true, build = empty)
        tap("launch_continue")
        tap("tab_simple_Morceau")
        compose.onNodeWithTag("tab_simple_Morceau").assertIsSelected()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        compose.onNodeWithTag("launch").assertIsDisplayed()
        tap("launch_continue")
        compose.onNodeWithTag("pad_0").assertIsDisplayed()
    }

    /**
     * Another project opened (« Mes projets », « Nouveau projet », a project imported then opened):
     * the activity is rebuilt with the screen of the project left; the new one opens by its own content.
     */
    @Test
    fun anotherProjectOpensByItsOwnContent() {
        val song = com.pocketstudio.model.Demos.ALL.first().third()
        assertEquals("an empty project after a song", AppTab.Rythme, OpeningTab.afterRebuild(AppTab.Morceau, "a", "b", asked = false, p = Project(name = "")))
        assertEquals("a song after an empty project", AppTab.Morceau, OpeningTab.afterRebuild(AppTab.Rythme, "a", "b", asked = false, p = song))
        assertEquals("a song after the mixer", AppTab.Morceau, OpeningTab.afterRebuild(AppTab.Mixeur, "a", "b", asked = false, p = song))
        assertEquals("a rotation keeps the screen", AppTab.Mixeur, OpeningTab.afterRebuild(AppTab.Mixeur, "a", "a", asked = false, p = Project(name = "")))
        assertEquals("the Remix asked for keeps it", AppTab.Audio, OpeningTab.afterRebuild(AppTab.Audio, "a", "b", asked = true, p = Project(name = "")))
        assertTrue(com.pocketstudio.model.OpenOn.song(song))
    }
}
