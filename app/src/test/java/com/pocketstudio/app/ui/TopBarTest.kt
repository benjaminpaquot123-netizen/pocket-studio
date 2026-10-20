package com.pocketstudio.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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

    private fun show(width: Int) = compose.setContent {
        PocketStudioTheme {
            Box(Modifier.width(width.dp)) {
                TopBar(TopBarUi(projectName = "Mon morceau de dubstep", tempoBpm = 140.0, playing = false, canUndo = true, canRedo = true), TopBarActions())
            }
        }
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
