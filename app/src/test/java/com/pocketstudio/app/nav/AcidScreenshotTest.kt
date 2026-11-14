package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The Acid 303 where Benjamin looks for it (2026-10-29): the Mode simple's Clavier, its editor, Sons. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class AcidScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)

    @After
    fun end() = h.finish()

    private fun open(tag: String) {
        compose.onNodeWithTag(tag).performClick()
        compose.mainClock.advanceTimeBy(800)
        compose.waitForIdle()
    }

    @Test
    fun modeSimpleClavier() {
        h.start(simple = true)
        open("tab_simple_Clavier")
        compose.onRoot().captureRoboImage("screenshots/acid_simple_clavier.png")
    }

    @Test
    fun modeSimpleEditor() {
        h.start(simple = true)
        open("tab_simple_Clavier")
        open("sons_Acide")
        compose.onRoot().captureRoboImage("screenshots/acid_simple_editor.png")
    }

    @Test
    fun fullModeSons() {
        h.start()
        open("tab_Sons")
        open("sons_Acide")
        compose.onRoot().captureRoboImage("screenshots/acid_pro_sons.png")
    }
}
