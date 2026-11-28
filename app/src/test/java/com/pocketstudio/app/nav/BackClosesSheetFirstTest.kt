package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import com.pocketstudio.model.AutoTarget
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 9, F131: Android's Retour closes the sheet or the editor open first; it leaves the project
 * (« Que veux-tu faire ? ») only last. Testers: from the « Automatiser » curve, Retour went home.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class BackClosesSheetFirstTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val h = AppHarness(compose)

    @After
    fun end() = h.finish()

    private fun shown(tag: String) = compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    private fun back() {
        androidx.test.espresso.Espresso.pressBackUnconditionally()
        h.settle()
    }

    @Test
    fun retourClosesTheAutomateSheetBeforeLeavingTheProject() {
        h.start(simple = true, launchChoice = true)
        h.awaitTag("launch_page")
        compose.onNode(hasTestTag("launch_continue")).performClick()
        h.settle()
        assertFalse("still home after « Continuer »", shown("launch_page"))
        compose.runOnIdle { h.vm.automateKnob(AutoTarget.masterVolume()) }
        h.awaitTag("automate_sheet")

        back()
        assertFalse("Retour left the project with the curve open", shown("launch_page"))
        assertFalse("Retour did not close the curve", shown("automate_sheet"))
        assertNull(h.vm.automateTarget.value)

        // Nothing left over the studio: now Retour goes home.
        back()
        assertTrue("the last Retour goes home", shown("launch_page"))
    }

}
