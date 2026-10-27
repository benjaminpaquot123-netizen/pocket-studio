package com.pocketstudio.app.projects

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Audit 16: opening another project clears the activity's ViewModels (the project's screens);
 * the app's own (the update check, the MIDI keyboard…) stay, not made again.
 */
@RunWith(RobolectricTestRunner::class)
class AppViewModelsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    class AppOwn : ViewModel() {
        var cleared = 0
        override fun onCleared() { cleared++ }
    }

    class ProjectOwn : ViewModel()

    @Test
    fun theAppsViewModelsOutliveAChangeOfProject() {
        val tick = mutableIntStateOf(0)
        val apps = mutableListOf<AppOwn>()
        val projects = mutableListOf<ProjectOwn>()
        compose.setContent {
            tick.intValue
            apps += appViewModel<AppOwn>()
            projects += viewModel<ProjectOwn>()
        }
        compose.waitForIdle()
        // What « Mes projets » does when another project opens (before the activity is rebuilt).
        compose.runOnUiThread { compose.activity.viewModelStore.clear() }
        tick.intValue++
        compose.waitForIdle()
        assertSame("the app's own kept", apps.first(), apps.last())
        assertEquals(0, apps.last().cleared)
        assertNotSame("the project's made again", projects.first(), projects.last())
    }
}
