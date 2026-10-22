package com.pocketstudio.app.ui

import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.pocketstudio.ui.Palettes
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A phone lying down with 3-button navigation: the navigation bar stands on the right (48 dp), the
 * camera hole on the left (32 dp). The top bar's buttons stay clear of both.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w860dp-h400dp-land-xxhdpi")
class TopBarInsetsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun playAndProjectsStayClearOfTheNavigationBarAndTheCameraHole() {
        compose.runOnUiThread { WindowCompat.setDecorFitsSystemWindows(compose.activity.window, false) }
        compose.setContent {
            PocketStudioTheme(Palettes.Studio) {
                Scaffold(topBar = {
                    TopBar(
                        TopBarUi("Mon premier rythme", 120.0, false, true, true, scene = SceneChipUi(0, listOf("A · Intro"))),
                        TopBarActions(),
                    )
                }) { padding -> Box(Modifier.padding(padding).fillMaxSize().testTag("content")) }
            }
        }
        val d = compose.activity.resources.displayMetrics.density
        compose.runOnUiThread {
            val insets = WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, (24 * d).toInt(), 0, 0))
                .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, (48 * d).toInt(), 0))
                .setInsets(WindowInsetsCompat.Type.displayCutout(), Insets.of((32 * d).toInt(), 0, 0, 0))
                .build()
            ViewCompat.dispatchApplyWindowInsets(compose.activity.findViewById<ViewGroup>(android.R.id.content), insets)
        }
        compose.waitForIdle()
        val root = compose.onRoot().getBoundsInRoot()
        val play = compose.onNodeWithTag("top_play").getBoundsInRoot()
        val projects = compose.onNodeWithTag("top_projects").getBoundsInRoot()
        val navigationStart = root.right.value - 48f
        assertTrue("play ends at ${play.right}, the navigation bar starts at $navigationStart dp", play.right.value <= navigationStart + 0.5f)
        assertTrue("projects starts at ${projects.left}, the camera hole ends at 32 dp", projects.left.value >= 32f - 0.5f)
        assertTrue("under the status bar", play.top.value >= 24f - 0.5f)
    }
}
