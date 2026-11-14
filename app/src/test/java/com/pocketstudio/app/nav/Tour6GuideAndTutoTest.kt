package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.tuto.PrefsTutoStore
import com.pocketstudio.app.tuto.TutoTestHooks
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.model.tuto.TutoTarget
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Audit ultra round 6, the Guide and the tutorial on one screen.
 *
 * T13 (seat D a #99, Japanese): « Quitter le tuto » left the line « Reprendre le tuto » AND the Guide's bar under the
 * page (« ガイド · 2つ目のパートを作ろう 42 % »): two guides, the second one about something the chapter never said.
 * While a tutorial runs or waits to be resumed, the Guide's bar stays away.
 *
 * N1 (seat D c #55, Chinese): Plus › Aide › « Ouvrir le Guide de mon morceau » during the tutorial only changed the
 * tab: no Guide, the tutorial's panel still there. Asked for by name, the Guide now shows, in the tutorial's place;
 * folded, it gives the place back.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-xhdpi")
class Tour6GuideAndTutoTest {
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

    /** The house tutorial's project, its course running ([active]) or quit (« Reprendre le tuto »). */
    private fun tuto(simple: Boolean, active: Boolean) {
        val target = TutoTarget.of("house")!!
        TutoTestHooks.kitOffer = false
        TutoTestHooks.player = ears
        h.start(simple = simple, build = {
            session.edit { target.blank() }
            PrefsTutoStore(h.app).save(projectId, TutoProgress("house", if (simple) TutoLevel.Facile else TutoLevel.Pro, active = active))
        })
        h.vm.guideRenderForTest = { _, _ -> FloatArray(48_000) { 0.3f * kotlin.math.sin(it * 0.05f) } to FloatArray(2) }
        assertTrue("the tutorial's line under the page", shell.reached { shell.has(if (active) "tuto_panel" else "tuto_resume") })
        // The Guide's own reading of the song has the time to come.
        shell.reached(3_000) { !h.vm.guide.ui.value.loading }
        shell.idle(600)
    }

    private fun guideOnScreen() = shell.has("guide_bar") || shell.has("guide_panel")

    private fun noSecondGuideOnceQuit(simple: Boolean) {
        tuto(simple, active = false)
        assertTrue(shell.has("tuto_resume"))
        assertFalse("« Reprendre le tuto » and the Guide's bar « ${shell.textOf("guide_bar_step")} » are both under the page", guideOnScreen())
        // On every screen of the project.
        val tabs = if (simple) listOf("tab_simple_Rythme", "tab_simple_Clavier", "tab_simple_Mix", "tab_simple_Morceau") else listOf("tab_Sons", "tab_Mix", "tab_Structure")
        tabs.forEach { tab ->
            shell.tap(tab)
            shell.idle(300)
            assertFalse("$tab: the Guide's bar « ${shell.textOf("guide_bar_step")} » next to « Reprendre le tuto »", shell.has("tuto_resume") && guideOnScreen())
        }
    }

    @Test fun aTutorialQuitShowsNoSecondGuideInSimple() = noSecondGuideOnceQuit(simple = true)

    @Test fun aTutorialQuitShowsNoSecondGuideInPro() = noSecondGuideOnceQuit(simple = false)

    @Test
    fun aTutorialRunningShowsNoSecondGuide() {
        tuto(simple = true, active = true)
        assertFalse("the tutorial's panel and the Guide's bar are both under the page", guideOnScreen())
    }

    private fun theGuideAskedForShows(simple: Boolean, active: Boolean) {
        tuto(simple, active)
        shell.tap(if (simple) "thumb_more" else "tab_Plus")
        shell.tap("tool_Aide")
        shell.tap("help_steps")
        val shown = shell.reached(5_000) { shell.has("guide_panel") }
        assertTrue("« Ouvrir le Guide de mon morceau » touched: no Guide on screen (the tutorial's line still there: ${shell.has("tuto_panel") || shell.has("tuto_resume")})", shown)
        // One guide at a time: the tutorial's line gives its place while the Guide is open.
        assertFalse("the Guide's panel and the tutorial's are both open", shell.has("tuto_panel") || shell.has("tuto_resume"))
        // Folded, the Guide gives the place back to the tutorial.
        shell.tap("guide_bar")
        assertTrue("the tutorial's line is back once the Guide is folded", shell.reached(5_000) { shell.has(if (active) "tuto_panel" else "tuto_resume") })
        assertFalse("the Guide's bar stays under the tutorial's line", guideOnScreen())
    }

    @Test fun duringATutorialTheGuideAskedForShowsInSimple() = theGuideAskedForShows(simple = true, active = true)

    @Test fun duringATutorialTheGuideAskedForShowsInPro() = theGuideAskedForShows(simple = false, active = true)

    @Test fun afterATutorialQuitTheGuideAskedForShows() = theGuideAskedForShows(simple = true, active = false)
}
