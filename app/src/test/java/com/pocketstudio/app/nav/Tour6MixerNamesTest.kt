package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.tuto.PrefsTutoStore
import com.pocketstudio.app.tuto.TutoTestHooks
import com.pocketstudio.model.DefaultNames
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.model.tuto.TutoTarget
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Audit ultra round 6, U27 (seat B b #72 and #73, the Pro tutorial on a tablet): Sons showed « Drums, Acid 303, Vocals,
 * Remix », the Mix « Drums, Synth, Remix »: « Acid 303 renommée, Vocals absente ». During a tutorial the Mix left the
 * Acid 303's strip out while it was silent, so the track the Sons screen names first had no strip, and the tester took
 * « Synth » for it. The Mix now has a strip for every track Sons shows, under the same name, tutorial or not.
 * (« Vocals » is the microphone's page: it has a strip once a take is recorded; saying so on the Mix needs words in
 * MixerScreen.kt, another lot's file.)
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-xhdpi")
class Tour6MixerNamesTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val h = AppHarness(compose)
    private val shell = Tour6Shell(compose)

    private val ears = object : GuideListenPlayer {
        override fun play(pcm: Pcm, ended: () -> Unit): Boolean = true
        override fun stop() = Unit
    }

    @After
    fun back() {
        TutoTestHooks.kitOffer = true
        TutoTestHooks.player = null
        h.finish()
    }

    private fun shows(text: String) = compose.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun duringTheProTutorialTheMixNamesTheAcidAsSonsDoes() {
        val target = TutoTarget.of("house")!!
        TutoTestHooks.kitOffer = false
        TutoTestHooks.player = ears
        h.start(simple = false, build = {
            session.edit { target.blank() }
            PrefsTutoStore(h.app).save(projectId, TutoProgress("house", TutoLevel.Pro))
        })
        assertTrue("the tutorial runs", shell.reached { shell.has("tuto_panel") })
        shell.tap("tab_Sons")
        shell.idle(300)
        assertTrue("Sons names the « ${DefaultNames.ACID} » track", shell.has("sons_Acide") && shows(DefaultNames.ACID))
        shell.tap("tab_Mix")
        shell.idle(600)
        assertTrue("the Mix is on screen", shell.has("mixer"))
        assertTrue("the Mix has no « ${DefaultNames.ACID} » strip: the track Sons names is missing there", shows(DefaultNames.ACID))
    }

    /** Outside a tutorial it was already so. */
    @Test
    fun theMixNamesTheAcidAsSonsDoes() {
        h.start(simple = false)
        shell.tap("tab_Mix")
        shell.idle(600)
        assertTrue("the Mix has no « ${DefaultNames.ACID} » strip", shows(DefaultNames.ACID))
    }
}
