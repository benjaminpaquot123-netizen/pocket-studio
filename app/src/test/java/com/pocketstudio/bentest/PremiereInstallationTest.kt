package com.pocketstudio.bentest

import android.view.inspector.WindowInspector
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.pocketstudio.app.journey.ComposeRuleDriver
import com.pocketstudio.app.journey.Journey
import com.pocketstudio.app.nav.AppHarness
import com.pocketstudio.app.onboarding.Intro
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * The essai éclair as Benjamin's phone ran it on 2026-11-11 (docs/tests-telephone/2026-11-11-eclair-1):
 * BenStudio Test just installed, so a first launch (the welcome still to come, the Mode simple, no
 * « Que veux-tu faire ? » yet), and the robot starting while the opening still plays over the studio.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class PremiereInstallationTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val h = AppHarness(compose)
    private var opened = 0L

    /** The test's thread is the app's main thread; the opening ends 1.5 s after the studio is asked for (6.5 s on the phone). */
    private val here = object : MainThread {
        override fun <T> run(block: () -> T): T = block()
        override fun pause(ms: Long) {
            ComposeRuleDriver(compose).idle(ms)
            if (Intro.playing && opened > 0 && System.currentTimeMillis() - opened > 1_500) Intro.playing = false
        }
    }

    @After
    fun back() {
        Intro.playing = false
        h.finish()
    }

    @Test
    fun theEssaiEclairOnAFirstInstall() {
        // As MainActivity at a start: the opening plays, the studio starts underneath, the welcome waits for its end.
        Intro.playing = true
        h.start(firstLaunch = true, launchChoice = true)
        val dir = File(h.app.cacheDir, "premiere-installation").apply { deleteRecursively() }
        val carnet = Carnet(File(dir, "carnet.tsv"))
        val appli = object : Appli {
            override fun ouvrirStudio() {
                opened = System.currentTimeMillis()
            }

            override fun export() = Appli.ExportVu(false, emptyList(), null)
            override fun capture(file: File) = Captures.windows(WindowInspector.getGlobalWindowViews(), file)
            override fun demander(question: String, maxMs: Long): Boolean? = true
        }
        val sonde = object : Sonde {
            override fun lire() = Sonde.Lecture(xruns = 0, cpu = 0.3f, peak = 0.5f, nonFinite = false, running = true)
        }
        val essai = EssaiEclair(Journey(InAppDriver(here)), sonde, appli, carnet, dir, ecouteMs = 1_000)
        Passage(carnet, essai.etapes()).run()
        val failed = carnet.lines().filter { it.kind == Carnet.ECHEC }.joinToString("\n") { "${it.step}: ${it.text}" }
        assertEquals("no step fails on a first install", "", failed)
    }
}
