package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import com.pocketstudio.model.i18n.tr
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Audit ultra round 6, U9 (seat E a #19 and #20, English, 130 %): Pad Live › Plus › « Effets » opened a box titled
 * « Effects · Remix ». The pads of Pad Live play through the strip the Remix also uses, and the title took that strip's
 * name: nothing said Pad Live, where the user was. Opened from Pad Live, the box is now titled « Effets · Pad Live ».
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-xhdpi")
class Tour6PadLiveEffectsTitleTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val h = AppHarness(compose)
    private val shell = Tour6Shell(compose)

    @After
    fun back() = h.finish()

    private fun shows(text: String) = compose.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun theEffectsOpenedFromPadLiveAreTitledPadLive() {
        // The « Live » project, continued from « Que veux-tu faire ? »: it goes on in Pad Live.
        h.start(launchChoice = true) { session.edit { com.pocketstudio.model.PadLive.newProject() } }
        h.awaitTag("launch_continue")
        shell.tap("launch_continue")
        assertTrue("Pad Live opens", shell.reached(30_000) { shell.has("padlive_more") })
        shell.idle(600)
        shell.tap("padlive_more")
        assertTrue("« Plus » of Pad Live", shell.reached(5_000) { shell.has("padlive_effects") })
        shell.tap("padlive_effects")
        assertTrue("the effects box opens", shell.reached(5_000) { shell.has("fx_close") })
        val remix = tr("Effets · %s", tr("Remix"))
        val padLive = tr("Effets · %s", tr("Pad Live"))
        assertFalse("opened from Pad Live, the effects box is titled « $remix »", shows(remix))
        assertTrue("the effects box does not say « $padLive »", shows(padLive))
    }
}
