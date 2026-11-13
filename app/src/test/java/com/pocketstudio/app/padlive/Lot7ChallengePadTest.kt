package com.pocketstudio.app.padlive

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.LivePads
import com.pocketstudio.model.PadChallenge
import com.pocketstudio.model.PadLive
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 6 lot 7 (« Boum boum », a candidate of the bench, confirmed by lot 6): the challenge never named the pad to
 * tap; only the colour of the rhythm's dots hinted at it. Before and during the try, the panel names it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-port-hdpi")
class Lot7ChallengePadTest {
    @get:Rule
    val compose = createComposeRule()

    private val pads: LivePads = PadLive.newProject().live.withAt(8, LiveClip("h.wav", "Klaxon", 1, oneShot = true)).withAt(9, LiveClip("l.wav", "Laser", 1, oneShot = true))

    private var shown by mutableStateOf<ChallengeUi>(ChallengeUi.Off)
    private var set = false

    private fun said(ui: ChallengeUi): String {
        if (!set) {
            compose.setContent { PocketStudioTheme { ChallengePanel(shown, pads, ChallengeActions()) } }
            set = true
        }
        compose.runOnIdle { shown = ui }
        compose.waitForIdle()
        return compose.onAllNodesWithTag("padlive_challenge_pads", useUnmergedTree = true).fetchSemanticsNodes()
            .firstOrNull()?.config?.getOrNull(SemanticsProperties.Text)?.joinToString().orEmpty()
    }

    @Test
    fun boumBoumNamesThePadToTapBeforeAndDuringTheTry() {
        val c = PadChallenge.make(1, PadChallenge.shotsOf(pads, 0), 100.0)!!
        val pad = c.hits.map { it.pad }.distinct().single()
        val name = padName(pad)
        for (ui in listOf(ChallengeUi.Ready(c), ChallengeUi.YourTurn(c, 0))) {
            val line = said(ui)
            assertTrue("« $line » names pad $name", line.contains(name))
        }
    }

    @Test
    fun twoPadsAreBothNamed() {
        val c = PadChallenge.make(2, PadChallenge.shotsOf(pads, 0), 100.0)!!
        val names = c.hits.map { it.pad }.distinct().map { padName(it) }
        val line = said(ChallengeUi.Ready(c))
        assertTrue("« $line » names $names", names.size > 1 && names.all { line.contains(it) })
    }

    /** The pad's sound named as the pads show it: in the interface's language (« Klaxon » is « Air horn » in English). */
    @Test
    fun thePadsSoundIsSaidInTheInterfacesLanguage() {
        val c = PadChallenge.make(1, PadChallenge.shotsOf(pads, 0), 100.0)!!
        com.pocketstudio.model.i18n.I18n.lang = com.pocketstudio.model.i18n.Lang.EN
        try {
            val line = said(ChallengeUi.Ready(c))
            assertTrue("« $line » says the sound in English", line.contains("Air horn") && !line.contains("Klaxon"))
        } finally {
            com.pocketstudio.model.i18n.I18n.lang = com.pocketstudio.model.i18n.Lang.FR
        }
    }
}
