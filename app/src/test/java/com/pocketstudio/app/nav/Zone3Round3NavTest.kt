package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.pocketstudio.app.projects.CurrentProject
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Audit ultra round 3, zone 3: the whole app (AppRoot), without the bench. */
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp")
class Zone3Round3NavTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)

    @After
    fun back() = h.finish()

    private fun tap(tag: String) {
        compose.onAllNodesWithTag(tag)[0].performClick()
        compose.waitForIdle()
    }

    /**
     * n° 18 (seat D a, écrans 39-46): the open project, the only one, deleted from « Mes projets »: its « ← » sent the app
     * behind (« rien ne bouge », the seat stuck). It leads to « Que veux-tu faire ? » (its no-project state) instead.
     */
    @Test
    fun backAfterDeletingTheOpenProjectLeadsHome() {
        // Where the seat was: the open project deleted, the app rebuilt on no project, « Mes projets » on its empty state.
        h.start(noProject = true, build = {})
        h.awaitTag("projects_empty")
        tap("projects_back")
        h.awaitTag("launch_no_project")
        assertFalse("not sent behind", shadowOf(compose.activity).isTaskMovedToBack)
        assertTrue(compose.onAllNodesWithTag("projects_empty").fetchSemanticsNodes().isEmpty())
    }

    /**
     * n° 21 (seat D b, phone lying down, Mode simple): the rail of the five spaces was cut (« Mix » half there,
     * « Envoyer » out of sight until scrolled). Lying down on a phone (with the feedback bar), the five fit whole.
     */
    @Test
    @Config(qualifiers = "w800dp-h360dp-land-hdpi")
    fun lyingDownTheFiveSpacesFitInTheRail() {
        h.start(simple = true)
        h.awaitTag("space_rail")
        val rail = compose.onNodeWithTag("space_rail").fetchSemanticsNode().boundsInRoot
        for (t in listOf("Morceau", "Rythme", "Clavier", "Mix", "Envoyer")) {
            val b = compose.onNodeWithTag("tab_simple_$t").fetchSemanticsNode().boundsInRoot
            assertTrue("$t whole in the rail: $b in $rail", b.top >= rail.top - 1 && b.bottom <= rail.bottom + 1)
        }
        compose.onRoot().captureRoboImage("screenshots/nav/phone_lying_rail_simple.png")
    }
}
