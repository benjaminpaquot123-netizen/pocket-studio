package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.i18n.tr
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

/**
 * Audit ultra round 6, N16 (seat C b #40): the song playing, Plus › Mes projets, then « ← »: « l'Accueil, et la musique
 * s'arrête sans le dire ». The way back to « Que veux-tu faire ? » stays (the owner's choice of 2026-10-29) and the
 * music still stops there (the home screen plays nothing); when it was playing, a short message now says it stopped
 * and that the song is saved. Nothing is said when nothing was playing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-xhdpi")
class Tour6HomeStopsMusicTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val h = AppHarness(compose)
    private val shell = Tour6Shell(compose)

    @Before
    fun quiet() = ShadowToast.reset()

    @After
    fun back() = h.finish()

    private fun home() = shell.has("launch_page") || shell.has("launch")

    private fun play() {
        h.vm.togglePlay()
        shell.idle(100)
        assertTrue("the song plays", h.vm.transport.value.playing)
    }

    /** Plus (the Mode simple: the thumb's ⋮) › Mes projets › « ← ». */
    private fun backFromMyProjects(simple: Boolean) {
        shell.tap(if (simple) "thumb_more" else "tab_Plus")
        shell.tap("plus_projects")
        h.awaitTag("projects_back")
        shell.tap("projects_back")
        assertTrue("« ← » of Mes projets leads to « Que veux-tu faire ? »", shell.reached(5_000) { home() })
        shell.reached(5_000) { !h.vm.transport.value.playing }
        shell.idle(300)
    }

    private fun saidWhenTheMusicStops(simple: Boolean, lang: Lang) {
        h.start(simple = simple, lang = lang)
        play()
        backFromMyProjects(simple)
        assertFalse("the home screen plays nothing", h.vm.transport.value.playing)
        // (Said once the song is saved, a moment after the music stops.)
        shell.reached(3_000) { ShadowToast.getTextOfLatestToast() != null }
        val said = ShadowToast.getTextOfLatestToast()
        assertNotNull("the music stopped on the way to « Que veux-tu faire ? » and nothing said so", said)
        assertTrue("« $said » says the music stopped (« ${tr("Arrêté")} »)", said!!.contains(tr("Arrêté")))
        assertTrue("« $said » is in ${lang.code}", lang == Lang.FR || !said.contains("Arrêté"))
    }

    @Test fun backFromMyProjectsSaysTheMusicStoppedInPro() = saidWhenTheMusicStops(simple = false, lang = Lang.FR)

    @Test fun backFromMyProjectsSaysTheMusicStoppedInSimple() = saidWhenTheMusicStops(simple = true, lang = Lang.FR)

    @Test fun backFromMyProjectsSaysTheMusicStoppedInPolish() = saidWhenTheMusicStops(simple = false, lang = Lang.PL)

    /** « Accueil » of Plus stops the music the same way: said the same way. */
    @Test
    fun homeFromPlusSaysTheMusicStopped() {
        h.start()
        play()
        shell.tap("tab_Plus")
        shell.tap("plus_home")
        assertTrue(shell.reached(5_000) { home() && !h.vm.transport.value.playing })
        shell.reached(3_000) { ShadowToast.getTextOfLatestToast() != null }
        val said = ShadowToast.getTextOfLatestToast()
        assertNotNull("the music stopped on the way to « Que veux-tu faire ? » and nothing said so", said)
        assertTrue("« $said » says the music stopped", said!!.contains(tr("Arrêté")))
    }

    /** Nothing playing: nothing stops, nothing to say. */
    @Test
    fun nothingIsSaidWhenNothingPlayed() {
        h.start()
        backFromMyProjects(simple = false)
        assertNull("a message about music that was not playing", ShadowToast.getTextOfLatestToast())
    }
}
